package ru.yourteam.lab.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.ProtocolService;
import ru.yourteam.lab.service.SampleService;
import ru.yourteam.lab.ui.MainWindow;

/**
 * Точка входа JavaFX-приложения.
 * Запуск: mvn javafx:run
 */
public class LabFxApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Создаём сервисы — единственный источник данных для всего UI
        SampleService sampleService = new SampleService();
        MeasurementService measurementService = new MeasurementService();
        ProtocolService protocolService = new ProtocolService();

        MainWindow mainWindow = new MainWindow(sampleService, measurementService, protocolService);

        Scene scene = new Scene(mainWindow.getRoot(), 1000, 650);
        primaryStage.setTitle("Лабораторная система управления образцами");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

