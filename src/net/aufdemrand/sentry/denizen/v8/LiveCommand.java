package net.aufdemrand.sentry.denizen.v8;

import org.bukkit.entity.LivingEntity;

import net.aufdemrand.denizen.exceptions.CommandExecutionException;
import net.aufdemrand.denizen.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.aufdemrand.denizen.scripts.commands.AbstractCommand;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.sentry.SentryInstance;
import net.aufdemrand.sentry.SentryTrait;


public class LiveCommand extends AbstractCommand {

	@Override
	public void execute(ScriptEntry theEntry) throws CommandExecutionException {
		LivingEntity ent = theEntry.getNPC().getEntity();

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



