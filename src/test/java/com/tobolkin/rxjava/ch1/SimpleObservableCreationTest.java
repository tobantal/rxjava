package com.tobolkin.rxjava.ch1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
}