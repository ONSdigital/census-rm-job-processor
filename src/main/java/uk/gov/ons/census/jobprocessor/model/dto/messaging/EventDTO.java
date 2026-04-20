package uk.gov.ons.census.jobprocessor.model.dto.messaging;

import lombok.Data;

@Data
public class EventDTO {
  private EventHeaderDTO header;
  private PayloadDTO payload;
}
