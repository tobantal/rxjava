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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.tobolkin.rxjava.util.Sleeper;

import org.junit.jupiter.api.Test;

import rx.Completable;
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
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    // ignore
                }
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
        final List<Integer> result = new ArrayList<>();
        final Observable<Integer> obs1 = Observable.create(s -> {
            for (int i = 0; i < 5; i++) {
                s.onNext(i);
            }
            s.onCompleted();
        });

        final Observable<Integer> obs2 = Observable.create(s -> {
            for (int i = 5; i < 10; i++) {
                s.onNext(i);
            }
            s.onCompleted();
        });

        final Observable<Integer> obs3 = Observable.merge(obs1, obs2);
        obs3.subscribe(s -> result.add(s));

        assertThat(result).hasSize(10);
        for (int i = 0; i < 10; i++) {
            assertThat(result).contains(i, atIndex(i));
        }
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
    @Test
    public void shouldFallbackAfterError() throws Exception {
        Supplier<String> nextData = () -> "foo";
        Supplier<String> error = () -> {throw new RuntimeException();};

        final List<String> result = new ArrayList<>();
        String args = "foo";
        Observable<String> someData = Observable.create(s -> {
            getDataFromServerWithCallback(args, data -> {
                s.onNext(nextData.get());
                try {
                    s.onNext(error.get());
                } catch(RuntimeException re) {
                    s.onError(new rx.exceptions.OnErrorNotImplementedException(re));
                }
                // does not work, why?
            });
        });

        someData.subscribe(result::add);

        Observable<String> lazyFallback = Observable.just("Fallback");
        someData.onErrorResumeNext(lazyFallback).subscribe(result::add);

        assertThat(result).containsExactly("foo", "Fallback");
    }
    */

    private void getDataFromServerWithCallback(String args, Consumer<String> consumer) {
        consumer.accept("Random: " + Math.random());
    }

    @Test
    public void shouldGetDataFromLocalMemorySynchronously() throws Exception {
        // Iterable<String> as Stream<String>
        // that contains 75 strings
        getDataFromLocalMemorySynchronously().skip(10).limit(5).map(s -> s + "_transformed")
                .forEach(System.out::println);
    }

    private Stream<String> getDataFromLocalMemorySynchronously() {
        return IntStream.range(0, 100).mapToObj(Integer::toString);
    }

    @Test
    public void shouldGetDataFromNetworkAsynchronously() throws Exception {
        final List<String> result = new ArrayList<>(3);
        final Observable<String> obs = getDataFromNetworkAsynchronously().skip(10).take(3).map(s -> s + "_transformed");

        obs.subscribe(result::add);

        assertThat(result).containsExactlyInAnyOrder("10_transformed", "11_transformed", "12_transformed");
    }

    private Observable<String> getDataFromNetworkAsynchronously() {
        return Observable.range(0, 100).map(Object::toString);
    }

    @Test
    public void shouldGetDataAsFuture() throws Exception {
        Function<Integer, CompletableFuture<Integer>> completableFutureFactory = i -> CompletableFuture.completedFuture(i*i);

        CompletableFuture<Integer> f1 = completableFutureFactory.apply(2);
        CompletableFuture<Integer> f2 = completableFutureFactory.apply(3);

        CompletableFuture<Integer> f3 = f1.thenCombine(f2, Integer::sum);

        assertThat(f3.get()).isEqualTo(13);
    }

    @Test
    public void sample_240() throws Exception {
        Observable<String> o1 = getDataAsObservable(1);
        Observable<String> o2 = getDataAsObservable(2);

        Observable<String> o3 = Observable.zip(o1, o2, (x, y) -> {
            return x + y;
        });
    }

    private Observable<String> getDataAsObservable(int i) {
        return Observable.just("Done: " + i + "\n");
    }

    @Test
    public void sample_254() throws Exception {
        Observable<String> o1 = getDataAsObservable(1);
        Observable<String> o2 = getDataAsObservable(2);

        // o3 is now a stream of o1 and o2 that emits each item without waiting
        Observable<String> o3 = Observable.merge(o1, o2);
    }

    @Test
    public void sample_265() throws Exception {
        // merge a & b into an Observable stream of 2 values
        Observable<String> a_merge_b = getDataA().mergeWith(getDataB());
    }

    public static Single<String> getDataA() {
        return Single.<String>create(o -> {
            o.onSuccess("DataA");
        }).subscribeOn(Schedulers.io());
    }

    @Test
    public void sample_277() throws Exception {
        // Observable<String> o1 = getDataAsObservable(1);
        // Observable<String> o2 = getDataAsObservable(2);

        Single<String> s1 = getDataAsSingle(1);
        Single<String> s2 = getDataAsSingle(2);

        // o3 is now a stream of s1 and s2 that emits each item without waiting
        Observable<String> o3 = Single.merge(s1, s2);
    }

    private Single<String> getDataAsSingle(int i) {
        return Single.just("Done: " + i);
    }

    public static Single<String> getDataB() {
        return Single.just("DataB").subscribeOn(Schedulers.io());
    }

    static Completable writeToDatabase(Object data) {
        return Completable.create(s -> {
            doAsyncWrite(data,
                    // callback for successful completion
                    () -> s.onCompleted(),
                    // callback for failure with Throwable
                    error -> s.onError(error));
        });
    }

    static void doAsyncWrite(Object data, Runnable onSuccess, Consumer<Exception> onError) {
        // store data an run asynchronously:
        onSuccess.run();
    }

}