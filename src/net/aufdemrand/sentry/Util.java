package net.aufdemrand.sentry;


import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.MobType;
import net.citizensnpcs.api.trait.trait.Owner;
import net.minecraft.server.v1_6_R2.Block;
import net.minecraft.server.v1_6_R2.Item;
import net.minecraft.server.v1_6_R2.LocaleI18n;

import org.bukkit.Location;
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

	public static void removeMount(NPC npc){

		if(npc.isSpawned() && npc.getBukkitEntity().isInsideVehicle()) {
			Entity v = npc.getBukkitEntity().getVehicle();
			v.setPassenger(null);
			NPC vnpc = CitizensAPI.getNPCRegistry().getNPC(v);
			if (vnpc != null){
				vnpc.destroy();
			}
		}

	}

	public static NPC getOrCreateMount(SentryInstance inst){
		NPC sentry = inst.myNPC;
		
		if( sentry.isSpawned()){

			NPC horseNPC = null;

			if (inst.isMounted()) {
				horseNPC =	net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getById(inst.MountID);
				if(horseNPC !=null){
					horseNPC.despawn();
				}
			}
			
			
			else {
				horseNPC =	net.citizensnpcs.api.CitizensAPI.getNPCRegistry().createNPC(org.bukkit.entity.EntityType.HORSE, sentry.getName() + "_Mount");
			}

			//look at my horse, my horse is amazing.
			horseNPC.spawn(sentry.getBukkitEntity().getLocation());
			horseNPC.data().set(NPC.DEFAULT_PROTECTED_METADATA, false);
			horseNPC.getNavigator().getDefaultParameters().attackStrategy(new MountAttackStrategy());
			horseNPC.getNavigator().getDefaultParameters().useNewPathfinder(false);
			horseNPC.getNavigator().getDefaultParameters().range(sentry.getNavigator().getDefaultParameters().range());

			horseNPC.getTrait(MobType.class).setType(org.bukkit.entity.EntityType.HORSE);

			Owner o = horseNPC.getTrait(Owner.class);
			o.setOwner(sentry.getTrait(Owner.class).getOwner());


			return horseNPC;

		}

		return null;
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
			Block b =Block.byId[MatId];
			return	b.getName();
		}
		else{
			Item b =Item.byId[MatId];
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



	public static Vector normalizeVector(Vector victor){
		double	mag = Math.sqrt(Math.pow(victor.getX(), 2) + Math.pow(victor.getY(), 2)  + Math.pow(victor.getZ(), 2)) ;
		if (mag !=0) return victor.multiply(1/mag);
		return victor.multiply(0);
	}


}
