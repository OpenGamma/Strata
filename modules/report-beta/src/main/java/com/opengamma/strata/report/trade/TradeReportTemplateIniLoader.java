/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.report.trade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.report.ReportTemplateIniLoader;

/**
 * Loader for trade report templates in the standard INI file format.
 */
public class TradeReportTemplateIniLoader implements ReportTemplateIniLoader<TradeReportTemplate> {

  private static final String VALUE_PROPERTY = "value";
  private static final String IGNORE_FAILURES_PROPERTY = "ignoreFailures";
  private static final String PATH_SEPARATOR = "\\.";

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

    String measureText;
    try {
      measureText = properties.getValue(VALUE_PROPERTY);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          Messages.format("Report template for column '{}' does not contain required property '{}'", columnName, VALUE_PROPERTY));
    }
    String[] measureBits = measureText.split(PATH_SEPARATOR);
    List<String> measurePath = Arrays.stream(measureBits).collect(Collectors.toList());
    String measureName = measurePath.remove(0);
    try {
      Measure measure = Measure.of(measureName);
      columnBuilder.measure(measure);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          Messages.format("Report template for column '{}' contains invalid measure name '{}'", columnName, measureText));
    }
    if (!measurePath.isEmpty()) {
      columnBuilder.path(measurePath);
    }
    if (properties.contains(IGNORE_FAILURES_PROPERTY)) {
      String ignoreFailureValue = properties.getValue(IGNORE_FAILURES_PROPERTY);
      boolean ignoreFailure = Boolean.valueOf(ignoreFailureValue);
      columnBuilder.ignoreFailure(ignoreFailure);
    }
    return columnBuilder.build();
  }

}
