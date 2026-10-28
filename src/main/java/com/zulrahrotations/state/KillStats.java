package com.zulrahrotations.state;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * Kill counter and timer fed only by chat messages and by entering or leaving the shrine.
 * Pure Java so it can be tested without a client.
 */
public final class KillStats
{
	private static final Pattern KILL_COUNT = Pattern.compile("Your Zulrah kill count is: ?([\\d,]+)");
	private static final Pattern FIGHT_DURATION = Pattern.compile("Fight duration: ?(\\d+:\\d{2}(?:\\.\\d+)?)");

	@Getter
	private int sessionKills;
	/** Kill count as last reported by the game, or -1 when unknown. */
	@Getter
	private int reportedKillCount = -1;
	/** Last fight duration text as the game printed it, or null. */
	@Getter
	private String lastFightDuration;
	private Instant timerStart;
	private Duration frozen;

	/**
	 * Feed a game chat message (tags already stripped). Returns true when it was a Zulrah
	 * kill count or fight duration message.
	 */
	/** Whether a game message is the kill count line that marks the end of a kill. */
	public static boolean isKillCount(String message)
	{
		return message != null && KILL_COUNT.matcher(message).find();
	}

	public boolean onChatMessage(String message)
	{
		if (message == null)
		{
			return false;
		}
		Matcher kc = KILL_COUNT.matcher(message);
		if (kc.find())
		{
			sessionKills++;
			try
			{
				reportedKillCount = Integer.parseInt(kc.group(1).replace(",", ""));
			}
			catch (NumberFormatException ignored)
			{
				reportedKillCount = -1;
			}
			stopTimer();
			return true;
		}
		Matcher fd = FIGHT_DURATION.matcher(message);
		if (fd.find())
		{
			lastFightDuration = fd.group(1);
			return true;
		}
		return false;
	}

	/** Start (or restart) the timer, normally when the player enters the shrine instance. */
	public void startTimer()
	{
		timerStart = Instant.now();
		frozen = null;
	}

	/** Freeze the timer at its current value, normally on a kill. */
	public void stopTimer()
	{
		if (timerStart != null && frozen == null)
		{
			frozen = Duration.between(timerStart, Instant.now());
		}
	}

	/** Clear the timer entirely, normally when leaving without a kill. */
	public void clearTimer()
	{
		timerStart = null;
		frozen = null;
	}

	public boolean isTimerRunning()
	{
		return timerStart != null && frozen == null;
	}

	/** Elapsed time, frozen or running, or null when no timer has been started. */
	public Duration elapsed()
	{
		if (frozen != null)
		{
			return frozen;
		}
		if (timerStart != null)
		{
			return Duration.between(timerStart, Instant.now());
		}
		return null;
	}

	/** "m:ss" text for the timer, or null when there is none. */
	public String elapsedText()
	{
		Duration d = elapsed();
		if (d == null)
		{
			return null;
		}
		long secs = Math.max(0, d.getSeconds());
		return String.format("%d:%02d", secs / 60, secs % 60);
	}

	public void resetSession()
	{
		sessionKills = 0;
		reportedKillCount = -1;
		lastFightDuration = null;
		clearTimer();
	}
}
