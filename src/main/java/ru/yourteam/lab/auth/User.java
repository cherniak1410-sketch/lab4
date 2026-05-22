package ru.yourteam.lab.auth;

/**
 * Пользователь системы.
 * Пароль хранится в виде SHA-256 хеша — никогда в открытом виде.
 */
public class User {

    private String login;
    private String passwordHash; // SHA-256 hex

    /** Конструктор для восстановления из файла (хеш уже готов). */
    public User(String login, String passwordHash) {
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public String getLogin() { return login; }
    public String getPasswordHash() { return passwordHash; }

    public void setLogin(String login) { this.login = login; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}

