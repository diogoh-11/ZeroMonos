Feature: Bulky waste pickup booking
  As a resident
  I want to schedule a pickup for a large bulky waste item
  So that the municipality can collect and dispose of it responsibly

  Background:
    Given I open the application home page

  Scenario: Navigate to the booking form
    When I click the "Criar Reserva" button
    Then the booking form page should be displayed

  Scenario Outline: Submit a valid booking
    Given I am on the booking form page
    When I search and select municipality "<Municipality>"
    And I set the reservation date to "<Date>"
    And I select the time slot "<TimeSlot>"
    And I enter the description:
      """
      <Description>
      """
    And I submit the booking form
    Then I should see a confirmation message containing "<ConfirmationText>"

    Examples:
      | Municipality | Date       | TimeSlot | Description                                   | ConfirmationText |
      | Mangualde    | 11-11-2025 | MORNING    | Frigorífico velho 170kg 1.90m x 0.30m         | reserva criada    |