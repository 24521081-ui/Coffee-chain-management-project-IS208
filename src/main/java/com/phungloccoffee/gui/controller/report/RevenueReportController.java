package com.phungloccoffee.gui.controller.report;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.gui.service.ReportFilterUtils;
import com.phungloccoffee.gui.service.RevenueReportService;
import com.phungloccoffee.gui.service.RevenueReportService.RevenueReportData;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.model.report.ReportModels.BranchRevenue;
import com.phungloccoffee.model.report.ReportModels.DailyRevenue;
import com.phungloccoffee.model.report.ReportModels.RevenueSummary;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
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

import java.math.BigDecimal;
import java.util.List;

import static com.phungloccoffee.model.report.ReportModels.ALL_BRANCH_ID;

public class RevenueReportController {
    private final RevenueReportService revenueReportService = new RevenueReportService();

    @FXML private ComboBox<BranchOption> branchFilterComboBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Label totalRevenueValue;
    @FXML private Label totalOrdersValue;
    @FXML private Label averageOrderValue;
    @FXML private Label growthValue;
    @FXML private LineChart<String, Number> revenueTrendChart;
    @FXML private BarChart<String, Number> branchRevenueChart;
    @FXML private TableView<RevenueRow> reportTable;
    @FXML private TableColumn<RevenueRow, String> branchColumn;
    @FXML private TableColumn<RevenueRow, String> revenueColumn;
    @FXML private TableColumn<RevenueRow, String> orderCountColumn;
    @FXML private TableColumn<RevenueRow, String> averageColumn;
    @FXML private TableColumn<RevenueRow, String> growthColumn;
    @FXML private TableColumn<RevenueRow, String> statusColumn;

    @FXML
    private void initialize() {
        fromDatePicker.setValue(ReportFilterUtils.defaultFromDate());
        toDatePicker.setValue(ReportFilterUtils.defaultToDate());
        setupBranchFilter();

        branchColumn.setCellValueFactory(new PropertyValueFactory<>("branch"));
        revenueColumn.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        orderCountColumn.setCellValueFactory(new PropertyValueFactory<>("orders"));
        averageColumn.setCellValueFactory(new PropertyValueFactory<>("average"));
        growthColumn.setCellValueFactory(new PropertyValueFactory<>("growth"));
        growthColumn.setCellFactory(column -> new GrowthCell<>());
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupChartAxes();
        branchFilterComboBox.valueProperty().addListener((observable, oldBranch, newBranch) -> loadReport());
        loadReport();
    }

    private void setupChartAxes() {
        formatRevenueAxis((NumberAxis) revenueTrendChart.getYAxis());
        formatRevenueAxis((NumberAxis) branchRevenueChart.getYAxis());
        CategoryAxis branchAxis = (CategoryAxis) branchRevenueChart.getXAxis();
        branchAxis.setTickLabelRotation(0);
        branchAxis.setTickLabelGap(8);
        branchAxis.setStartMargin(18);
        branchAxis.setEndMargin(18);
        branchRevenueChart.setCategoryGap(14);
        branchRevenueChart.setBarGap(3);
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
            branchFilterComboBox.getItems().setAll(revenueReportService.loadBranchOptions());
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

    private void formatRevenueAxis(NumberAxis axis) {
        axis.setLabel("");
        axis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return ReportFilterUtils.formatMoney(BigDecimal.valueOf(value.doubleValue()));
            }

            @Override
            public Number fromString(String value) {
                return 0;
            }
        });
    }

    @FXML
    private void loadReport() {
        String validationMessage = ReportFilterUtils.validateRange(fromDatePicker.getValue(), toDatePicker.getValue());
        if (validationMessage != null) {
            showWarning(validationMessage);
            return;
        }

        try {
            RevenueReportData reportData = revenueReportService.loadReport(
                    fromDatePicker.getValue(),
                    toDatePicker.getValue(),
                    selectedBranchId()
            );
            updateKpis(reportData.summary());
            seedTrendChart(reportData.dailyRevenue());
            seedBranchChart(reportData.branchRevenue());
            reportTable.setItems(FXCollections.observableArrayList(
                    reportData.branchRevenue().stream().map(this::toRevenueRow).toList()
            ));
            if (reportData.summary().orders() == 0) {
                showWarning("Không có dữ liệu trong khoảng thời gian đã chọn");
            }
        } catch (DatabaseException e) {
            clearReport();
            showDatabaseError(e);
        }
    }

    private void updateKpis(RevenueSummary summary) {
        totalRevenueValue.setText(ReportFilterUtils.formatMoney(summary.revenue()));
        totalOrdersValue.setText(ReportFilterUtils.formatNumber(summary.orders()));
        averageOrderValue.setText(ReportFilterUtils.averageOrderValue(summary.revenue(), summary.orders()));
        growthValue.setText(ReportFilterUtils.formatPercent(summary.growthPercent()));
    }

    private void seedTrendChart(List<DailyRevenue> rows) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        rows.forEach(row -> series.getData().add(
                new XYChart.Data<>(ReportFilterUtils.formatDateLabel(row.date()), row.revenue())
        ));
        revenueTrendChart.getData().setAll(series);
    }

    private String selectedBranchId() {
        BranchOption selectedBranch = branchFilterComboBox.getValue();
        return selectedBranch == null ? ALL_BRANCH_ID : selectedBranch.id();
    }

    private RevenueRow toRevenueRow(BranchRevenue branch) {
        return new RevenueRow(
                branch.fullName(),
                ReportFilterUtils.formatMoney(branch.revenue()),
                ReportFilterUtils.formatNumber(branch.orders()),
                ReportFilterUtils.averageOrderValue(branch.revenue(), branch.orders()),
                "0%",
                branch.revenue().compareTo(BigDecimal.ZERO) > 0 ? "Tốt" : "Không có dữ liệu"
        );
    }

    private void seedBranchChart(List<BranchRevenue> branches) {
        CategoryAxis branchAxis = (CategoryAxis) branchRevenueChart.getXAxis();
        branchAxis.setCategories(FXCollections.observableArrayList(
                branches.stream().map(BranchRevenue::chartLabel).toList()
        ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        branches.forEach(branch -> series.getData().add(
                new XYChart.Data<>(branch.chartLabel(), branch.revenue())
        ));
        branchRevenueChart.getData().setAll(series);
    }

    private void clearReport() {
        totalRevenueValue.setText("0M");
        totalOrdersValue.setText("0");
        averageOrderValue.setText("0K");
        growthValue.setText("0%");
        revenueTrendChart.getData().clear();
        branchRevenueChart.getData().clear();
        reportTable.getItems().clear();
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

    private static class GrowthCell<T> extends TableCell<T, String> {
        @Override
        protected void updateItem(String growth, boolean empty) {
            super.updateItem(growth, empty);
            if (empty || growth == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(growth);
            badge.getStyleClass().addAll("status-badge", growth.startsWith("-") ? "status-danger" : "status-success");
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
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
            badge.getStyleClass().addAll("status-badge", switch (status) {
                case "Tốt" -> "status-success";
                case "Không có dữ liệu" -> "status-neutral";
                default -> "status-warning";
            });
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    public static class RevenueRow {
        private final String branch;
        private final String revenue;
        private final String orders;
        private final String average;
        private final String growth;
        private final String status;

        public RevenueRow(String branch, String revenue, String orders, String average, String growth, String status) {
            this.branch = branch;
            this.revenue = revenue;
            this.orders = orders;
            this.average = average;
            this.growth = growth;
            this.status = status;
        }

        public String getBranch() {
            return branch;
        }

        public String getRevenue() {
            return revenue;
        }

        public String getOrders() {
            return orders;
        }

        public String getAverage() {
            return average;
        }

        public String getGrowth() {
            return growth;
        }

        public String getStatus() {
            return status;
        }
    }
}
