/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

import org.joda.beans.BeanBuilder;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantDateCurve.Meta;

/**
 * Test {@link IsdaCompliantDateCurve}.
 */
@Test
public class IsdaCompliantDateCurveTest {

  @SuppressWarnings("unused")
  public void cloneAndMetaTest() {
    double tol = 1.e-12;

    LocalDate baseDate = LocalDate.of(2013, 2, 3);
    LocalDate[] dates = new LocalDate[] {
        LocalDate.of(2013, 5, 14), LocalDate.of(2013, 9, 13), LocalDate.of(2013, 9, 14), LocalDate.of(2014, 1, 23)};
    double[] rates = new double[] {0.05, 0.06, 0.06, 0.04};
    int num = dates.length;
    DayCount dcc = DayCounts.ACT_365F;

    IsdaCompliantDateCurve curve365 = new IsdaCompliantDateCurve(baseDate, dates, rates);

    LocalDate[] clonedDates = curve365.getCurveDates();
    int modPosition = num - 2;
    IsdaCompliantDateCurve rateModCurve = curve365.withRate(rates[modPosition] * 2., modPosition);

    double[] tt = new double[num];
    double[] rtt = new double[num];
    for (int i = 0; i < num; ++i) {
      assertEquals(0, clonedDates[i].compareTo(dates[i]));
      assertEquals(0, curve365.getCurveDate(i).compareTo(dates[i]));
      if (i == modPosition) {
        assertEquals(rates[i] * 2., rateModCurve.getYValues()[i]);
      } else {
        assertEquals(rates[i], rateModCurve.getYValues()[i]);
      }
      tt[i] = dcc.yearFraction(baseDate, dates[i]);
      rtt[i] = tt[i] * rates[i];
    }
    assertNotSame(clonedDates, dates);

    LocalDate[] sampleDates =
        new LocalDate[] {LocalDate.of(2013, 2, 13), LocalDate.of(2013, 11, 19), LocalDate.of(2014, 1, 23)};
    assertEquals(rates[0], curve365.getZeroRate(sampleDates[0]));
    double t = dcc.yearFraction(baseDate, sampleDates[1]);
    double refT2 = dcc.yearFraction(baseDate, dates[2]);
    double refT3 = dcc.yearFraction(baseDate, dates[3]);
    assertEquals((rates[2] * refT2 * (refT3 - t) + rates[3] * refT3 * (t - refT2)) / (refT3 - refT2) / t,
        curve365.getZeroRate(sampleDates[1]), tol);
    assertEquals(rates[3], curve365.getZeroRate(sampleDates[2]));

    assertEquals(baseDate, curve365.getBaseDate());
    Property<LocalDate> propBase = curve365.baseDate();
    assertEquals(baseDate, propBase.get());
    Property<LocalDate[]> propDates = curve365.dates();
    for (int i = 0; i < num; ++i) {
      assertEquals(dates[i], propDates.get()[i]);
    }
    Property<DayCount> propDcc = curve365.dayCount();
    assertEquals(dcc, propDcc.get());

    IsdaCompliantDateCurve clonedCurve = curve365.clone();
    assertNotSame(clonedCurve, curve365);
    assertTrue(clonedCurve.equals(curve365));
    assertEquals(clonedCurve.hashCode(), curve365.hashCode());
    assertEquals(clonedCurve.toString(), curve365.toString());

    Meta meta = IsdaCompliantDateCurve.meta();
    Meta metafb = curve365.metaBean();
    assertEquals(meta, metafb);
    BeanBuilder<?> builder = meta.builder();
    Map<String, MetaProperty<?>> map = meta.metaPropertyMap();

    MetaProperty<LocalDate> propBaseDate = meta.baseDate();
    MetaProperty<LocalDate[]> metaDates = meta.dates();
    MetaProperty<DayCount> metaDcc = meta.dayCount();

    builder.set(propBaseDate.name(), baseDate);
    builder.set(propDates.name(), dates);
    builder.set(propDcc.name(), dcc);
    builder.set(meta.metaPropertyGet("metadata"), DefaultCurveMetadata.of("IsdaCompliantCurve"));
    builder.set(meta.metaPropertyGet("t"), tt);
    builder.set(meta.metaPropertyGet("rt"), rtt);
    IsdaCompliantDateCurve builtCurve = (IsdaCompliantDateCurve) builder.build();
    assertEquals(curve365, builtCurve);

    // errors expected
    try {
      double[] ratesShort = Arrays.copyOf(rates, num - 1);
      new IsdaCompliantDateCurve(baseDate, dates, ratesShort);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      dates[1] = LocalDate.of(2015, 1, 22);
      new IsdaCompliantDateCurve(baseDate, dates, rates);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
  }

  //-------------------------------------------------------------------------
  public void hashEqualsTest() {
    LocalDate baseDate = LocalDate.of(2009, 2, 3);
    LocalDate[] dates = new LocalDate[] {
        LocalDate.of(2012, 5, 14), LocalDate.of(2013, 6, 13), LocalDate.of(2013, 9, 14)};
    double[] rates = new double[] {0.05, 0.06, 0.04};
    DayCount dcc = DayCounts.ACT_360;

    IsdaCompliantDateCurve curve360 = new IsdaCompliantDateCurve(baseDate, dates, rates, dcc);
    IsdaCompliantDateCurve curve365 = new IsdaCompliantDateCurve(baseDate, dates, rates);
    IsdaCompliantDateCurve curve360a = new IsdaCompliantDateCurve(baseDate, new LocalDate[] {
        LocalDate.of(2012, 5, 14), LocalDate.of(2013, 7, 13), LocalDate.of(2013, 9, 14)}, rates, dcc);
    IsdaCompliantDateCurve curve360b = new IsdaCompliantDateCurve(baseDate.minusDays(6), dates, rates, dcc);
    IsdaCompliantDateCurve curve360c = new IsdaCompliantDateCurve(baseDate, dates, new double[] {0.05, 0.05, 0.04}, dcc);
    assertTrue(curve360.equals(curve360));

    assertTrue(curve360.hashCode() != curve365.hashCode());
    assertTrue(!(curve360.equals(curve365)));

    assertTrue(curve360.hashCode() != curve360a.hashCode());
    assertTrue(!(curve360.equals(curve360a)));

    assertTrue(curve360.hashCode() != curve360b.hashCode());
    assertTrue(!(curve360.equals(curve360b)));

    assertTrue(curve360.hashCode() != curve360c.hashCode());
    assertTrue(!(curve360.equals(curve360c)));

    assertTrue(!(curve360.equals(null)));
    assertTrue(!(curve360.equals(new IsdaCompliantDateYieldCurve(baseDate, dates, rates, dcc))));
  }

}
