package fr.pjthin.firstdocker;

import org.springframework.stereotype.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;

@Component
public class ListenerVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Utils.log("start listening...");
        MessageConsumer<Object> consumer = vertx.eventBus().consumer(Context.CHANNEL_EVENTBUS + Context.getID());
        consumer.handler(this::listen);
        consumer.completionHandler(startFuture.completer());
    }

    public void listen(Message<Object> message) {
        Utils.log("Receive from [" + message.replyAddress() + "] : " + message.body());
    }

}
