package ru.yourteam.lab.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.Measurement;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.SampleService;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Вкладка "Измерения".
 *
 * Этап 7: добавлена панель расширенной фильтрации.
 */
public class MeasurementTab {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final MeasurementService measurementService;
    private final SampleService sampleService;
    private final AuthService authService;

    private final BorderPane root;
    private final TableView<Measurement> tableView;

    // Поля фильтра
    private ComboBox<String>          filterSampleCombo;
    private ComboBox<String>          filterParamCombo;
    private TextField                 filterOwnerField;
    private TextField                 filterMinValueField;
    private TextField                 filterMaxValueField;
    private DatePicker                filterDateFrom;
    private DatePicker                filterDateTo;

    // Кэш: отображаемое имя -> id образца
    private final java.util.Map<String, Long> sampleNameToId = new java.util.LinkedHashMap<>();

    public MeasurementTab(MeasurementService measurementService,
                          SampleService sampleService,
                          AuthService authService) {
        this.measurementService = measurementService;
        this.sampleService      = sampleService;
        this.authService        = authService;

        tableView = buildTable();
        HBox toolbar = buildToolbar();
        TitledPane filterPane = buildFilterPane();

        VBox top = new VBox(4, filterPane, toolbar);

        root = new BorderPane();
        root.setTop(top);
        root.setCenter(tableView);
        BorderPane.setMargin(top, new Insets(4));
    }

    // ──────────────────────────────────────────────
    // Панель фильтров
    // ──────────────────────────────────────────────

    private TitledPane buildFilterPane() {
        // Образец
        filterSampleCombo = new ComboBox<>();
        filterSampleCombo.setPrefWidth(160);
        refreshSampleCombo();

        // Параметр
        filterParamCombo = new ComboBox<>();
        filterParamCombo.getItems().add("(все)");
        for (MeasurementParam p : MeasurementParam.values()) {
            filterParamCombo.getItems().add(p.name());
        }
        filterParamCombo.getSelectionModel().selectFirst();
        filterParamCombo.setPrefWidth(120);

        // Владелец
        filterOwnerField = new TextField();
        filterOwnerField.setPromptText("Владелец...");
        filterOwnerField.setPrefWidth(110);

        // Диапазон значений
        filterMinValueField = new TextField();
        filterMinValueField.setPromptText("от");
        filterMinValueField.setPrefWidth(70);

        filterMaxValueField = new TextField();
        filterMaxValueField.setPromptText("до");
        filterMaxValueField.setPrefWidth(70);

        // Дата
        filterDateFrom = new DatePicker();
        filterDateFrom.setPromptText("от");
        filterDateFrom.setPrefWidth(130);

        filterDateTo = new DatePicker();
        filterDateTo.setPromptText("до");
        filterDateTo.setPrefWidth(130);

        Button btnApply = new Button("Применить");
        Button btnReset = new Button("Сбросить");

        btnApply.setOnAction(e -> applyFilter());
        btnReset.setOnAction(e -> {
            filterSampleCombo.getSelectionModel().selectFirst();
            filterParamCombo.getSelectionModel().selectFirst();
            filterOwnerField.clear();
            filterMinValueField.clear();
            filterMaxValueField.clear();
            filterDateFrom.setValue(null);
            filterDateTo.setValue(null);
            refresh();
        });

        // Два ряда для удобства
        HBox row1 = new HBox(8,
                new Label("Образец:"), filterSampleCombo,
                new Label("Параметр:"), filterParamCombo,
                new Label("Владелец:"), filterOwnerField
        );
        HBox row2 = new HBox(8,
                new Label("Значение от:"), filterMinValueField,
                new Label("до:"), filterMaxValueField,
                new Label("Дата с:"), filterDateFrom,
                new Label("по:"), filterDateTo,
                btnApply, btnReset
        );
        row1.setPadding(new Insets(4, 6, 2, 6));
        row2.setPadding(new Insets(2, 6, 6, 6));

        VBox content = new VBox(4, row1, row2);
        content.setStyle("-fx-background-color: #f9f9f9;");

        TitledPane pane = new TitledPane("Фильтры", content);
        pane.setExpanded(false);
        pane.setAnimated(true);
        return pane;
    }

    private void refreshSampleCombo() {
        sampleNameToId.clear();
        filterSampleCombo.getItems().clear();
        filterSampleCombo.getItems().add("(все образцы)");
        for (Sample s : sampleService.getAll()) {
            String label = "#" + s.getId() + " " + s.getName();
            sampleNameToId.put(label, s.getId());
            filterSampleCombo.getItems().add(label);
        }
        filterSampleCombo.getSelectionModel().selectFirst();
    }

    private void applyFilter() {
        // Образец
        String sampleLabel = filterSampleCombo.getValue();
        long sampleId = 0;
        if (sampleLabel != null && !sampleLabel.equals("(все образцы)")) {
            sampleId = sampleNameToId.getOrDefault(sampleLabel, 0L);
        }

        // Параметр
        String paramStr = filterParamCombo.getValue();
        MeasurementParam param = (paramStr == null || paramStr.equals("(все)"))
                ? null : MeasurementParam.valueOf(paramStr);

        // Владелец
        String owner = filterOwnerField.getText().trim();

        // Диапазон значений
        Double minVal = null, maxVal = null;
        try { minVal = filterMinValueField.getText().isBlank() ? null
                : Double.parseDouble(filterMinValueField.getText().trim()); }
        catch (NumberFormatException ignored) {}
        try { maxVal = filterMaxValueField.getText().isBlank() ? null
                : Double.parseDouble(filterMaxValueField.getText().trim()); }
        catch (NumberFormatException ignored) {}

        // Дата
        java.time.Instant from = filterDateFrom.getValue() != null
                ? filterDateFrom.getValue().atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        java.time.Instant to   = filterDateTo.getValue() != null
                ? filterDateTo.getValue().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        List<Measurement> filtered = measurementService.filter(sampleId, param, owner, minVal, maxVal, from, to);
        tableView.setItems(FXCollections.observableArrayList(filtered));
        tableView.refresh();
    }

    // ──────────────────────────────────────────────
    // Таблица
    // ──────────────────────────────────────────────

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

        TableColumn<Measurement, String> colOwner = new TableColumn<>("Кто добавил");
        colOwner.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOwnerUsername()));
        colOwner.setPrefWidth(100);

        table.getColumns().addAll(colId, colSampleId, colParam, colValue, colUnit, colMethod, colTime, colOwner);
        return table;
    }

    // ──────────────────────────────────────────────
    // Тулбар
    // ──────────────────────────────────────────────

    private HBox buildToolbar() {
        Button btnAdd     = new Button("Добавить");
        Button btnRefresh = new Button("Обновить");
        Button btnDelete  = new Button("Удалить");
        Button btnRestore = new Button("Восстановить");

        btnAdd.setOnAction(e -> handleAdd());
        btnRefresh.setOnAction(e -> refresh());
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

        HBox toolbar = new HBox(8, btnAdd, btnDelete, btnRestore, btnRefresh);
        toolbar.setPadding(new Insets(4));
        return toolbar;
    }

    // ──────────────────────────────────────────────
    // Обработчики
    // ──────────────────────────────────────────────

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
                measurementService.add(data.sampleId(), data.param(), data.value(), data.unit(), data.method());
                refreshSampleCombo();
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

    public Parent getRoot() { return root; }
}
