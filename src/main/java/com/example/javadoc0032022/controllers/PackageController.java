package com.example.javadoc0032022.controllers;

import com.example.javadoc0032022.models.Package;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.enums.ERole;
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

        Map<String, Object> response = new HashMap<>();
        response.put("packages", pageTuts.getContent());
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
//        Role roleUser = roleRepository.findByRole(ERole.ROLE_USER).get();
//        Role roleEmployee = roleRepository.findByRole(ERole.ROLE_EMPLOYEE).get();
//        Role roleAdmin = roleRepository.findByRole(ERole.ROLE_ADMIN).get();



        List<Package> incoming = new ArrayList<>();
        List<Package> outgoing = new ArrayList<>();
        List<Package> drafts = packageService.findAllByUser(user).stream().filter(Package::isDraft).toList();
        List<Package> all = packageService.findAllByUser(user);

        boolean isAdmin = user.getRoles().stream().toList().get(0).getRole().equals(ERole.ROLE_ADMIN);
        boolean isEmployee = user.getRoles().stream().toList().get(0).getRole().equals(ERole.ROLE_EMPLOYEE);
        boolean isUser  = user.getRoles().stream().toList().get(0).getRole().equals(ERole.ROLE_USER);



        if (isAdmin || isEmployee) {
            outgoing.addAll(packageService.findAll().stream().filter(x -> x.getReceiverUser() != null &&
                    x.getSenderUser().equals(user)).toList());
        } else if (isUser) {
            incoming.addAll(packageService.findAll().stream().filter(x -> x.getSenderUser() != null &&
                    x.getReceiverUser().equals(user)).toList());
        }


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
