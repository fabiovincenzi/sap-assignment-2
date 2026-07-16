Feature: User registration
  As a new user
  I want to register
  So that I can place delivery orders

  Scenario: Successful registration
    Given I have not registered before
    When I register with username "mario" and password "Secret#123"
    Then I should see a confirmation that my account was created

  Scenario: Registration fails with duplicate username
    Given Someone already registered with username "mario"
    When I register with username "mario" and password "AnyPass#1"
    Then I should see an error "Username already taken: mario"
