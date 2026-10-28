package com.zulrahrotations.state;

import com.zulrahrotations.data.Phase;
import com.zulrahrotations.data.Prayer;
import com.zulrahrotations.data.Rotation;
import com.zulrahrotations.data.ZulrahForm;
import com.zulrahrotations.data.ZulrahPosition;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class RotationTrackerTest
{
	private RotationTracker tracker;

	@Before
	public void setUp()
	{
		tracker = new RotationTracker();
	}

	// ---- initial state -------------------------------------------------------------------

	@Test
	public void startsAtOpeningWithEveryRotationPossible()
	{
		assertEquals(EnumSet.allOf(Rotation.class), tracker.getCandidates());
		assertFalse(tracker.isRotationKnown());
		assertNull(tracker.getRotation());
		assertEquals(1, tracker.getPhaseNumber());
		assertEquals(Rotation.opening(), tracker.getCurrentPhase());
	}

	@Test
	public void openingPhaseIsASplitBetweenThreeFamilies()
	{
		assertTrue(tracker.isAtSplit());
		assertNull(tracker.getNextPhase());
		List<RotationChoice> choices = tracker.getChoices();
		assertEquals(3, choices.size());
		assertEquals("Crimson", choices.get(0).getLabel());
		assertEquals(EnumSet.of(Rotation.CRIMSON_A, Rotation.CRIMSON_B), choices.get(0).getRotations());
		assertEquals("Serp", choices.get(1).getLabel());
		assertEquals("Tanz", choices.get(2).getLabel());
		assertEquals("Pick the rotation", tracker.getStatusText());
	}

	// ---- stepping ------------------------------------------------------------------------

	@Test
	public void nextIsBlockedAtASplit()
	{
		assertFalse(tracker.next());
		assertEquals(1, tracker.getPhaseNumber());
	}

	@Test
	public void pickingAtASplitAdvancesOnePhase()
	{
		assertTrue(tracker.pick(tracker.getChoices().get(0)));
		assertEquals(2, tracker.getPhaseNumber());
		assertEquals(ZulrahForm.MAGMA, tracker.getCurrentPhase().getForm());
		assertFalse(tracker.isRotationKnown());
		assertEquals("Rotation not known yet", tracker.getStatusText());
	}

	@Test
	public void crimsonSplitsAgainAfterPhaseThree()
	{
		tracker.pick(EnumSet.of(Rotation.CRIMSON_A, Rotation.CRIMSON_B));
		assertTrue(tracker.next()); // phase 3, shared tanzanite
		assertEquals(3, tracker.getPhaseNumber());
		assertTrue(tracker.isAtSplit());
		assertFalse(tracker.next());
		List<RotationChoice> choices = tracker.getChoices();
		assertEquals(2, choices.size());
		assertEquals("Crimson A", choices.get(0).getLabel());
		assertEquals(ZulrahPosition.SOUTH, choices.get(0).getNextPhase().getPosition());
		assertEquals("Crimson B", choices.get(1).getLabel());
		assertEquals(ZulrahPosition.WEST, choices.get(1).getNextPhase().getPosition());

		tracker.pick(choices.get(1));
		assertSame(Rotation.CRIMSON_B, tracker.getRotation());
		assertEquals(4, tracker.getPhaseNumber());
		assertEquals("Rotation 2 · Crimson B", tracker.getStatusText());
	}

	@Test
	public void previousStepsBackAndStopsAtOpening()
	{
		tracker.setRotation(Rotation.SERP);
		tracker.next();
		tracker.next();
		assertEquals(3, tracker.getPhaseNumber());
		assertTrue(tracker.previous());
		assertTrue(tracker.previous());
		assertEquals(1, tracker.getPhaseNumber());
		assertFalse(tracker.previous());
		assertEquals(1, tracker.getPhaseNumber());
		assertSame("going back keeps the chosen rotation", Rotation.SERP, tracker.getRotation());
	}

	@Test
	public void nextAtTheLastPhaseResetsToOpening()
	{
		tracker.setRotation(Rotation.TANZ);
		int count = Rotation.TANZ.getPhases().size();
		assertTrue(tracker.jumpTo(count));
		assertTrue(tracker.isAtEnd());
		assertEquals(Rotation.opening(), tracker.getNextPhase());
		assertTrue(tracker.next());
		assertEquals(1, tracker.getPhaseNumber());
		assertFalse(tracker.isRotationKnown());
		assertEquals(EnumSet.allOf(Rotation.class), tracker.getCandidates());
	}

	@Test
	public void nextPhasePreviewFollowsTheData()
	{
		tracker.setRotation(Rotation.CRIMSON_A);
		for (int i = 0; i < Rotation.CRIMSON_A.getPhases().size() - 1; i++)
		{
			Phase expected = Rotation.CRIMSON_A.getPhases().get(i + 1);
			assertEquals(expected, tracker.getNextPhase());
			assertTrue(tracker.next());
		}
	}

	// ---- reset ---------------------------------------------------------------------------

	@Test
	public void resetClearsRotationAndPhase()
	{
		tracker.setRotation(Rotation.CRIMSON_A);
		tracker.jumpTo(7);
		tracker.reset();
		assertFalse(tracker.isRotationKnown());
		assertEquals(1, tracker.getPhaseNumber());
		assertTrue(tracker.isAtSplit());
	}

	// ---- overrides (hotkeys and chat commands) --------------------------------------------

	@Test
	public void setRotationKeepsPhaseWhenItExists()
	{
		tracker.pick(EnumSet.of(Rotation.CRIMSON_A, Rotation.CRIMSON_B));
		tracker.next();
		assertEquals(3, tracker.getPhaseNumber());
		assertTrue(tracker.setRotation(Rotation.CRIMSON_A));
		assertEquals(3, tracker.getPhaseNumber());
		assertTrue(tracker.isRotationKnown());
		assertFalse(tracker.isAtSplit());
		assertNotNull(tracker.getNextPhase());
	}

	@Test
	public void setRotationClampsPhaseToTheShorterRotation()
	{
		tracker.setRotation(Rotation.TANZ);
		tracker.jumpTo(Rotation.TANZ.getPhases().size());
		tracker.setRotation(Rotation.CRIMSON_A);
		assertEquals(Rotation.CRIMSON_A.getPhases().size(), tracker.getPhaseNumber());
	}

	@Test
	public void setRotationRejectsNull()
	{
		assertFalse(tracker.setRotation(null));
		assertFalse(tracker.isRotationKnown());
	}

	@Test
	public void rotationLookupByNumber()
	{
		assertSame(Rotation.CRIMSON_A, Rotation.byNumber(1));
		assertSame(Rotation.CRIMSON_B, Rotation.byNumber(2));
		assertSame(Rotation.SERP, Rotation.byNumber(3));
		assertSame(Rotation.TANZ, Rotation.byNumber(4));
		assertNull(Rotation.byNumber(0));
		assertNull(Rotation.byNumber(5));
	}

	@Test
	public void jumpToRespectsBoundsOfKnownRotation()
	{
		tracker.setRotation(Rotation.SERP);
		int count = Rotation.SERP.getPhases().size();
		assertFalse(tracker.jumpTo(0));
		assertFalse(tracker.jumpTo(count + 1));
		assertTrue(tracker.jumpTo(count));
		assertEquals(count, tracker.getPhaseNumber());
		assertTrue(tracker.jumpTo(1));
		assertEquals(1, tracker.getPhaseNumber());
	}

	@Test
	public void jumpToIsLimitedToTheSharedPrefixWhileUnknown()
	{
		assertFalse("only the opening is shared by all four", tracker.jumpTo(2));
		tracker.pick(EnumSet.of(Rotation.CRIMSON_A, Rotation.CRIMSON_B));
		assertTrue(tracker.jumpTo(3));
		assertFalse(tracker.jumpTo(4));
		assertTrue(tracker.jumpTo(1));
	}

	@Test
	public void pickRejectsRotationsOutsideTheCandidates()
	{
		tracker.pick(EnumSet.of(Rotation.SERP));
		assertFalse(tracker.pick(EnumSet.of(Rotation.TANZ)));
		assertFalse(tracker.pick(Collections.<Rotation>emptySet()));
		assertFalse(tracker.pick((Set<Rotation>) null));
		assertFalse(tracker.pick((RotationChoice) null));
		assertSame(Rotation.SERP, tracker.getRotation());
	}

	// ---- timeline ------------------------------------------------------------------------

	@Test
	public void timelineShowsSharedPrefixUntilRotationIsKnown()
	{
		assertEquals(1, tracker.getTimeline().size());
		tracker.pick(EnumSet.of(Rotation.CRIMSON_A, Rotation.CRIMSON_B));
		assertEquals(3, tracker.getTimeline().size());
		tracker.setRotation(Rotation.CRIMSON_B);
		assertEquals(Rotation.CRIMSON_B.getPhases(), tracker.getTimeline());
	}

	// ---- persistence ---------------------------------------------------------------------

	@Test
	public void snapshotRoundTrip()
	{
		tracker.pick(EnumSet.of(Rotation.CRIMSON_A, Rotation.CRIMSON_B));
		tracker.next();
		TrackerSnapshot snap = tracker.snapshot();

		RotationTracker other = new RotationTracker();
		assertTrue(other.restore(snap));
		assertEquals(tracker.getCandidates(), other.getCandidates());
		assertEquals(tracker.getPhaseNumber(), other.getPhaseNumber());
		assertEquals(tracker.getCurrentPhase(), other.getCurrentPhase());
	}

	@Test
	public void restoreRejectsBadSnapshots()
	{
		assertFalse(tracker.restore(null));
		assertFalse(tracker.restore(new TrackerSnapshot(Collections.<String>emptyList(), 0)));
		assertFalse(tracker.restore(new TrackerSnapshot(Collections.singletonList("NOPE"), 0)));
		assertFalse(tracker.restore(new TrackerSnapshot(Collections.singletonList("SERP"), 99)));
		assertFalse(tracker.restore(new TrackerSnapshot(Collections.singletonList("SERP"), -1)));
		assertFalse("index beyond shared prefix while ambiguous",
			tracker.restore(new TrackerSnapshot(Arrays.asList("CRIMSON_A", "CRIMSON_B"), 3)));
		assertEquals(1, tracker.getPhaseNumber());
		assertFalse(tracker.isRotationKnown());
	}

	// ---- listeners -----------------------------------------------------------------------

	@Test
	public void listenersFireOnEveryChangeOnly()
	{
		AtomicInteger calls = new AtomicInteger();
		RotationTracker.Listener l = t -> calls.incrementAndGet();
		tracker.addListener(l);
		tracker.next();          // blocked at split, no change
		assertEquals(0, calls.get());
		tracker.setRotation(Rotation.SERP);
		tracker.next();
		tracker.previous();
		tracker.reset();
		assertEquals(4, calls.get());
		tracker.removeListener(l);
		tracker.setRotation(Rotation.TANZ);
		assertEquals(4, calls.get());
	}

	// ---- data sanity ---------------------------------------------------------------------

	@Test
	public void rotationDataMatchesWikiShape()
	{
		assertEquals(10, Rotation.CRIMSON_A.getPhases().size());
		assertEquals(10, Rotation.CRIMSON_B.getPhases().size());
		assertEquals(11, Rotation.SERP.getPhases().size());
		assertEquals(12, Rotation.TANZ.getPhases().size());
		for (Rotation r : Rotation.values())
		{
			assertEquals(Rotation.opening(), r.getPhases().get(0));
			long jads = r.getPhases().stream().filter(p -> p.getForm().isJad()).count();
			assertEquals("exactly one Jad phase in " + r, 1, jads);
			Phase jad = r.getPhases().get(r.getPhases().size() - 2);
			assertTrue("Jad is second to last in " + r, jad.getForm().isJad());
			for (Phase p : r.getPhases())
			{
				assertNotNull(p.getForm());
				assertNotNull(p.getPosition());
				assertNotNull(p.getTile());
				assertNotNull(p.getPrayer());
				assertTrue(p.getAttacks() >= 0 && p.getVenom() >= 0 && p.getSnakelings() >= 0);
			}
		}
		// Crimson rotations share the first three phases, Serp and Tanz only the opening.
		assertEquals(Rotation.CRIMSON_A.getPhases().subList(0, 3), Rotation.CRIMSON_B.getPhases().subList(0, 3));
		assertNotEquals(Rotation.SERP.getPhases().get(1), Rotation.TANZ.getPhases().get(1));
		// Jad prayer order: crimson starts with missiles, serp and tanz with magic.
		assertEquals(Prayer.MISSILES, Rotation.CRIMSON_A.getPhases().get(8).getPrayer());
		assertEquals(Prayer.MISSILES, Rotation.CRIMSON_B.getPhases().get(8).getPrayer());
		assertEquals(Prayer.MAGIC, Rotation.SERP.getPhases().get(9).getPrayer());
		assertEquals(Prayer.MAGIC, Rotation.TANZ.getPhases().get(10).getPrayer());
	}

	@Test
	public void phaseTextHelpers()
	{
		Phase opening = Rotation.opening();
		assertEquals("Serpentine · Middle", opening.shortLabel());
		assertEquals("5 ranged, 4 venom", opening.eventsText());
		assertEquals("Protect from Missiles", opening.prayerText());

		Phase jad = Rotation.SERP.getPhases().get(9);
		assertEquals("Protect from Magic first, swap every attack", jad.prayerText());
		assertEquals("10 ranged / magic", jad.eventsText());

		Phase quiet = Rotation.CRIMSON_A.getPhases().get(6);
		assertEquals("3 venom, 4 snakelings", quiet.eventsText());
	}
}
