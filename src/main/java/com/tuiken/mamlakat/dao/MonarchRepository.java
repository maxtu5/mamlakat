package com.tuiken.mamlakat.dao;

import com.tuiken.mamlakat.model.Monarch;
import com.tuiken.mamlakat.model.Person;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface MonarchRepository extends CrudRepository<Monarch, UUID> {

    Optional<Monarch> findByUrl(String url);
}
