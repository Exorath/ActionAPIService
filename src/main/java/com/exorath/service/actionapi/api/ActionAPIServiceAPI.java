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

package com.exorath.service.actionapi.api;

import com.exorath.service.actionapi.impl.Subscription;
import com.exorath.service.actionapi.res.Action;
import com.exorath.service.actionapi.res.Success;
import com.google.gson.Gson;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.HttpRequestWithBody;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Created by toonsev on 2/26/2017.
 */
public class ActionAPIServiceAPI {
    private static Gson GSON = new Gson();
    private String address;

    public ActionAPIServiceAPI(String address){
        this.address = address;
    }
    /**
     * This subscription will automatically terminate if the server is no longer reachable. Make sure to implement a reconnection mechanism.
     */
    public void subscribe(Subscription subscription){
        Client client = new Client(subscription, URI.create("ws://" + url("/subscribe")));
        client.connect();
    }

    private class Client extends WebSocketClient {
        private Subscription subscription;
        private Disposable subscribeRequestStream;
        private long nextPing;
        private long lastPong;
        public Client(Subscription subscription, URI uri){
            super(uri);
            this.subscription = subscription;
             subscription.getCompletable().subscribe(() -> {
                this.close();
                this.handleClose("The completable closed.");
            });
        }

        @Override
        public void onOpen(ServerHandshake serverHandshake) {
            System.out.println("ActionAPIService: A connection opened");
            Schedulers.io().schedulePeriodicallyDirect(() -> {
                long delay =  TimeUnit.SECONDS.toMillis(20);
                long pingTime = System.currentTimeMillis();
                this.nextPing = pingTime + delay;
                send(String.valueOf(nextPing));

                Schedulers.io().scheduleDirect(() -> {
                    if(lastPong < pingTime ){
                        this.close();
                        this.handleClose("No pong received.");
                    }
                }, delay, TimeUnit.MILLISECONDS);
            }, 0, 5, TimeUnit.MILLISECONDS);

            this.subscribeRequestStream = subscription.getSubscribeRequestStream()
                    .subscribe(sr -> send(GSON.toJson(sr)));
        }

        @Override
        public void onMessage(String s) {
            System.out.println("ActionAPIService: A connection received msg: " + s);
            if(s.equals("{}")){
                this.lastPong = System.currentTimeMillis();
            }else {
                Action action = GSON.fromJson(s, Action.class);
                subscription.onAction(action);
            }
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("ActionAPIService: A connection closed");
            this.handleClose(reason);
        }

        private void handleClose(String reason){
            subscribeRequestStream.dispose();
            subscription.onClose(reason);
            System.out.println("ActionAPIService: A connection close was handled.");

        }
        @Override
        public void onError(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Publishes an action to specified destination
     * @param action the action to publish
     * @return whether or not the action was successfully published, this does not mean delivered nor executed!
     */
    public Success publishAction(Action action){
        try {
            HttpRequestWithBody req = Unirest.post("http://" + url("/action"));
            req.body(GSON.toJson(action));
            return GSON.fromJson(req.asString().getBody(), Success.class);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private String url(String endpoint) {
        return address + endpoint;
    }
}
