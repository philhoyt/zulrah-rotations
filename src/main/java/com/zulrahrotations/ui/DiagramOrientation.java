package com.zulrahrotations.ui;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Which compass direction sits at the top of the shrine diagram. */
@Getter
@RequiredArgsConstructor
public enum DiagramOrientation
{
	SOUTH("South at top (wiki chart)", 0.0, "S"),
	NORTH("North at top", Math.PI, "N"),
	EAST("East at top", Math.PI / 2, "E"),
	WEST("West at top", -Math.PI / 2, "W");

	private final String label;
	/** Rotation applied to the south-up geometry, in radians. */
	private final double radians;
	/** Compass letter shown at the top of the diagram. */
	private final String topLetter;

	@Override
	public String toString()
	{
		return label;
	}
}
