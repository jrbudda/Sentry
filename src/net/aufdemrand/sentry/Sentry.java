package net.aufdemrand.sentry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.character.CharacterFactory;
import net.citizensnpcs.api.trait.trait.Owner;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creature;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class Sentry extends JavaPlugin {

	private Map<NPC, Entity> SentryTarget = new HashMap<NPC, Entity>();
	private Map<NPC, Location> SentryLocation = new HashMap<NPC, Location>();
	public Map<NPC, Long> RespawnSentry = new HashMap<NPC, Long>();
	public Map<NPC, Double> SentrySpeed = new HashMap<NPC, Double>();
	public Map<NPC, List<String>> SentryAllowedTargets = new HashMap<NPC, List<String>>();
	public Map<NPC, Integer> SentryHealth = new HashMap<NPC, Integer>();
	public Map<NPC, Location> LocationMonitor = new HashMap<NPC, Location>();
	public static Permission perms = null;
	public boolean debug;

	@Override
	public void onEnable() {

		debug = false;
		setupPermissions();
		CitizensAPI.getCharacterManager().registerCharacter(new CharacterFactory(SentryCharacter.class).withName("sentry"));

		//	getServer().getPluginManager().registerEvents(new SentryListener(this), this);

		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() { searchForTargets(); }
		}, 10, 10);


		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() { 

				/////////////////////////
				// Respawn Dead Guards
				///////////////////////

				if (!RespawnSentry.isEmpty()) {
					for (Entry<NPC, Long> theSentry : RespawnSentry.entrySet()) {
						if (System.currentTimeMillis() >= theSentry.getValue()) {
							theSentry.getKey().spawn(SentryLocation.get(theSentry.getKey()));			
							theSentry.getKey().getBukkitEntity().setHealth(SentryHealth.get(theSentry.getKey()));
							if (debug) getServer().broadcastMessage("Respawning dead Sentry: " + theSentry.getKey().getName());
						}
					}
				}


				/////////////////////////
				// Respawn Stuck Guards
				///////////////////////

				try {

					if (!LocationMonitor.isEmpty()) {
						for (Entry<NPC, Location> theSentry : LocationMonitor.entrySet()) {

							if (theSentry.getKey().getBukkitEntity().getLocation().distance(theSentry.getValue()) < 1 && 
									theSentry.getKey().getBukkitEntity().getLocation().distance(SentryLocation.get(theSentry.getKey())) > 3) {

								theSentry.getKey().getBukkitEntity().getWorld().playEffect(theSentry.getKey().getBukkitEntity().getLocation(), Effect.POTION_BREAK, 0);
								if (debug) getServer().broadcastMessage("Respawning stuck Sentry: " + theSentry.getKey().getName());
								theSentry.getKey().despawn();
								theSentry.getKey().spawn(SentryLocation.get(theSentry.getKey()));			
								theSentry.getKey().getBukkitEntity().setHealth(SentryHealth.get(theSentry.getKey()));
							}
						}
					}
				}
				catch (java.lang.NullPointerException e) {

				}

			}
		}, 350, 350);


		this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() { loadHashMaps(); }
		}, 60);




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


	public void loadHashMaps() {

		Collection<NPC> SentryNPCs = CitizensAPI.getNPCRegistry().getNPCs(SentryCharacter.class);
		if (SentryNPCs.isEmpty()) return;
		List<NPC> SentryList = new ArrayList<NPC>(SentryNPCs);

		for (NPC thisSentry : SentryList) {

			if (getConfig().contains(thisSentry.getName() + "." + thisSentry.getId() + ".Guarding Location")) {

				String[] theGuardLocation = getConfig().getString(thisSentry.getName() + "." + thisSentry.getId() + ".Guarding Location").split(";");

				Location thisLocation = new Location(Bukkit.getServer().getWorld(theGuardLocation[0]),
						Double.valueOf(theGuardLocation[1]),
						Double.valueOf(theGuardLocation[2]),
						Double.valueOf(theGuardLocation[3]));

				SentryLocation.put(thisSentry, thisLocation);

				if (!thisSentry.isSpawned()) thisSentry.spawn(thisLocation);

				thisSentry.getAI().setDestination(thisLocation);

			}

			if (getConfig().contains(thisSentry.getName() + "." + thisSentry.getId() + ".Health")) {
				thisSentry.getBukkitEntity().setHealth(getConfig().getInt(thisSentry.getName() + "." + thisSentry.getId() + ".Health"));
				SentryHealth.put(thisSentry, getConfig().getInt(thisSentry.getName() + "." + thisSentry.getId() + ".Health"));
			}



			if (getConfig().contains(thisSentry.getName() + "." + thisSentry.getId() + ".Speed")) {
				SentrySpeed.put(thisSentry, getConfig().getDouble(thisSentry.getName() + "." + thisSentry.getId() + ".Speed"));
			}

			else SentrySpeed.put(thisSentry, .2);



			if (getConfig().contains(thisSentry.getName() + "." + thisSentry.getId() + ".Targets")) {
				SentryAllowedTargets.put(thisSentry, getConfig().getStringList(thisSentry.getName() + "." + thisSentry.getId() + ".Targets"));
			}






		}

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
			loadHashMaps();
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
			ThisNPC.getAI().setDestination(player.getLocation());
			SentryLocation.put(ThisNPC, player.getLocation());

			getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".Guarding Location", 
					player.getWorld().getName() + ";" +
							player.getLocation().getX() + ";" +
							player.getLocation().getY() + ";" +
							player.getLocation().getZ());							

			saveConfig();
			loadHashMaps();

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
				SentryHealth.put(ThisNPC, getConfig().getInt(ThisNPC.getName() + "." + ThisNPC.getId() + ".Health"));
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
					SentrySpeed.put(ThisNPC, getConfig().getDouble(ThisNPC.getName() + "." + ThisNPC.getId() + ".Speed"));
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
					currentList.add(args[2].toLowerCase());
					getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".Targets", currentList);
					saveConfig();
					player.sendMessage(ChatColor.GREEN + "Target added. Now targeting " + currentList.toString());
					loadHashMaps();
					return true;
				}

				else if (args[1].equals("remove") && !args[2].isEmpty()) {

					List<String> currentList = getConfig().getStringList(ThisNPC.getName() + "." + ThisNPC.getId() + ".Targets");
					if (currentList.contains(args[2].toLowerCase())) currentList.remove(args[2].toLowerCase());
					getConfig().set(ThisNPC.getName() + "." + ThisNPC.getId() + ".Targets", currentList);
					saveConfig();
					player.sendMessage(ChatColor.GREEN + "Target removed. Now targeting " + currentList.toString());
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






	private void searchForTargets(NPC theSentry) {


		/////////////////////////
		// Done for each Sentry
		///////////////////////

		if (theSentry.isSpawned() && SentryLocation.containsKey(theSentry))	{

			LocationMonitor.put(theSentry, theSentry.getBukkitEntity().getLocation());

			if (theSentry.getBukkitEntity().getLocation().distance(SentryLocation.get(theSentry)) < 18) {

				//////////////
				// TARGETER
				////////////

				Entity theTarget = GetTarget(SentryLocation.get(theSentry), theSentry, 15);

				if (theTarget != null) {
					SentryTarget.put(theSentry, theTarget);
					//	LocationMonitor.remove(theSentry);

					if (debug) getServer().broadcastMessage("Target aquired: " + theTarget.getType().toString());

				}

				else if (theTarget == null) {
					SentryTarget.clear();

					if (debug) getServer().broadcastMessage("Targets cleared.");

				}

				if (theSentry.getBukkitEntity().getLocation().distance(SentryLocation.get(theSentry)) > 2) {
					theSentry.getAI().setDestination(SentryLocation.get(theSentry));

					Vector newVec = theSentry.getBukkitEntity().getLocation().getDirection().multiply(SentrySpeed.get(theSentry));
					//   newVec.setY(newVec.getY()/1.1);
					theSentry.getBukkitEntity().setVelocity(newVec);

				}

				if (SentryTarget.containsKey(theSentry) && SentryTarget.get(theSentry).getLocation().distance(SentryLocation.get(theSentry)) < 15) {   

					theSentry.getAI().setTarget((LivingEntity) SentryTarget.get(theSentry), true);
					Vector newVec = theSentry.getBukkitEntity().getLocation().getDirection().multiply(SentrySpeed.get(theSentry));
					// newVec.setY(newVec.getY()/1.1);
					theSentry.getBukkitEntity().setVelocity(newVec);
				}


			}

			else {
				theSentry.getAI().setDestination(SentryLocation.get(theSentry));


			}

		}


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
