package net.aufdemrand.sentry;




import java.util.logging.Level;

import org.bukkit.entity.LivingEntity;


import net.aufdemrand.denizen.commands.AbstractCommand;

import net.aufdemrand.denizen.scripts.ScriptEntry;

import net.citizensnpcs.command.exception.CommandException;



/**

 * Your command! 

 * This class is a template for a Command in Denizen.

 * 

 * @author You!

 */


public class DieCommand extends AbstractCommand {


	/* COMMAND_NAME [TYPICAL] (ARGUMENTS) */



	/* 

	 * Arguments: [] - Required, () - Optional 

	 * [TYPICAL] argument with a description if necessary.

	 * (ARGUMENTS) should be clear and concise.

	 *   

	 * Modifiers:

	 * (MODIFIER:VALUE) These are typically advanced usage arguments.

	 * (DURATION:#) They should always be optional. Use standard modifiers

	 *   already established if at all possible.

	 *   

	 * Example Usage:

	 * COMMAND_NAME VALUE

	 * COMMAND_NAME DIFFERENTVALUE OPTIONALVALUE

	 * COMMAND_NAME ANOTHERVALUE 'MODIFIER:Show one-line examples.'

	 * 

	 */


	@Override



	// This is the method that is called when your command is ready to be executed.

	public boolean execute(ScriptEntry theEntry) throws CommandException {



		/* Initialize variables */ 



		// Typically initialized as null and filled as needed. Remember: theEntry

		// contains some information passed through the execution process.



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

		// Execution process.

		// Do whatever you want the command to do, here.

		/* Command has sucessfully finished */


		// else...



		/* Error processing */



		// Processing has gotten to here, there's probably not been enough arguments. 

		// Let's alert the console.





	}



	// You can include more methods in this class if necessary. Or not. :)



}

