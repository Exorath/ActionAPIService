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

package com.exorath.service.actionapi;

import com.exorath.service.actionapi.impl.Subscription;
import com.exorath.service.actionapi.res.Action;
import com.exorath.service.actionapi.res.SubscribeRequest;
import com.exorath.service.actionapi.res.Success;
import io.reactivex.Observable;

/**
 * See https://github.com/Exorath/LobbyMsgService for documentation
 * Created by toonsev on 11/1/2016.
 */
public interface Service {

    /**
     * Subscribe to actions
     */
    void subscribe(Subscription subscription);

    /**
     * Publishes an action to specified destination
     * @param action the action to publish
     * @return whether or not the action was successfully published, this does not mean delivered nor executed!
     */
    Success publishAction(Action action);

}
