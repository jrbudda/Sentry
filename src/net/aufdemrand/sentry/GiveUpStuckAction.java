package net.aufdemrand.sentry;

import org.bukkit.Location;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.npc.NPC;

public class GiveUpStuckAction implements StuckAction{
	SentryInstance inst =null;
	
	GiveUpStuckAction(SentryInstance inst){
		this.inst = inst; 
	}

	@Override
	public boolean run(NPC npc, Navigator navigator) {
	//	inst.plugin.getServer().broadcastMessage("give up stuck action");
		if (!npc.isSpawned())		
			return false;
        Location base = navigator.getTargetAsLocation();
        
        if (base.getWorld() == npc.getEntity().getLocation().getWorld()){
            if (npc.getEntity().getLocation().distanceSquared(base) <= 4)
                return true;
        }

		inst.setTarget(null, false);
		return false;
	}



}
