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
package ro.edi.util.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Class for intercepting RecyclerView's touch events and translating them to onItemClick and onItemLongClick callbacks.
 */
public abstract class OnRecyclerClickItemListener implements RecyclerView.OnItemTouchListener {
    private GestureDetector mGestureDetector;

    public OnRecyclerClickItemListener(final RecyclerView recyclerView) {
        if (recyclerView == null) {
            throw new IllegalArgumentException("RecyclerView is null");
        }

        mGestureDetector = new GestureDetector(recyclerView.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onShowPress(MotionEvent e) {
                View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (view != null) {
                    view.setPressed(true);
                }
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (view == null) {
                    return false;
                }

                view.setPressed(false);
                onItemClick(recyclerView, view, recyclerView.getChildPosition(view));
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (view == null) {
                    return;
                }

                view.setPressed(false);
                onItemLongClick(recyclerView, view, recyclerView.getChildPosition(view));
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
        return mGestureDetector.onTouchEvent(motionEvent);
        // return false;
    }

    @Override
    public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {

    }

    public abstract void onItemClick(RecyclerView parent, View clickedView, int position);

    public abstract void onItemLongClick(RecyclerView parent, View clickedView, int position);
}
