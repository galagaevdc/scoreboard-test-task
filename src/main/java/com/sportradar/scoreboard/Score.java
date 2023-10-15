package com.sportradar.scoreboard;

/**
 * Represent score of the team in match
 * @param country country name
 * @param score score in the match
 */
//TODO: Consider required fields validation
public record Score(String country, int score) {
}
