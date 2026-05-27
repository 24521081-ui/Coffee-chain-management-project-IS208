package com.phungloccoffee.gui.controller.inventory;

import com.phungloccoffee.bus.InventoryBUS;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.util.AlertUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.util.List;

public class InventoryListController {
    private static final String CATEGORY_ALL = "Tất cả nhóm";
    private static final String STATUS_ALL = "Tất cả trạng thái";
    private static final String STATUS_STABLE = "Ổn định";
    private static final String STATUS_LOW = "Tồn thấp";
    private static final String STATUS_OUT = "Hết hàng";

    @FXML private TextField keywordField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private Label trackedCountLabel;
    @FXML private Label lowStockCountLabel;
    @FXML private Label outOfStockCountLabel;
    @FXML private TableView<InventoryRow> inventoryTable;
    @FXML private TableColumn<InventoryRow, String> codeColumn;
    @FXML private TableColumn<InventoryRow, String> nameColumn;
    @FXML private TableColumn<InventoryRow, String> categoryColumn;
    @FXML private TableColumn<InventoryRow, String> stockColumn;
    @FXML private TableColumn<InventoryRow, String> warningColumn;
    @FXML private TableColumn<InventoryRow, String> statusColumn;
    @FXML private TableColumn<InventoryRow, Void> actionColumn;

    private final InventoryBUS inventoryBUS = new InventoryBUS();
    private final ObservableList<InventoryRow> allRows = FXCollections.observableArrayList();
    private final ObservableList<InventoryRow> filteredRows = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        categoryComboBox.getItems().setAll(CATEGORY_ALL);
        statusComboBox.getItems().setAll(STATUS_ALL, STATUS_STABLE, STATUS_LOW, STATUS_OUT);
        categoryComboBox.getSelectionModel().selectFirst();
        statusComboBox.getSelectionModel().selectFirst();

        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stock"));
        warningColumn.setCellValueFactory(new PropertyValueFactory<>("warning"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        actionColumn.setCellFactory(column -> new ActionCell());
        inventoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        inventoryTable.setItems(filteredRows);

        keywordField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        categoryComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        statusComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());

        loadInventory();
    }

    private void loadInventory() {
        try {
            List<InventoryItem> items = inventoryBUS.loadInventoryItemsForCurrentBranch();
            allRows.setAll(items.stream().map(InventoryRow::from).toList());
            applyFilter();
            updateStats();
        } catch (DatabaseException | PermissionException | com.phungloccoffee.exception.ValidationException e) {
            AlertUtils.showError(e.getMessage());
            allRows.clear();
            filteredRows.clear();
            updateStats();
        }
    }

    private void applyFilter() {
        String keyword = keywordField.getText() == null ? "" : keywordField.getText().trim().toLowerCase();
        String selectedCategory = categoryComboBox.getValue();
        String selectedStatus = statusComboBox.getValue();

        filteredRows.setAll(allRows.stream().filter(row -> {
            boolean matchesKeyword = keyword.isBlank()
                    || row.getCode().toLowerCase().contains(keyword)
                    || row.getName().toLowerCase().contains(keyword);
            boolean matchesCategory = selectedCategory == null
                    || CATEGORY_ALL.equals(selectedCategory)
                    || row.getCategory().equals(selectedCategory);
            boolean matchesStatus = selectedStatus == null
                    || STATUS_ALL.equals(selectedStatus)
                    || row.getStatus().equals(selectedStatus);
            return matchesKeyword && matchesCategory && matchesStatus;
        }).toList());
    }

    private void updateStats() {
        trackedCountLabel.setText(String.valueOf(allRows.size()));
        lowStockCountLabel.setText(String.valueOf(allRows.stream().filter(row -> STATUS_LOW.equals(row.getStatus())).count()));
        outOfStockCountLabel.setText(String.valueOf(allRows.stream().filter(row -> STATUS_OUT.equals(row.getStatus())).count()));
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
                case STATUS_STABLE -> "status-success";
                case STATUS_LOW -> "status-warning";
                default -> "status-danger";
            });
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    private static class ActionCell extends TableCell<InventoryRow, Void> {
        private final Button button = new Button("Cập nhật");

        ActionCell() {
            button.getStyleClass().addAll("action-button", "action-edit-button");
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : button);
        }
    }

    public static class InventoryRow {
        private final SimpleStringProperty code;
        private final SimpleStringProperty name;
        private final SimpleStringProperty category;
        private final SimpleStringProperty stock;
        private final SimpleStringProperty warning;
        private final SimpleStringProperty status;

        public InventoryRow(String code, String name, String category, String stock, String warning, String status) {
            this.code = new SimpleStringProperty(code);
            this.name = new SimpleStringProperty(name);
            this.category = new SimpleStringProperty(category);
            this.stock = new SimpleStringProperty(stock);
            this.warning = new SimpleStringProperty(warning);
            this.status = new SimpleStringProperty(status);
        }

        public static InventoryRow from(InventoryItem item) {
            BigDecimal stockQty = item.getQuantityOnHand() == null ? BigDecimal.ZERO : item.getQuantityOnHand();
            BigDecimal warningQty = item.getReorderLevel() == null ? BigDecimal.ZERO : item.getReorderLevel();
            String status = stockQty.compareTo(BigDecimal.ZERO) == 0
                    ? STATUS_OUT
                    : stockQty.compareTo(warningQty) <= 0
                    ? STATUS_LOW
                    : STATUS_STABLE;
            return new InventoryRow(
                    item.getItemCode(),
                    item.getItemName(),
                    "Nguyên liệu",
                    formatQuantity(stockQty, item.getUnit()),
                    formatQuantity(warningQty, item.getUnit()),
                    status
            );
        }

        public String getCode() { return code.get(); }
        public String getName() { return name.get(); }
        public String getCategory() { return category.get(); }
        public String getStock() { return stock.get(); }
        public String getWarning() { return warning.get(); }
        public String getStatus() { return status.get(); }

        private static String formatQuantity(BigDecimal value, String unit) {
            BigDecimal clean = value == null ? BigDecimal.ZERO : value.stripTrailingZeros();
            return clean.toPlainString() + " " + (unit == null ? "" : unit);
        }
    }
}
