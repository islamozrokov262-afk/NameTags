package ru.nametags;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents; // Правильный путь для 1.21.11
import net.minecraft.text.Text;

public class NametagsMod implements ClientModInitializer {
    public static boolean enabled = true;

    @Override
    public void onInitializeClient() {
        // AFTER_ENTITIES — правильное событие, чтобы рисовать поверх сущностей
        WorldRenderEvents.AFTER_ENTITIES.register(NametagsRenderer::render);

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
