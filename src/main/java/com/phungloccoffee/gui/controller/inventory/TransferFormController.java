package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.AutoCodeGenerator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;

public class TransferFormController {
    @FXML private ComboBox<String> sourceComboBox;
    @FXML private ComboBox<String> targetComboBox;
    @FXML private TableView<TransferRow> transferTable;
    @FXML private TableColumn<TransferRow, String> codeColumn;
    @FXML private TableColumn<TransferRow, String> materialColumn;
    @FXML private TableColumn<TransferRow, String> unitColumn;
    @FXML private TableColumn<TransferRow, String> quantityColumn;
    @FXML private TableColumn<TransferRow, String> statusColumn;
    @FXML private TableColumn<TransferRow, Void> actionColumn;
    private final List<String> savedTransferCodes = new ArrayList<>(List.of("DC001", "DC002"));

    @FXML
    private void initialize() {
        sourceComboBox.getItems().setAll("Kho CN001", "Kho CN002", "Kho CN003", "Kho trung tâm");
        targetComboBox.getItems().setAll("Kho CN001", "Kho CN002", "Kho CN003", "Kho CN005");
        sourceComboBox.setValue("Kho trung tâm");
        targetComboBox.setValue("Kho CN005");

        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        materialColumn.setCellValueFactory(new PropertyValueFactory<>("material"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        actionColumn.setCellFactory(column -> new ActionCell());
        transferTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        transferTable.setItems(FXCollections.observableArrayList(
                new TransferRow("NL001", "Cà phê rang xay", "kg", "10", "Hợp lệ"),
                new TransferRow("NL002", "Sữa tươi không đường", "lít", "24", "Hợp lệ"),
                new TransferRow("NL004", "Ly giấy M", "cái", "500", "Chờ kiểm tra")
        ));
    }

    @FXML
    private void saveTransfer() {
        String code = AutoCodeGenerator.generateNextCode("DC", savedTransferCodes);
        savedTransferCodes.add(code);
        AlertUtils.showInfo("Đã lưu phiếu điều chuyển " + code + " trên giao diện.");
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
            badge.getStyleClass().addAll("status-badge", "Hợp lệ".equals(status) ? "status-success" : "status-warning");
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    private static class ActionCell extends TableCell<TransferRow, Void> {
        private final HBox box = new HBox(8);
        private final Button editButton = new Button("Sửa");
        private final Button deleteButton = new Button("Xóa");

        ActionCell() {
            editButton.getStyleClass().addAll("action-button", "action-edit-button");
            deleteButton.getStyleClass().addAll("action-button", "action-lock-button");
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().addAll(editButton, deleteButton);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : box);
        }
    }

    public static class TransferRow {
        private final String code;
        private final String material;
        private final String unit;
        private final String quantity;
        private final String status;

        public TransferRow(String code, String material, String unit, String quantity, String status) {
            this.code = code;
            this.material = material;
            this.unit = unit;
            this.quantity = quantity;
            this.status = status;
        }

        public String getCode() {
            return code;
        }

        public String getMaterial() {
            return material;
        }

        public String getUnit() {
            return unit;
        }

        public String getQuantity() {
            return quantity;
        }

        public String getStatus() {
            return status;
        }
    }
}
