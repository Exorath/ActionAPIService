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

import com.exorath.service.commons.portProvider.PortProvider;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.IOException;

import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.webSocket;

/**
 * Created by toonsev on 11/12/2016.
 */
public class Transport {
    private static Service service;
    public static void setup(Service service, PortProvider portProvider){
        Transport.service = service;
        port(portProvider.getPort());
        webSocket("/subscribe", SubscribeWebSocket.class);
        post("/action", getPublishActionRoute(service));

    }

    public static Route getPublishActionRoute(Service service){
        return (req, res) -> "works";
    }

    @WebSocket
    public static class SubscribeWebSocket {
        @OnWebSocketConnect
        public void onConnect(Session user) throws Exception {
            System.out.println("connected");
        }
        @OnWebSocketClose
        public void closed(Session session, int statusCode, String reason) {
            System.out.println("closed");
        }

        @OnWebSocketMessage
        public void message(Session session, String message) throws IOException {
            System.out.println("Got: " + message);   // Print message
            session.getRemote().sendString(message); // and send it back
        }
    }
}
