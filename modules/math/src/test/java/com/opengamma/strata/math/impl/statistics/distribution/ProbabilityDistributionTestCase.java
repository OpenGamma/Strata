/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.offset;

import com.opengamma.strata.math.impl.cern.MersenneTwister;
import com.opengamma.strata.math.impl.cern.MersenneTwister64;
import com.opengamma.strata.math.impl.cern.RandomEngine;

/**
 * Abstract test.
 */
public abstract class ProbabilityDistributionTestCase {

  protected static final double EPS = 1e-5;
  protected static final RandomEngine ENGINE = new MersenneTwister64(MersenneTwister.DEFAULT_SEED);

  protected void assertCDF(final double[] p, final double[] x, final ProbabilityDistribution<Double> dist) {
    assertCDFWithNull(dist);
    for (int i = 0; i < p.length; i++) {
      assertThat(dist.getCDF(x[i])).isCloseTo(p[i], offset(EPS));
    }
  }

  protected void assertPDF(final double[] z, final double[] x, final ProbabilityDistribution<Double> dist) {
    assertPDFWithNull(dist);
    for (int i = 0; i < z.length; i++) {
      assertThat(dist.getPDF(x[i])).isCloseTo(z[i], offset(EPS));
    }
  }

  protected void assertInverseCDF(final double[] x, final ProbabilityDistribution<Double> dist) {
    assertInverseCDFWithNull(dist);
    for (final double d : x) {
      assertThat(dist.getInverseCDF(dist.getCDF(d))).isCloseTo(d, offset(EPS));
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> dist.getInverseCDF(3.4));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> dist.getInverseCDF(-0.2));
  }

  protected void assertInverseCDFWithNull(final ProbabilityDistribution<Double> dist) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> dist.getInverseCDF(null));
  }

  protected void assertPDFWithNull(final ProbabilityDistribution<Double> dist) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> dist.getPDF(null));
  }

  protected void assertCDFWithNull(final ProbabilityDistribution<Double> dist) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> dist.getCDF(null));
  }
}
