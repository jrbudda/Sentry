package net.aufdemrand.sentry;

//import java.util.HashMap;
import java.util.List;
//import java.util.Map;
import java.util.logging.Level;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.Owner;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Sentry extends JavaPlugin {

		
	public static Permission perms = null;
	public boolean debug = false;;

	@Override
	public void onEnable() {
		setupPermissions();
		CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(SentryTrait.class).withName("sentry"));
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

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] args) {

		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be in-game to execute commands.");
			return true;
		}

		if (args.length < 1) {
			sender.sendMessage(ChatColor.RED + "Use /sentry help for command reference.");
			return true;
		}

		Player player = (Player) sender;

		if (args[0].equalsIgnoreCase("help")) {

			player.sendMessage(ChatColor.GOLD + "------- Sentry Commands -------");
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
			return true;
		}


		else if (args[0].equalsIgnoreCase("debug")) {

			if (debug) debug = false;
			else debug = true;

			player.sendMessage(ChatColor.GREEN + "Debug now: " + debug);
			return true;
		}


		NPC ThisNPC;
   
		if(!player.getMetadata("selected").isEmpty() ){
			ThisNPC = CitizensAPI.getNPCRegistry().getById(player.getMetadata("selected").get(0).asInt());      // Gets NPC Selected
		}
		else{
			player.sendMessage(ChatColor.RED + "You must have a NPC selected to use this command");
			return true;
		}


		if (!ThisNPC.getTrait(Owner.class).getOwner().equalsIgnoreCase(player.getName())) {
			player.sendMessage(ChatColor.RED + "You must be the owner of the sentry to execute commands.");
			return true;
		}

		if (!ThisNPC.hasTrait(SentryTrait.class)) {
			player.sendMessage(ChatColor.RED + "That command must be performed on a sentry!");
			return true;
		}

		// Commands
				
	  	SentryInstance inst =	ThisNPC.getTrait(SentryTrait.class).getInstance();

		if (args[0].equalsIgnoreCase("invincible")) {
			if (inst.Invincible) {
				player.sendMessage(ChatColor.GREEN + "Sentry now takes damage..");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + "Sentry now INVINCIBLE.");   // Talk to the player.
			}

			inst.Invincible = !inst.Invincible;

			return true;
		}
		else if (args[0].equalsIgnoreCase("retaliate")) {
			if (inst.Retaliate) {
				player.sendMessage(ChatColor.GREEN + "Sentry will not retaliate.");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + "Sentry will retalitate against all attackers.");   // Talk to the player.
			}

			inst.Retaliate = !inst.Retaliate;

			return true;
		}
		else if (args[0].equalsIgnoreCase("criticals")) {
			if (inst.LuckyHits) {
				player.sendMessage(ChatColor.GREEN + "Sentry will take normal damamge.");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + "Sentry will take critical hits.");   // Talk to the player.
			}

			inst.LuckyHits = !inst.LuckyHits;

			return true;
		}
		else if (args[0].equalsIgnoreCase("drops")) {
				if (inst.DestroyInventory) {
				player.sendMessage(ChatColor.GREEN + "Sentry will drop items");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + "Sentry will not drop items.");   // Talk to the player.
			}

			inst.DestroyInventory = !inst.DestroyInventory;

			return true;
		}


		else if (args[0].equalsIgnoreCase("health")) {
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Health is " + inst.sentryHealth);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry health [1-20]   note: Typically players");
				player.sendMessage(ChatColor.GOLD + "  have 20 HPs when fully healed");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 20) HPs = 20;
				if (HPs <1)  HPs =1;

				player.sendMessage(ChatColor.GREEN + "Sentry health set to " + Integer.valueOf(args[1]) + ".");   // Talk to the player.
				inst.sentryHealth = HPs;
			}

			return true;
		}

		else if (args[0].equalsIgnoreCase("armor")) {
			
			
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Armor is " + inst.Armor);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry armor [0-10] ");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 10) HPs = 10;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + "Sentry armor set to " + HPs + ".");   // Talk to the player.
				inst.Armor = HPs;
			
			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("strength")) {
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Strength is " + inst.Strength);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry strength [0-10] ");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 10) HPs = 10;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + "Sentry strength set to " + HPs+ ".");   // Talk to the player.
				inst.Strength = HPs;
			
			}

			return true;
		}

		else if (args[0].equalsIgnoreCase("speed")) {
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Speed is " + inst.sentrySpeed);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry speed [0.0 - 1.5]");
			}
			else {

				double HPs = Double.valueOf(args[1]);
				if (HPs > 1.5) HPs = 1.5;
				if (HPs <0.0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + "Sentry speed set to " + HPs + ".");   // Talk to the player.
				inst.sentrySpeed = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("attackrate")) {
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Projectile Attack Rate is " + inst.AttackRateSeconds + "s between shots." );
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry speed [0.0 - 30.0]");
			}
			else {

				double HPs = Double.valueOf(args[1]);
				if (HPs > 30.0) HPs = 1.5;
				if (HPs < 0.0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + "Sentry Projectile Attack Rate set to " + HPs + ".");   // Talk to the player.
				inst.AttackRateSeconds = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("range")) {
			
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Range is " + inst.sentryRange);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry range [1 - 100]");
			}
			
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 100) HPs = 100;
				if (HPs <1)  HPs =1;

				player.sendMessage(ChatColor.GREEN + "Sentry range set to " + HPs + ".");   // Talk to the player.
				inst.sentryRange = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("info")) {
			player.sendMessage(ChatColor.GOLD + "------- Sentry Info for " + ThisNPC.getName() + "------");
			player.sendMessage(ChatColor.GREEN + inst.getStats());
			player.sendMessage(ChatColor.GREEN + "Invincible: " + inst.Invincible);
			player.sendMessage(ChatColor.GREEN + "Retaliate: " + inst.Retaliate);
			player.sendMessage(ChatColor.GREEN + "Drops Items: " + !inst.DestroyInventory);
			player.sendMessage(ChatColor.GREEN + "Critical Hits: " + inst.LuckyHits);
			player.sendMessage(ChatColor.GREEN + "Respawn Delay: " + inst.RespawnDelaySeconds + "s");
			player.sendMessage(ChatColor.BLUE + "Status: " + inst.sentryStatus);
			return true;
		}

		else if (args[0].equalsIgnoreCase("target")) {

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
					player.sendMessage(ChatColor.GREEN + "Target added. Now targeting " + currentList.toString());
					return true;
				}

				else if (args[1].equals("remove") && args.length > 2) {

					List<String> currentList =	inst.validTargets;
					currentList.remove(args[2].toUpperCase());
					player.sendMessage(ChatColor.GREEN + "Targets removed. Now targeting " + currentList.toString());
					return true;
				}

				else if (args[1].equals("clear")) {

					List<String> currentList =	inst.validTargets;
					currentList.clear();
					player.sendMessage(ChatColor.GREEN + "Targets cleared. Now targeting " + currentList.toString());
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

		return true;
	}




	// End of CLASS

}
