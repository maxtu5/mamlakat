package com.tuiken.mamlakat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.tuiken.mamlakat.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonarchServiceImplTest {

    @Autowired
    private MonarchService monarchService;

    @Autowired
    private MonarchRetriever monarchRetriever;

    @Autowired
    private WorkflowService workflowService;

}