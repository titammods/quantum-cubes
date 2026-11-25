package com.titammods.quantumcubes.block.entity;

import com.titammods.quantumcubes.block.CubeTier;
import com.titammods.quantumcubes.block.QuantumCubeBlock;
import com.titammods.quantumcubes.dimension.QuantumDimension;
import com.titammods.quantumcubes.registry.ModBlockEntities;
import com.titammods.quantumcubes.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class QuantumCubeBlockEntity extends BlockEntity implements Nameable {

    private boolean generated = false;
    private int cubeIndex = -1;
    private boolean suppressDrop = false;
    @Nullable
    private Component customName;

    public QuantumCubeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUANTUM_CUBE_BE.get(), pos, state);
    }

    public boolean isGenerated() {
        return generated;
    }

    public int getCubeIndex() {
        return cubeIndex;
    }

    public void setCube(int index) {
        this.cubeIndex = index;
        this.generated = true;
        setChanged();
    }

    public void suppressDrop() {
        this.suppressDrop = true;
    }

    @Override
    public Component getName() {
        Component name = getCustomName();
        return name != null ? name : getBlockState().getBlock().getName();
    }

    @Override
    @Nullable
    public Component getCustomName() {
        if (customName != null) {
            return customName;
        }
        return components().get(DataComponents.CUSTOM_NAME);
    }

    public CubeTier getTier() {
        if (getBlockState().getBlock() instanceof QuantumCubeBlock block) {
            return block.getTier();
        }
        return CubeTier.TIER_1;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        Integer index = components.get(ModDataComponents.CUBE_INDEX.get());
        components.get(ModDataComponents.CUBE_INTERIOR.get());
        this.customName = components.get(DataComponents.CUSTOM_NAME);
        if (index != null) {
            this.cubeIndex = index;
            this.generated = true;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        Component name = getCustomName();
        if (name != null) {
            builder.set(DataComponents.CUSTOM_NAME, name);
        }
        if (!generated) {
            return;
        }
        builder.set(ModDataComponents.CUBE_INDEX.get(), cubeIndex);
        if (level instanceof ServerLevel serverLevel) {
            builder.set(ModDataComponents.CUBE_INTERIOR.get(),
                    QuantumDimension.interiorOf(serverLevel.getServer(), cubeIndex, getTier()));
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (suppressDrop || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        ItemStack stack = new ItemStack(state.getBlock());
        stack.applyComponents(collectComponents());
        Block.popResource(serverLevel, pos, stack);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("generated", generated);
        output.putInt("cube_index", cubeIndex);
        output.storeNullable("CustomName", ComponentSerialization.CODEC, customName);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.generated = input.getBooleanOr("generated", false);
        this.cubeIndex = input.getIntOr("cube_index", -1);
        this.customName = input.read("CustomName", ComponentSerialization.CODEC).orElse(null);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("CustomName");
    }
}