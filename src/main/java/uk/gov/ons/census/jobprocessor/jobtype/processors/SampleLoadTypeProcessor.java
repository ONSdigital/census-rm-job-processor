package uk.gov.ons.census.jobprocessor.jobtype.processors;

import static com.google.cloud.spring.pubsub.support.PubSubTopicUtils.toProjectTopicName;

import uk.gov.ons.census.common.model.entity.CollectionExercise;
import uk.gov.ons.census.common.model.entity.JobRow;
import uk.gov.ons.census.common.model.entity.JobType;
import uk.gov.ons.census.common.model.entity.UserGroupAuthorisedActivityType;
import uk.gov.ons.census.common.validation.ColumnValidator;
import uk.gov.ons.census.jobprocessor.exceptions.ValidatorFieldNotFoundException;
import uk.gov.ons.census.jobprocessor.transformer.NewCaseTransformer;
import uk.gov.ons.census.jobprocessor.transformer.Transformer;

public class SampleLoadTypeProcessor extends JobTypeProcessor {
  private static final Transformer SAMPLE_LOAD_TRANSFORMER = new NewCaseTransformer();

  public SampleLoadTypeProcessor(
      String topic, String pubsubProject, CollectionExercise collectionExercise) {
    setJobType(JobType.SAMPLE);
    setTransformer(SAMPLE_LOAD_TRANSFORMER);
    setColumnValidators(collectionExercise.getSurvey().getSampleValidationRules());
    setTopic(toProjectTopicName(topic, pubsubProject).toString());
    setFileLoadPermission(UserGroupAuthorisedActivityType.LOAD_SAMPLE);
    setFileViewProgressPermission(UserGroupAuthorisedActivityType.VIEW_SAMPLE_LOAD_PROGRESS);
  }

  @Override
  public ColumnValidator[] getColumnValidators(JobRow jobRow)
      throws ValidatorFieldNotFoundException {
    return columnValidators.clone();
  }
}
