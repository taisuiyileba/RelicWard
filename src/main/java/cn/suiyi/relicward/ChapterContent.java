package cn.suiyi.relicward;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.registries.RegistryObject;
import cn.suiyi.relicward.item.BellArmorMaterial;

/** Equipment and furnishings from the single Bell Warden chapter. */
public final class ChapterContent {
    public static final RegistryObject<Item> GEAR=RelicWard.ITEMS.register("puppet_gear",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> PLATE=RelicWard.ITEMS.register("resonant_plate",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> BINDING_SEAL=RelicWard.ITEMS.register("binding_seal",()->new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> REPAIR_PASTE=RelicWard.ITEMS.register("repair_paste",()->new Item(new Item.Properties()));
    public static final RegistryObject<Item> SHIELD=RelicWard.ITEMS.register("bell_shield",()->new ShieldItem(new Item.Properties().durability(768)){
        @Override public boolean isValidRepairItem(ItemStack a,ItemStack b){return b.is(PLATE.get());}
    });
    public static final RegistryObject<Item> HELMET=armor("bell_helmet",ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> CHESTPLATE=armor("bell_chestplate",ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> LEGGINGS=armor("bell_leggings",ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> BOOTS=armor("bell_boots",ArmorItem.Type.BOOTS);
    public static final RegistryObject<Block> ANVIL=RelicContent.BLOCKS.register("bell_anvil",cn.suiyi.relicward.block.BellAnvilBlock::new);
    public static final RegistryObject<Block> LANTERN=RelicContent.BLOCKS.register("chime_lantern",()->new LanternBlock(RelicContent.bronze().noOcclusion().lightLevel(s->14)));
    public static final RegistryObject<Block> TILE=RelicContent.BLOCKS.register("chiseled_bronze",()->new Block(RelicContent.bronze()));
    private static RegistryObject<Item> armor(String name,ArmorItem.Type type){return RelicWard.ITEMS.register(name,()->new cn.suiyi.relicward.item.BellArmorItem(BellArmorMaterial.INSTANCE,type,new Item.Properties()));}
    public static void register(){for(var b:java.util.List.of(ANVIL,LANTERN,TILE))RelicWard.ITEMS.register(b.getId().getPath(),()->new BlockItem(b.get(),new Item.Properties()));}
}
