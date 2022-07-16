package com.example.javadoc0032022.repository;

import com.example.javadoc0032022.models.enums.ERole;
import com.example.javadoc0032022.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRole(ERole role);
}
