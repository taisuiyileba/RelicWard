package cn.suiyi.relicward.combat;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.registries.RegistryObject;

/** Repeated waves refresh one debuff instead of multiplying permanent modifiers. */
public final class ArmorFracture {
    public static final int DURATION=160;
    public static final RegistryObject<MobEffect> EFFECT=SuperGravity.EFFECTS.register("armor_fracture",FractureEffect::new);
    private static final class FractureEffect extends MobEffect {
        FractureEffect(){
            super(MobEffectCategory.HARMFUL,0xCB884D);
            addAttributeModifier(Attributes.ARMOR,"38644866-83f8-4746-ae5d-83d6b0f326c7",-.30,AttributeModifier.Operation.MULTIPLY_TOTAL);
            addAttributeModifier(Attributes.ARMOR_TOUGHNESS,"c5af5e17-6c9a-4f49-8202-7f2b70b90df1",-.30,AttributeModifier.Operation.MULTIPLY_TOTAL);
        }
    }
    public static void register(){} // Force registration before the shared effect registry is attached.
    public static void apply(LivingEntity target){apply(target,DURATION);}
    public static void apply(LivingEntity target,int ticks){
        if(!target.level().isClientSide&&target.isAlive())target.addEffect(new MobEffectInstance(EFFECT.get(),ticks,0));
    }
}
