package com.example.javadoc0032022.controllers;

import com.example.javadoc0032022.models.Document;
import com.example.javadoc0032022.models.Package;
import com.example.javadoc0032022.models.Role;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.enums.DocumentStatus;
import com.example.javadoc0032022.models.enums.ERole;
import com.example.javadoc0032022.models.enums.PackageType;
import com.example.javadoc0032022.payload.response.DocumentFilterResponse;
import com.example.javadoc0032022.payload.response.MessageResponse;
import com.example.javadoc0032022.repository.RoleRepository;
import com.example.javadoc0032022.services.DocumentService;
import com.example.javadoc0032022.services.PackageService;
import com.example.javadoc0032022.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/doc")
public class DocumentController {

    //    private static final String NEW_DOC_FILE = "src/main/resources/static/documents/docECP.docx";
//    @Value("${documents.files}")
//    private String docPath;
    private final DocumentService documentService;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final PackageService packageService;

    public DocumentController(DocumentService documentService, UserService userService, RoleRepository roleRepository, PackageService packageService) {
        this.documentService = documentService;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.packageService = packageService;
    }

    @GetMapping
    public ResponseEntity<List<Package>> getDocuments(@RequestParam(value = "revers", required = false, defaultValue = "false") boolean revers) {
        if (revers) {
            List<Package> packages = packageService.findAll();
            Collections.reverse(packages);
            return ResponseEntity.ok(packages);
        }

        return ResponseEntity.ok(packageService.findAll());
    }

    @GetMapping("/get_last_unsigned")
    public ResponseEntity<List<Document>> getDocsAwaitingSigning() {
        List<Document> documents = documentService.findAll().stream()
                .filter(x -> x.getStatus().equals(DocumentStatus.SENT_FOR_SIGNATURE))
                .collect(Collectors.toList());

        return ResponseEntity.ok(documents);
    }

    @GetMapping("status/{status}")
    public ResponseEntity<?> documentStatusList(@PathVariable DocumentStatus status) {
        List<Package> packageList = packageService.findAllByPackageStatus(status);
        return ResponseEntity.ok(packageList);
    }

    @GetMapping("/filters")
    public ResponseEntity<?> getFilters() {
        List<Package> packages = packageService.findAll();
        List<DocumentFilterResponse> documentFilterResponses = new ArrayList<>();

        int countInput = (int) packages.stream().map(Package::getReceiverUser).filter(Objects::nonNull).count();
        int countOutput = (int) packages.stream().map(Package::getSenderUser).filter(Objects::nonNull).count();
        int countDraft = (int) packages.stream().map(Package::isDraft).filter(x -> x.equals(true)).count();

        documentFilterResponses.add(new DocumentFilterResponse("input", countInput));
        documentFilterResponses.add(new DocumentFilterResponse("output", countOutput));
        documentFilterResponses.add(new DocumentFilterResponse("blackwork", countDraft));

        return ResponseEntity.ok(documentFilterResponses);
    }

    @GetMapping("{packId}")
    public ResponseEntity<?> getPackage(@PathVariable int packId) {
        Optional<Package> pack = packageService.findById(packId);
        if (pack.isEmpty())
            return new ResponseEntity<>(new MessageResponse("Package not found"), HttpStatus.NOT_FOUND);

        return ResponseEntity.ok(pack.get().getDocuments());
    }

    @Operation(summary = "Скачать документ", description = "Качать можно документ и пакет или по юзер айди " +
            ", скачайте все документы которые привязаны к его аккаунту")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Документ не найден"),
            @ApiResponse(responseCode = "200", description = "Документ успешно загружен")
    })
    @GetMapping("/download")
    public ResponseEntity<?> downloadDoc(@RequestParam(value = "packId", required = false) Integer packId,
                                         @RequestParam(value = "userId", required = false) Integer userId,
                                         @RequestParam(required = false) Integer dockId) throws IOException {


        if (packId != null) {
            Optional<Package> pack = packageService.findById(packId);
            if (pack.isEmpty())
                return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);

            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(bo);

            for (Document doc : pack.get().getDocuments()) {
                ZipEntry zipEntry = new ZipEntry(doc.getName());
                zipOut.putNextEntry(zipEntry);
                zipOut.write(doc.getFile());
                zipOut.closeEntry();
            }
            zipOut.close();
//            return ResponseEntity.ok(Map.of("name", "documents.zip",
//                    "file", bo.toByteArray()));

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=documents.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(bo.size()).body(bo.toByteArray());

        } else if (userId != null) {
            Optional<User> user = userService.findById(userId);
            return downloadZipFiles(user);
        } else if (dockId != null) {
            Optional<Document> document = documentService.findById(dockId);
            if (document.isEmpty())
                return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);

//            return ResponseEntity.ok(Map.of(
//                    "name", document.get().getName(),
//                    "file", document.get().getFile()
//            ));

            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + document.get().getName())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(document.get().getFile().length)
                    .body(document.get().getFile());
        }

        Optional<User> user = userService.findByLogin(SecurityContextHolder.getContext().getAuthentication().getName());
        return downloadZipFiles(user);
    }

    @Operation(summary = "создать пакет")
    @PostMapping("/create")
    public ResponseEntity<?> createDocument(@RequestParam("file") MultipartFile[] file,
                                            @Parameter(description = "коментарий") @RequestParam(value = "comment", required = false) String comment,
                                            @Parameter(required = true, description = "Имя пакета") @RequestParam String name,
                                            @Parameter(description = "Тип пакета. Есть 3 типа пакетов (CONTRACT, AGREEMENT, LETTER)")
                                            @RequestParam(defaultValue = "LETTER") PackageType type,
                                            @AuthenticationPrincipal User user) throws IOException {
        Package pack = new Package();
        List<Document> documents = new ArrayList<>();

        pack.setComment(comment);
        pack.setName(name);
        pack.setUser(user);
//        pack.setSenderUser(user);
        pack.setPackageType(type);
        pack.setDraft(false);
        pack.setPackageStatus(DocumentStatus.NOT_SIGNED);
        pack.setCreateAt(LocalDateTime.now());

        for (MultipartFile item : file) {
            String nameFile = StringUtils.cleanPath(item.getOriginalFilename());

            Document doc = new Document();
            doc.setFile(item.getBytes());
            doc.setName(nameFile);
            doc.setStatus(DocumentStatus.NOT_SIGNED);
            doc.setCreateAt(LocalDateTime.now());
            doc.setAPackage(pack);
            documents.add(doc);
        }

        pack.setDocuments(documents);
        Package savePack = packageService.save(pack);

        return ResponseEntity.ok(Map.of("packageId", savePack.getId()));
    }

    @PutMapping("/send/{packId}/{userId}")
    public ResponseEntity<?> sendDocument(@PathVariable int packId, @PathVariable int userId,
                                          @AuthenticationPrincipal User userPrincipal) {
        Optional<User> user = userService.findById(userId);
        Optional<Package> pack = packageService.findById(packId);
        if (user.isEmpty() || pack.isEmpty())
            return new ResponseEntity<>(new MessageResponse("User or package not found"), HttpStatus.NOT_FOUND);

        pack.get().setPackageStatus(DocumentStatus.SEND_FOR_APPROVAL);
        pack.get().getDocuments().forEach(x -> x.setStatus(DocumentStatus.SEND_FOR_APPROVAL));
        pack.get().setReceiverUser(user.get());
        pack.get().setSenderUser(userPrincipal);
        packageService.save(pack.get());

        return ResponseEntity.ok(new MessageResponse("Send to user: " + user.get().getId()));
    }

    @PutMapping("/cancel/{packId}")
    public ResponseEntity<?> cancelDoc(@PathVariable int packId) {
        Optional<Package> pack = packageService.findById(packId);
        if (pack.isEmpty())
            return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);

        pack.get().setPackageStatus(DocumentStatus.REJECTED);
        pack.get().getDocuments().forEach(x -> x.setStatus(DocumentStatus.REJECTED));
        pack.get().setReceiverUser(null);
        packageService.save(pack.get());

        return ResponseEntity.ok(new MessageResponse("Document cancel"));
    }

    @Operation(summary = "Сохраняет пакет в черновик или удаляет пакет из черновика")
    @PutMapping("/save_to_draft/{packId}")
    public ResponseEntity<?> saveToDraft(@PathVariable int packId) {
        Optional<Package> pack = packageService.findById(packId);
        if (pack.isEmpty())
            return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);

        pack.get().setDraft(!pack.get().isDraft());
        packageService.save(pack.get());
        return ResponseEntity.ok(new MessageResponse(pack.get().isDraft() ? "documents save to draft" : "documents removed from draft"));
    }

    @PutMapping("/add/{packId}")
    public ResponseEntity<?> addDocument(@PathVariable int packId,
                                         @RequestParam("file") MultipartFile[] files) throws IOException {
        Optional<Package> pack = packageService.findById(packId);
        if (pack.isEmpty())
            return new ResponseEntity<>(new MessageResponse("Package not found"), HttpStatus.NOT_FOUND);

//        for (var file : files) {
//            System.out.println(getFileExtension(file.getOriginalFilename()));
//        }

        for (MultipartFile file : files) {
            Document document = new Document();
            document.setName(file.getOriginalFilename());

            if (getFileExtension(file.getOriginalFilename()).equals("sig")) {
                document.setStatus(DocumentStatus.SIGNED);
            } else
                document.setStatus(DocumentStatus.NOT_SIGNED);

            document.setCreateAt(LocalDateTime.now());
            document.setFile(file.getBytes());
            document.setAPackage(pack.get());
            documentService.save(document);
        }

        return ResponseEntity.ok(pack.get());
    }

    @DeleteMapping("{dockId}")
    public ResponseEntity<MessageResponse> deletePackage(@PathVariable Integer dockId) {
        documentService.deleteById(dockId);
        return ResponseEntity.ok(new MessageResponse("Document " + dockId + " deleted"));

    }


    @Operation(summary = "Смена статуса документа",
            description = "статус документа меняется через индекс: 0 - документ согласован, 1 - документ подписан, " +
                    "2 - отказано, 3 - отменено")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Документ или индекс не найден",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "200", description = "Статус изменен",
                    content = @Content(schema = @Schema(implementation = Document.class)))
    })
    @PutMapping("/change_status/{index}")
    public ResponseEntity<?> changeStatusDocument(@Parameter(required = true, description = "Index") @PathVariable Integer index,
                                                  @RequestParam(value = "package_id", required = false) Integer packId,
                                                  @RequestParam(value = "document_id", required = false) Integer docId) {

        if (docId != null && packId == null) {
            Optional<Document> document = documentService.findById(docId);
            if (document.isEmpty())
                return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);

            switch (index) {
                case 0 -> documentService.agreed(document.get());
                case 1 -> documentService.subscribe(document.get());
                case 2 -> documentService.refuse(document.get());
                case 3 -> documentService.reject(document.get());
                default ->
                        new ResponseEntity<>(new MessageResponse("Index not found (range index 0-3)"), HttpStatus.NOT_FOUND);
            }

            Package pack = document.get().getAPackage();
            if (pack.getDocuments().stream().allMatch(x -> x.getStatus().equals(DocumentStatus.SIGNED))) {
                pack.setPackageStatus(DocumentStatus.SIGNED);
                packageService.save(pack);
            }

            return ResponseEntity.ok(document.get());
        } else if (packId != null && docId == null) {
            Optional<Package> pack = packageService.findById(packId);
            if (pack.isEmpty())
                return new ResponseEntity<>(new MessageResponse("Package not found"), HttpStatus.NOT_FOUND);

            switch (index) {
                case 0:
                    pack.get().setPackageStatus(DocumentStatus.AGREED);
                    break;
                case 1:
                    pack.get().setPackageStatus(DocumentStatus.SIGNED);
                    break;
                case 2:
                    pack.get().setPackageStatus(DocumentStatus.DENIED);
                    break;
                case 3:
                    pack.get().setPackageStatus(DocumentStatus.REJECTED);
                    break;
                default:
                    return new ResponseEntity<>(new MessageResponse("Index not found (range index 0-3)"), HttpStatus.NOT_FOUND);
            }

            return ResponseEntity.ok(packageService.save(pack.get()));
        } else if (packId != null && docId != null) {
            Optional<Document> document = documentService.findById(docId);
            Optional<Package> pack = packageService.findById(packId);
            if (document.isEmpty() || pack.isEmpty())
                return new ResponseEntity<>(new MessageResponse("Doc or pack not found"), HttpStatus.NOT_FOUND);

            switch (index) {
                case 0:
                    pack.get().setPackageStatus(DocumentStatus.AGREED);
                    documentService.agreed(document.get());
                    break;
                case 1:
                    pack.get().setPackageStatus(DocumentStatus.SIGNED);
                    documentService.subscribe(document.get());
                    break;
                case 2:
                    pack.get().setPackageStatus(DocumentStatus.DENIED);
                    documentService.refuse(document.get());
                    break;
                case 3:
                    pack.get().setPackageStatus(DocumentStatus.REJECTED);
                    documentService.reject(document.get());
                    break;
                default:
                    return new ResponseEntity<>(new MessageResponse("Index not found (range index 0-3)"), HttpStatus.NOT_FOUND);
            }

            return ResponseEntity.ok(packageService.save(pack.get()));
        } else {
            return new ResponseEntity<>(new MessageResponse("Enter ids"), HttpStatus.BAD_REQUEST);
        }
//
//        return ResponseEntity.ok(document.get());
    }

    //
//    @Operation(summary = "Удаление документа")
//    @DeleteMapping("{id}")
//    public ResponseEntity<MessageResponse> deleteDocument(@Parameter(required = true, description = "Document ID")
//                                                          @PathVariable int id) {
//        documentService.deleteById(id);
//        return ResponseEntity.ok(new MessageResponse("Document deleted"));
//    }
//
    private ResponseEntity<?> downloadZipFiles(Optional<User> user) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bo);

        Role roleUser = roleRepository.findByRole(ERole.ROLE_USER).get();
        Role roleEmployee = roleRepository.findByRole(ERole.ROLE_EMPLOYEE).get();
        if (user.isEmpty())
            return new ResponseEntity<>(new MessageResponse("User not found"), HttpStatus.NOT_FOUND);

        if (user.get().getRoles().contains(roleEmployee)) {
//            for (Document doc : user.get().getDocumentSenderList()) {
//                ZipEntry zipEntry = new ZipEntry(doc.getDocName());
//                zipOut.putNextEntry(zipEntry);
//                zipOut.write(doc.getFile());
//                zipOut.closeEntry();
//            }
            for (Package pack : user.get().getPackageSenderList()) {
                for (Document doc : pack.getDocuments()) {
                    ZipEntry zipEntry = new ZipEntry(doc.getName());
                    zipOut.putNextEntry(zipEntry);
                    zipOut.write(doc.getFile());
                    zipOut.closeEntry();
                }
            }
        } else if (user.get().getRoles().contains(roleUser)) {
            for (Package pack : user.get().getPackageReceiverList()) {
                for (Document doc : pack.getDocuments()) {
                    ZipEntry zipEntry = new ZipEntry(doc.getName());
                    zipOut.putNextEntry(zipEntry);
                    zipOut.write(doc.getFile());
                    zipOut.closeEntry();
                }
            }
        }

        zipOut.close();
//        return ResponseEntity.ok(Map.of(
//                "name", "documents.zip",
//                "file", bo.toByteArray()
//        ));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=documents.zip")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(bo.size()).body(bo.toByteArray());
    }

    private String getFileExtension(String fileName) {
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        } else
            return "";
    }
}
