package com.zulrahrotations.learner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class SessionHistoryTest
{
	private static KillRecord kill(String duration, boolean died, int damage, int food, PhaseStat... phases)
	{
		return new KillRecord(Instant.EPOCH, "Tanz", duration, died, damage, 0, 0, food, 0, 0, Arrays.asList(phases));
	}

	private static PhaseStat phase(int n, String label, int damage)
	{
		return PhaseStat.builder().phaseNumber(n).label(label).damage(damage).build();
	}

	@Test
	public void emptyHistory()
	{
		SessionHistory h = new SessionHistory();
		assertEquals(0, h.size());
		assertNull(h.last());
		assertEquals(0, h.averageDamage());
		assertNull(h.bestDuration());
		assertNull(h.costliestPhase());
	}

	@Test
	public void newestFirstAndAggregates()
	{
		SessionHistory h = new SessionHistory();
		h.add(kill("1:20", false, 40, 2, phase(2, "Tanzanite · East", 30), phase(6, "Jad · East", 10)));
		h.add(kill("0:50", true, 20, 1, phase(2, "Tanzanite · East", 20)));
		h.add(kill("1:05.2", false, 10, 0, phase(6, "Jad · East", 10)));

		assertEquals(3, h.size());
		assertEquals("1:05.2", h.last().getDuration());
		assertEquals(2, h.kills());
		assertEquals(1, h.deaths());
		assertEquals(23, h.averageDamage());
		assertEquals(3, h.totalFood());
		assertEquals("1:05.2", h.bestDuration());

		Map<String, Integer> byPhase = h.damageByPhase();
		List<String> order = Arrays.asList(byPhase.keySet().toArray(new String[0]));
		assertEquals(Arrays.asList("P2 Tanzanite · East", "P6 Jad · East"), order);
		assertEquals(50, (int) byPhase.get("P2 Tanzanite · East"));
		assertEquals("P2 Tanzanite · East", h.costliestPhase());
	}

	@Test
	public void deathsDoNotCountTowardsBestDuration()
	{
		SessionHistory h = new SessionHistory();
		h.add(kill("0:30", true, 5, 0));
		assertNull(h.bestDuration());
		assertEquals(0, h.kills());
	}

	@Test
	public void updateLastDurationReplacesOnlyTheNewestRecord()
	{
		SessionHistory h = new SessionHistory();
		h.add(kill("1:20", false, 1, 0));
		h.add(kill("1:10", false, 2, 0));
		h.updateLastDuration("1:09.6");
		assertEquals("1:09.6", h.getKills().get(0).getDuration());
		assertEquals(2, h.getKills().get(0).getDamage());
		assertEquals("1:20", h.getKills().get(1).getDuration());
		h.updateLastDuration(null);
		assertEquals("1:09.6", h.last().getDuration());
	}

	@Test
	public void historyIsCappedAndClearable()
	{
		SessionHistory h = new SessionHistory();
		for (int i = 0; i < SessionHistory.MAX_KILLS + 5; i++)
		{
			h.add(kill("1:00", false, i, 0));
		}
		assertEquals(SessionHistory.MAX_KILLS, h.size());
		assertEquals(SessionHistory.MAX_KILLS + 4, h.last().getDamage());
		h.clear();
		assertEquals(0, h.size());
		assertEquals(Collections.emptyList(), h.getKills());
	}
}
