/*
 * Copyright 2016 Exorath
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.exorath.service.actionapi.impl;

import com.exorath.service.actionapi.res.Action;
import com.exorath.service.actionapi.res.SubscribeRequest;
import io.reactivex.Completable;
import io.reactivex.Observable;

/**
 * Created by toonsev on 11/14/2016.
 */
public interface Subscription {
    /**
     * This will be called whenever an action is send to this subscription
     * @param action
     */
    void onAction(Action action);

    /**
     * Returns a stream for when the client (this subscription) sends a subscriberequest
     * @return
     */
    Observable<SubscribeRequest> getSubscribeRequestStream();

    /**
     * When the client does no longer want to subscribe, it can complete this to close all connections (note that when onClose() is called this should be closed still).
     * @return
     */
    Completable getCompletable();

    /**
     * Called when the connection is terminated when no ping is received for example, this is useful for the API
     * @param reason
     */
    default void onClose(String reason){};
}
