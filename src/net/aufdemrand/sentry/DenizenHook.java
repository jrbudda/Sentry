package net.aufdemrand.sentry;

import java.util.Set;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.aufdemrand.denizen.Denizen;
import net.aufdemrand.denizen.exceptions.CommandExecutionException;
import net.aufdemrand.denizen.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizen.npc.traits.TriggerTrait;
import net.aufdemrand.denizen.objects.dNPC;
import net.aufdemrand.denizen.objects.dPlayer;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.aufdemrand.denizen.scripts.commands.AbstractCommand;
import net.aufdemrand.denizen.scripts.containers.core.InteractScriptContainer;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.denizen.utilities.debugging.dB.DebugElement;

import net.citizensnpcs.api.npc.NPC;

public class DenizenHook {

	static boolean DenizenActive = false;
	static Plugin DenizenPlugin;
	static Sentry SentryPlugin;

	public static boolean SentryDeath(Set<Player> _myDamamgers, NPC npc){
		if (!DenizenActive) return false;
		
		try {
			boolean a = false, b = false, c = false;

			net.aufdemrand.denizen.Denizen d = (Denizen)DenizenPlugin;

			NpcdeathTrigger npcd = d.getTriggerRegistry().get(NpcdeathTrigger.class);
			NpcdeathTriggerOwner npcdo = d.getTriggerRegistry().get(NpcdeathTriggerOwner.class);

			if (npc !=null) a=	npcd.Die(_myDamamgers, npc);
			if (npc !=null) c=	npcdo.Die(npc);
			return (a||b||c);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	static void setupDenizenHook()  {

		DenizenHook me = new DenizenHook();

		me.new NpcdeathTriggerOwner().activate().as("Npcdeathowner");
		me.new NpcdeathTrigger().activate().as("Npcdeath");

		DieCommand dc = me.new DieCommand();
		LiveCommand lc = me.new LiveCommand();

		dc.activate().as("die").withOptions("die", 0);
		lc.activate().as("live").withOptions("live", 0);

		DenizenActive  =true;
	}

	public static void DenizenAction(NPC npc, String action, org.bukkit.OfflinePlayer player){
		if(DenizenActive){
			dNPC dnpc = dNPC.mirrorCitizensNPC(npc);	
			if (dnpc != null) {
				try {
					dnpc.action(action, dPlayer.mirrorBukkitPlayer(player));	
				} catch (Exception e) {	
				}
			}
		}
	}


	private class LiveCommand extends AbstractCommand {

		@Override
		public void execute(ScriptEntry theEntry) throws CommandExecutionException {
			Entity ent = theEntry.getNPC().getEntity();

			SentryInstance inst = theEntry.getNPC().getCitizen().getTrait(SentryTrait.class).getInstance();

			if (ent!=null){
				if (theEntry.getNPC().getCitizen().hasTrait(SentryTrait.class)){
					boolean deaggro = false;

					for(String arg : theEntry.getArguments()){
						if (arg.equalsIgnoreCase("peace")) deaggro = true;
					}

					String db = "RISE! " + theEntry.getNPC().getName() + "!";
					if (deaggro) db += " ..And fight no more!";
					dB.log(db);

					if(inst !=null){
						inst.sentryStatus = net.aufdemrand.sentry.SentryInstance.Status.isLOOKING;
						if (deaggro) inst.setTarget(null, false);
					}
				}
			}
			else	{
				throw new CommandExecutionException("Entity not found");
			}
		}

		@Override
		public void parseArgs(ScriptEntry arg0) throws InvalidArgumentsException {

		}
	}

	private class DieCommand extends net.aufdemrand.denizen.scripts.commands.AbstractCommand {

		@Override
		public void execute(ScriptEntry theEntry) throws CommandExecutionException {
			Entity ent = theEntry.getNPC().getEntity();

			SentryInstance inst = theEntry.getNPC().getCitizen().getTrait(SentryTrait.class).getInstance();

			if (inst!=null){
				dB.log("Goodbye, cruel world... ");
				inst.die(false, org.bukkit.event.entity.EntityDamageEvent.DamageCause.CUSTOM);
			}
			else if (ent != null){
				ent.remove();
			}
			else	{
				throw new CommandExecutionException("Entity not found");
			}
		}

		@Override
		public void parseArgs(ScriptEntry arg0) throws InvalidArgumentsException {
			// TODO Auto-generated method stub

		}
	}

	private class NpcdeathTriggerOwner extends net.aufdemrand.denizen.scripts.triggers.AbstractTrigger{

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

			dNPC theDenizen = dNPC.mirrorCitizensNPC(npc);

			dB.echoDebug(DebugElement.Header, "Parsing NPCDeath/Owner Trigger.");

			String owner = npc.getTrait(net.citizensnpcs.api.trait.trait.Owner.class).getOwner();

			dPlayer thePlayer = net.aufdemrand.denizen.objects.dPlayer.valueOf(owner);

			if (thePlayer ==null) {
				dB.echoDebug(DebugElement.Header,  "Owner not found!");
				return false;
			}

			InteractScriptContainer script = theDenizen.getInteractScriptQuietly(thePlayer, this.getClass());


			return	parse(theDenizen, thePlayer, script);

		}

	}

	private class NpcdeathTrigger extends net.aufdemrand.denizen.scripts.triggers.AbstractTrigger{

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

			dNPC theDenizen = dNPC.mirrorCitizensNPC(npc);

			dB.echoDebug(DebugElement.Header, "Parsing NPCDeath/Killers Trigger");

			boolean founone =false;

			for (Player thePlayer:_myDamamgers){

				if(thePlayer !=null && thePlayer.getLocation().distance(npc.getEntity().getLocation()) > 300) {
					dB.echoDebug(DebugElement.Header,  thePlayer.getName()+ " is to far away.");
					continue;
				}

				InteractScriptContainer script = theDenizen.getInteractScriptQuietly(dPlayer.mirrorBukkitPlayer(thePlayer), this.getClass());

				if (parse(theDenizen, dPlayer.mirrorBukkitPlayer(thePlayer), script))	founone = true;
			}

			return founone;
		}


	}

}
