/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import java.util.Locale;

import com.beust.jcommander.IStringConverter;
import com.opengamma.strata.report.framework.format.ReportOutputFormat;

/**
 * Parameter converter for {@link ReportOutputFormat}.
 * <p>
 * This parses the input leniently.
 */
public class ReportOutputFormatParameterConverter
    implements IStringConverter<ReportOutputFormat> {

  @Override
  public ReportOutputFormat convert(String value) {
    if (value.toLowerCase(Locale.ENGLISH).startsWith("c")) {
      return ReportOutputFormat.CSV;
    }
    return ReportOutputFormat.ASCII_TABLE;
  }

}
