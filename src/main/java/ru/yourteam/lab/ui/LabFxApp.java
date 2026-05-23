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
 * Цикл работы:
 *  1. Показываем LoginWindow.
 *  2. После успешного входа открываем главное окно.
 *  3. При нажатии "Выйти" — закрываем главное окно и снова показываем LoginWindow.
 *  4. Если LoginWindow закрыт без входа — завершаем приложение.
 */
public class LabFxApp extends Application {

    private AuthService authService;
    private UserStorage userStorage;
    private SampleService sampleService;
    private MeasurementService measurementService;
    private ProtocolService protocolService;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        this.userStorage = new UserStorage();
        this.authService = new AuthService();

        // Загружаем пользователей при старте приложения (один раз)
        try {
            authService.loadUsers(userStorage.load());
        } catch (IOException e) {
            System.out.println("Предупреждение: не удалось загрузить пользователей: " + e.getMessage());
        }

        // Создаём сервисы один раз — данные живут всё время работы приложения
        // даже при смене пользователя данные не сбрасываются
        this.sampleService      = new SampleService(authService);
        this.measurementService = new MeasurementService(authService);
        this.protocolService    = new ProtocolService(authService);

        // Запускаем цикл логин → главное окно
        showLoginAndThenMain();
    }

    /**
     * Показывает окно логина.
     * Если вошли — открывает главное окно.
     * Если закрыли без входа — завершает приложение.
     */
    private void showLoginAndThenMain() {
        LoginWindow loginWindow = new LoginWindow(authService, userStorage);
        boolean loggedIn = loginWindow.showAndWait();

        if (!loggedIn) {
            Platform.exit();
            return;
        }

        showMainWindow();
    }

    /**
     * Открывает главное окно.
     * Передаёт колбэк onLogout — при выходе снова показывает окно логина.
     */
    private void showMainWindow() {
        MainWindow mainWindow = new MainWindow(
                sampleService, measurementService, protocolService,
                authService, userStorage,
                this::onLogout   // колбэк для кнопки "Выйти"
        );

        Scene scene = new Scene(mainWindow.getRoot(), 1000, 650);
        primaryStage.setTitle("Лабораторная система — " + authService.getCurrentUsername());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Вызывается когда пользователь нажимает "Выйти" в главном окне.
     * Сбрасывает текущего пользователя и снова показывает окно логина.
     */
    private void onLogout() {
        authService.logout();
        primaryStage.hide();
        showLoginAndThenMain();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
