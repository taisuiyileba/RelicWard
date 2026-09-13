package cn.suiyi.relicward.item;

import cn.suiyi.relicward.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.*;

public final class ResonantBellItem extends Item {
    public static final int COOLDOWN=160;
    public ResonantBellItem(){super(new Properties().durability(384).rarity(Rarity.RARE));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!player.onGround()||player.getCooldowns().isOnCooldown(this)||(!level.isClientSide&&RelicEquipment.cooldown(player,"Wave")>0))return InteractionResultHolder.fail(stack);
        if(!level.isClientSide){
            var wave=RelicWard.RESONANT_WAVE.get().create(level);if(wave==null)return InteractionResultHolder.fail(stack);
            wave.initialize(player);if(!level.addFreshEntity(wave))return InteractionResultHolder.fail(stack);
            RelicEquipment.setCooldown(player,"Wave",COOLDOWN);player.getCooldowns().addCooldown(this,COOLDOWN);
            stack.hurtAndBreak(2,player,p->p.broadcastBreakEvent(hand));
            level.playSound(null,player.blockPosition(),SoundEvents.BELL_RESONATE,SoundSource.PLAYERS,1.2F,.8F);
        }
        player.swing(hand);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack ingredient){return ingredient.is(ChapterContent.PLATE.get());}
}
