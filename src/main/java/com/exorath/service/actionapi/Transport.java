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
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.api.annotations.*;
import spark.Route;

import java.io.IOException;
import java.util.HashMap;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.webSocket;

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
        post("/action", getPublishActionRoute(service));

    }

    public static Route getPublishActionRoute(Service service) {
        return (req, res) -> {
            Action action = GSON.fromJson(req.body(), Action.class);
            return service.publishAction(action);
        };
    }


    public static class SubscribeWebSocket extends WebSocketAdapter implements Subscription {
        private PublishSubject<SubscribeRequest> requestPublishSubject = PublishSubject.create();

        @Override
        public void onAction(Action action) {
            try {
                getRemote().sendString(GSON.toJson(action));
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

        @Override
        public void onWebSocketConnect(Session sess) {
            super.onWebSocketConnect(sess);
            //whenever an action is received, propagate it to the websocket
            service.subscribe(this);
        }

        @Override
        public void onWebSocketText(String message) {
            super.onWebSocketText(message);
            //Try and parse the websocket
            try {
                requestPublishSubject.onNext(GSON.fromJson(message, SubscribeRequest.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason) {
            super.onWebSocketClose(statusCode, reason);
            //close all subscriptions
            requestPublishSubject.onComplete();
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            //print a websocket error
            super.onWebSocketError(cause);
            cause.printStackTrace(System.err);
        }
    }
}
