package ru.yourteam.lab.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Читает параметры подключения к БД.
 *
 * Приоритет (от высшего к низшему):
 *  1. Переменные окружения: DB_URL, DB_USER, DB_PASSWORD
 *  2. Системное свойство JVM: -Ddb.config=путь/к/файлу
 *  3. Файл db.properties рядом с JAR (текущая директория)
 *  4. Файл db.properties из classpath
 */
public class DbConfig {

    private static final String DEFAULT_FILE = "db.properties";

    private final String url;
    private final String user;
    private final String password;

    public DbConfig() {
        Properties props = loadProperties();

        // Переменные окружения имеют наивысший приоритет
        this.url      = env("DB_URL",      props.getProperty("db.url",      "jdbc:postgresql://localhost:5432/labdb"));
        this.user     = env("DB_USER",     props.getProperty("db.user",     "labuser"));
        this.password = env("DB_PASSWORD", props.getProperty("db.password", "labpassword"));
    }

    public String getUrl()      { return url; }
    public String getUser()     { return user; }
    public String getPassword() { return password; }

    // -------------------------------------------------------------------------
    // Внутренние методы
    // -------------------------------------------------------------------------

    private Properties loadProperties() {
        Properties props = new Properties();

        // 1. Путь через системное свойство
        String customPath = System.getProperty("db.config");
        if (customPath != null) {
            try (InputStream in = new FileInputStream(customPath)) {
                props.load(in);
                return props;
            } catch (IOException e) {
                System.out.println("Предупреждение: не удалось прочитать " + customPath + ": " + e.getMessage());
            }
        }

        // 2. Файл в текущей директории
        try (InputStream in = new FileInputStream(DEFAULT_FILE)) {
            props.load(in);
            return props;
        } catch (IOException ignored) {
            // файл не найден — не критично
        }

        // 3. Файл из classpath
        try (InputStream in = DbConfig.class.getClassLoader().getResourceAsStream(DEFAULT_FILE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // не критично
        }

        return props;
    }

    /** Возвращает значение переменной окружения, если задана, иначе defaultValue. */
    private String env(String envName, String defaultValue) {
        String val = System.getenv(envName);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
