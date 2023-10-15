package com.sportradar.scoreboard;

import java.time.LocalDateTime;

//TODO: Consider required fields validations
public record Match(long id, Score homeTeamScore, Score awayTeamScore, LocalDateTime startDate) {

    public MatchSortKey compoundSortKey() {
        return new MatchSortKey(homeTeamScore().score() + awayTeamScore().score(), startDate());
    }
}
