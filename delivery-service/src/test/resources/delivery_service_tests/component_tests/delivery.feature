Feature: Delivery management
  As the system
  I want to schedule and manage deliveries
  So that packages are delivered to customers

  Scenario: Schedule a new delivery
    When a delivery is scheduled for order "order-1" from (44.0, 12.0) to (44.1, 12.1) weighing 2.5 kg
    Then the delivery is created with status "DRONE_ASSIGNED"

  Scenario: Complete a delivery
    Given a delivery in transit for order "order-2"
    When the delivery is completed
    Then the delivery status is "DELIVERED"
