package com.zulrahrotations.learner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kills recorded since the client started, newest first, with the session aggregates the
 * panel shows. Kept in memory only.
 */
public final class SessionHistory
{
	static final int MAX_KILLS = 50;

	private final List<KillRecord> kills = new ArrayList<>();

	public void add(KillRecord record)
	{
		kills.add(0, record);
		while (kills.size() > MAX_KILLS)
		{
			kills.remove(kills.size() - 1);
		}
	}

	public void clear()
	{
		kills.clear();
	}

	/** Newest first. */
	public List<KillRecord> getKills()
	{
		return Collections.unmodifiableList(kills);
	}

	public KillRecord last()
	{
		return kills.isEmpty() ? null : kills.get(0);
	}

	/** Replace the duration on the most recent record, e.g. once the game reports the exact fight time. */
	public void updateLastDuration(String duration)
	{
		if (kills.isEmpty() || duration == null)
		{
			return;
		}
		KillRecord r = kills.get(0);
		kills.set(0, new KillRecord(r.getEndedAt(), r.getRotationName(), duration, r.isDied(), r.getDamage(),
			r.getVenomDamage(), r.getHits(), r.getFood(), r.getPotions(), r.getPrayerUsed(), r.getPhases()));
	}

	public int size()
	{
		return kills.size();
	}

	public int kills()
	{
		int n = 0;
		for (KillRecord r : kills)
		{
			if (!r.isDied())
			{
				n++;
			}
		}
		return n;
	}

	public int deaths()
	{
		return kills.size() - kills();
	}

	/** Mean damage taken per record, deaths included, or 0 when empty. */
	public int averageDamage()
	{
		if (kills.isEmpty())
		{
			return 0;
		}
		int total = 0;
		for (KillRecord r : kills)
		{
			total += r.getDamage();
		}
		return Math.round((float) total / kills.size());
	}

	public int totalFood()
	{
		int n = 0;
		for (KillRecord r : kills)
		{
			n += r.getFood();
		}
		return n;
	}

	public int totalPotions()
	{
		int n = 0;
		for (KillRecord r : kills)
		{
			n += r.getPotions();
		}
		return n;
	}

	/** Fastest completed kill's duration text, or null. */
	public String bestDuration()
	{
		String best = null;
		double bestSecs = Double.MAX_VALUE;
		for (KillRecord r : kills)
		{
			double s = r.durationSeconds();
			if (!r.isDied() && s >= 0 && s < bestSecs)
			{
				bestSecs = s;
				best = r.getDuration();
			}
		}
		return best;
	}

	/** Damage taken per phase label, summed over the session, highest first. */
	public Map<String, Integer> damageByPhase()
	{
		Map<String, Integer> totals = new LinkedHashMap<>();
		for (KillRecord r : kills)
		{
			for (PhaseStat p : r.getPhases())
			{
				totals.merge(p.shortLabel(), p.getDamage(), Integer::sum);
			}
		}
		List<Map.Entry<String, Integer>> entries = new ArrayList<>(totals.entrySet());
		entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
		Map<String, Integer> sorted = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> e : entries)
		{
			sorted.put(e.getKey(), e.getValue());
		}
		return sorted;
	}

	/** The phase label that has cost the most damage this session, or null. */
	public String costliestPhase()
	{
		for (Map.Entry<String, Integer> e : damageByPhase().entrySet())
		{
			return e.getValue() > 0 ? e.getKey() : null;
		}
		return null;
	}
}
