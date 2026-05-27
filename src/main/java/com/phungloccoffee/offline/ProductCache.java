package com.phungloccoffee.offline;

import com.phungloccoffee.model.Product;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class ProductCache {
    private static final ProductCache INSTANCE = new ProductCache();

    private final Path storageDir = Path.of(System.getProperty("user.home"), ".phungloccoffee");
    private final Path cacheFile = storageDir.resolve("pos-products.bin");

    private ProductCache() {
    }

    public static ProductCache getInstance() {
        return INSTANCE;
    }

    public synchronized void save(List<Product> products) {
        try {
            Files.createDirectories(storageDir);
            Path tempFile = storageDir.resolve("pos-products.tmp");
            try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(tempFile))) {
                output.writeObject(new ArrayList<>(products == null ? List.of() : products));
            }
            Files.move(tempFile, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
        }
    }

    public synchronized List<Product> load() {
        if (!Files.exists(cacheFile)) {
            return List.of();
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(cacheFile))) {
            Object value = input.readObject();
            if (value instanceof List<?> list) {
                List<Product> products = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Product product) {
                        products.add(product);
                    }
                }
                return products;
            }
        } catch (EOFException ignored) {
        } catch (IOException | ClassNotFoundException ignored) {
        }
        return List.of();
    }
}
