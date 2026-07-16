Feature: Drone management
  As the system
  I want to manage the drone fleet
  So that drones can be assigned to deliveries

  Scenario: Register a new drone
    When I register a drone at location (44.0, 12.0) with capacity 5.0 kg
    Then the drone is registered with status "AVAILABLE"

  Scenario: Find available drone
    Given a registered drone at (44.0, 12.0) with capacity 5.0 kg
    When I request an available drone near (44.0, 12.0) for 3.0 kg
    Then an available drone is found
