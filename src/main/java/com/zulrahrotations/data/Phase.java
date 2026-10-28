package com.zulrahrotations.data;

import lombok.Builder;
import lombok.Value;

/**
 * One phase of a rotation: an immutable bundle of guide data.
 * Attack, venom and snakeling counts come from the OSRS Wiki rotation descriptions.
 */
@Value
@Builder
public class Phase
{
	ZulrahForm form;
	ZulrahPosition position;
	StandTile tile;
	/** For {@link ZulrahForm#JAD} this is the prayer to start with; swap on every attack. */
	Prayer prayer;
	int attacks;
	int venom;
	int snakelings;
	/** Optional short remark shown under the phase, may be null. */
	String note;

	/** "Serpentine - South" style one-liner for compact displays. */
	public String shortLabel()
	{
		return form.getDisplayName() + " · " + position.getDisplayName();
	}

	/** Prayer guidance text, with Jad-phase wording when needed. */
	public String prayerText()
	{
		if (form.isJad())
		{
			return prayer.getDisplayName() + " first, swap every attack";
		}
		return prayer.getDisplayName();
	}

	/** Compact list of what happens, e.g. "5 ranged, 2 venom, 4 snakelings". */
	public String eventsText()
	{
		StringBuilder sb = new StringBuilder();
		if (attacks > 0)
		{
			sb.append(attacks).append(' ').append(form.getAttacksWith().toLowerCase());
		}
		if (venom > 0)
		{
			append(sb, venom + " venom");
		}
		if (snakelings > 0)
		{
			append(sb, snakelings + (snakelings == 1 ? " snakeling" : " snakelings"));
		}
		return sb.length() == 0 ? "No direct attacks" : sb.toString();
	}

	private static void append(StringBuilder sb, String part)
	{
		if (sb.length() > 0)
		{
			sb.append(", ");
		}
		sb.append(part);
	}
}
