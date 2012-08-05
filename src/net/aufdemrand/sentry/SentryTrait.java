package net.aufdemrand.sentry;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;

import net.aufdemrand.sentry.SentryInstance.Status;
import net.citizensnpcs.api.event.CitizensReloadEvent;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCPushEvent;
import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.trait.Toggleable;

public class SentryTrait extends Trait implements Toggleable {

	private Sentry plugin;

	private boolean isToggled = true;

	public SentryTrait() {
		super("sentry");
	}
	private SentryInstance thisInstance;

	@SuppressWarnings("unchecked")
	@Override
	public void load(DataKey key) throws NPCLoadException {
		if (!this.getNPC().hasTrait(SentryTrait.class)) return;

		plugin = (Sentry) Bukkit.getServer().getPluginManager().getPlugin("Sentry");

	//	plugin.getServer().broadcastMessage("onLoad");
		
		if (thisInstance ==null) {
			thisInstance = new SentryInstance(plugin);
			thisInstance.myTrait = this;
		}

		isToggled=	key.getBoolean("toggled", true);
		thisInstance.Retaliate=	key.getBoolean("Retaliate", true);
		thisInstance.Invincible=	key.getBoolean("Invincinble", false);
		thisInstance.DestroyInventory=	key.getBoolean("DestroyInventory", true);
		thisInstance.LuckyHits=	key.getBoolean("CriticalHits", true);
		thisInstance.sentryHealth=	key.getInt("Health", 20);
		thisInstance.sentryRange=	key.getInt("Range", 10);
		thisInstance.RespawnDelaySeconds=	key.getInt("RespawnDelay", 10);
		thisInstance.sentrySpeed=	key.getDouble("Speed",1.0);
		thisInstance.sentryWeight=		key.getDouble("Weight",1.0);
		thisInstance.Armor=		key.getInt("Armor", 0);
		thisInstance.Strength=		key.getInt("Strength", 0);
		thisInstance.AttackRateSeconds = key.getDouble("AttackRate",2.0);
		
		Object derp =  key.getRaw("Targets");
		if (derp !=null) thisInstance.validTargets= (List<String>) key.getRaw("Targets");
		
	}

	public SentryInstance getInstance(){
		return thisInstance;
	}
	
	
	@Override
	public void onSpawn() {

		plugin = (Sentry) Bukkit.getPluginManager().getPlugin("Sentry");

		if (thisInstance ==null) {
			thisInstance = new SentryInstance(plugin);
			thisInstance.myTrait = this;
		}

		thisInstance.myNPC = this.getNPC();
		thisInstance.initialize();	
	
	  this.getNPC().getBukkitEntity().teleport(this.getNPC().getBukkitEntity().getLocation());
		
	//	plugin.getServer().broadcastMessage("onSpawn");
	}

	@Override
	public void onRemove() {

		plugin = (Sentry) Bukkit.getPluginManager().getPlugin("Sentry");

	//	plugin.getServer().broadcastMessage("onRemove");

	this.getNPC().getId();
	}

	@Override
	public void save(DataKey key) {

		key.setBoolean("toggled", isToggled);
		key.setBoolean("Retaliate", thisInstance.Retaliate);
		key.setBoolean("Invincinble", thisInstance.Invincible);
		key.setBoolean("DestroyInventory", thisInstance.DestroyInventory);
		key.setBoolean("CriticalHits", thisInstance.LuckyHits);
		key.setRaw("Targets", thisInstance.validTargets);
		key.setInt("Health", thisInstance.sentryHealth);
		key.setInt("Range", thisInstance.sentryRange);
		key.setInt("RespawnDelay", thisInstance.RespawnDelaySeconds);
		key.setDouble("Speed", thisInstance.sentrySpeed);
		key.setDouble("Weight", thisInstance.sentryWeight);
		key.setInt("Armor", thisInstance.Armor);
		key.setInt("Strength", thisInstance.Strength);
		key.setDouble("AttackRate", thisInstance.AttackRateSeconds);
	}

	@Override
	public boolean toggle() {
		isToggled = !isToggled;
		return isToggled;
	}

	public boolean isToggled() {
		return isToggled;
	}

	

	@EventHandler
	public void onDamaged(NPCDamageByEntityEvent  event) {


		if (thisInstance!=null){
			thisInstance.onDamage(event);	
		}

	}

	@EventHandler
	public void pushable(NPCPushEvent event) {

		if (thisInstance!=null){
			if (thisInstance.sentryStatus == Status.isHOSTILE) {
				event.setCancelled(false);
			}
			
		}
	}
	
	@EventHandler
	public void C2Reload(CitizensReloadEvent event) {

		//	onSpawn();
		//plugin.getServer().broadcastMessage("onReload");
	}
	
	
	@EventHandler
	public void onDamage(org.bukkit.event.entity.EntityDamageByEntityEvent  event) {

		Entity ent = event.getDamager();

	
		if(	ent  instanceof org.bukkit.entity.Projectile){
			ent = ((org.bukkit.entity.Projectile) ent).getShooter();
		}

		if(ent instanceof LivingEntity) {

			if (net.citizensnpcs.api.CitizensAPI.getNPCRegistry().isNPC(ent)){
			//	NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(ent);	
	
				if (thisInstance!=null){
									event.setDamage(event.getDamage() + thisInstance.Strength);
				}			
			}
		}

	}

	@EventHandler
	public void Despawn(NPCDespawnEvent event){
		//plugin.initializedSentries.remove(event.getNPC().getId());
		//plugin.getServer().broadcastMessage("onDespawn");
	}

	
	
	
	
}
