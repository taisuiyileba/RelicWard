package cn.suiyi.relicward.client;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.RelicContent;
import cn.suiyi.relicward.entity.CourtPuppet;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RelicWard.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientRegistration {
    @SubscribeEvent public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent e){e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)m->BellArmorClient.resetModels());}

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BellWardenModel.LAYER, BellWardenModel::createBodyLayer);
        event.registerLayerDefinition(CourtPuppetModel.LAYER,CourtPuppetModel::layer);
        event.registerLayerDefinition(BellArmorModel.LAYER, () -> BellArmorModel.createArmorLayer(new net.minecraft.client.model.geom.builders.CubeDeformation(0.6F)));
        event.registerLayerDefinition(BellArmorModel.LAYER_LEGS, () -> BellArmorModel.createLegsLayer(new net.minecraft.client.model.geom.builders.CubeDeformation(0.2F)));
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(RelicWard.BELL_WARDEN.get(), BellWardenRenderer::new);
        event.registerEntityRenderer(RelicWard.MAUL_PULSE.get(),MaulPulseRenderer::new);
        event.registerEntityRenderer(RelicWard.RESONANT_WAVE.get(),ResonantWaveRenderer::new);
        event.registerBlockEntityRenderer(RelicContent.TEACHING_ENTITY.get(),TeachingBellRenderer::new);
        event.registerBlockEntityRenderer(RelicContent.ALTAR_ENTITY.get(),CourtAltarRenderer::new);
        event.registerEntityRenderer(RelicWard.COURT_PUPPET.get(),ctx->new MobRenderer<CourtPuppet,CourtPuppetModel>(ctx,new CourtPuppetModel(ctx.bakeLayer(CourtPuppetModel.LAYER)),.5F) {
            @Override public ResourceLocation getTextureLocation(CourtPuppet e) {return new ResourceLocation("relicward","textures/entity/court_puppet.png");}
        });
    }
    @SubscribeEvent public static void properties(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){
        event.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(cn.suiyi.relicward.ChapterContent.SHIELD.get(),new ResourceLocation("minecraft","blocking"),
            (stack,level,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==stack?1F:0F));
        event.enqueueWork(()->net.minecraft.client.renderer.item.ItemProperties.register(RelicContent.MAUL.get(),new ResourceLocation("relicward","charging"),
            (stack,level,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==stack?1F:0F));
    }
}
