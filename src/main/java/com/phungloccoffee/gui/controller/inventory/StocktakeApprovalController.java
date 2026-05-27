package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.WarehouseWorkflowBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class StocktakeApprovalController {
    @FXML private TableView<Row> stocktakeTable;
    @FXML private TableColumn<Row, String> codeColumn;
    @FXML private TableColumn<Row, String> creatorColumn;
    @FXML private TableColumn<Row, String> dateColumn;
    @FXML private TableColumn<Row, String> noteColumn;
    @FXML private TableColumn<Row, Number> itemCountColumn;
    @FXML private TableColumn<Row, String> statusColumn;
    @FXML private TableColumn<Row, Void> actionColumn;

    private final WarehouseWorkflowBUS workflowBUS = new WarehouseWorkflowBUS();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private void initialize() {
        codeColumn.setCellValueFactory(data -> data.getValue().codeProperty());
        creatorColumn.setCellValueFactory(data -> data.getValue().creatorProperty());
        dateColumn.setCellValueFactory(data -> data.getValue().dateProperty());
        noteColumn.setCellValueFactory(data -> data.getValue().noteProperty());
        itemCountColumn.setCellValueFactory(data -> data.getValue().itemCountProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        statusColumn.setCellFactory(column -> new StatusCell());
        actionColumn.setCellFactory(column -> new ActionCell());
        stocktakeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        stocktakeTable.setItems(rows);
        loadRows();
    }

    private void loadRows() {
        try {
            List<WarehouseApprovalItem> items = workflowBUS.loadApprovalItems(WarehouseSlipType.STOCKTAKE, null);
            rows.setAll(items.stream().map(item -> new Row(
                    item.getSlipId(),
                    item.getCreatedBy(),
                    item.getCreatedAt().format(formatter),
                    item.getRelatedParty(),
                    item.getItemCount(),
                    WarehouseSlipStatus.toDisplay(item.getStatus())
            )).toList());
        } catch (Exception e) {
            rows.clear();
        }
    }

    private void approve(Row row) {
        try {
            workflowBUS.approve(WarehouseSlipType.STOCKTAKE, row.getCode());
            AlertUtils.showInfo("Đã duyệt phiếu kiểm kê và điều chỉnh tồn kho.");
            loadRows();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private void reject(Row row) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Từ chối phiếu kiểm kê");
        dialog.setHeaderText("Nhập lý do từ chối cho " + row.getCode());
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isBlank()) {
            AlertUtils.showWarning("Bat buoc nhap ly do tu choi.");
            return;
        }
        try {
            workflowBUS.reject(WarehouseSlipType.STOCKTAKE, row.getCode(), result.get());
            AlertUtils.showInfo("Đã từ chối phiếu kiểm kê.");
            loadRows();
        } catch (ValidationException | PermissionException | DatabaseException e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private class StatusCell extends TableCell<Row, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                return;
            }
            Label badge = new Label(status);
            String style = switch (status) {
                case "Đã duyệt" -> "badge-approved";
                case "Từ chối" -> "badge-rejected";
                default -> "badge-pending";
            };
            badge.getStyleClass().addAll("badge", style);
            setGraphic(badge);
            setAlignment(Pos.CENTER);
        }
    }

    private class ActionCell extends TableCell<Row, Void> {
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }
            Row row = getTableRow().getItem();
            if (!"Chờ duyệt".equals(row.getStatus())) {
                setGraphic(null);
                return;
            }
            Button approve = new Button("Duyệt");
            approve.getStyleClass().add("btn-approve");
            approve.setOnAction(event -> approve(row));
            Button reject = new Button("Từ chối");
            reject.getStyleClass().add("btn-reject");
            reject.setOnAction(event -> reject(row));
            HBox box = new HBox(6, approve, reject);
            box.setAlignment(Pos.CENTER);
            setGraphic(box);
        }
    }

    public static class Row {
        private final SimpleStringProperty code;
        private final SimpleStringProperty creator;
        private final SimpleStringProperty date;
        private final SimpleStringProperty note;
        private final SimpleIntegerProperty itemCount;
        private final SimpleStringProperty status;

        public Row(String code, String creator, String date, String note, int itemCount, String status) {
            this.code = new SimpleStringProperty(code);
            this.creator = new SimpleStringProperty(creator);
            this.date = new SimpleStringProperty(date);
            this.note = new SimpleStringProperty(note);
            this.itemCount = new SimpleIntegerProperty(itemCount);
            this.status = new SimpleStringProperty(status);
        }

        public SimpleStringProperty codeProperty() { return code; }
        public SimpleStringProperty creatorProperty() { return creator; }
        public SimpleStringProperty dateProperty() { return date; }
        public SimpleStringProperty noteProperty() { return note; }
        public SimpleIntegerProperty itemCountProperty() { return itemCount; }
        public SimpleStringProperty statusProperty() { return status; }
        public String getCode() { return code.get(); }
        public String getStatus() { return status.get(); }
    }
}
