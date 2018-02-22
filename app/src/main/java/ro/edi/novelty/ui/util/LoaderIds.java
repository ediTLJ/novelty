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

public interface LoaderIds {
    // Use ASYNC_ prefix for AsyncTaskLoader & CURSOR_ prefix for CursorLoader.
    // As LoaderManager uses these IDs to manage loaders' lifecycle, these values must be unique per activity.

    // FeedFragment
    int ASYNC_GET_FEED = 100; // reserved until 199
    int CURSOR_GET_FEED = 200; // reserved until 299
    int ASYNC_UPDATE_READ = 300;

    // StarredFragment
    int CURSOR_GET_STARRED = 310;

    // NewsInfoActivity
    int ASYNC_STAR = 320;
}