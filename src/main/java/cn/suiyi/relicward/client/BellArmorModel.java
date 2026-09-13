package cn.suiyi.relicward.client;

import cn.suiyi.relicward.RelicWard;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Custom 3D armor model for the Bell Warden / Bellforged Regalia (玄铎·钟铸古仪重铠).
 * Designed authentically from the Bell Warden boss:
 * - Temple-pavilion crown posts, bell finial, forehead lintel and crest badge
 * - Clear vanilla hat completely to prevent any mesh overlap/obstruction
 * - Sculpted bronze breastplate with central 3D sacred bell relief and glowing resonance core
 * - Ceremonial 3-post chime rack on the backplate
 * - Massive stepped temple-eave pauldrons with hanging bell pendants and cloud/thunder reliefs
 * - Armored ceremonial faulds / tassets (甲裙), rectangular knee bosses, and square-toed temple sabatons
 */
@OnlyIn(Dist.CLIENT)
public class BellArmorModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(RelicWard.ID, "bell_armor"), "main");
    public static final ModelLayerLocation LAYER_LEGS = new ModelLayerLocation(new ResourceLocation(RelicWard.ID, "bell_armor_legs"), "main");

    public BellArmorModel(ModelPart root) {
        super(root);
        this.hat.visible = false;
    }

    @Override
    public void setAllVisible(boolean visible) {
        super.setAllVisible(visible);
        this.hat.visible = false;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.hat.visible = false;
        super.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    /**
     * Layer 1: Helmet, Chestplate, Pauldrons, and Boots.
     */
    public static LayerDefinition createArmorLayer(CubeDeformation deformation) {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Completely erase vanilla hat to prevent any face/visor obstruction!
        partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition head = partdefinition.getChild("head");
        PartDefinition body = partdefinition.getChild("body");
        PartDefinition rightArm = partdefinition.getChild("right_arm");
        PartDefinition leftArm = partdefinition.getChild("left_arm");
        PartDefinition rightLeg = partdefinition.getChild("right_leg");
        PartDefinition leftLeg = partdefinition.getChild("left_leg");

        // 1. Helmet (Attached to head)
        // Horizontal brow lintel beam across forehead
        head.addOrReplaceChild("brow_beam",
            CubeListBuilder.create().texOffs(0, 34).addBox(-5.0F, -7.0F, -5.2F, 10.0F, 2.0F, 1.0F, deformation),
            PartPose.ZERO);

        // Forehead ceremonial crest badge with glowing cyan eye/gem
        head.addOrReplaceChild("brow_crest",
            CubeListBuilder.create().texOffs(23, 34).addBox(-2.0F, -9.0F, -5.4F, 4.0F, 3.0F, 1.0F, deformation),
            PartPose.ZERO);

        // Central bell finial on crown
        head.addOrReplaceChild("crown_center",
            CubeListBuilder.create().texOffs(34, 34).addBox(-1.5F, -11.0F, -1.5F, 3.0F, 3.0F, 3.0F, deformation),
            PartPose.ZERO);

        // Right & Left temple vertical posts and flared caps
        head.addOrReplaceChild("right_crown_prong",
            CubeListBuilder.create().texOffs(47, 34).mirror().addBox(-4.5F, -13.0F, -1.0F, 2.0F, 5.0F, 2.0F, deformation).mirror(false),
            PartPose.ZERO);
        head.addOrReplaceChild("right_crown_cap",
            CubeListBuilder.create().texOffs(56, 34).mirror().addBox(-5.0F, -14.0F, -1.5F, 3.0F, 1.0F, 3.0F, deformation).mirror(false),
            PartPose.ZERO);

        head.addOrReplaceChild("left_crown_prong",
            CubeListBuilder.create().texOffs(69, 34).addBox(2.5F, -13.0F, -1.0F, 2.0F, 5.0F, 2.0F, deformation),
            PartPose.ZERO);
        head.addOrReplaceChild("left_crown_cap",
            CubeListBuilder.create().texOffs(78, 34).addBox(2.0F, -14.0F, -1.5F, 3.0F, 1.0F, 3.0F, deformation),
            PartPose.ZERO);

        // Flared cheek ear plates (shared UV)
        head.addOrReplaceChild("right_ear_plate",
            CubeListBuilder.create().texOffs(91, 34).mirror().addBox(-4.8F, -6.0F, -2.0F, 1.0F, 6.0F, 4.0F, deformation).mirror(false),
            PartPose.ZERO);
        head.addOrReplaceChild("left_ear_plate",
            CubeListBuilder.create().texOffs(102, 34).addBox(3.8F, -6.0F, -2.0F, 1.0F, 6.0F, 4.0F, deformation),
            PartPose.ZERO);

        // Lower jaw ceremonial bronze guard
        head.addOrReplaceChild("mask_jaw",
            CubeListBuilder.create().texOffs(113, 34).addBox(-3.0F, -2.0F, -4.8F, 6.0F, 2.0F, 1.0F, deformation),
            PartPose.ZERO);

        // 2. Chestplate (Attached to body)
        // Architectural collar lintel
        body.addOrReplaceChild("collar_lintel",
            CubeListBuilder.create().texOffs(0, 45).addBox(-4.5F, -1.0F, -2.5F, 9.0F, 2.0F, 5.0F, deformation),
            PartPose.ZERO);

        // Protruding 3D sacred bronze bell on center chest with glowing resonance core
        body.addOrReplaceChild("chest_bell",
            CubeListBuilder.create().texOffs(29, 45).addBox(-2.5F, 2.0F, -3.2F, 5.0F, 6.0F, 2.0F, deformation),
            PartPose.ZERO);

        // Ceremonial 3-post chime rack on the backplate
        body.addOrReplaceChild("back_chime_rack",
            CubeListBuilder.create().texOffs(44, 45).addBox(-3.0F, 1.0F, 2.0F, 6.0F, 8.0F, 1.0F, deformation),
            PartPose.ZERO);

        body.addOrReplaceChild("spine_rail",CubeListBuilder.create().texOffs(59, 45).addBox(-0.5F,1.0F,3.6F,1.0F,9.0F,1.0F, deformation),PartPose.ZERO);
        body.addOrReplaceChild("back_rib_left",CubeListBuilder.create().texOffs(64, 45).addBox(-3.3F,-1.0F,3.5F,1.0F,8.0F,1.0F, deformation),PartPose.ZERO);
        body.addOrReplaceChild("back_rib_right",CubeListBuilder.create().texOffs(69, 45).addBox(2.3F,0.0F,3.5F,1.0F,7.0F,1.0F, deformation),PartPose.ZERO);
        body.addOrReplaceChild("back_crossbar",CubeListBuilder.create().texOffs(74, 45).addBox(-3.5F,7.0F,3.8F,7.0F,1.0F,1.0F, deformation),PartPose.ZERO);
        body.addOrReplaceChild("back_lumbar_band",CubeListBuilder.create().texOffs(91, 45).addBox(-4.0F,10.0F,2.5F,8.0F,1.0F,1.0F, deformation),PartPose.ZERO);
        head.addOrReplaceChild("rear_neck_lame",CubeListBuilder.create().texOffs(110, 45).addBox(-4.0F,-1.0F,3.4F,8.0F,2.0F,1.0F, deformation),PartPose.ZERO);

        // 3. Massive Layered Temple-Eave Pauldrons
        // Left Pauldron
        leftArm.addOrReplaceChild("left_pauldron",
            CubeListBuilder.create().texOffs(0, 56).addBox(-1.0F, -3.0F, -2.5F, 5.0F, 5.0F, 5.0F, deformation),
            PartPose.ZERO);
        leftArm.addOrReplaceChild("left_pauldron_roof",
            CubeListBuilder.create().texOffs(21, 56).addBox(-1.5F, -4.5F, -3.0F, 6.0F, 2.0F, 6.0F, deformation),
            PartPose.ZERO);
        leftArm.addOrReplaceChild("left_pauldron_trim",
            CubeListBuilder.create().texOffs(46, 56).addBox(-1.5F, 1.5F, -3.0F, 6.0F, 2.0F, 6.0F, deformation),
            PartPose.ZERO);
        leftArm.addOrReplaceChild("left_bell_pendant",
            CubeListBuilder.create().texOffs(71, 56).addBox(4.2F, -0.5F, -1.0F, 2.0F, 3.0F, 2.0F, deformation),
            PartPose.ZERO);

        // Right Pauldron
        rightArm.addOrReplaceChild("right_pauldron",
            CubeListBuilder.create().texOffs(80, 56).mirror().addBox(-4.0F, -3.0F, -2.5F, 5.0F, 5.0F, 5.0F, deformation).mirror(false),
            PartPose.ZERO);
        rightArm.addOrReplaceChild("right_pauldron_roof",
            CubeListBuilder.create().texOffs(101, 56).mirror().addBox(-4.5F, -4.5F, -3.0F, 6.0F, 2.0F, 6.0F, deformation).mirror(false),
            PartPose.ZERO);
        rightArm.addOrReplaceChild("right_pauldron_trim",
            CubeListBuilder.create().texOffs(0, 67).mirror().addBox(-4.5F, 1.5F, -3.0F, 6.0F, 2.0F, 6.0F, deformation).mirror(false),
            PartPose.ZERO);
        rightArm.addOrReplaceChild("right_bell_pendant",
            CubeListBuilder.create().texOffs(25, 67).mirror().addBox(-6.2F, -0.5F, -1.0F, 2.0F, 3.0F, 2.0F, deformation).mirror(false),
            PartPose.ZERO);

        // 4. Boots (Attached to right_leg and left_leg)
        rightLeg.addOrReplaceChild("right_ankle_cuff",
            CubeListBuilder.create().texOffs(34, 67).addBox(-2.5F, 7.0F, -2.5F, 5.0F, 2.0F, 5.0F, deformation),
            PartPose.ZERO);
        rightLeg.addOrReplaceChild("right_sabaton",
            CubeListBuilder.create().texOffs(55, 67).addBox(-2.5F, 9.0F, -3.5F, 5.0F, 3.0F, 5.0F, deformation),
            PartPose.ZERO);

        leftLeg.addOrReplaceChild("left_ankle_cuff",
            CubeListBuilder.create().texOffs(76, 67).mirror().addBox(-2.5F, 7.0F, -2.5F, 5.0F, 2.0F, 5.0F, deformation).mirror(false),
            PartPose.ZERO);
        leftLeg.addOrReplaceChild("left_sabaton",
            CubeListBuilder.create().texOffs(97, 67).mirror().addBox(-2.5F, 9.0F, -3.5F, 5.0F, 3.0F, 5.0F, deformation).mirror(false),
            PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    /**
     * Layer 2: Leggings, Armored Tassets (甲裙), and Knee Guards.
     */
    public static LayerDefinition createLegsLayer(CubeDeformation deformation) {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();

        // Clear hat on legs layer as well
        partdefinition.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = partdefinition.getChild("body");
        PartDefinition rightLeg = partdefinition.getChild("right_leg");
        PartDefinition leftLeg = partdefinition.getChild("left_leg");

        // 1. Armored Faulds & Ceremonial Waist Belt (Attached to body)
        body.addOrReplaceChild("waist_belt",
            CubeListBuilder.create().texOffs(0, 34).addBox(-4.5F, 9.0F, -2.5F, 9.0F, 3.0F, 5.0F, deformation),
            PartPose.ZERO);

        body.addOrReplaceChild("belt_boss",
            CubeListBuilder.create().texOffs(29, 34).addBox(-2.0F, 9.0F, -2.8F, 4.0F, 4.0F, 1.0F, deformation),
            PartPose.ZERO);

        body.addOrReplaceChild("front_fauld",
            CubeListBuilder.create().texOffs(40, 34).addBox(-3.0F, 12.0F, -2.6F, 6.0F, 5.0F, 1.0F, deformation),
            PartPose.ZERO);

        body.addOrReplaceChild("left_fauld",
            CubeListBuilder.create().texOffs(55, 34).addBox(3.8F, 11.5F, -2.5F, 1.0F, 5.0F, 5.0F, deformation),
            PartPose.ZERO);

        body.addOrReplaceChild("right_fauld",
            CubeListBuilder.create().texOffs(68, 34).mirror().addBox(-4.8F, 11.5F, -2.5F, 1.0F, 5.0F, 5.0F, deformation).mirror(false),
            PartPose.ZERO);

        body.addOrReplaceChild("rear_fauld",
            CubeListBuilder.create().texOffs(81, 34).addBox(-3.5F, 12.0F, 2.4F, 3.0F, 4.0F, 1.0F, deformation),
            PartPose.ZERO);

        body.addOrReplaceChild("rear_fauld_right",CubeListBuilder.create().texOffs(90, 34).addBox(.5F,12.0F,2.4F,3.0F,4.0F,1.0F,deformation),PartPose.ZERO);

        // 2. Rectangular Bronze Knee Bosses & Cyan Inlays
        rightLeg.addOrReplaceChild("right_knee_guard",
            CubeListBuilder.create().texOffs(99, 34).addBox(-2.5F, 3.5F, -2.5F, 5.0F, 4.0F, 2.0F, deformation),
            PartPose.ZERO);
        rightLeg.addOrReplaceChild("right_knee_inlay",
            CubeListBuilder.create().texOffs(114, 34).addBox(-1.5F, 4.5F, -2.8F, 3.0F, 2.0F, 1.0F, deformation),
            PartPose.ZERO);

        leftLeg.addOrReplaceChild("left_knee_guard",
            CubeListBuilder.create().texOffs(0, 45).mirror().addBox(-2.5F, 3.5F, -2.5F, 5.0F, 4.0F, 2.0F, deformation).mirror(false),
            PartPose.ZERO);
        leftLeg.addOrReplaceChild("left_knee_inlay",
            CubeListBuilder.create().texOffs(15, 45).mirror().addBox(-1.5F, 4.5F, -2.8F, 3.0F, 2.0F, 1.0F, deformation).mirror(false),
            PartPose.ZERO);

        return LayerDefinition.create(meshdefinition, 128, 128);
    }
}
