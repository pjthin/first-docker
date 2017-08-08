package fr.pjthin.firstdocker;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
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
        // TODO Auto-generated method stub
        super.start(startFuture);
        Utils.log("try get lock");
        Future<Lock> fLock = getLock();
        Future<Void> fSetId = fLock.compose(lock -> getCounter()).compose(counter -> getNextId(counter)).compose(id -> setId(id));

        Utils.log("end sleep");
        Utils.log("lock "+fLock.result());
//        CompositeFuture.join(fLock, fSetId).setHandler(allDone -> {
//            Utils.log("allDone");
//
//            // release lock when lock get and when id set
//            fLock.result().release();
//
//            // check my app id
//            Utils.log("My id is : '" + Context.getID() + "'");
//
//            // deploy other verticle
//            vertx.deployVerticle(listener);
//            vertx.deployVerticle(publisher);
//            
//            startFuture.complete();
//        });
        Utils.log("lock2 "+fLock.result());

        // play with file
        // checkFileAtStart();
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

    // void doWithLock(Lock lock) {
    // Utils.log("i've the lock");
    // vertx.sharedData().getCounter("pjthin.id", handleFailed(counter -> {
    // doWithCounter(counter).compose(v -> {
    // Utils.log("i've release the lock");
    // lock.release();
    // return Future.succeededFuture();
    // });
    // }));
    // }
    //
    // Future<Void> doWithCounter(Counter counter) {
    // Future<Void> future = Future.future();
    // counter.addAndGet(1, hId -> {
    // if (hId.succeeded()) {
    // long id = hId.result();
    // String eventPush = "pjthin." + (id - 1);
    // String eventRead = "pjthin." + id;
    // Utils.log("I'm [" + id + "]");
    // vertx.setTimer(10, h -> {
    // Utils.log("[" + id + "] start pushing on '" + eventPush + "' ...");
    // vertx.setPeriodic(1000, hPeriodic -> {
    // Utils.log("[" + id + "] i'm pushing...");
    // vertx.eventBus().publish(eventPush, "Hello I'm " + id);
    // });
    // });
    // Utils.log("[" + id + "] creating consumer on '" + eventRead + "'...");
    // MessageConsumer<Object> consumer = vertx.eventBus().consumer(eventRead);
    // consumer.handler(msg -> {
    // Utils.log("[" + id + "] receive : " + msg.body());
    // });
    // consumer.completionHandler(h -> {
    // if (h.succeeded()) {
    // Utils.log("[" + id + "] consumer ON");
    // } else {
    // Utils.log("[" + id + "] consumer OFF");
    // }
    // });
    // }
    // future.complete();
    // });
    // return future;
    // }
    //
    // private <T> Handler<AsyncResult<T>> handleFailed(Consumer<T> consumer) {
    // return handleFailed("Fail : ", consumer);
    // }
    //
    private <T> Handler<AsyncResult<T>> handleFailed(String error, Consumer<T> consumer) {
        return asyncResult -> {
            if (asyncResult.succeeded()) {
                consumer.accept(asyncResult.result());
            } else {
                Utils.log(error + asyncResult.cause().getMessage());
            }
        };
    }

    private void checkFileAtStart() {
        vertx.fileSystem().readDir("/save", handleFailed("failed read dir : ", files -> {
            String fileName = "data.txt";
            if (files.isEmpty()) {
                Buffer data = Buffer.buffer("1");
                // create file with 1
                vertx.fileSystem().writeFile("/save/" + fileName, data, handleFailed("Create first time file... KO: ", write -> {
                    Utils.log("Create first time file... OK");
                }));
            } else {
                vertx.fileSystem().readFile("/save/" + fileName, handleFailed("Read file... KO: ", buffer -> {
                    System.out.println("Read file... OK");
                    String string = new String(buffer.getBytes());
                    System.out.println("Read : '" + string + "'");
                    vertx.fileSystem().writeFile("/save/" + fileName, Buffer.buffer(string + "+1"), handleFailed("Create file... KO: ", write -> {
                        System.out.println("Create file... OK");
                    }));
                }));
            }
        }));
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
