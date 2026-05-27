package com.phungloccoffee.gui.controller.dashboard;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.gui.service.DashboardService;
import com.phungloccoffee.gui.service.DashboardService.DashboardData;
import com.phungloccoffee.gui.service.ReportFilterUtils;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.model.report.ReportModels.DailyRevenue;
import com.phungloccoffee.model.report.ReportModels.InventorySummary;
import com.phungloccoffee.model.report.ReportModels.ProductSummary;
import com.phungloccoffee.model.report.ReportModels.RevenueSummary;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.util.List;

import static com.phungloccoffee.model.report.ReportModels.ALL_BRANCH_ID;

public class DirectorDashboardController {
    private final DashboardService dashboardService = new DashboardService();

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<BranchOption> branchFilterComboBox;
    @FXML private Label revenueValue;
    @FXML private Label orderValue;
    @FXML private Label productValue;
    @FXML private Label alertValue;
    @FXML private Label periodInfo;
    @FXML private Label topBranchInfo;
    @FXML private Label topProductInfo;
    @FXML private Label inventoryWarningInfo;
    @FXML private BarChart<String, Number> branchRevenueChart;
    @FXML private TableView<ActivityRow> activityTable;
    @FXML private TableColumn<ActivityRow, String> branchColumn;
    @FXML private TableColumn<ActivityRow, String> metricColumn;
    @FXML private TableColumn<ActivityRow, String> valueColumn;
    @FXML private TableColumn<ActivityRow, String> statusColumn;
    @FXML private TableColumn<ActivityRow, String> noteColumn;

    @FXML
    private void initialize() {
        fromDatePicker.setValue(ReportFilterUtils.defaultFromDate());
        toDatePicker.setValue(ReportFilterUtils.defaultToDate());
        setupBranchFilter();
        branchColumn.setCellValueFactory(new PropertyValueFactory<>("branch"));
        metricColumn.setCellValueFactory(new PropertyValueFactory<>("metric"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
        activityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupChartAxes();
        loadDashboard();
    }

    private void setupBranchFilter() {
        branchFilterComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(BranchOption item) {
                return item == null ? "" : item.displayName();
            }

            @Override
            public BranchOption fromString(String value) {
                return branchFilterComboBox.getItems().stream()
                        .filter(item -> item.displayName().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        branchFilterComboBox.setCellFactory(list -> createBranchFilterCell());
        branchFilterComboBox.setButtonCell(createBranchFilterCell());
        try {
            branchFilterComboBox.getItems().setAll(dashboardService.loadBranchOptions());
            branchFilterComboBox.getSelectionModel().selectFirst();
        } catch (DatabaseException e) {
            showDatabaseError(e);
        }
    }

    private ListCell<BranchOption> createBranchFilterCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(BranchOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayName());
            }
        };
    }

    private void setupChartAxes() {
        CategoryAxis axis = (CategoryAxis) branchRevenueChart.getXAxis();
        axis.setTickLabelRotation(0);
        axis.setTickLabelGap(8);
        branchRevenueChart.setCategoryGap(16);
        branchRevenueChart.setBarGap(3);
    }

    @FXML
    private void loadDashboard() {
        String validationMessage = ReportFilterUtils.validateRange(fromDatePicker.getValue(), toDatePicker.getValue());
        if (validationMessage != null) {
            showWarning(validationMessage);
            return;
        }

        try {
            DashboardData data = dashboardService.loadDashboard(fromDatePicker.getValue(), toDatePicker.getValue(), selectedBranchId());
            updateKpis(data);
            seedChart(data.revenue().dailyRevenue());
            activityTable.setItems(FXCollections.observableArrayList(buildActivityRows(data)));
            if (data.revenue().summary().orders() == 0) {
                showWarning("Không có dữ liệu trong khoảng thời gian đã chọn");
            }
        } catch (DatabaseException e) {
            clearDashboard();
            showDatabaseError(e);
        }
    }

    private void updateKpis(DashboardData data) {
        RevenueSummary revenue = data.revenue().summary();
        ProductSummary products = data.bestSeller().summary();
        InventorySummary inventory = data.inventory().summary();
        revenueValue.setText(ReportFilterUtils.formatMoney(revenue.revenue()));
        orderValue.setText(ReportFilterUtils.formatNumber(revenue.orders()));
        productValue.setText(ReportFilterUtils.formatNumber(products.quantity()));
        alertValue.setText(ReportFilterUtils.formatNumber(inventory.lowStock() + inventory.outOfStock()));
        periodInfo.setText(ReportFilterUtils.formatDateLabel(fromDatePicker.getValue()) + " - " + ReportFilterUtils.formatDateLabel(toDatePicker.getValue()));
        topBranchInfo.setText(data.revenue().branchRevenue().isEmpty() ? "-" : data.revenue().branchRevenue().getFirst().displayName());
        topProductInfo.setText(products.topProduct() == null ? "-" : products.topProduct().productName());
        inventoryWarningInfo.setText(ReportFilterUtils.formatNumber(inventory.lowStock() + inventory.outOfStock()));
    }

    private void seedChart(List<DailyRevenue> rows) {
        CategoryAxis axis = (CategoryAxis) branchRevenueChart.getXAxis();
        axis.setCategories(FXCollections.observableArrayList(
                rows.stream().map(row -> ReportFilterUtils.formatDateLabel(row.date())).toList()
        ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu");
        rows.forEach(row -> series.getData().add(
                new XYChart.Data<>(ReportFilterUtils.formatDateLabel(row.date()), row.revenue())
        ));
        branchRevenueChart.getData().setAll(series);
    }

    private List<ActivityRow> buildActivityRows(DashboardData data) {
        RevenueSummary revenue = data.revenue().summary();
        ProductSummary products = data.bestSeller().summary();
        InventorySummary inventory = data.inventory().summary();
        String branchName = branchFilterComboBox.getValue() == null ? "Toàn hệ thống" : branchFilterComboBox.getValue().displayName();
        return List.of(
                new ActivityRow(branchName, "Doanh thu", ReportFilterUtils.formatMoney(revenue.revenue()), "Tốt", "Lấy từ hóa đơn đã thanh toán"),
                new ActivityRow(branchName, "Đơn hàng", ReportFilterUtils.formatNumber(revenue.orders()), "Tốt", "Không tính đơn đã hủy"),
                new ActivityRow(branchName, "Sản phẩm bán ra", ReportFilterUtils.formatNumber(products.quantity()), "Tốt", products.topProduct() == null ? "-" : "Top: " + products.topProduct().productName()),
                new ActivityRow(branchName, "Cảnh báo kho", ReportFilterUtils.formatNumber(inventory.lowStock() + inventory.outOfStock()), inventory.outOfStock() > 0 ? "Cảnh báo" : "Cần xử lý", "Tính theo tồn kho hiện tại trong Oracle")
        );
    }

    private String selectedBranchId() {
        BranchOption selectedBranch = branchFilterComboBox.getValue();
        return selectedBranch == null ? ALL_BRANCH_ID : selectedBranch.id();
    }

    private void clearDashboard() {
        revenueValue.setText("0M");
        orderValue.setText("0");
        productValue.setText("0");
        alertValue.setText("0");
        periodInfo.setText("-");
        topBranchInfo.setText("-");
        topProductInfo.setText("-");
        inventoryWarningInfo.setText("0");
        branchRevenueChart.getData().clear();
        activityTable.getItems().clear();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showDatabaseError(DatabaseException e) {
        e.printStackTrace();
        showWarning("Không thể kết nối cơ sở dữ liệu. Vui lòng kiểm tra cấu hình database.");
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
                case "Tốt" -> "status-success";
                case "Cần xử lý" -> "status-warning";
                default -> "status-danger";
            });
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    public static class ActivityRow {
        private final String branch;
        private final String metric;
        private final String value;
        private final String status;
        private final String note;

        public ActivityRow(String branch, String metric, String value, String status, String note) {
            this.branch = branch;
            this.metric = metric;
            this.value = value;
            this.status = status;
            this.note = note;
        }

        public String getBranch() {
            return branch;
        }

        public String getMetric() {
            return metric;
        }

        public String getValue() {
            return value;
        }

        public String getStatus() {
            return status;
        }

        public String getNote() {
            return note;
        }
    }
}
