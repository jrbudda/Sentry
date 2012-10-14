package net.aufdemrand.sentry;

import java.util.List;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import net.aufdemrand.denizen.npc.DenizenNPC;
import net.aufdemrand.denizen.npc.DenizenTrait;
import net.aufdemrand.denizen.scripts.ScriptEngine.QueueType;
import net.aufdemrand.denizen.scripts.ScriptHelper;
import net.citizensnpcs.api.npc.NPC;

public class NpcdeathTriggerOwner extends net.aufdemrand.denizen.triggers.AbstractTrigger{

	public  boolean Die(NPC npc) {

		DenizenNPC theDenizen = plugin.getDenizenNPCRegistry().getDenizen(npc);

		if (theDenizen==null) {
			//how did u get here?
			return false;
		}

		try {
			if (theDenizen.getCitizensEntity().getTrait(DenizenTrait.class).triggerIsEnabled("Npcdeath") == false) {
				if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "NPCDeath Owner Trigger not enabled");
				return false; 
			}
		} catch (Exception e) {
			return false;
		}

		ScriptHelper sE = plugin.getScriptEngine().helper;
		if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "Parsing NPCDeath Owner Trigger.");

		String owner = npc.getTrait(net.citizensnpcs.api.trait.trait.Owner.class).getOwner();

		Player thePlayer =this.plugin.getServer().getPlayer(owner);

		if (thePlayer ==null) {
			if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "Owner not found!");
			return false;
		}

		String theScriptName = theDenizen.getInteractScript(thePlayer, NpcdeathTriggerOwner.class);

		if (theScriptName == null) {
			if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "No script found.");
			return false;
		}

		Integer theStep = sE.getCurrentStep(thePlayer, theScriptName);

		if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "looking for " + triggerName);

		if (plugin.getScripts().contains(sE.getTriggerPath(theScriptName, theStep, triggerName))) {
			List<String> theScript = sE.getScript(sE.getTriggerPath(theScriptName, theStep, triggerName ) +  sE.scriptString);
			if(theScript ==null || theScript.isEmpty()){
				if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "Commands missing or empty");
				return false;
			}
			sE.queueScriptEntries(thePlayer, sE.buildScriptEntries(thePlayer, theDenizen, theScript, theScriptName, theStep), QueueType.TRIGGER);
		}
		else{
			if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "...found no matching Npcdeath Trigger on step " + theStep + " of " + theScriptName);
		}


		return true;
	}


}






