package com.phungloccoffee.gui.controller.report;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.gui.service.InventoryReportService;
import com.phungloccoffee.gui.service.InventoryReportService.InventoryReportData;
import com.phungloccoffee.gui.service.ReportFilterUtils;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.model.report.ReportModels.InventoryBranch;
import com.phungloccoffee.model.report.ReportModels.InventoryCategory;
import com.phungloccoffee.model.report.ReportModels.InventoryItem;
import com.phungloccoffee.model.report.ReportModels.InventorySummary;
import javafx.collections.FXCollections;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.util.List;

import static com.phungloccoffee.model.report.ReportModels.ALL_BRANCH_ID;

public class InventoryReportController {
    private final InventoryReportService inventoryReportService = new InventoryReportService();

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<BranchOption> branchFilterComboBox;
    @FXML private ComboBox<String> categoryFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private Label trackedValue;
    @FXML private Label lowStockValue;
    @FXML private Label outOfStockValue;
    @FXML private Label inventoryValue;
    @FXML private Label warningBranch1Name;
    @FXML private Label warningBranch1Value;
    @FXML private Label warningBranch2Name;
    @FXML private Label warningBranch2Value;
    @FXML private Label warningBranch3Name;
    @FXML private Label warningBranch3Value;
    @FXML private BarChart<String, Number> branchWarningChart;
    @FXML private PieChart categoryPieChart;
    @FXML private TableView<InventoryRow> inventoryTable;
    @FXML private TableColumn<InventoryRow, String> branchColumn;
    @FXML private TableColumn<InventoryRow, String> trackedColumn;
    @FXML private TableColumn<InventoryRow, String> lowStockColumn;
    @FXML private TableColumn<InventoryRow, String> outOfStockColumn;
    @FXML private TableColumn<InventoryRow, String> valueColumn;
    @FXML private TableColumn<InventoryRow, String> statusColumn;

    @FXML
    private void initialize() {
        fromDatePicker.setValue(ReportFilterUtils.defaultFromDate());
        toDatePicker.setValue(ReportFilterUtils.defaultToDate());
        setupBranchFilter();
        setupFilterOptions();

        branchColumn.setCellValueFactory(new PropertyValueFactory<>("branch"));
        trackedColumn.setCellValueFactory(new PropertyValueFactory<>("tracked"));
        lowStockColumn.setCellValueFactory(new PropertyValueFactory<>("lowStock"));
        outOfStockColumn.setCellValueFactory(new PropertyValueFactory<>("outOfStock"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("inventoryValue"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupChartAxes();
        loadReport();
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
            branchFilterComboBox.getItems().setAll(inventoryReportService.loadBranchOptions());
            branchFilterComboBox.getSelectionModel().selectFirst();
        } catch (DatabaseException e) {
            showDatabaseError(e);
        }
    }

    private void setupFilterOptions() {
        try {
            categoryFilterComboBox.getItems().setAll(inventoryReportService.loadCategories());
            categoryFilterComboBox.getSelectionModel().selectFirst();
        } catch (DatabaseException e) {
            showDatabaseError(e);
        }
        statusFilterComboBox.getItems().setAll(inventoryReportService.loadStatuses());
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

    private void setupChartAxes() {
        CategoryAxis axis = (CategoryAxis) branchWarningChart.getXAxis();
        axis.setTickLabelRotation(0);
        axis.setTickLabelGap(8);
        axis.setStartMargin(18);
        axis.setEndMargin(18);
        branchWarningChart.setCategoryGap(14);
        branchWarningChart.setBarGap(3);
    }

    @FXML
    private void loadReport() {
        String validationMessage = ReportFilterUtils.validateRange(fromDatePicker.getValue(), toDatePicker.getValue());
        if (validationMessage != null) {
            showWarning(validationMessage);
            return;
        }

        try {
            InventoryReportData reportData = inventoryReportService.loadReport(
                    fromDatePicker.getValue(),
                    toDatePicker.getValue(),
                    selectedBranchId(),
                    categoryFilterComboBox.getValue(),
                    statusFilterComboBox.getValue()
            );
            updateKpis(reportData.summary());
            updateWarningList(reportData.branches());
            seedCharts(reportData.branches(), reportData.categories());
            inventoryTable.setItems(FXCollections.observableArrayList(
                    reportData.items().stream().map(this::toInventoryRow).toList()
            ));
            if (reportData.items().isEmpty()) {
                showWarning("Không có dữ liệu trong khoảng thời gian đã chọn");
            }
        } catch (DatabaseException e) {
            clearReport();
            showDatabaseError(e);
        }
    }

    private void updateKpis(InventorySummary summary) {
        trackedValue.setText(ReportFilterUtils.formatNumber(summary.tracked()));
        lowStockValue.setText(ReportFilterUtils.formatNumber(summary.lowStock()));
        outOfStockValue.setText(ReportFilterUtils.formatNumber(summary.outOfStock()));
        inventoryValue.setText(ReportFilterUtils.formatMoney(summary.value()));
    }

    private void seedCharts(List<InventoryBranch> branchRows, List<InventoryCategory> categoryRows) {
        CategoryAxis axis = (CategoryAxis) branchWarningChart.getXAxis();
        axis.setCategories(FXCollections.observableArrayList(
                branchRows.stream().map(InventoryBranch::chartLabel).toList()
        ));

        XYChart.Series<String, Number> warnings = new XYChart.Series<>();
        branchRows.forEach(row -> warnings.getData().add(
                new XYChart.Data<>(row.chartLabel(), row.warningCount())
        ));
        branchWarningChart.getData().setAll(warnings);
        categoryPieChart.setData(FXCollections.observableArrayList(
                categoryRows.stream()
                        .map(row -> new PieChart.Data(row.category(), row.value().doubleValue()))
                        .toList()
        ));
    }

    private void updateWarningList(List<InventoryBranch> branches) {
        setWarningRow(0, branches, warningBranch1Name, warningBranch1Value);
        setWarningRow(1, branches, warningBranch2Name, warningBranch2Value);
        setWarningRow(2, branches, warningBranch3Name, warningBranch3Value);
    }

    private void setWarningRow(int index, List<InventoryBranch> branches, Label nameLabel, Label valueLabel) {
        if (index >= branches.size()) {
            nameLabel.setText("-");
            valueLabel.setText("0 cảnh báo");
            return;
        }
        InventoryBranch branch = branches.get(index);
        nameLabel.setText(branch.displayName());
        valueLabel.setText(ReportFilterUtils.formatNumber(branch.warningCount()) + " cảnh báo");
    }

    private InventoryRow toInventoryRow(InventoryItem item) {
        return new InventoryRow(
                item.productName(),
                item.branchName(),
                ReportFilterUtils.formatNumber(item.currentQuantity()),
                ReportFilterUtils.formatNumber(item.minQuantity()),
                ReportFilterUtils.formatMoney(item.value()),
                item.status()
        );
    }

    private String selectedBranchId() {
        BranchOption selectedBranch = branchFilterComboBox.getValue();
        return selectedBranch == null ? ALL_BRANCH_ID : selectedBranch.id();
    }

    private void clearReport() {
        trackedValue.setText("0");
        lowStockValue.setText("0");
        outOfStockValue.setText("0");
        inventoryValue.setText("0M");
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
                case "Ổn định" -> "status-success";
                case "Tồn thấp", "Cần kiểm tra" -> "status-warning";
                default -> "status-danger";
            });
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    public static class InventoryRow {
        private final String branch;
        private final String tracked;
        private final String lowStock;
        private final String outOfStock;
        private final String inventoryValue;
        private final String status;

        public InventoryRow(String branch, String tracked, String lowStock, String outOfStock, String inventoryValue, String status) {
            this.branch = branch;
            this.tracked = tracked;
            this.lowStock = lowStock;
            this.outOfStock = outOfStock;
            this.inventoryValue = inventoryValue;
            this.status = status;
        }

        public String getBranch() {
            return branch;
        }

        public String getTracked() {
            return tracked;
        }

        public String getLowStock() {
            return lowStock;
        }

        public String getOutOfStock() {
            return outOfStock;
        }

        public String getInventoryValue() {
            return inventoryValue;
        }

        public String getStatus() {
            return status;
        }
    }
}
