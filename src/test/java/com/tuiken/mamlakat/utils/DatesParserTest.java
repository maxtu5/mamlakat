package com.tuiken.mamlakat.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DatesParserTest {

    @Test
    void findTwoDates() {

    }

    @Test
    void findDate() {
        String date = "18 May [O.S. 6 May] 1868 Alexander Palace, Tsarskoye Selo, Russian Empire";
        Instant parserDate = DatesParser.findDate(date);
    }
}