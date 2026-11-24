package com.titammods.quantumcubes.compat.jade;

import com.titammods.quantumcubes.block.QuantumCubeBlock;
import com.titammods.quantumcubes.block.entity.QuantumCubeBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class QuantumJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(CubeJadeProvider.INSTANCE, QuantumCubeBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(CubeJadeClientProvider.INSTANCE, QuantumCubeBlock.class);
    }
}