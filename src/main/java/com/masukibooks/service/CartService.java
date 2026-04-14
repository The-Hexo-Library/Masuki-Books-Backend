package com.masukibooks.service;

import com.masukibooks.dto.request.CartItemRequest;
import com.masukibooks.dto.response.CartResponse;
import com.masukibooks.entity.BooksMetadata;
import com.masukibooks.entity.Cart;
import com.masukibooks.entity.CartItem;
import com.masukibooks.entity.User;
import com.masukibooks.exception.BusinessException;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.BooksMetadataRepository;
import com.masukibooks.repository.CartItemRepository;
import com.masukibooks.repository.CartRepository;
import com.masukibooks.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final BooksMetadataRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartResponse getOrCreateCartForUser(UUID userId) {
        Cart cart = cartRepository.findByUserUserIdAndStatus(userId, "active")
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Cart c = Cart.builder().user(user).status("active").build();
                    return cartRepository.save(c);
                });
        return toResponse(cart);
    }

    @Transactional
    public CartResponse getOrCreateGuestCart(String guestToken) {
        Cart cart = cartRepository.findByGuestTokenAndStatus(guestToken, "active")
                .orElseGet(() -> {
                    Cart c = Cart.builder().guestToken(guestToken).status("active").build();
                    return cartRepository.save(c);
                });
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(UUID cartId, CartItemRequest request) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        BooksMetadata product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        boolean isDigital = "digital".equals(product.getContentType()) || "both".equals(product.getContentType());

        CartItem item = cartItemRepository
                .findByCartCartIdAndProductProductId(cartId, request.getProductId())
                .orElse(CartItem.builder().cart(cart).product(product).quantity(0).build());

        // Digital products: quantity locked to 1
        int newQty;
        if (isDigital) {
            if (item.getCartItemId() != null) {
                return toResponse(cartRepository.findById(cartId).get()); // already in cart
            }
            newQty = 1;
        } else {
            newQty = item.getQuantity() + request.getQuantity();
        }

        item.setQuantity(newQty);
        item.setUnitPrice(product.getPrice());
        cartItemRepository.save(item);
        return toResponse(cartRepository.findById(cartId).get());
    }

    @Transactional
    public CartResponse updateItem(UUID cartId, UUID cartItemId, int quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!item.getCart().getCartId().equals(cartId)) {
            throw new BusinessException("Item does not belong to this cart");
        }
        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            boolean isDigital = "digital".equals(item.getProduct().getContentType())
                    || "both".equals(item.getProduct().getContentType());
            if (isDigital) {
                // Digital products are always quantity 1
                item.setQuantity(1);
            } else {
                item.setQuantity(quantity);
            }
            cartItemRepository.save(item);
        }
        return toResponse(cartRepository.findById(cartId).get());
    }

    @Transactional
    public CartResponse removeItem(UUID cartId, UUID cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        if (!item.getCart().getCartId().equals(cartId)) {
            throw new BusinessException("Item does not belong to this cart");
        }
        cartItemRepository.delete(item);
        return toResponse(cartRepository.findById(cartId).get());
    }

    @Transactional
    public void clearCart(UUID cartId) {
        cartItemRepository.deleteByCartCartId(cartId);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItem> items = cart.getItems() != null ? cart.getItems() : List.of();
        List<CartResponse.CartItemResponse> itemResponses = items.stream().map(i -> {
            boolean inStock = true;
            BigDecimal lineTotal = i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
            return (CartResponse.CartItemResponse) CartResponse.CartItemResponse.builder()
                    .cartItemId(i.getCartItemId())
                    .productId(i.getProduct().getProductId())
                    .productTitle(i.getProduct().getTitle())
                    .quantity(i.getQuantity())
                    .unitPrice(i.getUnitPrice())
                    .lineTotal(lineTotal)
                    .inStock(inStock)
                    .build();
        }).collect(Collectors.toList());

        BigDecimal subtotal = itemResponses.stream()
                .map(CartResponse.CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .items(itemResponses)
                .subtotal(subtotal)
                .totalItems(itemResponses.stream().mapToInt(CartResponse.CartItemResponse::getQuantity).sum())
                .build();
    }
}
