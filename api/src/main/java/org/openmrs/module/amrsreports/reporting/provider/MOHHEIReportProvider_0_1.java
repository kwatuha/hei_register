package org.openmrs.module.amrsreports.reporting.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAttributeType;
import org.openmrs.api.APIException;
import org.openmrs.api.context.Context;
import org.openmrs.module.amrsreports.reporting.cohort.definition.MohHEICohortDefinition;
import org.openmrs.module.amrsreports.reporting.converter.DateListCustomConverter;
import org.openmrs.module.amrsreports.reporting.converter.DecimalAgeConverter;
import org.openmrs.module.amrsreports.reporting.converter.ObsRepresentationValueNumericConverter;
import org.openmrs.module.amrsreports.reporting.converter.WHOStageConverter;
import org.openmrs.module.amrsreports.reporting.data.AgeAtEvaluationDateDataDefinition;
import org.openmrs.module.amrsreports.reporting.data.CohortRestrictedBirthdateDataDefinition;
import org.openmrs.module.amrsreports.reporting.data.CohortRestrictedGenderDataDefinition;
import org.openmrs.module.amrsreports.reporting.data.CohortRestrictedPatientIdentifierDataDefinition;
import org.openmrs.module.amrsreports.reporting.data.CohortRestrictedPersonAttributeDataDefinition;
import org.openmrs.module.amrsreports.reporting.data.CohortRestrictedPreferredNameDataDefinition;
import org.openmrs.module.amrsreports.reporting.data.CtxStartDataDefinition;
import org.openmrs.module.amrsreports.reporting.data.DateARTStartedDataDefinition;
import org.openmrs.module.amrsreports.reporting.data.ObsNearestARVStartDateDataDefinition;
import org.openmrs.module.amrsreports.service.MohCoreService;
import org.openmrs.module.amrsreports.util.MOHReportUtil;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.data.converter.BirthdateConverter;
import org.openmrs.module.reporting.data.person.definition.PersonIdDataDefinition;
import org.openmrs.module.reporting.data.person.definition.PreferredAddressDataDefinition;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.ReportDesignResource;
import org.openmrs.module.reporting.report.definition.PeriodIndicatorReportDefinition;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.reporting.report.renderer.ExcelTemplateRenderer;
import org.openmrs.util.OpenmrsClassLoader;

/**
 * Provides mechanisms for rendering the MOH HEI Register
 */
public class MOHHEIReportProvider_0_1 extends ReportProvider {

	public static final String CONTACT_PHONE_ATTRIBUTE_TYPE = "Contact Phone Number";
	private static final String MONTH_AND_YEAR_FORMAT = "MM/yyyy";

	public MOHHEIReportProvider_0_1() {
		this.name = "MOH HEI Register 0.1";
		this.visible = true;
	}

	@Override
	public ReportDefinition getReportDefinition() {

		String nullString = null;
		MohCoreService service = Context.getService(MohCoreService.class);

		ReportDefinition report = new PeriodIndicatorReportDefinition();
		report.setName("MOH HEI Register Report");

		// set up the DSD
		PatientDataSetDefinition dsd = new PatientDataSetDefinition();
		dsd.setName("allPatients");

		// set up the columns ...

		// patient id ... until we get this thing working proper
		dsd.addColumn("Person ID", new PersonIdDataDefinition(), nullString);

		// b. Date ART started (Transfer to ART register)
		dsd.addColumn("Date ART Started", new DateARTStartedDataDefinition(), nullString);

		// c. Unique Patient Number
		PatientIdentifierType pit = service.getCCCNumberIdentifierType();
		CohortRestrictedPatientIdentifierDataDefinition cccColumn = new CohortRestrictedPatientIdentifierDataDefinition("CCC", pit);
		cccColumn.setIncludeFirstNonNullOnly(true);
		dsd.addColumn("Unique Patient Number", cccColumn, nullString);

		List<PatientIdentifierType> idTypes = Context.getPatientService().getAllPatientIdentifierTypes();
		idTypes.remove(pit);
		CohortRestrictedPatientIdentifierDataDefinition idColumn = new CohortRestrictedPatientIdentifierDataDefinition("Identifier");
		idColumn.setTypes(idTypes);
		idColumn.setIncludeFirstNonNullOnly(true);
		dsd.addColumn("AMPATH Identifier", idColumn, nullString);

		// d. Patient's Name
		dsd.addColumn("Name", new CohortRestrictedPreferredNameDataDefinition(), nullString);

		// e. Sex
		dsd.addColumn("Sex", new CohortRestrictedGenderDataDefinition(), nullString);

		// f1. Date of Birth
		dsd.addColumn("Date of Birth", new CohortRestrictedBirthdateDataDefinition(), nullString, new BirthdateConverter(MOHReportUtil.DATE_FORMAT));

		// f1. Age
		AgeAtEvaluationDateDataDefinition add = new AgeAtEvaluationDateDataDefinition();
		dsd.addColumn("Age", add, nullString, new DecimalAgeConverter(2));

		// g1. Address
		PreferredAddressDataDefinition padd = new PreferredAddressDataDefinition();
		dsd.addColumn("Address", padd, nullString);

		// g2. Phone Number
		PersonAttributeType pat = Context.getPersonService().getPersonAttributeTypeByName(CONTACT_PHONE_ATTRIBUTE_TYPE);
		CohortRestrictedPersonAttributeDataDefinition patientPhoneContact = new CohortRestrictedPersonAttributeDataDefinition(pat);
		dsd.addColumn("Phone Number", patientPhoneContact, nullString);

		// i. WHO Stage at start of ARVs
		ObsNearestARVStartDateDataDefinition whoDef = new ObsNearestARVStartDateDataDefinition(
				"WHO closest to ARV start",
				Context.getConceptService().getConcept(5356),
				Context.getConceptService().getConcept(1224)
		);
		dsd.addColumn("WHO Stage at ART Start", whoDef, nullString, new WHOStageConverter());

		// j. CD4 at start of ARVs
		ObsNearestARVStartDateDataDefinition cd4Def = new ObsNearestARVStartDateDataDefinition(
				"CD4 closest to ARV start",
				Context.getConceptService().getConcept(5497),
				Context.getConceptService().getConcept(730)
		);
		dsd.addColumn("CD4 at ART Start", cd4Def, nullString, new ObsRepresentationValueNumericConverter(1));

		// k. Height at start of ARVs
		ObsNearestARVStartDateDataDefinition heightDef = new ObsNearestARVStartDateDataDefinition(
				"height closest to ARV start",
				Context.getConceptService().getConcept(5090)
		);
		heightDef.setAgeLimit(12);
		dsd.addColumn("Height at ART Start", heightDef, nullString, new ObsRepresentationValueNumericConverter(1));

		// l. Weight at start of ARVs
		ObsNearestARVStartDateDataDefinition weightDef = new ObsNearestARVStartDateDataDefinition(
				"weight closest to ARV start",
				Context.getConceptService().getConcept(5089)
		);
		weightDef.setAgeLimit(12);
		dsd.addColumn("Weight at ART Start", weightDef, nullString, new ObsRepresentationValueNumericConverter(1));

		// m. CTX start date
		dsd.addColumn("CTX Start Dates", new CtxStartDataDefinition(), nullString, new DateListCustomConverter(MONTH_AND_YEAR_FORMAT));

		report.addDataSetDefinition(dsd, null);

		return report;
	}

	@Override
	public CohortDefinition getCohortDefinition() {
		return new MohHEICohortDefinition();
	}

	@Override
	public ReportDesign getReportDesign() {
		ReportDesign design = new ReportDesign();
		design.setName("MOH HEI Register Design");
		design.setReportDefinition(this.getReportDefinition());
		design.setRendererType(ExcelTemplateRenderer.class);

		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:7,dataset:allPatients");

		design.setProperties(props);

		ReportDesignResource resource = new ReportDesignResource();
		resource.setName("template.xls");
		InputStream is = OpenmrsClassLoader.getInstance().getResourceAsStream("templates/MOHHEIReportTemplate_0_1");

		if (is == null)
			throw new APIException("Could not find report template.");

		try {
			resource.setContents(IOUtils.toByteArray(is));
		} catch (IOException ex) {
			throw new APIException("Could not create report design for MOH 361B Register.", ex);
		}

		IOUtils.closeQuietly(is);
		design.addResource(resource);

		return design;
	}
}