package com.zulrahrotations.ui;

import com.zulrahrotations.ZulrahRotationsConfig;
import com.zulrahrotations.data.Phase;
import com.zulrahrotations.data.Rotation;
import com.zulrahrotations.state.KillStats;
import com.zulrahrotations.state.RotationChoice;
import com.zulrahrotations.state.RotationTracker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

/**
 * Side panel: status, the current phase with a large shrine diagram, the next phase, the
 * rotation picker when the rotations split, previous/next buttons, kill stats and a
 * clickable timeline of the whole rotation.
 *
 * Every button here calls the tracker directly; the plugin listens to the tracker for
 * persistence and overlay updates. Nothing in this panel touches the game client.
 */
public class ZulrahRotationsPanel extends PluginPanel
{
	private static final int DIAGRAM_SIZE = 150;
	private static final int NEXT_DIAGRAM_SIZE = 64;
	private static final int TIMELINE_SWATCH = 12;
	private static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
	private static final Color CARD_HOVER = ColorScheme.DARKER_GRAY_HOVER_COLOR;
	private static final Color MUTED = new Color(170, 170, 170);

	private final RotationTracker tracker;
	private final KillStats killStats;
	private final ZulrahRotationsConfig config;

	private final JLabel statusLabel = new JLabel();
	private final JLabel phaseCountLabel = new JLabel();
	private final JPanel body = new JPanel();

	public ZulrahRotationsPanel(RotationTracker tracker, KillStats killStats, ZulrahRotationsConfig config)
	{
		super(false);
		this.tracker = tracker;
		this.killStats = killStats;
		this.config = config;

		setLayout(new BorderLayout(0, 8));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(buildHeader(), BorderLayout.NORTH);

		// One column, each row at its own preferred height and the full panel width.
		body.setLayout(new DynamicGridLayout(0, 1, 0, 8));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(body, BorderLayout.CENTER);

		rebuild();
	}

	private JPanel buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Zulrah Rotations");
		title.setFont(FontManager.getRunescapeBoldFont());
		title.setForeground(Color.WHITE);

		JButton reset = new JButton("Reset");
		reset.setToolTipText("Back to the opening phase");
		reset.setFocusable(false);
		reset.addActionListener(e -> tracker.reset());

		JPanel titles = new JPanel();
		titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
		titles.setBackground(ColorScheme.DARK_GRAY_COLOR);
		statusLabel.setFont(FontManager.getRunescapeSmallFont());
		phaseCountLabel.setFont(FontManager.getRunescapeSmallFont());
		phaseCountLabel.setForeground(MUTED);
		titles.add(title);
		titles.add(statusLabel);
		titles.add(phaseCountLabel);

		header.add(titles, BorderLayout.CENTER);
		header.add(reset, BorderLayout.EAST);
		return header;
	}

	/** Rebuild everything from the tracker. Call on the Swing thread. */
	public void rebuild()
	{
		boolean picking = tracker.isAtSplit();
		statusLabel.setText(tracker.getStatusText());
		statusLabel.setForeground(picking ? ColorScheme.BRAND_ORANGE : tracker.isRotationKnown() ? ColorScheme.PROGRESS_COMPLETE_COLOR : MUTED);
		phaseCountLabel.setText(tracker.isRotationKnown()
			? "Phase " + tracker.getPhaseNumber() + " of " + tracker.getPhaseCount()
			: "Phase " + tracker.getPhaseNumber());

		SwingUtil.fastRemoveAll(body);

		body.add(buildCurrentCard());
		body.add(buildNavRow());
		body.add(buildNextCard());
		if (config.showKillStats())
		{
			body.add(buildKillStats());
		}
		body.add(sectionLabel(tracker.isRotationKnown() ? tracker.getRotation().getDisplayName() + " timeline" : "Timeline"));
		body.add(buildTimeline());

		body.revalidate();
		body.repaint();
	}

	/** Refresh only the kill stat text, cheap enough to call every second. */
	public void refreshKillStats()
	{
		if (killStatsLabel != null)
		{
			killStatsLabel.setText(killStatsText());
		}
	}

	// ---- current phase -------------------------------------------------------------------

	private JPanel buildCurrentCard()
	{
		Phase phase = tracker.getCurrentPhase();
		JPanel card = card();
		card.setLayout(new BorderLayout(8, 0));

		JLabel diagram = new JLabel(new ImageIcon(ShrineDiagram.render(phase, DIAGRAM_SIZE,
			config.diagramOrientation(), config.colourblindPalette(), true)));
		diagram.setHorizontalAlignment(SwingConstants.CENTER);
		card.add(diagram, BorderLayout.NORTH);

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		text.setBorder(new EmptyBorder(6, 2, 2, 2));

		JLabel form = new JLabel("P" + tracker.getPhaseNumber() + "  " + phase.shortLabel());
		form.setFont(FontManager.getRunescapeBoldFont());
		form.setForeground(FormPalette.colour(phase.getForm(), config.colourblindPalette()));
		form.setAlignmentX(LEFT_ALIGNMENT);
		text.add(form);

		text.add(leftAligned(detail("Stand", phase.getTile().getDisplayName())));
		text.add(leftAligned(detail("Pray", phase.prayerText())));
		text.add(leftAligned(detail("Attack with", phase.getForm().getRecommendedStyle())));
		text.add(leftAligned(detail("Expect", phase.eventsText())));
		if (phase.getNote() != null)
		{
			JLabel note = new JLabel(phase.getNote());
			note.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.ITALIC));
			note.setForeground(MUTED);
			note.setBorder(new EmptyBorder(3, 0, 0, 0));
			note.setAlignmentX(LEFT_ALIGNMENT);
			text.add(note);
		}

		card.add(text, BorderLayout.CENTER);
		return card;
	}

	private static JPanel leftAligned(JPanel panel)
	{
		panel.setAlignmentX(LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
		return panel;
	}

	private JPanel detail(String key, String value)
	{
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		JLabel k = new JLabel(key);
		k.setFont(FontManager.getRunescapeSmallFont());
		k.setForeground(MUTED);
		k.setPreferredSize(new Dimension(64, 16));
		JLabel v = new JLabel(value);
		v.setFont(FontManager.getRunescapeSmallFont());
		v.setForeground(Color.WHITE);
		row.add(k, BorderLayout.WEST);
		row.add(v, BorderLayout.CENTER);
		return row;
	}

	// ---- navigation and picker -----------------------------------------------------------

	private JPanel buildNavRow()
	{
		JPanel wrap = new JPanel();
		wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
		wrap.setOpaque(false);

		if (tracker.isAtSplit())
		{
			JLabel hint = new JLabel("Which way did Zulrah go?");
			hint.setFont(FontManager.getRunescapeSmallFont());
			hint.setForeground(ColorScheme.BRAND_ORANGE);
			hint.setAlignmentX(LEFT_ALIGNMENT);
			wrap.add(hint);
			wrap.add(Box.createVerticalStrut(4));

			List<RotationChoice> choices = tracker.getChoices();
			JPanel buttons = new JPanel(new GridLayout(1, choices.size(), 4, 0));
			buttons.setOpaque(false);
			for (RotationChoice choice : choices)
			{
				buttons.add(choiceButton(choice));
			}
			buttons.setAlignmentX(LEFT_ALIGNMENT);
			wrap.add(buttons);
			wrap.add(Box.createVerticalStrut(6));
		}

		JPanel nav = new JPanel(new GridLayout(1, 2, 4, 0));
		nav.setOpaque(false);
		JButton prev = new JButton("◀ Previous");
		prev.setFocusable(false);
		prev.setEnabled(tracker.getPhaseIndex() > 0);
		prev.addActionListener(e -> tracker.previous());
		JButton next = new JButton(tracker.isAtEnd() ? "Restart ↻" : "Next ▶");
		next.setFocusable(false);
		next.setEnabled(!tracker.isAtSplit());
		next.setToolTipText(tracker.isAtSplit() ? "Pick the rotation first" : null);
		next.addActionListener(e -> tracker.next());
		nav.add(prev);
		nav.add(next);
		nav.setAlignmentX(LEFT_ALIGNMENT);
		wrap.add(nav);
		return wrap;
	}

	private JButton choiceButton(RotationChoice choice)
	{
		Phase next = choice.getNextPhase();
		JButton button = new JButton();
		button.setLayout(new BorderLayout());
		button.setFocusable(false);
		button.setMargin(new Insets(4, 4, 4, 4));
		button.setToolTipText("Next: " + next.shortLabel() + " · stand " + next.getTile().getDisplayName());

		JLabel icon = new JLabel(new ImageIcon(ShrineDiagram.render(next, 48, config.diagramOrientation(), config.colourblindPalette(), false)));
		icon.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel label = new JLabel(choice.getLabel(), SwingConstants.CENTER);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setForeground(FormPalette.colour(next.getForm(), config.colourblindPalette()));
		button.add(icon, BorderLayout.CENTER);
		button.add(label, BorderLayout.SOUTH);
		button.addActionListener(e -> tracker.pick(choice));
		return button;
	}

	// ---- next phase ----------------------------------------------------------------------

	private JPanel buildNextCard()
	{
		JPanel card = card();
		card.setLayout(new BorderLayout(8, 0));

		Phase next = tracker.getNextPhase();
		JLabel title = new JLabel();
		title.setFont(FontManager.getRunescapeSmallFont());
		title.setForeground(MUTED);

		if (next == null)
		{
			title.setText("Next: pick the rotation above");
			card.add(title, BorderLayout.CENTER);
			return card;
		}

		JLabel icon = new JLabel(new ImageIcon(ShrineDiagram.render(next, NEXT_DIAGRAM_SIZE,
			config.diagramOrientation(), config.colourblindPalette(), false)));
		card.add(icon, BorderLayout.WEST);

		JPanel text = new JPanel();
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		text.setOpaque(false);
		title.setText(tracker.isAtEnd() ? "Next: rotation restarts" : "Next: phase " + (tracker.getPhaseNumber() + 1));
		JLabel form = new JLabel(next.shortLabel());
		form.setFont(FontManager.getRunescapeBoldFont());
		form.setForeground(FormPalette.colour(next.getForm(), config.colourblindPalette()));
		JLabel stand = new JLabel("Stand: " + next.getTile().getDisplayName());
		stand.setFont(FontManager.getRunescapeSmallFont());
		stand.setForeground(Color.WHITE);
		JLabel pray = new JLabel("Pray: " + next.getPrayer().getShortName());
		pray.setFont(FontManager.getRunescapeSmallFont());
		pray.setForeground(Color.WHITE);
		text.add(title);
		text.add(form);
		text.add(stand);
		text.add(pray);
		card.add(text, BorderLayout.CENTER);
		return card;
	}

	// ---- kill stats ----------------------------------------------------------------------

	private JLabel killStatsLabel;

	private JPanel buildKillStats()
	{
		JPanel card = card();
		card.setLayout(new BorderLayout());
		killStatsLabel = new JLabel(killStatsText());
		killStatsLabel.setFont(FontManager.getRunescapeSmallFont());
		killStatsLabel.setForeground(MUTED);
		card.add(killStatsLabel, BorderLayout.CENTER);
		return card;
	}

	private String killStatsText()
	{
		StringBuilder sb = new StringBuilder("<html>Kills this session: ").append(killStats.getSessionKills());
		if (killStats.getReportedKillCount() >= 0)
		{
			sb.append(" (KC ").append(killStats.getReportedKillCount()).append(')');
		}
		if (killStats.getLastFightDuration() != null)
		{
			sb.append("<br>Last fight: ").append(killStats.getLastFightDuration());
		}
		String timer = killStats.elapsedText();
		if (timer != null)
		{
			sb.append("<br>Timer: ").append(timer).append(killStats.isTimerRunning() ? "" : " (done)");
		}
		return sb.append("</html>").toString();
	}

	// ---- timeline ------------------------------------------------------------------------

	private JPanel buildTimeline()
	{
		JPanel list = new JPanel(new GridBagLayout());
		list.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(0, 0, 2, 0);

		List<Phase> phases = tracker.getTimeline();
		for (int i = 0; i < phases.size(); i++)
		{
			list.add(timelineRow(i, phases.get(i)), c);
			c.gridy++;
		}

		if (!tracker.isRotationKnown())
		{
			JLabel more = new JLabel(tracker.getCandidates().size() == Rotation.values().length
				? "… then pick Crimson, Serp or Tanz"
				: "… then pick " + choiceSummary());
			more.setFont(FontManager.getRunescapeSmallFont());
			more.setForeground(ColorScheme.BRAND_ORANGE);
			more.setBorder(new EmptyBorder(4, 6, 0, 0));
			list.add(more, c);
		}
		else
		{
			JLabel end = new JLabel("↻ back to the opening phase");
			end.setFont(FontManager.getRunescapeSmallFont());
			end.setForeground(MUTED);
			end.setBorder(new EmptyBorder(4, 6, 0, 0));
			list.add(end, c);
		}
		return list;
	}

	private String choiceSummary()
	{
		StringBuilder sb = new StringBuilder();
		for (Rotation r : tracker.getCandidates())
		{
			if (sb.length() > 0)
			{
				sb.append(" or ");
			}
			sb.append(r.getDisplayName());
		}
		return sb.toString();
	}

	private JPanel timelineRow(int index, Phase phase)
	{
		boolean current = index == tracker.getPhaseIndex();
		int number = index + 1;

		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setBackground(current ? ColorScheme.MEDIUM_GRAY_COLOR : CARD);
		row.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 3, 0, 0, current ? ColorScheme.BRAND_ORANGE : FormPalette.colour(phase.getForm(), config.colourblindPalette())),
			new EmptyBorder(3, 6, 3, 6)));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText("Jump to phase " + number);

		JLabel num = new JLabel(String.valueOf(number));
		num.setFont(FontManager.getRunescapeSmallFont());
		num.setForeground(current ? Color.WHITE : MUTED);
		num.setPreferredSize(new Dimension(18, 16));

		JPanel swatch = new JPanel();
		swatch.setBackground(FormPalette.colour(phase.getForm(), config.colourblindPalette()));
		swatch.setPreferredSize(new Dimension(TIMELINE_SWATCH, TIMELINE_SWATCH));
		swatch.setMaximumSize(new Dimension(TIMELINE_SWATCH, TIMELINE_SWATCH));

		JLabel text = new JLabel(phase.shortLabel() + (phase.getForm().isJad() ? "" : "") );
		text.setFont(current ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
		text.setForeground(Color.WHITE);

		JLabel tile = new JLabel(phase.getTile().getDisplayName());
		tile.setFont(FontManager.getRunescapeSmallFont());
		tile.setForeground(MUTED);

		JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		left.setOpaque(false);
		left.add(num);
		left.add(swatch);

		JPanel centre = new JPanel();
		centre.setLayout(new BoxLayout(centre, BoxLayout.Y_AXIS));
		centre.setOpaque(false);
		centre.add(text);
		centre.add(tile);

		row.add(left, BorderLayout.WEST);
		row.add(centre, BorderLayout.CENTER);

		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				tracker.jumpTo(number);
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (!current)
				{
					row.setBackground(CARD_HOVER);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(current ? ColorScheme.MEDIUM_GRAY_COLOR : CARD);
			}
		});
		return row;
	}

	// ---- helpers -------------------------------------------------------------------------

	private static JPanel card()
	{
		JPanel card = new JPanel();
		card.setBackground(CARD);
		card.setBorder(new EmptyBorder(8, 8, 8, 8));
		card.setAlignmentX(LEFT_ALIGNMENT);
		return card;
	}

	private static JLabel sectionLabel(String text)
	{
		JLabel label = new JLabel(text.toUpperCase());
		label.setFont(FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD));
		label.setForeground(MUTED);
		label.setAlignmentX(LEFT_ALIGNMENT);
		return label;
	}
}
