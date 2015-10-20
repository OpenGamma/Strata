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

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.sensitivity.CurveUnitParameterSensitivity;

/**
 * Test {@link IsdaCompliantCurve}.
 */
@Test
public class IsdaCompliantCurveTest {
  private static final double EPS = 1e-5;

  /**
   * no shift
   */
  public void noShiftTest() {
    double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    double offset = 0.0;
    IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(9, offsetCurve.getNumberOfKnots());
    double rtOffset = offset * r[0];
    for (int i = 0; i < 100; i++) {
      double time = 3.5 * i / 100.0 + offset;
      double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      double rt2 = offsetCurve.getRT(time);
      assertEquals(rt1, rt2);
    }
  }

  /**
   * Shift less than first knot
   */
  public void baseShiftTest() {
    double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    double offset = 0.01;
    IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(9, offsetCurve.getNumberOfKnots());
    double rtOffset = offset * r[0];
    for (int i = 0; i < 100; i++) {
      double time = 3.5 * i / 100.0 + offset;
      double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      double rt2 = offsetCurve.getRT(time);
      assertEquals(rt1, rt2, 1e-15);
    }
  }

  /**
   * shift between two knots
   */
  public void baseShiftTest2() {
    double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    double offset = 0.3;
    IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(6, offsetCurve.getNumberOfKnots());
    double rtOffset = baseCurve.getRT(offset);
    for (int i = 0; i < 100; i++) {
      double time = 4.0 * i / 100.;
      double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      double rt2 = offsetCurve.getRT(time);

      assertEquals(rt1, rt2, 1e-15);
    }
  }

  /**
   * shift to just before last knot and extrapolate at long way out 
   */
  public void baseShiftTest3() {
    double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    double offset = 3.3;
    IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(1, offsetCurve.getNumberOfKnots());
    double rtOffset = baseCurve.getRT(offset);
    for (int i = 0; i < 100; i++) {
      double time = 5.0 * i / 100.;
      double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      double rt2 = offsetCurve.getRT(time);

      assertEquals(rt1, rt2, 1e-14);
    }
  }

  /**
   * shift exactly to one of the knots
   */
  public void baseShiftTest4() {
    double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4};
    double[] r = new double[] {1.0, 0.8, 0.7, 0.5, 1.2, 1.3, 1.2, 1.0, 0.9};
    double offset = 0.5;
    IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(5, offsetCurve.getNumberOfKnots());
    double rtOffset = baseCurve.getRT(offset);
    for (int i = 0; i < 100; i++) {
      double time = 4.0 * i / 100.;
      double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      double rt2 = offsetCurve.getRT(time);

      assertEquals(rt1, rt2, 1e-14);
    }
  }

  /**
   * shift to last knot 
   */
  public void baseShiftTest5() {
    double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4};
    double[] r = new double[] {0.4, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.5};
    double offset = 3.4;
    IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(1, offsetCurve.getNumberOfKnots());
    double rtOffset = baseCurve.getRT(offset);
    for (int i = 0; i < 100; i++) {
      double time = 4.0 * i / 100.;
      double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      double rt2 = offsetCurve.getRT(time);

      assertEquals(rt1, rt2, 1e-14);
    }
  }

  /**
   * shift past last knot 
   */
  public void baseShiftTest6() {
    double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    double offset = 3.5;
    IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(1, offsetCurve.getNumberOfKnots());
    double rtOffset = baseCurve.getRT(offset);
    for (int i = 0; i < 100; i++) {
      double time = 4.0 * i / 100.;
      double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      double rt2 = offsetCurve.getRT(time);

      assertEquals(rt1, rt2, 1e-14);
    }
  }

  /**
   * shift to first knot 
   */
  public void baseShiftTest7() {
    double[] t = new double[] {0.03, 0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4};
    double[] r = new double[] {0.4, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.5};
    double offset = 0.03;
    IsdaCompliantCurve baseCurve = new IsdaCompliantCurve(t, r);
    IsdaCompliantCurve offsetCurve = new IsdaCompliantCurve(t, r, offset);
    assertEquals(8, offsetCurve.getNumberOfKnots());
    double rtOffset = baseCurve.getRT(offset);
    for (int i = 0; i < 100; i++) {
      double time = 4.0 * i / 100.;
      double rt1 = baseCurve.getRT(time + offset) - rtOffset;
      double rt2 = offsetCurve.getRT(time);

      assertEquals(rt1, rt2, 1e-15);
    }
  }

  public void getRTTest() {
    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    int n = t.length;
    double[] rt = new double[n];
    for (int i = 0; i < n; i++) {

      rt[i] = r[i] * t[i];
    }
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);

    double rTCalculatedi = 0.0;
    double ti = 0.0;
    int iterationMax = 1000000;
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
            double t1 = t[indexpointi - 1];
            double t2 = t[indexpointi];
            double dt = t2 - t1;
            rTCalculatedi =
                ((t2 - ti) * r[indexpointi - 1] * t[indexpointi - 1] + (ti - t1) * r[indexpointi] * t[indexpointi]) / dt;
          }
        }
      }
      double rTexpectedi = curve.getRT(ti);
      assertEquals("Time: " + ti, rTexpectedi, rTCalculatedi, 1e-10);
    }
  }

  public void rtandSenseTest() {
    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    int n = t.length;
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);

    for (int i = 0; i < 100; i++) {
      double tt = i * 12.0 / 100;
      double rt1 = curve.getRT(tt);
      double[] fdSense = fdRTSense(curve, tt);
      for (int jj = 0; jj < n; jj++) {
        double[] rtandSense = curve.getRTandSensitivity(tt, jj);
        assertEquals("rt " + tt, rt1, rtandSense[0], 1e-14);
        assertEquals("sense " + tt + "\t" + jj, fdSense[jj], rtandSense[1], 1e-9);
      }
    }
  }

  public void getNodeSensitivity() {

    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    int n = t.length;
    double[] rt = new double[n];
    for (int i = 0; i < n; i++) {

      rt[i] = r[i] * t[i];
    }
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);

    double[] rTCalculatedi = new double[n];
    double ti = 0.0;
    int iterationMax = 1000000;
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
            double t1 = t[indexpointi - 1];
            double t2 = t[indexpointi];
            double dt = t2 - t1;
            rTCalculatedi[indexpointi - 1] = t1 * (t2 - ti) / dt / ti;
            rTCalculatedi[indexpointi] = t2 * (ti - t1) / dt / ti;
          }
        }
      }
      for (int j = 0; j < n; j++) {
        DoubleArray rTexpectedi = curve.getNodeSensitivity(ti);
        assertEquals("Time: " + ti, rTexpectedi.get(j), rTCalculatedi[j], 1e-10);
      }

    }
  }

  public void getNodeSensitivityvsFiniteDifferenceTest() {
    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    int n = t.length;
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);

    double ti = 0.001;
    int iterationMax = 10000;
    for (int i = 1; i < iterationMax; i++) {
      ti = ti + i / iterationMax * 100;
      DoubleArray sensi = curve.getNodeSensitivity(ti);
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
        assertEquals("node: " + j, sensi.get(j), sensii, 1e-5);
      }
    }
  }

  public void getSingleNodeSensitivityvsNodesensitivityTest() {
    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);
    double ti = 0.001;
    int iterationMax = 1000000;
    double sensitivityExpectedi = 0.0;
    double sensitivityCalculatedi = 0.0;

    for (int i = 0; i < iterationMax; i++) {
      ti = ti + i / iterationMax * 100;

      for (int j = 0; j < t.length; j++) {
        sensitivityExpectedi = curve.getSingleNodeSensitivity(ti, j);
        sensitivityCalculatedi = curve.getNodeSensitivity(ti).get(j);
        assertEquals("Time: " + ti, sensitivityExpectedi, sensitivityCalculatedi, EPS);
      }
    }
  }

  public void getSingleNodeSensitivityvsSingleNodeDiscountFactorsensitivityTest() {
    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);
    double ti = 0.001;
    int iterationMax = 10000;
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
    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r1 = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    double[] r2 = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r1);
    curve.withRates(r2);
    for (int i = 0; i < t.length; i++) {
      assertEquals("Node: " + i, curve.getZeroRate(t[i]), r2[i], EPS);
      assertEquals("Node: " + i, curve.getRT(t[i]), r2[i] * t[i], EPS);
      assertEquals("Node: " + i, curve.getDiscountFactor(t[i]), Math.exp(-r2[i] * t[i]), EPS);
    }
  }

  public void withRateTest() {
    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r1 = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    double[] r2 = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 3.0, 1.2, 1.0, 0.9};
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r1);
    IsdaCompliantCurve newcurve = curve.withRate(3.0, 5);
    for (int i = 0; i < t.length; i++) {
      assertEquals("Node: " + i, newcurve.getZeroRate(t[i]), r2[i], EPS);
      assertEquals("Node: " + i, newcurve.getRT(t[i]), r2[i] * t[i], EPS);
      assertEquals("Node: " + i, newcurve.getDiscountFactor(t[i]), Math.exp(-r2[i] * t[i]), EPS);
    }

  }

  public void withDiscountFactorTest() {
    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r1 = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    double[] r2 = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, -Math.log(0.6) / t[5], 1.2, 1.0, 0.9};
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r1);
    IsdaCompliantCurve newcurve = curve.withDiscountFactor(.6, 5);
    for (int i = 0; i < t.length; i++) {
      assertEquals("Node: " + i, newcurve.getZeroRate(t[i]), r2[i], EPS);
      assertEquals("Node: " + i, newcurve.getRT(t[i]), r2[i] * t[i], EPS);
      assertEquals("Node: " + i, newcurve.getDiscountFactor(t[i]), Math.exp(-r2[i] * t[i]), EPS);
    }

  }

  public void getZeroRateTest() {

    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);
    double ti = 0.001;
    int iterationMax = 1000000;
    for (int i = 0; i < iterationMax; i++) {
      ti = ti + i / iterationMax * 100;
      assertEquals("Time: " + ti, curve.getZeroRate(ti), curve.getRT(ti) / ti, EPS);
    }
  }

  public void getNumberOfKnotsTest() {
    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);
    assertEquals("length", curve.getNumberOfKnots(), t.length, EPS);
  }

  public void offsetTest() {
    double[] timesFromBase = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r = new double[] {0.04, 0.08, 0.07, 0.12, 0.12, 0.13, 0.12, 0.1, 0.09};

    double offset = -0.04;

    IsdaCompliantCurve c1 = new IsdaCompliantCurve(timesFromBase, r);
    IsdaCompliantCurve c2 = new IsdaCompliantCurve(timesFromBase, r, offset);

    double rtb = offset * r[0];
    double pb = Math.exp(-rtb);

    int steps = 1001;
    for (int i = 0; i < steps; i++) {
      double time = (12.0 * i) / (steps - 1) - offset;
      double p1 = c1.getDiscountFactor(time + offset) / pb;
      double p2 = c2.getDiscountFactor(time);

      double rt1 = c1.getRT(time + offset) - rtb;
      double rt2 = c2.getRT(time);

      assertEquals("discount " + time, p1, p2, 1e-15);
      assertEquals("rt " + time, rt1, rt2, 1e-15);
      if (time > 0.0) {
        double r1 = rt1 / time;
        double r2 = c2.getZeroRate(time);
        assertEquals("r " + time, r1, r2, 1e-15);
      }
    }
  }

  public void forwardRateTest() {
    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 7.0, 10.0};
    double[] r = new double[] {0.04, 0.08, 0.07, 0.12, 0.12, 0.13, 0.12, 0.1, 0.09};

    IsdaCompliantCurve c1 = new IsdaCompliantCurve(t, r);

    double eps = 1e-5;
    for (int i = 1; i < 241; i++) {
      double time = eps + i * 0.05;
      double f = c1.getForwardRate(time);
      double rtUp = c1.getRT(time + eps);
      double rtDown = c1.getRT(time - eps);
      double fd = (rtUp - rtDown) / 2 / eps;

      assertEquals(fd, f, 1e-10);
    }

  }

  public void senseTest() {
    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);

    int n = curve.getNumberOfKnots();
    int nExamples = 200;
    for (int jj = 0; jj < nExamples; jj++) {
      double time = jj * 11.0 / (nExamples - 1);
      double[] fd = fdSense(curve, time);
      DoubleArray anal = curve.getNodeSensitivity(time);
      for (int i = 0; i < n; i++) {
        double anal2 = curve.getSingleNodeSensitivity(time, i);
        assertEquals("test1 - Time: " + time, fd[i], anal.get(i), 1e-10);
        assertEquals("test2 - Time: " + time, anal.get(i), anal2, 0.0);
      }
    }

    // check nodes
    for (int jj = 0; jj < n; jj++) {
      DoubleArray anal = curve.getNodeSensitivity(t[jj]);
      for (int i = 0; i < n; i++) {
        double anal2 = curve.getSingleNodeSensitivity(t[jj], i);
        double expected = i == jj ? 1.0 : 0.0;
        assertEquals(expected, anal.get(i), 0.0);
        assertEquals(expected, anal2, 0.0);
      }
    }

  }

  public void discountFactorSenseTest() {
    double[] t = new double[] {0.1, 0.2, 0.5, 0.7, 1.0, 2.0, 3.0, 3.4, 10.0};
    double[] r = new double[] {1.0, 0.8, 0.7, 1.2, 1.2, 1.3, 1.2, 1.0, 0.9};
    IsdaCompliantCurve curve = new IsdaCompliantCurve(t, r);

    int n = curve.getNumberOfKnots();
    int nExamples = 200;
    for (int jj = 0; jj < nExamples; jj++) {
      double time = jj * 11.0 / (nExamples - 1);
      double[] fd = fdDiscountFactorSense(curve, time);

      for (int i = 0; i < n; i++) {
        double anal = curve.getSingleNodeDiscountFactorSensitivity(time, i);
        assertEquals("Time: " + time, fd[i], anal, 1e-10);
      }
    }

  }

  private double[] fdRTSense(IsdaCompliantCurve curve, double t) {
    int n = curve.getNumberOfKnots();
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      double r = curve.getZeroRateAtIndex(i);
      IsdaCompliantCurve curveUp = curve.withRate(r + EPS, i);
      IsdaCompliantCurve curveDown = curve.withRate(r - EPS, i);
      double up = curveUp.getRT(t);
      double down = curveDown.getRT(t);
      res[i] = (up - down) / 2 / EPS;
    }
    return res;
  }

  private double[] fdSense(IsdaCompliantCurve curve, double t) {
    int n = curve.getNumberOfKnots();
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      double r = curve.getZeroRateAtIndex(i);
      IsdaCompliantCurve curveUp = curve.withRate(r + EPS, i);
      IsdaCompliantCurve curveDown = curve.withRate(r - EPS, i);
      double up = curveUp.getZeroRate(t);
      double down = curveDown.getZeroRate(t);
      res[i] = (up - down) / 2 / EPS;
    }
    return res;
  }

  private double[] fdDiscountFactorSense(IsdaCompliantCurve curve, double t) {
    int n = curve.getNumberOfKnots();
    double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      double r = curve.getZeroRateAtIndex(i);
      IsdaCompliantCurve curveUp = curve.withRate(r + EPS, i);
      IsdaCompliantCurve curveDown = curve.withRate(r - EPS, i);
      double up = curveUp.getDiscountFactor(t);
      double down = curveDown.getDiscountFactor(t);
      res[i] = (up - down) / 2 / EPS;
    }
    return res;
  }

  /**
   * 
   */
  public void buildTest() {
    double tol = 1.e-13;

    double[] time = new double[] {0.1, 0.3, 0.5, 1., 3.};
    double[] forward = new double[] {0.06, 0.1, 0.05, 0.08, 0.11};
    int num = time.length;

    double[] r = new double[num];
    double[] rt = new double[num];

    IsdaCompliantCurve cv1 = IsdaCompliantCurve.makeFromForwardRates(time, forward);
    assertEquals(num, cv1.getParameterCount());
    double[] clonedTime = cv1.getKnotTimes();
    assertNotSame(time, clonedTime);

    rt[0] = forward[0] * time[0];
    r[0] = forward[0];
    double[] xData = cv1.getXValues().toArray();
    double[] yData = cv1.getYValues().toArray();
    for (int i = 1; i < num; ++i) {
      rt[i] = rt[i - 1] + forward[i] * (time[i] - time[i - 1]);
      r[i] = rt[i] / time[i];
      assertEquals(r[i], cv1.getZeroRate(time[i]), EPS);
      assertEquals(Math.exp(-rt[i]), cv1.getDiscountFactor(time[i]), EPS);
      assertEquals(time[i], clonedTime[i]);
    }

    IsdaCompliantCurve cv1Clone = cv1.clone();
    assertEquals(cv1.toString(), cv1Clone.toString());

    double offset = 0.34;
    IsdaCompliantCurve cv1Offset = cv1.withOffset(offset);
    assertEquals(cv1.getDiscountFactor(0.75) / cv1.getDiscountFactor(offset), cv1Offset.getDiscountFactor(0.75 - offset), 1.e-14);
    assertEquals(cv1.getDiscountFactor(1.) / cv1.getDiscountFactor(offset), cv1Offset.getDiscountFactor(1. - offset), 1.e-14);
    assertEquals(cv1.getDiscountFactor(4.) / cv1.getDiscountFactor(offset), cv1Offset.getDiscountFactor(4. - offset), 1.e-14);

    IsdaCompliantCurve cv2 = IsdaCompliantCurve.makeFromRT(time, rt);
    IsdaCompliantCurve cv3 = new IsdaCompliantCurve(time, r);
    assertEquals(cv1, cv2);
    for (int i = 0; i < num; ++i) {
      assertEquals(r[i], cv1.getZeroRate(time[i]), EPS);
      assertEquals(Math.exp(-rt[i]), cv1.getDiscountFactor(time[i]), EPS);
      assertEquals(time[i], xData[i]);
      assertEquals(r[i], yData[i], EPS);

      assertEquals(cv1.getTimeAtIndex(i), cv3.getTimeAtIndex(i), tol);
      assertEquals(cv1.getRTAtIndex(i), cv3.getRTAtIndex(i), tol);
    }

    double[] T = new double[] {-0.3, -0.1, -0.};
    double[] RT = new double[] {0.06, 0.1, 0.};
    IsdaCompliantCurve cv11 = IsdaCompliantCurve.makeFromRT(T, RT);
    double[] sen = cv11.getRTandSensitivity(0., 0);

    assertEquals(cv11.getRT(0.), sen[0], tol);
    assertEquals(0., sen[1], tol);

    assertEquals(cv11.getForwardRate(0. - tol), cv11.getForwardRate(0.));
    assertEquals(cv11.getForwardRate(T[1]), cv11.getForwardRate(T[1]));

    CurveUnitParameterSensitivity rtSense = cv1.yValueParameterSensitivity(0.33);
    for (int i = 0; i < num; ++i) {
      assertEquals(cv1.getSingleNodeSensitivity(0.33, i), rtSense.getSensitivity().get(i));
    }

    double[] refs = new double[] {0.04, 0.74, 2.};
    double eps = 1.e-6;
    double[] dydx = new double[] {cv1.firstDerivative(refs[0]), cv1.firstDerivative(refs[1]), cv1.firstDerivative(refs[2])};
    for (int i = 0; i < refs.length; ++i) {
      double res = 0.5 * (cv1.yValue(refs[i] * (1. + eps)) - cv1.yValue(refs[i] * (1. - eps))) / refs[i] / eps;
      assertEquals(res, dydx[i], Math.abs(dydx[i] * eps));
    }

    /*
     * Meta
     */
    IsdaCompliantCurve.Meta meta = cv1.metaBean();
    BeanBuilder<?> builder = meta.builder();
    builder.set(meta.metaPropertyGet("metadata"), DefaultCurveMetadata.of("IsdaCompliantCurve"));
    builder.set(meta.metaPropertyGet("t"), time);
    builder.set(meta.metaPropertyGet("rt"), rt);
    IsdaCompliantCurve builtCurve = (IsdaCompliantCurve) builder.build();
    assertEquals(cv1, builtCurve);
    assertEquals(meta.t(), meta.metaPropertyGet("t"));
    assertEquals(meta.rt(), meta.metaPropertyGet("rt"));

    IsdaCompliantCurve.Meta meta1 = IsdaCompliantCurve.meta();
    assertEquals(meta, meta1);

    /*
     * hashCode and equals
     */
    IsdaCompliantCurve cv4 = IsdaCompliantCurve.makeFromRT(new double[] {0.1, 0.2, 0.5, 1., 3.}, rt);
    IsdaCompliantCurve cv5 = IsdaCompliantCurve.makeFromRT(new double[] {0.1, 0.3, 0.5, 1., 3.}, rt);
    rt[1] *= 1.05;
    IsdaCompliantCurve cv6 = IsdaCompliantCurve.makeFromRT(new double[] {0.1, 0.3, 0.5, 1., 3.}, rt);

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
      double[] shotTime = Arrays.copyOf(time, num - 1);
      IsdaCompliantCurve.makeFromForwardRates(shotTime, forward);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      double[] shotTime = Arrays.copyOf(time, num - 1);
      IsdaCompliantCurve.makeFromRT(shotTime, rt);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      double[] shotTime = Arrays.copyOf(time, num - 1);
      new IsdaCompliantCurve(shotTime, rt);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      double[] negativeTime = Arrays.copyOf(time, num);
      negativeTime[0] *= -1.;
      new IsdaCompliantCurve(negativeTime, rt);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      double[] notSortTime = Arrays.copyOf(time, num);
      notSortTime[2] *= 10.;
      new IsdaCompliantCurve(notSortTime, rt);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getZeroRate(-2.);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getRTandSensitivity(-2., 1);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getRTandSensitivity(2., -1);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getRTandSensitivity(2., num + 2);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getSingleNodeSensitivity(-2., 1);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getSingleNodeSensitivity(2., -1);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getSingleNodeSensitivity(2., num + 2);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getSingleNodeRTSensitivity(-2., 1);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getSingleNodeRTSensitivity(2., -1);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.getSingleNodeRTSensitivity(2., num + 2);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.withRate(2., -1);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.withRate(2., num + 2);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.setRate(2., -1);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.setRate(2., num + 2);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.withDiscountFactor(2., -1);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    try {
      cv1.withDiscountFactor(2., num + 2);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
  }

}
