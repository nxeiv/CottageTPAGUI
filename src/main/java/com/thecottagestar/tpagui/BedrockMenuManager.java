package com.thecottagestar.tpagui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

final class BedrockMenuManager implements Listener {
    private static final int PLAYERS_PER_PAGE = 12;

    private static final TextColor SAGE = TextColor.color(0x7F936F);
    private static final TextColor GOLD = TextColor.color(0xC9A968);
    private static final TextColor CREAM = TextColor.color(0xFFFBF3);
    private static final TextColor DUSTY_ROSE = TextColor.color(0xC78F82);
    private static final TextColor TAUPE = TextColor.color(0x948979);

    private static final String DISCORD_URL = "https://discord.gg/323w42BxNN";
    private static final String WEBSITE_URL = "https://the-cottage.onrender.com/";

    private final JavaPlugin plugin;
    private final FloodgateApi floodgate;
    private final NamespacedKey compassKey;

    private enum RequestMode {
        TPA,
        TPAHERE
    }

    private record Tutorial(String title, String description) { }

    private static final List<Tutorial> TUTORIALS = List.of(
            new Tutorial(
                    "Teleport Requests",
                    "Use /tpamenu to open the teleport menu.\n\n/tpa <player> asks to teleport to someone.\n/tpahere <player> asks someone to teleport to you.\n\nUse /tpaccept [player] to accept, /tpdeny [player] to deny, and /tpatoggle to stop or allow incoming requests. Requests expire after the server's configured timeout."
            ),
            new Tutorial(
                    "Team System",
                    "Open your team controls with /team gui.\n\nUse the Team Ender Chest button or /team echest to access your team's shared storage. Team features and permissions are controlled by the server."
            ),
            new Tutorial(
                    "VeinMiner",
                    "VeinMiner can break connected supported blocks together when you mine one with a supported tool.\n\nIt only works for blocks and tools allowed by the server configuration, and server limits still apply."
            ),
            new Tutorial(
                    "Dynamic Lights",
                    "Hold a torch or another supported light source to illuminate the area around you. Dropped torches and other supported light sources can illuminate their surroundings too."
            ),
            new Tutorial(
                    "Better Mending",
                    "Hold the tool or armor piece with the Mending enchantment, then sneak and right-click to mend it. The item must have Mending."
            ),
            new Tutorial(
                    "Easy Gaps & Webs",
                    "Golden Apples: use an apple with four gold ingots in the plugin's recipe.\n\nCobwebs: arrange strings in an X pattern in the crafting grid.\n\nThese are the server's simplified crafting recipes."
            ),
            new Tutorial(
                    "Happy Ghasts",
                    "Happy Ghast flight speed is adjusted by the server's FasterHappyGhast plugin. Mount and steer a Happy Ghast normally; its flight speed follows the server configuration."
            ),
            new Tutorial(
                    "Skins",
                    "Use /skins to open the skin menu. SkinsRestorer lets you browse and apply supported Minecraft skins. Skin availability and refresh timing can depend on the skin service."
            ),
            new Tutorial(
                    "Mob Handling",
                    "ClickMobs supports picking up eligible mobs with the configured sneak-and-interact action, then placing them back using the carried mob item. Not every mob is necessarily supported; follow any in-game restrictions."
            ),
            new Tutorial(
                    "Villager Trading",
                    "InstaRestock adjusts villager trade restocking on the server. Trade availability still follows the server's configuration. Ask staff before moving or claiming villagers whose ownership is unclear."
            )
    );

    BedrockMenuManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.floodgate = FloodgateApi.getInstance();
        if (this.floodgate == null) {
            throw new IllegalStateException("Floodgate API is not available.");
        }
        this.compassKey = new NamespacedKey(plugin, "server-options-compass");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    boolean isBedrockPlayer(Player player) {
        return player != null && player.isOnline() && floodgate.isFloodgatePlayer(player.getUniqueId());
    }

    void openServerOptions(Player player) {
        if (!isBedrockPlayer(player)) {
            player.sendMessage(Component.text("Server Options★ is available from the Bedrock compass shortcut.", TAUPE));
            return;
        }

        SimpleForm form = SimpleForm.builder()
                .title("The Cottage★ | Server Options")
                .content("Chapter 1 · Season 6\nWelcome to the backyard. Choose an option below.")
                .button("Spawn")
                .button("Wanderer's Way")
                .button("Teleport Menu")
                .button("Player Tutorials")
                .button("Team System")
                .button("Team Ender Chest")
                .button("Discord")
                .button("Website")
                .button("Close")
                .validResultHandler(response -> runForPlayer(player.getUniqueId(), online -> {
                    switch (response.clickedButtonId()) {
                        case 0 -> teleportToSpawn(online);
                        case 1 -> runPlayerCommand(online, "rtp");
                        case 2 -> openTeleportMenu(online);
                        case 3 -> openTutorialMenu(online);
                        case 4 -> runPlayerCommand(online, "team gui");
                        case 5 -> runPlayerCommand(online, "team echest");
                        case 6 -> showLink(online, "The Cottage★ Discord", DISCORD_URL);
                        case 7 -> showLink(online, "The Cottage★ Website", WEBSITE_URL);
                        default -> { }
                    }
                }))
                .build();

        sendForm(player, form);
    }

    void openTeleportMenu(Player player) {
        if (!isBedrockPlayer(player)) {
            player.sendMessage(Component.text("Use /tpamenu to open the Java teleport menu.", TAUPE));
            return;
        }

        SimpleForm.Builder builder = SimpleForm.builder()
                .title("The Cottage★ | Teleport Menu")
                .content("Choose how you want to send a teleport request.");

        List<RequestMode> modes = new ArrayList<>();
        if (player.hasPermission("tpa.command.tpa")) {
            builder.button("Teleport to Player");
            modes.add(RequestMode.TPA);
        }
        if (player.hasPermission("tpa.command.tpahere")) {
            builder.button("Invite Player");
            modes.add(RequestMode.TPAHERE);
        }
        int backButton = modes.size();
        builder.button("Back to Server Options");

        SimpleForm form = builder.validResultHandler(response -> runForPlayer(player.getUniqueId(), online -> {
            int clicked = response.clickedButtonId();
            if (clicked < modes.size()) {
                openPlayerSelection(online, modes.get(clicked), 0);
            } else if (clicked == backButton) {
                openServerOptions(online);
            }
        })).build();

        sendForm(player, form);
    }

    private void openPlayerSelection(Player player, RequestMode mode, int requestedPage) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.removeIf(target -> target.getUniqueId().equals(player.getUniqueId()));
        onlinePlayers.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int totalPages = Math.max(1, (onlinePlayers.size() + PLAYERS_PER_PAGE - 1) / PLAYERS_PER_PAGE);
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        int start = page * PLAYERS_PER_PAGE;
        int end = Math.min(start + PLAYERS_PER_PAGE, onlinePlayers.size());

        List<UUID> shownPlayers = onlinePlayers.subList(start, end).stream()
                .map(Player::getUniqueId)
                .toList();

        String title = mode == RequestMode.TPA
                ? "The Cottage★ | Teleport to Player"
                : "The Cottage★ | Invite Player";
        String content = onlinePlayers.isEmpty()
                ? "Nobody else is online right now."
                : "Choose a player. Online players: " + onlinePlayers.size()
                    + "\nPage " + (page + 1) + " of " + totalPages + ".";

        SimpleForm.Builder builder = SimpleForm.builder().title(title).content(content);
        for (int index = start; index < end; index++) {
            builder.button(onlinePlayers.get(index).getName());
        }

        int nextButtonId = -1;
        int previousButtonId = -1;
        int refreshButtonId;
        int backButtonId;

        if (page > 0) {
            previousButtonId = shownPlayers.size();
            builder.button("Previous Page");
        }

        backButtonId = shownPlayers.size() + (page > 0 ? 1 : 0);
        builder.button("Back");

        refreshButtonId = backButtonId + 1;
        builder.button("Refresh");

        if (page < totalPages - 1) {
            nextButtonId = refreshButtonId + 1;
            builder.button("Next Page");
        }

        final int previousId = previousButtonId;
        final int nextId = nextButtonId;
        final int refreshId = refreshButtonId;
        final int backId = backButtonId;
        SimpleForm form = builder.validResultHandler(response -> runForPlayer(player.getUniqueId(), online -> {
            int clicked = response.clickedButtonId();
            if (clicked >= 0 && clicked < shownPlayers.size()) {
                sendTeleportRequest(online, shownPlayers.get(clicked), mode, page);
            } else if (clicked == previousId && previousId >= 0) {
                openPlayerSelection(online, mode, page - 1);
            } else if (clicked == nextId && nextId >= 0) {
                openPlayerSelection(online, mode, page + 1);
            } else if (clicked == refreshId) {
                openPlayerSelection(online, mode, page);
            } else if (clicked == backId) {
                openTeleportMenu(online);
            }
        })).build();

        sendForm(player, form);
    }

    private void sendTeleportRequest(Player sender, UUID targetId, RequestMode mode, int returnPage) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            tell(sender, "That player is no longer online. The list has been refreshed.", DUSTY_ROSE);
            openPlayerSelection(sender, mode, returnPage);
            return;
        }

        String command;
        String permission;
        if (mode == RequestMode.TPA) {
            command = "tpa " + target.getName();
            permission = "tpa.command.tpa";
        } else {
            command = "tpahere " + target.getName();
            permission = "tpa.command.tpahere";
        }

        if (!sender.hasPermission(permission)) {
            tell(sender, "You do not have permission to send that request.", DUSTY_ROSE);
            openTeleportMenu(sender);
            return;
        }

        // The request is executed as the Bedrock player so SimpleTPA keeps
        // ownership of its own cooldown, timeout, toggle, and permission rules.
        sender.performCommand(command);
    }

    private void openTutorialMenu(Player player) {
        SimpleForm.Builder builder = SimpleForm.builder()
                .title("The Cottage★ | Player Tutorials")
                .content("Choose a topic to learn more.");

        for (Tutorial tutorial : TUTORIALS) {
            builder.button(tutorial.title());
        }
        int backId = TUTORIALS.size();
        builder.button("Back to Server Options");

        SimpleForm form = builder.validResultHandler(response -> runForPlayer(player.getUniqueId(), online -> {
            int clicked = response.clickedButtonId();
            if (clicked >= 0 && clicked < TUTORIALS.size()) {
                openTutorialDetail(online, TUTORIALS.get(clicked));
            } else if (clicked == backId) {
                openServerOptions(online);
            }
        })).build();

        sendForm(player, form);
    }

    private void openTutorialDetail(Player player, Tutorial tutorial) {
        SimpleForm form = SimpleForm.builder()
                .title("The Cottage★ | " + tutorial.title())
                .content(tutorial.description())
                .button("Back to Tutorials")
                .button("Server Options")
                .validResultHandler(response -> runForPlayer(player.getUniqueId(), online -> {
                    if (response.clickedButtonId() == 0) {
                        openTutorialMenu(online);
                    } else if (response.clickedButtonId() == 1) {
                        openServerOptions(online);
                    }
                }))
                .build();

        sendForm(player, form);
    }

    private void showLink(Player player, String title, String url) {
        SimpleForm form = SimpleForm.builder()
                .title(title)
                .content(url + "\n\nA clickable copy of this link has also been sent in chat.")
                .button("Back to Server Options")
                .validResultHandler(response -> runForPlayer(player.getUniqueId(), this::openServerOptions))
                .build();

        sendForm(player, form);
        Component clickable = Component.text(title + ": ", SAGE)
                .append(Component.text(url, GOLD).clickEvent(ClickEvent.openUrl(url)));
        player.sendMessage(clickable);
    }

    private void teleportToSpawn(Player player) {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            tell(player, "The Overworld is not currently available.", DUSTY_ROSE);
            return;
        }

        Location spawn = new Location(world, -49.50, 70.50, 72.53, -269.80f, -33.56f);
        if (player.teleport(spawn)) {
            tell(player, "You have returned to The Cottage★ spawn.", SAGE);
        } else {
            tell(player, "The server could not teleport you to spawn.", DUSTY_ROSE);
        }
    }

    private void runPlayerCommand(Player player, String command) {
        boolean dispatched = player.performCommand(command);
        if (!dispatched) {
            tell(player, "That server feature is currently unavailable. Please contact staff.", DUSTY_ROSE);
        }
    }

    private void sendForm(Player player, SimpleForm form) {
        try {
            boolean sent = floodgate.sendForm(player.getUniqueId(), form);
            if (!sent) {
                tell(player, "Could not open the Bedrock menu. Try /options again.", DUSTY_ROSE);
            }
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("Could not send a Bedrock form to " + player.getName() + ": " + exception.getMessage());
            tell(player, "Could not open the Bedrock menu. Try /options again.", DUSTY_ROSE);
        }
    }

    private void runForPlayer(UUID uuid, Consumer<Player> action) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && isBedrockPlayer(player)) {
                action.accept(player);
            }
        });
    }

    private void tell(Player player, String message, TextColor color) {
        player.sendMessage(Component.text("The Cottage★", SAGE).decorate(TextDecoration.BOLD)
                .append(Component.text(" » ", GOLD))
                .append(Component.text(message, color)));
    }

    // Give each Bedrock player a protected Server Options★ compass without
    // replacing anything they already carry. /options remains the fallback.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                giveOptionsCompass(player);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                giveOptionsCompass(player);
            }
        }, 10L);
    }

    private void giveOptionsCompass(Player player) {
        if (!isBedrockPlayer(player) || hasOptionsCompass(player)) {
            return;
        }

        for (int slot = 0; slot < 9; slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType().isAir()) {
                player.getInventory().setItem(slot, createOptionsCompass());
                tell(player, "Server Options★ has been added to your hotbar. Right-click it anytime, or use /options.", SAGE);
                return;
            }
        }

        tell(player, "Your hotbar is full, so no Server Options★ compass was added. Use /options or free a hotbar slot and rejoin.", TAUPE);
    }

    private boolean hasOptionsCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isOptionsCompass(item)) {
                return true;
            }
        }
        return false;
    }

    private ItemStack createOptionsCompass() {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        meta.displayName(Component.text("Server Options★", GOLD).decorate(TextDecoration.BOLD));
        meta.lore(List.of(
                Component.text("Right-click to open the Cottage★ menu.", TAUPE),
                Component.text("Keep this compass in your inventory.", SAGE)
        ));
        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);
        compass.setItemMeta(meta);
        return compass;
    }

    private boolean isOptionsCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(compassKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCompassUse(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!isOptionsCompass(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        if (isBedrockPlayer(event.getPlayer())) {
            openServerOptions(event.getPlayer());
        } else {
            tell(event.getPlayer(), "Server Options★ is a Bedrock shortcut. Java players can use the pause menu.", TAUPE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCompassInventoryClick(InventoryClickEvent event) {
        if (isOptionsCompass(event.getCurrentItem()) || isOptionsCompass(event.getCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCompassInventoryDrag(InventoryDragEvent event) {
        if (isOptionsCompass(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCompassDrop(PlayerDropItemEvent event) {
        if (isOptionsCompass(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCompassSwap(PlayerSwapHandItemsEvent event) {
        if (isOptionsCompass(event.getMainHandItem()) || isOptionsCompass(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isOptionsCompass);
    }
}
