package com.example.javadoc0032022.repository;

import com.example.javadoc0032022.models.Role;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.enums.ERole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, PagingAndSortingRepository<User, Integer> {
    Optional<User> findByLogin(String login);

    Boolean existsByLogin(String login);
    Boolean existsByEmail(String email);

    User findByEmail(String email);

    @Transactional
    @Modifying
    @Query("UPDATE User a " +
            "SET a.enabled = TRUE WHERE a.email = ?1")
    int enableAppUser(String email);


    @Query("SELECT u FROM User u WHERE u.name LIKE concat(:search, '%') OR u.surName LIKE concat(:search, '%') " +
            "OR u.surName LIKE concat(:search, '%') OR u.nameOrganization LIKE concat(:search, '%') OR " +
            "u.identificationNumber LIKE concat(:search, '%') OR u.position LIKE concat(:search, '%') OR " +
            "u.subdivision LIKE concat(:search, '%')")
    Page<User> getUsersByFiltersSearch(Pageable pageable, @Param("search") String search);


}
