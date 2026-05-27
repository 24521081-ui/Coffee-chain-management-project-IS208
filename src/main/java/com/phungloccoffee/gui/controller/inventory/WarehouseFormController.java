package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.AutoCodeGenerator;
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

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WarehouseFormController {
    @FXML private TextField receiptCodeField;
    @FXML private DatePicker receiptDatePicker;
    @FXML private ComboBox<String> supplierComboBox;
    @FXML private TextField createdByField;
    @FXML private TextField attachmentField;
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
    @FXML private TableColumn<ImportDetailRow, String> detailPriceColumn;
    @FXML private TableColumn<ImportDetailRow, String> detailTotalColumn;
    @FXML private TableColumn<ImportDetailRow, Void> detailActionColumn;

    private final ObservableList<MaterialRow> materials = FXCollections.observableArrayList();
    private final ObservableList<ImportDetailRow> details = FXCollections.observableArrayList();
    private final NumberFormat currencyFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
    private final List<String> savedReceiptCodes = new ArrayList<>();

    @FXML
    private void initialize() {
        receiptDatePicker.setValue(LocalDate.now());
        setupAutoCodeField();
        supplierComboBox.getItems().setAll("Công ty cà phê Phụng Lộc", "Vinamilk", "Bao bì An Phát", "Sweet Syrup Việt Nam");
        supplierComboBox.getSelectionModel().selectFirst();

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
            updateTotal();
        });
        detailPriceColumn.setCellValueFactory(new PropertyValueFactory<>("priceText"));
        detailTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalText"));
        detailActionColumn.setCellFactory(column -> new RemoveCell());
        detailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        detailTable.setItems(details);

        loadMaterials();
    }

    private void loadMaterials() {
        materials.setAll(
                new MaterialRow("NL001", "Cà phê rang xay", "kg", 34, 180000),
                new MaterialRow("NL002", "Sữa tươi không đường", "lít", 12, 32000),
                new MaterialRow("NL003", "Đường cát trắng", "kg", 25, 22000),
                new MaterialRow("NL004", "Ly giấy M", "cái", 420, 1200)
        );
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
            AlertUtils.showWarning("Vui lòng chọn nguyên liệu cần nhập.");
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
            detailTable.refresh();
        }
        updateTotal();
    }

    @FXML
    private void saveDraft() {
        String code = consumeReceiptCode();
        AlertUtils.showInfo("Đã lưu nháp phiếu nhập kho " + code + ".");
    }

    @FXML
    private void saveImport() {
        if (details.isEmpty()) {
            AlertUtils.showWarning("Vui lòng thêm ít nhất một nguyên liệu vào phiếu nhập.");
            return;
        }
        String code = consumeReceiptCode();
        AlertUtils.showInfo("Đã lưu phiếu nhập kho " + code + " trên giao diện.");
    }

    @FXML
    private void cancelForm() {
        details.clear();
        updateTotal();
        noteArea.clear();
        attachmentField.clear();
    }

    private String consumeReceiptCode() {
        String code = nextReceiptCode();
        savedReceiptCodes.add(code);
        receiptCodeField.setText(nextReceiptCode());
        return code;
    }

    private String nextReceiptCode() {
        return AutoCodeGenerator.generateNextCode("PN", savedReceiptCodes);
    }

    private void updateTotal() {
        double total = details.stream().mapToDouble(ImportDetailRow::getTotal).sum();
        totalAmountLabel.setText(formatCurrency(total));
    }

    private String formatCurrency(double value) {
        return currencyFormat.format(value) + " đ";
    }

    private class RemoveCell extends TableCell<ImportDetailRow, Void> {
        private final Button removeButton = new Button("Xóa");
        private final HBox box = new HBox(removeButton);

        RemoveCell() {
            removeButton.getStyleClass().addAll("action-button", "action-lock-button");
            box.setAlignment(Pos.CENTER_LEFT);
            removeButton.setOnAction(event -> {
                ImportDetailRow row = getTableRow().getItem();
                if (row != null) {
                    details.remove(row);
                    updateTotal();
                }
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : box);
        }
    }

    public class MaterialRow {
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

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getUnit() {
            return unit;
        }

        public double getPrice() {
            return price;
        }

        public String getStockText() {
            return trim(stock) + " " + unit;
        }

        public String getPriceText() {
            return formatCurrency(price);
        }
    }

    public class ImportDetailRow {
        private final String code;
        private final String name;
        private final String unit;
        private double quantity;
        private final double price;

        public ImportDetailRow(String code, String name, String unit, double quantity, double price) {
            this.code = code;
            this.name = name;
            this.unit = unit;
            this.quantity = quantity;
            this.price = price;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getUnit() {
            return unit;
        }

        public double getQuantity() {
            return quantity;
        }

        public void setQuantity(double quantity) {
            this.quantity = quantity;
        }

        public double getTotal() {
            return quantity * price;
        }

        public String getPriceText() {
            return formatCurrency(price);
        }

        public String getTotalText() {
            return formatCurrency(getTotal());
        }
    }

    private String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
    }
}
