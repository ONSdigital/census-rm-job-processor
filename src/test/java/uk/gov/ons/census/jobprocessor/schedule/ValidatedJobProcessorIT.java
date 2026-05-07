package uk.gov.ons.census.jobprocessor.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import uk.gov.ons.census.jobprocessor.model.dto.messaging.EventDTO;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.RefusalTypeDTO;
import uk.gov.ons.census.jobprocessor.repository.JobRepository;
import uk.gov.ons.census.jobprocessor.repository.JobRowRepository;
import uk.gov.ons.census.jobprocessor.testutils.JunkDataHelper;
import uk.gov.ons.census.jobprocessor.testutils.PubsubHelper;
import uk.gov.ons.census.jobprocessor.testutils.QueueSpy;

@ContextConfiguration
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ValidatedJobProcessorIT {
  private static final String NEW_CASE_SUBSCRIPTION = "event_new-case_rm-case-processor";
  private static final String REFUSAL_SUBSCRIPTION = "event_refusal_rm-case-processor";
  private static final String INVALID_SUBSCRIPTION = "event_invalid-case_rm-case-processor";
  private static final String UPDATE_SAMPLE_SUBSCRIPTION = "event_update-sample_rm-case-processor";
  private static final String UPDATE_SAMPLE_SENSITIVE_SUBSCRIPTION =
      "event_update-sample-sensitive_rm-case-processor";

  @Autowired private JobRepository jobRepository;

  @Autowired private JobRowRepository jobRowRepository;

  @Autowired private JunkDataHelper junkDataHelper;

  @Autowired private PubsubHelper pubsubHelper;

  @Value("${queueconfig.new-case-topic}")
  private String newCaseTopic;

  @Value("${queueconfig.refusal-event-topic}")
  private String refusalEventTopic;

  @Value("${queueconfig.invalid-case-event-topic}")
  private String invalidCaseEventTopic;

  @Value("${queueconfig.update-sample-topic}")
  private String updateSampleTopic;

  @Value("${queueconfig.update-sample-sensitive-topic}")
  private String updateSampleSensitiveTopic;

  @Test
  void processStagedJobsSample() throws InterruptedException {
    pubsubHelper.purgePubsubProjectMessages(NEW_CASE_SUBSCRIPTION, newCaseTopic);

    try (QueueSpy<EventDTO> surveyUpdateQueue =
        pubsubHelper.pubsubProjectListen(NEW_CASE_SUBSCRIPTION, EventDTO.class)) {
      CollectionExercise collectionExercise = junkDataHelper.setupJunkCollex();

      Job job = new Job();
      job.setId(UUID.randomUUID());
      job.setCollectionExercise(collectionExercise);
      job.setJobStatus(JobStatus.VALIDATED_OK);
      job.setJobType(JobType.SAMPLE);
      job.setCreatedBy("norman");
      job.setCreatedAt(OffsetDateTime.now());
      job.setFileId(UUID.randomUUID());
      job.setFileName("normansfile.csv");
      job = jobRepository.saveAndFlush(job);

      JobRow jobRow = new JobRow();
      jobRow.setId(UUID.randomUUID());
      jobRow.setJob(job);
      jobRow.setJobRowStatus(JobRowStatus.VALIDATED_OK);
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
      jobRowData.put("CE_EXPECTED_CAPACITY", "0");
      jobRowData.put("CE_SECURE", "0");
      jobRowData.put("PRINT_BATCH", "01");
      jobRow.setRowData(jobRowData);
      jobRow.setOriginalRowData(new String[] {"foo", "bar"});
      jobRowRepository.saveAndFlush(jobRow);

      // This will unleash the hounds
      job.setJobStatus(JobStatus.PROCESSING_IN_PROGRESS);
      jobRepository.saveAndFlush(job);

      // Now check that the job processed OK
      EventDTO emittedEvent = surveyUpdateQueue.getQueue().poll(20, TimeUnit.SECONDS);
      assertThat(emittedEvent).isNotNull();
      assertThat(emittedEvent.getPayload().getNewCase()).isNotNull();
      assertThat(emittedEvent.getPayload().getNewCase().getCollectionExerciseId())
          .isEqualTo(collectionExercise.getId());
      assertThat(emittedEvent.getPayload().getNewCase().getUprn())
          .isEqualTo(jobRowData.get("UPRN"));
      Job processedJob = getProcessedJob(job.getId());
      assertThat(processedJob.getJobStatus()).isEqualTo(JobStatus.PROCESSED);
      assertThat(processedJob.getProcessingRowNumber()).isEqualTo(1);

      Optional<JobRow> optionalJobRow = jobRowRepository.findById(jobRow.getId());
      assertThat(optionalJobRow.isPresent()).isFalse();
    }
  }

  @Test
  void processStagedJobsBulkRefusal() throws InterruptedException {
    pubsubHelper.purgePubsubProjectMessages(REFUSAL_SUBSCRIPTION, refusalEventTopic);

    try (QueueSpy<EventDTO> surveyUpdateQueue =
        pubsubHelper.pubsubProjectListen(REFUSAL_SUBSCRIPTION, EventDTO.class)) {
      Case caze = junkDataHelper.setupJunkCase();
      CollectionExercise collectionExercise = caze.getCollectionExercise();

      Job job = new Job();
      job.setId(UUID.randomUUID());
      job.setCollectionExercise(collectionExercise);
      job.setJobStatus(JobStatus.VALIDATED_OK);
      job.setJobType(JobType.BULK_REFUSAL);
      job.setCreatedBy("norman");
      job.setCreatedAt(OffsetDateTime.now());
      job.setFileId(UUID.randomUUID());
      job.setFileName("normansfile.csv");
      job = jobRepository.saveAndFlush(job);

      JobRow jobRow = new JobRow();
      jobRow.setId(UUID.randomUUID());
      jobRow.setJob(job);
      jobRow.setJobRowStatus(JobRowStatus.VALIDATED_OK);
      jobRow.setRowData(Map.of("caseId", caze.getId().toString(), "refusalType", "HARD_REFUSAL"));
      jobRow.setOriginalRowData(new String[] {"foo", "bar"});
      jobRowRepository.saveAndFlush(jobRow);

      // This will unleash the hounds
      job.setJobStatus(JobStatus.PROCESSING_IN_PROGRESS);
      jobRepository.saveAndFlush(job);

      // Now check that the job processed OK
      EventDTO emittedEvent = surveyUpdateQueue.getQueue().poll(20, TimeUnit.SECONDS);
      assertThat(emittedEvent).isNotNull();
      assertThat(emittedEvent.getPayload().getRefusal()).isNotNull();
      assertThat(emittedEvent.getPayload().getRefusal().getCaseId()).isEqualTo(caze.getId());
      assertThat(emittedEvent.getPayload().getRefusal().getType())
          .isEqualTo(RefusalTypeDTO.HARD_REFUSAL);

      Job processedJob = getProcessedJob(job.getId());
      assertThat(processedJob.getJobStatus()).isEqualTo(JobStatus.PROCESSED);
      assertThat(processedJob.getProcessingRowNumber()).isEqualTo(1);

      Optional<JobRow> optionalJobRow = jobRowRepository.findById(jobRow.getId());
      assertThat(optionalJobRow.isPresent()).isFalse();
    }
  }

  @Test
  void processStagedJobsBulkInvalid() throws InterruptedException {
    pubsubHelper.purgePubsubProjectMessages(INVALID_SUBSCRIPTION, invalidCaseEventTopic);

    try (QueueSpy<EventDTO> surveyUpdateQueue =
        pubsubHelper.pubsubProjectListen(INVALID_SUBSCRIPTION, EventDTO.class)) {
      Case caze = junkDataHelper.setupJunkCase();
      CollectionExercise collectionExercise = caze.getCollectionExercise();

      Job job = new Job();
      job.setId(UUID.randomUUID());
      job.setCollectionExercise(collectionExercise);
      job.setJobStatus(JobStatus.VALIDATED_OK);
      job.setJobType(JobType.BULK_INVALID);
      job.setCreatedBy("norman");
      job.setCreatedAt(OffsetDateTime.now());
      job.setFileId(UUID.randomUUID());
      job.setFileName("normansfile.csv");
      job = jobRepository.saveAndFlush(job);

      JobRow jobRow = new JobRow();
      jobRow.setId(UUID.randomUUID());
      jobRow.setJob(job);
      jobRow.setJobRowStatus(JobRowStatus.VALIDATED_OK);
      jobRow.setRowData(Map.of("caseId", caze.getId().toString(), "reason", "why"));
      jobRow.setOriginalRowData(new String[] {"foo", "bar"});
      jobRowRepository.saveAndFlush(jobRow);

      // This will unleash the hounds
      job.setJobStatus(JobStatus.PROCESSING_IN_PROGRESS);
      jobRepository.saveAndFlush(job);

      // Now check that the job processed OK
      EventDTO emittedEvent = surveyUpdateQueue.getQueue().poll(20, TimeUnit.SECONDS);
      assertThat(emittedEvent).isNotNull();
      assertThat(emittedEvent.getPayload().getInvalidCase()).isNotNull();
      assertThat(emittedEvent.getPayload().getInvalidCase().getCaseId()).isEqualTo(caze.getId());
      assertThat(emittedEvent.getPayload().getInvalidCase().getReason()).isEqualTo("why");

      Job processedJob = getProcessedJob(job.getId());
      assertThat(processedJob.getJobStatus()).isEqualTo(JobStatus.PROCESSED);
      assertThat(processedJob.getProcessingRowNumber()).isEqualTo(1);

      Optional<JobRow> optionalJobRow = jobRowRepository.findById(jobRow.getId());
      assertThat(optionalJobRow.isPresent()).isFalse();
    }
  }

  private Job getProcessedJob(UUID jobId) {
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

      processedJob = jobRepository.findById(jobId).get();
    } while (processedJob.getJobStatus() == JobStatus.PROCESSING_IN_PROGRESS
        && LocalTime.now().isBefore(testTimeout));

    return processedJob;
  }
}
