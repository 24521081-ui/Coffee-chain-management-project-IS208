package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.WarehouseWorkflowBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.gui.service.SessionManager;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.NhaCungCap;
import com.phungloccoffee.model.WarehouseSlip;
import com.phungloccoffee.model.WarehouseSlipLine;
import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.AutoCodeGenerator;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.DoubleStringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WarehouseFormController {
    @FXML private TextField receiptCodeField;
    @FXML private DatePicker receiptDatePicker;
    @FXML private ComboBox<NhaCungCap> supplierComboBox;
    @FXML private TextField createdByField;
    @FXML private TextField attachmentField;
    @FXML private TextField totalQuantityField;
    @FXML private TextArea noteArea;
    @FXML private TextField materialSearchField;
    @FXML private TableView<MaterialRow> materialTable;
    @FXML private TableColumn<MaterialRow, String> materialCodeColumn;
    @FXML private TableColumn<MaterialRow, String> materialNameColumn;
    @FXML private TableColumn<MaterialRow, String> materialUnitColumn;
    @FXML private TableColumn<MaterialRow, String> materialStockColumn;
    @FXML private TableColumn<MaterialRow, String> materialPriceColumn;
    @FXML private Label totalAmountLabel;
    @FXML private TableView<ImportDetailRow> detailTable;
    @FXML private TableColumn<ImportDetailRow, String> detailCodeColumn;
    @FXML private TableColumn<ImportDetailRow, String> detailNameColumn;
    @FXML private TableColumn<ImportDetailRow, String> detailUnitColumn;
    @FXML private TableColumn<ImportDetailRow, Double> detailQuantityColumn;
    @FXML private TableColumn<ImportDetailRow, Double> detailPriceColumn;
    @FXML private TableColumn<ImportDetailRow, String> detailTotalColumn;
    @FXML private TableColumn<ImportDetailRow, Void> detailActionColumn;

    private final WarehouseWorkflowBUS workflowBUS = new WarehouseWorkflowBUS();
    private final ObservableList<MaterialRow> materials = FXCollections.observableArrayList();
    private final ObservableList<ImportDetailRow> details = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    private final List<String> savedReceiptCodes = new ArrayList<>();

    @FXML
    private void initialize() {
        receiptDatePicker.setValue(LocalDate.now());
        createdByField.setText(SessionManager.getCurrentUser() == null ? "" : SessionManager.getCurrentUser().getFullName());
        createdByField.setEditable(false);
        setupAutoCodeField();

        materialCodeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        materialNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        materialUnitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        materialStockColumn.setCellValueFactory(new PropertyValueFactory<>("stockText"));
        materialPriceColumn.setCellValueFactory(new PropertyValueFactory<>("priceText"));
        materialTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        materialTable.setItems(materials);
        materialTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                handleAddSelectedMaterial();
            }
        });

        detailCodeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        detailNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        detailUnitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        detailQuantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        detailQuantityColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        detailQuantityColumn.setOnEditCommit(event -> {
            ImportDetailRow row = event.getRowValue();
            row.setQuantity(Math.max(0, event.getNewValue()));
            detailTable.refresh();
            syncTotalsFromDetails();
        });
        detailPriceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        detailPriceColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        detailPriceColumn.setOnEditCommit(event -> {
            ImportDetailRow row = event.getRowValue();
            row.setPrice(Math.max(0, event.getNewValue()));
            detailTable.refresh();
            syncTotalsFromDetails();
        });
        detailTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalText"));
        detailActionColumn.setCellFactory(column -> new RemoveCell());
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        detailTable.setItems(details);

        loadSuppliers();
        loadMaterials();
    }

    private void loadSuppliers() {
        try {
            supplierComboBox.setItems(FXCollections.observableArrayList(workflowBUS.loadActiveSuppliers()));
            if (!supplierComboBox.getItems().isEmpty()) {
                supplierComboBox.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            supplierComboBox.setItems(FXCollections.observableArrayList(
                    new NhaCungCap("NCC001", "NhÃ  cung cáº¥p máº·c Ä‘á»‹nh", null, null, 1)
            ));
            supplierComboBox.getSelectionModel().selectFirst();
        }
    }

    private void loadMaterials() {
        try {
            List<InventoryItem> inventoryItems = workflowBUS.loadMaterialsForCurrentBranch();
            materials.setAll(inventoryItems.stream().map(MaterialRow::from).toList());
        } catch (Exception e) {
            materials.setAll(
                    new MaterialRow("NL001", "CÃ  phÃª rang xay", "KG", 34, 180000),
                    new MaterialRow("NL002", "Sá»¯a tÆ°Æ¡i khÃ´ng Ä‘Æ°á»ng", "L", 12, 32000)
            );
        }
    }

    private void setupAutoCodeField() {
        savedReceiptCodes.addAll(List.of("PN001", "PN002"));
        receiptCodeField.setEditable(false);
        receiptCodeField.getStyleClass().add("readonly-code-field");
        receiptCodeField.setText(nextReceiptCode());
    }

    @FXML
    private void handleAddSelectedMaterial() {
        MaterialRow selected = materialTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarning("Vui lÃ²ng chá»n nguyÃªn liá»‡u cáº§n nháº­p.");
            return;
        }
        ImportDetailRow existing = details.stream()
                .filter(row -> row.getCode().equals(selected.getCode()))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            details.add(new ImportDetailRow(selected.getCode(), selected.getName(), selected.getUnit(), 1, selected.getPrice()));
        } else {
            existing.setQuantity(existing.getQuantity() + 1);
        }
        detailTable.refresh();
        syncTotalsFromDetails();
    }

    @FXML
    private void saveDraft() {
        persist(false);
    }

    @FXML
    private void saveImport() {
        if (details.isEmpty()) {
            AlertUtils.showWarning("Vui lÃ²ng thÃªm Ã­t nháº¥t má»™t nguyÃªn liá»‡u vÃ o phiáº¿u nháº­p.");
            return;
        }
        persist(true);
    }

    @FXML
    private void cancelForm() {
        attachmentField.clear();
        totalQuantityField.clear();
        noteArea.clear();
        details.clear();
        syncTotalsFromDetails();
        receiptCodeField.setText(nextReceiptCode());
    }

    private void persist(boolean submit) {
        try {
            WarehouseSlip slip = new WarehouseSlip();
            slip.setSlipId(consumeReceiptCode());
            slip.setSupplierId(supplierComboBox.getValue() == null ? null : supplierComboBox.getValue().getNhaCungCapId());
            slip.setNote(joinNoteAndAttachment());
            slip.setLines(details.stream().map(ImportDetailRow::toLine).toList());
            if (submit) {
                workflowBUS.submitImport(slip);
                AlertUtils.showInfo("Phiáº¿u nháº­p kho Ä‘Ã£ gá»­i duyá»‡t. Tá»“n kho chÆ°a Ä‘Æ°á»£c cáº­p nháº­t.");
            } else {
                workflowBUS.saveImportDraft(slip);
                AlertUtils.showInfo("ÄÃ£ lÆ°u nhÃ¡p phiáº¿u nháº­p kho.");
            }
            attachmentField.clear();
            totalQuantityField.clear();
            noteArea.clear();
            details.clear();
            loadMaterials();
            syncTotalsFromDetails();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private String joinNoteAndAttachment() {
        String attachment = attachmentField.getText() == null ? "" : attachmentField.getText().trim();
        String note = noteArea.getText() == null ? "" : noteArea.getText().trim();
        String quantity = totalQuantityField.getText() == null ? "" : totalQuantityField.getText().trim();
        StringBuilder builder = new StringBuilder();
        if (!attachment.isBlank()) {
            builder.append("Chá»©ng tá»«: ").append(attachment);
        }
        if (!quantity.isBlank()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("Sá»‘ lÆ°á»£ng: ").append(quantity);
        }
        if (!note.isBlank()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(note);
        }
        return builder.toString();
    }

    private void syncTotalsFromDetails() {
        double totalAmount = details.stream().mapToDouble(ImportDetailRow::getTotal).sum();
        double totalQuantity = details.stream().mapToDouble(ImportDetailRow::getQuantity).sum();
        totalAmountLabel.setText(formatCurrency(totalAmount));
        totalQuantityField.setText(trim(totalQuantity));
    }

    private String consumeReceiptCode() {
        String code = receiptCodeField.getText();
        if (code == null || code.isBlank()) {
            code = nextReceiptCode();
        }
        savedReceiptCodes.add(code);
        receiptCodeField.setText(nextReceiptCode());
        return code;
    }

    private String nextReceiptCode() {
        return AutoCodeGenerator.generateNextCode("PN", savedReceiptCodes);
    }

    private String formatCurrency(double value) {
        return currencyFormat.format(value) + " Ä‘";
    }

    private String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
    }

    private class RemoveCell extends TableCell<ImportDetailRow, Void> {
        private final Button removeButton = new Button("XÃ³a");
        private final HBox box = new HBox(removeButton);

        RemoveCell() {
            removeButton.getStyleClass().addAll("action-button", "action-lock-button");
            box.setAlignment(Pos.CENTER_LEFT);
            removeButton.setOnAction(event -> {
                ImportDetailRow row = getTableRow().getItem();
                if (row != null) {
                    details.remove(row);
                    syncTotalsFromDetails();
                }
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : box);
        }
    }

    public static class MaterialRow {
        private final String code;
        private final String name;
        private final String unit;
        private final double stock;
        private final double price;

        public MaterialRow(String code, String name, String unit, double stock, double price) {
            this.code = code;
            this.name = name;
            this.unit = unit;
            this.stock = stock;
            this.price = price;
        }

        public static MaterialRow from(InventoryItem item) {
            return new MaterialRow(
                    item.getItemCode(),
                    item.getItemName(),
                    item.getUnit(),
                    item.getQuantityOnHand().doubleValue(),
                    0
            );
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getUnit() { return unit; }
        public double getPrice() { return price; }
        public String getStockText() { return valueText(stock, unit); }
        public String getPriceText() { return NumberFormat.getInstance(new Locale("vi", "VN")).format(price) + " Ä‘"; }

        private static String valueText(double value, String unit) {
            return (value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value)) + " " + unit;
        }
    }

    public class ImportDetailRow {
        private final SimpleStringProperty code;
        private final SimpleStringProperty name;
        private final SimpleStringProperty unit;
        private final SimpleDoubleProperty quantity;
        private final SimpleDoubleProperty price;

        public ImportDetailRow(String code, String name, String unit, double quantity, double price) {
            this.code = new SimpleStringProperty(code);
            this.name = new SimpleStringProperty(name);
            this.unit = new SimpleStringProperty(unit);
            this.quantity = new SimpleDoubleProperty(quantity);
            this.price = new SimpleDoubleProperty(price);
        }

        public String getCode() { return code.get(); }
        public String getName() { return name.get(); }
        public String getUnit() { return unit.get(); }
        public double getQuantity() { return quantity.get(); }
        public void setQuantity(double quantity) { this.quantity.set(quantity); }
        public double getPrice() { return price.get(); }
        public void setPrice(double value) { this.price.set(value); }
        public double getTotal() { return getQuantity() * getPrice(); }
        public String getTotalText() { return formatCurrency(getTotal()); }

        public WarehouseSlipLine toLine() {
            WarehouseSlipLine line = new WarehouseSlipLine();
            line.setItemId(getCode());
            line.setItemName(getName());
            line.setUnit(getUnit());
            line.setQuantity(BigDecimal.valueOf(getQuantity()));
            line.setUnitPrice(BigDecimal.valueOf(getPrice()));
            return line;
        }
    }
}
