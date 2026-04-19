package com.masukibooks.controller;

import com.masukibooks.dto.request.PublicLibraryRequest;
import com.masukibooks.dto.response.PublicLibraryResponse;
import com.masukibooks.service.PublicLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public-library")
@RequiredArgsConstructor
public class PublicLibraryController {

    private final PublicLibraryService publicLibraryService;

    @GetMapping
    public ResponseEntity<List<PublicLibraryResponse>> listPublicItems() {
        return ResponseEntity.ok(publicLibraryService.listPublicItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PublicLibraryResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(publicLibraryService.getById(id));
    }

    @PostMapping(consumes = { "application/json" })
    public ResponseEntity<PublicLibraryResponse> createOrUpdateJson(
            @RequestBody PublicLibraryRequest request) {
        PublicLibraryResponse response = publicLibraryService.createOrUpdateWithFile(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(consumes = { "multipart/form-data" })
    public ResponseEntity<PublicLibraryResponse> createOrUpdateWithFile(
            @RequestPart(value = "request", required = false) PublicLibraryRequest request,
            @RequestParam(value = "requestJson", required = false) String requestJson,
            @RequestPart(value = "file", required = false) org.springframework.web.multipart.MultipartFile pdfFile) {
        
        PublicLibraryRequest finalRequest = request;
        
        // Fallback for tools like Thunder Client that send JSON as a plain text form field
        if (finalRequest == null && requestJson != null) {
            try {
                finalRequest = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(requestJson, PublicLibraryRequest.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid JSON format in requestJson field");
            }
        }
        
        if (finalRequest == null) {
            throw new IllegalArgumentException("Request metadata is required");
        }

        PublicLibraryResponse response = publicLibraryService.createOrUpdateWithFile(finalRequest, pdfFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        publicLibraryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
