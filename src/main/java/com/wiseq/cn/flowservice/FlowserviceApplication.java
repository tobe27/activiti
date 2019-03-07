package com.wiseq.cn.flowservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"com.wiseq.cn"})
@SpringBootApplication(exclude = {
    org.activiti.spring.boot.SecurityAutoConfiguration.class
})
public class FlowserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlowserviceApplication.class, args);
    }
}

