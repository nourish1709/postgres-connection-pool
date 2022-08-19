package com.nourish1709.postgres.connectionpool;

import lombok.SneakyThrows;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

public class Demo {

    @SneakyThrows
    public static void main(String[] args) {
        final DataSource dataSource = initPooledDataSource();
//        final DataSource dataSource = initDataSource();

        var total = 0.0;
        var start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            try (var connection = dataSource.getConnection()) {
                connection.setAutoCommit(false);
                try (var statement = connection.createStatement()) {
                    var rs = statement.executeQuery("select random() from lessons");
                    rs.next();
                    total += rs.getDouble(1);
                }
                connection.rollback();
            }
        }
        System.out.println((System.nanoTime() - start) / 1000_000 + " ms");
        System.out.println(total);
    }

    private static PoolingDataSource initPooledDataSource() {
        return new PoolingDataSource("jdbc:postgresql://localhost:5432/deadlock");
    }

    private static PGSimpleDataSource initDataSource() {
        final PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL("jdbc:postgresql://localhost:5432/deadlock");
        dataSource.setUser("postgres");
        dataSource.setPassword("postgres");

        return dataSource;
    }
}
