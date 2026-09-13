package cn.suiyi.relicward.client;

import cn.suiyi.relicward.entity.ResonantWave;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public final class ResonantWaveRenderer extends EntityRenderer<ResonantWave> {
    public ResonantWaveRenderer(EntityRendererProvider.Context context){super(context);}
    @Override public ResourceLocation getTextureLocation(ResonantWave e){return new ResourceLocation("relicward","textures/effect/resonance.png");}
    @Override public void render(ResonantWave e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){Telegraphs.wave(p,b,Vec3.ZERO,e.age()+partial);}
}
