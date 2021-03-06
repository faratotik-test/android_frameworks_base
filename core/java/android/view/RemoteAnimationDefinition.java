/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.view;

import static android.app.WindowConfiguration.ACTIVITY_TYPE_UNDEFINED;

import android.annotation.Nullable;
import android.app.WindowConfiguration;
import android.app.WindowConfiguration.ActivityType;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArraySet;
import android.util.SparseArray;
import android.view.WindowManager.TransitionType;

/**
 * Defines which animation types should be overridden by which remote animation.
 *
 * @hide
 */
public class RemoteAnimationDefinition implements Parcelable {

    private final SparseArray<RemoteAnimationAdapterEntry> mTransitionAnimationMap;

    public RemoteAnimationDefinition() {
        mTransitionAnimationMap = new SparseArray<>();
    }

    /**
     * Registers a remote animation for a specific transition.
     *
     * @param transition The transition type. Must be one of WindowManager.TRANSIT_* values.
     * @param activityTypeFilter The remote animation only runs if an activity with type of this
     *                           parameter is involved in the transition.
     * @param adapter The adapter that described how to run the remote animation.
     */
    public void addRemoteAnimation(@TransitionType int transition,
            @ActivityType int activityTypeFilter, RemoteAnimationAdapter adapter) {
        mTransitionAnimationMap.put(transition,
                new RemoteAnimationAdapterEntry(adapter, activityTypeFilter));
    }

    /**
     * Registers a remote animation for a specific transition without defining an activity type
     * filter.
     *
     * @param transition The transition type. Must be one of WindowManager.TRANSIT_* values.
     * @param adapter The adapter that described how to run the remote animation.
     */
    public void addRemoteAnimation(@TransitionType int transition, RemoteAnimationAdapter adapter) {
        addRemoteAnimation(transition, ACTIVITY_TYPE_UNDEFINED, adapter);
    }

    /**
     * Checks whether a remote animation for specific transition is defined.
     *
     * @param transition The transition type. Must be one of WindowManager.TRANSIT_* values.
     * @param activityTypes The set of activity types of activities that are involved in the
     *                      transition. Will be used for filtering.
     * @return Whether this definition has defined a remote animation for the specified transition.
     */
    public boolean hasTransition(@TransitionType int transition, ArraySet<Integer> activityTypes) {
        return getAdapter(transition, activityTypes) != null;
    }

    /**
     * Retrieves the remote animation for a specific transition.
     *
     * @param transition The transition type. Must be one of WindowManager.TRANSIT_* values.
     * @param activityTypes The set of activity types of activities that are involved in the
     *                      transition. Will be used for filtering.
     * @return The remote animation adapter for the specified transition.
     */
    public @Nullable RemoteAnimationAdapter getAdapter(@TransitionType int transition,
            ArraySet<Integer> activityTypes) {
        final RemoteAnimationAdapterEntry entry = mTransitionAnimationMap.get(transition);
        if (entry == null) {
            return null;
        }
        if (entry.activityTypeFilter == ACTIVITY_TYPE_UNDEFINED
                || activityTypes.contains(entry.activityTypeFilter)) {
            return entry.adapter;
        } else {
            return null;
        }
    }

    public RemoteAnimationDefinition(Parcel in) {
        final int size = in.readInt();
        mTransitionAnimationMap = new SparseArray<>(size);
        for (int i = 0; i < size; i++) {
            final int transition = in.readInt();
            final RemoteAnimationAdapterEntry entry = in.readTypedObject(
                    RemoteAnimationAdapterEntry.CREATOR);
            mTransitionAnimationMap.put(transition, entry);
        }
    }

    /**
     * To be called by system_server to keep track which pid is running the remote animations inside
     * this definition.
     */
    public void setCallingPid(int pid) {
        for (int i = mTransitionAnimationMap.size() - 1; i >= 0; i--) {
            mTransitionAnimationMap.valueAt(i).adapter.setCallingPid(pid);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        final int size = mTransitionAnimationMap.size();
        dest.writeInt(size);
        for (int i = 0; i < size; i++) {
            dest.writeInt(mTransitionAnimationMap.keyAt(i));
            dest.writeTypedObject(mTransitionAnimationMap.valueAt(i), flags);
        }
    }

    public static final Creator<RemoteAnimationDefinition> CREATOR =
            new Creator<RemoteAnimationDefinition>() {
        public RemoteAnimationDefinition createFromParcel(Parcel in) {
            return new RemoteAnimationDefinition(in);
        }

        public RemoteAnimationDefinition[] newArray(int size) {
            return new RemoteAnimationDefinition[size];
        }
    };

    private static class RemoteAnimationAdapterEntry implements Parcelable {

        final RemoteAnimationAdapter adapter;

        /**
         * Only run the transition if one of the activities matches the filter.
         * {@link WindowConfiguration.ACTIVITY_TYPE_UNDEFINED} means no filter
         */
        @ActivityType final int activityTypeFilter;

        RemoteAnimationAdapterEntry(RemoteAnimationAdapter adapter, int activityTypeFilter) {
            this.adapter = adapter;
            this.activityTypeFilter = activityTypeFilter;
        }

        private RemoteAnimationAdapterEntry(Parcel in) {
            adapter = in.readParcelable(RemoteAnimationAdapter.class.getClassLoader());
            activityTypeFilter = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(adapter, flags);
            dest.writeInt(activityTypeFilter);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        private static final Creator<RemoteAnimationAdapterEntry> CREATOR
                = new Creator<RemoteAnimationAdapterEntry>() {

            @Override
            public RemoteAnimationAdapterEntry createFromParcel(Parcel in) {
                return new RemoteAnimationAdapterEntry(in);
            }

            @Override
            public RemoteAnimationAdapterEntry[] newArray(int size) {
                return new RemoteAnimationAdapterEntry[size];
            }
        };
    }
}
