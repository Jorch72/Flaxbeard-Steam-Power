package eiteam.esteemedinnovation.transport.block;

import eiteam.esteemedinnovation.api.tile.SteamTransporterTileEntity;
import eiteam.esteemedinnovation.api.wrench.WrenchDisplay;
import eiteam.esteemedinnovation.api.wrench.Wrenchable;
import eiteam.esteemedinnovation.commons.Config;
import eiteam.esteemedinnovation.transport.TransportationModule;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

public class TileEntityPlonker extends SteamTransporterTileEntity implements Wrenchable, WrenchDisplay, IInventory {
    private static final String MODE_KEY = "Mode";
    private static final String INV_KEY = "Inventory";
    private boolean prevRedstoneActivated;
    private boolean curRedstoneActivated;
    @Nonnull
    private Mode mode = Mode.ALWAYS_ON;
    private ItemStack inventory;

    public TileEntityPlonker() {
        super(EnumFacing.VALUES);
    }

    @Override
    public boolean canUpdate(IBlockState target) {
        return target.getBlock() == TransportationModule.PLONKER;
    }

    @Override
    public void initialUpdate() {
        super.initialUpdate();
        EnumFacing dir = worldObj.getBlockState(pos).getValue(BlockPlonker.FACING);
        addSideToGaugeBlacklist(dir);
        setValidDistributionDirectionsExcluding(dir);
    }

    @Override
    public void safeUpdate() {
        EnumFacing dir = worldObj.getBlockState(pos).getValue(BlockPlonker.FACING);
        if (getSteamShare() < Config.plonkerConsumption) {
            return;
        }

        curRedstoneActivated = worldObj.isBlockPowered(pos);

        if (canPlace() && worldObj instanceof WorldServer) {
            FakePlayer player = FakePlayerFactory.getMinecraft((WorldServer) worldObj);
            inventory.getItem().onItemUse(inventory, player, worldObj, getOffsetPos(dir), player.getActiveHand(), dir.getOpposite(), 0.5F, 0.5F, 0.5F);
            if (mode == Mode.ALWAYS_ON) {
                decrSteam(Config.plonkerConsumption);
            }
        }

        prevRedstoneActivated = worldObj.isBlockPowered(pos);

        super.safeUpdate();
    }

    private boolean isTargetAvailable() {
        EnumFacing dir = worldObj.getBlockState(pos).getValue(BlockPlonker.FACING);
        BlockPos target = getOffsetPos(dir);
        return worldObj.getBlockState(target).getBlock().isReplaceable(worldObj, target);
    }

    private static boolean canSwitchMode(EntityPlayer player) {
        return player.isSneaking();
    }

    private boolean canPlace() {
        return inventory != null && mode.canPlace(this) && isTargetAvailable();
    }

    @Override
    public void displayWrench(RenderGameOverlayEvent.Post event) {
        GlStateManager.pushMatrix();
        int color = canSwitchMode(Minecraft.getMinecraft().thePlayer) ? 0xC6C6C6 : 0x777777;
        int x = event.getResolution().getScaledWidth() / 2 - 8;
        int y = event.getResolution().getScaledHeight() / 2 - 8;
        String loc = I18n.format("esteemedinnovation.plonker." + mode.localizationKey());
        Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(loc, x + 15, y + 13, color);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean onWrench(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, IBlockState state, float hitX, float hitY, float hitZ) {
        if (canSwitchMode(player)) {
            mode = mode.cycle();
            markForResync();
        }

        return true;
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Nullable
    @Override
    public ItemStack getStackInSlot(int index) {
        return inventory;
    }

    @Nullable
    @Override
    public ItemStack decrStackSize(int index, int count) {
        inventory.stackSize--;
        return inventory;
    }

    @Nullable
    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack cache = inventory;
        inventory = null;
        return cache;
    }

    @Override
    public void setInventorySlotContents(int index, @Nullable ItemStack stack) {
        inventory = stack;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return false;
    }

    @Override
    public void openInventory(EntityPlayer player) {}

    @Override
    public void closeInventory(EntityPlayer player) {}

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return stack.getItem() instanceof ItemBlock;
    }

    @Override
    public void clear() {
        inventory = null;
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public void readFromNBT(NBTTagCompound access) {
        super.readFromNBT(access);
        mode = Mode.LOOKUP[access.getInteger(MODE_KEY)];
        inventory = access.hasKey(INV_KEY) ? ItemStack.loadItemStackFromNBT(access.getCompoundTag(INV_KEY)) : null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound access) {
        super.writeToNBT(access);
        access.setInteger(MODE_KEY, mode.ordinal());
        if (inventory != null) {
            access.setTag(INV_KEY, inventory.serializeNBT());
        }
        return access;
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound access = writeToNBT(getUpdateTag());
        return new SPacketUpdateTileEntity(pos, 1, access);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        readFromNBT(pkt.getNbtCompound());
        markForResync();
    }

    enum Mode {
        ALWAYS_ON("always", (plonker) -> true),
        REDSTONE_ACTIVATED("pulse", (plonker) -> plonker.curRedstoneActivated && !plonker.prevRedstoneActivated);

        static final Mode[] LOOKUP = new Mode[2];
        static {
            for (Mode mode : values()) {
                LOOKUP[mode.ordinal()] = mode;
            }
        }

        private final String locKey;
        private final Predicate<TileEntityPlonker> canPlace;

        Mode(String locKey, Predicate<TileEntityPlonker> canPlace) {
            this.locKey = locKey;
            this.canPlace = canPlace;
        }

        Mode cycle() {
            return this == ALWAYS_ON ? REDSTONE_ACTIVATED : ALWAYS_ON;
        }

        String localizationKey() {
            return locKey;
        }

        boolean canPlace(TileEntityPlonker plonker) {
            return canPlace.test(plonker);
        }
    }

    // Ignore this crap...

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {}

    @Override
    public int getFieldCount() {
        return 0;
    }
}
