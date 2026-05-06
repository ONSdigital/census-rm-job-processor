package uk.gov.ons.census.jobprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
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
import uk.gov.ons.census.common.validation.LengthRule;
import uk.gov.ons.census.common.validation.MandatoryRule;
import uk.gov.ons.census.common.validation.Rule;
import uk.gov.ons.census.jobprocessor.jobtype.processors.BulkUpdateSampleTypeProcessor;
import uk.gov.ons.census.jobprocessor.jobtype.processors.BulkUpdateSensitiveSampleTypeProcessor;
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
          new ColumnValidator("test column", false, new Rule[] {new MandatoryRule()})
        };

    Survey survey = new Survey();
    survey.setSampleValidationRules(columnValidators);
    collectionExercise.setSurvey(survey);

    JobTypeProcessor jobTypeProcessor = new SampleLoadTypeProcessor("", "");

    JobRow jobRow = new JobRow();
    Map<String, String> jobRowData = new HashMap<>();
    jobRowData.put("UPRN", "0000");
    jobRowData.put("ESTAB_UPRN", "0000");
    jobRowData.put("ADDRESS_TYPE", "HH");
    jobRowData.put("ESTAB_TYPE", "CARE HOME");
    jobRowData.put("ADDRESS_LEVEL", "U");
    jobRowData.put("ABP_CODE", "0000");
    jobRowData.put("ORGANISATION_NAME", "");
    jobRowData.put("ADDRESS_LINE1", "test ");
    jobRowData.put("ADDRESS_LINE2", "");
    jobRowData.put("ADDRESS_LINE3", "");
    jobRowData.put("TOWN_NAME", "Ponty");
    jobRowData.put("POSTCODE", "CFXX XXX");
    jobRowData.put("LATITUDE", "0000");
    jobRowData.put("LONGITUDE", "0000");
    jobRowData.put("OA", "0000");
    jobRowData.put("LSOA", "0000");
    jobRowData.put("MSOA", "0000");
    jobRowData.put("LAD", "0000");
    jobRowData.put("REGION", "0000");
    jobRowData.put("HTC_WILLINGNESS", "0");
    jobRowData.put("HTC_DIGITAL", "0");
    jobRowData.put("TREATMENT_CODE", "HH_LP1E");
    jobRowData.put("FIELDCOORDINATOR_ID", "0000");
    jobRowData.put("FIELDOFFICER_ID", "0000");
    jobRowData.put("CE_EXPECTED_CAPACITY", "");
    jobRowData.put("CE_SECURE", "0");
    jobRowData.put("PRINT_BATCH", "01");
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
          new ColumnValidator("test column", false, new Rule[] {new MandatoryRule()})
        };

    Survey survey = new Survey();
    survey.setSampleValidationRules(columnValidators);
    collectionExercise.setSurvey(survey);

    JobTypeProcessor jobTypeProcessor = new SampleLoadTypeProcessor("", "");

    JobRow jobRow = new JobRow();
    jobRow.setRowData(Map.of("test column", ""));
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
        .isEqualTo("Column 'test column' value '' validation error: Mandatory value missing");

    ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
    verify(jobRepository).saveAndFlush(jobArgumentCaptor.capture());
    Job actualJob = jobArgumentCaptor.getValue();

    assertThat(actualJob.getValidatingRowNumber()).isEqualTo(1);
    assertThat(actualJob.getErrorRowCount()).isEqualTo(1);
  }

  @Test
  void processChunkBulkUpdateSample() {
    // Given
    CollectionExercise collectionExercise = new CollectionExercise();
    Job job = new Job();
    job.setJobType(JobType.BULK_UPDATE_SAMPLE);
    job.setCollectionExercise(collectionExercise);

    ColumnValidator[] columnValidators =
        new ColumnValidator[] {
          new ColumnValidator("newValue", false, new Rule[] {new MandatoryRule()})
        };

    Survey survey = new Survey();
    survey.setSampleValidationRules(columnValidators);
    collectionExercise.setSurvey(survey);

    JobTypeProcessor jobTypeProcessor =
        new BulkUpdateSampleTypeProcessor("", "", collectionExercise);
    jobTypeProcessor.setSampleOrSensitiveValidationsMap(Map.of("test column", columnValidators));

    JobRow jobRow = new JobRow();
    jobRow.setRowData(Map.of("fieldToUpdate", "test column", "newValue", "test data"));
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
  void processChunkBulkUpdateSampleFailsValidation() {
    // Given
    CollectionExercise collectionExercise = new CollectionExercise();
    Job job = new Job();
    job.setJobType(JobType.BULK_UPDATE_SAMPLE);
    job.setCollectionExercise(collectionExercise);

    ColumnValidator[] columnValidators =
        new ColumnValidator[] {
          new ColumnValidator("newValue", false, new Rule[] {new MandatoryRule()})
        };

    Survey survey = new Survey();
    survey.setSampleValidationRules(columnValidators);
    collectionExercise.setSurvey(survey);

    JobTypeProcessor jobTypeProcessor =
        new BulkUpdateSampleTypeProcessor("", "", collectionExercise);
    jobTypeProcessor.setSampleOrSensitiveValidationsMap(Map.of("test column", columnValidators));

    JobRow jobRow = new JobRow();
    jobRow.setRowData(Map.of("fieldToUpdate", "test column", "newValue", ""));
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
        .isEqualTo("Column 'newValue' value '' validation error: Mandatory value missing");

    ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
    verify(jobRepository).saveAndFlush(jobArgumentCaptor.capture());
    Job actualJob = jobArgumentCaptor.getValue();

    assertThat(actualJob.getValidatingRowNumber()).isEqualTo(1);
    assertThat(actualJob.getErrorRowCount()).isEqualTo(1);
  }

  @Test
  void processChunkBulkUpdateSampleUnknownColumn() {
    // Given
    CollectionExercise collectionExercise = new CollectionExercise();
    Job job = new Job();
    job.setJobType(JobType.BULK_UPDATE_SAMPLE);
    job.setCollectionExercise(collectionExercise);

    ColumnValidator[] columnValidators =
        new ColumnValidator[] {
          new ColumnValidator("newValue", false, new Rule[] {new MandatoryRule()})
        };

    Survey survey = new Survey();
    survey.setSampleValidationRules(columnValidators);
    collectionExercise.setSurvey(survey);

    JobTypeProcessor jobTypeProcessor =
        new BulkUpdateSampleTypeProcessor("", "", collectionExercise);
    jobTypeProcessor.setSampleOrSensitiveValidationsMap(Map.of("test column", columnValidators));

    JobRow jobRow = new JobRow();
    jobRow.setRowData(Map.of("fieldToUpdate", "nonexistent column", "newValue", "test data"));
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
        .isEqualTo("fieldToUpdate column nonexistent column does not exist");

    ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
    verify(jobRepository).saveAndFlush(jobArgumentCaptor.capture());
    Job actualJob = jobArgumentCaptor.getValue();

    assertThat(actualJob.getValidatingRowNumber()).isEqualTo(1);
    assertThat(actualJob.getErrorRowCount()).isEqualTo(1);
  }

  @Test
  void processChunkBulkUpdateSampleSensitive() {
    // Given
    CollectionExercise collectionExercise = new CollectionExercise();
    Job job = new Job();
    job.setJobType(JobType.BULK_UPDATE_SAMPLE_SENSITIVE);
    job.setCollectionExercise(collectionExercise);

    ColumnValidator[] columnValidators =
        new ColumnValidator[] {
          new ColumnValidator("newValue", false, new Rule[] {new MandatoryRule()})
        };

    Survey survey = new Survey();
    survey.setSampleValidationRules(columnValidators);
    collectionExercise.setSurvey(survey);

    JobTypeProcessor jobTypeProcessor =
        new BulkUpdateSensitiveSampleTypeProcessor("", "", collectionExercise);
    jobTypeProcessor.setSampleOrSensitiveValidationsMap(Map.of("test column", columnValidators));

    JobRow jobRow = new JobRow();
    jobRow.setRowData(Map.of("fieldToUpdate", "test column", "newValue", "test data"));
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
  void processChunkBulkUpdateSampleSensitiveFailsValidation() {
    // Given
    CollectionExercise collectionExercise = new CollectionExercise();
    Job job = new Job();
    job.setJobType(JobType.BULK_UPDATE_SAMPLE_SENSITIVE);
    job.setCollectionExercise(collectionExercise);

    ColumnValidator[] columnValidators =
        new ColumnValidator[] {
          new ColumnValidator("newValue", false, new Rule[] {new LengthRule(5)})
        };

    Survey survey = new Survey();
    survey.setSampleValidationRules(columnValidators);
    collectionExercise.setSurvey(survey);

    JobTypeProcessor jobTypeProcessor =
        new BulkUpdateSensitiveSampleTypeProcessor("", "", collectionExercise);
    jobTypeProcessor.setSampleOrSensitiveValidationsMap(Map.of("test column", columnValidators));

    JobRow jobRow = new JobRow();
    jobRow.setRowData(Map.of("fieldToUpdate", "test column", "newValue", "123456789"));
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
        .isEqualTo(
            "Column 'newValue' value '123456789' validation error: Exceeded max length of 5");

    ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
    verify(jobRepository).saveAndFlush(jobArgumentCaptor.capture());
    Job actualJob = jobArgumentCaptor.getValue();

    assertThat(actualJob.getValidatingRowNumber()).isEqualTo(1);
    assertThat(actualJob.getErrorRowCount()).isEqualTo(1);
  }

  @Test
  void processChunkBulkUpdateSampleSensitiveBlankingAllowed() {
    // Given
    CollectionExercise collectionExercise = new CollectionExercise();
    Job job = new Job();
    job.setJobType(JobType.BULK_UPDATE_SAMPLE_SENSITIVE);
    job.setCollectionExercise(collectionExercise);

    ColumnValidator[] columnValidators =
        new ColumnValidator[] {
          new ColumnValidator("newValue", false, new Rule[] {new MandatoryRule()})
        };

    Survey survey = new Survey();
    survey.setSampleValidationRules(columnValidators);
    collectionExercise.setSurvey(survey);

    JobTypeProcessor jobTypeProcessor =
        new BulkUpdateSensitiveSampleTypeProcessor("", "", collectionExercise);
    jobTypeProcessor.setSampleOrSensitiveValidationsMap(Map.of("test column", columnValidators));

    JobRow jobRow = new JobRow();
    jobRow.setRowData(Map.of("fieldToUpdate", "test column", "newValue", ""));
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
  void processChunkBulkUpdateSampleSensitiveUnknownColumn() {
    // Given
    CollectionExercise collectionExercise = new CollectionExercise();
    Job job = new Job();
    job.setJobType(JobType.BULK_UPDATE_SAMPLE_SENSITIVE);
    job.setCollectionExercise(collectionExercise);

    ColumnValidator[] columnValidators =
        new ColumnValidator[] {
          new ColumnValidator("newValue", false, new Rule[] {new MandatoryRule()})
        };

    Survey survey = new Survey();
    survey.setSampleValidationRules(columnValidators);
    collectionExercise.setSurvey(survey);

    JobTypeProcessor jobTypeProcessor =
        new BulkUpdateSensitiveSampleTypeProcessor("", "", collectionExercise);
    jobTypeProcessor.setSampleOrSensitiveValidationsMap(Map.of("test column", columnValidators));

    JobRow jobRow = new JobRow();
    jobRow.setRowData(Map.of("fieldToUpdate", "nonexistent column", "newValue", "test data"));
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
        .isEqualTo("fieldToUpdate column nonexistent column does not exist");

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
