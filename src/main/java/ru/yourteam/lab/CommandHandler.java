package ru.yourteam.lab;

import ru.yourteam.lab.domain.*;
import ru.yourteam.lab.service.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.*;

public class CommandHandler {
    private final Scanner scanner;
    private final SampleService sampleService;
    private final MeasurementService measurementService;
    private final ProtocolService protocolService;

    public CommandHandler(Scanner scanner,
                          SampleService sampleService,
                          MeasurementService measurementService,
                          ProtocolService protocolService) {
        this.scanner = scanner;
        this.sampleService = sampleService;
        this.measurementService = measurementService;
        this.protocolService = protocolService;
    }
    private String formatInstant(Instant instant) {
        if (instant == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }
    public void executeCommand(String line) {
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

            //Помощь
            case "help":
                printHelp();
                break;


            default:
                System.out.println("Неизвестная команда: " + command);
        }
    }
    private void printHelp() {
        System.out.println("\n=== ДОСТУПНЫЕ КОМАНДЫ ===\n");

        System.out.println("--- Образцы (Samples) ---");
        System.out.println("  sample_add                      - создать новый образец");
        System.out.println("  sample_list [--status ACTIVE|ARCHIVED] [--mine] - список образцов");
        System.out.println("  sample_show <id>                - показать образец");
        System.out.println("  sample_update <id> field=value  - изменить образец");
        System.out.println("  sample_archive <id>             - архивировать образец");

        System.out.println("\n--- Измерения (Measurements) ---");
        System.out.println("  meas_add <sample_id>            - добавить измерение");
        System.out.println("  meas_list <sample_id> [--param PARAM] [--last N] - список измерений");
        System.out.println("  meas_stats <sample_id> <param>  - статистика по параметру");

        System.out.println("\n--- Протоколы (Protocols) ---");
        System.out.println("  prot_create                     - создать протокол");
        System.out.println("  prot_apply <protocol_id> <sample_id> - проверить протокол");

        System.out.println("\n--- Прочее ---");
        System.out.println("  help                            - показать эту справку");
        System.out.println("  exit                            - выйти из программы");

        System.out.println("\n=== КОНЕЦ СПРАВКИ ===\n");
    }
    private void sampleAdd() {
        System.out.println("Создание нового образца:");

        String name = readNonEmptyLine("Название: ", "название не может быть пустым");
        String type = readNonEmptyLine("Тип: ", "тип не может быть пустым");
        String location = readNonEmptyLine("Место: ", "место не может быть пустым");

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
        System.out.println("created: " + formatInstant(sample.getCreatedAt()));
        System.out.println("updated: " + formatInstant(sample.getUpdatedAt()));
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

        // Параметр (с проверкой)
        String paramStr;
        while (true) {
            System.out.print("Параметр (PH/CONDUCTIVITY/TURBIDITY/NITRATE): ");
            paramStr = scanner.nextLine().trim().toUpperCase();
            try {
                MeasurementParam.valueOf(paramStr);
                break;
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: неизвестный параметр. Допустимые: PH, CONDUCTIVITY, TURBIDITY, NITRATE");
            }
        }

        // Значение (число)
        double value;
        while (true) {
            System.out.print("Значение: ");
            String valueStr = scanner.nextLine().trim();
            try {
                value = Double.parseDouble(valueStr);
                if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                    break;
                }
            } catch (NumberFormatException e) {
                // продолжаем цикл
            }
            System.out.println("Ошибка: значение должно быть числом");
        }

        // Единицы
        String unit = readNonEmptyLine("Единицы: ", "единицы не могут быть пустыми");

        // Метод
        String method = readNonEmptyLine("Метод: ", "метод не может быть пустым");

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
            String timeStr = formatInstant(m.getMeasuredAt());
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

        String name = readNonEmptyLine("Название протокола: ", "имя протокола не может быть пустым");

        Set<MeasurementParam> requiredParams = new HashSet<>();
        while (true) {
            System.out.print("Обязательные параметры (через запятую): ");
            String paramsLine = scanner.nextLine().trim();
            if (paramsLine.isEmpty()) {
                System.out.println("Ошибка: нужно указать хотя бы один параметр");
                continue;
            }

            String[] paramNames = paramsLine.split("\\s*,\\s*");
            boolean allValid = true;

            for (String p : paramNames) {
                try {
                    requiredParams.add(MeasurementParam.valueOf(p.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: неизвестный параметр '" + p + "'. Допустимые: PH, CONDUCTIVITY, TURBIDITY, NITRATE");
                    allValid = false;
                    requiredParams.clear();
                    break;
                }
            }

            if (allValid && !requiredParams.isEmpty()) {
                break;
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
    private String readNonEmptyLine(String prompt, String errorMessage) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("Ошибка: " + errorMessage + " Попробуйте снова.");
        }
    }
}
