package za.co.taloms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@Slf4j
public class TalomsApplication {

    public static void main(String[] args) throws UnknownHostException {
        var app = SpringApplication.run(TalomsApplication.class, args);
        var env = app.getEnvironment();
        log.info("============================================");
        log.info("TALOMS STARTED - VERSION: 35ecdc8");
        log.info("Profile: {}", env.getProperty("spring.profiles.active"));
        log.info("URL: http://{}:{}", InetAddress.getLocalHost().getHostAddress(), env.getProperty("server.port"));
        log.info("============================================");
    }

}
