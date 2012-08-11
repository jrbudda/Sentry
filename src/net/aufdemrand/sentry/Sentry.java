package net.aufdemrand.sentry;

//import java.util.HashMap;
import java.rmi.activation.ActivationException;
import java.util.List;
//import java.util.Map;
import java.util.logging.Level;

import net.aufdemrand.denizen.Denizen;
import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.Owner;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Sentry extends JavaPlugin {

	public static Permission perms = null;
	public boolean debug = false;;


	@Override
	public void onEnable() {
		setupPermissions();


		try {
			setupDenizenHook();
		} catch (ActivationException e) {
			getLogger().log(Level.WARNING, "An error occured attempting to register the NPCDeath trigger with Denizen" + e.getMessage());
		}

		if (_dplugin != null)	getLogger().log(Level.INFO,"NPCDeath Trigger registered sucessfully with Denizen");

		CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(SentryTrait.class).withName("sentry"));
		this.getServer().getPluginManager().registerEvents(new SentryListener(this), this);
	}


	//***Denizen Hook
	private NpcdeathTrigger _denizenTrigger = null;
	private Denizen _dplugin = null;


	public boolean SentryDeath(List<Player> players, NPC npc){
		if (_denizenTrigger !=null && npc !=null) return	_denizenTrigger.Die(players, npc);
		return false;
	}

	private void setupDenizenHook() throws ActivationException {
		_dplugin = (Denizen) this.getServer().getPluginManager().getPlugin("Denizen");
		if (_dplugin != null) {
			_denizenTrigger = new NpcdeathTrigger();
			try {
				_denizenTrigger.activateAs("Npcdeath");
				DieCommand dc = new DieCommand();
				dc.activateAs("DIE");
			} catch (ActivationException e) {
				_dplugin =null;
				_denizenTrigger =null;
				throw e;
			}
		}
	}
	///

	public SentryInstance getSentry(Entity ent){
		if( ent == null) return null;
		NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(ent);
		if (npc !=null && npc.hasTrait(SentryTrait.class)){
			return npc.getTrait(SentryTrait.class).getInstance();
		}

		return null;
	}

	public SentryInstance getSentry(NPC npc){

		if (npc !=null && npc.hasTrait(SentryTrait.class)){
			return npc.getTrait(SentryTrait.class).getInstance();
		}

		return null;
	}

	private boolean setupPermissions() {

		RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
		perms = rsp.getProvider();
		return perms != null;
	}

	@Override
	public void onDisable() {

		getLogger().log(Level.INFO, " v" + getDescription().getVersion() + " disabled.");
		Bukkit.getServer().getScheduler().cancelTasks(this);
	}


	private	boolean tryParseInt(String value)  
	{  
		try  
		{  
			Integer.parseInt(value);  
			return true;  
		} catch(NumberFormatException nfe)  
		{  
			return false;  
		}  
	}


	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] inargs) {

		if (inargs.length < 1) {
			sender.sendMessage(ChatColor.RED + "Use /sentry help for command reference.");
			return true;
		}

		CommandSender player = (CommandSender) sender;

		int npcid = -1;
		int i = 0;

		//did player specify a id?
		if (tryParseInt(inargs[0])) {
			npcid = Integer.parseInt(inargs[0]);
			i = 1;
		}

		String[] args = new String[inargs.length-i];

		for (int j = i; j < inargs.length; j++) {
			args[j-i] = inargs[j];
		}

		
		if (args.length < 1) {
			sender.sendMessage(ChatColor.RED + "Use /sentry help for command reference.");
			return true;
		}
		

		if (args[0].equalsIgnoreCase("help")) {

			player.sendMessage(ChatColor.GOLD + "------- Sentry Commands -------");
			player.sendMessage(ChatColor.GOLD + "You can use /sentry (id) [command] [args] to perform any of these commands on a sentry without having it selected.");			
			player.sendMessage(ChatColor.GOLD + "");
			player.sendMessage(ChatColor.GOLD + "/sentry target [add|remove] [target]");
			player.sendMessage(ChatColor.GOLD + "  Adds or removes a target to attack.");
			player.sendMessage(ChatColor.GOLD + "/sentry target [list|clear]");
			player.sendMessage(ChatColor.GOLD + "  View or clear the target list..");
			player.sendMessage(ChatColor.GOLD + "/sentry info");
			player.sendMessage(ChatColor.GOLD + "  View all Sentry attributes");
			player.sendMessage(ChatColor.GOLD + "/sentry speed [0-1.5]");
			player.sendMessage(ChatColor.GOLD + "  Sets speed of the Sentry when attacking.");
			player.sendMessage(ChatColor.GOLD + "/sentry health [1-20]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's Health .");
			player.sendMessage(ChatColor.GOLD + "/sentry armor [0-10]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's Armor.");
			player.sendMessage(ChatColor.GOLD + "/sentry strength [0-10]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's Strength.");
			player.sendMessage(ChatColor.GOLD + "/sentry attackrate [0.0-30.0]");
			player.sendMessage(ChatColor.GOLD + "  Sets the time between the Sentry's projectile attacks.");
			player.sendMessage(ChatColor.GOLD + "/sentry healrate [0.0-300.0]");
			player.sendMessage(ChatColor.GOLD + "  Sets the frequency the sentry will heal 1 point. 0 to disable.");
			player.sendMessage(ChatColor.GOLD + "/sentry range [1-100]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's detection range.");
			player.sendMessage(ChatColor.GOLD + "/sentry invincible");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to take no damage or knockback.");
			player.sendMessage(ChatColor.GOLD + "/sentry retaliate");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to always attack an attacker.");
			player.sendMessage(ChatColor.GOLD + "/sentry criticals");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to take critical hits and misses");
			player.sendMessage(ChatColor.GOLD + "/sentry drops");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to drop equipped items on death");
			player.sendMessage(ChatColor.GOLD + "/sentry spawn");
			player.sendMessage(ChatColor.GOLD + "  Set the sentry to respawn at its current location");
			return true;
		}
		else if (args[0].equalsIgnoreCase("debug")) {

			if (debug) debug = false;
			else debug = true;

			player.sendMessage(ChatColor.GREEN + "Debug now: " + debug);
			return true;
		}

		NPC ThisNPC;

		if (npcid == -1){

			ThisNPC =	((Citizens)	this.getServer().getPluginManager().getPlugin("Citizens")).getNPCSelector().getSelected(sender);

			if(ThisNPC != null ){
				// Gets NPC Selected
				npcid = ThisNPC.getId();
			}

			else{
				player.sendMessage(ChatColor.RED + "You must have a NPC selected to use this command");
				return true;
			}			
		}


		ThisNPC = CitizensAPI.getNPCRegistry().getById(npcid); 

		if (ThisNPC == null) {
			player.sendMessage(ChatColor.RED + "NPC with id " + npcid + " not found");
			return true;
		}

		if (sender instanceof Player){
			if (!ThisNPC.getTrait(Owner.class).getOwner().equalsIgnoreCase(player.getName())) {

				player.sendMessage(ChatColor.RED + "You must be the owner of the sentry to execute commands.");

				return true;
			}			
		}


		if (!ThisNPC.hasTrait(SentryTrait.class)) {
			player.sendMessage(ChatColor.RED + "That command must be performed on a sentry!");
			return true;
		}

		// Commands

		SentryInstance inst =	ThisNPC.getTrait(SentryTrait.class).getInstance();

		if (args[0].equalsIgnoreCase("spawn")) {
			if(!player.hasPermission("sentry.spawn")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			inst.Spawn = ThisNPC.getBukkitEntity().getLocation();
			player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will respawn at its present location.");   // Talk to the player.
			return true;

		}
		else if (args[0].equalsIgnoreCase("invincible")) {
			if(!player.hasPermission("sentry.options.invincible")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (inst.Invincible) {
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " now takes damage..");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " now INVINCIBLE.");   // Talk to the player.
			}

			inst.Invincible = !inst.Invincible;

			return true;
		}
		else if (args[0].equalsIgnoreCase("retaliate")) {
			if(!player.hasPermission("sentry.options.retaliate")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (inst.Retaliate) {
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will not retaliate.");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will retalitate against all attackers.");   // Talk to the player.
			}

			inst.Retaliate = !inst.Retaliate;

			return true;
		}
		else if (args[0].equalsIgnoreCase("criticals")) {
			if(!player.hasPermission("sentry.options.criticals")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (inst.LuckyHits) {
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will take normal damamge.");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN +ThisNPC.getName() + " will take critical hits.");   // Talk to the player.
			}

			inst.LuckyHits = !inst.LuckyHits;

			return true;
		}
		else if (args[0].equalsIgnoreCase("drops")) {
			if(!player.hasPermission("sentry.options.retaliate")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (!inst.DropInventory) {
				player.sendMessage(ChatColor.GREEN +  ThisNPC.getName() + " will drop items");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will not drop items.");   // Talk to the player.
			}

			inst.DropInventory = !inst.DropInventory;

			return true;
		}
		else if (args[0].equalsIgnoreCase("guard")) {
			if(!player.hasPermission("sentry.guard")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			if (args.length > 1) {
				if (inst.setGuardTarget(args[1])) {
					player.sendMessage(ChatColor.GREEN +  ThisNPC.getName() + " is now guarding "+ args[1] );   // Talk to the player.
				}
				else {
					player.sendMessage(ChatColor.RED +  ThisNPC.getName() + " could not find " + args[1] + " in range.");   // Talk to the player.
				}
			}

			else
			{
				player.sendMessage(ChatColor.GREEN +  ThisNPC.getName() + " is now guarding a location. " );   // Talk to the player.
			}

			return true;
		}

		else if (args[0].equalsIgnoreCase("health")) {
			if(!player.hasPermission("sentry.stats.health")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Health is " + inst.sentryHealth);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry health [1-20]   note: Typically players");
				player.sendMessage(ChatColor.GOLD + "  have 20 HPs when fully healed");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 20) HPs = 20;
				if (HPs <1)  HPs =1;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " health set to " + Integer.valueOf(args[1]) + ".");   // Talk to the player.
				inst.sentryHealth = HPs;
			}

			return true;
		}

		else if (args[0].equalsIgnoreCase("armor")) {
			if(!player.hasPermission("sentry.stats.armor")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Armor is " + inst.Armor);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry armor [0-10] ");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 10) HPs = 10;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " armor set to " + HPs + ".");   // Talk to the player.
				inst.Armor = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("strength")) {
			if(!player.hasPermission("sentry.stats.strength")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Strength is " + inst.Strength);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry strength [0-10] ");
				player.sendMessage(ChatColor.GOLD + "Note: At Strength 0 the Sentry will do no damamge. ");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 10) HPs = 10;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " strength set to " + HPs+ ".");   // Talk to the player.
				inst.Strength = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("nightvision")) {
			if(!player.hasPermission("sentry.stats.nightvision")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Night Vision is " + inst.NightVision);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry nightvision [0-16] ");
				player.sendMessage(ChatColor.GOLD + "Usage: 0 = See nothing, 16 = See everything. ");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 10) HPs = 10;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Night Vision set to " + HPs+ ".");   // Talk to the player.
				inst.NightVision = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("respawn")) {
			if(!player.hasPermission("sentry.stats.respawn")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				if(inst.RespawnDelaySeconds == -1  ) player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " will not automatically respawn.");
				if(inst.RespawnDelaySeconds == 0 ) player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " will be deleted upon death");
				if(inst.RespawnDelaySeconds > 0 ) player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " respawns after " + inst.RespawnDelaySeconds + "s");

				player.sendMessage(ChatColor.GOLD + "Usage: /sentry respawn [-1 - 999999] ");
				player.sendMessage(ChatColor.GOLD + "Usage: set to 0 to prevent automatic respawn");
				player.sendMessage(ChatColor.GOLD + "Usage: set to -1 to *permanently* delete the Sentry on death.");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 300) HPs = 300;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " now respawns after " + HPs+ "s.");   // Talk to the player.
				inst.RespawnDelaySeconds = HPs;

			}
			return true;
		}

		else if (args[0].equalsIgnoreCase("speed")) {
			if(!player.hasPermission("sentry.stats.speed")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Speed is " + inst.sentrySpeed);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry speed [0.0 - 1.5]");
			}
			else {

				Float HPs = Float.valueOf(args[1]);
				if (HPs > 1.5) HPs = 1.5f;
				if (HPs <0.0)  HPs =0f;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " speed set to " + HPs + ".");   // Talk to the player.
				inst.sentrySpeed = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("attackrate")) {
			if(!player.hasPermission("sentry.stats.attackrate")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Projectile Attack Rate is " + inst.AttackRateSeconds + "s between shots." );
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry attackrate [0.0 - 30.0]");
			}
			else {

				Double HPs = Double.valueOf(args[1]);
				if (HPs > 30.0) HPs = 30.0;
				if (HPs < 0.0)  HPs = 0.0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Projectile Attack Rate set to " + HPs + ".");   // Talk to the player.
				inst.AttackRateSeconds = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("healrate")) {
			if(!player.hasPermission("sentry.stats.healrate")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Heal Rate is " + inst.HealRate + "s" );
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry healrate [0.0 - 300.0]");
				player.sendMessage(ChatColor.GOLD + "Usage: Set to 0 to disable healing");
			}
			else {

				Double HPs = Double.valueOf(args[1]);
				if (HPs > 300.0) HPs = 300.0;
				if (HPs < 0.0)  HPs = 0.0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Heal Rate set to " + HPs + ".");   // Talk to the player.
				inst.HealRate = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("range")) {
			if(!player.hasPermission("sentry.stats.range")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Range is " + inst.sentryRange);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry range [1 - 100]");
			}

			else {

				Integer HPs = Integer.valueOf(args[1]);
				if (HPs > 100) HPs = 100;
				if (HPs <1)  HPs =1;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " range set to " + HPs + ".");   // Talk to the player.
				inst.sentryRange = HPs;

			}

			return true;
		}

		else if (args[0].equalsIgnoreCase("info")) {
			if(!player.hasPermission("sentry.info")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			player.sendMessage(ChatColor.GOLD + "------- Sentry Info for " + ThisNPC.getName() + "------");
			player.sendMessage(ChatColor.GREEN + inst.getStats());
			player.sendMessage(ChatColor.GREEN + "Invincible: " + inst.Invincible + "  Retaliate: " + inst.Retaliate);
			player.sendMessage(ChatColor.GREEN + "Drops Items: " + inst.DropInventory+ "  Critical Hits: " + inst.LuckyHits);
			player.sendMessage(ChatColor.GREEN + "Critical Hits: " + inst.LuckyHits + "Respawn Delay: " + inst.RespawnDelaySeconds + "s");
			player.sendMessage(ChatColor.GREEN + "Friendly Fire: " + inst.FriendlyFire );
			player.sendMessage(ChatColor.BLUE + "Status: " + inst.sentryStatus);
			if (inst.meleeTarget == null){
				if(inst.projectileTarget ==null) player.sendMessage(ChatColor.BLUE + "Target: Nothing");
				else	player.sendMessage(ChatColor.BLUE + "Target: " + inst.projectileTarget.toString());
			}
			else 		player.sendMessage(ChatColor.BLUE + "Target: " + inst.meleeTarget.toString());

			if (inst.getGuardTarget() == null)	player.sendMessage(ChatColor.BLUE + "Guarding: My Location");
			else 		player.sendMessage(ChatColor.BLUE + "Guarding: " + inst.getGuardTarget().toString());

			return true;
		}

		else if (args[0].equalsIgnoreCase("target")) {
			if(!player.hasPermission("sentry.target")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			if (args.length<2 ){
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target add [entity:Name] or [player:Name] or [group:Name] or [entity:monster] or [entity:player]");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target remove [target]");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target clear");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target list");
				return true;
			}

			else {

				if (args[1].equals("add") && args.length > 2) {

					List<String> currentList =	inst.validTargets;
					currentList.add(args[2].toUpperCase());
					inst.setGuardTarget(null);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Target added. Now targeting " + currentList.toString());
					return true;
				}

				else if (args[1].equals("remove") && args.length > 2) {

					List<String> currentList =	inst.validTargets;
					currentList.remove(args[2].toUpperCase());
					inst.setGuardTarget(null);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Targets removed. Now targeting " + currentList.toString());
					return true;
				}

				else if (args[1].equals("clear")) {

					List<String> currentList =	inst.validTargets;
					currentList.clear();
					inst.setGuardTarget(null);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Targets cleared. Now targeting " + currentList.toString());
					return true;
				}
				else if (args[1].equals("list")) {

					List<String> currentList =	inst.validTargets;
					player.sendMessage(ChatColor.GREEN + "Targets: " + currentList.toString());
					return true;
				}

				else {

					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target add [ENTITY:Name] or [PLAYER:Name] or [GROUP:Name] or [ENTITY:MONSTER]");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target remove [ENTITY:Name] or [PLAYER:Name] or [GROUP:Name] or [ENTITY:MONSTER]");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target clear");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target list");
					return true;
				}
			}
		}
		return false;
	}




	// End of CLASS

}
