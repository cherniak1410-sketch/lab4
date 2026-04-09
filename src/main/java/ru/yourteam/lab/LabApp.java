package ru.yourteam.lab;

import ru.yourteam.lab.service.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class LabApp {
    private final Scanner scanner = new Scanner(System.in);
    private final SampleService sampleService = new SampleService();
    private final MeasurementService measurementService = new MeasurementService();
    private final ProtocolService protocolService = new ProtocolService();
    private final CommandHandler commandHandler;

    public LabApp() {
        this.commandHandler = new CommandHandler(
                scanner, sampleService, measurementService, protocolService
        );

        // Автозагрузка
        if (loadFile != null && !loadFile.isEmpty()) {
            try {
                commandHandler.loadFromFile(loadFile);
                System.out.println("Автоматически загружены данные из: " + loadFile);
            } catch (Exception e) {
                System.out.println("Не удалось загрузить файл: " + e.getMessage());
            }
        }
    }

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
                commandHandler.executeCommand(line);
            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }
}