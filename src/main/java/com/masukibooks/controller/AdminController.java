package com.masukibooks.controller;

import com.masukibooks.dto.request.DiscountCodeRequest;
import com.masukibooks.dto.request.ProductRequest;
import com.masukibooks.dto.response.ApiResponse;
import com.masukibooks.dto.response.DashboardStatsResponse;
import com.masukibooks.dto.response.OrderResponse;
import com.masukibooks.dto.response.ProductResponse;
import com.masukibooks.dto.response.SupportTicketResponse;
import com.masukibooks.entity.*;
import com.masukibooks.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('admin','superadmin')")
public class AdminController {

    private final ProductService productService;
    private final OrderService orderService;
    private final ReviewService reviewService;
    private final InventoryService inventoryService;
    private final DiscountService discountService;
    private final RefundService refundService;
    private final ShipmentService shipmentService;
    private final DigitalBookProcessingService digitalBookProcessingService;
    private final DashboardService dashboardService;
    private final SupportTicketService supportTicketService;
    private final UserService userService;

    // ---- Dashboard ----

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved",
                dashboardService.getDashboardStats()));
    }

    // ---- Support Tickets ----

    @GetMapping("/support-tickets")
    public ResponseEntity<ApiResponse<Page<SupportTicketResponse>>> getAllTickets(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Support tickets retrieved",
                supportTicketService.getAllTickets(status, pageable)));
    }

    @GetMapping("/support-tickets/{ticketId}")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> getTicket(@PathVariable UUID ticketId) {
        return ResponseEntity.ok(ApiResponse.success("Ticket retrieved",
                supportTicketService.getTicket(ticketId)));
    }

    @PatchMapping("/support-tickets/{ticketId}/respond")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> respondToTicket(
            @PathVariable UUID ticketId,
            @RequestBody Map<String, String> body) {
        String response = body.get("response");
        String status = body.get("status");
        String adminIdStr = body.get("adminId");
        UUID adminId = adminIdStr != null ? UUID.fromString(adminIdStr) : null;
        return ResponseEntity.ok(ApiResponse.success("Ticket updated",
                supportTicketService.respondToTicket(ticketId, response, status, adminId)));
    }

    @PatchMapping("/support-tickets/{ticketId}/assign")
    public ResponseEntity<ApiResponse<SupportTicketResponse>> assignTicket(
            @PathVariable UUID ticketId,
            @RequestBody Map<String, String> body) {
        UUID adminId = UUID.fromString(body.get("adminId"));
        return ResponseEntity.ok(ApiResponse.success("Ticket assigned",
                supportTicketService.assignTicket(ticketId, adminId)));
    }

    // ---- User Management ----

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<User>>> listUsers(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved",
                userService.listUsers(pageable)));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<User>> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved",
                userService.getUserById(userId)));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<User>> updateUserStatus(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("User status updated",
                userService.updateUserStatus(userId, body.get("status"))));
    }

    // ---- Orders ----

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", orderService.getAllOrders(status, pageable)));
    }

    @PatchMapping("/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated",
                orderService.updateOrderStatus(orderId, body.get("status"))));
    }

    // ---- Products ----

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product created", productService.createProduct(request)));
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable UUID productId,
                                                                      @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Product updated", productService.updateProduct(productId, request)));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }

    // ---- Inventory ----

    @GetMapping("/inventory/low-stock")
    public ResponseEntity<ApiResponse<List<Inventory>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.success("Low stock items", inventoryService.getLowStockItems()));
    }

    @PostMapping("/inventory/{productId}/adjust")
    public ResponseEntity<ApiResponse<Inventory>> adjustStock(@PathVariable UUID productId,
                                                              @RequestBody Map<String, Object> body) {
        int delta = (Integer) body.get("quantityDelta");
        String reason = (String) body.getOrDefault("reason", "Manual adjustment");
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted",
                inventoryService.adjustStock(productId, delta, reason, null)));
    }

    // ---- Reviews ----

    @GetMapping("/reviews/pending")
    public ResponseEntity<ApiResponse<Page<Review>>> getPendingReviews(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Pending reviews", reviewService.getPendingReviews(pageable)));
    }

    @PatchMapping("/reviews/{reviewId}/moderate")
    public ResponseEntity<ApiResponse<Review>> moderateReview(@PathVariable UUID reviewId,
                                                              @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Review moderated",
                reviewService.moderateReview(reviewId, body.get("status"))));
    }

    // ---- Discount Codes ----

    @GetMapping("/discounts")
    public ResponseEntity<ApiResponse<Page<DiscountCode>>> listDiscounts(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Discount codes", discountService.listAll(pageable)));
    }

    @PostMapping("/discounts")
    public ResponseEntity<ApiResponse<DiscountCode>> createDiscount(@RequestBody DiscountCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Discount code created", discountService.create(request)));
    }

    @PatchMapping("/discounts/{id}/toggle")
    public ResponseEntity<ApiResponse<DiscountCode>> toggleDiscount(@PathVariable UUID id,
                                                                    @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(ApiResponse.success("Discount code updated",
                discountService.toggle(id, body.get("active"))));
    }

    // ---- Refunds ----

    @GetMapping("/refunds")
    public ResponseEntity<ApiResponse<Page<Refund>>> getRefunds(
            @RequestParam(required = false, defaultValue = "pending") String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Refunds retrieved", refundService.getRefundsByStatus(status, pageable)));
    }

    @PatchMapping("/refunds/{refundId}/process")
    public ResponseEntity<ApiResponse<Refund>> processRefund(@PathVariable UUID refundId,
                                                             @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Refund processed",
                refundService.processRefund(refundId, body.get("status"))));
    }

    // ---- Shipments ----

    @PostMapping("/shipments")
    public ResponseEntity<ApiResponse<com.masukibooks.entity.Shipment>> createShipment(@RequestBody Map<String, Object> body) {
        UUID orderId = UUID.fromString((String) body.get("orderId"));
        String carrier = (String) body.get("carrier");
        String tracking = (String) body.get("trackingNumber");
        String serviceLevel = (String) body.getOrDefault("serviceLevel", "standard");
        return ResponseEntity.ok(ApiResponse.success("Shipment created",
                shipmentService.createShipment(orderId, carrier, tracking, serviceLevel, null)));
    }

    @PatchMapping("/shipments/{shipmentId}/status")
    public ResponseEntity<ApiResponse<com.masukibooks.entity.Shipment>> updateShipmentStatus(
            @PathVariable UUID shipmentId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("Shipment status updated",
                shipmentService.updateStatus(shipmentId, body.get("status"))));
    }

    // ---- Digital Book Content ----

    @PostMapping("/books/{productId}/upload-content")
    public ResponseEntity<ApiResponse<ProductResponse>> uploadDigitalContent(
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file) {
        Product product = digitalBookProcessingService.uploadAndProcessContent(productId, file);
        ProductResponse response = productService.getProduct(product.getProductId());
        return ResponseEntity.ok(ApiResponse.success(
                "Digital content uploaded and processed (" + product.getTotalPages() + " pages)", response));
    }
}
