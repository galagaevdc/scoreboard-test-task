package com.sportradar.scoreboard;

import java.time.LocalDateTime;
import java.util.Collection;

public interface Scoreboard {
    /**
     * Start match between home team and away team with initial score 0 - 0
     * @param homeTeamIsoCode home team iso code
     * @param awayTeamIsoCode away team iso code
     * @param startDate
     * @return id of started match. It could be used to update match score or finish match.
     * I don't know possible amount of the match, so I used long as id type, because it's
     * a common practice to use long as auto increment identifier in database
     */
    Long startMatch(String homeTeamIsoCode, String awayTeamIsoCode, LocalDateTime startDate);

    /**
     * Update score for the corresponding match
     * @param matchId identifier of the match
     * @param homeTeamScore home team score
     * @param awayTeamScore away team score
     */
    void updateScore(Long matchId, int homeTeamScore, int awayTeamScore);

    /**
     * Finish match and remove it from score board
     * @param matchId identifier of the match
     */
    void finishMatch(Long matchId);

    /**
     * Return list of running matches sorted by total score and then most recent started time.
     * I used long name to allow a library user understand logic of the method without checking code
     * or java docs
     *
     * @return list of sorted(first by total score, then by start time) running matches
     */
    Collection<Match> getRunningMatchesSortedByTotalScoreAndMostRecentStarted();
}
