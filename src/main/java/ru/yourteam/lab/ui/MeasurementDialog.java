package ru.yourteam.lab.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import ru.yourteam.lab.domain.MeasurementParam;
import ru.yourteam.lab.domain.Sample;

import java.util.List;

/**
 * Диалог добавления нового измерения.
 */
public class MeasurementDialog extends Dialog<MeasurementDialog.MeasurementFormData> {

    /** Данные из формы. */
    public record MeasurementFormData(
            long sampleId,
            String param,
            double value,
            String unit,
            String method
    ) {}

    public MeasurementDialog(List<Sample> samples) {
        setTitle("Добавить измерение");
        setHeaderText(null);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButton, cancelButton);

        // Выбор образца
        ComboBox<Sample> sampleCombo = new ComboBox<>();
        sampleCombo.getItems().addAll(samples);
        sampleCombo.setCellFactory(lv -> sampleCell());
        sampleCombo.setButtonCell(sampleCell());
        if (!samples.isEmpty()) sampleCombo.getSelectionModel().selectFirst();

        // Выбор параметра
        ComboBox<MeasurementParam> paramCombo = new ComboBox<>();
        paramCombo.getItems().addAll(MeasurementParam.values());
        paramCombo.getSelectionModel().selectFirst();

        TextField valueField = new TextField();
        valueField.setPromptText("Например: 7.12");

        TextField unitField = new TextField();
        unitField.setPromptText("Например: pH");

        TextField methodField = new TextField();
        methodField.setPromptText("Например: electrode");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        grid.add(new Label("Образец:"), 0, 0);
        grid.add(sampleCombo, 1, 0);
        grid.add(new Label("Параметр:"), 0, 1);
        grid.add(paramCombo, 1, 1);
        grid.add(new Label("Значение:"), 0, 2);
        grid.add(valueField, 1, 2);
        grid.add(new Label("Единицы:"), 0, 3);
        grid.add(unitField, 1, 3);
        grid.add(new Label("Метод:"), 0, 4);
        grid.add(methodField, 1, 4);

        getDialogPane().setContent(grid);
        valueField.requestFocus();

        setResultConverter(buttonType -> {
            if (buttonType == okButton) {
                Sample selectedSample = sampleCombo.getValue();
                MeasurementParam selectedParam = paramCombo.getValue();

                if (selectedSample == null || selectedParam == null) return null;

                String valueText = valueField.getText().trim();
                double value;
                try {
                    value = Double.parseDouble(valueText);
                } catch (NumberFormatException e) {
                    showError("Ошибка ввода", "Значение должно быть числом.");
                    return null;
                }

                return new MeasurementFormData(
                        selectedSample.getId(),
                        selectedParam.name(),
                        value,
                        unitField.getText().trim(),
                        methodField.getText().trim()
                );
            }
            return null;
        });
    }

    /** Ячейка ComboBox для отображения образца. */
    private ListCell<Sample> sampleCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Sample item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "#" + item.getId() + " " + item.getName());
            }
        };
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}

