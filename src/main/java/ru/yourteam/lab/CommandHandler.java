package ru.yourteam.lab;

import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.domain.*;
import ru.yourteam.lab.service.*;
import ru.yourteam.lab.storage.FileStorage;
import ru.yourteam.lab.storage.UserStorage;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Обрабатывает команды CLI.
 *
 * Этап 5:
 *  - Команды изменения данных (sample_add, sample_update, sample_archive,
 *    meas_add, prot_create) требуют авторизации — проверка делегируется сервисам.
 *  - Команды чтения (sample_list, sample_show, meas_list, meas_stats,
 *    prot_apply, save, load) доступны всем.
 *  - Новые команды: register, login, logout, whoami.
 */
public class CommandHandler {

    private final Scanner scanner;
    private final SampleService sampleService;
    private final MeasurementService measurementService;
    private final ProtocolService protocolService;
    private final AuthService authService;
    private final FileStorage fileStorage;
    private final UserStorage userStorage;

    public CommandHandler(Scanner scanner,
                          SampleService sampleService,
                          MeasurementService measurementService,
                          ProtocolService protocolService,
                          AuthService authService,
                          UserStorage userStorage) {
        this.scanner = scanner;
        this.sampleService = sampleService;
        this.measurementService = measurementService;
        this.protocolService = protocolService;
        this.authService = authService;
        this.fileStorage = new FileStorage();
        this.userStorage = userStorage;
    }

    // -------------------------------------------------------------------------
    // Диспетчер команд
    // -------------------------------------------------------------------------

    public void executeCommand(String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        switch (command) {
            // --- Авторизация ---
            case "register": cmdRegister();       break;
            case "login":    cmdLogin();          break;
            case "logout":   cmdLogout();         break;
            case "whoami":   cmdWhoami();         break;

            // --- Образцы ---
            case "sample_add":     sampleAdd();          break;
            case "sample_list":    sampleList(args);     break;
            case "sample_show":    sampleShow(args);     break;
            case "sample_update":  sampleUpdate(args);   break;
            case "sample_archive": sampleArchive(args);  break;

            // --- Измерения ---
            case "meas_add":   measAdd(args);   break;
            case "meas_list":  measList(args);  break;
            case "meas_stats": measStats(args); break;

            // --- Протоколы ---
            case "prot_create": protCreate();       break;
            case "prot_apply":  protApply(args);    break;

            // --- Файлы ---
            case "save": save(args); break;
            case "load": load(args); break;

            case "help": printHelp(); break;

            default:
                System.out.println("Неизвестная команда: " + command + ". Введите help для справки.");
        }
    }

    // =========================================================================
    // Авторизация
    // =========================================================================

    private void cmdRegister() {
        System.out.println("Регистрация нового пользователя:");
        String login = readNonEmptyLine("Логин: ", "логин не может быть пустым");
        String password = readNonEmptyLine("Пароль: ", "пароль не может быть пустым");

        authService.register(login, password);

        // Сразу сохраняем пользователей в файл
        saveUsersQuietly();

        System.out.println("OK пользователь «" + login + "» зарегистрирован. Войдите командой login.");
    }

    private void cmdLogin() {
        String login    = readNonEmptyLine("Логин: ", "логин не может быть пустым");
        String password = readNonEmptyLine("Пароль: ", "пароль не может быть пустым");

        authService.login(login, password);
        System.out.println("OK добро пожаловать, " + login + "!");
    }

    private void cmdLogout() {
        if (!authService.isLoggedIn()) {
            System.out.println("Вы не авторизованы.");
            return;
        }
        String name = authService.getCurrentUsername();
        authService.logout();
        System.out.println("OK выход из системы. До свидания, " + name + "!");
    }

    private void cmdWhoami() {
        if (authService.isLoggedIn()) {
            System.out.println("Текущий пользователь: " + authService.getCurrentUsername());
        } else {
            System.out.println("Вы не авторизованы.");
        }
    }

    // =========================================================================
    // Образцы
    // =========================================================================

    private void sampleAdd() {
        // requireLogin проверяется внутри sampleService.add()
        System.out.println("Создание нового образца:");
        String name     = readNonEmptyLine("Название: ", "название не может быть пустым");
        String type     = readNonEmptyLine("Тип: ", "тип не может быть пустым");
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
                    if (i + 1 >= args.length) throw new IllegalArgumentException("не указан статус");
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

        final SampleStatus finalStatus = statusFilter;
        final String currentUsername = authService.getCurrentUsername();
        final boolean finalMine = mine;

        List<Sample> samples = sampleService.getAll().stream()
                .filter(s -> finalStatus == null || s.getStatus() == finalStatus)
                .filter(s -> !finalMine || currentUsername.equals(s.getOwnerUsername()))
                .collect(Collectors.toList());

        System.out.printf("%-5s %-22s %-10s %-12s %-8s %-12s%n",
                "ID", "Название", "Тип", "Место", "Статус", "Владелец");
        System.out.println("-".repeat(75));

        for (Sample s : samples) {
            System.out.printf("%-5d %-22s %-10s %-12s %-8s %-12s%n",
                    s.getId(),
                    truncate(s.getName(), 22),
                    truncate(s.getType(), 10),
                    truncate(s.getLocation(), 12),
                    s.getStatus(),
                    s.getOwnerUsername());
        }

        if (samples.isEmpty()) System.out.println("Нет образцов для отображения.");
    }

    private void sampleShow(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("использование: sample_show <id>");
        long id = parseLong(args[0], "id");

        Sample sample = sampleService.getById(id);
        List<Measurement> measurements = measurementService.getBySampleId(id);
        Set<MeasurementParam> params = measurements.stream()
                .map(Measurement::getParam).collect(Collectors.toSet());

        System.out.println("Sample #" + sample.getId());
        System.out.println("name:         " + sample.getName());
        System.out.println("type:         " + sample.getType());
        System.out.println("location:     " + sample.getLocation());
        System.out.println("status:       " + sample.getStatus());
        System.out.println("owner:        " + sample.getOwnerUsername());
        System.out.println("measurements: " + measurements.size());
        System.out.println("params:       " + params.stream().map(Enum::name)
                .sorted().collect(Collectors.joining(", ")));
    }

    private void sampleUpdate(String[] args) {
        if (args.length < 2) throw new IllegalArgumentException("использование: sample_update <id> field=value ...");
        long id = parseLong(args[0], "id");

        for (int i = 1; i < args.length; i++) {
            String[] kv = args[i].split("=", 2);
            if (kv.length != 2) throw new IllegalArgumentException("формат: field=value, получено: " + args[i]);
            sampleService.update(id, kv[0], kv[1]);
        }
        System.out.println("OK");
    }

    private void sampleArchive(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("использование: sample_archive <id>");
        long id = parseLong(args[0], "id");
        sampleService.archive(id);
        System.out.println("OK sample " + id + " archived");
    }

    // =========================================================================
    // Измерения
    // =========================================================================

    private void measAdd(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("использование: meas_add <sample_id>");
        long sampleId = parseLong(args[0], "sample_id");

        Sample sample = sampleService.getById(sampleId);
        if (sample.getStatus() == SampleStatus.ARCHIVED) {
            throw new IllegalArgumentException("нельзя добавлять измерения к ARCHIVED образцу");
        }

        String paramStr = readParamLine();
        double value    = readDoubleLine("Значение: ");
        String unit     = readNonEmptyLine("Единицы: ", "единицы не могут быть пустыми");
        String method   = readNonEmptyLine("Метод: ", "метод не может быть пустым");

        Measurement m = measurementService.add(sampleId, paramStr, value, unit, method);
        System.out.println("OK measurement_id=" + m.getId());
    }

    private void measList(String[] args) {
        if (args.length < 1) throw new IllegalArgumentException("использование: meas_list <sample_id> [--param PARAM] [--last N]");
        long sampleId = parseLong(args[0], "sample_id");

        sampleService.getById(sampleId); // проверка существования

        MeasurementParam paramFilter = null;
        int lastN = Integer.MAX_VALUE;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--param":
                    if (i + 1 >= args.length) throw new IllegalArgumentException("не указан параметр");
                    try {
                        paramFilter = MeasurementParam.valueOf(args[++i].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("неизвестный параметр");
                    }
                    break;
                case "--last":
                    if (i + 1 >= args.length) throw new IllegalArgumentException("не указано N");
                    lastN = (int) parseLong(args[++i], "N");
                    break;
                default:
                    throw new IllegalArgumentException("неизвестная опция: " + args[i]);
            }
        }

        final MeasurementParam pf = paramFilter;
        List<Measurement> list = measurementService.getBySampleId(sampleId).stream()
                .filter(m -> pf == null || m.getParam() == pf)
                .limit(lastN)
                .collect(Collectors.toList());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

        System.out.printf("%-5s %-14s %-8s %-6s %-12s %-16s%n",
                "ID", "Параметр", "Знач.", "Ед.", "Метод", "Время");
        System.out.println("-".repeat(70));
        for (Measurement m : list) {
            System.out.printf("%-5d %-14s %-8.4f %-6s %-12s %-16s%n",
                    m.getId(), m.getParam(), m.getValue(),
                    truncate(m.getUnit(), 6), truncate(m.getMethod(), 12),
                    fmt.format(m.getMeasuredAt()));
        }
        if (list.isEmpty()) System.out.println("Нет измерений.");
    }

    private void measStats(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("использование: meas_stats <sample_id> <param>");
        long sampleId = parseLong(args[0], "sample_id");
        MeasurementParam param;
        try {
            param = MeasurementParam.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("неизвестный параметр");
        }

        Map<String, Object> stats = measurementService.getStats(sampleId, param);
        System.out.println("count: " + stats.get("count"));
        System.out.printf("min:   %.4f%n", stats.get("min"));
        System.out.printf("max:   %.4f%n", stats.get("max"));
        System.out.printf("avg:   %.4f%n", stats.get("avg"));
    }

    // =========================================================================
    // Протоколы
    // =========================================================================

    private void protCreate() {
        System.out.println("Создание нового протокола:");
        String name = readNonEmptyLine("Название протокола: ", "имя протокола не может быть пустым");

        Set<MeasurementParam> params = new HashSet<>();
        while (true) {
            System.out.print("Обязательные параметры (через запятую, PH/CONDUCTIVITY/TURBIDITY/NITRATE): ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                System.out.println("Ошибка: нужно указать хотя бы один параметр.");
                continue;
            }
            boolean valid = true;
            params.clear();
            for (String p : line.split("\\s*,\\s*")) {
                try {
                    params.add(MeasurementParam.valueOf(p.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    System.out.println("Ошибка: неизвестный параметр '" + p + "'.");
                    valid = false;
                    break;
                }
            }
            if (valid && !params.isEmpty()) break;
        }

        Protocol protocol = protocolService.add(name, params);
        System.out.println("OK protocol_id=" + protocol.getId());
    }

    private void protApply(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("использование: prot_apply <protocol_id> <sample_id>");
        long protocolId = parseLong(args[0], "protocol_id");
        long sampleId   = parseLong(args[1], "sample_id");

        protocolService.getById(protocolId);
        sampleService.getById(sampleId);

        Set<MeasurementParam> measured = measurementService.getBySampleId(sampleId).stream()
                .map(Measurement::getParam).collect(Collectors.toSet());

        Set<MeasurementParam> missing = protocolService.checkCompliance(protocolId, measured);

        if (missing.isEmpty()) {
            System.out.println("OK protocol is complete");
        } else {
            System.out.println("Missing params: " + missing.stream()
                    .map(Enum::name).sorted().collect(Collectors.joining(", ")));
        }
    }

    // =========================================================================
    // Файлы
    // =========================================================================

    private void save(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("использование: save <path>");
        try {
            fileStorage.save(args[0],
                    sampleService.getStorage(),
                    measurementService.getStorage(),
                    protocolService.getStorage());
            System.out.println("OK сохранено в: " + args[0]);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка сохранения: " + e.getMessage());
        }
    }

    private void load(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("использование: load <path>");
        loadFromFile(args[0]);
    }

    public void loadFromFile(String path) {
        try {
            Map<String, Object> data = fileStorage.load(path);

            restoreSamples((List<Sample>) data.get("samples"));
            restoreMeasurements((List<Measurement>) data.get("measurements"));
            restoreProtocols((List<Protocol>) data.get("protocols"));

            System.out.println("OK загружено из: " + path);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка загрузки: " + e.getMessage());
        }
    }

    private void restoreSamples(List<Sample> samples) {
        if (samples == null) return;
        Map<Long, Sample> store = sampleService.getStorage();
        long maxId = sampleService.getNextId() - 1;
        for (Sample s : samples) {
            if (store.containsKey(s.getId())) {
                long newId = sampleService.getNextId();
                s.setId(newId);
                sampleService.setNextId(newId + 1);
            }
            store.put(s.getId(), s);
            if (s.getId() > maxId) maxId = s.getId();
        }
        sampleService.setNextId(maxId + 1);
    }

    private void restoreMeasurements(List<Measurement> measurements) {
        if (measurements == null) return;
        Map<Long, Measurement> store = measurementService.getStorage();
        long maxId = measurementService.getNextId() - 1;
        for (Measurement m : measurements) {
            if (store.containsKey(m.getId())) {
                long newId = measurementService.getNextId();
                m.setId(newId);
                measurementService.setNextId(newId + 1);
            }
            store.put(m.getId(), m);
            if (m.getId() > maxId) maxId = m.getId();
        }
        measurementService.setNextId(maxId + 1);
    }

    private void restoreProtocols(List<Protocol> protocols) {
        if (protocols == null) return;
        Map<Long, Protocol> store = protocolService.getStorage();
        long maxId = protocolService.getNextId() - 1;
        for (Protocol p : protocols) {
            if (store.containsKey(p.getId())) {
                long newId = protocolService.getNextId();
                p.setId(newId);
                protocolService.setNextId(newId + 1);
            }
            store.put(p.getId(), p);
            if (p.getId() > maxId) maxId = p.getId();
        }
        protocolService.setNextId(maxId + 1);
    }

    // =========================================================================
    // Справка
    // =========================================================================

    private void printHelp() {
        System.out.println("\n=== ДОСТУПНЫЕ КОМАНДЫ ===\n");

        System.out.println("--- Авторизация ---");
        System.out.println("  register          - зарегистрировать нового пользователя");
        System.out.println("  login             - войти в систему");
        System.out.println("  logout            - выйти из системы");
        System.out.println("  whoami            - показать текущего пользователя");

        System.out.println("\n--- Образцы (требуют login для изменений) ---");
        System.out.println("  sample_add                             - создать образец");
        System.out.println("  sample_list [--status ACTIVE|ARCHIVED] [--mine]");
        System.out.println("  sample_show <id>                       - карточка образца");
        System.out.println("  sample_update <id> field=value ...     - изменить образец");
        System.out.println("  sample_archive <id>                    - архивировать образец");

        System.out.println("\n--- Измерения (требуют login для добавления) ---");
        System.out.println("  meas_add <sample_id>                   - добавить измерение");
        System.out.println("  meas_list <sample_id> [--param P] [--last N]");
        System.out.println("  meas_stats <sample_id> <param>         - статистика");

        System.out.println("\n--- Протоколы (требуют login для создания) ---");
        System.out.println("  prot_create                            - создать протокол");
        System.out.println("  prot_apply <protocol_id> <sample_id>  - проверить протокол");

        System.out.println("\n--- Файл ---");
        System.out.println("  save <path>   - сохранить данные");
        System.out.println("  load <path>   - загрузить данные");

        System.out.println("\n  help   - эта справка");
        System.out.println("  exit   - выйти из программы");
        System.out.println();
    }

    // =========================================================================
    // Вспомогательные методы ввода
    // =========================================================================

    private String readNonEmptyLine(String prompt, String errorMsg) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("Ошибка: " + errorMsg);
        }
    }

    private String readParamLine() {
        while (true) {
            System.out.print("Параметр (PH/CONDUCTIVITY/TURBIDITY/NITRATE): ");
            String input = scanner.nextLine().trim().toUpperCase();
            try {
                MeasurementParam.valueOf(input);
                return input;
            } catch (IllegalArgumentException e) {
                System.out.println("Ошибка: неизвестный параметр. Допустимые: PH, CONDUCTIVITY, TURBIDITY, NITRATE");
            }
        }
    }

    private double readDoubleLine(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: значение должно быть числом.");
            }
        }
    }

    private long parseLong(String s, String fieldName) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " должен быть числом");
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    private void saveUsersQuietly() {
        try {
            userStorage.save(authService.getUsers());
        } catch (IOException e) {
            System.out.println("Предупреждение: не удалось сохранить пользователей: " + e.getMessage());
        }
    }
}
