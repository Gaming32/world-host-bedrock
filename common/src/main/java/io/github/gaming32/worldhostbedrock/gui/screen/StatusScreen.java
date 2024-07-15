package io.github.gaming32.worldhostbedrock.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class StatusScreen extends Screen {
    private Component message = Component.empty();

    public StatusScreen(Component title) {
        super(title);
    }

    public Component getMessage() {
        return message;
    }

    public void setMessage(Component message) {
        this.message = message;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, titleTop(), 0xffffff);
        graphics.drawCenteredString(font, message, width / 2, messageTop(), 0xffffff);
    }

    private int titleTop() {
        return Mth.clamp((height - 9) / 2 - 29, 10, 80);
    }

    private int messageTop() {
        return titleTop() + 20;
    }
}
