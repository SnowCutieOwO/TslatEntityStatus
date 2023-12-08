package net.tslat.tes.networking;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.NetworkEvent;
import net.tslat.tes.api.TESConstants;

import java.util.Collection;
import java.util.Set;

public class RequestEffectsPacket {
	private final int entityId;

	public RequestEffectsPacket(final int entityId) {
		this.entityId = entityId;
	}

	public void encode(final FriendlyByteBuf buf) {
		buf.writeVarInt(this.entityId);
	}

	public static RequestEffectsPacket decode(final FriendlyByteBuf buf) {
		return new RequestEffectsPacket(buf.readVarInt());
	}

	public void handleMessage(NetworkEvent.Context context) {
		context.enqueueWork(() -> {
			Entity entity = context.getSender().level().getEntity(this.entityId);

			if (entity instanceof LivingEntity livingEntity) {
				Collection<MobEffectInstance> effects = livingEntity.getActiveEffects();
				Set<ResourceLocation> ids = new ObjectOpenHashSet<>(effects.size());

				for (MobEffectInstance instance : effects) {
					if (instance.isVisible() || instance.showIcon())
						ids.add(BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect()));
				}

				TESConstants.NETWORKING.sendEffectsSync(context.getSender(), this.entityId, ids, Set.of());
			}
		});

		context.setPacketHandled(true);
	}
}
