package com.raoulvdberge.refinedstorage.apiimpl.network.node.externalstorage;

import com.raoulvdberge.refinedstorage.RSUtils;
import com.raoulvdberge.refinedstorage.api.network.INetworkMaster;
import com.raoulvdberge.refinedstorage.integration.cyclopscore.SlotlessItemHandlerHelper;
import com.raoulvdberge.refinedstorage.tile.config.IFilterable;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.ItemHandlerHelper;
import org.cyclops.cyclopscore.inventory.IndexedSlotlessItemHandlerWrapper;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.cyclopscore.tileentity.InventoryTileEntityBase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

public class StorageItemCyclops extends StorageItemExternal {
    private NetworkNodeExternalStorage externalStorage;
    private EnumFacing opposite;
    private Supplier<InventoryTileEntityBase> cyclopsInv;
    private int oldInventoryHash = -1;

    public StorageItemCyclops(NetworkNodeExternalStorage externalStorage) {
        this.externalStorage = externalStorage;
        this.opposite = externalStorage.getHolder().getDirection().getOpposite();
        this.cyclopsInv = () -> (InventoryTileEntityBase) externalStorage.getFacingTile();
    }

    @Override
    public void detectChanges(INetworkMaster network) {
        InventoryTileEntityBase inv = cyclopsInv.get();
        if (inv != null) {
            int inventoryHash = inv.getInventoryHash();
            if (inventoryHash != oldInventoryHash) {
                super.detectChanges(network);
                oldInventoryHash = inventoryHash;
            }
        }
    }

    @Override
    public NonNullList<ItemStack> getStacks() {
        return getStacks(cyclopsInv.get());
    }

    @Override
    public int getStored() {
        return getStacks(cyclopsInv.get()).stream().mapToInt(s -> s.getCount()).sum();
    }

    @Override
    public int getPriority() {
        return this.externalStorage.getPriority();
    }

    @Override
    public int getCapacity() {
        InventoryTileEntityBase inv = cyclopsInv.get();

        return inv != null ? inv.getInventory().getSizeInventory() * 64 : 0;
    }

    @Nullable
    @Override
    public ItemStack insert(@Nonnull ItemStack stack, int size, boolean simulate) {
        InventoryTileEntityBase inv = cyclopsInv.get();

        if (IFilterable.canTake(externalStorage.getItemFilters(), externalStorage.getMode(), externalStorage.getCompare(), stack)) {
            return SlotlessItemHandlerHelper.insertItem(inv, opposite, stack, size, simulate);
        }

        return ItemHandlerHelper.copyStackWithSize(stack, size);
    }

    @Nullable
    @Override
    public ItemStack extract(@Nonnull ItemStack stack, int size, int flags, boolean simulate) {
        InventoryTileEntityBase inv = cyclopsInv.get();
        return SlotlessItemHandlerHelper.extractItem(inv, opposite, stack, size, flags, simulate);
    }

    private NonNullList<ItemStack> getStacks(@Nullable InventoryTileEntityBase inv) {
        if (inv != null) {
            if (inv.getInventory() instanceof IndexedSlotlessItemHandlerWrapper.IInventoryIndexReference) {
                return ((IndexedSlotlessItemHandlerWrapper.IInventoryIndexReference) inv.getInventory())
                        .getIndex().values().stream().flatMap(m -> m.valueCollection().stream()).map(ItemStack::copy).collect(RSUtils.toNonNullList());
            } else {
                return Arrays.stream(((SimpleInventory)inv.getInventory()).getItemStacks()).map(ItemStack::copy).collect(RSUtils.toNonNullList());
            }
        } else {
            return RSUtils.emptyNonNullList();
        }
    }

    public static boolean isValid(TileEntity facingTE, EnumFacing facing) {
        return facingTE instanceof InventoryTileEntityBase
                && (SlotlessItemHandlerHelper.isSlotless(facingTE, facing)
                    || ((InventoryTileEntityBase) facingTE).getInventory() instanceof SimpleInventory);
    }
}
