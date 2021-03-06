package eiteam.esteemedinnovation.materials.raw;

import eiteam.esteemedinnovation.commons.Config;
import eiteam.esteemedinnovation.commons.util.WorldHelper;
import net.minecraft.block.BlockFurnace;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

import static eiteam.esteemedinnovation.materials.MaterialsModule.ORE_DEPOSIT_GENERATOR;
import static eiteam.esteemedinnovation.materials.MaterialsModule.WORKED_OUT_ORE_DEPOSIT_LOOTTABLE;

public class SurfaceDepositOreGenerator implements IWorldGenerator {
    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (world.provider.getDimension() == 0) {
            boolean doCopperDeposit = random.nextInt(16) == 1;
            boolean doZincDeposit = random.nextInt(16) == 2;
            int coordX = chunkX * 16;
            int coordZ = chunkZ * 16;
            if (doCopperDeposit) {
                if (Config.genCopperOverworldDeposits) {
                    generateDepositGenerators(random, coordX, coordZ, world, BlockOreDepositGenerator.Types.COPPER);
                }
            } else if (doZincDeposit) {
                if (Config.genZincOverworldDeposits) {
                    generateDepositGenerators(random, coordX, coordZ, world, BlockOreDepositGenerator.Types.ZINC);
                }
            }
        }
    }

    private static void generateDepositGenerators(Random random, int baseX, int baseZ, World world, BlockOreDepositGenerator.Types type) {
        // 30% of deposits are worked out
        boolean workedOut = random.nextInt(10) <= 3;
        WorldGenSingleMinable minable = new WorldGenSingleMinable(ORE_DEPOSIT_GENERATOR.getDefaultState()
          .withProperty(BlockOreDepositGenerator.VARIANT, type)
          .withProperty(BlockOreDepositGenerator.WORKED_OUT, workedOut));
        int baseY = random.nextInt(40);
        for (int i = 0; i < 9; i++) {
            boolean generate = i <= 5 || random.nextBoolean();
            if (!generate) {
                continue;
            }
            BlockPos pos = new BlockPos(baseX + random.nextInt(16), baseY + random.nextInt(16), baseZ + random.nextInt(16));
            boolean generated = minable.generate(world, random, pos);
            if (!workedOut || !generated) {
                continue;
            }
            if (!random.nextBoolean()) {
                boolean genChest = random.nextBoolean();
                boolean genBench = random.nextBoolean();
                boolean genSmelt = random.nextBoolean();
                for (EnumFacing dir : EnumFacing.HORIZONTALS) {
                    BlockPos offset = pos.offset(dir, random.nextInt(3) + 1);
                    if (BlockOreDepositGenerator.canReplace(offset, world)) {
                        continue;
                    }
                    if (genChest) {
                        world.setTileEntity(offset, new TileEntityChest());
                        world.setBlockState(offset, Blocks.CHEST.correctFacing(world, offset, Blocks.CHEST.getDefaultState()));
                        TileEntity tileentity = world.getTileEntity(offset);
                        if (tileentity instanceof TileEntityChest) {
                            TileEntityChest chest = (TileEntityChest) tileentity;
                            chest.setLootTable(WORKED_OUT_ORE_DEPOSIT_LOOTTABLE, random.nextLong());
                        }
                        genChest = false;
                    } else if (genBench) {
                        world.setBlockState(offset, Blocks.CRAFTING_TABLE.getDefaultState());
                        genBench = false;
                    } else if (genSmelt) {
                        world.setBlockState(offset,
                          Blocks.FURNACE.getDefaultState().withProperty(BlockFurnace.FACING, WorldHelper.randomHorizontal(random)));
                        genSmelt = false;
                    }
                }
            }

            BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos(pos);
            for (int x = -3; x < 4; x++) {
                for (int y = 0; y < 2; y++) {
                    for (int z = -3; z < 4; z++) {
                        mutablePos.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                        if (BlockOreDepositGenerator.canReplace(mutablePos, world)) {
                            world.setBlockToAir(mutablePos);
                        }
                    }
                }
            }
        }
    }
}
