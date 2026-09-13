package cn.suiyi.relicward.item;

import cn.suiyi.relicward.*;
import cn.suiyi.relicward.combat.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.*;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=RelicWard.ID)
public final class BellShieldMechanics {
    public static final int PERFECT_WINDOW=10,COOLDOWN=80,START_OFFSET=5;
    @SubscribeEvent public static void start(net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Start event){
        if(event.getItem().is(ChapterContent.SHIELD.get()))event.setDuration(Math.max(1,event.getDuration()-START_OFFSET));
    }
    public static int elapsed(Player player){return Math.max(0,player.getTicksUsingItem()-START_OFFSET);}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void block(ShieldBlockEvent event){
        if(!(event.getEntity() instanceof Player player)||!(player.level() instanceof ServerLevel level)
                ||!player.getUseItem().is(ChapterContent.SHIELD.get())||!player.isBlocking()||event.getBlockedDamage()<=0
                ||elapsed(player)>PERFECT_WINDOW||RelicEquipment.cooldown(player,"Shield")>0)return;
        RelicEquipment.setCooldown(player,"Shield",COOLDOWN);event.setShieldTakesDamage(false);
        player.removeEffect(ArmorFracture.EFFECT.get());
        if(event.getDamageSource().getEntity() instanceof LivingEntity attacker&&PlayerCombat.canHit(player,attacker)
                &&player.distanceToSqr(attacker)<=16&&player.hasLineOfSight(attacker)){
            ArmorFracture.apply(attacker,80);attacker.knockback(.9,player.getX()-attacker.getX(),player.getZ()-attacker.getZ());
        }
        var pulse=RelicWard.MAUL_PULSE.get().create(level);
        if(pulse!=null){pulse.setPos(player.position().add(0,.02,0));level.addFreshEntity(pulse);}
        level.playSound(null,player.blockPosition(),SoundEvents.BELL_BLOCK,SoundSource.PLAYERS,1,1.4F);
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.relicward.perfect_block"),true);
    }
}
