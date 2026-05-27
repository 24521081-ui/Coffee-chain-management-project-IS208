package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.WarehouseWorkflowBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.gui.service.SessionManager;
import com.phungloccoffee.model.InventoryItem;
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
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.converter.DoubleStringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExportFormController {
    @FXML private TextField exportCodeField;
    @FXML private DatePicker exportDatePicker;
    @FXML private ComboBox<String> reasonComboBox;
    @FXML private TextField createdByField;
    @FXML private TextField receiverField;
    @FXML private TextField totalQuantityField;
    @FXML private TextArea noteArea;
    @FXML private TextField materialSearchField;
    @FXML private TableView<MaterialRow> materialTable;
    @FXML private TableColumn<MaterialRow, String> materialCodeColumn;
    @FXML private TableColumn<MaterialRow, String> materialNameColumn;
    @FXML private TableColumn<MaterialRow, String> materialUnitColumn;
    @FXML private TableColumn<MaterialRow, String> materialStockColumn;
    @FXML private TableColumn<MaterialRow, String> materialWarningColumn;
    @FXML private TableView<ExportDetailRow> detailTable;
    @FXML private TableColumn<ExportDetailRow, String> detailCodeColumn;
    @FXML private TableColumn<ExportDetailRow, String> detailNameColumn;
    @FXML private TableColumn<ExportDetailRow, String> detailUnitColumn;
    @FXML private TableColumn<ExportDetailRow, Double> detailQuantityColumn;
    @FXML private TableColumn<ExportDetailRow, String> detailStockColumn;
    @FXML private TableColumn<ExportDetailRow, String> detailAfterColumn;
    @FXML private TableColumn<ExportDetailRow, String> detailStatusColumn;
    @FXML private TableColumn<ExportDetailRow, Void> detailActionColumn;

    private final WarehouseWorkflowBUS workflowBUS = new WarehouseWorkflowBUS();
    private final ObservableList<MaterialRow> materials = FXCollections.observableArrayList();
    private final ObservableList<ExportDetailRow> details = FXCollections.observableArrayList();
    private final List<String> submittedExportCodes = new ArrayList<>();

    @FXML
    private void initialize() {
        exportDatePicker.setValue(LocalDate.now());
        createdByField.setText(SessionManager.getCurrentUser() == null ? "" : SessionManager.getCurrentUser().getFullName());
        createdByField.setEditable(false);
        setupAutoCodeField();
        reasonComboBox.getItems().setAll("Bá»• sung ca bÃ¡n", "Xuáº¥t há»§y", "Äiá»u chuyá»ƒn ná»™i bá»™", "KhÃ¡c");
        reasonComboBox.getSelectionModel().selectFirst();
        totalQuantityField.setEditable(false);

        materialCodeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        materialNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        materialUnitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        materialStockColumn.setCellValueFactory(new PropertyValueFactory<>("stockText"));
        materialWarningColumn.setCellValueFactory(new PropertyValueFactory<>("warningText"));
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
            ExportDetailRow row = event.getRowValue();
            row.setQuantity(Math.max(0, event.getNewValue()));
            detailTable.refresh();
            updateTotalQuantity();
        });
        detailStockColumn.setCellValueFactory(new PropertyValueFactory<>("stockText"));
        detailAfterColumn.setCellValueFactory(new PropertyValueFactory<>("stockAfterText"));
        detailStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        detailStatusColumn.setCellFactory(column -> new StatusCell<>());
        detailActionColumn.setCellFactory(column -> new RemoveCell());
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        detailTable.setItems(details);

        loadMaterials();
    }

    private void loadMaterials() {
        try {
            List<InventoryItem> inventoryItems = workflowBUS.loadMaterialsForCurrentBranch();
            materials.setAll(inventoryItems.stream().map(MaterialRow::from).toList());
        } catch (Exception e) {
            materials.setAll(
                    new MaterialRow("NL001", "Ca phe rang xay", "KG", 34, 20),
                    new MaterialRow("NL002", "Sá»¯a tÆ°Æ¡i khÃ´ng Ä‘Æ°á»ng", "L", 12, 15)
            );
        }
    }

    private void setupAutoCodeField() {
        submittedExportCodes.addAll(List.of("PX001", "PX002"));
        exportCodeField.setEditable(false);
        exportCodeField.getStyleClass().add("readonly-code-field");
        exportCodeField.setText(nextExportCode());
    }

    @FXML
    private void handleAddSelectedMaterial() {
        MaterialRow selected = materialTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarning("Vui lÃ²ng chá»n nguyÃªn liá»‡u cáº§n xuáº¥t.");
            return;
        }
        Optional<Double> quantity = promptForQuantity(selected);
        if (quantity.isEmpty()) {
            return;
        }
        ExportDetailRow existing = details.stream()
                .filter(row -> row.getCode().equals(selected.getCode()))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            details.add(new ExportDetailRow(selected.getCode(), selected.getName(), selected.getUnit(), quantity.get(),
                    selected.getStock(), selected.getWarningLevel()));
        } else {
            existing.setQuantity(existing.getQuantity() + quantity.get());
        }
        detailTable.refresh();
        updateTotalQuantity();
    }

    private Optional<Double> promptForQuantity(MaterialRow selected) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Chá»n sá»‘ lÆ°á»£ng xuáº¥t");
        dialog.setHeaderText("Nháº­p sá»‘ lÆ°á»£ng cho " + selected.getName());
        dialog.setContentText("Sá»‘ lÆ°á»£ng:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        try {
            double value = Double.parseDouble(result.get().trim());
            if (value <= 0) {
                AlertUtils.showWarning("Sá»‘ lÆ°á»£ng xuáº¥t pháº£i lá»›n hÆ¡n 0.");
                return Optional.empty();
            }
            return Optional.of(value);
        } catch (NumberFormatException ex) {
            AlertUtils.showWarning("Sá»‘ lÆ°á»£ng khÃ´ng há»£p lá»‡.");
            return Optional.empty();
        }
    }

    @FXML
    private void saveDraft() {
        persist(false);
    }

    @FXML
    private void submitExport() {
        if (details.isEmpty()) {
            AlertUtils.showWarning("Vui lÃ²ng thÃªm Ã­t nháº¥t má»™t nguyÃªn liá»‡u vÃ o phiáº¿u xuáº¥t.");
            return;
        }
        boolean hasInvalidStock = details.stream().anyMatch(row -> row.getStockAfter() < 0);
        if (hasInvalidStock) {
            AlertUtils.showError("KhÃ´ng thá»ƒ gá»­i duyá»‡t vÃ¬ cÃ³ dÃ²ng KhÃ´ng Ä‘á»§ tá»“n.");
            return;
        }
        persist(true);
    }

    @FXML
    private void cancelForm() {
        receiverField.clear();
        totalQuantityField.clear();
        noteArea.clear();
        details.clear();
        exportCodeField.setText(nextExportCode());
    }

    private void persist(boolean submit) {
        try {
            WarehouseSlip slip = new WarehouseSlip();
            slip.setSlipId(consumeExportCode());
            slip.setReason(reasonComboBox.getValue());
            slip.setNote(buildNote());
            slip.setLines(details.stream().map(ExportDetailRow::toLine).toList());
            if (submit) {
                workflowBUS.submitExport(slip);
                AlertUtils.showInfo("Phiáº¿u xuáº¥t kho Ä‘Ã£ gá»­i duyá»‡t. Tá»“n kho chÆ°a bá»‹ trá»«.");
            } else {
                workflowBUS.saveExportDraft(slip);
                AlertUtils.showInfo("ÄÃ£ lÆ°u nhÃ¡p phiáº¿u xuáº¥t kho.");
            }
            receiverField.clear();
            totalQuantityField.clear();
            noteArea.clear();
            details.clear();
            loadMaterials();
            updateTotalQuantity();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private String buildNote() {
        StringBuilder builder = new StringBuilder();
        if (receiverField.getText() != null && !receiverField.getText().isBlank()) {
            builder.append("NÆ¡i nháº­n: ").append(receiverField.getText().trim());
        }
        if (noteArea.getText() != null && !noteArea.getText().isBlank()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(noteArea.getText().trim());
        }
        return builder.toString();
    }

    private void updateTotalQuantity() {
        double total = details.stream().mapToDouble(ExportDetailRow::getQuantity).sum();
        totalQuantityField.setText(quantityText(total, "Ä‘Æ¡n vá»‹"));
    }

    private String consumeExportCode() {
        String code = exportCodeField.getText();
        if (code == null || code.isBlank()) {
            code = nextExportCode();
        }
        submittedExportCodes.add(code);
        exportCodeField.setText(nextExportCode());
        return code;
    }

    private String nextExportCode() {
        return AutoCodeGenerator.generateNextCode("PX", submittedExportCodes);
    }

    private String quantityText(double value, String unit) {
        return trim(value) + " " + unit;
    }

    private String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
    }

    private class StatusCell<T> extends TableCell<T, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(status);
            badge.getStyleClass().addAll("status-badge", switch (status) {
                case "KhÃ´ng Ä‘á»§ tá»“n" -> "status-danger";
                case "DÆ°á»›i má»©c cáº£nh bÃ¡o" -> "status-warning";
                default -> "status-success";
            });
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    private class RemoveCell extends TableCell<ExportDetailRow, Void> {
        private final Button removeButton = new Button("XÃ³a");
        private final HBox box = new HBox(removeButton);

        RemoveCell() {
            removeButton.getStyleClass().addAll("action-button", "action-lock-button");
            box.setAlignment(Pos.CENTER_LEFT);
            removeButton.setOnAction(event -> {
                ExportDetailRow row = getTableRow().getItem();
                if (row != null) {
                    details.remove(row);
                    updateTotalQuantity();
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
        private final double warningLevel;

        public MaterialRow(String code, String name, String unit, double stock, double warningLevel) {
            this.code = code;
            this.name = name;
            this.unit = unit;
            this.stock = stock;
            this.warningLevel = warningLevel;
        }

        public static MaterialRow from(InventoryItem item) {
            return new MaterialRow(
                    item.getItemCode(),
                    item.getItemName(),
                    item.getUnit(),
                    item.getQuantityOnHand().doubleValue(),
                    item.getReorderLevel().doubleValue()
            );
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getUnit() { return unit; }
        public double getStock() { return stock; }
        public double getWarningLevel() { return warningLevel; }
        public String getStockText() { return trim(stock) + " " + unit; }
        public String getWarningText() { return trim(warningLevel) + " " + unit; }

        private static String trim(double value) {
            return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
        }
    }

    public class ExportDetailRow {
        private final SimpleStringProperty code;
        private final SimpleStringProperty name;
        private final SimpleStringProperty unit;
        private final SimpleDoubleProperty quantity;
        private final double stock;
        private final double warningLevel;

        public ExportDetailRow(String code, String name, String unit, double quantity, double stock, double warningLevel) {
            this.code = new SimpleStringProperty(code);
            this.name = new SimpleStringProperty(name);
            this.unit = new SimpleStringProperty(unit);
            this.quantity = new SimpleDoubleProperty(quantity);
            this.stock = stock;
            this.warningLevel = warningLevel;
        }

        public String getCode() { return code.get(); }
        public String getName() { return name.get(); }
        public String getUnit() { return unit.get(); }
        public double getQuantity() { return quantity.get(); }
        public void setQuantity(double quantity) { this.quantity.set(quantity); }
        public double getStockAfter() { return stock - getQuantity(); }
        public String getStockText() { return quantityText(stock, getUnit()); }
        public String getStockAfterText() { return quantityText(getStockAfter(), getUnit()); }

        public String getStatus() {
            if (getStockAfter() < 0) {
                return "KhÃ´ng Ä‘á»§ tá»“n";
            }
            if (getStockAfter() < warningLevel) {
                return "DÆ°á»›i má»©c cáº£nh bÃ¡o";
            }
            return "Há»£p lá»‡";
        }

        public WarehouseSlipLine toLine() {
            WarehouseSlipLine line = new WarehouseSlipLine();
            line.setItemId(getCode());
            line.setItemName(getName());
            line.setUnit(getUnit());
            line.setQuantity(BigDecimal.valueOf(getQuantity()));
            line.setNote("Tá»“n hiá»‡n táº¡i: " + getStockText());
            return line;
        }
    }
}
