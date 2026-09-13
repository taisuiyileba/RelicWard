package cn.suiyi.relicward.client;

import cn.suiyi.relicward.entity.BellWarden;
import cn.suiyi.relicward.combat.WardenAction;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import java.util.EnumMap;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import org.joml.Matrix3f;

/** Authored whole-body key poses. Contact poses occur on the authoritative damage tick. */
public final class WardenAnimator {
    private static final int ROOT=0,BODY=1,HEAD=2,RARM=3,REL=4,MAUL=5,LARM=6,LEL=7,BELL=8,CLAPPER=9,RLEG=10,RKNEE=11,LLEG=12,LKNEE=13;
    private record Frame(float tick,Pose pose){}
    private static final class Pose {
        final float[] v=new float[84];
        Pose copy(){Pose result=new Pose();System.arraycopy(v,0,result.v,0,v.length);return result;}
        Pose r(int bone,float x,float y,float z){v[bone*6]=x;v[bone*6+1]=y;v[bone*6+2]=z;return this;}
        Pose p(int bone,float x,float y,float z){v[bone*6+3]=x;v[bone*6+4]=y;v[bone*6+5]=z;return this;}
    }
    private static Pose p(){return new Pose();}
    private static Frame f(float t,Pose p){return new Frame(t,p);}
    private static final Pose REST=p().p(ROOT,0,10,0).r(BODY,12,0,0).r(HEAD,18,0,0)
        .r(RLEG,-50,0,-5).r(RKNEE,74,0,0).r(LLEG,-50,0,5).r(LKNEE,74,0,0)
        .r(RARM,-44,0,-8).r(REL,12,0,0).r(LARM,-35,0,12).r(LEL,-25,0,0);
    private static final Pose WIND=p().r(BODY,-6,-32,-5).r(HEAD,4,22,0).r(RARM,-65,58,-18).r(REL,-64,-12,8)
        .r(MAUL,12,0,-12).r(LARM,-28,-20,24).r(LEL,-55,0,0).r(RLEG,-16,12,-6).r(LLEG,15,-8,6).r(RKNEE,18,0,0).p(ROOT,1,1,1);
    private static final Pose CUT=p().r(BODY,13,34,7).r(HEAD,-8,-22,-3).r(RARM,-60,-55,4).r(REL,7,0,-8)
        .r(MAUL,-8,0,16).r(LARM,22,10,18).r(LEL,-40,0,0).r(RLEG,16,0,-7).r(LLEG,-18,0,7).r(LKNEE,18,0,0).p(ROOT,-2,2,-3);
    private static final Pose THROUGH=p().r(BODY,17,48,9).r(HEAD,-9,-30,0).r(RARM,-46,-88,13).r(REL,-25,0,0)
        .r(MAUL,8,0,22).r(LARM,30,0,20).r(LEL,-30,0,0).r(RLEG,18,0,-6).r(LLEG,-21,0,6).r(LKNEE,24,0,0).p(ROOT,-3,2,-3);
    private static final Pose ABOVE=p().r(BODY,-13,-8,-3).r(HEAD,-18,0,0).r(RARM,-155,-10,-12).r(REL,-20,0,0)
        .r(LARM,-137,8,23).r(LEL,-43,0,0).r(RLEG,-12,0,-7).r(LLEG,-9,0,7).r(RKNEE,16,0,0).r(LKNEE,14,0,0).p(ROOT,0,-2,2);
    private static final Pose CRUSH=p().r(BODY,32,10,-4).r(HEAD,-16,0,0).r(RARM,-38,-12,-8).r(REL,2,0,0)
        .r(MAUL,9,0,0).r(LARM,-58,0,42).r(LEL,-8,0,-8).r(RLEG,-25,0,-6).r(RKNEE,42,0,0).r(LLEG,-18,0,6).r(LKNEE,32,0,0).p(ROOT,0,4,-4);
    private static final Pose DRAW_BELL=p().r(BODY,-8,-9,0).r(HEAD,12,0,0).r(LARM,-35,25,-30).r(LEL,-78,0,0)
        .r(RARM,-18,0,-12).r(REL,-12,0,0).r(BELL,0,0,-12).p(ROOT,0,1,0);
    private static final Pose SLAM_CRUSH=CRUSH.copy().r(RLEG,-55,0,0).r(RKNEE,100,0,0).r(LLEG,-48,0,0).r(LKNEE,90,0,0);
    private static final Pose TOLL=p().r(BODY,14,10,2).r(HEAD,-12,0,0).r(LARM,-25,-20,65).r(LEL,-80,0,0)
        .r(RARM,-28,0,-12).r(BELL,0,0,19).r(CLAPPER,0,0,-30).p(ROOT,0,2,-1);
    private static final Pose BRACE=p().r(BODY,26,-9,0).r(HEAD,-22,0,0).r(RARM,-83,5,-20).r(REL,-65,0,0)
        .r(MAUL,20,0,0).r(LARM,-63,-20,28).r(LEL,-85,0,0).r(RLEG,-30,0,-7).r(RKNEE,30,0,0).r(LLEG,14,0,7).p(ROOT,0,3,2);
    private static final Pose STUMBLE=p().p(ROOT,-2,12,2).r(BODY,26,-16,-8).r(HEAD,30,0,8).r(RARM,-57,0,-14).r(REL,28,0,0)
        .r(LARM,-12,-15,27).r(LEL,-75,0,0).r(RLEG,-68,0,-7).r(RKNEE,105,0,0).r(LLEG,-20,0,8).r(LKNEE,46,0,0).r(BELL,0,0,15);
    private static final EnumMap<WardenAction,Frame[]> TRACKS=new EnumMap<>(WardenAction.class);
    static {
        TRACKS.put(WardenAction.DORMANT,new Frame[]{f(0,REST)});
        TRACKS.put(WardenAction.AWAKEN,new Frame[]{f(0,REST),f(15,REST),f(35,p().r(BODY,22,0,0).r(RARM,-48,0,-12).r(REL,25,0,0).p(ROOT,0,5,0).r(RLEG,-30,0,0).r(RKNEE,48,0,0).r(LLEG,-30,0,0).r(LKNEE,48,0,0)),f(51,p().r(BODY,-6,0,0).r(HEAD,-15,0,0)),f(60,p())});
        TRACKS.put(WardenAction.SWEEP,new Frame[]{f(0,p()),f(11,WIND),f(14,WIND),f(18,CUT),f(23,THROUGH),f(29,THROUGH),f(44,p())});
        TRACKS.put(WardenAction.DOUBLE_SWEEP,new Frame[]{f(0,p()),f(10,WIND),f(12,WIND),f(16,CUT),f(21,THROUGH),f(25,THROUGH),f(29,WIND),f(36,WIND),f(49,p().r(RARM,-25,0,-10).r(REL,-25,0,0)),f(65,p())});
        TRACKS.put(WardenAction.SLAM,new Frame[]{f(0,p()),f(15,ABOVE),f(19,ABOVE),f(24,SLAM_CRUSH),f(29,SLAM_CRUSH),f(38,p().r(BODY,15,0,0).r(RARM,-28,0,-8).r(REL,-30,0,0).p(ROOT,0,2,0)),f(60,p())});
        TRACKS.put(WardenAction.WAVE,new Frame[]{f(0,p()),f(15,DRAW_BELL),f(18,DRAW_BELL),f(22,TOLL),f(29,DRAW_BELL),f(43,p()),f(88,p())});
        TRACKS.put(WardenAction.RESONANCE,new Frame[]{f(0,p()),f(23,DRAW_BELL),f(28,DRAW_BELL),f(32,TOLL),f(40,DRAW_BELL),f(46,DRAW_BELL),f(50,TOLL),f(60,DRAW_BELL),f(78,p()),f(126,p())});
        TRACKS.put(WardenAction.CHARGE,new Frame[]{f(0,p()),f(16,BRACE),f(28,BRACE),f(32,BRACE),f(62,BRACE),f(69,CRUSH),f(80,CRUSH),f(104,p())});
        TRACKS.put(WardenAction.STAGGER,new Frame[]{f(0,BRACE),f(8,STUMBLE),f(65,STUMBLE),f(82,REST),f(100,p())});
        TRACKS.put(WardenAction.TRANSFORM,new Frame[]{f(0,p()),f(14,p().r(BODY,18,0,0).r(HEAD,25,0,0).r(LARM,-30,0,25).r(RARM,-30,0,-25)),f(30,p().r(BODY,-10,0,0).r(HEAD,-22,0,0).r(LARM,-35,0,-35).r(RARM,-35,0,35)),f(42,p().r(HEAD,-10,0,0)),f(50,p())});
        TRACKS.put(WardenAction.DYING,new Frame[]{f(0,p()),f(14,STUMBLE),f(30,REST),f(53,p().p(ROOT,0,14,0).r(BODY,38,0,0).r(HEAD,25,0,0).r(RLEG,-60,0,0).r(RKNEE,95,0,0).r(LLEG,-60,0,0).r(LKNEE,95,0,0).r(RARM,-70,0,-15).r(LARM,-15,0,25))});
        TRACKS.put(WardenAction.HIGH_STRIKE,new Frame[]{f(0,p()),f(16,DRAW_BELL),f(24,TOLL),f(32,DRAW_BELL),f(60,p())});
        TRACKS.put(WardenAction.RECOVER,new Frame[]{f(0,BRACE),f(7,CRUSH),f(16,CRUSH),f(40,p())});
    }
    private final ModelPart root,body;
    private final ModelPart[] bones;
    public WardenAnimator(ModelPart root) {
        this.root=root;body=root.getChild("body");var r=body.getChild("right_arm");var l=body.getChild("left_arm");
        var rf=r.getChild("right_forearm");var lf=l.getChild("left_forearm");var rl=root.getChild("right_leg");var ll=root.getChild("left_leg");var bell=body.getChild("bell");
        bones=new ModelPart[]{root,body,body.getChild("head"),r,rf,rf.getChild("hammer"),l,lf,bell,bell.getChild("clapper"),rl,rl.getChild("right_shin"),ll,ll.getChild("left_shin")};
    }
    public static float component(WardenAction action,float tick,int channel) {
        Frame[] frames=TRACKS.get(action);if(frames==null)return 0;
        if(tick<=frames[0].tick)return frames[0].pose.v[channel];
        for(int i=1;i<frames.length;i++)if(tick<=frames[i].tick) {
            float f=(tick-frames[i-1].tick)/(frames[i].tick-frames[i-1].tick);f=f*f*(3-2*f);
            return Mth.lerp(f,frames[i-1].pose.v[channel],frames[i].pose.v[channel]);
        }
        return frames[frames.length-1].pose.v[channel];
    }
    public void applyPose(WardenAction action,float t,float age,float stride,float amount) {
        if(TRACKS.containsKey(action)) {
            root.getAllParts().forEach(ModelPart::resetPose);
            for(int i=0;i<bones.length;i++) {
                var b=bones[i];b.xRot+=component(action,t,i*6)*Mth.DEG_TO_RAD;b.yRot+=component(action,t,i*6+1)*Mth.DEG_TO_RAD;b.zRot+=component(action,t,i*6+2)*Mth.DEG_TO_RAD;
                b.x+=component(action,t,i*6+3);b.y+=component(action,t,i*6+4);b.z+=component(action,t,i*6+5);
            }
            if(action==WardenAction.CHARGE&&t>=30&&t<64){float run=Mth.sin((t-30)*.8F)*Mth.clamp((64-t)/6,0,1);bones[RLEG].xRot+=run*.4F;bones[LLEG].xRot-=run*.4F;}
            bones[CLAPPER].zRot+=Mth.sin(age*.15F)*.07F;
        }
        float gait=action==WardenAction.CHARGE&&t>=30&&t<64?Mth.sin((t-30)*.8F)*3*Mth.clamp((64-t)/6,0,1):
            action==WardenAction.IDLE?Mth.sin(stride*.55F)*Math.min(amount,1)*3:0;
        FootPlant.apply(root,Math.max(0,gait),Math.max(0,-gait));
        if(action==WardenAction.CHARGE){body.yRot=0;bones[HEAD].yRot=0;}
        if(action==WardenAction.SLAM){
            float w=Mth.clamp((t-19)/5,0,1)*(1-Mth.clamp((t-30)/8,0,1));w=w*w*(3-2*w);
            Quaternionf parent=rotation(body).mul(rotation(bones[RARM])).mul(rotation(bones[REL]));
            Matrix3f target=new Matrix3f().rotation(rotation(bones[MAUL]).slerp(parent.invert().rotateX(-(float)Math.PI/2),w).normalize());
            Vector3f angles=new Vector3f((float)Math.atan2(target.m12(),target.m22()),
                (float)Math.atan2(-target.m02(),Math.sqrt(target.m00()*target.m00()+target.m01()*target.m01())),
                (float)Math.atan2(target.m01(),target.m00()));
            bones[MAUL].xRot=angles.x;bones[MAUL].yRot=angles.y;bones[MAUL].zRot=angles.z;
            Matrix4f arm=transform(new Matrix4f(),root);transform(arm,body);transform(arm,bones[RARM]);transform(arm,bones[REL]);
            Vector3f bottom=transform(new Matrix4f(arm),bones[MAUL]).transformPosition(new Vector3f(0,21,15));
            Vector3f gripSlide=new Matrix4f(arm).invert().transformDirection(new Vector3f(0,24-bottom.y,0));
            bones[MAUL].x+=gripSlide.x*w;bones[MAUL].y+=gripSlide.y*w;bones[MAUL].z+=gripSlide.z*w;
        }
    }
    private static Quaternionf rotation(ModelPart p){return new Quaternionf().rotationZYX(p.zRot,p.yRot,p.xRot);}
    private static Matrix4f transform(Matrix4f m,ModelPart p){return m.translate(p.x,p.y,p.z).rotateZYX(p.zRot,p.yRot,p.xRot);}
    public void apply(BellWarden entity,float partial,float stride,float amount){
        float t=entity.actionTick()+partial;var action=entity.action();
        applyPose(action,t,entity.tickCount+partial,stride,amount);
        // Layer the synced gaze over authored poses, without steering committed attacks.
        float weight=switch(action){case IDLE -> 1F;case DORMANT -> .55F;case WAVE,RESONANCE -> .85F;
            case SWEEP,DOUBLE_SWEEP,SLAM,HIGH_STRIKE -> t<action.windup-8?.65F:0F;default -> 0F;};
        if(weight>0){
            float yaw=Mth.wrapDegrees(Mth.rotLerp(partial,entity.yHeadRotO,entity.yHeadRot)-Mth.rotLerp(partial,entity.yBodyRotO,entity.yBodyRot));
            float pitch=Mth.lerp(partial,entity.xRotO,entity.getXRot());
            bones[HEAD].yRot=Mth.lerp(weight,bones[HEAD].yRot,Mth.clamp(yaw*Mth.DEG_TO_RAD-body.yRot,-.96F,.96F));
            bones[HEAD].xRot=Mth.lerp(weight,bones[HEAD].xRot,Mth.clamp(pitch*Mth.DEG_TO_RAD-body.xRot,-.45F,.65F));
        }
        float open=entity.phase()==2?1:action==WardenAction.TRANSFORM?Mth.clamp((t-15)/20,0,1):0;
        body.getChild("chest_left").yRot=-.45F*open;body.getChild("chest_right").yRot=.45F*open;
    }
}
