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

/**
 * Represents the result of an in-app billing operation.
 * 
 * @author Bruno Oliveira (Google), modified by Jonathan Coe (Flexion)
 */
public class BillingResult {
    boolean mSuccess;
    String mMessage;
    
    /**
     * Creates a new IabResult, representing the result of an 
     * in-app-billing operation
     * 
     * @param success - Whether or not the result is a successful one
     * @param message - A message describing the result
     */
    public BillingResult(boolean success, String message) {
    	mSuccess = success;
        mMessage = message;
    }
    
    public String getMessage() { return mMessage; }
    public boolean isSuccess() { return mSuccess; }
    public boolean isFailure() { return !mSuccess; }
    public String toString() { return "Result: " + String.valueOf(mSuccess) + ". Message: " + mMessage; }
}