/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard period addition conventions.
 * <p>
 * The purpose of each convention is to define how to handle the addition of a period.
 * The default implementations include two different end-of-month rules.
 * The convention is generally only applicable for month-based periods.
 */
public final class PeriodAdditionConventions {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<PeriodAdditionConvention> ENUM_LOOKUP = ExtendedEnum.of(PeriodAdditionConvention.class);

  /**
   * No specific rule applies.
   * <p>
   * Given a date, the specified period is added using standard date arithmetic.
   * The business day adjustment is applied to produce the final result.
   * <p>
   * For example, adding a period of 1 month to June 30th will result in July 30th.
   */
  public static final PeriodAdditionConvention NONE =
      PeriodAdditionConvention.of(StandardPeriodAdditionConventions.NONE.getName());
  /**
   * Convention applying a last day of month rule, <i>ignoring business days</i>.
   * <p>
   * Given a date, the specified period is added using standard date arithmetic,
   * shifting to the end-of-month if the base date is the last day of the month.
   * The business day adjustment is applied to produce the final result.
   * Note that this rule is based on the last day of the month, not the last business day of the month.
   * <p>
   * For example, adding a period of 1 month to June 30th will result in July 31st.
   */
  public static final PeriodAdditionConvention LAST_DAY =
      PeriodAdditionConvention.of(StandardPeriodAdditionConventions.LAST_DAY.getName());
  /**
   * Convention applying a last <i>business</i> day of month rule.
   * <p>
   * Given a date, the specified period is added using standard date arithmetic,
   * shifting to the last business day of the month if the base date is the
   * last business day of the month.
   * The business day adjustment is applied to produce the final result.
   * <p>
   * For example, adding a period of 1 month to June 29th will result in July 31st
   * assuming that June 30th is not a valid business day and July 31st is.
   */
  public static final PeriodAdditionConvention LAST_BUSINESS_DAY =
      PeriodAdditionConvention.of(StandardPeriodAdditionConventions.LAST_BUSINESS_DAY.getName());

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private PeriodAdditionConventions() {
  }

}
