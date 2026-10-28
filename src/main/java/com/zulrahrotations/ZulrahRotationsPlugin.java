package com.zulrahrotations;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Provides;
import com.zulrahrotations.data.Rotation;
import com.zulrahrotations.overlay.ZulrahRotationsOverlay;
import com.zulrahrotations.state.KillStats;
import com.zulrahrotations.state.RotationTracker;
import com.zulrahrotations.state.TrackerSnapshot;
import com.zulrahrotations.ui.DiagramOrientation;
import com.zulrahrotations.ui.ShrineDiagram;
import com.zulrahrotations.ui.ZulrahRotationsPanel;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldView;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.Text;

/**
 * Manual Zulrah rotation guide.
 *
 * Rules note for reviewers: the only game state this plugin reads is
 * (1) the map region and instance flag, used to show or hide UI, enable hotkeys and reset
 *     when the player leaves the shrine, and
 * (2) chat messages, used for the kill counter, the fight duration and the death reset.
 * Zulrah itself (NPC id, form, animation, position, spawns, projectiles) is never read.
 * Every phase change is made by the player through a button, a hotkey or a chat command.
 */
@Slf4j
@PluginDescriptor(
	name = "Zulrah Rotations",
	description = "Manual Zulrah rotation guide: step through phases with hotkeys, a side panel and an overlay",
	tags = {"zulrah", "pvm", "boss", "rotation", "guide", "snake"}
)
public class ZulrahRotationsPlugin extends Plugin
{
	static final int REGION_ZUL_ANDRA = 8751;
	static final int REGION_SHRINE_SOUTH = 9007;
	static final int REGION_SHRINE_NORTH = 9008;
	static final String STATE_KEY = "state";
	static final String COMMAND_ROTATION = "zulrot";
	static final String COMMAND_PHASE = "zulphase";
	static final String DEATH_MESSAGE = "Oh dear, you are dead!";

	@Inject
	private Client client;
	@Inject
	private ClientToolbar clientToolbar;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private KeyManager keyManager;
	@Inject
	private ConfigManager configManager;
	@Inject
	private Gson gson;
	@Inject
	private ChatMessageManager chatMessageManager;
	@Inject
	private ZulrahRotationsConfig config;
	@Inject
	private ZulrahRotationsOverlay overlay;

	@Getter
	private final RotationTracker tracker = new RotationTracker();
	@Getter
	private final KillStats killStats = new KillStats();

	private ZulrahRotationsPanel panel;
	private NavigationButton navButton;
	private final List<HotkeyListener> hotkeys = new ArrayList<>();
	private final RotationTracker.Listener trackerListener = t -> onTrackerChanged();

	/** In Zul-Andra or inside the shrine instance. */
	@Getter
	private boolean inZulrahArea;
	/** Inside the shrine instance itself. */
	private boolean inInstance;
	private boolean navVisible;
	private boolean assistanceGiven;
	private boolean pendingRestore;

	@Provides
	ZulrahRotationsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ZulrahRotationsConfig.class);
	}

	@Override
	protected void startUp()
	{
		panel = new ZulrahRotationsPanel(tracker, killStats, config);
		navButton = NavigationButton.builder()
			.tooltip("Zulrah Rotations")
			.icon(ShrineDiagram.render(Rotation.opening(), 16, DiagramOrientation.SOUTH, false, false))
			.priority(6)
			.panel(panel)
			.build();

		tracker.addListener(trackerListener);
		overlayManager.add(overlay);
		registerHotkeys();

		inZulrahArea = false;
		inInstance = false;
		navVisible = false;
		assistanceGiven = false;
		pendingRestore = true;
		updateNavButton();
	}

	@Override
	protected void shutDown()
	{
		tracker.removeListener(trackerListener);
		overlayManager.remove(overlay);
		hotkeys.forEach(keyManager::unregisterKeyListener);
		hotkeys.clear();
		if (navVisible)
		{
			clientToolbar.removeNavigation(navButton);
			navVisible = false;
		}
		panel = null;
		navButton = null;
	}

	// ---- tracker -> persistence and UI ---------------------------------------------------

	private void onTrackerChanged()
	{
		persistState();
		rebuildPanel();
	}

	private void rebuildPanel()
	{
		ZulrahRotationsPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(p::rebuild);
		}
	}

	@Schedule(period = 1, unit = ChronoUnit.SECONDS)
	public void tickKillTimer()
	{
		ZulrahRotationsPanel p = panel;
		if (p != null && killStats.isTimerRunning() && config.showKillStats())
		{
			SwingUtilities.invokeLater(p::refreshKillStats);
		}
	}

	// ---- game events: region and login only ----------------------------------------------

	@Subscribe
	public void onGameTick(GameTick event)
	{
		WorldView view = client.getTopLevelWorldView();
		if (view == null)
		{
			return;
		}
		int[] regions = view.getMapRegions();
		boolean instanced = view.isInstance();
		boolean shrine = false;
		boolean zulAndra = false;
		if (regions != null)
		{
			for (int region : regions)
			{
				shrine |= region == REGION_SHRINE_SOUTH || region == REGION_SHRINE_NORTH;
				zulAndra |= region == REGION_ZUL_ANDRA;
			}
		}
		boolean nowInstance = shrine && instanced;
		boolean nowArea = nowInstance || zulAndra;

		if (nowInstance && !inInstance)
		{
			onEnterInstance();
		}
		else if (!nowInstance && inInstance)
		{
			onLeaveInstance();
		}
		inInstance = nowInstance;

		if (nowArea != inZulrahArea)
		{
			inZulrahArea = nowArea;
			updateNavButton();
			if (nowArea && config.openPanelOnArrival())
			{
				openPanel();
			}
		}

		if (pendingRestore)
		{
			pendingRestore = false;
			restoreState();
		}
	}

	private void onEnterInstance()
	{
		killStats.startTimer();
		if (config.assistanceMessages() && !tracker.isRotationKnown() && !assistanceGiven)
		{
			assistanceGiven = true;
			chat("Zulrah Rotations: pick the rotation in the side panel, press a rotation hotkey, "
				+ "or type ::zulrot 1-4 (1 Crimson A, 2 Crimson B, 3 Serp, 4 Tanz). ::zulphase N jumps to a phase.");
		}
	}

	private void onLeaveInstance()
	{
		assistanceGiven = false;
		if (killStats.isTimerRunning())
		{
			killStats.clearTimer();
		}
		if (config.resetOnLeave())
		{
			tracker.reset();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				inInstance = false;
				if (inZulrahArea)
				{
					inZulrahArea = false;
					updateNavButton();
				}
				pendingRestore = true;
				break;
			case LOGGED_IN:
				pendingRestore = true;
				break;
			default:
				break;
		}
	}

	// ---- chat: kill stats, death reset, commands -----------------------------------------

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		String message = Text.removeTags(event.getMessage());
		if (killStats.onChatMessage(message))
		{
			rebuildPanel();
			return;
		}
		if (config.resetOnDeath() && message.startsWith(DEATH_MESSAGE))
		{
			tracker.reset();
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		String command = event.getCommand();
		String[] args = event.getArguments();
		if (command.equalsIgnoreCase(COMMAND_ROTATION))
		{
			if (args.length < 1)
			{
				chat("Usage: ::zulrot 1-4 (1 Crimson A, 2 Crimson B, 3 Serp, 4 Tanz). You are on: " + tracker.getStatusText() + ".");
				return;
			}
			Rotation rotation = parseRotation(args[0]);
			if (rotation == null)
			{
				chat("Unknown rotation '" + args[0] + "'. Use 1-4, or a, b, serp, tanz.");
				return;
			}
			tracker.setRotation(rotation);
			chat("Rotation set to " + rotation.getNumber() + " (" + rotation.getDisplayName() + "), phase " + tracker.getPhaseNumber() + ".");
		}
		else if (command.equalsIgnoreCase(COMMAND_PHASE))
		{
			if (args.length < 1)
			{
				chat("Usage: ::zulphase N. You are on phase " + tracker.getPhaseNumber() + ".");
				return;
			}
			int number;
			try
			{
				number = Integer.parseInt(args[0].trim());
			}
			catch (NumberFormatException ex)
			{
				chat("::zulphase needs a phase number.");
				return;
			}
			if (!tracker.jumpTo(number))
			{
				chat(tracker.isRotationKnown()
					? "Phase must be between 1 and " + tracker.getPhaseCount() + " for " + tracker.getRotation().getDisplayName() + "."
					: "Set the rotation first (::zulrot 1-4); only phases 1-" + Math.max(1, tracker.getPhaseCount()) + " are shared.");
				return;
			}
			chat("Jumped to phase " + tracker.getPhaseNumber() + ": " + tracker.getCurrentPhase().shortLabel() + ".");
		}
	}

	static Rotation parseRotation(String arg)
	{
		String a = arg.trim().toLowerCase();
		switch (a)
		{
			case "a":
			case "crimsona":
			case "crimson-a":
			case "magma":
				return Rotation.CRIMSON_A;
			case "b":
			case "crimsonb":
			case "crimson-b":
				return Rotation.CRIMSON_B;
			case "serp":
			case "serpentine":
				return Rotation.SERP;
			case "tanz":
			case "tanzanite":
				return Rotation.TANZ;
			default:
				try
				{
					return Rotation.byNumber(Integer.parseInt(a));
				}
				catch (NumberFormatException ex)
				{
					return null;
				}
		}
	}

	private void chat(String message)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(message)
			.build());
	}

	// ---- config --------------------------------------------------------------------------

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!ZulrahRotationsConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		updateNavButton();
		rebuildPanel();
	}

	private void updateNavButton()
	{
		boolean show = config.displayMode().panel() && (!config.showPanelOnlyAtZulrah() || inZulrahArea);
		if (show == navVisible || navButton == null)
		{
			return;
		}
		navVisible = show;
		NavigationButton button = navButton;
		SwingUtilities.invokeLater(() ->
		{
			if (show)
			{
				clientToolbar.addNavigation(button);
			}
			else
			{
				clientToolbar.removeNavigation(button);
			}
		});
	}

	/** Brings the side panel to the front. Queued after any pending sidebar icon change. */
	private void openPanel()
	{
		NavigationButton button = navButton;
		if (button == null || !navVisible)
		{
			return;
		}
		SwingUtilities.invokeLater(() -> clientToolbar.openPanel(button));
	}

	// ---- hotkeys -------------------------------------------------------------------------

	private void registerHotkeys()
	{
		hotkey(config::nextPhaseHotkey, tracker::next);
		hotkey(config::previousPhaseHotkey, tracker::previous);
		hotkey(config::resetHotkey, tracker::reset);
		hotkey(config::rotation1Hotkey, () -> tracker.setRotation(Rotation.CRIMSON_A));
		hotkey(config::rotation2Hotkey, () -> tracker.setRotation(Rotation.CRIMSON_B));
		hotkey(config::rotation3Hotkey, () -> tracker.setRotation(Rotation.SERP));
		hotkey(config::rotation4Hotkey, () -> tracker.setRotation(Rotation.TANZ));
		hotkeys.forEach(keyManager::registerKeyListener);
	}

	private void hotkey(Supplier<Keybind> keybind, Runnable action)
	{
		hotkeys.add(new HotkeyListener(keybind)
		{
			@Override
			public void hotkeyPressed()
			{
				if (config.hotkeysEverywhere() || inZulrahArea)
				{
					action.run();
				}
			}
		});
	}

	// ---- per-account persistence ---------------------------------------------------------

	private boolean isFreshState()
	{
		return tracker.getPhaseIndex() == 0 && tracker.getCandidates().size() == Rotation.values().length;
	}

	private void persistState()
	{
		if (!config.rememberState() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (isFreshState())
		{
			configManager.unsetRSProfileConfiguration(ZulrahRotationsConfig.GROUP, STATE_KEY);
			return;
		}
		configManager.setRSProfileConfiguration(ZulrahRotationsConfig.GROUP, STATE_KEY, gson.toJson(tracker.snapshot()));
	}

	private void restoreState()
	{
		if (!config.rememberState() || !isFreshState())
		{
			return;
		}
		String json = configManager.getRSProfileConfiguration(ZulrahRotationsConfig.GROUP, STATE_KEY);
		if (json == null || json.isEmpty())
		{
			return;
		}
		try
		{
			TrackerSnapshot snapshot = gson.fromJson(json, TrackerSnapshot.class);
			if (!tracker.restore(snapshot))
			{
				configManager.unsetRSProfileConfiguration(ZulrahRotationsConfig.GROUP, STATE_KEY);
			}
		}
		catch (JsonSyntaxException ex)
		{
			log.debug("Discarding unreadable saved state", ex);
			configManager.unsetRSProfileConfiguration(ZulrahRotationsConfig.GROUP, STATE_KEY);
		}
	}
}
