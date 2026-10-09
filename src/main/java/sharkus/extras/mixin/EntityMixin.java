package sharkus.extras.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sharkus.extras.Config;

import java.util.Map;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void sharkus$rename(CallbackInfoReturnable<Component> cir) {
        Config c = Config.I;
        if (c == null || !c.nametags) return;

        Entity self = (Entity) (Object) this;
        String plain = self.getName().getString();
        String replacement = null;

        if (Minecraft.getInstance().player == self) {
            if (c.ownNameOverride != null && !c.ownNameOverride.isBlank()) replacement = c.ownNameOverride;
        } else if (c.nameReplacements != null) {
            for (Map.Entry<String, String> e : c.nameReplacements.entrySet()) {
                if (e.getKey().equalsIgnoreCase(plain)) {
                    replacement = e.getValue();
                    break;
                }
            }
        }
        if (replacement != null) cir.setReturnValue(Component.literal(replacement));
    }
}
