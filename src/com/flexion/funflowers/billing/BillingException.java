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
 * Exception thrown when something went wrong with in-app billing.
 * A BillingException has an associated BillingResult (an error).
 * To get the billing result that caused this exception to be thrown,
 * call {@link #getResult()}.
 * 
 * @author Bruno Oliveira (Google), modified by Jonathan Coe (Flexion)
 */
public class BillingException extends Exception {
	private static final long serialVersionUID = -3966342630051211353L;
	
	BillingResult mResult;

    public BillingException(BillingResult result) {
        this(result, null);
    }
    public BillingException(String message) {
        this(new BillingResult(false, message));
    }
    public BillingException(BillingResult result, Exception cause) {
        super(result.getMessage(), cause);
        mResult = result;
    }
    public BillingException(String message, Exception cause) {
        this(new BillingResult(false, message), cause);
    }

    /** Returns the billing result (error) that this exception signals. */
    public BillingResult getResult() { return mResult; }
}