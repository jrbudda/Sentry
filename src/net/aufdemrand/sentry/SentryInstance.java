package net.aufdemrand.sentry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.trait.CurrentLocation;
import net.citizensnpcs.trait.waypoint.Waypoints;
import net.citizensnpcs.api.npc.NPC;
import net.minecraft.server.EntityLiving;


public class SentryInstance implements Listener {

	/* plugin Constructer */
	Sentry plugin;

	public SentryInstance(Sentry plugin) { 
		this.plugin = plugin;
		isRespawnable = System.currentTimeMillis();
	}

	/* Technicals */
	private Integer taskID = null;
	private enum Status { isDEAD, isHOSTILE, isLOOKING, isDYING, isSTUCK }
	private Long isRespawnable = System.currentTimeMillis();
	private LivingEntity projectileTarget;
	/* Internals */
	private Status sentryStatus = Status.isDYING;
	private NPC myNPC = null;

	/* Setables */
	public SentryTrait myTrait; 
	public List<String> validTargets = new ArrayList<String>();
	public Integer sentryRange = 10;
	public Integer sentryHealth = 20;
	public Double sentrySpeed = 1.0;
	public Double sentryWeight = 1.0;
	public Boolean LuckyHits = true;
	public Boolean Invincible = false;
	public Boolean Retaliate = true;
	public Boolean DestroyInventory = true;
	public List<Location> guardPosts = new ArrayList<Location>();
	public Integer RespawnDelaySeconds = 10;


	public boolean containsTarget(String theTarget) {
		if (validTargets.contains(theTarget)) return true;
		else return false;
	}


	private void faceEntity(Entity from, Entity at) {
		if (from.getWorld() != at.getWorld())
			return;
		Location loc = from.getLocation();

		double xDiff = at.getLocation().getX() - loc.getX();
		double yDiff = at.getLocation().getY() - loc.getY();
		double zDiff = at.getLocation().getZ() - loc.getZ();

		double distanceXZ = Math.sqrt(xDiff * xDiff + zDiff * zDiff);
		double distanceY = Math.sqrt(distanceXZ * distanceXZ + yDiff * yDiff);

		double yaw = (Math.acos(xDiff / distanceXZ) * 180 / Math.PI);
		double pitch = (Math.acos(yDiff / distanceY) * 180 / Math.PI) - 90;
		if (zDiff < 0.0) {
			yaw = yaw + (Math.abs(180 - yaw) * 2);
		}

		EntityLiving handle = ((CraftLivingEntity) from).getHandle();
		handle.yaw = (float) yaw - 90;
		handle.pitch = (float) pitch;
		handle.as = handle.yaw;
	}



	public void Fire(LivingEntity theEntity){

		faceEntity(myNPC.getBukkitEntity(), theEntity);		

		Projectile theArrow =myNPC.getBukkitEntity().launchProjectile(myProjectile);
		//	theArrow.setShooter(myNPC.getBukkitEntity());
		Vector victor = new Vector();

		victor = theEntity.getLocation().subtract( myNPC.getBukkitEntity().getLocation()).toVector();

		Double dist =  Math.sqrt(Math.pow(victor.getX(), 2) + Math.pow(victor.getZ(), 2));

		if(dist == 0) return;

		Double elev = victor.getY();

		Double v = 24.0; //1/Math.cos(launchAngle) * Math.sqrt((8.5*Math.pow(dist, 2))/(dist*Math.tan(launchAngle) + elev));
		Double g = 17.0;
		//aim  above target

		double v2 = Math.pow(v,2);
		double v4 = Math.pow(v,4);
		double derp =  g*(g*Math.pow(dist,2)+2*elev*v2);

		if( v4 < derp) {
			//target unreachable
			this.projectileTarget = null;
			return;
		}


		Double launchAngle = Math.atan( (v2-   Math.sqrt(v4 - derp))/(g*dist));
		//Double launchAngle =  Math.PI/4 ;


		//Apply angle 
		victor.setY(Math.tan(launchAngle)* dist);

		double mag = Math.sqrt(Math.pow(victor.getX(), 2) + Math.pow(victor.getY(), 2)  + Math.pow(victor.getZ(), 2)) ;

		//normalize the vector
		victor.multiply(1/mag);

		//calulate launch velocity

		//	Double v = Math.sqrt(dist*18);

	//	plugin.getServer().broadcastMessage(elev.toString() + ":" +dist.toString());			
		//plugin.getServer().broadcastMessage(launchAngle.toString() + ":" +v.toString());	

		//apply power
		victor.multiply(v/18);

		theArrow.setVelocity(victor);
	//	plugin.getServer().broadcastMessage(victor.toString());	
	}


	private Class<?extends Projectile> myProjectile ;

	public void setTarget(LivingEntity theEntity) {

		if (!myNPC.isSpawned()) return;
		
		Material weapon = Material.AIR;

				
		if (myNPC.getBukkitEntity() instanceof HumanEntity) {
			weapon = ((HumanEntity) myNPC.getBukkitEntity()).getInventory().getItemInHand().getType();
		}
		
		switch (weapon){
		case BOW:
			myProjectile = org.bukkit.entity.Arrow.class;

			if (theEntity != null){
				projectileTarget = theEntity;
			}
			break;
		case BLAZE_ROD: 
			myProjectile = org.bukkit.entity.SmallFireball.class;

			if (theEntity != null){
				projectileTarget = theEntity;
			}
			break;
		case SNOW_BALL:
			myProjectile = org.bukkit.entity.Snowball.class;

			if (theEntity != null){
				projectileTarget = theEntity;
			}
			break;
		case EGG:
			myProjectile = org.bukkit.entity.Egg.class;

			if (theEntity != null){
				projectileTarget = theEntity;
			}

			break;
		case POTION:
			myProjectile = org.bukkit.entity.ThrownPotion.class;

			if (theEntity != null){
				projectileTarget = theEntity;
			}

			break;
		case RAW_FISH:
			myProjectile = org.bukkit.entity.Fish.class;

			if (theEntity != null){
				projectileTarget = theEntity;
			}

			break;

		default:
			//Manual Attack
			projectileTarget = null;
			if (theEntity ==null){
				//		myNPC.getNavigator().cancelNavigation();	
				myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(false);
			}
			else
			{
				myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);
				myNPC.getNavigator().setTarget(theEntity, true);			
			}
			break;

		}

	}

	public LivingEntity getTarget() {
		if(myNPC.getNavigator().getEntityTarget() == null) return null;
		return myNPC.getNavigator().getEntityTarget().getTarget();
	}

	public void initialize(NPC npc) {

	//	plugin.getServer().broadcastMessage("NPC " + npc.getName() + " INITIALIZING!");

		this.myNPC = npc;

		String config = myNPC.getName() + "." + myNPC.getId();

		/* Read locations */
		if (plugin.getConfig().contains(config + ".List Locations")) {
			List<String> guardLocationList = plugin.getConfig().getStringList(config + ".List Locations");
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
		if (plugin.getConfig().contains(config + ".Targets")) 
			validTargets.addAll(plugin.getConfig().getStringList(config + ".Targets"));

		/* Read Stats */
		if (plugin.getConfig().contains(config + ".Health")) 
			sentryHealth = plugin.getConfig().getInt(config + ".Health");

		if (plugin.getConfig().contains(config + ".Health")) 
			sentryHealth = plugin.getConfig().getInt(config + ".Health");

		if (plugin.getConfig().contains(config + ".Range")) 
			sentryRange = plugin.getConfig().getInt(config + ".Range");

		//	if (plugin.getConfig().contains(config + ".Effect")) 
		//	sentryRange = plugin.getConfig().getInt(config + ".Effect");

		if (plugin.getConfig().contains(config + ".Invincible")) 
			Invincible = plugin.getConfig().getBoolean(config + ".Invincible");


		if (plugin.getConfig().contains(config + ".DestroyInventory")) 
			DestroyInventory = plugin.getConfig().getBoolean(config + ".DestroyInventory");
		
		if (plugin.getConfig().contains(config + ".Retaliate")) 
			Retaliate = plugin.getConfig().getBoolean(config + ".Retaliate");

		if (plugin.getConfig().contains(config + ".CriticalHits")) 
			LuckyHits = plugin.getConfig().getBoolean(config + ".CriticalHits");

		this.myNPC.getBukkitEntity().setHealth(sentryHealth);

		sentryStatus=Status.isLOOKING;

	//	plugin.getServer().broadcastMessage("NPC GUARDING!");

		if(taskID==null) {
			taskID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new SentryLogicRunnable(),5,40);			
		}
	}

	private class SentryLogicRunnable implements Runnable {
		@Override
		public void run() { 



			if (sentryStatus == Status.isDEAD &&  System.currentTimeMillis() > isRespawnable) {
				// Respawn
				myNPC.spawn(myNPC.getTrait(CurrentLocation.class).getLocation());
			}

			else if (sentryStatus == Status.isHOSTILE && myNPC.isSpawned()) {			

				if (projectileTarget != null){

					if( !projectileTarget.isDead()){
						Fire(projectileTarget);
						return;	
					}

				}

				if (getTarget() != null) {
					// Did it get away?
					if (getTarget().getLocation().distance(myNPC.getBukkitEntity().getLocation()) > sentryRange){
						setTarget(null);
					//	plugin.getServer().broadcastMessage("it got away!");
						sentryStatus = Status.isLOOKING;
					}
				}

				else  {
					//Target dead?
					//			if (!guardPosts.isEmpty())
					//			if (myNPC.getBukkitEntity().getLocation().distance(guardPosts.get(0)) > 16) 
					//			myNPC.getNavigator().setTarget(guardPosts.get(0));
				//	plugin.getServer().broadcastMessage("Target null");
					setTarget(null);
					sentryStatus = Status.isLOOKING;
				}
			}

			else if (sentryStatus == Status.isLOOKING && myNPC.isSpawned()) {
				LivingEntity target = findTarget( sentryRange);
				if (target !=null){
					sentryStatus = Status.isHOSTILE;
				//	plugin.getServer().broadcastMessage("Target selected: " + target.toString());	
					setTarget(target);
				}


			}

		}


	}


	public void deactivate() {
		plugin.getServer().getScheduler().cancelTask(taskID);
	}


	@EventHandler
	public void onRightClick(NPCRightClickEvent  event) {

	}


	@EventHandler
	public void onDamage(NPCDamageByEntityEvent  event) {

		if (!event.getNPC().hasTrait(SentryTrait.class)) return;
		
		if (event.getNPC() != myNPC){
			//what?
			//plugin.getServer().broadcastMessage("Not ME!!!");
			myNPC = event.getNPC();
		}

		if (!myNPC.isSpawned()) {
	//\\how did youg get here?
			return;
		}

		NPC npc = event.getNPC();
		Player player = null;	

		if(Invincible) event.setDamage(0);

		int finaldamage = event.getDamage();

		if(event.getDamager() instanceof Player){
			player = (Player) event.getDamager();	

			if (this.Retaliate) {
				setTarget(player);
				sentryStatus = Status.isHOSTILE;
			}

			double damagemodifer = event.getDamage();

			if(LuckyHits && !Invincible){

				Random r = new Random();
				int luckeyhit = r.nextInt(100);

				//	if (damagemodifer == 1.0) luckeyhit += 30; //use a weapon, dummy

				if (luckeyhit < 5) {

					damagemodifer =  damagemodifer * 2.00;
					player.sendMessage(ChatColor.RED + "*** You maim " + myNPC.getName());
					npc.getBukkitEntity().playEffect(EntityEffect.HURT);
				}
				else if (luckeyhit < 15) {

					damagemodifer =  damagemodifer * 1.50;
					player.sendMessage(ChatColor.GOLD + "*** You dismember " + myNPC.getName());
					npc.getBukkitEntity().playEffect(EntityEffect.HURT);
				}
				else if (luckeyhit < 25) {

					damagemodifer =  damagemodifer * 1.50;
					player.sendMessage(ChatColor.YELLOW + "*** You injure " + myNPC.getName());
					npc.getBukkitEntity().playEffect(EntityEffect.HURT);
				}
				else if (luckeyhit > 95) {

					damagemodifer =  0;
					player.sendMessage(ChatColor.GRAY + "*** You miss " + myNPC.getName());

				}

			}

			finaldamage = (int) Math.round(damagemodifer);


		}

		else if( event.getDamager() instanceof org.bukkit.entity.Projectile){
			org.bukkit.entity.Projectile theArroe = (org.bukkit.entity.Projectile)event.getDamager();
			if (theArroe.getShooter() instanceof Player) {
				player = (Player) theArroe.getShooter();	

				if (this.Retaliate) {
					setTarget(player);
					sentryStatus = Status.isHOSTILE;
				}

				finaldamage = event.getDamage();

			}

		}

		if (finaldamage>0){
			//play hurt effect
			npc.getBukkitEntity().playEffect(EntityEffect.HURT);

			if (player !=null){
				//knockback
				Vector newVec = player.getLocation().getDirection().multiply(1.75);
				newVec.setY(newVec.getY()/sentryWeight);
				npc.getBukkitEntity().setVelocity(newVec);
				player.sendMessage("You deal " + finaldamage + " damage to " + myNPC.getName());
			}

			//is he dead?
			if 	(npc.getBukkitEntity().getHealth() - finaldamage <= 0)  {
				npc.getBukkitEntity().getWorld().playEffect(npc.getBukkitEntity().getLocation(), Effect.POTION_BREAK, 3);
				npc.getBukkitEntity().getWorld().playEffect(npc.getBukkitEntity().getLocation(), Effect.POTION_BREAK, 3);
				npc.getBukkitEntity().getWorld().playEffect(npc.getBukkitEntity().getLocation(), Effect.POTION_BREAK, 3);
				npc.getBukkitEntity().getLocation().getWorld().spawn(npc.getBukkitEntity().getLocation(), ExperienceOrb.class).setExperience(5);
				//	finaldamage = npc.getBukkitEntity().getHealth();

				if (myNPC.getBukkitEntity() instanceof HumanEntity && this.DestroyInventory) {
					 ((HumanEntity) myNPC.getBukkitEntity()).getInventory().clear();
					 ((HumanEntity) myNPC.getBukkitEntity()).getInventory().setArmorContents(null);
				}
				
				sentryStatus = Status.isDEAD;
				isRespawnable = System.currentTimeMillis() + RespawnDelaySeconds*1000 ;
			}

			event.setDamage(0);
			myNPC.getBukkitEntity().damage(finaldamage);

		}
		else{
			event.setDamage(0);
		}

	}

	public LivingEntity findTarget ( Integer Range) {

		List<Entity> EntitiesWithinRange = myNPC.getBukkitEntity().getNearbyEntities(Range, Range, Range);
		LivingEntity theTarget = null;
		Double distanceToBeat = new Double(Range);

		//	plugin.getServer().broadcastMessage("Targets scanned : " + EntitiesWithinRange.toString());

		for (Entity aTarget : EntitiesWithinRange) {

			if (aTarget instanceof Player) {

				if (this.containsTarget("ENTITY:PLAYER")) {
					if (((Player) aTarget).getLocation().distance(myNPC.getBukkitEntity().getLocation()) < distanceToBeat) {
						distanceToBeat = aTarget.getLocation().distance(myNPC.getBukkitEntity().getLocation());
						theTarget = (LivingEntity) aTarget;
					}
				}

				else if (this.containsTarget("PLAYER:" + ((Player) aTarget).getName().toUpperCase())) {
					if (((Player) aTarget).getLocation().distance(myNPC.getBukkitEntity().getLocation()) < distanceToBeat) {
						distanceToBeat = aTarget.getLocation().distance(myNPC.getBukkitEntity().getLocation());

						theTarget = (LivingEntity) aTarget;
					}
				}

				else if (this.containsTarget("GROUP:")) {
					String[] groups = Sentry.perms.getPlayerGroups((Player) aTarget);
					for (int i = 0; i < groups.length; i++ ) {
						if (this.containsTarget("GROUP:" + groups[i].toLowerCase())) {
							if (((Player) aTarget).getLocation().distance(myNPC.getBukkitEntity().getLocation()) < distanceToBeat) {
								distanceToBeat = aTarget.getLocation().distance(myNPC.getBukkitEntity().getLocation());

								theTarget = (LivingEntity) aTarget;	
							}						
						}
					}
				}
			}

			else if (aTarget instanceof Monster) {
				if (this.containsTarget("ENTITY:MONSTER")) {
					if (((Monster) aTarget).getLocation()
							.distance(myNPC.getBukkitEntity().getLocation()) < distanceToBeat) {
						distanceToBeat = aTarget.getLocation().distance(myNPC.getBukkitEntity().getLocation());

						theTarget = (LivingEntity) aTarget;
					}
				}
			}

			else if (aTarget instanceof Creature) {
				if (this.containsTarget("ENTITY:" + ((Creature) aTarget).getType())) {
					if (((Creature) aTarget).getLocation()
							.distance(myNPC.getBukkitEntity().getLocation()) < distanceToBeat) {
						distanceToBeat = aTarget.getLocation().distance(myNPC.getBukkitEntity().getLocation());
						theTarget = (LivingEntity) aTarget;
					}
				}
			}
		}

		if (theTarget != null) 
		{
		//	plugin.getServer().broadcastMessage("Targeting: " + theTarget.toString());
			return theTarget;
		}



		return null;
	}



}


