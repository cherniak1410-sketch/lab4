package ru.yourteam.lab.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.db.DbConfig;
import ru.yourteam.lab.db.DbConnectionFactory;
import ru.yourteam.lab.repository.*;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.ProtocolService;
import ru.yourteam.lab.service.SampleService;

public class LabFxApp extends Application {

    private AuthService authService;
    private SampleService sampleService;
    private MeasurementService measurementService;
    private ProtocolService protocolService;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // 1. Инициализация подключения к БД
        DbConfig dbConfig = new DbConfig();
        DbConnectionFactory connectionFactory = new DbConnectionFactory(dbConfig);

        // 2. Создание таблиц (если не существуют)
        SchemaInitializer schemaInitializer = new SchemaInitializer(connectionFactory);
        try {
            schemaInitializer.initialize();
            System.out.println("✅ База данных инициализирована");
        } catch (RuntimeException e) {
            showErrorAndExit("Ошибка подключения к БД", e.getMessage());
            return;
        }

        // 3. Создание репозиториев
        UserRepository userRepository = new UserRepository(connectionFactory);
        SampleRepository sampleRepository = new SampleRepository(connectionFactory);
        MeasurementRepository measurementRepository = new MeasurementRepository(connectionFactory);
        ProtocolRepository protocolRepository = new ProtocolRepository(connectionFactory);

        // 4. Создание AuthService с репозиторием
        this.authService = new AuthService(userRepository);

        // 5. Загрузка пользователей из БД в память AuthService
        authService.loadUsers(userRepository.findAll());
        System.out.println("👥 Загружено пользователей: " + authService.getUsers().size());

        // 6. Создание сервисов с репозиториями
        this.sampleService = new SampleService(authService, sampleRepository);
        this.measurementService = new MeasurementService(authService, measurementRepository);
        this.protocolService = new ProtocolService(authService, protocolRepository);

        // 7. Загрузка данных из БД в память сервисов
        sampleService.loadFromDatabase();
        measurementService.loadFromDatabase();
        protocolService.loadFromDatabase();

        System.out.println("📊 Загружено образцов: " + sampleService.getAll().size());
        System.out.println("📊 Загружено измерений: " + measurementService.getStorage().size());
        System.out.println("📊 Загружено протоколов: " + protocolService.getAll().size());

        // 8. Показываем окно входа
        showLoginAndThenMain();
    }

    private void showErrorAndExit(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR,
                message,
                javafx.scene.control.ButtonType.OK
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
        Platform.exit();
    }

    private void showLoginAndThenMain() {
        LoginWindow loginWindow = new LoginWindow(authService);
        boolean loggedIn = loginWindow.showAndWait();

        if (!loggedIn) {
            Platform.exit();
            return;
        }

        showMainWindow();
    }

    private void showMainWindow() {
        MainWindow mainWindow = new MainWindow(
                sampleService,
                measurementService,
                protocolService,
                authService,
                this::onLogout
        );

        Scene scene = new Scene(mainWindow.getRoot(), 1000, 650);
        primaryStage.setTitle("Лабораторная система — " + authService.getCurrentUsername());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void onLogout() {
        authService.logout();
        primaryStage.hide();
        showLoginAndThenMain();
    }

    public static void main(String[] args) {
        launch(args);
    }
}