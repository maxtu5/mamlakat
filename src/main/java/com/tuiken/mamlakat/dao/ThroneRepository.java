package com.tuiken.mamlakat.dao;

import com.tuiken.mamlakat.model.Country;
import com.tuiken.mamlakat.model.dtos.Throne;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface ThroneRepository extends CrudRepository<Throne, UUID> {

    List<Throne> findByCountry(Country country);
}
