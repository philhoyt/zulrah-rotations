package com.zulrahrotations.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Overhead prayer a guide recommends for a phase. Static data, never read from the game. */
@Getter
@RequiredArgsConstructor
public enum Prayer
{
	NONE("None needed", "None"),
	MISSILES("Protect from Missiles", "Missiles"),
	MAGIC("Protect from Magic", "Magic");

	private final String displayName;
	private final String shortName;
}
