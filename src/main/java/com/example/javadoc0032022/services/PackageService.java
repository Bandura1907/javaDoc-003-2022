package com.example.javadoc0032022.services;

import com.example.javadoc0032022.models.Package;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.enums.DocumentStatus;
import com.example.javadoc0032022.repository.PackageRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PackageService {

    private PackageRepository packageRepository;

    public List<Package> findAll() {
        return packageRepository.findAll();
    }

    public List<Package> findAllByUser(User user) {
        return packageRepository.findAllByUser(user);
    }

    public List<Package> findAllBySenderUser(User user) {
        return packageRepository.findAllBySenderUser(user);
    }

    public List<Package> findAllByReceiverUser(User user) {
        return packageRepository.findAllByReceiverUser(user);
    }

    public List<Package> findAllByPackageStatus(DocumentStatus status) {
        return packageRepository.findAllByPackageStatus(status);
    }

    public List<Package> getLastPackages() {
        return packageRepository.findLastPackages(LocalDateTime.now().minusDays(1));
    }

    public List<Package> getSendingPackages() {
        return packageRepository.findPackagesSending();
    }

    public Page<Package> findAllPage(Pageable pageable) {
        return packageRepository.findAll(pageable);
    }

    public Page<Package> findByPackageName(Pageable pageable, String name) {
        return packageRepository.findByPackageName(pageable, name);
    }

    public Optional<Package> findById(int id) {
        return packageRepository.findById(id);
    }

    public Package save(Package pack) {
        return packageRepository.save(pack);
    }

    public void deleteById(int id) {
        packageRepository.deleteById(id);
    }
}
