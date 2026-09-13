package cn.suiyi.relicward.client;

import cn.suiyi.relicward.block.TeachingBellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public final class TeachingBellRenderer implements BlockEntityRenderer<TeachingBellEntity> {
    public TeachingBellRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(TeachingBellEntity e,float partial,PoseStack p,MultiBufferSource b,int light,int overlay) {
        float t=e.cycle()+partial;
        if(t<20)Telegraphs.ring(p,b,new Vec3(.5,0,.5),2,1,.8F,.3F);
        else if(t<=32) {
            Telegraphs.ring(p,b,new Vec3(.5,0,.5),2+(t-20)*.5-.4,1,.8F,.3F);
            Telegraphs.ring(p,b,new Vec3(.5,0,.5),2+(t-20)*.5+.4,1,.8F,.3F);
        }
    }
    @Override public boolean shouldRenderOffScreen(TeachingBellEntity e) { return true; }
}
