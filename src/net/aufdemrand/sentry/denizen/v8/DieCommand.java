package net.aufdemrand.sentry.denizen.v8;

import org.bukkit.entity.LivingEntity;

import net.aufdemrand.denizen.exceptions.CommandExecutionException;
import net.aufdemrand.denizen.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.aufdemrand.sentry.SentryInstance;
import net.aufdemrand.sentry.SentryTrait;


public class DieCommand extends net.aufdemrand.denizen.scripts.commands.AbstractCommand {

	@Override
	public void execute(ScriptEntry theEntry) throws CommandExecutionException {
		LivingEntity ent = theEntry.getNPC().getEntity();

		SentryInstance inst = theEntry.getNPC().getCitizen().getTrait(SentryTrait.class).getInstance();

		if (inst!=null){
			dB.log("Goodbye, cruel world... ");
			inst.die(false);
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
