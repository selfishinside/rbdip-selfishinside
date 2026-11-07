package com.rbdip.bookstore.customer;

public record CustomerName(String firstName, String lastName) {

    public static CustomerName from(String fullName) {
        String normalized = fullName.trim().replaceAll("\\s+", " ");
        int separator = normalized.indexOf(' ');
        if (separator < 0) {
            return new CustomerName(normalized, "");
        }
        return new CustomerName(normalized.substring(0, separator), normalized.substring(separator + 1));
    }
}
