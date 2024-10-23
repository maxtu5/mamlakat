package com.tuiken.mamlakat.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class Converters {
    public static LocalDate toLocalDate(Instant source) {
        return source.atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
