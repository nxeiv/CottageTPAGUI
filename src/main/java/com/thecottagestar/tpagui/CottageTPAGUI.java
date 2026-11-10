package com.thecottagestar.tpagui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class CottageTPAGUI extends JavaPlugin implements Listener, CommandExecutor {
    private static final int PLAYER_SLOTS = 45;
    private static final int INVENTORY_SIZE = 54;

    // The Cottage★ palette.
    private static final TextColor SAGE = TextColor.color(0x7F936F);
    private static final TextColor GOLD = TextColor.color(0xC9A968);
    private static final TextColor CREAM = TextColor.color(0xFFFBF3);
    private static final TextColor DUSTY_ROSE = TextColor.color(0xC78F82);
    private static final TextColor TAUPE = TextColor.color(0x948979);

    private NamespacedKey actionKey;
    private NamespacedKey targetKey;
    private BedrockMenuManager bedrockMenus;

    @Override
    public void onEnable() {
        actionKey = new NamespacedKey(this, "menu-action");
        targetKey = new NamespacedKey(this, "target-uuid");

        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("tpamenu") == null || getCommand("options") == null) {
            getLogger().severe("A required command is missing from plugin.yml; disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("tpamenu").setExecutor(this);
        getCommand("options").setExecutor(this);

        Plugin floodgate = getServer().getPluginManager().getPlugin("floodgate");
        if (floodgate != null && floodgate.isEnabled()) {
            try {
                bedrockMenus = new BedrockMenuManager(this);
                getLogger().info("Floodgate detected. Native Bedrock Server Options are enabled.");
            } catch (LinkageError | RuntimeException exception) {
                getLogger().log(java.util.logging.Level.SEVERE,
                        "Could not initialize Bedrock forms. Java menus remain available.", exception);
            }
        } else {
            getLogger().warning("Floodgate was not detected. Java menus will work, but Bedrock forms and the compass shortcut are unavailable.");
        }

        Plugin simpleTpa = getServer().getPluginManager().getPlugin("SimpleTPA");
        if (simpleTpa == null || !simpleTpa.isEnabled()) {
            getLogger().warning("SimpleTPA was not detected by its plugin name. Teleport requests require SimpleTPA to be installed and enabled.");
        }
        getLogger().info("CottageTPAGUI enabled. Use /tpamenu for teleport requests and /options for server options.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("These menus can only be opened in-game.");
            return true;
        }

        String commandName = command.getName().toLowerCase(java.util.Locale.ROOT);
        if ("options".equals(commandName)) {
            if (!player.hasPermission("cottagetpagui.options")) {
                send(player, "You do not have permission to open Server Options★.", DUSTY_ROSE);
                return true;
            }
            if (bedrockMenus != null && bedrockMenus.isBedrockPlayer(player)) {
                bedrockMenus.openServerOptions(player);
            } else {
                send(player, "Open Server Options★ from your Java pause menu.", TAUPE);
            }
            return true;
        }

        if (!player.hasPermission("cottagetpagui.use")) {
            send(player, "You do not have permission to open the teleport menu.", DUSTY_ROSE);
            return true;
        }

        if (bedrockMenus != null && bedrockMenus.isBedrockPlayer(player)) {
            bedrockMenus.openTeleportMenu(player);
        } else {
            openMainMenu(player);
        }
        return true;
    }

    private void openMainMenu(Player player) {
        MenuHolder holder = new MenuHolder(player.getUniqueId(), MenuHolder.PageType.MAIN, null, 0);
        Inventory inventory = Bukkit.createInventory(holder, 27, title("Teleport Menu"));
        holder.inventory(inventory);
        fill(inventory, Material.GREEN_STAINED_GLASS_PANE);

        inventory.setItem(4, button(
                Material.BOOK,
                Component.text("The Cottage★ Teleport System", GOLD).decorate(TextDecoration.BOLD),
                List.of(line("Choose an action below.", TAUPE), line("Requests are handled by SimpleTPA.", TAUPE)),
                null
        ));

        boolean canTpa = player.hasPermission("tpa.command.tpa");
        boolean canTpaHere = player.hasPermission("tpa.command.tpahere");

        inventory.setItem(11, button(
                canTpa ? Material.COMPASS : Material.BARRIER,
                Component.text("Teleport to Player", canTpa ? CREAM : DUSTY_ROSE).decorate(TextDecoration.BOLD),
                List.of(
                        line("Choose someone who is online.", TAUPE),
                        line("They will receive a request to", TAUPE),
                        line("let you teleport to them.", TAUPE),
                        line(canTpa ? "Click to continue" : "Missing tpa.command.tpa", canTpa ? SAGE : DUSTY_ROSE)
                ),
                canTpa ? "open-tpa" : null
        ));

        inventory.setItem(15, button(
                canTpaHere ? Material.ENDER_PEARL : Material.BARRIER,
                Component.text("Invite Player", canTpaHere ? GOLD : DUSTY_ROSE).decorate(TextDecoration.BOLD),
                List.of(
                        line("Choose someone who is online.", TAUPE),
                        line("They will receive a request", TAUPE),
                        line("to teleport to your location.", TAUPE),
                        line(canTpaHere ? "Click to continue" : "Missing tpa.command.tpahere", canTpaHere ? SAGE : DUSTY_ROSE)
                ),
                canTpaHere ? "open-tpahere" : null
        ));

        inventory.setItem(22, button(
                Material.OAK_DOOR,
                Component.text("Close", DUSTY_ROSE),
                List.of(line("Close this menu.", TAUPE)),
                "close"
        ));

        player.openInventory(inventory);
    }

    private void openPlayerList(Player player, MenuHolder.RequestMode mode, int requestedPage) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.removeIf(target -> target.getUniqueId().equals(player.getUniqueId()));
        online.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int pageCount = Math.max(1, (online.size() + PLAYER_SLOTS - 1) / PLAYER_SLOTS);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));

        MenuHolder holder = new MenuHolder(player.getUniqueId(), MenuHolder.PageType.PLAYER_LIST, mode, page);
        String heading = mode == MenuHolder.RequestMode.TPA ? "Choose a Player" : "Invite a Player";
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE, title(heading));
        holder.inventory(inventory);
        fill(inventory, Material.GRAY_STAINED_GLASS_PANE);

        int start = page * PLAYER_SLOTS;
        int end = Math.min(start + PLAYER_SLOTS, online.size());
        for (int index = start; index < end; index++) {
            Player target = online.get(index);
            inventory.setItem(index - start, playerHead(target, mode));
        }

        if (online.isEmpty()) {
            inventory.setItem(22, button(
                    Material.BARRIER,
                    Component.text("Nobody else is online", DUSTY_ROSE).decorate(TextDecoration.BOLD),
                    List.of(line("There are no other players to select.", TAUPE), line("Try again after someone joins.", TAUPE)),
                    null
            ));
        }

        inventory.setItem(45, button(
                page > 0 ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE,
                Component.text("Previous Page", page > 0 ? CREAM : TAUPE),
                List.of(line(page > 0 ? "Go to the previous page." : "You are on the first page.", TAUPE)),
                page > 0 ? "previous" : null
        ));
        inventory.setItem(47, button(
                Material.OAK_DOOR,
                Component.text("Back", GOLD),
                List.of(line("Return to the teleport menu.", TAUPE)),
                "back"
        ));
        inventory.setItem(49, button(
                Material.PAPER,
                Component.text("Page " + (page + 1) + " / " + pageCount, CREAM),
                List.of(line(online.size() + " player(s) online besides you.", TAUPE)),
                null
        ));
        inventory.setItem(51, button(
                Material.CLOCK,
                Component.text("Refresh", SAGE),
                List.of(line("Update the online player list.", TAUPE)),
                "refresh"
        ));
        inventory.setItem(53, button(
                page < pageCount - 1 ? Material.ARROW : Material.GRAY_STAINED_GLASS_PANE,
                Component.text("Next Page", page < pageCount - 1 ? CREAM : TAUPE),
                List.of(line(page < pageCount - 1 ? "Go to the next page." : "You are on the last page.", TAUPE)),
                page < pageCount - 1 ? "next" : null
        ));

        player.openInventory(inventory);
    }

    private ItemStack playerHead(Player target, MenuHolder.RequestMode mode) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(Component.text(target.getName(), CREAM).decorate(TextDecoration.BOLD));
        String actionLabel = mode == MenuHolder.RequestMode.TPA ? "Send teleport request" : "Invite them to your location";
        meta.lore(List.of(line(actionLabel, SAGE), line("Click to select this player.", TAUPE)));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "select-player");
        meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, target.getUniqueId().toString());
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!holder.owner().equals(player.getUniqueId())) {
            player.closeInventory();
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        String action = readTag(clicked, actionKey);
        if (action == null) {
            return;
        }

        switch (action) {
            case "open-tpa" -> openPlayerList(player, MenuHolder.RequestMode.TPA, 0);
            case "open-tpahere" -> openPlayerList(player, MenuHolder.RequestMode.TPAHERE, 0);
            case "select-player" -> selectPlayer(player, holder, clicked);
            case "previous" -> openPlayerList(player, holder.requestMode(), holder.page() - 1);
            case "next" -> openPlayerList(player, holder.requestMode(), holder.page() + 1);
            case "refresh" -> openPlayerList(player, holder.requestMode(), holder.page());
            case "back" -> openMainMenu(player);
            case "close" -> player.closeInventory();
            default -> { }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private void selectPlayer(Player sender, MenuHolder holder, ItemStack clicked) {
        String rawUuid = readTag(clicked, targetKey);
        if (rawUuid == null) {
            return;
        }

        UUID targetId;
        try {
            targetId = UUID.fromString(rawUuid);
        } catch (IllegalArgumentException exception) {
            send(sender, "That player selection was invalid. Please refresh the list.", DUSTY_ROSE);
            openPlayerList(sender, holder.requestMode(), holder.page());
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            send(sender, "That player is no longer online. The list has been refreshed.", DUSTY_ROSE);
            openPlayerList(sender, holder.requestMode(), holder.page());
            return;
        }
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            send(sender, "You cannot send a teleport request to yourself.", DUSTY_ROSE);
            openPlayerList(sender, holder.requestMode(), holder.page());
            return;
        }

        String command;
        String permission;
        if (holder.requestMode() == MenuHolder.RequestMode.TPA) {
            command = "tpa " + target.getName();
            permission = "tpa.command.tpa";
        } else {
            command = "tpahere " + target.getName();
            permission = "tpa.command.tpahere";
        }

        if (!sender.hasPermission(permission)) {
            send(sender, "You no longer have permission to send that type of request.", DUSTY_ROSE);
            sender.closeInventory();
            return;
        }

        sender.closeInventory();
        boolean dispatched = sender.performCommand(command);
        if (!dispatched) {
            send(sender, "SimpleTPA did not accept the command. Check that it is installed and enabled.", DUSTY_ROSE);
        }
    }

    private ItemStack button(Material material, Component name, List<Component> lore, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        if (lore != null && !lore.isEmpty()) meta.lore(lore);
        if (action != null) meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inventory, Material material) {
        ItemStack filler = button(material, Component.text(" ", NamedTextColor.WHITE), List.of(), null);
        for (int slot = 0; slot < inventory.getSize(); slot++) inventory.setItem(slot, filler.clone());
    }

    private String readTag(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private Component title(String section) {
        return Component.text("The Cottage★", GOLD).decorate(TextDecoration.BOLD)
                .append(Component.text("  |  ", TAUPE))
                .append(Component.text(section, CREAM));
    }

    private Component line(String text, TextColor color) {
        return Component.text(text, color);
    }

    private void send(Player player, String text, TextColor color) {
        Component prefix = Component.text("The Cottage★", SAGE).decorate(TextDecoration.BOLD)
                .append(Component.text(" » ", GOLD));
        player.sendMessage(prefix.append(Component.text(text, color)));
    }
}
