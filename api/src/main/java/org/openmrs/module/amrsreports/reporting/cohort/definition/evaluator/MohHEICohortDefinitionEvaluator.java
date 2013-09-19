package org.openmrs.module.amrsreports.reporting.cohort.definition.evaluator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Location;
import org.openmrs.annotation.Handler;
import org.openmrs.api.context.Context;
import org.openmrs.module.amrsreports.reporting.cohort.definition.MohHEICohortDefinition;
import org.openmrs.module.reporting.cohort.EvaluatedCohort;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.evaluator.CohortDefinitionEvaluator;
import org.openmrs.module.reporting.cohort.definition.service.CohortDefinitionService;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.module.reporting.evaluation.EvaluationException;

/**
 * Evaluator for MOH HEI Register Cohort Definition
 */
@Handler(supports = {MohHEICohortDefinition.class})
public class MohHEICohortDefinitionEvaluator implements CohortDefinitionEvaluator {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public EvaluatedCohort evaluate(CohortDefinition cohortDefinition, EvaluationContext context) throws EvaluationException {

        MohHEICohortDefinition definition = (MohHEICohortDefinition) cohortDefinition;

        if (definition == null)
            return null;

		if (definition.getFacility() == null)
			return null;

		String reportDate = sdf.format(context.getEvaluationDate());
		List<Location> locationList = new ArrayList<Location>();
		locationList.addAll(definition.getFacility().getLocations());
		context.addParameterValue("locationList", locationList);

        String sql =
                "select patient_id" +
                        " from amrsreports_hiv_care_enrollment " +
                        " where " +
                        "  first_arv_date is not NULL" +
                        "  and enrollment_date is not NULL" +
                        "  and first_arv_date <= ':reportDate'" +
                        "  and first_arv_location_id in ( :locationList )";

        SqlCohortDefinition sqlCohortDefinition = new SqlCohortDefinition(sql.replaceAll(":reportDate", reportDate));
        Cohort results = Context.getService(CohortDefinitionService.class).evaluate(sqlCohortDefinition, context);

        return new EvaluatedCohort(results, sqlCohortDefinition, context);
    }
}
