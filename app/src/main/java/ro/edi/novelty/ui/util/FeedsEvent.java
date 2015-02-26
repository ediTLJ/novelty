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
package ro.edi.novelty.ui.util;

public class FeedsEvent {
    public static final int TYPE_ADD = 0;
    public static final int TYPE_REMOVE = 1;
    public static final int TYPE_SWAP = 2;
    public static final int TYPE_UPDATE = 3;

    private int mType;

    public FeedsEvent(int type) {
        mType = type;
    }

    public int getType() {
        return mType;
    }
}
