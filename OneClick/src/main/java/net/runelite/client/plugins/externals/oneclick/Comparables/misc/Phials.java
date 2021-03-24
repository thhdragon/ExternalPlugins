package net.runelite.client.plugins.externals.oneclick.comparables.misc;

import com.google.common.collect.ImmutableSet;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.plugins.externals.oneclick.comparables.ClickCompare;

import java.util.Set;

public class Phials extends ClickCompare
{
	private static final Set<Integer> NOTES = ImmutableSet.of(
			961, 8779, 8781, 8783, 1522, 6334, 8836
	);
	@Override
	public boolean isEntryValid(MenuEntry event)
	{
		return event.getOpcode() == MenuAction.NPC_FIRST_OPTION.getId() && !event.isForceLeftClick() &&
				event.getTarget().contains("Phials");
	}

	@Override
	public void modifyEntry(MenuEntry event)
	{
		if (findItem(NOTES).getLeft() == -1)
		{
			return;
		}

		MenuEntry e = event.clone();
		e.setOption("Use");
		e.setTarget("<col=ff9040>Notes<col=ffffff> -> " + getTargetMap().get(event.getIdentifier()));
		e.setOpcode(MenuAction.ITEM_USE_ON_NPC.getId());
		e.setForceLeftClick(true);
		insert(e);
	}

	@Override
	public boolean isClickValid(MenuOptionClicked event)
	{
		return event.getMenuAction() == MenuAction.ITEM_USE_ON_NPC &&
				event.getMenuTarget().contains("<col=ff9040>Notes<col=ffffff> -> ");
	}

	@Override
	public void modifyClick(MenuOptionClicked event)
	{
		updateSelectedItem(NOTES);
	}

	@Override
	public void backupEntryModify(MenuEntry e)
	{
		if (findItem(NOTES).getLeft() == -1)
		{
			return;
		}

		e.setOption("Use");
		e.setTarget("<col=ff9040>Notes<col=ffffff> -> " + getTargetMap().get(e.getIdentifier()));
		e.setOpcode(MenuAction.ITEM_USE_ON_NPC.getId());
		e.setForceLeftClick(true);
	}
}
