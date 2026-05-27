package com.phungloccoffee.gui.controller.customer;

import com.phungloccoffee.bus.CustomerBUS;
import com.phungloccoffee.model.CustomerPageResult;
import com.phungloccoffee.model.CustomerSummary;
import com.phungloccoffee.model.KhachHang;
import com.phungloccoffee.util.AlertUtils;

import javafx.collections.FXCollections;
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

import java.util.ArrayList;
import java.util.List;

public class CustomerListController {
    private static final String ALL_RANKS = "Tất cả hạng";
    private static final int DEFAULT_PAGE_SIZE = 10;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> tierFilterComboBox;
    @FXML private Label totalCustomersLabel;
    @FXML private Label newCustomersThisMonthLabel;
    @FXML private Label pageInfoLabel;
    @FXML private Label resultCountLabel;
    @FXML private Button previousPageButton;
    @FXML private Button nextPageButton;
    @FXML private TableView<KhachHang> customerTable;
    @FXML private TableColumn<KhachHang, String> codeColumn;
    @FXML private TableColumn<KhachHang, String> nameColumn;
    @FXML private TableColumn<KhachHang, String> phoneColumn;
    @FXML private TableColumn<KhachHang, String> emailColumn;
    @FXML private TableColumn<KhachHang, String> tierColumn;
    @FXML private TableColumn<KhachHang, Integer> pointsColumn;
    @FXML private TableColumn<KhachHang, Void> actionColumn;

    private final CustomerBUS customerBUS = new CustomerBUS();
    private int currentPage = 1;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private int totalRows;
    private int totalPages = 1;

    @FXML
    private void initialize() {
        configureColumns();
        loadRankFilter();
        configureEvents();
        loadSummary();
        loadCustomerPage();
    }

    private void configureColumns() {
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("khachHangId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("hoTen"));
        phoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        tierColumn.setCellValueFactory(new PropertyValueFactory<>("hangThanhVien"));
        pointsColumn.setCellValueFactory(new PropertyValueFactory<>("diemTichLuy"));
        actionColumn.setCellFactory(column -> new ActionCell());
        customerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadRankFilter() {
        List<String> ranks = new ArrayList<>();
        ranks.add(ALL_RANKS);
        try {
            ranks.addAll(customerBUS.getMembershipRanks());
        } catch (Exception e) {
            AlertUtils.showError(e.getMessage());
        }
        tierFilterComboBox.setItems(FXCollections.observableArrayList(ranks));
        tierFilterComboBox.getSelectionModel().selectFirst();
    }

    private void configureEvents() {
        searchField.setOnAction(event -> handleSearch());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            currentPage = 1;
            loadCustomerPage();
        });
        tierFilterComboBox.setOnAction(event -> handleFilterRank());
    }

    private void loadSummary() {
        try {
            CustomerSummary summary = customerBUS.getCustomerSummary();
            totalCustomersLabel.setText(String.valueOf(summary.getTotalCustomers()));
            newCustomersThisMonthLabel.setText(String.valueOf(summary.getNewCustomersThisMonth()));
        } catch (Exception e) {
            totalCustomersLabel.setText("0");
            newCustomersThisMonthLabel.setText("0");
            AlertUtils.showError(e.getMessage());
        }
    }

    private void loadCustomerPage() {
        try {
            CustomerPageResult result = customerBUS.getCustomerPage(
                    searchField.getText(),
                    tierFilterComboBox.getValue(),
                    currentPage,
                    pageSize
            );
            currentPage = result.getCurrentPage();
            pageSize = result.getPageSize();
            totalRows = result.getTotalRows();
            totalPages = result.getTotalPages();
            customerTable.setItems(FXCollections.observableArrayList(result.getCustomers()));
            updatePagination();
        } catch (Exception e) {
            customerTable.setItems(FXCollections.observableArrayList());
            totalRows = 0;
            totalPages = 1;
            currentPage = 1;
            updatePagination();
            AlertUtils.showError(e.getMessage());
        }
    }

    @FXML
    private void handleSearch() {
        currentPage = 1;
        loadCustomerPage();
    }

    @FXML
    private void handleFilterRank() {
        currentPage = 1;
        loadCustomerPage();
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadCustomerPage();
        }
    }

    @FXML
    private void handlePreviousPage() {
        if (currentPage > 1) {
            currentPage--;
            loadCustomerPage();
        }
    }

    @FXML
    private void handleViewCustomer() {
        handleViewCustomer(customerTable.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleEditCustomer() {
        handleEditCustomer(customerTable.getSelectionModel().getSelectedItem());
    }

    private void handleViewCustomer(KhachHang customer) {
        if (customer == null) {
            AlertUtils.showWarning("Vui lòng chọn khách hàng cần xem.");
            return;
        }
        AlertUtils.showInfo("Khách hàng: " + customer.getHoTen()
                + "\nMã KH: " + customer.getKhachHangId()
                + "\nĐiện thoại: " + nullToEmpty(customer.getPhone())
                + "\nEmail: " + nullToEmpty(customer.getEmail())
                + "\nHạng: " + nullToEmpty(customer.getHangThanhVien())
                + "\nĐiểm: " + customer.getDiemTichLuy());
    }

    private void handleEditCustomer(KhachHang customer) {
        if (customer == null) {
            AlertUtils.showWarning("Vui lòng chọn khách hàng cần sửa.");
            return;
        }
        AlertUtils.showInfo("Chức năng sửa khách hàng đang chờ màn hình nhập liệu. Mã KH: " + customer.getKhachHangId());
    }

    private void updatePagination() {
        pageInfoLabel.setText("Trang " + currentPage + " / " + totalPages);
        resultCountLabel.setText("Tổng " + totalRows + " khách hàng");
        previousPageButton.setDisable(totalRows == 0 || currentPage <= 1);
        nextPageButton.setDisable(totalRows == 0 || currentPage >= totalPages);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private class ActionCell extends TableCell<KhachHang, Void> {
        private final HBox box = new HBox(8);
        private final Button viewButton = new Button("Xem");
        private final Button editButton = new Button("Sửa");

        ActionCell() {
            viewButton.getStyleClass().addAll("action-button", "action-view-button");
            editButton.getStyleClass().addAll("action-button", "action-edit-button");
            viewButton.setOnAction(event -> handleViewCustomer(getTableRow().getItem()));
            editButton.setOnAction(event -> handleEditCustomer(getTableRow().getItem()));
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().addAll(viewButton, editButton);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }
            setGraphic(box);
        }
    }
}
