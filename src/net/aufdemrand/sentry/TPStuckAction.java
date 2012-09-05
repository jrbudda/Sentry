package net.aufdemrand.sentry;


import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.npc.NPC;

public class TPStuckAction implements StuckAction {
    private TPStuckAction() {
        // singleton
    }

    @Override
    public void run(NPC npc, Navigator navigator) {
        if (!npc.isSpawned())
            return;
        npc.getBukkitEntity().teleport(navigator.getTargetAsLocation().add(1, 0, 0));
    }

    public static TPStuckAction INSTANCE = new TPStuckAction();
}