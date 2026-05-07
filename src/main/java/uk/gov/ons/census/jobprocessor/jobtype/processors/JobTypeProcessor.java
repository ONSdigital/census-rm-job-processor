package uk.gov.ons.census.jobprocessor.jobtype.processors;

import lombok.Data;
import uk.gov.ons.census.common.model.entity.JobRow;
import uk.gov.ons.census.common.model.entity.JobType;
import uk.gov.ons.census.common.model.entity.UserGroupAuthorisedActivityType;
import uk.gov.ons.census.common.validation.ColumnValidator;
import uk.gov.ons.census.jobprocessor.exceptions.ValidatorFieldNotFoundException;
import uk.gov.ons.census.jobprocessor.transformer.Transformer;

@Data
public abstract class JobTypeProcessor {
  private JobType jobType;
  private Transformer transformer;
  protected ColumnValidator[] columnValidators;
  private String topic;
  private UserGroupAuthorisedActivityType fileLoadPermission;
  private UserGroupAuthorisedActivityType fileViewProgressPermission;

  public abstract ColumnValidator[] getColumnValidators(JobRow jobRow)
      throws ValidatorFieldNotFoundException;
}
