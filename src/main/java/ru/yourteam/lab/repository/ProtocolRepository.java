package ru.yourteam.lab.repository;

import ru.yourteam.lab.db.DbConnectionFactory;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.domain.Protocol;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JDBC-репозиторий для протоколов.
 *
 * Протокол хранится в двух таблицах:
 *   protocols      — id, name, owner_username, created_at, updated_at
 *   protocol_params — protocol_id, param  (1:N)
 *
 * Вставка протокола выполняется в транзакции:
 * сначала protocols, потом protocol_params.
 */
public class ProtocolRepository {

    private final DbConnectionFactory factory;

    public ProtocolRepository(DbConnectionFactory factory) {
        this.factory = factory;
    }

    // -------------------------------------------------------------------------
    // Запись
    // -------------------------------------------------------------------------

    /**
     * Вставляет протокол и его параметры в БД в одной транзакции.
     * Устанавливает сгенерированный БД id обратно в объект.
     */
    public void insert(Protocol protocol) {
        String sqlProto = """
                INSERT INTO protocols (name, owner_username, created_at, updated_at)
                VALUES (?, ?, ?, ?)
                """;
        String sqlParam = "INSERT INTO protocol_params (protocol_id, param) VALUES (?, ?)";

        try (Connection conn = factory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long generatedId;

                // 1. Вставляем протокол
                try (PreparedStatement ps = conn.prepareStatement(sqlProto, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, protocol.getName());
                    ps.setString(2, protocol.getOwnerUsername());
                    ps.setTimestamp(3, Timestamp.from(protocol.getCreatedAt()));
                    ps.setTimestamp(4, Timestamp.from(protocol.getUpdatedAt()));
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Не получен id протокола");
                        generatedId = keys.getLong(1);
                    }
                }

                protocol.setId(generatedId);

                // 2. Вставляем параметры
                try (PreparedStatement ps = conn.prepareStatement(sqlParam)) {
                    for (MeasurementParam param : protocol.getRequiredParams()) {
                        ps.setLong(1, generatedId);
                        ps.setString(2, param.name());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка БД при сохранении протокола: " + e.getMessage(), e);
        }
    }

    /**
     * Удаляет протокол из БД (CASCADE удалит и protocol_params).
     */
    public void delete(long id) {
        String sql = "DELETE FROM protocols WHERE id = ?";
        try (Connection conn = factory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка БД при удалении протокола: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Чтение
    // -------------------------------------------------------------------------

    /**
     * Загружает все протоколы вместе с их параметрами.
     * Вызывается при старте для заполнения in-memory коллекции.
     */
    public List<Protocol> findAll() {
        // Загружаем протоколы
        String sqlProto = "SELECT * FROM protocols ORDER BY id";
        List<Protocol> protocols = new ArrayList<>();

        try (Connection conn = factory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlProto);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                long id   = rs.getLong("id");
                String name  = rs.getString("name");
                String owner = rs.getString("owner_username");

                // Загружаем параметры для этого протокола
                Set<MeasurementParam> params = loadParams(conn, id);

                Protocol protocol = new Protocol(name, params);
                protocol.setId(id);
                protocol.setOwnerUsername(owner);
                protocols.add(protocol);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка БД при загрузке протоколов: " + e.getMessage(), e);
        }

        return protocols;
    }

    // -------------------------------------------------------------------------
    // Вспомогательные методы
    // -------------------------------------------------------------------------

    /** Загружает набор параметров протокола по его id. */
    private Set<MeasurementParam> loadParams(Connection conn, long protocolId) throws SQLException {
        String sql = "SELECT param FROM protocol_params WHERE protocol_id = ?";
        Set<MeasurementParam> params = new HashSet<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, protocolId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    params.add(MeasurementParam.valueOf(rs.getString("param")));
                }
            }
        }
        return params;
    }
}