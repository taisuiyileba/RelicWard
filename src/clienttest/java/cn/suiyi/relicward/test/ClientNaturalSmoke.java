package cn.suiyi.relicward.test;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.block.CourtAltarEntity;
import com.mojang.logging.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=RelicWard.ID,value=Dist.CLIENT)
public final class ClientNaturalSmoke {
    private static int stage,ticks,cx,cz;
    private static volatile boolean ready;
    private static volatile Throwable failure;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END||!Boolean.getBoolean("relicward.naturalSmoke"))return;
        var mc=Minecraft.getInstance();if(failure!=null)throw new RuntimeException(failure);
        if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null) {
            stage=1;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);mc.options.simulationDistance().set(6);mc.options.fov().set(60);
            mc.getTutorial().setStep(TutorialSteps.NONE);
            try {
                String[] fields=Files.readAllLines(Path.of("..","art","validation","worldgen_sites.csv")).get(1).split(",");
                cx=Integer.parseInt(fields[1]);cz=Integer.parseInt(fields[2]);
            }catch(Exception e){throw new RuntimeException(e);}
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            var settings=new LevelSettings("Relic Ward natural generation",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("natural-court-"+System.currentTimeMillis(),settings,new WorldOptions(8172,true,false),WorldPresets::createNormalWorldDimensions);
        }else if(stage==1&&mc.player!=null&&mc.level!=null&&mc.getSingleplayerServer()!=null) {
            stage=2;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
            server.execute(()->{
                try {
                    var level=server.overworld();level.setDayTime(4000);CourtAltarEntity altar=null;
                    for(int x=cx-4;x<=cx+4;x++)for(int z=cz-4;z<=cz+4;z++)
                        for(var be:level.getChunk(x,z).getBlockEntities().values())if(be instanceof CourtAltarEntity c)altar=c;
                    if(altar==null)throw new IllegalStateException("Natural chunk generation did not place an altar at "+cx+","+cz);
                    if(altar.obstruction()!=null)throw new IllegalStateException("Natural arena obstructed at "+altar.obstruction());
                    if(altar.ensureBoss()==null)throw new IllegalStateException("Natural guardian did not spawn");
                    var cam=altar.local(48,35,72);var target=altar.local(0,5,12);
                    double dx=target.getX()-cam.getX(),dy=target.getY()-cam.getY()-1.62,dz=target.getZ()-cam.getZ();
                    float yaw=(float)(Math.atan2(dz,dx)*180/Math.PI)-90,pitch=(float)(-Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))*180/Math.PI);
                    var player=server.getPlayerList().getPlayer(id);player.teleportTo(level,cam.getX(),cam.getY(),cam.getZ(),yaw,pitch);
                    player.getAbilities().flying=true;player.onUpdateAbilities();
                    LogUtils.getLogger().info("RELICWARD_NATURAL_ALTAR {}",altar.getBlockPos());ready=true;
                }catch(Throwable e){failure=e;}
            });
        }else if(stage==2&&ready&&mc.screen==null&&mc.getOverlay()==null) {
            mc.options.hideGui=true;if(++ticks<220)return;
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(mc.gameDirectory.toPath().resolve("court_natural.png")); }
            catch(Exception e){throw new RuntimeException(e);}
            LogUtils.getLogger().info("RELICWARD_NATURAL_SMOKE_OK");stage=3;ticks=0;
        }else if(stage==3&&++ticks>20){mc.stop();stage=4;}
    }
}
