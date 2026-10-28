package com.zulrahrotations.learner;

import java.time.Instant;
import java.util.List;
import lombok.Value;

/**
 * One finished kill (or death) as the player experienced it: damage taken, supplies used and
 * prayer spent, broken down by the phases the player stepped through. Everything in here was
 * read from the player's own character or from chat; nothing was read from Zulrah.
 */
@Value
public class KillRecord
{
	Instant endedAt;
	/** Rotation name if the player had picked one by the end, otherwise "Unknown rotation". */
	String rotationName;
	/** "m:ss" or "m:ss.d", or null when no timer was running. */
	String duration;
	boolean died;
	int damage;
	int venomDamage;
	int hits;
	int food;
	int potions;
	int prayerUsed;
	List<PhaseStat> phases;

	/** The phase in which the most damage was taken, or null if no damage was taken at all. */
	public PhaseStat worstPhase()
	{
		PhaseStat worst = null;
		for (PhaseStat p : phases)
		{
			if (p.getDamage() > 0 && (worst == null || p.getDamage() > worst.getDamage()))
			{
				worst = p;
			}
		}
		return worst;
	}

	/** Duration in seconds, or -1 when unknown or unparseable. */
	public double durationSeconds()
	{
		return parseDuration(duration);
	}

	static double parseDuration(String text)
	{
		if (text == null)
		{
			return -1;
		}
		String[] parts = text.trim().split(":");
		if (parts.length != 2)
		{
			return -1;
		}
		try
		{
			return Integer.parseInt(parts[0]) * 60 + Double.parseDouble(parts[1]);
		}
		catch (NumberFormatException ex)
		{
			return -1;
		}
	}

	/** One-line summary for chat and the panel. */
	public String summaryText()
	{
		StringBuilder sb = new StringBuilder();
		if (died)
		{
			sb.append("Died").append(duration != null ? " after " + duration : "");
		}
		else
		{
			sb.append("Kill").append(duration != null ? " in " + duration : "");
		}
		sb.append(". Took ").append(damage).append(" damage");
		if (venomDamage > 0)
		{
			sb.append(" (").append(venomDamage).append(" venom)");
		}
		sb.append(" over ").append(hits).append(hits == 1 ? " hit" : " hits");
		sb.append(", ").append(food).append(" food, ").append(potions).append(potions == 1 ? " potion" : " potions");
		sb.append(", ").append(prayerUsed).append(" prayer.");
		PhaseStat worst = worstPhase();
		if (worst != null)
		{
			sb.append(" Worst phase: ").append(worst.shortLabel()).append(" (").append(worst.getDamage()).append(").");
		}
		return sb.toString();
	}
}
