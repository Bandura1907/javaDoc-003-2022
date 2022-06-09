package com.example.javadoc0032022.repository;

import com.example.javadoc0032022.models.ERole;
import com.example.javadoc0032022.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRole(ERole role);
}
