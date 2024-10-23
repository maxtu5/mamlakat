package com.tuiken.mamlakat.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExportServiceTest {
    @Autowired
    private ExportService exportService;

    @Test
    void exportSample() throws Exception {
        exportService.exportSample();
    }
}