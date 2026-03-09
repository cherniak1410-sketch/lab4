
package ru.yourteam.lab.validator;

import ru.yourteam.lab.domain.MeasurementParam;

public class MeasurementValidator {

    public static void validateParam(String paramStr) {
        try {
            MeasurementParam.valueOf(paramStr);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("неизвестный параметр, допустимые: PH, CONDUCTIVITY, TURBIDITY, NITRATE");
        }
    }

    public static void validateValue(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("значение должно быть конечным числом");
        }
    }

    public static void validateUnit(String unit) {
        if (unit == null || unit.trim().isEmpty()) {
            throw new IllegalArgumentException("единицы не могут быть пустыми");
        }
        if (unit.length() > 16) {
            throw new IllegalArgumentException("единицы слишком длинные (макс. 16)");
        }
    }

    public static void validateMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            throw new IllegalArgumentException("метод не может быть пустым");
        }
        if (method.length() > 64) {
            throw new IllegalArgumentException("метод слишком длинный (макс. 64)");
        }
    }
}