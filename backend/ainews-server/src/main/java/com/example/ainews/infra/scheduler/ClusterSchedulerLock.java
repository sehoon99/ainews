package com.example.ainews.infra.scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ClusterSchedulerLock {

    private static final Logger log = LoggerFactory.getLogger(ClusterSchedulerLock.class);

    private final DataSource dataSource;

    public ClusterSchedulerLock(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void runWithLock(String lockName, Runnable action) {
        try (Connection connection = dataSource.getConnection()) {
            if (!acquire(connection, lockName)) {
                log.info("Skipping scheduled job because another task owns lock {}", lockName);
                return;
            }

            try {
                action.run();
            } finally {
                release(connection, lockName);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to manage scheduler lock " + lockName, e);
        }
    }

    private boolean acquire(Connection connection, String lockName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, 0)")) {
            statement.setString(1, lockName);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getInt(1) == 1;
            }
        }
    }

    private void release(Connection connection, String lockName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, lockName);
            statement.executeQuery();
        }
    }
}
