package uk.gov.ons.census.jobprocessor.testutils;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.census.common.model.entity.Case;
import uk.gov.ons.census.common.model.entity.CollectionExercise;
import uk.gov.ons.census.common.model.entity.CollectionInstrumentSelectionRule;
import uk.gov.ons.census.common.model.entity.Survey;
import uk.gov.ons.census.common.validation.ColumnValidator;
import uk.gov.ons.census.common.validation.LengthRule;
import uk.gov.ons.census.common.validation.MandatoryRule;
import uk.gov.ons.census.common.validation.Rule;
import uk.gov.ons.census.jobprocessor.repository.CaseRepository;
import uk.gov.ons.census.jobprocessor.repository.CollectionExerciseRepository;
import uk.gov.ons.census.jobprocessor.repository.SurveyRepository;

@Component
@ActiveProfiles("test")
public class JunkDataHelper {
  private static final Random RANDOM = new Random();

  @Autowired private CaseRepository caseRepository;
  @Autowired private CollectionExerciseRepository collectionExerciseRepository;
  @Autowired private SurveyRepository surveyRepository;

  public Case setupJunkCase() {
    Case junkCase = new Case();
    junkCase.setId(UUID.randomUUID());
    junkCase.setInvalid(false);
    junkCase.setCollectionExercise(setupJunkCollex());
    junkCase.setCaseRef(RANDOM.nextLong());
    junkCase.setAbpCode("abp");
    junkCase.setAddressLevel("abp");
    junkCase.setAddressLine2("abp");
    junkCase.setAddressType("HH");
    junkCase.setCeExpectedCapacity(0);
    junkCase.setFieldCoordinatorId("abp");
    junkCase.setFieldOfficerId("abp");
    junkCase.setHtcDigital("abp");
    junkCase.setHtcWillingness("abp");
    junkCase.setLad("abp");
    junkCase.setLatitude("abp");
    junkCase.setLsoa("abp");
    junkCase.setRegion("EN");
    junkCase.setOa("abp");
    junkCase.setMsoa("abp");
    junkCase.setPostcode("CFXX XXX");
    junkCase.setTreatmentCode("CFXX XXX");
    junkCase.setUprn("blank");
    junkCase.setTownName("test");
    caseRepository.save(junkCase);

    return junkCase;
  }

  public CollectionExercise setupJunkCollex() {
    Survey junkSurvey = new Survey();
    junkSurvey.setId(UUID.randomUUID());
    junkSurvey.setName("Junk survey");
    junkSurvey.setSampleValidationRules(
        new ColumnValidator[] {
          new ColumnValidator("Junk", false, new Rule[] {new MandatoryRule()}),
          new ColumnValidator(
              "SensitiveJunk", true, new Rule[] {new MandatoryRule(), new LengthRule(10)})
        });
    junkSurvey.setSampleSeparator('j');
    junkSurvey.setSampleDefinitionUrl("http://junk");
    surveyRepository.saveAndFlush(junkSurvey);

    CollectionExercise junkCollectionExercise = new CollectionExercise();
    junkCollectionExercise.setId(UUID.randomUUID());
    junkCollectionExercise.setName("Junk collex");
    junkCollectionExercise.setSurvey(junkSurvey);
    junkCollectionExercise.setReference("MVP012021");
    junkCollectionExercise.setStartDate(OffsetDateTime.now());
    junkCollectionExercise.setEndDate(OffsetDateTime.now().plusDays(2));
    junkCollectionExercise.setMetadata(null);
    junkCollectionExercise.setCollectionInstrumentSelectionRules(
        new CollectionInstrumentSelectionRule[] {
          new CollectionInstrumentSelectionRule(0, null, "junkCollectionInstrumentUrl", null)
        });
    collectionExerciseRepository.saveAndFlush(junkCollectionExercise);

    return junkCollectionExercise;
  }
}
