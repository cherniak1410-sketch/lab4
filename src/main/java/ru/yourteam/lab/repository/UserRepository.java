package ru.yourteam.lab.repository;

import ru.yourteam.lab.auth.User;
import ru.yourteam.lab.db.DbConnectionFactory;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JDBC-репозиторий для пользователей.
 *
 * Таблица: users (login VARCHAR PK, password_hash VARCHAR)
 */
public class UserRepository {

    private final DbConnectionFactory factory;

    public UserRepository(DbConnectionFactory factory) {
        this.factory = factory;
    }

    // -------------------------------------------------------------------------
    // Запись
    // -------------------------------------------------------------------------

    /**
     * Сохраняет нового пользователя в БД.
     * @throws IllegalArgumentException если логин уже занят
     */
    public void save(User user) {
        String sql = "INSERT INTO users (login, password_hash) VALUES (?, ?)";
        try (Connection conn = factory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getLogin());
            ps.setString(2, user.getPasswordHash());
            ps.executeUpdate();

        } catch (SQLException e) {
            // Нарушение уникальности PK — логин занят
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                throw new IllegalArgumentException(
                        "пользователь с логином «" + user.getLogin() + "» уже существует");
            }
            throw new RuntimeException("Ошибка БД при сохранении пользователя: " + e.getMessage(), e);
        }
    }

    /**
     * Удаляет пользователя по логину.
     */
    public void delete(String login) {
        String sql = "DELETE FROM users WHERE login = ?";
        try (Connection conn = factory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, login);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка БД при удалении пользователя: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Чтение
    // -------------------------------------------------------------------------

    /**
     * Загружает всех пользователей из БД.
     * Вызывается один раз при старте приложения.
     */
    public Map<String, User> findAll() {
        String sql = "SELECT login, password_hash FROM users";
        Map<String, User> result = new LinkedHashMap<>();

        try (Connection conn = factory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String login = rs.getString("login");
                String hash  = rs.getString("password_hash");
                result.put(login, new User(login, hash));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Ошибка БД при загрузке пользователей: " + e.getMessage(), e);
        }

        return result;
    }
}

