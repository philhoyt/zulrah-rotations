package com.zulrahrotations.state;

import com.zulrahrotations.data.Phase;
import com.zulrahrotations.data.Rotation;
import java.util.Set;
import lombok.Value;

/** One option offered to the player when the rotations split. */
@Value
public class RotationChoice
{
	/** Button text, e.g. "Crimson", "Serp", "Crimson A". */
	String label;
	/** Rotations that this choice keeps. */
	Set<Rotation> rotations;
	/** The phase the player would see after choosing. */
	Phase nextPhase;
}
