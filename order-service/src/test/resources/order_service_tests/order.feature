Feature: Order management
  As a customer
  I want to place and manage delivery orders
  So that my packages are delivered by drone

  Scenario: Successful order creation and confirmation
    Given a registered customer "mario"
    When "mario" creates an order with pickup "Via Roma 1" and delivery "Via Garibaldi 5" weighing 2.5 kg
    Then the order is created in PENDING status
    When the order is confirmed
    Then the order status is "CONFIRMED"

  Scenario: Cancel a pending order
    Given a registered customer "luigi"
    When "luigi" creates an order with pickup "Via Dante 3" and delivery "Via Mazzini 7" weighing 1.0 kg
    And the order is cancelled
    Then the order status is "CANCELLED"
