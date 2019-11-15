/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Tests {@link ExponentiallyWeightedInterpolationQuantileMethod}.
 */

public class ExponentiallyWeightedInterpolationQuantileMethodTest {

  private static final DoubleArray DATA_123 = DoubleArray.ofUnsafe(new double[] {
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

  @Test
  public void lambda_negative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ExponentiallyWeightedInterpolationQuantileMethod(-0.10d));
  }

  @Test
  public void lambda_zero() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ExponentiallyWeightedInterpolationQuantileMethod(0.0d));
  }

  @Test
  public void lambda_above_1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ExponentiallyWeightedInterpolationQuantileMethod(1.10d));
  }

  @Test
  public void quantile_not_extrapolated() {
    double level = 0.999;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> METHOD.quantileFromUnsorted(level, DATA_123));
  }

  @Test
  public void quantile_last() {
    double level = 0.999;
    double qComputed = METHOD.quantileWithExtrapolationFromUnsorted(level, DATA_123);
    DoubleArray dataSorted = DATA_123.sorted();
    assertThat(qComputed).as("Quantile.").isCloseTo(dataSorted.get(dataSorted.size() - 1), offset(TOLERANCE_WEIGHT));
  }

  @Test
  public void quantile() {
    double[] level = {0.98, 0.981, 0.9811, 0.97};
    for (int i = 0; i < level.length; i++) {
      check_quantile(level[i]);
    }
  }

  private void check_quantile(double level) {
    double[] w = METHOD.weights(DATA_123.size());
    double qComputed = METHOD.quantileFromUnsorted(level, DATA_123);
    double wi1 = 0.0d;
    int nbW = 0;
    for (int i = 0; i < DATA_123.size(); i++) {
      if (DATA_123.get(i) > qComputed) {
        wi1 += w[i];
        nbW++;
      }
    }
    assertThat(wi1 < 1.0d - level).as("Weight of tail lower than level").isTrue();
    double[] w2 = w.clone();
    double[] data = DATA_123.toArray();
    DoubleArrayMath.sortPairs(data, w2);
    double wi = wi1 + w2[w.length - 1 - nbW];
    assertThat(wi > 1.0d - level).as("Weight of tail+1 larger than level").isTrue();
    double alpha = (wi - (1 - level)) / (wi - wi1);
    double qExpected = (1 - alpha) * data[w.length - 1 - nbW] + alpha * data[w.length - 1 - nbW + 1];
    assertThat(qComputed).as("Quantile.").isCloseTo(qExpected, offset(TOLERANCE_WEIGHT));
  }

  @Test
  public void weights() {
    double[] wComputed = METHOD.weights(DATA_123.size());
    assertThat(wComputed.length).as("Weight size is same as sample size").isEqualTo(DATA_123.size());
    double wTotal = 0.0d;
    for (int i = 0; i < wComputed.length; i++) {
      wTotal += wComputed[i];
    }
    assertThat(wTotal).as("Total weight should be 1.").isCloseTo(1.0, offset(TOLERANCE_WEIGHT));
    for (int i = 0; i < wComputed.length - 1; i++) {
      assertThat(wComputed[i + 1]).as("Ratio between weights.").isCloseTo(wComputed[i] / LAMBDA, offset(TOLERANCE_WEIGHT));
    }
  }

  @Test
  public void quantile_details() {
    double[] level = {0.98, 0.981, 0.9811, 0.97};
    for (int i = 0; i < level.length; i++) {
      double q = METHOD.quantileFromUnsorted(level[i], DATA_123);
      QuantileResult r = METHOD.quantileDetailsFromUnsorted(level[i], DATA_123);
      assertThat(r.getValue()).isCloseTo(q, offset(TOLERANCE_QUANTILE));
      assertThat(r.getIndices().length).isEqualTo(r.getWeights().size());
      double qExpected = 0.0;
      for (int j = 0; j < r.getIndices().length; j++) { // Recompute quantile from details
        qExpected += DATA_123.get(r.getIndices()[j]) * r.getWeights().get(j);
      }
      assertThat(qExpected).isCloseTo(q, offset(TOLERANCE_QUANTILE));
    }
  }

  @Test
  public void es_details() {
    double[] level = {0.98, 0.981, 0.9811, 0.97};
    for (int i = 0; i < level.length; i++) {
      double es = METHOD.expectedShortfallFromUnsorted(level[i], DATA_123);
      QuantileResult r = METHOD.expectedShortfallDetailsFromUnsorted(level[i], DATA_123);
      assertThat(r.getValue()).isCloseTo(es, offset(TOLERANCE_QUANTILE));
      assertThat(r.getIndices().length).isEqualTo(r.getWeights().size());
      double qExpected = 0.0;
      for (int j = 0; j < r.getIndices().length; j++) { // Recompute ES from details
        qExpected += DATA_123.get(r.getIndices()[j]) * r.getWeights().get(j);
      }
      assertThat(qExpected).isCloseTo(es, offset(TOLERANCE_QUANTILE));
    }
  }

/* Compare Expected shortfall with numerical integral on the VaR. */

  @Test
  public void es() {
    double level = 0.95;
    double es = METHOD.expectedShortfallFromUnsorted(level, DATA_123);
    double q = METHOD.quantileFromUnsorted(level, DATA_123);
    assertThat(es > q).isTrue();
    int nbPts = 20;
    double esExpected = 0.0d;
    for (int i = 0; i < nbPts; i++) {
      double qIntegral = level + i / (nbPts - 1.0d) * (1 - level);
      esExpected += ((i == 0 || i == nbPts - 1) ? 0.5 : 1.0d) * METHOD.quantileWithExtrapolationFromUnsorted(qIntegral, DATA_123); // Trapezoid method
    }
    esExpected /= (nbPts - 1);
    assertThat(es).isCloseTo(esExpected, offset(TOLERANCE_ES_NI));
  }

  @Test
  public void es_extreme() {
    double level = 0.999;
    double es = METHOD.expectedShortfallFromUnsorted(level, DATA_123);
    double q = METHOD.quantileWithExtrapolationFromUnsorted(level, DATA_123);
    assertThat(es).isCloseTo(q, offset(TOLERANCE_QUANTILE));
  }

}
