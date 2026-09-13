package cn.suiyi.relicward.test;

import cn.suiyi.relicward.RelicWard;
import cn.suiyi.relicward.entity.BellWarden;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(RelicWard.ID)
@PrefixGameTestTemplate(false)
public final class CreatureTests {
    @GameTest(template = "creature_room", timeoutTicks = 80)
    public static void spawnEggCreatesNamedPersistentCreature(GameTestHelper helper) {
        var initialBounds=new AABB(helper.absolutePos(BlockPos.ZERO),helper.absolutePos(new BlockPos(16,12,16)));
        // Vanilla GameTest cleanup calls kill(); this boss intentionally has an 80-tick death animation.
        helper.getLevel().getEntitiesOfClass(BellWarden.class,initialBounds).forEach(e->e.discard());
        BlockPos floor = new BlockPos(6, 1, 6);
        helper.setBlock(floor, Blocks.STONE);
        var player = helper.makeMockSurvivalPlayer();
        var egg = new ItemStack(RelicWard.BELL_WARDEN_SPAWN_EGG.get());
        egg.setHoverName(Component.literal("Test Warden"));
        player.setItemInHand(InteractionHand.MAIN_HAND, egg);
        var absolute = helper.absolutePos(floor);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        var result = egg.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(result.consumesAction(), "Spawn egg must consume the interaction");
        helper.assertTrue(egg.isEmpty(), "Survival spawn must consume exactly one egg");
        helper.assertTrue(ForgeSpawnEggItem.fromEntityType(RelicWard.BELL_WARDEN.get()) == RelicWard.BELL_WARDEN_SPAWN_EGG.get(), "Egg lookup must be registered");
        helper.runAfterDelay(2, () -> {
            var bounds = new AABB(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(16, 12, 16)));
            var entities = helper.getLevel().getEntitiesOfClass(BellWarden.class, bounds,e->e.getName().getString().equals("Test Warden"));
            helper.assertTrue(entities.size() == 1, "Egg must create exactly one named warden, found "+entities.size());
            var warden = entities.get(0);
            helper.assertTrue(warden.getMaxHealth() == 480.0F, "Attributes must be registered");
            helper.assertTrue(warden.getHealth() == 480.0F, "Must spawn at full health");
            helper.assertTrue(Math.abs(warden.getBbHeight() - 5.2F) < 0.01F, "Model-scale collision height");
            helper.assertTrue(warden.isPersistenceRequired(), "Preview entity should not despawn");
            helper.assertTrue(warden.getName().getString().equals("Test Warden"), "Egg custom name must transfer");
            helper.succeed();
        });
    }

    @GameTest(template = "creature_room", timeoutTicks = 80)
    public static void creatureSurvivesSaveLoad(GameTestHelper helper) {
        var original = helper.spawn(RelicWard.BELL_WARDEN.get(), 6, 2, 6);
        original.setNoAi(true);
        original.setHealth(123.0F);
        original.setCustomName(Component.literal("Saved Warden"));
        CompoundTag saved = new CompoundTag();
        helper.assertTrue(original.save(saved), "Entity must be serializable");
        original.discard();
        var restored = EntityType.loadEntityRecursive(saved, helper.getLevel(), e -> e);
        helper.assertTrue(restored instanceof BellWarden, "Saved entity ID must restore the correct type");
        var warden = (BellWarden) restored;
        helper.assertTrue(warden.getHealth() == 123.0F, "Health must survive save/load");
        helper.assertTrue(warden.isNoAi() && warden.isPersistenceRequired(), "Preview flags must persist");
        helper.assertTrue(warden.getName().getString().equals("Saved Warden"), "Name must persist");
        helper.getLevel().addFreshEntity(warden);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(warden.isAlive(), "Loaded entity must tick without a crash");
            helper.succeed();
        });
    }
}
