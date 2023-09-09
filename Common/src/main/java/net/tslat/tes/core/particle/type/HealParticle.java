package net.tslat.tes.core.particle.type;

import com.mojang.math.Vector3f;
import net.tslat.tes.api.TESAPI;
import net.tslat.tes.api.TESConstants;
import net.tslat.tes.api.TESParticle;
import net.tslat.tes.core.state.EntityState;

/**
 * Built-in class for healing-type {@link TESParticle TES Particles}
 */
public class HealParticle extends NumericParticle {
	public HealParticle(EntityState entityState, Vector3f position, double amount) {
		this(entityState, position, Animation.RISE, amount);
	}

	public HealParticle(EntityState entityState, Vector3f position, Animation animation, double amount) {
		this(entityState, position, animation, amount, TESConstants.CONFIG.defaultParticleLifespan());
	}

	public HealParticle(EntityState entityState, Vector3f position, Animation animation, double amount, int lifespan) {
		super(entityState, position, animation, amount, lifespan);

		withColour(TESAPI.getConfig().getHealParticleColour());
	}
}
