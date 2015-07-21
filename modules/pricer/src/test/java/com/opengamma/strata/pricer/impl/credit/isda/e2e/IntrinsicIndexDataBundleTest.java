/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda.e2e;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;
import java.util.BitSet;

import org.testng.annotations.Test;

import com.opengamma.strata.pricer.impl.credit.isda.IsdaBaseTest;
import com.opengamma.strata.pricer.impl.credit.isda.IsdaCompliantCreditCurve;

/**
 * 
 */
@Test
public class IntrinsicIndexDataBundleTest extends IsdaBaseTest {
  private static final IsdaCompliantCreditCurve[] CREDIT_CURVES = CdsIndexProvider.getCDX_NA_HY_20140213_CreditCurves();
  private static final double[] RECOVERY_RATES = CdsIndexProvider.CDX_NA_HY_20140213_RECOVERY_RATES;

  /**
   * 
   */
  @SuppressWarnings("unused")
  @Test
  public void defaultedNamesTest() {
    final int[] defaultedIndex = new int[] {5, 14, 22, 45 };
    final int indexSize = CREDIT_CURVES.length;
    BitSet defaulted = new BitSet(indexSize);
    IntrinsicIndexDataBundle intrinsicData = new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES);
    final IntrinsicIndexDataBundle intrinsicDataNoDefault = new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, defaulted);
    assertTrue(checkEqual(intrinsicData, intrinsicDataNoDefault, 1.e-15));

    final double[] rrCp = Arrays.copyOf(RECOVERY_RATES, indexSize);
    rrCp[33] = -0.4;
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, rrCp);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("recovery rate must be between 0 and 1.Value of " + rrCp[33] + " given at index " + 33, e.getMessage());
    }
    rrCp[33] = 1.4;
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, rrCp);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("recovery rate must be between 0 and 1.Value of " + rrCp[33] + " given at index " + 33, e.getMessage());
    }

    IntrinsicIndexDataBundle intrinsicDataRec = new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES);
    intrinsicData = intrinsicData.withDefault(defaultedIndex);
    final IsdaCompliantCreditCurve[] creditCurveDefaulted = Arrays.copyOf(CREDIT_CURVES, indexSize);

    for (int i = 0; i < defaultedIndex.length; ++i) {
      defaulted.set(defaultedIndex[i]);
      creditCurveDefaulted[defaultedIndex[i]] = null;
      intrinsicDataRec = intrinsicDataRec.withDefault(defaultedIndex[i]);
    }
    final IntrinsicIndexDataBundle intrinsicDataDefaulted = new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, defaulted);
    assertTrue(checkEqual(intrinsicData, intrinsicDataDefaulted, 1.e-13));
    assertEquals(intrinsicData.getCreditCurves(), intrinsicDataDefaulted.getCreditCurves());
    assertTrue(checkEqual(intrinsicData, intrinsicDataRec, 1.e-13));
    assertEquals(intrinsicData.getCreditCurves(), intrinsicDataRec.getCreditCurves());

    final IntrinsicIndexDataBundle intrinsicDataWithCurves = intrinsicDataDefaulted.withCreditCurves(creditCurveDefaulted);
    assertTrue(checkEqual(intrinsicData, intrinsicDataWithCurves, 1.e-13));

    /*
     * throw exception
     */
    BitSet longSet = new BitSet(indexSize + 10);
    longSet.set(indexSize + 3);
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, longSet);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Length of defaulted (" + (indexSize + 3 + 1) + ") is greater than index size (" + indexSize + ")", e.getMessage());
    }

    final double[] shortRR = Arrays.copyOf(RECOVERY_RATES, indexSize - 1);
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, shortRR);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Length of recoveryRates (" + (indexSize - 1) + ") does not match index size (" + indexSize + ")", e.getMessage());
    }
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, shortRR, defaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Length of recoveryRates (" + (indexSize - 1) + ") does not match index size (" + indexSize + ")", e.getMessage());
    }

    creditCurveDefaulted[12] = null;
    try {
      intrinsicData.withCreditCurves(creditCurveDefaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("null curve at index 12, but this is not listed as defaulted", e.getMessage());
    }
    try {
      new IntrinsicIndexDataBundle(creditCurveDefaulted, RECOVERY_RATES, defaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Null credit curve, but not set as defaulted in alive list. Index is " + 12, e.getMessage());
    }
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, rrCp, defaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("recovery rate must be between 0 and 1.Value of " + rrCp[33] + " given at index " + 33, e.getMessage());
    }
    rrCp[33] = -2.4;
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, rrCp, defaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("recovery rate must be between 0 and 1.Value of " + rrCp[33] + " given at index " + 33, e.getMessage());
    }
    try {
      intrinsicData.withDefault(14);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Index " + 14 + " is already defaulted", e.getMessage());
    }
    try {
      intrinsicData.withDefault(new int[] {11, 15, 22 });
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Index " + 22 + " is already defaulted", e.getMessage());
    }

    final IsdaCompliantCreditCurve[] shortCC = Arrays.copyOf(CREDIT_CURVES, indexSize - 2);
    try {
      intrinsicDataNoDefault.withCreditCurves(shortCC);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("wrong number of curves. Require " + indexSize + ", but " + (indexSize - 2) + " given", e.getMessage());
    }
    try {
      intrinsicDataNoDefault.withDefault(indexSize);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("index (" + indexSize + ") should be smaller than index size (" + indexSize + ")", e.getMessage());
    }
    try {
      intrinsicDataNoDefault.withDefault(1, 4, indexSize + 2);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("index (" + (indexSize + 2) + ") should be smaller than index size (" + indexSize + ")", e.getMessage());
    }
  }

  /**
   * 
   */
  @SuppressWarnings("unused")
  @Test
  public void differentWeightTest() {
    final int indexSize = CREDIT_CURVES.length;
    final double[] weights = new double[indexSize];
    Arrays.fill(weights, 1.0 / indexSize);
    final double[] diff = new double[] {weights[0] * 0.1, weights[0] * 0.2, weights[0] * 0.3, weights[0] * 0.4, weights[0] * 0.5 };
    weights[2] += diff[0];
    weights[5] -= diff[0];
    weights[13] += diff[1];
    weights[22] -= diff[1];
    weights[42] += diff[2];
    weights[55] -= diff[2];
    weights[58] += diff[3];
    weights[78] -= diff[3];
    weights[79] += diff[4];
    weights[82] -= diff[4];
    final IntrinsicIndexDataBundle bundle = new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, weights);
    for (int i = 0; i < indexSize; ++i) {
      assertEquals(weights[i], bundle.getWeight(i));
    }

    final int[] defaultedIndex = new int[] {15, 34, 22, 65 };

    final IntrinsicIndexDataBundle bundleDefaulted = bundle.withDefault(defaultedIndex);
    IntrinsicIndexDataBundle bundleToDefaulted = new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, weights);

    BitSet defaulted = new BitSet(indexSize);
    final IntrinsicIndexDataBundle bundleNoDefault = new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, weights, defaulted);
    assertTrue(checkEqual(bundle, bundleNoDefault, 1.e-13));

    final IsdaCompliantCreditCurve[] ccCopy = Arrays.copyOf(CREDIT_CURVES, indexSize);
    ccCopy[defaultedIndex[1]] = null;//null is allowed if defaulted
    for (int i = 0; i < defaultedIndex.length; ++i) {
      defaulted.set(defaultedIndex[i]);
      bundleToDefaulted = bundleToDefaulted.withDefault(defaultedIndex[i]);
    }
    final IntrinsicIndexDataBundle bundleWithBitSet = new IntrinsicIndexDataBundle(ccCopy, RECOVERY_RATES, weights, defaulted);

    assertTrue(checkEqual(bundleWithBitSet, bundleDefaulted, 1.e-13));
    assertTrue(checkEqual(bundleWithBitSet, bundleToDefaulted, 1.e-13));

    /*
     * throw exception
     */
    BitSet longSet = new BitSet(indexSize + 10);
    longSet.set(indexSize + 4);
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, weights, longSet);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Length of defaulted (" + (indexSize + 4 + 1) + ") is greater than index size (" + indexSize + ")", e.getMessage());
    }

    final double[] shortRR = Arrays.copyOf(RECOVERY_RATES, indexSize - 1);
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, shortRR, weights, defaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Length of recoveryRates (" + (indexSize - 1) + ") does not match index size (" + indexSize + ")", e.getMessage());
    }
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, shortRR, weights);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Length of recoveryRates (" + (indexSize - 1) + ") does not match index size (" + indexSize + ")", e.getMessage());
    }
    final double[] longWeights = new double[indexSize + 1];
    Arrays.fill(longWeights, 1. / (indexSize + 1.));
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, longWeights);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Length of weights (" + (indexSize + 1) + ") does not match index size (" + indexSize + ")", e.getMessage());
    }
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, longWeights, defaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Length of weights (" + (indexSize + 1) + ") does not match index size (" + indexSize + ")", e.getMessage());
    }

    weights[14] *= -1.;
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, weights);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("weights must be positive. Value of " + weights[14] + " given at index " + 14, e.getMessage());
    }
    weights[14] *= -1.;
    weights[14] *= 10.;
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, weights);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
    weights[14] *= 0.1;

    final double[] rrCp = Arrays.copyOf(RECOVERY_RATES, indexSize);
    rrCp[24] *= -1.;
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, rrCp, weights);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("recovery rate must be between 0 and 1.Value of " + rrCp[24] + " given at index " + 24, e.getMessage());
    }
    rrCp[24] = 3.;
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, rrCp, weights);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("recovery rate must be between 0 and 1.Value of " + rrCp[24] + " given at index " + 24, e.getMessage());
    }

    final IsdaCompliantCreditCurve[] cc = Arrays.copyOf(CREDIT_CURVES, indexSize);
    cc[23] = null;
    try {
      new IntrinsicIndexDataBundle(cc, RECOVERY_RATES, weights, defaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("Null credit curve, but not set as defaulted in alive list. Index is " + 23, e.getMessage());
    }
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, rrCp, weights, defaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("recovery rate must be between 0 and 1.Value of " + rrCp[24] + " given at index " + 24, e.getMessage());
    }
    rrCp[24] = -3.;
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, rrCp, weights, defaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("recovery rate must be between 0 and 1.Value of " + rrCp[24] + " given at index " + 24, e.getMessage());
    }
    weights[56] *= -1.;
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, weights, defaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertEquals("weights must be positive. Value of " + weights[56] + " given at index " + 56, e.getMessage());
    }
    weights[56] = 5.1;
    try {
      new IntrinsicIndexDataBundle(CREDIT_CURVES, RECOVERY_RATES, weights, defaulted);
      throw new RuntimeException();
    } catch (final Exception e) {
      assertTrue(e instanceof IllegalArgumentException);
    }
  }

  private boolean checkEqual(final IntrinsicIndexDataBundle bundle1, final IntrinsicIndexDataBundle bundle2, final double tol) {
    if (bundle1.getIndexSize() != bundle2.getIndexSize()) {
      return false;
    }
    if (bundle1.getNumOfDefaults() != bundle2.getNumOfDefaults()) {
      return false;
    }
    if (Math.abs(bundle1.getIndexFactor() - bundle2.getIndexFactor()) > tol) {
      return false;
    }
    final int size = bundle1.getIndexSize();

    for (int i = 0; i < size; ++i) {
      if (Math.abs(bundle1.getWeight(i) - bundle2.getWeight(i)) > tol) {
        return false;
      }
      if (Math.abs(bundle1.getLGD(i) - bundle2.getLGD(i)) > tol) {
        return false;
      }
      if (bundle1.isDefaulted(i) != bundle2.isDefaulted(i)) {
        return false;
      }
      /*
       * Null is allowed if defaulted
       */
      if (!bundle1.isDefaulted(i)) {
        if (!(bundle1.getCreditCurve(i).equals(bundle2.getCreditCurve(i)))) {
          return false;
        }
      }
    }
    return true;
  }
}
