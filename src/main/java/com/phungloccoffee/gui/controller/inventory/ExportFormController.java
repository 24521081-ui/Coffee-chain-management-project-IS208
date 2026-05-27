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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ExportFormController {
    @FXML private TextField exportCodeField;
    @FXML private DatePicker exportDatePicker;
    @FXML private ComboBox<String> reasonComboBox;
    @FXML private TextField createdByField;
    @FXML private TextField receiverField;
    @FXML private TextField attachmentField;
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

    private final ObservableList<MaterialRow> materials = FXCollections.observableArrayList();
    private final ObservableList<ExportDetailRow> details = FXCollections.observableArrayList();
    private final List<String> submittedExportCodes = new ArrayList<>();

    @FXML
    private void initialize() {
        exportDatePicker.setValue(LocalDate.now());
        setupAutoCodeField();
        reasonComboBox.getItems().setAll("Bổ sung ca bán", "Chuyển quầy pha chế", "Hư hỏng", "Điều chuyển nội bộ");
        reasonComboBox.getSelectionModel().selectFirst();

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
        materials.setAll(
                new MaterialRow("NL001", "Cà phê rang xay", "kg", 34, 20),
                new MaterialRow("NL002", "Sữa tươi không đường", "lít", 12, 15),
                new MaterialRow("NL003", "Đường cát trắng", "kg", 25, 10),
                new MaterialRow("NL004", "Ly giấy M", "cái", 420, 200)
        );
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
            AlertUtils.showWarning("Vui lòng chọn nguyên liệu cần xuất.");
            return;
        }
        ExportDetailRow existing = details.stream()
                .filter(row -> row.getCode().equals(selected.getCode()))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            details.add(new ExportDetailRow(selected.getCode(), selected.getName(), selected.getUnit(), 1,
                    selected.getStock(), selected.getWarningLevel()));
        } else {
            existing.setQuantity(existing.getQuantity() + 1);
            detailTable.refresh();
        }
    }

    @FXML
    private void saveDraft() {
        String code = consumeExportCode();
        AlertUtils.showInfo("Đã lưu nháp phiếu xuất kho " + code + ".");
    }

    @FXML
    private void submitExport() {
        if (details.isEmpty()) {
            AlertUtils.showWarning("Vui lòng thêm ít nhất một nguyên liệu vào phiếu xuất.");
            return;
        }
        boolean hasInvalidStock = details.stream().anyMatch(row -> row.getStockAfter() < 0);
        if (hasInvalidStock) {
            AlertUtils.showError("Không thể gửi duyệt vì có dòng Không đủ tồn.");
            return;
        }
        String code = consumeExportCode();
        AlertUtils.showInfo("Đã gửi duyệt phiếu xuất kho " + code + " trên giao diện.");
    }

    @FXML
    private void cancelForm() {
        details.clear();
        noteArea.clear();
        attachmentField.clear();
    }

    private String consumeExportCode() {
        String code = nextExportCode();
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
                case "Không đủ tồn" -> "status-danger";
                case "Dưới mức cảnh báo" -> "status-warning";
                default -> "status-success";
            });
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    private class RemoveCell extends TableCell<ExportDetailRow, Void> {
        private final Button removeButton = new Button("Xóa");
        private final HBox box = new HBox(removeButton);

        RemoveCell() {
            removeButton.getStyleClass().addAll("action-button", "action-lock-button");
            box.setAlignment(Pos.CENTER_LEFT);
            removeButton.setOnAction(event -> {
                ExportDetailRow row = getTableRow().getItem();
                if (row != null) {
                    details.remove(row);
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
        private final double warningLevel;

        public MaterialRow(String code, String name, String unit, double stock, double warningLevel) {
            this.code = code;
            this.name = name;
            this.unit = unit;
            this.stock = stock;
            this.warningLevel = warningLevel;
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

        public double getStock() {
            return stock;
        }

        public double getWarningLevel() {
            return warningLevel;
        }

        public String getStockText() {
            return quantityText(stock, unit);
        }

        public String getWarningText() {
            return quantityText(warningLevel, unit);
        }
    }

    public class ExportDetailRow {
        private final String code;
        private final String name;
        private final String unit;
        private double quantity;
        private final double stock;
        private final double warningLevel;

        public ExportDetailRow(String code, String name, String unit, double quantity, double stock, double warningLevel) {
            this.code = code;
            this.name = name;
            this.unit = unit;
            this.quantity = quantity;
            this.stock = stock;
            this.warningLevel = warningLevel;
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

        public double getStockAfter() {
            return stock - quantity;
        }

        public String getStockText() {
            return quantityText(stock, unit);
        }

        public String getStockAfterText() {
            return quantityText(getStockAfter(), unit);
        }

        public String getStatus() {
            if (getStockAfter() < 0) {
                return "Không đủ tồn";
            }
            if (getStockAfter() < warningLevel) {
                return "Dưới mức cảnh báo";
            }
            return "Hợp lệ";
        }
    }
}
