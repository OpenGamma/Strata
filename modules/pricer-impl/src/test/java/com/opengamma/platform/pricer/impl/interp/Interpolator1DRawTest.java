/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.interp;

import java.util.Arrays;

import org.testng.annotations.Test;

@Test
public class Interpolator1DRawTest {

  @Test
  public void simpleInterpTest() {

    double[] x = new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    double[] y = new double[] {0.0262528607281266, 0.0332008172580452,
        0.0680406527800897, 0.3325986476978349, 0.3747764345251541,
        0.4333961926133433, 0.6636063423603406, 0.8088846531977805,
        0.8107605699814449, 0.9470649378271933};
    double[] xx = new double[] {-1, 0, 0.5, 1, 1.2, 3, 3.5, 7.7, 8, 8.1,
        9, 9.5, 9.9, 10, 10.1, 12};
    double[] yy;

    // vector interpolation pump
    yy = Interpolator1DRaw.interp(x, y, xx, InterpMethod.LINEAR);
    System.out.println(Arrays.toString(yy));

    yy = Interpolator1DRaw.interp(x, y, xx, InterpMethod.LOG_PCHIP_HYMAN);
    System.out.println(Arrays.toString(yy));

  }

  @Test
  public void simpleInterpTestPreemptLookup() {

    double[] x = new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    double[] y = new double[] {0.0262528607281266, 0.0332008172580452,
        0.0680406527800897, 0.3325986476978349, 0.3747764345251541,
        0.4333961926133433, 0.6636063423603406, 0.8088846531977805,
        0.8107605699814449, 0.9470649378271933};
    double[] xx = new double[] {-1, 0, 0.5, 1, 1.2, 3, 3.5, 7.7, 8, 8.1,
        9, 9.5, 9.9, 10, 10.1, 12};
    double[] yy;

    // preemptive reusable lookups
    int[] lookups = PPval.lookup_ppval(x, xx);

    yy = Interpolator1DRaw.interp(x, y, xx, lookups, InterpMethod.LINEAR);

    System.out.println(Arrays.toString(yy));

    yy = Interpolator1DRaw.interp(x, y, xx, lookups, InterpMethod.LOG_PCHIP_HYMAN);
    System.out.println(Arrays.toString(yy));
  }

  @Test
  public void simpleInterpTestextractPP() {

    double[] x = new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    double[] y = new double[] {0.0262528607281266, 0.0332008172580452,
        0.0680406527800897, 0.3325986476978349, 0.3747764345251541,
        0.4333961926133433, 0.6636063423603406, 0.8088846531977805,
        0.8107605699814449, 0.9470649378271933};
    double[] xx = new double[] {-1, 0, 0.5, 1, 1.2, 3, 3.5, 7.7, 8, 8.1,
        9, 9.5, 9.9, 10, 10.1, 12};
    PP_t pp;
    double[] yy;

    // caching pp's
    pp = Interpolator1DRaw.interp(x, y, InterpMethod.LINEAR);
    yy = Interpolator1DRaw.interp(xx, InterpMethod.LINEAR, pp);

    System.out.println(Arrays.toString(yy));

    pp = Interpolator1DRaw.interp(x, y, InterpMethod.LOG_PCHIP_HYMAN);
    yy = Interpolator1DRaw.interp(xx, InterpMethod.LOG_PCHIP_HYMAN, pp);
    System.out.println(Arrays.toString(yy));
  }

  @Test
  public void simpleInterpTestextractPPandPreemptLookup() {

    double[] x = new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
    double[] y = new double[] {0.0262528607281266, 0.0332008172580452,
        0.0680406527800897, 0.3325986476978349, 0.3747764345251541,
        0.4333961926133433, 0.6636063423603406, 0.8088846531977805,
        0.8107605699814449, 0.9470649378271933};
    double[] xx = new double[] {-1, 0, 0.5, 1, 1.2, 3, 3.5, 7.7, 8, 8.1,
        9, 9.5, 9.9, 10, 10.1, 12};
    PP_t pp;
    double[] yy;

    // preemptive reusable lookups
    int[] lookups = PPval.lookup_ppval(x, xx);

    pp = Interpolator1DRaw.interp(x, y, InterpMethod.LINEAR);
    yy = Interpolator1DRaw.interp(xx, lookups, InterpMethod.LINEAR, pp);

    System.out.println(Arrays.toString(yy));

    pp = Interpolator1DRaw.interp(x, y, InterpMethod.LOG_PCHIP_HYMAN);
    yy = Interpolator1DRaw.interp(xx, lookups, InterpMethod.LOG_PCHIP_HYMAN,
        pp);
    System.out.println(Arrays.toString(yy));
  }

}
