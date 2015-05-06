package com.opengamma.strata.finance.rate;

import static com.opengamma.strata.basics.index.PriceIndices.CH_CPI;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;

/**
 * Test.
 */
@Test
public class InflationInterpolatedRateObservationTest {

  private static final LocalDate START_DATE = date(2014, 1, 6);
  private static final LocalDate END_DATE = date(2015, 1, 5);

  public void test_of() {
    InflationMonthlyRateObservation test =
        InflationMonthlyRateObservation.of(GB_HICP, START_DATE, END_DATE);
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getReferenceStartDate(), START_DATE);
    assertEquals(test.getReferenceEndDate(), END_DATE);

  }

  public void test_builder() {
    InflationMonthlyRateObservation test = InflationMonthlyRateObservation.builder()
        .index(CH_CPI)
        .referenceStartDate(START_DATE)
        .referenceEndDate(END_DATE)
        .build();
    assertEquals(test.getIndex(), CH_CPI);
    assertEquals(test.getReferenceStartDate(), START_DATE);
    assertEquals(test.getReferenceEndDate(), END_DATE);

  }

  public void test_wrongDates() {
    assertThrowsIllegalArg(() -> InflationMonthlyRateObservation.of(GB_HICP, END_DATE, START_DATE));
  }

  public void test_collectIndices() {
    InflationMonthlyRateObservation test = InflationMonthlyRateObservation.builder()
        .index(CH_CPI)
        .referenceStartDate(START_DATE)
        .referenceEndDate(END_DATE)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(CH_CPI));
  }

  public void coverage() {
    InflationMonthlyRateObservation test1 =
        InflationMonthlyRateObservation.of(GB_HICP, START_DATE, END_DATE);
    coverImmutableBean(test1);
    InflationMonthlyRateObservation test2 =
        InflationMonthlyRateObservation.of(CH_CPI, date(2014, 4, 14), date(2015, 4, 13));
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationMonthlyRateObservation test =
        InflationMonthlyRateObservation.of(GB_HICP, START_DATE, END_DATE);
    assertSerialization(test);
  }
}
