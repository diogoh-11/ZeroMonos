Feature: Changing the state of a booking
  As a staff member
  I want to update a state of a booking to ASSIGNED
  So that the user can see that the workflow is already in course

  Background:
    Given I open the application home page

  Scenario: Navigate to the staff page
    When I click the "Aceder" button
    Then the staff page should be displayed

  Scenario: Search for a specific municipality and find a booking
    Given I am on the staff page
    When I select municipality "Mangualde" from the municipality filter
    And I click the filter button
    Then the bookings table should contain a row with token and status "RECEIVED"

  Scenario: Change the status of a booking to ASSIGNED
    Given I am on the staff page
    And the bookings table contains a row with token
    When I click the "Assign" action for token
    Then the row for token should show status "ASSIGNED"
    And I should see a message containing "Status atualizado"

  

