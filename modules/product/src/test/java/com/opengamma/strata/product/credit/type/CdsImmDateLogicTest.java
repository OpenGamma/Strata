/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.Test;

/**
 * Test {@link CdsImmDateLogic}.
 */
public class CdsImmDateLogicTest {

  @Test
  public void onImmDateTest() {
    LocalDate today = LocalDate.of(2013, Month.MARCH, 20);
    LocalDate prevIMM = CdsImmDateLogic.getPreviousImmDate(today);
    assertThat(prevIMM).isEqualTo(LocalDate.of(2012, Month.DECEMBER, 20));

    today = LocalDate.of(2017, Month.JUNE, 20);
    prevIMM = CdsImmDateLogic.getPreviousImmDate(today);
    assertThat(LocalDate.of(2017, Month.MARCH, 20)).isEqualTo(prevIMM);

    today = LocalDate.of(2011, Month.SEPTEMBER, 20);
    prevIMM = CdsImmDateLogic.getPreviousImmDate(today);
    assertThat(LocalDate.of(2011, Month.JUNE, 20)).isEqualTo(prevIMM);

    today = LocalDate.of(2015, Month.DECEMBER, 20);
    prevIMM = CdsImmDateLogic.getPreviousImmDate(today);
    assertThat(LocalDate.of(2015, Month.SEPTEMBER, 20)).isEqualTo(prevIMM);
  }

  @Test
  public void previousImmTest() {
    LocalDate today = LocalDate.of(2011, Month.JUNE, 21);
    LocalDate prevIMM = CdsImmDateLogic.getPreviousImmDate(today);
    assertThat(LocalDate.of(2011, Month.JUNE, 20)).isEqualTo(prevIMM);

    prevIMM = CdsImmDateLogic.getPreviousImmDate(CdsImmDateLogic.getPreviousImmDate(prevIMM));
    assertThat(LocalDate.of(2010, Month.DECEMBER, 20)).isEqualTo(prevIMM);

    today = LocalDate.of(2011, Month.JUNE, 18);
    prevIMM = CdsImmDateLogic.getPreviousImmDate(today);
    assertThat(LocalDate.of(2011, Month.MARCH, 20)).isEqualTo(prevIMM);

    today = LocalDate.of(1976, Month.JULY, 30);
    prevIMM = CdsImmDateLogic.getPreviousImmDate(today);
    assertThat(LocalDate.of(1976, Month.JUNE, 20)).isEqualTo(prevIMM);

    today = LocalDate.of(1977, Month.FEBRUARY, 13);
    prevIMM = CdsImmDateLogic.getPreviousImmDate(today);
    assertThat(LocalDate.of(1976, Month.DECEMBER, 20)).isEqualTo(prevIMM);

    today = LocalDate.of(2013, Month.MARCH, 1);
    prevIMM = CdsImmDateLogic.getPreviousImmDate(today);
    assertThat(LocalDate.of(2012, Month.DECEMBER, 20)).isEqualTo(prevIMM);
  }

  @Test
  public void isSemiAnnualRollDateTest() {
    LocalDate date0 = LocalDate.of(2013, 3, 14);
    LocalDate date1 = LocalDate.of(2013, 6, 20);
    LocalDate date2 = LocalDate.of(2013, 3, 20);
    LocalDate date3 = LocalDate.of(2013, 9, 20);
    assertThat(CdsImmDateLogic.isSemiAnnualRollDate(date0)).isFalse();
    assertThat(CdsImmDateLogic.isSemiAnnualRollDate(date1)).isFalse();
    assertThat(CdsImmDateLogic.isSemiAnnualRollDate(date2)).isTrue();
    assertThat(CdsImmDateLogic.isSemiAnnualRollDate(date3)).isTrue();
  }

  @Test
  public void getNextSemiAnnualRollDateTest() {
    LocalDate[] dates = new LocalDate[] {LocalDate.of(2013, 3, 14), LocalDate.of(2013, 6, 20), LocalDate.of(2013, 3, 20),
        LocalDate.of(2013, 9, 20), LocalDate.of(2013, 1, 21),
        LocalDate.of(2013, 3, 21), LocalDate.of(2013, 9, 19), LocalDate.of(2013, 9, 21), LocalDate.of(2013, 11, 21)};
    LocalDate[] datesExp = new LocalDate[] {LocalDate.of(2013, 3, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2013, 9, 20),
        LocalDate.of(2014, 3, 20), LocalDate.of(2013, 3, 20),
        LocalDate.of(2013, 9, 20), LocalDate.of(2013, 9, 20), LocalDate.of(2014, 3, 20), LocalDate.of(2014, 3, 20)};
    for (int i = 0; i < dates.length; ++i) {
      assertThat(CdsImmDateLogic.getNextSemiAnnualRollDate(dates[i])).isEqualTo(datesExp[i]);
    }
  }

}
