package ru.yourteam.lab;

import ru.yourteam.lab.domain.*;
import ru.yourteam.lab.repository.*;


import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class LabApp {
    private final Scanner scanner = new Scanner(System.in);
    private final SampleRepository sampleRepository = new SampleRepository();
    private final MeasurementRepository measurementRepository = new MeasurementRepository();
    private final ProtocolRepository protocolRepository = new ProtocolRepository();
    private final ReagentRepository reagentRepository = new ReagentRepository();    // ← добавить
    private final BatchRepository batchRepository = new BatchRepository();          // ← добавить// ← добавить


    public static void main(String[] args) {
        LabApp app = new LabApp();
        app.run();
    }

    public void run() {
        System.out.println("Лабораторная система управления образцами");
        System.out.println("Введите команду (exit для выхода):");

        while (true) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();

            if (line.equals("exit")) {
                System.out.println("До свидания!");
                break;
            }

            if (line.isEmpty()) {
                continue;
            }

            try {
                executeCommand(line);
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }

    private void executeCommand(String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        switch (command) {
            // ... существующие команды ...
            case "sample_add": sampleAdd(); break;
            case "sample_list": sampleList(args); break;
            case "sample_show": sampleShow(args); break;
            case "sample_update": sampleUpdate(args); break;
            case "sample_archive": sampleArchive(args); break;
            case "meas_add": measAdd(args); break;
            case "meas_list": measList(args); break;
            case "meas_stats": measStats(args); break;
            case "prot_create": protCreate(); break;
            case "prot_apply": protApply(args); break;

            // НОВЫЕ КОМАНДЫ ДНЯ 5 ↓↓↓
            case "reag_add":
                reagAdd();
                break;
            case "reag_list":
                reagList(args);
                break;
            case "batch_add":
                batchAdd(args);
                break;
            case "batch_list":
                batchList(args);
                break;

            default:
                System.out.println("Неизвестная команда: " + command);
        }
    }

    private void sampleAdd() {
        System.out.println("Создание нового образца:");

        System.out.print("Название: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("название не может быть пустым");
        }

        System.out.print("Тип: ");
        String type = scanner.nextLine().trim();
        if (type.isEmpty()) {
            throw new IllegalArgumentException("тип не может быть пустым");
        }

        System.out.print("Место: ");
        String location = scanner.nextLine().trim();
        if (location.isEmpty()) {
            throw new IllegalArgumentException("место не может быть пустым");
        }

        Sample sample = new Sample();
        sample.name = name;
        sample.type = type;
        sample.location = location;
        sample.status = SampleStatus.ACTIVE;
        sample.ownerUsername = "SYSTEM";
        sample.createdAt = Instant.now();
        sample.updatedAt = sample.createdAt;

        sampleRepository.save(sample);
        System.out.println("OK sample_id=" + sample.id);
    }
    private void sampleList(String[] args) {
        SampleStatus statusFilter = null;
        boolean mine = false;

        // Разбираем аргументы
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--status":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("не указан статус");
                    }
                    String statusStr = args[++i].toUpperCase();
                    try {
                        statusFilter = SampleStatus.valueOf(statusStr);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("неизвестный статус, используйте ACTIVE или ARCHIVED");
                    }
                    break;
                case "--mine":
                    mine = true;
                    break;
                default:
                    throw new IllegalArgumentException("неизвестная опция: " + args[i]);
            }
        }

        // Получаем все образцы
        List<Sample> allSamples = sampleRepository.findAll();
        List<Sample> samples = new ArrayList<>();

        // Фильтруем вручную (без лямбды)
        for (Sample s : allSamples) {
            boolean matches = true;

            // Проверяем фильтр по статусу
            if (statusFilter != null && s.status != statusFilter) {
                matches = false;
            }

            // Проверяем фильтр "мои"
            if (matches && mine) {
                String currentUser = "SYSTEM";
                if (!currentUser.equals(s.ownerUsername)) {
                    matches = false;
                }
            }

            if (matches) {
                samples.add(s);
            }
        }

        // Выводим заголовок
        System.out.printf("%-5s %-20s %-10s %-10s %-8s%n",
                "ID", "Name", "Type", "Location", "Status");
        System.out.println("-".repeat(60));

        // Выводим образцы
        for (Sample s : samples) {
            System.out.printf("%-5d %-20s %-10s %-10s %-8s%n",
                    s.id,
                    truncate(s.name, 20),
                    truncate(s.type, 10),
                    truncate(s.location, 10),
                    s.status);
        }

        if (samples.isEmpty()) {
            System.out.println("Нет образцов для отображения");
        }
    }
    private void sampleShow(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("использование: sample_show <id>");
        }

        long id;
        try {
            id = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id должен быть числом");
        }

        Sample sample = sampleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("образец с id=" + id + " не найден"));

        // Получаем измерения для этого образца
        List<Measurement> measurements = measurementRepository.findBySampleId(id);

        // Собираем уникальные параметры
        Set<MeasurementParam> params = new HashSet<>();
        for (Measurement m : measurements) {
            params.add(m.param);
        }

        System.out.println("Sample #" + sample.id);
        System.out.println("name: " + sample.name);
        System.out.println("type: " + sample.type);
        System.out.println("location: " + sample.location);
        System.out.println("status: " + sample.status);
        System.out.println("owner: " + sample.ownerUsername);
        System.out.println("created: " + sample.createdAt);
        System.out.println("updated: " + sample.updatedAt);
        System.out.println("measurements: " + measurements.size());

        if (params.isEmpty()) {
            System.out.println("params: ");
        } else {
            System.out.println("params: " + params.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(", ")));
        }
    }
    private void applyFieldUpdate(Sample sample, String field, String value) {
        switch (field) {
            case "name":
                if (value.isEmpty()) {
                    throw new IllegalArgumentException("name не может быть пустым");
                }
                if (value.length() > 128) {
                    throw new IllegalArgumentException("name слишком длинное (макс. 128)");
                }
                sample.name = value;
                break;

            case "type":
                if (value.isEmpty()) {
                    throw new IllegalArgumentException("type не может быть пустым");
                }
                if (value.length() > 64) {
                    throw new IllegalArgumentException("type слишком длинное (макс. 64)");
                }
                sample.type = value;
                break;

            case "location":
                if (value.isEmpty()) {
                    throw new IllegalArgumentException("location не может быть пустым");
                }
                if (value.length() > 64) {
                    throw new IllegalArgumentException("location слишком длинное (макс. 64)");
                }
                sample.location = value;
                break;

            case "status":
                try {
                    SampleStatus newStatus = SampleStatus.valueOf(value.toUpperCase());
                    sample.status = newStatus;
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("статус только ACTIVE или ARCHIVED");
                }
                break;

            default:
                throw new IllegalArgumentException("нельзя менять поле '" + field + "'");
        }
    }
    private void sampleUpdate(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("использование: sample_update <id> field=value ...");
        }

        long id;
        try {
            id = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id должен быть числом");
        }

        Sample sample = sampleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("образец с id=" + id + " не найден"));

        for (int i = 1; i < args.length; i++) {
            String[] parts = args[i].split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("неверный формат: " + args[i] + ". Используйте поле=значение");
            }

            String field = parts[0];
            String value = parts[1];

            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            applyFieldUpdate(sample, field, value);
        }

        sample.updatedAt = Instant.now();
        sampleRepository.save(sample);
        System.out.println("OK");
    }
    private void sampleArchive(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("использование: sample_archive <id>");
        }

        long id;
        try {
            id = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id должен быть числом");
        }

        Sample sample = sampleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("образец с id=" + id + " не найден"));

        if (sample.status == SampleStatus.ARCHIVED) {
            throw new IllegalArgumentException("образец уже ARCHIVED");
        }

        sample.status = SampleStatus.ARCHIVED;
        sample.updatedAt = Instant.now();
        sampleRepository.save(sample);

        System.out.println("OK sample " + id + " archived");
    }
    private String truncate(String s, int maxLength) {
        if (s == null) return "";
        if (s.length() <= maxLength) return s;
        return s.substring(0, maxLength - 3) + "...";
    }
    private void measAdd(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("использование: meas_add <sample_id>");
        }

        long sampleId;
        try {
            sampleId = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id образца должен быть числом");
        }

        Sample sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> new IllegalArgumentException("образец с id=" + sampleId + " не найден"));

        if (sample.status == SampleStatus.ARCHIVED) {
            throw new IllegalArgumentException("нельзя добавлять измерения к ARCHIVED образцу");
        }

        System.out.println("Добавление измерения к образцу #" + sampleId);

        System.out.print("Параметр (PH/CONDUCTIVITY/TURBIDITY/NITRATE): ");
        String paramStr = scanner.nextLine().trim().toUpperCase();
        MeasurementParam param;
        try {
            param = MeasurementParam.valueOf(paramStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("неизвестный параметр, допустимые: PH, CONDUCTIVITY, TURBIDITY, NITRATE");
        }

        System.out.print("Значение: ");
        String valueStr = scanner.nextLine().trim();
        double value;
        try {
            value = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("значение должно быть числом");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("значение должно быть конечным числом");
        }

        System.out.print("Единицы: ");
        String unit = scanner.nextLine().trim();
        if (unit.isEmpty()) {
            throw new IllegalArgumentException("единицы не могут быть пустыми");
        }

        System.out.print("Метод: ");
        String method = scanner.nextLine().trim();
        if (method.isEmpty()) {
            throw new IllegalArgumentException("метод не может быть пустым");
        }

        Measurement measurement = new Measurement();
        measurement.sampleId = sampleId;
        measurement.param = param;
        measurement.value = value;
        measurement.unit = unit;
        measurement.method = method;
        measurement.measuredAt = Instant.now();
        measurement.ownerUsername = "SYSTEM";
        measurement.createdAt = Instant.now();
        measurement.updatedAt = measurement.createdAt;

        measurementRepository.save(measurement);
        System.out.println("OK measurement_id=" + measurement.id);
    }
    private void measList(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("использование: meas_list <sample_id> [--param PARAM] [--last N]");
        }

        long sampleId;
        try {
            sampleId = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id образца должен быть числом");
        }

        if (sampleRepository.findById(sampleId).isEmpty()) {
            throw new IllegalArgumentException("образец с id=" + sampleId + " не найден");
        }

        MeasurementParam paramFilter = null;
        Integer last = null;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--param":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("не указан параметр");
                    }
                    String paramStr = args[++i].toUpperCase();
                    try {
                        paramFilter = MeasurementParam.valueOf(paramStr);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("неизвестный параметр, используйте PH, CONDUCTIVITY, TURBIDITY, NITRATE");
                    }
                    break;
                case "--last":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("не указано число");
                    }
                    try {
                        last = Integer.parseInt(args[++i]);
                        if (last <= 0) {
                            throw new IllegalArgumentException("число должно быть положительным");
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("некорректное число");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("неизвестная опция: " + args[i]);
            }
        }

        // Получаем все измерения
        List<Measurement> allMeasurements = measurementRepository.findBySampleId(sampleId);
        List<Measurement> measurements = new ArrayList<>();

        // Фильтруем вручную (без лямбд)
        for (Measurement m : allMeasurements) {
            boolean matches = true;

            // Проверяем фильтр по параметру
            if (paramFilter != null && m.param != paramFilter) {
                matches = false;
            }

            if (matches) {
                measurements.add(m);
            }
        }

        // Сортировка по убыванию времени (сначала новые)
        measurements.sort((m1, m2) -> m2.measuredAt.compareTo(m1.measuredAt));

        if (last != null && last < measurements.size()) {
            measurements = measurements.subList(0, last);
        }

        if (measurements.isEmpty()) {
            System.out.println("Нет измерений для отображения");
            return;
        }

        System.out.printf("%-5s %-12s %-10s %-8s %-15s %-20s%n",
                "ID", "Param", "Value", "Unit", "Method", "Time");
        System.out.println("-".repeat(80));

        for (Measurement m : measurements) {
            String timeStr = m.measuredAt.toString().replace("T", " ").substring(0, 19);
            System.out.printf("%-5d %-12s %-10.2f %-8s %-15s %-20s%n",
                    m.id, m.param, m.value, m.unit, truncate(m.method, 15), timeStr);
        }
    }

    private void measStats(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("использование: meas_stats <sample_id> <param>");
        }

        long sampleId;
        try {
            sampleId = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id образца должен быть числом");
        }

        String paramStr = args[1].toUpperCase();
        MeasurementParam param;
        try {
            param = MeasurementParam.valueOf(paramStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("неизвестный параметр, используйте PH, CONDUCTIVITY, TURBIDITY, NITRATE");
        }

        List<Measurement> measurements = measurementRepository.findBySampleIdAndParam(sampleId, param);

        if (measurements.isEmpty()) {
            throw new IllegalArgumentException("нет измерений " + param + " для sample=" + sampleId);
        }

        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (Measurement m : measurements) {
            double val = m.value;
            sum += val;
            if (val < min) min = val;
            if (val > max) max = val;
        }

        double avg = sum / measurements.size();

        System.out.println("count: " + measurements.size());
        System.out.println("min: " + min);
        System.out.println("max: " + max);
        System.out.println("avg: " + avg);
    }
    private void protCreate() {
        System.out.println("Создание нового протокола:");

        System.out.print("Название протокола: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("имя протокола не может быть пустым");
        }
        if (name.length() > 128) {
            throw new IllegalArgumentException("имя слишком длинное (макс. 128)");
        }

        System.out.print("Обязательные параметры (через запятую): ");
        String paramsLine = scanner.nextLine().trim();
        if (paramsLine.isEmpty()) {
            throw new IllegalArgumentException("нужно указать хотя бы один параметр");
        }

        String[] paramNames = paramsLine.split("\\s*,\\s*");
        Set<MeasurementParam> requiredParams = new HashSet<>();

        for (String p : paramNames) {
            try {
                requiredParams.add(MeasurementParam.valueOf(p.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("неизвестный параметр: " + p +
                        ". Допустимые: PH, CONDUCTIVITY, TURBIDITY, NITRATE");
            }
        }

        Protocol protocol = new Protocol();
        protocol.name = name;
        protocol.requiredParams = requiredParams;
        protocol.ownerUsername = "SYSTEM";
        protocol.createdAt = Instant.now();
        protocol.updatedAt = protocol.createdAt;

        protocolRepository.save(protocol);
        System.out.println("OK protocol_id=" + protocol.id);
    }
    private void protApply(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("использование: prot_apply <protocol_id> <sample_id>");
        }

        long protocolId;
        long sampleId;

        try {
            protocolId = Long.parseLong(args[0]);
            sampleId = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id должен быть числом");
        }

        Protocol protocol = protocolRepository.findById(protocolId)
                .orElseThrow(() -> new IllegalArgumentException("протокол с id=" + protocolId + " не найден"));

        if (sampleRepository.findById(sampleId).isEmpty()) {
            throw new IllegalArgumentException("образец с id=" + sampleId + " не найден");
        }

        // Получаем все измерения образца
        List<Measurement> measurements = measurementRepository.findBySampleId(sampleId);

        // Собираем множество параметров, которые уже измерены
        Set<MeasurementParam> measuredParams = new HashSet<>();
        for (Measurement m : measurements) {
            measuredParams.add(m.param);
        }

        // Находим недостающие параметры
        Set<MeasurementParam> missing = new HashSet<>(protocol.requiredParams);
        missing.removeAll(measuredParams);

        if (missing.isEmpty()) {
            System.out.println("OK protocol is complete");
        } else {
            System.out.println("Missing params: " + missing.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(", ")));
        }
    }
    private void reagAdd() {
        System.out.println("Создание нового реактива:");

        System.out.print("Название: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("название не может быть пустым");
        }
        if (name.length() > 128) {
            throw new IllegalArgumentException("название слишком длинное (макс. 128)");
        }

        System.out.print("Формула (можно пусто): ");
        String formula = scanner.nextLine().trim();
        if (formula.isEmpty()) formula = null;

        System.out.print("CAS (можно пусто): ");
        String cas = scanner.nextLine().trim();
        if (cas.isEmpty()) cas = null;

        System.out.print("Класс опасности (можно пусто): ");
        String hazard = scanner.nextLine().trim();
        if (hazard.isEmpty()) hazard = null;

        Reagent reagent = new Reagent();
        reagent.name = name;
        reagent.formula = formula;
        reagent.cas = cas;
        reagent.hazardClass = hazard;
        reagent.ownerUsername = "SYSTEM";
        reagent.createdAt = Instant.now();
        reagent.updatedAt = reagent.createdAt;

        reagentRepository.save(reagent);
        System.out.println("OK reagent_id=" + reagent.id);
    }
    private void reagList(String[] args) {
        String query = null;

        for (int i = 0; i < args.length; i++) {
            if ("--q".equals(args[i])) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("не указан запрос");
                }
                query = args[++i];
                if (query.length() > 64) {
                    throw new IllegalArgumentException("запрос слишком длинный (макс. 64)");
                }
            } else {
                throw new IllegalArgumentException("неизвестная опция: " + args[i]);
            }
        }

        List<Reagent> reagents;
        if (query != null) {
            reagents = reagentRepository.search(query);
        } else {
            reagents = reagentRepository.findAll();
        }

        if (reagents.isEmpty()) {
            System.out.println("Реактивы не найдены");
            return;
        }

        System.out.printf("%-5s %-20s %-15s %-15s%n", "ID", "Name", "Formula", "CAS");
        System.out.println("-".repeat(60));

        for (Reagent r : reagents) {
            System.out.printf("%-5d %-20s %-15s %-15s%n",
                    r.id,
                    truncate(r.name, 20),
                    r.formula != null ? truncate(r.formula, 15) : "",
                    r.cas != null ? r.cas : "");
        }
    }
    private void batchAdd(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("использование: batch_add <reagent_id>");
        }

        long reagentId;
        try {
            reagentId = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id реактива должен быть числом");
        }

        Reagent reagent = reagentRepository.findById(reagentId)
                .orElseThrow(() -> new IllegalArgumentException("реактив с id=" + reagentId + " не найден"));

        System.out.println("Добавление партии к реактиву: " + reagent.name);

        System.out.print("Номер партии (label): ");
        String label = scanner.nextLine().trim();
        if (label.isEmpty()) {
            throw new IllegalArgumentException("номер партии не может быть пустым");
        }

        System.out.print("Начальное количество: ");
        String qtyStr = scanner.nextLine().trim();
        double initialQty;
        try {
            initialQty = Double.parseDouble(qtyStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("количество должно быть числом");
        }
        if (initialQty <= 0) {
            throw new IllegalArgumentException("количество должно быть положительным");
        }

        System.out.print("Единицы (g|mL): ");
        String unit = scanner.nextLine().trim();
        if (!unit.equals("g") && !unit.equals("mL")) {
            throw new IllegalArgumentException("единицы только g или mL");
        }

        System.out.print("Где хранится: ");
        String location = scanner.nextLine().trim();
        if (location.isEmpty()) {
            throw new IllegalArgumentException("место не может быть пустым");
        }

        System.out.print("Годен до (YYYY-MM-DD, можно пусто): ");
        String expiryStr = scanner.nextLine().trim();
        LocalDate expiryDate = null;
        if (!expiryStr.isEmpty()) {
            try {
                expiryDate = LocalDate.parse(expiryStr);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("неверный формат даты, используйте YYYY-MM-DD");
            }
        }

        ReagentBatch batch = new ReagentBatch();
        batch.reagentId = reagentId;
        batch.label = label;
        batch.initialQty = initialQty;
        batch.currentQty = initialQty;
        batch.unit = unit;
        batch.location = location;
        batch.expiryDate = expiryDate;
        batch.status = "ACTIVE";
        batch.createdAt = Instant.now();
        batch.updatedAt = batch.createdAt;

        batchRepository.save(batch);
        System.out.println("OK batch_id=" + batch.id);
    }
    private void batchList(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("использование: batch_list <reagent_id> [--active]");
        }

        long reagentId;
        try {
            reagentId = Long.parseLong(args[0]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("id реактива должен быть числом");
        }

        if (reagentRepository.findById(reagentId).isEmpty()) {
            throw new IllegalArgumentException("реактив с id=" + reagentId + " не найден");
        }

        boolean activeOnly = false;
        for (int i = 1; i < args.length; i++) {
            if ("--active".equals(args[i])) {
                activeOnly = true;
            } else {
                throw new IllegalArgumentException("неизвестная опция: " + args[i]);
            }
        }

        List<ReagentBatch> batches = batchRepository.findByReagentId(reagentId);

        if (activeOnly) {
            List<ReagentBatch> filtered = new ArrayList<>();
            for (ReagentBatch b : batches) {
                if ("ACTIVE".equals(b.status)) {
                    filtered.add(b);
                }
            }
            batches = filtered;
        }

        if (batches.isEmpty()) {
            System.out.println("Партии не найдены");
            return;
        }

        System.out.printf("%-5s %-15s %-10s %-5s %-10s %-8s %-12s%n",
                "ID", "Label", "Qty", "Unit", "Location", "Status", "Expires");
        System.out.println("-".repeat(75));

        for (ReagentBatch b : batches) {
            String expiryStr = b.expiryDate != null ? b.expiryDate.toString() : "";
            System.out.printf("%-5d %-15s %-10.1f %-5s %-10s %-8s %-12s%n",
                    b.id,
                    truncate(b.label, 15),
                    b.currentQty,
                    b.unit,
                    truncate(b.location, 10),
                    b.status,
                    expiryStr);
        }
    }
}
