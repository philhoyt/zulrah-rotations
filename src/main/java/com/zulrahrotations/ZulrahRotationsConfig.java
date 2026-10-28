package com.zulrahrotations;

import com.zulrahrotations.ui.DiagramOrientation;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

@ConfigGroup(ZulrahRotationsConfig.GROUP)
public interface ZulrahRotationsConfig extends Config
{
	String GROUP = "zulrahrotations";

	String KEY_DISPLAY_MODE = "displayMode";
	String KEY_SHOW_PANEL_ONLY_AT_ZULRAH = "showPanelOnlyAtZulrah";
	String KEY_HOTKEYS_EVERYWHERE = "hotkeysEverywhere";

	enum DisplayMode
	{
		PANEL("Side panel"),
		OVERLAY("Overlay"),
		BOTH("Both");

		private final String name;

		DisplayMode(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}

		public boolean panel()
		{
			return this != OVERLAY;
		}

		public boolean overlay()
		{
			return this != PANEL;
		}
	}

	// ---- sections ------------------------------------------------------------------------

	@ConfigSection(name = "Display", description = "What to show and how it looks", position = 0)
	String SECTION_DISPLAY = "display";

	@ConfigSection(name = "Behaviour", description = "Resetting, remembering state and chat help", position = 1)
	String SECTION_BEHAVIOUR = "behaviour";

	@ConfigSection(name = "Hotkeys", description = "Keys that step through the rotation while the game has focus", position = 2)
	String SECTION_HOTKEYS = "hotkeys";

	@ConfigSection(name = "Kill stats", description = "Kill counter and timer read from chat messages", position = 3)
	String SECTION_KILLS = "kills";

	@ConfigSection(name = "Learner", description = "Post-kill summaries of damage taken and supplies used, by phase", position = 4)
	String SECTION_LEARNER = "learner";

	// ---- display -------------------------------------------------------------------------

	@ConfigItem(
		keyName = KEY_DISPLAY_MODE,
		name = "Show",
		description = "Show the side panel, the in-game overlay, or both",
		section = SECTION_DISPLAY,
		position = 0
	)
	default DisplayMode displayMode()
	{
		return DisplayMode.BOTH;
	}

	@ConfigItem(
		keyName = "overlayDiagram",
		name = "Overlay diagram",
		description = "Draw the small shrine diagram inside the overlay",
		section = SECTION_DISPLAY,
		position = 1
	)
	default boolean overlayDiagram()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overlayNextPhase",
		name = "Overlay next phase",
		description = "Show the upcoming phase in the overlay as well as the current one",
		section = SECTION_DISPLAY,
		position = 2
	)
	default boolean overlayNextPhase()
	{
		return true;
	}

	@ConfigItem(
		keyName = "diagramOrientation",
		name = "Diagram orientation",
		description = "Which compass direction is at the top of the shrine diagram. The wiki chart faces south.",
		section = SECTION_DISPLAY,
		position = 3
	)
	default DiagramOrientation diagramOrientation()
	{
		return DiagramOrientation.SOUTH;
	}

	@ConfigItem(
		keyName = "colourblindPalette",
		name = "Colourblind palette",
		description = "Use a colourblind-friendly set of form colours (teal, orange, blue) instead of green, red, blue",
		section = SECTION_DISPLAY,
		position = 4
	)
	default boolean colourblindPalette()
	{
		return false;
	}

	@ConfigItem(
		keyName = KEY_SHOW_PANEL_ONLY_AT_ZULRAH,
		name = "Sidebar icon only at Zulrah",
		description = "Hide the sidebar icon unless you are in Zul-Andra or the shrine",
		section = SECTION_DISPLAY,
		position = 5
	)
	default boolean showPanelOnlyAtZulrah()
	{
		return true;
	}

	@ConfigItem(
		keyName = "openPanelOnArrival",
		name = "Open panel on arrival",
		description = "Open the side panel when you reach Zul-Andra or the shrine",
		section = SECTION_DISPLAY,
		position = 6
	)
	default boolean openPanelOnArrival()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPrayerDetails",
		name = "Show prayer and attack details",
		description = "Print the recommended prayer and the expected attacks for the selected phase. Off, the plugin is a position guide only",
		section = SECTION_DISPLAY,
		position = 7
	)
	default boolean showPrayerDetails()
	{
		return true;
	}

	// ---- behaviour -----------------------------------------------------------------------

	@ConfigItem(
		keyName = "resetOnLeave",
		name = "Reset when leaving",
		description = "Go back to the start when you leave the shrine instance",
		section = SECTION_BEHAVIOUR,
		position = 0
	)
	default boolean resetOnLeave()
	{
		return true;
	}

	@ConfigItem(
		keyName = "resetOnDeath",
		name = "Reset on death",
		description = "Go back to the start when the game says you have died",
		section = SECTION_BEHAVIOUR,
		position = 1
	)
	default boolean resetOnDeath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "rememberState",
		name = "Remember progress per account",
		description = "Save the rotation and phase so a client restart or relog does not lose them",
		section = SECTION_BEHAVIOUR,
		position = 2
	)
	default boolean rememberState()
	{
		return true;
	}

	@ConfigItem(
		keyName = "assistanceMessages",
		name = "Chat help when unsure",
		description = "Post a chat message explaining how to set the rotation when you enter the shrine with no rotation chosen",
		section = SECTION_BEHAVIOUR,
		position = 3
	)
	default boolean assistanceMessages()
	{
		return true;
	}

	@ConfigItem(
		keyName = KEY_HOTKEYS_EVERYWHERE,
		name = "Hotkeys everywhere",
		description = "Let the hotkeys work anywhere, not only at Zulrah (handy for practising with the panel)",
		section = SECTION_BEHAVIOUR,
		position = 4
	)
	default boolean hotkeysEverywhere()
	{
		return false;
	}

	// ---- hotkeys -------------------------------------------------------------------------

	@ConfigItem(keyName = "nextPhaseHotkey", name = "Next phase", description = "Step to the next phase", section = SECTION_HOTKEYS, position = 0)
	default Keybind nextPhaseHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "previousPhaseHotkey", name = "Previous phase", description = "Step back one phase", section = SECTION_HOTKEYS, position = 1)
	default Keybind previousPhaseHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "resetHotkey", name = "Reset", description = "Back to the opening phase", section = SECTION_HOTKEYS, position = 2)
	default Keybind resetHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "rotation1Hotkey", name = "Rotation 1 (Crimson A)", description = "Set the rotation to Crimson A", section = SECTION_HOTKEYS, position = 3)
	default Keybind rotation1Hotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "rotation2Hotkey", name = "Rotation 2 (Crimson B)", description = "Set the rotation to Crimson B", section = SECTION_HOTKEYS, position = 4)
	default Keybind rotation2Hotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "rotation3Hotkey", name = "Rotation 3 (Serp)", description = "Set the rotation to Serp", section = SECTION_HOTKEYS, position = 5)
	default Keybind rotation3Hotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(keyName = "rotation4Hotkey", name = "Rotation 4 (Tanz)", description = "Set the rotation to Tanz", section = SECTION_HOTKEYS, position = 6)
	default Keybind rotation4Hotkey()
	{
		return Keybind.NOT_SET;
	}

	// ---- kill stats ----------------------------------------------------------------------

	@ConfigItem(
		keyName = "showKillStats",
		name = "Show kill stats",
		description = "Show kills this session, the last fight duration and a timer that starts when you enter the shrine",
		section = SECTION_KILLS,
		position = 0
	)
	default boolean showKillStats()
	{
		return true;
	}

	// ---- learner -------------------------------------------------------------------------

	@ConfigItem(
		keyName = "killSummaries",
		name = "Kill summaries",
		description = "After each kill, show damage taken, venom, food, potions and prayer used, broken down by the phases you stepped through",
		section = SECTION_LEARNER,
		position = 0
	)
	default boolean killSummaries()
	{
		return true;
	}

	@ConfigItem(
		keyName = "killSummaryChat",
		name = "Summary in chat",
		description = "Also post the one-line kill summary as a game message",
		section = SECTION_LEARNER,
		position = 1
	)
	default boolean killSummaryChat()
	{
		return true;
	}
}
