package mekanism.common.util;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import mcp.MethodsReturnNonnullByDefault;
import mekanism.api.Action;
import mekanism.api.DataHandlerUtils;
import mekanism.api.NBTConstants;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.chemical.gas.BasicGasTank;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.infuse.BasicInfusionTank;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.pigment.BasicPigmentTank;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.slurry.BasicSlurryTank;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.inventory.AutomationType;
import mekanism.api.providers.IChemicalProvider;
import mekanism.api.providers.IGasProvider;
import mekanism.api.providers.IInfuseTypeProvider;
import mekanism.api.providers.IPigmentProvider;
import mekanism.api.providers.ISlurryProvider;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.distribution.target.ChemicalHandlerTarget;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tier.ChemicalTankTier;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

/**
 * @apiNote This class is called ChemicalUtil instead of ChemicalUtils so that it does not overlap with {@link mekanism.api.chemical.ChemicalUtils}
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ChemicalUtil {

    public static <HANDLER extends IChemicalHandler<?, ?>> Capability<HANDLER> getCapabilityForChemical(Chemical<?> chemical) {
        if (chemical instanceof Gas) {
            return (Capability<HANDLER>) Capabilities.GAS_HANDLER_CAPABILITY;
        } else if (chemical instanceof InfuseType) {
            return (Capability<HANDLER>) Capabilities.INFUSION_HANDLER_CAPABILITY;
        } else if (chemical instanceof Pigment) {
            return (Capability<HANDLER>) Capabilities.PIGMENT_HANDLER_CAPABILITY;
        } else if (chemical instanceof Slurry) {
            return (Capability<HANDLER>) Capabilities.SLURRY_HANDLER_CAPABILITY;
        } else {
            throw new IllegalStateException("Unknown Chemical Type: " + chemical.getClass().getName());
        }
    }

    public static <HANDLER extends IChemicalHandler<?, ?>> Capability<HANDLER> getCapabilityForChemical(ChemicalStack<?> stack) {
        return getCapabilityForChemical(stack.getType());
    }

    public static <HANDLER extends IChemicalHandler<?, ?>> Capability<HANDLER> getCapabilityForChemical(IChemicalTank<?, ?> tank) {
        //Note: We just use getEmptyStack as it still has enough information
        return getCapabilityForChemical(tank.getEmptyStack());
    }

    /**
     * Helper to resize a chemical stack when we don't know what implementation it is.
     *
     * @param stack  Stack to copy
     * @param amount Desired size
     *
     * @return Copy of the input stack with the desired size
     *
     * @apiNote Should only be called if we know that copy returns STACK
     */
    public static <STACK extends ChemicalStack<?>> STACK copyWithAmount(STACK stack, long amount) {
        STACK copy = (STACK) stack.copy();
        copy.setAmount(amount);
        return copy;
    }

    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>, HANDLER extends IChemicalHandler<CHEMICAL, STACK>>
    HANDLER[] getConnectedAcceptors(BlockPos pos, @Nullable World world, Set<Direction> sides, Capability<HANDLER> capability) {
        HANDLER[] acceptors = (HANDLER[]) new IChemicalHandler[EnumUtils.DIRECTIONS.length];
        EmitUtils.forEachSide(world, pos, sides,
              (tile, side) -> CapabilityUtils.getCapability(tile, capability, side.getOpposite()).ifPresent(handler -> acceptors[side.ordinal()] = handler));
        return acceptors;
    }

    /**
     * Creates and returns a full chemical tank with the specified chemical type.
     *
     * @param chemical - chemical to fill the tank with
     *
     * @return filled gas tank
     */
    public static ItemStack getFullChemicalTank(ChemicalTankTier tier, @Nonnull Chemical<?> chemical) {
        ItemStack tankItem = getEmptyChemicalTank(tier);
        if (chemical instanceof Gas) {
            return getFilledVariant(tankItem, tier.getStorage(), (Gas) chemical);
        } else if (chemical instanceof InfuseType) {
            return getFilledVariant(tankItem, tier.getStorage(), (InfuseType) chemical);
        } else if (chemical instanceof Pigment) {
            return getFilledVariant(tankItem, tier.getStorage(), (Pigment) chemical);
        } else if (chemical instanceof Slurry) {
            return getFilledVariant(tankItem, tier.getStorage(), (Slurry) chemical);
        } else {
            throw new IllegalStateException("Unknown Chemical Type: " + chemical.getClass().getName());
        }
    }

    /**
     * Retrieves an empty Chemical Tank.
     *
     * @return empty chemical tank
     */
    private static ItemStack getEmptyChemicalTank(ChemicalTankTier tier) {
        switch (tier) {
            case BASIC:
                return MekanismBlocks.BASIC_GAS_TANK.getItemStack();
            case ADVANCED:
                return MekanismBlocks.ADVANCED_GAS_TANK.getItemStack();
            case ELITE:
                return MekanismBlocks.ELITE_GAS_TANK.getItemStack();
            case ULTIMATE:
                return MekanismBlocks.ULTIMATE_GAS_TANK.getItemStack();
            case CREATIVE:
                return MekanismBlocks.CREATIVE_GAS_TANK.getItemStack();
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack getFilledVariant(ItemStack toFill, long capacity, IGasProvider provider) {
        return getFilledVariant(toFill, BasicGasTank.createDummy(capacity), provider, NBTConstants.GAS_TANKS);
    }

    public static ItemStack getFilledVariant(ItemStack toFill, long capacity, IInfuseTypeProvider provider) {
        return getFilledVariant(toFill, BasicInfusionTank.createDummy(capacity), provider, NBTConstants.INFUSION_TANKS);
    }

    public static ItemStack getFilledVariant(ItemStack toFill, long capacity, IPigmentProvider provider) {
        return getFilledVariant(toFill, BasicPigmentTank.createDummy(capacity), provider, NBTConstants.PIGMENT_TANKS);
    }

    public static ItemStack getFilledVariant(ItemStack toFill, long capacity, ISlurryProvider provider) {
        return getFilledVariant(toFill, BasicSlurryTank.createDummy(capacity), provider, NBTConstants.SLURRY_TANKS);
    }

    private static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>, TANK extends IChemicalTank<CHEMICAL, STACK>>
    ItemStack getFilledVariant(ItemStack toFill, TANK dummyTank, IChemicalProvider<CHEMICAL> provider, String key) {
        //Manually handle filling it as capabilities are not necessarily loaded yet (at least not on the first call to this, which is made via fillItemGroup)
        dummyTank.setStack((STACK) provider.getStack(dummyTank.getCapacity()));
        ItemDataUtils.setList(toFill, key, DataHandlerUtils.writeContainers(Collections.singletonList(dummyTank)));
        //The item is now filled return it for convenience
        return toFill;
    }

    public static boolean hasGas(ItemStack stack) {
        return hasChemical(stack, s -> true, Capabilities.GAS_HANDLER_CAPABILITY);
    }

    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> boolean hasChemical(ItemStack stack, CHEMICAL type) {
        Capability<IChemicalHandler<CHEMICAL, STACK>> capability = getCapabilityForChemical(type);
        return hasChemical(stack, s -> s.isTypeEqual(type), capability);
    }

    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>, HANDLER extends IChemicalHandler<CHEMICAL, STACK>> boolean hasChemical(
          ItemStack stack, Predicate<STACK> validityCheck, Capability<HANDLER> capability) {
        Optional<HANDLER> cap = MekanismUtils.toOptional(stack.getCapability(capability));
        if (cap.isPresent()) {
            HANDLER handler = cap.get();
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                STACK chemicalStack = handler.getChemicalInTank(tank);
                if (!chemicalStack.isEmpty() && validityCheck.test(chemicalStack)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<ITextComponent> getAttributeTooltips(Chemical<?> chemical) {
        List<ITextComponent> list = new ArrayList<>();
        chemical.getAttributes().forEach(attr -> attr.addTooltipText(list));
        return list;
    }

    public static void emit(IChemicalTank<?, ?> tank, TileEntity from) {
        emit(EnumSet.allOf(Direction.class), tank, from);
    }

    public static void emit(Set<Direction> outputSides, IChemicalTank<?, ?> tank, TileEntity from) {
        emit(outputSides, tank, from, tank.getCapacity());
    }

    public static void emit(Set<Direction> outputSides, IChemicalTank<?, ?> tank, TileEntity from, long maxOutput) {
        if (!tank.isEmpty() && maxOutput > 0) {
            tank.extract(emit(outputSides, tank.extract(maxOutput, Action.SIMULATE, AutomationType.INTERNAL), from), Action.EXECUTE, AutomationType.INTERNAL);
        }
    }

    /**
     * Emits chemical from a central block by splitting the received stack among the sides given.
     *
     * @param sides - the list of sides to output from
     * @param stack - the stack to output
     * @param from  - the TileEntity to output from
     *
     * @return the amount of chemical emitted
     */
    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>> long emit(Set<Direction> sides, @Nonnull STACK stack, TileEntity from) {
        if (stack.isEmpty() || sides.isEmpty()) {
            return 0;
        }
        Capability<IChemicalHandler<CHEMICAL, STACK>> capability = getCapabilityForChemical(stack);
        //Fake that we have one target given we know that no sides will overlap
        // This allows us to have slightly better performance
        ChemicalHandlerTarget<CHEMICAL, STACK, IChemicalHandler<CHEMICAL, STACK>> target = new ChemicalHandlerTarget<>(stack);
        EmitUtils.forEachSide(from.getWorld(), from.getPos(), sides, (acceptor, side) -> {
            //Insert to access side
            Direction accessSide = side.getOpposite();
            //Collect cap
            CapabilityUtils.getCapability(acceptor, capability, accessSide).ifPresent(handler -> {
                if (canInsert(handler, stack)) {
                    target.addHandler(accessSide, handler);
                }
            });
        });
        int curHandlers = target.getHandlers().size();
        if (curHandlers > 0) {
            Set<ChemicalHandlerTarget<CHEMICAL, STACK, IChemicalHandler<CHEMICAL, STACK>>> targets = new ObjectOpenHashSet<>();
            targets.add(target);
            return EmitUtils.sendToAcceptors(targets, curHandlers, stack.getAmount(), (STACK) stack.copy());
        }
        return 0;
    }

    public static <CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>, HANDLER extends IChemicalHandler<CHEMICAL, STACK>> boolean canInsert(
          HANDLER handler, @Nonnull STACK stack) {
        return handler.insertChemical(stack, Action.SIMULATE).getAmount() < stack.getAmount();
    }
}