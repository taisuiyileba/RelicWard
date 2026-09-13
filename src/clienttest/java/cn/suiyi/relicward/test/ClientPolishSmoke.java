package cn.suiyi.relicward.test;

import cn.suiyi.relicward.*;
import cn.suiyi.relicward.block.CourtAltarEntity;
import cn.suiyi.relicward.client.WardenAnimator;
import cn.suiyi.relicward.combat.WardenAction;
import cn.suiyi.relicward.entity.*;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=RelicWard.ID,value=Dist.CLIENT)
public final class ClientPolishSmoke {
    private static int stage,ticks;private static UUID bossId,puppetId;private static volatile boolean ready;private static volatile Throwable failure;
    private static volatile String capture;
    private static void action(WardenAction move){var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->((BellWarden)mc.getSingleplayerServer().overworld().getEntity(bossId)).setAction(move));}
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event){
        if(event.phase!=TickEvent.Phase.END||capture==null)return;
        var mc=Minecraft.getInstance();String name=capture;capture=null;
        try(var img=Screenshot.takeScreenshot(mc.getMainRenderTarget())){img.writeToFile(mc.gameDirectory.toPath().resolve(name+".png"));}
        catch(Exception e){failure=e;}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("relicward.polishSmoke"))return;
        if(failure!=null)throw new RuntimeException(failure);var mc=Minecraft.getInstance();
        if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            stage=1;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);mc.options.fov().set(55);mc.getTutorial().setStep(TutorialSteps.NONE);
            for(var move:WardenAction.values())for(int i=1;i<move.duration*4;i++)for(int c=0;c<84;c++){
                float a=WardenAnimator.component(move,i*.25F,c),b=WardenAnimator.component(move,i*.25F-.001F,c);
                if(!Float.isFinite(a)||Math.abs(a-b)>1)throw new IllegalStateException("Animation discontinuity: "+move);
            }
            for(var item:RelicWard.ITEMS.getEntries())if(mc.getItemRenderer().getModel(new ItemStack(item.get()),null,null,0)==mc.getModelManager().getMissingModel())throw new IllegalStateException("Missing item "+item.getId());
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            mc.createWorldOpenFlows().createFreshLevel("polish-"+System.currentTimeMillis(),new LevelSettings("Relic Ward polish",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(8172,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }else if(stage==1&&mc.level!=null&&mc.player!=null&&mc.getSingleplayerServer()!=null){
            stage=2;ticks=0;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
            server.execute(()->{try{
                var l=server.overworld();l.setDayTime(5000);var origin=new BlockPos(-32,-62,-24);
                for(int x=-2;x<=2;x++)for(int z=-2;z<=3;z++)l.getChunk(x,z);
                l.getStructureManager().getOrCreate(new ResourceLocation("relicward","resonant_court")).placeInWorld(l,origin,origin,new StructurePlaceSettings().addProcessor(JigsawReplacementProcessor.INSTANCE),l.random,2);
                var altar=(CourtAltarEntity)l.getBlockEntity(new BlockPos(0,-57,17));if(altar.obstruction()!=null)throw new IllegalStateException("Decor blocks lane at "+altar.obstruction());
                var boss=altar.ensureBoss();boss.setNoAi(true);bossId=boss.getUUID();
                var puppet=RelicWard.COURT_PUPPET.get().create(l);puppet.moveTo(4,-57,4,0,0);puppet.setNoAi(true);l.addFreshEntity(puppet);puppetId=puppet.getUUID();
                var p=server.getPlayerList().getPlayer(id);p.teleportTo(l,48,-22,72,142,22);p.getAbilities().flying=true;p.onUpdateAbilities();ready=true;
            }catch(Throwable ex){failure=ex;}});
        }else if(stage==2&&ready&&mc.screen==null){
            mc.options.hideGui=true;if(++ticks<140)return;capture="polish_court";stage=12;ticks=0;
            var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
            server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.teleportTo(server.overworld(),-10,-55,37,114,10);p.getAbilities().flying=true;p.onUpdateAbilities();});
        }else if(stage==12&&++ticks==40){capture="polish_archive";stage=13;ticks=0;
            var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
            server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.teleportTo(server.overworld(),10,-55,37,-55,10);p.getAbilities().flying=true;p.onUpdateAbilities();});
        }else if(stage==13&&++ticks==40){capture="polish_workshop";stage=3;ticks=0;
            var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
            server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.teleportTo(server.overworld(),9,-53,15,151,13);p.getAbilities().flying=true;p.onUpdateAbilities();});
        }else if(stage==3&&++ticks==30){action(WardenAction.SWEEP);stage=4;ticks=0;
        }else if(stage==4){
            ticks++;if(ticks==14)capture="polish_sweep_wind";if(ticks==19)capture="polish_sweep_contact";if(ticks==24)capture="polish_sweep_follow";
            if(ticks==50){action(WardenAction.SLAM);stage=5;ticks=0;}
        }else if(stage==5){
            ticks++;if(ticks==19)capture="polish_slam_wind";if(ticks==25)capture="polish_slam_contact";
            if(ticks==70){stage=6;ticks=0;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{
                var l=server.overworld();var boss=(BellWarden)l.getEntity(bossId);boss.setAction(WardenAction.DORMANT);boss.setPos(-12,-57,0);
                var puppet=(CourtPuppet)l.getEntity(puppetId);puppet.moveTo(0,-57,0,0,0);
                var p=server.getPlayerList().getPlayer(id);p.teleportTo(l,2.8,-55.8,5,151,13);p.getAbilities().flying=true;p.onUpdateAbilities();
            });}
        }else if(stage==6&&++ticks==25){stage=7;ticks=0;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->((CourtPuppet)server.overworld().getEntity(puppetId)).beginStrike(server.getPlayerList().getPlayer(id),false));
        }else if(stage==7){
            ticks++;if(ticks==8)capture="polish_puppet_wind";if(ticks==13)capture="polish_puppet_contact";
            if(ticks==42){stage=8;ticks=0;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->((CourtPuppet)server.overworld().getEntity(puppetId)).beginStrike(server.getPlayerList().getPlayer(id),true));}
        }else if(stage==8){
            ticks++;if(ticks==17)capture="polish_puppet_bash";
            if(ticks==46){stage=9;ticks=0;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RelicContent.MAUL.get()));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(RelicContent.PENDANT.get()));});mc.options.hideGui=false;}
        }else if(stage==9&&++ticks==25){capture="polish_items_held";LogUtils.getLogger().info("RELICWARD_POLISH_SMOKE_OK");stage=10;ticks=0;
        }else if(stage==10&&++ticks>20){mc.stop();stage=11;}
    }
}
