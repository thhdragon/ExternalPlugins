/*
 * Copyright (c) 2019-2020, ganom <https://github.com/Ganom>
 * Copyright (c) 2019, TomC <https://github.com/tomcylke>
 * All rights reserved.
 * Licensed under GPL3, see LICENSE for the full scope.
 */
package net.runelite.client.plugins.externals.oneclick;

import static net.runelite.api.MenuAction.MENU_ACTION_DEPRIORITIZE_OFFSET;
import com.google.inject.Provides;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.externals.oneclick.comparables.ClickCompare;
import net.runelite.client.plugins.externals.oneclick.comparables.misc.Blank;
import net.runelite.client.plugins.externals.oneclick.comparables.misc.Custom;
import net.runelite.client.plugins.externals.oneclick.comparables.misc.Healer;
import net.runelite.client.plugins.externals.oneclick.comparables.skilling.Spell;
import org.pf4j.Extension;

@Extension
@PluginDescriptor(
		name = "One Click",
		description = "OP One Click methods."
)
@Getter
@Slf4j
public class OneClickPlugin extends Plugin
{

	private static final String MAGIC_IMBUE_EXPIRED_MESSAGE = "Your Magic Imbue charge has ended.";
	private static final String MAGIC_IMBUE_MESSAGE = "You are charged to combine runes!";
	private final Custom custom = new Custom();
	private final Map<Integer, String> targetMap = new HashMap<>();
	@Inject
	private Client client;
	@Inject
	private OneClickConfig config;
	private ClickCompare comparable = new Blank();
	private boolean enableImbue;
	private boolean imbue;

	@Setter
	private boolean tick;

	@Provides
	OneClickConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(OneClickConfig.class);
	}

	@Override
	protected void startUp()
	{
		updateConfig();
	}

	@Override
	protected void shutDown()
	{

	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && imbue)
		{
			imbue = false;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"oneclick".equals(event.getGroup()))
		{
			return;
		}
		updateConfig();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		switch (event.getMessage())
		{
			case MAGIC_IMBUE_MESSAGE:
				imbue = true;
				break;
			case MAGIC_IMBUE_EXPIRED_MESSAGE:
				imbue = false;
				break;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		tick = false;

		if (comparable instanceof Healer)
		{
			Widget widget = client.getWidget(488, 8);

			if (widget != null && widget.getText() != null)
			{
				((Healer) comparable).setRoleText(widget.getText().trim());
			}
			else
			{
				((Healer) comparable).setRoleText("");
			}
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (comparable instanceof Spell)
		{
			((Spell) comparable).onMenuOpened(event);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!event.isForceLeftClick())
		{
			targetMap.put(event.getIdentifier(), event.getTarget());
		}

		{
			if (config.getType() == Types.SEED_SET)
			{
				if (event.getOpcode() == MenuAction.WALK.getId())
				{
					MenuEntry menuEntry = client.getLeftClickMenuEntry();
					menuEntry.setOpcode(MenuAction.WALK.getId() + MENU_ACTION_DEPRIORITIZE_OFFSET);
					client.setLeftClickMenuEntry(menuEntry);
				}
			}
		}
		if (config.customInvSwap() && custom.isEntryValid(event))
		{
			custom.modifyEntry(event);
			return;
		}
		if (comparable == null)
		{
			throw new AssertionError("This should not be possible.");
		}

		if (comparable.isEntryValid(event))
		{
			comparable.modifyEntry(event);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (tick)
		{
			event.consume();
			return;
		}

		if (event.getMenuTarget() == null)
		{
			return;
		}

		if (config.customInvSwap() && custom.isClickValid(event))
		{
			custom.modifyClick(event);
			return;
		}

		if (comparable == null)
		{
			throw new AssertionError("This should not be possible.");
		}

		if (comparable.isClickValid(event))
		{
			comparable.modifyClick(event);
			return;
		}

		MenuEntry old = new MenuEntry(
				event.getMenuOption(), event.getMenuTarget(), event.getId(), event.getMenuAction().getId(),
				event.getActionParam(), event.getWidgetId(), false
		);
		MenuEntry tmp = old.clone();
		boolean updated = false;

		if (comparable.isEntryValid(tmp))
		{
			comparable.backupEntryModify(tmp);
			event.setMenuEntry(tmp);
			updated = true;
		}

		if (comparable.isClickValid(event) && updated)
		{
			comparable.modifyClick(event);
		}
		else if (!comparable.isClickValid(event) && updated)
		{
			event.setMenuEntry(old);
		}
	}

	private void updateConfig()
	{
		enableImbue = config.isUsingImbue();

		Types type = config.getType();

		if (config.customInvSwap())
		{
			custom.updateMap(config.swaps());
			custom.setClient(client);
			custom.setPlugin(this);
		}

		if (type == Types.SPELL)
		{
			comparable = config.getSpells().getComparable();
			comparable.setClient(client);
			comparable.setPlugin(this);
			if (comparable instanceof Spell)
			{
				((Spell) comparable).setSpellSelection(config.getSpells());
			}
		}
		else
		{
			comparable = type.getComparable();
			comparable.setClient(client);
			comparable.setPlugin(this);
		}
	}
}
