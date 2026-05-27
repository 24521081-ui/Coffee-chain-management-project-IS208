package com.phungloccoffee.offline;

import com.phungloccoffee.model.offline.OfflineOrder;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class OfflineStorage {
    private static final OfflineStorage INSTANCE = new OfflineStorage();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final Path storageDir = Path.of(System.getProperty("user.home"), ".phungloccoffee");
    private final Path storageFile = storageDir.resolve("offline-orders.bin");

    private OfflineStorage() {
    }

    public static OfflineStorage getInstance() {
        return INSTANCE;
    }

    public synchronized void addOrder(OfflineOrder order) throws IOException {
        List<OfflineOrder> orders = loadAll();
        orders.add(order);
        saveAll(orders);
    }

    public synchronized List<OfflineOrder> loadAll() throws IOException {
        if (!Files.exists(storageFile)) {
            return new ArrayList<>();
        }
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(storageFile))) {
            Object value = input.readObject();
            if (value instanceof List<?> list) {
                List<OfflineOrder> orders = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof OfflineOrder order) {
                        orders.add(order);
                    }
                }
                return orders;
            }
            return new ArrayList<>();
        } catch (EOFException e) {
            return new ArrayList<>();
        } catch (ClassNotFoundException e) {
            throw new IOException("Khong doc duoc file giao dich offline.", e);
        }
    }

    public synchronized Optional<OfflineOrder> findByLocalOrderId(String localOrderId) throws IOException {
        return loadAll().stream()
                .filter(order -> localOrderId != null && localOrderId.equals(order.getLocalOrderId()))
                .findFirst();
    }

    public synchronized List<OfflineOrder> findPendingOrders() throws IOException {
        return loadAll().stream()
                .filter(order -> OfflineOrder.SYNC_PENDING.equals(order.getSyncStatus()))
                .sorted(Comparator.comparing(OfflineOrder::getCreatedAt))
                .toList();
    }

    public synchronized long countPendingOrders() throws IOException {
        return loadAll().stream()
                .filter(order -> OfflineOrder.SYNC_PENDING.equals(order.getSyncStatus()))
                .count();
    }

    public synchronized void updateOrder(OfflineOrder updatedOrder) throws IOException {
        List<OfflineOrder> orders = loadAll();
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i).getLocalOrderId().equals(updatedOrder.getLocalOrderId())) {
                orders.set(i, updatedOrder);
                saveAll(orders);
                return;
            }
        }
        orders.add(updatedOrder);
        saveAll(orders);
    }

    public synchronized String nextLocalOrderId(String branchId) throws IOException {
        String branchCode = branchId == null || branchId.isBlank() ? "BRANCH" : branchId.trim();
        String today = LocalDate.now().format(DATE_FORMAT);
        String prefix = branchCode + "-" + today + "-";
        int next = loadAll().stream()
                .map(OfflineOrder::getLocalOrderId)
                .filter(id -> id != null && id.startsWith(prefix))
                .map(id -> id.substring(prefix.length()))
                .mapToInt(this::parseSequence)
                .max()
                .orElse(0) + 1;
        return prefix + String.format("%06d", next);
    }

    private void saveAll(List<OfflineOrder> orders) throws IOException {
        Files.createDirectories(storageDir);
        Path tempFile = storageDir.resolve("offline-orders.tmp");
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(tempFile))) {
            output.writeObject(new ArrayList<>(orders));
        }
        Files.move(tempFile, storageFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private int parseSequence(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
