package net.aufdemrand.sentry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.character.CharacterFactory;
import net.citizensnpcs.api.trait.trait.Owner;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Sentry extends JavaPlugin {

	public static Permission perms = null;
	public boolean debug = false;;
	public SentryCharacter interaction = new SentryCharacter();

	public Map<Integer, SentryInstance> initializedSentries = new HashMap<Integer, SentryInstance>();
	
	@Override
	public void onEnable() {

		setupPermissions();
		CitizensAPI.getCharacterManager().registerCharacter(new CharacterFactory(interaction.getClass()).withName("sentry"));
		getServer().getPluginManager().registerEvents(interaction, this);
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


		NPC ThisNPC = CitizensAPI.getNPCRegistry().getNPC(player.getMetadata("selected").get(0).asInt());      // Gets NPC Selected


		if (!ThisNPC.getTrait(Owner.class).getOwner().equalsIgnoreCase(player.getName())) {
			player.sendMessage(ChatColor.RED + "You must be the owner of the sentry to execute commands.");
			return true;
		}

		if (ThisNPC.getCharacter() == null || !ThisNPC.getCharacter().getName().equals("sentry")) {
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

					if (Double.valueOf(args[1]) > .49) {
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
			if (args[1].isEmpty()) {
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target add [Entity|Player|Group]");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target remove [Entity|Player|Group]");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target clear");
				return true;

			}
			else {

				if (args[1].equals("add") && !args[2].isEmpty()) {

					List<String> currentList = getConfig().getStringList(ThisNPC.getName() + "." + ThisNPC.getId() + ".Targets");
					currentList.add(args[2].toUpperCase());
					getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".Targets", currentList);
					saveConfig();
					player.sendMessage(ChatColor.GREEN + "Target added. Now targeting " + currentList.toString());
					
					initializedSentries.get(ThisNPC.getId()).validTargets.add(args[2].toUpperCase());
					
					return true;
				}

				else if (args[1].equals("remove") && !args[2].isEmpty()) {

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

					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target add [Entity|Player|Group]");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target remove [Entity|Player|Group]");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target clear");
					return true;
				}
			}
		}

		return true;
	}







	
	

	@SuppressWarnings("deprecation")
	public void findTarget (SentryInstance theSentry, Integer Range) {

		List<Entity> EntitiesWithinRange = theSentry.getSentry().getBukkitEntity().getNearbyEntities(Range, Range, Range);
		LivingEntity theTarget = null;
		Double distanceToBeat = new Double(Range);

		//	getServer().broadcastMessage("Targets scanned : " + EntitiesWithinRange.toString());

		try {
			for (Entity aTarget : EntitiesWithinRange) {

				if (aTarget instanceof Player) {

					if (theSentry.containsTarget("ENTITY:PLAYER")) {
						if (((Player) aTarget).getLocation()
								.distance(theSentry.getSentry()
										.getBukkitEntity().getLocation()) < distanceToBeat) {
							distanceToBeat = aTarget.getLocation().distance(theSentry.getSentry()
									.getBukkitEntity().getLocation());
							theTarget = (LivingEntity) aTarget;
						}
					}


					else if (theSentry.containsTarget("PLAYER:" + ((Player) aTarget).getName().toUpperCase())) {
						if (((Player) aTarget).getLocation()
								.distance(theSentry.getSentry()
										.getBukkitEntity().getLocation()) < distanceToBeat) {
							distanceToBeat = aTarget.getLocation().distance(theSentry.getSentry()
									.getBukkitEntity().getLocation());
							theTarget = (LivingEntity) aTarget;
						}
					}

					else if (theSentry.containsTarget("GROUP:")) {
						String[] groups = perms.getPlayerGroups((Player) aTarget);
						for (int i = 0; i < groups.length; i++ ) {
							if (theSentry.containsTarget("GROUP:" + groups[i].toLowerCase())) {
								if (((Player) aTarget).getLocation()
										.distance(theSentry.getSentry()
												.getBukkitEntity().getLocation()) < distanceToBeat) {
									distanceToBeat = aTarget.getLocation().distance(theSentry.getSentry()
											.getBukkitEntity().getLocation());
									theTarget = (LivingEntity) aTarget;	
								}						
							}
						}
					}
				}

				else if (aTarget instanceof Monster) {
					if (theSentry.containsTarget("ENTITY:MONSTER")) {
						if (((Monster) aTarget).getLocation()
								.distance(theSentry.getSentry()
										.getBukkitEntity().getLocation()) < distanceToBeat) {
							distanceToBeat = aTarget.getLocation().distance(theSentry.getSentry()
									.getBukkitEntity().getLocation());
							theTarget = (LivingEntity) aTarget;
						}
					}
				}

				else if (aTarget instanceof Creature) {
					if (theSentry.containsTarget("ENTITY:" + ((Creature) aTarget).getType())) {
						if (((Creature) aTarget).getLocation()
								.distance(theSentry.getSentry()
										.getBukkitEntity().getLocation()) < distanceToBeat) {
							distanceToBeat = aTarget.getLocation().distance(theSentry.getSentry()
									.getBukkitEntity().getLocation());
							theTarget = (LivingEntity) aTarget;
						}
					}
				}
			}
			
			if (theTarget != null) theSentry.setTarget(theTarget);
			
		} catch (java.lang.NullPointerException e) {
		
			
			
		}
		
		return;
	}

	
	

	// End of CLASS

}
