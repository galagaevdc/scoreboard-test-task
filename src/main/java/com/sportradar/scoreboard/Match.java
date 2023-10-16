package com.sportradar.scoreboard;

import java.time.LocalDateTime;
import java.util.Objects;

//TODO: Consider required fields validations
public record Match(long id, Score homeTeamScore, Score awayTeamScore, LocalDateTime startDate, boolean finished) {

    public Match createMatchCopy() {
        return new Match(id, homeTeamScore, awayTeamScore, startDate, finished);
    }

    public Match createFinishedMatchCopy() {
        return new Match(id, homeTeamScore, awayTeamScore, startDate, true);
    }

    public MatchSortKey compoundSortKey() {
        return new MatchSortKey(homeTeamScore().score() + awayTeamScore().score(), startDate());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Match) obj;
        return this.id == that.id &&
                Objects.equals(this.homeTeamScore, that.homeTeamScore) &&
                Objects.equals(this.awayTeamScore, that.awayTeamScore) &&
                Objects.equals(this.startDate, that.startDate) &&
                this.finished == that.finished;
    }

    @Override
    public String toString() {
        return "Match[" +
                "id=" + id + ", " +
                "homeTeamScore=" + homeTeamScore + ", " +
                "awayTeamScore=" + awayTeamScore + ", " +
                "startDate=" + startDate + ", " +
                "finished=" + finished + ']';
    }

}
