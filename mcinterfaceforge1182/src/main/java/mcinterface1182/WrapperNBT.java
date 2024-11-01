package mcinterface1182;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

class WrapperNBT implements IWrapperNBT {
    protected final CompoundTag tag;

    protected WrapperNBT() {
        this.tag = new CompoundTag();
    }

    protected WrapperNBT(CompoundTag tag) {
        this.tag = tag;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WrapperNBT && tag.equals(((WrapperNBT) obj).tag);
    }

    @Override
    public boolean getBoolean(String name) {
        return tag.getBoolean(name);
    }

    @Override
    public void setBoolean(String name, boolean value) {
        if (value) {
            tag.putBoolean(name, value);
        } else {
            tag.remove(name);
        }
    }

    @Override
    public int getInteger(String name) {
        return tag.getInt(name);
    }

    @Override
    public void setInteger(String name, int value) {
        if (value != 0) {
            tag.putInt(name, value);
        } else {
            tag.remove(name);
        }
    }

    @Override
    public double getDouble(String name) {
        return tag.getDouble(name);
    }

    @Override
    public void setDouble(String name, double value) {
        if (value != 0) {
            tag.putDouble(name, value);
        } else {
            tag.remove(name);
        }
    }

    @Override
    public String getString(String name) {
        return tag.getString(name);
    }

    @Override
    public void setString(String name, String value) {
        tag.putString(name, value);
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
        return tag.contains(name) ? UUID.fromString(tag.getString(name)) : null;
    }

    @Override
    public void setUUID(String name, UUID value) {
        tag.putString(name, value.toString());
    }

    @Override
    public List<IWrapperItemStack> getStacks(int count) {
        List<IWrapperItemStack> stacks = new ArrayList<>();
        NonNullList<ItemStack> mcList = NonNullList.withSize(count, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, mcList);
        for (ItemStack stack : mcList) {
            stacks.add(new WrapperItemStack(stack));
        }
        return stacks;
    }

    @Override
    public void setStacks(List<IWrapperItemStack> stacks) {
        NonNullList<ItemStack> mcList = NonNullList.create();
        for (IWrapperItemStack stack : stacks) {
            mcList.add(((WrapperItemStack) stack).stack);
        }
        ContainerHelper.saveAllItems(tag, mcList);
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
        return tag.contains(name, 10) ? new WrapperNBT(tag.getCompound(name)) : null;
    }

    @Override
    public void setData(String name, IWrapperNBT value) {
        tag.put(name, ((WrapperNBT) value).tag);
    }

    @Override
    public boolean hasKey(String name) {
        return tag.contains(name);
    }

    @Override
    public void deleteEntry(String name) {
        tag.remove(name);
    }

    @Override
    public Set<String> getAllNames() {
        return tag.getAllKeys();
    }
}
