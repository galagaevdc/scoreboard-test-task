package com.sportradar.scoreboard.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record Match(long id, Score homeTeamScore, Score awayTeamScore, LocalDateTime startDate, boolean finished) {

    public Match {
        Objects.requireNonNull(homeTeamScore, "homeTeamScore cannot be null");
        Objects.requireNonNull(awayTeamScore, "awayTeamScore cannot be null");
        Objects.requireNonNull(startDate, "startDate cannot be null");
    }

    public Match createMatchCopy(int homeTeamScore, int awayTeamScore) {
        return new Match(id, new Score(homeTeamScore().country(), homeTeamScore),
                new Score(awayTeamScore().country(), awayTeamScore), startDate, finished);
    }

    public Match createFinishedMatchCopy() {
        return new Match(id, homeTeamScore, awayTeamScore, startDate, true);
    }

    public MatchSortKey compoundSortKey() {
        return new MatchSortKey(homeTeamScore().score() + awayTeamScore().score(), this);
    }

}
