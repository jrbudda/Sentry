package net.aufdemrand.sentry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import java.util.logging.Level;


import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.Equipment;
import net.citizensnpcs.api.trait.trait.Owner;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

import com.palmergames.bukkit.towny.object.TownBlock;


public class Sentry extends JavaPlugin {

	public  int archer = -1;
	public Map<Integer, Double> ArmorBuffs = new HashMap<Integer, Double>();
	public Queue<Projectile> arrows = new LinkedList<Projectile>(); 
	public String BlockMessage = "";
	public  int bombardier = -1;
	public List<Integer> Boots  = new LinkedList<Integer>( java.util.Arrays.asList(301,305,309,313,317));

	public List<Integer> Chestplates  =  new LinkedList<Integer>(java.util.Arrays.asList(299,303,307,311,315));
	//SimpleClans sSuport
	boolean ClansActive = false;
	public int Crit1Chance;
	public String Crit1Message = "";
	public int Crit2Chance;
	public String Crit2Message = "";
	public int Crit3Chance;
	public String Crit3Message = "";
	public boolean debug = false;
	//***Denizen Hook
	public boolean DieLikePlayers = false;

	public boolean BodyguardsObeyProtection = true;

	public boolean IgnoreListInvincibility = true;

	//FactionsSuport
	static boolean FactionsActive = false;
	public int GlanceChance;
	public String GlanceMessage = "";
	public boolean GroupsChecked = false;
	public List<Integer> Helmets = new LinkedList<Integer>(java.util.Arrays.asList(298,302,306,310,314,91,86));

	public String HitMessage = "";

	public List<Integer> Leggings  = new LinkedList<Integer>( java.util.Arrays.asList(300,304,308,312,316));

	public int LogicTicks = 10;

	public  int magi = -1;

	public int MissChance;

	public String MissMessage = "";

	public net.milkbowl.vault.permission.Permission perms = null;

	public  int pyro1 = -1;

	public  int pyro2 = -1;
	public  int pyro3 = -1;
	public  int sc1 = -1;
	public  int sc2 = -1;
	public int sc3 = -1;
	public int SentryEXP = 5;
	public Map<Integer, Double> SpeedBuffs = new HashMap<Integer, Double>();
	public Map<Integer, Double> StrengthBuffs = new HashMap<Integer, Double>();
	//TownySupport
	boolean TownyActive = false;
	//War sSuport
	boolean WarActive = false;
	public int warlock1 = -1;
	public int warlock2 = -1;
	public int warlock3 = -1;
	public Map<Integer, List<PotionEffect>> WeaponEffects = new HashMap<Integer, List<PotionEffect>>();
	public  int witchdoctor = -1;

	boolean checkPlugin(String name){
		if(getServer().getPluginManager().getPlugin(name) != null){
			if(getServer().getPluginManager().getPlugin(name).isEnabled() == true) {
				return true;
			}	
		}
		return false;
	}

	public void debug(String s){
		if(debug) this.getServer().getLogger().info(s);
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
				else getLogger().log(Level.INFO,"Registered sucessfully with Vault: " + Gr.length + " groups found. The GROUP: target will function" );

			} catch (Exception e) {
				getLogger().log(Level.WARNING,"Error getting groups.  the GROUP target will not function.");
				perms = null;
			}	
		}

		GroupsChecked = true;

	}

	public boolean equip(NPC npc, ItemStack hand) {
		Equipment trait = npc.getTrait(Equipment.class);
		if (trait == null) return false;
		int slot = 0;
		Material type = hand == null ? Material.AIR : hand.getType();
		// First, determine the slot to edit

		if (Helmets.contains(type.getId())) slot = 1;
		else if (Chestplates.contains(type.getId())) slot = 2;
		else if (Leggings.contains(type.getId())) slot = 3;
		else if (Boots.contains(type.getId())) slot = 4;

		// Now edit the equipment based on the slot
		// Set the proper slot with one of the item

		if(type == Material.AIR){
			for (int i = 0; i < 5; i++) {
				if (trait.get(i) != null && trait.get(i).getType() != Material.AIR) {
					try {
						trait.set(i, null);
					} catch (Exception e) {
					}   
				}
			}
			return true;
		}
		else{
			ItemStack clone = hand.clone();
			clone.setAmount(1);

			try {
				trait.set(slot, clone);
			} catch (Exception e) {
				return false;
			}
			return true;	
		}

	}


	public  String getClan(Player player) {
		if (ClansActive == false)return null;
		try {
			net.sacredlabyrinth.phaed.simpleclans.Clan c =	net.sacredlabyrinth.phaed.simpleclans.SimpleClans.getInstance().getClanManager().getClanByPlayerName(player.getName());
			if(c!=null) return c.getName();
		} catch (Exception e) {
			getLogger().info("Error getting Clan " + e.getMessage());
			return null;
		}
		return null;
	}

	private int GetMat(String S){
		int item = -1;

		if (S == null) return item;

		String[] args = S.toUpperCase().split(":");


		org.bukkit.Material M = org.bukkit.Material.getMaterial(args[0]);

		if (item == -1) {	
			try {
				item = Integer.parseInt(S.split(":")[0]);
			} catch (Exception e) {
			}
		}

		if (M!=null){		
			item=M.getId();
		}

		return item;
	}


	public String getNationNameForLocation(Location l) {
		if (TownyActive == false)return null;
		try {
			TownBlock tb = com.palmergames.bukkit.towny.object.TownyUniverse.getTownBlock(l);
			if (tb !=null){
				if (tb.getTown().hasNation()) return tb.getTown().getNation().getName();
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}


	private PotionEffect getpot(String S){
		if (S == null) return null;
		String[] args = S.trim().split(":");

		PotionEffectType type = null;

		int dur = 10;
		int amp = 1;

		type = PotionEffectType.getByName((args[0].toUpperCase()));

		if (type == null) {
			try {
				type = PotionEffectType.getById(Integer.parseInt(args[0]));
			} catch (Exception e) {
			}
		}

		if (type==null) return null;

		if (args.length > 1){
			try {
				dur =	Integer.parseInt(args[1]);
			} catch (Exception e) {
			}
		}

		if (args.length > 2){
			try {
				amp =	Integer.parseInt(args[2]);
			} catch (Exception e) {
			}
		}

		return new PotionEffect(type,dur,amp);
	}

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


	public SentryInstance getSentry(Entity ent){
		if( ent == null) return null;
		if(!(ent instanceof org.bukkit.entity.LivingEntity)) return null;
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
	public  String getWarTeam(Player player) {
		if (WarActive == false)return null;
		try {
			com.tommytony.war.Team t =com.tommytony.war.Team.getTeamByPlayerName(player.getName());
			if (t!=null) return t.getName();
		} catch (Exception e) {
			getLogger().info("Error getting Team " + e.getMessage());
			return null;
		}
		return null;
	}

	public String getMCTeamName(Player player){
		Team t = getServer().getScoreboardManager().getMainScoreboard().getPlayerTeam(player);
		if (t != null){
			return t.getName();
		}
		return null;
	}

	boolean isNationEnemy(String Nation1, String Nation2) {
		if (TownyActive == false)return false;
		if (Nation1.equalsIgnoreCase(Nation2)) return false;
		try {

			if (!com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().hasNation(Nation1) || !com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().hasNation(Nation2)) return false;

			com.palmergames.bukkit.towny.object.Nation theNation1 = com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().getNation(Nation1);
			com.palmergames.bukkit.towny.object.Nation theNation2 = com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().getNation(Nation2);

			if(theNation1.hasEnemy(theNation2) ||theNation2.hasEnemy(theNation1) ) return true;

		} catch (Exception e) {
			return false;
		}

		return false;
	}


	public void loaditemlist(String key, List<Integer> list){
		List<String> strs = getConfig().getStringList(key);

		if (strs.size() > 0) list.clear();

		for(String s: getConfig().getStringList(key)){
			int	item = GetMat(s.trim());
			list.add(item);
		}

	}


	private void loadmap(String node, Map<Integer, Double> map){
		map.clear();
		for(String s: getConfig().getStringList(node)){
			String[] args = s.trim().split(" ");
			if(args.length != 2) continue;

			double val = 0;

			try {
				val = Double.parseDouble(args[1]);
			} catch (Exception e) {
			}

			int	item = GetMat(args[0]);

			if(item > 0 && val !=0 && !map.containsKey(item)){
				map.put(item, val);
			}
		}
	}
	private void loadpots(String node, Map<Integer, List<PotionEffect>> map){
		map.clear();
		for(String s: getConfig().getStringList(node)){
			String[] args = s.trim().split(" ");

			if (args.length < 2) continue;


			int item  = GetMat(args[0]);

			List<PotionEffect> list = new ArrayList<PotionEffect>();

			for(int i = 1;i< args.length;i++){
				PotionEffect val = getpot(args[i]);
				if(val !=null) list.add(val);

			}

			if(item >0 && list.isEmpty() == false)	map.put(item, list);


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


		Boolean set = null;
		if (args.length == 2){
			if (args[1].equalsIgnoreCase("true")) set = true;
			else if (args[1].equalsIgnoreCase("false")) set = false;
		}


		if (args[0].equalsIgnoreCase("help")) {

			player.sendMessage(ChatColor.GOLD + "------- Sentry Commands -------");
			player.sendMessage(ChatColor.GOLD + "You can use /sentry (id) [command] [args] to perform any of these commands on a sentry without having it selected.");			
			player.sendMessage(ChatColor.GOLD + "");
			player.sendMessage(ChatColor.GOLD + "/sentry reload");
			player.sendMessage(ChatColor.GOLD + "  reload the config.yml");
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
			player.sendMessage(ChatColor.GOLD + "/sentry equip [item|none]");
			player.sendMessage(ChatColor.GOLD + "  Equip an item on the Sentry, or remove all equipment.");
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
			player.sendMessage(ChatColor.GOLD + "/sentry follow [0-32]");
			player.sendMessage(ChatColor.GOLD + "  Sets the number of block away a bodyguard will follow. Default is 4");
			player.sendMessage(ChatColor.GOLD + "/sentry invincible");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to take no damage or knockback.");
			player.sendMessage(ChatColor.GOLD + "/sentry retaliate");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to always attack an attacker.");
			player.sendMessage(ChatColor.GOLD + "/sentry criticals");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to take critical hits and misses");
			player.sendMessage(ChatColor.GOLD + "/sentry drops");
			player.sendMessage(ChatColor.GOLD + "  Toggle the Sentry to drop equipped items on death");
			player.sendMessage(ChatColor.GOLD + "/sentry killdrops");
			player.sendMessage(ChatColor.GOLD + "  Toggle whether or not the sentry's victims drop items and exp");
			player.sendMessage(ChatColor.GOLD + "/sentry mount");
			player.sendMessage(ChatColor.GOLD + "  Toggle whether or not the sentry rides a mount");
			player.sendMessage(ChatColor.GOLD + "/sentry targetable");
			player.sendMessage(ChatColor.GOLD + "  Toggle whether or not the sentry is attacked by hostile mobs");
			player.sendMessage(ChatColor.GOLD + "/sentry spawn");
			player.sendMessage(ChatColor.GOLD + "  Set the sentry to respawn at its current location");
			player.sendMessage(ChatColor.GOLD + "/sentry warning 'The Test to use'");
			player.sendMessage(ChatColor.GOLD + "  Change the warning text. <NPC> and <PLAYER> can be used as placeholders");
			player.sendMessage(ChatColor.GOLD + "/sentry greeting 'The text to use'");
			player.sendMessage(ChatColor.GOLD + "  Change the greeting text. <NPC> and <PLAYER> can be used as placeholders");
			return true;
		}
		else if (args[0].equalsIgnoreCase("debug")) {

			debug = !debug;

			player.sendMessage(ChatColor.GREEN + "Debug now: " + debug);
			return true;
		}
		else if (args[0].equalsIgnoreCase("reload")) {
			if(!player.hasPermission("sentry.reload")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			this.reloadMyConfig();
			player.sendMessage(ChatColor.GREEN + "reloaded Sentry/config.yml");
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


		if (sender instanceof Player && !CitizensAPI.getNPCRegistry().isNPC((Entity) sender)){

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
			if (ThisNPC.getEntity() == null) {
				player.sendMessage(ChatColor.RED + "Cannot set spawn while " +  ThisNPC.getName()  + " is dead.");
				return true;
			}
			inst.Spawn = ThisNPC.getEntity().getLocation();
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

			inst.Invincible = set ==null ?  !inst.Invincible: set;

			if (!inst.Invincible) {
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " now takes damage..");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " now INVINCIBLE.");   // Talk to the player.
			}



			return true;
		}
		else if (args[0].equalsIgnoreCase("retaliate")) {
			if(!player.hasPermission("sentry.options.retaliate")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			inst.Retaliate = set ==null ?  !inst.Retaliate: set;

			if (!inst.Retaliate) {
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will not retaliate.");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will retalitate against all attackers.");   // Talk to the player.
			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("criticals")) {
			if(!player.hasPermission("sentry.options.criticals")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			inst.LuckyHits = set ==null ?  !inst.LuckyHits: set;

			if (!inst.LuckyHits) {
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will take normal damage.");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN +ThisNPC.getName() + " will take critical hits.");   // Talk to the player.
			}


			return true;
		}
		else if (args[0].equalsIgnoreCase("drops")) {
			if(!player.hasPermission("sentry.options.drops")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			inst.DropInventory = set ==null ?  !inst.DropInventory: set;

			if (inst.DropInventory) {
				player.sendMessage(ChatColor.GREEN +  ThisNPC.getName() + " will drop items");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will not drop items.");   // Talk to the player.
			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("killdrops")) {
			if(!player.hasPermission("sentry.options.killdrops")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			inst.KillsDropInventory = set ==null ?  !inst.KillsDropInventory: set;

			if (inst.KillsDropInventory) {
				player.sendMessage(ChatColor.GREEN +  ThisNPC.getName() + "'s kills will drop items or exp");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + "'s kills will not drop items or exp.");   // Talk to the player.
			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("targetable")) {
			if(!player.hasPermission("sentry.options.targetable")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			inst.Targetable = set ==null ?  !inst.Targetable: set;
			ThisNPC.data().set(NPC.TARGETABLE_METADATA, inst.Targetable);

			if (inst.Targetable) {
				player.sendMessage(ChatColor.GREEN +  ThisNPC.getName() + " will be targeted by mobs");   // Talk to the player.
			}
			else{
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will not be targeted by mobs");   // Talk to the player.
			}

			return true;
		}	else if (args[0].equalsIgnoreCase("mount")) {
			if(!player.hasPermission("sentry.options.mount")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			set = set ==null? !inst.isMounted() : set;


			if (set){
				player.sendMessage(ChatColor.GREEN +  ThisNPC.getName() + " is now Mounted");   // Talk to the player.
				inst.createMount();
				inst.mount();
			}
			else {
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " is no longer Mounted");   // Talk to the player.
				if(inst.isMounted()) Util.removeMount(inst.MountID);	
				inst.MountID = -1;
			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("guard")) {
			if(!player.hasPermission("sentry.guard")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}		

			boolean localonly = false;
			boolean playersonly = false;
			int start = 1;


			if (args.length > 1) {

				if (args[1].equalsIgnoreCase("-p")){
					start = 2;
					playersonly = true;
				}

				if (args[1].equalsIgnoreCase("-l")){
					start = 2;
					localonly = true;
				}

				String arg = "";
				for (i=start;i<args.length;i++){
					arg += " " + args[i];
				}
				arg = arg.trim();

				boolean ok = false;

				if(!playersonly){
					ok = inst.setGuardTarget(arg, false);
				}

				if(!localonly){
					ok = inst.setGuardTarget(arg, true);
				}

				if (ok) {
					player.sendMessage(ChatColor.GREEN +  ThisNPC.getName() + " is now guarding "+ arg );   // Talk to the player.
				}
				else {
					player.sendMessage(ChatColor.RED +  ThisNPC.getName() + " could not find " + arg + ".");   // Talk to the player.	
				}
				
			}

			else
			{
				if (inst.guardTarget == null){
					player.sendMessage(ChatColor.RED +  ThisNPC.getName() + " is already set to guard its immediate area" );   // Talk to the player.	
				}
				else{
					player.sendMessage(ChatColor.GREEN +  ThisNPC.getName() + " is now guarding its immediate area. " );   // Talk to the player.
				}
				inst.setGuardTarget(null, false);

			}
			return true;
		}

		else if (args[0].equalsIgnoreCase("follow")) {
			if(!player.hasPermission("sentry.stats.follow")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Follow Distance is " + inst.FollowDistance);
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry follow [#]. Default is 4. ");
			}
			else {

				int HPs = Integer.valueOf(args[1]);
				if (HPs > 32) HPs = 32;
				if (HPs <0)  HPs =0;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " follow distance set to " + HPs + ".");   // Talk to the player.
				inst.FollowDistance = HPs * HPs;

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
				if (HPs > 16) HPs = 16;
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
		else if (args[0].equalsIgnoreCase("equip")) {
			if(!player.hasPermission("sentry.equip")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			if (args.length <= 1) {
				player.sendMessage(ChatColor.RED + "You must specify a Item ID or Name. or specify 'none' to remove all equipment.");
			}

			else {


				if(ThisNPC.getEntity().getType() == org.bukkit.entity.EntityType.ENDERMAN || ThisNPC.getEntity().getType() == org.bukkit.entity.EntityType.PLAYER){
					if(args[1].equalsIgnoreCase("none")){
						//remove equipment
						equip(ThisNPC, null);
						inst.UpdateWeapon();
						player.sendMessage(ChatColor.YELLOW +ThisNPC.getName() + "'s equipment cleared."); 
					}
					else{
						int mat = GetMat(args[1]);
						if (mat>0){
							ItemStack is = new ItemStack(mat);
							if (equip(ThisNPC, is)){
								inst.UpdateWeapon();
								player.sendMessage(ChatColor.GREEN +" equipped " + is.getType().toString() + " on "+ ThisNPC.getName()); 
							}
							else player.sendMessage(ChatColor.RED +" Could not equip: invalid mob type?"); 
						}
						else player.sendMessage(ChatColor.RED +" Could not equip: unknown item name"); 
					}
				}
				else player.sendMessage(ChatColor.RED +" Could not equip: must be Player or Enderman type");
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
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " warning message set to " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&',str) + ".");   // Talk to the player.
				inst.WarningMessage = str;
			}
			else{
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Warning Message is: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&',inst.WarningMessage));
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
				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Greeting message set to "+ ChatColor.RESET  + ChatColor.translateAlternateColorCodes('&',str) + ".");   // Talk to the player.
				inst.GreetingMessage = str;
			}
			else{
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Greeting Message is: " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&',inst.GreetingMessage));
				player.sendMessage(ChatColor.GOLD + "Usage: /sentry greeting 'The Text to use'");
			}
			return true;
		}

		else if (args[0].equalsIgnoreCase("info")) {
			if(!player.hasPermission("sentry.info")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			player.sendMessage(ChatColor.GOLD + "------- Sentry Info for (" +ThisNPC.getId() + ") " +  ThisNPC.getName() + "------");
			player.sendMessage(ChatColor.GREEN + inst.getStats());
			player.sendMessage(ChatColor.GREEN + "Invincible: " + inst.Invincible + "  Retaliate: " + inst.Retaliate);
			player.sendMessage(ChatColor.GREEN + "Drops Items: " + inst.DropInventory+ "  Critical Hits: " + inst.LuckyHits);
			player.sendMessage(ChatColor.GREEN + "Kills Drop Items: "+ inst.KillsDropInventory + "  Respawn Delay: " + inst.RespawnDelaySeconds + "s");
			player.sendMessage(ChatColor.BLUE + "Status: " + inst.sentryStatus);
			if (inst.meleeTarget == null){
				if(inst.projectileTarget ==null) player.sendMessage(ChatColor.BLUE + "Target: Nothing");
				else	player.sendMessage(ChatColor.BLUE + "Target: " + inst.projectileTarget.toString());
			}
			else 		player.sendMessage(ChatColor.BLUE + "Target: " + inst.meleeTarget.toString());

			if (inst.getGuardTarget() == null)	player.sendMessage(ChatColor.BLUE + "Guarding: My Surroundings");
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


				if(arg.equalsIgnoreCase("nationenemies") && inst.myNPC.isSpawned()){
					String natname = getNationNameForLocation(inst.myNPC.getEntity().getLocation());
					if (natname !=null) {
						arg += ":" + natname;
					}
					else 	{
						player.sendMessage(ChatColor.RED + "Could not get Nation for this NPC's location");
						return true;
					}
				}

				if (args[1].equals("add") && arg.length() > 0 && arg.split(":").length>1) {


					if (!inst.containsTarget(arg.toUpperCase())) inst.validTargets.add(arg.toUpperCase());
					inst.processTargets();
					inst.setTarget(null, false);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Target added. Now targeting " + 	inst.validTargets.toString());
					return true;
				}

				else if (args[1].equals("remove") && arg.length() > 0 && arg.split(":").length>1) {

					inst.validTargets.remove(arg.toUpperCase());
					inst.processTargets();
					inst.setTarget(null, false);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Targets removed. Now targeting " + 	inst.validTargets.toString());
					return true;
				}

				else if (args[1].equals("clear")) {


					inst.validTargets.clear();
					inst.processTargets();
					inst.setTarget(null, false);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Targets cleared.");
					return true;
				}
				else if (args[1].equals("list")) {
					player.sendMessage(ChatColor.GREEN + "Targets: " + 	inst.validTargets.toString());
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
					if (!inst.containsIgnore(arg.toUpperCase()))	inst.ignoreTargets.add(arg.toUpperCase());
					inst.processTargets();
					inst.setTarget(null, false);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Ignore added. Now ignoring " + inst.ignoreTargets.toString());
					return true;
				}

				else if (args[1].equals("remove") && arg.length() > 0 && arg.split(":").length>1) {

					inst.ignoreTargets.remove(arg.toUpperCase());
					inst.processTargets();
					inst.setTarget(null, false);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Ignore removed. Now ignoring " + inst.ignoreTargets.toString());
					return true;
				}

				else if (args[1].equals("clear")) {

					inst.ignoreTargets.clear();
					inst.processTargets();
					inst.setTarget(null, false);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " Ignore cleared.");
					return true;
				}
				else if (args[1].equals("list")) {

					player.sendMessage(ChatColor.GREEN + "Ignores: " + inst.ignoreTargets.toString());
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
	@Override
	public void onDisable() {

		getLogger().log(Level.INFO, " v" + getDescription().getVersion() + " disabled.");
		Bukkit.getServer().getScheduler().cancelTasks(this);

	}


	boolean DenizenActive = false;

	@Override
	public void onEnable() {

		if(getServer().getPluginManager().getPlugin("Citizens") == null || getServer().getPluginManager().getPlugin("Citizens").isEnabled() == false) {
			getLogger().log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
			getServer().getPluginManager().disablePlugin(this);	
			return;
		}	

		try {

			if  (checkPlugin("Denizen")){	
				String vers = getServer().getPluginManager().getPlugin("Denizen").getDescription().getVersion();
				if(vers.startsWith("0.7") || vers.startsWith("0.8")) {
					getLogger().log(Level.WARNING, "Sentry is not compatible with Denizen .7 or .8");
				}
				else if(vers.startsWith("0.9")){
					DenizenHook.SentryPlugin = this;
					DenizenHook.DenizenPlugin = getServer().getPluginManager().getPlugin("Denizen");
					DenizenHook.setupDenizenHook();
					DenizenActive = true;
				}
				else {
					getLogger().log(Level.WARNING, "Unknown version of Denizen");
				}
			}
		}
		catch(NoClassDefFoundError e){
			getLogger().log(Level.WARNING, "An error occured attempting to register with Denizen " + e.getMessage());
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "An error occured attempting to register with Denizen " + e.getMessage());
		}

		if (DenizenActive)	getLogger().log(Level.INFO,"NPCDeath Triggers and DIE/LIVE command registered sucessfully with Denizen");
		else getLogger().log(Level.INFO,"Could not register with Denizen");


		if (checkPlugin("Towny")) {
			getLogger().log(Level.INFO,"Registered with Towny sucessfully. the TOWN: and NATION: targets will function" );
			TownyActive = true;
		}
		else getLogger().log(Level.INFO,"Could not find or register with Towny" );

		if (checkPlugin("Factions")){
			getLogger().log(Level.INFO,"Registered with Factions sucessfully. the FACTION: target will function" );
			FactionsActive = true;
		}
		else getLogger().log(Level.INFO,"Could not find or register with Factions." );

		if (checkPlugin("War")){
			getLogger().log(Level.INFO,"Registered with War sucessfully. The TEAM: target will function" );
			WarActive = true;
		}
		else getLogger().log(Level.INFO,"Could not find or register with War. " );

		if (checkPlugin("SimpleClans")){
			getLogger().log(Level.INFO,"Registered with SimpleClans sucessfully. The CLAN: target will function" );
			ClansActive = true;
		}
		else getLogger().log(Level.INFO,"Could not find or register with SimpleClans. " );

		CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(SentryTrait.class).withName("sentry"));

		this.getServer().getPluginManager().registerEvents(new SentryListener(this), this);


		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				//Unloaded chunk arrow cleanup
				while (arrows.size() > 200) {
					Projectile a = arrows.remove();
					if (a!=null ){
						a.remove();
						//	x++;
					}
				}
			}
		}, 40,  20*120);

		reloadMyConfig();
	}
	private void reloadMyConfig(){
		this.saveDefaultConfig();
		this.reloadConfig();
		loadmap("ArmorBuffs", ArmorBuffs);
		loadmap("StrengthBuffs", StrengthBuffs);
		loadmap("SpeedBuffs", SpeedBuffs);
		loadpots("WeaponEffects",WeaponEffects);
		loaditemlist("Helmets", Helmets);
		loaditemlist("Chestplates",Chestplates);
		loaditemlist("Leggings", Leggings);
		loaditemlist("Boots", Boots);
		archer = GetMat(getConfig().getString("AttackTypes.Archer",null));
		pyro1 = GetMat(getConfig().getString("AttackTypes.Pyro1",null));
		pyro2 = GetMat(getConfig().getString("AttackTypes.Pyro2",null));
		pyro3 = GetMat(getConfig().getString("AttackTypes.Pyro3",null));
		bombardier = GetMat(getConfig().getString("AttackTypes.Bombardier",null));
		sc1 = GetMat(getConfig().getString("AttackTypes.StormCaller1",null));
		sc2 = GetMat(getConfig().getString("AttackTypes.StormCaller2",null));
		witchdoctor = GetMat(getConfig().getString("AttackTypes.WitchDoctor",null));
		magi = GetMat(getConfig().getString("AttackTypes.IceMagi",null));
		sc3 = GetMat(getConfig().getString("AttackTypes.StormCaller3",null));
		warlock1 = GetMat(getConfig().getString("AttackTypes.Warlock1",null));
		warlock2 = GetMat(getConfig().getString("AttackTypes.Warlock2",null));
		warlock3 = GetMat(getConfig().getString("AttackTypes.Warlock3",null));
		DieLikePlayers = getConfig().getBoolean("Server.DieLikePlayers",false);
		BodyguardsObeyProtection = getConfig().getBoolean("Server.BodyguardsObeyProtection",true);
		IgnoreListInvincibility =  getConfig().getBoolean("Server.IgnoreListInvincibility",true);
		LogicTicks = getConfig().getInt("Server.LogicTicks",10);
		SentryEXP = getConfig().getInt("Server.ExpValue",5);
		MissMessage = getConfig().getString("GlobalTexts.Miss", null);
		HitMessage = getConfig().getString("GlobalTexts.Hit", null);
		BlockMessage = getConfig().getString("GlobalTexts.Block", null);
		Crit1Message = getConfig().getString("GlobalTexts.Crit1", null);
		Crit2Message = getConfig().getString("GlobalTexts.Crit2", null);
		Crit3Message = getConfig().getString("GlobalTexts.Crit3", null);
		GlanceMessage = getConfig().getString("GlobalTexts.Glance", null);
		MissChance = getConfig().getInt("HitChances.Miss",0);
		GlanceChance = getConfig().getInt("HitChances.Glance",0);
		Crit1Chance = getConfig().getInt("HitChances.Crit1",0);
		Crit2Chance = getConfig().getInt("HitChances.Crit2",0);
		Crit3Chance = getConfig().getInt("HitChances.Crit3",0);


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
			e.printStackTrace();
			return false;
		}
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






}

