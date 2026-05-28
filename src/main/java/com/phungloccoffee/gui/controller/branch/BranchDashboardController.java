package com.phungloccoffee.gui.controller.branch;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.gui.service.InventoryReportService;
import com.phungloccoffee.gui.service.RevenueReportService;
import com.phungloccoffee.gui.util.IconFactory;
import com.phungloccoffee.model.WarehouseApprovalItem;
import com.phungloccoffee.model.WarehouseSlipType;
import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.SessionManager;
import com.phungloccoffee.bus.WarehouseWorkflowBUS;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BranchDashboardController {
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");

    @FXML private Label revenueValueLabel;
    @FXML private Label revenueGrowthLabel;
    @FXML private Label ordersValueLabel;
    @FXML private Label ordersHintLabel;
    @FXML private Label approvalValueLabel;
    @FXML private Label approvalHintLabel;
    @FXML private Label stockWarningValueLabel;
    @FXML private Label stockWarningHintLabel;
    @FXML private Label chartTitleLabel;
    @FXML private Label chartSubtitleLabel;
    @FXML private Label pendingCountLabel;
    @FXML private Label approvedCountLabel;
    @FXML private Label rejectedCountLabel;
    @FXML private StackPane revenueIcon;
    @FXML private StackPane ordersIcon;
    @FXML private StackPane approvalIcon;
    @FXML private StackPane stockIcon;
    @FXML private LineChart<String, Number> revenueChart;
    @FXML private TableView<ActivityRow> activityTable;
    @FXML private TableColumn<ActivityRow, String> activityColumn;
    @FXML private TableColumn<ActivityRow, String> ownerColumn;
    @FXML private TableColumn<ActivityRow, String> timeColumn;
    @FXML private TableColumn<ActivityRow, String> statusColumn;

    private final RevenueReportService revenueReportService = new RevenueReportService();
    private final InventoryReportService inventoryReportService = new InventoryReportService();
    private final WarehouseWorkflowBUS workflowBUS = new WarehouseWorkflowBUS();

    @FXML
    private void initialize() {
        revenueIcon.getChildren().setAll(IconFactory.createReportIcon("money"));
        ordersIcon.getChildren().setAll(IconFactory.createReportIcon("receipt"));
        approvalIcon.getChildren().setAll(IconFactory.createReportIcon("clipboard"));
        stockIcon.getChildren().setAll(IconFactory.createReportIcon("alert"));

        activityColumn.setCellValueFactory(data -> data.getValue().activityProperty());
        ownerColumn.setCellValueFactory(data -> data.getValue().ownerProperty());
        timeColumn.setCellValueFactory(data -> data.getValue().timeProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        statusColumn.setCellFactory(column -> new StatusCell());
        activityTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        loadDashboard();
    }

    private void loadDashboard() {
        try {
            String branchId = SessionManager.getCurrentBranchId();
            LocalDate today = LocalDate.now();
            var revenue = revenueReportService.loadReport(today.minusDays(6), today, branchId);
            var inventory = inventoryReportService.loadReport(today.minusDays(30), today, branchId, "Tất cả nhóm", "Tất cả trạng thái");
            List<WarehouseApprovalItem> approvals = new ArrayList<>();
            approvals.addAll(workflowBUS.loadApprovalItems(WarehouseSlipType.IMPORT, null));
            approvals.addAll(workflowBUS.loadApprovalItems(WarehouseSlipType.EXPORT, null));
            approvals.addAll(workflowBUS.loadApprovalItems(WarehouseSlipType.STOCKTAKE, null));

            revenueValueLabel.setText(formatMoneyCompact(revenue.summary().revenue()));
            revenueGrowthLabel.setText(formatGrowth(revenue.summary().growthPercent()));
            ordersValueLabel.setText(String.valueOf(revenue.summary().orders()));
            ordersHintLabel.setText("7 ngày gần nhất");
            approvalValueLabel.setText(String.valueOf(approvals.size()));
            approvalHintLabel.setText("Cần xử lý");
            stockWarningValueLabel.setText(String.valueOf(inventory.summary().lowStock() + inventory.summary().outOfStock()));
            stockWarningHintLabel.setText("Kiểm tra ngay");

            chartTitleLabel.setText("Doanh thu theo ngày");
            chartSubtitleLabel.setText("7 ngày gần nhất");
            bindChart(revenue.dailyRevenue());

            pendingCountLabel.setText(String.valueOf(approvals.size()));
            approvedCountLabel.setText("0");
            rejectedCountLabel.setText("0");

            activityTable.setItems(FXCollections.observableArrayList(
                    approvals.stream()
                            .sorted(Comparator.comparing(WarehouseApprovalItem::getCreatedAt).reversed())
                            .limit(8)
                            .map(item -> new ActivityRow(
                                    describeApproval(item),
                                    item.getCreatedBy(),
                                    item.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")),
                                    "Chờ duyệt"
                            ))
                            .toList()
            ));
        } catch (DatabaseException | PermissionException e) {
            AlertUtils.showError(e.getMessage());
            revenueChart.getData().clear();
            activityTable.setItems(FXCollections.observableArrayList());
        }
    }

    private void bindChart(List<com.phungloccoffee.model.report.ReportModels.DailyRevenue> rows) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        rows.forEach(row -> series.getData().add(new XYChart.Data<>(DAY_FORMATTER.format(row.date()), row.revenue())));
        revenueChart.getData().setAll(series);
    }

    private String describeApproval(WarehouseApprovalItem item) {
        return switch (item.getSlipType()) {
            case WarehouseSlipType.IMPORT -> "Duyệt phiếu nhập " + item.getSlipId();
            case WarehouseSlipType.EXPORT -> "Duyệt phiếu xuất " + item.getSlipId();
            case WarehouseSlipType.STOCKTAKE -> "Duyệt phiếu kiểm kê " + item.getSlipId();
            default -> "Phiếu kho " + item.getSlipId();
        };
    }

    private String formatMoneyCompact(java.math.BigDecimal value) {
        java.math.BigDecimal million = value.divide(java.math.BigDecimal.valueOf(1_000_000), 1, java.math.RoundingMode.HALF_UP);
        return million.stripTrailingZeros().toPlainString() + "M";
    }

    private String formatGrowth(int growth) {
        return (growth > 0 ? "+" : "") + growth + "%";
    }

    private static String statusStyle(String status) {
        return switch (status) {
            case "Đang xử lý" -> "status-info";
            case "Hoàn tất" -> "status-success";
            default -> "status-warning";
        };
    }

    private class StatusCell extends TableCell<ActivityRow, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label badge = new Label(status);
            badge.getStyleClass().addAll("status-badge", statusStyle(status));
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    public static class ActivityRow {
        private final SimpleStringProperty activity;
        private final SimpleStringProperty owner;
        private final SimpleStringProperty time;
        private final SimpleStringProperty status;

        public ActivityRow(String activity, String owner, String time, String status) {
            this.activity = new SimpleStringProperty(activity);
            this.owner = new SimpleStringProperty(owner);
            this.time = new SimpleStringProperty(time);
            this.status = new SimpleStringProperty(status);
        }

        public SimpleStringProperty activityProperty() { return activity; }
        public SimpleStringProperty ownerProperty() { return owner; }
        public SimpleStringProperty timeProperty() { return time; }
        public SimpleStringProperty statusProperty() { return status; }
    }
}
