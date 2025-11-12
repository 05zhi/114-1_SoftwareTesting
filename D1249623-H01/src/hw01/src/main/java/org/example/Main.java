import org.example.model.Order;
import org.example.service.DeliveryService;
import org.example.exception.BusinessException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        DeliveryService service = new DeliveryService();
        // 建立訂單
        List<Order> orders = new ArrayList<>();
        // 訂單 001 : 正常訂單
        orders.add(new Order(
                "001", "Andy", "McDonalds",
                1000, 2100, 1300,
                2, 3, 5, 6));
        // 訂單 002 : 超出距離
        orders.add(new Order(
                "002", "Edward", "KFC",
                1000, 2100, 1800,
                0, 0, 15, 15));
        // 訂單 003 : 非營業時間
        orders.add(new Order(
                "003", "Walter", "Subway",
                1000, 2100, 2200,
                1, 1, 8, 5));
        // 依序處理每一筆訂單
        for (Order order : orders) {
            try {
                service.acceptOrder(order);  // 餐廳接受訂單
            } catch (BusinessException e) {
                logger.warn("Business Exception: {}", e.getMessage());
            }
            service.pickupOrder(order);   // 外送員取餐
            service.deliverOrder(order);  // 送達
        }

        logger.info("所有訂單處理完成。");
    }
}