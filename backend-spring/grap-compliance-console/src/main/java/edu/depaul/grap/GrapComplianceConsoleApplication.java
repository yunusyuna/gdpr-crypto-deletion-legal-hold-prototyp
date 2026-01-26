package edu.depaul.grap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "edu.depaul.grap")
public class GrapComplianceConsoleApplication {
    public static void main(String[] args) {
        SpringApplication.run(GrapComplianceConsoleApplication.class, args);
    }
}

