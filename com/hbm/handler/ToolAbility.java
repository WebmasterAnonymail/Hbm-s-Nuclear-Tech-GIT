package com.hbm.handler;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hbm.inventory.MachineRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemToolAbility;
import com.hbm.render.amlfrom1710.Vec3;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class ToolAbility {
	
	public abstract void onDig(World world, int x, int y, int z, EntityPlayer player, IBlockState block, ItemToolAbility tool, EnumHand hand);
	public abstract String getName();
	@SideOnly(Side.CLIENT)
	public abstract String getFullName();
	
	public static class RecursionAbility extends ToolAbility {
		
		int radius;
		
		public RecursionAbility(int radius) {
			this.radius = radius;
		}
		
		private Set<BlockPos> pos = new HashSet<BlockPos>();

		@Override
		public void onDig(World world, int x, int y, int z, EntityPlayer player, IBlockState block, ItemToolAbility tool, EnumHand hand) {
			
			List<Integer> indices = Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5});
			Collections.shuffle(indices);
			
			pos.clear();
			
			for(Integer i : indices) {
				switch(i) {
				case 0: breakExtra(world, x + 1, y, z, x, y, z, player, tool, hand); break;
				case 1: breakExtra(world, x - 1, y, z, x, y, z, player, tool, hand); break;
				case 2: breakExtra(world, x, y + 1, z, x, y, z, player, tool, hand); break;
				case 3: breakExtra(world, x, y - 1, z, x, y, z, player, tool, hand); break;
				case 4: breakExtra(world, x, y, z + 1, x, y, z, player, tool, hand); break;
				case 5: breakExtra(world, x, y, z - 1, x, y, z, player, tool, hand); break;
				}
			}
		}
		
		private void breakExtra(World world, int x, int y, int z, int refX, int refY, int refZ, EntityPlayer player, ItemToolAbility tool, EnumHand hand) {
			
			if(pos.contains(new BlockPos(x, y, z)))
				return;
			
			pos.add(new BlockPos(x, y, z));
			
			//don't lose the ref block just yet
			if(x == refX && y == refY && z == refZ)
				return;
			
			if(Vec3.createVectorHelper(x - refX, y - refY, z - refZ).lengthVector() > radius)
				return;
			
			IBlockState b = world.getBlockState(new BlockPos(x, y, z));
			IBlockState ref = world.getBlockState(new BlockPos(refX, refY, refZ));
			
			if(b != ref)
				return;
			
			if(player.getHeldItem(hand).isEmpty())
				return;
			
			tool.breakExtraBlock(world, x, y, z, player, refX, refY, refZ, hand);
			
			List<Integer> indices = Arrays.asList(new Integer[] {0, 1, 2, 3, 4, 5});
			Collections.shuffle(indices);
			
			for(Integer i : indices) {
				switch(i) {
				case 0: breakExtra(world, x + 1, y, z, refX, refY, refZ, player, tool, hand); break;
				case 1: breakExtra(world, x - 1, y, z, refX, refY, refZ, player, tool, hand); break;
				case 2: breakExtra(world, x, y + 1, z, refX, refY, refZ, player, tool, hand); break;
				case 3: breakExtra(world, x, y - 1, z, refX, refY, refZ, player, tool, hand); break;
				case 4: breakExtra(world, x, y, z + 1, refX, refY, refZ, player, tool, hand); break;
				case 5: breakExtra(world, x, y, z - 1, refX, refY, refZ, player, tool, hand); break;
				}
			}
		}

		@Override
		public String getName() {
			return "tool.ability.recursion";
		}

		@Override
		@SideOnly(Side.CLIENT)
		public String getFullName() {
			return I18n.format(getName()) + " (" + radius + ")";
		}
		
	}

	public static class HammerAbility extends ToolAbility {

		int range;
		
		public HammerAbility(int range) {
			this.range = range;
		}
		
		@Override
		public void onDig(World world, int x, int y, int z, EntityPlayer player, IBlockState block, ItemToolAbility tool, EnumHand hand) {
			
			for(int a = x - range; a <= x + range; a++) {
				for(int b = y - range; b <= y + range; b++) {
					for(int c = z - range; c <= z + range; c++) {
						
						if(a == x && b == y && c == z)
							continue;
						
						tool.breakExtraBlock(world, a, b ,c, player, x, y, z, hand);
					}
				}
			}
		}

		@Override
		public String getName() {
			return "tool.ability.hammer";
		}

		@Override
		@SideOnly(Side.CLIENT)
		public String getFullName() {
			return I18n.format(getName()) + " (" + range + ")";
		}
	}

	public static class SmelterAbility extends ToolAbility {

		@Override
		public void onDig(World world, int x, int y, int z, EntityPlayer player, IBlockState block, ItemToolAbility tool, EnumHand hand) {
			
			//a band-aid on a gaping wound
			if(block.getBlock() == Blocks.LIT_REDSTONE_ORE)
				block = Blocks.REDSTONE_ORE.getDefaultState();
			
			ItemStack stack = new ItemStack(block.getBlock(), 1, block.getBlock().getMetaFromState(block));
			ItemStack result = FurnaceRecipes.instance().getSmeltingResult(stack);
			
			if(result != null) {
				world.setBlockToAir(new BlockPos(x, y, z));
				world.spawnEntity(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, result.copy()));
			}
		}

		@Override
		public String getName() {
			return "tool.ability.smelter";
		}

		@Override
		@SideOnly(Side.CLIENT)
		public String getFullName() {
			return I18n.format(getName());
		}
	}
	
	public static class ShredderAbility extends ToolAbility {

		@Override
		public void onDig(World world, int x, int y, int z, EntityPlayer player, IBlockState block, ItemToolAbility tool, EnumHand hand) {
			
			//a band-aid on a gaping wound
			if(block.getBlock() == Blocks.LIT_REDSTONE_ORE)
				block = Blocks.REDSTONE_ORE.getDefaultState();
			
			ItemStack stack = new ItemStack(block.getBlock(), 1, block.getBlock().getMetaFromState(block));
			ItemStack result = MachineRecipes.getShredderResult(stack);
			
			if(result != null && result.getItem() != ModItems.scrap) {
				world.setBlockToAir(new BlockPos(x, y, z));
				world.spawnEntity(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, result.copy()));
			}
		}

		@Override
		public String getName() {
			return "tool.ability.shredder";
		}

		@Override
		@SideOnly(Side.CLIENT)
		public String getFullName() {
			return I18n.format(getName());
		}
	}
	
	public static class CentrifugeAbility extends ToolAbility {

		@Override
		public void onDig(World world, int x, int y, int z, EntityPlayer player, IBlockState block, ItemToolAbility tool, EnumHand hand) {
			
			//a band-aid on a gaping wound
			if(block.getBlock() == Blocks.LIT_REDSTONE_ORE)
				block = Blocks.REDSTONE_ORE.getDefaultState();
			
			ItemStack stack = new ItemStack(block.getBlock(), 1, block.getBlock().getMetaFromState(block));
			ItemStack[] result = MachineRecipes.getCentrifugeProcessingResult(stack);
			
			if(result != null) {
				world.setBlockToAir(new BlockPos(x, y, z));
				
				for(ItemStack st : result) {
					if(st != null)
						world.spawnEntity(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, st.copy()));
				}
			}
		}

		@Override
		public String getName() {
			return "tool.ability.centrifuge";
		}

		@Override
		@SideOnly(Side.CLIENT)
		public String getFullName() {
			return I18n.format(getName());
		}
	}
}