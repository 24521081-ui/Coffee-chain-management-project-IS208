package com.phungloccoffee.offline;

import com.phungloccoffee.util.DBConnection;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class NetworkMonitor {
    private static final NetworkMonitor INSTANCE = new NetworkMonitor();

    private final List<Consumer<Boolean>> listeners = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService executor;
    private volatile boolean online = true;

    private NetworkMonitor() {
    }

    public static NetworkMonitor getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (executor != null && !executor.isShutdown()) {
            return;
        }
        online = DBConnection.testConnection();
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "phungloc-network-monitor");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::checkAndNotify, 0, 10, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    public boolean isOnline() {
        return online;
    }

    public boolean checkNow() {
        boolean current = DBConnection.testConnection();
        updateStatus(current);
        return current;
    }

    public void addListener(Consumer<Boolean> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    private void checkAndNotify() {
        boolean current = DBConnection.testConnection();
        updateStatus(current);
    }

    private void updateStatus(boolean current) {
        boolean changed = current != online;
        online = current;
        if (changed) {
            for (Consumer<Boolean> listener : listeners) {
                listener.accept(online);
            }
        }
    }
}
