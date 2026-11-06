package co.datacenter.recaudo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecaudoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecaudoApplication.class, args);
    }
}
