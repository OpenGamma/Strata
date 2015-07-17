/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class IsdaCompliantCurveTest {
  private static final double EPS = 1e-5;

  /**
   * no shift
   */
  public void noShiftTest() {
    final double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final double offset = 0.0;
    final IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    final IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(9, offsetCurve.getNumberOfKnots());
    final double rtOffset = offset * r[0];
    for (int i = 0; i < 100; i++) {
      final double time = 3.5 * i / 100.0 + offset;
      final double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      final double rt2 = offsetCurve.getRT(time);
      assertEquals(rt1, rt2);
    }
  }

  /**
   * Shift less than first knot
   */
  public void baseShiftTest() {
    final double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final double offset = 0.01;
    final IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    final IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(9, offsetCurve.getNumberOfKnots());
    final double rtOffset = offset * r[0];
    for (int i = 0; i < 100; i++) {
      final double time = 3.5 * i / 100.0 + offset;
      final double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      final double rt2 = offsetCurve.getRT(time);
      assertEquals(rt1, rt2, 1e-15);
    }
  }

  /**
   * shift between two knots
   */
  public void baseShiftTest2() {
    final double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final double offset = 0.3;
    final IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    final IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(6, offsetCurve.getNumberOfKnots());
    final double rtOffset = baseCurve.getRT(offset);
    for (int i = 0; i < 100; i++) {
      final double time = 4.0 * i / 100.;
      final double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      final double rt2 = offsetCurve.getRT(time);

      assertEquals(rt1, rt2, 1e-15);
    }
  }

  /**
   * shift to just before last knot and extrapolate at long way out 
   */
  public void baseShiftTest3() {
    final double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final double offset = 3.3;
    final IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    final IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(1, offsetCurve.getNumberOfKnots());
    final double rtOffset = baseCurve.getRT(offset);
    for (int i = 0; i < 100; i++) {
      final double time = 5.0 * i / 100.;
      final double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      final double rt2 = offsetCurve.getRT(time);

      assertEquals(rt1, rt2, 1e-14);
    }
  }

  /**
   * shift exactly to one of the knots
   */
  public void baseShiftTest4() {
    final double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 0.5, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final double offset = 0.5;
    final IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    final IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(5, offsetCurve.getNumberOfKnots());
    final double rtOffset = baseCurve.getRT(offset);
    for (int i = 0; i < 100; i++) {
      final double time = 4.0 * i / 100.;
      final double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      final double rt2 = offsetCurve.getRT(time);

      assertEquals(rt1, rt2, 1e-14);
    }
  }

  /**
   * shift to last knot 
   */
  public void baseShiftTest5() {
    final double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4 };
    final double[] r = new double[] {0.4, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.5 };
    final double offset = 3.4;
    final IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    final IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(1, offsetCurve.getNumberOfKnots());
    final double rtOffset = baseCurve.getRT(offset);
    for (int i = 0; i < 100; i++) {
      final double time = 4.0 * i / 100.;
      final double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      final double rt2 = offsetCurve.getRT(time);

      assertEquals(rt1, rt2, 1e-14);
    }
  }

  /**
   * shift past last knot 
   */
  public void baseShiftTest6() {
    final double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final double offset = 3.5;
    final IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    final IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(1, offsetCurve.getNumberOfKnots());
    final double rtOffset = baseCurve.getRT(offset);
    for (int i = 0; i < 100; i++) {
      final double time = 4.0 * i / 100.;
      final double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      final double rt2 = offsetCurve.getRT(time);

      assertEquals(rt1, rt2, 1e-14);
    }
  }

  /**
   * shift to first knot 
   */
  public void baseShiftTest7() {
    final double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4 };
    final double[] r = new double[] {0.4, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.5 };
    final double offset = 0.03;
    final IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    final IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(8, offsetCurve.getNumberOfKnots());
    final double rtOffset = baseCurve.getRT(offset);
    for (int i = 0; i < 100; i++) {
      final double time = 4.0 * i / 100.;
      final double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      final double rt2 = offsetCurve.getRT(time);

      assertEquals(rt1, rt2, 1e-15);
    }
  }

  public void getRTTest() {
    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final int n = t.length;
    final double[] rt = new double[n];
    for (int i = 0; i < n; i++) {

      rt[i] = r[i] * t[i];
    }
    final IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);

    double rTCalculatedi = 0.0;
    double ti = 0.0;
    final int iterationMax = 1000000;
    for (int i = 0; i < iterationMax; i++) {
      ti = ti + i / iterationMax * 100;
      if (ti <= t[0]) {

        rTCalculatedi = ti * r[0];
      }

      else if (ti >= t[t.length - 1]) {
        rTCalculatedi = ti * r[t.length - 1];
      } else {
        int indexpointi = Arrays.binarySearch(t, ti);
        if (indexpointi >= 0) {
          rTCalculatedi = t[indexpointi] * r[indexpointi];
        } else {
          indexpointi = -(1 + indexpointi);
          if (indexpointi == 0) {
            rTCalculatedi = ti * r[0];
          } else if (indexpointi == n) {
            rTCalculatedi = ti * r[n - 1];
          } else {
            final double t1 = t[indexpointi - 1];
            final double t2 = t[indexpointi];
            final double dt = t2 - t1;
            rTCalculatedi = ((t2 - ti) * r[indexpointi - 1] * t[indexpointi - 1] + (ti - t1) * r[indexpointi] * t[indexpointi]) / dt;
          }
        }
      }
      final double rTexpectedi = curve.getRT(ti);
      assertEquals("Time: " + ti, rTexpectedi, rTCalculatedi, 1e-10);
    }
  }

  public void rtandSenseTest() {
    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final int n = t.length;
    final IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);

    for (int i = 0; i < 100; i++) {
      final double tt = i * 12.0 / 100;
      final double rt1 = curve.getRT(tt);
      final double[] fdSense = fdRTSense(curve, tt);
      for (int jj = 0; jj < n; jj++) {
        final double[] rtandSense = curve.getRTandSensitivity(tt, jj);
        assertEquals("rt " + tt, rt1, rtandSense[0], 1e-14);
        assertEquals("sense " + tt + "\t" + jj, fdSense[jj], rtandSense[1], 1e-9);
      }
    }
  }

  public void getNodeSensitivity() {

    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final int n = t.length;
    final double[] rt = new double[n];
    for (int i = 0; i < n; i++) {

      rt[i] = r[i] * t[i];
    }
    final IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);

    final double[] rTCalculatedi = new double[n];
    double ti = 0.0;
    final int iterationMax = 1000000;
    for (int i = 0; i < iterationMax; i++) {

      ti = ti + i / iterationMax * 100;
      if (ti <= t[0]) {

        rTCalculatedi[0] = 1.0;
      }

      else if (ti >= t[t.length - 1]) {
        rTCalculatedi[t.length - 1] = 1.0;
      } else {
        int indexpointi = Arrays.binarySearch(t, ti);
        if (indexpointi >= 0) {
          rTCalculatedi[indexpointi] = 1.0;
        } else {
          indexpointi = -(1 + indexpointi);
          if (indexpointi == 0) {
            rTCalculatedi[0] = 1.0;
          } else if (indexpointi == n) {
            rTCalculatedi[n - 1] = 1.0;
          } else {
            final double t1 = t[indexpointi - 1];
            final double t2 = t[indexpointi];
            final double dt = t2 - t1;
            rTCalculatedi[indexpointi - 1] = t1 * (t2 - ti) / dt / ti;
            rTCalculatedi[indexpointi] = t2 * (ti - t1) / dt / ti;
          }
        }
      }
      for (int j = 0; j < n; j++) {
        final double[] rTexpectedi = curve.getNodeSensitivity(ti);
        assertEquals("Time: " + ti, rTexpectedi[j], rTCalculatedi[j], 1e-10);
      }

    }
  }

  public void getNodeSensitivityvsFiniteDifferenceTest() {
    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final int n = t.length;
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);

    double ti = 0.001;
    final int iterationMax = 10000;
    for (int i = 1; i < iterationMax; i++) {
      ti = ti + i / iterationMax * 100;
      final double[] sensi = curve.getNodeSensitivity(ti);
      for (int j = 0; j < n; j++) {
        r[j] = r[j] + 10e-6;
        curve = new IsdaCompliantCurve(t, r);
        double sensii = curve.getRT(ti) / ti;
        r[j] = r[j] - 2 * 10e-6;
        curve = new IsdaCompliantCurve(t, r);
        sensii = sensii - curve.getRT(ti) / ti;
        sensii = sensii / (2 * 10e-6);
        r[j] = r[j] + 10e-6;
        curve = new IsdaCompliantCurve(t, r);
        assertEquals("node: " + j, sensi[j], sensii, 1e-5);
      }
    }
  }

  public void getSingleNodeSensitivityvsNodesensitivityTest() {
    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);
    double ti = 0.001;
    final int iterationMax = 1000000;
    double sensitivityExpectedi = 0.0;
    double sensitivityCalculatedi = 0.0;

    for (int i = 0; i < iterationMax; i++) {
      ti = ti + i / iterationMax * 100;

      for (int j = 0; j < t.length; j++) {
        sensitivityExpectedi = curve.getSingleNodeSensitivity(ti, j);
        sensitivityCalculatedi = curve.getNodeSensitivity(ti)[j];
        assertEquals("Time: " + ti, sensitivityExpectedi, sensitivityCalculatedi, EPS);
      }
    }
  }

  public void getSingleNodeSensitivityvsSingleNodeDiscountFactorsensitivityTest() {
    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);
    double ti = 0.001;
    final int iterationMax = 10000;
    double sensitivityExpectedi = 0.0;
    double sensitivityCalculatedi = 0.0;

    for (int i = 0; i < iterationMax; i++) {
      ti = ti + i / iterationMax * 100;

      for (int j = 0; j < t.length; j++) {
        sensitivityExpectedi = curve.getSingleNodeSensitivity(ti, j);
        sensitivityExpectedi = -ti * sensitivityExpectedi * Math.exp(-curve.getRT(ti));
        sensitivityCalculatedi = curve.getSingleNodeDiscountFactorSensitivity(ti, j);
        assertEquals("Time: " + ti, sensitivityExpectedi, sensitivityCalculatedi, EPS);
      }
    }

  }

  public void withRatesTest() {
    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r1 = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final double[] r2 = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r1);
    curve.withRates(r2);
    for (int i = 0; i < t.length; i++) {
      assertEquals("Node: " + i, curve.getZeroRate(t[i]), r2[i], EPS);
      assertEquals("Node: " + i, curve.getRT(t[i]), r2[i] * t[i], EPS);
      assertEquals("Node: " + i, curve.getDiscountFactor(t[i]), Math.exp(-r2[i] * t[i]), EPS);
    }
  }

  public void withRateTest() {
    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r1 = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final double[] r2 = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 3.0, 1.2, 1.0, 0.9 };
    final IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r1);
    final IsdaCompliantCurve newcurve = curve.withRate(3.0, 5);
    for (int i = 0; i < t.length; i++) {
      assertEquals("Node: " + i, newcurve.getZeroRate(t[i]), r2[i], EPS);
      assertEquals("Node: " + i, newcurve.getRT(t[i]), r2[i] * t[i], EPS);
      assertEquals("Node: " + i, newcurve.getDiscountFactor(t[i]), Math.exp(-r2[i] * t[i]), EPS);
    }

  }

  public void withDiscountFactorTest() {
    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r1 = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final double[] r2 = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, -Math.log(0.6) / t[5], 1.2, 1.0, 0.9 };
    final IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r1);
    final IsdaCompliantCurve newcurve = curve.withDiscountFactor(.6, 5);
    for (int i = 0; i < t.length; i++) {
      assertEquals("Node: " + i, newcurve.getZeroRate(t[i]), r2[i], EPS);
      assertEquals("Node: " + i, newcurve.getRT(t[i]), r2[i] * t[i], EPS);
      assertEquals("Node: " + i, newcurve.getDiscountFactor(t[i]), Math.exp(-r2[i] * t[i]), EPS);
    }

  }

  public void getZeroRateTest() {

    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);
    double ti = 0.001;
    final int iterationMax = 1000000;
    for (int i = 0; i < iterationMax; i++) {
      ti = ti + i / iterationMax * 100;
      assertEquals("Time: " + ti, curve.getZeroRate(ti), curve.getRT(ti) / ti, EPS);
    }
  }

  public void getNumberOfKnotsTest() {
    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);
    assertEquals("length", curve.getNumberOfKnots(), t.length, EPS);
  }

  public void offsetTest() {
    final double[] timesFromBase = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r = new double[] {0.04, 0.08, 0.07, 0.12, 0.12, 0.13, 0.12, 0.1, 0.09 };

    final double offset = -0.04;

    final IsdaCompliantCurve c1 = new IsdaCompliantCurve(timesFromBase, r);
    final IsdaCompliantCurve c2 = new IsdaCompliantCurve(timesFromBase, r, offset);

    final double rtb = offset * r[0];
    final double pb = Math.exp(-rtb);

    final int steps = 1001;
    for (int i = 0; i < steps; i++) {
      final double time = (12.0 * i) / (steps - 1) - offset;
      final double p1 = c1.getDiscountFactor(time + offset) / pb;
      final double p2 = c2.getDiscountFactor(time);

      final double rt1 = c1.getRT(time + offset) - rtb;
      final double rt2 = c2.getRT(time);

      assertEquals("discount " + time, p1, p2, 1e-15);
      assertEquals("rt " + time, rt1, rt2, 1e-15);
      if (time > 0.0) {
        final double r1 = rt1 / time;
        final double r2 = c2.getZeroRate(time);
        assertEquals("r " + time, r1, r2, 1e-15);
      }
    }
  }

  public void forwardRateTest() {
    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 7.0, 10.0 };
    final double[] r = new double[] {0.04, 0.08, 0.07, 0.12, 0.12, 0.13, 0.12, 0.1, 0.09 };

    final IsdaCompliantCurve c1 = new IsdaCompliantCurve(t, r);

    final double eps = 1e-5;
    for (int i = 1; i < 241; i++) {
      final double time = eps + i * 0.05;
      final double f = c1.getForwardRate(time);
      final double rtUp = c1.getRT(time + eps);
      final double rtDown = c1.getRT(time - eps);
      final double fd = (rtUp - rtDown) / 2 / eps;

      assertEquals(fd, f, 1e-10);
    }

  }

  public void senseTest() {
    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);

    final int n = curve.getNumberOfKnots();
    final int nExamples = 200;
    for (int jj = 0; jj < nExamples; jj++) {
      final double time = jj * 11.0 / (nExamples - 1);
      final double[] fd = fdSense(curve, time);
      final double[] anal = curve.getNodeSensitivity(time);
      for (int i = 0; i < n; i++) {
        final double anal2 = curve.getSingleNodeSensitivity(time, i);
        assertEquals("test1 - Time: " + time, fd[i], anal[i], 1e-10);
        assertEquals("test2 - Time: " + time, anal[i], anal2, 0.0);
      }
    }

    // check nodes
    for (int jj = 0; jj < n; jj++) {
      final double[] anal = curve.getNodeSensitivity(t[jj]);
      for (int i = 0; i < n; i++) {
        final double anal2 = curve.getSingleNodeSensitivity(t[jj], i);
        final double expected = i == jj ? 1.0 : 0.0;
        assertEquals(expected, anal[i], 0.0);
        assertEquals(expected, anal2, 0.0);
      }
    }

  }

  public void discountFactorSenseTest() {
    final double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0 };
    final double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9 };
    final IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);

    final int n = curve.getNumberOfKnots();
    final int nExamples = 200;
    for (int jj = 0; jj < nExamples; jj++) {
      final double time = jj * 11.0 / (nExamples - 1);
      final double[] fd = fdDiscountFactorSense(curve, time);

      for (int i = 0; i < n; i++) {
        final double anal = curve.getSingleNodeDiscountFactorSensitivity(time, i);
        assertEquals("Time: " + time, fd[i], anal, 1e-10);
      }
    }

  }

  private double[] fdRTSense(final IsdaCompliantCurve curve, final double t) {
    final int n = curve.getNumberOfKnots();
    final double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      final double r = curve.getZeroRateAtIndex(i);
      final IsdaCompliantCurve curveUp = curve.withRate(r + EPS, i);
      final IsdaCompliantCurve curveDown = curve.withRate(r - EPS, i);
      final double up = curveUp.getRT(t);
      final double down = curveDown.getRT(t);
      res[i] = (up - down) / 2 / EPS;
    }
    return res;
  }

  private double[] fdSense(final IsdaCompliantCurve curve, final double t) {
    final int n = curve.getNumberOfKnots();
    final double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      final double r = curve.getZeroRateAtIndex(i);
      final IsdaCompliantCurve curveUp = curve.withRate(r + EPS, i);
      final IsdaCompliantCurve curveDown = curve.withRate(r - EPS, i);
      final double up = curveUp.getZeroRate(t);
      final double down = curveDown.getZeroRate(t);
      res[i] = (up - down) / 2 / EPS;
    }
    return res;
  }

  private double[] fdDiscountFactorSense(final IsdaCompliantCurve curve, final double t) {
    final int n = curve.getNumberOfKnots();
    final double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      final double r = curve.getZeroRateAtIndex(i);
      final IsdaCompliantCurve curveUp = curve.withRate(r + EPS, i);
      final IsdaCompliantCurve curveDown = curve.withRate(r - EPS, i);
      final double up = curveUp.getDiscountFactor(t);
      final double down = curveDown.getDiscountFactor(t);
      res[i] = (up - down) / 2 / EPS;
    }
    return res;
  }

  /**
   * 
   */
  public void buildTest() {
    final double tol = 1.e-13;

    final double[] time = new double[] {0.1, 0.3, 0.5, 1., 3. };
    final double[] forward = new double[] {0.06, 0.1, 0.05, 0.08, 0.11 };
    final int num = time.length;

    final double[] r = new double[num];
    final double[] rt = new double[num];

    final IsdaCompliantCurve cv1 = IsdaCompliantCurve.makeFromForwardRates(time, forward);
    assertEquals(num, cv1.size());
    final double[] clonedTime = cv1.getKnotTimes();
    assertNotSame(time, clonedTime);

    rt[0] = forward[0] * time[0];
    r[0] = forward[0];
    final Double[] xData = cv1.getXData();
    final Double[] yData = cv1.getYData();
    for (int i = 1; i < num; ++i) {
      rt[i] = rt[i - 1] + forward[i] * (time[i] - time[i - 1]);
      r[i] = rt[i] / time[i];
      assertEquals(r[i], cv1.getZeroRate(time[i]), EPS);
      assertEquals(Math.exp(-rt[i]), cv1.getDiscountFactor(time[i]), EPS);
      assertEquals(time[i], clonedTime[i]);
    }

    final IsdaCompliantCurve cv1Clone = cv1.clone();
    assertEquals(cv1.toString(), cv1Clone.toString());

    final double offset = 0.34;
    final IsdaCompliantCurve cv1Offset = cv1.withOffset(offset);
    assertEquals(cv1.getDiscountFactor(0.75) / cv1.getDiscountFactor(offset), cv1Offset.getDiscountFactor(0.75 - offset), 1.e-14);
    assertEquals(cv1.getDiscountFactor(1.) / cv1.getDiscountFactor(offset), cv1Offset.getDiscountFactor(1. - offset), 1.e-14);
    assertEquals(cv1.getDiscountFactor(4.) / cv1.getDiscountFactor(offset), cv1Offset.getDiscountFactor(4. - offset), 1.e-14);

    final IsdaCompliantCurve cv2 = IsdaCompliantCurve.makeFromRT(time, rt);
    final IsdaCompliantCurve cv3 = new IsdaCompliantCurve(time, r);
    assertEquals(cv1, cv2);
    for (int i = 0; i < num; ++i) {
      assertEquals(r[i], cv1.getZeroRate(time[i]), EPS);
      assertEquals(Math.exp(-rt[i]), cv1.getDiscountFactor(time[i]), EPS);
      assertEquals(time[i], xData[i]);
      assertEquals(r[i], yData[i], EPS);

      assertEquals(cv1.getTimeAtIndex(i), cv3.getTimeAtIndex(i), tol);
      assertEquals(cv1.getRTAtIndex(i), cv3.getRTAtIndex(i), tol);
    }

    final double[] T = new double[] {-0.3, -0.1, -0. };
    final double[] RT = new double[] {0.06, 0.1, 0. };
    final IsdaCompliantCurve cv11 = IsdaCompliantCurve.makeFromRT(T, RT);
    final double[] sen = cv11.getRTandSensitivity(0., 0);

    assertEquals(cv11.getRT(0.), sen[0], tol);
    assertEquals(0., sen[1], tol);

    assertEquals(cv11.getForwardRate(0. - tol), cv11.getForwardRate(0.));
    assertEquals(cv11.getForwardRate(T[1]), cv11.getForwardRate(T[1]));

    final Double[] rtSense = cv1.getYValueParameterSensitivity(0.33);
    for (int i = 0; i < num; ++i) {
      assertEquals(cv1.getSingleNodeSensitivity(0.33, i), rtSense[i]);
    }

    final double[] refs = new double[] {0.04, 0.74, 2. };
    final double eps = 1.e-6;
    final double[] dydx = new double[] {cv1.getDyDx(refs[0]), cv1.getDyDx(refs[1]), cv1.getDyDx(refs[2]) };
    for (int i = 0; i < refs.length; ++i) {
      final double res = 0.5 * (cv1.getYValue(refs[i] * (1. + eps)) - cv1.getYValue(refs[i] * (1. - eps))) / refs[i] / eps;
      assertEquals(res, dydx[i], Math.abs(dydx[i] * eps));
    }

    /*
     * Meta
     */
    final IsdaCompliantCurve.Meta meta = cv1.metaBean();
    final BeanBuilder<?> builder = meta.builder();
    builder.set(meta.metaPropertyGet("name"), "");
    builder.set(meta.metaPropertyGet("t"), time);
    builder.set(meta.metaPropertyGet("rt"), rt);
    IsdaCompliantCurve builtCurve = (IsdaCompliantCurve) builder.build();
    assertEquals(cv1, builtCurve);
    assertEquals(meta.t(), meta.metaPropertyGet("t"));
    assertEquals(meta.rt(), meta.metaPropertyGet("rt"));

    final IsdaCompliantCurve.Meta meta1 = IsdaCompliantCurve.meta();
    assertEquals(meta, meta1);

    /*
     * hashCode and equals
     */
    final IsdaCompliantCurve cv4 = IsdaCompliantCurve.makeFromRT(new double[] {0.1, 0.2, 0.5, 1., 3. }, rt);
    final IsdaCompliantCurve cv5 = IsdaCompliantCurve.makeFromRT(new double[] {0.1, 0.3, 0.5, 1., 3. }, rt);
    rt[1] *= 1.05;
    final IsdaCompliantCurve cv6 = IsdaCompliantCurve.makeFromRT(new double[] {0.1, 0.3, 0.5, 1., 3. }, rt);

    assertTrue(cv1.equals(cv1));

    assertTrue(!(cv5.equals(IsdaCompliantYieldCurve.makeFromRT(time, rt))));

    assertTrue(cv1.hashCode() != cv6.hashCode());
    assertTrue(!(cv1.equals(cv6)));

    assertTrue(cv1.equals(cv5));
    assertTrue(cv1.hashCode() == cv5.hashCode());

    assertTrue(cv4.hashCode() != cv5.hashCode());
    assertTrue(!(cv4.equals(cv5)));

    /*
     * Error returned
     */
    try {
      final double[] shotTime = Arrays.copyOf(time, num - 1);
      IsdaCompliantCurve.makeFromForwardRates(shotTime, forward);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      final double[] shotTime = Arrays.copyOf(time, num - 1);
      IsdaCompliantCurve.makeFromRT(shotTime, rt);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      final double[] shotTime = Arrays.copyOf(time, num - 1);
      new IsdaCompliantCurve(shotTime, rt);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      final double[] negativeTime = Arrays.copyOf(time, num);
      negativeTime[0] *= -1.;
      new IsdaCompliantCurve(negativeTime, rt);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      final double[] notSortTime = Arrays.copyOf(time, num);
      notSortTime[2] *= 10.;
      new IsdaCompliantCurve(notSortTime, rt);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getZeroRate(-2.);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getRTandSensitivity(-2., 1);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getRTandSensitivity(2., -1);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getRTandSensitivity(2., num + 2);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getSingleNodeSensitivity(-2., 1);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getSingleNodeSensitivity(2., -1);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getSingleNodeSensitivity(2., num + 2);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getSingleNodeRTSensitivity(-2., 1);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getSingleNodeRTSensitivity(2., -1);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getSingleNodeRTSensitivity(2., num + 2);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.withRate(2., -1);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.withRate(2., num + 2);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.setRate(2., -1);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.setRate(2., num + 2);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.withDiscountFactor(2., -1);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.withDiscountFactor(2., num + 2);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
  }
}
