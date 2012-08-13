package net.aufdemrand.sentry;

import java.util.logging.Level;
import org.bukkit.entity.LivingEntity;
import net.aufdemrand.denizen.commands.AbstractCommand;
import net.aufdemrand.denizen.scripts.ScriptEntry;


public class DieCommand extends AbstractCommand {
	/* DIE */
	@Override
	// This is the method that is called when your command is ready to be executed.
	public boolean execute(ScriptEntry theEntry) {
		/* Execute the command, if all required variables are filled. */

		LivingEntity ent = theEntry.getDenizen().getEntity();

		if (ent!=null){
			if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "Goodbye, cruel world.");
			ent.setHealth(0);

			return true;
		}
		else	{
			if (plugin.debugMode) plugin.getLogger().log(Level.INFO, "Entity not found.");
			return false;
		}

	}

}

