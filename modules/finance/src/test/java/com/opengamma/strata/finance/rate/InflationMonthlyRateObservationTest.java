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
public class InflationMonthlyRateObservationTest {
  private static final LocalDate[] START_DATES = new LocalDate[] {date(2014, 1, 6), date(2014, 2, 5) };
  private static final LocalDate[] END_DATES = new LocalDate[] {date(2015, 1, 5), date(2015, 2, 5) };
  private static final double WEIGHT = 1.0 - 6.0 / 31.0;

  public void test_of() {
    InflationInterpolatedRateObservation test =
        InflationInterpolatedRateObservation.of(GB_HICP, START_DATES, END_DATES, WEIGHT);
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getReferenceStartDates(), START_DATES);
    assertEquals(test.getReferenceEndDates(), END_DATES);
    assertEquals(test.getWeight(), WEIGHT, 1.0e-14);
  }

  public void test_builder() {
    InflationInterpolatedRateObservation test = InflationInterpolatedRateObservation.builder()
        .index(CH_CPI)
        .referenceStartDates(START_DATES)
        .referenceEndDates(END_DATES)
        .weight(WEIGHT)
        .build();
    assertEquals(test.getIndex(), CH_CPI);
    assertEquals(test.getReferenceStartDates(), START_DATES);
    assertEquals(test.getReferenceEndDates(), END_DATES);
    assertEquals(test.getWeight(), WEIGHT, 1.0e-14);
  }

  public void test_wrong_datesLength1() {
    assertThrowsIllegalArg(() -> InflationInterpolatedRateObservation.of(GB_HICP, START_DATES,
        new LocalDate[] {date(2015, 1, 5), date(2015, 2, 5), date(2016, 2, 5) }, WEIGHT));
  }

  public void test_wrong_datesLength2() {
    assertThrowsIllegalArg(() -> InflationInterpolatedRateObservation.builder()
        .index(CH_CPI)
        .referenceStartDates(new LocalDate[] {date(2014, 1, 6), date(2014, 2, 5), date(2014, 3, 5) })
        .referenceEndDates(END_DATES)
        .weight(WEIGHT)
        .build());
  }

  public void test_wrong_datesOrder() {
    assertThrowsIllegalArg(() -> InflationInterpolatedRateObservation.of(
        GB_HICP, new LocalDate[] {date(2014, 1, 6), date(2014, 2, 5) },
        new LocalDate[] {date(2013, 1, 5), date(2013, 2, 5) }, WEIGHT));
  }

  public void test_collectIndices() {
    InflationInterpolatedRateObservation test = InflationInterpolatedRateObservation.builder()
        .index(CH_CPI)
        .referenceStartDates(START_DATES)
        .referenceEndDates(END_DATES)
        .weight(WEIGHT)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(CH_CPI));
  }

  public void coverage() {
    InflationInterpolatedRateObservation test1 =
        InflationInterpolatedRateObservation.of(GB_HICP, START_DATES, END_DATES, WEIGHT);
    coverImmutableBean(test1);
    InflationInterpolatedRateObservation test2 =
        InflationInterpolatedRateObservation.of(CH_CPI, START_DATES, END_DATES, WEIGHT);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationInterpolatedRateObservation test =
        InflationInterpolatedRateObservation.of(GB_HICP, START_DATES, END_DATES, WEIGHT);
    assertSerialization(test);
  }
}
