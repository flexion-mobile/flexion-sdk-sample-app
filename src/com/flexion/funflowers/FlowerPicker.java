package com.flexion.funflowers;

import java.util.Random;

/**
 * This class contains the logic for picking new flowers
 * that will be displayed to the user. 
 * 
 * @author Jonathan Coe
 */
public class FlowerPicker {
	
	/** The resource IDs for the top parts of the flowers */
	private static final int[] FLOWER_TOP_IDS = {R.drawable.flower_top_000, R.drawable.flower_top_001, 
		R.drawable.flower_top_002, R.drawable.flower_top_003, R.drawable.flower_top_004, 
		R.drawable.flower_top_005, R.drawable.flower_top_006, R.drawable.flower_top_007};
	
	/** The resource IDs for the bottom parts of the flowers */
	private static final int[] FLOWER_BOTTOM_IDS = {R.drawable.flower_bottom_000, R.drawable.flower_bottom_001, 
		R.drawable.flower_bottom_002, R.drawable.flower_bottom_003};
	
	/**
	 * Picks resource IDs to use for flower parts
	 *  
	 * @return An int[] containing the resource IDs to use. Currently the IDs supplied
	 * are for the top and bottom of the flower, in that order. 
	 */
	protected static int[] pickFlowerParts() {
		
		// Pick a top and a bottom for the flower
        Random random = new Random();
        int flowerTopIdPosition = random.nextInt(FLOWER_TOP_IDS.length - 1);
        int flowerBottomIdPosition = random.nextInt(FLOWER_BOTTOM_IDS.length - 1);
        
        int flowerTopId = FLOWER_TOP_IDS[flowerTopIdPosition];
        int flowerBottomId = FLOWER_BOTTOM_IDS[flowerBottomIdPosition];
        
        return new int[]{flowerTopId, flowerBottomId};
	}
}