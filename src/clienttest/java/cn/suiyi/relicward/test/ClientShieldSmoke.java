package cn.suiyi.relicward.test;

import cn.suiyi.relicward.*;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=RelicWard.ID,value=Dist.CLIENT)
public final class ClientShieldSmoke {
    private static int stage,ticks;private static volatile boolean ready;private static volatile Throwable failure;private static String capture;
    private static InteractionHand hand=InteractionHand.MAIN_HAND;
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){
        if(Boolean.getBoolean("relicward.shieldSmoke")&&e.phase==TickEvent.Phase.START&&stage==3){var p=Minecraft.getInstance().player;if(p!=null){float angle=((ticks>=23&&ticks<=27)||(ticks>=63&&ticks<=72))?55:0;p.yBodyRot=angle;p.yBodyRotO=angle;}}
        if(e.phase!=TickEvent.Phase.END||capture==null)return;var mc=Minecraft.getInstance();var n=capture;capture=null;
        try(var im=Screenshot.takeScreenshot(mc.getMainRenderTarget())){im.writeToFile(mc.gameDirectory.toPath().resolve(n+".png"));}catch(Exception x){failure=x;}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("relicward.shieldSmoke"))return;
        var mc=Minecraft.getInstance();if(failure!=null)throw new RuntimeException(failure);
        if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            stage=1;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(5);mc.options.fov().set(65);mc.options.hideGui=false;mc.getTutorial().setStep(TutorialSteps.NONE);
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            mc.createWorldOpenFlows().createFreshLevel("shield085-"+System.currentTimeMillis(),new LevelSettings("Shield pose review",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(8172,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }else if(stage==1&&mc.level!=null&&mc.player!=null&&mc.getSingleplayerServer()!=null){
            stage=2;var id=mc.player.getUUID();var server=mc.getSingleplayerServer();server.execute(()->{try{
                var l=server.overworld();l.setDayTime(4500);var p=server.getPlayerList().getPlayer(id);p.teleportTo(l,0,-60,0,0,0);
                p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(ChapterContent.HELMET.get()));p.setItemSlot(EquipmentSlot.CHEST,new ItemStack(ChapterContent.CHESTPLATE.get()));p.setItemSlot(EquipmentSlot.LEGS,new ItemStack(ChapterContent.LEGGINGS.get()));p.setItemSlot(EquipmentSlot.FEET,new ItemStack(ChapterContent.BOOTS.get()));
                p.setItemInHand(hand,new ItemStack(ChapterContent.SHIELD.get()));ready=true;
            }catch(Throwable t){failure=t;}});
        }else if(stage==2&&ready&&mc.screen==null){
            if(++ticks<80)return;mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);stage=3;ticks=0;
        }else if(stage==3){
            ticks++;String side=hand==InteractionHand.MAIN_HAND?"main":"off";
            if(ticks==20)capture="shield085_"+side+"_rest";
            if(ticks==26)capture="shield085_"+side+"_rest_side";
            if(ticks==30){mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,hand);}
            if(ticks==60){if(!mc.player.isBlocking())throw new IllegalStateException("Shield failed to block: "+hand);capture="shield085_"+side+"_guard";}
            if(ticks==70)capture="shield085_"+side+"_guard_side";
            if(ticks==75)mc.options.setCameraType(CameraType.FIRST_PERSON);
            if(ticks==95)capture="shield085_"+side+"_first_guard";
            if(ticks==105){mc.options.keyUse.setDown(false);mc.gameMode.releaseUsingItem(mc.player);}
            if(ticks==125)capture="shield085_"+side+"_first_rest";
            if(ticks==140){
                if(hand==InteractionHand.MAIN_HAND){hand=InteractionHand.OFF_HAND;var id=mc.player.getUUID();var s=mc.getSingleplayerServer();s.execute(()->{var p=s.getPlayerList().getPlayer(id);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ChapterContent.SHIELD.get()));});mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);ticks=0;}
                else {
                    stage=4;ticks=0;mc.options.hideGui=true;mc.options.fov().set(40);var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
                    server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=server.overworld();p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);l.setBlock(new net.minecraft.core.BlockPos(2,-60,3),net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState(),3);l.setBlock(new net.minecraft.core.BlockPos(2,-59,3),RelicContent.TROPHY.get().defaultBlockState(),3);p.teleportTo(l,2.5,-60,5.7,180,12);});
                }
            }
        }else if(stage==4){
            ticks++;
            if(ticks==30||ticks==60||ticks==90||ticks==120){capture="trophy085_view_"+(ticks/30);}
            if(ticks==35||ticks==65||ticks==95){var server=mc.getSingleplayerServer();var id=mc.player.getUUID();int angle=ticks;server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(angle==35)p.teleportTo(server.overworld(),4.7,-60,3.5,90,12);else if(angle==65)p.teleportTo(server.overworld(),2.5,-60,1.3,0,12);else p.teleportTo(server.overworld(),.3,-60,3.5,-90,12);});}
            if(ticks==135){LogUtils.getLogger().info("RELICWARD_SHIELD_SMOKE_OK: both hands block; twelve shield captures and four trophy views");mc.stop();stage=5;}
        }
    }
}
