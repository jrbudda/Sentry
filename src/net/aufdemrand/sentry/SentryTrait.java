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

	public SentryTrait() {
		super("sentry");
	}
	private SentryInstance thisInstance;

	@Override
	public void load(DataKey key) throws NPCLoadException {
	
				plugin = (Sentry) Bukkit.getServer().getPluginManager().getPlugin("Sentry");
				isToggled = key.getBoolean("toggled", false);

				plugin = (Sentry) Bukkit.getPluginManager().getPlugin("Sentry");

				plugin.getServer().broadcastMessage("NPC SPAWNED!");

				 thisInstance = new SentryInstance(plugin);
				 thisInstance.myTrait = this;
					plugin.initializedSentries.put(this.getNPC().getId(), thisInstance);
			plugin.getServer().getPluginManager().registerEvents(thisInstance, plugin);
			
					
	}


	@Override
	public void onSpawn() {
		thisInstance.initialize(this.getNPC());	
		
	}



	@Override
	public void onRemove() {

		plugin = (Sentry) Bukkit.getPluginManager().getPlugin("Sentry");

		plugin.getServer().broadcastMessage("NPC DESPAWNED!");

		plugin.initializedSentries.get(this.getNPC().getId()).deactivate();
		plugin.initializedSentries.remove(this.getNPC().getId());
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
