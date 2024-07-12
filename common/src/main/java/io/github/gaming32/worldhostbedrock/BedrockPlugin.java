package io.github.gaming32.worldhostbedrock;

import io.github.gaming32.worldhost.plugin.InfoTextsCategory;
import io.github.gaming32.worldhost.plugin.WorldHostPlugin;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

@WorldHostPlugin.Entrypoint
public class BedrockPlugin implements WorldHostPlugin {
    private static final List<Component> BEDROCK_FRIENDS_TEXT = List.of(Component.translatable(
        "world_host_bedrock.friends.bedrock_notice",
        Component.translatable("world_host_bedrock.friends.bedrock_notice.link").withStyle(s -> s
            .applyFormat(ChatFormatting.UNDERLINE)
            .withColor(ChatFormatting.BLUE)
            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://account.xbox.com/Profile"))
        )
    ));

    @Override
    public List<Component> getInfoTexts(InfoTextsCategory category) {
        return switch (category) {
            case FRIENDS_SCREEN -> BEDROCK_FRIENDS_TEXT;
            default -> List.of();
        };
    }
}
