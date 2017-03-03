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
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

import java.util.*;

/**
 * Created by toonsev on 11/12/2016.
 */
public class RedisService implements Service {
    private static final Logger LOG = LoggerFactory.getLogger(RedisService.class);
    private static final Gson GSON = new Gson();

    private JedisPool jedisPool;

    private HashMap<Subscription, Jedis> jedisSubscriptionBySubscription = new HashMap<>();
    private HashMap<Subscription, JedisPubSub> pubSubBySubscription = new HashMap<>();

    public RedisService(JedisProvider provider) {
        this.jedisPool = provider.getPool();
    }

    @Override
    public Success publishAction(Action action) {
        System.out.println("pubing: " + GSON.toJson(action));
        if (action.getAction() == null)
            return new Success(false, "Tried to publish action without required 'action' string parameter.");
        if (action.getMeta() == null)
            return new Success(false, "Tried to publish action without required 'meta' jsonobject parameter.");
        try {
            String[] channels = getChannels(action);
            action.setType(null);//Type not required on publish
            action.setDestination(null);//destionation not required on publish
            return publishToJedis(channels, action);
        } catch (Exception e) {
            return new Success(false, e.getMessage());
        }
    }

    @Override
    public void subscribe(Subscription subscription) {
        System.out.println("subing...");
        subscription.getSubscribeRequestStream().subscribe(subscribeRequest -> handleSubscription(subscription, subscribeRequest));
    }

    private void handleSubscription(Subscription subscription, SubscribeRequest subscribeRequest) {
        System.out.println("Received sub: " + GSON.toJson(subscribeRequest));
        String serverId = subscribeRequest.getServerId();
        String type = subscribeRequest.getType();
        if (serverId == null) {
            LOG.error("No serverId was provided in subscribeRequest");
            return;
        }
        if (type == null) {
            LOG.error("No 'type' was provided in subscribeRequest");
            return;
        }
        String[] players = subscribeRequest.getPlayers();
        handleSubscription(subscription, serverId, type, players);
    }

    private synchronized void handleSubscription(Subscription subscription, String serverId, String type, String[] players) {
        Set<String> channels = new HashSet<>();
        channels.add(getAllChannel(type));
        channels.add(getServerChannel(type, serverId));
        if (players != null)
            channels.addAll(Arrays.asList(getPlayersChannels(type, players)));
        System.out.println("channels: " + GSON.toJson(channels));
        handleJedisSubscription(subscription, channels);
    }

    private synchronized void handleJedisSubscription(Subscription subscription, Collection<String> channels) {
        JedisPubSub jedisPubSub = pubSubBySubscription.get(subscription);
        if (jedisPubSub == null) {
            try {
                Jedis jedis = jedisPool.getResource();
                closeJedisWhenComplete(subscription);
                pubSubBySubscription.put(subscription, subscribe(subscription, jedis, channels.toArray(new String[channels.size()])));
            } catch (JedisException e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        } else
            jedisPubSub.subscribe(channels.toArray(new String[channels.size()]));

    }

    private void closeJedisWhenComplete(Subscription subscription) {
        subscription.getCompletable().subscribe(() -> removeSubscription(subscription));
    }

    private synchronized void removeSubscription(Subscription subscription) {
        Jedis jedis = jedisSubscriptionBySubscription.remove(subscription);
        pubSubBySubscription.remove(subscription);
        if (jedis != null)
            jedis.close();
    }

    private JedisPubSub subscribe(Subscription subscription, Jedis jedis, String[] channels) {
        JedisPubSub jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {//Message=> the action
                subscription.onAction(GSON.fromJson(message, Action.class));
            }

        };
        new Thread(() -> jedis.subscribe(jedisPubSub, channels)).start();
        return jedisPubSub;
    }

    private String[] getChannels(Action action) {
        String actionType = action.getAction();
        JsonObject meta = action.getMeta();
        String subject = action.getSubject() == null ? "NONE" : action.getSubject();
        String destination = action.getDestination() == null ? "SUBJECT" : action.getDestination();
        String type = action.getType() == null ? "spigot" : action.getType();

        if (destination.equals("SUBJECT")) {
            if (subject.equals("ALL") || subject.equals("NONE"))
                destination = "ALL";
            else//subject is a list of players
                return getPlayersChannels(type, subject.split(","));
        }
        if (destination.equals("ALL"))//Message will be send to every server of type
            return new String[]{getAllChannel(type)};
        else//This should be a list of uniqueIds
            return getServersChannels(type, destination.split(","));
    }

    private Success publishToJedis(String[] channels, Action action) {
        String actionJson = GSON.toJson(action);
        try (Jedis jedis = jedisPool.getResource()) {
            for (String channel : channels)
                jedis.publish(channel, actionJson);
        } catch (Exception e) {
            return new Success(false, "Exception trying to publish action to redis.");
        }
        return new Success(true);
    }

    private String getAllChannel(String type) {
        return "servers." + type + ".ALL";
    }

    private String getServerChannel(String type, String serverId) {
        return "servers." + type + "." + serverId;
    }

    private String[] getPlayersChannels(String type, String[] players) {
        String[] channels = new String[players.length];
        String prefix = "players." + type + ".";
        for (int i = 0; i < players.length; i++)
            channels[i] = prefix + players[i];
        return channels;
    }

    private String[] getServersChannels(String type, String[] servers) {
        String[] channels = new String[servers.length];
        String prefix = "servers." + type + ".";
        for (int i = 0; i < servers.length; i++)
            channels[i] = prefix + servers[i];
        return channels;
    }
}
