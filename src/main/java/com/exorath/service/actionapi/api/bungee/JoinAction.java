/*
 * Copyright 2017 Exorath
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

package com.exorath.service.actionapi.api.bungee;

import com.exorath.service.actionapi.res.Action;
import com.google.gson.JsonObject;

/**
 * Created by toonsev on 3/13/2017.
 */
public class JoinAction extends Action {
    public JoinAction(String playerId, String address) {
        super(playerId, null, "JOIN", getMeta(address), false);
    }

    private static JsonObject getMeta(String address) {
        JsonObject meta = new JsonObject();
        meta.addProperty("address", address);
        return meta;
    }
}
