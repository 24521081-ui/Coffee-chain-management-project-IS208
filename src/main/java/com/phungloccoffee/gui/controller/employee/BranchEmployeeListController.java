package com.phungloccoffee.gui.controller.employee;

import com.phungloccoffee.dao.NhanVienDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.model.NhanVien;
import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BranchEmployeeListController {
    private static final String ALL_STATUS = "Tất cả trạng thái";
    private static final String ALL_POSITION = "Tất cả chức vụ";
    private static final String STATUS_ACTIVE = "Đang làm";
    private static final String STATUS_INACTIVE = "Đã nghỉ";

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> positionFilter;
    @FXML private TableView<EmployeeRow> employeeTable;
    @FXML private TableColumn<EmployeeRow, String> employeeIdColumn;
    @FXML private TableColumn<EmployeeRow, String> fullNameColumn;
    @FXML private TableColumn<EmployeeRow, String> positionColumn;
    @FXML private TableColumn<EmployeeRow, String> emailColumn;
    @FXML private TableColumn<EmployeeRow, String> phoneColumn;
    @FXML private TableColumn<EmployeeRow, String> statusColumn;
    @FXML private TableColumn<EmployeeRow, Void> actionColumn;

    private final NhanVienDAO nhanVienDAO = new NhanVienDAO();
    private final ObservableList<EmployeeRow> employees = FXCollections.observableArrayList();
    private FilteredList<EmployeeRow> filteredEmployees;

    @FXML
    private void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList(ALL_STATUS, STATUS_ACTIVE, STATUS_INACTIVE));
        statusFilter.getSelectionModel().selectFirst();

        employeeIdColumn.setCellValueFactory(data -> data.getValue().employeeIdProperty());
        fullNameColumn.setCellValueFactory(data -> data.getValue().fullNameProperty());
        positionColumn.setCellValueFactory(data -> data.getValue().positionProperty());
        emailColumn.setCellValueFactory(data -> data.getValue().emailProperty());
        phoneColumn.setCellValueFactory(data -> data.getValue().phoneProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        statusColumn.setCellFactory(column -> new StatusBadgeCell());
        actionColumn.setCellFactory(column -> new ActionCell());

        filteredEmployees = new FilteredList<>(employees, employee -> true);
        employeeTable.setItems(filteredEmployees);
        employeeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadEmployees();
    }

    private void loadEmployees() {
        try {
            String branchId = SessionManager.getCurrentBranchId();
            List<EmployeeRow> rows = nhanVienDAO.findAll().stream()
                    .filter(employee -> branchId != null && branchId.equals(employee.getChiNhanhId()))
                    .map(EmployeeRow::from)
                    .toList();
            employees.setAll(rows);
            populatePositionFilter();
            handleSearch();
        } catch (DatabaseException e) {
            AlertUtils.showError(e.getMessage());
            employees.clear();
        }
    }

    private void populatePositionFilter() {
        Set<String> positions = new LinkedHashSet<>();
        positions.add(ALL_POSITION);
        employees.stream().map(EmployeeRow::getPosition).forEach(positions::add);
        positionFilter.setItems(FXCollections.observableArrayList(positions));
        positionFilter.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleSearch() {
        String keyword = safe(searchField.getText()).toLowerCase();
        String status = statusFilter.getValue();
        String position = positionFilter.getValue();

        filteredEmployees.setPredicate(employee -> {
            boolean matchesKeyword = keyword.isBlank()
                    || employee.getFullName().toLowerCase().contains(keyword)
                    || employee.getEmail().toLowerCase().contains(keyword)
                    || employee.getPhone().toLowerCase().contains(keyword)
                    || employee.getEmployeeId().toLowerCase().contains(keyword);
            boolean matchesStatus = status == null || ALL_STATUS.equals(status) || employee.getStatus().equals(status);
            boolean matchesPosition = position == null || ALL_POSITION.equals(position) || employee.getPosition().equals(position);
            return matchesKeyword && matchesStatus && matchesPosition;
        });
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        positionFilter.getSelectionModel().selectFirst();
        filteredEmployees.setPredicate(employee -> true);
    }

    @FXML
    private void handleAddEmployee() {
        AlertUtils.showWarning("Màn này hiện đang dùng dữ liệu thật để tra cứu nhân viên chi nhánh. Thao tác thêm mới chưa được chuyển sang flow DB ở phạm vi này.");
    }

    private void showDetails(EmployeeRow employee) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi tiết nhân viên");
        alert.setHeaderText(employee.getEmployeeId() + " - " + employee.getFullName());
        alert.setContentText("Chức vụ: " + employee.getPosition()
                + "\nEmail: " + employee.getEmail()
                + "\nĐiện thoại: " + employee.getPhone()
                + "\nTrạng thái: " + employee.getStatus());
        alert.showAndWait();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String statusStyle(String status) {
        return STATUS_INACTIVE.equals(status) ? "status-danger" : "status-success";
    }

    private class StatusBadgeCell extends TableCell<EmployeeRow, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(status);
            label.getStyleClass().addAll("status-badge-cell", "status-badge", statusStyle(status));
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    private class ActionCell extends TableCell<EmployeeRow, Void> {
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            EmployeeRow employee = getTableRow().getItem();
            Button detail = new Button("Xem");
            detail.getStyleClass().addAll("action-button", "action-view-button");
            detail.setOnAction(event -> showDetails(employee));
            HBox actions = new HBox(6, detail);
            actions.setAlignment(Pos.CENTER_LEFT);
            setGraphic(actions);
            setText(null);
        }
    }

    public static class EmployeeRow {
        private final SimpleStringProperty employeeId;
        private final SimpleStringProperty fullName;
        private final SimpleStringProperty position;
        private final SimpleStringProperty email;
        private final SimpleStringProperty phone;
        private final SimpleStringProperty status;

        public EmployeeRow(String employeeId, String fullName, String position, String email, String phone, String status) {
            this.employeeId = new SimpleStringProperty(employeeId);
            this.fullName = new SimpleStringProperty(fullName);
            this.position = new SimpleStringProperty(position);
            this.email = new SimpleStringProperty(email);
            this.phone = new SimpleStringProperty(phone);
            this.status = new SimpleStringProperty(status);
        }

        public static EmployeeRow from(NhanVien employee) {
            String status = employee.getTrangThai() == 1 ? STATUS_ACTIVE : STATUS_INACTIVE;
            return new EmployeeRow(
                    employee.getNhanVienId(),
                    employee.getHoTen(),
                    employee.getChucVu(),
                    employee.getEmail() == null ? "" : employee.getEmail(),
                    employee.getPhone() == null ? "" : employee.getPhone(),
                    status
            );
        }

        public SimpleStringProperty employeeIdProperty() { return employeeId; }
        public SimpleStringProperty fullNameProperty() { return fullName; }
        public SimpleStringProperty positionProperty() { return position; }
        public SimpleStringProperty emailProperty() { return email; }
        public SimpleStringProperty phoneProperty() { return phone; }
        public SimpleStringProperty statusProperty() { return status; }
        public String getEmployeeId() { return employeeId.get(); }
        public String getFullName() { return fullName.get(); }
        public String getPosition() { return position.get(); }
        public String getEmail() { return email.get(); }
        public String getPhone() { return phone.get(); }
        public String getStatus() { return status.get(); }
    }
}
