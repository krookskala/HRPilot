package com.hrpilot.backend.config;

import com.hrpilot.backend.seed.DemoDataSeeder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final DemoDataSeeder demoDataSeeder;

    @Override
    @Transactional
    public void run(String... args) {
        demoDataSeeder.seedIfEmpty();
    }
}
