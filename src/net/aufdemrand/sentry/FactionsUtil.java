package net.aufdemrand.sentry;

public class FactionsUtil {

	static boolean isFactionEnemy(String  faction1, String faction2) {
	if (Sentry.FactionsActive == false)return false;
	if (faction1.equalsIgnoreCase(faction2)) return false;
	try {
	
		com.massivecraft.factions.iface.RelationParticipator derp = com.massivecraft.factions.Factions.i.getByTag(faction2);
		
	return	 com.massivecraft.factions.Factions.i.getByTag(faction1).getRelationTo(derp) == com.massivecraft.factions.struct.Rel.ENEMY;
	
	} catch (Exception e) {
		return false;
	}
}
}