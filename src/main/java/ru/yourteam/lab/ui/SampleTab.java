package ru.yourteam.lab.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.domain.SampleStatus;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.SampleService;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Вкладка "Образцы".
 *
 * Этап 7: добавлена панель расширенной фильтрации.
 */
public class SampleTab {

    private final SampleService sampleService;
    private final MeasurementService measurementService;
    private final AuthService authService;

    private final BorderPane root;
    private final TableView<Sample> tableView;

    // Поля фильтра
    private TextField filterNameField;
    private TextField filterTypeField;
    private TextField filterOwnerField;
    private ComboBox<String> filterStatusCombo;
    private DatePicker filterDateFrom;
    private DatePicker filterDateTo;

    public SampleTab(SampleService sampleService,
                     MeasurementService measurementService,
                     AuthService authService) {
        this.sampleService = sampleService;
        this.measurementService = measurementService;
        this.authService = authService;

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
        filterNameField  = new TextField();
        filterNameField.setPromptText("Часть названия...");
        filterNameField.setPrefWidth(140);

        filterTypeField  = new TextField();
        filterTypeField.setPromptText("Тип...");
        filterTypeField.setPrefWidth(110);

        filterOwnerField = new TextField();
        filterOwnerField.setPromptText("Владелец...");
        filterOwnerField.setPrefWidth(110);

        filterStatusCombo = new ComboBox<>();
        filterStatusCombo.getItems().addAll("(все)", "ACTIVE", "ARCHIVED");
        filterStatusCombo.getSelectionModel().selectFirst();
        filterStatusCombo.setPrefWidth(100);

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
            filterNameField.clear();
            filterTypeField.clear();
            filterOwnerField.clear();
            filterStatusCombo.getSelectionModel().selectFirst();
            filterDateFrom.setValue(null);
            filterDateTo.setValue(null);
            refresh();
        });

        HBox row = new HBox(8,
                new Label("Название:"), filterNameField,
                new Label("Тип:"),     filterTypeField,
                new Label("Владелец:"), filterOwnerField,
                new Label("Статус:"),  filterStatusCombo,
                new Label("Создан с:"), filterDateFrom,
                new Label("по:"),       filterDateTo,
                btnApply, btnReset
        );
        row.setPadding(new Insets(6));
        row.setStyle("-fx-background-color: #f9f9f9;");

        TitledPane pane = new TitledPane("Фильтры", row);
        pane.setExpanded(false);
        pane.setAnimated(true);
        return pane;
    }

    private void applyFilter() {
        String name  = filterNameField.getText().trim();
        String type  = filterTypeField.getText().trim();
        String owner = filterOwnerField.getText().trim();

        String statusStr = filterStatusCombo.getValue();
        SampleStatus status = (statusStr == null || statusStr.equals("(все)"))
                ? null : SampleStatus.valueOf(statusStr);

        java.time.Instant from = filterDateFrom.getValue() != null
                ? filterDateFrom.getValue().atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        java.time.Instant to   = filterDateTo.getValue() != null
                ? filterDateTo.getValue().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        List<Sample> filtered = sampleService.filter(status, owner, type, name, from, to);
        tableView.setItems(FXCollections.observableArrayList(filtered));
        tableView.refresh();
    }

    // ──────────────────────────────────────────────
    // Таблица
    // ──────────────────────────────────────────────

    private TableView<Sample> buildTable() {
        TableView<Sample> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Нет образцов. Нажмите «Добавить»."));

        TableColumn<Sample, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().getId())));
        colId.setPrefWidth(50);

        TableColumn<Sample, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getName()));

        TableColumn<Sample, String> colType = new TableColumn<>("Тип");
        colType.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getType()));

        TableColumn<Sample, String> colLocation = new TableColumn<>("Место");
        colLocation.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getLocation()));

        TableColumn<Sample, String> colStatus = new TableColumn<>("Статус");
        colStatus.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getStatus().name()));
        colStatus.setPrefWidth(90);

        TableColumn<Sample, String> colOwner = new TableColumn<>("Владелец");
        colOwner.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getOwnerUsername()));

        TableColumn<Sample, String> colMeasurements = new TableColumn<>("Измерений");
        colMeasurements.setCellValueFactory(cell -> {
            int count = measurementService.getBySampleId(cell.getValue().getId()).size();
            return new SimpleStringProperty(String.valueOf(count));
        });
        colMeasurements.setPrefWidth(90);

        TableColumn<Sample, String> colCovered = new TableColumn<>("Параметры");
        colCovered.setCellValueFactory(cell -> {
            var covered = measurementService.getCoveredParams(cell.getValue().getId());
            String str = covered.isEmpty() ? "—" :
                    covered.stream().map(Enum::name).sorted().reduce((a, b) -> a + ", " + b).orElse("—");
            return new SimpleStringProperty(str);
        });
        colCovered.setPrefWidth(160);

        table.getColumns().addAll(colId, colName, colType, colLocation, colStatus,
                colOwner, colMeasurements, colCovered);
        return table;
    }

    // ──────────────────────────────────────────────
    // Панель кнопок
    // ──────────────────────────────────────────────

    private HBox buildToolbar() {
        Button btnAdd     = new Button("Добавить");
        Button btnEdit    = new Button("Редактировать");
        Button btnArchive = new Button("Архивировать");
        Button btnDelete  = new Button("Удалить");
        Button btnRestore = new Button("Восстановить");
        Button btnRefresh = new Button("🔄 Обновить");

        btnAdd.setOnAction(e -> handleAdd());
        btnEdit.setOnAction(e -> handleEdit());
        btnArchive.setOnAction(e -> handleArchive());
        btnDelete.setOnAction(e -> handleDelete());
        btnRefresh.setOnAction(e -> refresh());
        btnRestore.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Восстановление образца");
            dialog.setHeaderText("Введите ID образца");
            dialog.showAndWait().ifPresent(value -> {
                try {
                    long id = Long.parseLong(value);
                    sampleService.restore(id);
                    refresh();
                } catch (Exception ex) {
                    showError("Ошибка", ex.getMessage());
                }
            });
        });

        HBox toolbar = new HBox(8,
                btnAdd, btnEdit, btnArchive, btnDelete, btnRestore,
                new Separator(), btnRefresh);
        toolbar.setPadding(new Insets(4, 4, 4, 4));
        return toolbar;
    }

    // ──────────────────────────────────────────────
    // Обработчики
    // ──────────────────────────────────────────────

    private void handleAdd() {
        SampleDialog dialog = new SampleDialog(null);
        Optional<SampleDialog.SampleFormData> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                sampleService.add(data.name(), data.type(), data.location());
                refresh();
            } catch (IllegalArgumentException ex) {
                showError("Ошибка при добавлении", ex.getMessage());
            }
        });
    }

    private void handleEdit() {
        Sample selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Ничего не выбрано", "Выберите образец в таблице.");
            return;
        }
        SampleDialog dialog = new SampleDialog(selected);
        Optional<SampleDialog.SampleFormData> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                sampleService.update(selected.getId(), "name", data.name());
                sampleService.update(selected.getId(), "type", data.type());
                sampleService.update(selected.getId(), "location", data.location());
                refresh();
            } catch (IllegalArgumentException ex) {
                showError("Ошибка при редактировании", ex.getMessage());
            }
        });
    }

    private void handleArchive() {
        Sample selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Ничего не выбрано", "Выберите образец в таблице.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Архивировать образец «" + selected.getName() + "»?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    sampleService.archive(selected.getId());
                    refresh();
                } catch (IllegalArgumentException ex) {
                    showError("Ошибка", ex.getMessage());
                }
            }
        });
    }

    private void handleDelete() {
        Sample selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Ничего не выбрано", "Выберите образец в таблице.");
            return;
        }

        String currentUser = authService.getCurrentUsername();
        String owner = selected.getOwnerUsername();
        if (!currentUser.equals(owner) && !"SYSTEM".equals(owner)) {
            showError("Нет прав", "Вы не можете удалить этот образец.\nВладелец: " + owner);
            return;
        }

        int measurementCount = measurementService.getBySampleId(selected.getId()).size();
        String warningText = "Удалить образец «" + selected.getName() + "»?";
        if (measurementCount > 0) {
            warningText += "\n\nВНИМАНИЕ: вместе с образцом удалятся " + measurementCount + " измерений!";
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, warningText, ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Подтверждение удаления");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    measurementService.getBySampleId(selected.getId())
                            .forEach(m -> measurementService.delete(m.getId()));
                    sampleService.delete(selected.getId());
                    refresh();
                } catch (IllegalArgumentException ex) {
                    showError("Ошибка при удалении", ex.getMessage());
                }
            }
        });
    }

    public void refresh() {
        List<Sample> samples = sampleService.getAll();
        javafx.application.Platform.runLater(() -> {
            tableView.setItems(FXCollections.observableArrayList(samples));
            tableView.refresh();
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public Parent getRoot() { return root; }
}
