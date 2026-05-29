package ru.yourteam.lab.repository;

import ru.yourteam.lab.db.DbConnectionFactory;
import ru.yourteam.lab.domain.Measurement;
import ru.yourteam.lab.domain.MeasurementParam;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-репозиторий для измерений.
 *
 * Таблица: measurements
 *  id, sample_id, param, value, unit, method, measured_at,
 *  owner_username, created_at, updated_at, deleted
 *
 * ID генерирует БД (BIGSERIAL).
 */
public class MeasurementRepository {

    private final DbConnectionFactory factory;

    public MeasurementRepository(DbConnectionFactory factory) {
        this.factory = factory;
    }

    // -------------------------------------------------------------------------
    // Запись
    // -------------------------------------------------------------------------

    /**
     * Вставляет новое измерение в БД.
     * Устанавливает сгенерированный БД id обратно в объект.
     */
    public void insert(Measurement measurement) {
        String sql = """
                INSERT INTO measurements
                    (sample_id, param, value, unit, method, measured_at,
                     owner_username, created_at, updated_at, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = factory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, measurement.getSampleId());
            ps.setString(2, measurement.getParam().name());
            ps.setDouble(3, measurement.getValue());
            ps.setString(4, measurement.getUnit());
            ps.setString(5, measurement.getMethod());
            ps.setTimestamp(6, Timestamp.from(measurement.getMeasuredAt()));
            ps.setString(7, measurement.getOwnerUsername());
            ps.setTimestamp(8, Timestamp.from(measurement.getCreatedAt()));
            ps.setTimestamp(9, Timestamp.from(measurement.getUpdatedAt()));
            ps.setBoolean(10, measurement.isDeleted());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    measurement.setId(keys.getLong(1));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка БД при сохранении измерения: " + e.getMessage(), e);
        }
    }

    /**
     * Обновляет поле deleted (мягкое удаление/восстановление).
     */
    public void updateDeleted(long id, boolean deleted) {
        String sql = "UPDATE measurements SET deleted = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = factory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setBoolean(1, deleted);
            ps.setLong(2, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка БД при обновлении измерения: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Чтение
    // -------------------------------------------------------------------------

    /**
     * Загружает все измерения из БД (включая удалённые).
     * Вызывается при старте для заполнения in-memory коллекции.
     */
    public List<Measurement> findAll() {
        String sql = "SELECT * FROM measurements ORDER BY id";
        List<Measurement> result = new ArrayList<>();

        try (Connection conn = factory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка БД при загрузке измерений: " + e.getMessage(), e);
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // Вспомогательные методы
    // -------------------------------------------------------------------------

    private Measurement mapRow(ResultSet rs) throws SQLException {
        Measurement m = new Measurement(
                rs.getLong("sample_id"),
                MeasurementParam.valueOf(rs.getString("param")),
                rs.getDouble("value"),
                rs.getString("unit"),
                rs.getString("method")
        );
        m.setId(rs.getLong("id"));
        m.setOwnerUsername(rs.getString("owner_username"));
        m.setDeleted(rs.getBoolean("deleted"));

        Timestamp measuredAt = rs.getTimestamp("measured_at");
        if (measuredAt != null) {
            m.setMeasuredAt(measuredAt.toInstant());
        }
        return m;
    }
}