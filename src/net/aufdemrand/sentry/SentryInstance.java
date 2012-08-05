package net.aufdemrand.sentry;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
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


public class SentryInstance  {

	/* plugin Constructer */
	Sentry plugin;

	public SentryInstance(Sentry plugin) { 
		this.plugin = plugin;
		isRespawnable = System.currentTimeMillis();
	}

	/* Technicals */
	private Integer taskID = null;
	public enum Status { isDEAD, isHOSTILE, isLOOKING, isDYING, isSTUCK }
	private Long isRespawnable = System.currentTimeMillis();
	private long oktoFire = System.currentTimeMillis();
	private LivingEntity projectileTarget;
	/* Internals */
	Status sentryStatus = Status.isDYING;
	public  NPC myNPC = null;

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
	public Integer RespawnDelaySeconds = 10;
	public  Integer Armor = 0;
	public Integer Strength = 0;
	public double AttackRateSeconds = 2.0;

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

	private void faceForward() {
		EntityLiving handle = ((CraftLivingEntity) this.myNPC.getBukkitEntity()).getHandle();
		handle.as = handle.yaw;
		handle.pitch = 0;
	}

	public void Fire(LivingEntity theEntity){

		double v  = 27;
		double g = 20;

		Effect effect = null;

		if (myProjectile == Arrow.class)
		{
			effect = Effect.BOW_FIRE;
		}
		else if (myProjectile == SmallFireball.class)
		{
			effect = Effect.BLAZE_SHOOT;
			v = 5000.0; 
			g = .01;
		}
		else  {
			v = 17.75; 
			g = 12.5;

		}

		//calc shooting spot.
		Location loco =  myNPC.getBukkitEntity().getEyeLocation();
		Vector norman = theEntity.getEyeLocation().subtract(loco).toVector();
		double mag = Math.sqrt(Math.pow(norman.getX(), 2) + Math.pow(norman.getY(), 2)  + Math.pow(norman.getZ(), 2)) ;
		norman.multiply(1/mag);
		Location loc =loco.add(norman);

		//Calc range		
		Vector victor = theEntity.getEyeLocation().subtract(loc).toVector();
		mag = Math.sqrt(Math.pow(victor.getX(), 2) + Math.pow(victor.getY(), 2)  + Math.pow(victor.getZ(), 2)) ;
		Double dist =  Math.sqrt(Math.pow(victor.getX(), 2) + Math.pow(victor.getZ(), 2));

		if(dist == 0) return;

		Double elev = victor.getY();

		double v2 = Math.pow(v,2);
		double v4 = Math.pow(v,4);
		double derp =  g*(g*Math.pow(dist,2)+2*elev*v2);

		Double launchAngle;


		//Check unhittable.
		if( v4 < derp) {
			//target unreachable

			// use this to fire at optimal max angle launchAngle = Math.atan( ( 2*g*elev + v2) / (2*g*elev + 2*v2));

			setTarget(null);
			return;
		}
		else {
			//calc angle
			launchAngle = Math.atan( (v2-   Math.sqrt(v4 - derp))/(g*dist));

		}


		boolean LOS = (((CraftLivingEntity) myNPC.getBukkitEntity()).getHandle()).l(((CraftLivingEntity)theEntity).getHandle()); 

		if(!LOS) {
			//target cant be seen..
			setTarget(null);
			return;
		}



		//OK we're shooting
		//go twang
		if (effect!=null)	myNPC.getBukkitEntity().getWorld().playEffect(myNPC.getBukkitEntity().getLocation(), effect, null);
		//look at target
		faceEntity(myNPC.getBukkitEntity(), theEntity);		

		//Apply angle 
		victor.setY(Math.tan(launchAngle)* dist);

		//normalize vector
		victor.multiply(1/mag);

		//apply power
		victor.multiply(v/20);

		if (myProjectile == SmallFireball.class){
			//this dont do nuffin
			victor.multiply(1/20);
		}


		//Shoot!
		//	Projectile theArrow =myNPC.getBukkitEntity().launchProjectile(myProjectile);

		Projectile	theArrow = myNPC.getBukkitEntity().getWorld().spawn(loc,myProjectile);
		theArrow.setShooter(myNPC.getBukkitEntity());
		theArrow.setVelocity(victor);

	}


	public LivingEntity getTarget() {
		if(myNPC.getNavigator().getEntityTarget() == null) return null;
		return myNPC.getNavigator().getEntityTarget().getTarget();
	}

	private Class<?extends Projectile> myProjectile ;

	public void setTarget(LivingEntity theEntity) {

		if (!myNPC.isSpawned()) return;

		if (theEntity ==null){
			if (myNPC.hasTrait(Waypoints.class)){
				if (myNPC.getTrait(Waypoints.class).getCurrentProvider() != null){
					myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(false);	
				}
			}


			sentryStatus = Status.isLOOKING;
			projectileTarget = null;	
			faceForward();
			return;
		}

		Material weapon = Material.AIR;

		faceEntity(myNPC.getBukkitEntity(), theEntity);		

		if (myNPC.getBukkitEntity() instanceof HumanEntity) {
			weapon = ((HumanEntity) myNPC.getBukkitEntity()).getInventory().getItemInHand().getType();
		}

		switch (weapon){
		case BOW:
			myProjectile = org.bukkit.entity.Arrow.class;
			projectileTarget = theEntity;
			break;
		case BLAZE_ROD: 
			myProjectile = org.bukkit.entity.SmallFireball.class;
			projectileTarget = theEntity;
			break;
		case SNOW_BALL:
			myProjectile = org.bukkit.entity.Snowball.class;
			projectileTarget = theEntity;
			break;
		case EGG:
			myProjectile = org.bukkit.entity.Egg.class;
			projectileTarget = theEntity;
			break;
		case POTION:
			myProjectile = org.bukkit.entity.ThrownPotion.class;
			projectileTarget = theEntity;

			break;

		default:
			//Manual Attack
			projectileTarget = null;

			myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);
			myNPC.getNavigator().setTarget(theEntity, true);			

			break;

		}

	}



	public void initialize() {

		//	plugin.getServer().broadcastMessage("NPC " + npc.getName() + " INITIALIZING!");


		//check for illegal values

		if (sentryWeight <=0) sentryWeight = 1.0;
		if (AttackRateSeconds > 30) AttackRateSeconds = 30.0;

		if (sentryHealth<0) sentryHealth =0;

		if (sentryRange <1) sentryRange = 1;
		if (sentryRange >100) sentryRange = 100;

		if (sentryWeight <=0) sentryWeight = 1.0;

		if (RespawnDelaySeconds < 1 ) RespawnDelaySeconds =1;

		this.myNPC.getBukkitEntity().setHealth(sentryHealth);


		this.setTarget(null);

		//	plugin.getServer().broadcastMessage("NPC GUARDING!");

		if(taskID==null) {
			taskID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new SentryLogicRunnable(),20,10);			
		}

	}

	private class SentryLogicRunnable implements Runnable {
		@Override
		public void run() { 

			//	plugin.getServer().broadcastMessage("tick " + sentryStatus);

			if (sentryStatus == Status.isDEAD &&  System.currentTimeMillis() > isRespawnable) {
				// Respawn

				//		Location loc = myNPC.getTrait(CurrentLocation.class).getLocation();
				//	if (myNPC.hasTrait(Waypoints.class)){
				//Waypoints wp = myNPC.getTrait(Waypoints.class);
				//	wp.getCurrentProvider()
				//	}

				//	plugin.getServer().broadcastMessage("Spawning...");
				myNPC.spawn(myNPC.getTrait(CurrentLocation.class).getLocation());
			}

			else if (sentryStatus == Status.isHOSTILE && myNPC.isSpawned()) {			

				if (projectileTarget != null){
					//have a projectile target
					if( !projectileTarget.isDead()){
						if (System.currentTimeMillis() > oktoFire) {
							//Fire!
							Fire(projectileTarget);
							oktoFire = (long) (System.currentTimeMillis() + AttackRateSeconds*1000);
						}
						return;	//keep at it
					}
					//is dead
					setTarget(null);
				}

				else if (getTarget() != null) {
					// Did it get away?
					if (getTarget().getLocation().distance(myNPC.getBukkitEntity().getLocation()) > sentryRange){
						//it got away...
						setTarget(null);
					}
				}

				else  {
					//melee target died
					setTarget(null);
				}
			}

			else if (sentryStatus == Status.isLOOKING && myNPC.isSpawned()) {
				LivingEntity target = findTarget( sentryRange);
				if (target !=null){
					sentryStatus = Status.isHOSTILE;
					//	plugin.getServer().broadcastMessage("Target selected: " + target.toString());	
					setTarget(target);
				}
				else setTarget(null);


			}

		}


	}


	public void deactivate() {
		plugin.getServer().getScheduler().cancelTask(taskID);
	}


	@EventHandler
	public void onRightClick(NPCRightClickEvent  event) {

	}


	public enum hittype {normal, miss, block,  injure, main, disembowel}



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
		LivingEntity player = null;	

		hittype hit = hittype.normal;


		int finaldamage = event.getDamage();


		//Find the attacker
		if( event.getDamager() instanceof Projectile){
			if(((Projectile) event.getDamager()).getShooter() instanceof LivingEntity){
				player =((Projectile) event.getDamager()).getShooter();
			}	
		}
		else if ( event.getDamager() instanceof LivingEntity)
		{
			player = (LivingEntity) event.getDamager();	
		}

		//can i kill it? lets go kill it.
		if (player != null){
			if (this.Retaliate) {
				setTarget(player);
				sentryStatus = Status.isHOSTILE;
			}					
		}

		if(Invincible) return;

		if(LuckyHits){
			//Calulate crits
			double damagemodifer = event.getDamage();

			Random r = new Random();
			int luckeyhit = r.nextInt(100);

			//	if (damagemodifer == 1.0) luckeyhit += 30; //use a weapon, dummy

			if (luckeyhit < 5) {
				damagemodifer =  damagemodifer * 2.00;
				hit = hittype.disembowel;
			}
			else if (luckeyhit < 15) {

				damagemodifer =  damagemodifer * 1.75;
				hit = hittype.main;
			}
			else if (luckeyhit < 25) {
				damagemodifer =  damagemodifer * 1.50;
				hit = hittype.injure;
			}
			else if (luckeyhit > 95) {

				damagemodifer =  0;
				hit = hittype.miss;

			}

			finaldamage = (int) Math.round(damagemodifer);
		}



		if (finaldamage > 0) {

			if (player !=null){
				//knockback
				Vector newVec = player.getLocation().getDirection().multiply(1.75);
				newVec.setY(newVec.getY()/sentryWeight);
				npc.getBukkitEntity().setVelocity(newVec);
			}

			//Apply armor
			finaldamage -= this.Armor;


			//there was damamge before armor.
			if (finaldamage<= 0 && LuckyHits)	hit = hittype.block; 

		}


		if (player instanceof Player) {
			//Messages
			switch(hit){
			case normal:
				((Player)player).sendMessage(ChatColor.WHITE  + "*** You hit " + myNPC.getName() + " for " + finaldamage +" damage");
				break;
			case miss:
				((Player)player).sendMessage(ChatColor.GRAY + "*** You miss " + myNPC.getName());
				break;
			case block:
				((Player)player).sendMessage(ChatColor.GRAY + "*** Your blow glances off " + myNPC.getName() + "'s armor");
				break;
			case main:
				((Player)player).sendMessage(ChatColor.GOLD  + "*** You MAIM " + myNPC.getName() + " for " + finaldamage +" damage");
				break;
			case disembowel:
				((Player)player).sendMessage(ChatColor.RED  + "*** You DISEMBOWEL " + myNPC.getName() + " for " + finaldamage +" damage");
				break;
			case injure:
				((Player)player).sendMessage(ChatColor.YELLOW  + "*** You injure " + myNPC.getName() + " for " + finaldamage +" damage");
				break;
			}	
		}


		if (finaldamage>0){
			npc.getBukkitEntity().playEffect(EntityEffect.HURT); 

			//is he dead?
			if 	(npc.getBukkitEntity().getHealth() - finaldamage < 0)  {
				npc.getBukkitEntity().getWorld().playEffect(npc.getBukkitEntity().getLocation(), Effect.SMOKE, 3);
				npc.getBukkitEntity().getLocation().getWorld().spawn(npc.getBukkitEntity().getLocation(), ExperienceOrb.class).setExperience(5);
				//	finaldamage = npc.getBukkitEntity().getHealth();

				if (myNPC.getBukkitEntity() instanceof HumanEntity && this.DestroyInventory) {
					((HumanEntity) myNPC.getBukkitEntity()).getInventory().clear();
					((HumanEntity) myNPC.getBukkitEntity()).getInventory().setArmorContents(null);
				}

				sentryStatus = Status.isDEAD;
				isRespawnable = System.currentTimeMillis() + RespawnDelaySeconds*1000 ;
				//	plugin.getServer().broadcastMessage("Dead!");
			}

			event.setDamage(finaldamage);
			//	myNPC.getBukkitEntity().damage(finaldamage);
			event.setCancelled(false);

		}
		else{
			// do nothing
		}

	}

	public String getStats(){
		DecimalFormat df = new DecimalFormat("#.0");
		return ChatColor.RED+ "[HP]:" +ChatColor.WHITE+ sentryHealth + ChatColor.RED+" [AP]:" +ChatColor.WHITE+  Armor +ChatColor.RED+" [STR]:" + ChatColor.WHITE+ Strength +ChatColor.RED+ " [SPD]:" +ChatColor.WHITE+  df.format(sentrySpeed) +ChatColor.RED+ " [RNG]:" +ChatColor.WHITE+  sentryRange + ChatColor.RED+ " [ATK]:" +ChatColor.WHITE+  AttackRateSeconds;
	}

	public LivingEntity findTarget ( Integer Range) {

		List<Entity> EntitiesWithinRange = myNPC.getBukkitEntity().getNearbyEntities(Range, Range, Range);
		LivingEntity theTarget = null;
		Double distanceToBeat = 99999.0;

		//	plugin.getServer().broadcastMessage("Targets scanned : " + EntitiesWithinRange.toString());

		for (Entity aTarget : EntitiesWithinRange) {

			boolean isATarget = false;

			if (aTarget instanceof Player) {

				if (this.containsTarget("ENTITY:PLAYER")) {
					isATarget = true;
				}


				else if (this.containsTarget("PLAYER:" + ((Player) aTarget).getName().toUpperCase())) {
					isATarget = true;
				}


				else if (this.containsTarget("GROUP:")) {
					String[] groups = Sentry.perms.getPlayerGroups((Player) aTarget);
					for (int i = 0; i < groups.length; i++ ) {
						if (this.containsTarget("GROUP:" + groups[i].toLowerCase())) {
							isATarget = true;
						}						
					}
				}
			}

			else if (aTarget instanceof Monster) {
				if (this.containsTarget("ENTITY:MONSTER")) {
					isATarget = true;
				}
			}

			else if (aTarget instanceof LivingEntity) {
				if (this.containsTarget("ENTITY:" + ((LivingEntity) aTarget).getType())) {
					isATarget = true;
				}
			}


			//find closest target
			if (isATarget) {


				//	double elev = victor.getY();

				//don't target entities straight down.
				/*	if( elev > (-2 * dist) ) {
					//now find closes mob
					if (dist< distanceToBeat) {
						distanceToBeat = dist;
						theTarget = (LivingEntity) aTarget;
					}					
				}*/


				Vector victor =  ((LivingEntity) aTarget).getEyeLocation().subtract(myNPC.getBukkitEntity().getEyeLocation()).toVector();
				double dist =  Math.sqrt(Math.pow(victor.getX(), 2) + Math.pow(victor.getZ(), 2));

				if (dist< distanceToBeat) {

					//LoS calc
					boolean LOS = (((CraftLivingEntity) myNPC.getBukkitEntity()).getHandle()).l(((CraftLivingEntity)aTarget).getHandle()); 
					//		plugin.getServer().broadcastMessage("LOS for: " + aTarget.toString() + LOS);

					if (LOS  ){
						//now find closes mob
						distanceToBeat = dist;
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


