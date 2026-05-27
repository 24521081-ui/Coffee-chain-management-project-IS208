package com.phungloccoffee.gui.controller.pos;

import com.phungloccoffee.bus.PaymentBUS;
import com.phungloccoffee.model.Order;
import com.phungloccoffee.model.OrderDetail;
import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.CurrencyFormatter;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.stream.Collectors;

public class PaymentController {
    private static final String PAID_STATUS = "DA_THANH_TOAN";

    @FXML private TextField orderIdField;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> methodComboBox;
    @FXML private Label statusLabel;
    @FXML private Label totalLabel;
    @FXML private Label itemsLabel;
    @FXML private Button confirmPaymentButton;

    private final PaymentBUS paymentBUS = new PaymentBUS();
    private Order currentOrder;

    @FXML
    private void initialize() {
        methodComboBox.getItems().setAll("Tien mat", "Chuyen khoan", "Vi dien tu", "The ngan hang");
    }

    public void setOrderId(String orderId) {
        orderIdField.setText(orderId);
        orderIdField.setEditable(false);
        loadOrderPaymentInfo(orderId);
    }

    public void loadOrderPaymentInfo(String orderId) {
        try {
            currentOrder = paymentBUS.getOrderForPayment(orderId);
            List<OrderDetail> details = paymentBUS.getOrderDetailsForPayment(orderId);
            amountField.setText(currentOrder.getTongTien() == null ? "0" : currentOrder.getTongTien().toPlainString());
            if (statusLabel != null) {
                statusLabel.setText(currentOrder.getTrangThaiThanhToan());
            }
            if (totalLabel != null) {
                totalLabel.setText(CurrencyFormatter.format(currentOrder.getTongTien()));
            }
            if (itemsLabel != null) {
                itemsLabel.setText(details.stream()
                        .map(detail -> detail.getSanPhamId() + " x" + detail.getSoLuong().stripTrailingZeros().toPlainString())
                        .collect(Collectors.joining(", ")));
            }
            updatePaymentControls();
        } catch (Exception e) {
            currentOrder = null;
            updatePaymentControls();
            AlertUtils.showError(e.getMessage());
        }
    }

    @FXML
    private void handleConfirmPayment() {
        try {
            String orderId = orderIdField.getText() == null ? "" : orderIdField.getText().trim();
            currentOrder = paymentBUS.confirmPayment(orderId, methodComboBox.getValue());
            loadOrderPaymentInfo(orderId);
            if (orderId.contains("-")) {
                AlertUtils.showInfo("Thanh toan offline thanh cong, don se duoc dong bo khi co mang.");
            } else {
                AlertUtils.showInfo("Thanh toan thanh cong.");
            }
        } catch (Exception e) {
            AlertUtils.showError(e.getMessage());
        }
    }

    @FXML
    private void handlePayment() {
        handleConfirmPayment();
    }

    private void updatePaymentControls() {
        boolean isPaid = currentOrder != null && PAID_STATUS.equals(currentOrder.getTrangThaiThanhToan());
        if (confirmPaymentButton != null) {
            confirmPaymentButton.setDisable(isPaid);
        }
        if (methodComboBox != null) {
            methodComboBox.setDisable(isPaid);
        }
        if (amountField != null) {
            amountField.setDisable(isPaid);
        }
    }
}
