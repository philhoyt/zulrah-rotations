package com.zulrahrotations.data;

import static com.zulrahrotations.data.Prayer.MAGIC;
import static com.zulrahrotations.data.Prayer.MISSILES;
import static com.zulrahrotations.data.Prayer.NONE;
import static com.zulrahrotations.data.StandTile.EAST_PILLAR;
import static com.zulrahrotations.data.StandTile.START;
import static com.zulrahrotations.data.StandTile.START_MELEE;
import static com.zulrahrotations.data.StandTile.TOP;
import static com.zulrahrotations.data.StandTile.WEST_PILLAR;
import static com.zulrahrotations.data.ZulrahForm.JAD;
import static com.zulrahrotations.data.ZulrahForm.MAGMA;
import static com.zulrahrotations.data.ZulrahForm.SERPENTINE;
import static com.zulrahrotations.data.ZulrahForm.TANZANITE;
import static com.zulrahrotations.data.ZulrahPosition.EAST;
import static com.zulrahrotations.data.ZulrahPosition.MIDDLE;
import static com.zulrahrotations.data.ZulrahPosition.SOUTH;
import static com.zulrahrotations.data.ZulrahPosition.WEST;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

/**
 * The four Zulrah rotations as described on the OSRS Wiki (Zulrah/Strategies, "Rotation
 * overview"). Phase 1 of every rotation is the shared opening phase; the final phase of a
 * rotation is that same opening phase for the next rotation, so it is not repeated here.
 *
 * These lists are the only place the rotation data lives. Nothing here is read from the
 * game: the player steps through these phases by hand.
 */
@Getter
public enum Rotation
{
	CRIMSON_A(1, "Crimson A", "Crimson", Arrays.asList(
		Data.OPENING,
		Data.CRIMSON_P2,
		Data.CRIMSON_P3,
		Data.p(SERPENTINE, SOUTH, WEST_PILLAR, MISSILES, 5, 2, 4, "Snakelings, venom, snakelings"),
		Data.p(MAGMA, MIDDLE, WEST_PILLAR, NONE, 2, 0, 0, null),
		Data.p(TANZANITE, WEST, TOP, MAGIC, 5, 0, 0, null),
		Data.p(SERPENTINE, SOUTH, EAST_PILLAR, NONE, 0, 3, 4, null),
		Data.p(TANZANITE, SOUTH, EAST_PILLAR, MAGIC, 5, 2, 3, "Then alternating snakes and venom"),
		Data.p(JAD, WEST, WEST_PILLAR, MISSILES, 10, 4, 0, "Venom clouds after the attacks"),
		Data.p(MAGMA, MIDDLE, START_MELEE, NONE, 2, 0, 0, "Next: back to the middle")
	)),

	CRIMSON_B(2, "Crimson B", "Crimson", Arrays.asList(
		Data.OPENING,
		Data.CRIMSON_P2,
		Data.CRIMSON_P3,
		Data.p(SERPENTINE, WEST, WEST_PILLAR, NONE, 0, 3, 4, null),
		Data.p(TANZANITE, SOUTH, WEST_PILLAR, MAGIC, 5, 2, 4, "Snakelings, venom, snakelings"),
		Data.p(MAGMA, MIDDLE, WEST_PILLAR, NONE, 2, 0, 0, null),
		Data.p(SERPENTINE, EAST, TOP, MISSILES, 5, 0, 0, null),
		Data.p(TANZANITE, SOUTH, WEST_PILLAR, MAGIC, 5, 2, 3, "Then alternating snakes and venom"),
		Data.p(JAD, WEST, WEST_PILLAR, MISSILES, 10, 4, 0, "Venom clouds after the attacks"),
		Data.p(MAGMA, MIDDLE, START_MELEE, NONE, 2, 0, 0, "Next: back to the middle")
	)),

	SERP(3, "Serp", "Serp", Arrays.asList(
		Data.OPENING,
		Data.p(SERPENTINE, EAST, START, MISSILES, 5, 0, 3, null),
		Data.p(MAGMA, MIDDLE, EAST_PILLAR, NONE, 2, 3, 3, "Venom and snakelings, then melee"),
		Data.p(TANZANITE, WEST, TOP, MAGIC, 5, 0, 0, null),
		Data.p(SERPENTINE, SOUTH, TOP, MISSILES, 5, 0, 0, null),
		Data.p(TANZANITE, EAST, TOP, MAGIC, 5, 0, 0, null),
		Data.p(SERPENTINE, MIDDLE, WEST_PILLAR, NONE, 0, 3, 3, null),
		Data.p(SERPENTINE, WEST, WEST_PILLAR, MISSILES, 5, 0, 0, null),
		Data.p(TANZANITE, MIDDLE, EAST_PILLAR, MAGIC, 5, 2, 3, null),
		Data.p(JAD, EAST, EAST_PILLAR, MAGIC, 10, 0, 0, "No venom after this one"),
		Data.p(TANZANITE, MIDDLE, EAST_PILLAR, NONE, 0, 0, 4, "Next: back to the middle")
	)),

	TANZ(4, "Tanz", "Tanz", Arrays.asList(
		Data.OPENING,
		Data.p(TANZANITE, EAST, START, MAGIC, 6, 0, 4, "Snakelings first, then attacks"),
		Data.p(SERPENTINE, SOUTH, WEST_PILLAR, MISSILES, 4, 2, 0, null),
		Data.p(TANZANITE, WEST, TOP, MAGIC, 4, 0, 4, "Snakelings first, then attacks"),
		Data.p(MAGMA, MIDDLE, EAST_PILLAR, NONE, 2, 2, 0, null),
		Data.p(SERPENTINE, EAST, EAST_PILLAR, MISSILES, 4, 0, 0, null),
		Data.p(SERPENTINE, SOUTH, WEST_PILLAR, NONE, 0, 3, 6, "Run east pillar to west pillar"),
		Data.p(TANZANITE, WEST, WEST_PILLAR, MAGIC, 5, 0, 4, null),
		Data.p(SERPENTINE, MIDDLE, EAST_PILLAR, MISSILES, 4, 0, 0, null),
		Data.p(TANZANITE, MIDDLE, EAST_PILLAR, MAGIC, 4, 3, 0, null),
		Data.p(JAD, EAST, EAST_PILLAR, MAGIC, 8, 0, 0, "No venom after this one"),
		Data.p(TANZANITE, MIDDLE, EAST_PILLAR, NONE, 0, 0, 4, "Next: back to the middle")
	));

	/** Number used by hotkeys and the ::zulrot command. */
	private final int number;
	private final String displayName;
	/** Shared name for rotations that start identically ("Crimson" for A and B). */
	private final String family;
	/** Phases in order, index 0 being the shared opening phase. */
	private final List<Phase> phases;

	Rotation(int number, String displayName, String family, List<Phase> phases)
	{
		this.number = number;
		this.displayName = displayName;
		this.family = family;
		this.phases = Collections.unmodifiableList(phases);
	}

	/** Phase 1 of every rotation: Zulrah in the middle, green, filling the shrine with venom. */
	public static Phase opening()
	{
		return Data.OPENING;
	}

	public static Rotation byNumber(int number)
	{
		for (Rotation r : values())
		{
			if (r.number == number)
			{
				return r;
			}
		}
		return null;
	}

	/** Phase data shared between rotations. Kept in a nested class so enum constructors can use it. */
	private static final class Data
	{
		static final Phase OPENING = p(SERPENTINE, MIDDLE, START, MISSILES, 5, 4, 0,
			"No attacks when the kill starts");
		static final Phase CRIMSON_P2 = p(MAGMA, MIDDLE, START_MELEE, NONE, 2, 0, 0, null);
		static final Phase CRIMSON_P3 = p(TANZANITE, MIDDLE, START, MAGIC, 4, 0, 0,
			"A goes south next, B goes west");

		static Phase p(ZulrahForm form, ZulrahPosition position, StandTile tile, Prayer prayer,
			int attacks, int venom, int snakelings, String note)
		{
			return Phase.builder()
				.form(form)
				.position(position)
				.tile(tile)
				.prayer(prayer)
				.attacks(attacks)
				.venom(venom)
				.snakelings(snakelings)
				.note(note)
				.build();
		}
	}
}
