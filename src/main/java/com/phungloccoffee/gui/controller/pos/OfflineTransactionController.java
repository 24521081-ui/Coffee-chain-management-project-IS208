package com.phungloccoffee.gui.controller.pos;

import com.phungloccoffee.model.offline.OfflineOrder;
import com.phungloccoffee.offline.OfflineStorage;
import com.phungloccoffee.offline.SyncService;
import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.CurrencyFormatter;
import com.phungloccoffee.util.SessionManager;

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

import java.time.format.DateTimeFormatter;
import java.util.List;

public class OfflineTransactionController {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private ComboBox<String> syncStatusComboBox;
    @FXML private ComboBox<String> deviceComboBox;
    @FXML private Label pendingCountLabel;
    @FXML private Label syncedCountLabel;
    @FXML private Label failedCountLabel;
    @FXML private TableView<OfflineRow> offlineTable;
    @FXML private TableColumn<OfflineRow, String> transactionColumn;
    @FXML private TableColumn<OfflineRow, String> deviceColumn;
    @FXML private TableColumn<OfflineRow, String> cashierColumn;
    @FXML private TableColumn<OfflineRow, String> amountColumn;
    @FXML private TableColumn<OfflineRow, String> timeColumn;
    @FXML private TableColumn<OfflineRow, String> statusColumn;
    @FXML private TableColumn<OfflineRow, String> errorColumn;
    @FXML private TableColumn<OfflineRow, Void> actionColumn;

    private final OfflineStorage offlineStorage = OfflineStorage.getInstance();
    private final SyncService syncService = new SyncService();

    @FXML
    private void initialize() {
        syncStatusComboBox.getItems().setAll("Tất cả trạng thái", OfflineOrder.SYNC_PENDING, OfflineOrder.SYNC_SYNCED, OfflineOrder.SYNC_FAILED);
        deviceComboBox.getItems().setAll("Tất cả chi nhánh");
        syncStatusComboBox.getSelectionModel().selectFirst();
        deviceComboBox.getSelectionModel().selectFirst();

        transactionColumn.setCellValueFactory(new PropertyValueFactory<>("transactionCode"));
        deviceColumn.setCellValueFactory(new PropertyValueFactory<>("device"));
        cashierColumn.setCellValueFactory(new PropertyValueFactory<>("cashier"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        errorColumn.setCellValueFactory(new PropertyValueFactory<>("lastError"));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        actionColumn.setCellFactory(column -> new ActionCell());
        offlineTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        syncStatusComboBox.setOnAction(event -> loadOfflineOrders());
        loadOfflineOrders();
    }

    @FXML
    private void syncSelected() {
        try {
            int synced = syncService.syncPending(SessionManager.getCurrentBranchId());
            AlertUtils.showInfo("Đã đồng bộ " + synced + " giao dịch offline.");
            loadOfflineOrders();
        } catch (Exception e) {
            AlertUtils.showError(e.getMessage());
            loadOfflineOrders();
        }
    }

    private void loadOfflineOrders() {
        try {
            List<OfflineOrder> orders = offlineStorage.loadAll();
            updateStats(orders);
            String selectedStatus = syncStatusComboBox.getValue();
            List<OfflineRow> rows = orders.stream()
                    .filter(order -> selectedStatus == null
                            || selectedStatus.startsWith("Tất cả")
                            || selectedStatus.equals(order.getSyncStatus()))
                    .map(this::toRow)
                    .toList();
            offlineTable.setItems(FXCollections.observableArrayList(rows));
        } catch (Exception e) {
            offlineTable.setItems(FXCollections.observableArrayList());
            AlertUtils.showError(e.getMessage());
        }
    }

    private void updateStats(List<OfflineOrder> orders) {
        long pending = orders.stream().filter(order -> OfflineOrder.SYNC_PENDING.equals(order.getSyncStatus())).count();
        long synced = orders.stream().filter(order -> OfflineOrder.SYNC_SYNCED.equals(order.getSyncStatus())).count();
        long failed = orders.stream().filter(order -> OfflineOrder.SYNC_FAILED.equals(order.getSyncStatus())).count();
        pendingCountLabel.setText(String.valueOf(pending));
        syncedCountLabel.setText(String.valueOf(synced));
        failedCountLabel.setText(String.valueOf(failed));
    }

    private OfflineRow toRow(OfflineOrder order) {
        return new OfflineRow(
                order.getLocalOrderId(),
                order.getBranchId(),
                order.getCashierName(),
                CurrencyFormatter.format(order.getTotalAmount()),
                order.getCreatedAt() == null ? "" : order.getCreatedAt().format(DATE_TIME_FORMAT),
                order.getSyncStatus(),
                order.getLastError() == null ? "" : order.getLastError()
        );
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
            badge.getStyleClass().addAll("status-badge", switch (status) {
                case OfflineOrder.SYNC_SYNCED -> "status-success";
                case OfflineOrder.SYNC_FAILED -> "status-danger";
                default -> "status-warning";
            });
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    private class ActionCell extends TableCell<OfflineRow, Void> {
        private final HBox box = new HBox(8);
        private final Button syncButton = new Button("Đồng bộ");

        ActionCell() {
            syncButton.getStyleClass().addAll("action-button", "action-approve-button");
            syncButton.setOnAction(event -> syncSelected());
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(syncButton);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : box);
        }
    }

    public static class OfflineRow {
        private final String transactionCode;
        private final String device;
        private final String cashier;
        private final String amount;
        private final String time;
        private final String status;
        private final String lastError;

        public OfflineRow(String transactionCode, String device, String cashier, String amount,
                          String time, String status, String lastError) {
            this.transactionCode = transactionCode;
            this.device = device;
            this.cashier = cashier;
            this.amount = amount;
            this.time = time;
            this.status = status;
            this.lastError = lastError;
        }

        public String getTransactionCode() { return transactionCode; }
        public String getDevice() { return device; }
        public String getCashier() { return cashier; }
        public String getAmount() { return amount; }
        public String getTime() { return time; }
        public String getStatus() { return status; }
        public String getLastError() { return lastError; }
    }
}
