package com.zulrahrotations.ui;

import com.zulrahrotations.data.ZulrahForm;
import java.awt.Color;

/** Colours for Zulrah's forms, with a colourblind-friendly alternative. */
public final class FormPalette
{
	private static final Color GREEN = new Color(68, 196, 84);
	private static final Color RED = new Color(226, 56, 56);
	private static final Color BLUE = new Color(70, 120, 255);

	// Okabe-Ito inspired: distinguishable under the common forms of colour vision deficiency.
	private static final Color CB_GREEN = new Color(0, 158, 115);
	private static final Color CB_RED = new Color(230, 120, 0);
	private static final Color CB_BLUE = new Color(0, 114, 178);

	public static final Color TILE = new Color(255, 214, 0);
	public static final Color LAND = new Color(96, 96, 96);
	public static final Color LAND_EDGE = new Color(140, 140, 140);
	public static final Color PILLAR = new Color(40, 40, 40);
	public static final Color OUTLINE = new Color(20, 20, 20);

	private FormPalette()
	{
	}

	public static Color colour(ZulrahForm form, boolean colourblind)
	{
		switch (form)
		{
			case MAGMA:
				return colourblind ? CB_RED : RED;
			case TANZANITE:
				return colourblind ? CB_BLUE : BLUE;
			case SERPENTINE:
			case JAD:
			default:
				return colourblind ? CB_GREEN : GREEN;
		}
	}
}
