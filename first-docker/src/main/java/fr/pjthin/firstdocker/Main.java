package fr.pjthin.firstdocker;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.shareddata.Counter;
import io.vertx.core.shareddata.Lock;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

@SpringBootApplication
@ComponentScan("fr.pjthin.firstdocker")
@Component
public class Main extends AbstractVerticle {

    @Autowired
    PublisherVerticle publisher;

    @Autowired
    ListenerVerticle listener;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        Utils.log("starting main...");
        Future<Lock> fLock = getLock();
        fLock
                .compose(lock -> getCounter())
                .compose(counter -> getNextId(counter))
                .compose(id -> setId(id))
                .compose(nothing -> releaseLock(fLock))
                .compose(nothing -> {
                    // deploy other verticle
                    vertx.deployVerticle(listener);
                    vertx.deployVerticle(publisher);
                }, startFuture)
                .otherwise(cause -> {
                    releaseLock(fLock);
                    startFuture.fail(cause);
                    return null;
                });
    }

    private Future<Void> releaseLock(Future<Lock> fLock) {
        Utils.log("releaseLock(" + fLock + ")");
        return fLock.compose(lock -> {
            lock.release();
            return Future.succeededFuture();
        });
    }

    private Future<Void> setId(Long id) {
        Utils.log("setId(" + id + ")");
        Context.setID(id);
        return Future.succeededFuture();
    }

    private Future<Long> getNextId(Counter counter) {
        Utils.log("getNextId");
        Future<Long> future = Future.future();
        counter.addAndGet(1, future.completer());
        return future;
    }

    private Future<Lock> getLock() {
        Utils.log("getLock");
        Future<Lock> future = Future.future();
        vertx.sharedData().getLock("pjthin.lockId", future.completer());
        return future;
    }

    private Future<Counter> getCounter() {
        Utils.log("getCounter");
        Future<Counter> future = Future.future();
        vertx.sharedData().getCounter("pjthin.id", future.completer());
        return future;
    }

    public static void main(String[] args) throws UnknownHostException {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);

        Main main = context.getBean(Main.class);

        HazelcastClusterManager hazelcastClusterManager = new HazelcastClusterManager();

        String ip = InetAddress.getLocalHost().getHostAddress();

        Utils.log("ip: " + ip);

        VertxOptions options = new VertxOptions()
                .setClustered(true)
                .setClusterHost(ip)
                .setClusterPort(0)
                .setClusterManager(hazelcastClusterManager);

        Vertx.clusteredVertx(options, hVertx -> {
            if (hVertx.succeeded()) {
                Vertx vertx = hVertx.result();
                vertx.deployVerticle(main);
            } else {
                throw new RuntimeException("fail vertx : " + hVertx.cause().getMessage(), hVertx.cause());
            }
        });
    }

}
