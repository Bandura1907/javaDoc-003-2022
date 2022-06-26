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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/api/doc")
@AllArgsConstructor
public class DocumentController {

    private static final String NEW_DOC_FILE = "src/main/resources/static/documents/docECP.docx";
    private DocumentService documentService;
    private UserService userService;

//    @GetMapping("/download/{fileName:.+}")
//    public ResponseEntity downloadDb(@PathVariable String fileName) {
//        Document document = documentRepository.findByDocName(fileName);
//        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
//                .body(document.getFile());
//    }

    @GetMapping("/create")
    public ResponseEntity<InputStreamResource> createDoc() throws FileNotFoundException {
        File file = new File(NEW_DOC_FILE);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + file.getName())
                .contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(file.length()).body(resource);


    }

    @PostMapping("/upload/{userId}")
    public ResponseEntity<MessageResponse> uploadToDb(@PathVariable int userId,
                                                      @RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "isDraft", required = false, defaultValue = "false")
                                                      boolean draft) throws IOException {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty())
            return new ResponseEntity<>(new MessageResponse("user not found"),
                    HttpStatus.NOT_FOUND);

        Document doc = new Document();
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        doc.setDocName(fileName);
        doc.setFile(file.getBytes());
        doc.setDraft(draft);
        doc.setUser(user.get());
        doc.setStatus(DocumentStatus.NOT_SIGNED);
        documentService.save(doc);
        return ResponseEntity.ok(new MessageResponse("upload"));
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
