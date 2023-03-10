package com.cloudcoin2.wallet.Utils;

import java.net.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class ConnectionPool<T> {
    private final int maxConnections;
    private final ConcurrentLinkedQueue<T> connectionQueue;
    private final Semaphore availableConnections;
    private final Supplier<T> connectionSupplier;

    public ConnectionPool(int maxConnections, Supplier<T> connectionSupplier) {
        this.maxConnections = maxConnections;
        this.connectionSupplier = connectionSupplier;
        this.connectionQueue = new ConcurrentLinkedQueue<>();
        this.availableConnections = new Semaphore(maxConnections, true);

        for (int i = 0; i < maxConnections; i++) {
            connectionQueue.add(connectionSupplier.get());
        }
    }

    public T getConnection() throws InterruptedException {
        availableConnections.acquire();
        return connectionQueue.poll();
    }

    public void releaseConnection(T connection) {
        connectionQueue.offer(connection);
        availableConnections.release();
    }

    public void closeAllConnections() {
        for (T connection : connectionQueue) {
            try {
                if (connection instanceof DatagramSocket) {
                    DatagramSocket socket = (DatagramSocket) connection;
                    socket.close();
                } else {
                    // Handle other types of connections here
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        connectionQueue.clear();
        availableConnections.drainPermits();
    }
}
