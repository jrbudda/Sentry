package net.aufdemrand.sentry;

import java.util.logging.Level;
import org.bukkit.entity.LivingEntity;
import net.aufdemrand.denizen.commands.AbstractCommand;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.aufdemrand.sentry.SentryInstance.Status;
import net.citizensnpcs.api.ai.GoalSelector;




public class DieCommand extends AbstractCommand {

	public boolean dielikeplayer = false;

	/* DIE */
	@Override
	// This is the method that is called when your command is ready to be executed.
	public boolean execute(ScriptEntry theEntry) {
		/* Execute the command, if all required variables are filled. */
		LivingEntity ent = theEntry.getDenizen().getEntity();

		SentryInstance inst = theEntry.getDenizen().getCitizensEntity().getTrait(SentryTrait.class).getInstance();

		if (ent!=null){
			if (theEntry.getCommand().equalsIgnoreCase("LIVE")){
				if (theEntry.getDenizen().getCitizensEntity().hasTrait(SentryTrait.class)){
					boolean deaggro = false;
					if (theEntry.arguments() != null)
						for(String arg : theEntry.arguments()){
							if (arg.equalsIgnoreCase("peace")) deaggro = true;
						}

					if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "RISE! " + theEntry.getDenizen().getName() + "!");
					theEntry.getDenizen().getCitizensEntity().getTrait(SentryTrait.class).getInstance().sentryStatus = net.aufdemrand.sentry.SentryInstance.Status.isLOOKING;
					if (deaggro) 	theEntry.getDenizen().getCitizensEntity().getTrait(SentryTrait.class).getInstance().setTarget(null, false);
				}			
			}
			else{

				if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "Goodbye, cruel world... ");
				inst.die( false);

			}		
			return true;
		}
		else	{
			if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "Entity not found.");
			return false;
		}
	}
}



