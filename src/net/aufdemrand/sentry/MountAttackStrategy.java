package net.aufdemrand.sentry;


import org.bukkit.entity.LivingEntity;
import net.citizensnpcs.api.CitizensAPI;

public class MountAttackStrategy implements net.citizensnpcs.api.ai.AttackStrategy{
  // make the rider attack when in range.
	
	@Override
    public boolean handle(LivingEntity attacker, LivingEntity bukkitTarget) {
		if(attacker == bukkitTarget) return true;
		if(attacker.getPassenger() !=null){
			return CitizensAPI.getNPCRegistry().getNPC(attacker).getNavigator().getDefaultParameters().attackStrategy().handle((LivingEntity) attacker.getPassenger(), bukkitTarget);
		}
		else {
			return CitizensAPI.getNPCRegistry().getNPC(attacker).getNavigator().getDefaultParameters().attackStrategy().handle((LivingEntity) attacker, bukkitTarget);
		}		
	}

}
