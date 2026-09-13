package cn.suiyi.relicward.client;

import cn.suiyi.relicward.entity.CourtPuppet;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class CourtPuppetModel extends HierarchicalModel<CourtPuppet> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(new ResourceLocation("relicward","court_puppet"),"main");
    private final ModelPart root,body,head,right,left,rel,lel,rleg,lleg;
    public CourtPuppetModel(ModelPart baked){
        root=baked.getChild("root");body=root.getChild("body");head=body.getChild("head");right=body.getChild("right");left=body.getChild("left");
        rel=right.getChild("elbow");lel=left.getChild("elbow");rleg=root.getChild("rleg");lleg=root.getChild("lleg");
    }
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var p=mesh.getRoot().addOrReplaceChild("root",CubeListBuilder.create(),PartPose.offset(0,24,0));
        var torso=p.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-5,-11,-3,10,10,6)
            .texOffs(33,0).addBox(-4,-9,-4,8,6,1).texOffs(52,0).addBox(-5,-2,-4,10,2,8),PartPose.offset(0,-7,0));
        torso.addOrReplaceChild("head",CubeListBuilder.create().texOffs(89,0).addBox(-4,-7,-4,8,7,8)
            .texOffs(0,17).addBox(-5,-8,-5,10,2,10).texOffs(41,17).addBox(-3,-4,-4.5F,2,1,1)
            .texOffs(48,17).addBox(1,-4,-4.5F,2,1,1).texOffs(55,17).addBox(-1,-4,-5,2,4,2),PartPose.offset(0,-11,0));
        var r=torso.addOrReplaceChild("right",CubeListBuilder.create().texOffs(64,17).addBox(-3,-1,-3,5,6,6)
            .texOffs(87,17).addBox(-4,-2,-4,7,3,8),PartPose.offset(-6,-10,0));
        var l=torso.addOrReplaceChild("left",CubeListBuilder.create().texOffs(0,30).addBox(-2,-1,-3,5,6,6)
            .texOffs(23,30).addBox(-3,-2,-4,7,3,8),PartPose.offset(6,-10,0));
        r.addOrReplaceChild("elbow",CubeListBuilder.create().texOffs(54,30).addBox(-3,0,-3,5,6,6)
            .texOffs(77,30).addBox(-1,3,-8,2,2,14).texOffs(0,47).addBox(-3,1,-11,6,6,5),PartPose.offset(0,5,0));
        l.addOrReplaceChild("elbow",CubeListBuilder.create().texOffs(23,47).addBox(-2,0,-3,5,6,6)
            .texOffs(46,47).addBox(-3,-2,-5,7,10,2).texOffs(65,47).addBox(-2,0,-6,5,6,1),PartPose.offset(0,5,0));
        for(String name:new String[]{"rleg","lleg"}){
            var leg=p.addOrReplaceChild(name,CubeListBuilder.create().texOffs(78,47).addBox(-2,0,-2,4,4,4),PartPose.offset(name.equals("rleg")?-3:3,-7,0));
            leg.addOrReplaceChild("knee",CubeListBuilder.create().texOffs(95,47).addBox(-2,0,-3,4,4,6),PartPose.offset(0,3,0));
        }
        torso.addOrReplaceChild("bond",CubeListBuilder.create().texOffs(0,60).addBox(-4,-10,-4.3F,8,1,1),PartPose.ZERO);
        return LayerDefinition.create(mesh,128,128);
    }
    @Override public ModelPart root(){return root;}
    private static float ease(float t){t=Mth.clamp(t,0,1);return t*t*(3-2*t);}
    @Override public void setupAnim(CourtPuppet e,float limb,float amount,float age,float yaw,float pitch){
        root.getAllParts().forEach(ModelPart::resetPose);body.getChild("bond").visible=e.isTame();head.yRot=Mth.clamp(yaw,-35,35)*Mth.DEG_TO_RAD;head.xRot=pitch*Mth.DEG_TO_RAD;
        float step=Mth.cos(limb*.8F)*amount;rleg.xRot=step*.8F;lleg.xRot=-step*.8F;
        rleg.getChild("knee").xRot=Math.max(0,-step)*.35F;lleg.getChild("knee").xRot=Math.max(0,step)*.35F;
        right.xRot=-step*.35F;left.xRot=step*.25F;rel.xRot=-.35F;lel.xRot=-.6F;
        if(e.isInSittingPose()){root.y+=2;rleg.xRot=-.55F;lleg.xRot=-.55F;body.xRot=.1F;}
        if(e.attackKind()==0)return;
        float t=e.attackTick()+age-e.tickCount,hit=e.contactTick();
        float wind=ease(t/(hit-4)),strike=ease((t-hit+4)/4),follow=ease((t-hit)/4),recovery=1-ease((t-hit-7)/(e.attackKind()==2?13:11));
        rleg.xRot=-.25F*wind*recovery;lleg.xRot=.2F*wind*recovery;root.z-=1.5F*strike*recovery;root.y+=.6F*strike*recovery;
        if(e.attackKind()==1){
            body.yRot=(-.45F*wind+.95F*strike+.18F*follow)*recovery;body.xRot=.2F*strike*recovery;
            right.xRot=(-1.9F*wind+1.5F*strike)*recovery;right.zRot=(-.2F*wind+.5F*strike)*recovery;
            rel.xRot=(-1.0F*wind+1.2F*strike)*recovery;left.xRot=-.6F*wind*recovery;lel.xRot=-.7F;
        }else{
            body.yRot=(.3F*wind-.6F*strike)*recovery;body.xRot=.3F*strike*recovery;
            left.xRot=(-.7F*wind-.65F*strike)*recovery;left.zRot=.15F*wind*recovery;lel.xRot=(.6F*wind+.65F*strike)*recovery;
            right.xRot=-.7F*wind*recovery;rel.xRot=-1.1F*wind*recovery;
        }
        head.yRot=-body.yRot*.5F;
    }
}
