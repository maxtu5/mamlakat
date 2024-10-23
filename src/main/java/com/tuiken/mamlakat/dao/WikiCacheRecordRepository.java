package com.tuiken.mamlakat.dao;

import com.tuiken.mamlakat.model.Monarch;
import com.tuiken.mamlakat.model.WikiCacheRecord;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface WikiCacheRecordRepository extends CrudRepository<WikiCacheRecord, UUID> {

    Optional<WikiCacheRecord> findByUrl(String url);
}
