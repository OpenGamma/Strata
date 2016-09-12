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
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantDateYieldCurve.Meta;

/**
 * Test {@link IsdaCompliantYieldCurve}.
 */
@Test
public class IsdaCompliantYieldCurveTest {

  private static final double EPS = 1.e-13;

  //-------------------------------------------------------------------------
  public void buildYieldCurveTest() {
    double[] time = new double[] {0.1, 0.3, 0.5, 1., 3.};
    double[] forward = new double[] {0.06, 0.1, 0.05, 0.08, 0.11};
    IsdaCompliantYieldCurve cv1 = IsdaCompliantYieldCurve.makeFromForwardRates(time, forward);

    int num = time.length;
    double[] rt = new double[num];
    double[] r = new double[num];
    rt[0] = forward[0] * time[0];
    r[0] = forward[0];
    for (int i = 1; i < num; ++i) {
      rt[i] = rt[i - 1] + forward[i] * (time[i] - time[i - 1]);
      r[i] = rt[i] / time[i];
    }
    IsdaCompliantYieldCurve cv2 = IsdaCompliantYieldCurve.makeFromRT(time, rt);
    IsdaCompliantYieldCurve cv3 = new IsdaCompliantYieldCurve(time, r);

    double base = 0.05;
    double[] timeMod = new double[num];
    double[] rMod = new double[num];
    for (int i = 0; i < num; ++i) {
      timeMod[i] = time[i] + base;
      rMod[i] = (rt[i] + r[0] * base) / timeMod[i];
    }
    IsdaCompliantYieldCurve cvWithBase1 = new IsdaCompliantYieldCurve(timeMod, rMod, base);
    IsdaCompliantYieldCurve cvWithBase2 = (new IsdaCompliantYieldCurve(timeMod, rMod)).withOffset(base);
    IsdaCompliantYieldCurve cv4 = (new IsdaCompliantYieldCurve(time, rMod)).withRates(r);
    IsdaCompliantYieldCurve cv1Clone = cv1.clone();
    assertNotSame(cv1, cv1Clone);
    assertEquals(cv1, cv1Clone);
    assertEquals(cv1.toString(), cv1Clone.toString());

    // forward rate curve shifted
    assertEquals(cv1.getForwardRate(0.32), cv1.withOffset(base).getForwardRate(0.32 - base), EPS);
    assertEquals(
        cv1.getDiscountFactor(0.32) / cv1.getDiscountFactor(base),
        cv1.withOffset(base).getDiscountFactor(0.32 - base),
        EPS);

    for (int i = 0; i < num; ++i) {
      assertEquals(cv1.getKnotTimes()[i], cv2.getKnotTimes()[i], EPS);
      assertEquals(cv1.getKnotTimes()[i], cv3.getKnotTimes()[i], EPS);
      assertEquals(cv1.getKnotTimes()[i], cvWithBase1.getKnotTimes()[i], EPS);
      assertEquals(cv1.getKnotTimes()[i], cvWithBase2.getKnotTimes()[i], EPS);
      assertEquals(cv1.getKnotTimes()[i], cv4.getKnotTimes()[i], EPS);
      assertEquals(cv1.getRt()[i], cv2.getRt()[i], EPS);
      assertEquals(cv1.getRt()[i], cv3.getRt()[i], EPS);
      assertEquals(cv1.getRt()[i], cvWithBase1.getRt()[i], EPS);
      assertEquals(cv1.getRt()[i], cvWithBase2.getRt()[i], EPS);
      assertEquals(cv1.getRt()[i], cv4.getRt()[i], EPS);
    }

    IsdaCompliantYieldCurve cvOnePoint = new IsdaCompliantYieldCurve(time[2], r[2]);
    assertEquals(r[2], cvOnePoint.getForwardRate(time[0]));
    assertEquals(r[2], cvOnePoint.getForwardRate(time[4]));

    // meta
    IsdaCompliantYieldCurve.Meta meta = cv1.metaBean();
    BeanBuilder<?> builder = meta.builder();
    builder.set(meta.metaPropertyGet("metadata"), DefaultCurveMetadata.of("IsdaCompliantCurve"));
    builder.set(meta.metaPropertyGet("t"), time);
    builder.set(meta.metaPropertyGet("rt"), rt);
    IsdaCompliantYieldCurve builtCurve = (IsdaCompliantYieldCurve) builder.build();
    assertEquals(cv1, builtCurve);

    IsdaCompliantYieldCurve.Meta meta1 = IsdaCompliantYieldCurve.meta();
    assertEquals(meta, meta1);

    // error expected
    try {
      double[] rtshort = Arrays.copyOf(rt, num - 2);
      IsdaCompliantYieldCurve.makeFromRT(time, rtshort);
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }

    // hashCode and equals
    assertTrue(cv1.equals(cv1));
    assertTrue(!(cv1.equals(null)));

    IsdaCompliantCurve superCv1 = IsdaCompliantCurve.makeFromForwardRates(time, forward);
    assertTrue(cv1.hashCode() != superCv1.hashCode());
    assertTrue(!(cv1.equals(superCv1)));
  }

  //-------------------------------------------------------------------------
  public void buildDateYieldCurveTest() {
    LocalDate baseDate = LocalDate.of(2012, 8, 8);
    LocalDate[] dates = new LocalDate[] {
        LocalDate.of(2012, 12, 3), LocalDate.of(2013, 4, 29), LocalDate.of(2013, 11, 12), LocalDate.of(2014, 5, 18)};
    double[] rates = new double[] {0.11, 0.22, 0.15, 0.09};
    DayCount dcc = DayCounts.ACT_365F;
    int num = dates.length;

    IsdaCompliantDateYieldCurve baseCurve = new IsdaCompliantDateYieldCurve(baseDate, dates, rates);
    LocalDate[] clonedDates = baseCurve.getCurveDates();
    assertNotSame(dates, clonedDates);
    int modPosition = 2;
    IsdaCompliantDateYieldCurve curveWithRate = baseCurve.withRate(rates[modPosition] * 1.5, modPosition);
    IsdaCompliantDateYieldCurve clonedCurve = baseCurve.clone();
    assertNotSame(baseCurve, clonedCurve);

    double[] t = new double[num];
    double[] rt = new double[num];
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

    LocalDate[] sampleDates = new LocalDate[] {baseDate.plusDays(2), LocalDate.of(2013, 7, 5), dates[2]};
    int nSampleDates = sampleDates.length;
    double[] sampleRates = new double[nSampleDates];
    double[] fracs = new double[nSampleDates];
    for (int i = 0; i < nSampleDates; ++i) {
      fracs[i] = dcc.yearFraction(baseDate, sampleDates[i]);
      sampleRates[i] = baseCurve.getZeroRate(sampleDates[i]);
    }
    assertEquals(rates[0], sampleRates[0]);
    assertEquals((rt[2] * (fracs[1] - t[1]) + rt[1] * (t[2] - fracs[1])) / (t[2] - t[1]) / fracs[1], sampleRates[1]);
    assertEquals(rates[2], sampleRates[2]);

    assertEquals(baseDate, baseCurve.getBaseDate());

    // meta
    Property<LocalDate> propBaseDate = baseCurve.baseDate();
    Property<LocalDate[]> propDates = baseCurve.dates();
    Property<DayCount> propDcc = baseCurve.dayCount();

    Meta meta = baseCurve.metaBean();
    BeanBuilder<?> builder = meta.builder();
    builder.set(propBaseDate.name(), baseDate);
    builder.set(propDates.name(), dates);
    builder.set(propDcc.name(), dcc);
    builder.set(meta.metaPropertyGet("metadata"), DefaultCurveMetadata.of("IsdaCompliantCurve"));
    builder.set(meta.metaPropertyGet("t"), t);
    builder.set(meta.metaPropertyGet("rt"), rt);
    IsdaCompliantDateYieldCurve builtCurve = (IsdaCompliantDateYieldCurve) builder.build();
    assertEquals(baseCurve, builtCurve);

    Meta meta1 = IsdaCompliantDateYieldCurve.meta();
    assertEquals(meta1, meta);

    // hash and equals 
    assertTrue(!(baseCurve.equals(null)));
    assertTrue(!(baseCurve.equals(new IsdaCompliantDateCurve(baseDate, dates, rates))));
    assertTrue(!(baseCurve.equals(new IsdaCompliantDateYieldCurve(baseDate.minusDays(1), dates, rates))));
    assertTrue(!(baseCurve.equals(new IsdaCompliantDateYieldCurve(baseDate, new LocalDate[] {LocalDate.of(2012, 12, 3),
        LocalDate.of(2013, 4, 29), LocalDate.of(2013, 11, 12),
        LocalDate.of(2014, 5, 19)}, rates))));
    assertTrue(!(baseCurve.equals(new IsdaCompliantDateYieldCurve(baseDate, dates, rates, DayCounts.ACT_365_25))));

    assertTrue(baseCurve.equals(baseCurve));

    assertTrue(baseCurve.hashCode() != curveWithRate.hashCode());
    assertTrue(!(baseCurve.equals(curveWithRate)));

    // toString
    assertEquals(baseCurve.toString(), clonedCurve.toString());
  }

}
