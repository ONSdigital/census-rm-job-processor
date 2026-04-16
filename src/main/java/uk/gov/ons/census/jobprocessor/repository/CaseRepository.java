package uk.gov.ons.census.jobprocessor.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.census.common.model.entity.Case;
import uk.gov.ons.census.common.model.entity.CollectionExercise;

public interface CaseRepository extends JpaRepository<Case, UUID> {
  boolean existsByIdAndCollectionExercise(UUID caseId, CollectionExercise collectionExercise);
}
