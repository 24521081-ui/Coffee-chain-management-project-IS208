package com.phungloccoffee.gui.controller.pos;

import com.phungloccoffee.App;
import com.phungloccoffee.bus.PaymentHistoryBUS;
import com.phungloccoffee.model.PaymentHistory;
import com.phungloccoffee.util.AlertUtils;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PaymentHistoryController {
    @FXML private TableView<PaymentHistory> historyTable;
    @FXML private TableColumn<PaymentHistory, String> orderCodeColumn;
    @FXML private TableColumn<PaymentHistory, String> cashierColumn;
    @FXML private TableColumn<PaymentHistory, String> methodColumn;
    @FXML private TableColumn<PaymentHistory, BigDecimal> amountColumn;
    @FXML private TableColumn<PaymentHistory, LocalDateTime> paidAtColumn;
    @FXML private TableColumn<PaymentHistory, String> statusColumn;
    @FXML private TableColumn<PaymentHistory, PaymentHistory> actionColumn;

    private final PaymentHistoryBUS historyBUS = new PaymentHistoryBUS();

    @FXML
    private void initialize() {
        orderCodeColumn.setCellValueFactory(new PropertyValueFactory<>("orderCode"));
        cashierColumn.setCellValueFactory(new PropertyValueFactory<>("cashierName"));
        methodColumn.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        paidAtColumn.setCellValueFactory(new PropertyValueFactory<>("paidAt"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        if (actionColumn != null) {
            actionColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
            actionColumn.setCellFactory(column -> new ActionCell());
        }
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        loadHistory();
    }

    @FXML
    private void loadHistory() {
        try {
            List<PaymentHistory> history = historyBUS.loadHistory();
            historyTable.setItems(FXCollections.observableArrayList(history));
        } catch (Exception e) {
            AlertUtils.showError(e.getMessage());
            historyTable.setItems(FXCollections.observableArrayList());
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
            badge.getStyleClass().addAll("status-badge", styleFor(status));
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }

        private String styleFor(String status) {
            String value = status.toLowerCase();
            if ("chua_thanh_toan".equals(value)) {
                return "status-warning";
            }
            if ("da_thanh_toan".equals(value)) {
                return "status-success";
            }
            if (value.contains("lỗi") || value.contains("thất bại") || value.contains("hủy")) {
                return "status-danger";
            }
            if (value.contains("chờ")) {
                return "status-warning";
            }
            if (value.contains("đang")) {
                return "status-info";
            }
            return "status-success";
        }
    }

    private class ActionCell extends TableCell<PaymentHistory, PaymentHistory> {
        @Override
        protected void updateItem(PaymentHistory history, boolean empty) {
            super.updateItem(history, empty);
            if (empty || history == null || !"CHUA_THANH_TOAN".equals(history.getStatus())) {
                setGraphic(null);
                setText(null);
                return;
            }
            Button button = new Button("Thanh toán");
            button.getStyleClass().add("primary-button");
            button.setOnAction(event -> openPaymentScreen(history.getOrderCode()));
            setGraphic(button);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private void openPaymentScreen(String orderId) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("/com/phungloccoffee/gui/view/PaymentScreen.fxml"));
            Parent page = loader.load();
            PaymentController controller = loader.getController();
            controller.setOrderId(orderId);
            findContentArea().getChildren().setAll(page);
        } catch (Exception e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    private StackPane findContentArea() {
        Node node = historyTable;
        while (node != null) {
            if (node instanceof StackPane stackPane && stackPane.getStyleClass().contains("content-area")) {
                return stackPane;
            }
            node = node.getParent();
        }
        throw new IllegalStateException("Không tìm thấy vùng hiển thị nội dung để mở màn hình thanh toán.");
    }
}
