package cn.suiyi.relicward.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;

public final class PlayerCombat {
    public static boolean canHit(Player owner,LivingEntity target){
        if(target==owner||!target.isAlive()||target.isInvulnerable()||owner.isAlliedTo(target)||target.isAlliedTo(owner))return false;
        if(target instanceof TamableAnimal pet&&pet.isTame()&&owner.getUUID().equals(pet.getOwnerUUID()))return false;
        return !(target instanceof Player p)||(!p.isCreative()&&!p.isSpectator()&&owner.level() instanceof ServerLevel l&&l.getServer().isPvpAllowed()&&owner.canHarmPlayer(p));
    }
}
