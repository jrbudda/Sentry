package net.aufdemrand.sentry;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

import net.citizensnpcs.trait.Toggleable;

public class SentryTrait extends Trait implements Toggleable {

private Map<String, Boolean> triggerMap = new HashMap<String, Boolean>();
private Sentry plugin;

private boolean isToggled = true;

	protected SentryTrait() {
		super("sentry");
	}


	@Override
public void load(DataKey key) throws NPCLoadException {
		plugin = (Sentry) Bukkit.getServer().getPluginManager().getPlugin("Sentry");

		isToggled = key.getBoolean("toggled", false);
}


@Override
public void save(DataKey key) {

	key.setBoolean("toggled", isToggled);
}

@Override
public boolean toggle() {
isToggled = !isToggled;
return isToggled;
}


public boolean isToggled() {
return isToggled;
}

}
