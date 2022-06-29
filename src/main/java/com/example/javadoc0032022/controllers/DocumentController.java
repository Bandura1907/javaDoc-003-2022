package com.example.javadoc0032022.controllers;

import com.example.javadoc0032022.models.Document;
import com.example.javadoc0032022.models.User;
import com.example.javadoc0032022.models.enums.DocumentStatus;
import com.example.javadoc0032022.payload.response.MessageResponse;
import com.example.javadoc0032022.services.DocumentService;
import com.example.javadoc0032022.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/doc")
@AllArgsConstructor
public class DocumentController {

    private static final String NEW_DOC_FILE = "src/main/resources/static/documents/docECP.docx";
    private DocumentService documentService;
    private UserService userService;

    @GetMapping("/create")
    public ResponseEntity<InputStreamResource> createDoc() throws FileNotFoundException {
        File file = new File(NEW_DOC_FILE);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(file.length()).body(resource);
    }

    @GetMapping
    public ResponseEntity<List<Document>> findAllDocuments() {
        return ResponseEntity.ok(documentService.findAll());
    }

    @GetMapping("/check_signature/{idDocument}")
    public ResponseEntity<?> checkSignature(@PathVariable int idDocument) {
        Optional<Document> document = documentService.findById(idDocument);
        if (document.isEmpty())
            return new ResponseEntity<>(new MessageResponse("Document not found"), HttpStatus.NOT_FOUND);

        if (document.get().getStatus().equals(DocumentStatus.SIGNED))
            return ResponseEntity.ok(Map.of("signature", true));
        else return ResponseEntity.ok(Map.of("signature", false));
    }

    @PostMapping("/upload")
    public ResponseEntity<MessageResponse> uploadToDb(@RequestParam("file") MultipartFile[] file,
                                                      @RequestParam(value = "isDraft", required = false, defaultValue = "false")
                                                      boolean draft) {
        Optional<User> user = userService.findByLogin(SecurityContextHolder.getContext().getAuthentication().getName());
        if (user.isEmpty())
            return new ResponseEntity<>(new MessageResponse("user not found"),
                    HttpStatus.NOT_FOUND);

        try {
            for (MultipartFile item : file) {
                Document doc = new Document();
                String fileName = StringUtils.cleanPath(item.getOriginalFilename());
                doc.setDocName(fileName);
                doc.setFile(item.getBytes());
                doc.setDraft(draft);
                doc.setSenderUser(user.get());
                doc.setStatus(DocumentStatus.NOT_SIGNED);
                documentService.save(doc);
            }

            return ResponseEntity.ok(new MessageResponse("upload"));
        } catch (Exception e) {
            return new ResponseEntity<>(new MessageResponse(e.getMessage()), HttpStatus.BAD_REQUEST);
        }

    }

    @PutMapping("/send_to_client_agreed/{userId}/{documentId}")
    public ResponseEntity<MessageResponse> sendToClientAgreed(@PathVariable int userId, @PathVariable int documentId) {
        Optional<User> user = userService.findById(userId);
        Optional<Document> document = documentService.findById(documentId);
        if (user.isEmpty() || document.isEmpty())
            return new ResponseEntity<>(new MessageResponse("User or document not found"), HttpStatus.NOT_FOUND);

        document.get().setStatus(DocumentStatus.SEND_FOR_APPROVAL);
        document.get().setReceiverUser(user.get());
        documentService.save(document.get());
        return ResponseEntity.ok(new MessageResponse("sent to client " + userId));
    }

    @PutMapping("/send_to_client_subscribe/{userId}/{documentId}")
    public ResponseEntity<?> sendToClientSubscribe(@PathVariable int userId, @PathVariable int documentId) {
        Optional<User> user = userService.findById(userId);
        Optional<Document> document = documentService.findById(documentId);
        if (user.isEmpty() || document.isEmpty())
            return new ResponseEntity<>(new MessageResponse("User or document not found"), HttpStatus.NOT_FOUND);

        document.get().setStatus(DocumentStatus.SENT_FOR_SIGNATURE);
        document.get().setReceiverUser(user.get());
        documentService.save(document.get());
        return ResponseEntity.ok(new MessageResponse("sent to client " + userId));
    }

    @PutMapping("/change_status/{idDocument}/{index}")
    public ResponseEntity<?> changeStatusDocument(@PathVariable int index, @PathVariable int idDocument) {
        Optional<Document> document = documentService.findById(idDocument);
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
    }

    @DeleteMapping("{id}")
    public ResponseEntity<MessageResponse> deleteDocument(@PathVariable int id) {
        documentService.deleteById(id);
        return ResponseEntity.ok(new MessageResponse("Document deleted"));
    }
}
