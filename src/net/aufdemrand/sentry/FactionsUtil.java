package net.aufdemrand.sentry;

import org.bukkit.entity.Player;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;

public class FactionsUtil {

	static boolean isFactionEnemy(String world, String  faction1, String faction2) {
		if (Sentry.FactionsActive == false)return false;
		if (faction1.equalsIgnoreCase(faction2)) return false;
		try {

			Faction f1 =	FactionColl.get().getByName(faction1);
			Faction f2 =	FactionColl.get().getByName(faction2);

			return f1.getRelationTo(f2) == com.massivecraft.factions.Rel.ENEMY;

		} catch (Exception e) {
			return false;
		}
	}
	

	static  String getFactionsTag(Player player) {
		if (Sentry.FactionsActive == false)return null;
		try {
			return	com.massivecraft.factions.entity.MPlayer.get(player).getFactionName();
		} catch (Exception e) {
			return null;
		}
	}
	
	
}