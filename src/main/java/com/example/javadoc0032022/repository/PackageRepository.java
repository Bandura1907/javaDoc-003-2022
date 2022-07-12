package com.example.javadoc0032022.repository;

import com.example.javadoc0032022.models.Package;
import com.example.javadoc0032022.models.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackageRepository extends JpaRepository<Package, Integer> {

    List<Package> findAllByPackageStatus(DocumentStatus status);
}
