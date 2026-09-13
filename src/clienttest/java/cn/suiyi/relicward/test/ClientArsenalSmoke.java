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
public final class ClientArsenalSmoke {
    private static int stage,ticks;private static UUID bossId;private static volatile boolean ready;private static volatile Throwable failure;private static volatile String capture;
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){
        if(e.phase!=TickEvent.Phase.END||capture==null)return;var mc=Minecraft.getInstance();String n=capture;capture=null;
        try(var im=Screenshot.takeScreenshot(mc.getMainRenderTarget())){im.writeToFile(mc.gameDirectory.toPath().resolve(n+".png"));}catch(Exception ex){failure=ex;}
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("relicward.arsenalSmoke"))return;
        var mc=Minecraft.getInstance();if(failure!=null){mc.options.keyUse.setDown(false);throw new RuntimeException(failure);}
        if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            stage=1;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);mc.options.fov().set(60);mc.getTutorial().setStep(TutorialSteps.NONE);
            for(var item:RelicWard.ITEMS.getEntries())if(mc.getItemRenderer().getModel(new ItemStack(item.get()),null,null,0)==mc.getModelManager().getMissingModel())throw new IllegalStateException("Missing item model "+item.getId());
            if(!mc.getItemRenderer().getModel(new ItemStack(RelicContent.MAUL.get()),null,null,0).isGui3d())throw new IllegalStateException("Maul GUI must use actual 3D geometry");
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            mc.createWorldOpenFlows().createFreshLevel("maul0810-"+System.currentTimeMillis(),new LevelSettings("Relic Ward gear",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(8172,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }else if(stage==1&&mc.level!=null&&mc.player!=null&&mc.getSingleplayerServer()!=null){
            stage=2;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{try{
                var l=server.overworld();l.setDayTime(5000);var origin=new BlockPos(-32,-62,-24);for(int x=-2;x<=2;x++)for(int z=-2;z<=3;z++)l.getChunk(x,z);
                l.getStructureManager().getOrCreate(new ResourceLocation("relicward","resonant_court")).placeInWorld(l,origin,origin,new StructurePlaceSettings().addProcessor(JigsawReplacementProcessor.INSTANCE),l.random,2);
                var boss=((CourtAltarEntity)l.getBlockEntity(new BlockPos(0,-57,17))).ensureBoss();bossId=boss.getUUID();
                var p=server.getPlayerList().getPlayer(id);p.teleportTo(l,6,-57,9,145,28);p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);
                p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RelicContent.MAUL.get()));p.getInventory().setItem(1,new ItemStack(RelicContent.ALTAR.get()));p.getInventory().setItem(2,new ItemStack(RelicContent.TEACHING_BELL.get()));p.getInventory().setItem(3,new ItemStack(RelicContent.TROPHY.get()));p.getInventory().setItem(4,new ItemStack(RelicContent.WAVE_BELL.get()));
                var slot=CuriosApi.getCuriosInventory(p).orElseThrow(()->new IllegalStateException("No Curios cap")).getStacksHandler("necklace").orElseThrow(()->new IllegalStateException("No necklace slot")).getStacks();
                var pendant=new ItemStack(RelicContent.PENDANT.get());if(!slot.isItemValid(0,pendant))throw new IllegalStateException("Pendant rejected by necklace slot");slot.setStackInSlot(0,pendant);ready=true;
            }catch(Throwable ex){failure=ex;}});
        }else if(stage==2&&ready&&mc.screen==null){
            if(++ticks<120)return;if(!RelicEquipment.pendantEquipped(mc.player)||!mc.player.getOffhandItem().isEmpty())throw new IllegalStateException("Curio did not sync or still requires offhand");
            checkJer();
            var neutral=cn.suiyi.relicward.client.MaulChargeClient.REST;
            if(!cn.suiyi.relicward.client.MaulChargeClient.firstPersonPose(0).equals(neutral)
                ||!cn.suiyi.relicward.client.MaulChargeClient.firstPersonPose(38).equals(neutral))throw new IllegalStateException("First-person endpoints must exactly match resting pose");
            if(cn.suiyi.relicward.client.MaulChargeClient.firstPersonPose(28).z()>=cn.suiyi.relicward.client.MaulChargeClient.firstPersonPose(18).z())throw new IllegalStateException("Impact must travel forward from raised position");
            capture="maul0810_hand_model";stage=3;ticks=0;mc.setScreen(new InventoryScreen(mc.player));
        }else if(stage==3&&++ticks==25){capture="maul0810_inventory_models";stage=4;ticks=0;
        }else if(stage==4&&++ticks==15){
            mc.setScreen(null);mc.options.keyUse.setDown(true);
            if(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit&&hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK)mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,hit);
            else mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
            stage=5;ticks=0;
        }else if(stage==5){
            ticks++;
            capture=String.format("maul0810_motion_%03d",ticks);
            if(ticks==10){
                var ext=net.minecraftforge.client.extensions.common.IClientItemExtensions.of(mc.player.getMainHandItem());
                if(ext.getArmPose(mc.player,InteractionHand.MAIN_HAND,mc.player.getMainHandItem())!=cn.suiyi.relicward.client.MaulChargeClient.CHARGE)throw new IllegalStateException("Custom maul charging arm pose missing");
                capture="maul0810_maul_charging";
            }if(ticks==23)capture="maul0810_maul_downswing";if(ticks==30)capture="maul0810_maul_impact";
            if(ticks==52){mc.options.keyUse.setDown(false);stage=6;ticks=0;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{try{
                var p=server.getPlayerList().getPlayer(id);if(p.getMainHandItem().getDamageValue()!=3||RelicEquipment.cooldown(p,"Maul")<=0)throw new IllegalStateException("Right-click charge did not fire exactly once");
                var boss=(BellWarden)server.overworld().getEntity(bossId);boss.startEncounter();boss.setAction(WardenAction.CHARGE);
            }catch(Throwable ex){failure=ex;}});}
        }else if(stage==6){
            ticks++;
            if(ticks==5||ticks==12){final int step=ticks;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{var p=server.getPlayerList().getPlayer(id);double x=step==5?-8:8,z=10;float yaw=step==5?-141:141;p.teleportTo(server.overworld(),x,-57,z,yaw,8);});}
            var bosses=mc.level.getEntitiesOfClass(BellWarden.class,mc.player.getBoundingBox().inflate(30));
            for(var b:bosses)if(b.getUUID().equals(bossId)&&b.action()==WardenAction.CHARGE&&Math.abs(BellWardenRenderer.facingForRender(b,999)-b.attackYaw())>.001)throw new IllegalStateException("Renderer and warning yaw diverge");
            if(ticks==10)capture="maul0810_charge_heading_left";if(ticks==20)capture="maul0810_charge_heading_right";
            if(ticks==32){LogUtils.getLogger().info("RELICWARD_GEAR_SMOKE_OK: shared model, actual right-click auto slam, synced necklace, charge heading");stage=7;ticks=0;}
        }else if(stage==7){
            ticks++;
            if(ticks==60){
                mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);
            }
            if(ticks==71)capture="maul0810_charge_third_person";
            if(ticks==83)capture="maul0810_downswing_third_person";
            if(ticks==91)capture="maul0810_impact_third_person";
            if(ticks==106){
                mc.options.keyUse.setDown(false);mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                var server=mc.getSingleplayerServer();server.execute(()->{
                    var p=server.getPlayerList().getPlayer(mc.player.getUUID());var boss=(BellWarden)server.overworld().getEntity(bossId);boss.resetEncounter();boss.setGlowingTag(true);
                    if(boss.getMaxHealth()!=480||boss.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)!=10)throw new IllegalStateException("Boss health/armor not updated");
                    var puppet=RelicWard.COURT_PUPPET.get().create(server.overworld());puppet.moveTo(4,-57,8,0,0);puppet.setNoAi(true);puppet.setGlowingTag(true);server.overworld().addFreshEntity(puppet);
                    p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(RelicContent.WAVE_BELL.get()));
                    cn.suiyi.relicward.combat.ArmorFracture.apply(p);p.teleportTo(server.overworld(),6,-57,9,145,35);
                });
            }
            if(ticks==122){mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);}
            if(ticks==129){
                if(mc.level.getEntitiesOfClass(cn.suiyi.relicward.entity.ResonantWave.class,mc.player.getBoundingBox().inflate(30)).isEmpty())throw new IllegalStateException("Handbell wave not networked");
                for(var mob:mc.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,mc.player.getBoundingBox().inflate(30)))
                    if((mob instanceof BellWarden||mob instanceof cn.suiyi.relicward.entity.CourtPuppet)&&mc.shouldEntityAppearGlowing(mob))throw new IllegalStateException("Red outline still enabled");
                capture="maul0810_handbell_wave";
            }
            if(ticks==140)mc.setScreen(new InventoryScreen(mc.player));
            if(ticks==147)capture="maul0810_fracture_inventory";
            if(ticks==162){LogUtils.getLogger().info("RELICWARD_MAUL0810_SMOKE_OK: JER actual registry, charge arm pose, handbell wave, fracture icon, outlines suppressed");mc.stop();stage=8;}
        }
    }
    private static void checkJer(){
        try {
            var type=Class.forName("jeresources.registry.MobRegistry");var registry=type.getMethod("getInstance").invoke(null);
            var mobs=(java.util.List<?>)type.getMethod("getMobs").invoke(registry);Object found=null;
            for(Object entry:mobs){var mob=(net.minecraft.world.entity.LivingEntity)entry.getClass().getMethod("getEntity").invoke(entry);if(mob.getType()==RelicWard.BELL_WARDEN.get()){found=entry;break;}}
            if(found==null)throw new IllegalStateException("JER does not list Bell Warden");
            var drops=(java.util.List<ItemStack>)found.getClass().getMethod("getDropsItemStacks").invoke(found);
            for(var item:java.util.List.of(RelicContent.CORE.get(),RelicContent.FRAGMENT.get(),RelicContent.BRICKS.get().asItem()))if(drops.stream().noneMatch(s->s.is(item)))throw new IllegalStateException("JER missing drop "+item);
            if(drops.stream().anyMatch(s->s.is(RelicContent.TROPHY.get().asItem())))throw new IllegalStateException("JER incorrectly lists first-clear trophy");
            LogUtils.getLogger().info("RELICWARD_JER_LOOT_OK: core, bronze fragments, bronze bricks recognized");
        }catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
    }

}
