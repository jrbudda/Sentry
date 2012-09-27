package net.aufdemrand.sentry;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.npc.NPC;

public class BodyguardTeleportStuckAction implements StuckAction{
	SentryInstance inst =null;
	Sentry plugin = null;
	BodyguardTeleportStuckAction(SentryInstance inst, Sentry plugin){
		this.inst = inst; 
		this.plugin = plugin;
	}

	@Override
	public boolean run(final NPC npc, Navigator navigator) {

		if (!npc.isSpawned())
			return false;
		Location base = navigator.getTargetAsLocation();

		if (base.getWorld() == npc.getBukkitEntity().getLocation().getWorld()){
			if (npc.getBukkitEntity().getLocation().distanceSquared(base) <= 4)
				return true;
		}

		Block block = base.getBlock();
		int iterations = 0;
		while (!block.isEmpty()) {
			block = block.getRelative(BlockFace.UP);
			if (++iterations >= MAX_ITERATIONS && !block.isEmpty())
				block = base.getBlock();
			break;
		}

		final Location loc = block.getLocation();
		
		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){

			@Override
			public void run() {
				npc.getBukkitEntity().teleport(loc);	
			}

		},2);

		//	inst.plugin.getServer().broadcastMessage("bgtp stuck teleport");
		return false;
	}
	private static int MAX_ITERATIONS = 10;
}