package com.tuiken.mamlakat.service;

import com.tuiken.mamlakat.dao.ProvenenceRepository;
import com.tuiken.mamlakat.model.Monarch;
import com.tuiken.mamlakat.model.Provenence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FamilyRetrieverTest {

@Autowired
FamilyRetriever familyRetriever;
@Autowired
ProvenenceRepository provenenceRepository;


    @Test
    void findProv() {
        Iterable<Provenence> p = provenenceRepository.findAll();
        System.out.println(p);
    }
}