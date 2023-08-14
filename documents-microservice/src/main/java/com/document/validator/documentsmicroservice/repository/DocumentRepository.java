package com.document.validator.documentsmicroservice.repository;

import com.document.validator.documentsmicroservice.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document,Integer> {
    Document findFirstByHashOriginalDocument(String hashOriginalDocument);
    Document findFirstByUuid(String uuid);
}
