package com.example.javadoc0032022.repository;

import com.example.javadoc0032022.models.Package;
import com.example.javadoc0032022.models.User;
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

//    List<Package> findAllByUser(User user);
//    List<Package> findAllBySenderUser(User user);
//    List<Package> findAllByReceiverUser(User user);

    List<Package> findAllByPackageStatus(DocumentStatus status);

//    Page<Package> findAllPage(Pageable pageable);

    @Query("SELECT p FROM Package p WHERE p.name LIKE concat(:name, '%')")
    Page<Package> findByPackageName(Pageable pageable, @Param("name") String name);

    @Query("SELECT p FROM Package p WHERE p.createAt >= :oneDayAgoDate")
    List<Package> findLastPackages(@Param("oneDayAgoDate") LocalDateTime dateTime);

    @Query("SELECT p FROM Package p WHERE p.packageStatus = 'SENT_FOR_SIGNATURE' OR p.packageStatus = 'NOT_SIGNED' OR " +
            "p.packageStatus = 'SEND_FOR_APPROVAL'")
    List<Package> findPackagesSending();

    @Query("SELECT p FROM Package p WHERE p.senderUser.id = :userId ORDER BY p.id DESC")
    List<Package> findOutgoingPackages(@Param("userId") int userId);

    @Query("SELECT p FROM Package p WHERE p.receiverUser.id = :userId ORDER BY p.id DESC")
    List<Package> findIncomingPackages(@Param("userId") int userId);

    @Query("SELECT p FROM Package p WHERE p.receiverUser.id = :userId OR p.senderUser.id = :userId ORDER BY p.id DESC")
    List<Package> findAllUserPackages(@Param("userId") int userId);

    @Query("SELECT p FROM Package p WHERE p.receiverUser.id = :userId OR p.senderUser.id = :userId ORDER BY p.id DESC")
    Page<Package> findAllUserPackagesPage(@Param("userId") int userId, Pageable pageable);
}
