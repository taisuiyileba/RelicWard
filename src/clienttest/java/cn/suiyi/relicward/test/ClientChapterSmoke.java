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
public final class ClientChapterSmoke {
    private static int stage,ticks;private static UUID bossId,puppetId;private static volatile boolean ready;private static volatile Throwable failure;private static volatile String capture;
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){
        if(e.phase!=TickEvent.Phase.END||capture==null)return;var mc=Minecraft.getInstance();String n=capture;capture=null;
        try(var im=Screenshot.takeScreenshot(mc.getMainRenderTarget())){im.writeToFile(mc.gameDirectory.toPath().resolve(n+".png"));}catch(Exception ex){failure=ex;}
    }
    private static void server(java.util.function.BiConsumer<net.minecraft.server.level.ServerLevel,net.minecraft.server.level.ServerPlayer> action){
        var mc=Minecraft.getInstance();var id=mc.player.getUUID();var s=mc.getSingleplayerServer();s.execute(()->{try{action.accept(s.overworld(),s.getPlayerList().getPlayer(id));}catch(Throwable t){failure=t;}});
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("relicward.chapterSmoke"))return;
        var mc=Minecraft.getInstance();if(failure!=null)throw new RuntimeException(failure);
        if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            stage=1;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(8);mc.options.fov().set(60);mc.options.hideGui=false;mc.getTutorial().setStep(TutorialSteps.NONE);
            for(var item:RelicWard.ITEMS.getEntries()){
                var model=mc.getItemRenderer().getModel(new ItemStack(item.get()),null,null,0);if(model==mc.getModelManager().getMissingModel())throw new IllegalStateException("Missing item "+item.getId());
                for(var quad:model.getQuads(null,null,net.minecraft.util.RandomSource.create(1)))if(quad.getSprite().contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()))throw new IllegalStateException("Missing texture "+item.getId());
            }
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            mc.createWorldOpenFlows().createFreshLevel("chapter080-"+System.currentTimeMillis(),new LevelSettings("Relic Ward chapter",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(8172,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }else if(stage==1&&mc.level!=null&&mc.player!=null&&mc.getSingleplayerServer()!=null){
            stage=2;server((l,p)->{
                l.setDayTime(5000);var origin=new BlockPos(-32,-62,-24);for(int x=-2;x<=2;x++)for(int z=-2;z<=3;z++)l.getChunk(x,z);
                // Canary above the new clearance envelope must survive actual placement.
                l.setBlock(new BlockPos(-30,-30,52),net.minecraft.world.level.block.Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT,true),2);
                l.getStructureManager().getOrCreate(new ResourceLocation("relicward","resonant_court")).placeInWorld(l,origin,origin,new StructurePlaceSettings().addProcessor(JigsawReplacementProcessor.INSTANCE),l.random,2);
                if(!l.getBlockState(new BlockPos(-30,-30,52)).is(net.minecraft.world.level.block.Blocks.OAK_LEAVES))throw new IllegalStateException("Exterior clearing regression");
                var altar=(CourtAltarEntity)l.getBlockEntity(new BlockPos(0,-57,17));var boss=altar.ensureBoss();bossId=boss.getUUID();
                p.teleportTo(l,.5,-57,4,180,-24);p.setGameMode(GameType.SURVIVAL);p.setInvulnerable(true);p.experienceLevel=30;
                boss.startEncounter();boss.setAction(cn.suiyi.relicward.combat.WardenAction.IDLE);boss.setNoAi(true);
                var puppet=RelicWard.COURT_PUPPET.get().create(l);puppet.moveTo(0,-57,46,0,0);puppet.setNoAi(true);l.addFreshEntity(puppet);puppetId=puppet.getUUID();
                l.setBlock(new BlockPos(0,-57,50),ChapterContent.ANVIL.get().defaultBlockState(),3);
                l.setBlock(new BlockPos(2,-57,50),ChapterContent.LANTERN.get().defaultBlockState(),3);
                p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(ChapterContent.HELMET.get()));p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(ChapterContent.CHESTPLATE.get()));p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,new ItemStack(ChapterContent.LEGGINGS.get()));p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(ChapterContent.BOOTS.get()));
                int i=0;for(var it:java.util.List.of(ChapterContent.SHIELD.get(),ChapterContent.BINDING_SEAL.get(),ChapterContent.REPAIR_PASTE.get(),ChapterContent.GEAR.get(),ChapterContent.PLATE.get(),ChapterContent.ANVIL.get().asItem(),ChapterContent.LANTERN.get().asItem(),ChapterContent.TILE.get().asItem()))p.getInventory().setItem(i++,new ItemStack(it));ready=true;
            });
        }else if(stage==2&&ready&&mc.screen==null){
            if(++ticks<110)return;capture="chapter080_jade";
            var bosses=mc.level.getEntitiesOfClass(BellWarden.class,mc.player.getBoundingBox().inflate(12));if(bosses.isEmpty()||!cn.suiyi.relicward.compat.JadePlugin.armorText(bosses.get(0)).getString().contains("3"))throw new IllegalStateException("Clock armor did not sync");
            stage=3;ticks=0;
        }else if(stage==3&&++ticks==10){mc.setScreen(new InventoryScreen(mc.player));stage=4;ticks=0;
        }else if(stage==4&&++ticks==30){capture="chapter080_equipment";stage=5;ticks=0;
        }else if(stage==5&&++ticks==10){mc.setScreen(null);server((l,p)->p.teleportTo(l,3,-57,53,145,15));mc.options.keyUse.setDown(true);mc.gameMode.useItem(mc.player,InteractionHand.MAIN_HAND);stage=6;ticks=0;
        }else if(stage==6&&++ticks==25){if(!mc.player.isBlocking())throw new IllegalStateException("Shield does not block");capture="chapter080_shield";stage=7;ticks=0;
        }else if(stage==7&&++ticks==10){mc.options.keyUse.setDown(false);mc.gameMode.releaseUsingItem(mc.player);server((l,p)->{p.teleportTo(l,2,-57,49,146,10);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ChapterContent.BINDING_SEAL.get(),2));});stage=8;ticks=0;
        }else if(stage==8&&++ticks==30){
            var puppets=mc.level.getEntitiesOfClass(cn.suiyi.relicward.entity.CourtPuppet.class,mc.player.getBoundingBox().inflate(8));var puppet=puppets.stream().filter(p->p.getUUID().equals(puppetId)).findFirst().orElseThrow();mc.gameMode.interact(mc.player,puppet,InteractionHand.MAIN_HAND);stage=9;ticks=0;
        }else if(stage==9&&++ticks==25){
            var puppet=mc.level.getEntitiesOfClass(cn.suiyi.relicward.entity.CourtPuppet.class,mc.player.getBoundingBox().inflate(8)).stream().filter(p->p.getUUID().equals(puppetId)).findFirst().orElseThrow();
            if(!puppet.isOwnedBy(mc.player)||mc.player.getMainHandItem().getCount()!=1)throw new IllegalStateException("Actual binding interaction failed");capture="chapter080_companion";stage=10;ticks=0;
        }else if(stage==10&&++ticks==10){server((l,p)->{p.teleportTo(l,.5,-57,52.5,180,25);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);});stage=11;ticks=0;
        }else if(stage==11&&++ticks==25){var pos=new BlockPos(0,-57,50);mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));stage=12;ticks=0;
        }else if(stage==12&&++ticks==20){if(!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AnvilScreen))throw new IllegalStateException("Anvil UI failed to open");server((l,p)->{var stack=new ItemStack(ChapterContent.SHIELD.get());stack.setDamageValue(100);p.containerMenu.getSlot(0).set(stack);p.containerMenu.getSlot(1).set(new ItemStack(ChapterContent.PLATE.get()));p.containerMenu.broadcastChanges();});stage=13;ticks=0;
        }else if(stage==13&&++ticks==25){capture="chapter080_anvil";LogUtils.getLogger().info("RELICWARD_CHAPTER_SMOKE_OK: assets, armor, shield use, Jade armor sync, actual companion binding, anvil screen, selective air");stage=14;ticks=0;
        }else if(stage==14&&++ticks==10){mc.player.closeContainer();server((l,p)->p.teleportTo(l,.5,-60,61,180,-12));stage=15;ticks=0;
        }else if(stage==15&&++ticks==30){capture="chapter080_entrance";stage=16;ticks=0;
        }else if(stage==16&&++ticks>20){mc.options.keyUse.setDown(false);mc.stop();stage=17;}
    }
}
