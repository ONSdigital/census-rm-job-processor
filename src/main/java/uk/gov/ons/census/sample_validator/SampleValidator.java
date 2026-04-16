package uk.gov.ons.census.sample_validator;

public class SampleValidator {

  public static void main(String[] args) {
    if (args.length != 2) {
      throw new RuntimeException("Expect to be passed a schema file and sample file");
    }
  }
}
