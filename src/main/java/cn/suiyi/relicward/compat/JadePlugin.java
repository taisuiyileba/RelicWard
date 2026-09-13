package cn.suiyi.relicward.compat;

import cn.suiyi.relicward.entity.*;
import cn.suiyi.relicward.combat.WardenAction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class JadePlugin implements IWailaPlugin {
    @Override public void registerClient(IWailaClientRegistration r){
        r.registerEntityComponent(new ArmorInfo(),BellWarden.class);
        r.registerEntityComponent(new CompanionInfo(),CourtPuppet.class);
    }
    public static Component armorText(BellWarden boss){
        return Component.translatable("jade.relicward.armor",boss.pillars(),boss.action()==WardenAction.STAGGER?0:boss.pillars()*10);
    }
    public static final class ArmorInfo implements IEntityComponentProvider {
        @Override public ResourceLocation getUid(){return new ResourceLocation("relicward","clock_armor");}
        @Override public void appendTooltip(ITooltip t,EntityAccessor a,IPluginConfig c){t.add(armorText((BellWarden)a.getEntity()));}
    }
    public static final class CompanionInfo implements IEntityComponentProvider {
        @Override public ResourceLocation getUid(){return new ResourceLocation("relicward","companion");}
        @Override public void appendTooltip(ITooltip t,EntityAccessor a,IPluginConfig c){
            var p=(CourtPuppet)a.getEntity();t.add(Component.translatable(p.isTame()?(p.isOrderedToSit()?"message.relicward.stay":"message.relicward.follow"):"jade.relicward.wild"));
        }
    }
}
