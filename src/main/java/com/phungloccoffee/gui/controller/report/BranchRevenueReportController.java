package com.phungloccoffee.gui.controller.report;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.gui.service.RevenueReportService;
import com.phungloccoffee.gui.util.IconFactory;
import com.phungloccoffee.model.report.ReportModels.DailyRevenue;
import com.phungloccoffee.model.report.ReportModels.RevenueSummary;
import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BranchRevenueReportController {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Button dayTab;
    @FXML private Button monthTab;
    @FXML private Button quarterTab;
    @FXML private Button yearTab;
    @FXML private ComboBox<String> branchCombo;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private Label revenueValueLabel;
    @FXML private Label revenueGrowthLabel;
    @FXML private Label ordersValueLabel;
    @FXML private Label ordersGrowthLabel;
    @FXML private Label avgValueLabel;
    @FXML private Label avgHintLabel;
    @FXML private Label growthValueLabel;
    @FXML private Label growthHintLabel;
    @FXML private Label chartSubtitleLabel;
    @FXML private Label tablePeriodLabel;
    @FXML private StackPane revenueIcon;
    @FXML private StackPane orderIcon;
    @FXML private StackPane avgIcon;
    @FXML private StackPane growthIcon;
    @FXML private LineChart<String, Number> revenueLineChart;
    @FXML private TableView<RevenueRow> revenueTable;
    @FXML private TableColumn<RevenueRow, String> dateColumn;
    @FXML private TableColumn<RevenueRow, String> revenueColumn;
    @FXML private TableColumn<RevenueRow, String> orderColumn;
    @FXML private TableColumn<RevenueRow, String> avgColumn;
    @FXML private TableColumn<RevenueRow, String> growthColumn;
    @FXML private TableColumn<RevenueRow, String> noteColumn;

    private final RevenueReportService revenueReportService = new RevenueReportService();

    @FXML
    private void initialize() {
        branchCombo.setItems(FXCollections.observableArrayList("Chi nhánh hiện tại"));
        branchCombo.getSelectionModel().selectFirst();
        fromDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        toDatePicker.setValue(LocalDate.now());

        revenueIcon.getChildren().setAll(IconFactory.createReportIcon("money"));
        orderIcon.getChildren().setAll(IconFactory.createReportIcon("receipt"));
        avgIcon.getChildren().setAll(IconFactory.createReportIcon("cart"));
        growthIcon.getChildren().setAll(IconFactory.createReportIcon("trend"));

        dateColumn.setCellValueFactory(data -> data.getValue().dateProperty());
        revenueColumn.setCellValueFactory(data -> data.getValue().revenueProperty());
        orderColumn.setCellValueFactory(data -> data.getValue().ordersProperty());
        avgColumn.setCellValueFactory(data -> data.getValue().averageProperty());
        growthColumn.setCellValueFactory(data -> data.getValue().growthProperty());
        noteColumn.setCellValueFactory(data -> data.getValue().noteProperty());
        growthColumn.setCellFactory(column -> new GrowthCell());
        revenueTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadReport();
    }

    @FXML private void handleDayPeriod() { setRange(LocalDate.now(), LocalDate.now(), dayTab); }
    @FXML private void handleMonthPeriod() { setRange(LocalDate.now().withDayOfMonth(1), LocalDate.now(), monthTab); }
    @FXML private void handleQuarterPeriod() {
        LocalDate now = LocalDate.now();
        int startMonth = ((now.getMonthValue() - 1) / 3) * 3 + 1;
        setRange(LocalDate.of(now.getYear(), startMonth, 1), now, quarterTab);
    }
    @FXML private void handleYearPeriod() { setRange(LocalDate.now().withDayOfYear(1), LocalDate.now(), yearTab); }

    private void setRange(LocalDate from, LocalDate to, Button active) {
        fromDatePicker.setValue(from);
        toDatePicker.setValue(to);
        setActivePeriod(active);
        loadReport();
    }

    private void setActivePeriod(Button active) {
        List.of(dayTab, monthTab, quarterTab, yearTab).forEach(button -> button.getStyleClass().remove("period-tab-active"));
        if (!active.getStyleClass().contains("period-tab-active")) {
            active.getStyleClass().add("period-tab-active");
        }
    }

    private void loadReport() {
        try {
            LocalDate fromDate = fromDatePicker.getValue();
            LocalDate toDate = toDatePicker.getValue();
            String branchId = SessionManager.getCurrentBranchId();
            var data = revenueReportService.loadReport(fromDate, toDate, branchId);
            bindSummary(data.summary());
            bindChart(data.dailyRevenue());
            bindTable(data.dailyRevenue());
            chartSubtitleLabel.setText("Từ " + DATE_FORMATTER.format(fromDate) + " đến " + DATE_FORMATTER.format(toDate));
            tablePeriodLabel.setText("Chi nhánh hiện tại");
        } catch (DatabaseException e) {
            AlertUtils.showError(e.getMessage());
            revenueLineChart.getData().clear();
            revenueTable.setItems(FXCollections.observableArrayList());
        }
    }

    private void bindSummary(RevenueSummary summary) {
        revenueValueLabel.setText(formatMoneyCompact(summary.revenue()));
        revenueGrowthLabel.setText(formatGrowth(summary.growthPercent()));
        ordersValueLabel.setText(String.valueOf(summary.orders()));
        ordersGrowthLabel.setText(summary.orders() > 0 ? "Có phát sinh" : "Chưa có đơn");
        BigDecimal average = summary.orders() == 0
                ? BigDecimal.ZERO
                : summary.revenue().divide(BigDecimal.valueOf(summary.orders()), 0, java.math.RoundingMode.HALF_UP);
        avgValueLabel.setText(formatMoneyCompact(average));
        avgHintLabel.setText("Giá trị trung bình / đơn");
        growthValueLabel.setText(formatGrowth(summary.growthPercent()));
        growthHintLabel.setText("So với kỳ trước");
    }

    private void bindChart(List<DailyRevenue> rows) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        rows.forEach(row -> series.getData().add(new XYChart.Data<>(DATE_FORMATTER.format(row.date()), row.revenue())));
        revenueLineChart.getData().setAll(series);
    }

    private void bindTable(List<DailyRevenue> rows) {
        List<RevenueRow> tableRows = rows.stream().map(row -> new RevenueRow(
                DATE_FORMATTER.format(row.date()),
                formatMoney(row.revenue()),
                String.valueOf(row.orders()),
                row.orders() == 0 ? "0 đ" : formatMoney(row.revenue().divide(BigDecimal.valueOf(row.orders()), 0, java.math.RoundingMode.HALF_UP)),
                "0%",
                row.orders() > 0 ? "Có phát sinh doanh thu" : "Không có đơn hàng"
        )).toList();
        revenueTable.setItems(FXCollections.observableArrayList(tableRows));
    }

    private String formatMoneyCompact(BigDecimal value) {
        BigDecimal million = value.divide(BigDecimal.valueOf(1_000_000), 1, java.math.RoundingMode.HALF_UP);
        return million.stripTrailingZeros().toPlainString() + "M";
    }

    private String formatMoney(BigDecimal value) {
        return String.format("%,.0f đ", value);
    }

    private String formatGrowth(int growth) {
        return (growth > 0 ? "+" : "") + growth + "%";
    }

    private class GrowthCell extends TableCell<RevenueRow, String> {
        @Override
        protected void updateItem(String growth, boolean empty) {
            super.updateItem(growth, empty);
            if (empty || growth == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(growth);
            String style = growth.startsWith("-") ? "status-danger" : growth.startsWith("+") ? "status-success" : "badge-neutral";
            badge.getStyleClass().addAll("badge", style);
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    public static class RevenueRow {
        private final SimpleStringProperty date;
        private final SimpleStringProperty revenue;
        private final SimpleStringProperty orders;
        private final SimpleStringProperty average;
        private final SimpleStringProperty growth;
        private final SimpleStringProperty note;

        public RevenueRow(String date, String revenue, String orders, String average, String growth, String note) {
            this.date = new SimpleStringProperty(date);
            this.revenue = new SimpleStringProperty(revenue);
            this.orders = new SimpleStringProperty(orders);
            this.average = new SimpleStringProperty(average);
            this.growth = new SimpleStringProperty(growth);
            this.note = new SimpleStringProperty(note);
        }

        public SimpleStringProperty dateProperty() { return date; }
        public SimpleStringProperty revenueProperty() { return revenue; }
        public SimpleStringProperty ordersProperty() { return orders; }
        public SimpleStringProperty averageProperty() { return average; }
        public SimpleStringProperty growthProperty() { return growth; }
        public SimpleStringProperty noteProperty() { return note; }
    }
}
