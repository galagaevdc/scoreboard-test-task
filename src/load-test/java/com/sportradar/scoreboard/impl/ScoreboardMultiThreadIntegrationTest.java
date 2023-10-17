package com.sportradar.scoreboard.impl;

import com.sportradar.scoreboard.Match;
import com.sportradar.scoreboard.Scoreboard;
import com.sportradar.scoreboard.exception.MatchNotFoundException;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ScoreboardMultiThreadIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(ScoreboardMultiThreadIntegrationTest.class);

    private static final LocalDateTime FIRST_MATCH_DATE = LocalDateTime.of(2022, Month.JULY, 15, 19, 30, 40);
    private static final int HOME_TEAM_UPDATE_FIRST = 1;
    private static final int MATCH_TO_FINISH_HOME_SCORE = 5;
    private static final int AWAY_TEAM_UPDATE_FIRST = 7;
    private static final int HOME_TEAM_UPDATE_SECOND = 3;
    private static final int AWAY_TEAM_UPDATE_SECOND = 0;
    private static final int HOME_TEAM_INITIAL_VALUE = 0;
    private static final int AWAY_TEAM_INITIAL_VALUE = 0;
    private static final int MATCH_TO_FINISH_AWAY_SCORE = 1;
    private static final ReentrantReadWriteLock pauseFinishUpdateLock = new ReentrantReadWriteLock();

    @RepeatedTest(1000)
    public void testInMultiThreading() throws InterruptedException, ExecutionException, TimeoutException {
        final Scoreboard scoreboard = new ScoreboardImpl();
        final Long matchId = startMatch(scoreboard);
        final Long matchIdToFinish = startMatchToFinish(scoreboard);

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        final List<Future<?>> futures = new ArrayList<>();
        futures.add(executorService.submit(() -> updateScore(scoreboard, HOME_TEAM_UPDATE_FIRST, AWAY_TEAM_UPDATE_FIRST, matchId)));
        futures.add(executorService.submit(() -> updateScore(scoreboard, HOME_TEAM_UPDATE_SECOND, AWAY_TEAM_UPDATE_SECOND, matchId)));
        futures.add(executorService.submit(() -> updateScore(scoreboard, MATCH_TO_FINISH_HOME_SCORE, MATCH_TO_FINISH_AWAY_SCORE, matchIdToFinish)));
        futures.add(executorService.submit(() -> finishMatch(scoreboard, matchIdToFinish)));
        futures.add(executorService.submit(() -> finishMatch(scoreboard, matchIdToFinish)));
        futures.add(executorService.submit(() -> checkScoreboard(scoreboard, matchId, matchIdToFinish)));

        for (Future<?> future : futures) {
            future.get(1, TimeUnit.SECONDS);
        }
    }

    private static void updateScore(Scoreboard scoreboard, int homeTeamScore, int awayTeamScore, Long idOfMatch) {
        try {
            pauseFinishUpdateLock.readLock().lock();
            logger.info("Updating score for match {} home score {} away score {} ", idOfMatch, homeTeamScore, awayTeamScore);
            scoreboard.updateScore(idOfMatch, homeTeamScore, awayTeamScore);
            logger.info("Updating score finished {}", idOfMatch);
        } catch (MatchNotFoundException matchNotFoundException) {
            logger.info("Match not started yet {}", idOfMatch);
        } finally {
            pauseFinishUpdateLock.readLock().unlock();
        }

    }

    private Long startMatch(final Scoreboard scoreboard) {
        logger.info("Start match");
        Long matchId = scoreboard.startMatch("MEX", "CAN", FIRST_MATCH_DATE);

        logger.info("Start match is completed {}", matchId);
        return matchId;
    }

    private Long startMatchToFinish(final Scoreboard scoreboard) {
        logger.info("Start match to finish");
        Long matchIdToFinish = scoreboard.startMatch("ESP", "FRA", FIRST_MATCH_DATE);

        logger.info("Start match is completed {}", matchIdToFinish);
        return matchIdToFinish;
    }

    private void finishMatch(Scoreboard scoreboard, Long matchIdToFinish) {
        try {
            pauseFinishUpdateLock.readLock().lock();
            logger.info("Finishing match {}", matchIdToFinish);
            scoreboard.finishMatch(matchIdToFinish);
            logger.info("Finish match {} is completed", matchIdToFinish);
        } catch (final MatchNotFoundException matchNotFoundException) {
            logger.info("Match not started yet {}", matchIdToFinish);
        } finally {
            pauseFinishUpdateLock.readLock().unlock();
        }
    }

    private static void checkScoreboard(Scoreboard scoreboard, long matchId, Long matchIdToFinish) {
        logger.info("Checking scoreboard");
        Collection<Match> runningMatches;
        Match match;
        Match matchToFinishById;

        /*
         * Pause update and finish threads in order to prevent update of
         * match in other threads before assertion.
         */
        try {
            pauseFinishUpdateLock.writeLock().lock();
            runningMatches = scoreboard.getRunningMatchesSortedByTotalScoreAndMostRecentStarted();
            match = scoreboard.getMatch(matchId);
            matchToFinishById = scoreboard.getMatch(matchIdToFinish);
        } finally {
            pauseFinishUpdateLock.writeLock().unlock();
        }

        if (runningMatches.size() == 1) {
            final Iterator<Match> iterator = runningMatches.iterator();

            final Match firstMatch = iterator.next();
            if (firstMatch.id() == matchId) {
                assertFirstMatch(firstMatch, matchId, match);
                assertMatchToFinish(null, matchIdToFinish, matchToFinishById);
            } else {
                assertMatchToFinish(firstMatch, matchIdToFinish, matchToFinishById);
            }
        }

        if (runningMatches.size() == 2) {
            final Iterator<Match> iterator = runningMatches.iterator();

            final Match firstMatch = iterator.next();

            if (firstMatch.id() == matchId) {
                assertFirstMatch(firstMatch, matchId, match);
            } else {
                assertMatchToFinish(firstMatch, matchIdToFinish, matchToFinishById);
            }

            final Match secondMatch = iterator.next();

            if (secondMatch.id() == matchId) {
                assertFirstMatch(secondMatch, matchId, match);
            } else {
                assertMatchToFinish(secondMatch, matchIdToFinish, matchToFinishById);
            }

        }


        logger.info("Checking scoreboard finished");
    }

    private static void assertMatchToFinish(Match matchToFinish, Long matchIdToFinish, Match matchToFinishById) {
        try {

            if (matchToFinish != null) {
                assertEquals(matchToFinish.id(), matchToFinishById.id());
                assertEquals(matchToFinish.homeTeamScore().score(), matchToFinishById.homeTeamScore().score());
                assertEquals(matchToFinish.awayTeamScore().score(), matchToFinishById.awayTeamScore().score());

                assertEquals(matchToFinish.homeTeamScore().country(), "Spain");
                assertEquals(matchToFinish.awayTeamScore().country(), "France");
            }

            if (matchToFinishById.homeTeamScore().score() == MATCH_TO_FINISH_HOME_SCORE) {
                assertEquals(matchToFinishById.awayTeamScore().score(), MATCH_TO_FINISH_AWAY_SCORE);
            } else if (matchToFinishById.homeTeamScore().score() == HOME_TEAM_INITIAL_VALUE) {
                assertEquals(matchToFinishById.awayTeamScore().score(), AWAY_TEAM_INITIAL_VALUE);
            } else {
                fail("Unexpected home score");
            }
        } catch (MatchNotFoundException matchNotFoundException) {
            logger.info("Match {} not started yet", matchIdToFinish);
        }
    }

    private static void assertFirstMatch(Match firstMatch, long matchId, Match runningMatch) {

        assertEquals(firstMatch.id(), runningMatch.id());
        assertEquals(firstMatch.homeTeamScore().score(), runningMatch.homeTeamScore().score());
        assertEquals(firstMatch.awayTeamScore().score(), runningMatch.awayTeamScore().score());
        assertEquals(firstMatch.homeTeamScore().country(), runningMatch.homeTeamScore().country());
        assertEquals(firstMatch.awayTeamScore().country(), runningMatch.awayTeamScore().country());

        assertEquals(firstMatch.id(), matchId);
        if (firstMatch.homeTeamScore().score() == HOME_TEAM_UPDATE_FIRST) {
            assertEquals(firstMatch.awayTeamScore().score(), AWAY_TEAM_UPDATE_FIRST);
        } else if (firstMatch.homeTeamScore().score() == HOME_TEAM_UPDATE_SECOND) {
            assertEquals(firstMatch.awayTeamScore().score(), AWAY_TEAM_UPDATE_SECOND);
        } else if (firstMatch.homeTeamScore().score() == HOME_TEAM_INITIAL_VALUE) {
            assertEquals(firstMatch.awayTeamScore().score(), AWAY_TEAM_INITIAL_VALUE);
        } else {
            fail("Unexpected home score");
        }
    }
}
