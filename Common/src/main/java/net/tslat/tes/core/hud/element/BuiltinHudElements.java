package net.tslat.tes.core.hud.element;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.tslat.tes.api.TESAPI;
import net.tslat.tes.api.TESConfig;
import net.tslat.tes.api.TESConstants;
import net.tslat.tes.api.util.TESClientUtil;
import net.tslat.tes.api.util.TESUtil;
import net.tslat.tes.core.hud.TESHud;
import net.tslat.tes.core.state.EntityState;
import net.tslat.tes.core.state.TESEntityTracking;

/**
 * Built-in HUD handles for the default rendering capabilities for the mod
 */
public final class BuiltinHudElements {
	private static final ResourceLocation BARS_TEXTURE = new ResourceLocation("textures/gui/bars.png");
	private static final ResourceLocation ICONS_TEXTURE = new ResourceLocation(TESConstants.MOD_ID, "textures/gui/tes_icons.png");

	public static int renderEntityName(GuiGraphics guiGraphics, Minecraft mc, float partialTick, LivingEntity entity, float opacity, boolean inWorldHud) {
		if (inWorldHud) {
			if (!TESAPI.getConfig().inWorldHudEntityName() && (!TESConstants.CONFIG.inWorldHudNameOverride() || !entity.hasCustomName()))
				return 0;

			TESClientUtil.centerTextForRender(entity.getDisplayName(), 0, 0, (x, y) -> TESAPI.getConfig().inWorldHudEntityNameFontStyle().render(mc.font, guiGraphics.pose(), entity.getDisplayName(), x, y, FastColor.ARGB32.color((int)(opacity * 255f), 255, 255, 255), guiGraphics.bufferSource()));
		}
		else {
			if (!TESAPI.getConfig().hudEntityName())
				return 0;

			TESAPI.getConfig().hudEntityNameFontStyle().render(mc.font, guiGraphics.pose(), entity.getDisplayName(), 0, 0, FastColor.ARGB32.color((int)(opacity * 255f), 255, 255, 255), guiGraphics.bufferSource());
		}

		TESEntityTracking.markNameRendered(entity);
		guiGraphics.bufferSource().endLastBatch();

		return mc.font.lineHeight;
	}

	public static int renderEntityHealth(GuiGraphics guiGraphics, Minecraft mc, float partialTick, LivingEntity entity, float opacity, boolean inWorldHud) {
		EntityState entityState = TESEntityTracking.getStateForEntity(entity);

		if (entityState == null)
			return 0;

		TESConfig config = TESAPI.getConfig();
		int barWidth = inWorldHud ? config.inWorldBarsLength() : config.hudHealthBarLength();
		TESHud.BarRenderType renderType = inWorldHud ? config.inWorldBarsRenderType() : config.hudHealthRenderType();
		PoseStack poseStack = guiGraphics.pose();

		poseStack.pushPose();
		poseStack.translate(0, inWorldHud ? 4 : 1, 0);

		if (inWorldHud) {
			poseStack.translate(barWidth * -0.5f, 0, 0);
			poseStack.scale(1, 1, -1);
		}

		TESClientUtil.prepRenderForTexture(BARS_TEXTURE);
		RenderSystem.setShaderColor(1, 1, 1, opacity);
		RenderSystem.enableDepthTest();

		if (renderType != TESHud.BarRenderType.NUMERIC) {
			float percentHealth = entityState.getHealth() / entity.getMaxHealth();
			float percentTransitionHealth = entityState.getLastTransitionHealth() / entity.getMaxHealth();
			boolean doSegments = inWorldHud ? config.inWorldBarsSegments() : config.hudHealthBarSegments();
			int uvY = TESConstants.UTILS.getEntityType(entity).getTextureYPos();

			TESClientUtil.constructBarRender(guiGraphics, 0, 0, barWidth, 60, 1, false, opacity);
			poseStack.translate(0, 0, 0.001f);

			if (percentTransitionHealth > percentHealth)
				TESClientUtil.constructBarRender(guiGraphics, 0, 0, barWidth, uvY, entityState.getLastTransitionHealth() / entity.getMaxHealth(), false, opacity);

			poseStack.translate(0, 0, 0.001f);

			RenderSystem.enableBlend();
			TESClientUtil.constructBarRender(guiGraphics, 0, 0, barWidth, uvY + 5, percentHealth, doSegments, opacity);
		}

		if (renderType != TESHud.BarRenderType.BAR) {
			String healthText = TESUtil.roundToDecimal(entityState.getHealth(), 1) + "/" + TESUtil.roundToDecimal(entity.getMaxHealth(), 1);
			float halfTextWidth = mc.font.width(healthText) / 2f;
			float center = barWidth / 2f;

			RenderSystem.setShader(GameRenderer::getPositionColorShader);

			poseStack.translate(0, 0, 0.001f);
			TESClientUtil.drawColouredSquare(guiGraphics, (int)(center - halfTextWidth - 1), -2, (int)(halfTextWidth * 2) + 1, 9, 0x090909 | (int)(opacity * 255 * TESConstants.CONFIG.hudBarFontBackingOpacity()) << 24);
			poseStack.translate(0, 0, 0.001f);

			(inWorldHud ? TESAPI.getConfig().inWorldHudHealthFontStyle() : TESAPI.getConfig().hudHealthFontStyle()).render(mc.font, guiGraphics.pose(), Component.literal(healthText), center - halfTextWidth, -1, FastColor.ARGB32.color((int)(opacity * 255f), 255, 255, 255), guiGraphics.bufferSource());
		}

		poseStack.popPose();

		return mc.font.lineHeight;
	}

	public static int renderEntityArmour(GuiGraphics guiGraphics, Minecraft mc, float partialTick, LivingEntity entity, float opacity, boolean inWorldHud) {
		if (inWorldHud) {
			if (!TESAPI.getConfig().inWorldHudArmour())
				return 0;
		}
		else {
			if (!TESAPI.getConfig().hudArmour())
				return 0;
		}

		int armour = TESUtil.getArmour(entity);

		if (armour <= 0)
			return 0;

		float toughness = TESUtil.getArmourToughness(entity);
		int textColour = FastColor.ARGB32.color((int)(opacity * 255f), 255, 255, 255);
		PoseStack poseStack = guiGraphics.pose();

		poseStack.pushPose();

		if (inWorldHud) {
			int totalWidth = toughness > 0 ? 43 + mc.font.width("x" + TESUtil.roundToDecimal(toughness, 1)) : mc.font.width("x" + armour) + 10;

			poseStack.translate(totalWidth * -0.5f, 0, 0);
		}

		TESClientUtil.prepRenderForTexture(TESClientUtil.VANILLA_GUI_ICONS_LOCATION);
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1, 1, 1, opacity);
		TESClientUtil.drawSimpleTexture(guiGraphics, 0, 0, 9, 9, 34, 9, 256);

		if (toughness > 0)
			TESClientUtil.drawSimpleTexture(guiGraphics, 33, 0, 9, 9, 43, 18, 256);

		(inWorldHud ? TESAPI.getConfig().inWorldHudArmourFontStyle() : TESAPI.getConfig().hudArmourFontStyle()).render(mc.font, guiGraphics.pose(), Component.literal("x" + armour), 9.5f, 1, textColour, guiGraphics.bufferSource());

		if (toughness > 0)
			(inWorldHud ? TESAPI.getConfig().inWorldHudArmourFontStyle() : TESAPI.getConfig().hudArmourFontStyle()).render(mc.font, guiGraphics.pose(), Component.literal("x" + TESUtil.roundToDecimal(toughness, 1)), 43, 1, textColour, guiGraphics.bufferSource());

		poseStack.popPose();

		return mc.font.lineHeight;
	}

	public static int renderEntityIcons(GuiGraphics guiGraphics, Minecraft mc, float partialTick, LivingEntity entity, float opacity, boolean inWorldHud) {
		if (inWorldHud) {
			if (!TESAPI.getConfig().inWorldHudEntityIcons())
				return 0;
		}
		else {
			if (!TESAPI.getConfig().hudEntityIcons())
				return 0;
		}

		int x = 0;

		TESClientUtil.prepRenderForTexture(ICONS_TEXTURE);

		if (TESUtil.isFireImmune(entity)) {
			TESClientUtil.drawSimpleTexture(guiGraphics, x, 0, 8, 8, 0, 0, 32);

			x += 9;
		}

		if (TESUtil.isMeleeMob(entity)) {
			TESClientUtil.drawSimpleTexture(guiGraphics, x, 0, 8, 8, 8, 0, 32);

			x += 9;
		}

		if (TESUtil.isRangedMob(entity)) {
			TESClientUtil.drawSimpleTexture(guiGraphics, x, 0, 8, 8, 16, 0, 32);

			x += 9;
		}

		MobType mobType = entity.getMobType();

		if (mobType != MobType.UNDEFINED) {
			int mobTypeU = mobType == MobType.WATER ? 24 : (mobType == MobType.ILLAGER ? 16 : (mobType == MobType.ARTHROPOD ? 8 : 0));

			TESClientUtil.drawSimpleTexture(guiGraphics, x, 0, 8, 8, mobTypeU, 8, 32);

			x += 9;
		}

		return x == 0 ? 0 : 8;
	}

	public static int renderEntityEffects(GuiGraphics guiGraphics, Minecraft mc, float partialTick, LivingEntity entity, float opacity, boolean inWorldHud) {
		if (inWorldHud) {
			if (!TESAPI.getConfig().inWorldHudPotionIcons())
				return 0;
		}
		else {
			if (!TESAPI.getConfig().hudPotionIcons())
				return 0;
		}

		EntityState entityState = TESEntityTracking.getStateForEntity(entity);

		if (entityState == null || entityState.getEffects().isEmpty())
			return 0;


		int effectsSize = entityState.getEffects().size();
		MobEffectTextureManager textureManager = mc.getMobEffectTextures();
		int barLength = inWorldHud ? TESAPI.getConfig().inWorldBarsLength() : TESAPI.getConfig().hudHealthBarLength();
		float maxX = barLength * 2f;
		int iconsPerRow = (int)Math.floor(maxX / 18f);
		int rows = (int)Math.ceil(effectsSize / (float)iconsPerRow);
		int x = inWorldHud ? (Math.min(effectsSize, iconsPerRow) * -9) : 0;
		int y = 0;
		int i = 0;
		PoseStack poseStack = guiGraphics.pose();

		poseStack.pushPose();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		poseStack.scale(0.5f, 0.5f, 1);

		if (inWorldHud)
			poseStack.translate(0, Math.floor(effectsSize * 18 / maxX) * -18, 0);

		for (ResourceLocation effectId : entityState.getEffects()) {
			TextureAtlasSprite sprite = textureManager.get(BuiltInRegistries.MOB_EFFECT.get(effectId));

			RenderSystem.setShaderTexture(0, sprite.atlasLocation());
			guiGraphics.blit(i * 18 + x, y, 0, 18, 18, sprite);

			if (++i >= iconsPerRow) {
				i = 0;
				y += 18;

				if (inWorldHud && y / 18 == rows - 1)
					x = (effectsSize % iconsPerRow) % iconsPerRow * -9;
			}
		}

		poseStack.popPose();

		return (int)Math.ceil(effectsSize / (float)iconsPerRow) * 9;
	}
}
