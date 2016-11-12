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

/**
 * Created by toonsev on 11/12/2016.
 */
public class SubscribeRequest {
    private String spigotId;
    private String bungeeId;
    private String[] players;

    public SubscribeRequest(String spigotId, String bungeeId, String[] players) {
        this.spigotId = spigotId;
        this.bungeeId = bungeeId;
        this.players = players;
    }

    public String getSpigotId() {
        return spigotId;
    }

    public String getBungeeId() {
        return bungeeId;
    }

    public String[] getPlayers() {
        return players;
    }
}
