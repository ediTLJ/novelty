/*
* Copyright 2015 Eduard Scarlat
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package ro.edi.util.ui;

/**
 * @author Eduard Scarlat
 */
public class LoaderPayload {
    public static final int STATUS_OK = 0;
    public static final int STATUS_ERROR = 1;

    private int mStatus;
    private Object mData = null;
    private String mError = null;
    private int mReason = -1; // keep this a negative value

    /**
     * Creates an object used for passing data through loaders.
     *
     * @param status       the result of the task
     * @param data         used for passing an object as a result of the task, can be null
     * @param errorMessage a custom error message in case of an error, can be null
     */
    public LoaderPayload(int status, Object data, String errorMessage) {
        mStatus = status;
        mData = data;
        mError = errorMessage;
    }

    public LoaderPayload(int status) {
        this(status, null, null);
    }

    /**
     * Creates an object used for passing data through loaders.
     *
     * @param status the result of the task
     * @param reason used for handle multiple objects with the same status
     * @param data   used for passing an object as a result of the task, can be null
     */
    public LoaderPayload(int status, int reason, Object data) {
        mStatus = status;
        mReason = reason;
        mData = data;
    }

    public LoaderPayload(int status, int reason) {
        this(status, reason, null);
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int status) {
        mStatus = status;
    }

    public Object getData() {
        return mData;
    }

    public String getDataAsString() {
        return mData.toString();
    }

    public void setData(Object data) {
        mData = data;
    }

    public String getErrorMessage() {
        return mError;
    }

    public void setErrorMessage(String errorMessage) {
        mError = errorMessage;
    }

    public int getReason() {
        return mReason;
    }

    public void setReason(int errorReason) {
        mReason = errorReason;
    }
}