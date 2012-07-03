package net.aufdemrand.sentry;

import java.util.ArrayList;
import java.util.List;

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
	private List<String> validTargets = new ArrayList<String>();
	private Integer sentryRange = 10;
	private List<Location> guardPosts = new ArrayList<Location>();

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

		if (theSentry == null) {

		}



		else {
			// Do nothing?
		}



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
