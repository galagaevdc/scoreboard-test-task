package com.sportradar.scoreboard.impl;

import com.sportradar.scoreboard.Match;
import com.sportradar.scoreboard.Scoreboard;
import org.junit.jupiter.api.RepeatedTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ScoreboardMultiThreadIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(ScoreboardMultiThreadIntegrationTest.class);

    private static final LocalDateTime FIRST_MATCH_DATE = LocalDateTime.of(2022, Month.JULY, 15,
            19, 30, 40);
    private static final int HOME_TEAM_UPATE_FIRST = 1;
    private static final int AWAY_TEAM_UPDATE_FIRST = 5;
    private static final int HOME_TEAM_UPDATE_SECOND = 3;
    private static final int AWAY_TEAM_UPDATE_SECOND = 0;
    private static final int HOME_TEAM_INITIAL_VALUE = 0;
    private static final int AWAY_TEAM_INITIAL_VALUE = 0;
    private static volatile Long mexicoCanadaMatchId;

    @RepeatedTest(10)
    public void testInMultiThreading() throws InterruptedException, ExecutionException, TimeoutException {
        final Scoreboard scoreboard = new ScoreboardImpl();

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        final List<Future<?>> futures = new ArrayList<>();
        futures.add(executorService.submit(() -> startMatch(scoreboard)));
        futures.add(executorService.submit(() -> updateScore(scoreboard, HOME_TEAM_UPATE_FIRST, AWAY_TEAM_UPDATE_FIRST)));
        futures.add(executorService.submit(() -> updateScore(scoreboard, HOME_TEAM_UPDATE_SECOND, AWAY_TEAM_UPDATE_SECOND)));
        futures.add(executorService.submit(() -> finishMatch(scoreboard)));
        futures.add(executorService.submit(() -> finishMatch(scoreboard)));
        futures.add(executorService.submit(() -> checkScoreboard(scoreboard)));

        for (Future<?> future : futures) {
            future.get(1, TimeUnit.SECONDS);
        }
    }

    private static void updateScore(Scoreboard scoreboard, int homeTeamScore, int awayTeamScore) {
        if (mexicoCanadaMatchId != null) {
            logger.info("Updating score");
            scoreboard.updateScore(mexicoCanadaMatchId, homeTeamScore, awayTeamScore);
            logger.info("Updating score finished");
        }
    }

    private void startMatch(Scoreboard scoreboard) {
        if (mexicoCanadaMatchId == null) {
            logger.info("Start match");
            mexicoCanadaMatchId = scoreboard.startMatch("MEX",
                    "CAN", FIRST_MATCH_DATE);

            logger.info("Start match is completed");
        }
    }

    private void finishMatch(Scoreboard scoreboard) {
        logger.info("Finish match");
        scoreboard.finishMatch(mexicoCanadaMatchId);
        logger.info("Finish match is completed");
    }

    private static void checkScoreboard(Scoreboard scoreboard) {
        logger.info("Checking scoreboard");
        final Collection<Match> runningMatches = scoreboard.getRunningMatchesSortedByTotalScoreAndMostRecentStarted();

        if (runningMatches.size() == 1) {
            final Iterator<Match> iterator = runningMatches.iterator();

            final Match firstMatch = iterator.next();
            logger.info("First match {}", firstMatch.toString());
            assertEquals(firstMatch.id(), mexicoCanadaMatchId);
            assertEquals(firstMatch.homeTeamScore().country(), "Mexico");
            if (firstMatch.homeTeamScore().score() == HOME_TEAM_UPATE_FIRST) {
                assertEquals(firstMatch.awayTeamScore().score(), AWAY_TEAM_UPDATE_FIRST);
            } else if (firstMatch.homeTeamScore().score() == HOME_TEAM_UPDATE_SECOND) {
                assertEquals(firstMatch.awayTeamScore().score(), AWAY_TEAM_UPDATE_SECOND);
            } else if (firstMatch.homeTeamScore().score() == HOME_TEAM_INITIAL_VALUE) {
                assertEquals(firstMatch.awayTeamScore().score(), AWAY_TEAM_INITIAL_VALUE);
            } else {
                fail("Unexpected home score");
            }
        }
        logger.info("Checking scoreboard finished");
    }
}
