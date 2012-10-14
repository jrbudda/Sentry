package net.aufdemrand.sentry;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.entity.Player;

import net.aufdemrand.denizen.npc.DenizenNPC;
import net.aufdemrand.denizen.npc.DenizenTrait;
import net.aufdemrand.denizen.scripts.ScriptEngine.QueueType;
import net.aufdemrand.denizen.scripts.ScriptHelper;
import net.citizensnpcs.api.npc.NPC;

public class NpcdeathTrigger extends net.aufdemrand.denizen.triggers.AbstractTrigger{

	public  boolean Die(Set<Player> _myDamamgers, NPC npc) {

		DenizenNPC theDenizen = plugin.getDenizenNPCRegistry().getDenizen(npc);

		if (theDenizen==null) {
			//how did u get here?
			return false;
		}

		try {
			if (theDenizen.getCitizensEntity().getTrait(DenizenTrait.class).triggerIsEnabled("Npcdeath") == false && 
					theDenizen.getCitizensEntity().getTrait(DenizenTrait.class).triggerIsEnabled("Npcdeath killers") == false) {
				if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "Npcdeath Trigger not enabled");
				return false; 
			}
		} catch (Exception e) {
			return false;
		}

		ScriptHelper sE = plugin.getScriptEngine().helper;
		if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "Parsing NPCDeath Killers Trigger.");

		boolean founone =false;

		for (Player thePlayer:_myDamamgers){
			if(thePlayer !=null && thePlayer.getLocation().distance(npc.getBukkitEntity().getLocation()) > 300) {
				if (plugin.debugMode) plugin.getLogger().log(Level.INFO, thePlayer.getName()+ " is to far away.");
				continue;
			}

			String theScriptName = theDenizen.getInteractScript(thePlayer, NpcdeathTrigger.class);
			if (theScriptName == null) {
				if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "No script found.");
				continue;
			}

			Integer theStep = sE.getCurrentStep(thePlayer, theScriptName);

			if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "looking for " + triggerName);

			if (plugin.getScripts().contains(sE.getTriggerPath(theScriptName, theStep, triggerName))) {
				List<String> theScript = sE.getScript(sE.getTriggerPath(theScriptName, theStep, triggerName ) +  sE.scriptString);
				if(theScript ==null || theScript.isEmpty()){
					if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "Commands missing or empty");
					continue;
				}
				sE.queueScriptEntries(thePlayer, sE.buildScriptEntries(thePlayer, theDenizen, theScript, theScriptName, theStep), QueueType.TRIGGER);
				founone = true;
			}
			else{
				if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "...found no matching Npcdeath Trigger on step " + theStep + " of " + theScriptName);
			}



		}
		return founone;
	}


}






