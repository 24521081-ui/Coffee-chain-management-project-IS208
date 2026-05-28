package com.phungloccoffee.gui.controller.report;

import com.phungloccoffee.bus.RevenueReportBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.gui.service.ReportFilterUtils;
import com.phungloccoffee.model.report.BranchRevenueReport;
import com.phungloccoffee.model.report.BranchRevenueStatus;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.model.report.RevenuePeriodType;
import com.phungloccoffee.model.report.RevenueReportData;
import com.phungloccoffee.model.report.RevenueReportFilter;
import com.phungloccoffee.model.report.RevenueSummary;
import com.phungloccoffee.model.report.RevenueTrendPoint;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

public class RevenueReportController {
    private final RevenueReportBUS revenueReportBUS = new RevenueReportBUS();
    private RevenuePeriodType selectedPeriodType = RevenuePeriodType.DAY;
    private boolean updatingControls;
    private Task<RevenueReportData> currentLoadTask;

    @FXML private Button dayPeriodButton;
    @FXML private Button monthPeriodButton;
    @FXML private Button quarterPeriodButton;
    @FXML private Button yearPeriodButton;
    @FXML private ComboBox<BranchOption> branchFilterComboBox;
    @FXML private ComboBox<Integer> monthComboBox;
    @FXML private ComboBox<Integer> quarterComboBox;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Label totalRevenueValue;
    @FXML private Label totalOrdersValue;
    @FXML private Label averageOrderValue;
    @FXML private Label growthValue;
    @FXML private Label revenueGrowthBadge;
    @FXML private Label ordersBadge;
    @FXML private Label averageBadge;
    @FXML private Label growthBadge;
    @FXML private Label trendTitleLabel;
    @FXML private Label trendSubtitleLabel;
    @FXML private Label branchChartTitleLabel;
    @FXML private Label branchChartSubtitleLabel;
    @FXML private LineChart<String, Number> revenueTrendChart;
    @FXML private BarChart<String, Number> branchRevenueChart;
    @FXML private TableView<BranchRevenueReport> reportTable;
    @FXML private TableColumn<BranchRevenueReport, String> branchColumn;
    @FXML private TableColumn<BranchRevenueReport, String> revenueColumn;
    @FXML private TableColumn<BranchRevenueReport, String> orderCountColumn;
    @FXML private TableColumn<BranchRevenueReport, String> averageColumn;
    @FXML private TableColumn<BranchRevenueReport, String> growthColumn;
    @FXML private TableColumn<BranchRevenueReport, BranchRevenueStatus> statusColumn;

    @FXML
    private void initialize() {
        updatingControls = true;
        setupPeriodFilters();
        setupBranchFilter();
        setupTable();
        setupChartAxes();
        applyPeriodButtonStyles();
        updatingControls = false;
        loadReport();
    }

    private void setupPeriodFilters() {
        LocalDate today = LocalDate.now();
        monthComboBox.setItems(FXCollections.observableArrayList(IntStream.rangeClosed(1, 12).boxed().toList()));
        quarterComboBox.setItems(FXCollections.observableArrayList(IntStream.rangeClosed(1, 4).boxed().toList()));
        yearComboBox.setItems(FXCollections.observableArrayList(IntStream.rangeClosed(today.getYear() - 5, today.getYear() + 1).boxed().toList()));
        monthComboBox.getSelectionModel().select(Integer.valueOf(today.getMonthValue()));
        quarterComboBox.getSelectionModel().select(Integer.valueOf(((today.getMonthValue() - 1) / 3) + 1));
        yearComboBox.getSelectionModel().select(Integer.valueOf(today.getYear()));
        fromDatePicker.setValue(ReportFilterUtils.defaultFromDate());
        toDatePicker.setValue(ReportFilterUtils.defaultToDate());

        monthComboBox.valueProperty().addListener((observable, oldValue, newValue) -> handlePeriodValueChanged());
        quarterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> handlePeriodValueChanged());
        yearComboBox.valueProperty().addListener((observable, oldValue, newValue) -> handlePeriodValueChanged());
        fromDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> reloadAfterFilterChanged());
        toDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> reloadAfterFilterChanged());
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
            branchFilterComboBox.getItems().setAll(revenueReportBUS.getBranchOptions());
            branchFilterComboBox.getSelectionModel().selectFirst();
        } catch (DatabaseException e) {
            showDatabaseError(e);
        }
        branchFilterComboBox.valueProperty().addListener((observable, oldBranch, newBranch) -> reloadAfterFilterChanged());
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

    private void setupTable() {
        branchColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().branchName()));
        revenueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(ReportFilterUtils.formatMoney(cell.getValue().revenue())));
        orderCountColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(ReportFilterUtils.formatNumber(cell.getValue().orderCount())));
        averageColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(ReportFilterUtils.formatMoney(cell.getValue().averageOrderValue())));
        growthColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().growthLabel()));
        growthColumn.setCellFactory(column -> new GrowthCell<>());
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().status()));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
    private void selectDayPeriod() {
        selectPeriod(RevenuePeriodType.DAY);
    }

    @FXML
    private void selectMonthPeriod() {
        selectPeriod(RevenuePeriodType.MONTH);
    }

    @FXML
    private void selectQuarterPeriod() {
        selectPeriod(RevenuePeriodType.QUARTER);
    }

    @FXML
    private void selectYearPeriod() {
        selectPeriod(RevenuePeriodType.YEAR);
    }

    private void selectPeriod(RevenuePeriodType periodType) {
        selectedPeriodType = periodType;
        applyPeriodButtonStyles();
        applyDateRangeFromPeriodControls();
        reloadAfterFilterChanged();
    }

    private void handlePeriodValueChanged() {
        if (updatingControls) {
            return;
        }
        applyDateRangeFromPeriodControls();
        reloadAfterFilterChanged();
    }

    private void applyDateRangeFromPeriodControls() {
        if (selectedPeriodType == RevenuePeriodType.DAY) {
            return;
        }
        Integer selectedYear = yearComboBox.getValue();
        if (selectedYear == null) {
            return;
        }
        LocalDate fromDate;
        LocalDate toDate;
        switch (selectedPeriodType) {
            case MONTH -> {
                int month = monthComboBox.getValue() == null ? LocalDate.now().getMonthValue() : monthComboBox.getValue();
                fromDate = LocalDate.of(selectedYear, month, 1);
                toDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth());
            }
            case QUARTER -> {
                int quarter = quarterComboBox.getValue() == null ? 1 : quarterComboBox.getValue();
                int startMonth = (quarter - 1) * 3 + 1;
                fromDate = LocalDate.of(selectedYear, startMonth, 1);
                toDate = fromDate.plusMonths(2).withDayOfMonth(fromDate.plusMonths(2).lengthOfMonth());
            }
            case YEAR -> {
                fromDate = LocalDate.of(selectedYear, 1, 1);
                toDate = LocalDate.of(selectedYear, 12, 31);
            }
            default -> {
                return;
            }
        }
        updatingControls = true;
        fromDatePicker.setValue(fromDate);
        toDatePicker.setValue(toDate);
        updatingControls = false;
    }

    private void applyPeriodButtonStyles() {
        List<Button> buttons = List.of(dayPeriodButton, monthPeriodButton, quarterPeriodButton, yearPeriodButton);
        buttons.forEach(button -> button.getStyleClass().remove("active"));
        switch (selectedPeriodType) {
            case DAY, CUSTOM -> dayPeriodButton.getStyleClass().add("active");
            case MONTH -> monthPeriodButton.getStyleClass().add("active");
            case QUARTER -> quarterPeriodButton.getStyleClass().add("active");
            case YEAR -> yearPeriodButton.getStyleClass().add("active");
        }
    }

    private void reloadAfterFilterChanged() {
        if (!updatingControls) {
            loadReport();
        }
    }

    @FXML
    private void loadReport() {
        RevenueReportFilter filter;
        try {
            filter = readFilter();
        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
            return;
        }

        if (currentLoadTask != null && currentLoadTask.isRunning()) {
            currentLoadTask.cancel();
        }
        setLoadingState(true);
        Task<RevenueReportData> loadTask = new Task<>() {
            @Override
            protected RevenueReportData call() throws Exception {
                return revenueReportBUS.getRevenueReport(filter);
            }
        };
        currentLoadTask = loadTask;
        loadTask.setOnSucceeded(event -> {
            if (loadTask == currentLoadTask && !loadTask.isCancelled()) {
                renderReport(loadTask.getValue());
                setLoadingState(false);
            }
        });
        loadTask.setOnFailed(event -> {
            if (loadTask != currentLoadTask) {
                return;
            }
            clearReport();
            setLoadingState(false);
            Throwable error = loadTask.getException();
            if (error instanceof IllegalArgumentException) {
                showWarning(error.getMessage());
            } else {
                showDatabaseError(error);
            }
        });
        Thread thread = new Thread(loadTask, "revenue-report-loader");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void exportReport() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText("Chức năng chưa cập nhật");
        alert.showAndWait();
    }

    private RevenueReportFilter readFilter() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Vui lòng chọn đầy đủ khoảng thời gian báo cáo");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }
        return new RevenueReportFilter(selectedPeriodType, fromDate, toDate, selectedBranchId());
    }

    private String selectedBranchId() {
        BranchOption selectedBranch = branchFilterComboBox.getValue();
        return selectedBranch == null || selectedBranch.isAll() ? null : selectedBranch.id();
    }

    private void renderReport(RevenueReportData reportData) {
        clearReport();
        updateKpis(reportData.summary());
        seedTrendChart(reportData.trendPoints());
        seedBranchChart(reportData.topBranches());
        reportTable.setItems(FXCollections.observableArrayList(reportData.branchReports()));
        trendTitleLabel.setText(reportData.trendTitle());
        trendSubtitleLabel.setText(reportData.trendSubtitle());
        branchChartTitleLabel.setText(reportData.branchChartTitle());
        branchChartSubtitleLabel.setText(reportData.branchChartSubtitle());
    }

    private void updateKpis(RevenueSummary summary) {
        totalRevenueValue.setText(ReportFilterUtils.formatMoney(summary.totalRevenue()));
        totalOrdersValue.setText(ReportFilterUtils.formatNumber(summary.totalOrders()));
        averageOrderValue.setText(ReportFilterUtils.formatMoney(summary.averageOrderValue()));
        growthValue.setText(summary.growthLabel());
        revenueGrowthBadge.setText(summary.growthLabel());
        growthBadge.setText("So với kỳ trước");
        ordersBadge.setText("Từ đơn đã thanh toán");
        averageBadge.setText("Tính từ doanh thu gốc");
        applyGrowthStyle(revenueGrowthBadge, summary.growthLabel());
    }

    private void seedTrendChart(List<RevenueTrendPoint> rows) {
        CategoryAxis axis = (CategoryAxis) revenueTrendChart.getXAxis();
        axis.setCategories(FXCollections.observableArrayList(rows.stream().map(RevenueTrendPoint::label).toList()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        rows.forEach(row -> series.getData().add(new XYChart.Data<>(row.label(), row.revenue())));
        revenueTrendChart.getData().setAll(series);
    }

    private void seedBranchChart(List<BranchRevenueReport> branches) {
        CategoryAxis branchAxis = (CategoryAxis) branchRevenueChart.getXAxis();
        branchAxis.setCategories(FXCollections.observableArrayList(branches.stream().map(BranchRevenueReport::chartLabel).toList()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        branches.forEach(branch -> series.getData().add(new XYChart.Data<>(branch.chartLabel(), branch.revenue())));
        branchRevenueChart.getData().setAll(series);
    }

    private void applyGrowthStyle(Label label, String growth) {
        label.getStyleClass().removeAll("kpi-badge-up", "kpi-badge-warning", "kpi-badge-danger", "kpi-badge-neutral");
        if (growth != null && growth.startsWith("-")) {
            label.getStyleClass().add("kpi-badge-danger");
        } else {
            label.getStyleClass().add("kpi-badge-up");
        }
    }

    private void setLoadingState(boolean loading) {
        dayPeriodButton.setDisable(loading);
        monthPeriodButton.setDisable(loading);
        quarterPeriodButton.setDisable(loading);
        yearPeriodButton.setDisable(loading);
        branchFilterComboBox.setDisable(loading);
        monthComboBox.setDisable(loading);
        quarterComboBox.setDisable(loading);
        yearComboBox.setDisable(loading);
        fromDatePicker.setDisable(loading);
        toDatePicker.setDisable(loading);
    }

    private void clearReport() {
        totalRevenueValue.setText("0M");
        totalOrdersValue.setText("0");
        averageOrderValue.setText("0M");
        growthValue.setText("0%");
        revenueGrowthBadge.setText("0%");
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

    private void showDatabaseError(Throwable e) {
        e.printStackTrace();
        showWarning("Không thể tải báo cáo doanh thu từ Oracle. Vui lòng kiểm tra cấu hình database và dữ liệu liên quan.");
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

    private static class StatusCell<T> extends TableCell<T, BranchRevenueStatus> {
        @Override
        protected void updateItem(BranchRevenueStatus status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(status.displayName());
            badge.getStyleClass().addAll("status-badge", switch (status) {
                case TOT -> "status-success";
                case ON_DINH -> "status-info";
                case CANH_BAO, KEM -> "status-warning";
                case NGUY_HIEM -> "status-danger";
            });
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }
}
