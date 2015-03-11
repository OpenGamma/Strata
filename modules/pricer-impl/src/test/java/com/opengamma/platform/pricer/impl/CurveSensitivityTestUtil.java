package com.opengamma.platform.pricer.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Comparator;
import java.util.List;

import com.opengamma.platform.pricer.sensitivity.multicurve.ForwardRateSensitivityLD;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;
import com.opengamma.platform.pricer.sensitivity.multicurve.ZeroRateSensitivityLD;

public abstract class CurveSensitivityTestUtil {

  static public void assertMulticurveSensitivity3LD(MulticurveSensitivity3LD computed, MulticurveSensitivity3LD expected,
      double absTol) {
    // dsc
    List<ZeroRateSensitivityLD> dscListComputed = computed.getZeroRateSensitivities();
    List<ZeroRateSensitivityLD> dscListExpected = expected.getZeroRateSensitivities();
    if (dscListExpected.isEmpty()) {
      assertTrue(dscListComputed.isEmpty());
    } else {
      int nSense = dscListExpected.size();
      assertEquals(dscListComputed.size(), nSense);
      dscListComputed.sort(zrComparator);
      dscListExpected.sort(zrComparator);
      for (int i = 0; i < nSense; ++i) {
        ZeroRateSensitivityLD senseComputed = dscListComputed.get(i);
        ZeroRateSensitivityLD senseExpected = dscListExpected.get(i);
        assertEquals(senseComputed.getCurrencyDiscount(), senseExpected.getCurrencyDiscount());
        assertEquals(senseComputed.getCurrencySensitivity(), senseExpected.getCurrencySensitivity());
        assertEquals(senseComputed.getDate(), senseExpected.getDate());
        assertEquals(senseComputed.getValue(), senseExpected.getValue(), absTol);
      }
    }
    // fwd
    List<ForwardRateSensitivityLD> fwdListComputed = computed.getForwardRateSensitivities();
    List<ForwardRateSensitivityLD> fwdListExpected = expected.getForwardRateSensitivities();
    if (fwdListExpected.isEmpty()) {
      assertTrue(fwdListComputed.isEmpty());
    } else {
      int nSense = fwdListExpected.size();
      assertEquals(fwdListComputed.size(), nSense);
      fwdListComputed.sort(fwdComparator);
      fwdListExpected.sort(fwdComparator);
      for (int i = 0; i < nSense; ++i) {
        ForwardRateSensitivityLD senseComputed = fwdListComputed.get(i);
        ForwardRateSensitivityLD senseExpected = fwdListExpected.get(i);
        assertEquals(senseComputed.getCurrency(), senseExpected.getCurrency());
        assertEquals(senseComputed.getFixingDate(), senseExpected.getFixingDate());
        assertEquals(senseComputed.getIndex(), senseExpected.getIndex());
        assertEquals(senseComputed.getValue(), senseExpected.getValue(), absTol);
      }
    }
  }

  private static final Comparator<ZeroRateSensitivityLD> zrComparator = new Comparator<ZeroRateSensitivityLD>() {
    @Override
    public int compare(ZeroRateSensitivityLD zrSensi1, ZeroRateSensitivityLD zrSensi2) {
      return zrSensi1.getDate().compareTo(zrSensi2.getDate());
    }
  };

  private static final Comparator<ForwardRateSensitivityLD> fwdComparator = new Comparator<ForwardRateSensitivityLD>() {
    @Override
    public int compare(ForwardRateSensitivityLD fwdSensi1, ForwardRateSensitivityLD fwdSensi2) {
      return fwdSensi1.getFixingDate().compareTo(fwdSensi2.getFixingDate());
    }
  };
}
