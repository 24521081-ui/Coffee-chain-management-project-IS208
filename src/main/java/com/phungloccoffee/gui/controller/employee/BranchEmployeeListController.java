package com.phungloccoffee.gui.controller.employee;

import com.phungloccoffee.util.AutoCodeGenerator;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class BranchEmployeeListController {
    private static final String ALL_STATUS = "T\u1ea5t c\u1ea3 tr\u1ea1ng th\u00e1i";
    private static final String ALL_POSITION = "T\u1ea5t c\u1ea3 ch\u1ee9c v\u1ee5";
    private static final String STATUS_ACTIVE = "\u0110ang l\u00e0m";
    private static final String STATUS_PAUSED = "T\u1ea1m ngh\u1ec9";
    private static final String STATUS_INACTIVE = "\u0110\u00e3 ngh\u1ec9";

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

    private final ObservableList<EmployeeRow> employees = FXCollections.observableArrayList(
            new EmployeeRow("NV001", "Nh\u00e2n vi\u00ean thu ng\u00e2n", "Thu ng\u00e2n", "cashier@phungloc.vn", "0866102769", STATUS_ACTIVE),
            new EmployeeRow("NV002", "Nh\u00e2n vi\u00ean kho", "Nh\u00e2n vi\u00ean kho", "kho@phungloc.vn", "0900000001", STATUS_ACTIVE),
            new EmployeeRow("NV003", "Pha ch\u1ebf ca s\u00e1ng", "Pha ch\u1ebf", "barista@phungloc.vn", "0900000002", STATUS_PAUSED),
            new EmployeeRow("NV004", "Nh\u00e2n vi\u00ean ph\u1ee5c v\u1ee5", "Ph\u1ee5c v\u1ee5", "staff@phungloc.vn", "0900000003", STATUS_ACTIVE),
            new EmployeeRow("NV005", "Qu\u1ea3n l\u00fd ca t\u1ed1i", "Qu\u1ea3n l\u00fd ca", "shiftlead@phungloc.vn", "0900000004", STATUS_INACTIVE)
    );
    private FilteredList<EmployeeRow> filteredEmployees;

    @FXML
    private void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList(ALL_STATUS, STATUS_ACTIVE, STATUS_PAUSED, STATUS_INACTIVE));
        positionFilter.setItems(FXCollections.observableArrayList(ALL_POSITION, "Thu ng\u00e2n", "Nh\u00e2n vi\u00ean kho", "Pha ch\u1ebf", "Ph\u1ee5c v\u1ee5", "Qu\u1ea3n l\u00fd ca"));
        statusFilter.getSelectionModel().selectFirst();
        positionFilter.getSelectionModel().selectFirst();

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
                    || employee.getPhone().toLowerCase().contains(keyword);
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
        showEmployeeDialog(null);
    }

    private void editEmployee(EmployeeRow employee) {
        showEmployeeDialog(employee);
    }

    private void toggleStatus(EmployeeRow employee) {
        employee.setStatus(STATUS_ACTIVE.equals(employee.getStatus()) ? STATUS_PAUSED : STATUS_ACTIVE);
        employeeTable.refresh();
        handleSearch();
    }

    private void showDetails(EmployeeRow employee) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chi ti\u1ebft nh\u00e2n vi\u00ean");
        alert.setHeaderText(employee.getEmployeeId() + " - " + employee.getFullName());
        alert.setContentText("Ch\u1ee9c v\u1ee5: " + employee.getPosition()
                + "\nEmail: " + employee.getEmail()
                + "\n\u0110i\u1ec7n tho\u1ea1i: " + employee.getPhone()
                + "\nTr\u1ea1ng th\u00e1i: " + employee.getStatus());
        alert.showAndWait();
    }

    private void showEmployeeDialog(EmployeeRow editing) {
        Dialog<EmployeeRow> dialog = new Dialog<>();
        dialog.setTitle(editing == null ? "Th\u00eam nh\u00e2n vi\u00ean" : "S\u1eeda nh\u00e2n vi\u00ean");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField(editing == null ? "" : editing.getFullName());
        TextField positionField = new TextField(editing == null ? "" : editing.getPosition());
        TextField emailField = new TextField(editing == null ? "" : editing.getEmail());
        TextField phoneField = new TextField(editing == null ? "" : editing.getPhone());
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList(STATUS_ACTIVE, STATUS_PAUSED, STATUS_INACTIVE));
        statusBox.getSelectionModel().select(editing == null ? STATUS_ACTIVE : editing.getStatus());

        nameField.setPromptText("H\u1ecd t\u00ean");
        positionField.setPromptText("Ch\u1ee9c v\u1ee5");
        emailField.setPromptText("Email");
        phoneField.setPromptText("\u0110i\u1ec7n tho\u1ea1i");

        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(12);
        form.setPadding(new Insets(18));
        form.addRow(0, new Label("H\u1ecd t\u00ean"), nameField);
        form.addRow(1, new Label("Ch\u1ee9c v\u1ee5"), positionField);
        form.addRow(2, new Label("Email"), emailField);
        form.addRow(3, new Label("\u0110i\u1ec7n tho\u1ea1i"), phoneField);
        form.addRow(4, new Label("Tr\u1ea1ng th\u00e1i"), statusBox);
        dialog.getDialogPane().setContent(form);

        dialog.setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            if (editing == null) {
                return new EmployeeRow(nextEmployeeId(), safe(nameField.getText()), safe(positionField.getText()),
                        safe(emailField.getText()), safe(phoneField.getText()), statusBox.getValue());
            }
            editing.setFullName(safe(nameField.getText()));
            editing.setPosition(safe(positionField.getText()));
            editing.setEmail(safe(emailField.getText()));
            editing.setPhone(safe(phoneField.getText()));
            editing.setStatus(statusBox.getValue());
            return editing;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (editing == null) {
                employees.add(result);
            }
            employeeTable.refresh();
            handleSearch();
        });
    }

    private String nextEmployeeId() {
        return AutoCodeGenerator.generateNextCode("NV", employees.stream()
                .map(EmployeeRow::getEmployeeId)
                .toList());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String statusStyle(String status) {
        return switch (status) {
            case STATUS_PAUSED -> "status-warning";
            case STATUS_INACTIVE -> "status-danger";
            default -> "status-success";
        };
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

            Button edit = new Button("S\u1eeda");
            edit.getStyleClass().addAll("action-button", "action-edit-button");
            edit.setOnAction(event -> editEmployee(employee));

            Button toggle = new Button(STATUS_ACTIVE.equals(employee.getStatus()) ? "Kh\u00f3a" : "M\u1edf");
            toggle.getStyleClass().addAll("action-button", "action-lock-button");
            toggle.setOnAction(event -> toggleStatus(employee));

            HBox actions = new HBox(6, detail, edit, toggle);
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
        public void setFullName(String value) { fullName.set(value); }
        public void setPosition(String value) { position.set(value); }
        public void setEmail(String value) { email.set(value); }
        public void setPhone(String value) { phone.set(value); }
        public void setStatus(String value) { status.set(value); }
    }
}
