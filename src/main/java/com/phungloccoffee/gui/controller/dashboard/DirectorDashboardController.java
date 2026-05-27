package com.phungloccoffee.gui.controller.dashboard;

import com.phungloccoffee.bus.DirectorDashboardBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.gui.service.ReportFilterUtils;
import com.phungloccoffee.model.dashboard.DashboardData;
import com.phungloccoffee.model.dashboard.DashboardFilter;
import com.phungloccoffee.model.dashboard.DashboardKpi;
import com.phungloccoffee.model.dashboard.DashboardMetric;
import com.phungloccoffee.model.dashboard.MetricStatus;
import com.phungloccoffee.model.dashboard.PeriodType;
import com.phungloccoffee.model.dashboard.RevenuePoint;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
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
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.IntStream;

public class DirectorDashboardController {
    private final DirectorDashboardBUS directorDashboardBUS = new DirectorDashboardBUS();
    private boolean updatingControls;
    private Task<DashboardData> currentLoadTask;

    @FXML private ComboBox<PeriodType> periodTypeComboBox;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<Integer> monthComboBox;
    @FXML private ComboBox<Integer> yearComboBox;
    @FXML private ComboBox<BranchOption> branchFilterComboBox;
    @FXML private Label revenueValue;
    @FXML private Label orderValue;
    @FXML private Label productValue;
    @FXML private Label alertValue;
    @FXML private Label revenueChangeLabel;
    @FXML private Label orderChangeLabel;
    @FXML private Label productChangeLabel;
    @FXML private Label alertStatusLabel;
    @FXML private Label chartSubtitleLabel;
    @FXML private Label periodInfo;
    @FXML private Label branchOverviewTitle;
    @FXML private Label topBranchInfo;
    @FXML private Label topProductInfo;
    @FXML private Label inventoryWarningInfo;
    @FXML private BarChart<String, Number> branchRevenueChart;
    @FXML private TableView<DashboardMetric> activityTable;
    @FXML private TableColumn<DashboardMetric, String> branchColumn;
    @FXML private TableColumn<DashboardMetric, String> metricColumn;
    @FXML private TableColumn<DashboardMetric, String> valueColumn;
    @FXML private TableColumn<DashboardMetric, MetricStatus> statusColumn;
    @FXML private TableColumn<DashboardMetric, String> noteColumn;

    @FXML
    private void initialize() {
        updatingControls = true;
        setupPeriodFilters();
        setupBranchFilter();
        setupTable();
        setupChartAxes();
        updatingControls = false;
        applyResolvedDatesFromPeriod();
        loadDashboard();
    }

    private void setupPeriodFilters() {
        periodTypeComboBox.setItems(FXCollections.observableArrayList(PeriodType.values()));
        periodTypeComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(PeriodType type) {
                return type == null ? "" : type.displayName();
            }

            @Override
            public PeriodType fromString(String value) {
                return periodTypeComboBox.getItems().stream()
                        .filter(type -> type.displayName().equals(value))
                        .findFirst()
                        .orElse(PeriodType.CUSTOM);
            }
        });
        periodTypeComboBox.getSelectionModel().select(PeriodType.THIS_WEEK);

        LocalDate today = LocalDate.now();
        monthComboBox.setItems(FXCollections.observableArrayList(IntStream.rangeClosed(1, 12).boxed().toList()));
        monthComboBox.getSelectionModel().select(Integer.valueOf(today.getMonthValue()));
        yearComboBox.setItems(FXCollections.observableArrayList(IntStream.rangeClosed(today.getYear() - 5, today.getYear() + 1).boxed().toList()));
        yearComboBox.getSelectionModel().select(Integer.valueOf(today.getYear()));

        fromDatePicker.setValue(ReportFilterUtils.defaultFromDate());
        toDatePicker.setValue(ReportFilterUtils.defaultToDate());

        periodTypeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> handlePeriodFilterChanged());
        monthComboBox.valueProperty().addListener((observable, oldValue, newValue) -> handlePeriodFilterChanged());
        yearComboBox.valueProperty().addListener((observable, oldValue, newValue) -> handlePeriodFilterChanged());
        fromDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleDateFilterChanged());
        toDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> handleDateFilterChanged());
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
            branchFilterComboBox.getItems().setAll(directorDashboardBUS.getBranchOptions());
            branchFilterComboBox.getSelectionModel().selectFirst();
        } catch (DatabaseException e) {
            showDatabaseError(e);
        }
        branchFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> reloadAfterFilterChanged());
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
        branchColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().branch()));
        metricColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().metric()));
        valueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().value()));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().status()));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        noteColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().note()));
        activityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupChartAxes() {
        CategoryAxis axis = (CategoryAxis) branchRevenueChart.getXAxis();
        axis.setTickLabelRotation(0);
        axis.setTickLabelGap(8);
        branchRevenueChart.setCategoryGap(16);
        branchRevenueChart.setBarGap(3);
    }

    private void handlePeriodFilterChanged() {
        if (updatingControls) {
            return;
        }
        applyResolvedDatesFromPeriod();
        reloadAfterFilterChanged();
    }

    private void handleDateFilterChanged() {
        if (updatingControls) {
            return;
        }
        updatingControls = true;
        periodTypeComboBox.getSelectionModel().select(PeriodType.CUSTOM);
        updatingControls = false;
        reloadAfterFilterChanged();
    }

    private void reloadAfterFilterChanged() {
        if (!updatingControls) {
            loadDashboard();
        }
    }

    private void applyResolvedDatesFromPeriod() {
        PeriodType type = periodTypeComboBox.getValue();
        if (type == null || type == PeriodType.CUSTOM) {
            return;
        }
        DashboardFilter resolved = directorDashboardBUS.resolvePeriod(
                new DashboardFilter(fromDatePicker.getValue(), toDatePicker.getValue(), selectedBranchId(), type),
                monthComboBox.getValue(),
                yearComboBox.getValue()
        );
        updatingControls = true;
        fromDatePicker.setValue(resolved.fromDate());
        toDatePicker.setValue(resolved.toDate());
        updatingControls = false;
    }

    @FXML
    private void loadDashboard() {
        DashboardFilter filter;
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
        Task<DashboardData> loadTask = new Task<>() {
            @Override
            protected DashboardData call() throws Exception {
                return directorDashboardBUS.getDashboardData(filter);
            }
        };
        currentLoadTask = loadTask;
        loadTask.setOnSucceeded(event -> {
            if (loadTask == currentLoadTask && !loadTask.isCancelled()) {
                renderDashboard(loadTask.getValue());
                setLoadingState(false);
            }
        });
        loadTask.setOnFailed(event -> {
            if (loadTask != currentLoadTask) {
                return;
            }
            clearDashboard();
            setLoadingState(false);
            Throwable error = loadTask.getException();
            if (error instanceof IllegalArgumentException) {
                showWarning(error.getMessage());
            } else {
                showDatabaseError(error);
            }
        });
        Thread thread = new Thread(loadTask, "director-dashboard-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private DashboardFilter readFilter() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Vui lòng chọn đầy đủ khoảng thời gian báo cáo");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }
        return new DashboardFilter(fromDate, toDate, selectedBranchId(), periodTypeComboBox.getValue());
    }

    private String selectedBranchId() {
        BranchOption selectedBranch = branchFilterComboBox.getValue();
        return selectedBranch == null || selectedBranch.isAll() ? null : selectedBranch.id();
    }

    private void renderDashboard(DashboardData data) {
        updateKpis(data);
        seedChart(data);
        activityTable.setItems(FXCollections.observableArrayList(data.activityMetrics()));
        periodInfo.setText(data.periodLabel());
        branchOverviewTitle.setText(data.branchOverviewLabel());
        topBranchInfo.setText(data.topBranch() == null ? "Không có dữ liệu" : data.topBranch().branchName());
        topProductInfo.setText(data.topProduct() == null ? "Không có dữ liệu" : data.topProduct().productName());
        inventoryWarningInfo.setText(ReportFilterUtils.formatNumber(data.inventoryWarningKpi().value()));
        chartSubtitleLabel.setText("Đơn vị: đồng, kỳ " + data.periodLabel());
    }

    private void updateKpis(DashboardData data) {
        revenueValue.setText(ReportFilterUtils.formatMoney(data.revenueKpi().value()));
        orderValue.setText(formatInteger(data.orderKpi().value()));
        productValue.setText(ReportFilterUtils.formatNumber(data.productKpi().value()));
        alertValue.setText(formatInteger(data.inventoryWarningKpi().value()));
        updateChangeBadge(revenueChangeLabel, data.revenueKpi());
        updateChangeBadge(orderChangeLabel, data.orderKpi());
        updateChangeBadge(productChangeLabel, data.productKpi());
        alertStatusLabel.setText(data.inventoryWarningKpi().status().displayName());
        applyKpiStatusStyle(alertStatusLabel, data.inventoryWarningKpi().status());
    }

    private void updateChangeBadge(Label label, DashboardKpi kpi) {
        label.setText(kpi.formattedChange());
        applyKpiStatusStyle(label, kpi.status());
    }

    private void applyKpiStatusStyle(Label label, MetricStatus status) {
        label.getStyleClass().removeAll("kpi-badge-up", "kpi-badge-warning", "kpi-badge-danger", "kpi-badge-neutral");
        label.getStyleClass().add(switch (status) {
            case GOOD, NEW -> "kpi-badge-up";
            case WARNING -> "kpi-badge-warning";
            case DANGER -> "kpi-badge-danger";
        });
    }

    private void seedChart(DashboardData data) {
        CategoryAxis axis = (CategoryAxis) branchRevenueChart.getXAxis();
        axis.setCategories(FXCollections.observableArrayList(
                data.revenueByDate().stream().map(point -> ReportFilterUtils.formatDateLabel(point.date())).toList()
        ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu");
        for (RevenuePoint point : data.revenueByDate()) {
            series.getData().add(new XYChart.Data<>(ReportFilterUtils.formatDateLabel(point.date()), point.revenue()));
        }
        branchRevenueChart.getData().setAll(series);
    }

    private String formatInteger(BigDecimal value) {
        return ReportFilterUtils.formatNumber(value);
    }

    private void setLoadingState(boolean loading) {
        periodTypeComboBox.setDisable(loading);
        fromDatePicker.setDisable(loading);
        toDatePicker.setDisable(loading);
        monthComboBox.setDisable(loading);
        yearComboBox.setDisable(loading);
        branchFilterComboBox.setDisable(loading);
    }

    private void clearDashboard() {
        revenueValue.setText("0M");
        orderValue.setText("0");
        productValue.setText("0");
        alertValue.setText("0");
        revenueChangeLabel.setText("0%");
        orderChangeLabel.setText("0%");
        productChangeLabel.setText("0%");
        alertStatusLabel.setText("-");
        periodInfo.setText("-");
        branchOverviewTitle.setText("Chi nhánh dẫn đầu");
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

    private void showDatabaseError(Throwable e) {
        e.printStackTrace();
        showWarning("Không thể tải dữ liệu dashboard từ Oracle. Vui lòng kiểm tra cấu hình database và dữ liệu liên quan.");
    }

    private static class StatusCell<T> extends TableCell<T, MetricStatus> {
        @Override
        protected void updateItem(MetricStatus status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(status.displayName());
            badge.getStyleClass().addAll("status-badge", switch (status) {
                case GOOD, NEW -> "status-success";
                case WARNING -> "status-warning";
                case DANGER -> "status-danger";
            });
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }
}
