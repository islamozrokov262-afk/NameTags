package ru.nametags;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.WorldRenderContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.Map;

public class NametagsRenderer {
    public static void render(WorldRenderContext context) {
        if (!NametagsMod.enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = context.matrixStack();
        if (matrices == null) return;

        Vec3d cameraPos = context.camera().getPos();
        TextRenderer textRenderer = mc.textRenderer;
        float tickDelta = context.tickCounter().getTickDelta(true);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;

            // Получаем текст (чары на мече и броне)
            MutableText enchantText = getEnchantText(player);
            if (enchantText == null) continue;

            // Позиция над головой
            Vec3d pos = player.getLerpedPos(tickDelta).add(0, player.getHeight() + 0.5, 0);
            
            // Переводим 3D в 2D экрана
            Vector4f screenPos = new Vector4f(
                    (float) (pos.x - cameraPos.x),
                    (float) (pos.y - cameraPos.y),
                    (float) (pos.z - cameraPos.z),
                    1.0f
            );
            
            // Применяем матрицу проекции (необходимо для 1.21+)
            screenPos.mul(matrices.peek().getPositionMatrix());
            
            if (screenPos.w > 0.0f) {
                float x = screenPos.x / screenPos.w;
                float y = screenPos.y / screenPos.w;
                float z = screenPos.z / screenPos.w;

                // Если игрок в поле зрения
                if (x >= -1 && x <= 1 && y >= -1 && y <= 1 && z >= 0 && z <= 1) {
                    int screenX = (int) ((x + 1.0f) / 2.0f * mc.getWindow().getScaledWidth());
                    int screenY = (int) ((1.0f - y) / 2.0f * mc.getWindow().getScaledHeight());

                    // Отрисовка текста
                    matrices.push();
                    // Перемещаем в 2D пространство экрана
                    matrices.translate(screenX, screenY, 0); 
                    matrices.scale(-0.025f, -0.025f, 0.025f); // Масштаб текста
                    
                    // Рисуем фон (черный полупрозрачный)
                    int width = textRenderer.getWidth(enchantText);
                    context.consumers().getBuffer(net.minecraft.client.render.RenderLayer.getTextBackgroundSeeThrough())
                            .fill(-width / 2 - 1, -1, width / 2 + 1, 9, 0x55000000);

                    // Рисуем сам текст
                    textRenderer.draw(enchantText, -width / 2.0f, 0, 0xFFFFFF, true, matrices.peek().getPositionMatrix(), context.consumers(), TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
                    
                    matrices.pop();
                }
            }
        }
    }

    private static MutableText getEnchantText(PlayerEntity player) {
        MutableText text = Text.literal("");

        // Меч/Инструмент в руке
        addEnchants(text, player.getMainHandStack());
        // Броня (Шлем, Нагрудник, Штаны, Ботинки)
        for (ItemStack armor : player.getArmorItems()) {
            addEnchants(text, armor);
        }

        return text.getString().isEmpty() ? null : text;
    }

    private static void addEnchants(MutableText text, ItemStack stack) {
        if (stack.isEmpty() || !stack.hasEnchantments()) return;

        var enchantments = net.minecraft.enchantment.EnchantmentHelper.getEnchantments(stack);
        for (Map.Entry<RegistryEntry<Enchantment>, Integer> entry : enchantments.getEnchantmentEntries()) {
            String name = entry.getKey().value().description().getString();
            // Сокращаем название (например, "Острота" -> "Остр")
            if (name.length() > 4) name = name.substring(0, 4); 
            
            int level = entry.getValue();
            
            // Выделяем цвета в зависимости от уровня (красный = 5, фиолетовый = 4 и т.д.)
            Formatting color = Formatting.WHITE;
            if (level >= 5) color = Formatting.RED;
            else if (level == 4) color = Formatting.LIGHT_PURPLE;
            else if (level == 3) color = Formatting.AQUA;

            text.append(Text.literal(name + level + " ").formatted(color));
        }
    }
}
