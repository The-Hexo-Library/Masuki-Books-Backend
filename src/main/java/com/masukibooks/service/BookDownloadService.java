package com.masukibooks.service;

import com.masukibooks.entity.BooksMetadata;
import com.masukibooks.entity.UserLibrary;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.BooksMetadataRepository;
import com.masukibooks.repository.UserLibraryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookDownloadService {

    private final UserLibraryRepository userLibraryRepository;
    private final BooksMetadataRepository booksMetadataRepository;
    private final BookStorageService bookStorageService;

    /**
     * Check if user has access to download a book and return the file key.
     * Access is granted only when user has an active purchased library record.
     */
    public String getDownloadFileKey(UUID userId, UUID bookId) {
        // Fetch the book
        BooksMetadata book = booksMetadataRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

        // Check user's access to this book first (purchased users can download)
        Optional<UserLibrary> userLibraryEntry = userLibraryRepository.findByUserUserIdAndProductProductId(userId, bookId);

        if (userLibraryEntry.isPresent()) {
            UserLibrary entry = userLibraryEntry.get();

            // Check if the library entry is active
            if (!"active".equalsIgnoreCase(entry.getStatus())) {
                throw new BusinessException("Your access to this book has expired or been revoked");
            }

            // Check if borrowed book has expired
            if ("borrowed".equalsIgnoreCase(entry.getAccessType()) && entry.getExpiresAt() != null) {
                if (entry.getExpiresAt().isBefore(LocalDateTime.now())) {
                    throw new BusinessException("Your access to this book has expired");
                }
            }

            if (!"purchased".equalsIgnoreCase(entry.getAccessType())) {
                throw new BusinessException("Purchase this book to view or download it");
            }

            // Check if book has a file
            if (book.getFileKey() == null || book.getFileKey().isBlank()) {
                throw new BusinessException("No file available for this book");
            }

            // User has purchased access - return the file key
            log.info("User {} granted purchased download access to book {}", userId, bookId);
            return book.getFileKey();
        }

        throw new BusinessException("Purchase this book to view or download it");
    }

    /**
     * Get full download details for a book (filename, size, etc.)
     */
    public BookDownloadDetails getDownloadDetails(UUID userId, UUID bookId) {
        String fileKey = getDownloadFileKey(userId, bookId);
        BooksMetadata book = booksMetadataRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

        String filename = generateDownloadFilename(book);

        return BookDownloadDetails.builder()
                .fileKey(fileKey)
                .filename(filename)
                .fileFormat(book.getFileFormat())
                .fileSizeBytes(book.getFileSizeBytes())
                .title(book.getTitle())
                .author(book.getAuthor())
                .build();
    }

    /**
     * Generate a meaningful filename for download
     */
    private String generateDownloadFilename(BooksMetadata book) {
        String extension = book.getFileFormat() != null ? "." + book.getFileFormat().toLowerCase() : ".pdf";
        String filename = book.getTitle().replaceAll("[^a-zA-Z0-9_\\-\\s]", "").replaceAll("\\s+", "_");
        return filename + extension;
    }

    @lombok.Data
    @lombok.Builder
    public static class BookDownloadDetails {
        private String fileKey;
        private String filename;
        private String fileFormat;
        private Long fileSizeBytes;
        private String title;
        private String author;
    }
}
