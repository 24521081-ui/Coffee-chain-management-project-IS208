package com.phungloccoffee.gui.controller.report;

import com.phungloccoffee.bus.InventoryReportBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.gui.service.ReportFilterUtils;
import com.phungloccoffee.model.report.InventoryBranchAlertPoint;
import com.phungloccoffee.model.report.InventoryCategoryOption;
import com.phungloccoffee.model.report.InventoryCategoryValuePoint;
import com.phungloccoffee.model.report.InventoryItemReport;
import com.phungloccoffee.model.report.InventoryReportData;
import com.phungloccoffee.model.report.InventoryReportFilter;
import com.phungloccoffee.model.report.InventoryStatus;
import com.phungloccoffee.model.report.InventorySummary;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.PieChart;
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

import java.time.LocalDate;
import java.util.List;

public class InventoryReportController {
    private final InventoryReportBUS inventoryReportBUS = new InventoryReportBUS();
    private boolean updatingControls;
    private Task<InventoryReportData> currentLoadTask;

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<BranchOption> branchFilterComboBox;
    @FXML private ComboBox<InventoryCategoryOption> categoryFilterComboBox;
    @FXML private ComboBox<InventoryStatus> statusFilterComboBox;
    @FXML private Label trackedValue;
    @FXML private Label lowStockValue;
    @FXML private Label outOfStockValue;
    @FXML private Label inventoryValue;
    @FXML private Label inventoryGrowthBadge;
    @FXML private Label categoryChartTitleLabel;
    @FXML private Label warningBranch1Name;
    @FXML private Label warningBranch1Value;
    @FXML private Label warningBranch2Name;
    @FXML private Label warningBranch2Value;
    @FXML private Label warningBranch3Name;
    @FXML private Label warningBranch3Value;
    @FXML private BarChart<String, Number> branchWarningChart;
    @FXML private PieChart categoryPieChart;
    @FXML private TableView<InventoryItemReport> inventoryTable;
    @FXML private TableColumn<InventoryItemReport, String> branchColumn;
    @FXML private TableColumn<InventoryItemReport, String> trackedColumn;
    @FXML private TableColumn<InventoryItemReport, String> lowStockColumn;
    @FXML private TableColumn<InventoryItemReport, String> outOfStockColumn;
    @FXML private TableColumn<InventoryItemReport, String> valueColumn;
    @FXML private TableColumn<InventoryItemReport, InventoryStatus> statusColumn;

    @FXML
    private void initialize() {
        updatingControls = true;
        setupBranchFilter();
        setupCategoryFilter();
        setupStatusFilter();
        setupDefaultDates();
        setupTable();
        setupChartAxes();
        setupReloadListeners();
        updatingControls = false;
        loadReport();
    }

    private void setupDefaultDates() {
        try {
            LocalDate latestDate = inventoryReportBUS.getDefaultReportDate();
            fromDatePicker.setValue(latestDate.minusDays(30));
            toDatePicker.setValue(latestDate);
        } catch (DatabaseException e) {
            fromDatePicker.setValue(ReportFilterUtils.defaultFromDate());
            toDatePicker.setValue(ReportFilterUtils.defaultToDate());
            showDatabaseError(e);
        }
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
            branchFilterComboBox.getItems().setAll(inventoryReportBUS.getBranchOptions());
            branchFilterComboBox.getSelectionModel().selectFirst();
        } catch (DatabaseException e) {
            showDatabaseError(e);
        }
    }

    private void setupCategoryFilter() {
        categoryFilterComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(InventoryCategoryOption item) {
                return item == null ? "" : item.displayName();
            }

            @Override
            public InventoryCategoryOption fromString(String value) {
                return categoryFilterComboBox.getItems().stream()
                        .filter(item -> item.displayName().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        categoryFilterComboBox.setCellFactory(list -> createCategoryFilterCell());
        categoryFilterComboBox.setButtonCell(createCategoryFilterCell());
        try {
            categoryFilterComboBox.getItems().setAll(inventoryReportBUS.getCategoryOptions());
            categoryFilterComboBox.getSelectionModel().selectFirst();
        } catch (DatabaseException e) {
            showDatabaseError(e);
        }
    }

    private void setupStatusFilter() {
        statusFilterComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(InventoryStatus status) {
                return status == null ? "Tất cả trạng thái" : status.displayName();
            }

            @Override
            public InventoryStatus fromString(String value) {
                return statusFilterComboBox.getItems().stream()
                        .filter(status -> status != null && status.displayName().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        statusFilterComboBox.setCellFactory(list -> createStatusFilterCell());
        statusFilterComboBox.setButtonCell(createStatusFilterCell());
        statusFilterComboBox.setItems(FXCollections.observableArrayList(null, InventoryStatus.STABLE,
                InventoryStatus.LOW_STOCK, InventoryStatus.OUT_OF_STOCK));
        statusFilterComboBox.getSelectionModel().selectFirst();
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

    private ListCell<InventoryCategoryOption> createCategoryFilterCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(InventoryCategoryOption item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.displayName());
            }
        };
    }

    private ListCell<InventoryStatus> createStatusFilterCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(InventoryStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "Tất cả trạng thái" : item.displayName());
            }
        };
    }

    private void setupReloadListeners() {
        fromDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> reloadAfterFilterChanged());
        toDatePicker.valueProperty().addListener((observable, oldValue, newValue) -> reloadAfterFilterChanged());
        branchFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> reloadAfterFilterChanged());
        categoryFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> reloadAfterFilterChanged());
        statusFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> reloadAfterFilterChanged());
    }

    private void setupTable() {
        branchColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().productName()));
        trackedColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().branchName()));
        lowStockColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(ReportFilterUtils.formatNumber(cell.getValue().quantityOnHand())));
        outOfStockColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(ReportFilterUtils.formatNumber(cell.getValue().minQuantity())));
        valueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(ReportFilterUtils.formatMoney(cell.getValue().inventoryValue())));
        statusColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().status()));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupChartAxes() {
        CategoryAxis axis = (CategoryAxis) branchWarningChart.getXAxis();
        axis.setTickLabelRotation(0);
        axis.setTickLabelGap(8);
        axis.setStartMargin(18);
        axis.setEndMargin(18);
        branchWarningChart.setCategoryGap(14);
        branchWarningChart.setBarGap(3);
    }

    private void reloadAfterFilterChanged() {
        if (!updatingControls) {
            loadReport();
        }
    }

    @FXML
    private void loadReport() {
        InventoryReportFilter filter;
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
        Task<InventoryReportData> loadTask = new Task<>() {
            @Override
            protected InventoryReportData call() throws Exception {
                return inventoryReportBUS.getInventoryReport(filter);
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
        Thread thread = new Thread(loadTask, "inventory-report-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private InventoryReportFilter readFilter() {
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Vui lòng chọn đầy đủ khoảng thời gian báo cáo");
        }
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được lớn hơn ngày kết thúc");
        }
        return new InventoryReportFilter(
                fromDate,
                toDate,
                selectedBranchId(),
                selectedCategoryId(),
                statusFilterComboBox.getValue()
        );
    }

    private String selectedBranchId() {
        BranchOption selectedBranch = branchFilterComboBox.getValue();
        return selectedBranch == null || selectedBranch.isAll() ? null : selectedBranch.id();
    }

    private String selectedCategoryId() {
        InventoryCategoryOption selectedCategory = categoryFilterComboBox.getValue();
        return selectedCategory == null || selectedCategory.isAll() ? null : selectedCategory.id();
    }

    private void renderReport(InventoryReportData reportData) {
        clearReport();
        updateKpis(reportData.summary());
        updateWarningList(reportData.branchAlerts());
        seedCharts(reportData.branchAlerts(), reportData.categoryValues());
        categoryChartTitleLabel.setText(reportData.categoryChartTitle());
        inventoryTable.setItems(FXCollections.observableArrayList(reportData.items()));
    }

    private void updateKpis(InventorySummary summary) {
        trackedValue.setText(ReportFilterUtils.formatNumber(summary.totalTrackedProducts()));
        lowStockValue.setText(ReportFilterUtils.formatNumber(summary.alertBranchCount()));
        outOfStockValue.setText(ReportFilterUtils.formatNumber(summary.lowStockItemCount()));
        inventoryValue.setText(ReportFilterUtils.formatMoney(summary.totalInventoryValue()));
        inventoryGrowthBadge.setText(summary.growthLabel());
        applyGrowthStyle(inventoryGrowthBadge, summary.growthLabel());
    }

    private void seedCharts(List<InventoryBranchAlertPoint> branchRows, List<InventoryCategoryValuePoint> categoryRows) {
        CategoryAxis axis = (CategoryAxis) branchWarningChart.getXAxis();
        axis.setCategories(FXCollections.observableArrayList(
                branchRows.stream().map(InventoryBranchAlertPoint::chartLabel).toList()
        ));

        XYChart.Series<String, Number> warnings = new XYChart.Series<>();
        branchRows.forEach(row -> warnings.getData().add(
                new XYChart.Data<>(row.chartLabel(), row.alertCount())
        ));
        branchWarningChart.getData().setAll(warnings);
        categoryPieChart.setData(FXCollections.observableArrayList(
                categoryRows.stream()
                        .map(row -> new PieChart.Data(row.categoryName(), row.inventoryValue().doubleValue()))
                        .toList()
        ));
    }

    private void updateWarningList(List<InventoryBranchAlertPoint> branches) {
        setWarningRow(0, branches, warningBranch1Name, warningBranch1Value);
        setWarningRow(1, branches, warningBranch2Name, warningBranch2Value);
        setWarningRow(2, branches, warningBranch3Name, warningBranch3Value);
    }

    private void setWarningRow(int index, List<InventoryBranchAlertPoint> branches, Label nameLabel, Label valueLabel) {
        if (index >= branches.size()) {
            nameLabel.setText("-");
            valueLabel.setText("0 cảnh báo");
            return;
        }
        InventoryBranchAlertPoint branch = branches.get(index);
        nameLabel.setText(branch.branchName());
        valueLabel.setText(ReportFilterUtils.formatNumber(branch.alertCount()) + " cảnh báo");
    }

    private void applyGrowthStyle(Label label, String growth) {
        label.getStyleClass().removeAll("kpi-badge-up", "kpi-badge-warning", "kpi-badge-danger", "kpi-badge-neutral");
        label.getStyleClass().add(growth != null && growth.startsWith("-") ? "kpi-badge-danger" : "kpi-badge-up");
    }

    private void setLoadingState(boolean loading) {
        fromDatePicker.setDisable(loading);
        toDatePicker.setDisable(loading);
        branchFilterComboBox.setDisable(loading);
        categoryFilterComboBox.setDisable(loading);
        statusFilterComboBox.setDisable(loading);
    }

    private void clearReport() {
        trackedValue.setText("0");
        lowStockValue.setText("0");
        outOfStockValue.setText("0");
        inventoryValue.setText("0M");
        inventoryGrowthBadge.setText("0%");
        updateWarningList(List.of());
        branchWarningChart.getData().clear();
        categoryPieChart.getData().clear();
        inventoryTable.getItems().clear();
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
        showWarning("Không thể tải báo cáo tồn kho từ Oracle. Vui lòng kiểm tra cấu hình database và dữ liệu liên quan.");
    }

    private static class StatusCell<T> extends TableCell<T, InventoryStatus> {
        @Override
        protected void updateItem(InventoryStatus status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(status.displayName());
            badge.getStyleClass().addAll("status-badge", switch (status) {
                case STABLE -> "status-success";
                case LOW_STOCK -> "status-warning";
                case OUT_OF_STOCK -> "status-danger";
            });
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }
}
