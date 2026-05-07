package uk.gov.ons.census.jobprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.ons.census.common.model.entity.Case;
import uk.gov.ons.census.common.model.entity.CollectionExercise;
import uk.gov.ons.census.common.model.entity.Job;
import uk.gov.ons.census.common.model.entity.JobRow;
import uk.gov.ons.census.common.model.entity.JobRowStatus;
import uk.gov.ons.census.common.model.entity.JobStatus;
import uk.gov.ons.census.common.model.entity.JobType;
import uk.gov.ons.census.jobprocessor.repository.JobRepository;
import uk.gov.ons.census.jobprocessor.repository.JobRowRepository;
import uk.gov.ons.census.jobprocessor.testutils.JunkDataHelper;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class StagedJobValidatorIT {
  @Autowired private JobRepository jobRepository;
  @Autowired private JobRowRepository jobRowRepository;
  @Autowired private JunkDataHelper junkDataHelper;

  @Test
  void processStagedJobs() {
    CollectionExercise collectionExercise = junkDataHelper.setupJunkCollex();

    Job job = new Job();
    job.setId(UUID.randomUUID());
    job.setCollectionExercise(collectionExercise);
    job.setJobStatus(JobStatus.STAGING_IN_PROGRESS);
    job.setJobType(JobType.SAMPLE);
    job.setCreatedBy("norman");
    job.setCreatedAt(OffsetDateTime.now());
    job.setFileId(UUID.randomUUID());
    job.setFileName("normansfile.csv");
    job = jobRepository.saveAndFlush(job);

    JobRow jobRow = new JobRow();
    jobRow.setId(UUID.randomUUID());
    jobRow.setJob(job);
    jobRow.setJobRowStatus(JobRowStatus.STAGED);
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
    jobRow.setOriginalRowData(new String[] {"foo", "bar"});
    jobRowRepository.saveAndFlush(jobRow);

    // This will unleash the hounds
    job.setJobStatus(JobStatus.VALIDATION_IN_PROGRESS);
    jobRepository.saveAndFlush(job);

    // Now check that the job processed OK
    LocalTime testTimeout = LocalTime.now().plusSeconds(60);

    Job processedJob = null;

    do {
      if (processedJob != null) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          // Ignored
        }
      }

      processedJob = jobRepository.findById(job.getId()).get();
    } while (processedJob.getJobStatus() == JobStatus.VALIDATION_IN_PROGRESS
        && LocalTime.now().isBefore(testTimeout));

    assertThat(processedJob.getJobStatus()).isEqualTo(JobStatus.VALIDATED_OK);
    assertThat(processedJob.getValidatingRowNumber()).isEqualTo(1);
    assertThat(processedJob.getErrorRowCount()).isEqualTo(0);

    JobRow processedJobRow = jobRowRepository.findById(jobRow.getId()).get();
    assertThat(processedJobRow.getJobRowStatus()).isEqualTo(JobRowStatus.VALIDATED_OK);
  }

  @Test
  void processStagedJobsFailsValidation() {
    CollectionExercise collectionExercise = junkDataHelper.setupJunkCollex();

    Job job = new Job();
    job.setId(UUID.randomUUID());
    job.setCollectionExercise(collectionExercise);
    job.setJobStatus(JobStatus.STAGING_IN_PROGRESS);
    job.setJobType(JobType.SAMPLE);
    job.setCreatedBy("norman");
    job.setCreatedAt(OffsetDateTime.now());
    job.setFileId(UUID.randomUUID());
    job.setFileName("normansfile.csv");
    job = jobRepository.saveAndFlush(job);

    JobRow jobRow = new JobRow();
    jobRow.setId(UUID.randomUUID());
    jobRow.setJob(job);
    jobRow.setJobRowStatus(JobRowStatus.STAGED);
    Map<String, String> jobRowData = new HashMap<>();
    jobRowData.put("UPRN", "");
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
    jobRow.setOriginalRowData(new String[] {"foo", "bar"});
    jobRowRepository.saveAndFlush(jobRow);

    // This will unleash the hounds
    job.setJobStatus(JobStatus.VALIDATION_IN_PROGRESS);
    jobRepository.saveAndFlush(job);

    // Now check that the job processed OK
    LocalTime testTimeout = LocalTime.now().plusSeconds(60);

    Job processedJob = null;

    do {
      if (processedJob != null) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          // Ignored
        }
      }

      processedJob = jobRepository.findById(job.getId()).get();
    } while (processedJob.getJobStatus() == JobStatus.VALIDATION_IN_PROGRESS
        && LocalTime.now().isBefore(testTimeout));

    assertThat(processedJob.getJobStatus()).isEqualTo(JobStatus.VALIDATED_WITH_ERRORS);
    assertThat(processedJob.getValidatingRowNumber()).isEqualTo(1);
    assertThat(processedJob.getErrorRowCount()).isEqualTo(1);

    JobRow processedJobRow = jobRowRepository.findById(jobRow.getId()).get();
    assertThat(processedJobRow.getJobRowStatus()).isEqualTo(JobRowStatus.VALIDATED_ERROR);
    assertThat(processedJobRow.getValidationErrorDescriptions())
        .isEqualTo("Column 'UPRN' value '' validation error: Mandatory value missing");
  }

  @Test
  void processStagedJobsBulkRefusal() {
    Case caze = junkDataHelper.setupJunkCase();
    CollectionExercise collectionExercise = caze.getCollectionExercise();

    Job job = new Job();
    job.setId(UUID.randomUUID());
    job.setCollectionExercise(collectionExercise);
    job.setJobStatus(JobStatus.STAGING_IN_PROGRESS);
    job.setJobType(JobType.BULK_REFUSAL);
    job.setCreatedBy("norman");
    job.setCreatedAt(OffsetDateTime.now());
    job.setFileId(UUID.randomUUID());
    job.setFileName("normansfile.csv");
    job = jobRepository.saveAndFlush(job);

    JobRow jobRow = new JobRow();
    jobRow.setId(UUID.randomUUID());
    jobRow.setJob(job);
    jobRow.setJobRowStatus(JobRowStatus.STAGED);
    jobRow.setRowData(Map.of("caseId", caze.getId().toString(), "refusalType", "HARD_REFUSAL"));
    jobRow.setOriginalRowData(new String[] {"foo", "bar"});
    jobRowRepository.saveAndFlush(jobRow);

    // This will unleash the hounds
    job.setJobStatus(JobStatus.VALIDATION_IN_PROGRESS);
    jobRepository.saveAndFlush(job);

    // Now check that the job processed OK
    LocalTime testTimeout = LocalTime.now().plusSeconds(60);

    Job processedJob = null;

    do {
      if (processedJob != null) {
        try {
          Thread.sleep(60);
        } catch (InterruptedException e) {
          // Ignored
        }
      }

      processedJob = jobRepository.findById(job.getId()).get();
    } while (processedJob.getJobStatus() == JobStatus.VALIDATION_IN_PROGRESS
        && LocalTime.now().isBefore(testTimeout));

    assertThat(processedJob.getJobStatus()).isEqualTo(JobStatus.VALIDATED_OK);
    assertThat(processedJob.getValidatingRowNumber()).isEqualTo(1);
    assertThat(processedJob.getErrorRowCount()).isEqualTo(0);

    JobRow processedJobRow = jobRowRepository.findById(jobRow.getId()).get();
    assertThat(processedJobRow.getJobRowStatus()).isEqualTo(JobRowStatus.VALIDATED_OK);
  }

  @Test
  void processStagedJobsBulkInvalidCase() {
    Case caze = junkDataHelper.setupJunkCase();
    CollectionExercise collectionExercise = caze.getCollectionExercise();

    Job job = new Job();
    job.setId(UUID.randomUUID());
    job.setCollectionExercise(collectionExercise);
    job.setJobStatus(JobStatus.STAGING_IN_PROGRESS);
    job.setJobType(JobType.BULK_INVALID);
    job.setCreatedBy("norman");
    job.setCreatedAt(OffsetDateTime.now());
    job.setFileId(UUID.randomUUID());
    job.setFileName("normansfile.csv");
    job = jobRepository.saveAndFlush(job);

    JobRow jobRow = new JobRow();
    jobRow.setId(UUID.randomUUID());
    jobRow.setJob(job);
    jobRow.setJobRowStatus(JobRowStatus.STAGED);
    jobRow.setRowData(Map.of("caseId", caze.getId().toString(), "reason", "wrong country"));
    jobRow.setOriginalRowData(new String[] {"foo", "bar"});
    jobRowRepository.saveAndFlush(jobRow);

    // This will unleash the hounds
    job.setJobStatus(JobStatus.VALIDATION_IN_PROGRESS);
    jobRepository.saveAndFlush(job);

    // Now check that the job processed OK
    LocalTime testTimeout = LocalTime.now().plusSeconds(60);

    Job processedJob = null;

    do {
      if (processedJob != null) {
        try {
          Thread.sleep(60);
        } catch (InterruptedException e) {
          // Ignored
        }
      }

      processedJob = jobRepository.findById(job.getId()).get();
    } while (processedJob.getJobStatus() == JobStatus.VALIDATION_IN_PROGRESS
        && LocalTime.now().isBefore(testTimeout));

    assertThat(processedJob.getJobStatus()).isEqualTo(JobStatus.VALIDATED_OK);
    assertThat(processedJob.getValidatingRowNumber()).isEqualTo(1);
    assertThat(processedJob.getErrorRowCount()).isEqualTo(0);

    JobRow processedJobRow = jobRowRepository.findById(jobRow.getId()).get();
    assertThat(processedJobRow.getJobRowStatus()).isEqualTo(JobRowStatus.VALIDATED_OK);
  }
}
