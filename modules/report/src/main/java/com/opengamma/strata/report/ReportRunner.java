/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report;

/**
 * Runs a report for a specific template type.
 * <p>
 * A report is a transformation from trade and/or aggregate calculation results into a
 * specific business format.
 * 
 * @param <T>  the type of the report template
 */
public interface ReportRunner<T extends ReportTemplate> {

  /**
   * Gets a description of the requirements to run a report for the given template.
   * Requirements include trade-level measures.
   * <p>
   * The report may be run on calculation results including at least these requirements.
   * 
   * @param reportTemplate  the report template
   * @return the requirements to run the report
   */
  public abstract ReportRequirements requirements(T reportTemplate);

  /**
   * Runs a report from a set of calculation results.
   * The contents of the report are dictated by the template provided.
   * The calculation results may be substantially more complete than the template requires.
   * 
   * @param calculationResults  the calculation results
   * @param reportTemplate  the report template
   * @return  the report
   */
  public abstract Report runReport(ReportCalculationResults calculationResults, T reportTemplate);

}
