package cn.suiyi.relicward.client;

import cn.suiyi.relicward.ClientPreferences;
import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.combat.WardenAction;
import cn.suiyi.relicward.entity.BellWarden;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=RelicWard.ID,value=Dist.CLIENT)
public final class ImpactCamera {
    @SubscribeEvent public static void camera(ViewportEvent.ComputeCameraAngles e){
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null||!ClientPreferences.IMPACT_SHAKE.get())return;
        for(var boss:mc.level.getEntitiesOfClass(BellWarden.class,mc.player.getBoundingBox().inflate(24))){
            if(boss.action()!=WardenAction.SLAM&&boss.action()!=WardenAction.STAGGER)continue;
            double age=boss.actionTick()+e.getPartialTick()-(boss.action()==WardenAction.SLAM?24:0);
            if(age<0||age>8)continue;double s=(1-age/8)*Math.max(0,1-boss.distanceTo(mc.player)/24.0)*.45;
            e.setPitch(e.getPitch()+(float)(Math.sin(age*3.1)*s));e.setRoll(e.getRoll()+(float)(Math.sin(age*2.3)*s*.6));
        }
    }
}
