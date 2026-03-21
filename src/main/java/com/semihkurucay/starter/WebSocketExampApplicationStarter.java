package com.semihkurucay.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {"com.semihkurucay"})
@SpringBootApplication
public class WebSocketExampApplicationStarter {

	public static void main(String[] args) {
		SpringApplication.run(WebSocketExampApplicationStarter.class, args);
	}

}
