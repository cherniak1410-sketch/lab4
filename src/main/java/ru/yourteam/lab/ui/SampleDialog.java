package ru.yourteam.lab.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import ru.yourteam.lab.domain.Sample;

/**
 * Диалог создания / редактирования образца.
 * Если передан существующий Sample — поля заполняются его данными (режим редактирования).
 * Если null — режим создания.
 */
public class SampleDialog extends Dialog<SampleDialog.SampleFormData> {

    /** Данные из формы. */
    public record SampleFormData(String name, String type, String location) {}

    public SampleDialog(Sample existing) {
        boolean isEdit = existing != null;
        setTitle(isEdit ? "Редактировать образец" : "Добавить образец");
        setHeaderText(null);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButton, cancelButton);

        // Поля ввода
        TextField nameField = new TextField(isEdit ? existing.getName() : "");
        nameField.setPromptText("Например: River water #3");

        TextField typeField = new TextField(isEdit ? existing.getType() : "");
        typeField.setPromptText("Например: water");

        TextField locationField = new TextField(isEdit ? existing.getLocation() : "");
        locationField.setPromptText("Например: Fridge-2");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Тип:"), 0, 1);
        grid.add(typeField, 1, 1);
        grid.add(new Label("Место хранения:"), 0, 2);
        grid.add(locationField, 1, 2);

        getDialogPane().setContent(grid);

        // Фокус на первое поле
        nameField.requestFocus();

        // Конвертер результата
        setResultConverter(buttonType -> {
            if (buttonType == okButton) {
                return new SampleFormData(
                        nameField.getText().trim(),
                        typeField.getText().trim(),
                        locationField.getText().trim()
                );
            }
            return null;
        });
    }
}
