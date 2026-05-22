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

/**
 * Вкладка "Образцы".
 *
 * Этап 5:
 *  - Добавлена колонка "Владелец".
 *  - Кнопки "Редактировать" и "Архивировать" активны только если:
 *      * пользователь авторизован, И
 *      * объект принадлежит текущему пользователю ИЛИ его владелец "SYSTEM".
 */
public class SampleTab {

    private final SampleService sampleService;
    private final MeasurementService measurementService;
    private final AuthService authService;

    private final BorderPane root;
    private final TableView<Sample> tableView;
    private final Button btnEdit;
    private final Button btnArchive;

    public SampleTab(SampleService sampleService,
                     MeasurementService measurementService,
                     AuthService authService) {
        this.sampleService      = sampleService;
        this.measurementService = measurementService;
        this.authService        = authService;

        btnEdit    = new Button("Редактировать");
        btnArchive = new Button("Архивировать");

        tableView = buildTable();
        HBox toolbar = buildToolbar();

        root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(tableView);
        BorderPane.setMargin(toolbar, new Insets(8));

        // При смене выбора обновляем доступность кнопок
        tableView.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> updateButtonState(selected)
        );
        updateButtonState(null);
    }

    // -------------------------------------------------------------------------
    // Таблица
    // -------------------------------------------------------------------------

    private TableView<Sample> buildTable() {
        TableView<Sample> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Нет образцов. Нажмите «Добавить»."));

        TableColumn<Sample, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colId.setPrefWidth(45);

        TableColumn<Sample, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));

        TableColumn<Sample, String> colType = new TableColumn<>("Тип");
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));

        TableColumn<Sample, String> colLocation = new TableColumn<>("Место");
        colLocation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLocation()));

        TableColumn<Sample, String> colStatus = new TableColumn<>("Статус");
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colStatus.setPrefWidth(80);

        // Колонка владельца (этап 5)
        TableColumn<Sample, String> colOwner = new TableColumn<>("Владелец");
        colOwner.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOwnerUsername()));
        colOwner.setPrefWidth(100);

        TableColumn<Sample, String> colMeasurements = new TableColumn<>("Измерений");
        colMeasurements.setCellValueFactory(c -> {
            int count = measurementService.getBySampleId(c.getValue().getId()).size();
            return new SimpleStringProperty(String.valueOf(count));
        });
        colMeasurements.setPrefWidth(80);

        table.getColumns().addAll(colId, colName, colType, colLocation, colStatus, colOwner, colMeasurements);
        return table;
    }

    // -------------------------------------------------------------------------
    // Панель кнопок
    // -------------------------------------------------------------------------

    private HBox buildToolbar() {
        Button btnAdd     = new Button("Добавить");
        Button btnRefresh = new Button("🔄 Обновить");

        btnAdd.setOnAction(e -> handleAdd());
        btnEdit.setOnAction(e -> handleEdit());
        btnArchive.setOnAction(e -> handleArchive());
        btnRefresh.setOnAction(e -> refresh());

        HBox toolbar = new HBox(8, btnAdd, btnEdit, btnArchive, new Separator(), btnRefresh);
        toolbar.setPadding(new Insets(4));
        return toolbar;
    }

    // -------------------------------------------------------------------------
    // Управление доступностью кнопок
    // -------------------------------------------------------------------------

    /**
     * Кнопки Edit и Archive активны только если:
     *  - пользователь авторизован
     *  - выбран объект
     *  - текущий пользователь является владельцем ИЛИ владелец "SYSTEM"
     */
    private void updateButtonState(Sample selected) {
        if (!authService.isLoggedIn() || selected == null) {
            btnEdit.setDisable(true);
            btnArchive.setDisable(true);
            return;
        }

        boolean canEdit = canModify(selected.getOwnerUsername());
        btnEdit.setDisable(!canEdit);
        btnArchive.setDisable(!canEdit);
    }

    /**
     * Проверяет право на изменение по той же логике, что AuthService.requireOwnership().
     */
    private boolean canModify(String ownerUsername) {
        if (!authService.isLoggedIn()) return false;
        if ("SYSTEM".equals(ownerUsername)) return true;
        return authService.getCurrentUsername().equals(ownerUsername);
    }

    // -------------------------------------------------------------------------
    // Обработчики
    // -------------------------------------------------------------------------

    private void handleAdd() {
        if (!authService.isLoggedIn()) {
            showError("Требуется авторизация", "Войдите в систему, чтобы добавлять образцы.");
            return;
        }
        SampleDialog dialog = new SampleDialog(null);
        Optional<SampleDialog.SampleFormData> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                sampleService.add(data.name(), data.type(), data.location());
                refresh();
            } catch (Exception ex) {
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
                sampleService.update(selected.getId(), "name",     data.name());
                sampleService.update(selected.getId(), "type",     data.type());
                sampleService.update(selected.getId(), "location", data.location());
                refresh();
            } catch (Exception ex) {
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
                } catch (Exception ex) {
                    showError("Ошибка", ex.getMessage());
                }
            }
        });
    }

    public void refresh() {
        List<Sample> samples = sampleService.getAll();
        tableView.setItems(FXCollections.observableArrayList(samples));
        tableView.refresh();
        // После обновления перепроверяем состояние кнопок
        updateButtonState(tableView.getSelectionModel().getSelectedItem());
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public Parent getRoot() { return root; }
}
