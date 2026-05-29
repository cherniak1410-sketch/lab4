package ru.yourteam.lab.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.SampleService;

import java.util.List;
import java.util.Optional;

public class SampleTab {

    private final SampleService sampleService;
    private final MeasurementService measurementService;
    private final AuthService authService; // ← НОВОЕ ПОЛЕ

    private final BorderPane root;
    private final TableView<Sample> tableView;

    public SampleTab(SampleService sampleService,
                     MeasurementService measurementService,
                     AuthService authService) { // ← ДОБАВИЛИ ПАРАМЕТР
        this.sampleService = sampleService;
        this.measurementService = measurementService;
        this.authService = authService; // ← СОХРАНЯЕМ

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

        table.getColumns().addAll(colId, colName, colType, colLocation, colStatus, colOwner, colMeasurements);
        return table;
    }

    // ──────────────────────────────────────────────
    // Панель кнопок
    // ──────────────────────────────────────────────

    private HBox buildToolbar() {
        Button btnAdd     = new Button("Добавить");
        Button btnEdit    = new Button("Редактировать");
        Button btnArchive = new Button("Архивировать");
        Button btnDelete  = new Button("Удалить"); // ← НОВАЯ КНОПКА
        Button btnRestore = new Button("Восстановить");
        Button btnRefresh = new Button("🔄 Обновить");

        btnAdd.setOnAction(e -> handleAdd());
        btnEdit.setOnAction(e -> handleEdit());
        btnArchive.setOnAction(e -> handleArchive());
        btnDelete.setOnAction(e -> handleDelete()); // ← НОВЫЙ ОБРАБОТЧИК
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

        HBox toolbar = new HBox(
                8,
                btnAdd,
                btnEdit,
                btnArchive,
                btnDelete,
                btnRestore,
                new Separator(),
                btnRefresh
        );
        toolbar.setPadding(new Insets(4));
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
        // Берём выбранный образец
        Sample selected = tableView.getSelectionModel().getSelectedItem();

        // Если ничего не выбрано — ошибка
        if (selected == null) {
            showError("Ничего не выбрано", "Выберите образец в таблице.");
            return;
        }

        // Проверяем права — кто текущий пользователь и кто владелец
        String currentUser = authService.getCurrentUsername();
        String owner = selected.getOwnerUsername();

        // Если текущий пользователь не владелец — запрещаем
        if (!currentUser.equals(owner) && !"SYSTEM".equals(owner)) {
            showError("Нет прав",
                    "Вы не можете удалить этот образец.\n" +
                            "Владелец: " + owner);
            return;
        }

        // Считаем измерения — предупредим что они тоже удалятся
        int measurementCount = measurementService.getBySampleId(selected.getId()).size();

        String warningText = "Удалить образец «" + selected.getName() + "»?";
        if (measurementCount > 0) {
            warningText += "\n\nВНИМАНИЕ: вместе с образцом удалятся "
                    + measurementCount + " измерений!";
        }

        // Диалог подтверждения
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                warningText,
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Подтверждение удаления");
        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    // Сначала удаляем все измерения этого образца
                    measurementService.getBySampleId(selected.getId())
                            .forEach(m -> measurementService.delete(m.getId()));
                    // Потом удаляем сам образец
                    sampleService.delete(selected.getId());
                    refresh();
                } catch (IllegalArgumentException ex) {
                    showError("Ошибка при удалении", ex.getMessage());
                }
            }
        });
    }

    public void refresh() {
        // Получаем свежие данные
        List<Sample> samples = sampleService.getAll();

        // Создаём НОВЫЙ ObservableList (это заставляет таблицу перерисоваться)
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

    public Parent getRoot() {
        return root;
    }
}