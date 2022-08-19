package com.nourish1709.postgres.connectionpool;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.postgresql.PGConnection;
import org.postgresql.ds.PGSimpleDataSource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PoolingDataSource extends PGSimpleDataSource {

    private final Queue<Connection> connectionPool;

    public PoolingDataSource(String url) {
        this(url, "postgres", "postgres");
    }

    @SneakyThrows
    public PoolingDataSource(String url, String user, String password) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(url);
        dataSource.setUser(user);
        dataSource.setPassword(password);

        connectionPool = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < 10; i++) {
            final Connection connection = dataSource.getConnection();

            final NonCloseableConnectionHandler handler = new NonCloseableConnectionHandler(connection);
            Connection connectionProxy = (Connection) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                    new Class[]{Connection.class, PGConnection.class},
                    handler);
            connectionPool.add(connectionProxy);
        }
    }

    @RequiredArgsConstructor
    private class NonCloseableConnectionHandler implements InvocationHandler {

        private final Connection connection;

        @Override
        public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
            if (method.getName().equals("close")) {
                releaseConnection((Connection) o);
                return null;
            }
            return method.invoke(this.connection, objects);
        }

        private void releaseConnection(Connection connection) {
            connectionPool.add(connection);
        }
    }

    @Override
    @SneakyThrows
    public Connection getConnection() {
        return connectionPool.poll();
    }
}
