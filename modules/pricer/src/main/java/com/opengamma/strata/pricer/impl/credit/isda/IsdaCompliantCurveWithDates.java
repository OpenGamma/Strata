/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import java.time.LocalDate;

/**
 * 
 */
public interface IsdaCompliantCurveWithDates {

  LocalDate getBaseDate();

  LocalDate getCurveDate(int index);

  LocalDate[] getCurveDates();

  double getZeroRate(LocalDate date);

}
