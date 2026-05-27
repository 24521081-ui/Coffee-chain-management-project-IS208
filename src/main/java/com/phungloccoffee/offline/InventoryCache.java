package com.phungloccoffee.offline;

import com.phungloccoffee.dao.InventoryDAO;
import com.phungloccoffee.model.InventoryItem;
import com.phungloccoffee.model.offline.OfflineOrderDetail;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InventoryCache {
    private static final InventoryCache INSTANCE = new InventoryCache();

    private final Map<String, BigDecimal> quantities = new ConcurrentHashMap<>();
    private final Map<String, Map<String, BigDecimal>> recipes = new ConcurrentHashMap<>();
    private final Path storageDir = Path.of(System.getProperty("user.home"), ".phungloccoffee");
    private final Path cacheFile = storageDir.resolve("inventory-cache.bin");
    private String branchId;
    private String khoId;

    private InventoryCache() {
        loadFromDisk();
    }

    public static InventoryCache getInstance() {
        return INSTANCE;
    }

    public synchronized void loadFromList(String branchId, String khoId, List<InventoryItem> items) {
        this.branchId = branchId;
        this.khoId = khoId;
        quantities.clear();
        if (items == null) {
            return;
        }
        for (InventoryItem item : items) {
            quantities.put(item.getItemCode(), nullToZero(item.getQuantityOnHand()));
        }
        saveToDisk();
    }

    public synchronized void loadRecipes(Map<String, List<InventoryDAO.StockDeduction>> recipeData) {
        recipes.clear();
        if (recipeData == null) {
            return;
        }
        for (Map.Entry<String, List<InventoryDAO.StockDeduction>> entry : recipeData.entrySet()) {
            Map<String, BigDecimal> requirements = new LinkedHashMap<>();
            for (InventoryDAO.StockDeduction deduction : entry.getValue()) {
                requirements.put(deduction.getSanPhamId(), nullToZero(deduction.getQuantity()));
            }
            recipes.put(entry.getKey(), requirements);
        }
        saveToDisk();
    }

    public BigDecimal getAvailableQuantity(String productId) {
        return quantities.getOrDefault(productId, BigDecimal.ZERO);
    }

    public boolean hasEnough(String productId, BigDecimal quantity) {
        return getAvailableQuantity(productId).compareTo(nullToZero(quantity)) >= 0;
    }

    public synchronized boolean hasEnough(Map<String, BigDecimal> requiredQuantities) {
        for (Map.Entry<String, BigDecimal> entry : requiredQuantities.entrySet()) {
            if (!hasEnough(entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    public synchronized void decrease(String productId, BigDecimal quantity) {
        BigDecimal current = getAvailableQuantity(productId);
        quantities.put(productId, current.subtract(nullToZero(quantity)));
    }

    public synchronized void decrease(Map<String, BigDecimal> requiredQuantities) {
        for (Map.Entry<String, BigDecimal> entry : requiredQuantities.entrySet()) {
            decrease(entry.getKey(), entry.getValue());
        }
    }

    public synchronized void restore(Map<String, BigDecimal> quantitiesToRestore) {
        for (Map.Entry<String, BigDecimal> entry : quantitiesToRestore.entrySet()) {
            BigDecimal current = getAvailableQuantity(entry.getKey());
            quantities.put(entry.getKey(), current.add(nullToZero(entry.getValue())));
        }
    }

    public Map<String, BigDecimal> snapshot() {
        return new LinkedHashMap<>(quantities);
    }

    public Map<String, BigDecimal> calculateRequiredQuantities(List<OfflineOrderDetail> details) {
        Map<String, BigDecimal> required = new LinkedHashMap<>();
        if (details == null) {
            return required;
        }
        for (OfflineOrderDetail detail : details) {
            BigDecimal orderQuantity = nullToZero(detail.getQuantity());
            Map<String, BigDecimal> recipe = recipes.get(detail.getProductId());
            if (recipe == null || recipe.isEmpty()) {
                addRequired(required, detail.getProductId(), orderQuantity);
                continue;
            }
            for (Map.Entry<String, BigDecimal> entry : recipe.entrySet()) {
                addRequired(required, entry.getKey(), entry.getValue().multiply(orderQuantity));
            }
        }
        return required;
    }

    public List<String> findMissingOrInsufficientItems(Map<String, BigDecimal> requiredQuantities) {
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : requiredQuantities.entrySet()) {
            BigDecimal available = getAvailableQuantity(entry.getKey());
            if (available.compareTo(entry.getValue()) < 0) {
                errors.add(entry.getKey() + " can " + entry.getValue() + ", con " + available);
            }
        }
        return errors;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getKhoId() {
        return khoId;
    }

    private void addRequired(Map<String, BigDecimal> required, String productId, BigDecimal quantity) {
        required.merge(productId, nullToZero(quantity), BigDecimal::add);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private synchronized void saveToDisk() {
        try {
            Files.createDirectories(storageDir);
            Path tempFile = storageDir.resolve("inventory-cache.tmp");
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(tempFile))) {
                output.writeObject(new CacheData(branchId, khoId, new LinkedHashMap<>(quantities), new LinkedHashMap<>(recipes)));
            }
            Files.move(tempFile, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadFromDisk() {
        if (!Files.exists(cacheFile)) {
            return;
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(cacheFile))) {
            Object value = input.readObject();
            if (value instanceof CacheData cacheData) {
                branchId = cacheData.branchId;
                khoId = cacheData.khoId;
                quantities.clear();
                recipes.clear();
                quantities.putAll(cacheData.quantities);
                recipes.putAll(cacheData.recipes);
            }
        } catch (EOFException ignored) {
        } catch (IOException | ClassNotFoundException ignored) {
        }
    }

    private record CacheData(String branchId, String khoId,
                             Map<String, BigDecimal> quantities,
                             Map<String, Map<String, BigDecimal>> recipes) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
