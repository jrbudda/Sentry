package net.aufdemrand.sentry.denizen.v8;

import org.bukkit.entity.Player;

import net.aufdemrand.denizen.npc.dNPC;
import net.aufdemrand.denizen.npc.traits.TriggerTrait;
import net.aufdemrand.denizen.scripts.containers.core.InteractScriptContainer;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizen.utilities.debugging.dB.DebugElement;
import net.citizensnpcs.api.npc.NPC;

public class NpcdeathTriggerOwner extends net.aufdemrand.denizen.scripts.triggers.AbstractTrigger{

	@Override
	public void onEnable() {
		// TODO Auto-generated method stub
	}

	public boolean Die(NPC npc) {

		// Check if NPC has triggers.
		if (!npc.hasTrait(TriggerTrait.class)) return false;

		// Check if trigger is enabled.
		if (!npc.getTrait(TriggerTrait.class).isEnabled(name)) {
			dB.echoDebug(DebugElement.Header,  this.getName() +  " Trigger not enabled");
			return false;
		}

		dNPC theDenizen = denizen.getNPCRegistry().getDenizen(npc);

		dB.echoDebug(DebugElement.Header, "Parsing NPCDeath/Owner Trigger.");

		String owner = npc.getTrait(net.citizensnpcs.api.trait.trait.Owner.class).getOwner();

		Player thePlayer =this.denizen.getServer().getPlayer(owner);

		if (thePlayer ==null) {
			dB.echoDebug(DebugElement.Header,  "Owner not found!");
			return false;
		}

		InteractScriptContainer script = theDenizen.getInteractScriptQuietly(thePlayer, this.getClass());


		return	parse(theDenizen, thePlayer, script);

	}

}






