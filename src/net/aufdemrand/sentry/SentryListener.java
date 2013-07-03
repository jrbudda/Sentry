package net.aufdemrand.sentry;

import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.util.Vector;


import net.citizensnpcs.api.CitizensAPI;


import net.citizensnpcs.api.npc.NPC;


public class SentryListener implements Listener {

	public Sentry plugin; 

	public SentryListener(Sentry sentry) {
		plugin = sentry;
	}


	@EventHandler
	public void kill(org.bukkit.event.entity.EntityDeathEvent event){

		if(event.getEntity() == null) return;

		//dont mess with player death.
		if (event.getEntity() instanceof Player && event.getEntity().hasMetadata("NPC") == false) return;

		SentryInstance sentry = plugin.getSentry(event.getEntity().getKiller());

		if(sentry !=null && sentry.KillsDropInventory == false){
			event.getDrops().clear();
			event.setDroppedExp(0);	
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void despawn(net.citizensnpcs.api.event.NPCDespawnEvent event) {
		SentryInstance sentry = plugin.getSentry(event.getNPC());
		//dont despawn active bodyguaards on chunk unload
		if(sentry !=null && event.getReason() == net.citizensnpcs.api.event.DespawnReason.CHUNK_UNLOAD && sentry.guardEntity != null){
			event.setCancelled(true);
		}
	}

	@EventHandler(priority =org.bukkit.event.EventPriority.HIGHEST)
	public void entteleportevent(org.bukkit.event.entity.EntityTeleportEvent event) {
		SentryInstance sentry = plugin.getSentry(event.getEntity());
		if(sentry !=null && sentry.epcount != 0 && sentry.isWarlock1()){
			event.setCancelled(true);
		}
	}

	@EventHandler(priority =org.bukkit.event.EventPriority.HIGHEST)
	public void entteleportevent(org.bukkit.event.player.PlayerTeleportEvent event) {
		SentryInstance sentry = plugin.getSentry(event.getPlayer());
		if(sentry !=null){
			//	plugin.debug("teleport!!: " + event.getPlayer()  + event.isCancelled() + " "+ sentry.epcount);
			if(	sentry.epcount != 0 && sentry.isWarlock1() ){
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority =org.bukkit.event.EventPriority.MONITOR)
	public void projectilehit(org.bukkit.event.entity.ProjectileHitEvent event) {
		if (event.getEntity() instanceof org.bukkit.entity.EnderPearl){	
			SentryInstance sentry = plugin.getSentry(event.getEntity().getShooter());
			if(sentry !=null){
				sentry.epcount--;
				if (sentry.epcount<0) sentry.epcount=0;
				event.getEntity().getLocation().getWorld().playEffect(event.getEntity().getLocation(), org.bukkit.Effect.ENDER_SIGNAL, 1, 100);
				//ender pearl from a sentry
			}
		}
		else 	if (event.getEntity() instanceof org.bukkit.entity.SmallFireball){	
			final org.bukkit.block.Block block = event.getEntity().getLocation().getBlock();
			SentryInstance sentry = plugin.getSentry(event.getEntity().getShooter());

			if(sentry !=null && sentry.isPyromancer1()){

				plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
					public void run(){

						for (BlockFace face :org.bukkit.block.BlockFace.values()){
							if (block.getRelative(face).getType() == org.bukkit.Material.FIRE) block.getRelative(face).setType(org.bukkit.Material.AIR);
						}

						if (block.getType() == org.bukkit.Material.FIRE) block.setType(org.bukkit.Material.AIR);

					}
				}
						);
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority =org.bukkit.event.EventPriority.HIGH)
	public void tarsdfget(EntityTargetEvent event) {
		SentryInstance inst = plugin.getSentry(event.getTarget());
		if(inst!=null){
			event.setCancelled(false); //inst.myNPC.data().get(NPC.DEFAULT_PROTECTED_METADATA, false));
		}
	}

	@EventHandler(priority =org.bukkit.event.EventPriority.HIGHEST)
	public void EnvDamage(EntityDamageEvent event) {

		if (event instanceof EntityDamageByEntityEvent) return;

		SentryInstance inst = plugin.getSentry(event.getEntity());
		if (inst == null) return;

		event.setCancelled(true);

		DamageCause cause = event.getCause();
		//	plugin.getLogger().log(Level.INFO, "Damage " + cause.toString() + " " + event.getDamage());

		switch (cause){
		case CONTACT: case DROWNING: case LAVA: case SUFFOCATION: case CUSTOM:  case BLOCK_EXPLOSION: case VOID: case SUICIDE: case MAGIC: 
			inst.onEnvironmentDamae(event);
			break;
		case LIGHTNING: 
			if (!inst.isStormcaller()) inst.onEnvironmentDamae(event);
			break;
		case FIRE: case FIRE_TICK:
			if (!inst.isPyromancer() && !inst.isStormcaller()) inst.onEnvironmentDamae(event);
			break;
		case POISON:
			if (!inst.isWitchDoctor()) inst.onEnvironmentDamae(event);
			break;
		case FALL:
			break;
		default:
			break;
		}	
	}

	@EventHandler(priority =org.bukkit.event.EventPriority.HIGHEST) //highest for worldguard...
	public void onDamage(org.bukkit.event.entity.EntityDamageByEntityEvent  event) {

		Entity entfrom = event.getDamager();
		Entity entto = event.getEntity();

		if(	entfrom  instanceof org.bukkit.entity.Projectile){
			entfrom = ((org.bukkit.entity.Projectile) entfrom).getShooter();
		}

		SentryInstance from = plugin.getSentry(entfrom);
		SentryInstance to = plugin.getSentry(entto);

		//process this event on each sentry to check for respondable events.
		if (event.isCancelled() == false && entfrom != entto && event.getDamage() > 0){
			for (NPC npc : CitizensAPI.getNPCRegistry()) {
				SentryInstance inst =plugin.getSentry(npc);

				if (inst == null || !npc.isSpawned() || npc.getBukkitEntity().getWorld() != entto.getWorld()) continue; //not a sentry, or not this world, or dead.

				if (inst.guardEntity == entto ){
					if (inst.Retaliate && entfrom instanceof LivingEntity)  inst.setTarget((LivingEntity) entfrom, true);
				}

				if (inst.hasTargetType(16)  && inst.sentryStatus == net.aufdemrand.sentry.SentryInstance.Status.isLOOKING && entfrom instanceof Player && CitizensAPI.getNPCRegistry().isNPC(entfrom) ==false ){
					//pv-something event.
					if (npc.getBukkitEntity().getLocation().distance(entto.getLocation()) <= inst.sentryRange ||npc.getBukkitEntity().getLocation().distance(entfrom.getLocation()) <= inst.sentryRange){
						// in range
						if(inst.NightVision  >= entfrom.getLocation().getBlock().getLightLevel() || inst.NightVision  >= entto.getLocation().getBlock().getLightLevel() ){
							//can see
							if (npc.getBukkitEntity().hasLineOfSight(entfrom) || npc.getBukkitEntity().hasLineOfSight(entto)){
								//have los
								if ( (!(entto instanceof Player) && inst.containsTarget("event:pve")) ||
										(entto instanceof Player && CitizensAPI.getNPCRegistry().isNPC(entto) ==false && inst.containsTarget("event:pvp")) || 
										(CitizensAPI.getNPCRegistry().isNPC(entto) == true && inst.containsTarget("event:pvnpc")) ||
										(to !=null && inst.containsTarget("event:pvsentry")))	{
									//Valid event, attack
									if (!inst.isIgnored((LivingEntity)entfrom)){
										inst.setTarget( (LivingEntity) entfrom, true); //attack the aggressor
									}
								}
							}
						}	
					}
				}
			}
		}

		plugin.debug("start: from: " + entfrom + " to " + entto + " cancelled " + event.isCancelled() + " damage " + event.getDamage() + " cause " + event.getCause());

		if (from !=null) {
			
			//projectiles go thru ignore targets.
			if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
				if (entto instanceof LivingEntity && from.isIgnored((LivingEntity) entto)){
					event.setCancelled(true);
					event.getDamager().remove();
					Projectile newProjectile =	(Projectile)(entfrom.getWorld().spawnEntity(event.getDamager().getLocation().add(event.getDamager().getVelocity()), event.getDamager().getType()));
					newProjectile.setVelocity(event.getDamager().getVelocity());		
					newProjectile.setShooter((LivingEntity) entfrom);
					newProjectile.setTicksLived(event.getDamager().getTicksLived());
					return;
				}
			}
			
			//from a sentry
			event.setDamage(from.getStrength());

			//uncancel if not bodyguard.
			if (from.guardTarget == null || plugin.BodyguardsObeyProtection == false) event.setCancelled(false);	

			//cancel if invulnerable non-sentry npc
			if (to == null){
				NPC n = CitizensAPI.getNPCRegistry().getNPC(entto);
				if (n != null){
					boolean derp = (Boolean) n.data().get(NPC.DEFAULT_PROTECTED_METADATA,true);
					event.setCancelled(derp);
				}	
			}

			//dont hurt guard target.
			if(entto == from.guardEntity) event.setCancelled(true);

			//stop hittin yourself.
			if(entfrom == entto) event.setCancelled(true);

			//apply potion effects
			if (from.potionEffects!=null && event.isCancelled() == false){		
				((LivingEntity)entto).addPotionEffects(from.potionEffects);		
			}

			if (from.isWarlock1()) {
				if (event.isCancelled()==false){
					if(to == null)	event.setCancelled(true); //warlock 1 should not do direct damamge, except to other sentries which take no fall damage.

					double h =from.getStrength()+3;
					double v =7.7*Math.sqrt(h) + .2;
					if (h<=3) v-=2;
					if(v>150) v = 150;

					entto.setVelocity(new Vector(0,v/20 ,0));

				}

			}

		}

		if (to  != null) {
			//to a sentry

			//stop hittin yourself.
			if (entfrom == entto) return;

			//innate protections
			if (event.getCause() == DamageCause.LIGHTNING && to.isStormcaller()) return;
			if ((event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) && (to.isPyromancer()||to.isStormcaller())) return;

			//only bodyguards obey pvp-protection
			if (to.guardTarget ==null) event.setCancelled(false);	

			//dont take damamge from guard entity.
			if(entfrom == to.guardEntity) event.setCancelled(true);

			if (entfrom!=null)	{
				NPC	npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(entfrom);
				if (npc !=null && npc.hasTrait(SentryTrait.class) && to.guardEntity !=null){
					if ( npc.getTrait(SentryTrait.class).getInstance().guardEntity == to.guardEntity) { //dont take damage from co-guards.
						event.setCancelled(true);
					}
				}
			}

			//process event
			if (!event.isCancelled()) to.onDamage(event);	

		}

		return;
	}




}

