package com.sportradar.scoreboard.model;

import java.util.Objects;

/**
 * Represent score of the team in match
 * @param country country name
 * @param score score in the match
 */
public record Score(String country, int score) {
    public Score {
        Objects.requireNonNull(country, "country cannot be null");
    }
}
