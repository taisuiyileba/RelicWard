package cn.suiyi.relicward.client;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.entity.BellWarden;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public final class BellWardenRenderer extends MobRenderer<BellWarden, BellWardenModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(RelicWard.ID, "textures/entity/bell_warden.png");
    private static final RenderType GLOW = RenderType.eyes(new ResourceLocation(RelicWard.ID, "textures/entity/bell_warden_glow.png"));

    public BellWardenRenderer(EntityRendererProvider.Context context) {
        super(context, new BellWardenModel(context.bakeLayer(BellWardenModel.LAYER)), 1.5F);
        addLayer(new EyesLayer<BellWarden, BellWardenModel>(this) {
            @Override public RenderType renderType() { return GLOW; }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(BellWarden entity) { return TEXTURE; }
    @Override protected float getFlipDegrees(BellWarden entity) { return 0; }
    public static float facingForRender(BellWarden e,float bodyYaw){return e.action()==cn.suiyi.relicward.combat.WardenAction.CHARGE?e.attackYaw():bodyYaw;}
    @Override protected void setupRotations(BellWarden e,PoseStack p,float age,float bodyYaw,float partial){super.setupRotations(e,p,age,facingForRender(e,bodyYaw),partial);}
    @Override public void render(BellWarden e,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light) {
        super.render(e,yaw,partial,poses,buffers,light);Telegraphs.warden(e,partial,poses,buffers);
    }
}
