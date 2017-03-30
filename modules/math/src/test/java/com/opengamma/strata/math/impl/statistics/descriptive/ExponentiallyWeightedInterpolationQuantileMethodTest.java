/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Tests {@link ExponentiallyWeightedInterpolationQuantileMethod}.
 */

@Test
public class ExponentiallyWeightedInterpolationQuantileMethodTest {
  
  private static final DoubleArray DATA_123 = DoubleArray.ofUnsafe(new double[]{
      0.1746, 0.9716, 0.1963, 0.1982, 0.2020, 0.2155, 0.2222, 0.4534, 0.4690, 0.3717,
      0.0286, 0.0363, 0.0379, 0.0582, 0.0611, 0.0622, 0.9368, 0.0776, 0.0779, 0.0822,
      0.8053, 0.8075, 0.8190, 0.8190, 0.8216, 0.8234, 0.8399, 0.8421, 0.8557, 0.8914,
      0.6243, 0.8631, 0.1544, 0.1674, 0.1740, 0.2401, 0.2413, 0.2582, 0.2666, 0.9620,
      0.5807, 0.6278, 0.7532, 0.7543, 0.2936, 0.2979, 0.2998, 0.3000, 0.9524, 0.3028,
      0.3057, 0.3076, 0.3461, 0.3508, 0.9157, 0.3781, 0.4011, 0.4157, 0.4197, 0.8814,
      0.8870, 0.4285, 0.4463, 0.9077, 0.9150, 0.4481, 0.4863, 0.4878, 0.4908, 0.4942,
      0.5029, 0.5083, 0.5108, 0.5212, 0.5224, 0.5290, 0.5578, 0.5780, 0.5803, 0.6325,
      0.0849, 0.0916, 0.1055, 0.9275, 0.1358, 0.1474, 0.9731, 0.9600, 0.5921, 0.6174,
      0.6343, 0.6416, 0.6423, 0.6694, 0.6748, 0.6763, 0.6793, 0.6804, 0.6859, 0.8483,
      0.8511, 0.6460, 0.6495, 0.6504, 0.6570, 0.9570, 0.6666, 0.6862, 0.7136, 0.7145,
      0.7289, 0.7291, 0.9813, 0.7360, 0.7444, 0.7602, 0.7714, 0.7768, 0.8024, 0.1841,
      0.8963, 0.9198, 0.3734});
  private static final double LAMBDA = 0.995;
  private static final ExponentiallyWeightedInterpolationQuantileMethod METHOD =
      new ExponentiallyWeightedInterpolationQuantileMethod(LAMBDA);

  private static final double TOLERANCE_WEIGHT = 1.0E-6;
  private static final double TOLERANCE_QUANTILE = 1.0E-6;
  private static final double TOLERANCE_ES_NI = 1.0E-5;


  public void lambda_negative() {
    assertThrowsIllegalArg(() -> new ExponentiallyWeightedInterpolationQuantileMethod(-0.10d));
  }

  public void lambda_zero() {
    assertThrowsIllegalArg(() -> new ExponentiallyWeightedInterpolationQuantileMethod(0.0d));
  }

  public void lambda_above_1() {
    assertThrowsIllegalArg(() -> new ExponentiallyWeightedInterpolationQuantileMethod(1.10d));
  }

  public void quantile_not_extrapolated() {
    double level = 0.999;
    assertThrowsIllegalArg(() -> METHOD.quantileFromUnsorted(level, DATA_123));
  }

  public void quantile_last() {
    double level = 0.999;
    double qComputed = METHOD.quantileWithExtrapolationFromUnsorted(level, DATA_123).getValue();
    DoubleArray dataSorted = DATA_123.sorted();
    assertEquals(qComputed, dataSorted.get(dataSorted.size() - 1), TOLERANCE_WEIGHT, "Quantile.");
  }

  public void quantile() {
    double[] level = {0.98, 0.981, 0.9811, 0.97};
    for (int i = 0; i < level.length; i++) {
      check_quantile(level[i]);
    }
  }

  private void check_quantile(double level) {
    double[] w = METHOD.weights(DATA_123.size());
    double qComputed = METHOD.quantileFromUnsorted(level, DATA_123).getValue();
    double WI1 = 0.0d;
    int nbW = 0;
    for (int i = 0; i < DATA_123.size(); i++) {
      if (DATA_123.get(i) > qComputed) {
        WI1 += w[i];
        nbW++;
      }
    }
    assertTrue(WI1 < 1.0d - level, "Weight of tail lower than level");
    double[] w2 = w.clone();
    double[] data = DATA_123.toArray();
    DoubleArrayMath.sortPairs(data, w2);
    double WI = WI1 + w2[w.length - 1 - nbW];
    assertTrue(WI > 1.0d - level, "Weight of tail+1 larger than level");
    double alpha = (WI - (1 - level)) / (WI - WI1);
    double qExpected = (1 - alpha) * data[w.length - 1 - nbW] + alpha * data[w.length - 1 - nbW + 1];
    assertEquals(qComputed, qExpected, TOLERANCE_WEIGHT, "Quantile.");
  }

  public void weights() {
    double[] wComputed = METHOD.weights(DATA_123.size());
    assertEquals(wComputed.length, DATA_123.size(), "Weight size is same as sample size");
    double wTotal = 0.0d;
    for (int i = 0; i < wComputed.length; i++) {
      wTotal += wComputed[i];
    }
    assertEquals(wTotal, 1.0, TOLERANCE_WEIGHT, "Total weight should be 1.");
    for (int i = 0; i < wComputed.length - 1; i++) {
      assertEquals(wComputed[i + 1], wComputed[i] / LAMBDA, TOLERANCE_WEIGHT, "Ratio between weights.");
    }
  }

  public void quantile_details() {
    double[] level = {0.98, 0.981, 0.9811, 0.97};
    for (int i = 0; i < level.length; i++) {
      double q = METHOD.quantileFromUnsorted(level[i], DATA_123).getValue();
      QuantileResult r = METHOD.quantileDetailsFromUnsorted(level[i], DATA_123);
      assertEquals(r.getValue(), q, TOLERANCE_QUANTILE);
      assertEquals(r.getIndices().length, r.getWeights().size());
      double qExpected = 0.0;
      for (int j = 0; j < r.getIndices().length; j++) { // Recompute quantile from details
        qExpected += DATA_123.get(r.getIndices()[j]) * r.getWeights().get(j);
      }
      assertEquals(qExpected, q, TOLERANCE_QUANTILE);
    }
  }

  public void es_details() {
    double[] level = {0.98, 0.981, 0.9811, 0.97};
    for (int i = 0; i < level.length; i++) {
      double es = METHOD.expectedShortfallFromUnsorted(level[i], DATA_123).getValue();
      QuantileResult r = METHOD.expectedShortfallDetailsFromUnsorted(level[i], DATA_123);
      assertEquals(r.getValue(), es, TOLERANCE_QUANTILE);
      assertEquals(r.getIndices().length, r.getWeights().size());
      double qExpected = 0.0;
      for (int j = 0; j < r.getIndices().length; j++) { // Recompute ES from details
        qExpected += DATA_123.get(r.getIndices()[j]) * r.getWeights().get(j);
      }
      assertEquals(qExpected, es, TOLERANCE_QUANTILE);
    }
  }

/* Compare Expected shortfall with numerical integral on the VaR. */

  public void es() {
    double level = 0.95;
    double es = METHOD.expectedShortfallFromUnsorted(level, DATA_123).getValue();
    double q = METHOD.quantileFromUnsorted(level, DATA_123).getValue();
    assertTrue(es > q);
    int nbPts = 20;
    double esExpected = 0.0d;
    for (int i = 0; i < nbPts; i++) {
      double qIntegral = level + i / (nbPts - 1.0d) * (1 - level);
      esExpected += ((i == 0 || i == nbPts - 1) ? 0.5 : 1.0d)
          * METHOD.quantileWithExtrapolationFromUnsorted(qIntegral, DATA_123).getValue(); // Trapezoid method
    }
    esExpected /= (nbPts - 1);
    assertEquals(es, esExpected, TOLERANCE_ES_NI);
  }

  public void es_extreme() {
    double level = 0.999;
    double es = METHOD.expectedShortfallFromUnsorted(level, DATA_123).getValue();
    double q = METHOD.quantileWithExtrapolationFromUnsorted(level, DATA_123).getValue();
    assertEquals(es, q, TOLERANCE_QUANTILE);
  }

}
