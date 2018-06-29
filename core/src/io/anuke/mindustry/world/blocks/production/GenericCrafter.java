package io.anuke.mindustry.world.blocks.production;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockBar;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.mindustry.world.meta.StatUnit;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GenericCrafter extends Block{
	protected final int timerDump = timers++;
	
	/**Can be null. If you use this, make sure to set hasItems to true!*/
	protected ItemStack inputItem;
	/**Can be null. If you use this, make sure to set hasLiquids to true!*/
	protected Liquid inputLiquid;
	/**Required.*/
	protected Item output;
	protected float craftTime = 80;
	protected float powerUse;
	protected float liquidUse;
	protected Effect craftEffect = BlockFx.purify;
	protected Effect updateEffect = Fx.none;
	protected float updateEffectChance = 0.04f;

	public GenericCrafter(String name) {
		super(name);
		update = true;
		solid = true;
		health = 60;
	}

	@Override
	public void setBars(){
		super.setBars();

		if(inputItem != null) bars.replace(new BlockBar(BarType.inventory, true,
				tile -> (float)tile.entity.items.getItem(inputItem.item) / itemCapacity));
	}
	
	@Override
	public void setStats(){
		super.setStats();
		stats.add(BlockStat.craftSpeed, 60f/craftTime, StatUnit.itemsSecond);
		stats.add(BlockStat.outputItem, output);

		if(inputLiquid != null) stats.add(BlockStat.inputLiquid, inputLiquid);
		if(inputLiquid != null) stats.add(BlockStat.liquidUse, (liquidUse * craftTime), StatUnit.liquidSecond);
		if(inputItem != null) stats.add(BlockStat.inputItem, inputItem);
		if(hasPower) stats.add(BlockStat.powerUse, powerUse * 60f, StatUnit.powerSecond);
	}
	
	@Override
	public void draw(Tile tile){
		Draw.rect(name(), tile.drawx(), tile.drawy());
		
		if(!hasLiquids) return;
		
		Draw.color(tile.entity.liquids.liquid.color);
		Draw.alpha(tile.entity.liquids.amount / liquidCapacity);
		Draw.rect("blank", tile.drawx(), tile.drawy(), 2, 2);
		Draw.color();
	}

	@Override
	public TextureRegion[] getIcon(){
		return new TextureRegion[]{Draw.region(name)};
	}
	
	@Override
	public void update(Tile tile){
		GenericCrafterEntity entity = tile.entity();

		float powerUsed = Math.min(powerCapacity, powerUse * Timers.delta());
		float liquidUsed = Math.min(liquidCapacity, liquidUse * Timers.delta());
		int itemsUsed = (inputItem == null ? 0 : (int)(1 + inputItem.amount * entity.progress));

		if((!hasLiquids || entity.liquids.amount >= liquidUsed) &&
				(!hasPower || entity.power.amount >= powerUsed) &&
				(inputItem == null || entity.items.hasItem(inputItem.item, itemsUsed))){

			entity.progress += 1f / craftTime * Timers.delta();
			entity.totalProgress += Timers.delta();
			entity.warmup = Mathf.lerp(entity.warmup, 1f, 0.02f);
			if(hasPower) entity.power.amount -= powerUsed;
			if(hasLiquids) entity.liquids.amount -= liquidUsed;

			if(Mathf.chance(Timers.delta() * updateEffectChance))
				Effects.effect(updateEffect, entity.x + Mathf.range(size*4f), entity.y + Mathf.range(size*4));
		}else{
			entity.warmup = Mathf.lerp(entity.warmup, 0f, 0.02f);
		}

		if(entity.progress >= 1f){
			
			if(inputItem != null) tile.entity.items.removeItem(inputItem);
			offloadNear(tile, output);
			Effects.effect(craftEffect, tile.drawx(), tile.drawy());
			entity.progress = 0f;
		}
		
		if(tile.entity.timer.get(timerDump, 5)){
			tryDump(tile, output);
		}
	}
	
	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return super.acceptLiquid(tile, source, liquid, amount) && liquid == inputLiquid;
	}
	
	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		TileEntity entity = tile.entity();
		return inputItem != null && item == inputItem.item && entity.items.getItem(inputItem.item) < itemCapacity;
	}

	@Override
	public TileEntity getEntity() {
		return new GenericCrafterEntity();
	}

	public static class GenericCrafterEntity extends TileEntity{
		public float progress;
		public float totalProgress;
		public float warmup;

		@Override
		public void write(DataOutputStream stream) throws IOException {
			stream.writeFloat(progress);
			stream.writeFloat(warmup);
		}

		@Override
		public void read(DataInputStream stream) throws IOException {
			progress = stream.readFloat();
			warmup = stream.readFloat();
		}
	}
}
