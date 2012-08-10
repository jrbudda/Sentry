package net.aufdemrand.sentry;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.waypoint.Waypoints;
import net.minecraft.server.EntityLiving;

import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public class SentryInstance {

	private class SentryLogicRunnable implements Runnable {
		@Override
		public void run() {

			// plugin.getServer().broadcastMessage("tick " + (myNPC ==null) +
			// " " + sentryStatus);

			if (!myNPC.isSpawned()) sentryStatus = Status.isDEAD; // incase it dies in a way im not handling.....

			if (sentryStatus != Status.isDEAD && System.currentTimeMillis() > oktoheal && HealRate > 0) {
				if (myNPC.getBukkitEntity().getHealth() < sentryHealth) {
					myNPC.getBukkitEntity().setHealth(myNPC.getBukkitEntity().getHealth() + 1);
					myNPC.getBukkitEntity().playEffect(EntityEffect.SHEEP_EAT);
				}
				oktoheal = (long) (System.currentTimeMillis() + HealRate * 1000);
			}

			if (sentryStatus == Status.isDEAD && System.currentTimeMillis() > isRespawnable && RespawnDelaySeconds != 0) {
				// Respawn

				// Location loc =
				// myNPC.getTrait(CurrentLocation.class).getLocation();
				// if (myNPC.hasTrait(Waypoints.class)){
				// Waypoints wp = myNPC.getTrait(Waypoints.class);
				// wp.getCurrentProvider()
				// }

				// plugin.getServer().broadcastMessage("Spawning...");
				if (guardEntity == null) {
					myNPC.spawn(Spawn);
				} else {
					myNPC.spawn(guardEntity.getLocation().add(2, 0, 2));
				}
				return;

			}

			else if (sentryStatus == Status.isHOSTILE && myNPC.isSpawned()) {

				if (projectileTarget != null && !projectileTarget.isDead()) {
					if (_projTargetLostLoc == null)
						_projTargetLostLoc = projectileTarget.getLocation();

					faceEntity(myNPC.getBukkitEntity(), projectileTarget);

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
					// Did it get away?
					if (meleeTarget.getLocation().distance(myNPC.getBukkitEntity().getLocation()) > sentryRange) {
						// it got away...
						setTarget(null);
					}
				}

				else {
					// target died or null
					setTarget(null);
				}
			}

			else if (sentryStatus == Status.isLOOKING && myNPC.isSpawned()) {

				if (guardTarget != null && guardEntity == null) {
					// daddy? where are u?
					setGuardTarget(guardTarget);
				}

				LivingEntity target = findTarget(sentryRange);

				if (target != null) {
					// plugin.getServer().broadcastMessage("Target selected: " +
					// target.toString());
					setTarget(target);
				} else
					setTarget(null);

			}

		}

	}

	public enum Status {
		isDEAD, isHOSTILE, isLOOKING, isDYING, isSTUCK
	}

	public enum hittype {
		normal, miss, block, injure, main, disembowel
	}

	/* plugin Constructer */
	Sentry plugin;

	/* Technicals */
	private Integer taskID = null;
	private Long isRespawnable = System.currentTimeMillis();
	private long oktoFire = System.currentTimeMillis();
	private long oktoheal = System.currentTimeMillis();
	private int _logicTick = 10;
	private List<Player> _myDamamgers = new ArrayList<Player>();

	public LivingEntity projectileTarget;
	public LivingEntity meleeTarget;
	/* Internals */
	Status sentryStatus = Status.isDYING;

	public NPC myNPC = null;
	/* Setables */
	public SentryTrait myTrait;
	public List<String> validTargets = new ArrayList<String>();
	public Integer sentryRange = 10;
	public Integer sentryHealth = 20;
	public float sentrySpeed =  (float) 1.0;
	public float defaultSpeed = (float) 1.0;
	public Double sentryWeight = 1.0;
	public String guardTarget = null;
	public LivingEntity guardEntity = null;
	public Boolean FriendlyFire = false;
	public Boolean LuckyHits = true;
	public Boolean Invincible = false;
	public Boolean Retaliate = true;
	public Boolean DropInventory = false;
	public Integer RespawnDelaySeconds = 10;
	public Integer Armor = 0;
	public Integer Strength = 1;
	public Integer NightVision = 16;
	public Double AttackRateSeconds = 2.0;
	public Double HealRate = 0.0;

	public Location Spawn = null;

	private Location _projTargetLostLoc;

	private Class<? extends Projectile> myProjectile;

	public SentryInstance(Sentry plugin) {
		this.plugin = plugin;
		isRespawnable = System.currentTimeMillis();
	}

	// private Random r = new Random();

	public void cancelRunnable() {
		if (taskID != null) {
			plugin.getServer().getScheduler().cancelTask(taskID);
		}
	}

	public boolean containsTarget(String theTarget) {
		if (validTargets.contains(theTarget))
			return true;
		else
			return false;
	}

	public void deactivate() {
		plugin.getServer().getScheduler().cancelTask(taskID);
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

	public LivingEntity findTarget(Integer Range) {

		if (this.validTargets.size() == 0) return null;

		List<Entity> EntitiesWithinRange = myNPC.getBukkitEntity().getNearbyEntities(Range, Range, Range);
		LivingEntity theTarget = null;
		Double distanceToBeat = 99999.0;

		// plugin.getServer().broadcastMessage("Targets scanned : " +
		// EntitiesWithinRange.toString());

		for (Entity aTarget : EntitiesWithinRange) {

			boolean isATarget = false;

			if (aTarget instanceof Player) {

				if (this.containsTarget("ENTITY:PLAYER")) {
					isATarget = true;
				}
				if (this.containsTarget("ENTITY:PLAYERS")) {
					isATarget = true;
				}

				else if (this.containsTarget("PLAYER:" + ((Player) aTarget).getName().toUpperCase())) {
					isATarget = true;
				}

				else if (this.containsTarget("GROUP:")) {
					String[] groups = Sentry.perms.getPlayerGroups((Player) aTarget);
					for (int i = 0; i < groups.length; i++) {
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

				if (this.containsTarget("ENTITY:MONSTERS")) {
					isATarget = true;
				}

				if (this.containsTarget("ENTITY:" + ((LivingEntity) aTarget).getType())) {
					isATarget = true;
				}
			}

			else if (aTarget instanceof LivingEntity) {
				if (this.containsTarget("ENTITY:" + ((LivingEntity) aTarget).getType())) {
					isATarget = true;
				}
			}

			// find closest target
			if (isATarget) {

				// can i see it?
				// too dark?
				double ll = (double) aTarget.getLocation().getBlock().getLightLevel();
				// sneaking cut light in half
				if (aTarget instanceof Player)
					if (((Player) aTarget).isSneaking())
						ll /= 2;

				// too dark?
				if (ll >= (16 - this.NightVision)) {

					Vector victor = ((LivingEntity) aTarget).getEyeLocation().subtract(myNPC.getBukkitEntity().getEyeLocation()).toVector();
					double dist = Math.sqrt(Math.pow(victor.getX(), 2) + Math.pow(victor.getZ(), 2));

					if (dist < distanceToBeat) {

						// LoS calc
						boolean LOS = (((CraftLivingEntity) myNPC.getBukkitEntity()).getHandle()).l(((CraftLivingEntity) aTarget).getHandle());
						// plugin.getServer().broadcastMessage("LOS for: " +
						// aTarget.toString() + LOS);

						if (LOS) {
							// now find closes mob
							distanceToBeat = dist;
							theTarget = (LivingEntity) aTarget;
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

	public void Fire(LivingEntity theEntity) {

		double v = 34;
		double g = 20;

		Effect effect = null;

		if (myProjectile == Arrow.class) {
			effect = Effect.BOW_FIRE;
		} else if (myProjectile == SmallFireball.class) {
			effect = Effect.BLAZE_SHOOT;
			v = 5000.0;
			g = .01;
		} else {
			v = 17.75;
			g = 13.5;

		}

		// calc shooting spot.
		Location loc = Util.getFireSource(myNPC.getBukkitEntity(), theEntity);

		Location targetsHeart = theEntity.getLocation();
		targetsHeart = targetsHeart.add(0, .33, 0);

		// lead the target
		Vector test = targetsHeart.clone().subtract(loc).toVector();

		Double elev = test.getY();

		Double testAngle = Util.launchAngle(loc, targetsHeart, v, elev, g);

		if (testAngle == null) {
			// testAngle = Math.atan( ( 2*g*elev + Math.pow(v, 2)) / (2*g*elev +
			// 2*Math.pow(v,2))); //cant hit it where it is, try aiming as far
			// as you can.
			setTarget(null);
			// plugin.getServer().broadcastMessage("Can't hit test angle");
			return;
		}

		// plugin.getServer().broadcastMessage("ta " + testAngle.toString());

		Double hangtime = Util.hangtime(testAngle, v, elev, g);
		// plugin.getServer().broadcastMessage("ht " + hangtime.toString());

		Vector targetVelocity = theEntity.getLocation().subtract(_projTargetLostLoc).toVector();
		// plugin.getServer().broadcastMessage("tv" + targetVelocity);

		targetVelocity.multiply(20 / _logicTick);

		Location to = Util.leadLocation(targetsHeart, targetVelocity, hangtime);
		// plugin.getServer().broadcastMessage("to " + to);
		// Calc range

		Vector victor = to.clone().subtract(loc).toVector();

		Double dist = Math.sqrt(Math.pow(victor.getX(), 2) + Math.pow(victor.getZ(), 2));
		elev = victor.getY();
		if (dist == 0)
			return;
		boolean LOS = (((CraftLivingEntity) myNPC.getBukkitEntity()).getHandle()).l(((CraftLivingEntity) theEntity).getHandle());
		if (!LOS) {
			// target cant be seen..
			setTarget(null);
			// plugin.getServer().broadcastMessage("No LoS");
			return;
		}

		// plugin.getServer().broadcastMessage("delta " + victor);

		// plugin.getServer().broadcastMessage("ld " +
		// to.clone().subtract(theEntity.getEyeLocation()));

		Double launchAngle = Util.launchAngle(loc, to, v, elev, g);

		if (launchAngle == null) {
			// target cant be hit
			setTarget(null);
			// plugin.getServer().broadcastMessage("Can't hit lead");
			return;

		}

		// OK we're shooting
		// go twang
		if (effect != null)
			myNPC.getBukkitEntity().getWorld().playEffect(myNPC.getBukkitEntity().getLocation(), effect, null);
		// if (myNPC.getBukkitEntity() instanceof HumanEntity){
		// net.minecraft.server.EntityPlayer p = (EntityPlayer)
		// ((CraftLivingEntity)myNPC.getBukkitEntity()).getHandle();
		// plugin.getServer().broadcastMessage("play anim");

		// p.netServerHandler.sendPacket(new
		// net.minecraft.server.Packet18ArmAnimation(p, -42));
		// }

		// Apply angle
		victor.setY(Math.tan(launchAngle) * dist);

		Vector noise = Vector.getRandom();

		// normalize vector
		victor = Util.normalizeVector(victor);

		noise = noise.multiply(1 / 10.0);

		// victor = victor.add(noise);

		v = v + (1.19 * Math.pow(hangtime, 2));

		// v = v+ (r.nextDouble() - 1.3)/2;

		// apply power
		victor = victor.multiply(v / 20.0);

		if (myProjectile == SmallFireball.class) {
			// this dont do nuffin
			victor.multiply(1 / 20.0);
		}

		// Shoot!
		// Projectile theArrow
		// =myNPC.getBukkitEntity().launchProjectile(myProjectile);

		Projectile theArrow = myNPC.getBukkitEntity().getWorld().spawn(loc, myProjectile);
		theArrow.setShooter(myNPC.getBukkitEntity());
		theArrow.setVelocity(victor);

	}

	public LivingEntity getGuardTarget() {
		return this.guardEntity;
	}

	public String getStats() {
		DecimalFormat df = new DecimalFormat("#.0");
		int h = 0;

		if(myNPC !=null &&  myNPC.getBukkitEntity()!=null) h = myNPC.getBukkitEntity().getHealth(); 

		return ChatColor.RED + "[HP]:" + ChatColor.WHITE + h + "/" + sentryHealth + ChatColor.RED + " [AP]:" + ChatColor.WHITE + Armor + ChatColor.RED + " [STR]:" + ChatColor.WHITE + Strength + ChatColor.RED + " [SPD]:" + ChatColor.WHITE + df.format(sentrySpeed) + ChatColor.RED + " [RNG]:" + ChatColor.WHITE + sentryRange + ChatColor.RED + " [ATK]:" + ChatColor.WHITE + AttackRateSeconds + ChatColor.RED + " [VIS]:" + ChatColor.WHITE + NightVision + ChatColor.RED + " [HEAL]:" + ChatColor.WHITE + HealRate;

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
		if (sentryRange > 100)
			sentryRange = 100;

		if (sentryWeight <= 0)
			sentryWeight =  1.0;

		if (RespawnDelaySeconds < -1)
			RespawnDelaySeconds = -1;

		if (Spawn == null)
			Spawn = myNPC.getBukkitEntity().getLocation();

		// defaultSpeed = myNPC.getNavigator().getSpeed();

		((CraftLivingEntity) myNPC.getBukkitEntity()).getHandle().setHealth(sentryHealth);

		_myDamamgers.clear();

		this.sentryStatus = Status.isLOOKING;
		faceForward();

		// plugin.getServer().broadcastMessage("NPC GUARDING!");

		if (taskID == null) {
			taskID = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new SentryLogicRunnable(), 20, _logicTick);
		}

	}

	public void onDamage(EntityDamageByEntityEvent event) {

		event.setCancelled(true);

		if(sentryStatus == Status.isDYING) return;

		if (!myNPC.isSpawned()) {
			// \\how did youg get here?
			return;
		}

		NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(event.getEntity());

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

		// can i kill it? lets go kill it.
		if (player != null) {
			if (this.Retaliate) {
				setTarget(player);
			}
		}

		if (Invincible)
			return;

		if (LuckyHits) {
			// Calulate crits
			double damagemodifer = event.getDamage();

			Random r = new Random();
			int luckeyhit = r.nextInt(100);

			// if (damagemodifer == 1.0) luckeyhit += 30; //use a weapon, dummy

			if (luckeyhit < 4) {
				damagemodifer = damagemodifer * 2.00;
				hit = hittype.disembowel;
			} else if (luckeyhit < 10) {

				damagemodifer = damagemodifer * 1.75;
				hit = hittype.main;
			} else if (luckeyhit < 18) {
				damagemodifer = damagemodifer * 1.50;
				hit = hittype.injure;
			} else if (luckeyhit > 95) {

				damagemodifer = 0;
				hit = hittype.miss;

			}

			finaldamage = (int) Math.round(damagemodifer);
		}

		if (finaldamage > 0) {

			if (player != null) {
				// knockback
				Vector newVec = player.getLocation().getDirection().multiply(1.75);
				newVec.setY(newVec.getY() / (double) sentryWeight);
				npc.getBukkitEntity().setVelocity(newVec);
			}

			// Apply armor
			finaldamage -= this.Armor;

			// there was damamge before armor.
			if (finaldamage <= 0 && LuckyHits)
				hit = hittype.block;

		}

		if (player instanceof CraftPlayer) {

			if(!_myDamamgers.contains(player)) _myDamamgers.add((Player) player);

			// Messages
			switch (hit) {
			case normal:
				((Player) player).sendMessage(ChatColor.WHITE + "*** You hit " + myNPC.getName() + " for " + finaldamage + " damage");
				break;
			case miss:
				((Player) player).sendMessage(ChatColor.GRAY + "*** You miss " + myNPC.getName());
				break;
			case block:
				((Player) player).sendMessage(ChatColor.GRAY + "*** Your blow glances off " + myNPC.getName() + "'s armor");
				break;
			case main:
				((Player) player).sendMessage(ChatColor.GOLD + "*** You MAIM " + myNPC.getName() + " for " + finaldamage + " damage");
				break;
			case disembowel:
				((Player) player).sendMessage(ChatColor.RED + "*** You DISEMBOWEL " + myNPC.getName() + " for " + finaldamage + " damage");
				break;
			case injure:
				((Player) player).sendMessage(ChatColor.YELLOW + "*** You injure " + myNPC.getName() + " for " + finaldamage + " damage");
				break;
			}
		}

		if (finaldamage > 0) {
			npc.getBukkitEntity().playEffect(EntityEffect.HURT);

			// is he dead?
			if (npc.getBukkitEntity().getHealth() - finaldamage <= 0) {
				npc.getBukkitEntity().getWorld().playEffect(npc.getBukkitEntity().getLocation(), Effect.SMOKE, 3);
				npc.getBukkitEntity().getLocation().getWorld().spawn(npc.getBukkitEntity().getLocation(), ExperienceOrb.class).setExperience(5);
				// finaldamage = npc.getBukkitEntity().getHealth();

				if (myNPC.getBukkitEntity() instanceof HumanEntity && !this.DropInventory) {
					((HumanEntity) myNPC.getBukkitEntity()).getInventory().clear();
					((HumanEntity) myNPC.getBukkitEntity()).getInventory().setArmorContents(null);
				}


				if		(plugin.SentryDeath(_myDamamgers, myNPC)){
					//Denizen is handling this death
					sentryStatus = Status.isDYING;
					
					if (RespawnDelaySeconds == -1) {
						myNPC.destroy();
					} else {
						isRespawnable = System.currentTimeMillis() + RespawnDelaySeconds * 1000;
					}
					
					return;
				}
				else
				{
					//Denizen is NOT handling this death
					sentryStatus = Status.isDEAD;
					if (RespawnDelaySeconds == -1) {
						myNPC.destroy();
					} else {
						isRespawnable = System.currentTimeMillis() + RespawnDelaySeconds * 1000;
					}

					myNPC.getBukkitEntity().damage(finaldamage);
					return;

				}


				// plugin.getServer().broadcastMessage("Dead!");
			}

			// plugin.getServer().broadcastMessage("Damage: " + myNPC.getName()
			// + " " + finaldamage + " hp " +
			// myNPC.getBukkitEntity().getHealth());

			myNPC.getBukkitEntity().damage(finaldamage);

		}

		else {
			// do nothing
		}

	}



	@EventHandler
	public void onRightClick(NPCRightClickEvent event) {

	}

	public boolean setGuardTarget(String name) {
		// plugin.getServer().broadcastMessage("Setting guard");

		if (myNPC == null)
			return false;

		if (name == null) {
			guardEntity = null;
			guardTarget = null;
			setTarget(null);// clear active hostile target
			return true;
		}

		List<Entity> EntitiesWithinRange = myNPC.getBukkitEntity().getNearbyEntities(sentryRange, sentryRange, sentryRange);

		for (Entity aTarget : EntitiesWithinRange) {

			if (aTarget instanceof Player) {
				if (((Player) aTarget).getName().equals(name)) {
					guardEntity = (LivingEntity) aTarget;
					guardTarget = ((Player) aTarget).getName();
					setTarget(null); // clear active hostile target
					return true;
				}

			}

		}
		return false;
	}

	public void setTarget(LivingEntity theEntity) {

		if (theEntity == null) {
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

			if (guardEntity != null) {
				myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);
				myNPC.getNavigator().setTarget(guardEntity, false);
			} else {
				// myNPC.getNavigator().setTarget(myNPC.getBukkitEntity().getLocation());

				if (myNPC.getTrait(Waypoints.class).getCurrentProvider().isPaused()) {
					// plugin.getServer().broadcastMessage("setting speed to default: "
					// + defaultSpeed);
					myNPC.getNavigator().setSpeed((Float) defaultSpeed);
				}

				myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(false);
			}

			faceForward();
			return;
		}

		if (theEntity == guardEntity)
			return; // dont attack my dude.

		sentryStatus = Status.isHOSTILE;

		Material weapon = Material.AIR;

		faceEntity(myNPC.getBukkitEntity(), theEntity);

		if (myNPC.getBukkitEntity() instanceof HumanEntity) {
			weapon = ((HumanEntity) myNPC.getBukkitEntity()).getInventory().getItemInHand().getType();
		}

		switch (weapon) {
		case BOW:
			myProjectile = org.bukkit.entity.Arrow.class;
			projectileTarget = theEntity;
			meleeTarget = null;
			break;
		case BLAZE_ROD:
			myProjectile = org.bukkit.entity.SmallFireball.class;
			projectileTarget = theEntity;
			meleeTarget = null;
			break;
		case SNOW_BALL:
			myProjectile = org.bukkit.entity.Snowball.class;
			projectileTarget = theEntity;
			meleeTarget = null;
			break;
		case EGG:
			myProjectile = org.bukkit.entity.Egg.class;
			projectileTarget = theEntity;
			meleeTarget = null;
			break;
		case POTION:
			myProjectile = org.bukkit.entity.ThrownPotion.class;
			projectileTarget = theEntity;
			meleeTarget = null;
			break;

		default:
			// Manual Attack
			projectileTarget = null;
			meleeTarget = theEntity;
			if (!myNPC.getTrait(Waypoints.class).getCurrentProvider().isPaused()) 	defaultSpeed = myNPC.getNavigator().getSpeed();
			myNPC.getTrait(Waypoints.class).getCurrentProvider().setPaused(true);
			myNPC.getNavigator().setSpeed(sentrySpeed);
			// plugin.getServer().broadcastMessage("setting speed to: " +
			// sentrySpeed);
			myNPC.getNavigator().setPathfindingRange((sentryRange));
			myNPC.getNavigator().setTarget(theEntity, true);

			break;
		}
	}

}
