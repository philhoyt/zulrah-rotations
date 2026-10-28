package com.zulrahrotations.overlay;

import com.zulrahrotations.ZulrahRotationsConfig;
import com.zulrahrotations.ZulrahRotationsPlugin;
import com.zulrahrotations.data.Phase;
import com.zulrahrotations.state.KillStats;
import com.zulrahrotations.state.RotationTracker;
import com.zulrahrotations.ui.DiagramOrientation;
import com.zulrahrotations.ui.FormPalette;
import com.zulrahrotations.ui.ShrineDiagram;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Compact in-game panel showing what the tracker believes: rotation, current phase, where
 * to stand, what to pray, and the next phase. Movable and resizable like other overlays.
 * It only reflects the manually selected phase; nothing here reads Zulrah.
 */
public class ZulrahRotationsOverlay extends OverlayPanel
{
	private static final int DEFAULT_WIDTH = 176;
	private static final int MIN_WIDTH = 120;
	private static final Color MUTED = new Color(190, 190, 190);

	private final ZulrahRotationsPlugin plugin;
	private final ZulrahRotationsConfig config;

	// Small render cache so the diagram is not redrawn every frame.
	private BufferedImage cachedImage;
	private Phase cachedPhase;
	private int cachedSize;
	private DiagramOrientation cachedOrientation;
	private boolean cachedColourblind;

	@Inject
	ZulrahRotationsOverlay(ZulrahRotationsPlugin plugin, ZulrahRotationsConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setResizable(true);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.displayMode().overlay() || !plugin.isInZulrahArea())
		{
			return null;
		}

		RotationTracker tracker = plugin.getTracker();
		Phase phase = tracker.getCurrentPhase();
		boolean cb = config.colourblindPalette();

		Dimension pref = getPreferredSize();
		int width = pref != null && pref.width >= MIN_WIDTH ? pref.width : DEFAULT_WIDTH;
		panelComponent.setPreferredSize(new Dimension(width, 0));

		panelComponent.getChildren().add(TitleComponent.builder()
			.text(tracker.getStatusText())
			.color(tracker.isAtSplit() ? ColorScheme.BRAND_ORANGE : tracker.isRotationKnown() ? ColorScheme.PROGRESS_COMPLETE_COLOR : MUTED)
			.build());

		if (config.overlayDiagram())
		{
			int size = Math.max(48, Math.min(96, width - 40));
			panelComponent.getChildren().add(new ImageComponent(diagram(phase, size, config.diagramOrientation(), cb)));
		}

		panelComponent.getChildren().add(LineComponent.builder()
			.left("P" + tracker.getPhaseNumber())
			.leftColor(MUTED)
			.right(phase.shortLabel())
			.rightColor(FormPalette.colour(phase.getForm(), cb))
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Stand")
			.leftColor(MUTED)
			.right(phase.getTile().getDisplayName())
			.rightColor(FormPalette.TILE)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Pray")
			.leftColor(MUTED)
			.right(phase.getForm().isJad() ? phase.getPrayer().getShortName() + " first, swap" : phase.getPrayer().getShortName())
			.rightColor(Color.WHITE)
			.build());

		if (config.overlayNextPhase())
		{
			Phase next = tracker.getNextPhase();
			if (next == null)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Next")
					.leftColor(MUTED)
					.right("Pick rotation")
					.rightColor(ColorScheme.BRAND_ORANGE)
					.build());
			}
			else
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left(tracker.isAtEnd() ? "Restart" : "Next")
					.leftColor(MUTED)
					.right(next.shortLabel() + " @ " + next.getTile().getDisplayName())
					.rightColor(FormPalette.colour(next.getForm(), cb))
					.build());
			}
		}

		if (config.showKillStats())
		{
			KillStats stats = plugin.getKillStats();
			String timer = stats.elapsedText();
			if (timer != null)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Timer")
					.leftColor(MUTED)
					.right(timer)
					.rightColor(stats.isTimerRunning() ? Color.WHITE : MUTED)
					.build());
			}
			if (stats.getSessionKills() > 0)
			{
				panelComponent.getChildren().add(LineComponent.builder()
					.left("Kills")
					.leftColor(MUTED)
					.right(stats.getSessionKills() + (stats.getLastFightDuration() != null ? " (last " + stats.getLastFightDuration() + ")" : ""))
					.rightColor(Color.WHITE)
					.build());
			}
		}

		return super.render(graphics);
	}

	private BufferedImage diagram(Phase phase, int size, DiagramOrientation orientation, boolean colourblind)
	{
		if (cachedImage == null || cachedPhase != phase || cachedSize != size
			|| cachedOrientation != orientation || cachedColourblind != colourblind)
		{
			cachedImage = ShrineDiagram.render(phase, size, orientation, colourblind, false);
			cachedPhase = phase;
			cachedSize = size;
			cachedOrientation = orientation;
			cachedColourblind = colourblind;
		}
		return cachedImage;
	}
}
