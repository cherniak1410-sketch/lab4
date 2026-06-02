package ru.yourteam.lab.ui;


import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.domain.SampleStatus;
import ru.yourteam.lab.service.MeasurementService;
import ru.yourteam.lab.service.ProtocolService;
import ru.yourteam.lab.service.SampleService;

import java.util.Map;

/**
 * Вкладка "Статистика".
 * Этап 7: сводная панель по образцам, измерениям и протоколам.
 */
public class StatsTab {

    private final SampleService      sampleService;
    private final MeasurementService measurementService;
    private final ProtocolService    protocolService;
    private final AuthService        authService;

    private final BorderPane root;

    private ComboBox<String> sampleCombo;
    private final java.util.Map<String, Long> sampleLabelToId = new java.util.LinkedHashMap<>();
    private TableView<String[]> sampleStatsTable;

    public StatsTab(SampleService sampleService,
                    MeasurementService measurementService,
                    ProtocolService protocolService,
                    AuthService authService) {
        this.sampleService      = sampleService;
        this.measurementService = measurementService;
        this.protocolService    = protocolService;
        this.authService        = authService;

        ScrollPane scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");

        Button btnRefresh = new Button("🔄 Обновить статистику");
        btnRefresh.setOnAction(e -> refresh());
        HBox top = new HBox(btnRefresh);
        top.setPadding(new Insets(6, 8, 2, 8));

        root = new BorderPane();
        root.setTop(top);
        root.setCenter(scroll);
    }

    // Построение контента

    private VBox buildContent() {
        VBox vbox = new VBox(12,
                buildSummarySection(),
                buildSamplesByOwnerSection(),
                buildSamplesByTypeSection(),
                buildSamplesByStatusSection(),
                buildGlobalMeasurementStatsSection(),
                buildPerSampleStatsSection(),
                buildProtocolParamSection(),
                buildProtocolOwnerSection(),
                buildUserSection()
        );
        vbox.setPadding(new Insets(8));
        return vbox;
    }

    //  1. Общая сводка

    private TitledPane buildSummarySection() {
        Map<String, Long>   sampleSummary = sampleService.getSummary();
        Map<String, Long>   measSummary   = measurementService.getSummary();
        Map<String, Object> protoSummary  = protocolService.getSummary();

        VBox colSamples = buildLongColumn("Образцы",   sampleSummary);
        VBox colMeas    = buildLongColumn("Измерения",  measSummary);
        VBox colProto   = buildObjColumn("Протоколы",  protoSummary);

        HBox hbox = new HBox(40, colSamples, colMeas, colProto);
        hbox.setPadding(new Insets(10));

        TitledPane tp = new TitledPane("📊 Общая сводка", hbox);
        tp.setExpanded(true);
        return tp;
    }

    private VBox buildLongColumn(String title, Map<String, Long> data) {
        VBox box = new VBox(5);
        box.getChildren().add(boldLabel(title));
        for (var e : data.entrySet()) {
            HBox row = new HBox(8, new Label(e.getKey() + ":"), new Label(String.valueOf(e.getValue())));
            box.getChildren().add(row);
        }
        return box;
    }

    private VBox buildObjColumn(String title, Map<String, Object> data) {
        VBox box = new VBox(5);
        box.getChildren().add(boldLabel(title));
        for (var e : data.entrySet()) {
            HBox row = new HBox(8, new Label(e.getKey() + ":"), new Label(String.valueOf(e.getValue())));
            box.getChildren().add(row);
        }
        return box;
    }

    //  2. Образцы по владельцу

    private TitledPane buildSamplesByOwnerSection() {
        Map<String, Long> data = sampleService.countByOwner();
        TableView<Map.Entry<String, Long>> table = buildSimpleKVTable("Владелец", "Образцов", data);
        TitledPane tp = new TitledPane("👤 Образцы по владельцу", table);
        tp.setExpanded(false);
        return tp;
    }

    //  3. Образцы по типу

    private TitledPane buildSamplesByTypeSection() {
        Map<String, Long> data = sampleService.countByType();
        TableView<Map.Entry<String, Long>> table = buildSimpleKVTable("Тип", "Образцов", data);
        TitledPane tp = new TitledPane("🧪 Образцы по типу", table);
        tp.setExpanded(false);
        return tp;
    }

    //  4. Образцы по статусу

    private TitledPane buildSamplesByStatusSection() {
        Map<SampleStatus, Long> raw = sampleService.countByStatus();
        java.util.Map<String, Long> data = new java.util.LinkedHashMap<>();
        for (var e : raw.entrySet()) data.put(e.getKey().name(), e.getValue());
        TableView<Map.Entry<String, Long>> table = buildSimpleKVTable("Статус", "Образцов", data);
        TitledPane tp = new TitledPane("📁 Образцы по статусу", table);
        tp.setExpanded(false);
        return tp;
    }

    //  5. Глобальная статистика измерений

    private TitledPane buildGlobalMeasurementStatsSection() {
        Map<MeasurementParam, Map<String, Object>> stats = measurementService.getGlobalStats();

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMaxHeight(200);
        table.setPlaceholder(new Label("Нет данных"));

        TableColumn<String[], String> colParam = strCol("Параметр", 0);
        TableColumn<String[], String> colCount = strCol("Кол-во",   1);
        TableColumn<String[], String> colMin   = strCol("Min",      2);
        TableColumn<String[], String> colMax   = strCol("Max",      3);
        TableColumn<String[], String> colAvg   = strCol("Среднее",  4);
        //noinspection unchecked
        table.getColumns().addAll(colParam, colCount, colMin, colMax, colAvg);

        var rows = FXCollections.<String[]>observableArrayList();
        for (var e : stats.entrySet()) {
            Map<String, Object> m = e.getValue();
            rows.add(new String[]{
                    e.getKey().name(),
                    String.valueOf(m.get("count")),
                    fmt(m.get("min")),
                    fmt(m.get("max")),
                    fmt(m.get("avg"))
            });
        }
        table.setItems(rows);

        TitledPane tp = new TitledPane("📈 Статистика измерений (глобальная)", table);
        tp.setExpanded(true);
        return tp;
    }

    //  6. Статистика по конкретному образцу

    private TitledPane buildPerSampleStatsSection() {
        sampleCombo = new ComboBox<>();
        sampleCombo.setPrefWidth(220);
        refreshSampleCombo();

        Button btnLoad = new Button("Показать");
        btnLoad.setOnAction(e -> loadSampleStats());

        HBox header = new HBox(8, new Label("Образец:"), sampleCombo, btnLoad);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 0, 6, 0));

        sampleStatsTable = new TableView<>();
        sampleStatsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        sampleStatsTable.setMaxHeight(180);
        sampleStatsTable.setPlaceholder(new Label("Выберите образец и нажмите «Показать»"));

        TableColumn<String[], String> colParam = strCol("Параметр", 0);
        TableColumn<String[], String> colCount = strCol("Кол-во",   1);
        TableColumn<String[], String> colMin   = strCol("Min",      2);
        TableColumn<String[], String> colMax   = strCol("Max",      3);
        TableColumn<String[], String> colAvg   = strCol("Среднее",  4);
        //noinspection unchecked
        sampleStatsTable.getColumns().addAll(colParam, colCount, colMin, colMax, colAvg);

        VBox content = new VBox(4, header, sampleStatsTable);
        content.setPadding(new Insets(6));

        TitledPane tp = new TitledPane("🔬 Статистика по образцу", content);
        tp.setExpanded(false);
        return tp;
    }

    private void loadSampleStats() {
        String label = sampleCombo.getValue();
        if (label == null || !sampleLabelToId.containsKey(label)) return;
        long sampleId = sampleLabelToId.get(label);

        Map<MeasurementParam, Map<String, Object>> stats =
                measurementService.getStatsBySample(sampleId);

        var rows = FXCollections.<String[]>observableArrayList();
        for (var e : stats.entrySet()) {
            Map<String, Object> m = e.getValue();
            rows.add(new String[]{
                    e.getKey().name(),
                    String.valueOf(m.get("count")),
                    fmt(m.get("min")),
                    fmt(m.get("max")),
                    fmt(m.get("avg"))
            });
        }
        if (rows.isEmpty()) {
            sampleStatsTable.setPlaceholder(new Label("Нет измерений для этого образца"));
        }
        sampleStatsTable.setItems(rows);
    }

    // 7. Протоколы по параметрам

    private TitledPane buildProtocolParamSection() {
        Map<MeasurementParam, Long> data = protocolService.countByParam();
        java.util.Map<String, Long> converted = new java.util.LinkedHashMap<>();
        for (var e : data.entrySet()) converted.put(e.getKey().name(), e.getValue());
        TableView<Map.Entry<String, Long>> table = buildSimpleKVTable("Параметр", "Протоколов", converted);
        TitledPane tp = new TitledPane("📋 Протоколы — покрытие параметров", table);
        tp.setExpanded(false);
        return tp;
    }

    //  8. Протоколы по владельцу

    private TitledPane buildProtocolOwnerSection() {
        Map<String, Long> data = protocolService.countByOwner();
        TableView<Map.Entry<String, Long>> table = buildSimpleKVTable("Владелец", "Протоколов", data);
        TitledPane tp = new TitledPane("👤 Протоколы по владельцу", table);
        tp.setExpanded(false);
        return tp;
    }

    // 9. Пользователи — сводка

    private TitledPane buildUserSection() {
        Map<String, Long> samplesByUser      = sampleService.countByOwner();
        Map<String, Long> measurementsByUser = measurementService.countByOwner();
        Map<String, Long> protocolsByUser    = protocolService.countByOwner();

        java.util.Set<String> users = new java.util.LinkedHashSet<>();
        users.addAll(samplesByUser.keySet());
        users.addAll(measurementsByUser.keySet());
        users.addAll(protocolsByUser.keySet());

        TableView<String[]> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMaxHeight(220);
        table.setPlaceholder(new Label("Нет данных"));

        TableColumn<String[], String> colUser  = strCol("Пользователь", 0);
        TableColumn<String[], String> colSamp  = strCol("Образцов",     1);
        TableColumn<String[], String> colMeas  = strCol("Измерений",    2);
        TableColumn<String[], String> colProto = strCol("Протоколов",   3);
        //noinspection unchecked
        table.getColumns().addAll(colUser, colSamp, colMeas, colProto);

        var rows = FXCollections.<String[]>observableArrayList();
        for (String u : users) {
            rows.add(new String[]{
                    u,
                    String.valueOf(samplesByUser.getOrDefault(u, 0L)),
                    String.valueOf(measurementsByUser.getOrDefault(u, 0L)),
                    String.valueOf(protocolsByUser.getOrDefault(u, 0L))
            });
        }
        table.setItems(rows);

        TitledPane tp = new TitledPane("🧑‍💻 Активность пользователей", table);
        tp.setExpanded(false);
        return tp;
    }

    // Обновление

    public void refresh() {
        ScrollPane scroll = (ScrollPane) root.getCenter();
        scroll.setContent(buildContent());
        refreshSampleCombo();
    }

    private void refreshSampleCombo() {
        if (sampleCombo == null) return;
        sampleLabelToId.clear();
        sampleCombo.getItems().clear();
        for (Sample s : sampleService.getAll()) {
            String label = "#" + s.getId() + " " + s.getName();
            sampleLabelToId.put(label, s.getId());
            sampleCombo.getItems().add(label);
        }
        if (!sampleCombo.getItems().isEmpty()) {
            sampleCombo.getSelectionModel().selectFirst();
        }
    }

    // Вспомогательные методы

    private TableView<Map.Entry<String, Long>> buildSimpleKVTable(
            String keyHeader, String valHeader, Map<String, Long> data) {

        TableView<Map.Entry<String, Long>> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setMaxHeight(160);
        table.setPlaceholder(new Label("Нет данных"));

        TableColumn<Map.Entry<String, Long>, String> colKey = new TableColumn<>(keyHeader);
        colKey.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getKey()));

        TableColumn<Map.Entry<String, Long>, String> colVal = new TableColumn<>(valHeader);
        colVal.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getValue())));
        colVal.setPrefWidth(100);

        //noinspection unchecked
        table.getColumns().addAll(colKey, colVal);
        table.setItems(FXCollections.observableArrayList(data.entrySet()));
        return table;
    }

    private TableColumn<String[], String> strCol(String title, int idx) {
        TableColumn<String[], String> c = new TableColumn<>(title);
        c.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue()[idx] != null ? cell.getValue()[idx] : ""));
        return c;
    }

    private Label boldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private String fmt(Object val) {
        if (val == null) return "—";
        if (val instanceof Double d) return String.format("%.4f", d);
        return val.toString();
    }

    public Parent getRoot() { return root; }
}