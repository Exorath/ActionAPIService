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

import com.exorath.service.actionapi.Service;
import com.exorath.service.actionapi.res.Action;
import com.exorath.service.actionapi.res.SubscribeRequest;
import com.exorath.service.actionapi.res.Success;
import io.reactivex.Observable;

/**
 * Created by toonsev on 11/12/2016.
 */
public class SimpleService implements Service{

    @Override
    public Observable<Action> subscribe(Observable<SubscribeRequest> inStream) {
        return null;
    }

    @Override
    public Success publishAction(Action action) {
        return null;
    }
}
