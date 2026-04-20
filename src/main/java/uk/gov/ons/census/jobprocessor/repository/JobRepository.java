package uk.gov.ons.census.jobprocessor.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.census.common.model.entity.Job;
import uk.gov.ons.census.common.model.entity.JobStatus;

public interface JobRepository extends JpaRepository<Job, UUID> {
  List<Job> findByJobStatus(JobStatus jobStatus);
}
