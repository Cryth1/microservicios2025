package com.microservice.monitoring;

import org.springframework.boot.SpringApplication;

public class TestMonitoringApplication {

    public static void main(String[] args) {
        SpringApplication.from(MonitoringApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
