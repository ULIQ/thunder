/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package network.thunder.core.communication.nio;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import network.thunder.core.communication.ClientObject;
import network.thunder.core.communication.layer.ContextFactory;
import network.thunder.core.communication.layer.PipelineInitialiser;
import network.thunder.core.etc.Tools;
import network.thunder.core.helper.callback.ConnectionListener;
import org.slf4j.Logger;

/**
 */
public final class P2PClient {
    private static final Logger log = Tools.getLogger();

    ContextFactory contextFactory;

    public P2PClient (ContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    //We will add a new handler for the different layers
    //Furthermore, we will add a new handler for the different message types,
    //as it will greatly improve readability and maintainability of the code.

    public void connectTo (ClientObject node, ConnectionListener connectionListener) {
        new Thread(new Runnable() {
            @Override
            public void run () {
                try {
                    connect(node, connectionListener);
                } catch (Exception e) {
                    log.warn("", e);
                }

            }
        }).start();

    }

    public void connectBlocking (ClientObject node, ConnectionListener connectionListener) {
        connect(node, connectionListener);
    }

    private void connect (ClientObject node, ConnectionListener connectionListener) {
        boolean successfulConnection = false;
        try {
            log.info("Connect to " + node.host + ":" + node.port + " - " + node.intent);
            Channel ch = createChannel(node);
            if (ch.isOpen()) {
                successfulConnection = true;
                connectionListener.onSuccess.execute();
            }
            ch.closeFuture().sync();

            log.info("Connection to " + node.host + " closed..");
        } catch (Exception e) {
            if (!successfulConnection) {
                //Don't notify a previously successful connection again..
                connectionListener.onFailure.execute();
            }
            throw new RuntimeException(e);
        }
    }

    private Channel createChannel (ClientObject node) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        b.group(group).channel(NioSocketChannel.class).handler(new PipelineInitialiser(contextFactory, node));
        return b.connect(node.host, node.port).sync().channel();
    }
}
