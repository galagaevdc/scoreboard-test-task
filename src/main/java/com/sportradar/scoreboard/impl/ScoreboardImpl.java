package com.sportradar.scoreboard.impl;

import com.sportradar.scoreboard.model.Match;
import com.sportradar.scoreboard.model.MatchSortKey;
import com.sportradar.scoreboard.model.Score;
import com.sportradar.scoreboard.Scoreboard;
import com.sportradar.scoreboard.exception.CountryNotSupportedException;
import com.sportradar.scoreboard.exception.MatchNotFoundException;
import com.sportradar.scoreboard.exception.ScoreNotPositiveException;
import com.sportradar.scoreboard.exception.TeamAlreadyPlayingException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
            Locale locale = new Locale("", country);
            countriesByIsoCode.put(locale.getISO3Country(),
                    locale.getDisplayCountry(Locale.ENGLISH));
        }
        return countriesByIsoCode;
    }

    @Override
    public Long startMatch(String homeTeamIsoCode,
                           String awayTeamIsoCode, LocalDateTime startDate) {
        try {
            this.reentrantReadWriteLock.writeLock().lock();
            String homeTeamCountryName = getCountryName(homeTeamIsoCode);
            String awayTeamCountryName = getCountryName(awayTeamIsoCode);
            checkIsCountryPlaying(homeTeamCountryName);
            checkIsCountryPlaying(awayTeamCountryName);

            final var match = new Match(matchIdCounter.incrementAndGet(),
                    new Score(homeTeamCountryName, 0),
                    new Score(awayTeamCountryName, 0), startDate, false);
            updateMatch(match, match);
            return match.id();
        } finally {
            this.reentrantReadWriteLock.writeLock().unlock();
        }
    }

    private void checkIsCountryPlaying(String homeTeamCountryName) {
        matchesById.values().stream().filter(match ->
                        match.awayTeamScore().country().equals(homeTeamCountryName)
                                || match.homeTeamScore().country().equals(homeTeamCountryName))
                .findAny().ifPresent(match -> {
                    throw new TeamAlreadyPlayingException(homeTeamCountryName);
                });
    }

    @Override
    public Match getMatch(final Long matchId) {
        try {
            reentrantReadWriteLock.readLock().lock();
            Match match = this.matchesById.get(matchId);
            if (match == null) {
                throw new MatchNotFoundException(matchId);
            }
            return match;
        } finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }

    private static String getCountryName(String iso3Code) {
        String countryName = countriesByIsoCode.get(iso3Code);
        if (countryName == null) {
            throw new CountryNotSupportedException(iso3Code);
        }
        return countryName;
    }

    @Override
    public void updateScore(Long matchId, int homeTeamScore, int awayTeamScore) {
        try {
            this.reentrantReadWriteLock.writeLock().lock();
            if (homeTeamScore < 0 || awayTeamScore < 0) {
                throw new ScoreNotPositiveException();
            }
            final Match match = matchesById.get(matchId);
            if (match == null) {
                throw new MatchNotFoundException(matchId);
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
                throw new MatchNotFoundException(matchId);
            }
            sortedMatches.remove(matchToRemove.compoundSortKey());
            Match newMatch = matchToRemove.createFinishedMatchCopy();
            matchesById.put(newMatch.id(), newMatch);
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
