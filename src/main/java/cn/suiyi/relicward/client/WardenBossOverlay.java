package cn.suiyi.relicward.client;

import cn.suiyi.relicward.ClientPreferences;
import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.entity.BellWarden;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid=RelicWard.ID, value=Dist.CLIENT)
public final class WardenBossOverlay {
    private static final ResourceLocation ICON_P1 = new ResourceLocation(RelicWard.ID, "textures/gui/boss_bar/bell_warden_phase1.png");
    private static final ResourceLocation ICON_P2 = new ResourceLocation(RelicWard.ID, "textures/gui/boss_bar/bell_warden_phase2.png");
    private static final ResourceLocation BRACKET_P1 = new ResourceLocation(RelicWard.ID, "textures/gui/boss_bar/bar_right_bracket_p1.png");
    private static final ResourceLocation BRACKET_P2 = new ResourceLocation(RelicWard.ID, "textures/gui/boss_bar/bar_right_bracket_p2.png");
    private static final Map<UUID, Trail> TRAILS=new HashMap<>();
    private static Object lastLevel;
    private static class Trail { float value; long time; Trail(float v,long t){value=v;time=t;} }

    @SubscribeEvent public static void render(CustomizeGuiOverlayEvent.BossEventProgress event) {
        var mc=Minecraft.getInstance();
        if(mc.level!=lastLevel){TRAILS.clear();lastLevel=mc.level;}
        if(mc.level==null||!ClientPreferences.CUSTOM_BOSS_BAR.get())return;
        BellWarden boss=null;
        for(var entity:mc.level.entitiesForRendering())
            if(entity instanceof BellWarden w&&w.bossBarId().filter(event.getBossEvent().getId()::equals).isPresent()){boss=w;break;}
        if(boss==null)return;
        event.setCanceled(true);event.setIncrement(42);
        var g=event.getGuiGraphics();
        int width=Math.min(256,mc.getWindow().getGuiScaledWidth()-48);
        int x=(mc.getWindow().getGuiScaledWidth()-width)/2,y=event.getY();
        float progress=Mth.clamp(event.getBossEvent().getProgress(),0,1);
        long now=net.minecraft.Util.getMillis();
        TRAILS.entrySet().removeIf(e->now-e.getValue().time>3000);
        var trail=TRAILS.computeIfAbsent(event.getBossEvent().getId(),id->new Trail(progress,now));
        trail.value=Math.max(progress,trail.value-Math.min(0.1F,(now-trail.time)/1000F)*0.22F);trail.time=now;
        int gold=0xFFBE9958, light=0xFFFFDCA0;
        int fill=boss.phase()==2?0xFFE57237:0xFF4DAC98;
        g.fill(x-3,y-2,x+width+3,y+13,0xD9101819);
        g.fill(x-2,y-1,x+width+2,y+12,gold);
        g.fill(x,y+1,x+width,y+10,0xFF172725);
        g.fill(x+1,y+2,x+1+(int)((width-2)*trail.value),y+9,0xFFB89567);
        int end=x+1+(int)((width-2)*progress);
        if(end>x+1){g.fill(x+1,y+2,end,y+9,fill);g.fill(x+1,y+2,end,y+3,light);g.fill(x+1,y+7,end,y+9,boss.phase()==2?0xFF99462C:0xFF29685C);}
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // Cataclysm-tier Bell Warden crest on Left (28x28) and sacred bronze bracket on Right (16x24)
        ResourceLocation iconTex = boss.phase() == 2 ? ICON_P2 : ICON_P1;
        g.blit(iconTex, x - 16, y - 7, 0.0F, 0.0F, 28, 28, 28, 28);
        ResourceLocation bracketTex = boss.phase() == 2 ? BRACKET_P2 : BRACKET_P1;
        g.blit(bracketTex, x + width - 2, y - 4, 0.0F, 0.0F, 16, 24, 16, 24);
        // The server's translated status carries pillar count, phase, stagger and transformation.
        var title=event.getBossEvent().getName();
        float scale=Math.min(1F,(width+24F)/Math.max(1,mc.font.width(title)));
        g.pose().pushPose();g.pose().translate(mc.getWindow().getGuiScaledWidth()/2F,y-11,0);g.pose().scale(scale,scale,1);
        g.drawCenteredString(mc.font,title,0,0,0xFFE8D3A3);g.pose().popPose();
        String percent=Math.round(progress*100)+"%";
        g.drawCenteredString(mc.font,percent,x+width/2,y+16,0xFFD5CCB6);
        for(int i=0;i<3;i++) {
            int px=x+width/2-48+i*9;
            g.fill(px,y+17,px+5,y+22,i<boss.pillars()?light:0xFF364340);
        }
        g.drawString(mc.font,boss.phase()==2?"II":"I",x+width/2+35,y+16,fill,false);
    }
}
