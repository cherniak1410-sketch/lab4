package ru.yourteam.lab.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import ru.yourteam.lab.auth.AuthService;
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
 *
 * Этап 5: кнопка "Создать" активна только для авторизованных пользователей.
 * Добавлена колонка "Владелец".
 */
public class ProtocolTab {

    private final ProtocolService protocolService;
    private final MeasurementService measurementService;
    private final SampleService sampleService;
    private final AuthService authService;

    private final BorderPane root;
    private final TableView<Protocol> tableView;

    public ProtocolTab(ProtocolService protocolService,
                       MeasurementService measurementService,
                       SampleService sampleService,
                       AuthService authService) {
        this.protocolService    = protocolService;
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

    private TableView<Protocol> buildTable() {
        TableView<Protocol> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Нет протоколов. Нажмите «Создать»."));

        TableColumn<Protocol, String> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colId.setPrefWidth(45);

        TableColumn<Protocol, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));

        TableColumn<Protocol, String> colParams = new TableColumn<>("Обязательные параметры");
        colParams.setCellValueFactory(c -> {
            String params = c.getValue().getRequiredParams().stream()
                    .map(MeasurementParam::name).sorted().collect(Collectors.joining(", "));
            return new SimpleStringProperty(params);
        });

        // Колонка владельца (этап 5)
        TableColumn<Protocol, String> colOwner = new TableColumn<>("Владелец");
        colOwner.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOwnerUsername()));
        colOwner.setPrefWidth(100);

        table.getColumns().addAll(colId, colName, colParams, colOwner);
        return table;
    }

    private HBox buildToolbar() {
        Button btnCreate  = new Button("Создать");
        Button btnApply   = new Button("Проверить соответствие");
        Button btnRefresh = new Button(" Обновить");
        Button btnDelete = new Button("Удалить");

        btnCreate.setOnAction(e -> handleCreate());
        btnApply.setOnAction(e -> handleApply());
        btnDelete.setOnAction(e -> handleDelete());
        btnRefresh.setOnAction(e -> refresh());

        HBox toolbar = new HBox(8, btnCreate, btnApply, btnDelete,new Separator(), btnRefresh);
        toolbar.setPadding(new Insets(4));
        return toolbar;
    }

    private void handleCreate() {
        if (!authService.isLoggedIn()) {
            showError("Требуется авторизация", "Войдите в систему, чтобы создавать протоколы.");
            return;
        }
        ProtocolDialog dialog = new ProtocolDialog();
        Optional<ProtocolDialog.ProtocolFormData> result = dialog.showAndWait();
        result.ifPresent(data -> {
            try {
                protocolService.add(data.name(), data.params());
                refresh();
            } catch (Exception ex) {
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

        ChoiceDialog<Sample> choiceDialog = new ChoiceDialog<>(samples.get(0), samples);
        choiceDialog.setTitle("Проверить соответствие протоколу");
        choiceDialog.setHeaderText("Протокол: " + selectedProtocol.getName());
        choiceDialog.setContentText("Выберите образец:");

        choiceDialog.showAndWait().ifPresent(sample -> {
            Set<MeasurementParam> measured = measurementService.getBySampleId(sample.getId()).stream()
                    .map(m -> m.getParam()).collect(Collectors.toSet());

            Set<MeasurementParam> missing = protocolService.checkCompliance(
                    selectedProtocol.getId(), measured);

            String message;
            Alert.AlertType type;
            if (missing.isEmpty()) {
                message = " Протокол выполнен полностью для образца «" + sample.getName() + "».";
                type = Alert.AlertType.INFORMATION;
            } else {
                String missingStr = missing.stream().map(MeasurementParam::name)
                        .sorted().collect(Collectors.joining(", "));
                message = "⚠ Не хватает измерений для образца «" + sample.getName() + "»:\n" + missingStr;
                type = Alert.AlertType.WARNING;
            }

            Alert resultAlert = new Alert(type, message, ButtonType.OK);
            resultAlert.setTitle("Результат проверки");
            resultAlert.setHeaderText(null);
            resultAlert.showAndWait();
        });
    }

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

    public Parent getRoot() { return root; }
    private void handleDelete() {
        Protocol selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Ничего не выбрано", "Выберите протокол в таблице.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Удалить протокол «" + selected.getName() + "»?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Подтверждение удаления");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    protocolService.delete(selected.getId());
                    refresh();
                } catch (IllegalArgumentException ex) {
                    showError("Ошибка при удалении", ex.getMessage());
                }
            }
        });
    }
}
