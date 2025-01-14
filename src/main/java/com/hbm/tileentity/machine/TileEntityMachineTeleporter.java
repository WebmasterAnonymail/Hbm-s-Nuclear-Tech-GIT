package com.hbm.tileentity.machine;

import java.util.List;

import com.hbm.interfaces.IConsumer;
import com.hbm.lib.ModDamageSource;
import com.hbm.packet.AuxElectricityPacket;
import com.hbm.packet.PacketDispatcher;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;

public class TileEntityMachineTeleporter extends TileEntity implements ITickable, IConsumer {

	public long power = 0;
	public BlockPos target = null;
	public boolean linked = false;
	// true: send; false: receive
	public boolean mode = false;
	public static final int maxPower = 100000;

	@Override
	public void readFromNBT(NBTTagCompound compound) {
		power = compound.getLong("power");
		if(compound.getBoolean("hastarget")) {
			int x = compound.getInteger("x1");
			int y = compound.getInteger("y1");
			int z = compound.getInteger("z1");
			target = new BlockPos(x, y, z);
		}
		linked = compound.getBoolean("linked");
		mode = compound.getBoolean("mode");
		super.readFromNBT(compound);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setLong("power", power);
		if(target != null) {
			compound.setBoolean("hastarget", true);
			compound.setInteger("x1", target.getX());
			compound.setInteger("y1", target.getY());
			compound.setInteger("z1", target.getZ());
		} else {
			compound.setBoolean("hastarget", false);
		}
		compound.setBoolean("linked", linked);
		compound.setBoolean("mode", mode);
		return super.writeToNBT(compound);
	}

	@Override
	public void update() {
		boolean b0 = false;

		if(!this.world.isRemote) {
			List<Entity> entities = this.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(pos.getX() - 0.25, pos.getY(), pos.getZ() - 0.25, pos.getX() + 1.5, pos.getY() + 2, pos.getZ() + 1.5));
			if(!entities.isEmpty())
				for(Entity e : entities) {
					if(e.ticksExisted >= 10) {
						teleport(e);
						b0 = true;
					}
				}

			PacketDispatcher.wrapper.sendToAllAround(new AuxElectricityPacket(pos, power), new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 10));
		}

		if(b0)
			world.spawnParticle(EnumParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 0.0D, 0.1D, 0.0D);
	}
	
	public void teleport(Entity entity) {

		if (!this.linked || !this.mode || this.power < 50000 || target == null)
			return;

		TileEntity te = this.world.getTileEntity(target);

		if (te == null || !(te instanceof TileEntityMachineTeleporter) || ((TileEntityMachineTeleporter) te).mode) {
			entity.attackEntityFrom(ModDamageSource.teleporter, 10000);
		} else {
			if ((entity instanceof EntityPlayerMP)) {
				((EntityPlayerMP) entity).setPositionAndUpdate(target.getX() + 0.5D,
						target.getY() + 1.5D + entity.getYOffset(), target.getZ() + 0.5D);
			} else {
				entity.setPositionAndRotation(target.getX() + 0.5D, target.getY() + 1.5D + entity.getYOffset(),
						target.getZ() + 0.5D, entity.rotationYaw, entity.rotationPitch);
			}
		}
		
		this.power -= 50000;
	}

	@Override
	public void setPower(long i) {
		power = i;
	}

	@Override
	public long getPower() {
		return power;
	}

	@Override
	public long getMaxPower() {
		return maxPower;
	}

}
