package net.aufdemrand.sentry;

import org.bukkit.entity.LivingEntity;

public class CreeperAttackStrategy implements net.citizensnpcs.api.ai.AttackStrategy{


	@Override
	public boolean handle(LivingEntity arg0, LivingEntity arg1) {	
		((org.bukkit.craftbukkit.entity.CraftCreeper)arg0).getHandle().a(1);
		return true;
	}
}
