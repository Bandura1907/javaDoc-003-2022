package com.example.javadoc0032022.repository;

import com.example.javadoc0032022.models.OperationsHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationsHistoryRepository extends JpaRepository<OperationsHistory, Integer>, PagingAndSortingRepository<OperationsHistory, Integer> {

    @Query("SELECT o FROM OperationsHistory o ORDER BY o.id DESC")
    @Override
    Page<OperationsHistory> findAll(Pageable pageable);
}
