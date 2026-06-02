package ru.yourteam.lab.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import ru.yourteam.lab.auth.AuthService;
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
 *
 * Этап 5: кнопка "Добавить" активна только для авторизованных пользователей.
 * Добавлена колонка "Кто добавил".
 */
public class MeasurementTab {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final MeasurementService measurementService;
    private final SampleService sampleService;
    private final AuthService authService;

    private final BorderPane root;
    private final TableView<Measurement> tableView;
    public MeasurementTab(MeasurementService measurementService,
                          SampleService sampleService,
                          AuthService authService) {
        this.measurementService = measurementService;
        this.sampleService      = sampleService;
        this.authService        = authService;

        tableView = buildTable();
        HBox toolbar = buildToolbar();

        root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(tableView);
        BorderPane.setMargin(toolbar, new Insets(8));
    }

    private TableView<Measurement> buildTable() {
        TableView<Measurement> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Нет измерений. Нажмите «Добавить»."));

        TableColumn<Measurement, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colId.setPrefWidth(45);

        TableColumn<Measurement, String> colSampleId = new TableColumn<>("Образец ID");
        colSampleId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getSampleId())));
        colSampleId.setPrefWidth(80);

        TableColumn<Measurement, String> colParam = new TableColumn<>("Параметр");
        colParam.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getParam().name()));

        TableColumn<Measurement, String> colValue = new TableColumn<>("Значение");
        colValue.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getValue())));

        TableColumn<Measurement, String> colUnit = new TableColumn<>("Единицы");
        colUnit.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUnit()));

        TableColumn<Measurement, String> colMethod = new TableColumn<>("Метод");
        colMethod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMethod()));

        TableColumn<Measurement, String> colTime = new TableColumn<>("Время");
        colTime.setCellValueFactory(c -> new SimpleStringProperty(FORMATTER.format(c.getValue().getMeasuredAt())));

        // Колонка владельца (этап 5)
        TableColumn<Measurement, String> colOwner = new TableColumn<>("Кто добавил");
        colOwner.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOwnerUsername()));
        colOwner.setPrefWidth(100);

        table.getColumns().addAll(colId, colSampleId, colParam, colValue, colUnit, colMethod, colTime, colOwner);
        return table;
    }

    private HBox buildToolbar() {
        Button btnAdd     = new Button("Добавить");
        Button btnRefresh = new Button("Обновить");
        Button btnStats = new Button("Статистика");
        Button btnDelete = new Button("Удалить");
        Button btnRestore = new Button("Восстановить");

        btnAdd.setOnAction(e -> handleAdd());
        btnRefresh.setOnAction(e -> refresh());
        btnStats.setOnAction(e -> showStats());
        btnDelete.setOnAction(e -> handleDelete());
        btnRestore.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Восстановление");
            dialog.setHeaderText("Введите ID измерения");

            dialog.showAndWait().ifPresent(value -> {
                try {
                    long id = Long.parseLong(value);
                    measurementService.restore(id);
                    refresh();
                } catch (Exception ex) {
                    showError("Ошибка", ex.getMessage());
                }
            });
        });

        HBox toolbar = new HBox(
                8,
                btnAdd,
                btnDelete,
                btnRestore,
                btnStats,
                btnRefresh
        );
        toolbar.setPadding(new Insets(4));
        return toolbar;
    }

    private void handleAdd() {
        if (!authService.isLoggedIn()) {
            showError("Требуется авторизация", "Войдите в систему, чтобы добавлять измерения.");
            return;
        }
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

                measurementService.add(data.sampleId(), data.param(),
                        data.value(), data.unit(), data.method());
                refresh();
            } catch (Exception ex) {
                showError("Ошибка при добавлении", ex.getMessage());
            }
        });
    }

    private void handleDelete() {
        Measurement selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Ничего не выбрано", "Выберите измерение в таблице.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить измерение #" + selected.getId()
                        + " (" + selected.getParam().name() + " = " + selected.getValue() + ")?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Подтверждение удаления");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    measurementService.delete(selected.getId());
                    refresh();
                } catch (IllegalArgumentException ex) {
                    showError("Ошибка при удалении", ex.getMessage());
                }
            }
        });
    }


    public void refresh() {
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
    private void showStats() {

        Measurement selected =
                tableView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError(
                    "Ничего не выбрано",
                    "Выберите измерение."
            );
            return;
        }

        var stats = measurementService.getStats(
                selected.getSampleId(),
                selected.getParam()
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("Статистика");
        alert.setHeaderText(
                "Параметр " + selected.getParam()
        );

        alert.setContentText(
                "Количество: " + stats.get("count") + "\n" +
                        "Минимум: " + stats.get("min") + "\n" +
                        "Максимум: " + stats.get("max") + "\n" +
                        "Среднее: " + stats.get("avg") + "\n" +
                        "Сумма: " + stats.get("sum")
        );

        alert.showAndWait();
    }
    public Parent getRoot() { return root; }
}
