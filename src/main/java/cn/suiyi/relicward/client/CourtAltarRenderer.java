package cn.suiyi.relicward.client;

import cn.suiyi.relicward.RelicContent;
import cn.suiyi.relicward.block.CourtAltarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class CourtAltarRenderer implements BlockEntityRenderer<CourtAltarEntity> {
    public CourtAltarRenderer(BlockEntityRendererProvider.Context c){}
    @Override public void render(CourtAltarEntity altar,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){
        if(altar.getLevel()==null)return;var mc=Minecraft.getInstance();double now=altar.getLevel().getGameTime()+partial;
        if(altar.active()){
            Vec3 center=Vec3.atBottomCenterOf(altar.center()).subtract(Vec3.atLowerCornerOf(altar.getBlockPos()));
            Telegraphs.band(p,b,center,17.75,17.88,0,360,.35F,.85F,.66F,.25F);
            for(int i=0;i<12;i++)Telegraphs.band(p,b,center.add(0,.01,0),17.3,17.65,i*30,5,.55F,1,.75F,.42F);
        }
        if(!altar.completed())return;
        Telegraphs.rune(p,b,new Vec3(.5,1.02,.5),.75F,(float)now*.02F,.55F);
        p.pushPose();p.translate(.5,1.4+Math.sin(now*.05)*.06,.5);p.mulPose(new Quaternionf().rotateY((float)now*.04F));p.scale(.3F,.3F,.3F);
        mc.getItemRenderer().renderStatic(new ItemStack(RelicContent.CORE.get()),ItemDisplayContext.GROUND,15728880,OverlayTexture.NO_OVERLAY,p,b,altar.getLevel(),0);p.popPose();
        if(altar.respawnAt()>0){
            float progress=(float)Math.max(0,Math.min(1,1-(altar.respawnAt()-now)/60));Vec3 center=Vec3.atBottomCenterOf(altar.center()).subtract(Vec3.atLowerCornerOf(altar.getBlockPos()));
            Telegraphs.rune(p,b,center,4-progress*2,(float)now*.03F,.8F);
            for(int i=0;i<4;i++)Telegraphs.band(p,b,center.add(0,progress*4+i*.3,0),1.2,1.3,now*3+i*50,240,1,.78F,.3F,(1-progress)*.55F);
        }
        if(mc.player!=null&&mc.player.distanceToSqr(Vec3.atCenterOf(altar.getBlockPos()))<16*16){
            var text=Component.translatable(altar.respawnAt()>0?"message.relicward.resummoning":"message.relicward.cooldown",Math.max(0,(altar.readyAt()-altar.getLevel().getGameTime()+19)/20));
            p.pushPose();p.translate(.5,2,.5);p.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());p.scale(.02F,-.02F,.02F);
            mc.font.drawInBatch(text,-mc.font.width(text)/2F,0,0xFFE6B2,false,p.last().pose(),b,Font.DisplayMode.NORMAL,0x66000000,15728880);p.popPose();
        }
    }
    @Override public boolean shouldRenderOffScreen(CourtAltarEntity a){return true;}
}
