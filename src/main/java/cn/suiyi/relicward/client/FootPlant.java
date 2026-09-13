package cn.suiyi.relicward.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Two-segment sagittal IK; model pixels, floor Y=24, 18px thigh + 17px shin + 3px sole. */
public final class FootPlant {
    private static final float THIGH=18,SHIN=17;
    private FootPlant(){}
    public static void apply(ModelPart root,float rightLift,float leftLift){
        ModelPart right=root.getChild("right_leg"),left=root.getChild("left_leg");
        ModelPart rk=right.getChild("right_shin"),lk=left.getChild("left_shin");
        float rz=THIGH*Mth.sin(right.xRot)+SHIN*Mth.sin(right.xRot+rk.xRot);
        float lz=THIGH*Mth.sin(left.xRot)+SHIN*Mth.sin(left.xRot+lk.xRot);
        float ry=THIGH*Mth.cos(right.xRot)+SHIN*Mth.cos(right.xRot+rk.xRot);
        float ly=THIGH*Mth.cos(left.xRot)+SHIN*Mth.cos(left.xRot+lk.xRot);
        float depth=Mth.clamp(Math.min(ry,ly),17,35);
        // Discard authored root sinking: pelvis height must agree with actual leg reach.
        root.y=21-right.y-depth;
        solve(right,rk,rk.getChild("right_foot"),depth-rightLift,rz);
        solve(left,lk,lk.getChild("left_foot"),depth-leftLift,lz);
    }
    private static void solve(ModelPart hip,ModelPart knee,ModelPart foot,float y,float z){
        y=Mth.clamp(y,8,35);
        float reach=(float)Math.sqrt(Math.max(0,35*35-y*y));z=Mth.clamp(z,-reach,reach);
        float cos=Mth.clamp((y*y+z*z-THIGH*THIGH-SHIN*SHIN)/(2*THIGH*SHIN),-1,1);
        float bend=(float)Math.acos(cos);
        hip.xRot=(float)(Math.atan2(z,y)-Math.atan2(SHIN*Math.sin(bend),THIGH+SHIN*Math.cos(bend)));
        hip.yRot=0;hip.zRot=0;knee.xRot=bend;knee.yRot=0;knee.zRot=0;
        foot.xRot=-hip.xRot-knee.xRot;foot.yRot=0;foot.zRot=0;
    }
}
