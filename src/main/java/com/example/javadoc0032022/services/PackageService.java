package com.example.javadoc0032022.services;

import com.example.javadoc0032022.models.Package;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.enums.DocumentStatus;
import com.example.javadoc0032022.repository.PackageRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PackageService {

    private PackageRepository packageRepository;
    private TemplateEngine templateEngine;

    public List<Package> findAll() {
        return packageRepository.findAll();
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

    public List<Package> findOutgoingPackages(int userId) {
        return packageRepository.findOutgoingPackages(userId);
    }

    public List<Package> findIncomingPackages(int userId) {
        return packageRepository.findIncomingPackages(userId);
    }

    public List<Package> findAllUserPackages(int userId) {
        return packageRepository.findAllUserPackages(userId);
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

    public String buildNotificationEmail(String packageName) {
        Context context = new Context();
        context.setVariable("packageName", packageName);
        return templateEngine.process("document-mail", context);
    }
}
