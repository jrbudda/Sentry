package net.aufdemrand.sentry;

//import java.util.HashMap;
import java.rmi.activation.ActivationException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
//import java.util.Map;
import java.util.logging.Level;

import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.Owner;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.massivecraft.factions.P;

public class Sentry extends JavaPlugin {

	public net.milkbowl.vault.permission.Permission perms = null;
	public boolean debug = false;;
	public boolean GroupsChecked = false; 
	public Queue<Projectile> arrows = new LinkedList<Projectile>();

	@Override
	public void onEnable() {

		if(getServer().getPluginManager().getPlugin("Citizens") == null || getServer().getPluginManager().getPlugin("Citizens").isEnabled() == false) {
			getLogger().log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
			getServer().getPluginManager().disablePlugin(this);	
			return;
		}	

		try {
			setupDenizenHook();
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "An error occured attempting to register the NPCDeath trigger with Denizen" + e.getMessage());
			_dplugin =null;
			_denizenTrigger =null;
		}

		if (_dplugin != null)	getLogger().log(Level.INFO,"NPCDeath Trigger and DIE command registered sucessfully with Denizen");
		else getLogger().log(Level.INFO,"Could not register with Denizen");

		if (setupTowny()) getLogger().log(Level.INFO,"Registered with Towny sucessfully" );
		else getLogger().log(Level.INFO,"Could not register with Towny. the TOWN target will not function." );

		if (setupFactions()) getLogger().log(Level.INFO,"Registered with Factions sucessfully" );
		else getLogger().log(Level.INFO,"Could not register with Factions. the FACTION target will not function." );


		CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(SentryTrait.class).withName("sentry"));
		this.getServer().getPluginManager().registerEvents(new SentryListener(this), this);


		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {

				//		int x = 0;
				//		int y = arrows.size();
				while (arrows.size() > 200) {
					Projectile a = arrows.remove();
					if (a!=null ){
						a.remove();
						//	x++;
					}

				}

				//		getLogger().log(Level.INFO,y + " arrows in queue " + x + " arrows removed " );

				//				List<World> worlds = getServer().getWorlds();
				//				//clean up dem arrows
				//				int x = 0;
				//				for (World world :worlds){
				//					Collection<Arrow> arrows = world.getEntitiesByClass(Arrow.class);
				//					for (Arrow arrow: arrows ){
				//		//				Integer derp = arrow.getTicksLived();
				//	//					getLogger().log(Level.INFO,derp.toString() );
				//						if (arrow.getTicksLived() == 0){
				//							arrow.remove();
				//							x++;
				//						}
				//					}
				//				}
				//
				//		if (x>0)	getLogger().log(Level.INFO,x + " arrows removed" );
				//

			}
		}, 40,  20*120);

	}



	//***Denizen Hook
	private NpcdeathTrigger _denizenTrigger = null;
	private Plugin _dplugin = null;

	public boolean SentryDeath(List<Player> players, NPC npc){
		if (_denizenTrigger !=null && npc !=null) return	_denizenTrigger.Die(players, npc);
		return false;
	}

	private void setupDenizenHook() throws ActivationException {
		_dplugin = this.getServer().getPluginManager().getPlugin("Denizen");
		if (_dplugin != null) {
			if (_dplugin.isEnabled()) {
				_denizenTrigger = new NpcdeathTrigger();
				_denizenTrigger.activateAs("Npcdeath");
				DieCommand dc = new DieCommand();
				dc.activateAs("DIE");
				dc.activateAs("LIVE");
			}
			else _dplugin =null;
		}
	}
	///

	public SentryInstance getSentry(Entity ent){
		if( ent == null) return null;
		NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(ent);
		if (npc !=null && npc.hasTrait(SentryTrait.class)){
			return npc.getTrait(SentryTrait.class).getInstance();
		}

		return null;
	}

	public SentryInstance getSentry(NPC npc){

		if (npc !=null && npc.hasTrait(SentryTrait.class)){
			return npc.getTrait(SentryTrait.class).getInstance();
		}

		return null;
	}

	private boolean setupPermissions()
	{
		try {

			if(getServer().getPluginManager().getPlugin("Vault") == null || getServer().getPluginManager().getPlugin("Vault").isEnabled() == false) {
				return false ;
			}	

			RegisteredServiceProvider<net.milkbowl.vault.permission.Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);

			if (permissionProvider != null) {
				perms = permissionProvider.getProvider();
			}

			return (perms != null);
		} catch (Exception e) {
			return false;
		}
	}



	@Override
	public void onDisable() {

		getLogger().log(Level.INFO, " v" + getDescription().getVersion() + " disabled.");
		Bukkit.getServer().getScheduler().cancelTasks(this);

	}


	public void doGroups(){
		if (!setupPermissions()) getLogger().log(Level.WARNING,"Could not register with Vault!  the GROUP target will not function.");
		else{
			try {
				String[] Gr = perms.getGroups();
				if (Gr.length == 0){
					getLogger().log(Level.WARNING,"No permission groups found.  the GROUP target will not function.");
					perms = null;
				}
				else getLogger().log(Level.INFO,"Registered sucessfully with Vault: " + Gr.length + " groups found." );

			} catch (Exception e) {
				getLogger().log(Level.WARNING,"Error getting groups.  the GROUP target will not function.");
				perms = null;
			}	
		}

		GroupsChecked = true;

	}

	private	boolean tryParseInt(String value)  
	{  
		try  
		{  
			Integer.parseInt(value);  
			return true;  
		} catch(NumberFormatException nfe)  
		{  
			return false;  
		}  
	}


	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] inargs) {

		if (inargs.length < 1) {
			sender.sendMessage(ChatColor.RED + "Use /sentry help for command reference.");
			return true;
		}

		CommandSender player = (CommandSender) sender;

		int npcid = -1;
		int i = 0;

		//did player specify a id?
		if (tryParseInt(inargs[0])) {
			npcid = Integer.parseInt(inargs[0]);
			i = 1;
		}

		String[] args = new String[inargs.length-i];

		for (int j = i; j < inargs.length; j++) {
			args[j-i] = inargs[j];
		}


		if (args.length < 1) {
			sender.sendMessage(ChatColor.RED + "Use /sentry help for command reference.");
			return true;
		}


		if (args[0].equalsIgnoreCase("help")) {

			player.sendMessage(ChatColor.GOLD + "------- Sentry Commands -------");
			player.sendMessage(ChatColor.GOLD + "You can use /sentry (id) [command] [args] to perform any of these commands on a sentry without having it selected.");			
			player.sendMessage(ChatColor.GOLD + "");
			player.sendMessage(ChatColor.GOLD + "/sentry target [add|remove] [target]");
			player.sendMessage(ChatColor.GOLD + "  Adds or removes a target to attack.");
			player.sendMessage(ChatColor.GOLD + "/sentry target [list|clear]");
			player.sendMessage(ChatColor.GOLD + "  View or clear the target list..");
			player.sendMessage(ChatColor.GOLD + "/sentry ignore [add|remove] [target]");
			player.sendMessage(ChatColor.GOLD + "  Adds or removes a target to ignore.");
			player.sendMessage(ChatColor.GOLD + "/sentry ignore [list|clear]");
			player.sendMessage(ChatColor.GOLD + "  View or clear the ignore list..");
			player.sendMessage(ChatColor.GOLD + "/sentry info");
			player.sendMessage(ChatColor.GOLD + "  View all Sentry attributes");
			player.sendMessage(ChatColor.GOLD + "/sentry speed [0-1.5]");
			player.sendMessage(ChatColor.GOLD + "  Sets speed of the Sentry when attacking.");
			player.sendMessage(ChatColor.GOLD + "/sentry health [1-2000000]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's Health .");
			player.sendMessage(ChatColor.GOLD + "/sentry armor [0-2000000]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's Armor.");
			player.sendMessage(ChatColor.GOLD + "/sentry strength [0-2000000]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's Strength.");
			player.sendMessage(ChatColor.GOLD + "/sentry attackrate [0.0-30.0]");
			player.sendMessage(ChatColor.GOLD + "  Sets the time between the Sentry's projectile attacks.");
			player.sendMessage(ChatColor.GOLD + "/sentry healrate [0.0-300.0]");
			player.sendMessage(ChatColor.GOLD + "  Sets the frequency the sentry will heal 1 point. 0 to disable.");
			player.sendMessage(ChatColor.GOLD + "/sentry range [1-100]");
			player.sendMessage(ChatColor.GOLD + "  Sets the Sentry's detection range.");
			player.sendMessage(ChatColor.GOLD + "/sentry warningrange [0-50]");
			player.sendMessage(ChatColor.GOLD + "  Sets the range, beyond the detection range, that the Sentry will warn targets.");
			player.sendMessage(ChatColor.GOLD + "/sentry respawn [-1-2000000]");
			player.sendMessage(ChatColor.GOLD + "  Sets the number of seconds after death the Sentry will respawn.");
			player.sendMessage(ChatColor.GOLD + "/sentry invincible");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to take no damage or knockback.");
			player.sendMessage(ChatColor.GOLD + "/sentry retaliate");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to always attack an attacker.");
			player.sendMessage(ChatColor.GOLD + "/sentry criticals");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to take critical hits and misses");
			player.sendMessage(ChatColor.GOLD + "/sentry drops");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to drop equipped items on death");
			player.sendMessage(ChatColor.GOLD + "/sentry spawn");
			player.sendMessage(ChatColor.GOLD + "  Set the sentry to respawn at its current location");
			player.sendMessage(ChatColor.GOLD + "/sentry warning 'The Test to use'");
			player.sendMessage(ChatColor.GOLD + "  Change the warning text. <NPC> and <PLAYER> can be used as placeholders");
			player.sendMessage(ChatColor.GOLD + "/sentry greeting 'The text to use'");
			player.sendMessage(ChatColor.GOLD + "  Change the greeting text. <NPC> and <PLAYER> can be used as placeholders");
			return true;
		}
		else if (args[0].equalsIgnoreCase("debug")) {

			if (debug) debug = false;
			else debug = true;

			player.sendMessage(ChatColor.GREEN + "Debug now: " + debug);
			return true;
		}

		NPC ThisNPC;

		if (npcid == -1){

			ThisNPC =	((Citizens)	this.getServer().getPluginManager().getPlugin("Citizens")).getNPCSelector().getSelected(sender);

			if(ThisNPC != null ){
				// Gets NPC Selected
				npcid = ThisNPC.getId();
			}

			else{
				player.sendMessage(ChatColor.RED + "You must have a NPC selected to use this command");
				return true;
			}			
		}


		ThisNPC = CitizensAPI.getNPCRegistry().getById(npcid); 

		if (ThisNPC == null) {
			player.sendMessage(ChatColor.RED + "NPC with id " + npcid + " not found");
			return true;
		}


		if (!ThisNPC.hasTrait(SentryTrait.class)) {
			player.sendMessage(ChatColor.RED + "That command must be performed on a Sentry!");
			return true;
		}


		if (sender instanceof Player){

			if (ThisNPC.getTrait(Owner.class).getOwner().equalsIgnoreCase(player.getName())) {
				//OK!

			}

			else {
				//not player is owner
				if (((Player)sender).hasPermission("citizens.admin") == false){
					//no c2 admin.
					player.sendMessage(ChatColor.RED + "You must be the owner of this Sentry to execute commands.");
					return true;
				}
				else{
					//has citizens.admin
					if (!ThisNPC.getTrait(Owner.class).getOwner().equalsIgnoreCase("server")) {
						//not server-owned NPC
						player.sendMessage(ChatColor.RED + "You, or the server, must be the owner of this Sentry to execute commands.");
						return true;
					}
				}


			}




		}



		// Commands

		SentryInstance inst =	ThisNPC.getTrait(SentryTrait.class).getInstance();

		if (args[0].equalsIgnoreCase("spawn")) {
			if(!player.hasPermission("sentry.spawn")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (ThisNPC.getBukkitEntity() == null) {
				player.sendMessage(ChatColor.RED + "Cannot set spawn while " +  ThisNPC.getName()  + " is dead.");
				return true;
			}
			inst.Spawn = ThisNPC.getBukkitEntity().getLocation();
			player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will respawn at its present location.");   // Talk to the player.
			return true;

		}

		//		if (args[0].equalsIgnoreCase("derp")) {
		//			org.bukkit.inventory.PlayerInventory inv = ((Player)sender).getInventory();
		//
		//			for (org.bukkit.inventory.ItemStack ii:inv.getContents()){
		//				if (ii ==null) {
		//					player.sendMessage("item null");
		//					continue;
		//				}
		//				player.sendMessage(ii.getTypeId() + ":" + ii.getData());   // Talk to the player.
		//
		//			}
		//
		//			org.bukkit.inventory.ItemStack is = new org.bukkit.inventory.ItemStack(358,1,(short)0,(byte)2);
		//			player.sendMessage(is.getData().toString()); 
		//			//Prints MAP(2), OK!
		//
		//			org.bukkit.inventory.ItemStack is2 = new org.bukkit.inventory.ItemStack(358);
		//			is2.setDurability((short)2);
		//			player.sendMessage(is2.getData().toString()); 
		//			//Prints MAP(2), OK!
		//
		//			org.bukkit.inventory.ItemStack is3 = new org.bukkit.inventory.ItemStack(358);
		//			is3.setData(new org.bukkit.material.MaterialData(358,(byte)2));
		//			player.sendMessage(is3.getData().toString()); 
		//			//Prints MAP(0), WHY???
		//
		//
		//			HashMap<Integer, ItemStack> poop = inv.removeItem(is);
		//
		//			return true;
		//
		//		}


		else if (args[0].equalsIgnoreCase("invincible")) {
			if(!player.hasPermission("sentry.options.invincible")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (inst.Invincible) {
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " now takes damage..");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " now INVINCIBLE.");   // Talk to the player.
			}

			inst.Invincible = !inst.Invincible;

			return true;
		}
		else if (args[0].equalsIgnoreCase("retaliate")) {
			if(!player.hasPermission("sentry.options.retaliate")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (inst.Retaliate) {
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will not retaliate.");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will retalitate against all attackers.");   // Talk to the player.
			}

			inst.Retaliate = !inst.Retaliate;

			return true;
		}
		else if (args[0].equalsIgnoreCase("criticals")) {
			if(!player.hasPermission("sentry.options.criticals")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (inst.LuckyHits) {
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will take normal damamge.");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN +ThisNPC.getName() + " will take critical hits.");   // Talk to the player.
			}

			inst.LuckyHits = !inst.LuckyHits;

			return true;
		}
		else if (args[0].equalsIgnoreCase("drops")) {
			if(!player.hasPermission("sentry.options.retaliate")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (!inst.DropInventory) {
				player.sendMessage(ChatColor.GREEN +  ThisNPC.getName() + " will drop items");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will not drop items.");   // Talk to the player.
			}

			inst.DropInventory = !inst.DropInventory;

			return true;
		}
		else if (args[0].equalsIgnoreCase("guard")) {
			if(!player.hasPermission("sentry.guard")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}		

			if (args.length > 1) {

				String arg = "";
				for (i=1;i<args.length;i++){
					arg += " " + args[i];
				}
				arg = arg.trim();



				if (inst.setGuardTarget(arg)) {
					player.sendMessage(ChatColor.GREEN +  ThisNPC.getName() + " is now guarding "+ arg );   // Talk to the player.
				}
				else {
					player.sendMessage(ChatColor.RED +  ThisNPC.getName() + " could not find " + arg + " in range.");   // Talk to the player.
				}
			}

			else
			{
				if (inst.guardTarget == null){
					player.sendMessage(ChatColor.RED +  ThisNPC.getName() + " is already set to guard its location" );   // Talk to the player.	
				}
				else{
					player.sendMessage(ChatColor.GREEN +  ThisNPC.getName() + " is now guarding its location. " );   // Talk to the player.
				}
				inst.setGuardTarget(null);
			}
			return true;
		}

		else if (args[0].equalsIgnoreCase("health")) {
			if(!player.hasPermission("sentry.stats.health")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Health is " + inst.sentryHealth);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry health [#]   note: Typically players");
				player.sendMessage(ChatColor.GOLD + "  have 20 HPs when fully healed");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 2000000) HPs = 2000000;
				if (HPs <1)  HPs =1;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " health set to " + HPs + ".");   // Talk to the player.
				inst.sentryHealth = HPs;
				inst.setHealth(HPs);
			}

			return true;
		}

		else if (args[0].equalsIgnoreCase("armor")) {
			if(!player.hasPermission("sentry.stats.armor")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Armor is " + inst.Armor);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry armor [#] ");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 2000000) HPs = 2000000;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " armor set to " + HPs + ".");   // Talk to the player.
				inst.Armor = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("strength")) {
			if(!player.hasPermission("sentry.stats.strength")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Strength is " + inst.Strength);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry strength # ");
				player.sendMessage(ChatColor.GOLD + "Note: At Strength 0 the Sentry will do no damamge. ");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 2000000) HPs = 2000000;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " strength set to " + HPs+ ".");   // Talk to the player.
				inst.Strength = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("nightvision")) {
			if(!player.hasPermission("sentry.stats.nightvision")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Night Vision is " + inst.NightVision);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry nightvision [0-16] ");
				player.sendMessage(ChatColor.GOLD + "Usage: 0 = See nothing, 16 = See everything. ");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 10) HPs = 10;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Night Vision set to " + HPs+ ".");   // Talk to the player.
				inst.NightVision = HPs;

			}

			return true;
		}

		else if (args[0].equalsIgnoreCase("respawn")) {
			if(!player.hasPermission("sentry.stats.respawn")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				if(inst.RespawnDelaySeconds == 0  ) player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " will not automatically respawn.");
				if(inst.RespawnDelaySeconds == -1 ) player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " will be deleted upon death");
				if(inst.RespawnDelaySeconds > 0 ) player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " respawns after " + inst.RespawnDelaySeconds + "s");

				player.sendMessage(ChatColor.GOLD + "Usage: /sentry respawn [-1 - 2000000] ");
				player.sendMessage(ChatColor.GOLD + "Usage: set to 0 to prevent automatic respawn");
				player.sendMessage(ChatColor.GOLD + "Usage: set to -1 to *permanently* delete the Sentry on death.");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 2000000) HPs = 2000000;
				if (HPs <-1)  HPs =-1;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " now respawns after " + HPs+ "s.");   // Talk to the player.
				inst.RespawnDelaySeconds = HPs;

			}
			return true;
		}

		else if (args[0].equalsIgnoreCase("speed")) {
			if(!player.hasPermission("sentry.stats.speed")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Speed is " + inst.sentrySpeed);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry speed [0.0 - 2.0]");
			}
			else {

				Float HPs = Float.valueOf(args[1]);
				if (HPs > 2.0) HPs = 2.0f;
				if (HPs <0.0)  HPs =0f;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " speed set to " + HPs + ".");   // Talk to the player.
				inst.sentrySpeed = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("attackrate")) {
			if(!player.hasPermission("sentry.stats.attackrate")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Projectile Attack Rate is " + inst.AttackRateSeconds + "s between shots." );
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry attackrate [0.0 - 30.0]");
			}
			else {

				Double HPs = Double.valueOf(args[1]);
				if (HPs > 30.0) HPs = 30.0;
				if (HPs < 0.0)  HPs = 0.0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Projectile Attack Rate set to " + HPs + ".");   // Talk to the player.
				inst.AttackRateSeconds = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("healrate")) {
			if(!player.hasPermission("sentry.stats.healrate")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Heal Rate is " + inst.HealRate + "s" );
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry healrate [0.0 - 300.0]");
				player.sendMessage(ChatColor.GOLD + "Usage: Set to 0 to disable healing");
			}
			else {

				Double HPs = Double.valueOf(args[1]);
				if (HPs > 300.0) HPs = 300.0;
				if (HPs < 0.0)  HPs = 0.0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Heal Rate set to " + HPs + ".");   // Talk to the player.
				inst.HealRate = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("range")) {
			if(!player.hasPermission("sentry.stats.range")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Range is " + inst.sentryRange);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry range [1 - 100]");
			}

			else {

				Integer HPs = Integer.valueOf(args[1]);
				if (HPs > 100) HPs = 100;
				if (HPs <1)  HPs =1;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " range set to " + HPs + ".");   // Talk to the player.
				inst.sentryRange = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("warningrange")) {
			if(!player.hasPermission("sentry.stats.warningrange")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Warning Range is " + inst.WarningRange);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry warningrangee [0 - 50]");
			}

			else {

				Integer HPs = Integer.valueOf(args[1]);
				if (HPs > 50) HPs = 50;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " warning range set to " + HPs + ".");   // Talk to the player.
				inst.WarningRange = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("warning")) {
			if(!player.hasPermission("sentry.warning")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length >=2) {
				String arg = "";
				for (i=1;i<args.length;i++){
					arg += " " + args[i];
				}
				arg = arg.trim();

				String str = arg.replaceAll("\"$", "").replaceAll("^\"", "").replaceAll("'$", "").replaceAll("^'", "");
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " warning message set to " + ChatColor.RESET + str + ".");   // Talk to the player.
				inst.WarningMessage = str;
			}
			else{
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Warning Message is: " + ChatColor.RESET + inst.WarningMessage);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry warning 'The Text to use'");
			}
			return true;
		}
		else if (args[0].equalsIgnoreCase("greeting")) {
			if(!player.hasPermission("sentry.greeting")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			if (args.length >=2) {

				String arg = "";
				for (i=1;i<args.length;i++){
					arg += " " + args[i];
				}
				arg = arg.trim();

				String str = arg.replaceAll("\"$", "").replaceAll("^\"", "").replaceAll("'$", "").replaceAll("^'", "");
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Greeting message set to "+ ChatColor.RESET  + str + ".");   // Talk to the player.
				inst.GreetingMessage = str;
			}
			else{
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Greeting Message is: " + ChatColor.RESET + inst.GreetingMessage);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry greeting 'The Text to use'");
			}
			return true;
		}

		else if (args[0].equalsIgnoreCase("info")) {
			if(!player.hasPermission("sentry.info")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			player.sendMessage(ChatColor.GOLD + "------- Sentry Info for " + ThisNPC.getName() + "------");
			player.sendMessage(ChatColor.GREEN + inst.getStats());
			player.sendMessage(ChatColor.GREEN + "Invincible: " + inst.Invincible + "  Retaliate: " + inst.Retaliate);
			player.sendMessage(ChatColor.GREEN + "Drops Items: " + inst.DropInventory+ "  Critical Hits: " + inst.LuckyHits);
			player.sendMessage(ChatColor.GREEN + "Respawn Delay: " + inst.RespawnDelaySeconds + "s" + " Friendly Fire: " + inst.FriendlyFire );
			player.sendMessage(ChatColor.BLUE + "Status: " + inst.sentryStatus);
			if (inst.meleeTarget == null){
				if(inst.projectileTarget ==null) player.sendMessage(ChatColor.BLUE + "Target: Nothing");
				else	player.sendMessage(ChatColor.BLUE + "Target: " + inst.projectileTarget.toString());
			}
			else 		player.sendMessage(ChatColor.BLUE + "Target: " + inst.meleeTarget.toString());

			if (inst.getGuardTarget() == null)	player.sendMessage(ChatColor.BLUE + "Guarding: My Location");
			else 		player.sendMessage(ChatColor.BLUE + "Guarding: " + inst.getGuardTarget().toString());

			return true;
		}

		else if (args[0].equalsIgnoreCase("target")) {
			if(!player.hasPermission("sentry.target")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			if (args.length<2 ){
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target add [entity:Name] or [player:Name] or [group:Name] or [entity:monster] or [entity:player]");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target remove [target]");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target clear");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry target list");
				return true;
			}

			else {

				String arg = "";
				for (i=2;i<args.length;i++){
					arg += " " + args[i];
				}
				arg = arg.trim();


				if (args[1].equals("add") && arg.length() > 0 && arg.split(":").length>1) {

					List<String> currentList =	inst.validTargets;
					currentList.add(arg.toUpperCase());
					inst.setGuardTarget(null);
					inst.setTarget(null, false);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Target added. Now targeting " + currentList.toString());
					return true;
				}

				else if (args[1].equals("remove") && arg.length() > 0 && arg.split(":").length>1) {

					List<String> currentList =	inst.validTargets;
					currentList.remove(arg.toUpperCase());
					inst.setGuardTarget(null);
					inst.setTarget(null, false);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Targets removed. Now targeting " + currentList.toString());
					return true;
				}

				else if (args[1].equals("clear")) {

					List<String> currentList =	inst.validTargets;
					currentList.clear();
					inst.setGuardTarget(null);
					inst.setTarget(null, false);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Targets cleared.");
					return true;
				}
				else if (args[1].equals("list")) {

					List<String> currentList =	inst.validTargets;
					player.sendMessage(ChatColor.GREEN + "Targets: " + currentList.toString());
					return true;
				}

				else {
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target list");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target clear");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target add type:name");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry target remove type:name");
					player.sendMessage(ChatColor.GOLD + "type:name can be any of the following: entity:MobName entity:monster entity:player entity:all player:PlayerName group:GroupName town:TownName nation:NationName faction:FactionName");


					return true;
				}
			}
		}

		else if (args[0].equalsIgnoreCase("ignore")) {
			if(!player.hasPermission("sentry.ignore")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			if (args.length<2 ){
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore list");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore clear");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore add type:name");
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore remove type:name");
				player.sendMessage(ChatColor.GOLD + "type:name can be any of the following: entity:MobName entity:monster entity:player entity:all player:PlayerName group:GroupName town:TownName nation:NationName faction:FactionName");

				return true;
			}

			else {

				String arg = "";
				for (i=2;i<args.length;i++){
					arg += " " + args[i];
				}
				arg = arg.trim();

				if (args[1].equals("add") && arg.length() > 0 && arg.split(":").length>1) {

					List<String> currentList =	inst.ignoreTargets;
					currentList.add(arg.toUpperCase());
					inst.setGuardTarget(null);
					inst.setTarget(null, false);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Ignore added. Now ignoring " + currentList.toString());
					return true;
				}

				else if (args[1].equals("remove") && arg.length() > 0 && arg.split(":").length>1) {

					List<String> currentList =	inst.ignoreTargets;
					currentList.remove(arg.toUpperCase());
					inst.setGuardTarget(null);
					inst.setTarget(null, false);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Ignore removed. Now ignoring " + currentList.toString());
					return true;
				}

				else if (args[1].equals("clear")) {

					List<String> currentList =	inst.ignoreTargets;
					currentList.clear();
					inst.setGuardTarget(null);
					inst.setTarget(null, false);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Ignore cleared.");
					return true;
				}
				else if (args[1].equals("list")) {

					List<String> currentList =	inst.ignoreTargets;
					player.sendMessage(ChatColor.GREEN + "Ignores: " + currentList.toString());
					return true;
				}

				else {

					player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore add [ENTITY:Name] or [PLAYER:Name] or [GROUP:Name] or [ENTITY:MONSTER]");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore remove [ENTITY:Name] or [PLAYER:Name] or [GROUP:Name] or [ENTITY:MONSTER]");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore clear");
					player.sendMessage(ChatColor.GOLD + "Usage: /sentry ignore list");
					return true;
				}
			}
		}
		return false;
	}

	//TownySupport
	boolean TownyActive = false;
	public String[] getResidentTownyInfo(Player player) {
		String[] info = {null,null};

		if (TownyActive == false)return info;

		com.palmergames.bukkit.towny.object.Resident resident;
		try {
			resident = com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().getResident(player.getName());

			if(resident.hasTown()) {
				info[1] = resident.getTown().getName();
				if( resident.getTown().hasNation()){
					info[0] =resident.getTown().getNation().getName();
				}

			}			
		} catch (Exception e) {
			return info;
		}

		return info;
	}



	private boolean setupTowny(){
		if(getServer().getPluginManager().getPlugin("Towny") != null){
			if(getServer().getPluginManager().getPlugin("Towny").isEnabled() == true) {
				TownyActive = true;
			}	
		}
		return TownyActive;
	}

	//FactionsSuport
	boolean FactionsActive = false;
	public  String getFactionsTag(Player player) {
		if (FactionsActive == false)return null;
		try {
			return	com.massivecraft.factions.FPlayers.i.get(player).getTag();
		} catch (Exception e) {
			getLogger().info("Error getting Faction " + e.getMessage());
			return null;
		}
	}

	private boolean setupFactions(){
		if(getServer().getPluginManager().getPlugin("Factions") != null){
			if(getServer().getPluginManager().getPlugin("Factions").isEnabled() == true) {
				FactionsActive = true;
			}	
		}
		return FactionsActive;
	}

	// End of CLASS

}
