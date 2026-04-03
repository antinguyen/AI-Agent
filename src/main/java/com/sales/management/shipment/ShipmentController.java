package com.sales.management.shipment;

import com.sales.management.shipment.dto.ShipmentCreateRequest;
import com.sales.management.shipment.dto.ShipmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShipmentResponse create(@Valid @RequestBody ShipmentCreateRequest request) {
        return shipmentService.create(request);
    }

    @GetMapping
    public List<ShipmentResponse> list() {
        return shipmentService.list();
    }

    @GetMapping("/{id}")
    public ShipmentResponse getById(@PathVariable("id") Long id) {
        return shipmentService.getById(id);
    }

    @GetMapping("/order/{orderId}")
    public ShipmentResponse getByOrderId(@PathVariable("orderId") Long orderId) {
        return shipmentService.getByOrderId(orderId);
    }

    @PostMapping("/{id}/ship")
    public ShipmentResponse markShipped(@PathVariable("id") Long id) {
        return shipmentService.markShipped(id);
    }

    @PostMapping("/{id}/cancel")
    public ShipmentResponse cancel(@PathVariable("id") Long id) {
        return shipmentService.cancel(id);
    }
}
