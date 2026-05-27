package com.phungloccoffee.bus;

import com.phungloccoffee.dao.ProductDAO;
import com.phungloccoffee.exception.DatabaseException;
import com.phungloccoffee.exception.PermissionException;
import com.phungloccoffee.exception.ValidationException;
import com.phungloccoffee.model.Product;
import com.phungloccoffee.model.ProductCategory;
import com.phungloccoffee.offline.ProductCache;
import com.phungloccoffee.util.ValidationUtils;

import java.math.BigDecimal;
import java.util.List;

public class ProductBUS extends PermissionBUS {
    private final ProductDAO productDAO = new ProductDAO();
    private final ProductCache productCache = ProductCache.getInstance();

    public List<Product> loadProducts() throws DatabaseException, PermissionException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return productDAO.findAll();
    }

    public List<Product> getProductsForPOS(String branchId) throws DatabaseException, PermissionException {
        requireRole("THU_NGAN", "QUAN_LY_CHI_NHANH", "IT_ADMIN");
        try {
            List<Product> products = productDAO.findProductsForPOS();
            productCache.save(products);
            return products;
        } catch (DatabaseException e) {
            List<Product> cachedProducts = productCache.load();
            if (!cachedProducts.isEmpty()) {
                return cachedProducts;
            }
            throw e;
        }
    }

    public List<ProductCategory> loadCategories() throws DatabaseException, PermissionException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        return productDAO.findCategories();
    }

    public void saveProduct(Product product) throws ValidationException, PermissionException, DatabaseException {
        requireRole("QUAN_LY_CHI_NHANH", "IT_ADMIN");
        ValidationUtils.requireText(product.getCode(), "Mã sản phẩm");
        ValidationUtils.requireText(product.getName(), "Tên sản phẩm");
        if (!isValidProductType(product.getLoaiSanPham())) {
            throw new ValidationException("Loại sản phẩm không hợp lệ.");
        }
        if (!isValidUnit(product.getDonViTinh())) {
            throw new ValidationException("Đơn vị tính không hợp lệ.");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Giá bán không hợp lệ.");
        }
        productDAO.save(product);
    }

    private boolean isValidProductType(String value) {
        return "THANH_PHAM".equals(value) || "NGUYEN_LIEU".equals(value) || "BAN_THANH_PHAM".equals(value);
    }

    private boolean isValidUnit(String value) {
        return "ML".equals(value) || "L".equals(value) || "MG".equals(value) || "KG".equals(value);
    }
}
