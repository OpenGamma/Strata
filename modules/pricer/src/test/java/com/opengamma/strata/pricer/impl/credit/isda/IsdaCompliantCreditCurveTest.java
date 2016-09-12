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

import org.joda.beans.BeanBuilder;
import org.joda.beans.Property;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantDateCreditCurve.Meta;

/**
 * 
 */
@Test
public class IsdaCompliantCreditCurveTest {

  private static final double EPS = 1.e-13;

  /**
   * 
   */
  @Test
  public void buildCreditCurveTest() {
    final double[] time = new double[] {0.1, 0.3, 0.5, 1., 3. };
    final double[] forward = new double[] {0.06, 0.1, 0.05, 0.08, 0.11 };
    final IsdaCompliantCreditCurve cv1 = IsdaCompliantCreditCurve.makeFromForwardRates(time, forward);

    final int num = time.length;
    final double[] rt = new double[num];
    final double[] r = new double[num];
    rt[0] = forward[0] * time[0];
    r[0] = forward[0];
    for (int i = 1; i < num; ++i) {
      rt[i] = rt[i - 1] + forward[i] * (time[i] - time[i - 1]);
      r[i] = rt[i] / time[i];
      assertEquals(r[i], cv1.getHazardRate(time[i]), EPS);
      assertEquals(Math.exp(-rt[i]), cv1.getSurvivalProbability(time[i]), EPS);
    }
    final IsdaCompliantCreditCurve cv2 = IsdaCompliantCreditCurve.makeFromRT(time, rt);
    final IsdaCompliantCreditCurve cv3 = new IsdaCompliantCreditCurve(time, r);

    final double base = 0.05;
    final double[] timeMod = new double[num];
    final double[] rMod = new double[num];
    for (int i = 0; i < num; ++i) {
      timeMod[i] = time[i] + base;
      rMod[i] = (rt[i] + r[0] * base) / timeMod[i];
    }
    final IsdaCompliantCreditCurve cv4 = (new IsdaCompliantCreditCurve(time, rMod)).withRates(r);
    final IsdaCompliantCreditCurve cv1Clone = cv1.clone();
    assertNotSame(cv1, cv1Clone);
    assertEquals(cv1, cv1Clone);
    assertEquals(cv1.toString(), cv1Clone.toString());

    for (int i = 0; i < num; ++i) {
      assertEquals(cv1.getKnotTimes()[i], cv2.getKnotTimes()[i], EPS);
      assertEquals(cv1.getKnotTimes()[i], cv3.getKnotTimes()[i], EPS);
      assertEquals(cv1.getKnotTimes()[i], cv4.getKnotTimes()[i], EPS);
      assertEquals(cv1.getRt()[i], cv2.getRt()[i], EPS);
      assertEquals(cv1.getRt()[i], cv3.getRt()[i], EPS);
      assertEquals(cv1.getRt()[i], cv4.getRt()[i], EPS);
    }

    final IsdaCompliantCreditCurve cvOnePoint = new IsdaCompliantCreditCurve(time[2], r[2]);
    assertEquals(r[2], cvOnePoint.getForwardRate(time[0]));
    assertEquals(r[2], cvOnePoint.getForwardRate(time[4]));

    /*
     * Meta
     */
    final IsdaCompliantCreditCurve.Meta meta = cv1.metaBean();
    final BeanBuilder<?> builder = meta.builder();
    builder.set(meta.metaPropertyGet("metadata"), DefaultCurveMetadata.of("IsdaCompliantCurve"));
    builder.set(meta.metaPropertyGet("t"), time);
    builder.set(meta.metaPropertyGet("rt"), rt);
    IsdaCompliantCreditCurve builtCurve = (IsdaCompliantCreditCurve) builder.build();
    assertEquals(cv1, builtCurve);

    final IsdaCompliantCreditCurve.Meta meta1 = IsdaCompliantCreditCurve.meta();
    assertEquals(meta, meta1);

    /*
     * Error expected
     */
    try {
      final double[] rtshort = Arrays.copyOf(rt, num - 2);
      IsdaCompliantCreditCurve.makeFromRT(time, rtshort);
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }

    /*
     * hashCode and equals
     */
    assertTrue(cv1.equals(cv1));
    assertTrue(!(cv1.equals(null)));

    final IsdaCompliantCurve superCv1 = IsdaCompliantCurve.makeFromForwardRates(time, forward);
    assertTrue(cv1.hashCode() != superCv1.hashCode());
    assertTrue(!(cv1.equals(superCv1)));

  }

  /**
   * 
   */
  @Test
  public void buildDateCurveTest() {
    final LocalDate baseDate = LocalDate.of(2012, 8, 8);
    final LocalDate[] dates = new LocalDate[] {LocalDate.of(2012, 12, 3), LocalDate.of(2013, 4, 29), LocalDate.of(2013, 11, 12), LocalDate.of(2014, 5, 18) };
    final double[] rates = new double[] {0.11, 0.22, 0.15, 0.09 };
    final DayCount dcc = DayCounts.ACT_365F;
    final int num = dates.length;

    final IsdaCompliantDateCreditCurve baseCurve = new IsdaCompliantDateCreditCurve(baseDate, dates, rates);
    final LocalDate[] clonedDates = baseCurve.getCurveDates();
    assertNotSame(dates, clonedDates);
    final int modPosition = 2;
    final IsdaCompliantDateCreditCurve curveWithRate = baseCurve.withRate(rates[modPosition] * 1.5, modPosition);
    final IsdaCompliantDateCreditCurve clonedCurve = baseCurve.clone();
    assertNotSame(baseCurve, clonedCurve);

    final double[] t = new double[num];
    final double[] rt = new double[num];
    for (int i = 0; i < num; ++i) {
      assertEquals(dates[i], baseCurve.getCurveDate(i));
      assertEquals(dates[i], curveWithRate.getCurveDate(i));
      assertEquals(dates[i], clonedDates[i]);
      assertEquals(clonedCurve.getCurveDate(i), baseCurve.getCurveDate(i));
      if (i == modPosition) {
        assertEquals(rates[i] * 1.5, curveWithRate.getZeroRateAtIndex(i));
      }
      t[i] = dcc.yearFraction(baseDate, dates[i]);
      rt[i] = t[i] * rates[i];
    }

    final LocalDate[] sampleDates = new LocalDate[] {baseDate.plusDays(2), LocalDate.of(2013, 7, 5), dates[2] };
    final int nSampleDates = sampleDates.length;
    final double[] sampleRates = new double[nSampleDates];
    final double[] fracs = new double[nSampleDates];
    for (int i = 0; i < nSampleDates; ++i) {
      fracs[i] = dcc.yearFraction(baseDate, sampleDates[i]);
      sampleRates[i] = baseCurve.getZeroRate(sampleDates[i]);
    }
    assertEquals(rates[0], sampleRates[0]);
    assertEquals((rt[2] * (fracs[1] - t[1]) + rt[1] * (t[2] - fracs[1])) / (t[2] - t[1]) / fracs[1], sampleRates[1]);
    assertEquals(rates[2], sampleRates[2]);

    assertEquals(baseDate, baseCurve.getBaseDate());

    /*
     * Test meta
     */
    final Property<LocalDate> propBaseDate = baseCurve.baseDate();
    final Property<LocalDate[]> propDates = baseCurve.dates();
    final Property<DayCount> propDcc = baseCurve.dayCount();

    final Meta meta = baseCurve.metaBean();
    final BeanBuilder<?> builder = meta.builder();
    builder.set(propBaseDate.name(), baseDate);
    builder.set(propDates.name(), dates);
    builder.set(propDcc.name(), dcc);
    builder.set(meta.metaPropertyGet("metadata"), DefaultCurveMetadata.of("IsdaCompliantCurve"));
    builder.set(meta.metaPropertyGet("t"), t);
    builder.set(meta.metaPropertyGet("rt"), rt);
    IsdaCompliantDateCreditCurve builtCurve = (IsdaCompliantDateCreditCurve) builder.build();
    assertEquals(baseCurve, builtCurve);

    final Meta meta1 = IsdaCompliantDateCreditCurve.meta();
    assertEquals(meta1, meta);

    /*
     * hash and equals 
     */
    assertTrue(!(baseCurve.equals(null)));
    assertTrue(!(baseCurve.equals(new IsdaCompliantDateCurve(baseDate, dates, rates))));
    assertTrue(!(baseCurve.equals(new IsdaCompliantDateCreditCurve(baseDate.minusDays(1), dates, rates))));
    assertTrue(!(baseCurve.equals(new IsdaCompliantDateCreditCurve(baseDate, new LocalDate[] {LocalDate.of(2012, 12, 3), LocalDate.of(2013, 4, 29), LocalDate.of(2013, 11, 12),
      LocalDate.of(2014, 5, 19) }, rates))));
    assertTrue(!(baseCurve.equals(new IsdaCompliantDateCreditCurve(baseDate, dates, rates, DayCounts.ACT_365_25))));

    assertTrue(baseCurve.equals(baseCurve));

    assertTrue(baseCurve.hashCode() != curveWithRate.hashCode());
    assertTrue(!(baseCurve.equals(curveWithRate)));

    /*
     * String
     */
    assertEquals(baseCurve.toString(), clonedCurve.toString());
  }
}
