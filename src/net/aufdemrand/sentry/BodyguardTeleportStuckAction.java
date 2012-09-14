package net.aufdemrand.sentry;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.npc.NPC;

public class BodyguardTeleportStuckAction implements StuckAction{
	SentryInstance inst =null;

	BodyguardTeleportStuckAction(SentryInstance inst){
		this.inst = inst; 
	}

	@Override
	public boolean run(NPC npc, Navigator navigator) {
	//	inst.plugin.getServer().broadcastMessage("bgtp stuck action");
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
	        }
	        npc.getBukkitEntity().teleport(block.getLocation());
		//	inst.plugin.getServer().broadcastMessage("bgtp stuck teleport");
	        return true;
	    }
	    private static int MAX_ITERATIONS = 10;
	}