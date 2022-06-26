package com.example.javadoc0032022.services;

import com.example.javadoc0032022.models.Document;
import com.example.javadoc0032022.models.enums.DocumentStatus;
import com.example.javadoc0032022.repository.DocumentRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class DocumentService {

    private DocumentRepository documentRepository;

    public Optional<Document> findById(int id) {
        return documentRepository.findById(id);
    }

    public void deleteById(int id) {
        documentRepository.deleteById(id);
    }

    public void save(Document document) {
        documentRepository.save(document);
    }

    //function - agree
    public void agreed(Document document) {
        document.setStatus(DocumentStatus.AGREED);
        documentRepository.save(document);
    }

    //function - sign
    public void subscribe(Document document) {
        document.setStatus(DocumentStatus.SIGNED);
        documentRepository.save(document);
    }

    //function - refuse
    public void refuse(Document document) {
        document.setStatus(DocumentStatus.DENIED);
        documentRepository.save(document);
    }

    //function - reject
    public void reject(Document document) {
        document.setStatus(DocumentStatus.REJECTED);
        documentRepository.save(document);
    }
}
