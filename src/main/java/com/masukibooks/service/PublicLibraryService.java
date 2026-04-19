package com.masukibooks.service;

import com.masukibooks.dto.request.PublicLibraryRequest;
import com.masukibooks.dto.response.PublicLibraryResponse;
import com.masukibooks.entity.BooksMetadata;
import com.masukibooks.entity.PublicLibrary;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.BooksMetadataRepository;
import com.masukibooks.repository.PublicLibraryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicLibraryService {

    private final PublicLibraryRepository publicLibraryRepository;
    private final BooksMetadataRepository productRepository;
    private final BookStorageService bookStorageService;
    private final PublicLibraryStorageService publicLibraryStorageService;

    public List<PublicLibraryResponse> listPublicItems() {
        return publicLibraryRepository.findByVisibilityOrderByCreatedAtDesc("public")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public PublicLibraryResponse getById(UUID id) {
        return toResponse(publicLibraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Public library record not found")));
    }

    @Transactional
    public PublicLibraryResponse createOrUpdate(PublicLibraryRequest request) {
        return createOrUpdateWithFile(request, null);
    }

    @Transactional
    public PublicLibraryResponse createOrUpdateWithFile(PublicLibraryRequest request, MultipartFile pdfFile) {
        BooksMetadata product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Book metadata not found"));

        PublicLibrary record = publicLibraryRepository.findByProductProductId(request.getProductId())
                .orElse(PublicLibrary.builder().product(product).build());

        if (request.getIsFeatured() != null) {
            record.setIsFeatured(request.getIsFeatured());
        }
        if (request.getVisibility() != null) {
            record.setVisibility(request.getVisibility());
        }
        if (request.getNotes() != null) {
            record.setNotes(request.getNotes());
        }
        if (request.getEditable() != null) {
            record.setEditable(request.getEditable());
        }

        PublicLibrary savedRecord = publicLibraryRepository.save(record);

        // Trigger S3 operations if a file is provided
        if (pdfFile != null && !pdfFile.isEmpty()) {
            publicLibraryStorageService.uploadBook(product.getTitle(), pdfFile, product);
        }

        return toResponse(savedRecord);
    }

    @Transactional
    public void delete(UUID id) {
        PublicLibrary record = publicLibraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Public library record not found"));
        
        // Trigger S3 deletion
        publicLibraryStorageService.deleteBook(record.getProduct().getTitle());
        
        publicLibraryRepository.delete(record);
    }

    private PublicLibraryResponse toResponse(PublicLibrary record) {
        String pdfUrl = publicLibraryStorageService.getBookPdfUrl(record.getProduct().getTitle());
        // Fallback to general storage service if public library URL fails/is missing
        if (pdfUrl == null) {
            pdfUrl = bookStorageService.resolvePublicUrl(record.getProduct().getFileKey());
        }

        return PublicLibraryResponse.builder()
                .publicLibraryId(record.getPublicLibraryId())
                .productId(record.getProduct().getProductId())
                .title(record.getProduct().getTitle())
                .author(record.getProduct().getAuthor())
                .fileUrl(pdfUrl)
                .visibility(record.getVisibility())
                .isFeatured(record.getIsFeatured())
                .notes(record.getNotes())
                .editable(record.getEditable())
                .build();
    }
}
