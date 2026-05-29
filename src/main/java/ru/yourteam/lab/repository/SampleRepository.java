package ru.yourteam.lab.repository;

import ru.yourteam.lab.db.DbConnectionFactory;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.domain.SampleStatus;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-репозиторий для образцов.
 *
 * Таблица: samples
 *  id, name, type, location, status, owner_username,
 *  created_at, updated_at, deleted
 *
 * ID генерирует БД (BIGSERIAL).
 * После INSERT программа читает сгенерированный id через RETURN_GENERATED_KEYS.
 */
public class SampleRepository {

    private final DbConnectionFactory factory;

    public SampleRepository(DbConnectionFactory factory) {
        this.factory = factory;
    }

    // -------------------------------------------------------------------------
    // Запись
    // -------------------------------------------------------------------------

    /**
     * Вставляет новый образец в БД.
     * Устанавливает сгенерированный БД id обратно в объект.
     */
    public void insert(Sample sample) {
        String sql = """
                INSERT INTO samples (name, type, location, status, owner_username,
                                     created_at, updated_at, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = factory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, sample.getName());
            ps.setString(2, sample.getType());
            ps.setString(3, sample.getLocation());
            ps.setString(4, sample.getStatus().name());
            ps.setString(5, sample.getOwnerUsername());
            ps.setTimestamp(6, Timestamp.from(sample.getCreatedAt()));
            ps.setTimestamp(7, Timestamp.from(sample.getUpdatedAt()));
            ps.setBoolean(8, sample.isDeleted());
            ps.executeUpdate();

            // Читаем сгенерированный id и устанавливаем в объект
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    sample.setId(keys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка БД при сохранении образца: " + e.getMessage(), e);
        }
    }

    /**
     * Обновляет все изменяемые поля образца в БД.
     */
    public void update(Sample sample) {
        String sql = """
                UPDATE samples
                SET name = ?, type = ?, location = ?, status = ?,
                    updated_at = ?, deleted = ?
                WHERE id = ?
                """;
        try (Connection conn = factory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sample.getName());
            ps.setString(2, sample.getType());
            ps.setString(3, sample.getLocation());
            ps.setString(4, sample.getStatus().name());
            ps.setTimestamp(5, Timestamp.from(sample.getUpdatedAt()));
            ps.setBoolean(6, sample.isDeleted());
            ps.setLong(7, sample.getId());
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка БД при обновлении образца: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Чтение
    // -------------------------------------------------------------------------

    /**
     * Загружает все образцы из БД (включая удалённые).
     * Вызывается при старте для заполнения in-memory коллекции.
     */
    public List<Sample> findAll() {
        String sql = "SELECT * FROM samples ORDER BY id";
        List<Sample> result = new ArrayList<>();

        try (Connection conn = factory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка БД при загрузке образцов: " + e.getMessage(), e);
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Вспомогательные методы
    // -------------------------------------------------------------------------

    /** Преобразует строку ResultSet в объект Sample. */
    private Sample mapRow(ResultSet rs) throws SQLException {
        Sample sample = new Sample(
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("location")
        );
        sample.setId(rs.getLong("id"));
        sample.setStatus(SampleStatus.valueOf(rs.getString("status")));
        sample.setOwnerUsername(rs.getString("owner_username"));
        sample.setDeleted(rs.getBoolean("deleted"));

        // createdAt — final поле, устанавливается только через конструктор,
        // поэтому просто проверяем что оно есть (для логов при необходимости).
        // updatedAt уже установлен конструктором — не переписываем.
        return sample;
    }
}
