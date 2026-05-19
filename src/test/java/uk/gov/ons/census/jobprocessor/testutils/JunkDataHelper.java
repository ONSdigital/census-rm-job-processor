package uk.gov.ons.census.jobprocessor.testutils;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.ons.census.common.model.entity.Case;
import uk.gov.ons.census.common.model.entity.CollectionExercise;
import uk.gov.ons.census.common.model.entity.Survey;
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
    junkCase.setAddressLevel("1 Address street");
    junkCase.setAddressType("HH");
    junkCase.setCeExpectedCapacity(0);
    junkCase.setFieldCoordinatorId("fcor_id");
    junkCase.setFieldOfficerId("foff_id");
    junkCase.setHtcDigital("0");
    junkCase.setHtcWillingness("0");
    junkCase.setLad("0000");
    junkCase.setLatitude("0.0.0.0.0.0");
    junkCase.setLsoa("0000");
    junkCase.setRegion("EN");
    junkCase.setOa("0000");
    junkCase.setMsoa("0000");
    junkCase.setPostcode("CFXX XXX");
    junkCase.setTreatmentCode("BLJF_FEJG");
    junkCase.setUprn("000000");
    junkCase.setTownName("Best Town");
    caseRepository.save(junkCase);

    return junkCase;
  }

  public CollectionExercise setupJunkCollex() {
    Survey junkSurvey = new Survey();
    junkSurvey.setId(UUID.randomUUID());
    junkSurvey.setName("Junk survey");
    junkSurvey.setSampleSeparator('j');
    surveyRepository.saveAndFlush(junkSurvey);

    CollectionExercise junkCollectionExercise = new CollectionExercise();
    junkCollectionExercise.setId(UUID.randomUUID());
    junkCollectionExercise.setName("Junk collex");
    junkCollectionExercise.setSurvey(junkSurvey);
    junkCollectionExercise.setReference("MVP012021");
    junkCollectionExercise.setStartDate(OffsetDateTime.now());
    junkCollectionExercise.setEndDate(OffsetDateTime.now().plusDays(2));
    junkCollectionExercise.setMetadata(null);

    collectionExerciseRepository.saveAndFlush(junkCollectionExercise);

    return junkCollectionExercise;
  }

  public static @NonNull Map<String, String> getJobRowData() {
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
    return jobRowData;
  }
}
