package com.phungloccoffee.gui.service;

import com.phungloccoffee.bus.ProductBUS;
import com.phungloccoffee.dao.ProductDAO;
import com.phungloccoffee.dao.ProductRecipeDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.gui.model.ProductOption;
import com.phungloccoffee.gui.model.ProductOption.ProductStatus;
import com.phungloccoffee.model.Product;
import com.phungloccoffee.model.ProductCategory;
import com.phungloccoffee.model.product.ProductCatalogModels.Ingredient;
import com.phungloccoffee.model.product.ProductCatalogModels.ProductMetadata;
import com.phungloccoffee.model.product.ProductCatalogModels.ProductRecipe;
import com.phungloccoffee.model.product.ProductCatalogModels.RecipeDisplayRow;

import java.time.LocalDateTime;
import java.util.List;

import static com.phungloccoffee.model.product.ProductCatalogModels.DEFAULT_PRODUCT_TYPE;

public class ProductCatalogService {
    private final ProductBUS productBUS = new ProductBUS();
    private final ProductDAO productDAO = new ProductDAO();
    private final ProductRecipeDAO productRecipeDAO = new ProductRecipeDAO();

    public List<Product> loadProducts() throws DatabaseException, PermissionException {
        return productBUS.loadProducts();
    }

    public List<ProductCategory> loadCategories() throws DatabaseException, PermissionException {
        return productBUS.loadCategories();
    }

    public void saveProduct(Product product) throws ValidationException, PermissionException, DatabaseException {
        productBUS.saveProduct(product);
    }

    public List<Ingredient> loadIngredients() throws DatabaseException {
        return productRecipeDAO.findIngredients();
    }

    public List<ProductRecipe> loadRecipeRows(String productCode) throws DatabaseException {
        return productRecipeDAO.findRecipeRows(productCode);
    }

    public void replaceRecipeRows(String productCode, List<ProductRecipe> rows) throws DatabaseException {
        productRecipeDAO.replaceRecipeRows(productCode, rows);
    }

    public boolean hasRecipe(String productCode) throws DatabaseException {
        return productRecipeDAO.hasRecipe(productCode);
    }

    public ProductMetadata metadataFor(Product product) {
        LocalDateTime publishedAt = product == null ? null : product.getCreatedAt();
        LocalDateTime stoppedAt = product != null && product.getTrangThai() == 0 ? product.getUpdatedAt() : null;
        return new ProductMetadata("", publishedAt, stoppedAt);
    }

    public List<RecipeDisplayRow> loadRecipeDisplayRows() throws DatabaseException {
        return productRecipeDAO.findAllRecipeDisplayRows();
    }

    public List<ProductOption> loadPosProductOptions() throws DatabaseException {
        List<ProductCategory> categories = productDAO.findCategories();
        return productDAO.findAllActive().stream()
                .filter(product -> DEFAULT_PRODUCT_TYPE.equals(product.getLoaiSanPham()))
                .map(product -> new ProductOption(
                        product.getCode(),
                        product.getName(),
                        categoryName(product.getDanhMucId(), categories),
                        product.getPrice(),
                        product.getTrangThai() == 1 ? ProductStatus.AVAILABLE : ProductStatus.PAUSED
                ))
                .toList();
    }

    public List<String> loadPosCategories() throws DatabaseException {
        return productDAO.findCategories().stream()
                .map(ProductCategory::getTenDanhMuc)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }

    public String categoryName(String categoryId, List<ProductCategory> categories) {
        return categories.stream()
                .filter(category -> safe(categoryId).equals(category.getDanhMucId()))
                .map(ProductCategory::getTenDanhMuc)
                .findFirst()
                .orElse(safe(categoryId).isEmpty() ? "Chưa phân loại" : categoryId);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
