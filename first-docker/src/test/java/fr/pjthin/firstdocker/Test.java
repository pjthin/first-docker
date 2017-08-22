package fr.pjthin.firstdocker;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class Test {

    Vertx vertx = Vertx.vertx();

    public static void main(String[] args) {
        new Test().testFuture();
    }

    void testFuture() {
        Future<Void> fut1 = doSomethingLong("1");
        Future<Void> fut2 = fut1.compose(then -> doSomethingLong("2"));
        // CompositeFuture.join(fut1, fut2).setHandler(allDone -> {
        // if (allDone.succeeded()) {
        // System.out.println("allDone " + allDone);
        // } else {
        // System.out.println(allDone.cause());
        // }
        // });
        CompositeFuture.join(doSomethingLong("3"), doSomethingLong("4")).setHandler(allDone -> {
            if (allDone.succeeded()) {
                System.out.println("allDone-2 " + allDone);
            } else {
                System.out.println(allDone.cause());
            }
        });
    }

    Future<Void> doSomethingLong(String id) {
        System.out.println("enter " + id);
        Future<Void> fut = Future.future();
        vertx.executeBlocking(future -> {
            System.out.println("sleep " + id);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("end sleep " + id);
            future.complete();
        }, fut.completer());
        System.out.println("exit " + id);
        return fut;
    }
}
