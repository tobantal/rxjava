package com.tobolkin.rxjava.ch1;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.tobolkin.rxjava.util.Sleeper;

import rx.Observable;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;

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
}