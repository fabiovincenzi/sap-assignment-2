# Acceptance Tests (Gherkin)

```gherkin
Feature: Customer registration

  As a visitor
  I want to register an account
  So that I can use the delivery service

  Scenario: Successful registration
    Given no account exists with username "mario_rossi"
    When I register with username "mario_rossi" and password "Secret#123"
    Then my account is created

  Scenario: Duplicate username
    Given an account exists with username "mario_rossi"
    When I try to register with username "mario_rossi"
    Then I receive an error indicating the username is taken
```

```gherkin
Feature: Place delivery order

  As a customer
  I want to place a delivery order specifying pickup, delivery location and package weight
  So that my package is delivered by drone

  Scenario: Successful immediate order
    Given I am a logged-in customer
    And at least one drone is available near the pickup area
    When I place a delivery order with:
      | pickup   | Via Roma 1, Cesena     |
      | delivery | Via Garibaldi 5, Forli |
      | weight   | 2.5 kg                 |
      | when     | immediate              |
    Then a delivery order is created in CONFIRMED status
    And I receive a confirmation with estimated delivery time

  Scenario: Weight exceeds limit
    Given I am a logged-in customer
    When I place an order with a package weighing 50 kg
    Then the order is rejected

  Scenario: No drones available
    Given I am a logged-in customer
    And no drones are available near the pickup area
    When I place a delivery order
    Then I am informed that no drones are available
```

```gherkin
Feature: Delivery tracking

  As a customer
  I want to track my delivery in real time
  So that I know where my package is

  Scenario: Track active delivery
    Given I have a delivery in IN_TRANSIT status
    When I open the tracking view
    Then I see the drone position and ETA
    And the position updates automatically

  Scenario: Track completed delivery
    Given my delivery has been completed
    When I open the tracking view
    Then I see status DELIVERED with completion time
```

```gherkin
Feature: Cancel order

  As a customer
  I want to cancel a pending order
  So that I can change my mind before dispatch

  Scenario: Cancel before dispatch
    Given I have an order in CONFIRMED status
    When I cancel the order
    Then the order status is CANCELLED
    And the assigned drone is released

  Scenario: Cannot cancel in transit
    Given I have a delivery in IN_TRANSIT status
    When I try to cancel the order
    Then I receive an error
```

```gherkin
Feature: Drone assignment

  As the system
  I want to automatically assign the nearest available drone to a confirmed order
  So that deliveries are dispatched efficiently

  Scenario: Automatic assignment
    Given an order has been confirmed
    And a drone is available with sufficient capacity
    When the system processes the order
    Then the nearest drone is assigned
    And delivery status is DRONE_ASSIGNED

  Scenario: No suitable drone
    Given an order has been confirmed
    And no suitable drone is available
    When the system attempts to assign a drone
    Then the delivery remains SCHEDULED
```

```gherkin
Feature: Delivery completion

  As the system
  I want to complete a delivery when the drone arrives
  So that the order lifecycle is finalized

  Scenario: Successful delivery
    Given a drone is in transit and reaches the delivery location
    When the drone signals arrival
    Then delivery status is DELIVERED
    And order status is COMPLETED
    And drone status is AVAILABLE
    And the customer is notified
```
