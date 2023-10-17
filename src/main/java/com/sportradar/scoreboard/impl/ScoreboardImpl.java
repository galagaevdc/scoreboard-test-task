package com.sportradar.scoreboard.impl;

import com.sportradar.scoreboard.Match;
import com.sportradar.scoreboard.MatchSortKey;
import com.sportradar.scoreboard.Score;
import com.sportradar.scoreboard.Scoreboard;
import com.sportradar.scoreboard.exception.MatchNotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

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
        try {
            this.reentrantReadWriteLock.writeLock().lock();
            String homeTeamCountryName = getCountryName(homeTeamIsoCode);
            String awayTeamCountryName = getCountryName(awayTeamIsoCode);
            final var match = new Match(matchIdCounter.incrementAndGet(),
                    new Score(homeTeamCountryName, 0),
                    new Score(awayTeamCountryName, 0), startDate, false);
            updateMatch(match, match);
            return match.id();
        } finally {
            this.reentrantReadWriteLock.writeLock().unlock();
        }
    }

    @Override
    public Match getMatch(final Long matchId) {
        try {
            reentrantReadWriteLock.readLock().lock();
            Match match = this.matchesById.get(matchId);
            if (match == null) {
                throw new MatchNotFoundException();
            }
            return match;
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }
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
        // TODO: Add validation that scores are positives and at least one is higher than previous one
        try {
            this.reentrantReadWriteLock.writeLock().lock();
            final Match match = matchesById.get(matchId);
            if (match == null) {
                throw new MatchNotFoundException();
            }
            Match newMatch = match.createMatchCopy(homeTeamScore, awayTeamScore);
            updateMatch(match, newMatch);
        } finally {
            this.reentrantReadWriteLock.writeLock().unlock();
        }
    }

    private void updateMatch(Match match, Match newMatch) {
        matchesById.put(match.id(), newMatch);
        sortedMatches.remove(match.compoundSortKey());
        sortedMatches.put(newMatch.compoundSortKey(), newMatch);
    }

    @Override
    public void finishMatch(Long matchId) {
        try {
            this.reentrantReadWriteLock.writeLock().lock();
            Match matchToRemove = matchesById.get(matchId);
            if (matchToRemove == null) {
                throw new MatchNotFoundException();
            }
            sortedMatches.remove(matchToRemove.compoundSortKey());
            Match newMatch = matchToRemove.createFinishedMatchCopy();
            updateMatch(matchToRemove, newMatch);
        } finally {
            this.reentrantReadWriteLock.writeLock().unlock();
        }
    }

    @Override
    public Collection<Match> getRunningMatchesSortedByTotalScoreAndMostRecentStarted() {
        try {
            reentrantReadWriteLock.readLock().lock();
            return new ArrayList<>(sortedMatches.values());
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }

    }
}
