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
public final class ClientArmorSmoke {
    private static int stage,ticks;private static volatile boolean ready;private static volatile Throwable failure;private static String capture;
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent e){if(e.phase!=TickEvent.Phase.END||capture==null)return;var mc=Minecraft.getInstance();var n=capture;capture=null;try(var im=Screenshot.takeScreenshot(mc.getMainRenderTarget())){im.writeToFile(mc.gameDirectory.toPath().resolve(n+".png"));}catch(Exception x){failure=x;}}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!Boolean.getBoolean("relicward.armorSmoke"))return;var mc=Minecraft.getInstance();if(failure!=null)throw new RuntimeException(failure);
        if(stage==0&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){stage=1;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(6);mc.options.fov().set(55);mc.getTutorial().setStep(TutorialSteps.NONE);
            var rules=new GameRules();rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
            mc.createWorldOpenFlows().createFreshLevel("armor081-"+System.currentTimeMillis(),new LevelSettings("Armor review",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(8172,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        }else if(stage==1&&mc.level!=null&&mc.player!=null&&mc.getSingleplayerServer()!=null){stage=2;var id=mc.player.getUUID();var server=mc.getSingleplayerServer();server.execute(()->{try{
            var l=server.overworld();l.setDayTime(4500);var p=server.getPlayerList().getPlayer(id);p.teleportTo(l,1,-59.3,7,180,7);p.getAbilities().flying=true;p.onUpdateAbilities();
            for(int i=0;i<3;i++){
                var a=net.minecraft.world.entity.EntityType.ARMOR_STAND.create(l);a.moveTo(-2+i*3,-60,0,i==0?0:i==1?180:90,0);a.setYBodyRot(a.getYRot());a.setYHeadRot(a.getYRot());a.setShowArms(true);a.setNoGravity(true);
                a.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new ItemStack(ChapterContent.HELMET.get()));a.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,new ItemStack(ChapterContent.CHESTPLATE.get()));a.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,new ItemStack(ChapterContent.LEGGINGS.get()));a.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,new ItemStack(ChapterContent.BOOTS.get()));l.addFreshEntity(a);
            }
            int slot=0;for(var item:java.util.List.of(ChapterContent.HELMET.get(),ChapterContent.CHESTPLATE.get(),ChapterContent.LEGGINGS.get(),ChapterContent.BOOTS.get(),ChapterContent.GEAR.get(),ChapterContent.BINDING_SEAL.get(),ChapterContent.PLATE.get(),ChapterContent.REPAIR_PASTE.get(),ChapterContent.LANTERN.get().asItem()))p.getInventory().setItem(slot++,new ItemStack(item));ready=true;
        }catch(Throwable t){failure=t;}});
        }else if(stage==2&&ready&&mc.screen==null){if(++ticks<100)return;mc.options.hideGui=true;capture="armor081_front_back_side";stage=3;ticks=0;
        }else if(stage==3&&++ticks==15){mc.options.hideGui=false;mc.setScreen(new InventoryScreen(mc.player));stage=4;ticks=0;
        }else if(stage==4&&++ticks==25){capture="armor081_icons";stage=5;ticks=0;LogUtils.getLogger().info("RELICWARD_ARMOR_SMOKE_OK: front/back/side armor models and material icons loaded");
        }else if(stage==5&&++ticks==20){mc.stop();stage=6;}
    }
}
