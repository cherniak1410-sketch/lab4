package ru.yourteam.lab.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.yourteam.lab.auth.AuthService;
import ru.yourteam.lab.auth.PasswordHasher;

import java.io.IOException;

/**
 * Отдельное окно авторизации (Stage).
 * Показывается при запуске приложения.
 * Главное окно открывается только после успешного входа.
 *
 * Поддерживает: вход в существующий аккаунт и быструю регистрацию.
 *
 * ВНИМАНИЕ: UserStorage больше не используется, пользователи хранятся в БД.
 * Регистрация и вход работают через AuthService, который синхронизируется с БД.
 */
public class LoginWindow {

    private final AuthService authService;
    private final Stage stage;

    /** true если пользователь успешно вошёл или зарегистрировался. */
    private boolean loginSuccess = false;

    public LoginWindow(AuthService authService) {
        this.authService = authService;
        this.stage = new Stage();
        stage.setTitle("Вход в систему");
        stage.setResizable(false);
        stage.initModality(Modality.APPLICATION_MODAL);
    }

    /**
     * Показывает окно и ждёт закрытия.
     * @return true если пользователь успешно вошёл
     */
    public boolean showAndWait() {
        stage.setScene(buildScene());
        stage.showAndWait();
        return loginSuccess;
    }

    private Scene buildScene() {
        // --- Поля ввода ---
        Label labelLogin    = new Label("Логин:");
        Label labelPassword = new Label("Пароль:");

        TextField loginField       = new TextField();
        loginField.setPromptText("Ваш логин");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Ваш пароль");

        // --- Сообщение об ошибке ---
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");
        errorLabel.setWrapText(true);

        // --- Кнопки ---
        Button btnLogin    = new Button("Войти");
        Button btnRegister = new Button("Зарегистрироваться");

        btnLogin.setDefaultButton(true);
        btnLogin.setPrefWidth(160);
        btnRegister.setPrefWidth(160);

        btnLogin.setOnAction(e -> {
            String login    = loginField.getText().trim();
            String password = passwordField.getText();
            try {
                authService.login(login, password);
                loginSuccess = true;
                stage.close();
            } catch (IllegalArgumentException ex) {
                errorLabel.setText("Ошибка: " + ex.getMessage());
                passwordField.clear();
            }
        });

        btnRegister.setOnAction(e -> {
            String login    = loginField.getText().trim();
            String password = passwordField.getText();
            try {
                authService.register(login, password);
                // После регистрации пользователь уже сохранён в БД через UserRepository
                // Теперь входим
                authService.login(login, password);
                loginSuccess = true;
                stage.close();
            } catch (IllegalArgumentException ex) {
                errorLabel.setText("Ошибка: " + ex.getMessage());
            }
        });

        // --- Сетка полей ---
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        grid.add(labelLogin,    0, 0);
        grid.add(loginField,    1, 0);
        grid.add(labelPassword, 0, 1);
        grid.add(passwordField, 1, 1);

        GridPane.setHgrow(loginField,    Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);

        // --- Кнопки в ряд ---
        HBox buttons = new HBox(10, btnLogin, btnRegister);
        buttons.setAlignment(Pos.CENTER);

        // --- Подпись ---
        Label hint = new Label("Новый пользователь? Введите логин/пароль и нажмите «Зарегистрироваться».");
        hint.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        hint.setWrapText(true);

        // --- Главный контейнер ---
        VBox root = new VBox(16, grid, buttons, errorLabel, hint);
        root.setPadding(new Insets(16));
        root.setAlignment(Pos.TOP_CENTER);
        root.setPrefWidth(380);

        return new Scene(root);
    }
}