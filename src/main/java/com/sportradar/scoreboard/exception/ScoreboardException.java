package com.sportradar.scoreboard.exception;

public abstract class ScoreboardException extends RuntimeException {
    public ScoreboardException(final String message) {
        super(message);
    }
}
