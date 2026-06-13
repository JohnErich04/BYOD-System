package com.example.service;

public class RegistrationValidator {

    private static final String ID_PATTERN = "^\\d{4}-\\d{5}-SR-\\d{1}$";
    private static final String NAME_PATTERN = "^[a-zA-Z\\s]+$";
    private static final String CONTACT_PATTERN = "^\\d{11}$";

    // Year: 1-9
    // Section: 1 or 2 digits (\d{1,2})
    private static final String YEAR_SEC_PATTERN = "^[1-9]-\\d{1,2}$";

    public static String validate(String sid, String fn, String ln, String contact, String yearSec) {
        if (!sid.matches(ID_PATTERN))
            return "Invalid Student ID! Format must be: 2020-00001-SR-0";
        if (!fn.matches(NAME_PATTERN) || !ln.matches(NAME_PATTERN))
            return "Names must contain letters only.";
        if (!yearSec.matches(YEAR_SEC_PATTERN))
            return "Year & Section must follow the format: X-X or X-XX (Year 1-9, e.g., 4-1, 4-10)";
        if (!contact.matches(CONTACT_PATTERN))
            return "Contact number must be exactly 11 digits.";

        return "VALID";
    }
}