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
public final class ClientGravitySmoke {
    private static int stage,ticks;private static UUID bossId;private static volatile boolean ready;private static volatile Throwable failure;private static volatile String capture;
    private static final BlockPos ALTAR=new BlockPos(0,-57,17);
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){
        if(e.phase!=TickEvent.Phase.END||capture==null)return;var mc=Minecraft.getInstance();String name=capture;capture=null;
        try(var im=Screenshot.takeScreenshot(mc.getMainRenderTarget())){im.writeToFile(mc.gameDirectory.toPath().resolve(name+".png"));}catch(Exception ex){failure=ex;}
    }
    private static void action(WardenAction action){var mc=Minecraft.getInstance();mc.getSingleplayerServer().execute(()->((BellWarden)mc.getSingleplayerServer().overworld().getEntity(bossId)).setAction(action));}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("relicward.gravitySmoke"))return;var mc=Minecraft.getInstance();if(failure!=null)throw new RuntimeException(failure);
        if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            stage=1;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);mc.options.fov().set(60);mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MASTER).set(0.15);mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set(1.0);mc.getTutorial().setStep(TutorialSteps.NONE);
            for(var item:RelicWard.ITEMS.getEntries())if(mc.getItemRenderer().getModel(new ItemStack(item.get()),null,null,0)==mc.getModelManager().getMissingModel())throw new IllegalStateException("Missing item: "+item.getId());
            if(mc.getResourceManager().getResource(new ResourceLocation("relicward","textures/effect/resonance.png")).isEmpty())throw new IllegalStateException("Missing rune texture");
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            mc.createWorldOpenFlows().createFreshLevel("gravity087-"+System.currentTimeMillis(),new LevelSettings("Relic Ward experience",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(8172,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }else if(stage==1&&mc.level!=null&&mc.player!=null&&mc.getSingleplayerServer()!=null){
            stage=2;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{try{
                var l=server.overworld();l.setDayTime(6000);var origin=new BlockPos(-32,-62,-24);for(int x=-2;x<=2;x++)for(int z=-2;z<=3;z++)l.getChunk(x,z);
                l.getStructureManager().getOrCreate(new ResourceLocation("relicward","resonant_court")).placeInWorld(l,origin,origin,new StructurePlaceSettings().addProcessor(JigsawReplacementProcessor.INSTANCE),l.random,2);
                var altar=(CourtAltarEntity)l.getBlockEntity(ALTAR);var boss=altar.ensureBoss();bossId=boss.getUUID();
                var p=server.getPlayerList().getPlayer(id);p.teleportTo(l,7,-54,10,145,10);p.getAbilities().flying=true;p.onUpdateAbilities();ready=true;
            }catch(Throwable ex){failure=ex;}});
        }else if(stage==2&&ready&&mc.screen==null){
            if(++ticks<100)return;mc.options.hideGui=false;stage=3;ticks=0;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{
                var p=server.getPlayerList().getPlayer(id);p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();
                var boss=(BellWarden)server.overworld().getEntity(bossId);boss.startEncounter();boss.setAction(WardenAction.IDLE);
            });
        }else if(stage==3){
            ticks++;
            if(ticks==60){if(mc.player.hasEffect(cn.suiyi.relicward.combat.SuperGravity.EFFECT.get()))throw new IllegalStateException("Gravity during phase one");capture="gravity087_phase_one";checkMusic(1);}
            if(ticks==80){var server=mc.getSingleplayerServer();server.execute(()->{var boss=(BellWarden)server.overworld().getEntity(bossId);boss.setHealth(boss.getMaxHealth()*.4F);boss.setAction(WardenAction.IDLE);});}
            if(ticks==210){if(!mc.player.hasEffect(cn.suiyi.relicward.combat.SuperGravity.EFFECT.get())||mc.player.getAbilities().flying||!mc.player.getAbilities().mayfly)throw new IllegalStateException("Client gravity or flight sync failed");
                var boss=mc.level.getEntitiesOfClass(BellWarden.class,mc.player.getBoundingBox().inflate(64)).stream().filter(b->b.getUUID().equals(bossId)).findFirst().orElseThrow();
                if(mc.level.getEntitiesOfClass(cn.suiyi.relicward.entity.CourtPuppet.class,boss.getBoundingBox().inflate(30),p->p.isReinforcement()).size()!=4)throw new IllegalStateException("Four reinforcements not synchronized to client");
                if(Math.abs(mc.player.getAttributeValue(net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get())-.128)>1e-6)throw new IllegalStateException("Strengthened gravity not synced");
                capture="gravity087_phase_two";checkMusic(2);}
            if(ticks==215)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            if(ticks==225)capture="gravity087_effect_inventory";
            if(ticks==235)mc.setScreen(null);
            if(ticks==240){var server=mc.getSingleplayerServer();server.execute(()->((BellWarden)server.overworld().getEntity(bossId)).resetEncounter());}
            if(ticks==300){
                if(mc.player.hasEffect(cn.suiyi.relicward.combat.SuperGravity.EFFECT.get()))throw new IllegalStateException("Gravity survived reset");
                if(!mc.level.getEntitiesOfClass(cn.suiyi.relicward.entity.CourtPuppet.class,mc.player.getBoundingBox().inflate(64),p->p.isReinforcement()).isEmpty())throw new IllegalStateException("Reinforcements survived reset on client");
                try {var f=cn.suiyi.relicward.client.WardenBattleMusic.class.getDeclaredField("current");f.setAccessible(true);if(f.get(null)!=null)throw new IllegalStateException("Music survived encounter reset");}
                catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
                LogUtils.getLogger().info("RELICWARD_GRAVITY_SMOKE_OK: both phase tracks active, HUD screenshots, reset stops music");mc.stop();stage=4;
            }
        }
    }
    private static void checkMusic(int phase){
        try {
            var f=cn.suiyi.relicward.client.WardenBattleMusic.class.getDeclaredField("current");f.setAccessible(true);
            var track=(net.minecraft.client.resources.sounds.SoundInstance)f.get(null);
            if(track==null||!track.getLocation().getPath().endsWith("_"+phase)||!Minecraft.getInstance().getSoundManager().isActive(track))throw new IllegalStateException("Phase "+phase+" music not playing");
        }catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
    }
}
