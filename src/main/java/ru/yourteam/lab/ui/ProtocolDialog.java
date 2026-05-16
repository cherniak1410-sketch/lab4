package ru.yourteam.lab.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import ru.yourteam.lab.domain.MeasurementParam;

import java.util.EnumSet;
import java.util.Set;

/**
 * Диалог создания нового протокола.
 * Пользователь вводит название и выбирает обязательные параметры (чекбоксы).
 */
public class ProtocolDialog extends Dialog<ProtocolDialog.ProtocolFormData> {

    /** Данные из формы. */
    public record ProtocolFormData(String name, Set<MeasurementParam> params) {}

    public ProtocolDialog() {
        setTitle("Создать протокол");
        setHeaderText(null);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(okButton, cancelButton);

        TextField nameField = new TextField();
        nameField.setPromptText("Например: Water basic");

        // Чекбоксы для параметров
        CheckBox cbPh = new CheckBox("PH");
        CheckBox cbConductivity = new CheckBox("CONDUCTIVITY");
        CheckBox cbTurbidity = new CheckBox("TURBIDITY");
        CheckBox cbNitrate = new CheckBox("NITRATE");

        VBox checkboxes = new VBox(6, cbPh, cbConductivity, cbTurbidity, cbNitrate);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        grid.add(new Label("Название протокола:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Обязательные параметры:"), 0, 1);
        grid.add(checkboxes, 1, 1);

        getDialogPane().setContent(grid);
        nameField.requestFocus();

        setResultConverter(buttonType -> {
            if (buttonType == okButton) {
                Set<MeasurementParam> params = EnumSet.noneOf(MeasurementParam.class);
                if (cbPh.isSelected()) params.add(MeasurementParam.PH);
                if (cbConductivity.isSelected()) params.add(MeasurementParam.CONDUCTIVITY);
                if (cbTurbidity.isSelected()) params.add(MeasurementParam.TURBIDITY);
                if (cbNitrate.isSelected()) params.add(MeasurementParam.NITRATE);

                return new ProtocolFormData(nameField.getText().trim(), params);
            }
            return null;
        });
    }
}
