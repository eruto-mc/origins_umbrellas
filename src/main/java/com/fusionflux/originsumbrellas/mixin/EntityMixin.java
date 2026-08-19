package com.fusionflux.originsumbrellas.mixin;

import com.fusionflux.originsumbrellas.OriginsUmbrellas;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An umbrella keeps the rain off, so while one is up the entity does not count as being rained on.
 *
 * <p>This covers Apoli's {@code apoli:in_rain} for free: that condition reaches the same method
 * through Apoli's own {@code EntityAccessor}, whose {@code @Invoker} names {@code isInRain}
 * (read out of apoli-forge-1.20.1-2.9.0.8.jar).
 *
 * <p>⚠ It also covers vanilla — a raised umbrella now puts out a burning entity's fire the same
 * way standing indoors would not, keeps Endermen from taking rain damage, and so on. That is
 * upstream's behaviour, kept deliberately.
 */
@Mixin(Entity.class)
public class EntityMixin {

    @Inject(method = "isInRain", at = @At("RETURN"), cancellable = true)
    private void originsumbrellas$rainBlockedByUmbrella(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && OriginsUmbrellas.isHoldingUmbrella((Entity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
