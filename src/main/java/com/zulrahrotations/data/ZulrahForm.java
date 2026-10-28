package com.zulrahrotations.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The form Zulrah takes during a phase. This is static guide data attached to a
 * manually selected phase; the plugin never reads Zulrah's actual form from the game.
 */
@Getter
@RequiredArgsConstructor
public enum ZulrahForm
{
	SERPENTINE("Serpentine", "Green", "Ranged", "Magic"),
	MAGMA("Magma", "Red", "Melee", "Ranged"),
	TANZANITE("Tanzanite", "Blue", "Magic", "Ranged"),
	JAD("Jad", "Green", "Ranged / Magic", "Magic");

	/** Guide name of the form. */
	private final String displayName;
	/** Colour players usually call the form by. */
	private final String colourName;
	/** What Zulrah attacks with in this form. */
	private final String attacksWith;
	/** Attack style a guide recommends against this form. */
	private final String recommendedStyle;

	public boolean isJad()
	{
		return this == JAD;
	}
}
