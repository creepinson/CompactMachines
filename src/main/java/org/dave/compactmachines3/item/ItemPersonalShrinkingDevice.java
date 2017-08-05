package org.dave.compactmachines3.item;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.dave.compactmachines3.CompactMachines3;
import org.dave.compactmachines3.block.BlockMiniaturizationFluid;
import org.dave.compactmachines3.init.Blockss;
import org.dave.compactmachines3.init.Fluidss;
import org.dave.compactmachines3.item.psd.PSDCapabilityProvider;
import org.dave.compactmachines3.item.psd.PSDFluidStorage;
import org.dave.compactmachines3.misc.ConfigurationHandler;
import org.dave.compactmachines3.misc.CreativeTabCompactMachines3;
import org.dave.compactmachines3.reference.GuiIds;
import org.dave.compactmachines3.utility.TextFormattingHelper;
import org.dave.compactmachines3.world.WorldSavedDataMachines;
import org.dave.compactmachines3.world.tools.StructureTools;
import org.dave.compactmachines3.world.tools.TeleportationTools;

import javax.annotation.Nullable;
import java.util.List;

public class ItemPersonalShrinkingDevice extends ItemBase {
    public ItemPersonalShrinkingDevice() {
        super();

        this.setCreativeTab(CreativeTabCompactMachines3.COMPACTMACHINES3_TAB);
        this.setMaxStackSize(1);
        this.setMaxDamage(Fluid.BUCKET_VOLUME * 4);
    }

    @SideOnly(Side.CLIENT)
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(getRegistryName(), "inventory"));
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        PSDFluidStorage tank = (PSDFluidStorage) stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
        return 1D - ((double)tank.getFluidAmount() / (double)tank.getCapacity());
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
        return new PSDCapabilityProvider(stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        PSDFluidStorage tank = (PSDFluidStorage) stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);

        String chargeToolTip = I18n.format("tooltip." + CompactMachines3.MODID + ".psd.charge", tank.getFluidAmount() * 100 / tank.getCapacity());
        tooltip.add(TextFormattingHelper.colorizeKeyValue(chargeToolTip));

        if(GuiScreen.isShiftKeyDown()) {
            tooltip.add(TextFormatting.YELLOW + I18n.format("tooltip." + CompactMachines3.MODID + ".psd.hint"));
        }
    }

    @Override
    public boolean isDamageable() {
        return true;
    }

    @Override
    public boolean isRepairable() {
        return false;
    }

    @Override
    public boolean isDamaged(ItemStack stack) {
        PSDFluidStorage tank = (PSDFluidStorage) stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
        if(tank.getFluidAmount() == 0) {
            return false;
        }
        return true;
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if(hand == EnumHand.OFF_HAND) {
            return new ActionResult(EnumActionResult.FAIL, stack);
        }

        PSDFluidStorage tank = (PSDFluidStorage) stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
        if (tank.getFluidAmount() <= 3500) {
            RayTraceResult raytraceresult = this.rayTrace(world, player, true);
            if (raytraceresult != null && raytraceresult.typeOfHit == RayTraceResult.Type.BLOCK) {
                BlockPos tracepos = raytraceresult.getBlockPos();
                IBlockState state = world.getBlockState(tracepos);
                boolean allowed = world.isBlockModifiable(player, tracepos) && player.canPlayerEdit(tracepos.offset(raytraceresult.sideHit), raytraceresult.sideHit, stack);

                if (allowed && state.getBlock() == Blockss.miniaturizationFluidBlock && state.getValue(BlockMiniaturizationFluid.LEVEL).intValue() == 0) {
                    world.setBlockState(tracepos, Blocks.AIR.getDefaultState(), 11);
                    player.playSound(SoundEvents.ITEM_BUCKET_FILL, 1.0F, 1.0F);

                    tank.fill(new FluidStack(Fluidss.miniaturizationFluid, 1000), true);
                    return new ActionResult(EnumActionResult.SUCCESS, stack);
                }
            }
        }

        if(world.provider.getDimension() != ConfigurationHandler.Settings.dimensionId) {
            player.openGui(CompactMachines3.instance, GuiIds.PSD_WELCOME.ordinal(), world, (int) player.posX, (int) player.posY, (int) player.posZ);
            return new ActionResult(EnumActionResult.SUCCESS, stack);
        }

        if(!world.isRemote && world.provider.getDimension() == ConfigurationHandler.Settings.dimensionId && player instanceof EntityPlayerMP) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP)player;

            if(player.isSneaking()) {
                int coords = StructureTools.getCoordsForPos(player.getPosition());
                Vec3d pos = player.getPositionVector();
                WorldSavedDataMachines.INSTANCE.addSpawnPoint(coords, pos.x, pos.y, pos.z);

                TextComponentTranslation tc = new TextComponentTranslation("item.compactmachines3.psd.spawnpoint_set");
                tc.getStyle().setColor(TextFormatting.GREEN);
                player.sendStatusMessage(tc, false);

                return new ActionResult(EnumActionResult.SUCCESS, stack);
            }

            TeleportationTools.teleportPlayerOutOfMachine(serverPlayer);
            return new ActionResult(EnumActionResult.SUCCESS, stack);
        }

        return new ActionResult(EnumActionResult.FAIL, stack);
    }



}
