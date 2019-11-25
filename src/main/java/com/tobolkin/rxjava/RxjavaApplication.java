package com.tobolkin.rxjava;

import java.util.UUID;

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
            for(int i = 0; i < 10; i++) {
                try {
					Thread.sleep(1_000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
                }
                s.onNext(Thread.currentThread().getName() + " >>> " + UUID.randomUUID().toString());
            }
            s.onCompleted();
        });

        Observable<String> obs2 = Observable.create(s -> {
            s.onNext("Hello World from SecondObservable!");
            for(int i = 0; i < 10; i++) {
                try {
					Thread.sleep(1_000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
                }
                s.onNext(Thread.currentThread().getName() + " >>> " + UUID.randomUUID().toString());
            }
            s.onCompleted();
        });

        Observable<String> obs3 = Observable.merge(obs1, obs2);

        obs3.subscribe(System.out::println);

        System.exit(0);
    }

}
