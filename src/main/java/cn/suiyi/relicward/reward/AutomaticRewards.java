package cn.suiyi.relicward.reward;

import cn.suiyi.relicward.RelicWard;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=RelicWard.ID)
public final class AutomaticRewards {
    public static void deliver(ServerPlayer p){
        if(!p.isAlive()||p.isRemoved())return;
        var ledger=RewardLedger.get(p.serverLevel());int count=ledger.deliverAll(p);
        if(count>0){p.displayClientMessage(Component.translatable("message.relicward.auto_rewards",count),false);p.playNotifySound(SoundEvents.PLAYER_LEVELUP,SoundSource.PLAYERS,.6F,1.2F);}
        boolean pending=ledger.hasPending(null,p.getUUID());
        if(pending&&!p.getPersistentData().getBoolean("RelicPendingNotice"))p.displayClientMessage(Component.translatable("message.relicward.auto_full"),false);
        p.getPersistentData().putBoolean("RelicPendingNotice",pending);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&e.player instanceof ServerPlayer p&&p.tickCount%20==0)deliver(p);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)deliver(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)deliver(p);}
}
