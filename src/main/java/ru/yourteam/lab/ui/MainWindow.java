package ru.yourteam.lab.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import ru.yourteam.lab.domain.Measurement;
import ru.yourteam.lab.domain.Protocol;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.ProtocolService;
import ru.yourteam.lab.service.SampleService;
import ru.yourteam.lab.storage.FileStorage;

import java.io.File;
import java.util.HashMap;
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

        Button btnSave = new Button(" Сохранить");
        Button btnLoad = new Button("Загрузить");

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

                // Загружаем образцы с проверкой конфликтов ID
                var samples = (List<Sample>) data.get("samples");
                Map<Long, Sample> existingSamples = sampleService.getStorage();
                long maxSampleId = getCurrentMaxSampleId(existingSamples.keySet());

                for (Sample s : samples) {
                    if (existingSamples.containsKey(s.getId())) {
                        // Конфликт — присваиваем новый ID
                        long newId = maxSampleId++;
                        s.setId(newId);
                        existingSamples.put(newId, s);
                    } else {
                        existingSamples.put(s.getId(), s);
                        if (s.getId() >= maxSampleId) {
                            maxSampleId = s.getId() + 1;
                        }
                    }
                }
                sampleService.setNextId(maxSampleId);

                // Загружаем измерения с проверкой конфликтов ID
                var measurements = (List<Measurement>) data.get("measurements");
                Map<Long, Measurement> existingMeasurements = measurementService.getStorage();
                long maxMeasId = getCurrentMaxMeasurementId(existingMeasurements.keySet());

                for (Measurement m : measurements) {
                    if (existingMeasurements.containsKey(m.getId())) {
                        long newId = maxMeasId++;
                        m.setId(newId);
                        existingMeasurements.put(newId, m);
                    } else {
                        existingMeasurements.put(m.getId(), m);
                        if (m.getId() >= maxMeasId) {
                            maxMeasId = m.getId() + 1;
                        }
                    }
                }
                measurementService.setNextId(maxMeasId);

                // Загружаем протоколы с проверкой конфликтов ID
                var protocols = (List<Protocol>) data.get("protocols");
                Map<Long, Protocol> existingProtocols = protocolService.getStorage();
                long maxProtId = getCurrentMaxProtocolId(existingProtocols.keySet());

                for (Protocol p : protocols) {
                    if (existingProtocols.containsKey(p.getId())) {
                        long newId = maxProtId++;
                        p.setId(newId);
                        existingProtocols.put(newId, p);
                    } else {
                        existingProtocols.put(p.getId(), p);
                        if (p.getId() >= maxProtId) {
                            maxProtId = p.getId() + 1;
                        }
                    }
                }
                protocolService.setNextId(maxProtId);

                // Обновляем все таблицы
                sampleTab.refresh();
                measurementTab.refresh();
                protocolTab.refresh();

                showInfo("Загружено", "Данные добавлены из:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                showError("Ошибка загрузки", ex.getMessage());
            }
        }
    }

    private long getCurrentMaxSampleId(Iterable<Long> ids) {
        long max = 1;
        for (Long id : ids) {
            if (id >= max) max = id + 1;
        }
        return max;
    }

    private long getCurrentMaxMeasurementId(Iterable<Long> ids) {
        long max = 1;
        for (Long id : ids) {
            if (id >= max) max = id + 1;
        }
        return max;
    }

    private long getCurrentMaxProtocolId(Iterable<Long> ids) {
        long max = 1;
        for (Long id : ids) {
            if (id >= max) max = id + 1;
        }
        return max;
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