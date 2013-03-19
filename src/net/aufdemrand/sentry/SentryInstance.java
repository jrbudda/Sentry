package net.aufdemrand.sentry;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Owner;

//Version Specifics
import net.minecraft.server.v1_5_R1.EntityHuman;
import net.minecraft.server.v1_5_R1.EntityPotion;
import net.minecraft.server.v1_5_R1.Packet;
import net.minecraft.server.v1_5_R1.Packet18ArmAnimation;
import org.bukkit.craftbukkit.v1_5_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_5_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_5_R1.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_5_R1.inventory.CraftItemStack;
/////////////////////////

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;


public class SentryInstance {

	public enum hittype {
		block, disembowel, glance, injure, main, miss, normal,
	}

	public enum Status {
		isDEAD, isDYING,isHOSTILE, isLOOKING, isRETALIATING, isSTUCK, isWWAITING
	}

	private Set<Player> _myDamamgers = new HashSet<Player>();

	private Location _projTargetLostLoc;

	public Integer Armor = 0;

	public Double AttackRateSeconds = 2.0;

	public Boolean DropInventory = false;

	public int epcount = 0;
	private GiveUpStuckAction giveup = new GiveUpStuckAction(this);

	public String GreetingMessage = "§a<NPC> says: Welcome, <PLAYER>!";
	public LivingEntity guardEntity = null;;
	public String guardTarget = null;

	Packet healanim = null;
	public Double HealRate = 0.0;

	public List<String> ignoreTargets = new ArrayList<String>();
	public List<String> validTargets = new ArrayList<String>();

	public Set<String> _ignoreTargets = new HashSet<String>();
	public Set<String> _validTargets = new HashSet<String>();

	private boolean inciendary = false;
	public Boolean Invincible = false;
	Long isRespawnable = System.currentTimeMillis();
	boolean lightning = false;
	int lightninglevel = 0;
	public boolean loaded = false;
	public Boolean LuckyHits = true;
	public LivingEntity meleeTarget;
	public NPC myNPC = null;
	private Class<? extends Projectile> myProjectile;
	/* Setables */
	public SentryTrait myTrait;
	public Integer NightVision = 16;
	private long oktoFire = System.currentTimeMillis();
	private long oktoheal = System.currentTimeMillis();
	private long oktoreasses= System.currentTimeMillis();
	private long okToTakedamage = 0;
	/* plugin Constructer */
	Sentry plugin;
	public List<PotionEffect> potionEffects = null;
	ItemStack potiontype = null;
	public LivingEntity projectileTarget;
	Random r = new Random();
	public Integer RespawnDelaySeconds = 10;
	public Boolean Retaliate = true;
	public Integer sentryHealth = 20;

	public Integer sentryRange = 10;

	public float sentrySpeed =  (float) 1.0;

	/* Internals */
	public Status sentryStatus = Status.isDYING;

	public Double sentryWeight = 1.0;

	public Location Spawn = null;

	public Integer Strength = 1;

	/* Technicals */
	private Integer taskID = null;


	public String WarningMessage = "§c<NPC> says: Halt! Come no further!";

	public Integer WarningRange = 0;

	private Map<Player, Long> Warnings = new  HashMap<Player, Long>();

	public SentryInstance(Sentry plugin) {
		this.plugin = plugin;
		isRespawnable = System.currentTimeMillis();
	}
	public void cancelRunnable() {
		if (taskID != null) {
			plugin.getServer().getScheduler().cancelTask(taskID);
		}
	}


	public boolean hasTargetType(int type){
		return (this.targets & type) == type;
	}
	public boolean hasIgnoreType(int type){
		return (this.ignores & type) == type;
	}

	private boolean checkTarget (LivingEntity aTarget){
		//cheak ignores

		if(ignores > 0){

			if (hasIgnoreType(all)) return false;

			if (aTarget instanceof Player && !net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget)) {

				if (hasIgnoreType(players)) return false;		

				else{
					String name = ((Player) aTarget).getName();

					if ( this.hasIgnoreType(namedplayers) && containsIgnore("PLAYER:" + name)) 	return false;

					if ( this.hasIgnoreType(owner)  && name.equalsIgnoreCase(myNPC.getTrait(Owner.class).getOwner()))		return false;

					else if(this.hasIgnoreType(groups)) {

						String[] groups1 = plugin.perms.getPlayerGroups(aTarget.getWorld(),name); // world perms
						String[] groups2 = plugin.perms.getPlayerGroups((World)null,name); //global perms
						//		String[] groups3 = plugin.perms.getPlayerGroups(aTarget.getWorld().getName(),name); // world perms
						//	String[] groups4 = plugin.perms.getPlayerGroups((Player)aTarget); // world perms


						if (groups1 !=null){
							for (int i = 0; i < groups1.length; i++) {
								//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found world1 group " + groups1[i] + " on " + name);
								if (this.containsIgnore("GROUP:" + groups1[i]))	return false;
							}	
						}

						if ( groups2 !=null){
							for (int i = 0; i < groups2.length; i++) {
								//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found global group " + groups2[i] + " on " + name);
								if (this.containsIgnore("GROUP:" + groups2[i]))		return false;
							}	
						}
					}

					if(this.hasIgnoreType(towny)) {
						String[] info = plugin.getResidentTownyInfo((Player)aTarget);

						if (info[1]!=null) {
							if (this.containsIgnore("TOWN:" + info[1]))	return false;
						}

						if (info[0]!=null) {
							if (this.containsIgnore("NATION:" + info[0]))	return false;
						}
					}

					if( this.hasIgnoreType(faction) ) {
						String faction = plugin.getFactionsTag((Player)aTarget);
						//	plugin.getLogger().info(faction);
						if (faction !=null) {
							if (this.containsIgnore("FACTION:" + faction))	return false;
						}
					}
					if( this.hasIgnoreType(war) ) {
						String team = plugin.getWarTeam((Player)aTarget);
						//	plugin.getLogger().info(faction);
						if (team !=null) {
							if (this.containsIgnore("TEAM:" + team))	return false;
						}
					}
					if( this.hasIgnoreType(clans) ) {
						String clan = plugin.getClan((Player)aTarget);
						//	plugin.getLogger().info(faction);
						if (clan !=null) {
							if (this.containsIgnore("CLAN:" + clan))	return false;
						}
					}
				}
			}

			else if(net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget)){

				if (this.hasIgnoreType(npcs)) {
					return false;
				}

				NPC npc =  net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(aTarget);

				if (npc !=null) {

					String name =npc.getName();

					if (this.hasIgnoreType(namednpcs) && this.containsIgnore("NPC:" + name)) 	return false;

					else if(hasIgnoreType(groups)) {

						String[] groups1 = plugin.perms.getPlayerGroups(aTarget.getWorld(),name); // world perms
						String[] groups2 = plugin.perms.getPlayerGroups((World)null,name); //global perms

						if (groups1 !=null){
							for (int i = 0; i < groups1.length; i++) {
								//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found world1 group " + groups1[i] + " on " + name);
								if (this.containsIgnore("GROUP:" + groups1[i]))	return false;
							}	
						}

						if ( groups2 !=null){
							for (int i = 0; i < groups2.length; i++) {
								//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found global group " + groups2[i] + " on " + name);
								if (this.containsIgnore("GROUP:" + groups2[i]))		return false;
							}	
						}
					}
				}
			}


			else if (aTarget instanceof Monster && hasIgnoreType(monsters)) return false;

			else if (aTarget instanceof LivingEntity && hasIgnoreType(namedentities)) {
				if (this.containsIgnore("ENTITY:" + aTarget.getType()))	return false;
			}
		}

		//not ignored, ok!


		if (this.hasTargetType(all)) 	return true;

		//Check if target
		if (aTarget instanceof Player && !net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget)) {

			if (this.hasTargetType(players)) {
				return true;
			}

			else{
				String name = ((Player) aTarget).getName();

				if (hasTargetType(namedplayers) && this.containsTarget("PLAYER:" + name)) 	return true;

				if ( this.containsTarget("ENTITY:OWNER")  && name.equalsIgnoreCase(myNPC.getTrait(Owner.class).getOwner()))	 return true;

				if(hasTargetType(groups)) {

					String[] groups1 = plugin.perms.getPlayerGroups(aTarget.getWorld(),name); // world perms
					String[] groups2 = plugin.perms.getPlayerGroups((World)null,name); //global perms

					if (groups1 !=null){
						for (int i = 0; i < groups1.length; i++) {
							//			plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found world1 group " + groups1[i] + " on " + name);
							if (this.containsTarget("GROUP:" + groups1[i]))	return true;
						}	
					}

					if ( groups2 !=null){
						for (int i = 0; i < groups2.length; i++) {
							//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found global group " + groups2[i] + " on " + name);
							if (this.containsTarget("GROUP:" + groups2[i]))	return true;
						}	
					}
				}

				if(this.hasTargetType(towny) || (this.hasTargetType(townyenemies))) {
					String[] info = plugin.getResidentTownyInfo((Player)aTarget);

					if (this.hasTargetType(towny) && info[1]!=null) {
						if (this.containsTarget("TOWN:" + info[1]))return true;
					}

					if (info[0]!=null) {
						if (this.hasTargetType(towny) && this.containsTarget("NATION:" + info[0]))return true;

						if(this.hasTargetType(townyenemies)){
							for (String s : NationsEnemies) {
								if (plugin.isNationEnemy(s,  info[0]))	return true;
							}	
						}

					}
				}

				if(this.hasTargetType(faction) ) {
					String faction = plugin.getFactionsTag((Player)aTarget);
					if (faction !=null) {
						if (this.containsTarget("FACTION:" + faction))return true;
					}
				}
				if(this.hasTargetType(war) ) {
					String team = plugin.getWarTeam((Player)aTarget);
					//	plugin.getLogger().info(faction);
					if (team !=null) {
						if (this.containsTarget("TEAM:" + team))	return true;
					}
				}
				if( this.hasTargetType(clans) ) {
					String clan = plugin.getClan((Player)aTarget);
					//	plugin.getLogger().info(faction);
					if (clan !=null) {
						if (this.containsTarget("CLAN:" + clan))	return true;
					}
				}
			}
		}

		else if( net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget)){

			if (this.hasTargetType(npcs)) {
				return true;
			}

			NPC npc =  net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(aTarget);

			String name =npc.getName();

			if ( this.hasTargetType(namednpcs) && containsTarget("NPC:" + name)) return true;

			if(this.hasTargetType(groups)) {

				String[] groups1 = plugin.perms.getPlayerGroups(aTarget.getWorld(),name); // world perms
				String[] groups2 = plugin.perms.getPlayerGroups((World)null,name); //global perms
				//		String[] groups3 = plugin.perms.getPlayerGroups(aTarget.getWorld().getName(),name); // world perms
				//	String[] groups4 = plugin.perms.getPlayerGroups((Player)aTarget); // world perms

				if (groups1 !=null){
					for (int i = 0; i < groups1.length; i++) {
						//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found world1 group " + groups1[i] + " on " + name);
						if (this.containsTarget("GROUP:" + groups1[i]))	return true;
					}	
				}

				if ( groups2 !=null){
					for (int i = 0; i < groups2.length; i++) {
						//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found global group " + groups2[i] + " on " + name);
						if (this.containsTarget("GROUP:" + groups2[i]))		return true;
					}	
				}
			}	
		}
		else if (aTarget instanceof Monster && this.hasTargetType(monsters)) 		return true;

		else if (aTarget instanceof LivingEntity && hasTargetType(namedentities)) {
			if (this.containsTarget("ENTITY:" + aTarget.getType())) return true;
		}


		return false;

	}

	// private Random r = new Random();

	public boolean containsIgnore(String theTarget) {
		return _ignoreTargets.contains(theTarget.toUpperCase());
	}

	public boolean containsTarget(String theTarget) {
		return _validTargets.contains(theTarget.toUpperCase());

	}

	public void deactivate() {
		plugin.getServer().getScheduler().cancelTask(taskID);
	}

	public void die(boolean runscripts){
		if (sentryStatus == Status.isDYING || sentryStatus == Status.isDEAD) return;

		sentryStatus = Status.isDYING;

		setTarget(null, false);
		//		myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);

		if(!runscripts || !DenizenHook.SentryDeath(_myDamamgers, myNPC))	{

			//Denizen is NOT handling this death

			sentryStatus = Status.isDEAD;

			if (this.DropInventory)  myNPC.getBukkitEntity().getLocation().getWorld().spawn(myNPC.getBukkitEntity().getLocation(), ExperienceOrb.class).setExperience(plugin.SentryEXP);

			if (plugin.DieLikePlayers){
				if (myNPC.getBukkitEntity() instanceof HumanEntity && !this.DropInventory) {
					//delete armor so it wont drop naturally.
					((HumanEntity) myNPC.getBukkitEntity()).getInventory().clear();
					((HumanEntity) myNPC.getBukkitEntity()).getInventory().setArmorContents(null);
				}

				myNPC.getBukkitEntity().setHealth(0);		


				if (RespawnDelaySeconds == -1) {
					cancelRunnable();
					myNPC.destroy();
					return;
				} else {
					isRespawnable = System.currentTimeMillis() + RespawnDelaySeconds * 1000;
				}

			}
			else{

				List<ItemStack> items = new java.util.LinkedList<ItemStack>();

				if (myNPC.getBukkitEntity() instanceof HumanEntity && this.DropInventory) {
					//manually drop inventory.
					for( ItemStack is:	((HumanEntity) myNPC.getBukkitEntity()).getInventory().getArmorContents()){
						if (is.getTypeId()>0)	items.add(is);  
					}	
					ItemStack is = ((HumanEntity) myNPC.getBukkitEntity()).getInventory().getItemInHand();
					if (is.getTypeId()>0)	items.add(is);  
				}		

				org.bukkit.event.entity.EntityDeathEvent ed = new org.bukkit.event.entity.EntityDeathEvent(myNPC.getBukkitEntity(), items);
				net.citizensnpcs.api.event.NPCDeathEvent nd = new net.citizensnpcs.api.event.NPCDeathEvent(myNPC, ed);

				plugin.getServer().getPluginManager().callEvent(nd);

				for (ItemStack is : items){
					myNPC.getBukkitEntity().getWorld().dropItemNaturally(myNPC.getBukkitEntity().getLocation(), is);
				}

				myNPC.despawn();

				if (RespawnDelaySeconds == -1) {
					cancelRunnable();
					myNPC.destroy();
					return;
				} else {
					isRespawnable = System.currentTimeMillis() + RespawnDelaySeconds * 1000;
				}


			}
		}
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

		net.citizensnpcs.util.NMS.look((LivingEntity) from, (float) yaw - 90, (float) pitch);

	}

	private void faceForward() {
		net.citizensnpcs.util.NMS.look((LivingEntity) myNPC.getBukkitEntity(), 0, 0);
	}

	public LivingEntity findTarget(Integer Range) {
		Range+=WarningRange; 
		List<Entity> EntitiesWithinRange = myNPC.getBukkitEntity().getNearbyEntities(Range, Range, Range);
		LivingEntity theTarget = null;
		Double distanceToBeat = 99999.0;

		// plugin.getServer().broadcastMessage("Targets scanned : " +
		// EntitiesWithinRange.toString());

		for (Entity aTarget : EntitiesWithinRange) {
			if (!(aTarget instanceof LivingEntity)) continue;

			// find closest target
			if (checkTarget((LivingEntity) aTarget)) {

				// can i see it?
				// too dark?
				double ll = aTarget.getLocation().getBlock().getLightLevel();
				// sneaking cut light in half
				if (aTarget instanceof Player)
					if (((Player) aTarget).isSneaking())
						ll /= 2;

				// too dark?
				if (ll >= (16 - this.NightVision)) {


					double dist = aTarget.getLocation().distance(myNPC.getBukkitEntity().getLocation());

					boolean LOS = myNPC.getBukkitEntity().hasLineOfSight(aTarget);
					if (LOS) {					


						if (WarningRange >0 && sentryStatus == Status.isLOOKING && aTarget instanceof Player &&  dist > (Range - WarningRange) && !net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget) & !(WarningMessage.isEmpty())){

							if (Warnings.containsKey(aTarget) && System.currentTimeMillis() < Warnings.get(aTarget) + 60*1000){
								//already warned u in last 30 seconds.
							}
							else{
								((Player)aTarget).sendMessage(getWarningMessage((Player) aTarget)); 
								if(!myNPC.getNavigator().isNavigating())	faceEntity(myNPC.getBukkitEntity(), aTarget);
								Warnings.put((Player) aTarget,System.currentTimeMillis());
							}

						}
						else if	(dist < distanceToBeat) {				
							// now find closes mob
							distanceToBeat = dist;
							theTarget = (LivingEntity) aTarget;
						}
					}


				}

			}
			else {
				//not a target

				if (WarningRange >0 && sentryStatus == Status.isLOOKING && aTarget instanceof Player &&  !net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget) && !(GreetingMessage.isEmpty())){
					boolean LOS = myNPC.getBukkitEntity().hasLineOfSight(aTarget);
					if (LOS) {			
						if (Warnings.containsKey(aTarget) && System.currentTimeMillis() < Warnings.get(aTarget) + 60*1000){
							//already greeted u in last 30 seconds.
						}
						else{
							((Player)aTarget).sendMessage(getGreetingMEssage((Player) aTarget)); 
							faceEntity(myNPC.getBukkitEntity(), aTarget);
							Warnings.put((Player) aTarget,System.currentTimeMillis());
						}
					}
				}

			}

		}


		if (theTarget != null) {
			// plugin.getServer().broadcastMessage("Targeting: " +
			// theTarget.toString());
			return theTarget;
		}

		return null;
	}


	public void Draw(boolean on){
		((CraftLivingEntity)(myNPC.getBukkitEntity())).getHandle().e(on);
	}

	public void Fire(LivingEntity theEntity) {

		double v = 34;
		double g = 20;

		Effect effect = null;

		boolean ballistics = true;

		if (myProjectile == Arrow.class) {
			effect = Effect.BOW_FIRE;
		} else if (myProjectile == SmallFireball.class || myProjectile == Fireball.class || myProjectile == org.bukkit.entity.WitherSkull.class) {
			effect = Effect.BLAZE_SHOOT;
			ballistics =false;
		}
		else if (myProjectile == org.bukkit.entity.ThrownPotion.class){
			v = 21;
			g = 20;
		}
		else {
			v = 17.75;
			g = 13.5;
		}

		if(lightning) {
			ballistics = false;
			effect =null;
		}

		// calc shooting spot.
		Location loc = Util.getFireSource(myNPC.getBukkitEntity(), theEntity);

		Location targetsHeart = theEntity.getLocation();
		targetsHeart = targetsHeart.add(0, .33, 0);

		Vector test = targetsHeart.clone().subtract(loc).toVector();

		Double elev = test.getY();

		Double testAngle = Util.launchAngle(loc, targetsHeart, v, elev, g);

		if (testAngle == null) {
			// testAngle = Math.atan( ( 2*g*elev + Math.pow(v, 2)) / (2*g*elev +
			// 2*Math.pow(v,2))); //cant hit it where it is, try aiming as far
			// as you can.
			setTarget(null, false);
			// plugin.getServer().broadcastMessage("Can't hit test angle");
			return;
		}

		// plugin.getServer().broadcastMessage("ta " + testAngle.toString());

		Double hangtime = Util.hangtime(testAngle, v, elev, g);
		// plugin.getServer().broadcastMessage("ht " + hangtime.toString());

		Vector targetVelocity = theEntity.getLocation().subtract(_projTargetLostLoc).toVector();
		// plugin.getServer().broadcastMessage("tv" + targetVelocity);

		targetVelocity.multiply(20 / plugin.LogicTicks);

		Location to = Util.leadLocation(targetsHeart, targetVelocity, hangtime);
		// plugin.getServer().broadcastMessage("to " + to);
		// Calc range

		Vector victor = to.clone().subtract(loc).toVector();

		Double dist = Math.sqrt(Math.pow(victor.getX(), 2) + Math.pow(victor.getZ(), 2));
		elev = victor.getY();
		if (dist == 0)
			return;
		boolean LOS =  myNPC.getBukkitEntity().hasLineOfSight(theEntity);
		if (!LOS) {
			// target cant be seen..
			setTarget(null, false);
			// plugin.getServer().broadcastMessage("No LoS");
			return;
		}

		// plugin.getServer().broadcastMessage("delta " + victor);

		// plugin.getServer().broadcastMessage("ld " +
		// to.clone().subtract(theEntity.getEyeLocation()));

		if(ballistics){
			Double launchAngle = Util.launchAngle(loc, to, v, elev, g);
			if (launchAngle == null) {
				// target cant be hit
				setTarget(null, false);
				// plugin.getServer().broadcastMessage("Can't hit lead");
				return;

			}

			//	plugin.getServer().broadcastMessage(anim.a + " " + anim.b + " " + anim.a() + " " +anim.);
			// Apply angle
			victor.setY(Math.tan(launchAngle) * dist);
			Vector noise = Vector.getRandom();
			// normalize vector
			victor = Util.normalizeVector(victor);

			noise = noise.multiply(1 / 10.0);

			// victor = victor.add(noise);

			if (myProjectile == Arrow.class || myProjectile == org.bukkit.entity.ThrownPotion.class){
				v = v + (1.188 * Math.pow(hangtime, 2));		
			}
			else {
				v = v + (.5 * Math.pow(hangtime, 2));	
			}

			v = v+ (r.nextDouble() - .8)/2;

			// apply power
			victor = victor.multiply(v / 20.0);

			// Shoot!
			// Projectile theArrow
			// =myNPC.getBukkitEntity().launchProjectile(myProjectile);

		}
		else{
			if (dist > sentryRange) {
				// target cant be hit
				setTarget(null, false);
				// plugin.getServer().broadcastMessage("Can't hit lead");
				return;

			}
		}

		if(lightning){
			if (lightninglevel ==2){
				to.getWorld().strikeLightning(to);
			}
			else if (lightninglevel == 1){
				to.getWorld().strikeLightningEffect(to);
				theEntity.damage(getStrength(), myNPC.getBukkitEntity());
			}	
			else if (lightninglevel == 3){
				to.getWorld().strikeLightningEffect(to);
				theEntity.setHealth(0);
			}
		}
		else
		{

			Projectile theArrow = null;


			if(myProjectile == org.bukkit.entity.ThrownPotion.class){
				net.minecraft.server.v1_5_R1.World nmsWorld = ((CraftWorld)myNPC.getBukkitEntity().getWorld()).getHandle();
				EntityPotion ent = new EntityPotion(nmsWorld, loc.getX(), loc.getY(), loc.getZ(), CraftItemStack.asNMSCopy(potiontype));
				nmsWorld.addEntity(ent);
				theArrow = (Projectile) ent.getBukkitEntity();

			}

			else if(myProjectile == org.bukkit.entity.EnderPearl.class){
				theArrow = myNPC.getBukkitEntity().launchProjectile(myProjectile);	
			}

			else{
				theArrow = myNPC.getBukkitEntity().getWorld().spawn(loc, myProjectile);	
			}


			if (myProjectile == Fireball.class || myProjectile == org.bukkit.entity.WitherSkull.class) {
				victor = victor.multiply(1/1000000000);
			}
			else if (myProjectile == SmallFireball.class) {
				victor = victor.multiply(1/1000000000);
				((SmallFireball)theArrow).setIsIncendiary(inciendary);
				if(!inciendary)	{
					((SmallFireball)theArrow).setFireTicks(0);
					((SmallFireball)theArrow).setYield(0);
				}
			}
			else if (myProjectile == org.bukkit.entity.EnderPearl.class){
				epcount++;
				if (epcount > Integer.MAX_VALUE-1) epcount=0;
				plugin.debug(epcount + "");
			}

			plugin.arrows.add(theArrow);
			theArrow.setShooter(myNPC.getBukkitEntity());
			theArrow.setVelocity(victor);
		}

		// OK we're shooting
		// go twang
		if (effect != null)
			myNPC.getBukkitEntity().getWorld().playEffect(myNPC.getBukkitEntity().getLocation(), effect, null);

		if (myProjectile == Arrow.class){
			Draw(false);
		}
		else {
			if(myNPC.getBukkitEntity() instanceof org.bukkit.entity.Player)	{
				net.citizensnpcs.util.PlayerAnimation.ARM_SWING.play((Player) myNPC.getBukkitEntity(), 64);
			}
		}




	}


	public int getArmor(){

		double mod = 0;
		if ( myNPC.getBukkitEntity() instanceof Player){
			for (ItemStack is:((Player)myNPC.getBukkitEntity()).getInventory().getArmorContents()){
				if (plugin.ArmorBuffs.containsKey(is.getTypeId())) mod += plugin.ArmorBuffs.get(is.getTypeId());		
			}
		}

		return (int) (Armor + mod);
	}
	String getGreetingMEssage(Player player){
		String str=  GreetingMessage.replace("<NPC>", myNPC.getName()).replace("<PLAYER>", player.getName());
		return ChatColor.translateAlternateColorCodes('&', str);
	}
	public LivingEntity getGuardTarget() {
		return this.guardEntity;
	}
	public int getHealth(){
		if (myNPC == null) return 0;
		if (myNPC.getBukkitEntity() == null) return 0;
		return ((CraftLivingEntity) myNPC.getBukkitEntity()).getHandle().getHealth(); 
	}
	public float getSpeed(){
		double mod = 0;
		if ( myNPC.getBukkitEntity() instanceof Player){
			for (ItemStack is:((Player)myNPC.getBukkitEntity()).getInventory().getArmorContents()){
				if (plugin.SpeedBuffs.containsKey(is.getTypeId())) mod += plugin.SpeedBuffs.get(is.getTypeId());		
			}
		}	
		return (float) (sentrySpeed + mod);
	}
	public String getStats() {
		DecimalFormat df = new DecimalFormat("#.0");
		int h = getHealth();

		return ChatColor.RED + "[HP]:" + ChatColor.WHITE + h + "/" + sentryHealth + ChatColor.RED + " [AP]:" + ChatColor.WHITE + getArmor() + ChatColor.RED + " [STR]:" + ChatColor.WHITE + getStrength() + ChatColor.RED + " [SPD]:" + ChatColor.WHITE + df.format(getSpeed()) + ChatColor.RED + " [RNG]:" + ChatColor.WHITE + sentryRange + ChatColor.RED + " [ATK]:" + ChatColor.WHITE + AttackRateSeconds + ChatColor.RED + " [VIS]:" + ChatColor.WHITE + NightVision + ChatColor.RED + " [HEAL]:" + ChatColor.WHITE + HealRate + ChatColor.RED + " [WARN]:" + ChatColor.WHITE + WarningRange;

	}

	public int getStrength(){
		double mod = 0;

		if ( myNPC.getBukkitEntity() instanceof Player){
			if (plugin.StrengthBuffs.containsKey(((Player)myNPC.getBukkitEntity()).getInventory().getItemInHand().getTypeId())) mod += plugin.StrengthBuffs.get(((Player)myNPC.getBukkitEntity()).getInventory().getItemInHand().getTypeId());		
		}

		return (int) (Strength + mod);
	}

	String getWarningMessage(Player player){
		String str=  WarningMessage.replace("<NPC>", myNPC.getName()).replace("<PLAYER>", player.getName());
		return ChatColor.translateAlternateColorCodes('&', str);

	}

	public void initialize() {

		// plugin.getServer().broadcastMessage("NPC " + npc.getName() +
		// " INITIALIZING!");

		// check for illegal values

		if (sentryWeight <= 0)
			sentryWeight = 1.0;
		if (AttackRateSeconds > 30)
			AttackRateSeconds = 30.0;

		if (sentryHealth < 0)
			sentryHealth = 0;

		if (sentryRange < 1)
			sentryRange = 1;
		if (sentryRange > 200)
			sentryRange = 200;

		if (sentryWeight <= 0)
			sentryWeight =  1.0;

		if (RespawnDelaySeconds < -1)
			RespawnDelaySeconds = -1;

		if (Spawn == null)
			Spawn = myNPC.getBukkitEntity().getLocation();


		// defaultSpeed = myNPC.getNavigator().getSpeed();


		setHealth(sentryHealth);
		//		}
		//		else {
		//			myNPC.getBukkitEntity().setHealth(myNPC.getBukkitEntity().getMaxHealth());
		//			_myhps = sentryHealth;
		//		}



		_myDamamgers.clear();

		this.sentryStatus = Status.isLOOKING;
		faceForward();

		healanim = new Packet18ArmAnimation( ((CraftEntity)myNPC.getBukkitEntity()).getHandle(),6);

		//	Packet derp = new net.minecraft.server.Packet15Place();

		if (guardTarget == null){
			myNPC.getBukkitEntity().teleport(Spawn); //it should be there... but maybe not if the position was saved elsewhere.
		}

		float pf = myNPC.getNavigator().getDefaultParameters().range();
		if(pf < sentryRange+5){
			myNPC.getNavigator().getDefaultParameters().range(sentryRange+5);
		}

		myNPC.getNavigator().getDefaultParameters().range(pf);
		myNPC.getNavigator().getDefaultParameters().stationaryTicks(5*20);
		//	myNPC.getNavigator().getDefaultParameters().stuckAction(new BodyguardTeleportStuckAction(this, this.plugin));

		// plugin.getServer().broadcastMessage("NPC GUARDING!");

		if (myNPC.getBukkitEntity() instanceof org.bukkit.entity.Creeper){
			myNPC.getNavigator().getDefaultParameters().attackStrategy(new CreeperAttackStrategy());
		}
		else if (myNPC.getBukkitEntity() instanceof org.bukkit.entity.Spider){
			myNPC.getNavigator().getDefaultParameters().attackStrategy(new SpiderAttackStrategy(plugin));
		}

		processTargets();

		if (taskID == null) {
			taskID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new SentryLogic(), 40 + this.myNPC.getId(),  plugin.LogicTicks);
		}
	}

	public boolean isPyromancer(){
		return (myProjectile == Fireball.class || myProjectile == SmallFireball.class) ;
	}

	public boolean isPyromancer1(){
		return (!inciendary && myProjectile == SmallFireball.class) ;
	}


	public boolean isPyromancer2(){
		return (inciendary && myProjectile == SmallFireball.class) ;
	}

	public boolean isPyromancer3(){
		return (myProjectile == Fireball.class) ;
	}

	public boolean isStormcaller(){
		return (lightning) ;
	}

	public boolean isWarlock1(){
		return (myProjectile == org.bukkit.entity.EnderPearl.class) ;
	}

	public boolean isWitchDoctor(){
		return (myProjectile == org.bukkit.entity.ThrownPotion.class) ;
	}


	public void onDamage(EntityDamageByEntityEvent event) {

		event.setCancelled(true);
 		if(sentryStatus == Status.isDYING) return;
		
		if (myNPC == null || !myNPC.isSpawned()) {
			// \\how did you get here?
			return;
		}
		
    	event.getEntity().setLastDamageCause(event);
    	
		if (guardTarget != null && guardEntity == null) return; //dont take damage when bodyguard target isnt around.

		if (System.currentTimeMillis() <  okToTakedamage + 500) return;
		okToTakedamage = System.currentTimeMillis(); 

		NPC npc = myNPC;

		LivingEntity player = null;

		hittype hit = hittype.normal;

		int finaldamage = event.getDamage();

		// Find the attacker
		if (event.getDamager() instanceof Projectile) {
			if (((Projectile) event.getDamager()).getShooter() instanceof LivingEntity) {
				player = ((Projectile) event.getDamager()).getShooter();
			}
		} else if (event.getDamager() instanceof LivingEntity) {
			player = (LivingEntity) event.getDamager();
		}


		if (Invincible)
			return;

		// can i kill it? lets go kill it.
		if (player != null) {
			if (this.Retaliate) {
				if (!(event.getDamager() instanceof Projectile) || (net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(player) ==null)) {
					// only retaliate to players or non-projectlies. Prevents stray sentry arrows from causing retaliation.
					setTarget(player, true);	
				}
			}
		}

		if (LuckyHits) {
			// Calulate crits
			double damagemodifer = event.getDamage();

			int luckeyhit = r.nextInt(100);

			if (luckeyhit < plugin.Crit3Chance) {
				damagemodifer = damagemodifer * 2.00;
				hit = hittype.disembowel;
			} else if (luckeyhit < plugin.Crit3Chance + plugin.Crit2Chance) {
				damagemodifer = damagemodifer * 1.75;
				hit = hittype.main;
			} else if (luckeyhit < plugin.Crit3Chance + plugin.Crit2Chance + plugin.Crit1Chance) {
				damagemodifer = damagemodifer * 1.50;
				hit = hittype.injure;
			} else if (luckeyhit <  plugin.Crit3Chance + plugin.Crit2Chance + plugin.Crit1Chance + plugin.GlanceChance) {
				damagemodifer = damagemodifer * 0.50;
				hit = hittype.glance;
			} else if (luckeyhit < plugin.Crit3Chance + plugin.Crit2Chance + plugin.Crit1Chance + plugin.GlanceChance + plugin.MissChance) {
				damagemodifer = 0;
				hit = hittype.miss;
			}

			finaldamage = (int) Math.round(damagemodifer);
		}

		int arm = getArmor();

		if (finaldamage > 0) {

			if (player != null) {
				// knockback
				npc.getBukkitEntity().setVelocity( player.getLocation().getDirection().multiply(1.0 / (sentryWeight + (arm/5))));
			}

			// Apply armor
			finaldamage -= arm;

			// there was damamge before armor.
			if (finaldamage <= 0){
				npc.getBukkitEntity().getWorld().playEffect(npc.getBukkitEntity().getLocation(), org.bukkit.Effect.ZOMBIE_CHEW_IRON_DOOR,1);
				hit = hittype.block;
			}
		}

		if (player instanceof Player && !net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(player)) {

			_myDamamgers.add((Player) player);
			String msg = null;
			// Messages
			switch (hit) {
			case normal:
				msg = plugin.HitMessage;
				break;
			case miss:
				msg = plugin.MissMessage;
				break;
			case block:
				msg = plugin.BlockMessage;
				break;
			case main:
				msg = plugin.Crit2Message;
				break;
			case disembowel:
				msg = plugin.Crit3Message;
				break;
			case injure:
				msg = plugin.Crit1Message;
				break;
			case glance:
				msg = plugin.GlanceMessage;
				break;
			}

			if(msg!=null && msg.isEmpty() == false){
				((Player) player).sendMessage(plugin.format(msg, npc, (CommandSender) player, ((Player) player).getItemInHand().getTypeId(), finaldamage+""));
			}	
		}

		if (finaldamage > 0) {
			npc.getBukkitEntity().playEffect(EntityEffect.HURT);

			// is he dead?
			if (getHealth() - finaldamage <= 0) {

				//set the killer
				if (event.getDamager() instanceof HumanEntity) 	((CraftLivingEntity)myNPC.getBukkitEntity()).getHandle().killer = (EntityHuman) ((CraftLivingEntity) event.getDamager()).getHandle();

				die(true);
				// plugin.getServer().broadcastMessage("Dead!");
			}
			else 	myNPC.getBukkitEntity().damage(finaldamage);
		}
	}


	public void onEnvironmentDamae(EntityDamageEvent event){

		if(sentryStatus == Status.isDYING) return;

		if (!myNPC.isSpawned() || Invincible) {
			return;
		}

		if (guardTarget != null && guardEntity == null) return; //dont take damage when bodyguard target isnt around.

		if (System.currentTimeMillis() <  okToTakedamage + 500) return;
		okToTakedamage = System.currentTimeMillis(); 

		myNPC.getBukkitEntity().setLastDamageCause(event);

		int finaldamage = event.getDamage();

		if (event.getCause() == DamageCause.CONTACT || event.getCause() == DamageCause.BLOCK_EXPLOSION){
			finaldamage -= getArmor();
		}

		if (finaldamage > 0 ){
			myNPC.getBukkitEntity().playEffect(EntityEffect.HURT);

			if (event.getCause() == DamageCause.FIRE){
				if (!myNPC.getNavigator().isNavigating()){
					Random R = new Random();
					myNPC.getNavigator().setTarget(myNPC.getBukkitEntity().getLocation().add(R.nextInt(2)-1, 0, R.nextInt(2)-1));
				}
			}

			if (getHealth() - finaldamage <= 0) {

				die(true);

				// plugin.getServer().broadcastMessage("Dead!");
			}
			else {
				myNPC.getBukkitEntity().damage(finaldamage);

			}
		}


	} 

	@EventHandler
	public void onRightClick(NPCRightClickEvent event) {

	}

	final int all = 1;
	final int players = 2;
	final int npcs = 4;
	final int monsters = 8;
	final int events = 16;
	final int namedentities = 32;
	final int namedplayers = 64;
	final int namednpcs = 128;
	final int faction = 256;
	final int towny = 512;
	final int war = 1024;
	final int groups = 2048;
	final int owner = 4096;
	final int clans = 8192;
	final int townyenemies = 16384;
	private int targets = 0;
	private int ignores = 0;

	List<String> NationsEnemies = new ArrayList<String>();

	public void processTargets(){
		try {

			targets = 0;
			ignores = 0;
			_ignoreTargets.clear();
			_validTargets.clear();
			NationsEnemies.clear();

			for (String t: validTargets){
				if (t.contains("ENTITY:ALL")) targets |= all;	
				else	if(t.contains("ENTITY:MONSTER")) targets |= monsters;
				else	if(t.contains("ENTITY:PLAYER")) targets |= players;
				else	if(t.contains("ENTITY:NPC")) targets |= npcs;
				else{
					_validTargets.add(t);
					if(t.contains("NPC:")) targets |= namednpcs;
					else if (plugin.perms!=null && plugin.perms.isEnabled() && t.contains("GROUP:")) targets |= groups;
					else if (t.contains("EVENT:"))  targets |= events;
					else	if(t.contains("PLAYER:")) targets |= namedplayers;
					else	if(t.contains("ENTITY:")) targets |= namedentities;
					else	if (plugin.FactionsActive && t.contains("FACTION:")) targets |= faction;
					else	if (plugin.TownyActive && t.contains("TOWN:")) targets |= towny;
					else	if (plugin.TownyActive && t.contains("NATIONENEMIES:")) {
						targets |= townyenemies;
						NationsEnemies.add(t.split(":")[1]);
					}
					else	if (plugin.TownyActive && t.contains("NATION:"))  targets |= towny;
					else	if (plugin.WarActive && t.contains("TEAM:"))  targets |= war;
					else	if (plugin.ClansActive && t.contains("CLAN:"))  targets |= clans;
				}
			}
			for (String t: ignoreTargets){
				if(t.contains("ENTITY:ALL")) ignores |= all;	
				else	if(t.contains("ENTITY:MONSTER")) ignores |= monsters;
				else	if(t.contains("ENTITY:PLAYER")) ignores |= players;
				else	if(t.contains("ENTITY:NPC")) ignores |= npcs;
				else	if(t.contains("ENTITY:OWNER")) ignores |= owner;
				else{
					_ignoreTargets.add(t);	
					if (plugin.perms!=null && plugin.perms.isEnabled() && t.contains("GROUP:")) ignores |= groups;
					else	if(t.contains("NPC:")) ignores |= namednpcs;
					else	if(t.contains("PLAYER:")) ignores |= namedplayers;
					else	if(t.contains("ENTITY:")) ignores |= namedentities;
					else	if (plugin.FactionsActive && t.contains("FACTION:")) ignores |= faction;
					else	if (plugin.TownyActive && t.contains("TOWN:")) ignores |= towny;
					else	if (plugin.TownyActive && t.contains("NATION:"))  ignores |= towny;
					else	if (plugin.WarActive && t.contains("TEAM:"))  ignores |= war;
					else	if (plugin.ClansActive && t.contains("CLAN:"))  ignores |= clans;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}


	private class SentryLogic implements Runnable {

		@Override
		public void run() {
			// plugin.getServer().broadcastMessage("tick " + (myNPC ==null) +			
			if (myNPC.getBukkitEntity() == null ) sentryStatus = Status.isDEAD; // incase it dies in a way im not handling.....

			//in case it was changed
			if (UpdateWeapon()){
				//ranged
				if(meleeTarget !=null) {
					plugin.debug("Switch to ranged");
					LivingEntity derp = meleeTarget;
					boolean ret = sentryStatus == Status.isRETALIATING;
					setTarget(null, false);
					setTarget(derp, ret);
				}
			}
			else{
				//melee
				if(projectileTarget !=null) {
					plugin.debug("Switch to melee");
					boolean ret = sentryStatus == Status.isRETALIATING;
					LivingEntity derp = projectileTarget;
					setTarget(null, false);
					setTarget(derp, ret);
				}	
			}

			if (sentryStatus != Status.isDEAD &&  HealRate > 0) {
				if(System.currentTimeMillis() > oktoheal ){
					if (getHealth() < sentryHealth && sentryStatus !=  Status.isDEAD && sentryStatus != Status.isDYING) {
						int heal = 1;
						if (HealRate <0.5) heal = (int) (0.5 / HealRate);

						if (getHealth() + heal <= sentryHealth){

							setHealth(	getHealth() + heal);
						}
						else{
							setHealth(sentryHealth);
						}

						if (healanim!=null)net.citizensnpcs.util.NMS.sendPacketsNearby(myNPC.getBukkitEntity().getLocation(),healanim);


						if (getHealth() >= sentryHealth) _myDamamgers.clear(); //healed to full, forget attackers

					}
					oktoheal = (long) (System.currentTimeMillis() + HealRate * 1000);
				}

			}

			if (sentryStatus == Status.isDEAD && System.currentTimeMillis() > isRespawnable && RespawnDelaySeconds > 0 & Spawn.getWorld().isChunkLoaded(Spawn.getBlockX()>>4, Spawn.getBlockZ()>>4)) {
				// Respawn

				plugin.debug("respawning" + myNPC.getName());
				if (guardEntity == null) {
					myNPC.spawn(Spawn);
				} else {
					myNPC.spawn(guardEntity.getLocation().add(2, 0, 2));
				}
				return;
			}
			else if ((sentryStatus == Status.isHOSTILE || sentryStatus == Status.isRETALIATING) && myNPC.isSpawned()) {

				if (!isMyChunkLoaded()){
					setTarget(null, false);
					return;
				}

				if (targets >0 && sentryStatus == Status.isHOSTILE && System.currentTimeMillis() > oktoreasses) {
					LivingEntity target = findTarget(sentryRange);
					setTarget(target, false);
					oktoreasses = System.currentTimeMillis() + 3000;
				}

				if (projectileTarget != null && !projectileTarget.isDead() && projectileTarget.getWorld() == myNPC.getBukkitEntity().getLocation().getWorld() ) {
					if (_projTargetLostLoc == null)
						_projTargetLostLoc = projectileTarget.getLocation();

					if (!myNPC.getNavigator().isNavigating())	faceEntity(myNPC.getBukkitEntity(), projectileTarget);

					Draw(true);

					if (System.currentTimeMillis() > oktoFire) {
						// Fire!
						oktoFire = (long) (System.currentTimeMillis() + AttackRateSeconds * 1000.0);
						Fire(projectileTarget);
					}
					if (projectileTarget != null)
						_projTargetLostLoc = projectileTarget.getLocation();

					return; // keep at it
				}

				else if (meleeTarget != null && !meleeTarget.isDead()) {

					if (meleeTarget.getWorld() == myNPC.getBukkitEntity().getLocation().getWorld()) {
						double dist=  meleeTarget.getLocation().distance(myNPC.getBukkitEntity().getLocation());
						//block if in range 
						Draw(dist < 3);
						// Did it get away?
						if(dist > sentryRange) {
							// it got away...
							setTarget(null, false);
						}
					}
					else {
						setTarget(null, false);
					}

				}

				else {
					// target died or null
					setTarget(null, false);
				}

			}

			else if (sentryStatus == Status.isLOOKING && myNPC.isSpawned()) {

				if (guardEntity instanceof Player){
					if (((Player)guardEntity).isOnline() == false){
						guardEntity = null;
					}
				}

				if (guardTarget != null && guardEntity == null) {
					// daddy? where are u?
					setGuardTarget(guardTarget);
				}	

				if (guardEntity !=null){

					Location npcLoc = myNPC.getBukkitEntity().getLocation();

					if (guardEntity.getLocation().getWorld() != npcLoc.getWorld() || !isMyChunkLoaded()){
						myNPC.despawn();
						myNPC.spawn((guardEntity.getLocation().add(1, 0, 1)));
					}
					else{
						double dist = npcLoc.distanceSquared(guardEntity.getLocation());
						plugin.debug(myNPC.getName() + dist + myNPC.getNavigator().isNavigating() + " " +myNPC.getNavigator().getEntityTarget() + " " );
						if(dist > 1024) {
							myNPC.getBukkitEntity().teleport(guardEntity.getLocation().add(1,0,1));
						}
						else if(dist > 16 && !myNPC.getNavigator().isNavigating()) {
							myNPC.getNavigator().setTarget(guardEntity, false);
							myNPC.getNavigator().getLocalParameters().stationaryTicks(3*20);	
						}
						else if (dist < 16 && myNPC.getNavigator().isNavigating()) {
							myNPC.getNavigator().cancelNavigation();
						}
					}
				}

				LivingEntity target = null;

				if(targets > 0){
					target = findTarget(sentryRange);		
				}

				if (target !=null)	{
					oktoreasses = System.currentTimeMillis() + 3000;
					setTarget(target, false);
				}

			}

		}
	}


	private boolean isMyChunkLoaded(){
		if (myNPC.getBukkitEntity() == null) return false;
		Location npcLoc = myNPC.getBukkitEntity().getLocation();
		return npcLoc.getWorld().isChunkLoaded(npcLoc.getBlockX()>>4, npcLoc.getBlockZ()>>4);
	}

	public boolean setGuardTarget(String name) {

		if (myNPC == null)
			return false;

		if (name == null) {
			guardEntity = null;
			guardTarget = null;
			setTarget(null, false);// clear active hostile target
			return true;
		}

		List<Entity> EntitiesWithinRange = myNPC.getBukkitEntity().getNearbyEntities(sentryRange, sentryRange, sentryRange);

		for (Entity aTarget : EntitiesWithinRange) {

			if (aTarget instanceof Player) {
				if (((Player) aTarget).getName().equals(name)) {
					guardEntity = (LivingEntity) aTarget;
					guardTarget = ((Player) aTarget).getName();
					setTarget(null, false); // clear active hostile target
					return true;
				}

			}

		}
		return false;
	}

	public void setHealth(int health){
		if (myNPC == null) return;
		if (myNPC.getBukkitEntity() == null) return;
		((CraftLivingEntity) myNPC.getBukkitEntity()).getHandle().setHealth(health); 	
	}


	public boolean UpdateWeapon(){
		int weapon = 0;

		ItemStack is = null;

		if (myNPC.getBukkitEntity() instanceof HumanEntity) {
			is = ((HumanEntity) myNPC.getBukkitEntity()).getInventory().getItemInHand();
			weapon = is.getTypeId();
			if(	weapon != plugin.witchdoctor) is.setDurability((short) 0);
		}

		lightning = false;
		lightninglevel = 0;
		inciendary = false;
		potionEffects = plugin.WeaponEffects.get(weapon);

		myProjectile = null;

		if(weapon == plugin.archer || myNPC.getBukkitEntity() instanceof org.bukkit.entity.Skeleton){
			myProjectile = org.bukkit.entity.Arrow.class;
		}
		else if(weapon ==  plugin.pyro3 || myNPC.getBukkitEntity() instanceof org.bukkit.entity.Ghast){
			myProjectile = org.bukkit.entity.Fireball.class;
		}
		else if(weapon ==  plugin.pyro2 || myNPC.getBukkitEntity() instanceof org.bukkit.entity.Blaze || myNPC.getBukkitEntity() instanceof org.bukkit.entity.EnderDragon){
			myProjectile = org.bukkit.entity.SmallFireball.class;
			inciendary = true;
		}
		else if(weapon ==  plugin.pyro1){
			myProjectile = org.bukkit.entity.SmallFireball.class;
			inciendary =false;
		}
		else if(weapon == plugin.magi || myNPC.getBukkitEntity() instanceof org.bukkit.entity.Snowman){
			myProjectile = org.bukkit.entity.Snowball.class;
		}
		else if(weapon == plugin.warlock1){
			myProjectile = org.bukkit.entity.EnderPearl.class;
		}
		else if(weapon == plugin.warlock2 || myNPC.getBukkitEntity() instanceof org.bukkit.entity.Wither){
			myProjectile = org.bukkit.entity.WitherSkull.class;
		}
		else if(weapon == plugin.warlock3){
			myProjectile = org.bukkit.entity.WitherSkull.class;
		}
		else if(weapon == plugin.bombardier){
			myProjectile = org.bukkit.entity.Egg.class;
		}
		else if(weapon == plugin.witchdoctor || myNPC.getBukkitEntity() instanceof org.bukkit.entity.Witch ){
			if (is == null){
				is = new ItemStack(373,1,(short) 16396);	
			}
			myProjectile = org.bukkit.entity.ThrownPotion.class;
			potiontype = is;
		}
		else if(weapon == plugin.sc1){
			myProjectile = org.bukkit.entity.ThrownPotion.class;
			lightning = true;
			lightninglevel = 1;	
		}
		else if (weapon == plugin.sc2){
			myProjectile = org.bukkit.entity.ThrownPotion.class;
			lightning = true;
			lightninglevel = 2;	
		}
		else if (weapon == plugin.sc3){
			myProjectile = org.bukkit.entity.ThrownPotion.class;
			lightning = true;
			lightninglevel = 3;	
		}
		else{
			return false;
		}

		return true;
	}
	public void setTarget(LivingEntity theEntity, boolean isretaliation) {

		if (myNPC.getBukkitEntity() == null ) return;

		if (theEntity == myNPC.getBukkitEntity()) return; //I don't care how you got here. No. just No.

		if (guardTarget != null && guardEntity == null) theEntity =null; //dont go aggro when bodyguard target isnt around.

		if (theEntity == null) {
			plugin.debug("Set Target Null");
			// this gets called while npc is dead, reset things.
			sentryStatus = Status.isLOOKING;
			projectileTarget = null;
			meleeTarget = null;
			_projTargetLostLoc = null;
		}

		if (myNPC == null)
			return;
		if (!myNPC.isSpawned())
			return;

		if (theEntity == null) {
			// no hostile target

			Draw(false);

			//		plugin.getServer().broadcastMessage(myNPC.getNavigator().getTargetAsLocation().toString());
			//plugin.getServer().broadcastMessage(((Boolean)myNPC.getTrait(Waypoints.class).getCurrentProvider().isPaused()).toString());

			if (guardEntity != null) {
				// yarr... im a guarrrd.

				myNPC.getDefaultGoalController().setPaused(true);
				//	if (!myNPC.getTrait(Waypoints.class).getCurrentProvider().isPaused())  myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);

				if (myNPC.getNavigator().getEntityTarget() == null || (myNPC.getNavigator().getEntityTarget() != null && myNPC.getNavigator().getEntityTarget().getTarget() != guardEntity)){

					if (guardEntity.getLocation().getWorld() != myNPC.getBukkitEntity().getLocation().getWorld()){
						myNPC.despawn();
						myNPC.spawn((guardEntity.getLocation().add(1, 0, 1)));
						return;
					}

					myNPC.getNavigator().setTarget(guardEntity, false);
					//		myNPC.getNavigator().getLocalParameters().stuckAction(bgteleport);
					myNPC.getNavigator().getLocalParameters().stationaryTicks(3*20);
				}
			} else {
				//not a guard
				myNPC.getNavigator().cancelNavigation();
				if (myNPC.getDefaultGoalController().isPaused()) 
					myNPC.getDefaultGoalController().setPaused(false);


				else faceForward();
			}
			return;
		}

		if (theEntity == guardEntity)
			return; // dont attack my dude.

		if (isretaliation) sentryStatus = Status.isRETALIATING;
		else sentryStatus = Status.isHOSTILE;


		if(!myNPC.getNavigator().isNavigating()) faceEntity(myNPC.getBukkitEntity(), theEntity);


		if(UpdateWeapon()){
			//ranged
			plugin.debug("Set Target ranged");
			projectileTarget = theEntity;	
			meleeTarget = null;

		}
		else
		{
			//melee
			// Manual Attack
			plugin.debug("Set Target melee");
			meleeTarget = theEntity;
			projectileTarget = null;
			if (myNPC.getNavigator().getEntityTarget() != null && myNPC.getNavigator().getEntityTarget().getTarget() == theEntity) return; //already attacking this, dummy.

			if (!myNPC.getDefaultGoalController().isPaused()) 
				myNPC.getDefaultGoalController().setPaused(true);

			myNPC.getNavigator().setTarget(theEntity, true);
			myNPC.getNavigator().getLocalParameters().speedModifier(getSpeed());
			myNPC.getNavigator().getLocalParameters().stuckAction(giveup);
			myNPC.getNavigator().getLocalParameters().stationaryTicks(5*20);
		}
	}

}
