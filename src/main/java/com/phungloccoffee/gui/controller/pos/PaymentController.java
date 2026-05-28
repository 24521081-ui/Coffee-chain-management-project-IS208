package com.phungloccoffee.gui.controller.pos;

import com.phungloccoffee.bus.PaymentBUS;
import com.phungloccoffee.model.Order;
import com.phungloccoffee.model.OrderDetail;
import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.CurrencyFormatter;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class PaymentController {
    private static final String PAID_STATUS = "DA_THANH_TOAN";

    @FXML private TextField orderIdField;
    @FXML private TextField amountField;
    @FXML private ComboBox<String> methodComboBox;
    @FXML private CheckBox registerCustomerCheckBox;
    @FXML private TextField customerNameField;
    @FXML private TextField customerPhoneField;
    @FXML private TextField customerEmailField;
    @FXML private Label statusLabel;
    @FXML private Label totalLabel;
    @FXML private Label itemsLabel;
    @FXML private Button confirmPaymentButton;

    private final PaymentBUS paymentBUS = new PaymentBUS();
    private Order currentOrder;

    @FXML
    private void initialize() {
        methodComboBox.getItems().setAll(
                "Ti\u1ec1n m\u1eb7t",
                "Chuy\u1ec3n kho\u1ea3n",
                "V\u00ed \u0111i\u1ec7n t\u1eed",
                "Th\u1ebb ng\u00e2n h\u00e0ng"
        );
        methodComboBox.getSelectionModel().selectFirst();
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
                        .map(this::formatDetailForPayment)
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
            String orderId = safe(orderIdField.getText());
            validatePaymentAmount();
            boolean shouldRegisterCustomer = shouldRegisterCustomer();
            PaymentBUS.PaymentResult result = paymentBUS.confirmPaymentWithOptionalCustomer(
                    orderId,
                    methodComboBox.getValue(),
                    shouldRegisterCustomer,
                    customerNameField.getText(),
                    customerPhoneField.getText(),
                    customerEmailField.getText()
            );
            currentOrder = result.order();
            loadOrderPaymentInfo(orderId);
            clearCustomerForm();

            if (result.customerCreated()) {
                AlertUtils.showInfo("Thanh toán thành công. Thêm khách hàng thành công.");
            } else if (orderId.contains("-")) {
                AlertUtils.showInfo("Thanh toán offline thành công, đơn sẽ được đồng bộ khi có mạng.");
            } else {
                AlertUtils.showInfo("Thanh toán thành công.");
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
        if (registerCustomerCheckBox != null) {
            registerCustomerCheckBox.setDisable(isPaid);
        }
        if (customerNameField != null) {
            customerNameField.setDisable(isPaid);
        }
        if (customerPhoneField != null) {
            customerPhoneField.setDisable(isPaid);
        }
        if (customerEmailField != null) {
            customerEmailField.setDisable(isPaid);
        }
    }

    private boolean shouldRegisterCustomer() {
        return registerCustomerCheckBox != null && registerCustomerCheckBox.isSelected()
                || !safe(customerNameField.getText()).isBlank()
                || !safe(customerPhoneField.getText()).isBlank()
                || !safe(customerEmailField.getText()).isBlank();
    }

    private void validatePaymentAmount() {
        BigDecimal paidAmount;
        try {
            paidAmount = new BigDecimal(safe(amountField.getText()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Số tiền thanh toán không hợp lệ.");
        }
        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0.");
        }
        if (currentOrder != null && currentOrder.getTongTien() != null
                && paidAmount.compareTo(currentOrder.getTongTien()) < 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn hoặc bằng tổng tiền.");
        }
    }

    private void clearCustomerForm() {
        if (registerCustomerCheckBox != null) {
            registerCustomerCheckBox.setSelected(false);
        }
        if (customerNameField != null) {
            customerNameField.clear();
        }
        if (customerPhoneField != null) {
            customerPhoneField.clear();
        }
        if (customerEmailField != null) {
            customerEmailField.clear();
        }
    }

    private String formatDetailForPayment(OrderDetail detail) {
        String quantity = detail.getSoLuong() == null ? "0" : detail.getSoLuong().stripTrailingZeros().toPlainString();
        String note = detail.getGhiChu() == null || detail.getGhiChu().isBlank() ? "" : " - " + detail.getGhiChu();
        return detail.getSanPhamId() + " x" + quantity + note;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
