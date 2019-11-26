package com.tobolkin.rxjava.ch1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.tobolkin.rxjava.util.Sleeper;

import org.junit.jupiter.api.Test;

import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

public class SimpleObservableCreationTest {

    @Test
    void shouldCreateObservableFromCreateMethod() {
        final List<String> result = new ArrayList<>();
        final Observable<String> obs = Observable.create(s -> {
            s.onNext("Hello World!");
            s.onCompleted();
        });

        obs.subscribe(s -> result.add(s));
        assertThat(result).hasSize(1);
        assertThat(result).contains("Hello World!", atIndex(0));
    }

    @Test
    public void shoulGetRemoteValue() throws Exception {
        final Map<String, String> cache = new ConcurrentHashMap<>();
        final String someKey = "foo";
        cache.put(someKey, "123");

        Observable.create(s -> {
            s.onNext(cache.get(someKey));
            s.onCompleted();
        }).subscribe(value -> assertThat(value).isEqualTo("123"));
    }

    @Test
    void shouldGetValueWithSleep() {
        final List<Integer> result = new ArrayList<>();
        final Observable<Integer> obs = Observable.create(s -> {
            for (int i = 0; i < 10; i++) {
                Sleeper.sleep(Duration.ofMillis(50L));
                s.onNext(i);
            }
            s.onCompleted();
        });
        obs.subscribe(s -> result.add(s));

        assertThat(result).hasSize(10);
        assertThat(result).contains(4, atIndex(4));
    }

    @Test
    void shouldMergeValuesInSync() {
        final List<String> result = new ArrayList<>();
        final Observable<String> obs1 = Observable.create(s -> {
            s.onNext("a");
            s.onNext("b");
            s.onCompleted();
        });

        final Observable<String> obs2 = Observable.create(s -> {
            s.onNext("c");
            s.onNext("d");
            s.onCompleted();
        });

        final Observable<String> obs3 = Observable.merge(obs1, obs2);
        obs3.subscribe(result::add);

        assertThat(result).containsExactly("a", "b", "c", "d");
    }

    @Test
    public void shouldGetDataAsynchronously() throws Exception {
        final String someKey = "foo";
        // pseudo-code
        Observable.create(s -> {
            String fromCache = getFromCache(someKey);
            if (fromCache != null) {
                // emit synchronously
                s.onNext(fromCache);
                s.onCompleted();
            } else {
                // fetch asynchronously
                getDataAsynchronously(someKey).onResponse(v -> {
                    putInCache(someKey, v);
                    s.onNext(v);
                    s.onCompleted();
                }).onFailure(exception -> {
                    s.onError(exception);
                });
            }
        }).subscribe(s -> System.out.println(s));

        Sleeper.sleep(Duration.ofSeconds(2));
    }

    private void putInCache(String key, String value) {
        // do nothing
    }

    private Callback getDataAsynchronously(String key) {
        final Callback callback = new Callback();
        new Thread(() -> {
            Sleeper.sleep(Duration.ofSeconds(1));
            callback.getOnResponse().accept(key + ":123");
        }).start();
        return callback;
    }

    private String getFromCache(String key) {
        // return null;
        return key + ":123";
    }

    @Test
    public void shouldMap() throws Exception {
        final List<String> result = new ArrayList<>();
        Observable<Integer> o = Observable.create(s -> {
            s.onNext(1);
            s.onNext(2);
            s.onNext(3);
            s.onCompleted();
        });

        o.map(i -> String.format("Number %d", +i)).subscribe(s -> result.add(s));

        assertThat(result).containsExactlyInAnyOrder("Number 1", "Number 2", "Number 3");
    }

    // unchecked tests
    @Test
    public void shouldAyncSubscriptionAndDataEmission() throws Exception {
        Observable.<Integer>create(s -> {
            // ... async subscription and data emission ...
            new Thread(() -> s.onNext(42), "MyThread").start();
        }).doOnNext(i -> System.out.println(Thread.currentThread())).filter(i -> i % 2 == 0)
                .map(i -> "Value " + i + " processed on " + Thread.currentThread())
                .subscribe(s -> System.out.println("SOME VALUE =>" + s));
        System.out.println("Will print BEFORE values are emitted because Observable is async");
        Sleeper.sleep(Duration.ofSeconds(1));
    }

    @Test
    public void shouldNotRunUntilSubscribe() throws Exception {
        final List<Thread> threads = new ArrayList<>();
        final Observable<String> obs = Observable.create(s -> {
            Thread thread = new Thread(() -> {
                s.onNext("one");
                s.onNext("two");
                s.onNext("three");
                s.onNext("four");
                s.onCompleted();
            });
            thread.start();
            threads.add(thread);
        });

        assertThat(threads).isEmpty();

        obs.subscribe();

        assertThat(threads.get(0).getState()).isEqualTo(Thread.State.RUNNABLE);
    }

    @Test
    public void shouldSubscribeMultipleSubscribers() throws Exception {
        String args = "foo";
        Observable<String> someData = Observable.create(s -> {
            getDataFromServerWithCallback(args, data -> {
                s.onNext(data);
                s.onCompleted();
            });
        });

        someData.subscribe(s -> System.out.println("Subscriber 1: " + s));
        someData.subscribe(s -> System.out.println("Subscriber 2: " + s));

        Observable<String> lazyFallback = Observable.just("Fallback");
        someData.onErrorResumeNext(lazyFallback).subscribe(s -> System.out.println(s));

    }

    /*
     * @Test public void shouldFallbackAfterError() throws Exception {
     * Supplier<String> nextData = () -> "foo"; Supplier<String> error = () ->
     * {throw new RuntimeException();};
     *
     * final List<String> result = new ArrayList<>(); String args = "foo";
     * Observable<String> someData = Observable.create(s -> {
     * getDataFromServerWithCallback(args, data -> { s.onNext(nextData.get()); try {
     * s.onNext(error.get()); } catch(RuntimeException re) { s.onError(new
     * rx.exceptions.OnErrorNotImplementedException(re)); } // does not work, why?
     * }); });
     *
     * someData.subscribe(result::add);
     *
     * Observable<String> lazyFallback = Observable.just("Fallback");
     * someData.onErrorResumeNext(lazyFallback).subscribe(result::add);
     *
     * assertThat(result).containsExactly("foo", "Fallback"); }
     */

    private void getDataFromServerWithCallback(String args, Consumer<String> consumer) {
        consumer.accept("Random: " + Math.random());
    }

    @Test
    public void shouldGetDataFromLocalMemorySynchronously() throws Exception {
        final List<String> result = new ArrayList<>(3);
        final Supplier<Stream<String>> dataFromLocalMemorySynchronouslySupplier = () -> IntStream.range(0, 100)
                .mapToObj(Integer::toString);

        dataFromLocalMemorySynchronouslySupplier.get().skip(10).limit(3).map(s -> s + "_transformed")
                .forEach(result::add);

        assertThat(result).containsExactly("10_transformed", "11_transformed", "12_transformed");
    }

    @Test
    public void shouldGetDataFromNetworkAsynchronously() throws Exception {
        final List<String> result = new ArrayList<>(3);
        final Supplier<Observable<String>> dataFromNetworkAsynchronouslySupplier = () -> Observable.range(0, 100)
                .map(Object::toString);
        final Observable<String> obs = dataFromNetworkAsynchronouslySupplier.get().skip(10).take(3)
                .map(s -> s + "_transformed");

        obs.subscribe(result::add);

        assertThat(result).containsExactly("10_transformed", "11_transformed", "12_transformed");
    }

    @Test
    public void shouldGetDataAsFuture() throws Exception {
        Function<Integer, CompletableFuture<Integer>> completableFutureFactory = i -> CompletableFuture
                .completedFuture(i * i);

        CompletableFuture<Integer> f1 = completableFutureFactory.apply(2);
        CompletableFuture<Integer> f2 = completableFutureFactory.apply(3);

        CompletableFuture<Integer> f3 = f1.thenCombine(f2, Integer::sum);

        assertThat(f3.get()).isEqualTo(13);
    }

    @Test
    public void shouldZip() throws Exception {
        List<Integer> results = new ArrayList<>(1);
        Function<Integer, Observable<Integer>> observableFactory = Observable::just;
        Observable<Integer> o1 = observableFactory.apply(2);
        Observable<Integer> o2 = observableFactory.apply(3);

        Observable<Integer> o3 = Observable.zip(o1, o2, (x, y) -> x + y);

        o3.subscribe(results::add);

        assertThat(results.get(0)).isEqualTo(5);
    }

    @Test
    public void shouldJustMerge() throws Exception {
        List<Integer> results = new ArrayList<>(2);
        Function<Integer, Observable<Integer>> observableFactory = Observable::just;
        Observable<Integer> o1 = observableFactory.apply(2);
        Observable<Integer> o2 = observableFactory.apply(3);

        // o3 is now a stream of o1 and o2 that emits each item without waiting
        Observable<Integer> o3 = Observable.merge(o1, o2);

        o3.subscribe(results::add);

        assertThat(results).containsExactly(2, 3);
    }

    @Test
    public void shouldAsyncMergeSingle() throws Exception {
        List<String> results = new ArrayList<>(2);
        // merge a & b into an Observable stream of 2 values
        Observable<String> obs = getDataA().mergeWith(getDataB());

        obs.subscribe(results::add);
        Sleeper.sleep(Duration.ofSeconds(1));

        assertThat(results).containsExactly("DataB", "DataA");
    }

    private static Single<String> getDataA() {
        return Single.<String>create(o -> {
            Sleeper.sleep(Duration.ofMillis(100));
            o.onSuccess("DataA");
        }).subscribeOn(Schedulers.io());
    }

    private static Single<String> getDataB() {
        return Single.just("DataB").subscribeOn(Schedulers.io());
    }

    @Test
    public void shouldSyncMergeSingle() throws Exception {
        final List<Integer> results = new ArrayList<>(2);
        final Single<Integer> s1 = Single.just(1);
        final Single<Integer> s2 = Single.just(2);

        // o3 is now a stream of s1 and s2 that emits each item without waiting
        final Observable<Integer> o3 = Single.merge(s1, s2);

        o3.subscribe(results::add);

        assertThat(results).containsExactly(1, 2);
    }

    /*
     * static Completable writeToDatabase(Object data) { return Completable.create(s
     * -> { doAsyncWrite(data, // callback for successful completion () ->
     * s.onCompleted(), // callback for failure with Throwable error ->
     * s.onError(error)); }); }
     *
     * static void doAsyncWrite(Object data, Runnable onSuccess, Consumer<Exception>
     * onError) { // store data an run asynchronously: onSuccess.run(); }
     */

}