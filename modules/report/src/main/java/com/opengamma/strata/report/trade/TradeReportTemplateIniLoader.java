/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.report.ReportTemplateIniLoader;

/**
 * Loads a trade report template from the standard INI file format.
 * <p>
 * In a trade report template, the sections in the INI file (other than the special settings
 * section) correspond to the columns in the report, in the order they are declared.
 * <p>
 * Each section can specify the following properties:
 * <ul>
 * <li>value - identifies the value to display in the column's cells
 * <li>ignoreFailures - optional boolean flag to disable failure messages in this column
 * </ul>
 */
public class TradeReportTemplateIniLoader
    implements ReportTemplateIniLoader<TradeReportTemplate> {

  /**
   * The report type.
   */
  private static final String REPORT_TYPE = "trade";
  /**
   * The value property name.
   */
  private static final String VALUE_PROPERTY = "value";
  /**
   * The ignore-failures property name.
   */
  private static final String IGNORE_FAILURES_PROPERTY = "ignoreFailures";

  //-------------------------------------------------------------------------
  @Override
  public String getReportType() {
    return REPORT_TYPE;
  }

  @Override
  public TradeReportTemplate load(IniFile iniFile) {
    List<TradeReportColumn> reportColumns = new ArrayList<TradeReportColumn>();
    for (String columnName : iniFile.sections()) {
      if (columnName.toLowerCase(Locale.ENGLISH).equals(SETTINGS_SECTION)) {
        continue;
      }
      PropertySet properties = iniFile.section(columnName);
      reportColumns.add(parseColumn(columnName, properties));
    }
    return TradeReportTemplate.builder()
        .columns(reportColumns)
        .build();
  }

  private TradeReportColumn parseColumn(String columnName, PropertySet properties) {
    TradeReportColumn.Builder columnBuilder = TradeReportColumn.builder();
    columnBuilder.header(columnName);

    if (properties.contains(VALUE_PROPERTY)) {
      columnBuilder.value(properties.value(VALUE_PROPERTY));
    }
    if (properties.contains(IGNORE_FAILURES_PROPERTY)) {
      String ignoreFailuresValue = properties.value(IGNORE_FAILURES_PROPERTY);
      boolean ignoresFailure = Boolean.valueOf(ignoreFailuresValue);
      columnBuilder.ignoreFailures(ignoresFailure);
    }
    return columnBuilder.build();
  }

}
