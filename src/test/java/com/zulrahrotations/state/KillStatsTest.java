package com.zulrahrotations.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class KillStatsTest
{
	@Test
	public void countsKillsFromTheKillCountMessage()
	{
		KillStats stats = new KillStats();
		assertFalse(stats.onChatMessage("Welcome to Old School RuneScape."));
		assertTrue(stats.onChatMessage("Your Zulrah kill count is: 1,234."));
		assertEquals(1, stats.getSessionKills());
		assertEquals(1234, stats.getReportedKillCount());
		assertTrue(stats.onChatMessage("Your Zulrah kill count is: 1235."));
		assertEquals(2, stats.getSessionKills());
		assertEquals(1235, stats.getReportedKillCount());
	}

	@Test
	public void readsFightDuration()
	{
		KillStats stats = new KillStats();
		assertTrue(stats.onChatMessage("Fight duration: 1:05. Personal best: 0:58"));
		assertEquals("1:05", stats.getLastFightDuration());
		assertTrue(stats.onChatMessage("Fight duration: 1:05.40 (new personal best)"));
		assertEquals("1:05.40", stats.getLastFightDuration());
		assertEquals(0, stats.getSessionKills());
	}

	@Test
	public void timerLifecycle()
	{
		KillStats stats = new KillStats();
		assertNull(stats.elapsed());
		assertNull(stats.elapsedText());
		stats.startTimer();
		assertTrue(stats.isTimerRunning());
		assertNotNull(stats.elapsedText());
		stats.onChatMessage("Your Zulrah kill count is: 2.");
		assertFalse("a kill freezes the timer", stats.isTimerRunning());
		assertNotNull(stats.elapsed());
		stats.clearTimer();
		assertNull(stats.elapsed());
		stats.resetSession();
		assertEquals(0, stats.getSessionKills());
		assertEquals(-1, stats.getReportedKillCount());
	}

	@Test
	public void ignoresNull()
	{
		assertFalse(new KillStats().onChatMessage(null));
	}
}
