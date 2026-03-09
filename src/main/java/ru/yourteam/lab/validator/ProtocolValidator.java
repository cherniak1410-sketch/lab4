package ru.yourteam.lab.validator;

import java.util.Set;

public class ProtocolValidator {

    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("имя протокола не может быть пустым");
        }
        if (name.length() > 128) {
            throw new IllegalArgumentException("имя слишком длинное (макс. 128)");
        }
    }

    public static void validateRequiredParams(Set<?> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("нужно указать хотя бы один параметр");
        }
    }
}
