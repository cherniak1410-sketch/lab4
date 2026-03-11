package ru.yourteam.lab;

import ru.yourteam.lab.domain.*;
import ru.yourteam.lab.service.*;

import java.time.Instant;
import java.util.*;

public class LabApp {
    private final Scanner scanner = new Scanner(System.in);
    private final SampleService sampleService = new SampleService();
    private final MeasurementService measurementService = new MeasurementService();
    private final ProtocolService protocolService = new ProtocolService();

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
            // === Образцы (Samples) ===
            case "sample_add":
                sampleAdd();
                break;
            case "sample_list":
                sampleList(args);
                break;
            case "sample_show":
                sampleShow(args);
                break;
            case "sample_update":
                sampleUpdate(args);
                break;
            case "sample_archive":
                sampleArchive(args);
                break;

            // === Измерения (Measurements) ===
            case "meas_add":
                measAdd(args);
                break;
            case "meas_list":
                measList(args);
                break;
            case "meas_stats":
                measStats(args);
                break;

            // === Протоколы (Protocols) ===
            case "prot_create":
                protCreate();
                break;
            case "prot_apply":
                protApply(args);
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

        Sample sample = sampleService.add(name, type, location);
        System.out.println("OK sample_id=" + sample.getId());
    }

    private void sampleList(String[] args) {
        SampleStatus statusFilter = null;
        boolean mine = false;

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

        List<Sample> allSamples = sampleService.getAll();
        List<Sample> samples = new ArrayList<>();

        for (Sample s : allSamples) {
            boolean matches = statusFilter == null || s.getStatus() == statusFilter;

            if (matches && mine) {
                String currentUser = "SYSTEM";
                if (!currentUser.equals(s.getOwnerUsername())) {
                    matches = false;
                }
            }

            if (matches) {
                samples.add(s);
            }
        }

        System.out.printf("%-5s %-20s %-10s %-10s %-8s%n",
                "ID", "Name", "Type", "Location", "Status");
        System.out.println("-".repeat(60));

        for (Sample s : samples) {
            System.out.printf("%-5d %-20s %-10s %-10s %-8s%n",
                    s.getId(),
                    truncate(s.getName(), 20),
                    truncate(s.getType(), 10),
                    truncate(s.getLocation(), 10),
                    s.getStatus());
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

        Sample sample = sampleService.getById(id);
        List<Measurement> measurements = measurementService.getBySampleId(id);

        Set<MeasurementParam> params = new HashSet<>();
        for (Measurement m : measurements) {
            params.add(m.getParam());
        }

        System.out.println("Sample #" + sample.getId());
        System.out.println("name: " + sample.getName());
        System.out.println("type: " + sample.getType());
        System.out.println("location: " + sample.getLocation());
        System.out.println("status: " + sample.getStatus());
        System.out.println("owner: " + sample.getOwnerUsername());
        System.out.println("created: " + sample.getCreatedAt());
        System.out.println("updated: " + sample.getUpdatedAt());
        System.out.println("measurements: " + measurements.size());

        if (params.isEmpty()) {
            System.out.println("params: ");
        } else {
            System.out.println("params: " + String.join(", ",
                    params.stream().map(Enum::name).toArray(String[]::new)));
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

            sampleService.update(id, field, value);
        }

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

        sampleService.archive(id);
        System.out.println("OK sample " + id + " archived");
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

        Sample sample = sampleService.getById(sampleId);

        if (sample.getStatus() == SampleStatus.ARCHIVED) {
            throw new IllegalArgumentException("нельзя добавлять измерения к ARCHIVED образцу");
        }

        System.out.println("Добавление измерения к образцу #" + sampleId);

        System.out.print("Параметр (PH/CONDUCTIVITY/TURBIDITY/NITRATE): ");
        String paramStr = scanner.nextLine().trim().toUpperCase();

        System.out.print("Значение: ");
        String valueStr = scanner.nextLine().trim();
        double value;
        try {
            value = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("значение должно быть числом");
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

        Measurement measurement = measurementService.add(sampleId, paramStr, value, unit, method);
        System.out.println("OK measurement_id=" + measurement.getId());
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

        sampleService.getById(sampleId); // проверка существования

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

        List<Measurement> measurements;
        if (paramFilter != null) {
            measurements = measurementService.getBySampleIdAndParam(sampleId, paramFilter);
        } else {
            measurements = measurementService.getBySampleId(sampleId);
        }

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
            String timeStr = m.getMeasuredAt().toString().replace("T", " ").substring(0, 19);
            System.out.printf("%-5d %-12s %-10.2f %-8s %-15s %-20s%n",
                    m.getId(), m.getParam(), m.getValue(), m.getUnit(),
                    truncate(m.getMethod(), 15), timeStr);
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

        Map<String, Object> stats = measurementService.getStats(sampleId, param);

        System.out.println("count: " + stats.get("count"));
        System.out.println("min: " + stats.get("min"));
        System.out.println("max: " + stats.get("max"));
        System.out.println("avg: " + stats.get("avg"));
    }

    private void protCreate() {
        System.out.println("Создание нового протокола:");

        System.out.print("Название протокола: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("имя протокола не может быть пустым");
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

        Protocol protocol = protocolService.add(name, requiredParams);
        System.out.println("OK protocol_id=" + protocol.getId());
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

        protocolService.getById(protocolId); // проверка существования
        sampleService.getById(sampleId); // проверка существования

        List<Measurement> measurements = measurementService.getBySampleId(sampleId);

        Set<MeasurementParam> measuredParams = new HashSet<>();
        for (Measurement m : measurements) {
            measuredParams.add(m.getParam());
        }

        Set<MeasurementParam> missing = protocolService.checkCompliance(protocolId, measuredParams);

        if (missing.isEmpty()) {
            System.out.println("OK protocol is complete");
        } else {
            System.out.println("Missing params: " + String.join(", ",
                    missing.stream().map(Enum::name).toArray(String[]::new)));
        }
    }

    private String truncate(String s, int maxLength) {
        if (s == null) return "";
        if (s.length() <= maxLength) return s;
        return s.substring(0, maxLength - 3) + "...";
    }
}