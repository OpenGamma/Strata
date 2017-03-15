/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.report.framework.format;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;

/**
 * Test {@link AdjustableDateValueFormatter}.
 */
@Test
public class AdjustableDateValueFormatterTest {

  public void formatForDisplay() {
    AdjustableDate date = AdjustableDate.of(date(2016, 6, 30), BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN));
    assertThat(AdjustableDateValueFormatter.INSTANCE.formatForDisplay(date)).isEqualTo("2016-06-30");
  }

  public void formatForCsv() {
    AdjustableDate date = AdjustableDate.of(date(2016, 6, 30), BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN));
    assertThat(AdjustableDateValueFormatter.INSTANCE.formatForCsv(date)).isEqualTo("2016-06-30");
  }
}
