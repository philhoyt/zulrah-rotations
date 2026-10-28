package com.zulrahrotations.learner;

import com.zulrahrotations.data.Phase;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Records one kill at a time. The plugin feeds it the player's own hitsplats, prayer level
 * changes and chat messages, plus every phase change the player makes by hand; this class
 * buckets them by phase and produces a {@link KillRecord} when the kill ends.
 *
 * Pure Java: nothing here touches the client, and nothing here knows what Zulrah is doing.
 */
public final class KillRecorder
{
	static final String DEATH_MESSAGE = "Oh dear, you are dead!";
	private static final Pattern FOOD = Pattern.compile("^You eat (the|some|an?) ");
	private static final Pattern POTION = Pattern.compile("^You drink (some of )?(the|your) ");

	private boolean recording;
	private boolean died;
	private int prayerLevel = -1;
	private int prayerUsed;
	private final List<Bucket> buckets = new ArrayList<>();
	private Bucket current;

	public boolean isRecording()
	{
		return recording;
	}

	/** Begin a kill at the phase the player is on. Any kill in progress is discarded. */
	public void start(int phaseNumber, Phase phase, int prayerLevel)
	{
		abandon();
		recording = true;
		this.prayerLevel = prayerLevel;
		openBucket(phaseNumber, phase);
	}

	/** The player stepped to a phase. Consecutive calls for the same phase are ignored. */
	public void onPhase(int phaseNumber, Phase phase)
	{
		if (!recording)
		{
			return;
		}
		if (current != null && current.phaseNumber == phaseNumber && current.phase.equals(phase))
		{
			return;
		}
		openBucket(phaseNumber, phase);
	}

	/** A damaging hitsplat landed on the player. Venom and poison are tracked separately. */
	public void onDamage(int amount, boolean venom)
	{
		if (!recording || amount <= 0)
		{
			return;
		}
		current.damage += amount;
		if (venom)
		{
			current.venomDamage += amount;
		}
		else
		{
			current.hits++;
		}
	}

	/** The player's boosted prayer level changed. Only drops count as prayer used. */
	public void onPrayerLevel(int boostedLevel)
	{
		if (!recording)
		{
			return;
		}
		if (prayerLevel >= 0 && boostedLevel < prayerLevel)
		{
			prayerUsed += prayerLevel - boostedLevel;
		}
		prayerLevel = boostedLevel;
	}

	/**
	 * A game message. Counts food, potions and death.
	 *
	 * @return true if the message was one this recorder understands
	 */
	public boolean onChatMessage(String message)
	{
		if (message == null)
		{
			return false;
		}
		if (message.startsWith(DEATH_MESSAGE))
		{
			if (recording)
			{
				died = true;
			}
			return true;
		}
		if (FOOD.matcher(message).find())
		{
			if (recording)
			{
				current.food++;
			}
			return true;
		}
		if (POTION.matcher(message).find())
		{
			if (recording)
			{
				current.potions++;
			}
			return true;
		}
		return false;
	}

	/** Whether the death message has been seen during this kill. */
	public boolean hasDied()
	{
		return recording && died;
	}

	/**
	 * End the kill and return its record, or null if nothing was being recorded.
	 *
	 * @param rotationName name of the rotation the player had picked, or null
	 * @param duration     "m:ss" from the timer, or null
	 */
	public KillRecord finish(String rotationName, String duration)
	{
		if (!recording)
		{
			return null;
		}
		List<PhaseStat> phases = new ArrayList<>(buckets.size());
		int damage = 0;
		int venom = 0;
		int hits = 0;
		int food = 0;
		int potions = 0;
		for (Bucket b : buckets)
		{
			phases.add(PhaseStat.builder()
				.phaseNumber(b.phaseNumber)
				.label(b.phase.shortLabel())
				.damage(b.damage)
				.venomDamage(b.venomDamage)
				.hits(b.hits)
				.food(b.food)
				.potions(b.potions)
				.build());
			damage += b.damage;
			venom += b.venomDamage;
			hits += b.hits;
			food += b.food;
			potions += b.potions;
		}
		KillRecord record = new KillRecord(
			Instant.now(),
			rotationName != null ? rotationName : "Unknown rotation",
			duration,
			died,
			damage,
			venom,
			hits,
			food,
			potions,
			prayerUsed,
			phases);
		abandon();
		return record;
	}

	/** Discard the kill in progress, if any. */
	public void abandon()
	{
		recording = false;
		died = false;
		prayerLevel = -1;
		prayerUsed = 0;
		buckets.clear();
		current = null;
	}

	private void openBucket(int phaseNumber, Phase phase)
	{
		current = new Bucket(phaseNumber, phase);
		buckets.add(current);
	}

	private static final class Bucket
	{
		final int phaseNumber;
		final Phase phase;
		int damage;
		int venomDamage;
		int hits;
		int food;
		int potions;

		Bucket(int phaseNumber, Phase phase)
		{
			this.phaseNumber = phaseNumber;
			this.phase = phase;
		}
	}
}
