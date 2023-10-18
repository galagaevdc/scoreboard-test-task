package com.sportradar.scoreboard.exception;

public class ScoreNotPositiveException extends ScoreboardException {
    public ScoreNotPositiveException() {
        super("Score must be positive");
    }
}
