package net.aufdemrand.sentry;

import org.bukkit.entity.Player;

import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColls;

public class FactionsUtil {

    static boolean isFactionEnemy(String world, String faction1, String faction2) {
        if (!Sentry.FactionsActive) return false;
        if (faction1.equalsIgnoreCase(faction2)) return false;
        try {

            Faction f1 = FactionColls.get().getForWorld(world).getByName(faction1);
            Faction f2 = FactionColls.get().getForWorld(world).getByName(faction2);

            return f1.getRelationTo(f2) == com.massivecraft.factions.Rel.ENEMY;

        } catch (Exception e) {
            return false;
        }
    }


    static String getFactionsTag(Player player) {
        if (!Sentry.FactionsActive) return null;
        try {
            return com.massivecraft.factions.entity.UPlayer.get(player).getFactionName();
        } catch (Exception e) {
            return null;
        }
    }


}
