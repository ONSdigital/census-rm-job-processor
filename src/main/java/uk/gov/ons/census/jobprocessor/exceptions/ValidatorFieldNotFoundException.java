package uk.gov.ons.census.jobprocessor.exceptions;

public class ValidatorFieldNotFoundException extends Exception {
  public ValidatorFieldNotFoundException(String errorMessage) {
    super(errorMessage);
  }
}
