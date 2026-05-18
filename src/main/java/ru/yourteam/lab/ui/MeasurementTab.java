package ru.yourteam.lab.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import ru.yourteam.lab.domain.Measurement;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.SampleService;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Вкладка "Измерения".
 * Показывает все измерения (или фильтрует по образцу).
 * Кнопки: Добавить, Обновить.
 */
public class MeasurementTab {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final MeasurementService measurementService;
    private final SampleService sampleService;

    private final BorderPane root;
    private final TableView<Measurement> tableView;

    public MeasurementTab(MeasurementService measurementService, SampleService sampleService) {
        this.measurementService = measurementService;
        this.sampleService = sampleService;

        tableView = buildTable();
        HBox toolbar = buildToolbar();

        root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(tableView);
        BorderPane.setMargin(toolbar, new Insets(8));
    }

    // ──────────────────────────────────────────────
    // Таблица
    // ──────────────────────────────────────────────

    private TableView<Measurement> buildTable() {
        TableView<Measurement> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Нет измерений. Нажмите «Добавить»."));

        TableColumn<Measurement, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().getId())));
        colId.setPrefWidth(50);

        TableColumn<Measurement, String> colSampleId = new TableColumn<>("Образец ID");
        colSampleId.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().getSampleId())));
        colSampleId.setPrefWidth(90);

        TableColumn<Measurement, String> colParam = new TableColumn<>("Параметр");
        colParam.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getParam().name()));

        TableColumn<Measurement, String> colValue = new TableColumn<>("Значение");
        colValue.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().getValue())));

        TableColumn<Measurement, String> colUnit = new TableColumn<>("Единицы");
        colUnit.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getUnit()));

        TableColumn<Measurement, String> colMethod = new TableColumn<>("Метод");
        colMethod.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getMethod()));

        TableColumn<Measurement, String> colTime = new TableColumn<>("Время");
        colTime.setCellValueFactory(cell ->
                new SimpleStringProperty(FORMATTER.format(cell.getValue().getMeasuredAt())));

        TableColumn<Measurement, String> colOwner = new TableColumn<>("Кто добавил");
        colOwner.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getOwnerUsername()));

        table.getColumns().addAll(colId, colSampleId, colParam, colValue, colUnit, colMethod, colTime, colOwner);
        return table;
    }

    // ──────────────────────────────────────────────
    // Панель кнопок
    // ──────────────────────────────────────────────

    private HBox buildToolbar() {
        Button btnAdd = new Button("Добавить");
        Button btnRefresh = new Button(" Обновить");

        btnAdd.setOnAction(e -> handleAdd());
        btnRefresh.setOnAction(e -> refresh());

        HBox toolbar = new HBox(8, btnAdd, new Separator(), btnRefresh);
        toolbar.setPadding(new Insets(4));
        return toolbar;
    }

    // ──────────────────────────────────────────────
    // Обработчики
    // ──────────────────────────────────────────────

    private void handleAdd() {
        List<Sample> samples = sampleService.getAll();
        if (samples.isEmpty()) {
            showError("Нет образцов", "Сначала добавьте хотя бы один образец на вкладке «Образцы».");
            return;
        }

        MeasurementDialog dialog = new MeasurementDialog(samples);
        Optional<MeasurementDialog.MeasurementFormData> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                Sample sample = sampleService.findById(data.sampleId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "образец с id=" + data.sampleId() + " не найден"));

                if (sample.getStatus().name().equals("ARCHIVED")) {
                    showError("Ошибка", "Нельзя добавлять измерения к ARCHIVED образцу.");
                    return;
                }

                measurementService.add(
                        data.sampleId(),
                        data.param(),
                        data.value(),
                        data.unit(),
                        data.method()
                );
                refresh();
            } catch (IllegalArgumentException ex) {
                showError("Ошибка при добавлении", ex.getMessage());
            }
        });
    }

    /** Перечитывает данные из сервиса и перерисовывает таблицу. */
    public void refresh() {
        // Собираем все измерения из всех образцов
        List<Sample> allSamples = sampleService.getAll();
        List<Measurement> allMeasurements = allSamples.stream()
                .flatMap(s -> measurementService.getBySampleId(s.getId()).stream())
                .toList();
        tableView.setItems(FXCollections.observableArrayList(allMeasurements));
        tableView.refresh();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public Parent getRoot() {
        return root;
    }
}

