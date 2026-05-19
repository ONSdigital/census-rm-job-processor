package uk.gov.ons.census.jobprocessor.transformer;

import java.util.Map;
import uk.gov.ons.census.common.model.entity.Job;
import uk.gov.ons.census.common.model.entity.JobRow;
import uk.gov.ons.census.common.validation.ColumnValidator;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.EventDTO;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.EventHeaderDTO;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.NewCase;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.PayloadDTO;
import uk.gov.ons.census.jobprocessor.utility.EventHelper;

public class NewCaseTransformer implements Transformer {
  @Override
  public Object transformRow(
      Job job, JobRow jobRow, ColumnValidator[] columnValidators, String topic) {

    Map<String, String> rowData = jobRow.getRowData();

    NewCase newCase = new NewCase();
    newCase.setCaseId(jobRow.getId()); // Use row ID so we get no dupes if support tool crashes
    newCase.setCollectionExerciseId(job.getCollectionExercise().getId());

    newCase.setUprn(rowData.get("UPRN"));
    newCase.setEstabUprn(rowData.get("ESTAB_UPRN"));
    newCase.setAddressType(rowData.get("ADDRESS_TYPE"));
    newCase.setEstabType(rowData.get("ESTAB_TYPE"));
    newCase.setAddressLevel(rowData.get("ADDRESS_LEVEL"));
    newCase.setAbpCode(rowData.get("ABP_CODE"));
    newCase.setOrganisationName(rowData.get("ORGANISATION_NAME"));
    newCase.setAddressLine1(rowData.get("ADDRESS_LINE1"));
    newCase.setAddressLine2(rowData.get("ADDRESS_LINE2"));
    newCase.setAddressLine3(rowData.get("ADDRESS_LINE3"));
    newCase.setTownName(rowData.get("TOWN_NAME"));
    newCase.setPostcode(rowData.get("POSTCODE"));
    newCase.setLatitude(rowData.get("LATITUDE"));
    newCase.setLongitude(rowData.get("LONGITUDE"));
    newCase.setOa(rowData.get("OA"));
    newCase.setLsoa(rowData.get("LSOA"));
    newCase.setMsoa(rowData.get("MSOA"));
    newCase.setLad(rowData.get("LAD"));
    newCase.setRegion(rowData.get("REGION"));
    newCase.setHtcWillingness(rowData.get("HTC_WILLINGNESS"));
    newCase.setHtcDigital(rowData.get("HTC_DIGITAL"));
    newCase.setTreatmentCode(rowData.get("TREATMENT_CODE"));
    newCase.setFieldCoordinatorId(rowData.get("FIELDCOORDINATOR_ID"));
    newCase.setFieldOfficerId(rowData.get("FIELDOFFICER_ID"));
    newCase.setSecureEstablishment(Boolean.parseBoolean(rowData.get("CE_SECURE")));
    newCase.setPrintBatch(rowData.get("PRINT_BATCH"));
    if (!rowData.get("CE_EXPECTED_CAPACITY").isEmpty()) {
      newCase.setCeExpectedCapacity(Integer.parseInt(rowData.get("CE_EXPECTED_CAPACITY")));
    }

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setNewCase(newCase);

    EventDTO event = new EventDTO();
    EventHeaderDTO eventHeader = EventHelper.createEventDTO(topic, job.getProcessedBy());
    eventHeader.setCorrelationId(job.getId());
    event.setHeader(eventHeader);
    event.setPayload(payloadDTO);

    return event;
  }
}
