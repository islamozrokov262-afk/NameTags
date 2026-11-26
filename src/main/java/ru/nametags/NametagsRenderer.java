package ru.nametags;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext; // НОВЫЙ ПУТЬ
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

import java.util.Map;

public class NametagsRenderer {
    public static void render(LevelRenderContext context) {
        if (!NametagsMod.enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        // В LevelRenderContext метод называется poseStack()
        MatrixStack matrices = context.poseStack();
        if (matrices == null) return;

        // Камера теперь берется из уровня рендера
        Vec3d cameraPos = new Vec3d(
                context.levelState().cameraRenderState.pos.x,
                context.levelState().cameraRenderState.pos.y,
                context.levelState().cameraRenderState.pos.z
        );

        TextRenderer textRenderer = mc.textRenderer;
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        VertexConsumerProvider vertexConsumers = context.bufferSource();
        if (vertexConsumers == null) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;

            MutableText enchantText = getEnchantText(player);
            if (enchantText == null) continue;

            Vec3d pos = player.getLerpedPos(tickDelta).add(0, player.getHeight() + 0.5, 0);
            
            Vector4f screenPos = new Vector4f(
                    (float) (pos.x - cameraPos.x),
                    (float) (pos.y - cameraPos.y),
                    (float) (pos.z - cameraPos.z),
                    1.0f
            );
            
            // Применяем матрицу (теперь она лежит в poseStack)
            screenPos.mul(matrices.peek().getPositionMatrix());
            
            if (screenPos.w > 0.0f) {
                float x = screenPos.x / screenPos.w;
                float y = screenPos.y / screenPos.w;
                float z = screenPos.z / screenPos.w;

                if (x >= -1 && x <= 1 && y >= -1 && y <= 1 && z >= 0 && z <= 1) {
                    int screenX = (int) ((x + 1.0f) / 2.0f * mc.getWindow().getScaledWidth());
                    int screenY = (int) ((1.0f - y) / 2.0f * mc.getWindow().getScaledHeight());

                    matrices.push();
                    matrices.translate(screenX, screenY, 0); 
                    matrices.scale(-0.025f, -0.025f, 0.025f); 
                    
                    int width = textRenderer.getWidth(enchantText);
                    
                    // Безопасный вызов RenderLayer (в 1.21.11 он остался, но лучше проверить)
                    try {
                        vertexConsumers.getBuffer(RenderLayer.getTextBackgroundSeeThrough())
                                .fill(-width / 2 - 1, -1, width / 2 + 1, 9, 0x55000000);
                    } catch (Throwable ignored) {
                        // Если метод переименовали, просто пропускаем отрисовку фона
                    }

                    textRenderer.draw(
                            enchantText, 
                            -width / 2.0f, 
                            0, 
                            0xFFFFFF, 
                            true, 
                            matrices.peek().getPositionMatrix(), 
                            vertexConsumers, 
                            TextRenderer.TextLayerType.SEE_THROUGH, 
                            0, 
                            15728880
                    );
                    
                    matrices.pop();
                }
            }
        }
    }

    private static MutableText getEnchantText(PlayerEntity player) {
        MutableText text = Text.literal("");

        addEnchants(text, player.getMainHandStack());
        
        // В 1.21.11 getArmorItems() может быть недоступен, используем getEquippedStack
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            addEnchants(text, player.getEquippedStack(slot));
        }

        return text.getString().isEmpty() ? null : text;
    }

    private static void addEnchants(MutableText text, ItemStack stack) {
        if (stack.isEmpty() || !stack.hasEnchantments()) return;

        var enchantments = net.minecraft.enchantment.EnchantmentHelper.getEnchantments(stack);
        for (Map.Entry<RegistryEntry<Enchantment>, Integer> entry : enchantments.getEnchantmentEntries()) {
            String name = entry.getKey().value().description().getString();
            if (name.length() > 4) name = name.substring(0, 4); 
            
            int level = entry.getValue();
            
            Formatting color = Formatting.WHITE;
            if (level >= 5) color = Formatting.RED;
            else if (level == 4) color = Formatting.LIGHT_PURPLE;
            else if (level == 3) color = Formatting.AQUA;

            text.append(Text.literal(name + level + " ").formatted(color));
        }
    }
}
