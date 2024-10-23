package com.tuiken.mamlakat.model.workflows;

import com.tuiken.mamlakat.model.Provenence;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SaveFamilyConfiguration {
    List<Provenence> toCreate = new ArrayList<>();

    public void print() {
        System.out.println("=== Ready to update families ===");
        toCreate.forEach(s-> {
            System.out.println("== Relation: " + s.getId());
            System.out.println("= Father: " + s.getFather());
            System.out.println("= Mother: " + s.getMother());
        });
    }
}
