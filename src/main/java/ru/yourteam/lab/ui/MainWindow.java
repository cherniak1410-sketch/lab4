package ru.yourteam.lab.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.Measurement;
import ru.yourteam.lab.domain.Protocol;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.ProtocolService;
import ru.yourteam.lab.service.SampleService;

import java.io.File;
import java.util.List;
import java.util.Map;

public class MainWindow {

    private final BorderPane root;
    private final SampleTab sampleTab;
    private final MeasurementTab measurementTab;
    private final ProtocolTab protocolTab;
    private final SampleService sampleService;
    private final MeasurementService measurementService;
    private final ProtocolService protocolService;
    private final AuthService authService;

    /**
     * @param onLogout колбэк, вызывается при нажатии кнопки "Выйти"
     */
    public MainWindow(SampleService sampleService,
                      MeasurementService measurementService,
                      ProtocolService protocolService,
                      AuthService authService,
                      Runnable onLogout) {
        this.sampleService      = sampleService;
        this.measurementService = measurementService;
        this.protocolService    = protocolService;
        this.authService        = authService;

        sampleTab      = new SampleTab(sampleService, measurementService, authService);
        measurementTab = new MeasurementTab(measurementService, sampleService, authService);
        protocolTab    = new ProtocolTab(protocolService, measurementService, sampleService, authService);

        Tab tabSamples      = new Tab("Образцы",   sampleTab.getRoot());
        Tab tabMeasurements = new Tab("Измерения", measurementTab.getRoot());
        Tab tabProtocols    = new Tab("Протоколы", protocolTab.getRoot());

        tabSamples.setClosable(false);
        tabMeasurements.setClosable(false);
        tabProtocols.setClosable(false);

        TabPane tabPane = new TabPane(tabSamples, tabMeasurements, tabProtocols);

        // Верхняя панель
        Button btnSave   = new Button(" Сохранить");
        Button btnLoad   = new Button(" Загрузить");
        Button btnLogout = new Button(" Выйти");
        Button btnDeleteAccount = new Button("Удалить аккаунт");
        btnDeleteAccount.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
        btnDeleteAccount.setOnAction(e -> {
            Alert confirm = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Удалить аккаунт без возможности восстановления?",
                    ButtonType.YES,
                    ButtonType.NO
            );

            confirm.setHeaderText("Все данные будут удалены");

            confirm.showAndWait().ifPresent(result -> {
                if (result == ButtonType.YES) {
                    String username = authService.getCurrentUsername();

                    // Удаляем все образцы пользователя
                    for (Sample sample : sampleService.getAll()) {
                        if (sample.getOwnerUsername().equals(username)) {
                            sampleService.delete(sample.getId());
                        }
                    }

                    // Удаляем все измерения пользователя
                    for (Measurement measurement :
                            measurementService.getStorage().values()) {

                        if (measurement.getOwnerUsername().equals(username)) {
                            measurement.setDeleted(true);
                        }
                    }

                    authService.deleteCurrentUser();
                    Alert ok = new Alert(
                            Alert.AlertType.INFORMATION,
                            "Аккаунт удалён"
                    );

                    ok.showAndWait();

                    onLogout.run();
                }
            });
        });

        btnLogout.setStyle("-fx-text-fill: #c0392b;");

        Label userLabel = new Label("👤 " + authService.getCurrentUsername());
        userLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnSave.setOnAction(e -> {
            // TODO: сохранение в JSON (опционально, можно убрать)
            showInfo("Информация", "Сохранение в JSON отключено. Данные хранятся в PostgreSQL.");
        });

        btnLoad.setOnAction(e -> {
            showInfo("Информация", "Загрузка из JSON отключена. Данные загружаются из PostgreSQL при старте.");
        });

        // При нажатии "Выйти" спрашиваем подтверждение, затем вызываем колбэк
        btnLogout.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Выйти из аккаунта «" + authService.getCurrentUsername() + "»?",
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setTitle("Подтверждение выхода");
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.OK) {
                    onLogout.run();
                }
            });
        });

        HBox topBar = new HBox(
                8,
                btnSave,
                btnLoad,
                spacer,
                userLabel,
                btnDeleteAccount,
                btnLogout
        );
        topBar.setPadding(new Insets(6, 10, 6, 8));
        topBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc; -fx-border-width: 0 0 1 0;");

        root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabPane);
    }

    // Вспомогательные диалоги

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public Parent getRoot() { return root; }
}