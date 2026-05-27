package com.phungloccoffee.gui.controller.product;

import com.phungloccoffee.gui.service.ProductCatalogService;
import com.phungloccoffee.model.product.ProductCatalogModels.RecipeDisplayRow;
import com.phungloccoffee.util.AlertUtils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class ProductRecipeController {
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TableView<RecipeRow> recipeTable;
    @FXML private TableColumn<RecipeRow, String> productCodeColumn;
    @FXML private TableColumn<RecipeRow, String> productNameColumn;
    @FXML private TableColumn<RecipeRow, String> materialColumn;
    @FXML private TableColumn<RecipeRow, String> quantityColumn;
    @FXML private TableColumn<RecipeRow, String> unitColumn;
    @FXML private TableColumn<RecipeRow, String> statusColumn;

    private final ProductCatalogService productCatalogService = new ProductCatalogService();

    @FXML
    private void initialize() {
        categoryComboBox.getItems().setAll("Tất cả nhóm");
        try {
            categoryComboBox.getItems().addAll(productCatalogService.loadPosCategories());
        } catch (Exception e) {
            AlertUtils.showError("Không thể tải danh mục sản phẩm từ cơ sở dữ liệu.");
            e.printStackTrace();
        }
        statusComboBox.getItems().setAll("Tất cả trạng thái", "Đang áp dụng", "Cần rà soát");
        categoryComboBox.getSelectionModel().selectFirst();
        statusComboBox.getSelectionModel().selectFirst();

        productCodeColumn.setCellValueFactory(new PropertyValueFactory<>("productCode"));
        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        materialColumn.setCellValueFactory(new PropertyValueFactory<>("material"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        unitColumn.setCellValueFactory(new PropertyValueFactory<>("unit"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new StatusCell<>());
        recipeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        loadRecipes();
    }

    private void loadRecipes() {
        try {
            recipeTable.setItems(FXCollections.observableArrayList(productCatalogService.loadRecipeDisplayRows().stream()
                    .map(this::toRecipeRow)
                    .toList()));
        } catch (Exception e) {
            recipeTable.setItems(FXCollections.observableArrayList());
            AlertUtils.showError("Không thể tải định mức sản phẩm từ cơ sở dữ liệu.");
            e.printStackTrace();
        }
    }

    private RecipeRow toRecipeRow(RecipeDisplayRow row) {
        return new RecipeRow(row.productCode(), row.productName(), row.material(), row.quantity(), row.unit(), row.status());
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
            badge.getStyleClass().addAll("status-badge", "Đang áp dụng".equals(status) ? "status-success" : "status-warning");
            setGraphic(badge);
            setText(null);
            setAlignment(Pos.CENTER_LEFT);
        }
    }

    public static class RecipeRow {
        private final String productCode;
        private final String productName;
        private final String material;
        private final String quantity;
        private final String unit;
        private final String status;

        public RecipeRow(String productCode, String productName, String material, String quantity, String unit, String status) {
            this.productCode = productCode;
            this.productName = productName;
            this.material = material;
            this.quantity = quantity;
            this.unit = unit;
            this.status = status;
        }

        public String getProductCode() {
            return productCode;
        }

        public String getProductName() {
            return productName;
        }

        public String getMaterial() {
            return material;
        }

        public String getQuantity() {
            return quantity;
        }

        public String getUnit() {
            return unit;
        }

        public String getStatus() {
            return status;
        }
    }
}
