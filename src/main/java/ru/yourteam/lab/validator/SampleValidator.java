package ru.yourteam.lab.validator;

public class SampleValidator {

    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("название не может быть пустым");
        }
        if (name.length() > 128) {
            throw new IllegalArgumentException("название слишком длинное (макс. 128)");
        }
    }

    public static void validateType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("тип не может быть пустым");
        }
        if (type.length() > 64) {
            throw new IllegalArgumentException("тип слишком длинный (макс. 64)");
        }
    }

    public static void validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("место не может быть пустым");
        }
        if (location.length() > 64) {
            throw new IllegalArgumentException("место слишком длинное (макс. 64)");
        }
    }

    public static void validateStatus(String status) {
        if (!status.equals("ACTIVE") && !status.equals("ARCHIVED")) {
            throw new IllegalArgumentException("статус только ACTIVE или ARCHIVED");
        }
    }
}
