/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import com.beust.jcommander.IStringConverter;

/**
 * Parameter converter for {@link ReportOutputType}.
 */
public class ReportOutputTypeParameterConverter implements IStringConverter<ReportOutputType> {

  @Override
  public ReportOutputType convert(String value) {
    if (value.toLowerCase().startsWith("c")) {
      return ReportOutputType.CSV;
    }
    return ReportOutputType.ASCII_TABLE;
  }

}
