package flaxbeard.steamcraft.tile;

import flaxbeard.steamcraft.Config;
import flaxbeard.steamcraft.Steamcraft;
import flaxbeard.steamcraft.api.ISteamTransporter;
import flaxbeard.steamcraft.api.IWrenchDisplay;
import flaxbeard.steamcraft.api.IWrenchable;
import flaxbeard.steamcraft.api.steamnet.SteamNetwork;
import flaxbeard.steamcraft.api.tile.SteamTransporterTileEntity;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Post;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.BlockFluidFinite;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class TileEntityFan extends SteamTransporterTileEntity implements ISteamTransporter, IWrenchable, IWrenchDisplay {
    public boolean active;
    public boolean powered = false;
    public boolean lastSteam = false;
    public int rotateTicks = 0;
    public int range = 9;
    private boolean isInitialized = false;
    private static final int STEAM_CONSUMPTION = Config.fanConsumption;

    public TileEntityFan() {
        addSidesToGaugeBlacklist(EnumFacing.HORIZONTALS);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound access) {
        super.writeToNBT(access);
        access.setBoolean("powered", powered);
        access.setShort("range", (short) range);

        return access;
    }

    @Override
    public void readFromNBT(NBTTagCompound access) {
        super.readFromNBT(access);
        powered = access.getBoolean("powered");
        range = access.getShort("range");

    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound access = super.getDescriptionTag();
        access.setBoolean("active", getSteamShare() > 0 && !powered);
        access.setShort("range", (short) this.range);
        return new SPacketUpdateTileEntity(pos, 1, access);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        NBTTagCompound access = pkt.getNbtCompound();
        active = access.getBoolean("active");
        range = access.getShort("range");
        markDirty();
    }

    @Override
    public void update() {
        if (lastSteam != this.getSteamShare() >= STEAM_CONSUMPTION) {
            markDirty();
        }
        lastSteam = getSteamShare() > STEAM_CONSUMPTION;
        if (!isInitialized) {
            powered = worldObj.isBlockIndirectlyGettingPowered(pos) > 0;
            setDistributionDirections(new EnumFacing[] { EnumFacing.getFront(getBlockMetadata()).getOpposite() });
            isInitialized = true;
        }
        super.update();
        if (active && worldObj.isRemote) {
            rotateTicks++;
        }
        if (active && worldObj.isRemote || (getSteamShare() >= STEAM_CONSUMPTION && !powered)) {
            if (!this.worldObj.isRemote) {
                this.decrSteam(STEAM_CONSUMPTION);
            }
            int meta = this.worldObj.getBlockMetadata(xCoord, yCoord, zCoord);
            ForgeDirection dir = ForgeDirection.getOrientation(meta);
            this.worldObj.spawnParticle("smoke", xCoord + (dir.offsetX == 0 ? Math.random() : 0.5F), yCoord + (dir.offsetY == 0 ? Math.random() : 0.5F), zCoord + (dir.offsetZ == 0 ? Math.random() : 0.5F), dir.offsetX * 0.2F, dir.offsetY * 0.2F, dir.offsetZ * 0.2F);
            int blocksInFront = 0;
            boolean blocked = false;
            for (int i = 1; i < range; i++) {
                int x = xCoord + dir.offsetX * i;
                int y = yCoord + dir.offsetY * i;
                int z = zCoord + dir.offsetZ * i;
                if (!this.worldObj.isRemote && this.worldObj.rand.nextInt(20) == 0 && !blocked && this.worldObj.getBlock(x, y, z) != Blocks.air && this.worldObj.getBlock(x, y, z).isReplaceable(worldObj, x, y, z) || this.worldObj.getBlock(x, y, z) instanceof BlockCrops) {
                    int tMeta = this.worldObj.getBlockMetadata(x, y, z);
                    if (//...
                      !(this.worldObj.getBlock(x, y, z) instanceof BlockFluidBase)    ||
                      !(this.worldObj.getBlock(x, y, z) instanceof BlockFluidClassic) ||
                      !(this.worldObj.getBlock(x, y, z) instanceof BlockFluidFinite)) {
                        this.worldObj.getBlock(x, y, z).dropBlockAsItem(worldObj, x, y, z, tMeta, 0);
                        for (int v = 0; v < 5; v++) {
                            Steamcraft.proxy
                              .spawnBreakParticles(worldObj, xCoord + dir.offsetX * i + 0.5F,
                                yCoord + dir.offsetY * i + 0.5F, zCoord + dir.offsetZ * i + 0.5F,
                                this.worldObj.getBlock(x, y, z), 0.0F, 0.0F, 0.0F);
                        }
                    }
                    this.worldObj.setBlockToAir(x, y, z);
                }
                if (!blocked && (this.worldObj.getBlock(x, y, z).isReplaceable(worldObj, x, y, z)
                        || this.worldObj.isAirBlock(x, y, z)
                        || this.worldObj.getBlock(x, y, z) instanceof BlockTrapDoor
                        || this.worldObj.getBlock(x, y, z).getCollisionBoundingBoxFromPool(worldObj, x, y, z) == null)) {
                    blocksInFront = i;
                    if (i != range - 1)
                        this.worldObj.spawnParticle("smoke", xCoord + dir.offsetX * i + (dir.offsetX == 0 ? Math.random() : 0.5F), yCoord + dir.offsetY * i + (dir.offsetY == 0 ? Math.random() : 0.5F), zCoord + dir.offsetZ * i + (dir.offsetZ == 0 ? Math.random() : 0.5F), dir.offsetX * 0.2F, dir.offsetY * 0.2F, dir.offsetZ * 0.2F);
                } else {
                    blocked = true;
                }
            }
            List entities = worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(xCoord + (dir.offsetX < 0 ? dir.offsetX * blocksInFront : 0), yCoord + (dir.offsetY < 0 ? dir.offsetY * blocksInFront : 0), zCoord + (dir.offsetZ < 0 ? dir.offsetZ * blocksInFront : 0), xCoord + 1 + (dir.offsetX > 0 ? dir.offsetX * blocksInFront : 0), yCoord + 1 + (dir.offsetY > 0 ? dir.offsetY * blocksInFront : 0), zCoord + 1 + (dir.offsetZ > 0 ? dir.offsetZ * blocksInFront : 0)));
            for (Object obj : entities) {
                Entity entity = (Entity) obj;
                if (!(entity instanceof EntityPlayer) || !(((EntityPlayer) entity).capabilities.isFlying && ((EntityPlayer) entity).capabilities.isCreativeMode)) {
                    if (entity instanceof EntityPlayer && entity.isSneaking()) {
                        entity.motionX += dir.offsetX * 0.025F;
                        entity.motionY += dir.offsetY * 0.05F;
                        entity.motionZ += dir.offsetZ * 0.025F;
                    } else {
                        entity.motionX += dir.offsetX * 0.075F;
                        entity.motionY += dir.offsetY * 0.1F;
                        entity.motionZ += dir.offsetZ * 0.075F;
                    }
                    entity.fallDistance = 0.0F;
                }
            }
        }
    }

    public void updateRedstoneState(boolean flag) {
        if (flag != powered) {
            this.powered = flag;
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    @Override
    public boolean onWrench(ItemStack stack, EntityPlayer player, World world,
                            int x, int y, int z, int side, float xO, float yO, float zO) {
        if (player.isSneaking()) {
            switch (range) {
                case 9:
                    range = 11;
                    break;
                case 11:
                    range = 13;
                    break;
                case 13:
                    range = 15;
                    break;
                case 15:
                    range = 17;
                    break;
                case 17:
                    range = 19;
                    break;
                case 19:
                    range = 5;
                    break;
                case 5:
                    range = 7;
                    break;
                case 7:
                    range = 9;
                    break;
            }
            //Steamcraft.log.debug(range);
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            return true;
        } else {
            int steam = this.getSteamShare();
            this.getNetwork().split(this, true);
            this.setDistributionDirections(new ForgeDirection[]{ForgeDirection.getOrientation(this.worldObj.getBlockMetadata(xCoord, yCoord, zCoord)).getOpposite()});

            SteamNetwork.newOrJoin(this);
            this.getNetwork().addSteam(steam);
            return true;
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void displayWrench(Post event) {
        GL11.glPushMatrix();
        int color = Minecraft.getMinecraft().thePlayer.isSneaking() ? 0xC6C6C6 : 0x777777;
        int x = event.resolution.getScaledWidth() / 2 - 8;
        int y = event.resolution.getScaledHeight() / 2 - 8;
        Minecraft.getMinecraft().fontRenderer.drawStringWithShadow(StatCollector.translateToLocal("steamcraft.fan.range") + " " + this.range + " " + StatCollector.translateToLocal("steamcraft.fan.blocks"), x + 15, y + 13, color);
        GL11.glPopMatrix();
    }
}
