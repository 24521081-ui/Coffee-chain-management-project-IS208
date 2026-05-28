package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.SupplierBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.model.SupplierDirectoryItem;
import com.phungloccoffee.util.AlertUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SupplierListController {
    private static final String ALL_CATEGORY = "Tất cả nhóm";
    private static final String ALL_STATUS = "Tất cả trạng thái";
    private static final String STATUS_ACTIVE = "Đang hợp tác";
    private static final String STATUS_INACTIVE = "Tạm dừng";

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TableView<SupplierRow> supplierTable;
    @FXML private TableColumn<SupplierRow, String> codeColumn;
    @FXML private TableColumn<SupplierRow, String> nameColumn;
    @FXML private TableColumn<SupplierRow, String> categoryColumn;
    @FXML private TableColumn<SupplierRow, String> contactColumn;
    @FXML private TableColumn<SupplierRow, String> phoneColumn;
    @FXML private TableColumn<SupplierRow, String> statusColumn;
    @FXML private TableColumn<SupplierRow, Void> actionColumn;

    private final SupplierBUS supplierBUS = new SupplierBUS();
    private final ObservableList<SupplierRow> allRows = FXCollections.observableArrayList();
    private final ObservableList<SupplierRow> filteredRows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        categoryComboBox.getItems().setAll(ALL_CATEGORY);
        statusComboBox.getItems().setAll(ALL_STATUS, STATUS_ACTIVE, STATUS_INACTIVE);
        categoryComboBox.getSelectionModel().selectFirst();
        statusComboBox.getSelectionModel().selectFirst();

        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        contactColumn.setCellValueFactory(new PropertyValueFactory<>("contact"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        actionColumn.setCellFactory(column -> new ActionCell());

        supplierTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        supplierTable.setItems(filteredRows);

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        categoryComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        statusComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());

        loadSuppliers();
    }

    private void loadSuppliers() {
        try {
            List<SupplierDirectoryItem> suppliers = supplierBUS.loadSuppliers();
            allRows.setAll(suppliers.stream().map(SupplierRow::from).toList());
            populateCategoryFilter();
            applyFilter();
        } catch (DatabaseException | PermissionException e) {
            AlertUtils.showError(e.getMessage());
            allRows.clear();
            filteredRows.clear();
        }
    }

    private void populateCategoryFilter() {
        Set<String> categories = new LinkedHashSet<>();
        categories.add(ALL_CATEGORY);
        allRows.stream()
                .map(SupplierRow::getCategory)
                .filter(value -> value != null && !value.isBlank())
                .forEach(categories::add);
        categoryComboBox.getItems().setAll(categories);
        categoryComboBox.getSelectionModel().selectFirst();
    }

    private void applyFilter() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String category = categoryComboBox.getValue();
        String status = statusComboBox.getValue();

        filteredRows.setAll(allRows.stream().filter(row -> {
            boolean matchesKeyword = keyword.isBlank()
                    || row.getCode().toLowerCase().contains(keyword)
                    || row.getName().toLowerCase().contains(keyword)
                    || row.getPhone().toLowerCase().contains(keyword)
                    || row.getContact().toLowerCase().contains(keyword);
            boolean matchesCategory = category == null || ALL_CATEGORY.equals(category) || row.getCategory().equals(category);
            boolean matchesStatus = status == null || ALL_STATUS.equals(status) || row.getStatus().equals(status);
            return matchesKeyword && matchesCategory && matchesStatus;
        }).toList());
    }

    private void showDetails(SupplierRow row) {
        AlertUtils.showInfo("Mã NCC: " + row.getCode()
                + "\nNhà cung cấp: " + row.getName()
                + "\nNhóm hàng: " + row.getCategory()
                + "\nLiên hệ: " + row.getContact()
                + "\nĐiện thoại: " + row.getPhone()
                + "\nEmail: " + row.getEmail()
                + "\nTrạng thái: " + row.getStatus());
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
            badge.getStyleClass().addAll("status-badge", STATUS_ACTIVE.equals(status) ? "status-success" : "status-warning");
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    private class ActionCell extends TableCell<SupplierRow, Void> {
        private final HBox box = new HBox(8);
        private final Button viewButton = new Button("Xem");

        ActionCell() {
            viewButton.getStyleClass().addAll("action-button", "action-view-button");
            viewButton.setOnAction(event -> {
                SupplierRow row = getTableRow() == null ? null : getTableRow().getItem();
                if (row != null) {
                    showDetails(row);
                }
            });
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(viewButton);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : box);
        }
    }

    public static class SupplierRow {
        private final String code;
        private final String name;
        private final String category;
        private final String contact;
        private final String phone;
        private final String email;
        private final String status;

        public SupplierRow(String code, String name, String category, String contact, String phone,
                           String email, String status) {
            this.code = code;
            this.name = name;
            this.category = category;
            this.contact = contact;
            this.phone = phone;
            this.email = email;
            this.status = status;
        }

        public static SupplierRow from(SupplierDirectoryItem item) {
            String statusLabel = item.getStatus() == 1 ? STATUS_ACTIVE : STATUS_INACTIVE;
            String contact = !"Chưa cập nhật".equals(item.getEmail()) ? item.getEmail() : item.getPhone();
            return new SupplierRow(
                    item.getSupplierId(),
                    item.getSupplierName(),
                    item.getCategory(),
                    contact,
                    item.getPhone(),
                    item.getEmail(),
                    statusLabel
            );
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public String getContact() {
            return contact;
        }

        public String getPhone() {
            return phone;
        }

        public String getEmail() {
            return email;
        }

        public String getStatus() {
            return status;
        }
    }
}
