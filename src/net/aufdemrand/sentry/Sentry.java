package net.aufdemrand.sentry;



import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	public Map<Integer, SentryInstance> initializedSentries = new HashMap<Integer, SentryInstance>();

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
			player.sendMessage(ChatColor.GOLD + "/sentry location [add|remove|list|clear]");
			player.sendMessage(ChatColor.GOLD + "  Adds or removes a location to guard.");
			player.sendMessage(ChatColor.GOLD + "  to the position you are standing on.");
			player.sendMessage(ChatColor.GOLD + "/sentry target [add|remove|list|clear] ([target])");
			player.sendMessage(ChatColor.GOLD + "  Adds or removes an entity to target.");
			player.sendMessage(ChatColor.GOLD + "/sentry ignore [add|remove|list|clear] ([target])");
			player.sendMessage(ChatColor.GOLD + "  Adds or removes an entity to ignore.");			
			player.sendMessage(ChatColor.GOLD + "");			
			player.sendMessage(ChatColor.GOLD + "/sentry speed [0-1.5]");
			player.sendMessage(ChatColor.GOLD + "  Sets speed modifier.");
			player.sendMessage(ChatColor.GOLD + "/sentry health [1-20]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's health points.");
			player.sendMessage(ChatColor.GOLD + "/sentry invincible");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to take no damage or knockback.");
			player.sendMessage(ChatColor.GOLD + "/sentry retaliate");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to always attack an attacker.");
			player.sendMessage(ChatColor.GOLD + "/sentry criticals");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to take critical hits and misses");
			player.sendMessage(ChatColor.GOLD + "/sentry drops");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to drop equipped items on death");
			player.sendMessage(ChatColor.GOLD + "/sentry save|reload");
			player.sendMessage(ChatColor.GOLD + "  Saves or reloads the config.yml.");
			return true;
		}

		else if (args[0].equalsIgnoreCase("save")) {
			this.saveConfig();
			player.sendMessage(ChatColor.GREEN + "Saved config.yml for Sentry.");
			return true;
		}

		else if (args[0].equalsIgnoreCase("debug")) {

			if (debug) debug = false;
			else debug = true;

			player.sendMessage(ChatColor.GREEN + "Debug now: " + debug);
			return true;
		}

		else if (args[0].equalsIgnoreCase("reload")) {
			this.reloadConfig();
			player.sendMessage(ChatColor.GREEN + "Reloaded config.yml for Sentry.");
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

		else if (args[0].equalsIgnoreCase("guard")) {
			player.sendMessage(ChatColor.GREEN + "Sentry now guarding this position.");   // Talk to the player.

			initializedSentries.get(ThisNPC.getId()).guardPosts.add(player.getLocation());

			getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".Guarding Location", 
					player.getWorld().getName() + ";" +
							player.getLocation().getX() + ";" +
							player.getLocation().getY() + ";" +
							player.getLocation().getZ());							

			saveConfig();
			return true;
		}
		else if (args[0].equalsIgnoreCase("invincible")) {
			SentryInstance inst = initializedSentries.get(ThisNPC.getId());
			if (inst.Invincible) {
				player.sendMessage(ChatColor.GREEN + "Sentry now takes damage..");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + "Sentry now INVINCIBLE.");   // Talk to the player.
			}

			inst.Invincible = !inst.Invincible;
			getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".Invincible",inst.Invincible );


			saveConfig();
			return true;
		}
		else if (args[0].equalsIgnoreCase("retaliate")) {
			SentryInstance inst = initializedSentries.get(ThisNPC.getId());
			if (inst.Retaliate) {
				player.sendMessage(ChatColor.GREEN + "Sentry will not retaliate.");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + "Sentry will retalitate against all attackers.");   // Talk to the player.
			}

			inst.Retaliate = !inst.Retaliate;
			getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".Retaliate",inst.Retaliate );


			saveConfig();
			return true;
		}
		else if (args[0].equalsIgnoreCase("criticals")) {
			SentryInstance inst = initializedSentries.get(ThisNPC.getId());
			if (inst.LuckyHits) {
				player.sendMessage(ChatColor.GREEN + "Sentry will take normal damamge.");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + "Sentry will take critical hits.");   // Talk to the player.
			}

			inst.LuckyHits = !inst.LuckyHits;
			getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".CriticalHits",inst.LuckyHits );


			saveConfig();
			return true;
		}
		else if (args[0].equalsIgnoreCase("drops")) {
			SentryInstance inst = initializedSentries.get(ThisNPC.getId());
			if (inst.DestroyInventory) {
				player.sendMessage(ChatColor.GREEN + "Sentry will drop items");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + "Sentry will not drop items.");   // Talk to the player.
			}

			inst.DestroyInventory = !inst.DestroyInventory;
			getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".DestroyInventory",inst.DestroyInventory );


			saveConfig();
			return true;
		}


		else if (args[0].equalsIgnoreCase("health")) {
			if (args[1].isEmpty()) {
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry health [1-20]   note: Typically players");
				player.sendMessage(ChatColor.GOLD + "  have 20 HPs when fully healed");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 100) {
					player.sendMessage(ChatColor.RED + "Woah! It seems your health for this Sentry is really high!");
				}

				getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".Health", HPs);

				player.sendMessage(ChatColor.GREEN + "Sentry health set to " + Integer.valueOf(args[1]) + ".");   // Talk to the player.
				initializedSentries.get(ThisNPC.getId()).sentryHealth = HPs;
				saveConfig();
				return true;
			}

			return true;
		}


		else if (args[0].equalsIgnoreCase("speed")) {
			if (args[1].isEmpty()) {
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry speed [0.0 - 1.5]");
			}
			else {
				if (Double.valueOf(args[1]) <= 1.5 && Double.valueOf(args[1]) >= 0.0) { 

					if (Double.valueOf(args[1]) > 1.49) {
						player.sendMessage(ChatColor.GOLD + "Caution! Speeds this high can get weird!");
						player.sendMessage(ChatColor.GOLD + "It's best that the Sentry be on flat-ish terrain");
						player.sendMessage(ChatColor.GOLD + "when using unusually high speeds.");
					}

					getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".Speed", Double.valueOf(args[1]));

					player.sendMessage(ChatColor.GREEN + "Sentry speed set to " + Double.valueOf(args[1]) + ".");   // Talk to the player.
					initializedSentries.get(ThisNPC.getId()).sentrySpeed = Double.valueOf(args[1]);
					saveConfig();
				}

				else player.sendMessage(ChatColor.RED + "Invalid speed. Use a number between 0.0 and 1.5");

			}

			return true;
		}


		else if (args[0].equalsIgnoreCase("target")) {
			
			if (args.length<2 ){
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target add [ENTITY:Name, PLAYER:Name, GROUP:Name, ENTITY:MONSTER]");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target remove [ENTITY:Name, PLAYER:Name, GROUP:Name, ENTITY:MONSTER]");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target clear");
				return true;
			}

			else {

				if (args[1].equals("add") && args.length > 2) {

					List<String> currentList = getConfig().getStringList(ThisNPC.getName() + "." + ThisNPC.getId() + ".Targets");
					currentList.add(args[2].toUpperCase());
					getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".Targets", currentList);
					saveConfig();
					player.sendMessage(ChatColor.GREEN + "Target added. Now targeting " + currentList.toString());

					initializedSentries.get(ThisNPC.getId()).validTargets.add(args[2].toUpperCase());

					return true;
				}

				else if (args[1].equals("remove") && args.length > 2) {

					List<String> currentList = getConfig().getStringList(ThisNPC.getName() + "." + ThisNPC.getId() + ".Targets");
					if (currentList.contains(args[2].toLowerCase())) currentList.remove(args[2].toLowerCase());
					getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".Targets", currentList);
					saveConfig();
					player.sendMessage(ChatColor.GREEN + "Target removed. Now targeting " + currentList.toString());

					initializedSentries.get(ThisNPC.getId()).validTargets.remove(args[2].toUpperCase());

					return true;
				}

				else if (args[1].equals("clear")) {

					List<String> currentList = getConfig().getStringList(ThisNPC.getName() + "." + ThisNPC.getId() + ".Targets");
					currentList.clear();
					getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".Targets", currentList);
					saveConfig();
					player.sendMessage(ChatColor.GREEN + "Targets cleared. Now targeting " + currentList.toString());
					return true;
				}

				else {

					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target add [ENTITY:Name, PLAYER:Name, GROUP:Name, ENTITY:MONSTER]");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target remove [ENTITY:Name, PLAYER:Name, GROUP:Name, ENTITY:MONSTER]");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target clear");
					return true;
				}
			}
		}

		return true;
	}




	// End of CLASS

}
