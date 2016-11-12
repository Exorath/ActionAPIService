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

package com.exorath.service.actionapi.res;

import com.google.gson.JsonObject;

/**
 * Created by toonsev on 11/12/2016.
 */
public class Action {
    private String subject;
    private String destination;
    private String action;
    private JsonObject meta;

    public Action(String subject, String destination, String action, JsonObject meta) {
        this.subject = subject;
        this.destination = destination;
        this.action = action;
        this.meta = meta;
    }

    public String getSubject() {
        return subject;
    }

    public String getDestination() {
        return destination;
    }

    public String getAction() {
        return action;
    }

    public JsonObject getMeta() {
        return meta;
    }
}
