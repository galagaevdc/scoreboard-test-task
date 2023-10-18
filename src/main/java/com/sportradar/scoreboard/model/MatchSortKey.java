package com.sportradar.scoreboard.model;

import java.util.Comparator;
import java.util.Objects;

public record MatchSortKey(int totalScore, Match match) implements Comparable<MatchSortKey> {
    public MatchSortKey {
        Objects.requireNonNull(match, "match cannot be null");
    }

    @Override
    public int compareTo(MatchSortKey o) {
        return Comparator.comparing(MatchSortKey::totalScore)
                .thenComparing((o1, o2) -> o1.match().startDate().compareTo(o2.match.startDate()))
                .thenComparing((o1, o2) -> o2.match().hashCode() - o1.match().hashCode())
                .reversed().compare(this, o);
    }
}
