package ru.yourteam.lab;

import ru.yourteam.lab.domain.Sample;
import ru.yourteam.lab.domain.SampleStatus;
import ru.yourteam.lab.repository.SampleRepository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;

public class LabApp {
    private final Scanner scanner = new Scanner(System.in);
    private final SampleRepository sampleRepository = new SampleRepository();

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

        System.out.println("Sample #" + sample.id);
        System.out.println("name: " + sample.name);
        System.out.println("type: " + sample.type);
        System.out.println("location: " + sample.location);
        System.out.println("status: " + sample.status);
        System.out.println("owner: " + sample.ownerUsername);
        System.out.println("created: " + sample.createdAt);
        System.out.println("updated: " + sample.updatedAt);
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
}