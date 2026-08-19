package com.fusionflux.originsumbrellas;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Origins: Umbrellas — Forge 1.20.1 port.
 *
 * <p>Upstream (Fabric) is Fusion-Flux/Origins-Umbrellas, LGPL-3.0. What the mod does is
 * unchanged: holding an umbrella makes you count as neither "exposed to the sun/sky" nor
 * "in the rain", the umbrella soaks while you stand in rain, and it dries when you do not.
 *
 * <p>The Fabric build reached those three states through Fabric-only entry points. The
 * Forge port keeps the behaviour and swaps the plumbing:
 * <ul>
 *   <li>item registration → {@link DeferredRegister}</li>
 *   <li>{@code ClientModInitializer} colour provider → {@code RegisterColorHandlersEvent.Item}</li>
 *   <li>{@code PlayerEntityMixin#tick} → {@link TickEvent.PlayerTickEvent}</li>
 *   <li>the two Apoli conditions → one mixin on Apoli's {@code SimpleEntityCondition}</li>
 * </ul>
 */
@Mod(OriginsUmbrellas.MOD_ID)
public class OriginsUmbrellas {

    public static final String MOD_ID = "originsumbrellas";

    /** Ticks of rain the umbrella can soak up before it stops keeping anything off you. */
    public static final int SOAK_LIMIT = 1200;

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);

    public static final RegistryObject<Item> UMBRELLA = ITEMS.register(
            "umbrella", () -> new UmbrellaItem(new Item.Properties().stacksTo(1).durability(SOAK_LIMIT)));

    public OriginsUmbrellas() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        modBus.addListener(OriginsUmbrellas::addToCreativeTab);
        MinecraftForge.EVENT_BUS.register(OriginsUmbrellas.class);
    }

    /**
     * An umbrella only shelters you while it still has dry left in it. A fully soaked one
     * (damage at the limit minus one) is dead weight until it dries out.
     */
    public static boolean isHoldingUmbrella(Entity entity) {
        for (ItemStack stack : entity.getHandSlots()) {
            if (stack.is(UMBRELLA.get()) && stack.getDamageValue() < stack.getMaxDamage() - 1) {
                return true;
            }
        }
        return false;
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(UMBRELLA.get());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide) {
            // The damage tag rides along with the slot sync, so only the server needs to count.
            return;
        }

        boolean rainedOn = isActuallyRainedOn(player);

        if (rainedOn && player.tickCount % 10 == 0) {
            for (ItemStack stack : player.getHandSlots()) {
                if (stack.is(UMBRELLA.get()) && stack.getDamageValue() < stack.getMaxDamage() - 1) {
                    // Set the value directly. ItemStack#hurt would play the break animation
                    // once per soaked tick, and the umbrella is never meant to break.
                    stack.setDamageValue(stack.getDamageValue() + 1);
                }
            }
        } else if (!rainedOn && player.tickCount % 20 == 0) {
            BlockPos pos = player.blockPosition();
            boolean warm = !player.level().getBiome(pos).value().coldEnoughToSnow(pos);

            dry(player.getItemBySlot(EquipmentSlot.OFFHAND), warm, true);
            for (int i = 0; i < 36; i++) {
                dry(player.getInventory().getItem(i), warm, player.getInventory().selected == i);
            }
        }
    }

    /** Warm air and open air each add a step of drying on top of the base one. */
    private static void dry(ItemStack stack, boolean warm, boolean inTheOpen) {
        if (!stack.is(UMBRELLA.get()) || !stack.isDamaged()) {
            return;
        }
        int steps = 1 + (warm ? 1 : 0) + (inTheOpen ? 1 : 0);
        stack.setDamageValue(stack.getDamageValue() - steps);
    }

    /**
     * Vanilla's own rain test, re-implemented.
     *
     * <p>⚠ Deliberately NOT {@code player.isInRain()}: this mod's {@code EntityMixin} makes that
     * method answer "no" while an umbrella is up, so calling it here would mean a raised umbrella
     * never soaks and never dries down. Mirrors {@code Entity#isInRain} in 1.20.1.
     */
    private static boolean isActuallyRainedOn(Player player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        return level.isRainingAt(pos)
                || level.isRainingAt(BlockPos.containing(pos.getX(), player.getBoundingBox().maxY, pos.getZ()));
    }
}
