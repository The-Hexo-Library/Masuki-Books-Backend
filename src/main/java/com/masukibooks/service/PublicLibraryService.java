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
        backfillPublicLibraryRecords();
        return publicLibraryRepository.findByVisibilityIgnoreCaseOrderByCreatedAtDesc("public")
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
        BooksMetadata product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Book metadata not found"));

        PublicLibrary record = publicLibraryRepository.findByProductProductId(request.getProductId())
                .orElse(PublicLibrary.builder().product(product).build());

        if (request.getIsFeatured() != null) {
            record.setIsFeatured(request.getIsFeatured());
        }
        if (request.getVisibility() != null) {
            record.setVisibility(normalizeVisibility(request.getVisibility()));
        } else if (record.getVisibility() == null || record.getVisibility().isBlank()) {
            record.setVisibility("public");
        }
        if (request.getNotes() != null) {
            record.setNotes(request.getNotes());
        }
        if (request.getEditable() != null) {
            record.setEditable(request.getEditable());
        }

        return toResponse(publicLibraryRepository.save(record));
    }

    @Transactional
    public PublicLibraryResponse createOrUpdateWithFile(PublicLibraryRequest request, org.springframework.web.multipart.MultipartFile pdfFile) {
        BooksMetadata product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Book metadata not found"));

        PublicLibrary record = publicLibraryRepository.findByProductProductId(request.getProductId())
                .orElse(PublicLibrary.builder().product(product).build());      

        if (request.getIsFeatured() != null) {
            record.setIsFeatured(request.getIsFeatured());
        }
        if (request.getVisibility() != null) {
            record.setVisibility(normalizeVisibility(request.getVisibility()));
        } else if (record.getVisibility() == null || record.getVisibility().isBlank()) {
            record.setVisibility("public");
        }
        if (request.getNotes() != null) {
            record.setNotes(request.getNotes());
        }
        if (request.getEditable() != null) {
            record.setEditable(request.getEditable());
        }

        PublicLibrary savedRecord = publicLibraryRepository.save(record);

        if (pdfFile != null && !pdfFile.isEmpty()) {
            publicLibraryStorageService.uploadBook(product.getTitle(), pdfFile, product);
        }

        return toResponse(savedRecord);
    }

    @Transactional
    public void delete(UUID id) {
        PublicLibrary record = publicLibraryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Public library record not found"));
        
        publicLibraryStorageService.deleteBook(record.getProduct().getTitle());
        
        publicLibraryRepository.delete(record);
    }

    @Transactional
    protected void backfillPublicLibraryRecords() {
        List<BooksMetadata> books = productRepository.findAll();
        for (BooksMetadata book : books) {
            if (!isPublicEligible(book)) {
                continue;
            }

            PublicLibrary record = publicLibraryRepository.findByProductProductId(book.getProductId())
                    .orElse(PublicLibrary.builder().product(book).build());

            if (record.getVisibility() == null || !"public".equalsIgnoreCase(record.getVisibility())) {
                record.setVisibility("public");
            }
            if (record.getEditable() == null) {
                record.setEditable(true);
            }
            if (record.getIsFeatured() == null) {
                record.setIsFeatured(false);
            }

            publicLibraryRepository.save(record);
        }
    }

    private boolean isPublicEligible(BooksMetadata book) {
        if (book == null) {
            return false;
        }
        String status = book.getStatus() == null ? "" : book.getStatus().trim().toLowerCase();
        boolean statusOk = "active".equals(status) || "published".equals(status);
        String fileKey = book.getFileKey();
        boolean hasFile = fileKey != null && !fileKey.trim().isEmpty();
        return statusOk && hasFile;
    }

    private String normalizeVisibility(String visibility) {
        if (visibility == null || visibility.isBlank()) {
            return "public";
        }
        String normalized = visibility.trim().toLowerCase();
        return normalized;
    }

    private PublicLibraryResponse toResponse(PublicLibrary record) {
        String pdfUrl = publicLibraryStorageService.getBookPdfUrl(record.getProduct().getTitle());
        if (pdfUrl == null) {
            pdfUrl = bookStorageService.resolvePublicUrl(record.getProduct().getFileKey());
        }

        BooksMetadata product = record.getProduct();
        String categoryName = null;
        if (product.getCategory() != null) {
            categoryName = product.getCategory().getName();
        }

        return PublicLibraryResponse.builder()
                .publicLibraryId(record.getPublicLibraryId())
                .productId(product.getProductId())
                .title(product.getTitle())
                .author(product.getAuthor())
                .fileUrl(pdfUrl)
                .visibility(record.getVisibility())
                .isFeatured(record.getIsFeatured())
                .notes(record.getNotes())
                .editable(record.getEditable())
                .categoryName(categoryName)
                .price(product.getPrice())
                .build();
    }
}
