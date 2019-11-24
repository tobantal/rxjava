package com.tobolkin.rxjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import rx.Observable;

@SpringBootApplication
public class RxjavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RxjavaApplication.class, args);
        // System.out.println(">>>>>>>>>>>>>> Hello World!");

        Observable<String> obs1 = Observable.create(s -> {
            s.onNext("Hello World!");
            s.onNext("This is your world!");
            s.onCompleted();
        });

        Observable<String> obs2 = Observable.create(s -> {
            s.onNext("Hello World from SecondObservable!");
            s.onCompleted();
        });

        Observable<String> obs3 = Observable.merge(obs1, obs2);

        obs3.subscribe(System.out::println);

        System.exit(0);
    }

}
