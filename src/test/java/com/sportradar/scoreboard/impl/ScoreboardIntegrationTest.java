package com.sportradar.scoreboard.impl;

import com.sportradar.scoreboard.Match;
import com.sportradar.scoreboard.Scoreboard;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScoreboardIntegrationTest {

    public static final LocalDateTime FIRST_MATCH_DATE = LocalDateTime.of(2022, Month.JULY, 15,
            19, 30, 40);

    @Test
    public void testSuccessCase() {
        final Scoreboard scoreboard = new ScoreboardImpl();

        final Long mexicoCanadaMatchId = scoreboard.startMatch("MEX",
                "CAN", FIRST_MATCH_DATE);
        final Long spainBrazilMatchId = scoreboard.startMatch("ESP", "BRA",
                FIRST_MATCH_DATE.plus(5, ChronoUnit.MINUTES));
        final Long germanyFranceMatchId = scoreboard.startMatch("DEU", "FRA",
                FIRST_MATCH_DATE.plus(10, ChronoUnit.MINUTES));
        final Long uruguayItalyMatchId = scoreboard.startMatch("URY", "ITA",
                FIRST_MATCH_DATE.plus(15, ChronoUnit.MINUTES));
        final Long argentinaAustriaMatchId = scoreboard.startMatch("ARG", "AUS",
                FIRST_MATCH_DATE.plus(20, ChronoUnit.MINUTES));

        scoreboard.updateScore(mexicoCanadaMatchId, 0, 5);
        scoreboard.updateScore(spainBrazilMatchId, 10, 2);
        scoreboard.updateScore(germanyFranceMatchId, 2,2);
        scoreboard.updateScore(uruguayItalyMatchId, 6, 6);
        scoreboard.updateScore(argentinaAustriaMatchId, 3, 1);

        final Collection<Match> runningMatches = scoreboard.getRunningMatchesSortedByTotalScoreAndMostRecentStarted();

        assertEquals(runningMatches.size(), 5);
        final Iterator<Match> iterator = runningMatches.iterator();

        final Match firstMatch = iterator.next();
        assertEquals(firstMatch.id(), uruguayItalyMatchId);
        assertEquals(firstMatch.homeTeamScore().country(), "Uruguay");
        assertEquals(firstMatch.homeTeamScore().score(), 6);
        assertEquals(firstMatch.awayTeamScore().country(), "Italy");
        assertEquals(firstMatch.awayTeamScore().score(), 6);

        final Match secondMatch = iterator.next();
        assertEquals(secondMatch.id(), spainBrazilMatchId);
        assertEquals(secondMatch.homeTeamScore().country(), "Spain");
        assertEquals(secondMatch.homeTeamScore().score(), 10);
        assertEquals(secondMatch.awayTeamScore().country(), "Brazil");
        assertEquals(secondMatch.awayTeamScore().score(), 2);

        final Match thirdMatch = iterator.next();
        assertEquals(thirdMatch.id(), mexicoCanadaMatchId);
        assertEquals(thirdMatch.homeTeamScore().country(), "Mexico");
        assertEquals(thirdMatch.homeTeamScore().score(), 0);
        assertEquals(thirdMatch.awayTeamScore().country(), "Canada");
        assertEquals(thirdMatch.awayTeamScore().score(), 5);

        final Match forthMatch = iterator.next();
        assertEquals(forthMatch.id(), argentinaAustriaMatchId);
        assertEquals(forthMatch.homeTeamScore().country(), "Argentina");
        assertEquals(forthMatch.homeTeamScore().score(), 3);
        assertEquals(forthMatch.awayTeamScore().country(), "Australia");
        assertEquals(forthMatch.awayTeamScore().score(), 1);

        final Match fiveMatch = iterator.next();
        assertEquals(fiveMatch.id(), germanyFranceMatchId);
        assertEquals(fiveMatch.homeTeamScore().country(), "Germany");
        assertEquals(fiveMatch.homeTeamScore().score(), 2);
        assertEquals(fiveMatch.awayTeamScore().country(), "France");
        assertEquals(fiveMatch.awayTeamScore().score(), 2);
    }
}
