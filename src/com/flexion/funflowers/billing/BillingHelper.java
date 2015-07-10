/* 
 * TrivialDrive copyright 2012 Google Inc.
 * Fun Flowers copyright 2015 Flexion Mobile Ltd.
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
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.flexionmobile.sdk.Flexion;
import com.flexionmobile.sdk.billing.BillingError;
import com.flexionmobile.sdk.billing.FlexionBillingService;
import com.flexionmobile.sdk.billing.Item;
import com.flexionmobile.sdk.billing.ItemType;
import com.flexionmobile.sdk.billing.OnConsumeFinishedCallback;
import com.flexionmobile.sdk.billing.OnPurchaseFinishedCallback;
import com.flexionmobile.sdk.billing.OnQueryGetPurchasesFinishedCallback;
import com.flexionmobile.sdk.billing.OnQueryItemDetailsFinishedCallback;
import com.flexionmobile.sdk.billing.Purchase;
import com.flexionmobile.sdk.billing.PurchaseState;

/**
 * Provides convenience methods for in-app billing. You can create one instance of this
 * class for your application and use it to process in-app billing operations.
 *
 * When you are done with this object, don't forget to call {@link #dispose}
 * to ensure proper cleanup. This object holds a binding to the in-app billing
 * service, which will leak unless you dispose of it correctly. If you created
 * the object on an Activity's onCreate method, then the recommended
 * place to dispose of it is the Activity's onDestroy method.<br><br>
 *
 * @author Bruno Oliveira (Google), modified by Jonathan Coe (Flexion)
 */
public class BillingHelper {

    /** Has this object been disposed of? (If so, we should ignore callbacks, etc) */
    boolean mDisposed = false;
	
    /** Context we were passed during initialization */
    Context mContext;
    
    /** The FlexionBillingService instance */
    private FlexionBillingService mBillingService;

    /** The inventory of purchasable items */
    private Inventory mInventory;
    
    /** Are subscriptions supported? */
    private static final boolean SUBSCRIPTIONS_SUPPORTED = true;
    
    /** This value controls whether or not debug messages will be logged. This should be
     * set to false in production usage */
    private static final boolean DEBUG_LOGGING_ENABLED = true;
    
    /** The tag used to mark log messages from this class */
    private static final String TAG = "BillingHelper";
    
    /**
     * Creates a BillingHelper instance.
     *
     * @param context - Your application or Activity context. Needed to bind to the in-app billing service.
     */
    public BillingHelper(Context context) {
        mContext = context.getApplicationContext();
        logDebug("IAB helper created.");
        
        // Create the Flexion billing service instance
        mBillingService = Flexion.createBillingService(mContext);
        logDebug("FlexionBillingService instance created.");
        
        // Create the Inventory object
        mInventory = new Inventory();
        logDebug("Inventory instance created.");
    }
    
    /**
     * Dispose of the BillingHelper object, releasing its resources. It's very important
     * to call this method when you are done with this object. It will release any resources
     * used by it such as service connections. Naturally, once the object is
     * disposed of, it can't be used again.
     */
    public void dispose() {
        logDebug("IahHelper.dispose() called.");
        if (mBillingService != null) {
        	logDebug("Disposing of FlexionBillingService instance.");
        	mBillingService.dispose();
        }
        mDisposed = true;
        mContext = null;
        mPurchaseListener = null;
    }
    
    /**
     * Checks whether the running instance of this class has been disposed of. 
     * If so, this method throws an IllegalStateException. 
     */
    private void checkNotDisposed() {
        if (mDisposed) throw new IllegalStateException("BillingHelper was disposed of, so it cannot be used.");
    }
    
    /** Returns whether subscriptions are supported. */
    public boolean subscriptionsSupported() {
        checkNotDisposed();
        return SUBSCRIPTIONS_SUPPORTED;
    }
    
    /**
     * Callback that notifies when a purchase is finished.
     */
    public interface OnIabPurchaseFinishedListener {
        /**
         * Called to notify that an in-app purchase finished. If the purchase was successful,
         * then the item parameter specifies which item was purchased. If the purchase failed,
         * the item and extraData parameters may or may not be null, depending on how far the purchase
         * process went.
         *
         * @param result The result of the purchase.
         * @param info The purchase information (null if purchase failed)
         */
        void onIabPurchaseFinished(BillingResult result, PurchasedItem info);
    }
    
    // The listener registered on launchPurchaseFlow, which we have to call back when
    // the purchase finishes
    OnIabPurchaseFinishedListener mPurchaseListener;

    /**
     * Initiate the UI flow for an in-app purchase. Call this method to initiate an in-app purchase,
     * which will involve bringing up a Flexion billing screen.
     *
     * @param activity The calling activity.
     * @param item The item to purchase.
     * @param itemType indicates if it's a product or a subscription
     * @param listener The listener to notify when the purchase process finishes
     * @param developerPayload Extra data (developer payload), which will be returned with the purchase data
     *     when the purchase completes. This extra data will be permanently bound to that purchase
     *     and will always be returned when the purchase is queried.
     */
    public void launchPurchaseFlow(Activity activity, String item, ItemType itemType,
                        final OnIabPurchaseFinishedListener listener, String developerPayload) {
    	logDebug("BillingHelper.launchPurchaseFlow() called");
    	
        checkNotDisposed();
        
        try {     
            // Create a callback object to handle the response to the purchase attempt
            OnPurchaseFinishedCallback purchaseFinishedCallback = new OnPurchaseFinishedCallback() {
	            public void onSuccess(Purchase purchase) {
            		if (PurchaseState.PURCHASED == purchase.getState()) {
                        logDebug("Successfully made a purchase.");
                        handlePurchaseResult(purchase, listener, true, null);
            		}
            		else {
            			logDebug("Failed to make a purchase.");
            			handlePurchaseResult(purchase, listener, false, null);
            		}
            		
                    logDebug("Item Id: " + purchase.getItemId());
                    logDebug("Item Type: " + purchase.getItemType());
                    logDebug("Order Id: " + purchase.getOrderId());
                    logDebug("Developer Payload: " + purchase.getDeveloperPayload());
                    logDebug("Purchase Time: " + purchase.getPurchaseTime().toString());
                    logDebug("Purchase State: " + purchase.getState().toString());
                    logDebug("Purchase Token: " + purchase.getToken());
	            }
	            public void onPending (Purchase purchase) {
	            	logDebug("While running BillingHelper.launchPurchaseFlow.purchaseFinishedCallback, "
	            			+ "the following purchase was returned as pending: " + purchase.getItemId());
	            	handlePurchaseResult(purchase, listener, false, null);
	            }
	            public void onUserCanceled () {
	            	logDebug("While running BillingHelper.launchPurchaseFlow.purchaseFinishedCallback, "
	            			+ "a purchase was cancelled by the user");
	            	handlePurchaseResult(null, listener, false, null);
	            }
	            public void onError(BillingError error) {
	                logError("BillingHelper.launchPurchaseFlow.purchaseFinishedCallback returned an error. "
                            + "The error's description was:\n"
                            + error.getDescription());
	                handlePurchaseResult(null, listener, false, error);
	            }
            };
            
            logDebug("About to run mBillingService.launchPurchaseFlow()");
            mBillingService.launchPurchaseFlow(activity, item, itemType, developerPayload, purchaseFinishedCallback);
        }
        catch (Exception exception) {
           logException("Exception occurred in BillingHelper.launchPurchaseFlow()", exception);
            handlePurchaseResult(null, listener, false, null);
        }
    }
    
    /**
     * Handles the result of a purchase attempt. 
     * 
     * @param purchase - A Purchase object for the item we attempted to purchase
     * @param listener - A listener that will respond to the purchase result
     * @param purchaseSuccessful - A boolean indicating whether or not the purchase attempt was successful
     * @param error - A BillingError resulting from the purchase attempt. If no BillingError is available, 
     * for example if the purchase was successful, then this parameter can be set to null. 
     */
    private void handlePurchaseResult(Purchase purchase, OnIabPurchaseFinishedListener listener,
    		boolean purchaseSuccessful, BillingError error) {
    	BillingResult result;
    	
    	if (purchaseSuccessful) {
    		result = new BillingResult(true, "Successfully purchased item");
    		
        	// Use the Purchase to create a new PurchasedItem that we can pass to the listener
    		PurchasedItem purchasedItem = createPurchasedItemFromPurchase(purchase);
    		listener.onIabPurchaseFinished(result, purchasedItem);
    		return;
    	}
    	
    	// If we have an BillingError available, use its description
    	else if (error != null) {
    		result = new BillingResult(false, error.getDescription());
    	}
    	
    	// If we have an Purchase available, use its state
    	else if (purchase != null) {
    		result = new BillingResult(false, "Purchase state: " + purchase.getState().toString());
    	}
    	
    	// Otherwise, simply report the failure of the purchase attempt
    	else {
            result = new BillingResult(false, "Unable to buy item");
        }
    	
		// Pass the BillingResult reporting the failed purchase to the listener
		listener.onIabPurchaseFinished(result, null);
    }
    
    /**
     * Queries the inventory. This will query all owned items from the server, as well as
     * information on additional items, if specified.
     *
     * @param listener - A listener that will respond the to result of the query attempt
     * @param queryItemDetails if true, item details (price, description, etc) will be queried as well
     *     as purchase information.
     * @param moreItems additional PRODUCT items to query information on, regardless of ownership.
     *     Ignored if null or if queryItemDetails is false.
     * @throws BillingException if a problem occurs while refreshing the inventory.
     */
    public void queryInventory(QueryInventoryFinishedListener listener, boolean queryItemDetails, List<String> moreItems)
            throws BillingException {
        checkNotDisposed();
        try {
        	// Query for purchases
            queryPurchases(ItemType.IN_APP);
                       
            if (queryItemDetails) {
            	queryItemDetails(ItemType.IN_APP, moreItems);
            }

            // If subscriptions are supported, then also query for subscriptions
            if (SUBSCRIPTIONS_SUPPORTED) {
            	queryPurchases(ItemType.SUBSCRIPTION);

                if (queryItemDetails) {
                	queryItemDetails(ItemType.SUBSCRIPTION, moreItems);
                }
            }
            
            // Inform the listener that the inventory query has finished
            listener.onQueryInventoryFinished(new BillingResult(true, "Successfully queried inventory"), mInventory);
        }
        catch (Exception exception) {
            logException("Exception occurred in BillingHelper.queryInventory()", exception);
            
            // Inform the listener that the inventory query failed
            listener.onQueryInventoryFinished(new BillingResult(true, "Failed to query the inventory"), mInventory);
        }
    }
    
    /**
     * Listener that notifies when an inventory query operation completes.
     */
    public interface QueryInventoryFinishedListener {
        /**
         * Called to notify that an inventory query operation completed.
         *
         * @param result The result of the operation.
         * @param inv The inventory.
         */
        void onQueryInventoryFinished(BillingResult result, Inventory inv);
    }
    
    /**
     * Consumes a given in-app product. Consuming can only be done on an item
     * that's owned, and as a result of consumption, the user will no longer own it.
     *
     * @param purchasedItem The PurchaseInfo that represents the item to consume.
     * @param listener - A listener to wait for a single item to be consumed
     */
    public void consume(final PurchasedItem purchasedItem, final OnConsumeFinishedListener listener) {
    	
        checkNotDisposed();
        
        // Check that the item is of the IN_APP type - only items of this type can be consumed
        if (!purchasedItem.getItemType().equals(ItemType.IN_APP)) {
        	logError("Items of type '" + purchasedItem.getItemType() + "' can't be consumed.");
        }
        
        try {
        	// Check whether the PurchasedItems's token is present
            String token = purchasedItem.getToken();
            String itemId = purchasedItem.getItemId();
            if (token == null || token.equals("")) {
                logError("PurchaseInfo is missing token for item: " + itemId + " " + purchasedItem);
            }
            
            logDebug("Consuming item: " + itemId + ", token: " + token);
            
            // Create the callback instance and make the 'consume item' request
            OnConsumeFinishedCallback onConsumeFinishedCallBack = new OnConsumeFinishedCallback() {
            	public void onSuccess() {
            		logDebug("OnConsumeFinishedCallback.onSuccess() called");
            		mInventory.erasePurchase(purchasedItem.getItemId());
            		logDebug("Successfully consumed item: " + purchasedItem.getItemId());
            		
                    // Inform the listener that the item has been consumed
                    listener.onConsumeFinished(purchasedItem, new BillingResult(true, "Successfully consumed item"));
            	}
            	public void onError(BillingError error) {
	                logError("BillingHelper.OnConsumeFinishedCallback() returned an error. The error's description was:\n"
                            + error.getDescription());

                    // Inform the listener that the item has not been consumed
                    listener.onConsumeFinished(purchasedItem, new BillingResult(false, "An error occurred while consuming an item"));
	            }
            };
            mBillingService.consumePurchase(token, onConsumeFinishedCallBack);
        }
        catch (Exception exception) {
            logException("Exception occurred in BillingHelper.consume()", exception);
        }
    }
    
    /**
     * Callback that notifies when a consumption operation finishes.
     */
    public interface OnConsumeFinishedListener {
        /**
         * Called to notify that a consumption has finished.
         *
         * @param purchase The purchase that was (or was to be) consumed.
         * @param result The result of the consumption operation.
         */
        void onConsumeFinished(PurchasedItem purchase, BillingResult result);
    }
    
    /**
     * Makes a query for purchases of the given item type
     * 
     * @param itemType - The item type which we are querying for
     * 
     * @throws JSONException
     * @throws RemoteException
     */
    private void queryPurchases(ItemType itemType) throws JSONException, RemoteException {
        logDebug("Querying owned items, item type: " + itemType);
        logDebug("Package name: " + mContext.getPackageName());
        
        try {
	        // Create a callback object to handle any response to the purchases query
	        OnQueryGetPurchasesFinishedCallback queryGetPurchasesCallback = new OnQueryGetPurchasesFinishedCallback() {
	        	@Override
	            public void onSuccess(ItemType itemType, Map<String, Purchase> purchases)
	            {
					for (Map.Entry<String, Purchase> entry : purchases.entrySet()) {
					    // Log the ID of the owned item
						Purchase purchase = entry.getValue();
					    logDebug("Item is owned: " + purchase.getItemId());
					    
					    // Take each Purchase and use it to create a new PurchasedItem to go in the inventory
					    PurchasedItem purchasedItem = createPurchasedItemFromPurchase(purchase);
						
					    // Add the PurchasedItem to the inventory
						mInventory.addPurchase(purchasedItem);
					}
	            }
	        	@Override
	            public void onError(BillingError error) {
	                logError("BillingHelper.OnQueryGetPurchasesFinishedCallback() returned an error. The error's description was:\n"
                            + error.getDescription());
	            }
	        };
	        
	        // Make the 'get purchases' query call. Passing in an empty List gives us all
	        // purchased items rather than only some.
	        mBillingService.getPurchases(itemType, new ArrayList<String>(), queryGetPurchasesCallback);
        }
        catch (Exception exception) {
        	logException("Exception occurred in BillingHelper.queryPurchases()", exception);
        }
    }
    
    /**
     * Makes an query for the details of all items of the given item type.
     * If retrieved successfully, these item details are added to the
     * mInventory instance. 
     * 
     * @param itemType - The item type to make the query for
     * @param moreItems - Any items to be queried which are not already
     * part of the supplied Inventory object.
     */
    private void queryItemDetails(ItemType itemType, List<String> moreItems) {
        try {	
	    	logDebug("Querying Item details.");
	    	
	    	// Create the list of items to get the details of
	        ArrayList<String> itemList = new ArrayList<String>();
	        itemList.addAll(mInventory.getAllOwnedItems(itemType));
	        if (moreItems != null) {
	            for (String item : moreItems) {
	                if (!itemList.contains(item)) {
	                    itemList.add(item);
	                }
	            }
	        }
	        
	        // If there are no items to get the details of, return
	        if (itemList.size() == 0) {
	            logDebug("BillingHelper.queryPrices(): nothing to do because there are no items.");
	            return;
	        }
	
	        // Create a callback object for handling the response to the item details query 
	        OnQueryItemDetailsFinishedCallback queryItemDetailsCallBack = new OnQueryItemDetailsFinishedCallback() {
	        	@Override
	        	public void onSuccess (ItemType itemType, Map<String, Item> detailsMap) {
	        		// Add each item to the inventory
			        for (String itemId : detailsMap.keySet()) {
			        	final Item item = detailsMap.get(itemId);
			        	logDebug("Got item: " + item.getId());
				        mInventory.addItemDetails(item);
			        }
			    }
	
				@Override
				public void onError(BillingError error) {
	                logError("BillingHelper.OnQueryItemDetailsFinishedCallback() returned an error. The error's description was:\n"
                            + error.getDescription());
				}
	        };
	        
	        // Make the query call for the item details
	        mBillingService.getItemDetails(ItemType.IN_APP, itemList, queryItemDetailsCallBack);
        }
        catch (Exception exception) {
        	logException("Exception occurred in BillingHelper.queryItemDetails()", exception);
        }
    }
    
    /**
     * Takes a Purchase and use it to create a new PurchasedItem
     * 
     * @param purchase - The Purchase to use
     * 
     * @return The new PurchasedItem
     */
    private PurchasedItem createPurchasedItemFromPurchase(Purchase purchase){
	    long purchaseTime = purchase.getPurchaseTime().getTime();
	    return new PurchasedItem(purchase.getItemType(), purchase.getOrderId(), purchase.getItemId(),
	    		purchaseTime, purchase.getState(), purchase.getDeveloperPayload(), purchase.getToken());
    }
    
    /**
     * If debug logging is enabled, logs a debugging message
     *  
     * @param message - The message to be logged
     */
    private void logDebug(String message) {
        if (DEBUG_LOGGING_ENABLED) {
        	Log.d(TAG, message);
        }
    }
    
    /**
     * Logs an exception
     *  
     * @param message - A message about the exception to be logged
     * @param exception - The exception to be logged
     */
    private void logException(String message, Exception exception) {
        Log.e(TAG, message, exception);
    }

    /**
     * Logs an error message
     *
     * @param message - The error message to be logged
     */
    private void logError(String message) {
        Log.e(TAG, message);
    }
}