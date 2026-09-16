package za.co.taloms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class TalomsApplication {

    public static void main(String[] args) throws UnknownHostException {
        // PDFBox 3.0.0 workaround: set font directory BEFORE any PDFBox classes load
        // to avoid FileSystemFontProvider crashes on macOS malformed fonts
        try {
            Path safeFontDir = Files.createTempDirectory("pdfbox-fonts");
            System.setProperty("pdfbox.fontdir", safeFontDir.toString());
            log.info("PDFBox font directory set to: {}", safeFontDir);
        } catch (Exception e) {
            Path fallbackDir = Path.of(System.getProperty("java.io.tmpdir"));
            System.setProperty("pdfbox.fontdir", fallbackDir.toString());
            log.warn("Failed to create PDFBox font directory, using fallback: {}", fallbackDir);
        }

        var app = SpringApplication.run(TalomsApplication.class, args);
        var env = app.getEnvironment();
        log.info("============================================");
        log.info("TALOMS STARTED - VERSION: 35ecdc8");
        log.info("Profile: {}", env.getProperty("spring.profiles.active"));
        log.info("URL: http://{}:{}", InetAddress.getLocalHost().getHostAddress(), env.getProperty("server.port"));
        log.info("============================================");
    }

}


