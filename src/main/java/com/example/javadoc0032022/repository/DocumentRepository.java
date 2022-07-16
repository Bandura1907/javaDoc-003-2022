package com.example.javadoc0032022.repository;

import com.example.javadoc0032022.models.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {

    Document findByName(String name);
}
