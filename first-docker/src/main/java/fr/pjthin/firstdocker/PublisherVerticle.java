package fr.pjthin.firstdocker;

import org.springframework.stereotype.Component;

import io.vertx.core.AbstractVerticle;

@Component
public class PublisherVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        vertx.setPeriodic(1000, this::publish);
    }

    public void publish(Long time) {
        // publish at ID+1
        vertx.eventBus().publish(Context.CHANNEL_EVENTBUS + (Context.getID() + 1), "Hi! I'm " + Context.getID());
    }
}
