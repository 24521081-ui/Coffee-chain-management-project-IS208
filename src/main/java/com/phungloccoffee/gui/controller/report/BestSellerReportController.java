package com.phungloccoffee.gui.controller.report;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.gui.service.BestSellerReportService;
import com.phungloccoffee.gui.service.BestSellerReportService.BestSellerReportData;
import com.phungloccoffee.gui.service.ReportFilterUtils;
import com.phungloccoffee.model.report.ReportModels.BranchOption;
import com.phungloccoffee.model.report.ReportModels.ProductSale;
import com.phungloccoffee.model.report.ReportModels.ProductSummary;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.phungloccoffee.model.report.ReportModels.ALL_BRANCH_ID;

public class BestSellerReportController {
    private final BestSellerReportService bestSellerReportService = new BestSellerReportService();

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<BranchOption> branchComboBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Label totalQuantityValue;
    @FXML private Label topProductValue;
    @FXML private Label topProductBadge;
    @FXML private Label topRevenueValue;
    @FXML private Label insightLabel;
    @FXML private Label coffeeShareValue;
    @FXML private Label teaShareValue;
    @FXML private Label bakeryShareValue;
    @FXML private BarChart<String, Number> bestSellerChart;
    @FXML private TableView<BestSellerRow> bestSellerTable;
    @FXML private TableColumn<BestSellerRow, String> rankColumn;
    @FXML private TableColumn<BestSellerRow, String> productColumn;
    @FXML private TableColumn<BestSellerRow, String> categoryColumn;
    @FXML private TableColumn<BestSellerRow, String> quantityColumn;
    @FXML private TableColumn<BestSellerRow, String> revenueColumn;
    @FXML private TableColumn<BestSellerRow, String> shareColumn;
    @FXML private TableColumn<BestSellerRow, String> trendColumn;

    @FXML
    private void initialize() {
        fromDatePicker.setValue(ReportFilterUtils.defaultFromDate());
        toDatePicker.setValue(ReportFilterUtils.defaultToDate());
        setupBranchFilter();
        setupCategoryFilter();

        rankColumn.setCellValueFactory(new PropertyValueFactory<>("rank"));
        productColumn.setCellValueFactory(new PropertyValueFactory<>("product"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        revenueColumn.setCellValueFactory(new PropertyValueFactory<>("revenue"));
        shareColumn.setCellValueFactory(new PropertyValueFactory<>("share"));
        trendColumn.setCellValueFactory(new PropertyValueFactory<>("trend"));
        trendColumn.setCellFactory(column -> new TrendCell<>());
        bestSellerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setupChartAxes();
        loadReport();
    }

    private void setupBranchFilter() {
        branchComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(BranchOption item) {
                return item == null ? "" : item.displayName();
            }

            @Override
            public BranchOption fromString(String value) {
                return branchComboBox.getItems().stream()
                        .filter(item -> item.displayName().equals(value))
                        .findFirst()
                        .orElse(null);
            }
        });
        branchComboBox.setCellFactory(list -> createBranchFilterCell());
        branchComboBox.setButtonCell(createBranchFilterCell());
        try {
            branchComboBox.getItems().setAll(bestSellerReportService.loadBranchOptions());
            branchComboBox.getSelectionModel().selectFirst();
        } catch (DatabaseException e) {
            showDatabaseError(e);
        }
    }

    private void setupCategoryFilter() {
        try {
            categoryComboBox.getItems().setAll(bestSellerReportService.loadCategories());
            categoryComboBox.getSelectionModel().selectFirst();
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
        CategoryAxis axis = (CategoryAxis) bestSellerChart.getXAxis();
        axis.setTickLabelRotation(0);
        axis.setTickLabelGap(8);
        axis.setStartMargin(16);
        axis.setEndMargin(16);
        bestSellerChart.setCategoryGap(10);
        bestSellerChart.setBarGap(3);
    }

    @FXML
    private void loadReport() {
        String validationMessage = ReportFilterUtils.validateRange(fromDatePicker.getValue(), toDatePicker.getValue());
        if (validationMessage != null) {
            showWarning(validationMessage);
            return;
        }

        try {
            BestSellerReportData reportData = bestSellerReportService.loadReport(
                    fromDatePicker.getValue(),
                    toDatePicker.getValue(),
                    selectedBranchId(),
                    categoryComboBox.getValue()
            );
            updateKpis(reportData.summary());
            updateInsight(reportData.products());
            seedChart(reportData.products().stream().limit(10).toList());
            bestSellerTable.setItems(FXCollections.observableArrayList(toRows(reportData.products())));
            if (reportData.products().isEmpty()) {
                showWarning("Không có dữ liệu trong khoảng thời gian đã chọn");
            }
        } catch (DatabaseException e) {
            clearReport();
            showDatabaseError(e);
        }
    }

    private void updateKpis(ProductSummary summary) {
        totalQuantityValue.setText(ReportFilterUtils.formatNumber(summary.quantity()));
        topRevenueValue.setText(ReportFilterUtils.formatMoney(summary.revenue()));
        if (summary.topProduct() == null) {
            topProductValue.setText("-");
            topProductBadge.setText("0 ly");
            return;
        }
        topProductValue.setText(summary.topProduct().productName());
        topProductBadge.setText(ReportFilterUtils.formatNumber(summary.topProduct().quantity()) + " ly");
    }

    private void updateInsight(List<ProductSale> rows) {
        if (rows.isEmpty()) {
            insightLabel.setText("Không có dữ liệu trong khoảng thời gian đã chọn.");
            coffeeShareValue.setText("0%");
            teaShareValue.setText("0%");
            bakeryShareValue.setText("0%");
            return;
        }
        ProductSale top = rows.getFirst();
        insightLabel.setText(top.productName() + " đang dẫn đầu trong khoảng ngày đã chọn. Dữ liệu lấy từ hóa đơn đã thanh toán trong Oracle Database.");
        Map<String, BigDecimal> shares = rows.stream()
                .collect(Collectors.groupingBy(ProductSale::categoryName, Collectors.mapping(ProductSale::quantity, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        BigDecimal total = rows.stream().map(ProductSale::quantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        coffeeShareValue.setText(categoryShare(shares, total, "Cà phê"));
        teaShareValue.setText(categoryShare(shares, total, "Trà"));
        bakeryShareValue.setText(categoryShare(shares, total, "Bánh"));
    }

    private String categoryShare(Map<String, BigDecimal> shares, BigDecimal total, String category) {
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return "0%";
        }
        return shares.getOrDefault(category, BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP)
                .toPlainString() + "%";
    }

    private void seedChart(List<ProductSale> rows) {
        CategoryAxis axis = (CategoryAxis) bestSellerChart.getXAxis();
        axis.setCategories(FXCollections.observableArrayList(
                rows.stream().map(ProductSale::chartLabel).toList()
        ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        rows.forEach(row -> series.getData().add(
                new XYChart.Data<>(row.chartLabel(), row.quantity())
        ));
        bestSellerChart.getData().setAll(series);
    }

    private List<BestSellerRow> toRows(List<ProductSale> rows) {
        return rows.stream()
                .map(row -> new BestSellerRow(
                        String.valueOf(rows.indexOf(row) + 1),
                        row.productName(),
                        row.categoryName(),
                        ReportFilterUtils.formatNumber(row.quantity()),
                        ReportFilterUtils.formatMoney(row.revenue()),
                        row.sharePercent() + "%",
                        ReportFilterUtils.formatPercent(row.trendPercent())
                ))
                .toList();
    }

    private String selectedBranchId() {
        BranchOption selectedBranch = branchComboBox.getValue();
        return selectedBranch == null ? ALL_BRANCH_ID : selectedBranch.id();
    }

    private void clearReport() {
        totalQuantityValue.setText("0");
        topProductValue.setText("-");
        topProductBadge.setText("0 ly");
        topRevenueValue.setText("0M");
        insightLabel.setText("Không có dữ liệu trong khoảng thời gian đã chọn.");
        coffeeShareValue.setText("0%");
        teaShareValue.setText("0%");
        bakeryShareValue.setText("0%");
        bestSellerChart.getData().clear();
        bestSellerTable.getItems().clear();
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

    private static class TrendCell<T> extends TableCell<T, String> {
        @Override
        protected void updateItem(String trend, boolean empty) {
            super.updateItem(trend, empty);
            if (empty || trend == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(trend);
            badge.getStyleClass().addAll("status-badge", trend.startsWith("-") ? "status-danger" : "status-success");
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    public static class BestSellerRow {
        private final String rank;
        private final String product;
        private final String category;
        private final String quantity;
        private final String revenue;
        private final String share;
        private final String trend;

        public BestSellerRow(String rank, String product, String category, String quantity,
                             String revenue, String share, String trend) {
            this.rank = rank;
            this.product = product;
            this.category = category;
            this.quantity = quantity;
            this.revenue = revenue;
            this.share = share;
            this.trend = trend;
        }

        public String getRank() {
            return rank;
        }

        public String getProduct() {
            return product;
        }

        public String getCategory() {
            return category;
        }

        public String getQuantity() {
            return quantity;
        }

        public String getRevenue() {
            return revenue;
        }

        public String getShare() {
            return share;
        }

        public String getTrend() {
            return trend;
        }
    }
}
