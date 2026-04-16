package uk.gov.ons.census.jobprocessor.transformer;

import uk.gov.ons.census.common.model.entity.Job;
import uk.gov.ons.census.common.model.entity.JobRow;
import uk.gov.ons.census.common.validation.ColumnValidator;

public interface Transformer {
  Object transformRow(Job job, JobRow jobRow, ColumnValidator[] columnValidators, String topic);
}
