package net.aufdemrand.sentry;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;

import net.citizensnpcs.api.npc.NPC;

public class SentryInstance {

	/* plugin Constructer */
	Sentry plugin;
	public SentryInstance(Sentry plugin) { this.plugin = plugin; }

	/* Technicals */
	public SentryInstance thisInstance = this;
	private Integer taskID = null;
	private enum Status { isDEAD, isHOSTILE, isLOOKING, isDYING, isSTUCK }
	private Long isRespawnable = System.currentTimeMillis();

	/* Internals */
	private LivingEntity currentTarget = null;
	private Status sentryStatus = null;
	private NPC theSentry = null;

	/* Setables */
	public List<String> validTargets = new ArrayList<String>();
	private Integer sentryRange = 10;
	public Integer sentryHealth = 10;
	public Double sentrySpeed = 0.2;
	private boolean sentryIsAggressive = false;
	public List<Location> guardPosts = new ArrayList<Location>();

	/* Helper methods */
	public NPC getSentry() {
		return theSentry;
	}

	public boolean containsTarget(String theTarget) {
		if (validTargets.contains(theTarget)) return true;
		else return false;
	}

	public void setTarget(LivingEntity theEntity) {
		currentTarget = theEntity;
	}

	public LivingEntity getTarget() {
		return ((Creature) theSentry.getBukkitEntity()).getTarget();
	}





	public void initialize(NPC npc) {

		theSentry = npc;


		/* Read locations */
		if (plugin.getConfig().contains(theSentry.getName() + "." + theSentry.getId() + ".List Locations")) {
			List<String> guardLocationList = plugin.getConfig().getStringList(theSentry.getName() + "." + theSentry.getId() + ".List Locations");
			for (String locationString : guardLocationList) {
				String[] split = locationString.split(";");
				try {
					guardPosts.add(new Location(Bukkit.getServer().getWorld(split[0]),
							Double.valueOf(split[1]),
							Double.valueOf(split[2]),
							Double.valueOf(split[3])));
				} catch (Throwable e) { }
			}

		}


		/* Read targets */
		if (plugin.getConfig().contains(theSentry.getName() + "." + theSentry.getId() + ".Targets")) 
			validTargets.addAll(plugin.getConfig().getStringList(theSentry.getName() + "." + theSentry.getId() + ".Targets"));


		/* Read Stats */
		if (plugin.getConfig().contains(theSentry.getName() + "." + theSentry.getId() + ".Health")) 
			sentryHealth = plugin.getConfig().getInt(theSentry.getName() + "." + theSentry.getId() + ".Health");

		if (plugin.getConfig().contains(theSentry.getName() + "." + theSentry.getId() + ".Aggressive")) 
			sentryIsAggressive = plugin.getConfig().getBoolean(theSentry.getName() + "." + theSentry.getId() + ".Aggressive");

		if (plugin.getConfig().contains(theSentry.getName() + "." + theSentry.getId() + ".Health")) 
			sentryHealth = plugin.getConfig().getInt(theSentry.getName() + "." + theSentry.getId() + ".Health");

		if (plugin.getConfig().contains(theSentry.getName() + "." + theSentry.getId() + ".Range")) 
			sentryRange = plugin.getConfig().getInt(theSentry.getName() + "." + theSentry.getId() + ".Range");

		if (plugin.getConfig().contains(theSentry.getName() + "." + theSentry.getId() + ".Effect")) 
			sentryRange = plugin.getConfig().getInt(theSentry.getName() + "." + theSentry.getId() + ".Effect");


		((LivingEntity) theSentry).setHealth(sentryHealth);

		sentryStatus = Status.isLOOKING;

		guard();


	}





	public void guard() {
		taskID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {

			@Override
			public void run() { 
				if (sentryStatus == Status.isDEAD && isRespawnable > System.currentTimeMillis()) {
					// Respawn
				}

				else if (sentryStatus == Status.isHOSTILE && theSentry.isSpawned()) {			
					if (getTarget() != null) {

						if (getTarget() == currentTarget) {
							// Carry on.
						}

						else if (getTarget() != currentTarget) {
							if (currentTarget.getLocation().distance(theSentry.getBukkitEntity().getLocation()) < sentryRange) 
								theSentry.getAI().setTarget(currentTarget, true);
							else sentryStatus = Status.isLOOKING;
						}
					}

					else if (getTarget() == null) {

						if (!guardPosts.isEmpty())

							if (theSentry.getBukkitEntity().getLocation().distance(guardPosts.get(0)) > 16) 
								theSentry.getAI().setDestination(guardPosts.get(0));


						sentryStatus = Status.isLOOKING;
						currentTarget = null;
					}


				}

				else if (sentryStatus == Status.isLOOKING && theSentry.isSpawned()) {
					plugin.findTarget(thisInstance, sentryRange);
					if (currentTarget != null) sentryStatus = Status.isHOSTILE;
				}
			}

		}, 5, 5);
	}


	public void deactivate() {
		plugin.getServer().getScheduler().cancelTask(taskID);
	}

}
