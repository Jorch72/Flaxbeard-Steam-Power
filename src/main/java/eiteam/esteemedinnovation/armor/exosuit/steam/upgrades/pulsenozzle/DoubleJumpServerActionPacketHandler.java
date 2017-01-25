package eiteam.esteemedinnovation.armor.exosuit.steam.upgrades.pulsenozzle;

import eiteam.esteemedinnovation.api.ChargableUtility;
import eiteam.esteemedinnovation.api.SteamChargable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class DoubleJumpServerActionPacketHandler implements IMessageHandler<DoubleJumpServerActionPacket, DoubleJumpClientResponsePacket> {
    @Override
    public DoubleJumpClientResponsePacket onMessage(DoubleJumpServerActionPacket message, MessageContext ctx) {
        EntityPlayer player = ctx.getServerHandler().playerEntity;
        if (ChargableUtility.hasPower(player, 15)) {
            // We know that this slot has the armor and that armor has the upgrade. Don't need to check any of that,
            // because it is handled automatically in ItemDoubleJumpUpgrade.
            ItemStack armorStack = player.getItemStackFromSlot(message.getSlot());

            if (!armorStack.getTagCompound().hasKey("usedJump")) {
                armorStack.getTagCompound().setBoolean("usedJump", false);
            }

            if (!armorStack.getTagCompound().getBoolean("usedJump") && !player.capabilities.isFlying) {
                armorStack.getTagCompound().setBoolean("usedJump", true);
                ItemStack chest = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
                ((SteamChargable) chest.getItem()).drainSteam(chest, 10, player);
                player.motionY = 0.65D;
                player.fallDistance = 0F;
                return new DoubleJumpClientResponsePacket();
            }
        }
        return null;
    }
}
