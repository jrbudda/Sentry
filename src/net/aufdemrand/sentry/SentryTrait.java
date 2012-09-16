package net.aufdemrand.sentry;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import net.citizensnpcs.api.exception.NPCLoadException;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.trait.Toggleable;

public class SentryTrait extends Trait implements Toggleable {

	private Sentry plugin = null;

	private boolean isToggled = true;

	public SentryTrait() {
		super("sentry");
	}
	private SentryInstance thisInstance;

	@SuppressWarnings("unchecked")
	@Override
	public void load(DataKey key) throws NPCLoadException {

		if(plugin==null)	plugin = (Sentry) Bukkit.getServer().getPluginManager().getPlugin("Sentry");

		if (thisInstance !=null ){
			thisInstance.cancelRunnable();
			if (thisInstance.myNPC != null) thisInstance.myNPC.despawn();
			thisInstance = null;
		}

		thisInstance = new SentryInstance(plugin);
		thisInstance.myTrait = this;

		if(key.keyExists("traits")) key = key.getRelative("traits");

		isToggled=	key.getBoolean("toggled", true);
		thisInstance.Retaliate=	key.getBoolean("Retaliate", plugin.getConfig().getBoolean("DefaultOptions.Retaliate",true));
		thisInstance.Invincible=	key.getBoolean("Invincinble", plugin.getConfig().getBoolean("DefaultOptions.Invincible",false));
		thisInstance.DropInventory=	key.getBoolean("DropInventory", plugin.getConfig().getBoolean("DefaultOptions.Drops",false));
		thisInstance.FriendlyFire = key.getBoolean("FriendlyFire", plugin.getConfig().getBoolean("DefaultOptions.FriendlyFire",false));
		thisInstance.LuckyHits=	key.getBoolean("CriticalHits", plugin.getConfig().getBoolean("DefaultOptions.Criticals",true));
		thisInstance.sentryHealth=	key.getInt("Health", plugin.getConfig().getInt("DefaultStats.Health",20));
		thisInstance.sentryRange=	key.getInt("Range", plugin.getConfig().getInt("DefaultStats.Range",10));
		thisInstance.RespawnDelaySeconds=	key.getInt("RespawnDelay", plugin.getConfig().getInt("DefaultStats.Respawn",10));
		thisInstance.sentrySpeed=	(float) (key.getDouble("Speed", plugin.getConfig().getDouble("DefaultStats.Speed",1.0)));
		thisInstance.sentryWeight=	 key.getDouble("Weight", plugin.getConfig().getDouble("DefaultStats.Weight",1.0));
		thisInstance.Armor=		key.getInt("Armor", plugin.getConfig().getInt("DefaultStats.Armor",0));
		thisInstance.Strength=		key.getInt("Strength", plugin.getConfig().getInt("DefaultStats.Strength",1));
		thisInstance.guardTarget = (key.getString("GuardTarget", null));
		thisInstance.GreetingMessage = (key.getString("Greeting",plugin.getConfig().getString("DefaultTexts.Greeting", "'§b<NPC> says Welcome, <PLAYER>'")));
		thisInstance.WarningMessage = (key.getString("Warning",plugin.getConfig().getString("DefaultTexts.Warning", "'§c<NPC> says Halt! Come no closer!'")));
		thisInstance.WarningRange = key.getInt("WarningRange", plugin.getConfig().getInt("DefaultStats.WarningRange",0));
		thisInstance.AttackRateSeconds =  key.getDouble("AttackRate", plugin.getConfig().getDouble("DefaultStats.AttackRate",2.0));
		thisInstance.HealRate =  key.getDouble("HealRate", plugin.getConfig().getDouble("DefaultStats.HealRate",0.0));
		thisInstance.NightVision = key.getInt("NightVision", plugin.getConfig().getInt("DefaultStats.NightVision",0));

		if( key.keyExists("Spawn")){
			try {
				thisInstance.Spawn = new Location(plugin.getServer().getWorld(key.getString("Spawn.world")), key.getDouble("Spawn.x"),key.getDouble("Spawn.y"), key.getDouble("Spawn.z"), (float) key.getDouble("Spawn.yaw"), (float) key.getDouble("Spawn.pitch"));
			} catch (Exception e) {
				thisInstance.Spawn = null;
			}

			if(  thisInstance.Spawn.getWorld() == null ) thisInstance.Spawn = null;
		}

		Object derp =  key.getRaw("Targets");
		if (derp !=null) thisInstance.validTargets= (List<String>) key.getRaw("Targets");
		else thisInstance.validTargets = plugin.getConfig().getStringList("DefaultTargets");

		Object herp =  key.getRaw("Ignores");
		if (herp !=null) thisInstance.ignoreTargets= (List<String>) key.getRaw("Ignores");
		else thisInstance.ignoreTargets = plugin.getConfig().getStringList("DefaultIgnores");

		thisInstance.loaded = true;

	}

	public SentryInstance getInstance(){
		return thisInstance;
	}

	@Override
	public void onSpawn() {

		if (thisInstance == null ) {
			try {
				load(new net.citizensnpcs.api.util.MemoryDataKey());
			} catch (NPCLoadException e) {
			}

		}
		
		
		if (!plugin.GroupsChecked) plugin.doGroups(); // lazy checking for lazy vault.

		npc.data().set(NPC.DEFAULT_PROTECTED_METADATA, false);

		thisInstance.myNPC = this.getNPC();

		thisInstance.initialize();	

		// this.getNPC().getBukkitEntity().teleport(this.getNPC().getBukkitEntity().getLocation());

		//plugin.getServer().broadcastMessage("onSpawn");

	}

	@Override
	public void onRemove() {

		//	plugin = (Sentry) Bukkit.getPluginManager().getPlugin("Sentry");

		if (thisInstance!=null){
			//	plugin.getServer().broadcastMessage("onRemove");
			thisInstance.cancelRunnable();
		}

		thisInstance = null;
	}

	@Override
	public void save(DataKey key) {
		if (thisInstance==null) return;
		key.setBoolean("toggled", isToggled);
		key.setBoolean("Retaliate", thisInstance.Retaliate);
		key.setBoolean("Invincinble", thisInstance.Invincible);
		key.setBoolean("DropInventory", thisInstance.DropInventory);
		key.setBoolean("CriticalHits", thisInstance.LuckyHits);
		key.setRaw("Targets", thisInstance.validTargets);
		key.setRaw("Ignores", thisInstance.ignoreTargets);

		if (thisInstance.Spawn!=null){
			key.setDouble("Spawn.x", thisInstance.Spawn.getX());
			key.setDouble("Spawn.y", thisInstance.Spawn.getY());
			key.setDouble("Spawn.z", thisInstance.Spawn.getZ());
			key.setString("Spawn.world", thisInstance.Spawn.getWorld().getName());
			key.setDouble("Spawn.yaw", thisInstance.Spawn.getYaw());
			key.setDouble("Spawn.pitch", thisInstance.Spawn.getPitch());		
		}

		key.setInt("Health", thisInstance.sentryHealth);
		key.setInt("Range", thisInstance.sentryRange);
		key.setInt("RespawnDelay", thisInstance.RespawnDelaySeconds);
		key.setDouble("Speed", (double) thisInstance.sentrySpeed);
		key.setDouble("Weight", thisInstance.sentryWeight);
		key.setDouble("HealRate", thisInstance.HealRate);
		key.setInt("Armor", thisInstance.Armor);
		key.setInt("Strength", thisInstance.Strength);
		key.setInt("WarningRange", thisInstance.WarningRange);
		key.setDouble("AttackRate", thisInstance.AttackRateSeconds);
		key.setBoolean("FriendlyFire", thisInstance.FriendlyFire);
		key.setInt("NightVision", thisInstance.NightVision);
		if (thisInstance.guardTarget !=null)	key.setString("GuardTarget", thisInstance.guardTarget);
		key.setString("Warning",thisInstance.WarningMessage);
		key.setString("Greeting",thisInstance.GreetingMessage);
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
