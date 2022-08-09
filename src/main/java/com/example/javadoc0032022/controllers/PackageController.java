package com.example.javadoc0032022.controllers;

import com.example.javadoc0032022.models.Package;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.payload.response.MessageResponse;
import com.example.javadoc0032022.repository.RoleRepository;
import com.example.javadoc0032022.services.PackageService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/package")
@AllArgsConstructor
public class PackageController {

    private PackageService packageService;
    private RoleRepository roleRepository;

    @GetMapping
    public ResponseEntity<?> findAll(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(required = false) String searchByName) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Package> pageTuts;

        if (searchByName != null) {
            pageTuts = packageService.findByPackageName(pageable, searchByName);
        } else pageTuts = packageService.findAllPage(pageable);

        List<Package> pack = pageTuts.getContent();
        Collections.reverse(pack);

        Map<String, Object> response = new HashMap<>();
        response.put("packages", pack);
        response.put("currentPage", pageTuts.getNumber());
        response.put("totalItems", pageTuts.getTotalElements());
        response.put("totalPages", pageTuts.getTotalPages());

        return ResponseEntity.ok(response);

    }

    @GetMapping("{packId}")
    public ResponseEntity<?> getPackage(@PathVariable int packId) {
        Optional<Package> pack = packageService.findById(packId);
        if (pack.isEmpty())
            return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);

        return ResponseEntity.ok(pack.get());
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

    @GetMapping("/packs_by_filters")
    public ResponseEntity<?> getPackagesByFilters(@AuthenticationPrincipal User user) {

        List<Package> incoming = packageService.findIncomingPackages(user.getId());
        List<Package> outgoing = packageService.findOutgoingPackages(user.getId());
        List<Package> drafts = packageService.findAllUserPackages(user.getId()).stream().filter(Package::isDraft).toList();
        List<Package> all = packageService.findAllUserPackages(user.getId());


        return ResponseEntity.ok(Map.of(
                "all", all,
                "incoming", incoming,
                "outgoing", outgoing,
                "drafts", drafts));
    }

    @DeleteMapping("/{packId}")
    public ResponseEntity<MessageResponse> deletePack(@PathVariable int packId) {
        packageService.deleteById(packId);
        return ResponseEntity.ok(new MessageResponse("Package " + packId + " deleted"));
    }
}
