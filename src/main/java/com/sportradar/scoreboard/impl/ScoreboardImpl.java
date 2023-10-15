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

    @Override
    public void updateScore(Long matchId, int homeTeamScore, int awayTeamScore) {
        // TODO: Add validation that such match exists
        // TODO: Add validation that scores are positives and at least one is higher than previous one
        final Match match = matchesById.get(matchId);
        //TODO: To consider. Initial idea is to re create match to make this operation atomic
        Match newMatch = new Match(matchId,
                new Score(match.homeTeamScore().country(), homeTeamScore),
                new Score(match.awayTeamScore().country(), awayTeamScore), match.startDate());
        updateMatch(match, newMatch);
    }

    private void updateMatch(Match match, Match newMatch) {
        // TODO: This operation should be atomic or synchronized
        matchesById.put(match.id(), newMatch);
        sortedMatches.remove(match.compoundSortKey());
        sortedMatches.put(newMatch.compoundSortKey(), newMatch);
    }

    @Override
    public void finishMatch(Long matchId) {
        // TODO: This operation should be atomic or synchronized
        // TODO: Consider add validation that such match exists or
        //  not(for concurrency reasons it could be better to ignore such case)
        Match removedMatch = matchesById.remove(matchId);
        sortedMatches.remove(removedMatch.compoundSortKey());
    }

    @Override
    public Collection<Match> getRunningMatchesSortedByTotalScoreAndMostRecentStarted() {
        return sortedMatches.values();
    }
}
