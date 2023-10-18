package com.sportradar.scoreboard.exception;

public class CountryNotSupportedException extends ScoreboardException {
    private final String notSupportedCode;

    public CountryNotSupportedException(final String notSupportedCode) {
        super("Country " + notSupportedCode + " is not supported");
        this.notSupportedCode = notSupportedCode;
    }

    public String getNotSupportedCode() {
        return notSupportedCode;
    }
}
