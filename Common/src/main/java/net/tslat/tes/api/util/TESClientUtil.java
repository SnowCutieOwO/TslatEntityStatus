package net.tslat.tes.api.util;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.tslat.tes.api.TESAPI;
import net.tslat.tes.mixin.client.GuiGraphicsAccessor;
import org.joml.Matrix4f;

import java.util.function.BiConsumer;

/**
 * Various helper methods for client-side functions
 */
public final class TESClientUtil {
	public static final ResourceLocation VANILLA_GUI_ICONS_LOCATION = new ResourceLocation("textures/gui/icons.png");
	public static final ResourceLocation CREATIVE_INVENTORY_TEXTURE = new ResourceLocation("textures/gui/container/creative_inventory/tab_inventory.png");

	/**
	 * Draw some text on screen at a given position, offset for the text's height and width
	 * @deprecated Use {@link TESClientUtil#centerTextForRender} and the various render style methods instead
	 */
	@Deprecated(forRemoval = true)
	public static void renderCenteredText(GuiGraphics guiGraphics, String text, float x, float y, int colour) {
		renderCenteredText(guiGraphics, Component.literal(text), x, y, colour);
	}

	/**
	 * Draw some text on screen at a given position, offset for the text's height and width
	 * @deprecated Use {@link TESClientUtil#centerTextForRender} and the various render style methods instead
	 */
	@Deprecated(forRemoval = true)
	public static void renderCenteredText(GuiGraphics guiGraphics, Component text, float x, float y, int colour) {
		drawText(guiGraphics, Minecraft.getInstance().font, text, x - Minecraft.getInstance().font.width(text) / 2f, y + 4f, colour);
	}

	/**
	 * Draw some text on screen at a given position, offset for the text's height and width
	 */
	public static void centerTextForRender(Component text, float x, float y, BiConsumer<Float, Float> renderRunnable) {
		renderRunnable.accept(x - Minecraft.getInstance().font.width(text) / 2f, y + (Minecraft.getInstance().font.lineHeight - 1) / 2f);
	}

	/**
	 * Translate the given {@link PoseStack} to face the game camera
	 */
	public static void positionFacingCamera(PoseStack poseStack) {
		poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
	}

	/**
	 * Prep the shader and bind a texture for the given ResourceLocation
	 */
	public static void prepRenderForTexture(ResourceLocation texture) {
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, texture);
	}

	/**
	 * Render a TES-style health bar at a given position for a given width
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param x The x-position of the bar (use 0 and translate the {@link PoseStack} for in-world rendering
	 * @param y The y-position of the bar (use 0 and translate the {@link PoseStack} for in-world rendering
	 * @param width The width of the bar. This method reconstructs the bar accurately using the two ends and a stretched center to make an indefinitely-applicable width
	 * @param v The v coordinate of the bar texture (from 'textures/gui/bars.png')
	 * @param percentComplete Percentage progress of the bar (use 1 for background or overlay pieces)
	 * @param withBarOverlay Render the bar segments overlay
	 * @param opacity The overall opacity of the bar
	 */
	public static void constructBarRender(GuiGraphics guiGraphics, int x, int y, int width, int v, float percentComplete, boolean withBarOverlay, float opacity) {
		int percentPixels = Math.round(percentComplete * width);
		int midBarWidth = width - 10;

		drawSimpleTexture(guiGraphics, x, y, Math.min(5, percentPixels), 5, 0, v, 256);

		if (percentPixels > 5) {
			if (midBarWidth > 0)
				drawSimpleTexture(guiGraphics, x + 5, y, Math.min(midBarWidth, percentPixels - 5), 5, 5, v, 256);

			if (percentPixels > width - 5)
				drawSimpleTexture(guiGraphics, x + 5 + midBarWidth, y, Math.min(5, percentPixels - 5), 5, 177, v, 256);
		}

		if (withBarOverlay && width > 10) {
			RenderSystem.setShaderColor(1, 1, 1, 0.75f * opacity);
			drawSimpleTexture(guiGraphics, x, y, width, 5, 0, 80, 182, 5, 256, 256);
		}
	}

	/**
	 * Render a statically-positioned view of an {@link LivingEntity entity} instance, optionally including the frame TES usually renders with
	 */
	public static void renderEntityIcon(GuiGraphics guiGraphics, Minecraft mc, float partialTick, LivingEntity entity, float opacity, boolean includeFrame) {
		float scale = 0.04f * (float)Math.pow(Math.min(30 / entity.getBbWidth(), 40 / entity.getBbHeight()), 0.95f);
		MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
		PoseStack poseStack = guiGraphics.pose();

		poseStack.pushPose();

		if (includeFrame) {
			TESClientUtil.prepRenderForTexture(TESClientUtil.CREATIVE_INVENTORY_TEXTURE);
			RenderSystem.enableBlend();
			RenderSystem.setShaderColor(1, 1, 1, 0.5f * opacity);
			TESClientUtil.drawSimpleTexture(guiGraphics, 2, 2, 34, 45, 72, 5, 256);

			poseStack.translate(20, 25, 0);
			poseStack.scale(-20, -20, 20);
		}

		poseStack.scale(scale, scale, scale);
		poseStack.translate(0, entity.getBbHeight() * -0.5f, entity.getBbWidth());

		float yBodyRotPrev = entity.yBodyRot;
		float yRotPrev = entity.getYRot();
		float xRotPrev = entity.getXRot();
		float yHeadRotOldPrev = entity.yHeadRotO;
		float yHeadRotPrev = entity.yHeadRot;
		int hurtTicks = entity.hurtTime;
		float attackTimePrev = entity.attackAnim;
		float attackTimeOldPrev = entity.oAttackAnim;

		entity.setYRot(22.5f);
		entity.setXRot(0);

		entity.yBodyRot = 22.5f;
		entity.yHeadRot = entity.getYRot();
		entity.yHeadRotO = entity.getYRot();

		entity.hurtTime = TESAPI.getConfig().hudEntityDamageOverlay() ? entity.hurtTime : 0;
		entity.attackAnim = 0;
		entity.oAttackAnim = 0;

		Lighting.setupForEntityInInventory();

		RenderSystem.runAsFancy(() -> renderEntityRaw(poseStack, entity, partialTick, 0, LightTexture.FULL_BRIGHT, bufferSource));

		bufferSource.endBatch();
		Lighting.setupFor3DItems();

		entity.setYRot(yRotPrev);
		entity.setXRot(xRotPrev);

		entity.yBodyRot = yBodyRotPrev;
		entity.yHeadRot = yHeadRotPrev;
		entity.yHeadRotO = yHeadRotOldPrev;

		entity.hurtTime = hurtTicks;
		entity.attackAnim = attackTimePrev;
		entity.oAttackAnim = attackTimeOldPrev;

		poseStack.popPose();
	}

	/**
	 * Render a static instance of an entity, skipping its shadows and hitbox
	 */
	public static <T extends Entity> void renderEntityRaw(PoseStack poseStack, T entity, float partialTick, float rotYaw, int packedLight, MultiBufferSource bufferSource) {
		EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		EntityRenderer<T> entityRenderer = (EntityRenderer<T>)entityRenderDispatcher.getRenderer(entity);

		try {
			Vec3 renderOffset = entityRenderer.getRenderOffset(entity, partialTick);

			poseStack.pushPose();
			poseStack.translate(renderOffset.x, renderOffset.y, renderOffset.z);
			entityRenderer.render(entity, rotYaw, 1, poseStack, bufferSource, packedLight);
			poseStack.popPose();
		}
		catch (Exception ex) {
			CrashReport wrappedReport = CrashReport.forThrowable(ex, "Rendering entity in world");
			CrashReportCategory entityReportCategory = wrappedReport.addCategory("Entity being rendered");

			entity.fillCrashReportCategory(entityReportCategory);

			CrashReportCategory rendererReportCategory = wrappedReport.addCategory("Renderer details");

			rendererReportCategory.setDetail("Assigned renderer", entityRenderer);

			throw new ReportedException(wrappedReport);
		}
	}

	/**
	 * Wrapper for {@link GuiGraphics#blit} to make it easier to use
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param posX The x position on the screen to render at
	 * @param posY The y position on the screen to render at
	 * @param width The width of the image
	 * @param height The height of the image
	 * @param u The x position on the texture image to render from
	 * @param v The y position on the texture image to render from
	 * @param pngSize The pixel-size of the png file (only for square png files)
	 */
	public static void drawSimpleTexture(GuiGraphics guiGraphics, int posX, int posY, int width, int height, float u, float v, int pngSize) {
		drawSimpleTexture(guiGraphics, posX, posY, width, height, u, v, pngSize, pngSize);
	}

	/**
	 * Wrapper for {@link GuiGraphics#blit} to make it easier to use
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param posX The x position on the screen to render at
	 * @param posY The y position on the screen to render at
	 * @param width The width of the image
	 * @param height The height of the image
	 * @param u The x position on the texture image to render from
	 * @param v The y position on the texture image to render from
	 * @param pngWidth The pixel-width of the png file
	 * @param pngHeight The pixel-height of the png file
	 */
	public static void drawSimpleTexture(GuiGraphics guiGraphics, int posX, int posY, int width, int height, float u, float v, int pngWidth, int pngHeight) {
		drawSimpleTexture(guiGraphics, posX, posY, width, height, u, v, width, height, pngWidth, pngHeight);
	}

	/**
	 * Wrapper for {@link GuiGraphics#blit} to make it easier to use
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param posX The x position on the screen to render at
	 * @param posY The y position on the screen to render at
	 * @param width The width of the in-game render
	 * @param height The height of the in-game render
	 * @param u The x position on the texture image to render from
	 * @param v The y position on the texture image to render from
	 * @param uWidth The width of the texture on the image to render
	 * @param vHeight The height of the texture on the image to render
	 * @param pngWidth The width of the entire png file
	 * @param pngHeight The height of the entire png file
	 */
	public static void drawSimpleTexture(GuiGraphics guiGraphics, int posX, int posY, int width, int height, float u, float v, int uWidth, int vHeight, int pngWidth, int pngHeight) {
		final Matrix4f pose = guiGraphics.pose().last().pose();
		final BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		final float widthRatio = 1.0F / pngWidth;
		final float heightRatio = 1.0F / pngHeight;

		RenderSystem.enableBlend();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(pose, (float)posX, (float)posY, 0).uv(u * widthRatio, v * heightRatio).endVertex();
		buffer.vertex(pose, (float)posX, (float)posY + height, 0).uv(u * widthRatio, (v + vHeight) * heightRatio).endVertex();
		buffer.vertex(pose, (float)posX + width, (float)posY + height, 0).uv((u + uWidth) * widthRatio, (v + vHeight) * heightRatio).endVertex();
		buffer.vertex(pose, (float)posX + width, (float)posY, 0).uv((u + uWidth) * widthRatio, v * heightRatio).endVertex();
		BufferUploader.drawWithShader(buffer.end());
	}

	public static void drawSprite(GuiGraphics guiGraphics, TextureAtlasSprite sprite, int posX, int posY, int width, int height, int u, int v, int uWidth, int vHeight, int pngWidth, int pngHeight) {
		final Matrix4f pose = guiGraphics.pose().last().pose();
		final BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		final float uMin = sprite.getU(u / (float)pngWidth);
		final float uMax = sprite.getU((u + uWidth) / (float)pngWidth);
		final float vMin = sprite.getV(v / (float)pngHeight);
		final float vMax = sprite.getV((v + vHeight) / (float)pngHeight);

		RenderSystem.enableBlend();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(pose, (float)posX, (float)posY, 0).uv(uMin, vMin).endVertex();
		buffer.vertex(pose, (float)posX, (float)posY + height, 0).uv(uMin, vMax).endVertex();
		buffer.vertex(pose, (float)posX + width, (float)posY + height, 0).uv(uMax, vMax).endVertex();
		buffer.vertex(pose, (float)posX + width, (float)posY, 0).uv(uMax, vMin).endVertex();
		BufferUploader.drawWithShader(buffer.end());
	}

	/**
	 * Wrapper for {@link GuiGraphics#drawString} to make it easier/more consistent to use
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param font The font instance to use for rendering the text
	 * @param text The text to draw
	 * @param x The x position on the screen to render at
	 * @param y The y position on the screen to render at
	 * @param colour The {@link net.minecraft.util.FastColor packed int} colour for the text
	 */
	public static void drawText(GuiGraphics guiGraphics, Font font, String text, float x, float y, int colour) {
		drawText(guiGraphics, font, Component.literal(text), x, y, colour);
	}

	/**
	 * Wrapper for {@link GuiGraphics#drawString} to make it easier/more consistent to use
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param font The font instance to use for rendering the text
	 * @param text The text to draw
	 * @param x The x position on the screen to render at
	 * @param y The y position on the screen to render at
	 * @param colour The {@link net.minecraft.util.FastColor packed int} colour for the text
	 */
	public static void drawText(GuiGraphics guiGraphics, Font font, Component text, float x, float y, int colour) {
		renderDefaultStyleText(font, guiGraphics.pose().last().pose(), text.getVisualOrderText(), x, y, colour, TextRenderType.NORMAL.getOutlineColour(colour), LightTexture.FULL_BRIGHT, guiGraphics.bufferSource());
		((GuiGraphicsAccessor)guiGraphics).callFlushIfUnmanaged();
	}

	/**
	 * Render text with no additional styling
	 */
	public static void renderDefaultStyleText(final Font fontRenderer, final Matrix4f pose, final FormattedCharSequence text, float x, float y, int colour, int outlineColour, int packedLight, final MultiBufferSource.BufferSource bufferSource) {
		fontRenderer.drawInBatch(text, x, y, colour, false, pose, bufferSource, Font.DisplayMode.NORMAL, outlineColour, packedLight);
	}

	/**
	 * Wrapper for {@link GuiGraphics#drawString} to make it easier/more consistent to use
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param font The font instance to use for rendering the text
	 * @param text The text to draw
	 * @param x The x position on the screen to render at
	 * @param y The y position on the screen to render at
	 * @param colour The {@link net.minecraft.util.FastColor packed int} colour for the text
	 */
	public static void drawTextWithShadow(GuiGraphics guiGraphics, Font font, String text, float x, float y, int colour) {
		drawTextWithShadow(guiGraphics, font, Component.literal(text), x, y, colour);
	}

	/**
	 * Wrapper for {@link GuiGraphics#drawString} to make it easier/more consistent to use, with a drop shadow
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param text The text to draw
	 * @param x The x position on the screen to render at
	 * @param y The y position on the screen to render at
	 * @param colour The {@link net.minecraft.util.FastColor packed int} colour for the text
	 */
	public static void drawTextWithShadow(GuiGraphics guiGraphics, Font font, Component text, float x, float y, int colour) {
		renderDropShadowStyleText(font, guiGraphics.pose().last().pose(), text.getVisualOrderText(), x, y, colour, TextRenderType.DROP_SHADOW.getOutlineColour(colour), LightTexture.FULL_BRIGHT, guiGraphics.bufferSource());
		((GuiGraphicsAccessor)guiGraphics).callFlushIfUnmanaged();
	}

	/**
	 * Render text with an 'drop-shadow' style - The text has a shadow of itself offset to the bottom right a handful of pixels
	 */
	public static void renderDropShadowStyleText(final Font fontRenderer, final Matrix4f pose, final FormattedCharSequence text, float x, float y, int colour, int outlineColour, int packedLight, final MultiBufferSource.BufferSource bufferSource) {
		final int borderColour = (outlineColour & -67108864) == 0 ? outlineColour | -16777216 : outlineColour;
		final Font.StringRenderOutput outlineOutput = fontRenderer.new StringRenderOutput(bufferSource, 0, 0, borderColour, false, pose, Font.DisplayMode.NORMAL, packedLight);
		final float[] newX = new float[] {x};

		text.accept((currentPosition, style, codePoint) -> {
			GlyphInfo glyphInfo = fontRenderer.getFontSet(style.getFont()).getGlyphInfo(codePoint, fontRenderer.filterFishyGlyphs);
			outlineOutput.x = newX[0] + glyphInfo.getShadowOffset();
			outlineOutput.y = y + glyphInfo.getShadowOffset();
			newX[0] += glyphInfo.getAdvance(style.isBold());

			return outlineOutput.accept(currentPosition, style.withColor(borderColour), codePoint);
		});

		Font.StringRenderOutput output = fontRenderer.new StringRenderOutput(bufferSource, x, y, (colour & -67108864) == 0 ? colour | -16777216 : colour, false, pose, Font.DisplayMode.POLYGON_OFFSET, packedLight);

		text.accept(output);
		output.finish(0, x);
	}

	/**
	 * Wrapper for {@link GuiGraphics#drawString} to make it easier/more consistent to use
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param font The font instance to use for rendering the text
	 * @param text The text to draw
	 * @param x The x position on the screen to render at
	 * @param y The y position on the screen to render at
	 * @param colour The {@link net.minecraft.util.FastColor packed int} colour for the text
	 */
	public static void drawTextWithGlow(GuiGraphics guiGraphics, Font font, String text, float x, float y, int colour) {
		drawTextWithGlow(guiGraphics, font, Component.literal(text), x, y, colour);
	}

	/**
	 * Wrapper for {@link GuiGraphics#drawString} to make it easier/more consistent to use, with a glowing/full outline
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param text The text to draw
	 * @param x The x position on the screen to render at
	 * @param y The y position on the screen to render at
	 * @param colour The {@link net.minecraft.util.FastColor packed int} colour for the text
	 */
	public static void drawTextWithGlow(GuiGraphics guiGraphics, Font font, Component text, float x, float y, int colour) {
		renderGlowingStyleText(font, guiGraphics.pose().last().pose(), text.getVisualOrderText(), x, y, colour, TextRenderType.GLOWING.getOutlineColour(colour), LightTexture.FULL_BRIGHT, guiGraphics.bufferSource());
		((GuiGraphicsAccessor)guiGraphics).callFlushIfUnmanaged();
	}

	/**
	 * Render text with a 'glowing' style - The text is surrounded on all sides by a thick border with maximum brightness
	 */
	public static void renderGlowingStyleText(final Font fontRenderer, final Matrix4f pose, final FormattedCharSequence text, float x, float y, int colour, int outlineColour, int packedLight, final MultiBufferSource.BufferSource bufferSource) {
		fontRenderer.drawInBatch8xOutline(text, x, y, colour, outlineColour, pose, bufferSource, packedLight);
	}

	/**
	 * Wrapper for {@link GuiGraphics#drawString} to make it easier/more consistent to use
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param font The font instance to use for rendering the text
	 * @param text The text to draw
	 * @param x The x position on the screen to render at
	 * @param y The y position on the screen to render at
	 * @param colour The {@link net.minecraft.util.FastColor packed int} colour for the text
	 */
	public static void drawTextWithOutline(GuiGraphics guiGraphics, Font font, String text, float x, float y, int colour) {
		drawTextWithOutline(guiGraphics, font, Component.literal(text), x, y, colour);
	}

	/**
	 * Wrapper for {@link GuiGraphics#drawString} to make it easier/more consistent to use, with a thin outline
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param text The text to draw
	 * @param x The x position on the screen to render at
	 * @param y The y position on the screen to render at
	 * @param colour The {@link net.minecraft.util.FastColor packed int} colour for the text
	 */
	public static void drawTextWithOutline(GuiGraphics guiGraphics, Font font, Component text, float x, float y, int colour) {
		renderOutlineStyleText(font, guiGraphics.pose().last().pose(), text.getVisualOrderText(), x, y, colour, TextRenderType.OUTLINED.getOutlineColour(colour), LightTexture.FULL_BRIGHT, guiGraphics.bufferSource());
		((GuiGraphicsAccessor)guiGraphics).callFlushIfUnmanaged();
	}

	/**
	 * Render text with an 'outlined' style - The text is surrounded on all sides by a thin border
	 */
	public static void renderOutlineStyleText(final Font fontRenderer, final Matrix4f pose, final FormattedCharSequence text, float x, float y, int colour, int outlineColour, int packedLight, final MultiBufferSource.BufferSource bufferSource) {
		final int borderColour = (outlineColour & -67108864) == 0 ? outlineColour | -16777216 : outlineColour;
		final Font.StringRenderOutput outlineOutput = fontRenderer.new StringRenderOutput(bufferSource, 0, 0, borderColour, false, pose, Font.DisplayMode.NORMAL, packedLight);

		for (float deltaX = -1; deltaX <= 1; deltaX++) {
			for (float deltaY = -1; deltaY <= 1; deltaY++) {
				if (deltaX == 0 ^ deltaY == 0) {
					final float[] newX = new float[] {x};
					final float offsetX = deltaX;
					final float offsetY = deltaY;

					text.accept((currentPosition, style, codePoint) -> {
						GlyphInfo glyphInfo = fontRenderer.getFontSet(style.getFont()).getGlyphInfo(codePoint, fontRenderer.filterFishyGlyphs);
						outlineOutput.x = newX[0] + offsetX * glyphInfo.getShadowOffset() * 0.6f;
						outlineOutput.y = y + offsetY * glyphInfo.getShadowOffset() * 0.6f;
						newX[0] += glyphInfo.getAdvance(style.isBold());

						return outlineOutput.accept(currentPosition, style.withColor(borderColour), codePoint);
					});
				}
			}
		}

		Font.StringRenderOutput output = fontRenderer.new StringRenderOutput(bufferSource, x, y, (colour & -67108864) == 0 ? colour | -16777216 : colour, false, pose, Font.DisplayMode.POLYGON_OFFSET, packedLight);

		text.accept(output);
		output.finish(0, x);
	}

	/**
	 * Wrapper for {@link GuiGraphics#fill} to make it easier/more consistent to use
	 * @param guiGraphics The GuiGraphics instance for the current render state
	 * @param posX The x position on the screen to render at
	 * @param posY The y position on the screen to render at
	 * @param width The width of the square
	 * @param height The height of the square
	 * @param colour The {@link net.minecraft.util.FastColor packed int} colour for the square
	 */
	public static void drawColouredSquare(GuiGraphics guiGraphics, int posX, int posY, int width, int height, int colour) {
		guiGraphics.fill(posX, posY, posX + width, posY + height, colour);
	}

	/**
	 * Translate a given locale key to its locally-translated string equivalent
	 */
	public static String translateKey(String key, Object... args) {
		return I18n.get(key, args);
	}

	/**
	 * Proxy-style getter for the client player
	 */
	public static Player getClientPlayer() {
		return Minecraft.getInstance().player;
	}

	/**
	 * Create a new {@link GuiGraphics} instance from the provided {@link net.minecraft.client.renderer.MultiBufferSource.BufferSource BufferSource}, pre-multiplying the {@link PoseStack} in line with the current pose
	 */
	public static GuiGraphics createInlineGuiGraphics(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
		GuiGraphics guiGraphics = new GuiGraphics(Minecraft.getInstance(), bufferSource);

		guiGraphics.pose().mulPoseMatrix(poseStack.last().pose());

		return guiGraphics;
	}

	/**
	 * Brighten/darken an ARGB-format colour packed integer by a given percentage
	 */
	public static int multiplyARGBColour(int colour, float multiplier) {
		return ARGB32.color(colour >>> 24,
				Mth.floor((colour >> 16 & 255) * multiplier) & 255,
				Mth.floor((colour >> 8 & 255) * multiplier) & 255,
				Mth.floor((colour & 255) * multiplier) & 255);
	}

	@FunctionalInterface
	public interface StyledTextRenderer {
		void render(final Font fontRenderer, final Matrix4f pose, final FormattedCharSequence text, float x, float y, int colour, int outlineColour, int packedLight, final MultiBufferSource.BufferSource bufferSource);
	}

	/**
	 * Text render types for rendering text in different styles
	 */
	public enum TextRenderType {
		NORMAL(TESClientUtil::renderDefaultStyleText, colour -> 0),
		DROP_SHADOW(TESClientUtil::renderDropShadowStyleText, colour -> TESClientUtil.multiplyARGBColour(colour, 0.25f)),
		GLOWING(TESClientUtil::renderGlowingStyleText, colour -> TESClientUtil.multiplyARGBColour(colour, 0.25f)),
		OUTLINED(TESClientUtil::renderOutlineStyleText, colour -> 0);

		private final StyledTextRenderer style;
		private final Int2IntFunction outlineColourGenerator;

		TextRenderType(StyledTextRenderer style, Int2IntFunction outlineColourGenerator) {
			this.style = style;
			this.outlineColourGenerator = outlineColourGenerator;
		}

		public void render(final Font fontRenderer, final PoseStack poseStack, final Component component, float x, float y, int colour, MultiBufferSource.BufferSource bufferSource) {
			render(fontRenderer, poseStack.last().pose(), component.getVisualOrderText(), x, y, colour, this.outlineColourGenerator.applyAsInt(colour), LightTexture.FULL_BRIGHT, bufferSource);
		}

		public void render(final Font fontRenderer, final Matrix4f pose, final FormattedCharSequence text, float x, float y, int colour, int outlineColour, int packedLight, final MultiBufferSource.BufferSource bufferSource) {
			this.style.render(fontRenderer, pose, text, x, y, colour, outlineColour, packedLight, bufferSource);
		}

		public int getOutlineColour(int colour) {
			return this.outlineColourGenerator.applyAsInt(colour);
		}
	}
}
