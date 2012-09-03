package net.aufdemrand.sentry;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.npc.NPC;

public class GiveUpStuckAction implements StuckAction{
	SentryInstance inst =null;
	
	GiveUpStuckAction(SentryInstance inst){
		this.inst = inst; 
	}

	@Override
	public void run(NPC npc, Navigator navigator) {
		if (!npc.isSpawned())
			return;
		
		inst.setTarget(null, false);
	}



}
