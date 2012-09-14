package net.aufdemrand.sentry;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCDespawnEvent;

import net.citizensnpcs.api.npc.NPC;


public class SentryListener implements Listener {

	public Sentry plugin; 

	public SentryListener(Sentry sentry) {
		plugin = sentry;
	}
	//
	//	@EventHandler
	//	public void pushable(NPCPushEvent event) {
	//		SentryInstance thisInstance = plugin.getSentry(event.getNPC());
	//		if (thisInstance!=null){
	//			event.setCancelled(false);
	//		}
	//	}

	//	@EventHandler
	//	public void C2Reload(CitizensReloadEvent event) {
	//	}
	//	

	//	@EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
	//	public void C2Reload(org.bukkit.event.entity.CreatureSpawnEvent event) {
	//		plugin.getLogger().log(Level.INFO, "SPAWN " + event.isCancelled() + " " + event.getEntityType().toString());
	//	}

	//
	//	@EventHandler
	//	public void ncom(net.citizensnpcs.api.ai.event.NavigationCompleteEvent event) {
	//		plugin.getLogger().info("nav complete" );
	//	}
	//
	//	@EventHandler
	//	public void ncan(net.citizensnpcs.api.ai.event.NavigationCancelEvent event) {
	//		plugin.getLogger().info("nav cancel " + event.getCancelReason());
	//	}

	@EventHandler(priority =org.bukkit.event.EventPriority.HIGH)
	public void target(EntityTargetEvent event) {
		if (plugin.getSentry(event.getTarget()) !=null) event.setCancelled(false);
	}

	@EventHandler(priority =org.bukkit.event.EventPriority.HIGHEST)
	public void EnvDamage(EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent) return;
		SentryInstance inst = plugin.getSentry(event.getEntity());

		if (inst == null) return;

		DamageCause cause = event.getCause();
		//	plugin.getLogger().log(Level.INFO, "Damage " + cause.toString() + " " + event.getDamage());

		switch (cause){
		case CONTACT: case DROWNING: case LAVA: case FALL: case SUFFOCATION: case CUSTOM:  case BLOCK_EXPLOSION: case VOID: case SUICIDE: case MAGIC:
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


		for (NPC npc : CitizensAPI.getNPCRegistry()) {
			SentryInstance inst =plugin.getSentry(npc);
			if (inst!=null &&  inst.guardEntity == entto ){
				if (inst.Retaliate) inst.setTarget((LivingEntity)entfrom, true);
			}

			if (inst != null  && event.getDamage() > 0 && npc.isSpawned()  && inst.sentryStatus == net.aufdemrand.sentry.SentryInstance.Status.isLOOKING && entfrom instanceof Player && CitizensAPI.getNPCRegistry().isNPC(entfrom) ==false && npc.getBukkitEntity().getWorld() == entto.getWorld()){
				//pvp event.
				if ( ( event.isCancelled() == false && CitizensAPI.getNPCRegistry().isNPC(entto) ==false && inst.containsTarget("event:pvp") && !inst.containsIgnore("event:pvp")) || 
						(CitizensAPI.getNPCRegistry().isNPC(entto) == true && inst.containsTarget("event:pvnpc") && !inst.containsIgnore("event:pvnpc")) ||
						(to !=null && inst.containsTarget("event:pvsentry") && !inst.containsIgnore("event:pvsentry"))
						){
					//looking for pvp or pvnpc event
					if (npc.getBukkitEntity().getLocation().distance(entto.getLocation()) <= inst.sentryRange ||npc.getBukkitEntity().getLocation().distance(entfrom.getLocation()) <= inst.sentryRange){
						// in range
						if(inst.NightVision  >= entfrom.getLocation().getBlock().getLightLevel() || inst.NightVision  >= entto.getLocation().getBlock().getLightLevel() ){
							//can see
							if (npc.getBukkitEntity().hasLineOfSight(entfrom) || npc.getBukkitEntity().hasLineOfSight(entto)){
								//have los
								inst.setTarget((LivingEntity) entfrom, true); //attack the aggressor
							}
						}
					}	
				}
			}
		}



	
		//	plugin.getLogger().info("start: from: " + entfrom + " to " + entto + " cancelled " + event.isCancelled() + " damage " + event.getDamage() + " cause " + event.getCause());

		if (from !=null) {
			if (from == to && !from.FriendlyFire) return;
			//from a sentry
			event.setCancelled(false);	
			event.setDamage(from.getStrength());
			if(entto == from.guardEntity && !from.FriendlyFire) event.setCancelled(true);
			if(entfrom == entto) event.setCancelled(true);
			if (from.potionEffects!=null && event.isCancelled() == false){		
				((LivingEntity)entto).addPotionEffects(from.potionEffects);		
			}
		}

		if (to  != null) {
			if (from == to && !from.FriendlyFire) return;
			if (event.getCause() == DamageCause.LIGHTNING && to.isStormcaller()) return;
			if ((event.getCause() == DamageCause.FIRE || event.getCause() == DamageCause.FIRE_TICK) && (to.isPyromancer()||to.isStormcaller())) return;
			//to a sentry
			event.setCancelled(false);	
			if(entfrom == to.guardEntity && !to.FriendlyFire) event.setCancelled(true);
			NPC npc =null;
			if (entfrom!=null)	 npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(entfrom);

			if (npc !=null && npc.hasTrait(SentryTrait.class) && to.guardEntity !=null){
				if ( npc.getTrait(SentryTrait.class).getInstance().guardEntity == to.guardEntity) { //dont take damage from co-guards.
					event.setCancelled(true);
				}
			}

			//plugin.getLogger().log(Level.INFO, "Entity Damage " + event.getCause().toString() + " " + event.getDamage() + " " + event.isCancelled());
			if (!event.isCancelled()) to.onDamage(event);		
		}

		//	plugin.getLogger().info("final: from: " + entfrom + " to " + entto + " cancelled " + event.isCancelled() + " damage " + event.getDamage());
		return;
	}

	PotionEffect slowEffect = new PotionEffect(PotionEffectType.getByName("SLOW"), 3*20 ,1);

	@EventHandler
	public void Despawn(NPCDespawnEvent event){

		//plugin.initializedSentries.remove(event.getNPC().getId());
		//	plugin.getServer().broadcastMessage("onDespawn");

	}

	/*	@EventHandler
	public void something(ChunkUnloadEvent event){

		Entity[] ents = event.getChunk().getEntities();

		for ( Entity ent:ents) {
			if (!(ent instanceof LivingEntity)) continue;
			SentryInstance inst = plugin.getSentry(ent);
			plugin.getLogger().log(Level.INFO,"Chunk unload " + ent.toString());
			if (inst !=null){
				plugin.getLogger().log(Level.INFO,"Chunk unload " + inst.myNPC.getName());
				inst.cancelRunnable();
			}
		}	
	}

	@EventHandler
	public void something2(ChunkLoadEvent event){
		Entity[] ents = event.getChunk().getEntities();
		for ( Entity ent:ents) {
			if (!(ent instanceof LivingEntity)) continue;
			plugin.getLogger().log(Level.INFO,"Chunk load " + ent.toString());
			SentryInstance inst = plugin.getSentry(ent);
			if (inst !=null){
				plugin.getLogger().log(Level.INFO,"Chunk load" + inst.myNPC.getName());
				inst.initialize();
			}
		}	
	}
	 */

}
