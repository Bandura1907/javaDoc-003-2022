package com.example.javadoc0032022.repository;

import com.example.javadoc0032022.models.Package;
import com.example.javadoc0032022.models.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PackageRepository extends JpaRepository<Package, Integer>, PagingAndSortingRepository<Package, Integer> {

    List<Package> findAllByPackageStatus(DocumentStatus status);

//    Page<Package> findAllPage(Pageable pageable);

    @Query("SELECT p FROM Package p WHERE p.name LIKE concat(:name, '%') ")
    Page<Package> findByPackageName(Pageable pageable, @Param("name") String name);

    @Query("SELECT p FROM Package p WHERE p.createAt >= :oneDayAgoDate")
    List<Package> findLastPackages(@Param("oneDayAgoDate") LocalDateTime dateTime);

    @Query("SELECT p FROM Package p WHERE p.packageStatus = 'SENT_FOR_SIGNATURE'")
    List<Package> findPackagesSending();
}
