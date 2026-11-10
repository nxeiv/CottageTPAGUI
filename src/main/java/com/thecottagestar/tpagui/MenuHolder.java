package com.thecottagestar.tpagui;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

final class MenuHolder implements InventoryHolder {
    enum PageType {
        MAIN,
        PLAYER_LIST
    }

    enum RequestMode {
        TPA,
        TPAHERE
    }

    private final UUID owner;
    private final PageType pageType;
    private final RequestMode requestMode;
    private final int page;
    private Inventory inventory;

    MenuHolder(UUID owner, PageType pageType, RequestMode requestMode, int page) {
        this.owner = owner;
        this.pageType = pageType;
        this.requestMode = requestMode;
        this.page = page;
    }

    UUID owner() {
        return owner;
    }

    PageType pageType() {
        return pageType;
    }

    RequestMode requestMode() {
        return requestMode;
    }

    int page() {
        return page;
    }

    void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
