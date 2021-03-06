package eiteam.esteemedinnovation.armor.exosuit.steam;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import eiteam.esteemedinnovation.api.Constants;
import eiteam.esteemedinnovation.api.SteamChargable;
import eiteam.esteemedinnovation.api.exosuit.*;
import eiteam.esteemedinnovation.armor.ArmorModule;
import eiteam.esteemedinnovation.commons.Config;
import eiteam.esteemedinnovation.commons.EsteemedInnovation;
import eiteam.esteemedinnovation.storage.steam.BlockTankItem;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemSteamExosuitArmor extends ItemArmor implements ExosuitArmor, SteamChargable {
    public ItemSteamExosuitArmor(EntityEquipmentSlot slot, ArmorMaterial mat) {
        super(mat, 1, slot);
        setMaxDamage(0);
    }

    @Override
    public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
        return false;
    }

    @Override
    public ResourceLocation getItemIconResource() {
        return new ResourceLocation(EsteemedInnovation.MOD_ID, "items/steam_exosuit_" + armorType.getName());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModelBiped getArmorModel(EntityLivingBase entityLiving, ItemStack itemStack, EntityEquipmentSlot armorSlot, ModelBiped defaultModel) {
        if (!(entityLiving instanceof EntityPlayer)) {
            return null;
        }

        ModelSteamExosuit modelExosuit = (ModelSteamExosuit) SteamExosuitModelCache.INSTANCE.getModel((EntityPlayer) entityLiving, armorSlot);

        boolean head = armorSlot == EntityEquipmentSlot.HEAD;
        modelExosuit.bipedHead.showModel = head;
        modelExosuit.bipedHeadwear.showModel = head;

        boolean body = armorSlot == EntityEquipmentSlot.CHEST;
        boolean legs = armorSlot == EntityEquipmentSlot.LEGS;
        modelExosuit.bipedBody.showModel = body || legs;
        modelExosuit.bipedRightArm.showModel = body;
        modelExosuit.bipedLeftArm.showModel = body;

        boolean feet = armorSlot == EntityEquipmentSlot.FEET;
        modelExosuit.bipedRightLeg.showModel = legs || feet;
        modelExosuit.bipedLeftLeg.showModel = legs || feet;

        return modelExosuit;
    }

    @Override
    public ArmorProperties getProperties(EntityLivingBase player, ItemStack armor, DamageSource source, double damage, int slot) {
        if (armor.hasTagCompound()) {
            if (armor.getTagCompound().hasKey("Plate")) {
                ExosuitPlate plate = UtilPlates.getPlate(armor.getTagCompound().getString("Plate"));
                return new ArmorProperties(0, plate.getDamageReductionAmount(armorType, source) / 25.0D, ItemArmor.ArmorMaterial.IRON.getDurability(armorType));
            }
        }
        return new ArmorProperties(0, ItemArmor.ArmorMaterial.IRON.getDamageReductionAmount(armorType) / 25.0D, ItemArmor.ArmorMaterial.IRON.getDurability(armorType));
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        updateSteamNBT(stack);
        //return 0.9D;
        return 1.0D - (stack.getTagCompound().getInteger("SteamStored") / (double) stack.getTagCompound().getInteger("SteamCapacity"));
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        if (!stack.getTagCompound().hasKey("SteamCapacity")) {
            stack.getTagCompound().setInteger("SteamCapacity", 0);
        }
        return stack.getTagCompound().getInteger("SteamCapacity") > 0;
    }

    @Override
    public int getArmorDisplay(EntityPlayer player, ItemStack armor, int slot) {
        if (armor.hasTagCompound()) {
            if (armor.getTagCompound().hasKey("Plate")) {
                ExosuitPlate plate = UtilPlates.getPlate(armor.getTagCompound().getString("Plate"));
                return plate.getDamageReductionAmount(armorType, DamageSource.generic);
            }
        }
        return ItemArmor.ArmorMaterial.LEATHER.getDamageReductionAmount(armorType);
    }

    @Override
    public void damageArmor(EntityLivingBase entity, ItemStack stack, DamageSource source, int damage, int slot) {
        if (armorType == EntityEquipmentSlot.CHEST) {
            drainSteam(stack, damage * 40, entity);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Pair<Integer, Integer>[] engineerCoordinates() {
        switch (armorType) {
            case HEAD: {
                return new Pair[] {
                  Pair.of(1, 19),
                  Pair.of(1, 1),
                  Pair.of(39, 16),
                  Pair.of(59, 36)
                };
            }
            case CHEST: {
                return new Pair[] {
                  Pair.of(1, 19),
                  Pair.of(1, 1),
                  Pair.of(49, 33),
                  Pair.of(75, 26),
                  Pair.of(1, 37)
                };
            }
            case LEGS: {
                return new Pair[] {
                  Pair.of(1, 19),
                  Pair.of(1, 1),
                  Pair.of(60, 12),
                  Pair.of(37, 40)
                };
            }
            case FEET: {
                return new Pair[] {
                  Pair.of(1, 19),
                  Pair.of(1, 1),
                  Pair.of(60, 18),
                  Pair.of(28, 40)
                };
            }
            default: {
                break;
            }
        }
        return new Pair[] { Pair.of(49, 26) };
    }

    public boolean hasPlates(ItemStack me) {
        if (getStackInSlot(me, 1) != null) {
            if (!me.hasTagCompound()) {
                me.setTagCompound(new NBTTagCompound());
            }
            ItemStack clone = getStackInSlot(me, 1).copy();
            clone.stackSize = 1;
            if (UtilPlates.getPlate(clone) != null) {
                me.getTagCompound().setString("Plate", UtilPlates.getPlate(clone).getIdentifier());
                return true;
            } else {
                UtilPlates.removePlate(me);
                return false;
            }
        } else {
            if (!me.hasTagCompound()) {
                me.setTagCompound(new NBTTagCompound());
            }
            UtilPlates.removePlate(me);
            return false;
        }
    }

    @Override
    public boolean hasUpgrade(ItemStack me, Item check) {
        if (me != null && check != null && me.hasTagCompound() && me.getTagCompound().hasKey("Upgrades")) {
            for (int i = 1; i < 10; i++) {
                if (me.getTagCompound().getCompoundTag("Upgrades").hasKey(Integer.toString(i))) {
                    ItemStack stack = ItemStack.loadItemStackFromNBT(me.getTagCompound().getCompoundTag("Upgrades").getCompoundTag(Integer.toString(i)));
                    if (stack.getItem() == check) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public ItemStack getStackInSlot(ItemStack me, int var1) {
        if (me.hasTagCompound()) {
            if (me.getTagCompound().hasKey("Upgrades")) {
                if (me.getTagCompound().getCompoundTag("Upgrades").hasKey(Integer.toString(var1))) {
                    return ItemStack.loadItemStackFromNBT(me.getTagCompound().getCompoundTag("Upgrades").getCompoundTag(Integer.toString(var1)));
                }
            }
        }
        return null;
    }

    @Override
    public void setInventorySlotContents(ItemStack me, int var1, ItemStack stack) {
        if (!me.hasTagCompound()) {
            me.setTagCompound(new NBTTagCompound());
        }
        if (!me.getTagCompound().hasKey("Upgrades")) {
            me.getTagCompound().setTag("Upgrades", new NBTTagCompound());
        }
        if (me.getTagCompound().getCompoundTag("Upgrades").hasKey(Integer.toString(var1))) {
            me.getTagCompound().getCompoundTag("Upgrades").removeTag(Integer.toString(var1));
        }
        NBTTagCompound stc = new NBTTagCompound();
        if (stack != null) {
            stack.writeToNBT(stc);
            me.getTagCompound().getCompoundTag("Upgrades").setTag(Integer.toString(var1), stc);
            if (var1 == 5 && armorType == EntityEquipmentSlot.CHEST) {
                me.getTagCompound().setInteger("SteamStored", 0);
                me.getTagCompound().setInteger("SteamCapacity", ((ExosuitTank) stack.getItem()).getStorage(me));
                if (stack.getItem() instanceof BlockTankItem && stack.getItemDamage() == 1) {
                    me.getTagCompound().setInteger("SteamStored", me.getTagCompound().getInteger("SteamCapacity"));
                }
            }
        }
        hasPlates(me);
    }

    @Override
    public boolean isItemValidForSlot(ItemStack me, int var1, ItemStack var2) {
        return true;
    }

    @Override
    public ItemStack decrStackSize(ItemStack me, int var1, int var2) {
        if (getStackInSlot(me, var1) != null) {
            ItemStack itemstack;
            if (getStackInSlot(me, var1).stackSize <= var2) {
                itemstack = getStackInSlot(me, var1);
                setInventorySlotContents(me, var1, null);
                hasPlates(me);
                return itemstack;
            } else {
                ItemStack stack2 = getStackInSlot(me, var1);
                itemstack = stack2.splitStack(var2);
                setInventorySlotContents(me, var1, stack2);

                if (getStackInSlot(me, var1).stackSize == 0) {
                    setInventorySlotContents(me, var1, null);
                }
                hasPlates(me);
                return itemstack;
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean canPutInSlot(ItemStack me, int slotNum, ItemStack upgrade) {
        if (slotNum == 0) {
            ItemStack clone = upgrade.copy();
            clone.stackSize = 1;
            return UtilPlates.getPlate(clone) != null;
        }
        if (upgrade.getItem() instanceof ExosuitUpgrade) {
            ExosuitUpgrade upgradeItem = (ExosuitUpgrade) upgrade.getItem();
            ExosuitSlot upgradeSlot = upgradeItem.getSlot();
            return (upgradeSlot.getArmorPiece() == armorType && upgradeSlot.getEngineeringSlot() == slotNum) || (upgradeItem.getSlot() == ExosuitSlot.VANITY && upgradeSlot.getEngineeringSlot() == slotNum);
        } else if (slotNum == ExosuitSlot.VANITY.getEngineeringSlot()) {
            // TODO: Optimize by using a static list of dye oredicts generated at load time (OreDictHelper).
            int[] ids = OreDictionary.getOreIDs(upgrade);
            for (int id : ids) {
                String str = OreDictionary.getOreName(id);
                if (str.contains("dye")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if the stack has the steam-related NBT values, and if not, sets them to 0.
     * @param me The ItemStack to check.
     */
    public void updateSteamNBT(ItemStack me) {
        if (!me.hasTagCompound()) {
            me.setTagCompound(new NBTTagCompound());
        }
        if (!me.getTagCompound().hasKey("SteamStored")) {
            me.getTagCompound().setInteger("SteamStored", 0);
        }
        if (!me.getTagCompound().hasKey("SteamCapacity")) {
            me.getTagCompound().setInteger("SteamCapacity", 0);
        }
    }

    @Override
    public boolean hasPower(ItemStack me, int powerNeeded) {
        if (armorType == EntityEquipmentSlot.CHEST) {
            updateSteamNBT(me);
            if (me.getTagCompound().getInteger("SteamStored") > powerNeeded) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean needsPower(ItemStack me, int powerNeeded) {
        if (armorType == EntityEquipmentSlot.CHEST) {
            updateSteamNBT(me);
            if (me.getTagCompound().getInteger("SteamStored") + powerNeeded < me.getTagCompound().getInteger("SteamCapacity")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets whether the given armor has a tank upgrade.
     * @param me The ItemStack
     */
    public boolean hasTank(ItemStack me) {
        if (armorType != EntityEquipmentSlot.CHEST) {
            return false;
        }
        if (!me.hasTagCompound()) {
            return false;
        }
        if (!me.getTagCompound().hasKey("Upgrades")) {
            return false;
        }
        NBTTagCompound inv = me.getTagCompound().getCompoundTag("Upgrades");
        for (int i = 1; i < 10; i++) {
            String s = Integer.toString(i);
            if (inv.hasKey(s)) {
                ItemStack stack = ItemStack.loadItemStackFromNBT(inv.getCompoundTag(s));
                if (stack != null && stack.getItem() != null && stack.getItem() instanceof ExosuitTank) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @Nonnull
    public ExosuitUpgrade[] getUpgrades(ItemStack me) {
        ArrayList<ExosuitUpgrade> upgrades = new ArrayList<>();
        if (me.hasTagCompound()) {
            if (me.getTagCompound().hasKey("Upgrades")) {
                for (int i = 2; i < 10; i++) {
                    if (me.getTagCompound().getCompoundTag("Upgrades").hasKey(Integer.toString(i))) {
                        ItemStack stack = ItemStack.loadItemStackFromNBT(me.getTagCompound().getCompoundTag("Upgrades").getCompoundTag(Integer.toString(i)));
                        if (stack.getItem() instanceof ExosuitUpgrade) {
                            upgrades.add((ExosuitUpgrade) stack.getItem());
                        }
                    }
                }
            }
        }
        return upgrades.toArray(new ExosuitUpgrade[0]);
    }

    @Override
    public void drawSlot(GuiContainer guiEngineeringTable, int slotNum, int i, int j) {
        guiEngineeringTable.mc.getTextureManager().bindTexture(Constants.ENG_GUI_TEXTURES);
        switch (armorType) {
            case HEAD:
            case LEGS:
            case FEET: {
                switch (slotNum) {
                    case 0: {
                        guiEngineeringTable.drawTexturedModalRect(i, j, 194, 0, 18, 18);
                        break;
                    }
                    case 1: {
                        guiEngineeringTable.drawTexturedModalRect(i, j, 230, 36, 18, 18);
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
            case CHEST: {
                switch (slotNum) {
                    case 0: {
                        guiEngineeringTable.drawTexturedModalRect(i, j, 194, 0, 18, 18);
                        break;
                    }
                    case 1: {
                        guiEngineeringTable.drawTexturedModalRect(i, j, 230, 36, 18, 18);
                        break;
                    }
                    case 4: {
                        guiEngineeringTable.drawTexturedModalRect(i, j, 176, 36, 18, 18);
                        break;
                    }
                    default: {
                        guiEngineeringTable.drawTexturedModalRect(i, j, 176, 0, 18, 18);
                    }
                }
            }
            default: {
                break;
            }
        }
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        if (armorType == EntityEquipmentSlot.CHEST) {
            return 10_000;
        }
        return 0;
    }

    @Override
    public int getDamage(ItemStack stack) {
        updateSteamNBT(stack);
        return (int) (((double) stack.getTagCompound().getInteger("SteamStored")) /
          stack.getTagCompound().getInteger("SteamCapacity") * 10_000.0D);
    }

    @Override
    public int steamPerDurability() {
        return Config.exoConsumption;
    }

    @Override
    public boolean canCharge(ItemStack stack) {
        if (armorType == EntityEquipmentSlot.CHEST) {
            ItemSteamExosuitArmor item = (ItemSteamExosuitArmor) stack.getItem();
            if (item.getStackInSlot(stack, 5) != null && item.getStackInSlot(stack, 5).getItem() instanceof ExosuitTank) {
                ExosuitTank tank = (ExosuitTank) item.getStackInSlot(stack, 5).getItem();
                return tank.canFill(stack);
            }
        }
        return false;
    }

    @Override
    public boolean addSteam(ItemStack me, int amount, EntityLivingBase player) {
        int curSteam = me.getTagCompound().getInteger("SteamStored");
        if (needsPower(me, amount)) {
            me.getTagCompound().setInteger("SteamStored", curSteam + amount);
            return true;
        }
        return false;
    }

    @Override
    public boolean drainSteam(ItemStack me, int amountToDrain, EntityLivingBase entity) {
        if (me != null) {
            if (me.getTagCompound() == null) {
                me.setTagCompound(new NBTTagCompound());
            }
            if (!me.getTagCompound().hasKey("SteamStored")) {
                me.getTagCompound().setInteger("SteamStored", 0);
            }
            int fill = me.getTagCompound().getInteger("SteamStored");
            fill = Math.max(0, fill - amountToDrain);
            me.getTagCompound().setInteger("SteamStored", fill);
            return true;
        }
        return false;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot armorSlot, ItemStack stack) {
        Multimap<String, AttributeModifier> map = HashMultimap.create();

        ItemSteamExosuitArmor armor = (ItemSteamExosuitArmor) stack.getItem();

        if (armor.hasPlates(stack)) {
            ExosuitPlate plate = UtilPlates.getPlate(stack);
            if (plate != null) {
                map.putAll(plate.getAttributeModifiersForExosuit(armorSlot, stack));
            }
        }

        ExosuitUpgrade[] upgrades = armor.getUpgrades(stack);
        for (ExosuitUpgrade upgrade : upgrades) {
            map.putAll(upgrade.getAttributeModifiersForExosuit(armorSlot, stack));
        }

        return map;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack me, EntityPlayer player, List<String> list, boolean advanced) {
        super.addInformation(me, player, list, advanced);
        if (me.hasTagCompound()) {
            // TODO: Abstract into API
            if (hasPlates(me) && !"Thaumium".equals(UtilPlates.getPlate(me.getTagCompound().getString("Plate")).getIdentifier()) &&
              !"Terrasteel".equals(UtilPlates.getPlate(me.getTagCompound().getString("Plate")).getIdentifier())) {
                list.add(TextFormatting.BLUE + UtilPlates.getPlate(me.getTagCompound().getString("Plate")).effect());
            }
            if (me.getTagCompound().hasKey("Upgrades")) {
                for (int i = 3; i < 10; i++) {
                    if (me.getTagCompound().getCompoundTag("Upgrades").hasKey(Integer.toString(i))) {
                        ItemStack stack = ItemStack.loadItemStackFromNBT(me.getTagCompound().getCompoundTag("Upgrades").getCompoundTag(Integer.toString(i)));
                        list.add(TextFormatting.RED + stack.getDisplayName());
                    }
                }
            }
            if (me.getTagCompound().getCompoundTag("Upgrades").hasKey("2")) {
                ItemStack stack = ItemStack.loadItemStackFromNBT(me.getTagCompound().getCompoundTag("Upgrades").getCompoundTag("2"));
                // TODO: Abstract into API
                if (stack.getItem() == ArmorModule.ENDER_SHROUD) {
                    list.add(TextFormatting.DARK_GREEN + I18n.format("esteemedinnovation.exosuit.shroud"));
                } else {
                    int dye = -1;
                    int dyeIndex = ModelSteamExosuit.findDyeIndexFromItemStack(stack);
                    if (dyeIndex != -1) {
                        dye = dyeIndex;
                    }
                    if (dye != -1) {
                        list.add(TextFormatting.DARK_GREEN + I18n.format("esteemedinnovation.color." + ModelSteamExosuit.DYES[dye].toLowerCase()));
                    } else {
                        list.add(TextFormatting.DARK_GREEN + stack.getDisplayName());
                    }
                }
            }
        }
        updateSteamNBT(me);
        if (armorType == EntityEquipmentSlot.CHEST) {
           list.add(TextFormatting.WHITE + "" + me.getTagCompound().getInteger("SteamStored") * 5 + "/" + me.getTagCompound().getInteger("SteamCapacity") * 5 + " SU");
        }
    }

    @Override
    public void drawBackground(GuiContainer guiEngineeringTable, int i, int j, int k) {
        guiEngineeringTable.mc.getTextureManager().bindTexture(Constants.ENG_ARMOR_TEXTURES);
        guiEngineeringTable.drawTexturedModalRect(j + 26, k + 3, 64 * armorType.getIndex(), 0, 64, 64);
    }

    @Nonnull
    @Override
    public ExosuitEventHandler[] getInstalledEventHandlers(ItemStack self) {
        List<ExosuitEventHandler> handlers = new ArrayList<>(Arrays.asList(getUpgrades(self)));
        if (self.hasTagCompound() && self.getTagCompound().hasKey("Plate")) {
            handlers.add(UtilPlates.getPlate(self.getTagCompound().getString("Plate")));
        }
        return handlers.toArray(new ExosuitEventHandler[handlers.size()]);
    }
}