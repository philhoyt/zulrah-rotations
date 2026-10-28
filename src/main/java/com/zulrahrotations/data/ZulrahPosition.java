package com.zulrahrotations.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Where Zulrah surfaces. Names follow the OSRS Wiki, which describes the shrine with
 * the camera facing south: "south" is the far end beyond the closed side of the shrine,
 * "east" is to the player's left and "west" to the player's right.
 * The x/y values are unit coordinates on the shrine diagram in that orientation.
 */
@Getter
@RequiredArgsConstructor
public enum ZulrahPosition
{
	MIDDLE("Middle", 0.500, 0.600),
	SOUTH("South", 0.500, 0.085),
	EAST("East", 0.085, 0.550),
	WEST("West", 0.915, 0.550);

	private final String displayName;
	private final double x;
	private final double y;
}
