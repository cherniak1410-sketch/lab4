package ru.yourteam.lab.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.domain.Protocol;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.ProtocolService;
import ru.yourteam.lab.service.SampleService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Вкладка "Протоколы".
 * Показывает таблицу протоколов.
 * Кнопки: Создать, Проверить соответствие, Обновить.
 */
public class ProtocolTab {

    private final ProtocolService protocolService;
    private final MeasurementService measurementService;
    private final SampleService sampleService;

    private final BorderPane root;
    private final TableView<Protocol> tableView;

    public ProtocolTab(ProtocolService protocolService,
                       MeasurementService measurementService,
                       SampleService sampleService) {
        this.protocolService = protocolService;
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

    private TableView<Protocol> buildTable() {
        TableView<Protocol> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Нет протоколов. Нажмите «Создать»."));

        TableColumn<Protocol, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(cell ->
                new SimpleStringProperty(String.valueOf(cell.getValue().getId())));
        colId.setPrefWidth(50);

        TableColumn<Protocol, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getName()));

        TableColumn<Protocol, String> colParams = new TableColumn<>("Обязательные параметры");
        colParams.setCellValueFactory(cell -> {
            String params = cell.getValue().getRequiredParams().stream()
                    .map(MeasurementParam::name)
                    .sorted()
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(params);
        });

        TableColumn<Protocol, String> colOwner = new TableColumn<>("Владелец");
        colOwner.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().getOwnerUsername()));

        table.getColumns().addAll(colId, colName, colParams, colOwner);
        return table;
    }

    // ──────────────────────────────────────────────
    // Панель кнопок
    // ──────────────────────────────────────────────

    private HBox buildToolbar() {
        Button btnCreate = new Button("Создать");
        Button btnApply = new Button("Проверить соответствие");
        Button btnRefresh = new Button("🔄 Обновить");

        btnCreate.setOnAction(e -> handleCreate());
        btnApply.setOnAction(e -> handleApply());
        btnRefresh.setOnAction(e -> refresh());

        HBox toolbar = new HBox(8, btnCreate, btnApply, new Separator(), btnRefresh);
        toolbar.setPadding(new Insets(4));
        return toolbar;
    }

    // ──────────────────────────────────────────────
    // Обработчики
    // ──────────────────────────────────────────────

    private void handleCreate() {
        ProtocolDialog dialog = new ProtocolDialog();
        Optional<ProtocolDialog.ProtocolFormData> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                protocolService.add(data.name(), data.params());
                refresh();
            } catch (IllegalArgumentException ex) {
                showError("Ошибка при создании", ex.getMessage());
            }
        });
    }

    private void handleApply() {
        Protocol selectedProtocol = tableView.getSelectionModel().getSelectedItem();
        if (selectedProtocol == null) {
            showError("Ничего не выбрано", "Выберите протокол в таблице.");
            return;
        }

        List<Sample> samples = sampleService.getAll();
        if (samples.isEmpty()) {
            showError("Нет образцов", "Сначала добавьте образцы.");
            return;
        }

        // Диалог выбора образца для проверки
        ChoiceDialog<Sample> choiceDialog = new ChoiceDialog<>(samples.get(0), samples);
        choiceDialog.setTitle("Проверить соответствие протоколу");
        choiceDialog.setHeaderText("Протокол: " + selectedProtocol.getName());
        choiceDialog.setContentText("Выберите образец:");

        // Переопределяем отображение образца в диалоге
        choiceDialog.showAndWait().ifPresent(sample -> {
            // Собираем параметры, которые уже измерены у образца
            Set<MeasurementParam> measuredParams = measurementService
                    .getBySampleId(sample.getId())
                    .stream()
                    .map(m -> m.getParam())
                    .collect(Collectors.toSet());

            Set<MeasurementParam> missing = protocolService.checkCompliance(
                    selectedProtocol.getId(), measuredParams);

            String message;
            Alert.AlertType alertType;
            if (missing.isEmpty()) {
                message = " Протокол выполнен полностью для образца «" + sample.getName() + "».";
                alertType = Alert.AlertType.INFORMATION;
            } else {
                String missingStr = missing.stream()
                        .map(MeasurementParam::name)
                        .sorted()
                        .collect(Collectors.joining(", "));
                message = " Не хватает измерений для образца «" + sample.getName() + "»:\n" + missingStr;
                alertType = Alert.AlertType.WARNING;
            }

            Alert resultAlert = new Alert(alertType, message, ButtonType.OK);
            resultAlert.setTitle("Результат проверки");
            resultAlert.setHeaderText(null);
            resultAlert.showAndWait();
        });
    }

    /** Перечитывает данные из сервиса и перерисовывает таблицу. */
    public void refresh() {
        List<Protocol> protocols = protocolService.getAll();
        tableView.setItems(FXCollections.observableArrayList(protocols));
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
