package me.chimkenu.bopbipEssentials.mixin;

import me.chimkenu.bopbipEssentials.client.BopbipScreen;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(
            method = "initWidgetsNormal",
            at = @At(value = "HEAD")
    )
    private void replace(int y, int spacingY, CallbackInfo ci) {
        int x = this.width / 2 - 100;
        int width = 200;
        int height = 20;

        this.addDrawableChild(ButtonWidget.builder(
                        Text.of("bopbip"), button -> {
                            assert(client != null);
                            client.setScreen(new MessageScreen(Text.of("Fetching bopbip status...")));
                            new BopbipScreen(client);
                        })
                .dimensions(x, y - spacingY, width, height)
                .build()
        );
    }
}
