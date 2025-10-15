package com.devcollab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.awt.Desktop;
import java.net.URI;

@SpringBootApplication
public class DevCollabApp {

    public static void main(String[] args) {
        SpringApplication.run(DevCollabApp.class, args);
        try {
            Thread.sleep(3000);
            Desktop.getDesktop().browse(new URI("http://localhost:8082/view/home"));
            System.out.println("Browser opened: http://localhost:8082/view/home");
        } catch (Exception e) {
            System.err.println(" Không thể mở trình duyệt: " + e.getMessage());
        }
    }
}
