package com.masukibooks.service;

import com.masukibooks.entity.Order;
import com.masukibooks.entity.Shipment;
import com.masukibooks.exception.ResourceNotFoundException;
import com.masukibooks.repository.OrderRepository;
import com.masukibooks.repository.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public Shipment createShipment(UUID orderId, String carrier, String trackingNumber,
                                   String serviceLevel, UUID createdByAdminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        Shipment shipment = Shipment.builder()
                .order(order)
                .carrier(carrier)
                .trackingNumber(trackingNumber)
                .status("shipped")
                .build();
        order.setStatus("shipped");
        orderRepository.save(order);
        return shipmentRepository.save(shipment);
    }

    @Transactional
    public Shipment updateStatus(UUID shipmentId, String status) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
        shipment.setStatus(status);
        if ("delivered".equals(status)) {
            shipment.setDeliveredAt(LocalDateTime.now());
            Order order = shipment.getOrder();
            order.setStatus("delivered");
            orderRepository.save(order);
        }
        return shipmentRepository.save(shipment);
    }

    public Shipment getByOrder(UUID orderId) {
        return shipmentRepository.findTopByOrderOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found"));
    }
}
