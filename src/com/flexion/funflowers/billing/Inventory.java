/* Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flexion.funflowers.billing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flexionmobile.sdk.billing.Item;
import com.flexionmobile.sdk.billing.ItemType;

/**
 * Represents a block of information about in-app items.
 * An Inventory is returned by such methods as {@link BillingHelper#queryInventory}.
 * 
 * @author Bruno Oliveira (Google), modified by Jonathan Coe (Flexion)
 */
public class Inventory {
    Map<String,Item> mItemMap = new HashMap<String,Item>();
    Map<String,PurchasedItem> mPurchaseMap = new HashMap<String,PurchasedItem>();

    Inventory() { }

    /** Returns the listing details for an in-app product. */
    public Item getItem(String item) {
        return mItemMap.get(item);
    }

    /** Returns purchase information for a given product, or null if there is no purchase. */
    public PurchasedItem getPurchase(String item) {
        return mPurchaseMap.get(item);
    }

    /** Returns whether or not there exists a purchase of the given product. */
    public boolean hasPurchase(String item) {
        return mPurchaseMap.containsKey(item);
    }

    /** Return whether or not details about the given product are available. */
    public boolean hasItem(String item) {
        return mItemMap.containsKey(item);
    }

    /**
     * Erase a purchase (locally) from the inventory, given its product ID. This just
     * modifies the Inventory object locally and has no effect on the server! This is
     * useful when you have an existing Inventory object which you know to be up to date,
     * and you have just consumed an item successfully, which means that erasing its
     * purchase data from the Inventory you already have is quicker than querying for
     * a new Inventory.
     */
    public void erasePurchase(String item) {
        if (mPurchaseMap.containsKey(item)) {
        	mPurchaseMap.remove(item);
        }
    }

    /** Returns a list of all owned product IDs. */
    List<String> getAllOwnedItems() {
        return new ArrayList<String>(mPurchaseMap.keySet());
    }

    /** Returns a list of all owned product IDs of a given type */
    List<String> getAllOwnedItems(ItemType itemType) {
        List<String> result = new ArrayList<String>();
        for (PurchasedItem p : mPurchaseMap.values()) {
            if (p.getItemType().equals(itemType)) result.add(p.getItemId());
        }
        return result;
    }

    /** Returns a list of all purchases. */
    List<PurchasedItem> getAllPurchases() {
        return new ArrayList<PurchasedItem>(mPurchaseMap.values());
    }

    void addItemDetails(Item item) {
        mItemMap.put(item.getId(), item);
    }

    void addPurchase(PurchasedItem p) {
        mPurchaseMap.put(p.getItemId(), p);
    }
}