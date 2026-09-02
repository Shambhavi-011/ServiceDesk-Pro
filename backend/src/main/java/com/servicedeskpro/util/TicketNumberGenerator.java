package com.servicedeskpro.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class TicketNumberGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private TicketNumberGenerator() {}

    public static String generate() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String randomSuffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return String.format("TCK-%s-%s", datePart, randomSuffix);
    }
}