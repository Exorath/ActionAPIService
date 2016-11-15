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
import com.exorath.service.commons.jedisProvider.JedisProvider;
import com.google.gson.Gson;
import io.reactivex.Observable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by toonsev on 11/12/2016.
 */
public class RedisService implements Service {
    private static final Set<String> bungeeActions = new HashSet() {
        {
            this.add("join");
            this.add("chat");
            this.add("kick");
        }
    };
    private static final Gson GSON = new Gson();

    private JedisPool jedisPool;

    private HashMap<Subscription, Jedis> jedisSubscriptionBySubscription = new HashMap<>();
    private HashMap<Subscription, JedisPubSub> pubSubBySubscription = new HashMap<>();

    public RedisService(JedisProvider provider) {
        this.jedisPool = provider.getPool();
    }

    @Override
    public void subscribe(Subscription subscription) {
        subscription.getSubscribeRequestStream().subscribe(subscribeRequest -> handleSubscription(subscription, subscribeRequest));
    }

    private void handleSubscription(Subscription subscription, SubscribeRequest subscribeRequest) {
        String spigotId = subscribeRequest.getSpigotId();
        String bungeeId = subscribeRequest.getBungeeId();
        if (bungeeId == null && spigotId == null) {
            System.err.println("A subscribe requyest was made without a bungeeId/spigotId");
            return;
        }
        if (bungeeId != null && spigotId != null) {
            System.err.println("A subscribe requyest was made with both a bungeeId and a spigotId");
            return;
        }
        boolean isSpigot = spigotId != null;
        String serverId = isSpigot ? spigotId : bungeeId;
        String[] players = subscribeRequest.getPlayers();
        handleSubscription(subscription, isSpigot, serverId, players);
    }

    private synchronized void handleSubscription(Subscription subscription, boolean isSpigot, String serverId, String[] players) {
        Set<String> channels = new HashSet<>();
        channels.add(getAllChannel(isSpigot));
        channels.add(getServerChannel(isSpigot, serverId));
        channels.addAll(Arrays.asList(getPlayersChannels(isSpigot, players)));

        JedisPubSub jedisPubSub = pubSubBySubscription.get(subscription);
        if (jedisPubSub == null) {
            try {
                Jedis jedis = jedisPool.getResource();
                closeJedisWhenComplete(subscription, jedis);
                pubSubBySubscription.put(subscription, subscribe(jedis, channels.toArray(new String[channels.size()])));
            } catch (JedisException e) {
                e.printStackTrace();
            }
        } else
            jedisPubSub.subscribe(channels.toArray(new String[channels.size()]));
    }

    private void closeJedisWhenComplete(Subscription subscription, Jedis jedis) {
        subscription.getCompletable().subscribe(() -> jedis.close());
    }

    private JedisPubSub subscribe(Jedis jedis, String[] channels) {
        JedisPubSub jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {//Message=> the action
                super.onMessage(channel, message);
            }
        };
        new Thread(() -> jedis.subscribe(jedisPubSub, channels)).start();
        return jedisPubSub;
    }

    @Override
    public Success publishAction(Action action) {
        if (action.getAction)
            String[] channels = getChannels(action);
        try (Jedis jedis = jedisPool.getResource()) {
            for (String channel : channels)
                jedis.publish(channel, GSON.toJson(action));
        } catch (Exception e) {
            return new Success(false, "Exception trying to publish action to redis");
        }
        return new Success(true);
    }

    private String[] getChannels(Action action) {
        String actionJson = GSON.toJson(action);

        String destination = action.getDestination();
        boolean spigot = bungeeActions.contains(action.getAction());
            if (destination == null || destination.equals("SUBJECT")) {
                String subject = action.getSubject();
                if(subject.equals("ALL"))
            } else if (destination.equals("ALL")) {
                return new String[] {getAllChannel(spigot)};
            } else {//This should be a list of uniqueIds
                String[] serverIds = destination.split(",");
                String[] channels = new String[serverIds.length];
                for(int i = 0; i < serverIds.length; i++)
                    channels[i] = getServerChannel(spigot, serverIds[i]);
                return channels;
            }
        }
    }

    private String getAllChannel(boolean spigot) {
        return "servers." + (spigot ? "spigot" : "bungee") + ".ALL";
    }

    private String getServerChannel(boolean spigot, String serverId) {
        return "servers." + (spigot ? "spigot" : "bungee") + "." + serverId;
    }

    private String[] getPlayersChannels(boolean spigot, String[] players) {
        String[] channels = new String[players.length];
        String prefix = "players." + (spigot ? "spigot" : "bungee") + ".";
        for (int i = 0; i < players.length; i++)
            results[i] = prefix + players[i];
        return channels;
    }
}
