package com.sportradar.scoreboard.exception;

public class MatchNotFoundException extends ScoreboardException {
    private final Long matchId;

    public MatchNotFoundException(Long matchId) {
        super("Match " + matchId + " not found");
        this.matchId = matchId;
    }

    public Long getMatchId() {
        return matchId;
    }
}
