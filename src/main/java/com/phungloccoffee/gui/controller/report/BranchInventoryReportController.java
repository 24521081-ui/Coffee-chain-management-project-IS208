package com.phungloccoffee.gui.controller.report;

import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.gui.service.InventoryReportService;
import com.phungloccoffee.gui.util.IconFactory;
import com.phungloccoffee.model.report.ReportModels.InventoryCategory;
import com.phungloccoffee.model.report.ReportModels.InventoryItem;
import com.phungloccoffee.util.AlertUtils;
import com.phungloccoffee.util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public class BranchInventoryReportController {
    private static final String ALL_STATUS = "Tất cả trạng thái";
    private static final String ALL_CATEGORY = "Tất cả nhóm";
    private static final String STATUS_STABLE = "Ổn định";
    private static final String STATUS_LOW = "Tồn thấp";
    private static final String STATUS_OUT = "Hết hàng";

    @FXML private HBox tabOverview;
    @FXML private HBox tabDetail;
    @FXML private VBox overviewPane;
    @FXML private VBox detailPane;
    @FXML private StackPane overviewTabIcon;
    @FXML private StackPane detailTabIcon;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private Label trackedValueLabel;
    @FXML private Label lowStockValueLabel;
    @FXML private Label outOfStockValueLabel;
    @FXML private Label inventoryValueLabel;
    @FXML private StackPane productIcon;
    @FXML private StackPane lowStockIcon;
    @FXML private StackPane reorderIcon;
    @FXML private StackPane valueIcon;
    @FXML private BarChart<String, Number> inventoryBarChart;
    @FXML private VBox inventoryAlertList;
    @FXML private TableView<InventoryRow> inventoryDetailTable;
    @FXML private TableColumn<InventoryRow, String> codeColumn;
    @FXML private TableColumn<InventoryRow, String> productColumn;
    @FXML private TableColumn<InventoryRow, String> categoryColumn;
    @FXML private TableColumn<InventoryRow, String> stockColumn;
    @FXML private TableColumn<InventoryRow, String> warningColumn;
    @FXML private TableColumn<InventoryRow, String> ratioColumn;
    @FXML private TableColumn<InventoryRow, String> differenceColumn;
    @FXML private TableColumn<InventoryRow, String> statusColumn;
    @FXML private TableColumn<InventoryRow, String> suggestionColumn;

    private final InventoryReportService inventoryReportService = new InventoryReportService();
    private final ObservableList<InventoryRow> rows = FXCollections.observableArrayList();
    private FilteredList<InventoryRow> filteredRows;

    @FXML
    private void initialize() {
        setupTabs();
        setupFilters();
        setupIcons();
        setupTable();
        loadReport();
    }

    @FXML
    private void showOverviewTab() {
        setActiveTab(true);
    }

    @FXML
    private void showDetailTab() {
        setActiveTab(false);
    }

    @FXML
    private void handleFilter() {
        loadReport();
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        statusFilter.getSelectionModel().select(ALL_STATUS);
        categoryFilter.getSelectionModel().select(ALL_CATEGORY);
        loadReport();
    }

    private void setupTabs() {
        overviewTabIcon.getChildren().setAll(IconFactory.createReportIcon("trend"));
        detailTabIcon.getChildren().setAll(IconFactory.createReportIcon("list"));
        setActiveTab(true);
    }

    private void setActiveTab(boolean overviewActive) {
        overviewPane.setVisible(overviewActive);
        overviewPane.setManaged(overviewActive);
        detailPane.setVisible(!overviewActive);
        detailPane.setManaged(!overviewActive);
        updateTabStyle(tabOverview, overviewActive);
        updateTabStyle(tabDetail, !overviewActive);
    }

    private void updateTabStyle(HBox tab, boolean active) {
        tab.getStyleClass().remove("inventory-tab-active");
        if (active) {
            tab.getStyleClass().add("inventory-tab-active");
        }
    }

    private void setupFilters() {
        try {
            statusFilter.setItems(FXCollections.observableArrayList(inventoryReportService.loadStatuses()));
            categoryFilter.setItems(FXCollections.observableArrayList(inventoryReportService.loadCategories()));
        } catch (DatabaseException e) {
            statusFilter.setItems(FXCollections.observableArrayList(ALL_STATUS, STATUS_STABLE, STATUS_LOW, STATUS_OUT));
            categoryFilter.setItems(FXCollections.observableArrayList(ALL_CATEGORY));
        }

        statusFilter.getSelectionModel().select(ALL_STATUS);
        categoryFilter.getSelectionModel().select(ALL_CATEGORY);
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyKeywordFilter());
    }

    private void setupIcons() {
        productIcon.getChildren().setAll(IconFactory.createReportIcon("package"));
        lowStockIcon.getChildren().setAll(IconFactory.createReportIcon("alert"));
        reorderIcon.getChildren().setAll(IconFactory.createReportIcon("clipboard"));
        valueIcon.getChildren().setAll(IconFactory.createReportIcon("money"));
    }

    private void setupTable() {
        codeColumn.setCellValueFactory(data -> data.getValue().codeProperty());
        productColumn.setCellValueFactory(data -> data.getValue().productProperty());
        categoryColumn.setCellValueFactory(data -> data.getValue().categoryProperty());
        stockColumn.setCellValueFactory(data -> data.getValue().stockProperty());
        warningColumn.setCellValueFactory(data -> data.getValue().warningProperty());
        ratioColumn.setCellValueFactory(data -> data.getValue().ratioProperty());
        differenceColumn.setCellValueFactory(data -> data.getValue().differenceProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());
        suggestionColumn.setCellValueFactory(data -> data.getValue().suggestionProperty());

        codeColumn.setCellFactory(column -> new BlueTextCell());
        ratioColumn.setCellFactory(column -> new RatioCell());
        differenceColumn.setCellFactory(column -> new DifferenceCell());
        statusColumn.setCellFactory(column -> new StatusCell());
        suggestionColumn.setCellFactory(column -> new SuggestionCell());

        filteredRows = new FilteredList<>(rows, row -> true);
        inventoryDetailTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        inventoryDetailTable.setItems(filteredRows);
    }

    private void loadReport() {
        try {
            var data = inventoryReportService.loadReport(
                    LocalDate.now().minusDays(30),
                    LocalDate.now(),
                    SessionManager.getCurrentBranchId(),
                    categoryFilter.getValue(),
                    statusFilter.getValue()
            );

            trackedValueLabel.setText(String.valueOf(data.summary().tracked()));
            lowStockValueLabel.setText(String.valueOf(data.summary().lowStock()));
            outOfStockValueLabel.setText(String.valueOf(data.summary().outOfStock()));
            inventoryValueLabel.setText(formatMoneyCompact(data.summary().value()));

            rows.setAll(data.items().stream().map(InventoryRow::from).toList());
            applyKeywordFilter();
            renderAlertList(rows);
            bindChart(data.categories());
        } catch (DatabaseException e) {
            rows.clear();
            applyKeywordFilter();
            inventoryAlertList.getChildren().clear();
            inventoryBarChart.getData().clear();
            trackedValueLabel.setText("0");
            lowStockValueLabel.setText("0");
            outOfStockValueLabel.setText("0");
            inventoryValueLabel.setText("0");
            AlertUtils.showError(e.getMessage());
        }
    }

    private void applyKeywordFilter() {
        String keyword = safe(searchField.getText()).toLowerCase();
        filteredRows.setPredicate(row -> keyword.isBlank()
                || row.getCode().toLowerCase().contains(keyword)
                || row.getProduct().toLowerCase().contains(keyword)
                || row.getCategory().toLowerCase().contains(keyword));
    }

    private void bindChart(List<InventoryCategory> categories) {
        XYChart.Series<String, Number> currentSeries = new XYChart.Series<>();
        currentSeries.setName("Giá trị tồn");
        categories.forEach(category ->
                currentSeries.getData().add(new XYChart.Data<>(category.category(), category.value())));
        inventoryBarChart.getData().setAll(currentSeries);
        Platform.runLater(() -> currentSeries.getData().forEach(data -> {
            if (data.getNode() != null) {
                data.getNode().setStyle("-fx-bar-fill: #16a34a;");
            }
        }));
    }

    private void renderAlertList(List<InventoryRow> currentRows) {
        List<Node> alerts = currentRows.stream()
                .filter(row -> !STATUS_STABLE.equals(row.getStatus()))
                .map(row -> (Node) createAlertRow(
                        row.getProduct(),
                        row.getStatus(),
                        "Còn " + row.getStock() + " / cảnh báo " + row.getWarning()
                ))
                .toList();

        if (alerts.isEmpty()) {
            Label empty = new Label("Không có nguyên liệu cần cảnh báo.");
            empty.getStyleClass().add("section-subtitle");
            inventoryAlertList.getChildren().setAll(empty);
            return;
        }

        inventoryAlertList.getChildren().setAll(alerts);
    }

    private VBox createAlertRow(String product, String status, String detail) {
        Label productLabel = new Label(product);
        productLabel.getStyleClass().add("report-list-label");

        Label badge = new Label(status);
        badge.getStyleClass().addAll("badge", statusStyle(status));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(productLabel, spacer, badge);
        header.setAlignment(Pos.CENTER_LEFT);

        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add("section-subtitle");

        VBox row = new VBox(6, header, detailLabel);
        row.getStyleClass().add("alert-item");
        row.setPadding(new Insets(0, 0, 12, 0));
        return row;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String formatMoneyCompact(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        if (safeValue.compareTo(BigDecimal.ZERO) == 0) {
            return "0";
        }
        BigDecimal million = safeValue.divide(BigDecimal.valueOf(1_000_000), 1, RoundingMode.HALF_UP);
        return million.stripTrailingZeros().toPlainString() + "M";
    }

    private static String statusStyle(String value) {
        return switch (value) {
            case STATUS_LOW -> "status-warning";
            case STATUS_OUT -> "status-danger";
            default -> "status-success";
        };
    }

    private static String suggestionStyle(String value) {
        return switch (value) {
            case "Nhập thêm" -> "status-warning";
            case "Nhập khẩn" -> "status-danger";
            default -> "badge-neutral";
        };
    }

    private static class BlueTextCell extends TableCell<InventoryRow, String> {
        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            getStyleClass().remove("cell-code");
            if (empty || value == null) {
                setText(null);
                return;
            }
            setText(value);
            getStyleClass().add("cell-code");
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    private static class RatioCell extends TableCell<InventoryRow, String> {
        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(value);
            label.getStyleClass().addAll("badge", "badge-neutral");
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private static class DifferenceCell extends TableCell<InventoryRow, String> {
        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(value);
            label.getStyleClass().addAll("badge", value.startsWith("-") ? "status-danger" : "status-success");
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private static class StatusCell extends TableCell<InventoryRow, String> {
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);
            if (empty || status == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(status);
            label.getStyleClass().addAll("badge", statusStyle(status));
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    private static class SuggestionCell extends TableCell<InventoryRow, String> {
        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            if (empty || value == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            Label label = new Label(value);
            label.getStyleClass().addAll("badge", suggestionStyle(value));
            setGraphic(label);
            setText(null);
            setAlignment(Pos.CENTER);
        }
    }

    public static class InventoryRow {
        private final SimpleStringProperty code;
        private final SimpleStringProperty product;
        private final SimpleStringProperty category;
        private final SimpleStringProperty stock;
        private final SimpleStringProperty warning;
        private final SimpleStringProperty ratio;
        private final SimpleStringProperty difference;
        private final SimpleStringProperty status;
        private final SimpleStringProperty suggestion;

        public InventoryRow(String code, String product, String category, String stock, String warning,
                            String ratio, String difference, String status, String suggestion) {
            this.code = new SimpleStringProperty(code);
            this.product = new SimpleStringProperty(product);
            this.category = new SimpleStringProperty(category);
            this.stock = new SimpleStringProperty(stock);
            this.warning = new SimpleStringProperty(warning);
            this.ratio = new SimpleStringProperty(ratio);
            this.difference = new SimpleStringProperty(difference);
            this.status = new SimpleStringProperty(status);
            this.suggestion = new SimpleStringProperty(suggestion);
        }

        public static InventoryRow from(InventoryItem item) {
            BigDecimal current = item.currentQuantity() == null ? BigDecimal.ZERO : item.currentQuantity();
            BigDecimal min = item.minQuantity() == null ? BigDecimal.ZERO : item.minQuantity();
            BigDecimal diff = current.subtract(min);
            String ratio = min.compareTo(BigDecimal.ZERO) <= 0
                    ? "N/A"
                    : current.multiply(BigDecimal.valueOf(100)).divide(min, 0, RoundingMode.HALF_UP) + "%";
            String suggestion = switch (item.status()) {
                case STATUS_OUT -> "Nhập khẩn";
                case STATUS_LOW -> "Nhập thêm";
                default -> "Theo dõi";
            };
            return new InventoryRow(
                    item.productId(),
                    item.productName(),
                    item.categoryName(),
                    current.stripTrailingZeros().toPlainString(),
                    min.stripTrailingZeros().toPlainString(),
                    ratio,
                    diff.stripTrailingZeros().toPlainString(),
                    item.status(),
                    suggestion
            );
        }

        public SimpleStringProperty codeProperty() { return code; }
        public SimpleStringProperty productProperty() { return product; }
        public SimpleStringProperty categoryProperty() { return category; }
        public SimpleStringProperty stockProperty() { return stock; }
        public SimpleStringProperty warningProperty() { return warning; }
        public SimpleStringProperty ratioProperty() { return ratio; }
        public SimpleStringProperty differenceProperty() { return difference; }
        public SimpleStringProperty statusProperty() { return status; }
        public SimpleStringProperty suggestionProperty() { return suggestion; }
        public String getCode() { return code.get(); }
        public String getProduct() { return product.get(); }
        public String getCategory() { return category.get(); }
        public String getStock() { return stock.get(); }
        public String getWarning() { return warning.get(); }
        public String getStatus() { return status.get(); }
    }
}
