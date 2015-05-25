package net.aufdemrand.sentry;

import org.bukkit.entity.LivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftCreeper;

public class CreeperAttackStrategy implements net.citizensnpcs.api.ai.AttackStrategy{

	@Override
	public boolean handle(LivingEntity arg0, LivingEntity arg1) {
		((CraftCreeper)arg0).getHandle().a(1);
		return true;
	}
}
