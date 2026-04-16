package uk.gov.ons.census.jobprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("uk.gov.ons.census.common.model.entity")
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class);
  }
}
