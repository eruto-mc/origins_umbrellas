package com.fusionflux.originsumbrellas.mixin;

import com.fusionflux.originsumbrellas.OriginsUmbrellas;
import io.github.edwinmindcraft.apoli.common.condition.entity.SimpleEntityCondition;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes {@code apoli:exposed_to_sun} and {@code apoli:exposed_to_sky} answer "no" while an
 * umbrella is up — the whole point of the mod, since that is the condition sun-burning origins
 * are built on.
 *
 * <p>⚠ Where this differs from upstream. The Fabric build had to attack this from two angles:
 * a mixin on {@code ExposedToSunCondition} plus a {@code @WrapOperation} that re-wrapped every
 * condition factory at registration time just to reach {@code exposed_to_sky}. EdwinMindcraft's
 * Forge port routes both conditions through two public statics on one class
 * ({@code SimpleEntityCondition.isExposedToSun} / {@code isExposedToSky}, confirmed by reading
 * the {@code BootstrapMethods} table of {@code ApoliEntityConditions} in
 * apoli-forge-1.20.1-2.9.0.8.jar), so both land in this one mixin.
 *
 * <p>{@code remap = false} throughout: these are Apoli's own methods, not Minecraft's, so there
 * is no SRG name to map to.
 */
@Mixin(value = SimpleEntityCondition.class, remap = false)
public class SimpleEntityConditionMixin {

    @Inject(method = "isExposedToSun", at = @At("RETURN"), cancellable = true, remap = false)
    private static void originsumbrellas$sunBlockedByUmbrella(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && OriginsUmbrellas.isHoldingUmbrella(entity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isExposedToSky", at = @At("RETURN"), cancellable = true, remap = false)
    private static void originsumbrellas$skyBlockedByUmbrella(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && OriginsUmbrellas.isHoldingUmbrella(entity)) {
            cir.setReturnValue(false);
        }
    }
}
