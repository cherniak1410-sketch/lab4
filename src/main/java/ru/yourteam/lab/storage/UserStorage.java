package ru.yourteam.lab.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ru.yourteam.lab.auth.User;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Хранит пользователей в отдельном JSON-файле.
 * Формат: {"users": [{"login": "...", "passwordHash": "..."}]}
 *
 * Пароли хранятся только в виде SHA-256 хешей — никогда в открытом виде.
 */
public class UserStorage {

    private static final String DEFAULT_PATH = "users.json";

    private final String filePath;
    private final ObjectMapper objectMapper;

    public UserStorage() {
        this(DEFAULT_PATH);
    }

    public UserStorage(String filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Сохраняет всех пользователей в файл.
     */
    public void save(Map<String, User> users) throws IOException {
        List<Map<String, String>> list = new ArrayList<>();
        for (User user : users.values()) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("login", user.getLogin());
            entry.put("passwordHash", user.getPasswordHash());
            list.add(entry);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", list);

        objectMapper.writeValue(new File(filePath), data);
    }

    /**
     * Загружает пользователей из файла.
     * Если файл не существует — возвращает пустую карту (первый запуск).
     */
    public Map<String, User> load() throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> data = objectMapper.readValue(file, Map.class);
        List<Map<String, String>> list = (List<Map<String, String>>) data.get("users");

        Map<String, User> result = new LinkedHashMap<>();
        if (list != null) {
            for (Map<String, String> entry : list) {
                String login = entry.get("login");
                String hash = entry.get("passwordHash");
                if (login == null || hash == null) {
                    throw new IOException("Повреждённый файл пользователей: отсутствует login или passwordHash");
                }
                result.put(login, new User(login, hash));
            }
        }
        return result;
    }

    public String getFilePath() {
        return filePath;
    }
}

