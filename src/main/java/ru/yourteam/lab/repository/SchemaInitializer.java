package ru.yourteam.lab.repository;

import ru.yourteam.lab.db.DbConnectionFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Инициализатор схемы БД.
 * Создаёт таблицы при первом запуске приложения.
 */
public class SchemaInitializer {

    private final DbConnectionFactory factory;

    public SchemaInitializer(DbConnectionFactory factory) {
        this.factory = factory;
    }

    /**
     * Проверяет подключение к БД и создаёт таблицы, если их нет.
     * @throws RuntimeException если подключение не удалось или ошибка SQL
     */
    public void initialize() {
        // 1. Проверяем подключение
        if (!factory.testConnection()) {
            throw new RuntimeException(
                    "Не удалось подключиться к PostgreSQL.\n" +
                            "Проверьте:\n" +
                            "  1. PostgreSQL запущен\n" +
                            "  2. База данных создана (createdb lab_db)\n" +
                            "  3. db.properties настроен правильно"
            );
        }

        // 2. Читаем schema.sql
        String schemaSql = readSchemaFile();

        // 3. Выполняем скрипт
        try (Connection conn = factory.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : schemaSql.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            System.out.println("[DB] Схема БД успешно инициализирована");

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при создании таблиц: " + e.getMessage(), e);
        }
    }

    private String readSchemaFile() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new RuntimeException("Файл schema.sql не найден в resources");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения schema.sql: " + e.getMessage(), e);
        }
    }
}
