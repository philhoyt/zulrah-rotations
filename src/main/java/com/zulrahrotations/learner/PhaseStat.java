package com.zulrahrotations.learner;

import lombok.Builder;
import lombok.Value;

/**
 * What happened to the player during one manually selected phase of one kill. The phase
 * number and label come from the player's own stepping, so they are only as accurate as the
 * stepping was.
 */
@Value
@Builder
public class PhaseStat
{
	int phaseNumber;
	String label;
	/** Total damage taken, venom included. */
	int damage;
	int venomDamage;
	/** Damaging hitsplats that were not venom or poison. */
	int hits;
	int food;
	int potions;

	public String shortLabel()
	{
		return "P" + phaseNumber + " " + label;
	}
}
