package com.tobolkin.rxjava.ch1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Index.atIndex;

public class SimpleObservableCreationTest {

    @Test
	void shouldCreateObservableFromCreateMethod() {
        final List<String> result = new ArrayList<>();
        Observable<String> obs = Observable.create(s -> {
            s.onNext("Hello World!");
            s.onCompleted();
        });

        obs.subscribe(s -> result.add(s));
        assertThat(result).hasSize(1);
        assertThat(result).contains("Hello World!", atIndex(0));
    }

}