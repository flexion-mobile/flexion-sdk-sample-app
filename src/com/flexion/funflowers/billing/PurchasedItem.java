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

import com.flexionmobile.sdk.billing.ItemType;
import com.flexionmobile.sdk.billing.PurchaseState;

/**
 * Represents an in-app billing purchase.
 * 
 * @author Bruno Oliveira (Google), modified by Jonathan Coe (Flexion)
 */
public class PurchasedItem {
    private ItemType mItemType;
    private String mOrderId;
    private String mItemId;
    private long mPurchaseTime;
    private PurchaseState mPurchaseState;
    private String mDeveloperPayload;
    private String mToken;
    
    /**
     * Creates a new PurchasedItem instance
     * 
     * @param itemType - The item type
     * @param orderId - The order ID
     * @param itemId - The item ID
     * @param purchaseTime - The time at which the item was purchased
     * @param purchaseState - The state of the purchase
     * @param developerPayload - The developer payload for this purchase
     * @param token - The token for this purchase
     */
    public PurchasedItem(ItemType itemType, String orderId, String itemId, long purchaseTime,
    		PurchaseState purchaseState, String developerPayload, String token) {
        mItemType = itemType;
        mOrderId = orderId;
        mItemId = itemId;
        mPurchaseTime = purchaseTime;
        mPurchaseState = purchaseState;
        mDeveloperPayload = developerPayload;
        mToken = token;
    }
    
    public ItemType getItemType() { return mItemType; }
    public String getOrderId() { return mOrderId; }
    public String getItemId() { return mItemId; }
    public long getPurchaseTime() { return mPurchaseTime; }
    public PurchaseState getPurchaseState() { return mPurchaseState; }
    public String getDeveloperPayload() { return mDeveloperPayload; }
    public String getToken() { return mToken; }

    @Override
    public String toString() { return mItemId; }
}