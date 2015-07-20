/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.report;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;
import com.opengamma.strata.collect.Messages;

/**
 * Parameter converter for {@link LocalDate}.
 */
public class LocalDateParameterConverter
    implements IStringConverter<LocalDate> {

  @Override
  public LocalDate convert(String value) {
    try {
      return LocalDate.parse(value);

    } catch (DateTimeParseException ex) {
      throw new ParameterException(Messages.format("Invalid valuation date: {}", value));
    } catch (RuntimeException ex) {
      throw new ParameterException(
          Messages.format("Invalid valuation date: {}" +
              System.lineSeparator() + "Exception: {}", value, ex.getMessage()));
    }
  }

}
