package cn.suiyi.relicward.client;

import cn.suiyi.relicward.entity.MaulPulse;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class MaulPulseRenderer extends EntityRenderer<MaulPulse> {
    public MaulPulseRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(MaulPulse e){return new ResourceLocation("relicward","textures/effect/resonance.png");}
    @Override public void render(MaulPulse e,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
        float age=e.tickCount+partial,fade=Math.max(0,1-age/22);
        Telegraphs.rune(poses,buffers,Vec3.ZERO,2.2F,0,fade*.55F);
        Telegraphs.band(poses,buffers,Vec3.ZERO,Math.max(0,age*.09-.2),.3+age*.09,0,360,1,.75F,.3F,fade*.65F);
    }
}
