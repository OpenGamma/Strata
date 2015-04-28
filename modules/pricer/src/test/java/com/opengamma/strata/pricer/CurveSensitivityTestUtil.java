/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.sensitivity.ZeroRateSensitivity;

/**
 * Test utilities for curve sensitivity.
 */
public abstract class CurveSensitivityTestUtil {

  /**
   * @param computed Computed sensitivity
   * @param expected Expected sensitivity
   * @param absTol Absolute expected
   */
  static public void assertMulticurveSensitivity(
      PointSensitivities computed,
      PointSensitivities expected,
      double absTol) {
    List<ZeroRateSensitivity> dscListComputed = new ArrayList<ZeroRateSensitivity>();
    List<ZeroRateSensitivity> dscListExpected = new ArrayList<ZeroRateSensitivity>();
    List<IborRateSensitivity> iborListComputed = new ArrayList<IborRateSensitivity>();
    List<IborRateSensitivity> iborListExpected = new ArrayList<IborRateSensitivity>();
    List<OvernightRateSensitivity> onListComputed = new ArrayList<OvernightRateSensitivity>();
    List<OvernightRateSensitivity> onListExpected = new ArrayList<OvernightRateSensitivity>();
    List<PointSensitivity> listComputed = computed.getSensitivities();
    List<PointSensitivity> listExpected = expected.getSensitivities();

    int totalSize = listExpected.size();
    assertEquals(listComputed.size(), totalSize);
    createSensitivityLists(listComputed, iborListComputed, onListComputed, dscListComputed);
    createSensitivityLists(listExpected, iborListExpected, onListExpected, dscListExpected);
    // dsc
    if (dscListExpected.isEmpty()) {
      assertTrue(dscListComputed.isEmpty());
    } else {
      int nSense = dscListExpected.size();
      assertEquals(dscListComputed.size(), nSense);
      dscListComputed.sort(zrComparator);
      dscListExpected.sort(zrComparator);
      for (int i = 0; i < nSense; ++i) {
        ZeroRateSensitivity senseComputed = dscListComputed.get(i);
        ZeroRateSensitivity senseExpected = dscListExpected.get(i);
        assertEquals(senseComputed.getCurrency(), senseExpected.getCurrency());
        assertEquals(senseComputed.getDate(), senseExpected.getDate());
        assertEquals(senseComputed.getSensitivity(), senseExpected.getSensitivity(), absTol);
      }
    }
    // fwd - Ibor
    if (iborListExpected.isEmpty()) {
      assertTrue(iborListComputed.isEmpty());
    } else {
      int nSense = iborListExpected.size();
      assertEquals(iborListComputed.size(), nSense);
      iborListComputed.sort(iborComparator);
      iborListExpected.sort(iborComparator);
      for (int i = 0; i < nSense; ++i) {
        IborRateSensitivity senseComputed = iborListComputed.get(i);
        IborRateSensitivity senseExpected = iborListExpected.get(i);
        assertEquals(senseComputed.getCurrency(), senseExpected.getCurrency());
        assertEquals(senseComputed.getDate(), senseExpected.getDate());
        assertEquals(senseComputed.getFixingDate(), senseExpected.getFixingDate());
        assertEquals(senseComputed.getIndex(), senseExpected.getIndex());
        assertEquals(senseComputed.getSensitivity(), senseExpected.getSensitivity(), absTol);
      }
    }
    // fwd - ON 
    if (onListExpected.isEmpty()) {
      assertTrue(onListComputed.isEmpty());
    } else {
      int nSense = onListExpected.size();
      assertEquals(onListComputed.size(), nSense);
      onListComputed.sort(onComparator);
      onListExpected.sort(onComparator);
      for (int i = 0; i < nSense; ++i) {
        OvernightRateSensitivity senseComputed = onListComputed.get(i);
        OvernightRateSensitivity senseExpected = onListExpected.get(i);
        assertEquals(senseComputed.getCurrency(), senseExpected.getCurrency());
        assertEquals(senseComputed.getDate(), senseExpected.getDate());
        assertEquals(senseComputed.getFixingDate(), senseExpected.getFixingDate());
        assertEquals(senseComputed.getIndex(), senseExpected.getIndex());
        assertEquals(senseComputed.getSensitivity(), senseExpected.getSensitivity(), absTol);
      }
    }
  }

  private static void createSensitivityLists(
      List<PointSensitivity> originalList,
      List<IborRateSensitivity> iborList,
      List<OvernightRateSensitivity> onList,
      List<ZeroRateSensitivity> dscList) {
    int totalSize = originalList.size();
    for (int i = 0; i < totalSize; ++i) {
      PointSensitivity sense = originalList.get(i);
      if (sense instanceof ZeroRateSensitivity) {
        dscList.add((ZeroRateSensitivity) sense);
      }
      if (sense instanceof IborRateSensitivity) {
        iborList.add((IborRateSensitivity) sense);
      }
      if (sense instanceof OvernightRateSensitivity) {
        onList.add((OvernightRateSensitivity) sense);
      }
    }
  }

  private static final Comparator<ZeroRateSensitivity> zrComparator = new Comparator<ZeroRateSensitivity>() {
    @Override
    public int compare(ZeroRateSensitivity zrSensi1, ZeroRateSensitivity zrSensi2) {
      return zrSensi1.getDate().compareTo(zrSensi2.getDate());
    }
  };

  private static final Comparator<IborRateSensitivity> iborComparator = new Comparator<IborRateSensitivity>() {
    @Override
    public int compare(IborRateSensitivity fwdSensi1, IborRateSensitivity fwdSensi2) {
      return fwdSensi1.getFixingDate().compareTo(fwdSensi2.getFixingDate());
    }
  };

  private static final Comparator<OvernightRateSensitivity> onComparator = new Comparator<OvernightRateSensitivity>() {
    @Override
    public int compare(OvernightRateSensitivity fwdSensi1, OvernightRateSensitivity fwdSensi2) {
      return fwdSensi1.getFixingDate().compareTo(fwdSensi2.getFixingDate());
    }
  };

}
