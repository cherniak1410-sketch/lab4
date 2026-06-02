package ru.yourteam.lab.ui;


import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.domain.Protocol;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.ProtocolService;
import ru.yourteam.lab.service.SampleService;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Вкладка "Протоколы".
 *
 * Этап 7: добавлена панель расширенной фильтрации.
 */
public class ProtocolTab {

    private final ProtocolService protocolService;
    private final MeasurementService measurementService;
    private final SampleService sampleService;
    private final AuthService authService;

    private final BorderPane root;
    private final TableView<Protocol> tableView;

    // Поля фильтра
    private TextField        filterOwnerField;
    private TextField        filterNameField;
    private CheckBox         cbFilterPh;
    private CheckBox         cbFilterConductivity;
    private CheckBox         cbFilterTurbidity;
    private CheckBox         cbFilterNitrate;
    private ToggleGroup      paramMatchGroup;  // ALL / ANY

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
        filterOwnerField = new TextField();
        filterOwnerField.setPromptText("Владелец...");
        filterOwnerField.setPrefWidth(120);

        filterNameField = new TextField();
        filterNameField.setPromptText("Часть названия...");
        filterNameField.setPrefWidth(150);

        cbFilterPh           = new CheckBox("PH");
        cbFilterConductivity = new CheckBox("CONDUCTIVITY");
        cbFilterTurbidity    = new CheckBox("TURBIDITY");
        cbFilterNitrate      = new CheckBox("NITRATE");

        // ALL / ANY переключатель
        paramMatchGroup = new ToggleGroup();
        RadioButton rbAll = new RadioButton("все выбранные");
        RadioButton rbAny = new RadioButton("хотя бы один");
        rbAll.setToggleGroup(paramMatchGroup);
        rbAny.setToggleGroup(paramMatchGroup);
        rbAll.setSelected(true);
        rbAll.setUserData("ALL");
        rbAny.setUserData("ANY");

        Button btnApply = new Button("Применить");
        Button btnReset = new Button("Сбросить");

        btnApply.setOnAction(e -> applyFilter());
        btnReset.setOnAction(e -> {
            filterOwnerField.clear();
            filterNameField.clear();
            cbFilterPh.setSelected(false);
            cbFilterConductivity.setSelected(false);
            cbFilterTurbidity.setSelected(false);
            cbFilterNitrate.setSelected(false);
            rbAll.setSelected(true);
            refresh();
        });

        HBox row = new HBox(10,
                new Label("Владелец:"), filterOwnerField,
                new Label("Название:"), filterNameField,
                new Label("Параметры:"),
                cbFilterPh, cbFilterConductivity, cbFilterTurbidity, cbFilterNitrate,
                new Separator(),
                rbAll, rbAny,
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
        String owner = filterOwnerField.getText().trim();
        String name  = filterNameField.getText().trim();

        // Собираем выбранные параметры
        Set<MeasurementParam> selectedParams = EnumSet.noneOf(MeasurementParam.class);
        if (cbFilterPh.isSelected())           selectedParams.add(MeasurementParam.PH);
        if (cbFilterConductivity.isSelected()) selectedParams.add(MeasurementParam.CONDUCTIVITY);
        if (cbFilterTurbidity.isSelected())    selectedParams.add(MeasurementParam.TURBIDITY);
        if (cbFilterNitrate.isSelected())      selectedParams.add(MeasurementParam.NITRATE);

        String mode = paramMatchGroup.getSelectedToggle() != null
                ? (String) paramMatchGroup.getSelectedToggle().getUserData() : "ALL";

        Set<MeasurementParam> hasAll = "ALL".equals(mode) ? selectedParams : null;
        Set<MeasurementParam> hasAny = "ANY".equals(mode) ? selectedParams : null;

        List<Protocol> filtered = protocolService.filter(owner, name, hasAll, hasAny);
        tableView.setItems(FXCollections.observableArrayList(filtered));
        tableView.refresh();
    }

    // ──────────────────────────────────────────────
    // Таблица
    // ──────────────────────────────────────────────

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

        TableColumn<Protocol, String> colCount = new TableColumn<>("Кол-во пар-ров");
        colCount.setCellValueFactory(c ->
                new SimpleStringProperty(String.valueOf(c.getValue().getRequiredParams().size())));
        colCount.setPrefWidth(110);

        TableColumn<Protocol, String> colOwner = new TableColumn<>("Владелец");
        colOwner.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOwnerUsername()));
        colOwner.setPrefWidth(100);

        table.getColumns().addAll(colId, colName, colParams, colCount, colOwner);
        return table;
    }

    // ──────────────────────────────────────────────
    // Тулбар
    // ──────────────────────────────────────────────

    private HBox buildToolbar() {
        Button btnCreate  = new Button("Создать");
        Button btnApply   = new Button("Проверить соответствие");
        Button btnRefresh = new Button("Обновить");
        Button btnDelete  = new Button("Удалить");

        btnCreate.setOnAction(e -> handleCreate());
        btnApply.setOnAction(e -> handleApply());
        btnDelete.setOnAction(e -> handleDelete());
        btnRefresh.setOnAction(e -> refresh());

        HBox toolbar = new HBox(8, btnCreate, btnApply, btnDelete, new Separator(), btnRefresh);
        toolbar.setPadding(new Insets(4));
        return toolbar;
    }

    // ──────────────────────────────────────────────
    // Обработчики
    // ──────────────────────────────────────────────

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
                message = "✅ Протокол выполнен полностью для образца «" + sample.getName() + "».";
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
}