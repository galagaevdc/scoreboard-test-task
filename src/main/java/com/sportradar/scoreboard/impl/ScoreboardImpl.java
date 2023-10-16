package com.sportradar.scoreboard.impl;

import com.sportradar.scoreboard.Match;
import com.sportradar.scoreboard.MatchSortKey;
import com.sportradar.scoreboard.Score;
import com.sportradar.scoreboard.Scoreboard;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

//TODO: Implement concurrency support
public class ScoreboardImpl implements Scoreboard {
    /**
     * Initial idea that write pattern is extremely less frequent, that's why it will be better
     * to sort matches on insert
     */
    public Map<Long, Match> matchesById = new HashMap<>();
    public Map<MatchSortKey, Match> sortedMatches = new TreeMap<>();
    public AtomicInteger matchIdCounter = new AtomicInteger();
    private static final Map<String, String> countriesByIsoCode = initCountriesByCode();

    private static Map<String, String> initCountriesByCode() {
        Map<String, String> countriesByIsoCode = new HashMap<>();
        final String[] isoCountries = Locale.getISOCountries();

        for (final String country : isoCountries) {
            // country name , country code map
            Locale locale = new Locale("", country);
            countriesByIsoCode.put(locale.getISO3Country(),
                    locale.getDisplayCountry(Locale.ENGLISH));
        }
        return countriesByIsoCode;
    }

    @Override
    public Long startMatch(String homeTeamIsoCode,
                           String awayTeamIsoCode, LocalDateTime startDate) {
        // TODO: Add validation that there is no running match with any of team
        String homeTeamCountryName = getCountryName(homeTeamIsoCode);
        String awayTeamCountryName = getCountryName(awayTeamIsoCode);
        final var match = new Match(matchIdCounter.incrementAndGet(),
                new Score(homeTeamCountryName, 0),
                new Score(awayTeamCountryName, 0), startDate);
        updateMatch(match, match);
        return match.id();
    }

    private static String getCountryName(String iso3Code) {
        String countryName = countriesByIsoCode.get(iso3Code);
        if (countryName == null) {
            //TODO: Consider to change to domain exception
            throw new IllegalArgumentException("Unable found country for code " + iso3Code);
        }
        return countryName;
    }

    /**
     * This operation is idempotent. According to requirements,
     * it's needed to update operation with absolute values. It means that
     * result of operation doesn't depend on previous match value.
     * Subsequence of it that it's enough to create a new match object each time to
     * make this operation atomic.
     * There is a possible case when {@link ScoreboardImpl#matchesById}
     * and {@link ScoreboardImpl#sortedMatches} could have different values
     * for the same match. But according to the requirements there is no need to
     * check the score for a single match that's why I don't add additional
     * code to make this update atomic/synchronized
     *
     * @param matchId       identifier of the match
     * @param homeTeamScore home team score
     * @param awayTeamScore away team score
     */
    @Override
    public void updateScore(Long matchId, int homeTeamScore, int awayTeamScore) {
        // TODO: Add validation that such match exists
        // TODO: Add validation that scores are positives and at least one is higher than previous one
        final Match match = matchesById.get(matchId);
        // If match doesn't exist, it means it finished or not started.
        // It will be nice to throw exception in a case we are updating the not started match,
        // but now it's difficult to differ it from finished one
        // According to the requirements,
        // we could ignore results of finished matches
        if (match != null) {
            Match newMatch = new Match(matchId,
                    new Score(match.homeTeamScore().country(), homeTeamScore),
                    new Score(match.awayTeamScore().country(), awayTeamScore), match.startDate());
            updateMatch(match, newMatch);
        }
    }

    private void updateMatch(Match match, Match newMatch) {
        matchesById.put(match.id(), newMatch);
        sortedMatches.remove(match.compoundSortKey());
        sortedMatches.put(newMatch.compoundSortKey(), newMatch);
    }

    /**
     * The possible side effect of this operation that in
     * a case if operation failed after removing match from
     * {@link ScoreboardImpl#sortedMatches}. The obsolete match still could
     * be stored {@link ScoreboardImpl#matchesById}.
     * Due to according to the requirements there is no need to
     * check the result of the single match and the requirement to
     * implement the simplest solution you can think of
     * that works, I ignored this case. In real world, database transactions
     * the simplest option to cover it.
     *
     * @param matchId identifier of the match
     */
    @Override
    public void finishMatch(Long matchId) {
        Match matchToRemove = matchesById.get(matchId);
        if (matchToRemove != null) {
            sortedMatches.remove(matchToRemove.compoundSortKey());
            matchesById.remove(matchId);
        }
    }

    @Override
    public Collection<Match> getRunningMatchesSortedByTotalScoreAndMostRecentStarted() {
        return sortedMatches.values();
    }
}
