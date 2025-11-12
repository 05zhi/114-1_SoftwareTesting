package org.example.service;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.exception.BusinessException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeliveryService {

    private static final Logger logger = LogManager.getLogger(DeliveryService.class);
    private static final double MAX_DISTANCE = 10.0;  // 最大外送距離

    // 檢查是否在營業時間內
    private void checkOpenClose(Order order) throws BusinessException {
        int hour = order.getOrderHour();
        if(hour < order.getRestaurantOpenHour() || hour >= order.getRestaurantCloseHour()) {
            throw new BusinessException(
                    "目前非 " + order.getRestaurantName() + " 營業時間 (營業時間: " +
                            order.getRestaurantOpenHour() + "-" + order.getRestaurantCloseHour() +
                            "，訂單時間: " + hour + ")。"
            );
        }
        logger.info("餐廳 {} 營業中，可接單。", order.getRestaurantName());
    }


    // 檢查餐廳和顧客距離是否合理
    private void checkDeliveryDistance(Order order) throws BusinessException {
        double distance = Math.sqrt(Math.pow(order.getRestaurantX() - order.getCustomerX(), 2)
                + Math.pow(order.getRestaurantY() - order.getCustomerY(), 2));
        if (distance > MAX_DISTANCE) {
            throw new BusinessException(
                    // 距離顯示到小數點後一位就好
                    String.format("餐廳 %s 與顧客 %s 距離 %.1f km 超出合理外送距離。",
                            order.getRestaurantName(), order.getCustomerName(), distance)
            );
        }
        String distanceStr = String.format("%.1f", distance);  // 距離顯示到小數點後一位就好
        logger.info("餐廳 {} 與顧客 {} 距離 {} km，在合理範圍內。",
                order.getRestaurantName(), order.getCustomerName(), distanceStr);
    }

    // 餐廳接單
    public void acceptOrder(Order order) throws BusinessException {
        logger.info("餐廳 {} 正在接受訂單 {}。", order.getRestaurantName(), order.getOrderId());

        checkOpenClose(order);         // 檢查營業時間
        checkDeliveryDistance(order);  // 檢查距離

        if(order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("只能接 PENDING 狀態的訂單。");
        }

        order.setStatus(OrderStatus.ACCEPTED);
        // 訂單狀態有更新就在日誌中紀錄
        logger.info("訂單 {} 狀態變更為 {}。", order.getOrderId(), order.getStatus());
    }

    // 外送員取餐
    public void pickupOrder(Order order) {
        try {
            // 訂單要先被餐廳接受才能被外送員取餐
            if(order.getStatus() != OrderStatus.ACCEPTED) {
                throw new BusinessException("訂單尚未被餐廳接單，無法取餐。");
            }

            order.setStatus(OrderStatus.PICKED_UP);
            // 訂單狀態有更新就在日誌中紀錄
            logger.info("{} 狀態變更為 {}。", order.getOrderId(), order.getStatus());

        } catch (BusinessException e) {
            logger.warn("{}", e.getMessage());
        }
    }

    // 訂單送達
    public void deliverOrder(Order order) {
        try {
            // 訂單要先被外送員取餐才能送餐
            if(order.getStatus() != OrderStatus.PICKED_UP) {
                throw new BusinessException("訂單尚未被取餐，無法送達。");
            }

            order.setStatus(OrderStatus.DELIVERED);
            // 訂單狀態有更新就在日誌中紀錄
            logger.info("訂單 {} 狀態變更為 {}。", order.getOrderId(), order.getStatus());

        } catch (BusinessException e) {
            logger.warn("{}", e.getMessage());
        }
    }
}
