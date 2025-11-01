Feature: Search for an existing booking
  As a resident
  I want to check the status and details of my booking
  So that I can verify whether it is active or canceled

  Background:
    Given I open the application home page

  Scenario: Navigate to the booking search page
    When I click the "Consultar"
    Then the booking search page should be displayed

  Scenario: Search for a booking by token
    Given I am on the booking search page
    And I enter the token:
    When I click the search button
    Then I should see the booking details containing "Frigorífico velho 170kg 1.90m x 0.30m"
