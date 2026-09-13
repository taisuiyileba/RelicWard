package cn.suiyi.relicward.test;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.client.BellWardenModel;
import cn.suiyi.relicward.entity.BellWarden;
import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Development-only integration test. This source set is excluded from release jars. */
@Mod.EventBusSubscriber(modid = RelicWard.ID, value = Dist.CLIENT)
public final class ClientSmoke {
    private static int stage, ticks;
    private static volatile boolean sceneReady;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Boolean.getBoolean("relicward.modelSmoke")) return;
        Minecraft mc = Minecraft.getInstance();
        if (stage == 0 && mc.screen instanceof TitleScreen && mc.getOverlay() == null) {
            stage = 1;
            mc.options.pauseOnLostFocus = false;
            mc.options.renderDistance().set(6);
            mc.options.simulationDistance().set(5);
            mc.options.fov().set(55);
            mc.options.guiScale().set(2);
            var model = new BellWardenModel(mc.getEntityModels().bakeLayer(BellWardenModel.LAYER));
            if (model.root().getAllParts().count() != 18) throw new IllegalStateException("Missing model bones");
            var bakedEgg = mc.getItemRenderer().getModel(new ItemStack(RelicWard.BELL_WARDEN_SPAWN_EGG.get()), null, null, 0);
            if (bakedEgg == mc.getModelManager().getMissingModel()) throw new IllegalStateException("Missing spawn egg model");
            LogUtils.getLogger().info("RELICWARD_SMOKE: model and egg assets baked successfully");
            GameRules rules = new GameRules();
            rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
            rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
            var settings = new LevelSettings("Relic Ward model test", GameType.CREATIVE, false,
                    Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("model-smoke-" + System.currentTimeMillis(), settings,
                    new WorldOptions(8172L, false, false),
                    registry -> registry.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
        } else if (stage == 1 && mc.level != null && mc.player != null && mc.getSingleplayerServer() != null) {
            stage = 2;
            var server = mc.getSingleplayerServer();
            var playerId = mc.player.getUUID();
            server.execute(() -> {
                var level = server.overworld();
                level.setDayTime(5000);
                for (int x = -9; x <= 9; x++) for (int z = -9; z <= 9; z++)
                    level.setBlockAndUpdate(new BlockPos(x, -60, z), Blocks.STONE_BRICKS.defaultBlockState());
                BellWarden warden = RelicWard.BELL_WARDEN.get().create(level);
                if (warden == null) throw new IllegalStateException("Cannot create warden");
                warden.moveTo(.5, -59, .5, 0, 0);
                warden.setNoAi(true);
                level.addFreshEntity(warden);
                var player = server.getPlayerList().getPlayer(playerId);
                player.teleportTo(level, 9, -56, 14, 148.0F, 6.0F);
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(RelicWard.BELL_WARDEN_SPAWN_EGG.get()));
                sceneReady = true;
            });
        } else if (stage == 2 && sceneReady && mc.getOverlay() == null && mc.screen == null) {
            if (++ticks < 140) return;
            var found = mc.level.getEntitiesOfClass(BellWarden.class, mc.player.getBoundingBox().inflate(32));
            if (found.size() != 1) throw new IllegalStateException("Spawned entity not synchronized to client");
            if (mc.getEntityRenderDispatcher().getRenderer(found.get(0)) == null)
                throw new IllegalStateException("Renderer missing");
            mc.options.hideGui = true;
            stage = 3;
            ticks = 0;
        } else if (stage == 3 && ++ticks == 10) {
            try (var shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                Path file = mc.gameDirectory.toPath().resolve("bell_warden_ingame.png");
                shot.writeToFile(file);
                LogUtils.getLogger().info("RELICWARD_SMOKE_OK: in-world entity rendered, screenshot {}", file);
            } catch (Exception error) {
                throw new RuntimeException(error);
            }
            stage = 4;
            ticks = 0;
        } else if (stage == 4 && ++ticks > 20) {
            mc.stop();
            stage = 5;
        }
    }
}
