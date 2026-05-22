package ru.yourteam.lab.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.ProtocolService;
import ru.yourteam.lab.service.SampleService;
import ru.yourteam.lab.storage.UserStorage;

import java.io.IOException;

/**
 * Точка входа JavaFX-приложения.
 *
 * Этап 5:
 *  1. Загружаем пользователей из users.json.
 *  2. Показываем окно авторизации (LoginWindow).
 *  3. Если пользователь закрыл окно не войдя — завершаем приложение.
 *  4. Если вошёл — открываем главное окно (MainWindow).
 *
 * Запуск: mvn javafx:run
 */
public class LabFxApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        AuthService authService = new AuthService();
        UserStorage userStorage = new UserStorage();

        // Загружаем пользователей при старте
        try {
            authService.loadUsers(userStorage.load());
        } catch (IOException e) {
            System.out.println("Предупреждение: не удалось загрузить пользователей: " + e.getMessage());
        }

        // --- Окно авторизации ---
        LoginWindow loginWindow = new LoginWindow(authService, userStorage);
        boolean loggedIn = loginWindow.showAndWait();

        if (!loggedIn) {
            // Пользователь закрыл окно не войдя — завершаем
            Platform.exit();
            return;
        }

        // --- Создаём сервисы с AuthService ---
        SampleService sampleService          = new SampleService(authService);
        MeasurementService measurementService = new MeasurementService(authService);
        ProtocolService protocolService       = new ProtocolService(authService);

        // --- Главное окно ---
        MainWindow mainWindow = new MainWindow(
                sampleService, measurementService, protocolService, authService, userStorage
        );

        Scene scene = new Scene(mainWindow.getRoot(), 1000, 650);
        primaryStage.setTitle("Лабораторная система — " + authService.getCurrentUsername());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
