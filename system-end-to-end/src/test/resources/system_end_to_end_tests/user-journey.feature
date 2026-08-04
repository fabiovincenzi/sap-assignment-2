Feature: Shipping a package
  As a customer
  I want to place an order and follow it
  So that I know a drone is bringing my package

  Scenario: A confirmed order gets a delivery with a drone assigned
    Given the shipping system is running
    And a drone able to carry 5.0 kg is available
    When I register as "mario" and place an order weighing 2.5 kg
    Then the order is created with status "PENDING"
    When I confirm the order
    Then the tracking of my order reports a delivery
    And the delivery has a drone assigned
