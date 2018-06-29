package io.anuke.mindustry.world.meta;

import io.anuke.ucore.util.Bundles;

/**Describes one type of stat for a block.*/
public enum BlockStat {
    health(StatCategory.general),
    size(StatCategory.general),

    itemCapacity(StatCategory.items),
    inputItemCapacity(StatCategory.items),
    outputItemCapacity(StatCategory.items),
    itemSpeed(StatCategory.items),

    liquidCapacity(StatCategory.liquids),
    liquidOutput(StatCategory.liquids),

    powerCapacity(StatCategory.power),
    powerUse(StatCategory.power),
    powerRange(StatCategory.power),
    powerTransferSpeed(StatCategory.power),
    maxPowerGeneration(StatCategory.power),

    inputLiquid(StatCategory.crafting),
    inputLiquidAux(StatCategory.crafting),
    liquidUse(StatCategory.crafting),
    inputItem(StatCategory.crafting),
    inputItems(StatCategory.crafting),
    inputFuel(StatCategory.crafting),
    fuelBurnTime(StatCategory.crafting),
    craftSpeed(StatCategory.crafting),
    outputItem(StatCategory.crafting),
    drillTier(StatCategory.crafting),
    drillSpeed(StatCategory.crafting),

    shootRange(StatCategory.shooting),
    inaccuracy(StatCategory.shooting),
    shots(StatCategory.shooting),
    reload(StatCategory.shooting),
    powerShot(StatCategory.shooting),

    ;


    public final StatCategory category;

    BlockStat(StatCategory category) {
        this.category = category;
    }

    public String localized(){
        return Bundles.get("text.blocks." + name().toLowerCase());
    }
}
