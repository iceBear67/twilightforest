package twilightforest.client.model.entity;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.model.ListModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import twilightforest.entity.CarminiteGhastguardEntity;

import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class TFGhastModel<T extends CarminiteGhastguardEntity> extends ListModel<T> {
	public ModelPart body;
	protected ModelPart[] tentacles = new ModelPart[9];

	public TFGhastModel() {
		byte yOffset = -16;
		this.body = new ModelPart(this, 0, 0);
		this.body.addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16);
		this.body.y += 24 + yOffset;
		Random rand = new Random(1660L);

		for (int i = 0; i < this.tentacles.length; ++i) {
			makeTentacle(yOffset, rand, i);
		}
	}

	protected void makeTentacle(byte yOffset, Random random, int i) {
		this.tentacles[i] = new ModelPart(this, 0, 0);
		float xPoint = ((i % 3 - i / 3 % 2 * 0.5F + 0.25F) / 2.0F * 2.0F - 1.0F) * 5.0F;
		float zPoint = (i / 3 / 2.0F * 2.0F - 1.0F) * 5.0F;
		int length = random.nextInt(7) + 8;
		this.tentacles[i].addBox(-1.0F, 0.0F, -1.0F, 2, length, 2);
		this.tentacles[i].x = xPoint;
		this.tentacles[i].z = zPoint;
		this.tentacles[i].y = 23 + yOffset;

		this.body.addChild(this.tentacles[i]);
	}

	/**
	 * Sets the model's various rotation angles. For bipeds, par1 and par2 are used for animating the movement of arms
	 * and legs, where par1 represents the time(so that arms and legs swing back and forth) and par2 represents how
	 * "far" arms and legs can swing at most.
	 */
	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		// wave tentacles
		for (int i = 0; i < this.tentacles.length; ++i) {
			this.tentacles[i].xRot = 0.2F * Mth.sin(ageInTicks * 0.3F + i) + 0.4F;
		}

		// make body face what we're looking at
		this.body.xRot = headPitch / (180F / (float) Math.PI);
		this.body.yRot = netHeadYaw / (180F / (float) Math.PI);
	}

	/**
	 * Sets the models various rotation angles then renders the model.
	 */

    @Override
	public Iterable<ModelPart> parts() {
		return ImmutableList.of(body);
	}
}