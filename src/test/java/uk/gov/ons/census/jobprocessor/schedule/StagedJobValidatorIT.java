package uk.gov.ons.census.jobprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.census.jobprocessor.testutils.JunkDataHelper.getJobRowData;

import java.time.LocalTime;
import java.time.OffsetDateTime;
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
    Map<String, String> jobRowData = getJobRowData();
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
    Map<String, String> jobRowData = getJobRowData();
    jobRowData.put("UPRN", "");
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
