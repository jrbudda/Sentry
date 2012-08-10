package net.aufdemrand.sentry;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import net.aufdemrand.sentry.SentryInstance.Status;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCPushEvent;

import net.citizensnpcs.api.npc.NPC;


public class SentryListener implements Listener {

	public Sentry plugin; 

	public SentryListener(Sentry sentry) {
		plugin = sentry;
	}

	@EventHandler
	public void pushable(NPCPushEvent event) {
		SentryInstance thisInstance = plugin.getSentry(event.getNPC());

		if (thisInstance!=null){
			if (thisInstance.sentryStatus == Status.isHOSTILE || thisInstance.guardEntity != null) {
				event.setCancelled(false);
			}

		}
	}

	@EventHandler
	public void C2Reload(CitizensReloadEvent event) {

		
	}


	@EventHandler
	public void onDamage(org.bukkit.event.entity.EntityDamageByEntityEvent  event) {

		Entity entfrom = event.getDamager();
		Entity entto = event.getEntity();

		if(	entfrom  instanceof org.bukkit.entity.Projectile)	entfrom = ((org.bukkit.entity.Projectile) entfrom).getShooter();



		if (entto instanceof Player){

			for (NPC npc : CitizensAPI.getNPCRegistry()) {
				SentryInstance inst =plugin.getSentry(npc);
				if (inst!=null &&  inst.guardEntity == entto ){
					if (inst.Retaliate) inst.setTarget((LivingEntity)entfrom);
				}
			}
		}


		SentryInstance from = plugin.getSentry(entfrom);
		SentryInstance to = plugin.getSentry(entto);


	//	plugin.getLogger().info("start: from: " + entfrom + " to " + entto + " cancelled " + event.isCancelled() + " damage " + event.getDamage());


		if (from !=null) {

			//from a sentry
			event.setCancelled(false);	
			event.setDamage(from.Strength);
			if(entto == from.guardEntity && !from.FriendlyFire) event.setCancelled(true);
			if(entfrom == entto) event.setCancelled(true);
		}


		if (to  != null) {
			
			//to a sentry
			event.setCancelled(false);	
			if(entfrom == to.guardEntity && !to.FriendlyFire) event.setCancelled(true);
						
			NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(entfrom);
			if (npc !=null && npc.hasTrait(SentryTrait.class) && to.guardEntity !=null){
				if ( npc.getTrait(SentryTrait.class).getInstance().guardEntity == to.guardEntity) { //dont take damage from co-guards.
					event.setCancelled(true);
				}
			}

			if (!event.isCancelled()) to.onDamage(event);		

		}

//	plugin.getLogger().info("final: from: " + entfrom + " to " + entto + " cancelled " + event.isCancelled() + " damage " + event.getDamage());
	}


	@EventHandler
	public void Despawn(NPCDespawnEvent event){

		//plugin.initializedSentries.remove(event.getNPC().getId());
		//	plugin.getServer().broadcastMessage("onDespawn");

	}




}
