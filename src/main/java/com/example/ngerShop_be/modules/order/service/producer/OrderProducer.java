package com.example.ngerShop_be.modules.order.service.producer;

import com.example.ngerShop_be.modules.order.dto.OrderConfirmation;
import com.example.ngerShop_be.modules.order.dto.OrderItemRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log4j2
public class OrderProducer {

    // Hàm này dùng để gửi thông tin xác nhận đơn hàng (sau này dùng gửi Email)
    public void sendOrderConfirmation(OrderConfirmation confirmation) {
        log.info("--- [PRODUCER] Đang gửi thông báo đơn hàng: {} ---", confirmation.reference());
        // Sau này bạn sẽ thêm KafkaTemplate.send(...) ở đây
    }

    // Hàm này dùng để gửi lệnh cập nhật kho sang module Product
    public void sendUpdateStock(List<OrderItemRequest> items) {
        log.info("--- [PRODUCER] Đang gửi yêu cầu cập nhật kho cho {} sản phẩm ---", items.size());
        // Sau này bạn sẽ thêm KafkaTemplate.send(...) ở đây
    }
}
