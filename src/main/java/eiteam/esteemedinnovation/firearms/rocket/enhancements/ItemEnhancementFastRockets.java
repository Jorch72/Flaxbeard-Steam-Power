package eiteam.esteemedinnovation.firearms.rocket.enhancements;

import eiteam.esteemedinnovation.api.enhancement.EnhancementRocketLauncher;
import eiteam.esteemedinnovation.api.entity.EntityRocket;
import eiteam.esteemedinnovation.commons.EsteemedInnovation;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import static eiteam.esteemedinnovation.firearms.FirearmModule.ROCKET_LAUNCHER;

public class ItemEnhancementFastRockets extends Item implements EnhancementRocketLauncher {
    @Override
    public boolean canApplyTo(ItemStack stack) {
        return stack.getItem() == ROCKET_LAUNCHER;
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EsteemedInnovation.upgrade;
    }

    @Override
    public String getID() {
        return "fastRocket";
    }

    @Override
    public ResourceLocation getModel(Item item) {
        return new ResourceLocation(EsteemedInnovation.MOD_ID, "rocket_launcher_fast");
    }

    @Override
    public String getName(Item item) {
        return "item.esteemedinnovation:rocketLauncherFast";
    }

    @Override
    public float getAccuracyChange(Item weapon) {
        return 0;
    }

    @Override
    public int getReloadChange(Item weapon) {
        return 0;
    }

    @Override
    public int getClipSizeChange(Item weapon) {
        return 0;
    }

    @Override
    public float getExplosionChange(Item weapon) {
        return 0;
    }

    @Override
    public int getFireDelayChange(ItemStack weapon) {
        return 0;
    }

    @Override
    public EntityRocket changeBullet(EntityRocket bullet) {
        bullet.motionX *= 2.0F;
        bullet.motionY *= 2.0F;
        bullet.motionZ *= 2.0F;

        return bullet;
    }

}
