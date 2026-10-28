package com.zulrahrotations.ui;

import com.zulrahrotations.data.Phase;
import com.zulrahrotations.data.StandTile;
import com.zulrahrotations.data.ZulrahPosition;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/**
 * Draws the shrine as a horseshoe with Zulrah's position and the suggested stand tile for a
 * phase. Everything is vector-drawn so it stays crisp at any size and needs no image files.
 *
 * Geometry is defined with south at the top (the wiki chart orientation) and rotated for
 * other orientations.
 */
public final class ShrineDiagram
{
	private static final double ARCH_CX = 0.5;
	private static final double ARCH_CY = 0.5;
	private static final double ARM_X = 0.265;      // offset of each arm centre line from the middle
	private static final double LAND_WIDTH = 0.13;  // thickness of the horseshoe
	private static final double ARM_BOTTOM = 0.90;
	private static final double PILLAR_Y = 0.47;
	private static final double ZULRAH_RADIUS = 0.072;
	private static final double TILE_RADIUS = 0.045;

	private ShrineDiagram()
	{
	}

	/** Render a phase at the given pixel size. Null phase draws the empty shrine. */
	public static BufferedImage render(Phase phase, int size, DiagramOrientation orientation, boolean colourblind, boolean compass)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		try
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			draw(g, phase, size, orientation, colourblind, compass);
		}
		finally
		{
			g.dispose();
		}
		return image;
	}

	/** Draw into an existing graphics context, at origin, filling a size x size square. */
	public static void draw(Graphics2D g, Phase phase, int size, DiagramOrientation orientation, boolean colourblind, boolean compass)
	{
		AffineTransform saved = g.getTransform();
		g.transform(AffineTransform.getRotateInstance(orientation.getRadians(), size / 2.0, size / 2.0));

		drawLand(g, size);

		if (phase != null)
		{
			drawTile(g, phase.getTile(), size);
			drawZulrah(g, phase, size, colourblind);
		}

		g.setTransform(saved);

		if (compass)
		{
			drawCompass(g, size, orientation);
		}
	}

	private static void drawLand(Graphics2D g, int size)
	{
		double s = size;
		double left = (ARCH_CX - ARM_X) * s;
		double right = (ARCH_CX + ARM_X) * s;
		double cy = ARCH_CY * s;
		double bottom = ARM_BOTTOM * s;
		double r = ARM_X * s;

		Path2D.Double path = new Path2D.Double();
		path.moveTo(left, bottom);
		path.lineTo(left, cy);
		path.append(new Arc2D.Double(ARCH_CX * s - r, cy - r, 2 * r, 2 * r, 180, -180, Arc2D.OPEN), true);
		path.lineTo(right, bottom);

		float width = (float) (LAND_WIDTH * s);
		g.setStroke(new BasicStroke(width + Math.max(1f, size / 60f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(FormPalette.LAND_EDGE);
		g.draw(path);
		g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(FormPalette.LAND);
		g.draw(path);

		double pr = Math.max(1.5, 0.035 * s);
		g.setColor(FormPalette.PILLAR);
		g.fill(circle(left, PILLAR_Y * s, pr));
		g.fill(circle(right, PILLAR_Y * s, pr));
	}

	private static void drawZulrah(Graphics2D g, Phase phase, int size, boolean colourblind)
	{
		ZulrahPosition pos = phase.getPosition();
		double x = pos.getX() * size;
		double y = pos.getY() * size;
		double r = ZULRAH_RADIUS * size;
		Color colour = FormPalette.colour(phase.getForm(), colourblind);

		g.setColor(FormPalette.OUTLINE);
		g.fill(circle(x, y, r + Math.max(1, size / 80.0)));
		g.setColor(colour);
		g.fill(circle(x, y, r));

		if (phase.getForm().isJad())
		{
			// Jad: a white ring and letter so the green form reads as "alternating".
			g.setColor(Color.WHITE);
			g.setStroke(new BasicStroke(Math.max(1f, size / 70f)));
			g.draw(circle(x, y, r * 0.62));
			if (size >= 48)
			{
				drawCentredText(g, "J", x, y, (float) (r * 0.95), Color.WHITE, FormPalette.OUTLINE);
			}
		}
	}

	private static void drawTile(Graphics2D g, StandTile tile, int size)
	{
		double x = tile.getX() * size;
		double y = tile.getY() * size;
		double r = TILE_RADIUS * size;
		float stroke = Math.max(1.5f, size / 40f);

		g.setStroke(new BasicStroke(stroke + Math.max(1f, size / 60f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(FormPalette.OUTLINE);
		drawCross(g, x, y, r);
		g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(FormPalette.TILE);
		drawCross(g, x, y, r);
	}

	private static void drawCross(Graphics2D g, double x, double y, double r)
	{
		Path2D.Double p = new Path2D.Double();
		p.moveTo(x - r, y - r);
		p.lineTo(x + r, y + r);
		p.moveTo(x - r, y + r);
		p.lineTo(x + r, y - r);
		g.draw(p);
	}

	private static void drawCompass(Graphics2D g, int size, DiagramOrientation orientation)
	{
		float fontSize = Math.max(8f, size / 9f);
		double x = size * 0.09;
		double y = size * 0.11;
		drawCentredText(g, orientation.getTopLetter(), x, y, fontSize, new Color(200, 200, 200), FormPalette.OUTLINE);
	}

	private static void drawCentredText(Graphics2D g, String text, double x, double y, float fontSize, Color fill, Color shadow)
	{
		Font font = g.getFont().deriveFont(Font.BOLD, fontSize);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		float tx = (float) (x - fm.stringWidth(text) / 2.0);
		float ty = (float) (y + (fm.getAscent() - fm.getDescent()) / 2.0);
		g.setColor(shadow);
		g.drawString(text, tx + 1, ty + 1);
		g.setColor(fill);
		g.drawString(text, tx, ty);
	}

	private static Ellipse2D circle(double cx, double cy, double r)
	{
		return new Ellipse2D.Double(cx - r, cy - r, 2 * r, 2 * r);
	}
}
