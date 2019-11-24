package com.tobolkin.rxjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import rx.Observable;

@SpringBootApplication
public class RxjavaApplication {

	public static void main(String[] args) {
		SpringApplication.run(RxjavaApplication.class, args);
		//System.out.println(">>>>>>>>>>>>>> Hello World!");

		 Observable.create(s -> {
        	   	 s.onNext("Hello World!");
			 s.onCompleted();
       		 }).subscribe(hello -> System.out.println(hello));
	}

}
