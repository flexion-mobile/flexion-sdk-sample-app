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

package com.flexion.funflowers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.flexion.funflowers.billing.BillingHelper;
import com.flexion.funflowers.billing.BillingResult;
import com.flexion.funflowers.billing.Inventory;
import com.flexion.funflowers.billing.PurchasedItem;
import com.flexionmobile.sdk.billing.ItemType;

/**
Fun Flowers<br><br>

Example game using the Flexion billing SDK. The app uses a local simulated Flexion
billing server, so it should work as a stand-alone application. <br><br>

This app is a simple game where the player can buy seeds and use it to 'grow'
randomly generated flowers. The player starts the game with a set amount of seeds. 
When the player grows a new flower, they consume a seed. If the player runs 
out of seeds, they can buy more using an in-app purchase.<br><br>

The user can also purchase a "premium upgrade" that unlocks a special theme
for the app.<br><br>

The user can also purchase a subscription ("magical water") which will 
make the flowers they grow larger and possibly more beautiful. <br><br>

It's important to note the consumption mechanics for each item:<br><br>

PREMIUM THEME: the item is purchased and NEVER consumed. So, after the original
purchase, the player will always own that item. The application knows to
display the special picture because it queries whether the premium "item" is
owned or not.<br><br>

MAGICAL WATER: this is a subscription, and subscriptions can't be consumed.<br><br>

SEEDS: when seeds are purchased, the "seeds" item is then owned. We
consume it when we apply that item's effects to our app's world, which to
us means giving the player a fixed number of seeds. This happens immediately
after purchase! It's at this point (and not when the user drives) that the
"seeds" item is CONSUMED. Consumption should always happen when your game
world was safely updated to apply the effect of the purchase. So, in an
example scenario:<br><br>

BEFORE:      the player has 5 seeds<br>
ON PURCHASE: the player has 5 seeds, "seeds" item is owned<br>
IMMEDIATELY: the player has 25 seeds, "seeds" item is consumed<br>
AFTER:       the player has 25 seeds, "seeds" item NOT owned any more<br><br>

Another important point to notice is that it may so happen that
the application crashed (or anything else happened) after the user
purchased the "seeds" item, but before it was consumed. That's why,
on startup, we check if we own the "seeds" item, and, if so,
we have to apply its effects to our world and consume it. This
is also very important!<br><br>

@author TrivalDrive originally by Bruno Oliveira (Google). Modified to
Fun Flowers by Jonathan Coe (Flexion). 
 */
public class FlowerActivity extends Activity {
	
    /** Does the user have the premium upgrade? */
    private boolean mIsPremium = false;

    /** Is the user currently subscribed to the 'magical water' upgrade? */
    private boolean mSubscribedToMagicalWater = false;

    /** Item ID for the premium theme item */
    private static final String ITEM_PREMIUM_THEME = "1023610";
    
    /** Item ID for the seeds item */
    private static final String ITEM_SEEDS = "1023608";

    /** Item ID for the magical water item */
    private static final String ITEM_MAGICAl_WATER = "1023609";
    
    /** The number of displayed seeds that a purchase of one "seeds" item corresponds to.
     e.g. If the player buys one "seeds" item then they will receive this many 'seeds'
     to grow flowers with */
    private static final int SEEDS_PER_PURCHASE = 20;
    
    /** (arbitrary) request code for the purchase flow */
    private static final int RC_REQUEST = 10001;
    
    /** The number of seeds that the player starts with when they first run the game */
    private static final int PLAYER_STARTING_SEEDS = 20;
        
    /** A key value used to reference a stored variable that records the player's
     * available number of seeds */
    private static final String KEY_PLAYER_SEEDS = "key_player_seeds";
        
    /** Current number of seeds that the player has */
    private long mPlayerSeeds;
    
    /** An int value that references the resource ID for the current flower top to use */ 
    private int mCurrentFlowerTopId;
    
    /** An int value that references the resource ID for the current flower bottom to use */ 
    private int mCurrentFlowerBottomId;
    
    /** A boolean that records whether or not the player has grown a flower yet */
    private boolean mFlowerGrown;
    
    /** Defines the size of the enlarged layout area to be used when the user is subscribed to
     * the 'magical water' upgrade */
    private static final int ENLARGED_FLOWER_LAYOUT_SIZE = 300;
    
    /** The in-app-billing helper object */
    private BillingHelper mBillingHelper;
    
	/** The tag used to mark log messages from this class */
    private static final String TAG = "FlowerActivity";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flower);
        
        try {
            // Load game data
            loadData();
            
            // Create an instance of the helper class for in-app-billing
            Log.d(TAG, "Creating IabHelper instance");
            mBillingHelper = new BillingHelper(this);
            
            // Use the billing helper to query for inventory items
            mBillingHelper.queryInventory(mGotInventoryListener, true, null, null);
        }
        catch (Exception e) {
        	Log.e(TAG, "Exception occurred in FlowerActivity.onCreate(). The exception message was: "
        			+ e.getMessage());
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Dispose of the billing helper instance
        Log.d(TAG, "Destroying helper.");
        if (mBillingHelper != null) {
            mBillingHelper.dispose();
            mBillingHelper = null;
        }
    }
    
    // User clicked the 'grow flower' button
	public void onGrowFlowerButtonClicked(View arg0) {
        Log.d(TAG, "Grow flower button clicked");
        
        // Check whether the player has any available seeds
        if (mPlayerSeeds <= 0)
        {
        	displayAlert("Oh no! You have run out of seeds! Buy some more so you can keep growing flowers!");
        	return;
        }
        
        // Pick new flower parts to be displayed
        int[] flowerPartIds = FlowerPicker.pickFlowerParts();
        mCurrentFlowerTopId = flowerPartIds[0];
        mCurrentFlowerBottomId = flowerPartIds[1];
        
        // Record that the player has grown a flower
        mFlowerGrown = true;
        
        // Take one seed from the player
        mPlayerSeeds --;
        
        saveData();
        updateUi();
        
        // Log the player's new balance
        Log.d(TAG, "The player now has " + mPlayerSeeds + " seeds");
    }
    
    // User clicked the "Buy Seeds" button
    public void onBuySeedsButtonClicked(View arg0) {
        Log.d(TAG, "Buy seeds button clicked.");
        
        // launch the seeds purchase UI flow.
        // We will be notified of completion via mPurchaseFinishedListener
        setWaitScreen(true);
        Log.d(TAG, "Launching purchase flow for seeds");

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a sample, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "";
        
        try {
            mBillingHelper.launchPurchaseFlow(this, ITEM_SEEDS, RC_REQUEST,
                    mPurchaseFinishedListener, payload);
        }
        catch (Exception e) {
        	Log.e(TAG, "Exception occurred in FlowerActivity.onBuySeedsButtonClicked(). The exception message was:\n"
        			+ e.getMessage());
            displayAlert("Unfortunately your purchase could not be completed.\n\n"
            		+ "Message: " + e.getMessage());
        }
    }

    // User clicked the "Upgrade to Premium" button.
    public void onUpgradeAppButtonClicked(View arg0) {
        Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
        setWaitScreen(true);

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a sample, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "";
        
        try {
	        mBillingHelper.launchPurchaseFlow(this, ITEM_PREMIUM_THEME, RC_REQUEST,
	                mPurchaseFinishedListener, payload);
        }
        catch (Exception e) {
        	Log.e(TAG, "Exception occurred in FlowerActivity.onUpgradeAppButtonClicked(). The exception message was:\n"
        			+ e.getMessage());
            displayAlert("Unfortunately your purchase could not be completed.\n\n"
            		+ "Message: " + e.getMessage());
        }
    }

    // "Subscribe to magical water" button clicked. Explain to user, then start purchase
    // flow for subscription.
    public void onBuyMagicalWaterButtonClicked(View arg0) {
        if (!mBillingHelper.subscriptionsSupported()) {
            complain("Subscriptions are not supported on your device yet. Sorry!");
            return;
        }
        
        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a sample, we just use
         *        an empty string, but on a production app you should carefully generate this. */
        String payload = "";
        
        setWaitScreen(true);
        Log.d(TAG, "Launching purchase flow for magical water subscription.");
        
        try {
	        mBillingHelper.launchPurchaseFlow(this,
	                ITEM_MAGICAl_WATER, ItemType.SUBSCRIPTION,
	                RC_REQUEST, mPurchaseFinishedListener, payload);
	    }
	    catch (Exception e) {
	    	Log.e(TAG, "Exception occurred in FlowerActivity.onBuyMagicalWaterButtonClicked(). The exception message was:\n"
	    			+ e.getMessage());
	        displayAlert("Unfortunately your purchase could not be completed.\n\n"
	        		+ "Message: " + e.getMessage());
	    }
    }

    /** Verifies the developer payload of a purchase. */
    private boolean verifyDeveloperPayload(PurchasedItem p) {
        /*
         * TODO: Verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */
        return true;
    }
    
    // Listener that's called when we finish querying the items and subscriptions we own
    private BillingHelper.QueryInventoryFinishedListener mGotInventoryListener = new BillingHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(BillingResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished");

            // Have we been disposed of in the meantime? If so, quit.
            if (mBillingHelper == null) return;

            // Did we fail to query the inventory
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }

            Log.d(TAG, "Query inventory was successful");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium theme?
            PurchasedItem premiumPurchase = inventory.getPurchase(ITEM_PREMIUM_THEME);
            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            Log.d(TAG, "User " + (mIsPremium ? "IS premium" : "is NOT premium"));

            // Do we have the  magical water subscription?
            PurchasedItem magicalWaterPurchase = inventory.getPurchase(ITEM_MAGICAl_WATER);
            mSubscribedToMagicalWater = (magicalWaterPurchase != null &&
                    verifyDeveloperPayload(magicalWaterPurchase));
            Log.d(TAG, "User " + (mSubscribedToMagicalWater ? "HAS" : "does NOT have")
                        + " magical water subscription");

            // Check for newly purchased seeds -- if we own seeds, we should add them to the player's 
            // available seeds immediately
            PurchasedItem seedsPurchase = inventory.getPurchase(ITEM_SEEDS);
            if (seedsPurchase != null && verifyDeveloperPayload(seedsPurchase)) {
                Log.d(TAG, "We have a unit of purchased seeds available - consuming it");
                mBillingHelper.consume(inventory.getPurchase(ITEM_SEEDS), mConsumeFinishedListener);
                return;
            }
            
            Log.d(TAG, "Initial inventory query finished; enabling main UI");
            updateUi();
            setWaitScreen(false);
        }
    };

    // Callback for when a purchase is finished
    private BillingHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new BillingHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(BillingResult result, PurchasedItem purchase) {
        	try {
	            // If we were disposed of in the meantime, quit.
	            if (mBillingHelper == null) return;
	        	
	            // Check whether we were passed a purchase
	        	if (purchase != null) {
	        		Log.d(TAG, "Purchase attempt finished of item " + purchase.getItemId() + " finished. Result: " + result);
	        	}
	        	else {
	        		Log.d(TAG, "Purchase attempt failed. Result: " + result);
	        	}
	        	
	        	// Check whether the purchase result was a success or failure and
	        	// respond accordingly
	            if (result.isFailure()) {
	                displayAlert("Unfortunately your purchase could not be completed.\n\n"
	                		+ "Message: " + result.getMessage());
	                setWaitScreen(false);
	                // Use the billing helper to query for inventory items
	                mBillingHelper.queryInventory(mGotInventoryListener, true, null, null);
	                return;
	            }
	            
	            // Attempt to verify the developer payload and respond accordingly
	            if (!verifyDeveloperPayload(purchase)) {
	                complain("Error purchasing item. Developer payload verification failed.");
	                setWaitScreen(false);
	                // Use the billing helper to query for inventory items
	                mBillingHelper.queryInventory(mGotInventoryListener, true, null, null);
	                return;
	            }
	            
	            // If we reached this point, the purchase was successful
	            Log.d(TAG, "Purchase successful.");
	
	            if (purchase.getItemId().equals(ITEM_SEEDS)) {
	                // The user purchased one unit of seeds. Consume it.
	                Log.d(TAG, "Purchase is seeds. Starting seed consumption.");
	                mBillingHelper.consume(purchase, mConsumeFinishedListener);
	            }
	            else if (purchase.getItemId().equals(ITEM_PREMIUM_THEME)) {
	                // The user purchased the premium upgrade!
	                Log.d(TAG, "Purchase is premium theme. Congratulating user.");
	                displayAlert("Thank you for upgrading to premium!");
	                mIsPremium = true;
	                updateUi();
	                setWaitScreen(false);
	            }
	            else if (purchase.getItemId().equals(ITEM_MAGICAl_WATER)) {
	                // The user purchased the magical water subscription
	                Log.d(TAG, "Magical water subscription purchased.");
	                displayAlert("Thank you for subscribing to the magical water upgrade!");
	                mSubscribedToMagicalWater = true;
	                updateUi();
	                setWaitScreen(false);
	            }
        	}
        	catch (Exception e) {
        		Log.e(TAG, "Exception occurred in FlowerActivity.OnIabPurchaseFinishedListener. The exception message was:\n"
        				+ e.getMessage());
        	}
        }
    };

    // Called when consumption is complete
    private BillingHelper.OnConsumeFinishedListener mConsumeFinishedListener = new BillingHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(PurchasedItem purchase, BillingResult result) {
            Log.d(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);
            
            // if we were disposed of in the meantime, quit.
            if (mBillingHelper == null) return;

            // We know this is the "seeds" item because it's the only one we consume,
            // so we don't check which item was consumed. If you have more than one
            // consumable item, you should check...
            if (result.isSuccess()) {
                // Successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means adding seeds to the player's account
                Log.d(TAG, "Consumption successful. Provisioning.");
                mPlayerSeeds = mPlayerSeeds + SEEDS_PER_PURCHASE;
                saveData();
                displayAlert("You purchased " + SEEDS_PER_PURCHASE + " seeds!\n\n"
                		+ "You now have " + mPlayerSeeds + " seeds to grow flowers with!");
            }
            else {
                complain("Error while consuming: " + result);
            }
            updateUi();
            setWaitScreen(false);
            Log.d(TAG, "End consumption flow");
        }
    };

    // Updates the UI to reflect the model
    private void updateUi() {
    	runOnUiThread(new Runnable() {
    	    public void run() {
		        // Update the UI to reflect premium status or lack thereof
    	    	if (mIsPremium) {
    	    		((ImageView)findViewById(R.id.free_or_premium)).setImageResource(R.drawable.premium_theme);
    	    		findViewById(R.id.title).setVisibility(View.GONE);
    	    	}
    	    	else {
    	    		((ImageView)findViewById(R.id.free_or_premium)).setImageResource(R.drawable.free);
    	    	}
		
		        // "Upgrade" button is only visible if the user is not premium
		        findViewById(R.id.upgrade_button).setVisibility(mIsPremium ? View.GONE : View.VISIBLE);
		
		        // "Upgrade to magical water" button is only visible if the user is not subscribed yet
		        findViewById(R.id.buy_magical_water_button).setVisibility(mSubscribedToMagicalWater ? View.GONE : View.VISIBLE);
		
		        // Update displayed available funds
		        TextView playerSeedsTextView = (TextView) findViewById(R.id.player_seeds);
		        playerSeedsTextView.setText("Seeds: " + String.valueOf(mPlayerSeeds));
		        
		        if (mFlowerGrown) {
			        // Update the displayed flower components (currently top and bottom)
			        ImageView flowerTop = ((ImageView)findViewById(R.id.flower_top));
			        ImageView flowerBottom = ((ImageView)findViewById(R.id.flower_bottom));
			        flowerTop.setImageResource(mCurrentFlowerTopId);
			        flowerBottom.setImageResource(mCurrentFlowerBottomId);
			        
			        // If the user is subscribed to the 'magical water' upgrade, enlarge the flowers displayed
			        if (mSubscribedToMagicalWater) {
						LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ENLARGED_FLOWER_LAYOUT_SIZE, ENLARGED_FLOWER_LAYOUT_SIZE);
						flowerTop.setLayoutParams(layoutParams);
						flowerBottom.setLayoutParams(layoutParams);	
			        }
		        }
    	    }
    	});
    }

    /** Enables or disables the "please wait" screen. */
    private void setWaitScreen(final boolean set) {
    	runOnUiThread(new Runnable() {
    	    public void run() {
		        findViewById(R.id.screen_main).setVisibility(set ? View.GONE : View.VISIBLE);
		        findViewById(R.id.screen_wait).setVisibility(set ? View.VISIBLE : View.GONE);
    	    }
    	});
    }
    
    /** 
     * Takes an error messages and:<br>
     * 	i)  Logs it<br>
     * 	ii) Displays an alert dialog with the message to the user 
     */
    private void complain(final String message) {
    	runOnUiThread(new Runnable() {
    	    public void run() {
		        Log.e(TAG, "**** Fun Flowers Error: " + message);
		        displayAlert("Error: " + message);
    	    }
    	});
    }
    
    /**
     * Display an alert message to the user
     * 
     * @param message - The alert message to display
     */
    private void displayAlert(final String message) {
    	runOnUiThread(new Runnable() {
    	    public void run() {
		        AlertDialog.Builder builder = new AlertDialog.Builder(FlowerActivity.this);
		        builder.setMessage(message);
		        builder.setNeutralButton("OK", null);
		        Log.d(TAG, "Showing alert dialog: " + message);
		        builder.create().show();
    	    }
    	});
    }
    
    /**
     * Saves the player's game state. 
     */
    private void saveData() {
        /*
         * Note: in a real application, we recommend you save data in a secure way to
         * prevent tampering. For simplicity in this sample, we simply store the player data
         * in SharedPreferences.
         */
        SharedPreferences.Editor sharedPrefs = getPreferences(MODE_PRIVATE).edit();
        sharedPrefs.putLong(KEY_PLAYER_SEEDS, mPlayerSeeds);
        sharedPrefs.commit();
        Log.d(TAG, "Saved data: current player seeds = " + mPlayerSeeds);
    }
    
    /**
     * Loads the player's game state. 
     */
    private void loadData() {
        SharedPreferences sharedPrefs = getPreferences(MODE_PRIVATE);
        mPlayerSeeds = sharedPrefs.getLong(KEY_PLAYER_SEEDS, PLAYER_STARTING_SEEDS);
        Log.d(TAG, "Loaded data: current player seeds = " + mPlayerSeeds);
    }
}