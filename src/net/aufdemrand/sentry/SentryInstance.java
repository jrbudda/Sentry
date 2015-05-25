package net.aufdemrand.sentry;

import java.text.DecimalFormat;
import java.util.*;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;

//Version Specifics
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityPotion;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
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
import org.bukkit.entity.Horse;
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
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
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

	public boolean KillsDropInventory = true;
	public boolean DropInventory = false;
	public boolean Targetable = true;

	public int MountID = -1;

	public boolean isMounted(){
		return MountID >=0;
	}

	public int epcount = 0;
	private GiveUpStuckAction giveup = new GiveUpStuckAction(this);

	public String GreetingMessage = "&a<NPC> says: Welcome, <PLAYER>!";
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
	public double sentryHealth = 20;
    public boolean IgnoreLOS;

	public Integer sentryRange = 10;

	public float sentrySpeed =  (float) 1.0;

	/* Internals */
	public Status sentryStatus = Status.isDYING;

	public Double sentryWeight = 1.0;

	public Location Spawn = null;

	public Integer Strength = 1;

	/* Technicals */
	private Integer taskID = null;

	public int FollowDistance  = 16 ;

	public String WarningMessage = "&a<NPC> says: Halt! Come no further!";

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

	public boolean isIgnored(LivingEntity aTarget){
		//cheak ignores

		if(aTarget == this.guardEntity) return true;

		if(ignores == 0) return false;

		if (hasIgnoreType(all)) return true;

		if (aTarget instanceof Player && !net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget)) {

			if (hasIgnoreType(players)) return true;

			else{
				String name = ((Player) aTarget).getName();

				if ( this.hasIgnoreType(namedplayers) && containsIgnore("PLAYER:" + name)) 	return true;

				if ( this.hasIgnoreType(owner)  && name.equalsIgnoreCase(myNPC.getTrait(Owner.class).getOwner()))		return true;

				else if(this.hasIgnoreType(groups)) {

					String[] groups1 = plugin.perms.getPlayerGroups(aTarget.getWorld(),name); // world perms
					String[] groups2 = plugin.perms.getPlayerGroups((World)null,name); //global perms
					//		String[] groups3 = plugin.perms.getPlayerGroups(aTarget.getWorld().getName(),name); // world perms
					//	String[] groups4 = plugin.perms.getPlayerGroups((Player)aTarget); // world perms


					if (groups1 !=null){
						for (int i = 0; i < groups1.length; i++) {
							//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found world1 group " + groups1[i] + " on " + name);
							if (this.containsIgnore("GROUP:" + groups1[i]))	return true;
						}
					}

					if ( groups2 !=null){
						for (int i = 0; i < groups2.length; i++) {
							//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found global group " + groups2[i] + " on " + name);
							if (this.containsIgnore("GROUP:" + groups2[i]))		return true;
						}
					}
				}

				if(this.hasIgnoreType(towny)) {
					String[] info = plugin.getResidentTownyInfo((Player)aTarget);

					if (info[1]!=null) {
						if (this.containsIgnore("TOWN:" + info[1]))	return true;
					}

					if (info[0]!=null) {
						if (this.containsIgnore("NATION:" + info[0]))	return true;
					}
				}

				if( this.hasIgnoreType(faction) ) {
					String faction = FactionsUtil.getFactionsTag((Player)aTarget);
					//	plugin.getLogger().info(faction);
					if (faction !=null) {
						if (this.containsIgnore("FACTION:" + faction))	return true;
					}
				}
				if( this.hasIgnoreType(war) ) {
					String team = plugin.getWarTeam((Player)aTarget);
					//	plugin.getLogger().info(faction);
					if (team !=null) {
						if (this.containsIgnore("WARTEAM:" + team))	return true;
					}
				}
				if( this.hasIgnoreType(mcTeams) ) {
					String team = plugin.getMCTeamName((Player)aTarget);
					//	plugin.getLogger().info(faction);
					if (team !=null) {
						if (this.containsIgnore("TEAM:" + team))	return true;
					}
				}
				if( this.hasIgnoreType(clans) ) {
					String clan = plugin.getClan((Player)aTarget);
					//	plugin.getLogger().info(faction);
					if (clan !=null) {
						if (this.containsIgnore("CLAN:" + clan))	return true;
					}
				}
			}
		}

		else if(net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget)){

			if (this.hasIgnoreType(npcs)) {
				return true;
			}

			NPC npc =  net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(aTarget);

			if (npc !=null) {

				String name =npc.getName();

				if (this.hasIgnoreType(namednpcs) && this.containsIgnore("NPC:" + name)) 	return true;

				else if(hasIgnoreType(groups)) {

					String[] groups1 = plugin.perms.getPlayerGroups(aTarget.getWorld(),name); // world perms
					String[] groups2 = plugin.perms.getPlayerGroups((World)null,name); //global perms

					if (groups1 !=null){
						for (int i = 0; i < groups1.length; i++) {
							//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found world1 group " + groups1[i] + " on " + name);
							if (this.containsIgnore("GROUP:" + groups1[i]))	return true;
						}
					}

					if ( groups2 !=null){
						for (int i = 0; i < groups2.length; i++) {
							//	plugin.getLogger().log(java.util.logging.Level.INFO , myNPC.getName() + "  found global group " + groups2[i] + " on " + name);
							if (this.containsIgnore("GROUP:" + groups2[i]))		return true;
						}
					}
				}
			}
		}


		else if (aTarget instanceof Monster && hasIgnoreType(monsters)) return true;

		else if (aTarget instanceof LivingEntity && hasIgnoreType(namedentities)) {
			if (this.containsIgnore("ENTITY:" + aTarget.getType()))	return true;
		}


		//not ignored, ok!
		return false;
	}

	public boolean isTarget(LivingEntity aTarget){

		if (targets == 0 || targets == events) return false;

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

				if(this.hasTargetType(faction) || this.hasTargetType(factionenemies) ) {
					if (Sentry.FactionsActive){
						String faction = FactionsUtil.getFactionsTag((Player)aTarget);

						if (faction !=null) {
							if (this.containsTarget("FACTION:" + faction))return true;

							if(this.hasTargetType(factionenemies)){
								for (String s : FactionEnemies) {
									if (FactionsUtil.isFactionEnemy( getMyEntity().getWorld().getName(),s, faction)) return true;
								}
							}
						}
					}
				}

				if(this.hasTargetType(war) ) {
					String team = plugin.getWarTeam((Player)aTarget);
					//	plugin.getLogger().info(faction);
					if (team !=null) {
						if (this.containsTarget("WARTEAM:" + team))	return true;
					}
				}
				if( this.hasTargetType(mcTeams) ) {
					String team = plugin.getMCTeamName((Player)aTarget);
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

	public void die(boolean runscripts, org.bukkit.event.entity.EntityDamageEvent.DamageCause cause){
		if (sentryStatus == Status.isDYING || sentryStatus == Status.isDEAD || getMyEntity() instanceof LivingEntity == false) return;

		sentryStatus = Status.isDYING;

		setTarget(null, false);
		//		myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);


		boolean handled = false;

		if(runscripts && plugin.DenizenActive){
			handled = DenizenHook.SentryDeath(_myDamamgers, myNPC);
		}
		if(handled) return;

		if (plugin.DenizenActive){
			try {
				Entity killer = getMyEntity().getKiller();
				if(killer ==null){
					//might have been a projectile.
					EntityDamageEvent ev = getMyEntity().getLastDamageCause();
					if(ev !=null && ev instanceof EntityDamageByEntityEvent){
						killer = ((EntityDamageByEntityEvent)ev).getDamager();
					}
				}

				DenizenHook.DenizenAction(myNPC, "death", null);
				DenizenHook.DenizenAction(myNPC, "death by" + cause.toString().replace(" " ,"_"), null);



				if(killer !=null){

					if(killer instanceof Projectile && ((Projectile) killer).getShooter() != null
                            && ((Projectile)killer).getShooter() instanceof Entity)
                        killer = (Entity) ((Projectile) killer).getShooter();

					plugin.debug("Running Denizen actions for " + myNPC.getName() + " with killer: " + killer.toString());

					if(killer instanceof org.bukkit.OfflinePlayer){
						DenizenHook.DenizenAction(myNPC, "death by player", (org.bukkit.OfflinePlayer) killer);
					}
					else {
						DenizenHook.DenizenAction(myNPC, "death by entity", null);
						DenizenHook.DenizenAction(myNPC, "death by " + killer.getType().toString(), null);
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}


		sentryStatus = Status.isDEAD;

		if (this.DropInventory)  getMyEntity().getLocation().getWorld().spawn(getMyEntity().getLocation(), ExperienceOrb.class).setExperience(plugin.SentryEXP);


		List<ItemStack> items = new java.util.LinkedList<ItemStack>();

		if (getMyEntity() instanceof HumanEntity) {
			//get drop inventory.
			for( ItemStack is:	((HumanEntity) getMyEntity()).getInventory().getArmorContents()){
				if (is.getTypeId()>0)	items.add(is);
			}

			ItemStack is = ((HumanEntity) getMyEntity()).getInventory().getItemInHand();
			if (is.getTypeId()>0)	items.add(is);

			((HumanEntity) getMyEntity()).getInventory().clear();
			((HumanEntity) getMyEntity()).getInventory().setArmorContents(null);
			((HumanEntity) getMyEntity()).getInventory().setItemInHand(null);
		}

		if(items.isEmpty())	getMyEntity().playEffect(EntityEffect.DEATH);
		else getMyEntity().playEffect(EntityEffect.HURT);

		if (!DropInventory) items.clear();

		for (ItemStack is : items){
			getMyEntity().getWorld().dropItemNaturally(getMyEntity().getLocation(), is);
		}


		if (plugin.DieLikePlayers){
			//die!


			((LivingEntity)getMyEntity()).setHealth(0);

		}
		else{
			org.bukkit.event.entity.EntityDeathEvent ed = new org.bukkit.event.entity.EntityDeathEvent((LivingEntity) getMyEntity(), items);

			plugin.getServer().getPluginManager().callEvent(ed);
			//citizens will despawn it.

		}




		if (RespawnDelaySeconds == -1) {
			cancelRunnable();
			if(this.isMounted())	Util.removeMount(MountID);
			myNPC.destroy();
			return;
		} else {
			isRespawnable = System.currentTimeMillis() + RespawnDelaySeconds * 1000;
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
		net.citizensnpcs.util.NMS.look(getMyEntity(), getMyEntity().getLocation().getYaw(), 0);
	}

	private void faceAlignWithVehicle(){
		org.bukkit.entity.Entity v = getMyEntity().getVehicle();
		net.citizensnpcs.util.NMS.look((LivingEntity) getMyEntity(), v.getLocation().getYaw(), 0);
	}

	public LivingEntity findTarget(Integer Range) {
		Range+=WarningRange;
		List<Entity> EntitiesWithinRange = getMyEntity().getNearbyEntities(Range, Range, Range);
		LivingEntity theTarget = null;
		Double distanceToBeat = 99999.0;

		// plugin.getServer().broadcastMessage("Targets scanned : " +
		// EntitiesWithinRange.toString());

		for (Entity aTarget : EntitiesWithinRange) {
			if (!(aTarget instanceof LivingEntity)) continue;

			// find closest target

			if (!isIgnored((LivingEntity) aTarget) && isTarget((LivingEntity) aTarget)) {

				// can i see it?
				// too dark?
				double ll = aTarget.getLocation().getBlock().getLightLevel();
				// sneaking cut light in half
				if (aTarget instanceof Player)
					if (((Player) aTarget).isSneaking())
						ll /= 2;

				// too dark?
				if (ll >= (16 - this.NightVision)) {


					double dist = aTarget.getLocation().distance(getMyEntity().getLocation());

					if (hasLOS(aTarget)) {


						if (WarningRange >0 && sentryStatus == Status.isLOOKING && aTarget instanceof Player &&  dist > (Range - WarningRange) && !net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(aTarget) & !(WarningMessage.isEmpty())){

							if (Warnings.containsKey(aTarget) && System.currentTimeMillis() < Warnings.get(aTarget) + 60*1000){
								//already warned u in last 30 seconds.
							}
							else{
								((Player)aTarget).sendMessage(getWarningMessage((Player) aTarget));
								if(!getNavigator().isNavigating())	faceEntity(getMyEntity(), aTarget);
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
					boolean LOS = getMyEntity().hasLineOfSight(aTarget);
					if (LOS) {
						if (Warnings.containsKey(aTarget) && System.currentTimeMillis() < Warnings.get(aTarget) + 60*1000){
							//already greeted u in last 30 seconds.
						}
						else{
							((Player)aTarget).sendMessage(getGreetingMEssage((Player) aTarget));
							faceEntity(getMyEntity(), aTarget);
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
		((CraftLivingEntity)(getMyEntity())).getHandle().b(on); // TODO: 1.8 UPDATE - IS THIS CORRECT?
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
		Location loc = Util.getFireSource(getMyEntity(), theEntity);

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

		if (!hasLOS(theEntity)) {
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
			// =getMyEntity().launchProjectile(myProjectile);

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
				theEntity.damage((double)getStrength(), getMyEntity());
			}
			else if (lightninglevel == 3){
				to.getWorld().strikeLightningEffect(to);
				theEntity.setHealth((double)0);
			}
		}
		else
		{

			Projectile theArrow = null;


			if(myProjectile == org.bukkit.entity.ThrownPotion.class){
				net.minecraft.server.v1_8_R3.World nmsWorld = ((CraftWorld)getMyEntity().getWorld()).getHandle();
				EntityPotion ent = new EntityPotion(nmsWorld, loc.getX(), loc.getY(), loc.getZ(), CraftItemStack.asNMSCopy(potiontype));
				nmsWorld.addEntity(ent);
				theArrow = (Projectile) ent.getBukkitEntity();

			}

			else if(myProjectile == org.bukkit.entity.EnderPearl.class){
				theArrow = getMyEntity().launchProjectile(myProjectile);
			}

			else{
				theArrow = getMyEntity().getWorld().spawn(loc, myProjectile);
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
			theArrow.setShooter(getMyEntity());
			theArrow.setVelocity(victor);
		}

		// OK we're shooting
		// go twang
		if (effect != null)
			getMyEntity().getWorld().playEffect(getMyEntity().getLocation(), effect, null);

		if (myProjectile == Arrow.class){
			Draw(false);
		}
		else {
			if(getMyEntity() instanceof org.bukkit.entity.Player)	{
				net.citizensnpcs.util.PlayerAnimation.ARM_SWING.play((Player) getMyEntity(), 64);
			}
		}




	}


	public int getArmor(){

		double mod = 0;
		if ( getMyEntity() instanceof Player){
			for (ItemStack is:((Player)getMyEntity()).getInventory().getArmorContents()){
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

	public double getHealth(){
		if (myNPC == null) return 0;
		if (getMyEntity() == null) return 0;
		return  ((CraftLivingEntity)getMyEntity()).getHealth();
	}

	public float getSpeed(){
		if(myNPC.isSpawned() == false) return sentrySpeed;
		double mod = 0;
		if ( getMyEntity() instanceof Player){
			for (ItemStack is:((Player)getMyEntity()).getInventory().getArmorContents()){
				if (plugin.SpeedBuffs.containsKey(is.getTypeId())) mod += plugin.SpeedBuffs.get(is.getTypeId());
			}
		}
		return (float) (sentrySpeed + mod) * (this.getMyEntity().isInsideVehicle() ? 2 : 1);
	}
	public String getStats() {
		DecimalFormat df = new DecimalFormat("#.0");
		double h = getHealth();

		return ChatColor.RED + "[HP]:" + ChatColor.WHITE + h + "/" + sentryHealth + ChatColor.RED + " [AP]:" + ChatColor.WHITE + getArmor() +
				ChatColor.RED + " [STR]:" + ChatColor.WHITE + getStrength() + ChatColor.RED + " [SPD]:" + ChatColor.WHITE + df.format(getSpeed()) +
				ChatColor.RED + " [RNG]:" + ChatColor.WHITE + sentryRange + ChatColor.RED + " [ATK]:" + ChatColor.WHITE + AttackRateSeconds + ChatColor.RED + " [VIS]:" + ChatColor.WHITE + NightVision +
				ChatColor.RED + " [HEAL]:" + ChatColor.WHITE + HealRate + ChatColor.RED + " [WARN]:" + ChatColor.WHITE + WarningRange + ChatColor.RED + " [FOL]:" + ChatColor.WHITE + Math.sqrt(FollowDistance);

	}

	public int getStrength(){
		double mod = 0;

		if ( getMyEntity() instanceof Player){
			if (plugin.StrengthBuffs.containsKey(((Player)getMyEntity()).getInventory().getItemInHand().getTypeId())) mod += plugin.StrengthBuffs.get(((Player)getMyEntity()).getInventory().getItemInHand().getTypeId());
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

		if (Spawn == null) {
			Spawn = getMyEntity().getLocation();
		}


		if(plugin.DenizenActive){
			if (myNPC.hasTrait(net.aufdemrand.denizen.npc.traits.HealthTrait.class)) myNPC.removeTrait(net.aufdemrand.denizen.npc.traits.HealthTrait.class);
		}

		//disable citizens respawning. Cause Sentry doesnt always raise EntityDeath
		myNPC.data().set("respawn-delay",-1);

		setHealth(sentryHealth);

		_myDamamgers.clear();

		this.sentryStatus = Status.isLOOKING;
		faceForward();

		healanim = new PacketPlayOutAnimation( ((CraftEntity)getMyEntity()).getHandle(), 6);

		//	Packet derp = new net.minecraft.server.Packet15Place();

		if (guardTarget == null){
			myNPC.teleport(Spawn,TeleportCause.PLUGIN); //it should be there... but maybe not if the position was saved elsewhere.
		}

		float pf = myNPC.getNavigator().getDefaultParameters().range();

		if(pf < sentryRange+5){
			pf=sentryRange+5;
		}

		myNPC.data().set(NPC.DEFAULT_PROTECTED_METADATA, false);
		myNPC.data().set(NPC.TARGETABLE_METADATA, this.Targetable);


		myNPC.getNavigator().getDefaultParameters().range(pf);
		myNPC.getNavigator().getDefaultParameters().stationaryTicks(5*20);
		myNPC.getNavigator().getDefaultParameters().useNewPathfinder(false);
		//	myNPC.getNavigator().getDefaultParameters().stuckAction(new BodyguardTeleportStuckAction(this, this.plugin));

		// plugin.getServer().broadcastMessage("NPC GUARDING!");

		if (getMyEntity() instanceof org.bukkit.entity.Creeper){
			myNPC.getNavigator().getDefaultParameters().attackStrategy(new CreeperAttackStrategy());
		}
		else if (getMyEntity() instanceof org.bukkit.entity.Spider){
			myNPC.getNavigator().getDefaultParameters().attackStrategy(new SpiderAttackStrategy(plugin));
		}

		processTargets();

		if (taskID == null) {
			taskID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new SentryLogic(), 40 + this.myNPC.getId(),  plugin.LogicTicks);
		}

		mountCreated = false;
	}

	private boolean mountCreated = false;

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

		if(sentryStatus == Status.isDYING) return;

		if (myNPC == null || !myNPC.isSpawned()) {
			// \\how did you get here?
			return;
		}

		if (guardTarget != null && guardEntity == null) return; //dont take damage when bodyguard target isnt around.

		if (System.currentTimeMillis() <  okToTakedamage + 500) return;
		okToTakedamage = System.currentTimeMillis();

		event.getEntity().setLastDamageCause(event);

		NPC npc = myNPC;

		LivingEntity attacker = null;

		hittype hit = hittype.normal;

		double finaldamage = event.getDamage();

		// Find the attacker
		if (event.getDamager() instanceof Projectile) {
			if (((Projectile) event.getDamager()).getShooter() instanceof LivingEntity) {
				attacker = (LivingEntity) ((Projectile) event.getDamager()).getShooter();
			}
		} else if (event.getDamager() instanceof LivingEntity) {
			attacker = (LivingEntity) event.getDamager();
		}

		if (Invincible)
			return;



		if(plugin.IgnoreListInvincibility ){
			if(isIgnored(attacker)) return;
		}

		// can i kill it? lets go kill it.
		if (attacker != null) {
			if (this.Retaliate) {
				if ( !(event.getDamager() instanceof Projectile) || (net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(attacker) == null)) {
					// only retaliate to players or non-projectlies. Prevents stray sentry arrows from causing retaliation.

					setTarget(attacker, true);

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

			finaldamage = Math.round(damagemodifer);
		}

		int arm = getArmor();

		if (finaldamage > 0) {

			if (attacker != null) {
				// knockback
				npc.getEntity().setVelocity( attacker.getLocation().getDirection().multiply(1.0 / (sentryWeight + (arm/5))));
			}

			// Apply armor
			finaldamage -= arm;

			// there was damamge before armor.
			if (finaldamage <= 0){
				npc.getEntity().getWorld().playEffect(npc.getEntity().getLocation(), org.bukkit.Effect.ZOMBIE_CHEW_IRON_DOOR,1);
				hit = hittype.block;
			}
		}

		if (attacker instanceof Player && !net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(attacker)) {

			_myDamamgers.add((Player) attacker);
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
				((Player) attacker).sendMessage(Util.format(msg, npc, (CommandSender) attacker, ((Player) attacker).getItemInHand().getTypeId(), finaldamage+""));
			}
		}

		if (finaldamage > 0) {
			npc.getEntity().playEffect(EntityEffect.HURT);

			// is he dead?
			if (getHealth() - finaldamage <= 0) {

				//set the killer
				if (event.getDamager() instanceof HumanEntity) 	((CraftLivingEntity)getMyEntity()).getHandle().killer = (EntityHuman) ((CraftLivingEntity) event.getDamager()).getHandle();

				die(true, event.getCause());

			}
			else 	getMyEntity().damage(finaldamage);
		}
	}

	Random R = new Random();

	public void onEnvironmentDamae(EntityDamageEvent event){

		if(sentryStatus == Status.isDYING) return;

		if (!myNPC.isSpawned() || Invincible) {
			return;
		}

		if (guardTarget != null && guardEntity == null) return; //dont take damage when bodyguard target isnt around.

		if (System.currentTimeMillis() <  okToTakedamage + 500) return;
		okToTakedamage = System.currentTimeMillis();

		getMyEntity().setLastDamageCause(event);

		double finaldamage = event.getDamage();

		if (event.getCause() == DamageCause.CONTACT || event.getCause() == DamageCause.BLOCK_EXPLOSION){
			finaldamage -= getArmor();
		}

		if (finaldamage > 0 ){
			getMyEntity().playEffect(EntityEffect.HURT);

			if (event.getCause() == DamageCause.FIRE){
				if (!getNavigator().isNavigating()){
					getNavigator().setTarget(getMyEntity().getLocation().add(R.nextInt(2)-1, 0, R.nextInt(2)-1));
				}
			}

			if (getHealth() - finaldamage <= 0) {

				die(true, event.getCause());

				// plugin.getServer().broadcastMessage("Dead!");
			}
			else {
				getMyEntity().damage(finaldamage);

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
	final int factionenemies = 16384*2;
	final int mcTeams = 16384*4;

	private int targets = 0;
	private int ignores = 0;

	List<String> NationsEnemies = new ArrayList<String>();
	List<String> FactionEnemies = new ArrayList<String>();

	public void processTargets(){
		try {

			targets = 0;
			ignores = 0;
			_ignoreTargets.clear();
			_validTargets.clear();
			NationsEnemies.clear();
			FactionEnemies.clear();

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
					else	if (Sentry.FactionsActive && t.contains("FACTION:")) targets |= faction;
					else	if (Sentry.FactionsActive && t.contains("FACTIONENEMIES:")){
						targets |= factionenemies;
						FactionEnemies.add(t.split(":")[1]);
					}
					else	if (plugin.TownyActive && t.contains("TOWN:")) targets |= towny;
					else	if (plugin.TownyActive && t.contains("NATIONENEMIES:")) {
						targets |= townyenemies;
						NationsEnemies.add(t.split(":")[1]);
					}
					else	if (plugin.TownyActive && t.contains("NATION:"))  targets |= towny;
					else	if (plugin.WarActive && t.contains("WARTEAM:"))  targets |= war;
					else	if (t.contains("TEAM:"))  targets |= mcTeams;
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
					else	if (Sentry.FactionsActive && t.contains("FACTION:")) ignores |= faction;
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
			if (getMyEntity() == null ) sentryStatus = Status.isDEAD; // incase it dies in a way im not handling.....

			if (UpdateWeapon()){
				//ranged
				if(meleeTarget !=null) {
					plugin.debug(myNPC.getName() + " Switched to ranged");
					LivingEntity derp = meleeTarget;
					boolean ret = sentryStatus == Status.isRETALIATING;
					setTarget(null, false);
					setTarget(derp, ret);
				}
			}
			else{
				//melee
				if(projectileTarget != null) {
					plugin.debug(myNPC.getName() + " Switched to melee");
					boolean ret = sentryStatus == Status.isRETALIATING;
					LivingEntity derp = projectileTarget;
					setTarget(null, false);
					setTarget(derp, ret);
				}
			}

			if (sentryStatus != Status.isDEAD &&  HealRate > 0) {
				if(System.currentTimeMillis() > oktoheal ){
					if (getHealth() < sentryHealth && sentryStatus !=  Status.isDEAD && sentryStatus != Status.isDYING) {
						double heal = 1;
						if (HealRate <0.5) heal = (0.5 / HealRate);


						setHealth(getHealth() + heal);


						if (healanim!=null)net.citizensnpcs.util.NMS.sendPacketsNearby(null, getMyEntity().getLocation(),healanim);

						if (getHealth() >= sentryHealth) _myDamamgers.clear(); //healed to full, forget attackers

					}
					oktoheal = (long) (System.currentTimeMillis() + HealRate * 1000);
				}

			}

			if(myNPC.isSpawned() && getMyEntity().isInsideVehicle() == false && isMounted() && isMyChunkLoaded()) mount();

			if (sentryStatus == Status.isDEAD && System.currentTimeMillis() > isRespawnable && RespawnDelaySeconds > 0 & Spawn.getWorld().isChunkLoaded(Spawn.getBlockX()>>4, Spawn.getBlockZ()>>4)) {
				// Respawn

				plugin.debug("respawning" + myNPC.getName());
				if (guardEntity == null) {
					myNPC.spawn(Spawn.clone());
					//	myNPC.teleport(Spawn,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
				} else {
					myNPC.spawn(guardEntity.getLocation().add(2, 0, 2));
					//	myNPC.teleport(guardEntity.getLocation().add(2, 0, 2),org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN);
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

				if (projectileTarget != null && !projectileTarget.isDead() && projectileTarget.getWorld() == getMyEntity().getLocation().getWorld() ) {
					if (_projTargetLostLoc == null)
						_projTargetLostLoc = projectileTarget.getLocation();

					if (!getNavigator().isNavigating())	faceEntity(getMyEntity(), projectileTarget);

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

					if (isMounted()) faceEntity(getMyEntity(), meleeTarget);

					if (meleeTarget.getWorld() == getMyEntity().getLocation().getWorld()) {
						double dist=  meleeTarget.getLocation().distance(getMyEntity().getLocation());
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

				if(getMyEntity().isInsideVehicle() == true) faceAlignWithVehicle(); //sync the rider with the vehicle.


				if (guardEntity instanceof Player){
					if (((Player)guardEntity).isOnline() == false){
						guardEntity = null;
					}
				}

				if (guardTarget != null && guardEntity == null) {
					// daddy? where are u?
					setGuardTarget(guardTarget, false);
				}

				if (guardTarget != null && guardEntity == null) {
					// daddy? where are u?
					setGuardTarget(guardTarget, true);
				}

				if (guardEntity !=null){

					Location npcLoc = getMyEntity().getLocation();

					if (guardEntity.getLocation().getWorld() != npcLoc.getWorld() || !isMyChunkLoaded()){
						if(Util.CanWarp(guardEntity, myNPC)){
							myNPC.despawn();
							myNPC.spawn((guardEntity.getLocation().add(1, 0, 1)));
						}
						else {
							((Player) guardEntity).sendMessage(myNPC.getName() + " cannot follow you to " + guardEntity.getWorld().getName());
							guardEntity = null;
						}

					}
					else{
						double dist = npcLoc.distanceSquared(guardEntity.getLocation());
						plugin.debug(myNPC.getName() + dist + getNavigator().isNavigating() + " " +getNavigator().getEntityTarget() + " " );
						if(dist > 1024) {
							myNPC.teleport(guardEntity.getLocation().add(1,0,1),TeleportCause.PLUGIN);
						}
						else if(dist > FollowDistance && !getNavigator().isNavigating()) {
							getNavigator().setTarget((Entity)guardEntity, false);
							getNavigator().getLocalParameters().stationaryTicks(3*20);
						}
						else if (dist < FollowDistance && getNavigator().isNavigating()) {
							getNavigator().cancelNavigation();
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
		if (getMyEntity() == null) return false;
		Location npcLoc = getMyEntity().getLocation();
		return npcLoc.getWorld().isChunkLoaded(npcLoc.getBlockX()>>4, npcLoc.getBlockZ()>>4);
	}

	public boolean setGuardTarget(String name, boolean forcePlayer) {

		if (myNPC == null)
			return false;

		if (name == null) {
			guardEntity = null;
			guardTarget = null;
			setTarget(null, false);// clear active hostile target
			return true;
		}

		if (!forcePlayer){

			List<Entity> EntitiesWithinRange = getMyEntity().getNearbyEntities(sentryRange, sentryRange, sentryRange);

			for (Entity aTarget : EntitiesWithinRange) {

				if (aTarget instanceof Player) {
					//chesk for players
					if (((Player) aTarget).getName().equals(name)) {
						guardEntity = (LivingEntity) aTarget;
						guardTarget = ((Player) aTarget).getName();
						setTarget(null, false); // clear active hostile target
						return true;
					}
				}
				else if (aTarget instanceof LivingEntity) {
					//check for named mobs.
					String ename = ((LivingEntity) aTarget).getCustomName();
					if (ename !=null && ename.equals(name)) {
						guardEntity = (LivingEntity) aTarget;
						guardTarget = ename;
						setTarget(null, false); // clear active hostile target
						return true;
					}
				}

			}
		}
		else {

			for (Player player : plugin.getServer().getOnlinePlayers()) {
				if (player.getName().equals(name)) {
					guardEntity = (LivingEntity) player;
					guardTarget = player.getName();
					setTarget(null, false); // clear active hostile target
					return true;
				}

			}

		}



		return false;

	}

	public void setHealth(double health){
		if (myNPC == null) return;
		if (getMyEntity() == null) return;
		if (((CraftLivingEntity)getMyEntity()).getMaxHealth() != sentryHealth)
			getMyEntity().setMaxHealth(sentryHealth);
		if(health > sentryHealth) health = sentryHealth;

		getMyEntity().setHealth(health);
	}


	public boolean UpdateWeapon(){
		int weapon = 0;

		ItemStack is = null;

		if (getMyEntity() instanceof HumanEntity) {
			is = ((HumanEntity) getMyEntity()).getInventory().getItemInHand();
			weapon = is.getTypeId();
			if(	weapon != plugin.witchdoctor) is.setDurability((short) 0);
		}

		lightning = false;
		lightninglevel = 0;
		inciendary = false;
		potionEffects = plugin.WeaponEffects.get(weapon);

		myProjectile = null;

		if(weapon == plugin.archer || getMyEntity() instanceof org.bukkit.entity.Skeleton){
			myProjectile = org.bukkit.entity.Arrow.class;
		}
		else if(weapon ==  plugin.pyro3 || getMyEntity() instanceof org.bukkit.entity.Ghast){
			myProjectile = org.bukkit.entity.Fireball.class;
		}
		else if(weapon ==  plugin.pyro2 || getMyEntity() instanceof org.bukkit.entity.Blaze || getMyEntity() instanceof org.bukkit.entity.EnderDragon){
			myProjectile = org.bukkit.entity.SmallFireball.class;
			inciendary = true;
		}
		else if(weapon ==  plugin.pyro1){
			myProjectile = org.bukkit.entity.SmallFireball.class;
			inciendary =false;
		}
		else if(weapon == plugin.magi || getMyEntity() instanceof org.bukkit.entity.Snowman){
			myProjectile = org.bukkit.entity.Snowball.class;
		}
		else if(weapon == plugin.warlock1){
			myProjectile = org.bukkit.entity.EnderPearl.class;
		}
		else if(weapon == plugin.warlock2 || getMyEntity() instanceof org.bukkit.entity.Wither){
			myProjectile = org.bukkit.entity.WitherSkull.class;
		}
		else if(weapon == plugin.warlock3){
			myProjectile = org.bukkit.entity.WitherSkull.class;
		}
		else if(weapon == plugin.bombardier){
			myProjectile = org.bukkit.entity.Egg.class;
		}
		else if(weapon == plugin.witchdoctor || getMyEntity() instanceof org.bukkit.entity.Witch ){
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
			return false; //melee
		}

		return true; //ranged
	}
	public void setTarget(LivingEntity theEntity, boolean isretaliation) {

		if (getMyEntity() == null ) return;

		if (theEntity == getMyEntity()) return; //I don't care how you got here. No. just No.

		if (guardTarget != null && guardEntity == null) theEntity =null; //dont go aggro when bodyguard target isnt around.

		if (theEntity == null) {
			plugin.debug(myNPC.getName() + "- Set Target Null");
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

				getGoalController().setPaused(true);
				//	if (!myNPC.getTrait(Waypoints.class).getCurrentProvider().isPaused())  myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);

				if (getNavigator().getEntityTarget() == null || (getNavigator().getEntityTarget() != null && getNavigator().getEntityTarget().getTarget() != guardEntity)){

					if (guardEntity.getLocation().getWorld() != getMyEntity().getLocation().getWorld()){
						myNPC.despawn();
						myNPC.spawn((guardEntity.getLocation().add(1, 0, 1)));
						return;
					}

					getNavigator().setTarget((Entity)guardEntity, false);
					//		myNPC.getNavigator().getLocalParameters().stuckAction(bgteleport);
					getNavigator().getLocalParameters().stationaryTicks(3*20);
				}
			} else {
				//not a guard
				getNavigator().cancelNavigation();

				faceForward();

				if (getGoalController().isPaused())
					getGoalController().setPaused(false);
			}
			return;
		}

		if (theEntity == guardEntity)
			return; // dont attack my dude.

		if (isretaliation) sentryStatus = Status.isRETALIATING;
		else sentryStatus = Status.isHOSTILE;


		if(!getNavigator().isNavigating()) faceEntity(getMyEntity(), theEntity);

		if(UpdateWeapon()){
			//ranged
			plugin.debug(myNPC.getName() + "- Set Target projectile");
			projectileTarget = theEntity;
			meleeTarget = null;
		}
		else
		{
			//melee
			// Manual Attack
			plugin.debug(myNPC.getName() + "- Set Target melee");
			meleeTarget = theEntity;
			projectileTarget = null;
			if (getNavigator().getEntityTarget() != null && getNavigator().getEntityTarget().getTarget() == theEntity) return; //already attacking this, dummy.
			if (!getGoalController().isPaused())
				getGoalController().setPaused(true);
			getNavigator().setTarget((Entity)theEntity, true);
			getNavigator().getLocalParameters().speedModifier(getSpeed());
			getNavigator().getLocalParameters().stuckAction(giveup);
			getNavigator().getLocalParameters().stationaryTicks(5*20);
		}
	}

	protected net.citizensnpcs.api.ai.Navigator getNavigator(){
		NPC npc = getMountNPC();
		if (npc == null || npc.isSpawned() == false) npc = myNPC;
		return npc.getNavigator();
	}

	protected net.citizensnpcs.api.ai.GoalController getGoalController(){
		NPC npc = getMountNPC();
		if (npc == null || npc.isSpawned() == false) npc = myNPC;
		return npc.getDefaultGoalController();
	}

	public void dismount(){
		//get off and despawn the horse.
		if (myNPC.isSpawned()){
			if (getMyEntity().isInsideVehicle()){
				NPC n = getMountNPC();
				if (n!=null){
					getMyEntity().getVehicle().setPassenger(null);
					n.despawn(net.citizensnpcs.api.event.DespawnReason.PLUGIN);
				}
			}
		}
	}

	public void mount(){
		if (myNPC.isSpawned()){
			if (getMyEntity().isInsideVehicle()) getMyEntity().getVehicle().setPassenger(null);
			NPC n = getMountNPC();

			if(n == null || (!n.isSpawned() && !mountCreated)) {
				n = createMount();
			}

			if (n!=null){
				mountCreated = true;
				if(n.isSpawned() == false) return; //dead mount
				n.data().set(NPC.DEFAULT_PROTECTED_METADATA, false);
				n.getNavigator().getDefaultParameters().attackStrategy(new MountAttackStrategy());
				n.getNavigator().getDefaultParameters().useNewPathfinder(false);
				n.getNavigator().getDefaultParameters().speedModifier(myNPC.getNavigator().getDefaultParameters().speedModifier()*2);
				n.getNavigator().getDefaultParameters().range(myNPC.getNavigator().getDefaultParameters().range() + 5);
				((CraftLivingEntity) n.getEntity()).setCustomNameVisible(false);
				n.getEntity().setPassenger(null);
				n.getEntity().setPassenger(getMyEntity());
			}
			else this.MountID = -1;

		}
	}

	public  NPC createMount(){
		plugin.debug("Creating mount for " + this.myNPC.getName());

		if (myNPC.isSpawned()){

			NPC horseNPC = null;

			if (isMounted()) {
				horseNPC =	CitizensAPI.getNPCRegistry().getById(MountID);

				if(horseNPC !=null){
					horseNPC.despawn();
				}
				else {
					plugin.getServer().getLogger().info("Cannot find mount NPC " + MountID);
				}
			}

			else {
				horseNPC =	net.citizensnpcs.api.CitizensAPI.getNPCRegistry().createNPC(org.bukkit.entity.EntityType.HORSE, myNPC.getName() + "_Mount");
				horseNPC.getTrait(MobType.class).setType(org.bukkit.entity.EntityType.HORSE);
			}

			if(horseNPC == null){
				plugin.getServer().getLogger().info("Cannot create mount NPC!");
			}

			if(getMyEntity() == null){
				plugin.getServer().getLogger().info("why is this spawned but bukkit entity is null???");
			}

			//look at my horse, my horse is amazing.
			horseNPC.spawn(getMyEntity().getLocation());
			Owner o = horseNPC.getTrait(Owner.class);
			o.setOwner(myNPC.getTrait(Owner.class).getOwner());
			//cant do this is screws up the pathfinding.
			((Horse)horseNPC.getEntity()).getInventory().setSaddle(new ItemStack(org.bukkit.Material.SADDLE));

			this.MountID = horseNPC.getId();

			return horseNPC;

		}

		return null;
	}

	public boolean hasLOS(Entity other){
		if (!myNPC.isSpawned()) return false;
        if (IgnoreLOS) return true;
		return getMyEntity().hasLineOfSight(other);
	}

	public LivingEntity getMyEntity() {
		if (myNPC == null) return null;
		if (myNPC.getEntity() == null) return null;
		if (myNPC.getEntity().isDead()) return null;
		if (!( myNPC.getEntity() instanceof LivingEntity)){
			plugin.getServer().getLogger().info("Sentry " + myNPC.getName() + " is not a living entity! Errors inbound....");
			return null;
		}
		return (LivingEntity) myNPC.getEntity();
	}


	protected NPC getMountNPC(){
		if(this.isMounted() && net.citizensnpcs.api.CitizensAPI.hasImplementation()){

			return net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getById(this.MountID);

		}
		return null;
	}



}
