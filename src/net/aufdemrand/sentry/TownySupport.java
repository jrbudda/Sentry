package net.aufdemrand.sentry;


import org.bukkit.entity.Player;

public class TownySupport{
	
    public static String getResidentTown(Player player) {
		com.palmergames.bukkit.towny.object.Resident resident;
		try {
			resident = com.palmergames.bukkit.towny.object.TownyUniverse.getDataSource().getResident(player.getName());
			String town;
			if(resident.hasTown()) {
				town = resident.getTown().getName();
				return town;	
			}
		} catch (com.palmergames.bukkit.towny.exceptions.NotRegisteredException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }
}