package org.example.model;

public class Order {
    private String orderId;
    private String customerName;
    private String restaurantName;
    private OrderStatus status;

    // 用四位數字表示時間，例如晚上九點半為 2100
    private int restaurantOpenHour;
    private int restaurantCloseHour;
    private int orderHour;

    // 位置用座標表示
    private int restaurantX;
    private int restaurantY;
    private int customerX;
    private int customerY;

    public Order(String orderId, String customerName, String restaurantName,
                 int openHour, int closeHour, int orderHour,
                 int restaurantX, int restaurantY, int customerX, int customerY) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.restaurantName = restaurantName;
        this.status = OrderStatus.PENDING;

        this.restaurantOpenHour = openHour;
        this.restaurantCloseHour = closeHour;
        this.orderHour = orderHour;

        this.restaurantX = restaurantX;
        this.restaurantY = restaurantY;
        this.customerX = customerX;
        this.customerY = customerY;
    }

    public int getRestaurantOpenHour() { return restaurantOpenHour; }
    public int getRestaurantCloseHour() { return restaurantCloseHour; }
    public int getOrderHour() { return orderHour; }

    public int getRestaurantX() { return restaurantX; }
    public int getRestaurantY() { return restaurantY; }
    public int getCustomerX() { return customerX; }
    public int getCustomerY() { return customerY; }

    public String getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public String getRestaurantName() { return restaurantName; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "Order{" +
                "orderId='" + orderId + '\'' +
                ", customerName='" + customerName + '\'' +
                ", restaurantName='" + restaurantName + '\'' +
                ", status=" + status +
                '}';
    }
}
