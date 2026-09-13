package cn.suiyi.relicward.test;

import cn.suiyi.relicward.*;
import cn.suiyi.relicward.block.CourtAltarEntity;
import cn.suiyi.relicward.client.BellWardenRenderer;
import cn.suiyi.relicward.combat.WardenAction;
import cn.suiyi.relicward.entity.BellWarden;
import cn.suiyi.relicward.item.RelicEquipment;
import com.mojang.logging.LogUtils;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid=RelicWard.ID,value=Dist.CLIENT)
public final class ClientCourtSmoke {
    private static int stage,ticks;private static UUID bossId;private static volatile boolean ready;private static volatile Throwable failure;private static volatile String capture;
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){
        if(e.phase!=TickEvent.Phase.END||capture==null)return;var mc=Minecraft.getInstance();String n=capture;capture=null;
        try(var im=Screenshot.takeScreenshot(mc.getMainRenderTarget())){im.writeToFile(mc.gameDirectory.toPath().resolve(n+".png"));}catch(Exception ex){failure=ex;}
    }
    private static void camera(double x,double y,double z,float yaw,float pitch){
        var mc=Minecraft.getInstance();var id=mc.player.getUUID();var server=mc.getSingleplayerServer();
        server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.teleportTo(server.overworld(),x,y,z,yaw,pitch);p.getAbilities().flying=true;p.onUpdateAbilities();});
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("relicward.courtSmoke"))return;
        var mc=Minecraft.getInstance();if(failure!=null)throw new RuntimeException(failure);
        if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            stage=1;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);mc.options.fov().set(60);mc.getTutorial().setStep(TutorialSteps.NONE);
            for(var item:RelicWard.ITEMS.getEntries())if(mc.getItemRenderer().getModel(new ItemStack(item.get()),null,null,0)==mc.getModelManager().getMissingModel())throw new IllegalStateException("Missing item "+item.getId());
            var trophy=mc.getItemRenderer().getModel(new ItemStack(RelicContent.TROPHY.get()),null,null,0);
            for(var quad:trophy.getQuads(null,null,net.minecraft.util.RandomSource.create(1)))
                if(quad.getSprite().contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()))throw new IllegalStateException("Missing trophy atlas texture");
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            mc.createWorldOpenFlows().createFreshLevel("court-070-"+System.currentTimeMillis(),new LevelSettings("Relic Ward court",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(8172,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }else if(stage==1&&mc.level!=null&&mc.player!=null&&mc.getSingleplayerServer()!=null){
            stage=2;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{try{
                var l=server.overworld();l.setDayTime(5000);var origin=new BlockPos(-32,-62,-24);for(int x=-2;x<=2;x++)for(int z=-2;z<=3;z++)l.getChunk(x,z);
                l.getStructureManager().getOrCreate(new ResourceLocation("relicward","resonant_court")).placeInWorld(l,origin,origin,new StructurePlaceSettings().addProcessor(JigsawReplacementProcessor.INSTANCE),l.random,2);
                var altar=(CourtAltarEntity)l.getBlockEntity(new BlockPos(0,-57,17));if(altar.obstruction()!=null)throw new IllegalStateException("Blocked court "+altar.obstruction());
                var boss=altar.ensureBoss();bossId=boss.getUUID();
                var p=server.getPlayerList().getPlayer(id);p.teleportTo(l,48,-22,72,142,22);p.getAbilities().flying=true;p.onUpdateAbilities();
                l.setBlock(new BlockPos(0,-57,50),net.minecraft.world.level.block.Blocks.POLISHED_DEEPSLATE.defaultBlockState(),3);
                l.setBlock(new BlockPos(0,-56,50),RelicContent.TROPHY.get().defaultBlockState().setValue(cn.suiyi.relicward.block.WardenTrophyBlock.FACING,net.minecraft.core.Direction.SOUTH),3);
                ready=true;
            }catch(Throwable ex){failure=ex;}});
        }else if(stage==2&&ready&&mc.screen==null){
            mc.options.hideGui=true;if(++ticks<120)return;capture="court070_overview";stage=3;ticks=0;
        }else if(stage==3&&++ticks==10){camera(2,-55.5,53,144,16);stage=4;ticks=0;
        }else if(stage==4&&++ticks==35){capture="court070_trophy";stage=5;ticks=0;
        }else if(stage==5&&++ticks==10){camera(-8,-54,0,45,14);stage=6;ticks=0;
        }else if(stage==6&&++ticks==35){capture="court070_gallery";stage=7;ticks=0;
        }else if(stage==7&&++ticks==10){camera(-12,-55.8,-11,140,23);stage=8;ticks=0;
        }else if(stage==8&&++ticks==35){capture="court070_candles";stage=9;ticks=0;
        }else if(stage==9&&++ticks==10){camera(6,-57,9,146,-8);stage=10;ticks=0;
        }else if(stage==10&&++ticks==40){
            capture="court070_gaze";
            var model=new cn.suiyi.relicward.client.BellWardenModel(mc.getEntityModels().bakeLayer(cn.suiyi.relicward.client.BellWardenModel.LAYER));
            var b=RelicWard.BELL_WARDEN.get().create(mc.level);b.setAction(WardenAction.WAVE);b.yBodyRot=b.yBodyRotO=0;b.yHeadRot=b.yHeadRotO=40;b.setXRot(25);b.xRotO=25;
            model.setupAnim(b,0,0,b.tickCount,40,25);
            if(model.root().getChild("body").getChild("head").yRot<.3F)throw new IllegalStateException("Authored animation erased gaze");
            b.setAction(WardenAction.CHARGE);model.setupAnim(b,0,0,b.tickCount,40,25);
            if(Math.abs(model.root().getChild("body").getChild("head").yRot)>.001F)throw new IllegalStateException("Charge must retain locked gaze");
            LogUtils.getLogger().info("RELICWARD_COURT_SMOKE_OK: court unobstructed, trophy models, gaze layered after poses, charge lock preserved");stage=11;ticks=0;
        }else if(stage==11&&++ticks>20){mc.options.hideGui=false;mc.stop();stage=12;}
    }
}
