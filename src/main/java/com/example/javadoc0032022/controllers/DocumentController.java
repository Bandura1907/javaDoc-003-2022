package com.example.javadoc0032022.controllers;

import com.example.javadoc0032022.models.Document;
import com.example.javadoc0032022.models.Package;
import com.example.javadoc0032022.models.Role;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.enums.DocumentStatus;
import com.example.javadoc0032022.models.enums.ERole;
import com.example.javadoc0032022.payload.response.DocumentFilterResponse;
import com.example.javadoc0032022.payload.response.MessageResponse;
import com.example.javadoc0032022.repository.PackageRepository;
import com.example.javadoc0032022.repository.RoleRepository;
import com.example.javadoc0032022.services.DocumentService;
import com.example.javadoc0032022.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
    @Value("${documents.files}")
    private String docPath;
    private final DocumentService documentService;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final PackageRepository packageRepository;

    public DocumentController(DocumentService documentService, UserService userService, RoleRepository roleRepository, PackageRepository packageRepository) {
        this.documentService = documentService;
        this.userService = userService;
        this.roleRepository = roleRepository;
        this.packageRepository = packageRepository;
    }

    @GetMapping
    public ResponseEntity<List<Package>> getDocuments(@RequestParam(value = "revers", required = false, defaultValue = "false") boolean revers) {
        if (revers) {
            List<Package> packages = packageRepository.findAll();
            Collections.reverse(packages);
            return ResponseEntity.ok(packages);
        }

        return ResponseEntity.ok(packageRepository.findAll());
    }

    @GetMapping("/get_awaiting_signing")
    public ResponseEntity<List<Document>> getDocsAwaitingSigning() {
        List<Document> documents = documentService.findAll().stream()
                .filter(x -> x.getStatus().equals(DocumentStatus.SENT_FOR_SIGNATURE))
                .collect(Collectors.toList());

        return ResponseEntity.ok(documents);
    }

    @GetMapping("status/{status}")
    public ResponseEntity<?> documentStatusList(@PathVariable DocumentStatus status) {
        List<Package> packageList = packageRepository.findAllByPackageStatus(status);
        return ResponseEntity.ok(packageList);
    }

    @GetMapping("/filters")
    public ResponseEntity<?> getFilters() {
        List<Package> packages = packageRepository.findAll();
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
        Optional<Package> pack = packageRepository.findById(packId);
        if (pack.isEmpty())
            return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);

        return ResponseEntity.ok(pack.get());
    }

    @Operation(summary = "Скачать документ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "404", description = "Документ не найден"),
            @ApiResponse(responseCode = "200", description = "Документ успешно загружен")
    })
    @GetMapping("/download")
    public ResponseEntity<?> downloadDoc(@RequestParam(value = "packId", required = false) Integer packId,
                                         @RequestParam(value = "userId", required = false) Integer userId) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(bo);

        if (packId != null) {
            Optional<Package> pack = packageRepository.findById(packId);
            if (pack.isEmpty())
                return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);

            for (Document doc : pack.get().getDocuments()) {
                ZipEntry zipEntry = new ZipEntry(doc.getName());
                zipOut.putNextEntry(zipEntry);
                zipOut.write(doc.getFile());
                zipOut.closeEntry();
            }
            zipOut.close();
            return ResponseEntity.ok(Map.of("name", "documents.zip",
                    "file", bo.toByteArray()));

        } else if (userId != null) {
            Optional<User> user = userService.findById(userId);
            return downloadZipFiles(user);
        }

        Optional<User> user = userService.findByLogin(SecurityContextHolder.getContext().getAuthentication().getName());
        return downloadZipFiles(user);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createDocument(@RequestParam("file") MultipartFile[] file,
                                            @RequestParam("comment") String comment,
                                            @RequestParam("name") String name,
                                            @AuthenticationPrincipal User user) throws IOException {
        Package pack = new Package();
        List<Document> documents = new ArrayList<>();

        pack.setComment(comment);
        pack.setName(name);
        pack.setSenderUser(user);
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
        Package savePack = packageRepository.save(pack);

        return ResponseEntity.ok(Map.of("packageId", savePack.getId()));
    }

    @PutMapping("/send/{packId}/{userId}")
    public ResponseEntity<?> sendDocument(@PathVariable int packId, @PathVariable int userId) {
        Optional<User> user = userService.findById(userId);
        Optional<Package> pack = packageRepository.findById(packId);
        if (user.isEmpty() || pack.isEmpty())
            return new ResponseEntity<>(new MessageResponse("User or package not found"), HttpStatus.NOT_FOUND);

        pack.get().setPackageStatus(DocumentStatus.SEND_FOR_APPROVAL);
        pack.get().getDocuments().forEach(x -> x.setStatus(DocumentStatus.SEND_FOR_APPROVAL));
        pack.get().setReceiverUser(user.get());
        packageRepository.save(pack.get());

        return ResponseEntity.ok(new MessageResponse("Send to user: " + user.get().getId()));
    }

    @PutMapping("/cancel/{packId}")
    public ResponseEntity<?> cancelDoc(@PathVariable int packId) {
        Optional<Package> pack = packageRepository.findById(packId);
        if (pack.isEmpty())
            return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);

        pack.get().setPackageStatus(DocumentStatus.REJECTED);
        pack.get().getDocuments().forEach(x -> x.setStatus(DocumentStatus.REJECTED));
        pack.get().setReceiverUser(null);
        packageRepository.save(pack.get());

        return ResponseEntity.ok(new MessageResponse("Document cancel"));
    }

    @PutMapping("/save_to_draft/{packId}")
    public ResponseEntity<?> saveToDraft(@PathVariable int packId) {
        Optional<Package> pack = packageRepository.findById(packId);
        if (pack.isEmpty())
            return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);

        pack.get().setDraft(true);
        packageRepository.save(pack.get());
        return ResponseEntity.ok(new MessageResponse("documents save to draft"));
    }

    @PutMapping("/add/{packId}")
    public ResponseEntity<?> addDocument(@PathVariable int packId,
                                         @RequestParam("file") MultipartFile[] files) throws IOException {
        Optional<Package> pack = packageRepository.findById(packId);
        if (pack.isEmpty())
            return new ResponseEntity<>(new MessageResponse("Package not found"), HttpStatus.NOT_FOUND);

        for (MultipartFile file : files) {
            Document document = new Document();
            document.setName(StringUtils.cleanPath(file.getOriginalFilename()));
            document.setStatus(DocumentStatus.NOT_SIGNED);
            document.setCreateAt(LocalDateTime.now());
            document.setFile(file.getBytes());
            document.setAPackage(pack.get());
            documentService.save(document);
        }

        return ResponseEntity.ok(pack.get());
    }

    @DeleteMapping("delete")
    public ResponseEntity<MessageResponse> deletePackage(@RequestParam(value = "document_id", required = false) Integer docId,
                                                         @RequestParam(value = "package_id", required = false) Integer packId) {
        if (docId != null) {
            documentService.deleteById(docId);
            return ResponseEntity.ok(new MessageResponse("Document " + docId + " deleted"));
        } else if (packId != null) {
            packageRepository.deleteById(packId);
            return ResponseEntity.ok(new MessageResponse("Package " + packId + " deleted"));
        } else return new ResponseEntity<>(new MessageResponse("Enter id doc or id pack"), HttpStatus.BAD_REQUEST);
    }

//    @Operation(summary = "Получение всех пакетов", description = "Документ передается в байтах")
//    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Package.class)))
//    @GetMapping
//    public ResponseEntity<List<Package>> findAllDocuments() {
//        return ResponseEntity.ok(packageRepository.findAll());
//    }
//
//    @GetMapping("{id}")
//    public ResponseEntity<?> findDocument(@PathVariable int id) {
//        Optional<Package> aPackage = packageRepository.findById(id);
//        if (aPackage.isEmpty())
//            return new ResponseEntity<>(new MessageResponse("package not found"), HttpStatus.NOT_FOUND);
//
//        return ResponseEntity.ok(aPackage.get());
//    }
//
//
//
//    @GetMapping("/view_users_subscribe/{documentId}")
//    public ResponseEntity<?> viewUsersSubscribe(@PathVariable int documentId) {
//        Optional<Document> document = documentService.findById(documentId);
//        if (document.isEmpty())
//            return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);
//
//        if (document.get().getStatus().equals(DocumentStatus.SIGNED)) {
//            return ResponseEntity.ok(new DocUsersSubscribeResponse(document.get().getId(), document.get().getStatus(),
//                    document.get().getAPackage().getSenderUser(), document.get().getAPackage().getReceiverUser()));
//        } else return ResponseEntity.ok(new MessageResponse("the document is not signed"));
//    }
//
//    @Operation(summary = "метод создания документа")
//    @ApiResponse(responseCode = "200", description = "Загрузка файла",
//            content = @Content(schema = @Schema(implementation = InputStreamResource.class)))
//    @GetMapping("/create")
//    public ResponseEntity<InputStreamResource> createDoc() throws FileNotFoundException {
//        File file = new File(docPath + "docECP.docx");
//        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
//        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
//                .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(file.length()).body(resource);
//    }
//
//    @Operation(summary = "Скачать документ")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "404", description = "Документ не найден",
//                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
//            @ApiResponse(responseCode = "200", description = "Документ успешно загружен")
//    })
//    @GetMapping("/download")
//    public ResponseEntity<?> downloadDoc(@RequestParam(value = "document_id", required = false) Integer documentId,
//                                         @RequestParam(value = "user_id", required = false) Integer userId) throws IOException {
//
//        if (documentId != null) {
//            Optional<Document> document = documentService.findById(documentId);
//            if (document.isEmpty())
//                return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);
//
//            return ResponseEntity.ok(Map.of("name", document.get().getDocName(),
//                    "file", document.get().getFile()));
////            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" +
////                            document.get().getDocName()).contentType(MediaType.APPLICATION_OCTET_STREAM)
////                    .contentLength(document.get().getFile().length).body(document.get().getFile());
//        } else if (userId != null) {
//            Optional<User> user = userService.findById(userId);
//           return downloadZipFiles(user);
//        }
//
//        Optional<User> user = userService.findByLogin(SecurityContextHolder.getContext().getAuthentication().getName());
//        return downloadZipFiles(user);
//
//    }
//
//    @Operation(summary = "Проверка подписи документа")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "{signature: false} или {signature: true}"),
//            @ApiResponse(responseCode = "404", description = "Документ не найден",
//                    content = @Content(schema = @Schema(implementation = MessageResponse.class)))
//    })
//    @GetMapping("/check_signature/{idDocument}")
//    public ResponseEntity<?> checkSignature(@Parameter(required = true, description = "id document") @PathVariable int idDocument) {
//        Optional<Document> document = documentService.findById(idDocument);
//        if (document.isEmpty())
//            return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);
//
//        if (document.get().getStatus().equals(DocumentStatus.SIGNED))
//            return ResponseEntity.ok(Map.of("signature", true));
//        else return ResponseEntity.ok(Map.of("signature", false));
//    }
//
//    @Operation(summary = "Загрузка файла", description = "Можно загрузить несколько файлов. Файли загружаются юзеру в аккаунт")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Файлы загружены",
//                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
//            @ApiResponse(responseCode = "404", description = "Юзер не найден"),
//            @ApiResponse(responseCode = "400")
//    })
//    @PostMapping("/upload")
//    public ResponseEntity<MessageResponse> uploadToDb(
//            @Parameter(required = true, description = "Передавать в Form Data. Параметр принимает файлы")
//            @RequestParam("file") MultipartFile[] file,
//            @RequestParam("name") String name,
//            @Parameter(description = "isDraft это черновик или нет. Необъязательно. Тоже передавать в form data")
//            @RequestParam(value = "isDraft", required = false, defaultValue = "false") boolean draft) {
//
//        Package aPackage = new Package();
//        List<Document> documents = new ArrayList<>();
//        Optional<User> user = userService.findByLogin(SecurityContextHolder.getContext().getAuthentication().getName());
//        if (user.isEmpty())
//            return new ResponseEntity<>(new MessageResponse("user not found"),
//                    HttpStatus.NOT_FOUND);
//
//        aPackage.setName(name);
//        aPackage.setSenderUser(user.get());
//
//        try {
//            for (MultipartFile item : file) {
//                Document doc = new Document();
//                String fileName = StringUtils.cleanPath(item.getOriginalFilename());
//                doc.setDocName(fileName);
//                doc.setFile(item.getBytes());
//                doc.setDraft(draft);
//                doc.setStatus(DocumentStatus.NOT_SIGNED);
//                doc.setAPackage(aPackage);
//                documents.add(doc);
//            }
//
//            aPackage.setDocuments(documents);
//            aPackage.setCreateAt(LocalDateTime.now());
//            packageRepository.save(aPackage);
//
//            return ResponseEntity.ok(new MessageResponse("upload"));
//        } catch (Exception e) {
//            return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
//        }
//
//    }
//
//    @Operation(summary = "Отправка документа на соглосование с клиентом")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "404", description = "Документ или юзер не найден",
//                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
//            @ApiResponse(responseCode = "200", description = "Документ отправлен на соглосование")
//    })
//    @PutMapping("/send_to_client_agreed/{userId}/{packageId}")
//    public ResponseEntity<MessageResponse> sendToClientAgreed(@Parameter(required = true, description = "User ID")
//                                                              @PathVariable int userId,
//                                                              @Parameter(required = true, description = "Document ID")
//                                                              @PathVariable int packageId) {
//        Optional<User> user = userService.findById(userId);
//        Optional<Package> aPackage = packageRepository.findById(packageId);
//        if (user.isEmpty() || aPackage.isEmpty())
//            return new ResponseEntity<>(new MessageResponse("User or document not found"), HttpStatus.NOT_FOUND);
//        aPackage.get().getDocuments().forEach(doc -> doc.setStatus(DocumentStatus.SEND_FOR_APPROVAL));
////        document.get().setStatus(DocumentStatus.SEND_FOR_APPROVAL);
//        aPackage.get().setReceiverUser(user.get());
//        packageRepository.save(aPackage.get());
//        return ResponseEntity.ok(new MessageResponse("sent to client " + userId));
//    }
//
//    @Operation(summary = "Отправка документа на подпись")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "404", description = "Документ или юзер не найден",
//                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
//            @ApiResponse(responseCode = "200", description = "Документ отправлен на подпись")
//    })
//    @PutMapping("/send_to_client_subscribe/{userId}/{documentId}")
//    public ResponseEntity<?> sendToClientSubscribe(@Parameter(required = true, description = "User ID")
//                                                   @PathVariable int userId,
//                                                   @Parameter(required = true, description = "Document ID")
//                                                   @PathVariable int documentId) {
//        Optional<User> user = userService.findById(userId);
//        Optional<Package> aPackage = packageRepository.findById(documentId);
//        if (user.isEmpty() || aPackage.isEmpty())
//            return new ResponseEntity<>(new MessageResponse("User or document not found"), HttpStatus.NOT_FOUND);
//        aPackage.get().getDocuments().forEach(doc -> doc.setStatus(DocumentStatus.SENT_FOR_SIGNATURE));
////        for (Document doc : aPackage.get().getDocuments()) {
////            doc.setStatus(DocumentStatus.SENT_FOR_SIGNATURE);
////        }
//
//        aPackage.get().setReceiverUser(user.get());
//        packageRepository.save(aPackage.get());
//        return ResponseEntity.ok(new MessageResponse("sent to client " + userId));
//    }
//

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
                case 0:
                    documentService.agreed(document.get());
                    break;
                case 1:
                    documentService.subscribe(document.get());
                    break;
                case 2:
                    documentService.refuse(document.get());
                    break;
                case 3:
                    documentService.reject(document.get());
                    break;
                default:
                    return new ResponseEntity<>(new MessageResponse("Index not found (range index 0-3)"), HttpStatus.NOT_FOUND);
            }

            return ResponseEntity.ok(document.get());
        } else if (packId != null && docId == null) {
            Optional<Package> pack = packageRepository.findById(packId);
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

            return ResponseEntity.ok(packageRepository.save(pack.get()));
        } else if (packId != null && docId != null) {
            Optional<Document> document = documentService.findById(docId);
            Optional<Package> pack = packageRepository.findById(packId);
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

            return ResponseEntity.ok(packageRepository.save(pack.get()));
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
        return ResponseEntity.ok(Map.of(
                "name", "documents.zip",
                "file", bo.toByteArray()
        ));
//        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=documents.zip")
//                .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(bo.size()).body(bo.toByteArray());
    }
}
