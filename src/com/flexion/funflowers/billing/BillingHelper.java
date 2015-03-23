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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
	
    /** Is setup done? */
    boolean mSetupDone = false;

    /** Has this object been disposed of? (If so, we should ignore callbacks, etc) */
    boolean mDisposed = false;
	
    /** Context we were passed during initialization */
    Context mContext;
    
    /** The FlexionBillingService instance */
    private FlexionBillingService mBillingService;

    /** The request code used to launch purchase flow */
    int mRequestCode;

    /** The item type of the current purchase flow */
    ItemType mPurchasingItemType;
    
    /** The inventory of purchasable items */
    private Inventory mInventory;
    
    /** Are subscriptions supported? */
    private static final boolean SUBSCRIPTIONS_SUPPORTED = true;
    
    /** The amount of time which we allow for billing requests */
    private static final int REQUEST_TIMEOUT_SECONDS = 60;
    
    /** A flag used for recording whether the most recent 'get purchases' query
     * was successful.*/
    private boolean mPurchasesQuerySuccessful;
    
    /** A flag used for recording whether the most recent 'get item details' query
     * was successful.*/
    private boolean mItemDetailsQuerySuccessful;
    
    
    /** A flag used for recording whether the most recent 'consume item' attempt
     * was successful*/
    private boolean mConsumeAttemptSuccessful;
    
    /** This value controls whether or not debug messages will be logged. This should be
     * set to false in production usage */
    private static final boolean DEBUG_LOGGING_ENABLED = true;
    
    /** The tag used to mark log messages from this class */
    private static final String TAG = "BillingHelper";
    
    /**
     * Creates an instance. After creation, it will not yet be ready to use. You must perform
     * setup by calling {@link #startSetup} and wait for setup to complete. This constructor does not
     * block and is safe to call from a UI thread.
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
     * Dispose of object, releasing resources. It's very important to call this
     * method when you are done with this object. It will release any resources
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
        public void onIabPurchaseFinished(BillingResult result, PurchasedItem info);
    }

    // The listener registered on launchPurchaseFlow, which we have to call back when
    // the purchase finishes
    OnIabPurchaseFinishedListener mPurchaseListener;

    public void launchPurchaseFlow(Activity act, String item, int requestCode, OnIabPurchaseFinishedListener listener) {
        launchPurchaseFlow(act, item, requestCode, listener, "");
    }

    public void launchPurchaseFlow(Activity act, String item, int requestCode,
            OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, item, ItemType.IN_APP, requestCode, listener, extraData);
    }

    public void launchSubscriptionPurchaseFlow(Activity act, String item, int requestCode,
            OnIabPurchaseFinishedListener listener) {
        launchSubscriptionPurchaseFlow(act, item, requestCode, listener, "");
    }

    public void launchSubscriptionPurchaseFlow(Activity act, String item, int requestCode,
            OnIabPurchaseFinishedListener listener, String extraData) {
        launchPurchaseFlow(act, item, ItemType.SUBSCRIPTION, requestCode, listener, extraData);
    }

    /**
     * Initiate the UI flow for an in-app purchase. Call this method to initiate an in-app purchase,
     * which will involve bringing up a Flexion billing screen. The calling activity will be paused while
     * the user interacts with the billing UI, and the result will be delivered via the activity's
     * {@link android.app.Activity#onActivityResult} method, at which point you must call
     * this object's {@link #handleActivityResult} method to continue the purchase flow. This method
     * MUST be called from the UI thread of the Activity.
     *
     * @param activity The calling activity.
     * @param item The item to purchase.
     * @param itemType indicates if it's a product or a subscription
     * @param requestCode A request code (to differentiate from other responses --
     *     as in {@link android.app.Activity#startActivityForResult}).
     * @param listener The listener to notify when the purchase process finishes
     * @param developerPayload Extra data (developer payload), which will be returned with the purchase data
     *     when the purchase completes. This extra data will be permanently bound to that purchase
     *     and will always be returned when the purchase is queried.
     */
    public void launchPurchaseFlow(Activity activity, String item, ItemType itemType, int requestCode,
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
        catch (Exception e) {
           logError("Exception occurred in BillingHelper.launchPurchaseFlow(). The exception message was:\n"
            		+ e.getMessage());
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
     * @param moreSubscriptionItems additional SUBSCRIPTIONS items to query information on, regardless of ownership.
     *     Ignored if null or if queryItemDetails is false.
     * @throws BillingException if a problem occurs while refreshing the inventory.
     */
    public void queryInventory(QueryInventoryFinishedListener listener, boolean queryItemDetails, List<String> moreItems,
                                        List<String> moreSubscriptionItems) throws BillingException { 
        checkNotDisposed();
        try {
            boolean querySuccess = queryPurchases(ItemType.IN_APP);
            if (!querySuccess) {
                throw new BillingException("Error refreshing inventory (querying owned items).");
            }
            
            if (queryItemDetails) {
            	querySuccess = queryItemDetails(ItemType.IN_APP, moreItems);
                if (!querySuccess) {
                    throw new BillingException("Error refreshing inventory (querying item details).");
                }
            }

            // If subscriptions are supported, then also query for subscriptions
            if (SUBSCRIPTIONS_SUPPORTED) {
            	querySuccess = queryPurchases(ItemType.SUBSCRIPTION);
                if (!querySuccess) {
                    throw new BillingException("Error refreshing inventory (querying owned subscriptions).");
                }

                if (queryItemDetails) {
                	querySuccess = queryItemDetails(ItemType.SUBSCRIPTION, moreItems);
                    if (!querySuccess) {
                        throw new BillingException("Error refreshing inventory (querying subscription details).");
                    }
                }
            }

            // Inform the listener that the inventory query succeeded
            listener.onQueryInventoryFinished(new BillingResult(true, "Successfully queried inventory"), mInventory);
        }
        catch (Exception e) {
            logError("Exception occurred in BillingHelper.queryInventory(). The exception message was:\n"
            		+ e.getMessage());
            
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
        public void onQueryInventoryFinished(BillingResult result, Inventory inv);
    }

    /**
     * Consumes a given in-app product. Consuming can only be done on an item
     * that's owned, and as a result of consumption, the user will no longer own it.
     *
     * @param purchasedItem The PurchaseInfo that represents the item to consume.
     * @param listener - A listener to wait for a single item to be consumed
     * 
     * @throws BillingException if there is a problem during consumption.
     */
    public void consume(final PurchasedItem purchasedItem, OnConsumeFinishedListener listener) {
    	
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
            
	        // Set up a CountDownLatch to await the result of the item details query
	        final CountDownLatch latch = new CountDownLatch(1);
            
            // Reset the flag that records whether or not the latest consume attempt was successful
            mConsumeAttemptSuccessful = false;
            
            OnConsumeFinishedCallback onConsumeFinishedCallBack = new OnConsumeFinishedCallback() {
            	public void onSuccess() {
            		logDebug("OnConsumeFinishedCallback.onSuccess() called");
            		mInventory.erasePurchase(purchasedItem.getItemId());
            		mConsumeAttemptSuccessful = true;
            		latch.countDown();
            	}
            	public void onError(BillingError error) {
	                logError("BillingHelper.OnQueryGetPurchasesFinishedCallback() returned an error. The error's description was:\n"
	                		+ error.getDescription());
	                latch.countDown();
	            }
            };
            
            // Create the callback instance and make the 'consume item' request
            mBillingService.consumePurchase(token, onConsumeFinishedCallBack);
            
	        // Await the result of the request
	        latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
	        // Respond to the result of the request
            if (mConsumeAttemptSuccessful) {
               logDebug("Successfully consumed item: " + itemId);
            }
            else {
               logDebug("Failed to consume item " + itemId);
            }
            
            // Inform the listener that the item has been consumed
            listener.onConsumeFinished(purchasedItem, new BillingResult(true, "Successfully consumed item"));
        }
        catch (Exception e) {
            logError("Exception occurred in BillingHelper.consume(). The exception message was:\n"
            		+ e.getMessage());
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
        public void onConsumeFinished(PurchasedItem purchase, BillingResult result);
    }

    /**
     * Callback that notifies when a multi-item consumption operation finishes.
     */
    public interface OnConsumeMultiFinishedListener {
        /**
         * Called to notify that a consumption of multiple items has finished.
         *
         * @param purchases The purchases that were (or were to be) consumed.
         * @param results The results of each consumption operation, corresponding to each item.
         */
        public void onConsumeMultiFinished(List<PurchasedItem> purchases, List<BillingResult> results);
    }
        
    /**
     * Makes a query for purchases of the given item type
     * 
     * @param itemType - The item type which we are querying for
     * 
     * @return A boolean indicating the success or failure of the query
     * 
     * @throws JSONException
     * @throws RemoteException
     */
    private boolean queryPurchases(ItemType itemType) throws JSONException, RemoteException {
        logDebug("Querying owned items, item type: " + itemType);
        logDebug("Package name: " + mContext.getPackageName());
        
        try {
	        // Set up a CountDownLatch to await the result of the item details query
	        final CountDownLatch latch = new CountDownLatch(1);
	        
	        // Reset the flag that records the success or failure of this query
	        mPurchasesQuerySuccessful = false;
	
	        // Create a callback object to handle any response to the purchases query
	        OnQueryGetPurchasesFinishedCallback queryGetPurchasesCallback = new OnQueryGetPurchasesFinishedCallback() {
	        	@Override
	            public void onSuccess(ItemType itemType, Map<String, Purchase> purchases)
	            {
					for (Map.Entry<String, Purchase> entry : purchases.entrySet())
					{
					    // Log the ID of the owned item
						Purchase purchase = entry.getValue();
					    logDebug("Item is owned: " + purchase.getItemId());
					    
					    // Take each Purchase and use it to create a new PurchasedItem to go in the inventory
					    PurchasedItem purchasedItem = createPurchasedItemFromPurchase(purchase);
						
					    // Add the PurchasedItem to the inventory
						mInventory.addPurchase(purchasedItem);
					}
	        		
	        		// Report the success of the query
	        		mPurchasesQuerySuccessful = true;
	        		latch.countDown();
	            }
	        	@Override
	            public void onError(BillingError error) {
	                logError("BillingHelper.OnQueryGetPurchasesFinishedCallback() returned an error. The error's description was:\n"
	                		+ error.getDescription());
	                
	                // Report the failure of the query, along with the error message
			        mItemDetailsQuerySuccessful = false;
	                latch.countDown();
	            }
	        };
	        
	        // Make the 'get purchases' query call. Passing in an empty List gives us all
	        // purchased items rather than only some.
	        mBillingService.getPurchases(itemType, new ArrayList<String>(), queryGetPurchasesCallback);
	        
	        // Await the result of the item details query
	        latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
	        
	        // Return whether or not the query was successful
	        return mPurchasesQuerySuccessful;
        }
        catch (Exception e)
        {
        	logError("Exception occurred in BillingHelper.queryPurchases(). The exception message was:\n"
        			+ e.getMessage());
        	return false;
        }
    }
    
    /**
     * Makes an query for the details of all items of the given item type.
     * If retrieved successfully, these item details are added to the
     * mInventory instance. 
     * 
     * @param itemType - The item type to make the query for
     * @param moreItems - Any items to be queried which are not already
     * part of the supplied Inventory object
     * 
     * @return A boolean indicating the success or failure of the query
     */
    private boolean queryItemDetails(ItemType itemType, List<String> moreItems) {
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
	            return true;
	        }
	        
	        // Set up a CountDownLatch to await the result of the item details query
	        final CountDownLatch latch = new CountDownLatch(1);
	        
	        // Reset the flag that records whether this query is successful
	        mItemDetailsQuerySuccessful = false;
	
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
			        
			        // Report the success of the query
			        mItemDetailsQuerySuccessful = true;
			        latch.countDown();
			    }
	
				@Override
				public void onError(BillingError error) {
	                logError("BillingHelper.OnQueryItemDetailsFinishedCallback() returned an error. The error's description was:\n"
	                		+ error.getDescription());
	                
	                // Report the failure of the query, along with the error message
			        mItemDetailsQuerySuccessful = false;
	                latch.countDown();
				}
	        };
	        
	        // Make the query call for the item details
	        mBillingService.getItemDetails(ItemType.IN_APP, itemList, queryItemDetailsCallBack);
	        
	        // Await the result of the item details query
	        latch.await(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
	        
	        // Return whether or not the query was successful
	        return mItemDetailsQuerySuccessful;
        }
        catch (Exception e)
        {
        	logError("Exception occurred in BillingHelper.queryItemDetails(). The exception message was:\n"
        			+ e.getMessage());
        	return false;
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
     * Logs an error message
     *  
     * @param message - The message to be logged
     */
    private void logError(String message) {
        Log.e(TAG, message);
    }
}