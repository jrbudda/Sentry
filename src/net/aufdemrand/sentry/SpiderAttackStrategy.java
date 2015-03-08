package net.aufdemrand.sentry;

import java.util.Random;


import net.minecraft.server.v1_8_R2.Entity;
import org.bukkit.craftbukkit.v1_8_R2.entity.CraftEntity;
import org.bukkit.entity.LivingEntity;

public class SpiderAttackStrategy implements net.citizensnpcs.api.ai.AttackStrategy{
	private Random random = new Random();

	Sentry plugin = null;

	public SpiderAttackStrategy(Sentry plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean handle(LivingEntity arg0, LivingEntity arg1) {

		plugin.debug("Spider ATTACK!");

		Entity entity = ((CraftEntity)arg1).getHandle();
		Entity me = ((CraftEntity)arg0).getHandle();

		if (this.random.nextInt(20) == 0) {
				double d0 = entity.locX - me.locX;
				double d1 = entity.locZ - me.locZ;
				double f2 = Math.sqrt(d0 * d0 + d1 * d1);

				me.motX = d0 /  f2 * 0.5D * 0.800000011920929D + me.motX * 0.20000000298023224D;
				me.motZ = d1 /  f2 * 0.5D * 0.800000011920929D + me.motZ * 0.20000000298023224D;
				me.motY = 0.4000000059604645D;
		}

		return false;
	}
}
