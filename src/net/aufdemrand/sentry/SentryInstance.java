package net.aufdemrand.sentry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import net.citizensnpcs.api.event.NPCLeftClickEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;

import net.citizensnpcs.npc.entity.CitizensHumanNPC;
import net.citizensnpcs.npc.entity.EntityHumanNPC;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.GoalController;
import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.util.DataKey;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityLiving;

public class SentryInstance implements Listener {

	/* plugin Constructer */
	Sentry plugin;

	public SentryInstance(Sentry plugin) { 
		this.plugin = plugin;
		isRespawnable = System.currentTimeMillis();

	}

	/* Technicals */
	public SentryInstance thisInstance = this;
	private Integer taskID = null;
	private enum Status { isDEAD, isHOSTILE, isLOOKING, isDYING, isSTUCK }
	private Long isRespawnable = System.currentTimeMillis();

	/* Internals */
	private LivingEntity currentTarget = null;
	public Status sentryStatus = Status.isDYING;
	private NPC theSentry = null;

	/* Setables */
	public List<String> validTargets = new ArrayList<String>();
	private Integer sentryRange = 10;
	public Integer sentryHealth = 20;
	public Double sentrySpeed = 1.0;
	public Double sentryWeight = 1.0;
	private boolean sentryIsAggressive = false;
	public Boolean LuckyHits = true;
	public Boolean Invincible = false;
	public List<Location> guardPosts = new ArrayList<Location>();
	public Integer RespawnDelaySeconds = 10;

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
		return ((org.bukkit.entity.NPC) theSentry.getBukkitEntity()).getTarget();
	}



	public void initialize(NPC npc) {

		plugin.getServer().broadcastMessage("NPC " + npc.getName() + " INITIALIZING!");

		this.theSentry = npc;

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


		plugin.getServer().broadcastMessage("SETTING HEALTH TO " + sentryHealth + "!");


		this.theSentry.getBukkitEntity().setHealth(sentryHealth);

		plugin.getServer().broadcastMessage("HEALTH SET TO " + theSentry.getBukkitEntity().getHealth() + "!");


		sentryStatus=Status.isSTUCK;
		
		guard();


	}

	
	
private class derp implements Runnable {
	@Override
	public void run() { 
		
		plugin.getServer().broadcastMessage("Sentry Run: " + sentryStatus);

		sentryStatus=Status.isDEAD;
		
		if (sentryStatus == Status.isDEAD &&  System.currentTimeMillis() > isRespawnable) {
			// Respawn
			theSentry.spawn(theSentry.getBukkitEntity().getLocation());
		}

		else if (sentryStatus == Status.isHOSTILE && theSentry.isSpawned()) {			
			if (getTarget() != null) {

				if (getTarget() == currentTarget) {
					// Carry on.
				}

				else if (getTarget() != currentTarget) {
					if (currentTarget.getLocation().distance(theSentry.getBukkitEntity().getLocation()) < sentryRange){
						theSentry.getNavigator().setTarget(currentTarget, true);

					}

					//theSentry.getAI().setTarget(currentTarget, true);
					else sentryStatus = Status.isLOOKING;
				}
			}

			else if (getTarget() == null) {

				if (!guardPosts.isEmpty())

					if (theSentry.getBukkitEntity().getLocation().distance(guardPosts.get(0)) > 16) 
						theSentry.getNavigator().setTarget(guardPosts.get(0));
				//theSentry.getAI().setDestination(guardPosts.get(0));


			//	sentryStatus = Status.isLOOKING;
				currentTarget = null;
			}


		}

		else if (sentryStatus == Status.isLOOKING && theSentry.isSpawned()) {
			plugin.findTarget(thisInstance, sentryRange);
			if (currentTarget != null) sentryStatus = Status.isHOSTILE;
		}
		
			plugin.getServer().broadcastMessage("Sentry Run: " + sentryStatus + theSentry.getFullName());
	}

	

	
	
	}

	


	public void guard() {

		plugin.getServer().broadcastMessage("NPC GUARDING!");
							
	
		taskID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new derp(),5,40);
		
	}


	public void deactivate() {
		plugin.getServer().getScheduler().cancelTask(taskID);
	}


	@EventHandler
	public void onRightClick(NPCRightClickEvent  event) {

	}


	@EventHandler
	public void onLeftClick(NPCLeftClickEvent  event) {

		Player player = event.getClicker();
		NPC npc = event.getNPC();

		plugin.getServer().broadcastMessage("Sentry Run: " + sentryStatus );
		
		 SentryInstance.this.sentryStatus = Status.isSTUCK;

		 
			plugin.getServer().broadcastMessage("Sentry Run: " + SentryInstance.this.sentryStatus + npc.getFullName());
		 
		ItemStack iteminhand = player.getItemInHand();

		double damagemodifer = 1.0;

		if (iteminhand.getType().equals(Material.WOOD_SWORD)) damagemodifer =  1.25;
		if (iteminhand.getType().equals(Material.IRON_SWORD)) damagemodifer =  1.50;
		if (iteminhand.getType().equals(Material.GOLD_SWORD)) damagemodifer =  1.75;
		if (iteminhand.getType().equals(Material.DIAMOND_SWORD)) damagemodifer =  2.0;

		if(Invincible) damagemodifer =0.0;

		if(LuckyHits && !Invincible){

			Random r = new Random();
			int luckeyhit = r.nextInt(100);

			if (damagemodifer == 1.0) luckeyhit += 50; //use a weapon, dummy

			if (luckeyhit < 5) {

				damagemodifer =  damagemodifer * 2.00;
				player.sendMessage(ChatColor.RED + "*** You maim the Sentry!");
				npc.getBukkitEntity().playEffect(EntityEffect.HURT);
			}
			else if (luckeyhit < 15) {

				damagemodifer =  damagemodifer * 1.50;
				player.sendMessage(ChatColor.GOLD + "*** You dismember the Sentry!");
				npc.getBukkitEntity().playEffect(EntityEffect.HURT);
			}
			else if (luckeyhit < 25) {

				damagemodifer =  damagemodifer * 1.50;
				player.sendMessage(ChatColor.YELLOW + "*** You injure the Sentry!");
				npc.getBukkitEntity().playEffect(EntityEffect.HURT);
			}
			else if (luckeyhit > 95) {

				damagemodifer =  0;
				player.sendMessage(ChatColor.GRAY + "*** You miss the Sentry!");

			}

			else npc.getBukkitEntity().playEffect(EntityEffect.HURT);
		}

		if (damagemodifer > 0) {
			Vector newVec = player.getLocation().getDirection().multiply(1.75);
			newVec.setY(newVec.getY()/sentryWeight);
			npc.getBukkitEntity().setVelocity(newVec);
		}



		int finaldamage = (int) Math.round(damagemodifer);


		if 	(npc.getBukkitEntity().getHealth() - finaldamage <= 0)  {
			isRespawnable = System.currentTimeMillis() + RespawnDelaySeconds*1000 ;
			npc.getBukkitEntity().getWorld().playEffect(npc.getBukkitEntity().getLocation(), Effect.POTION_BREAK, 3);
			npc.getBukkitEntity().getWorld().playEffect(npc.getBukkitEntity().getLocation(), Effect.POTION_BREAK, 3);
			npc.getBukkitEntity().getWorld().playEffect(npc.getBukkitEntity().getLocation(), Effect.POTION_BREAK, 3);
			player.sendMessage(ChatColor.GREEN + "*** You lay a mortal blow to the Sentry!");
			npc.getBukkitEntity().getLocation().getWorld().spawn(npc.getBukkitEntity().getLocation(), ExperienceOrb.class).setExperience(5);

			finaldamage = npc.getBukkitEntity().getHealth();
			npc.getBukkitEntity().damage(finaldamage);
	
		}

		else npc.getBukkitEntity().damage(finaldamage);
		// npc.getBukkitEntity().damage(finaldamage, player);


	}



}


