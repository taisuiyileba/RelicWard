package cn.suiyi.relicward.test;

import cn.suiyi.relicward.*;
import cn.suiyi.relicward.block.CourtAltarEntity;
import cn.suiyi.relicward.client.*;
import cn.suiyi.relicward.combat.WardenAction;
import cn.suiyi.relicward.entity.BellWarden;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid=RelicWard.ID,value=Dist.CLIENT)
public final class ClientImpactSmoke {
    private static int stage,ticks;private static UUID bossId;private static volatile boolean ready;private static volatile Throwable failure;private static volatile String capture;
    private static float[] bounds(ModelPart root,ModelPart leg){
        var stack=new PoseStack();root.translateAndRotate(stack);float[] b={Float.MAX_VALUE,-Float.MAX_VALUE,-Float.MAX_VALUE};
        leg.visit(stack,(pose,path,index,cube)->{
            for(float x:new float[]{cube.minX,cube.maxX})for(float y:new float[]{cube.minY,cube.maxY})for(float z:new float[]{cube.minZ,cube.maxZ}){
                var p=pose.pose().transformPosition(new Vector3f(x/16,y/16,z/16)).mul(16);
                b[0]=Math.min(b[0],p.x);b[1]=Math.max(b[1],p.x);b[2]=Math.max(b[2],p.y);
            }
        });return b;
    }
    private static void verifyFeet(Minecraft mc){
        var model=new BellWardenModel(mc.getEntityModels().bakeLayer(BellWardenModel.LAYER));var root=model.root();var animator=new WardenAnimator(root);int samples=0;
        for(var move:WardenAction.values())for(float t=0;t<=Math.max(1,move.duration);t+=.25F){
            root.getAllParts().forEach(ModelPart::resetPose);animator.applyPose(move,t,t,t,.7F);
            float[] right=bounds(root,root.getChild("right_leg")),left=bounds(root,root.getChild("left_leg"));
            if(right[2]>24.04||left[2]>24.04)throw new IllegalStateException("Floor intersection "+move+" tick="+t+" bottoms="+right[2]+","+left[2]);
            if(right[1]>=left[0]-.1)throw new IllegalStateException("Legs overlap "+move+" tick="+t);
            samples++;
        }
        root.getAllParts().forEach(ModelPart::resetPose);animator.applyPose(WardenAction.SLAM,24,24,0,0);
        var stack=new PoseStack();root.translateAndRotate(stack);var body=root.getChild("body");body.translateAndRotate(stack);
        var arm=body.getChild("right_arm");arm.translateAndRotate(stack);var elbow=arm.getChild("right_forearm");elbow.translateAndRotate(stack);elbow.getChild("hammer").translateAndRotate(stack);
        var normal=stack.last().pose().transformDirection(new Vector3f(0,0,1)).normalize();
        var contact=stack.last().pose().transformPosition(new Vector3f(0,21/16F,15/16F)).mul(16);
        if(Math.abs(normal.y-1)>.001||Math.abs(contact.y-24)>.04)throw new IllegalStateException("Hammer face not flat on floor: "+normal+" / "+contact);
        LogUtils.getLogger().info("RELICWARD_FOOT_GEOMETRY_OK {} samples, hammer contact {}",samples,contact);
    }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){
        if(e.phase!=TickEvent.Phase.END||capture==null)return;var mc=Minecraft.getInstance();var name=capture;capture=null;
        try(var im=Screenshot.takeScreenshot(mc.getMainRenderTarget())){im.writeToFile(mc.gameDirectory.toPath().resolve(name+".png"));}catch(Exception error){failure=error;}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("relicward.impactSmoke"))return;
        if(failure!=null)throw new RuntimeException(failure);var mc=Minecraft.getInstance();
        if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            stage=1;verifyFeet(mc);mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);mc.options.fov().set(60);mc.getTutorial().setStep(TutorialSteps.NONE);
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            mc.createWorldOpenFlows().createFreshLevel("impact-"+System.currentTimeMillis(),new LevelSettings("Relic Ward impact",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(8172,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }else if(stage==1&&mc.level!=null&&mc.player!=null&&mc.getSingleplayerServer()!=null){
            stage=2;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{try{
                var l=server.overworld();l.setDayTime(5000);var origin=new BlockPos(-32,-62,-24);for(int x=-2;x<=2;x++)for(int z=-2;z<=3;z++)l.getChunk(x,z);
                l.getStructureManager().getOrCreate(new ResourceLocation("relicward","resonant_court")).placeInWorld(l,origin,origin,new StructurePlaceSettings().addProcessor(JigsawReplacementProcessor.INSTANCE),l.random,2);
                var altar=(CourtAltarEntity)l.getBlockEntity(new BlockPos(0,-57,17));var boss=altar.ensureBoss();boss.setNoAi(true);boss.setAction(WardenAction.IDLE);bossId=boss.getUUID();
                var p=server.getPlayerList().getPlayer(id);p.teleportTo(l,8,-55.5,11,143,4);p.getAbilities().flying=true;p.onUpdateAbilities();ready=true;
            }catch(Throwable error){failure=error;}});
        }else if(stage==2&&ready&&mc.screen==null){
            mc.options.hideGui=true;if(++ticks<100)return;capture="hammer_front_0.4.1";stage=6;ticks=0;
            var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.teleportTo(server.overworld(),-8,-55.5,-11,-36,4);p.getAbilities().flying=true;p.onUpdateAbilities();});
        }else if(stage==6&&++ticks==35){capture="hammer_rear_0.4.1";stage=7;ticks=0;
            var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.teleportTo(server.overworld(),8,-55.5,11,143,4);p.getAbilities().flying=true;p.onUpdateAbilities();});
        }else if(stage==7&&++ticks==35){stage=3;ticks=0;var server=mc.getSingleplayerServer();server.execute(()->((BellWarden)server.overworld().getEntity(bossId)).setAction(WardenAction.CHARGE));
        }else if(stage==3){
            ticks++;if(ticks==70)capture="impact_charge_stop";if(ticks==88)capture="impact_recovery_feet";
            if(ticks==112){stage=4;ticks=0;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{
                var p=server.getPlayerList().getPlayer(id);p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();
                var boss=(BellWarden)server.overworld().getEntity(bossId);boss.setAction(WardenAction.DORMANT);boss.setNoAi(false);boss.startEncounter();boss.setAction(WardenAction.SLAM);
            });}
        }else if(stage==4){
            ticks++;if(ticks==18)capture="impact_raise_hammer";if(ticks==27)capture="impact_slam_contact";if(ticks==35)capture="impact_floor_cracks";if(ticks==63)capture="impact_floor_cleared";
            if(ticks>68){LogUtils.getLogger().info("RELICWARD_IMPACT_SMOKE_OK");stage=5;ticks=0;}
        }else if(stage==5&&++ticks>20){mc.stop();stage=6;}
    }
}
