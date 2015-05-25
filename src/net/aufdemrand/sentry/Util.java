package net.aufdemrand.sentry;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.Item;
import net.minecraft.server.v1_8_R3.LocaleI18n;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Util {

	public static Location getFireSource(LivingEntity from, LivingEntity to){

		Location loco =  from.getEyeLocation();
		Vector norman = to.getEyeLocation().subtract(loco).toVector();
		norman = normalizeVector(norman);
		norman.multiply(.5);

		Location loc =loco.add(norman);

		return loc;

	}

	public static Location leadLocation(Location loc, Vector victor, double t){

		return loc.clone().add(victor.clone().multiply(t));

	}

	public static void removeMount(int npcid){
		NPC vnpc = CitizensAPI.getNPCRegistry().getById(npcid);
		if (vnpc!=null) {
			if (vnpc.getEntity() != null) {
				vnpc.getEntity().setPassenger(null);
			}
			vnpc.destroy();
		}
	}

	public static boolean CanWarp(Entity player, NPC bodyguyard){

		if (player instanceof Player ){

			if (((Player) player).hasPermission("sentry.bodyguard.*")){
				//have * perm, which all players do by default.

				if (((Player) player).isPermissionSet("sentry.bodyguard." + player.getWorld().getName())){

					if (!((Player) player).hasPermission("sentry.bodyguard." + player.getWorld().getName())){
						//denied this world.
						return false;
					}

				}
				else 	return true;

			}


			if (((Player) player).hasPermission("sentry.bodyguard." + player.getWorld().getName())){
				//no * but specifically allowed this world.
				return true;
			}

		}

		return false;
	}

	public static String getLocalItemName(int MatId){
		if (MatId==0) return  "Hand";
		if(MatId < 256){
			Block b =getMCBlock(MatId);
			return	b.getName();
		}
		else{
			Item b =getMCItem(MatId);
			return LocaleI18n.get(b.getName() + ".name");
		}
	}

	public static double hangtime(double launchAngle, double v, double elev, double g){

		double a = v * Math.sin(launchAngle);
		double b = -2*g*elev;

		if(Math.pow(a, 2) + b < 0){
			return 0;
		}

		return (a + Math.sqrt(Math.pow(a, 2) + b))  /  g;

	}


	//check for obfuscation change
	public static Item getMCItem(int id) {
		return Item.getById(id);
	}
	//check for obfuscation change
	public static Block getMCBlock(int id) {
		return Block.getById(id);
	}

	public static Double launchAngle(Location from, Location to, double v, double elev, double g){

		Vector victor = from.clone().subtract(to).toVector();
		Double dist =  Math.sqrt(Math.pow(victor.getX(), 2) + Math.pow(victor.getZ(), 2));

		double v2 = Math.pow(v,2);
		double v4 = Math.pow(v,4);

		double derp =  g*(g*Math.pow(dist,2)+2*elev*v2);




		//Check unhittable.
		if( v4 < derp) {
			//target unreachable
			// use this to fire at optimal max angle launchAngle = Math.atan( ( 2*g*elev + v2) / (2*g*elev + 2*v2));
			return null;
		}
		else {
			//calc angle
			return Math.atan( (v2-   Math.sqrt(v4 - derp))/(g*dist));
		}



	}

	public static String format(String input, NPC npc, CommandSender player, int item, String amount){
		if(input == null) return null;
		input = input.replace("<NPC>",npc.getName());
		input = input.replace("<PLAYER>", player == null ? "" : player.getName());
		input = input.replace("<ITEM>", Util.getLocalItemName(item));
		input = input.replace("<AMOUNT>", amount.toString());
		input =	ChatColor.translateAlternateColorCodes('&', input);
		return input;
	}

	public static Vector normalizeVector(Vector victor){
		double	mag = Math.sqrt(Math.pow(victor.getX(), 2) + Math.pow(victor.getY(), 2)  + Math.pow(victor.getZ(), 2)) ;
		if (mag !=0) return victor.multiply(1/mag);
		return victor.multiply(0);
	}


}
