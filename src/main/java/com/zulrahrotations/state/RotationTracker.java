package com.zulrahrotations.state;

import com.zulrahrotations.data.Phase;
import com.zulrahrotations.data.Rotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Pure-Java state machine for the player's position in a Zulrah rotation.
 *
 * The state is a set of candidate rotations plus a phase index. All candidates always agree
 * on every phase up to and including the current index, so the current phase is well defined
 * even before the player has said which rotation it is. When the candidates would disagree
 * about the next phase the tracker is "at a split" and the player has to pick.
 *
 * This class has no RuneLite dependencies so it can be unit tested on its own. Every change
 * comes from an explicit call made on the player's behalf (button, hotkey or chat command).
 */
public final class RotationTracker
{
	public interface Listener
	{
		void onTrackerChanged(RotationTracker tracker);
	}

	private final List<Listener> listeners = new CopyOnWriteArrayList<>();
	private EnumSet<Rotation> candidates = EnumSet.allOf(Rotation.class);
	private int phaseIndex = 0;

	public void addListener(Listener listener)
	{
		listeners.add(listener);
	}

	public void removeListener(Listener listener)
	{
		listeners.remove(listener);
	}

	// ---- queries -------------------------------------------------------------------------

	public Set<Rotation> getCandidates()
	{
		return Collections.unmodifiableSet(candidates);
	}

	public boolean isRotationKnown()
	{
		return candidates.size() == 1;
	}

	/** The single known rotation, or null while more than one is still possible. */
	public Rotation getRotation()
	{
		return isRotationKnown() ? candidates.iterator().next() : null;
	}

	public int getPhaseIndex()
	{
		return phaseIndex;
	}

	/** One-based phase number as players and the wiki count them. */
	public int getPhaseNumber()
	{
		return phaseIndex + 1;
	}

	/** Phase count of the known rotation, or the shared prefix length while unknown. */
	public int getPhaseCount()
	{
		return isRotationKnown() ? getRotation().getPhases().size() : sharedPrefixLength();
	}

	public Phase getCurrentPhase()
	{
		return phaseAt(phaseIndex);
	}

	/** True when candidates disagree about the next phase and the player must choose. */
	public boolean isAtSplit()
	{
		return !isRotationKnown() && phaseIndex + 1 >= sharedPrefixLength();
	}

	/** True when the current phase is the last of a known rotation. */
	public boolean isAtEnd()
	{
		return isRotationKnown() && phaseIndex == getRotation().getPhases().size() - 1;
	}

	/**
	 * The phase that follows the current one, the opening phase when the rotation is about to
	 * restart, or null when the player first has to pick a rotation.
	 */
	public Phase getNextPhase()
	{
		if (isAtSplit())
		{
			return null;
		}
		if (isAtEnd())
		{
			return Rotation.opening();
		}
		return phaseAt(phaseIndex + 1);
	}

	/** Options available at a split, grouped so identical next phases share a button. */
	public List<RotationChoice> getChoices()
	{
		if (!isAtSplit())
		{
			return Collections.emptyList();
		}
		Map<Phase, EnumSet<Rotation>> groups = new LinkedHashMap<>();
		for (Rotation r : candidates)
		{
			Phase next = r.getPhases().get(phaseIndex + 1);
			groups.computeIfAbsent(next, k -> EnumSet.noneOf(Rotation.class)).add(r);
		}
		List<RotationChoice> choices = new ArrayList<>();
		for (Map.Entry<Phase, EnumSet<Rotation>> e : groups.entrySet())
		{
			choices.add(new RotationChoice(labelFor(e.getValue()), e.getValue(), e.getKey()));
		}
		return choices;
	}

	/** Phases to show on a timeline: the whole rotation when known, else the shared prefix. */
	public List<Phase> getTimeline()
	{
		List<Phase> all = candidates.iterator().next().getPhases();
		return all.subList(0, isRotationKnown() ? all.size() : sharedPrefixLength());
	}

	/** Number of leading phases every candidate has in common. */
	public int sharedPrefixLength()
	{
		return sharedPrefixLength(candidates);
	}

	/** Short human description of what the tracker believes. */
	public String getStatusText()
	{
		if (isRotationKnown())
		{
			return "Rotation " + getRotation().getNumber() + " · " + getRotation().getDisplayName();
		}
		if (isAtSplit())
		{
			return "Pick the rotation";
		}
		return "Rotation not known yet";
	}

	// ---- commands ------------------------------------------------------------------------

	/** Advance one phase. Returns false at a split, where the player must pick first. */
	public boolean next()
	{
		if (isAtSplit())
		{
			return false;
		}
		if (isAtEnd())
		{
			reset();
			return true;
		}
		phaseIndex++;
		changed();
		return true;
	}

	/** Step back one phase. Returns false at the opening phase. */
	public boolean previous()
	{
		if (phaseIndex == 0)
		{
			return false;
		}
		phaseIndex--;
		changed();
		return true;
	}

	/** Back to the shared opening phase with every rotation possible. */
	public void reset()
	{
		candidates = EnumSet.allOf(Rotation.class);
		phaseIndex = 0;
		changed();
	}

	/**
	 * Narrow the candidates from a split choice. When the tracker was at a split, the choice
	 * is the player observing the next phase, so it also advances by one.
	 */
	public boolean pick(Set<Rotation> rotations)
	{
		if (rotations == null || rotations.isEmpty() || !candidates.containsAll(rotations))
		{
			return false;
		}
		boolean wasAtSplit = isAtSplit();
		candidates = EnumSet.copyOf(rotations);
		if (wasAtSplit)
		{
			phaseIndex++;
		}
		changed();
		return true;
	}

	/** Convenience for {@link #pick(Set)} with a single choice. */
	public boolean pick(RotationChoice choice)
	{
		return choice != null && pick(choice.getRotations());
	}

	/**
	 * Override the rotation outright (hotkey or chat command), keeping the current phase when
	 * it exists in that rotation and clamping otherwise. Does not advance.
	 */
	public boolean setRotation(Rotation rotation)
	{
		if (rotation == null)
		{
			return false;
		}
		candidates = EnumSet.of(rotation);
		phaseIndex = Math.min(phaseIndex, rotation.getPhases().size() - 1);
		changed();
		return true;
	}

	/**
	 * Jump to a one-based phase number. While the rotation is unknown only phases inside the
	 * shared prefix are reachable. Returns false when the number is out of range.
	 */
	public boolean jumpTo(int phaseNumber)
	{
		int index = phaseNumber - 1;
		if (index < 0 || index >= getPhaseCount())
		{
			return false;
		}
		phaseIndex = index;
		changed();
		return true;
	}

	// ---- persistence ---------------------------------------------------------------------

	public TrackerSnapshot snapshot()
	{
		List<String> names = candidates.stream().map(Enum::name).collect(Collectors.toList());
		return new TrackerSnapshot(names, phaseIndex);
	}

	/** Restore a snapshot; returns false and leaves the state alone when it is invalid. */
	public boolean restore(TrackerSnapshot snapshot)
	{
		if (snapshot == null || snapshot.getCandidates() == null || snapshot.getCandidates().isEmpty())
		{
			return false;
		}
		EnumSet<Rotation> set = EnumSet.noneOf(Rotation.class);
		for (String name : snapshot.getCandidates())
		{
			try
			{
				set.add(Rotation.valueOf(name));
			}
			catch (IllegalArgumentException ex)
			{
				return false;
			}
		}
		int index = snapshot.getPhaseIndex();
		int limit = set.size() == 1 ? set.iterator().next().getPhases().size() : sharedPrefixLength(set);
		if (index < 0 || index >= limit)
		{
			return false;
		}
		candidates = set;
		phaseIndex = index;
		changed();
		return true;
	}

	// ---- helpers -------------------------------------------------------------------------

	private Phase phaseAt(int index)
	{
		return candidates.iterator().next().getPhases().get(index);
	}

	private static int sharedPrefixLength(Set<Rotation> rotations)
	{
		List<List<Phase>> lists = rotations.stream().map(Rotation::getPhases).collect(Collectors.toList());
		int shortest = lists.stream().mapToInt(List::size).min().orElse(0);
		for (int i = 0; i < shortest; i++)
		{
			Phase first = lists.get(0).get(i);
			for (List<Phase> l : lists)
			{
				if (!l.get(i).equals(first))
				{
					return i;
				}
			}
		}
		return shortest;
	}

	private static String labelFor(Set<Rotation> rotations)
	{
		if (rotations.size() == 1)
		{
			return rotations.iterator().next().getDisplayName();
		}
		Set<String> families = rotations.stream().map(Rotation::getFamily).collect(Collectors.toSet());
		if (families.size() == 1)
		{
			return families.iterator().next();
		}
		return rotations.stream().map(Rotation::getDisplayName).collect(Collectors.joining(" / "));
	}

	private void changed()
	{
		for (Listener l : listeners)
		{
			l.onTrackerChanged(this);
		}
	}
}
