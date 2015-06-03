/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import java.time.LocalDate;

import com.beust.jcommander.IStringConverter;

/**
 * Parameter converter for {@link LocalDate}.
 */
public class LocalDateParameterConverter implements IStringConverter<LocalDate> {

  @Override
  public LocalDate convert(String value) {
    return LocalDate.parse(value);
  }

}
