package net.aufdemrand.sentry.denizen.v8;

import java.util.Set;
import org.bukkit.entity.Player;

import net.aufdemrand.denizen.npc.dNPC;
import net.aufdemrand.denizen.npc.traits.TriggerTrait;
import net.aufdemrand.denizen.scripts.containers.core.InteractScriptContainer;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizen.utilities.debugging.dB.DebugElement;
import net.citizensnpcs.api.npc.NPC;

public class NpcdeathTrigger extends net.aufdemrand.denizen.scripts.triggers.AbstractTrigger{

	@Override
	public void onEnable() {
		// TODO Auto-generated method stub
	}

	public  boolean Die(Set<Player> _myDamamgers, NPC npc) {

		// Check if NPC has triggers.
		if (!npc.hasTrait(TriggerTrait.class)) return false;

		// Check if trigger is enabled.
		if (!npc.getTrait(TriggerTrait.class).isEnabled(name)) {
			dB.echoDebug(DebugElement.Header,  this.getName() +  " Trigger not enabled");
			return false;
		}

		dNPC theDenizen = denizen.getNPCRegistry().getDenizen(npc);

		dB.echoDebug(DebugElement.Header, "Parsing NPCDeath/Killers Trigger");

		boolean founone =false;

		for (Player thePlayer:_myDamamgers){

			if(thePlayer !=null && thePlayer.getLocation().distance(npc.getBukkitEntity().getLocation()) > 300) {
				dB.echoDebug(DebugElement.Header,  thePlayer.getName()+ " is to far away.");
				continue;
			}

			InteractScriptContainer script = theDenizen.getInteractScriptQuietly(thePlayer, this.getClass());

			if (parse(theDenizen, thePlayer, script))	founone = true;
		}

		return founone;
	}


}






