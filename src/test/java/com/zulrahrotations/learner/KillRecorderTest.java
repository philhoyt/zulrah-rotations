package com.zulrahrotations.learner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.zulrahrotations.data.Phase;
import com.zulrahrotations.data.Rotation;
import org.junit.Test;

public class KillRecorderTest
{
	private static final Phase P1 = Rotation.opening();
	private static final Phase P2 = Rotation.TANZ.getPhases().get(1);
	private static final Phase P3 = Rotation.TANZ.getPhases().get(2);

	@Test
	public void nothingIsRecordedBeforeStart()
	{
		KillRecorder r = new KillRecorder();
		r.onDamage(10, false);
		r.onChatMessage("You eat the shark.");
		r.onPrayerLevel(50);
		assertFalse(r.isRecording());
		assertNull(r.finish("Tanz", "1:00"));
	}

	@Test
	public void damageIsBucketedByThePhaseThePlayerSteppedTo()
	{
		KillRecorder r = new KillRecorder();
		r.start(1, P1, 70);
		r.onDamage(10, false);
		r.onDamage(0, false);
		r.onPhase(2, P2);
		r.onDamage(6, true);
		r.onDamage(4, false);
		r.onPhase(3, P3);

		KillRecord k = r.finish("Tanz", "1:12");
		assertNotNull(k);
		assertEquals(3, k.getPhases().size());
		assertEquals(10, k.getPhases().get(0).getDamage());
		assertEquals(1, k.getPhases().get(0).getHits());
		assertEquals(10, k.getPhases().get(1).getDamage());
		assertEquals(6, k.getPhases().get(1).getVenomDamage());
		assertEquals(1, k.getPhases().get(1).getHits());
		assertEquals(0, k.getPhases().get(2).getDamage());
		assertEquals(20, k.getDamage());
		assertEquals(6, k.getVenomDamage());
		assertEquals(2, k.getHits());
		assertEquals("Tanz", k.getRotationName());
		assertEquals("1:12", k.getDuration());
		assertFalse(k.isDied());
		assertFalse(r.isRecording());
	}

	@Test
	public void repeatingTheSamePhaseDoesNotOpenANewBucket()
	{
		KillRecorder r = new KillRecorder();
		r.start(1, P1, 70);
		r.onPhase(1, P1);
		r.onPhase(1, P1);
		r.onDamage(3, false);
		KillRecord k = r.finish(null, null);
		assertEquals(1, k.getPhases().size());
		assertEquals("Unknown rotation", k.getRotationName());
		assertNull(k.getDuration());
	}

	@Test
	public void foodAndPotionsComeFromChatAndLandInTheCurrentPhase()
	{
		KillRecorder r = new KillRecorder();
		r.start(1, P1, 70);
		assertTrue(r.onChatMessage("You eat the shark."));
		assertTrue(r.onChatMessage("You eat the cooked karambwan."));
		r.onPhase(2, P2);
		assertTrue(r.onChatMessage("You drink some of your Saradomin brew."));
		assertTrue(r.onChatMessage("You drink the last of your super restore potion."));
		assertFalse(r.onChatMessage("You have finished your potion."));
		assertFalse(r.onChatMessage("Your Zulrah kill count is: 5."));

		KillRecord k = r.finish("Tanz", "1:00");
		assertEquals(2, k.getPhases().get(0).getFood());
		assertEquals(0, k.getPhases().get(0).getPotions());
		assertEquals(2, k.getPhases().get(1).getPotions());
		assertEquals(2, k.getFood());
		assertEquals(2, k.getPotions());
	}

	@Test
	public void onlyPrayerDropsCountAsPrayerUsed()
	{
		KillRecorder r = new KillRecorder();
		r.start(1, P1, 70);
		r.onPrayerLevel(60);
		r.onPrayerLevel(65);
		r.onPrayerLevel(50);
		assertEquals(25, r.finish("Tanz", "1:00").getPrayerUsed());
	}

	@Test
	public void deathIsFlaggedFromTheDeathMessage()
	{
		KillRecorder r = new KillRecorder();
		r.start(1, P1, 70);
		assertFalse(r.hasDied());
		assertTrue(r.onChatMessage("Oh dear, you are dead!"));
		assertTrue(r.hasDied());
		KillRecord k = r.finish("Serp", "0:48");
		assertTrue(k.isDied());
		assertTrue(k.summaryText().startsWith("Died after 0:48."));
	}

	@Test
	public void abandonDropsTheKillInProgress()
	{
		KillRecorder r = new KillRecorder();
		r.start(1, P1, 70);
		r.onDamage(9, false);
		r.abandon();
		assertFalse(r.isRecording());
		assertNull(r.finish("Tanz", "1:00"));
	}

	@Test
	public void startAgainDiscardsThePreviousKill()
	{
		KillRecorder r = new KillRecorder();
		r.start(1, P1, 70);
		r.onDamage(9, false);
		r.start(1, P1, 70);
		KillRecord k = r.finish("Tanz", "1:00");
		assertEquals(0, k.getDamage());
		assertEquals(1, k.getPhases().size());
	}

	@Test
	public void worstPhaseAndSummaryText()
	{
		KillRecorder r = new KillRecorder();
		r.start(1, P1, 70);
		r.onDamage(5, false);
		r.onPhase(2, P2);
		r.onDamage(12, false);
		r.onDamage(8, true);
		r.onChatMessage("You eat the shark.");
		r.onPrayerLevel(40);
		KillRecord k = r.finish("Tanz", "1:12");

		PhaseStat worst = k.worstPhase();
		assertEquals(2, worst.getPhaseNumber());
		assertEquals("P2 " + P2.shortLabel(), worst.shortLabel());
		assertEquals("Kill in 1:12. Took 25 damage (8 venom) over 2 hits, 1 food, 0 potions, 30 prayer. Worst phase: P2 "
			+ P2.shortLabel() + " (20).", k.summaryText());
		assertEquals(72.0, k.durationSeconds(), 0.001);
	}

	@Test
	public void noDamageMeansNoWorstPhase()
	{
		KillRecorder r = new KillRecorder();
		r.start(1, P1, 70);
		KillRecord k = r.finish("Tanz", "1:05.4");
		assertNull(k.worstPhase());
		assertEquals(65.4, k.durationSeconds(), 0.001);
		assertEquals("Kill in 1:05.4. Took 0 damage over 0 hits, 0 food, 0 potions, 0 prayer.", k.summaryText());
	}
}
