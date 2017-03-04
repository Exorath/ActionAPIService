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
import com.exorath.service.commons.portProvider.PortProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.annotations.*;
import spark.Route;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static spark.Spark.*;

/**
 * Created by toonsev on 11/12/2016.
 */
public class Transport {
    private static final Gson GSON = new Gson();
    private static Service service;

    public static void setup(Service service, PortProvider portProvider) {
        Transport.service = service;
        port(portProvider.getPort());
        webSocket("/subscribe", SubscribeWebSocket.class);
        post("/action", getPublishActionRoute(service), GSON::toJson);
        init();

    }

    public static Route getPublishActionRoute(Service service) {
        return (req, res) -> {
            Action action = GSON.fromJson(req.body(), Action.class);
            return service.publishAction(action);
        };
    }

    @WebSocket
    public static class SubscribeWebSocket {
        private HashMap<Session, SubscriptionImpl> subscriptions = new HashMap<>();

        @OnWebSocketConnect
        public void onWebSocketConnect(Session session) {
            SubscriptionImpl subscription = new SubscriptionImpl(session);
            subscriptions.put(session, subscription);
            //whenever an action is received, propagate it to the websocket
            service.subscribe(subscription);
        }

        @OnWebSocketMessage
        public void onWebSocketText(Session session, String message) {
            System.out.println(session.getRemoteAddress().toString());
            //Try and parse the websocket
            if (subscriptions.get(session).handlePing(message))
                return;
            try {
                JsonObject messageObj = GSON.fromJson(message, JsonObject.class);
                if (messageObj.has("subscribe")) {
                    subscriptions.get(session).requestPublishSubject.onNext(GSON.fromJson(messageObj.get("subscribe"), SubscribeRequest.class));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @OnWebSocketClose
        public void onWebSocketClose(Session session, int statusCode, String reason) {
            subscriptions.get(session).requestPublishSubject.onComplete();//close all subscriptions
            if (session != null && session.isOpen())
                session.close();
            subscriptions.remove(session);
            System.out.println("session removed");
        }

        @OnWebSocketError
        public void onWebSocketError(Throwable cause) {
            //print a websocket error
            cause.printStackTrace(System.err);
        }
    }

    private static class SubscriptionImpl implements Subscription {
        private Long lastTimeout;
        private PublishSubject<SubscribeRequest> requestPublishSubject = PublishSubject.create();
        private Session session;

        public SubscriptionImpl(Session session) {
            this.session = session;
        }

        @Override
        public void onAction(Action action) {
            try {
                session.getRemote().sendString(GSON.toJson(action));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Observable<SubscribeRequest> getSubscribeRequestStream() {
            return requestPublishSubject;
        }

        @Override
        public Completable getCompletable() {
            return Completable.fromObservable(requestPublishSubject);
        }


        //Handles the ping to keep the connection open (should be pinged within 300 seconds)
        public boolean handlePing(String message) {
            try {
                setLastTimeout(Long.valueOf(message));
                if (!closeIfTimeout()) {
                    session.getRemote().sendString("{}");//send pong
                    Observable.create(sub -> {
                        sub.onNext(new Object());
                        sub.onComplete();
                    }).delay(getLastTimeout() - System.currentTimeMillis(), TimeUnit.MILLISECONDS).doOnNext(o ->
                            closeIfTimeout()).subscribe();
                }
                return true;
            } catch (NumberFormatException e) {
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return true;
            }
        }

        private synchronized void setLastTimeout(long timeout) {
            this.lastTimeout = timeout;
        }

        private synchronized long getLastTimeout() {
            return lastTimeout;
        }

        private synchronized boolean closeIfTimeout() {
            long timeout = getLastTimeout() - System.currentTimeMillis();
            System.out.println(timeout);
            System.out.println("closed?");
            if (timeout < 0) {
                System.out.println("closed");
                session.close();
                return true;
            }
            return false;
        }
    }
}
