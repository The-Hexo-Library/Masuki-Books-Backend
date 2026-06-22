package com.masukibooks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class MasukibooksApplication {
    public static void main(String[] args) {
        loadEnv();
        SpringApplication.run(MasukibooksApplication.class, args);
    }

    private static void loadEnv() {
        try {
            java.nio.file.Path envPath = Paths.get(".env");
            if (!Files.exists(envPath)) {
                envPath = Paths.get("backend", ".env");
            }
            if (Files.exists(envPath)) {
                System.out.println("Loading environment variables from: " + envPath.toAbsolutePath());
                List<String> lines = Files.readAllLines(envPath);
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        }
                        System.setProperty(key, value);
                    }
                }
            } else {
                System.out.println("No .env file found at .env or backend/.env");
            }
        } catch (IOException e) {
            System.err.println("Could not load .env file: " + e.getMessage());
        }
    }
}
