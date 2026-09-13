package cn.suiyi.relicward.client;

import cn.suiyi.relicward.combat.CombatMath;
import cn.suiyi.relicward.combat.SweepProfile;
import cn.suiyi.relicward.combat.WardenAction;
import cn.suiyi.relicward.entity.BellWarden;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/** Soft warning sectors, directional strike ribbons and layered resonant effects. */
public final class Telegraphs {
    private static final ResourceLocation RUNE=new ResourceLocation("relicward","textures/effect/resonance.png");
    private static final int FULL=15728880;
    private static void point(VertexConsumer v,PoseStack p,Vec3 a,float r,float g,float b,float alpha){v.vertex(p.last().pose(),(float)a.x,(float)a.y,(float)a.z).color(r,g,b,alpha).endVertex();}
    private static void quad(PoseStack p,MultiBufferSource buffers,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float r,float g,float blue,float alpha){
        var v=buffers.getBuffer(EffectRenderTypes.GLOW);point(v,p,a,r,g,blue,alpha);point(v,p,b,r,g,blue,alpha);point(v,p,c,r,g,blue,alpha);point(v,p,d,r,g,blue,alpha);
    }
    private static Vec3 radial(Vec3 c,double radius,double degrees,double height){double a=Math.toRadians(degrees);return c.add(-Math.sin(a)*radius,height,Math.cos(a)*radius);}
    public static void band(PoseStack p,MultiBufferSource b,Vec3 c,double inner,double outer,double start,double span,float r,float g,float blue,float alpha){
        if(outer<=inner||alpha<=0)return;int segments=Math.max(8,(int)(Math.abs(span)/4));
        for(int i=0;i<segments;i++){double a=start+span*i/segments,z=start+span*(i+1)/segments;
            quad(p,b,radial(c,inner,a,.035),radial(c,outer,a,.035),radial(c,outer,z,.035),radial(c,inner,z,.035),r,g,blue,alpha);
        }
    }
    public static void ring(PoseStack p,MultiBufferSource b,Vec3 c,double radius,float r,float g,float blue){
        band(p,b,c,Math.max(0,radius-.12),radius+.12,0,360,r,g,blue,.24F);
        band(p,b,c.add(0,.008,0),Math.max(0,radius-.025),radius+.025,0,360,r,g,blue,.85F);
    }
    public static void rune(PoseStack p,MultiBufferSource b,Vec3 center,float radius,float rotation,float alpha){
        if(alpha<=0)return;p.pushPose();p.translate(center.x,center.y+.05,center.z);p.mulPose(new Quaternionf(new AxisAngle4f(rotation,0,1,0)));
        var v=b.getBuffer(RenderType.entityTranslucent(RUNE));float[][] corners={{-radius,-radius,0,0},{-radius,radius,0,1},{radius,radius,1,1},{radius,-radius,1,0}};
        for(var c:corners)v.vertex(p.last().pose(),c[0],0,c[1]).color(1,.85F,.5F,alpha).uv(c[2],c[3]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(FULL).normal(p.last().normal(),0,1,0).endVertex();
        p.popPose();
    }
    private static void sweep(BellWarden e,float t,PoseStack p,MultiBufferSource b,int swing){
        int hit=SweepProfile.contact(e.action(),swing),start=SweepProfile.warningStart(e.action(),swing);float yaw=e.attackYaw();
        Vec3 ground=Vec3.ZERO;double radius=SweepProfile.RADIUS,half=SweepProfile.ANGLE/2;
        if(t>=start&&t<hit){
            float progress=(t-start)/(hit-start);
            band(p,b,ground,.65,radius,yaw-half,half*2,1,.52F,.10F,.035F+.095F*progress);
            band(p,b,ground.add(0,.015,0),radius-.12,radius,yaw-half,half*2,1,.72F,.22F,.4F+.4F*progress);
            for(int k=0;k<7;k++)band(p,b,ground.add(0,.025,0),radius-.42,radius-.22,yaw-half+k*(half*2/7),4,1,.82F,.38F,.7F);
        }
        float age=t-hit;
        if(age>=-3&&age<7){
            float progress=Mth.clamp((age+3)/7,0,1),fade=1-Mth.clamp(age/7,0,1);
            double lead=yaw+(swing==0?half-half*2*progress:-half+half*2*progress);
            double direction=swing==0?1:-1;
            Vec3 air=new Vec3(0,1.35-.15*progress,0);
            for(int layer=0;layer<4;layer++){
                double inner=radius-.6-layer*.18,outer=radius-.05+layer*.1;
                band(p,b,air,inner,outer,lead,55*direction,1,.65F+.08F*layer,.18F+.15F*layer,fade*(.32F-layer*.065F));
            }
            band(p,b,air.add(0,.01,0),radius-.20,radius-.10,lead,40*direction,1,.96F,.73F,.92F*fade);
        }
    }
    public static void warden(BellWarden e,float partial,PoseStack p,MultiBufferSource b){
        var action=e.action();float t=e.actionTick()+partial;Vec3 impact=e.aim().subtract(e.position());
        if(action==WardenAction.SWEEP||action==WardenAction.DOUBLE_SWEEP){sweep(e,t,p,b,0);if(action==WardenAction.DOUBLE_SWEEP)sweep(e,t,p,b,1);}
        else if(action==WardenAction.SLAM||action==WardenAction.HIGH_STRIKE){
            float radius=action==WardenAction.SLAM?3.5F:2.5F;
            if(t<24){rune(p,b,impact,radius,(float)Math.toRadians(t*2),.16F+.4F*t/24);ring(p,b,impact,radius,1,.59F,.18F);}
            else if(t<45){float age=t-24,fade=1-age/21;
                rune(p,b,impact,radius,0,.65F*fade);band(p,b,impact,Math.max(0,age*.25-.4),.7+age*.25,0,360,1,.72F,.3F,fade*.4F);
                if(action==WardenAction.SLAM)debris(e,impact,age,p,b);
            }
        }else if(action==WardenAction.WAVE||action==WardenAction.RESONANCE){
            if(t<action.windup)rune(p,b,impact,3.2F,t*.035F,.25F+.45F*t/action.windup);
            wave(p,b,impact,t-action.windup);if(action==WardenAction.RESONANCE)wave(p,b,impact,t-50);
        }else if(action==WardenAction.CHARGE){
            if(t<30){Vec3 f=CombatMath.forward(e.attackYaw()),side=new Vec3(f.z,0,-f.x);
                quad(p,b,side.scale(-1.5).add(0,.06,0),side.scale(1.5).add(0,.06,0),f.scale(22).add(side.scale(1.5)).add(0,.06,0),f.scale(22).subtract(side.scale(1.5)).add(0,.06,0),1,.52F,.14F,.06F+t*.003F);
                for(int k=1;k<8;k++){Vec3 c=f.scale(k*2.7);quad(p,b,c.subtract(side).add(0,.08,0),c.add(f.scale(1.1)).add(0,.08,0),c.add(side).add(0,.08,0),c.add(f.scale(.7)).add(0,.08,0),1,.8F,.35F,.7F);}
            }else if(t<64)band(p,b,Vec3.ZERO,1.3,2,0,360,.75F,.63F,.38F,.24F);
        }else if(action==WardenAction.STAGGER){rune(p,b,Vec3.ZERO,2.7F,-t*.015F,.35F);band(p,b,new Vec3(0,3.4,0),.7,.85,t*6,260,.4F,1,.78F,.5F);}
        else if(action==WardenAction.TRANSFORM||action==WardenAction.AWAKEN){
            float q=t/action.duration;rune(p,b,Vec3.ZERO,2+q*2,t*.025F,(float)Math.sin(Math.PI*q)*.65F);
            for(int k=0;k<3;k++)band(p,b,new Vec3(0,q*3+k*.3,0),1.1+k*.25,1.17+k*.25,t*3+k*70,240,1,.78F,.27F,(1-q)*.4F);
        }else if(action==WardenAction.DYING){rune(p,b,Vec3.ZERO,3.5F,t*.03F,(float)Math.sin(Math.PI*Math.min(t,80)/80)*.6F);}
    }
    public static void wave(PoseStack p,MultiBufferSource b,Vec3 c,float t){
        if(t<0||t>36)return;double radius=2+t*.5;float alpha=Math.min(1,(37-t)/5);
        band(p,b,c,radius-.65,radius+.65,0,360,1,.64F,.15F,.24F*alpha);
        band(p,b,c.add(0,.018,0),radius-.16,radius+.16,0,360,1,.9F,.55F,.85F*alpha);
        for(int k=0;k<3;k++)band(p,b,c.add(0,.13+k*.10,0),radius-.35+k*.1,radius+.15-k*.03,0,360,1,.76F,.32F,(.28F-k*.07F)*alpha);
        for(int i=0;i<24;i++)band(p,b,c.add(0,.03,0),radius-.5,radius+.5,i*15+t,2,1,.95F,.67F,.8F*alpha);
    }
    private static void debris(BellWarden e,Vec3 impact,float age,PoseStack p,MultiBufferSource b){
        var state=e.level().getBlockState(BlockPos.containing(e.aim()).below());if(state.isAir())return;
        for(int i=0;i<10;i++){
            double angle=i*2.399,r=.4+(i%3)*.4+age*.11,y=Math.max(0,(.25+(i%4)*.055)*age-.018*age*age);
            float scale=(.16F+(i%3)*.065F)*(1-age/24);if(scale<=0)continue;
            p.pushPose();p.translate(impact.x+Math.cos(angle)*r,impact.y+y+.1,impact.z+Math.sin(angle)*r);p.mulPose(new Quaternionf().rotateY((float)angle+age*.1F).rotateX(age*.12F));p.scale(scale,scale,scale);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,p,b,FULL,OverlayTexture.NO_OVERLAY);p.popPose();
        }
    }
}
