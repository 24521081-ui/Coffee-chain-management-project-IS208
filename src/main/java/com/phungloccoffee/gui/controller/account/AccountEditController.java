package com.phungloccoffee.gui.controller.account;

import com.phungloccoffee.util.AlertUtils;
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

public class AccountEditController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterRoleComboBox;
    @FXML private ComboBox<String> filterStatusComboBox;
    @FXML private TableView<AccountRow> accountTable;
    @FXML private TableColumn<AccountRow, String> userIdColumn;
    @FXML private TableColumn<AccountRow, String> usernameColumn;
    @FXML private TableColumn<AccountRow, String> employeeCodeColumn;
    @FXML private TableColumn<AccountRow, String> fullNameColumn;
    @FXML private TableColumn<AccountRow, String> roleColumn;
    @FXML private TableColumn<AccountRow, String> statusColumn;
    @FXML private TextField userIdField;
    @FXML private TextField usernameField;
    @FXML private TextField employeeCodeField;
    @FXML private TextField fullNameField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TextArea noteArea;

    private final ObservableList<AccountRow> accounts = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        filterRoleComboBox.getItems().setAll("Tất cả vai trò", "THU_NGAN", "NHAN_VIEN_KHO",
                "QUAN_LY_CHI_NHANH", "IT_ADMIN", "BAN_GIAM_DOC");
        filterStatusComboBox.getItems().setAll("Tất cả trạng thái", "Hoạt động", "Tạm khóa", "Ngừng hoạt động");
        filterRoleComboBox.getSelectionModel().selectFirst();
        filterStatusComboBox.getSelectionModel().selectFirst();

        roleComboBox.getItems().setAll("THU_NGAN", "NHAN_VIEN_KHO", "QUAN_LY_CHI_NHANH", "IT_ADMIN", "BAN_GIAM_DOC");
        statusComboBox.getItems().setAll("Hoạt động", "Tạm khóa", "Ngừng hoạt động");

        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        employeeCodeColumn.setCellValueFactory(new PropertyValueFactory<>("employeeCode"));
        fullNameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        accountTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        accountTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, account) -> fillForm(account));
        userIdField.getStyleClass().add("readonly-code-field");
        employeeCodeField.setEditable(false);
        employeeCodeField.getStyleClass().add("readonly-code-field");

        loadAccounts();
    }

    @FXML
    private void loadAccounts() {
        accounts.setAll(
                new AccountRow("TK001", "admin", "NV001", "Quản trị viên", "IT_ADMIN", "Hoạt động", ""),
                new AccountRow("TK002", "thungan", "NV002", "Nhân viên thu ngân", "THU_NGAN", "Hoạt động", ""),
                new AccountRow("TK003", "quanly", "NV003", "Quản lý chi nhánh", "QUAN_LY_CHI_NHANH", "Hoạt động", ""),
                new AccountRow("TK004", "kho", "NV004", "Nhân viên kho", "NHAN_VIEN_KHO", "Hoạt động", ""),
                new AccountRow("TK005", "giamdoc", "NV005", "Ban giám đốc", "BAN_GIAM_DOC", "Hoạt động", "")
        );
        accountTable.setItems(accounts);
        accountTable.getSelectionModel().selectFirst();
    }

    @FXML
    private void saveAccount() {
        AccountRow selected = accountTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarning("Vui lòng chọn tài khoản cần cập nhật.");
            return;
        }
        selected.setFullName(fullNameField.getText());
        selected.setRole(roleComboBox.getValue());
        selected.setStatus(statusComboBox.getValue());
        selected.setNote(noteArea.getText());
        accountTable.refresh();
        AlertUtils.showInfo("Đã cập nhật thông tin tài khoản trên giao diện.");
    }

    private void fillForm(AccountRow account) {
        if (account == null) {
            return;
        }
        userIdField.setText(account.getUserId());
        usernameField.setText(account.getUsername());
        employeeCodeField.setText(account.getEmployeeCode());
        fullNameField.setText(account.getFullName());
        roleComboBox.setValue(account.getRole());
        statusComboBox.setValue(account.getStatus());
        noteArea.setText(account.getNote());
    }

    private static class StatusCell<T> extends TableCell<T, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null || status.isBlank()) {
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
            if ("Tạm khóa".equals(status)) {
                return "status-warning";
            }
            if ("Ngừng hoạt động".equals(status)) {
                return "status-danger";
            }
            return "status-success";
        }
    }

    public static class AccountRow {
        private String userId;
        private String username;
        private String employeeCode;
        private String fullName;
        private String role;
        private String status;
        private String note;

        public AccountRow(String userId, String username, String employeeCode, String fullName,
                          String role, String status, String note) {
            this.userId = userId;
            this.username = username;
            this.employeeCode = employeeCode;
            this.fullName = fullName;
            this.role = role;
            this.status = status;
            this.note = note;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmployeeCode() {
            return employeeCode;
        }

        public void setEmployeeCode(String employeeCode) {
            this.employeeCode = employeeCode;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }
}
