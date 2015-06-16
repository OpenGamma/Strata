/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import java.util.ArrayList;
import java.util.List;

import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.report.ReportTemplateIniLoader;

/**
 * Loader for trade report templates in the standard INI file format.
 */
public class TradeReportTemplateIniLoader implements ReportTemplateIniLoader<TradeReportTemplate> {

  private static final String VALUE_PROPERTY = "value";
  private static final String IGNORE_FAILURES_PROPERTY = "ignoreFailures";

  @Override
  public TradeReportTemplate load(IniFile iniFile) {
    List<TradeReportColumn> reportColumns = new ArrayList<TradeReportColumn>();
    for (String columnName : iniFile.keys()) {
      if (columnName.toLowerCase().equals(SETTINGS_SECTION)) {
        continue;
      }
      PropertySet properties = iniFile.getSection(columnName);
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
      columnBuilder.value(properties.getValue(VALUE_PROPERTY));
    }
    if (properties.contains(IGNORE_FAILURES_PROPERTY)) {
      String ignoreFailuresValue = properties.getValue(IGNORE_FAILURES_PROPERTY);
      boolean ignoresFailure = Boolean.valueOf(ignoreFailuresValue);
      columnBuilder.ignoreFailures(ignoresFailure);
    }
    return columnBuilder.build();
  }

}
