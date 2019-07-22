/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.PeriodAdditionConventions.LAST_BUSINESS_DAY;
import static com.opengamma.strata.basics.date.PeriodAdditionConventions.LAST_DAY;
import static com.opengamma.strata.basics.date.PeriodAdditionConventions.NONE;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.Optional;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Test {@link PeriodAdditionConvention}.
 */
@Test
public class PeriodAdditionConventionTest {

  @DataProvider(name = "types")
  public static Object[][] data_types() {
    StandardPeriodAdditionConventions[] conv = StandardPeriodAdditionConventions.values();
    Object[][] result = new Object[conv.length][];
    for (int i = 0; i < conv.length; i++) {
      result[i] = new Object[] {conv[i]};
    }
    return result;
  }

  @Test(dataProvider = "types")
  public void test_null(PeriodAdditionConvention type) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> type.adjust(null, Period.ofMonths(3), HolidayCalendars.NO_HOLIDAYS));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> type.adjust(date(2014, 7, 11), null, HolidayCalendars.NO_HOLIDAYS));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> type.adjust(date(2014, 7, 11), Period.ofMonths(3), null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> type.adjust(null, null, null));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "convention")
  public static Object[][] data_convention() {
    return new Object[][] {
        // None
        {NONE, date(2014, 7, 11), 1, date(2014, 8, 11)},  // Fri, Mon
        {NONE, date(2014, 7, 31), 1, date(2014, 8, 31)},  // Thu, Sun
        {NONE, date(2014, 6, 30), 2, date(2014, 8, 30)},  // Mon, Sat
        // LastDay
        {LAST_DAY, date(2014, 7, 11), 1, date(2014, 8, 11)},  // Fri, Mon
        {LAST_DAY, date(2014, 7, 31), 1, date(2014, 8, 31)},  // Thu, Sun
        {LAST_DAY, date(2014, 6, 30), 2, date(2014, 8, 31)},  // Mon, Sun
        // LastBusinessDay
        {LAST_BUSINESS_DAY, date(2014, 7, 11), 1, date(2014, 8, 11)},  // Fri, Mon
        {LAST_BUSINESS_DAY, date(2014, 7, 31), 1, date(2014, 8, 29)},  // Thu, Sun to Fri
        {LAST_BUSINESS_DAY, date(2014, 6, 30), 2, date(2014, 8, 29)},  // Mon, Sun to Fri
    };
  }

  @Test(dataProvider = "convention")
  public void test_convention(PeriodAdditionConvention convention, LocalDate input, int months, LocalDate expected) {
    assertEquals(convention.adjust(input, Period.ofMonths(months), HolidayCalendars.SAT_SUN), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {NONE, "None"},
        {LAST_DAY, "LastDay"},
        {LAST_BUSINESS_DAY, "LastBusinessDay"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(PeriodAdditionConvention convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(PeriodAdditionConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(PeriodAdditionConvention convention, String name) {
    assertEquals(PeriodAdditionConvention.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(PeriodAdditionConvention convention, String name) {
    ImmutableMap<String, PeriodAdditionConvention> map = PeriodAdditionConvention.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodAdditionConvention.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> PeriodAdditionConvention.of(null));
  }

  //-------------------------------------------------------------------------
  public void test_lenientLookup_constants() throws IllegalAccessException {
    Field[] fields = PeriodAdditionConventions.class.getDeclaredFields();
    for (Field field : fields) {
      if (Modifier.isPublic(field.getModifiers()) &&
          Modifier.isStatic(field.getModifiers()) &&
          Modifier.isFinal(field.getModifiers())) {

        String name = field.getName();
        Object value = field.get(null);
        ExtendedEnum<PeriodAdditionConvention> ext = PeriodAdditionConvention.extendedEnum();
        assertEquals(ext.findLenient(name), Optional.of(value));
        assertEquals(ext.findLenient(name.toLowerCase(Locale.ENGLISH)), Optional.of(value));
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(PeriodAdditionConventions.class);
    coverEnum(StandardPeriodAdditionConventions.class);
  }

  public void test_serialization() {
    assertSerialization(LAST_DAY);
  }

  public void test_jodaConvert() {
    assertJodaConvert(PeriodAdditionConvention.class, NONE);
    assertJodaConvert(PeriodAdditionConvention.class, LAST_BUSINESS_DAY);
  }

}
