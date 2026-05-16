package ru.yourteam.lab.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.ProtocolService;
import ru.yourteam.lab.service.SampleService;
import ru.yourteam.lab.storage.FileStorage;

import java.io.File;

public class MainWindow {

    private final BorderPane root;
    private final SampleTab sampleTab;
    private final MeasurementTab measurementTab;
    private final ProtocolTab protocolTab;
    private final SampleService sampleService;
    private final MeasurementService measurementService;
    private final ProtocolService protocolService;
    private final FileStorage fileStorage = new FileStorage();

    public MainWindow(SampleService sampleService,
                      MeasurementService measurementService,
                      ProtocolService protocolService) {
        this.sampleService = sampleService;
        this.measurementService = measurementService;
        this.protocolService = protocolService;

        sampleTab = new SampleTab(sampleService, measurementService);
        measurementTab = new MeasurementTab(measurementService, sampleService);
        protocolTab = new ProtocolTab(protocolService, measurementService, sampleService);

        Tab tabSamples = new Tab("Образцы", sampleTab.getRoot());
        tabSamples.setClosable(false);

        Tab tabMeasurements = new Tab("Измерения", measurementTab.getRoot());
        tabMeasurements.setClosable(false);

        Tab tabProtocols = new Tab("Протоколы", protocolTab.getRoot());
        tabProtocols.setClosable(false);

        TabPane tabPane = new TabPane(tabSamples, tabMeasurements, tabProtocols);

        // Кнопки сохранения и загрузки
        Button btnSave = new Button("💾 Сохранить");
        Button btnLoad = new Button("📂 Загрузить");

        btnSave.setOnAction(e -> handleSave());
        btnLoad.setOnAction(e -> handleLoad());

        HBox topBar = new HBox(8, btnSave, btnLoad);
        topBar.setPadding(new Insets(6, 8, 6, 8));

        root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(tabPane);
    }

    private void handleSave() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить данные");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON файлы", "*.json"));
        fileChooser.setInitialFileName("lab_data.json");

        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (file != null) {
            try {
                fileStorage.save(
                        file.getAbsolutePath(),
                        sampleService.getStorage(),
                        measurementService.getStorage(),
                        protocolService.getStorage()
                );
                showInfo("Сохранено", "Данные сохранены в:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                showError("Ошибка сохранения", ex.getMessage());
            }
        }
    }

    private void handleLoad() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Загрузить данные");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON файлы", "*.json"));

        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            try {
                var data = fileStorage.load(file.getAbsolutePath());

                // Загружаем образцы
                var samples = (java.util.List<ru.yourteam.lab.domain.Sample>) data.get("samples");
                sampleService.getStorage().clear();
                long maxSampleId = 1;
                for (var s : samples) {
                    sampleService.getStorage().put(s.getId(), s);
                    if (s.getId() >= maxSampleId) maxSampleId = s.getId() + 1;
                }
                sampleService.setNextId(maxSampleId);

                // Загружаем измерения
                var measurements = (java.util.List<ru.yourteam.lab.domain.Measurement>) data.get("measurements");
                measurementService.getStorage().clear();
                long maxMeasId = 1;
                for (var m : measurements) {
                    measurementService.getStorage().put(m.getId(), m);
                    if (m.getId() >= maxMeasId) maxMeasId = m.getId() + 1;
                }
                measurementService.setNextId(maxMeasId);

                // Загружаем протоколы
                var protocols = (java.util.List<ru.yourteam.lab.domain.Protocol>) data.get("protocols");
                protocolService.getStorage().clear();
                long maxProtId = 1;
                for (var p : protocols) {
                    protocolService.getStorage().put(p.getId(), p);
                    if (p.getId() >= maxProtId) maxProtId = p.getId() + 1;
                }
                protocolService.setNextId(maxProtId);

                // Обновляем все таблицы
                sampleTab.refresh();
                measurementTab.refresh();
                protocolTab.refresh();

                showInfo("Загружено", "Данные загружены из:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                showError("Ошибка загрузки", ex.getMessage());
            }
        }
    }

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

    public Parent getRoot() {
        return root;
    }
}