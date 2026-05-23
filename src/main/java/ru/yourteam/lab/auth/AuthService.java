package ru.yourteam.lab.auth;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Сервис управления пользователями и авторизацией.
 *
 * Правила владения объектами:
 *  - SYSTEM-объекты: может редактировать/удалять любой авторизованный пользователь.
 *  - Объекты с конкретным владельцем: только сам владелец.
 *
 * Текущий пользователь сессии хранится в currentUser (null = не авторизован).
 */
public class AuthService {

    private final Map<String, User> users = new LinkedHashMap<>();
    private User currentUser = null;

    // -------------------------------------------------------------------------
    // Регистрация / Вход / Выход
    // -------------------------------------------------------------------------

    /**
     * Регистрирует нового пользователя.
     * @throws IllegalArgumentException если логин занят или данные невалидны
     */
    public User register(String login, String password) {
        validateLogin(login);
        validatePassword(password);

        if (users.containsKey(login)) {
            throw new IllegalArgumentException("пользователь с логином «" + login + "» уже существует");
        }

        String hash = PasswordHasher.hash(password);
        User user = new User(login, hash);
        users.put(login, user);
        return user;
    }

    /**
     * Авторизует пользователя; устанавливает currentUser при успехе.
     * @throws IllegalArgumentException при неверном логине или пароле
     */
    public User login(String login, String password) {
        User user = users.get(login);
        if (user == null || !PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("неверный логин или пароль");
        }
        currentUser = user;
        return user;
    }

    /** Выход из системы. */
    public void logout() {
        currentUser = null;
    }

    // -------------------------------------------------------------------------
    // Текущий пользователь
    // -------------------------------------------------------------------------

    public User getCurrentUser()      { return currentUser; }
    public boolean isLoggedIn()       { return currentUser != null; }

    /** Логин текущего пользователя или "SYSTEM", если никто не вошёл. */
    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getLogin() : "SYSTEM";
    }

    // -------------------------------------------------------------------------
    // Работа с хранилищем
    // -------------------------------------------------------------------------

    /** Возвращает всех пользователей (для сохранения в файл). */
    public Map<String, User> getUsers() { return users; }

    /** Загружает пользователей из файлового хранилища. Вызывается при старте. */
    public void loadUsers(Map<String, User> loaded) {
        users.clear();
        users.putAll(loaded);
    }

    // -------------------------------------------------------------------------
    // Проверки прав
    // -------------------------------------------------------------------------

    /**
     * Требует авторизации. Бросает исключение если пользователь не вошёл.
     */
    public void requireLogin() {
        if (!isLoggedIn()) {
            throw new IllegalStateException("требуется авторизация. Используйте команду login");
        }
    }

    /**
     * Проверяет права на изменение/удаление объекта.
     *
     * Логика:
     *  - не авторизован → ошибка
     *  - владелец "SYSTEM" → разрешено любому авторизованному пользователю
     *  - владелец совпадает с текущим → разрешено
     *  - иначе → ошибка
     *
     * @param ownerUsername  владелец объекта (поле ownerUsername)
     * @param objectLabel    описание объекта для сообщения об ошибке
     */
    public void requireOwnership(String ownerUsername, String objectLabel) {
        requireLogin();
        // SYSTEM-объекты доступны любому авторизованному пользователю
        if ("SYSTEM".equals(ownerUsername)) {
            return;
        }
        // Личные объекты — только владельцу
        if(!currentUser.getLogin().equals(ownerUsername)) {
            throw new IllegalArgumentException(
                    "нет прав на изменение объекта «" + objectLabel + "» (владелец: " + ownerUsername + ")");
        }
    }

    // -------------------------------------------------------------------------
    // Валидация
    // -------------------------------------------------------------------------

    private void validateLogin(String login) {
        if (login == null || login.trim().isEmpty()) {
            throw new IllegalArgumentException("логин не может быть пустым");
        }
        if (login.length() > 64) {
            throw new IllegalArgumentException("логин слишком длинный (макс. 64)");
        }
        if (!login.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("логин может содержать только латинские буквы, цифры и _");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("пароль не может быть пустым");
        }
        if (password.length() < 4) {
            throw new IllegalArgumentException("пароль слишком короткий (минимум 4 символа)");
        }
    }
}