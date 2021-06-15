/*
 * This file is part of BlockProt, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 spnda
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.sean.blockprot.bukkit.inventories;

import de.sean.blockprot.BlockProt;
import de.sean.blockprot.TranslationKey;
import de.sean.blockprot.Translator;
import de.sean.blockprot.bukkit.nbt.BlockNBTHandler;
import de.sean.blockprot.bukkit.nbt.PlayerSettingsHandler;
import de.sean.blockprot.bukkit.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class FriendManageInventory extends FriendModifyInventory {
    private int maxSkulls = InventoryConstants.tripleLine - 4;

    @Override
    public int getSize() {
        return InventoryConstants.tripleLine;
    }

    @NotNull
    @Override
    public String getTranslatedInventoryName() {
        return Translator.get(TranslationKey.INVENTORIES__FRIENDS__MANAGE);
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event, @NotNull InventoryState state) {
        final Player player = (Player) event.getWhoClicked();
        final ItemStack item = event.getCurrentItem();
        if (item == null) return;
        switch (item.getType()) {
            case BLACK_STAINED_GLASS_PANE: {
                // Exit the modify inventory and return to the base lock inventory.
                exitModifyInventory(player, state);
                break;
            }
            case CYAN_STAINED_GLASS_PANE: {
                if (state.getFriendPage() >= 1) {
                    state.setFriendPage(state.getFriendPage() - 1);

                    player.closeInventory();
                    player.openInventory(fill(player));
                }
                break;
            }
            case BLUE_STAINED_GLASS_PANE: {
                ItemStack lastFriendInInventory = event.getInventory().getItem(maxSkulls);
                if (lastFriendInInventory != null && lastFriendInInventory.getAmount() == 0) {
                    // There's an item in the last slot => The page is fully filled up, meaning
                    // we should go to the next page.
                    state.setFriendPage(state.getFriendPage() + 1);

                    player.closeInventory();
                    player.openInventory(fill(player));
                }
                break;
            }
            case SKELETON_SKULL:
            case PLAYER_HEAD: {
                // Get the clicked player head and open the detail inventory.
                int index = findItemIndex(item);
                OfflinePlayer friend = state.getFriendResultCache().get(index);
                state.setCurFriend(friend);
                final Inventory inv = new FriendDetailInventory().fill(player);
                player.closeInventory();
                player.openInventory(inv);
                break;
            }
            case MAP: {
                FriendSearchInventory.INSTANCE.openAnvilInventory(player);
                break;
            }
            default: {
                // Unexpected, exit the inventory.
                player.closeInventory();
                InventoryState.Companion.remove(player.getUniqueId());
                break;
            }
        }
        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event, @NotNull InventoryState state) {

    }

    @NotNull
    public Inventory fill(@NotNull Player player) {
        final InventoryState state = InventoryState.Companion.get(player.getUniqueId());
        if (state == null) return inventory;

        List<OfflinePlayer> players;
        switch (state.getFriendSearchState()) {
            case FRIEND_SEARCH: {
                final BlockNBTHandler handler =
                    new BlockNBTHandler(Objects.requireNonNull(state.getBlock()));
                players = mapFriendsToPlayer(handler.getFriendsStream());
                break;
            }
            case DEFAULT_FRIEND_SEARCH: {
                // We have 1 button less, as that button is only for blocks, which gives us room
                // for one more friend.
                maxSkulls += 1;
                final PlayerSettingsHandler settingsHandler = new PlayerSettingsHandler(player);
                List<String> currentFriends = settingsHandler.getDefaultFriends();
                final String selfUuid = player.getUniqueId().toString();
                players =
                    filterList(
                        currentFriends,
                        Arrays.asList(Bukkit.getOfflinePlayers()),
                        (uuid, cur) -> cur.contains(uuid) && !uuid.equals(selfUuid));
                break;
            }
            default: {
                throw new RuntimeException(
                    "Could not build "
                        + this.getClass().getName()
                        + " due to invalid friend search state: "
                        + state.getFriendSearchState());
            }
        }

        // Fill the first page inventory with skeleton skulls.
        state.getFriendResultCache().clear();
        int pageOffset = maxSkulls * state.getFriendPage();
        for (int i = pageOffset; i < Math.min(players.size() - pageOffset, maxSkulls); i++) {
            final OfflinePlayer curPlayer = players.get(i);
            inventory.setItem(
                i - pageOffset,
                ItemUtil.INSTANCE.getItemStack(
                    1, Material.SKELETON_SKULL, curPlayer.getName()));
            state.getFriendResultCache().add(curPlayer);
        }

        // Only show the page buttons if there's more than 1 page.
        if (state.getFriendPage() == 0 && players.size() >= maxSkulls) {
            setItemStack(
                maxSkulls,
                Material.CYAN_STAINED_GLASS_PANE,
                TranslationKey.INVENTORIES__LAST_PAGE);
            setItemStack(
                maxSkulls + 1,
                Material.BLUE_STAINED_GLASS_PANE,
                TranslationKey.INVENTORIES__NEXT_PAGE);
        }

        setItemStack(
            InventoryConstants.tripleLine - 2,
            Material.MAP,
            TranslationKey.INVENTORIES__FRIENDS__SEARCH);
        setBackButton();

        Bukkit.getScheduler()
            .runTaskAsynchronously(
                BlockProt.instance,
                () -> {
                    int i = 0;
                    while (i < maxSkulls && i < state.getFriendResultCache().size()) {
                        inventory.setItem(
                            i,
                            ItemUtil.INSTANCE.getPlayerSkull(
                                state.getFriendResultCache().get(i)));
                        i++;
                    }
                });

        return inventory;
    }
}
