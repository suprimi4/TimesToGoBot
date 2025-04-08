package suprimi4.timetogobot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TimeToGoBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimeToGoBotApplication.class, args);
    }

}
