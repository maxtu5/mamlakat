package com.tuiken.mamlakat.dao;

import com.tuiken.mamlakat.model.Monarch;
import com.tuiken.mamlakat.model.UnhandledRecord;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface UnhandledRecordRepository extends CrudRepository<UnhandledRecord, UUID> {

}
