package cn.suiyi.relicward.test;

import cn.suiyi.relicward.*;
import cn.suiyi.relicward.block.CourtAltarEntity;
import cn.suiyi.relicward.combat.WardenAction;
import cn.suiyi.relicward.entity.BellWarden;
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
public final class ClientExperienceSmoke {
    private static int stage,ticks;private static UUID bossId;private static volatile boolean ready;private static volatile Throwable failure;private static volatile String capture;
    private static final BlockPos ALTAR=new BlockPos(0,-57,17);
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){
        if(e.phase!=TickEvent.Phase.END||capture==null)return;var mc=Minecraft.getInstance();String name=capture;capture=null;
        try(var im=Screenshot.takeScreenshot(mc.getMainRenderTarget())){im.writeToFile(mc.gameDirectory.toPath().resolve(name+".png"));}catch(Exception ex){failure=ex;}
    }
    private static void action(WardenAction action){var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->((BellWarden)mc.getSingleplayerServer().overworld().getEntity(bossId)).setAction(action));}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("relicward.experienceSmoke"))return;var mc=Minecraft.getInstance();if(failure!=null)throw new RuntimeException(failure);
        if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            stage=1;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);mc.options.fov().set(60);mc.getTutorial().setStep(TutorialSteps.NONE);
            for(var item:RelicWard.ITEMS.getEntries())if(mc.getItemRenderer().getModel(new ItemStack(item.get()),null,null,0)==mc.getModelManager().getMissingModel())throw new IllegalStateException("Missing item: "+item.getId());
            if(mc.getResourceManager().getResource(new ResourceLocation("relicward","textures/effect/resonance.png")).isEmpty())throw new IllegalStateException("Missing rune texture");
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            mc.createWorldOpenFlows().createFreshLevel("experience-"+System.currentTimeMillis(),new LevelSettings("Relic Ward experience",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(8172,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }else if(stage==1&&mc.level!=null&&mc.player!=null&&mc.getSingleplayerServer()!=null){
            stage=2;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{try{
                var l=server.overworld();l.setDayTime(6000);var origin=new BlockPos(-32,-62,-24);for(int x=-2;x<=2;x++)for(int z=-2;z<=3;z++)l.getChunk(x,z);
                l.getStructureManager().getOrCreate(new ResourceLocation("relicward","resonant_court")).placeInWorld(l,origin,origin,new StructurePlaceSettings().addProcessor(JigsawReplacementProcessor.INSTANCE),l.random,2);
                var altar=(CourtAltarEntity)l.getBlockEntity(ALTAR);var boss=altar.ensureBoss();bossId=boss.getUUID();
                var p=server.getPlayerList().getPlayer(id);p.teleportTo(l,7,-54,10,145,10);p.getAbilities().flying=true;p.onUpdateAbilities();ready=true;
            }catch(Throwable ex){failure=ex;}});
        }else if(stage==2&&ready&&mc.screen==null){
            if(++ticks<100)return;mc.options.hideGui=true;stage=3;ticks=0;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{
                var p=server.getPlayerList().getPlayer(id);p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();
                var boss=(BellWarden)server.overworld().getEntity(bossId);boss.startEncounter();boss.setAction(WardenAction.DOUBLE_SWEEP);
            });
        }else if(stage==3){
            ticks++;if(ticks==7)capture="experience_sweep_warning";if(ticks==17)capture="experience_sweep_trail";if(ticks==23)capture="experience_second_warning";if(ticks==30)capture="experience_reverse_trail";
            if(ticks==72){stage=4;ticks=0;action(WardenAction.WAVE);}
        }else if(stage==4){
            ticks++;if(ticks==32)capture="experience_wave";if(ticks==96){stage=5;ticks=0;action(WardenAction.SLAM);}
        }else if(stage==5){
            ticks++;if(ticks==18)capture="experience_slam_rune";if(ticks==28)capture="experience_slam_debris";
            if(ticks==65){stage=6;ticks=0;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{
                var boss=(BellWarden)server.overworld().getEntity(bossId);boss.setAction(WardenAction.IDLE);boss.setNoAi(true);boss.hurt(server.overworld().damageSources().playerAttack(server.getPlayerList().getPlayer(id)),10000);
            });}
        }else if(stage==6){
            ticks++;if(ticks==45)capture="experience_victory_aura";
            if(ticks==95){stage=7;ticks=0;mc.options.hideGui=false;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{try{
                var p=server.getPlayerList().getPlayer(id);if(p.getInventory().countItem(RelicContent.CORE.get())!=1||p.getInventory().countItem(RelicContent.TROPHY.get().asItem())!=1)throw new IllegalStateException("Rewards not automatically delivered");
                var altar=(CourtAltarEntity)server.overworld().getBlockEntity(ALTAR);if(p.gameMode.destroyBlock(altar.center().below()))throw new IllegalStateException("Arena is breakable");
                // Shorten only the isolated test altar's cooldown; production remains five minutes.
                var field=CourtAltarEntity.class.getDeclaredField("readyAt");field.setAccessible(true);field.setLong(altar,server.overworld().getGameTime()+60);altar.advanceRespawn();
            }catch(Throwable ex){failure=ex;}});}
        }else if(stage==7){
            ticks++;if(ticks==12)capture="experience_auto_rewards";if(ticks==38){
                var altar=(CourtAltarEntity)mc.level.getBlockEntity(ALTAR);if(altar==null||!altar.completed()||altar.respawnAt()==0)throw new IllegalStateException("Respawn ceremony not synchronized to client");
                capture="experience_respawn_ritual";
            }
            if(ticks==95){stage=8;ticks=0;var server=mc.getSingleplayerServer();server.execute(()->{try{
                var altar=(CourtAltarEntity)server.overworld().getBlockEntity(ALTAR);var boss=altar.boss();
                if(boss==null||boss.isEncounterActive()||boss.getUUID().equals(bossId))throw new IllegalStateException("Automatic dormant respawn failed");
                LogUtils.getLogger().info("RELICWARD_EXPERIENCE_SMOKE_OK: auto rewards, protected floor, dormant respawn");
            }catch(Throwable ex){failure=ex;}});capture="experience_respawn_ready";}
        }else if(stage==8&&++ticks>25){mc.stop();stage=9;}
    }
}
