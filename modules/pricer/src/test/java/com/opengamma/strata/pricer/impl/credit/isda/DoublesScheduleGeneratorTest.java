/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

/**
 * 
 */
@Test
public class DoublesScheduleGeneratorTest {

  /**
   * 
   */
  @Test
  public void getIntegrationsPointsTest() {
    final double start = 0.1;
    final double end = 2.;

    final double[] setA0 = new double[] {0.5, 0.9, 1.4 };
    final double[] setB0 = new double[] {0.3, 0.4, 1.5, 1.6 };
    final double[] exp0 = new double[] {0.1, 0.3, 0.4, 0.5, 0.9, 1.4, 1.5, 1.6, 2. };
    final double[] res0 = DoublesScheduleGenerator.getIntegrationsPoints(start, end, setA0, setB0);
    assertTrue(equalArrays(exp0, res0));

    final double[] setA1 = new double[] {0.5, 0.9, 1.4 };
    final double[] setB1 = new double[] {0.3, 0.4, 0.5, 1.5, 1.6 };
    final double[] exp1 = new double[] {0.1, 0.3, 0.4, 0.5, 0.9, 1.4, 1.5, 1.6, 2. };
    final double[] res1 = DoublesScheduleGenerator.getIntegrationsPoints(start, end, setA1, setB1);
    assertTrue(equalArrays(exp1, res1));

    /*
     * different(temp[pos], end) == false
     */
    final double[] setA2 = new double[] {0.2, 0.9, 1.4 };
    final double[] setB2 = new double[] {0.3, 0.4, 0.5, 1.5, 2. - 1.e-3 };
    final double[] exp2 = new double[] {0.1, 0.2, 0.3, 0.4, 0.5, 0.9, 1.4, 1.5, 2. };
    final double[] res2 = DoublesScheduleGenerator.getIntegrationsPoints(start, end, setA2, setB2);
    assertTrue(equalArrays(exp2, res2));

    final double[] setA3 = new double[] {0.05, 0.07 };
    final double[] setB3 = new double[] {0.03, 0.04 };
    final double[] exp3 = new double[] {0.1, 2. };
    final double[] res3 = DoublesScheduleGenerator.getIntegrationsPoints(start, end, setA3, setB3);
    assertTrue(equalArrays(exp3, res3));

    final double[] setA4 = new double[] {2.2, 2.7 };
    final double[] setB4 = new double[] {2.3, 2.4 };
    final double[] exp4 = new double[] {0.1, 2. };
    final double[] res4 = DoublesScheduleGenerator.getIntegrationsPoints(start, end, setA4, setB4);
    assertTrue(equalArrays(exp4, res4));

    final double[] setA5 = new double[] {-0.5, 0., 1.2 };
    final double[] setB5 = new double[] {-0.2, -0., 1.2 };
    final double[] exp5 = new double[] {-0.3, -0.2, 0., 1.2, 2. };
    final double[] res5 = DoublesScheduleGenerator.getIntegrationsPoints(-0.3, end, setA5, setB5);
    assertTrue(equalArrays(exp5, res5));
  }

  /**
   * 
   */
  @Test
  public void combineSetsTest() {
    final double[] setA0 = new double[] {-2., -1., 0., 2. };
    final double[] setB0 = new double[] {-1., -0., 2.1 };
    final double[] exp0 = new double[] {-2., -1., 0., 2., 2.1 };
    final double[] res0 = DoublesScheduleGenerator.combineSets(setA0, setB0);
    assertTrue(equalArrays(res0, exp0));

    final double[] setA1 = new double[] {-2., 0., 2. };
    final double[] setB1 = new double[] {-1., 2.1 };
    final double[] exp1 = new double[] {-2., -1., 0., 2., 2.1 };
    final double[] res1 = DoublesScheduleGenerator.combineSets(setA1, setB1);
    assertTrue(equalArrays(res1, exp1));
  }

  /**
   * 
   */
  @Test
  public void truncateSetInclusiveTest() {
    final double lower = 0.1;
    final double upper = 2.5;

    final double[] set0 = new double[] {-0.2, 1.5, 2.9 };
    final double[] exp0 = new double[] {0.1, 1.5, 2.5 };
    final double[] res0 = DoublesScheduleGenerator.truncateSetInclusive(lower, upper, set0);
    assertTrue(equalArrays(exp0, res0));

    final double[] set1 = new double[] {-0.2, -0.1 };
    final double[] exp1 = new double[] {0.1, 2.5 };
    final double[] res1 = DoublesScheduleGenerator.truncateSetInclusive(lower, upper, set1);
    assertTrue(equalArrays(exp1, res1));

    final double[] set2 = new double[] {0.1 + 1.e-3, 1.5, 2.8 };
    final double[] exp2 = new double[] {0.1, 1.5, 2.5 };
    final double[] res2 = DoublesScheduleGenerator.truncateSetInclusive(lower, upper, set2);
    assertTrue(equalArrays(exp2, res2));

    final double[] set3 = new double[] {0., 1.5, 2.5 + 1.e-3 };
    final double[] exp3 = new double[] {0.1, 1.5, 2.5 };
    final double[] res3 = DoublesScheduleGenerator.truncateSetInclusive(lower, upper, set3);
    assertTrue(equalArrays(exp3, res3));

    final double[] set4 = new double[] {0.1 - 1.e-4, 1.5, 2.5 - 1.e-4 };
    final double[] exp4 = new double[] {0.1, 1.5, 2.5 };
    final double[] res4 = DoublesScheduleGenerator.truncateSetInclusive(lower, upper, set4);
    assertTrue(equalArrays(exp4, res4));

    final double[] set5 = new double[] {lower + 1.e-4, lower + 2.e-4 };
    final double[] exp5 = new double[] {lower, lower + 1.e-3 };
    final double[] res5 = DoublesScheduleGenerator.truncateSetInclusive(lower, lower + 1.e-3, set5);
    assertTrue(equalArrays(exp5, res5));
  }

  /**
   * 
   */
  @Test
  public void leftTruncateTest() {
    final double lower = 0.2;

    final double[] set0 = new double[] {0.1, 0.3, 0.5 };
    final double[] exp0 = new double[] {0.3, 0.5 };
    final double[] res0 = DoublesScheduleGenerator.leftTruncate(lower, set0);
    assertTrue(equalArrays(exp0, res0));

    final double[] set1 = new double[] {0.1, 0.15 };
    final double[] exp1 = new double[] {};
    final double[] res1 = DoublesScheduleGenerator.leftTruncate(lower, set1);
    assertTrue(equalArrays(exp1, res1));

    final double[] set2 = new double[] {0.25, 0.3, 0.5 };
    final double[] exp2 = new double[] {0.25, 0.3, 0.5 };
    final double[] res2 = DoublesScheduleGenerator.leftTruncate(lower, set2);
    assertTrue(equalArrays(exp2, res2));

    final double[] set3 = new double[] {};
    final double[] exp3 = new double[] {};
    final double[] res3 = DoublesScheduleGenerator.leftTruncate(lower, set3);
    assertTrue(equalArrays(exp3, res3));

    final double[] set4 = new double[] {0.0, 0.3, 0.5 };
    final double[] exp4 = new double[] {0.0, 0.3, 0.5 };
    final double[] res4 = DoublesScheduleGenerator.leftTruncate(-0.0, set4);
    assertTrue(equalArrays(exp4, res4));
  }

  private boolean equalArrays(final double[] exp, final double[] res) {
    final int num = exp.length;
    if (res.length != num) {
      return false;
    }
    for (int i = 0; i < num; ++i) {
      if (exp[i] != res[i]) {
        return false;
      }
    }
    return true;
  }
}
