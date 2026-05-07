package uk.gov.ons.census.jobprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ons.census.jobprocessor.testutils.JunkDataHelper.getJobRowData;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ons.census.common.model.entity.CollectionExercise;
import uk.gov.ons.census.common.model.entity.Job;
import uk.gov.ons.census.common.model.entity.JobRow;
import uk.gov.ons.census.common.model.entity.JobRowStatus;
import uk.gov.ons.census.common.model.entity.JobType;
import uk.gov.ons.census.common.model.entity.Survey;
import uk.gov.ons.census.common.validation.ColumnValidator;
import uk.gov.ons.census.common.validation.MandatoryRule;
import uk.gov.ons.census.common.validation.Rule;
import uk.gov.ons.census.jobprocessor.jobtype.processors.JobTypeProcessor;
import uk.gov.ons.census.jobprocessor.jobtype.processors.SampleLoadTypeProcessor;
import uk.gov.ons.census.jobprocessor.repository.JobRepository;
import uk.gov.ons.census.jobprocessor.repository.JobRowRepository;
import uk.gov.ons.census.jobprocessor.utility.JobTypeHelper;

@ExtendWith(MockitoExtension.class)
class RowChunkValidatorTest {
  @Mock JobRowRepository jobRowRepository;
  @Mock JobRepository jobRepository;
  @Mock JobTypeHelper jobTypeHelper;

  @InjectMocks RowChunkValidator underTest;

  @Test
  void processChunk() {
    // Given
    CollectionExercise collectionExercise = new CollectionExercise();
    Job job = new Job();
    job.setJobType(JobType.SAMPLE);
    job.setCollectionExercise(collectionExercise);

    ColumnValidator[] columnValidators =
        new ColumnValidator[] {
          new ColumnValidator("test column", new Rule[] {new MandatoryRule()})
        };

    Survey survey = new Survey();
    survey.setSampleValidationRules(columnValidators);
    collectionExercise.setSurvey(survey);

    JobTypeProcessor jobTypeProcessor = new SampleLoadTypeProcessor("", "");

    JobRow jobRow = new JobRow();
    Map<String, String> jobRowData = getJobRowData();
    jobRow.setRowData(jobRowData);
    List<JobRow> jobRows = List.of(jobRow);

    when(jobTypeHelper.getJobTypeProcessor(job.getJobType(), collectionExercise))
        .thenReturn(jobTypeProcessor);

    when(jobRowRepository.findTop500ByJobAndJobRowStatus(job, JobRowStatus.STAGED))
        .thenReturn(jobRows);

    // When
    underTest.processChunk(job);

    // Then
    ArgumentCaptor<List<JobRow>> jobRowArgumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(jobRowRepository).saveAll(jobRowArgumentCaptor.capture());
    List<JobRow> actualJobRows = jobRowArgumentCaptor.getValue();

    assertThat(actualJobRows.size()).isEqualTo(1);
    assertThat(actualJobRows.get(0).getJobRowStatus()).isEqualTo(JobRowStatus.VALIDATED_OK);
    assertThat(actualJobRows.get(0).getValidationErrorDescriptions()).isEmpty();

    ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
    verify(jobRepository).saveAndFlush(jobArgumentCaptor.capture());
    Job actualJob = jobArgumentCaptor.getValue();

    assertThat(actualJob.getValidatingRowNumber()).isEqualTo(1);
    assertThat(actualJob.getErrorRowCount()).isEqualTo(0);
  }

  @Test
  void processChunkFailsValidation() {
    // Given
    CollectionExercise collectionExercise = new CollectionExercise();
    Job job = new Job();
    job.setJobType(JobType.SAMPLE);
    job.setCollectionExercise(collectionExercise);

    ColumnValidator[] columnValidators =
        new ColumnValidator[] {
          new ColumnValidator("test column", new Rule[] {new MandatoryRule()})
        };

    Survey survey = new Survey();
    survey.setSampleValidationRules(columnValidators);
    collectionExercise.setSurvey(survey);

    JobTypeProcessor jobTypeProcessor = new SampleLoadTypeProcessor("", "");

    JobRow jobRow = new JobRow();
    Map<String, String> jobRowData = getJobRowData();
    jobRowData.put("UPRN", "");
    jobRow.setRowData(jobRowData);
    List<JobRow> jobRows = List.of(jobRow);

    when(jobTypeHelper.getJobTypeProcessor(job.getJobType(), collectionExercise))
        .thenReturn(jobTypeProcessor);

    when(jobRowRepository.findTop500ByJobAndJobRowStatus(job, JobRowStatus.STAGED))
        .thenReturn(jobRows);

    // When
    underTest.processChunk(job);

    // Then
    ArgumentCaptor<List<JobRow>> jobRowArgumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(jobRowRepository).saveAll(jobRowArgumentCaptor.capture());
    List<JobRow> actualJobRows = jobRowArgumentCaptor.getValue();

    assertThat(actualJobRows.size()).isEqualTo(1);
    assertThat(actualJobRows.get(0).getJobRowStatus()).isEqualTo(JobRowStatus.VALIDATED_ERROR);
    assertThat(actualJobRows.get(0).getValidationErrorDescriptions())
        .isEqualTo("Column 'UPRN' value '' validation error: Mandatory value missing");

    ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
    verify(jobRepository).saveAndFlush(jobArgumentCaptor.capture());
    Job actualJob = jobArgumentCaptor.getValue();

    assertThat(actualJob.getValidatingRowNumber()).isEqualTo(1);
    assertThat(actualJob.getErrorRowCount()).isEqualTo(1);
  }

  @Test
  void processChunkFailsValidationOnUnexpectedErrorDuringValidation() {
    // Given
    CollectionExercise collectionExercise = new CollectionExercise();
    Job job = new Job();
    job.setJobType(JobType.SAMPLE);
    job.setCollectionExercise(collectionExercise);

    ColumnValidator columnValidatorMock = Mockito.mock(ColumnValidator.class);

    Survey survey = new Survey();
    survey.setSampleValidationRules(new ColumnValidator[] {columnValidatorMock});
    collectionExercise.setSurvey(survey);

    JobTypeProcessor jobTypeProcessor = new SampleLoadTypeProcessor("", "");
    jobTypeProcessor.setColumnValidators(new ColumnValidator[] {columnValidatorMock});

    Map<String, String> jobRowData = Map.of("test column", "test@example.com");

    JobRow jobRow = new JobRow();
    jobRow.setRowData(jobRowData);
    List<JobRow> jobRows = List.of(jobRow);

    when(jobTypeHelper.getJobTypeProcessor(job.getJobType(), collectionExercise))
        .thenReturn(jobTypeProcessor);

    when(jobRowRepository.findTop500ByJobAndJobRowStatus(job, JobRowStatus.STAGED))
        .thenReturn(jobRows);

    when(columnValidatorMock.validateRow(jobRowData))
        .thenThrow(new NullPointerException("An unexpected error occured"));

    // When
    underTest.processChunk(job);

    // Then
    ArgumentCaptor<List<JobRow>> jobRowArgumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(jobRowRepository).saveAll(jobRowArgumentCaptor.capture());
    List<JobRow> actualJobRows = jobRowArgumentCaptor.getValue();

    assertThat(actualJobRows.size()).isEqualTo(1);
    assertThat(actualJobRows.get(0).getJobRowStatus()).isEqualTo(JobRowStatus.VALIDATED_ERROR);
    assertThat(actualJobRows.get(0).getValidationErrorDescriptions())
        .isEqualTo(
            "Unexpected technical failure, please report this to the dev team: An unexpected error occured");
    assertThrows(NullPointerException.class, () -> columnValidatorMock.validateRow(jobRowData));

    ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
    verify(jobRepository).saveAndFlush(jobArgumentCaptor.capture());
    Job actualJob = jobArgumentCaptor.getValue();

    assertThat(actualJob.getValidatingRowNumber()).isEqualTo(1);
    assertThat(actualJob.getErrorRowCount()).isEqualTo(1);
  }
}
