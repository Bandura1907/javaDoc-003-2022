package com.example.javadoc0032022.controllers;

import com.example.javadoc0032022.models.Package;
import com.example.javadoc0032022.services.PackageService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/package")
@AllArgsConstructor
public class PackageController {

    private PackageService packageService;

    @GetMapping
    public ResponseEntity<?> findAll(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(required = false) String searchByName) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Package> pageTuts;

        if (searchByName != null) {
            pageTuts = packageService.findByPackageName(pageable, searchByName);
        } else pageTuts = packageService.findAllPage(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("packages", pageTuts.getContent());
        response.put("currentPage", pageTuts.getNumber());
        response.put("totalItems", pageTuts.getTotalElements());
        response.put("totalPages", pageTuts.getTotalPages());

        return ResponseEntity.ok(response);

    }

    @GetMapping("/get_last_unsigned")
    public ResponseEntity<?> getLastUnsigned() {
        List<Package> last = packageService.getLastPackages();
        List<Package> unsigned = packageService.getSendingPackages();
        return ResponseEntity.ok(Map.of(
                "last", last,
                "unsigned", unsigned
        ));
    }
}
