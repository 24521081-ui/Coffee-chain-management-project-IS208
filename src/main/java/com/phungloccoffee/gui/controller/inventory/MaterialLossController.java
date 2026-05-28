package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.MaterialLossBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.MaterialLossRecord;
import com.phungloccoffee.util.AlertUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MaterialLossController {
    private static final String STATUS_RECORDED = "Đã ghi nhận";
    private static final String REASON_OTHER = "Khác";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private ComboBox<InventoryItem> materialComboBox;
    @FXML private TextField quantityField;
    @FXML private ComboBox<String> reasonComboBox;
    @FXML private TextArea noteArea;
    @FXML private TableView<LossRow> lossTable;
    @FXML private TableColumn<LossRow, String> codeColumn;
    @FXML private TableColumn<LossRow, String> materialColumn;
    @FXML private TableColumn<LossRow, String> quantityColumn;
    @FXML private TableColumn<LossRow, String> reasonColumn;
    @FXML private TableColumn<LossRow, String> statusColumn;

    private final MaterialLossBUS materialLossBUS = new MaterialLossBUS();
    private final ObservableList<LossRow> losses = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        reasonComboBox.getItems().setAll(
                "Làm đổ khi pha chế",
                "Pha lỗi món",
                "Hư hỏng / rơi vỡ",
                "Quá hạn sử dụng",
                REASON_OTHER
        );
        reasonComboBox.getSelectionModel().selectFirst();

        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        materialColumn.setCellValueFactory(new PropertyValueFactory<>("material"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        reasonColumn.setCellValueFactory(new PropertyValueFactory<>("reason"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        lossTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        lossTable.setItems(losses);

        loadMaterials();
        loadLossHistory();
    }

    private void loadMaterials() {
        try {
            List<InventoryItem> materials = materialLossBUS.loadMaterialsForCurrentBranch();
            materialComboBox.setItems(FXCollections.observableArrayList(materials));
            materialComboBox.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(InventoryItem item) {
                    if (item == null) {
                        return "";
                    }
                    return item.getItemCode() + " - " + item.getItemName();
                }

                @Override
                public InventoryItem fromString(String string) {
                    return null;
                }
            });
            if (!materials.isEmpty()) {
                materialComboBox.getSelectionModel().selectFirst();
            }
        } catch (DatabaseException | PermissionException | ValidationException e) {
            AlertUtils.showError(e.getMessage());
            materialComboBox.getItems().clear();
        }
    }

    private void loadLossHistory() {
        try {
            losses.setAll(materialLossBUS.loadLossHistory().stream().map(LossRow::from).toList());
        } catch (DatabaseException | PermissionException e) {
            AlertUtils.showError(e.getMessage());
            losses.clear();
        }
    }

    @FXML
    private void recordLoss() {
        try {
            InventoryItem material = materialComboBox.getValue();
            if (material == null) {
                throw new ValidationException("Bạn cần chọn nguyên liệu hao hụt.");
            }
            BigDecimal quantity = parseQuantity(quantityField.getText());
            String selectedReason = reasonComboBox.getValue();

            MaterialLossRecord record = new MaterialLossRecord();
            record.setMaterialId(material.getItemCode());
            record.setMaterialName(material.getItemName());
            record.setUnit(material.getUnit());
            record.setQuantity(quantity);
            record.setReason(selectedReason);
            record.setNote(noteArea.getText() == null ? null : noteArea.getText().trim());

            materialLossBUS.recordLoss(record);
            AlertUtils.showInfo("Ghi nhận hao hụt nguyên liệu thành công.");
            quantityField.clear();
            noteArea.clear();
            reasonComboBox.getSelectionModel().selectFirst();
            loadLossHistory();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private BigDecimal parseQuantity(String value) throws ValidationException {
        try {
            BigDecimal quantity = new BigDecimal(value == null ? "" : value.trim());
            if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Số lượng hao hụt phải lớn hơn 0.");
            }
            return quantity;
        } catch (NumberFormatException e) {
            throw new ValidationException("Số lượng hao hụt không hợp lệ.");
        }
    }

    private static class StatusCell<T> extends TableCell<T, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(status);
            badge.getStyleClass().addAll("status-badge", "status-success");
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    public static class LossRow {
        private final SimpleStringProperty code;
        private final SimpleStringProperty material;
        private final SimpleStringProperty quantity;
        private final SimpleStringProperty reason;
        private final SimpleStringProperty status;

        public LossRow(String code, String material, String quantity, String reason, String status) {
            this.code = new SimpleStringProperty(code);
            this.material = new SimpleStringProperty(material);
            this.quantity = new SimpleStringProperty(quantity);
            this.reason = new SimpleStringProperty(reason);
            this.status = new SimpleStringProperty(status);
        }

        public static LossRow from(MaterialLossRecord record) {
            String createdAt = record.getCreatedAt() == null ? "" : DATE_TIME_FORMATTER.format(record.getCreatedAt());
            String materialLabel = record.getMaterialId() + " - " + record.getMaterialName();
            String quantityLabel = formatQuantity(record.getQuantity(), record.getUnit());
            String reasonLabel = record.getReason() == null ? "" : record.getReason();
            if (!createdAt.isBlank()) {
                reasonLabel = reasonLabel + "\nGhi nhận lúc: " + createdAt;
            }
            return new LossRow(record.getLossId(), materialLabel, quantityLabel, reasonLabel.trim(), STATUS_RECORDED);
        }

        public String getCode() {
            return code.get();
        }

        public String getMaterial() {
            return material.get();
        }

        public String getQuantity() {
            return quantity.get();
        }

        public String getReason() {
            return reason.get();
        }

        public String getStatus() {
            return status.get();
        }

        private static String formatQuantity(BigDecimal value, String unit) {
            BigDecimal clean = value == null ? BigDecimal.ZERO : value.stripTrailingZeros();
            return clean.toPlainString() + " " + (unit == null ? "" : unit);
        }
    }
}
