package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.WarehouseWorkflowBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.gui.util.IconFactory;
import com.phungloccoffee.model.WarehouseApprovalItem;
import com.phungloccoffee.model.WarehouseSlipStatus;
import com.phungloccoffee.model.WarehouseSlipType;
import com.phungloccoffee.util.AlertUtils;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ExportApprovalController {
    @FXML private TableView<ExportRequestRow> exportTable;
    @FXML private TableColumn<ExportRequestRow, String> codeColumn;
    @FXML private TableColumn<ExportRequestRow, String> employeeColumn;
    @FXML private TableColumn<ExportRequestRow, String> timeColumn;
    @FXML private TableColumn<ExportRequestRow, String> reasonColumn;
    @FXML private TableColumn<ExportRequestRow, Number> itemCountColumn;
    @FXML private TableColumn<ExportRequestRow, String> totalColumn;
    @FXML private TableColumn<ExportRequestRow, String> statusColumn;
    @FXML private TableColumn<ExportRequestRow, Void> actionColumn;
    @FXML private StackPane approvalTabIconContainer;
    @FXML private StackPane detailTabIconContainer;

    private final WarehouseWorkflowBUS workflowBUS = new WarehouseWorkflowBUS();
    private final ObservableList<ExportRequestRow> rows = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        approvalTabIconContainer.getChildren().setAll(IconFactory.createPageTabIcon("back"));
        detailTabIconContainer.getChildren().setAll(IconFactory.createPageTabIcon("clipboard"));

        codeColumn.setCellValueFactory(data -> data.getValue().codeProperty());
        employeeColumn.setCellValueFactory(data -> data.getValue().employeeIdProperty());
        timeColumn.setCellValueFactory(data -> data.getValue().timeProperty());
        reasonColumn.setCellValueFactory(data -> data.getValue().reasonProperty());
        itemCountColumn.setCellValueFactory(data -> data.getValue().itemCountProperty());
        totalColumn.setCellValueFactory(data -> data.getValue().totalProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());

        codeColumn.setCellFactory(column -> new LabelCell<>("cell-code"));
        employeeColumn.setCellFactory(column -> new LabelCell<>("cell-nv"));
        reasonColumn.setCellFactory(column -> new WrapTextCell());
        itemCountColumn.setCellFactory(column -> new QuantityCell());
        totalColumn.setCellFactory(column -> new LabelCell<>("cell-price"));
        statusColumn.setCellFactory(column -> new StatusBadgeCell());
        actionColumn.setCellFactory(column -> new ActionCell());

        exportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        exportTable.setItems(rows);
        loadRows();
    }

    private void loadRows() {
        try {
            List<WarehouseApprovalItem> items = workflowBUS.loadApprovalItems(WarehouseSlipType.EXPORT, null);
            rows.setAll(items.stream().map(item -> new ExportRequestRow(
                    item.getSlipId(),
                    item.getCreatedBy(),
                    item.getCreatedAt().format(formatter),
                    item.getRelatedParty(),
                    item.getItemCount(),
                    "-",
                    WarehouseSlipStatus.toDisplay(item.getStatus())
            )).toList());
        } catch (Exception e) {
            rows.clear();
        }
    }

    private void approve(ExportRequestRow row) {
        try {
            workflowBUS.approve(WarehouseSlipType.EXPORT, row.getCode());
            AlertUtils.showInfo("Đã duyệt phiếu xuất kho và cập nhật tồn kho.");
            loadRows();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private void reject(ExportRequestRow row) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Từ chối phiếu");
        dialog.setHeaderText("Nhập lý do từ chối cho " + row.getCode());
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            AlertUtils.showWarning("Bat buoc nhap ly do tu choi.");
            return;
        }
        try {
            workflowBUS.reject(WarehouseSlipType.EXPORT, row.getCode(), result.get());
            AlertUtils.showInfo("Đã từ chối phiếu xuất kho.");
            loadRows();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private void showDetails(ExportRequestRow row) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết phiếu xuất");
        alert.setHeaderText(row.getCode() + " - " + row.getStatus());
        alert.setContentText("Người lập: " + row.getEmployeeId()
                + "\nThời gian: " + row.getTime()
                + "\nLý do: " + row.getReason()
                + "\nSố lượng mặt hàng: " + row.getItemCount());
        alert.showAndWait();
    }

    private static String statusStyle(String status) {
        return switch (status) {
            case "Đã duyệt" -> "badge-approved";
            case "Từ chối" -> "badge-rejected";
            default -> "badge-pending";
        };
    }

    private class LabelCell<T> extends TableCell<ExportRequestRow, T> {
        private final String styleClass;

        private LabelCell(String styleClass) {
            this.styleClass = styleClass;
        }

        @Override
        protected void updateItem(T value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(String.valueOf(value));
            label.getStyleClass().add(styleClass);
            label.setWrapText(true);
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class WrapTextCell extends TableCell<ExportRequestRow, String> {
        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(String.valueOf(value));
            label.setWrapText(true);
            label.setMaxWidth(210);
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class QuantityCell extends TableCell<ExportRequestRow, Number> {
        @Override
        protected void updateItem(Number count, boolean empty) {
            super.updateItem(count, empty);
            if (empty || count == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(String.valueOf(count.intValue()));
            label.getStyleClass().add("cell-qty");
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class StatusBadgeCell extends TableCell<ExportRequestRow, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(status);
            label.getStyleClass().addAll("badge", statusStyle(status));
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class ActionCell extends TableCell<ExportRequestRow, Void> {
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            ExportRequestRow row = getTableRow().getItem();
            VBox actionStack = new VBox(6);
            actionStack.setAlignment(Pos.CENTER);
            Button detail = new Button("Xem");
            detail.setOnAction(event -> showDetails(row));

            if ("Chờ duyệt".equals(row.getStatus())) {
                detail.getStyleClass().add("btn-text-blue");
                Button reject = new Button("Từ chối");
                reject.getStyleClass().add("btn-reject");
                reject.setOnAction(event -> reject(row));
                Button approve = new Button("Duyệt");
                approve.getStyleClass().add("btn-approve");
                approve.setOnAction(event -> approve(row));
                HBox actionRow = new HBox(6, reject, approve);
                actionRow.setAlignment(Pos.CENTER);
                actionStack.getChildren().addAll(detail, actionRow);
            } else {
                detail.getStyleClass().add("btn-view-bg");
                actionStack.getChildren().add(detail);
            }

            setGraphic(actionStack);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    public static class ExportRequestRow {
        private final SimpleStringProperty code;
        private final SimpleStringProperty employeeId;
        private final SimpleStringProperty time;
        private final SimpleStringProperty reason;
        private final SimpleIntegerProperty itemCount;
        private final SimpleStringProperty total;
        private final SimpleStringProperty status;

        public ExportRequestRow(String code, String employeeId, String time, String reason,
                                int itemCount, String total, String status) {
            this.code = new SimpleStringProperty(code);
            this.employeeId = new SimpleStringProperty(employeeId);
            this.time = new SimpleStringProperty(time);
            this.reason = new SimpleStringProperty(reason);
            this.itemCount = new SimpleIntegerProperty(itemCount);
            this.total = new SimpleStringProperty(total);
            this.status = new SimpleStringProperty(status);
        }

        public SimpleStringProperty codeProperty() { return code; }
        public SimpleStringProperty employeeIdProperty() { return employeeId; }
        public SimpleStringProperty timeProperty() { return time; }
        public SimpleStringProperty reasonProperty() { return reason; }
        public SimpleIntegerProperty itemCountProperty() { return itemCount; }
        public SimpleStringProperty totalProperty() { return total; }
        public SimpleStringProperty statusProperty() { return status; }
        public String getCode() { return code.get(); }
        public String getEmployeeId() { return employeeId.get(); }
        public String getTime() { return time.get(); }
        public String getReason() { return reason.get(); }
        public int getItemCount() { return itemCount.get(); }
        public String getStatus() { return status.get(); }
    }
}
