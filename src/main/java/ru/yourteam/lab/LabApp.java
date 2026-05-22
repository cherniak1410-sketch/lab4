package ru.yourteam.lab;

import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.service.*;
import ru.yourteam.lab.storage.UserStorage;

import java.io.IOException;
import java.util.Scanner;

/**
 * Точка входа CLI-приложения.
 *
 * Этап 5:
 *  - При старте загружаются пользователи из users.json (если файл есть).
 *  - Пользователь не обязан входить сразу; командная строка работает в любом случае,
 *    но команды изменения данных требуют авторизации.
 *  - При регистрации пользователь сохраняется в users.json немедленно.
 */
public class LabApp {

    private final Scanner scanner = new Scanner(System.in);
    private final AuthService authService = new AuthService();
    private final UserStorage userStorage;
    private final SampleService sampleService;
    private final MeasurementService measurementService;
    private final ProtocolService protocolService;
    private final CommandHandler commandHandler;

    public LabApp(String loadFile, String usersFile) {
        // Хранилище пользователей (отдельный файл)
        this.userStorage = usersFile != null ? new UserStorage(usersFile) : new UserStorage();

        // Загружаем пользователей при старте
        try {
            authService.loadUsers(userStorage.load());
            System.out.println("Пользователи загружены из: " + userStorage.getFilePath());
        } catch (IOException e) {
            System.out.println("Не удалось загрузить пользователей: " + e.getMessage());
        }

        // Сервисы с поддержкой авторизации
        this.sampleService      = new SampleService(authService);
        this.measurementService = new MeasurementService(authService);
        this.protocolService    = new ProtocolService(authService);

        this.commandHandler = new CommandHandler(
                scanner, sampleService, measurementService, protocolService,
                authService, userStorage
        );

        // Автозагрузка данных
        if (loadFile != null && !loadFile.isEmpty()) {
            try {
                commandHandler.loadFromFile(loadFile);
                System.out.println("Автоматически загружены данные из: " + loadFile);
            } catch (Exception e) {
                System.out.println("Не удалось загрузить файл данных: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        String loadFile  = args.length > 0 ? args[0] : null;
        String usersFile = args.length > 1 ? args[1] : null;

        LabApp app = new LabApp(loadFile, usersFile);
        app.run();
    }

    public void run() {
        System.out.println("\nЛабораторная система управления образцами");
        System.out.println("Введите help для списка команд. exit — для выхода.");
        System.out.println("Для изменения данных необходима авторизация (login).\n");

        while (true) {
            String prompt = authService.isLoggedIn()
                    ? "[" + authService.getCurrentUsername() + "]> "
                    : "[гость]> ";
            System.out.print(prompt);

            String line = scanner.nextLine().trim();

            if (line.equals("exit")) {
                System.out.println("До свидания!");
                break;
            }

            if (line.isEmpty()) continue;

            try {
                commandHandler.executeCommand(line);
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Непредвиденная ошибка: " + e.getMessage());
            }
        }
    }
}
