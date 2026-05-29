package ru.yourteam.lab.auth;

import ru.yourteam.lab.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.Map;

public class AuthService {

    private final Map<String, User> users = new LinkedHashMap<>();
    private User currentUser = null;
    private UserRepository userRepository;  // ← ДОБАВИТЬ

    public AuthService() {
        this.userRepository = null; // временно, будет установлен позже
    }

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String login, String password) {
        validateLogin(login);
        validatePassword(password);

        if (users.containsKey(login)) {
            throw new IllegalArgumentException("пользователь с логином «" + login + "» уже существует");
        }

        String hash = PasswordHasher.hash(password);
        User user = new User(login, hash);
        users.put(login, user);

        // ← СОХРАНЯЕМ В БД
        if (userRepository != null) {
            userRepository.save(user);
        }

        return user;
    }

    public User login(String login, String password) {
        User user = users.get(login);
        if (user == null || !PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("неверный логин или пароль");
        }
        currentUser = user;
        return user;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() { return currentUser; }
    public boolean isLoggedIn() { return currentUser != null; }

    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getLogin() : "SYSTEM";
    }

    public Map<String, User> getUsers() { return users; }

    public void loadUsers(Map<String, User> loaded) {
        users.clear();
        users.putAll(loaded);
    }

    public void requireLogin() {
        if (!isLoggedIn()) {
            throw new IllegalStateException("требуется авторизация");
        }
    }

    public void requireOwnership(String ownerUsername, String objectLabel) {
        requireLogin();
        if ("SYSTEM".equals(ownerUsername)) {
            return;
        }
        if (!currentUser.getLogin().equals(ownerUsername)) {
            throw new IllegalArgumentException(
                    "нет прав на изменение объекта «" + objectLabel + "» (владелец: " + ownerUsername + ")");
        }
    }

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

    public void deleteCurrentUser() {
        requireLogin();
        if (userRepository != null) {
            userRepository.delete(currentUser.getLogin());
        }
        users.remove(currentUser.getLogin());
        currentUser = null;
    }
}