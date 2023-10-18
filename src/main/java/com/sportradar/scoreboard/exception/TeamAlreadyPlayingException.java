package com.sportradar.scoreboard.exception;

public class TeamAlreadyPlayingException extends ScoreboardException {
    private final String alreadyPlayingCountry;

    public TeamAlreadyPlayingException(String alreadyPlayingCountry) {
        super("Team " + alreadyPlayingCountry + " is playing in another match");
        this.alreadyPlayingCountry = alreadyPlayingCountry;
    }

    public String getAlreadyPlayingCountry() {
        return alreadyPlayingCountry;
    }
}
