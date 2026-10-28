package com.zulrahrotations.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Where a guide suggests standing during a phase, as unit coordinates on the shrine
 * diagram (camera facing south, see {@link ZulrahPosition}). The arms of the shrine run
 * down the diagram at x = 0.235 (east) and x = 0.765 (west).
 */
@Getter
@RequiredArgsConstructor
public enum StandTile
{
	START("Start tile", "Tip of the east arm, where you arrive", 0.235, 0.860),
	START_MELEE("Melee safe spot", "Just off the start tile, out of melee reach", 0.105, 0.800),
	EAST_PILLAR("East pillar", "Beside the east (left-hand) pillar", 0.235, 0.600),
	WEST_PILLAR("West pillar", "Beside the west (right-hand) pillar", 0.765, 0.600),
	TOP("Top of the shrine", "Far edge, between the two pillars", 0.500, 0.235);

	private final String displayName;
	private final String description;
	private final double x;
	private final double y;
}
