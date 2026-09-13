package cn.suiyi.relicward.client;

import cn.suiyi.relicward.item.EchoMaulItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class MaulChargeClient implements IClientItemExtensions {
    public static final MaulChargeClient INSTANCE=new MaulChargeClient();
    private static float smooth(float q){q=Mth.clamp(q,0,1);return q*q*(3-2*q);}
    public static float lift(float ticks){
        if(ticks<=EchoMaulItem.CHARGE_TICKS)return smooth(ticks/EchoMaulItem.CHARGE_TICKS);
        if(ticks<=EchoMaulItem.CONTACT_TICK)return 1-1.25F*smooth((ticks-EchoMaulItem.CHARGE_TICKS)/EchoMaulItem.DOWN_TICKS);
        return -.25F*(1-smooth((ticks-EchoMaulItem.CONTACT_TICK)/10));
    }
    public static final HumanoidModel.ArmPose CHARGE=HumanoidModel.ArmPose.create("RELICWARD_MAUL_CHARGE",true,(model,entity,arm)->{
        float q=lift(entity.getTicksUsingItem());
        var main=arm==HumanoidArm.RIGHT?model.rightArm:model.leftArm;
        var support=arm==HumanoidArm.RIGHT?model.leftArm:model.rightArm;
        float side=arm==HumanoidArm.RIGHT?1:-1;
        main.xRot=-1.1F-q*1.5F;main.yRot=-.15F*side;main.zRot=(.25F+q*.12F)*side;
        support.xRot=-1.05F-q*1.3F;support.yRot=.55F*side;support.zRot=-.3F*side;
    });
    @Override public HumanoidModel.ArmPose getArmPose(LivingEntity entity,InteractionHand hand,ItemStack stack){
        return entity.isUsingItem()&&entity.getUseItem()==stack?CHARGE:null;
    }
    /** Camera-space offsets from the exact vanilla resting transform; independent of the arm pose. */
    public record HandPose(float x,float y,float z,float pitch,float yaw,float roll){
        HandPose blend(HandPose next,float progress){
            float q=smooth(progress);
            return new HandPose(Mth.lerp(q,x,next.x),Mth.lerp(q,y,next.y),Mth.lerp(q,z,next.z),
                Mth.lerp(q,pitch,next.pitch),Mth.lerp(q,yaw,next.yaw),Mth.lerp(q,roll,next.roll));
        }
    }
    public static final HandPose REST=new HandPose(0,0,0,0,0,0);
    private static final HandPose READY=new HandPose(.06F,.25F,.08F,32,-8,-12);
    private static final HandPose IMPACT=new HandPose(-.30F,-.24F,-.35F,-105,8,-8);
    public static HandPose firstPersonPose(float ticks){
        if(ticks<=0||ticks>=EchoMaulItem.END_TICK)return REST;
        if(ticks<=EchoMaulItem.CHARGE_TICKS)return REST.blend(READY,ticks/EchoMaulItem.CHARGE_TICKS);
        if(ticks<=EchoMaulItem.CONTACT_TICK)return READY.blend(IMPACT,(ticks-EchoMaulItem.CHARGE_TICKS)/EchoMaulItem.DOWN_TICKS);
        float recovery=(ticks-EchoMaulItem.CONTACT_TICK)/(EchoMaulItem.END_TICK-EchoMaulItem.CONTACT_TICK);
        HandPose p=IMPACT.blend(REST,recovery);
        // Return around the low right side, not backwards through the striking arc.
        float bow=Mth.sin(Mth.clamp(recovery,0,1)*(float)Math.PI);
        return new HandPose(p.x+.18F*bow,p.y-.10F*bow,p.z,p.pitch,p.yaw,p.roll+12*bow);
    }
    private LocalPlayer lastPlayer;
    private HumanoidArm lastArm;
    private float lastFrame=-100,releaseAt=-1;
    private boolean wasUsing;
    private HandPose lastPose=REST,releasePose=REST;
    @Override public boolean applyForgeHandTransform(PoseStack pose,LocalPlayer player,HumanoidArm arm,ItemStack stack,float partial,float equip,float swing){
        // The maul skill is main-hand only. Never replay its pose on a spare offhand maul.
        if(arm!=player.getMainArm())return false;
        float now=player.tickCount+partial;
        if(lastPlayer!=player||lastArm!=arm||now-lastFrame>2||now<lastFrame){wasUsing=false;releaseAt=-1;lastPose=REST;}
        lastPlayer=player;lastArm=arm;lastFrame=now;
        boolean using=player.isUsingItem()&&player.getUseItem()==stack;
        HandPose p;
        if(using){p=firstPersonPose(player.getTicksUsingItem()+partial);releaseAt=-1;}
        else {
            if(wasUsing){releasePose=lastPose;releaseAt=now;}
            if(releaseAt<0||now-releaseAt>=6){wasUsing=false;lastPose=REST;return false;}
            p=releasePose.blend(REST,(now-releaseAt)/6);
        }
        wasUsing=using;lastPose=p;
        float side=arm==HumanoidArm.RIGHT?1:-1;
        // Match vanilla at both endpoints, including its equip offset, to avoid a release snap.
        pose.translate(side*(.56F+p.x),-.52F-equip*.6F+p.y,-.72F+p.z);
        pose.mulPose(Axis.XP.rotationDegrees(p.pitch));
        pose.mulPose(Axis.YP.rotationDegrees(side*p.yaw));
        pose.mulPose(Axis.ZP.rotationDegrees(side*p.roll));
        return true;
    }
}
