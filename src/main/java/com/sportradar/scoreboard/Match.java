package com.sportradar.scoreboard;

import java.time.LocalDateTime;

//TODO: Consider required fields validations
public record Match(long id, Score homeTeamScore, Score awayTeamScore, LocalDateTime startDate, boolean finished) {

    public Match createMatchCopy(int homeTeamScore, int awayTeamScore) {
        return new Match(id, new Score(homeTeamScore().country(), homeTeamScore),
                new Score(awayTeamScore().country(), awayTeamScore), startDate, finished);
    }

    public Match createFinishedMatchCopy() {
        return new Match(id, homeTeamScore, awayTeamScore, startDate, true);
    }

    public MatchSortKey compoundSortKey() {
        return new MatchSortKey(homeTeamScore().score() + awayTeamScore().score(), startDate());
    }

}
