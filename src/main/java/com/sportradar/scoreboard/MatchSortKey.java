package com.sportradar.scoreboard;

import java.time.LocalDateTime;
import java.util.Comparator;

//TODO: Consider required fields validation
//TODO: Consider an edge case: two matches started in the same time and have the same score
public record MatchSortKey(int totalScore, LocalDateTime startDate) implements Comparable<MatchSortKey>{
    @Override
    public int compareTo(MatchSortKey o) {
        return Comparator.comparing(MatchSortKey::totalScore)
                .thenComparing(MatchSortKey::startDate).reversed().compare(this, o);
    }
}
