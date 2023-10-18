package com.sportradar.scoreboard.impl;

import com.sportradar.scoreboard.Scoreboard;
import com.sportradar.scoreboard.exception.CountryNotSupportedException;
import com.sportradar.scoreboard.exception.MatchNotFoundException;
import com.sportradar.scoreboard.exception.ScoreNotPositiveException;
import com.sportradar.scoreboard.exception.TeamAlreadyPlayingException;
import com.sportradar.scoreboard.model.Match;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScoreboardImplUnitTest {

    private static final String OTHER_ALREADY_PLAYING_COUNTRY = "ESP";
    private static final String OTHER_PLAYING_COUNTRY = "DEU";
    private static final String ALREADY_PLAYING_COUNTRY_NAME = "Mexico";
    private static final String ALREADY_PLAYING_COUNTRY_CODE = "MEX";
    private static final String FAKE_ISO_CODE = "FAKE_ISO_CODE";
    private static final String PLAYING_COUNTRY = "ESP";
    private static final LocalDateTime MATCH_DATE = LocalDateTime.of(2022, Month.JULY, 15,
            19, 30, 40);
    public static final String ONE_MORE_HOME_COUNRY = "URY";
    public static final String ONE_MORE_AWAY_COUNTRY = "ITA";
    public static final long NOT_EXISTING_MATCH = 1L;

    private static Stream<Arguments> getAlreadyPlayingParameters() {
        return Stream.of(
                Arguments.of(ALREADY_PLAYING_COUNTRY_CODE, true, true),
                Arguments.of(ALREADY_PLAYING_COUNTRY_CODE, true, false),
                Arguments.of(ALREADY_PLAYING_COUNTRY_CODE, false, true),
                Arguments.of(ALREADY_PLAYING_COUNTRY_CODE, false, false)
        );
    }

    private static Stream<Arguments> getNegativeScoreParameters() {
        return Stream.of(
                Arguments.of(0, -1),
                Arguments.of(-2, 5)
        );
    }

    @MethodSource("getAlreadyPlayingParameters")
    @ParameterizedTest
    public void shouldThrowTeamAlreadyPlayingExceptionWhenAlreadyPlayingCountryStartMatch(final String playingCountryIsoCode,
                                                                                          boolean isHomeCounty,
                                                                                          boolean isHomeAlreadyPlaying) {
        Scoreboard scoreboard = new ScoreboardImpl();
        if (isHomeAlreadyPlaying) {
            scoreboard.startMatch(playingCountryIsoCode, OTHER_ALREADY_PLAYING_COUNTRY, LocalDateTime.now());
        } else {
            scoreboard.startMatch(OTHER_ALREADY_PLAYING_COUNTRY, playingCountryIsoCode, LocalDateTime.now());
        }

        TeamAlreadyPlayingException teamAlreadyPlayingException;
        if (isHomeCounty) {
            teamAlreadyPlayingException = Assertions.assertThrows(TeamAlreadyPlayingException.class,
                    () -> scoreboard.startMatch(playingCountryIsoCode, OTHER_PLAYING_COUNTRY, LocalDateTime.now()));

        } else {
            teamAlreadyPlayingException = Assertions.assertThrows(TeamAlreadyPlayingException.class,
                    () -> scoreboard.startMatch(OTHER_PLAYING_COUNTRY, playingCountryIsoCode, LocalDateTime.now()));
        }


        assertEquals(ALREADY_PLAYING_COUNTRY_NAME, teamAlreadyPlayingException.getAlreadyPlayingCountry());
    }


    @MethodSource("getNegativeScoreParameters")
    @ParameterizedTest
    public void shouldThrowScoreIsNegativeExceptionWhenNegativeScoreIsProvided(int homeScore, int awayScore) {
        Scoreboard scoreboard = new ScoreboardImpl();
        Long matchId = scoreboard.startMatch(OTHER_PLAYING_COUNTRY, PLAYING_COUNTRY, LocalDateTime.now());

        ScoreNotPositiveException scoreNotPositiveException = Assertions.assertThrows(ScoreNotPositiveException.class,
                () -> scoreboard.updateScore(matchId, homeScore, awayScore));


        assertNotNull(scoreNotPositiveException);
    }

    @Test
    public void shouldThrowCountryNotSupportedExceptionWhenIncorrectIsoCodeCountryStartMatch() {
        Scoreboard scoreboard = new ScoreboardImpl();

        CountryNotSupportedException countryNotSupportedException = Assertions.assertThrows(CountryNotSupportedException.class,
                () -> scoreboard.startMatch(FAKE_ISO_CODE, OTHER_PLAYING_COUNTRY, LocalDateTime.now()));

        assertEquals(FAKE_ISO_CODE, countryNotSupportedException.getNotSupportedCode());
    }

    @Test
    public void shouldReturnTwoMatchesInScheduleWhenTheTotalScoreAndStartTimeIsTheSame() {
        Scoreboard scoreboard = new ScoreboardImpl();
        Long firstMatchId = scoreboard.startMatch(PLAYING_COUNTRY, ALREADY_PLAYING_COUNTRY_CODE, MATCH_DATE);
        Long secondMatchId = scoreboard.startMatch(ONE_MORE_HOME_COUNRY, ONE_MORE_AWAY_COUNTRY, MATCH_DATE);

        Collection<Match> matches = scoreboard.getRunningMatchesSortedByTotalScoreAndMostRecentStarted();

        assertEquals(2, matches.size());
        final Set<Long> matchIds = matches.stream().map(Match::id).collect(Collectors.toSet());
        assertTrue(matchIds.contains(firstMatchId));
        assertTrue(matchIds.contains(secondMatchId));
    }

    @Test
    public void shouldNotReturnMatchInScheduleWhenItIsFinished() {
        Scoreboard scoreboard = new ScoreboardImpl();
        Long firstMatchId = scoreboard.startMatch(PLAYING_COUNTRY, ALREADY_PLAYING_COUNTRY_CODE, MATCH_DATE);
        Long secondMatchId = scoreboard.startMatch(ONE_MORE_HOME_COUNRY, ONE_MORE_AWAY_COUNTRY, MATCH_DATE);
        scoreboard.finishMatch(secondMatchId);


        Collection<Match> matches = scoreboard.getRunningMatchesSortedByTotalScoreAndMostRecentStarted();

        assertEquals(1, matches.size());
        final Set<Long> matchIds = matches.stream().map(Match::id).collect(Collectors.toSet());
        assertTrue(matchIds.contains(firstMatchId));
    }

    @Test
    public void shouldThrowMatchNotFoundExceptionWhenGettingNotExistMatch() {
        Scoreboard scoreboard = new ScoreboardImpl();

        MatchNotFoundException matchNotFoundException = Assertions.assertThrows(MatchNotFoundException.class,
                () -> scoreboard.getMatch(NOT_EXISTING_MATCH));

        assertEquals(NOT_EXISTING_MATCH, matchNotFoundException.getMatchId());
    }


    @Test
    public void shouldThrowMatchNotFoundExceptionWhenUpdatingNotExistMatch() {
        Scoreboard scoreboard = new ScoreboardImpl();

        MatchNotFoundException matchNotFoundException = Assertions.assertThrows(MatchNotFoundException.class,
                () -> scoreboard.updateScore(NOT_EXISTING_MATCH, 1, 2));

        assertEquals(NOT_EXISTING_MATCH, matchNotFoundException.getMatchId());
    }


    @Test
    public void shouldThrowMatchNotFoundExceptionWhenFinishinNotExistMatch() {
        Scoreboard scoreboard = new ScoreboardImpl();

        MatchNotFoundException matchNotFoundException = Assertions.assertThrows(MatchNotFoundException.class,
                () -> scoreboard.finishMatch(NOT_EXISTING_MATCH));

        assertEquals(NOT_EXISTING_MATCH, matchNotFoundException.getMatchId());
    }


}
