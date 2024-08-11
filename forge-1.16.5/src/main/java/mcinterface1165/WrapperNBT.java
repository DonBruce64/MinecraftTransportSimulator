package mcinterface1165;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;

import java.util.*;

class WrapperNBT implements IWrapperNBT {
    protected final NbtCompound compound;

    protected WrapperNBT() {
        this.compound = new NbtCompound();
    }

    protected WrapperNBT(NbtCompound compound) {
        this.compound = compound;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WrapperNBT && compound.equals(((WrapperNBT) obj).compound);
    }

    @Override
    public boolean getBoolean(String name) {
        return compound.getBoolean(name);
    }

    @Override
    public void setBoolean(String name, boolean value) {
        if (value) {
            compound.putBoolean(name, value);
        } else {
            compound.remove(name);
        }
    }

    @Override
    public int getInteger(String name) {
        return compound.getInt(name);
    }

    @Override
    public void setInteger(String name, int value) {
        if (value != 0) {
            compound.putInt(name, value);
        } else {
            compound.remove(name);
        }
    }

    @Override
    public double getDouble(String name) {
        return compound.getDouble(name);
    }

    @Override
    public void setDouble(String name, double value) {
        if (value != 0) {
            compound.putDouble(name, value);
        } else {
            compound.remove(name);
        }
    }

    @Override
    public String getString(String name) {
        return compound.getString(name);
    }

    @Override
    public void setString(String name, String value) {
        compound.putString(name, value);
    }

    @Override
    public List<String> getStrings(String name) {
        return getStrings(name, getInteger(name + "count"));
    }

    @Override
    public List<String> getStrings(String name, int count) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            values.add(getString(name + i));
        }
        return values;
    }

    @Override
    public void setStrings(String name, Collection<String> values) {
        setInteger(name + "count", values.size());
        int index = 0;
        for (String value : values) {
            setString(name + index++, value);
        }
    }

    @Override
    public UUID getUUID(String name) {
        return compound.contains(name) ? UUID.fromString(compound.getString(name)) : null;
    }

    @Override
    public void setUUID(String name, UUID value) {
        compound.putString(name, value.toString());
    }

    @Override
    public List<IWrapperItemStack> getStacks(int count) {
        List<IWrapperItemStack> stacks = new ArrayList<>();
        DefaultedList<ItemStack> mcList = DefaultedList.ofSize(count, ItemStack.EMPTY);
        Inventories.readNbt(compound, mcList);
        for (ItemStack stack : mcList) {
            stacks.add(new WrapperItemStack(stack));
        }
        return stacks;
    }

    @Override
    public void setStacks(List<IWrapperItemStack> stacks) {
        DefaultedList<ItemStack> mcList = DefaultedList.of();
        for (IWrapperItemStack stack : stacks) {
            mcList.add(((WrapperItemStack) stack).stack);
        }
        Inventories.writeNbt(compound, mcList);
    }

    @Override
    public Point3D getPoint3d(String name) {
        return new Point3D(getDouble(name + "x"), getDouble(name + "y"), getDouble(name + "z"));
    }

    @Override
    public void setPoint3d(String name, Point3D value) {
        if (!value.isZero()) {
            setDouble(name + "x", value.x);
            setDouble(name + "y", value.y);
            setDouble(name + "z", value.z);
        }
    }

    @Override
    public List<Point3D> getPoint3ds(String name) {
        List<Point3D> values = new ArrayList<>();
        int count = getInteger(name + "count");
        for (int i = 0; i < count; ++i) {
            Point3D point = getPoint3d(name + i);
            if (!point.isZero()) {
                values.add(point);
            }
        }
        return values;
    }

    @Override
    public void setPoint3ds(String name, Collection<Point3D> values) {
        setInteger(name + "count", values.size());
        int index = 0;
        for (Point3D value : values) {
            setPoint3d(name + index++, value);
        }
    }

    @Override
    public Point3D getPoint3dCompact(String name) {
        return new Point3D(getInteger(name + "x"), getInteger(name + "y"), getInteger(name + "z"));
    }

    @Override
    public void setPoint3dCompact(String name, Point3D value) {
        if (!value.isZero()) {
            setInteger(name + "x", (int) Math.floor(value.x));
            setInteger(name + "y", (int) Math.floor(value.y));
            setInteger(name + "z", (int) Math.floor(value.z));
        }
    }

    @Override
    public List<Point3D> getPoint3dsCompact(String name) {
        List<Point3D> values = new ArrayList<>();
        int count = getInteger(name + "count");
        for (int i = 0; i < count; ++i) {
            Point3D point = getPoint3dCompact(name + i);
            if (!point.isZero()) {
                values.add(point);
            }
        }
        return values;
    }

    @Override
    public void setPoint3dsCompact(String name, Collection<Point3D> values) {
        setInteger(name + "count", values.size());
        int index = 0;
        for (Point3D value : values) {
            setPoint3dCompact(name + index++, value);
        }
    }

    @Override
    public WrapperNBT getData(String name) {
        return compound.contains(name, 10) ? new WrapperNBT(compound.getCompound(name)) : null;
    }

    @Override
    public void setData(String name, IWrapperNBT value) {
        compound.put(name, ((WrapperNBT) value).compound);
    }

    @Override
    public boolean hasKey(String name) {
        return compound.contains(name);
    }

    @Override
    public void deleteEntry(String name) {
        compound.remove(name);
    }

    @Override
    public Set<String> getAllNames() {
        return compound.getKeys();
    }
}
