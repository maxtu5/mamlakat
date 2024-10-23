package com.tuiken.mamlakat.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

//@SpringBootTest
@DataNeo4jTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GraphFirstServiceTest {

    @Autowired
    private GraphFirstService graphFirstService;


}