/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link Business252DayCount}.
 */
public class Business252DayCountTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  @Test
  public void test_factory_name() {
    DayCount test = DayCount.of("Bus/252 EUTA");
    assertThat(test.getName()).isEqualTo("Bus/252 EUTA");
    assertThat(test.toString()).isEqualTo("Bus/252 EUTA");

    assertThat(DayCount.of("Bus/252 EUTA")).isSameAs(test);
    assertThat(DayCount.ofBus252(EUTA)).isSameAs(test);
  }

  @Test
  public void test_factory_nameUpper() {
    DayCount test = DayCount.of("BUS/252 EUTA");
    assertThat(test.getName()).isEqualTo("Bus/252 EUTA");
    assertThat(test.toString()).isEqualTo("Bus/252 EUTA");

    assertThat(DayCount.of("Bus/252 EUTA")).isSameAs(test);
    assertThat(DayCount.ofBus252(EUTA)).isSameAs(test);
  }

  @Test
  public void test_factory_calendar() {
    DayCount test = DayCount.ofBus252(GBLO);
    assertThat(test.getName()).isEqualTo("Bus/252 GBLO");
    assertThat(test.toString()).isEqualTo("Bus/252 GBLO");

    assertThat(DayCount.of("Bus/252 GBLO")).isSameAs(test);
    assertThat(DayCount.ofBus252(GBLO)).isSameAs(test);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_yearFraction() {
    DayCount test = DayCount.of("Bus/252 EUTA");
    LocalDate date1 = date(2014, 12, 1);
    LocalDate date2 = date(2014, 12, 1);
    for (int i = 0; i < 366; i++) {
      assertThat(test.yearFraction(date1, date2)).isEqualTo(EUTA.resolve(REF_DATA).daysBetween(date1, date2) / 252d);
      date2 = date2.plusDays(1);
    }
  }

  @Test
  public void test_yearFraction_badOrder() {
    DayCount test = DayCount.of("Bus/252 EUTA");
    LocalDate date1 = date(2014, 12, 2);
    LocalDate date2 = date(2014, 12, 1);
    assertThatIllegalArgumentException().isThrownBy(() -> test.yearFraction(date1, date2));
  }

  @Test
  public void test_days() {
    DayCount test = DayCount.of("Bus/252 EUTA");
    LocalDate date1 = date(2014, 12, 1);
    LocalDate date2 = date(2014, 12, 1);
    for (int i = 0; i < 366; i++) {
      assertThat(test.days(date1, date2)).isEqualTo(EUTA.resolve(REF_DATA).daysBetween(date1, date2));
      date2 = date2.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    DayCount a = DayCount.of("Bus/252 EUTA");
    DayCount b = DayCount.of("Bus/252 GBLO");
    assertThat(a.equals(a)).isEqualTo(true);
    assertThat(a.equals(b)).isEqualTo(false);
    assertThat(a.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(a.equals(null)).isEqualTo(false);
    assertThat(a.hashCode()).isEqualTo(a.hashCode());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(Business252DayCount.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(DayCount.ofBus252(EUTA));
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(DayCount.class, DayCount.ofBus252(EUTA));
  }

}
