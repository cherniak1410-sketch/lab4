package ru.yourteam.lab.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Простая фабрика соединений с PostgreSQL.
 *
 * Для учебного проекта достаточно одного синглтона без пула.
 * Каждый вызов getConnection() открывает новое соединение —
 * закрывать его обязан вызывающий код (try-with-resources).
 *
 * Если нужен пул — достаточно заменить тело getConnection()
 * на HikariCP или c3p0, не меняя остальной код.
 */
public class DbConnectionFactory {

    private final DbConfig config;

    public DbConnectionFactory(DbConfig config) {
        this.config = config;
    }

    /**
     * Открывает новое соединение с БД.
     * @throws SQLException если подключение невозможно
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                config.getUrl(),
                config.getUser(),
                config.getPassword()
        );
    }

    /**
     * Проверяет доступность БД.
     * Используется при старте приложения для понятной диагностики.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }
}

