package ru.nametags;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents; // ПРАВИЛЬНЫЙ ПУТЬ
import net.minecraft.text.Text;

public class NametagsMod implements ClientModInitializer {
    public static boolean enabled = true;

    @Override
    public void onInitializeClient() {
        // В 1.21.11 используем LevelRenderEvents.END_MAIN
        LevelRenderEvents.END_MAIN.register(NametagsRenderer::render);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("nametags")
                .executes(context -> {
                    enabled = !enabled;
                    context.getSource().sendFeedback(Text.literal("Nametags: " + (enabled ? "§aON" : "§cOFF")));
                    return 1;
                })
            );
        });
    }
}
