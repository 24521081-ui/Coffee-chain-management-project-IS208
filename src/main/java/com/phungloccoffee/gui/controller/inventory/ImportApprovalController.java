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

public class ImportApprovalController {
    @FXML private TableView<Row> importTable;
    @FXML private TableColumn<Row, String> codeColumn;
    @FXML private TableColumn<Row, String> creatorColumn;
    @FXML private TableColumn<Row, String> dateColumn;
    @FXML private TableColumn<Row, String> supplierColumn;
    @FXML private TableColumn<Row, Number> itemCountColumn;
    @FXML private TableColumn<Row, String> totalColumn;
    @FXML private TableColumn<Row, String> statusColumn;
    @FXML private TableColumn<Row, Void> actionColumn;
    @FXML private StackPane importTabIconContainer;
    @FXML private StackPane detailTabIconContainer;

    private final WarehouseWorkflowBUS workflowBUS = new WarehouseWorkflowBUS();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        importTabIconContainer.getChildren().setAll(IconFactory.createPageTabIcon("arrow-down"));
        detailTabIconContainer.getChildren().setAll(IconFactory.createPageTabIcon("clipboard"));

        codeColumn.setCellValueFactory(data -> data.getValue().codeProperty());
        creatorColumn.setCellValueFactory(data -> data.getValue().creatorProperty());
        dateColumn.setCellValueFactory(data -> data.getValue().dateProperty());
        supplierColumn.setCellValueFactory(data -> data.getValue().supplierProperty());
        itemCountColumn.setCellValueFactory(data -> data.getValue().itemCountProperty());
        totalColumn.setCellValueFactory(data -> data.getValue().totalProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());

        codeColumn.setCellFactory(column -> new LabelCell<>("cell-code"));
        creatorColumn.setCellFactory(column -> new LabelCell<>("cell-nv"));
        supplierColumn.setCellFactory(column -> new WrapTextCell());
        itemCountColumn.setCellFactory(column -> new QuantityCell());
        totalColumn.setCellFactory(column -> new LabelCell<>("cell-price"));
        statusColumn.setCellFactory(column -> new StatusCell());
        actionColumn.setCellFactory(column -> new ActionCell());

        importTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        importTable.setItems(rows);
        loadRows();
    }

    private void loadRows() {
        try {
            List<WarehouseApprovalItem> items = workflowBUS.loadApprovalItems(WarehouseSlipType.IMPORT, null);
            rows.setAll(items.stream().map(item -> new Row(
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

    private void approve(Row row) {
        try {
            workflowBUS.approve(WarehouseSlipType.IMPORT, row.getCode());
            AlertUtils.showInfo("Đã duyệt phiếu nhập kho và cập nhật tồn kho.");
            loadRows();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private void reject(Row row) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Từ chối phiếu");
        dialog.setHeaderText("Nhập lý do từ chối cho " + row.getCode());
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            AlertUtils.showWarning("Bat buoc nhap ly do tu choi.");
            return;
        }
        try {
            workflowBUS.reject(WarehouseSlipType.IMPORT, row.getCode(), result.get());
            AlertUtils.showInfo("Đã từ chối phiếu nhập kho.");
            loadRows();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private void showDetails(Row row) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết phiếu nhập");
        alert.setHeaderText(row.getCode() + " - " + row.getStatus());
        alert.setContentText("Người lập: " + row.getCreator()
                + "\nNgày lập: " + row.getDate()
                + "\nNhà cung cấp: " + row.getSupplier()
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

    private class LabelCell<T> extends TableCell<Row, T> {
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

    private class WrapTextCell extends TableCell<Row, String> {
        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(value);
            label.setWrapText(true);
            label.setMaxWidth(240);
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class QuantityCell extends TableCell<Row, Number> {
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

    private class StatusCell extends TableCell<Row, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(status);
            badge.getStyleClass().addAll("badge", statusStyle(status));
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private class ActionCell extends TableCell<Row, Void> {
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Row row = getTableRow().getItem();
            VBox actionStack = new VBox(6);
            actionStack.setAlignment(Pos.CENTER);
            Button detail = new Button("Xem");
            detail.setOnAction(event -> showDetails(row));

            if ("Chờ duyệt".equals(row.getStatus())) {
                detail.getStyleClass().add("btn-text-blue");
                Button approve = new Button("Duyệt");
                approve.getStyleClass().add("btn-approve");
                approve.setOnAction(event -> approve(row));
                Button reject = new Button("Từ chối");
                reject.getStyleClass().add("btn-reject");
                reject.setOnAction(event -> reject(row));
                HBox actionRow = new HBox(6, approve, reject);
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

    public static class Row {
        private final SimpleStringProperty code;
        private final SimpleStringProperty creator;
        private final SimpleStringProperty date;
        private final SimpleStringProperty supplier;
        private final SimpleIntegerProperty itemCount;
        private final SimpleStringProperty total;
        private final SimpleStringProperty status;

        public Row(String code, String creator, String date, String supplier, int itemCount, String total, String status) {
            this.code = new SimpleStringProperty(code);
            this.creator = new SimpleStringProperty(creator);
            this.date = new SimpleStringProperty(date);
            this.supplier = new SimpleStringProperty(supplier);
            this.itemCount = new SimpleIntegerProperty(itemCount);
            this.total = new SimpleStringProperty(total);
            this.status = new SimpleStringProperty(status);
        }

        public SimpleStringProperty codeProperty() { return code; }
        public SimpleStringProperty creatorProperty() { return creator; }
        public SimpleStringProperty dateProperty() { return date; }
        public SimpleStringProperty supplierProperty() { return supplier; }
        public SimpleIntegerProperty itemCountProperty() { return itemCount; }
        public SimpleStringProperty totalProperty() { return total; }
        public SimpleStringProperty statusProperty() { return status; }
        public String getCode() { return code.get(); }
        public String getCreator() { return creator.get(); }
        public String getDate() { return date.get(); }
        public String getSupplier() { return supplier.get(); }
        public int getItemCount() { return itemCount.get(); }
        public String getStatus() { return status.get(); }
    }
}
