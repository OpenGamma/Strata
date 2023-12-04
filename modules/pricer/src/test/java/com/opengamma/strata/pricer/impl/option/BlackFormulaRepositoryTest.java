/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static com.opengamma.strata.product.common.PutCall.CALL;
import static com.opengamma.strata.product.common.PutCall.PUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.integration.GaussHermiteQuadratureIntegrator1D;
import com.opengamma.strata.math.impl.integration.RungeKuttaIntegrator1D;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.product.common.PutCall;

/**
 * Test {@link BlackFormulaRepository}.
 */
public class BlackFormulaRepositoryTest {

  private static final double EPS = 1.e-10;
  private static final double DELTA = 1.e-6;
  private static final ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1);

  private static final double TIME_TO_EXPIRY = 4.5;
  private static final double FORWARD = 104;
  private static final double[] STRIKES_INPUT = new double[] {
      85.0, 90.0, 95.0, 100.0, 103.0, 108.0, 120.0, 150.0, 250.0};
  private static final double[] VOLS = new double[] {0.1, 0.12, 0.15, 0.2, 0.3, 0.5, 0.8};

  private static final double[][] PRE_COMPUTER_PRICES = new double[][] {
      {20.816241352493662, 21.901361401145017, 23.739999392248883, 27.103751052550102, 34.22506482807403,
          48.312929458905, 66.87809290575849},
      {17.01547107842069, 18.355904456594594, 20.492964568435653, 24.216799858954104, 31.81781516125381,
          46.52941355755593, 65.73985671517116},
      {13.655000481751557, 15.203913570037663, 17.57850003037605, 21.58860329455819, 29.58397731664536,
          44.842632571211, 64.65045683512315},
      {10.76221357246159, 12.452317171280882, 14.990716295389468, 19.207654124402573, 27.51258894693435,
          43.24555444486169, 63.606185385322505},
      {9.251680464551534, 10.990050517334176, 13.589326615797177, 17.892024398947207, 26.343236303647927,
          42.327678792768694, 62.99989771948578},
      {7.094602606393259, 8.852863501660629, 11.492701186228047, 15.876921735149438, 24.50948746286295,
          40.86105495729011, 62.02112426294542},
      {3.523029591534474, 5.0769317175689395, 7.551079210499658, 11.857770325364342, 20.641589813250427,
          37.63447312094027, 59.81944968154744},
      {0.4521972353043875, 1.0637022636084144, 2.442608010436077, 5.613178543779881, 13.579915684294491,
          31.040979917191127, 55.062112340600244},
      {1.328198130230618E-4, 0.0029567128738985232, 0.04468941116428932, 0.47558224046532205, 3.8091577630027356,
          18.03481967011267, 43.99634090899799}};

  @Test
  public void zeroVolTest() {
    boolean isCall = true;
    int n = STRIKES_INPUT.length;
    for (int i = 0; i < n; i++) {
      double intrinic = Math.max(0, FORWARD - STRIKES_INPUT[i]);
      double price = BlackFormulaRepository.price(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, 0.0, isCall);
      assertThat(intrinic).isCloseTo(price, offset(1e-15));
    }
  }

  @Test
  public void zeroExpiryTest() {
    boolean isCall = false;
    int n = STRIKES_INPUT.length;
    for (int i = 0; i < n; i++) {
      double intrinic = Math.max(0, STRIKES_INPUT[i] - FORWARD);
      double price = BlackFormulaRepository.price(FORWARD, STRIKES_INPUT[i], 0.0, 0.3, isCall);
      assertThat(intrinic).isCloseTo(price, offset(1e-15));
    }
  }

  @Test
  public void tinyVolTest() {
    double vol = 1e-4;
    boolean isCall = true;
    int n = STRIKES_INPUT.length;
    for (int i = 0; i < n; i++) {
      double intrinic = Math.max(0, FORWARD - STRIKES_INPUT[i]);
      double price = BlackFormulaRepository.price(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, vol, isCall);
      assertThat(intrinic).isCloseTo(price, offset(1e-15));
    }
  }

  @Test
  public void shortExpiryTest() {
    double t = 1e-5;
    double vol = 0.4;
    boolean isCall = false;
    int n = STRIKES_INPUT.length;
    for (int i = 0; i < n; i++) {
      double intrinic = Math.max(0, STRIKES_INPUT[i] - FORWARD);
      double price = BlackFormulaRepository.price(FORWARD, STRIKES_INPUT[i], t, vol, isCall);
      assertThat(intrinic).isCloseTo(price, offset(1e-15));
    }
  }

  @Test
  public void massiveVolTest() {
    double vol = 8.0; // 800% vol
    boolean isCall = true;
    int n = STRIKES_INPUT.length;
    for (int i = 0; i < n; i++) {
      double price = BlackFormulaRepository.price(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, vol, isCall);
      assertThat(FORWARD).isCloseTo(price, offset(1e-15));
    }
  }

  @Test
  public void zeroStikeTest() {
    boolean isCall = true;
    int n = VOLS.length;
    for (int i = 0; i < n; i++) {
      double price = BlackFormulaRepository.price(FORWARD, 0.0, TIME_TO_EXPIRY, VOLS[i], isCall);
      assertThat(FORWARD).isCloseTo(price, offset(1e-15));
    }
  }

  @Test
  public void putCallParityTest() {
    int n = VOLS.length;
    int m = STRIKES_INPUT.length;
    for (int i = 0; i < m; i++) {
      double fk = FORWARD - STRIKES_INPUT[i];
      for (int j = 0; j < n; j++) {
        double call = BlackFormulaRepository.price(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], true);
        double put = BlackFormulaRepository.price(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], false);
        assertThat(fk).isCloseTo(call - put, offset(1e-13));
      }
    }
  }

  @Test
  public void nonEdgeCaseTest() {
    boolean isCall = true;
    int n = VOLS.length;
    int m = STRIKES_INPUT.length;
    for (int i = 0; i < m; i++) {
      for (int j = 0; j < n; j++) {
        double price = BlackFormulaRepository.price(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], isCall);
        assertThat(PRE_COMPUTER_PRICES[i][j]).isCloseTo(price, offset(1e-13 * price));
      }
    }
  }

  /**
   * Tests the strikes in a range of strikes, volatilities and call/put.
   */
  @Test
  public void impliedStrike() {
    int nbStrike = STRIKES_INPUT.length;
    int nbVols = VOLS.length;
    boolean callput = false;
    for (int loopcall = 0; loopcall < 2; loopcall++) {
      callput = !callput;
      for (int loopstrike = 0; loopstrike < nbStrike; loopstrike++) {
        for (int loopVols = 0; loopVols < nbVols; loopVols++) {
          ValueDerivatives d = BlackFormulaRepository
              .priceAdjoint(FORWARD, STRIKES_INPUT[loopstrike], TIME_TO_EXPIRY, VOLS[loopVols], callput);
          double delta = d.getDerivative(0);
          double strikeOutput =
              BlackFormulaRepository.impliedStrike(delta, callput, FORWARD, TIME_TO_EXPIRY, VOLS[loopVols]);
          assertThat(STRIKES_INPUT[loopstrike]).as("Implied strike: (data " + loopstrike + " / " + callput + ")").isCloseTo(strikeOutput, offset(1.0E-8));
        }
      }
    }
  }

  /**
   * Tests the strikes in a range of strikes, volatilities and call/put.
   */
  @Test
  public void impliedStrikeDerivatives() {
    double[] delta = new double[] {0.25, -0.25, 0.49};
    boolean[] cap = new boolean[] {true, false, true};
    double[] forward = new double[] {104, 100, 10};
    double[] time = new double[] {2.5, 5.0, 0.5};
    double[] vol = new double[] {0.25, 0.10, 0.50};
    double shift = 0.000001;
    double shiftF = 0.001;
    double[] derivatives = new double[4];
    for (int loop = 0; loop < delta.length; loop++) {
      double strike = BlackFormulaRepository.impliedStrike(delta[loop], cap[loop], forward[loop], time[loop],
          vol[loop], derivatives);
      double strikeD = BlackFormulaRepository.impliedStrike(delta[loop] + shift, cap[loop], forward[loop],
          time[loop], vol[loop]);
      assertThat((strikeD - strike) / shift).as("Implied strike: derivative delta").isCloseTo(derivatives[0], offset(1.0E-3));
      double strikeF = BlackFormulaRepository.impliedStrike(delta[loop], cap[loop], forward[loop] + shiftF,
          time[loop], vol[loop]);
      assertThat((strikeF - strike) / shiftF).as("Implied strike: derivative forward").isCloseTo(derivatives[1], offset(1.0E-5));
      double strikeT = BlackFormulaRepository.impliedStrike(delta[loop], cap[loop], forward[loop], time[loop] +
          shift, vol[loop]);
      assertThat((strikeT - strike) / shift).as("Implied strike: derivative time").isCloseTo(derivatives[2], offset(1.0E-4));
      double strikeV = BlackFormulaRepository.impliedStrike(delta[loop], cap[loop], forward[loop], time[loop],
          vol[loop] + shift);
      assertThat((strikeV - strike) / shift).as("Implied strike: derivative volatility").isCloseTo(derivatives[3], offset(1.0E-3));
    }
  }

  /**
   * finite difference vs greek methods
   */
  @Test
  public void greeksTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;

    double[] upStrikes = new double[nStrikes];
    double[] dwStrikes = new double[nStrikes];
    double upFwd = FORWARD * (1. + DELTA);
    double dwFwd = FORWARD * (1. - DELTA);
    double upTime = TIME_TO_EXPIRY * (1. + DELTA);
    double dwTime = TIME_TO_EXPIRY * (1. - DELTA);
    double[] upVOLS = new double[nVols];
    double[] dwVOLS = new double[nVols];
    for (int i = 0; i < nStrikes; ++i) {
      upStrikes[i] = STRIKES_INPUT[i] * (1. + DELTA);
      dwStrikes[i] = STRIKES_INPUT[i] * (1. - DELTA);
    }
    for (int i = 0; i < nVols; ++i) {
      upVOLS[i] = VOLS[i] * (1. + DELTA);
      dwVOLS[i] = VOLS[i] * (1. - DELTA);
    }
    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double finDeltaC =
            (BlackFormulaRepository.price(upFwd, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], true) - BlackFormulaRepository
                .price(dwFwd, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], true)) / 2. / FORWARD / DELTA;
        double finDeltaP =
            (BlackFormulaRepository.price(upFwd, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], false) - BlackFormulaRepository
                .price(dwFwd, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], false)) / 2. / FORWARD / DELTA;
        assertThat(finDeltaC).isCloseTo(BlackFormulaRepository.delta(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], true), offset(Math.abs(finDeltaC) * DELTA));
        assertThat(finDeltaP).isCloseTo(BlackFormulaRepository.delta(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], false), offset(Math.abs(finDeltaP) * DELTA));

        double finDualDeltaC = (BlackFormulaRepository
            .price(FORWARD, upStrikes[i], TIME_TO_EXPIRY, VOLS[j], true) - BlackFormulaRepository.price(FORWARD,
            dwStrikes[i], TIME_TO_EXPIRY, VOLS[j], true)) /
            2. / STRIKES_INPUT[i] / DELTA;
        double finDualDeltaP = (BlackFormulaRepository.price(FORWARD, upStrikes[i], TIME_TO_EXPIRY, VOLS[j],
            false) - BlackFormulaRepository.price(FORWARD, dwStrikes[i], TIME_TO_EXPIRY, VOLS[j], false)) /
            2. /
            STRIKES_INPUT[i] / DELTA;
        assertThat(finDualDeltaC).isCloseTo(BlackFormulaRepository.dualDelta(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], true), offset(Math.abs(finDualDeltaC) * DELTA));
        assertThat(finDualDeltaP).isCloseTo(BlackFormulaRepository.dualDelta(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], false), offset(Math.abs(finDualDeltaP) * DELTA));

        double finGamma =
            (BlackFormulaRepository.delta(upFwd, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], true) - BlackFormulaRepository
                .delta(dwFwd, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], true)) / 2. / FORWARD / DELTA;
        assertThat(finGamma).isCloseTo(BlackFormulaRepository.gamma(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j]), offset(Math.abs(finGamma) * DELTA));

        double finDualGamma = (BlackFormulaRepository.dualDelta(FORWARD, upStrikes[i], TIME_TO_EXPIRY, VOLS[j],
            true) - BlackFormulaRepository.dualDelta(FORWARD, dwStrikes[i], TIME_TO_EXPIRY, VOLS[j], true)) /
            2. /
            STRIKES_INPUT[i] / DELTA;
        assertThat(finDualGamma).isCloseTo(BlackFormulaRepository.dualGamma(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j]), offset(Math.abs(finDualGamma) * DELTA));

        double finCrossGamma = (BlackFormulaRepository.dualDelta(upFwd, STRIKES_INPUT[i], TIME_TO_EXPIRY,
            VOLS[j], true) - BlackFormulaRepository.dualDelta(dwFwd, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], true)) /
            2. / FORWARD / DELTA;
        assertThat(finCrossGamma).isCloseTo(BlackFormulaRepository.crossGamma(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j]), offset(Math.abs(finCrossGamma) * DELTA));

        double finThetaC =
            -(BlackFormulaRepository.price(FORWARD, STRIKES_INPUT[i], upTime, VOLS[j], true) - BlackFormulaRepository
                .price(FORWARD, STRIKES_INPUT[i], dwTime, VOLS[j], true)) / 2. / TIME_TO_EXPIRY / DELTA;
        assertThat(finThetaC).isCloseTo(BlackFormulaRepository.driftlessTheta(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j]), offset(Math.abs(finThetaC) * DELTA));

        double finVega = (BlackFormulaRepository
            .price(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, upVOLS[j], true) - BlackFormulaRepository.price(FORWARD,
            STRIKES_INPUT[i], TIME_TO_EXPIRY, dwVOLS[j], true)) /
            2. / VOLS[j] / DELTA;
        assertThat(finVega).isCloseTo(BlackFormulaRepository.vega(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j]), offset(Math.abs(finVega) * DELTA));

        double finVanna = (BlackFormulaRepository.delta(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, upVOLS[j],
            true) - BlackFormulaRepository.delta(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, dwVOLS[j], true)) /
            2. /
            VOLS[j] / DELTA;
        assertThat(finVanna).isCloseTo(BlackFormulaRepository.vanna(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j]), offset(Math.abs(finVanna) * DELTA));

        double finDualVanna = (BlackFormulaRepository.dualDelta(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY,
            upVOLS[j], true) - BlackFormulaRepository.dualDelta(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, dwVOLS[j],
            true)) /
            2. / VOLS[j] / DELTA;
        assertThat(finDualVanna).isCloseTo(BlackFormulaRepository.dualVanna(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j]), offset(Math.abs(finDualVanna) * DELTA));

        double finVomma =
            (BlackFormulaRepository.vega(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, upVOLS[j]) - BlackFormulaRepository
                .vega(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, dwVOLS[j])) / 2. / VOLS[j] / DELTA;
        assertThat(finVomma).isCloseTo(BlackFormulaRepository.vomma(FORWARD, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j]), offset(Math.abs(finVomma) * DELTA));
      }
    }

  }

  /**
   * Large/small values for price
   */
  @Test
  public void exPriceTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.price(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, true);
        double refC1 = BlackFormulaRepository.price(0., strike, TIME_TO_EXPIRY, vol, true);
        double resC2 = BlackFormulaRepository.price(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, true);
        double refC2 = BlackFormulaRepository.price(inf, strike, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.price(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, false);
        double refP1 = BlackFormulaRepository.price(0., strike, TIME_TO_EXPIRY, vol, false);
        double resP2 = BlackFormulaRepository.price(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, false);
        double refP2 = BlackFormulaRepository.price(inf, strike, TIME_TO_EXPIRY, vol, false);
        assertThat(0.).isCloseTo(resC1, offset(EPS));
        assertThat(1.e12 * strike - strike).isCloseTo(resC2, offset(EPS * 1.e12 * strike));
        assertThat(strike - 1.e-12 * strike).isCloseTo(resP1, offset(EPS * strike));
        assertThat(0.).isCloseTo(resP2, offset(EPS * strike));

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-11).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-11));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.price(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol, true);
        double resC2 = BlackFormulaRepository.price(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.price(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol, false);
        double resP2 = BlackFormulaRepository.price(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol, false);
        double refC1 = BlackFormulaRepository.price(forward, 0., TIME_TO_EXPIRY, vol, true);
        double refC2 = BlackFormulaRepository.price(forward, inf, TIME_TO_EXPIRY, vol, true);
        double refP1 = BlackFormulaRepository.price(forward, 0., TIME_TO_EXPIRY, vol, false);
        double refP2 = BlackFormulaRepository.price(forward, inf, TIME_TO_EXPIRY, vol, false);
        assertThat(forward).isCloseTo(resC1, offset(forward * EPS));
        assertThat(0.).isCloseTo(resC2, offset(EPS));
        assertThat(1.e12 * forward).isCloseTo(resP2, offset(1.e12 * forward * EPS));
        assertThat(0.).isCloseTo(resP1, offset(EPS));

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.price(FORWARD, strike, 1e-24, vol, true);
        double resC2 = BlackFormulaRepository.price(FORWARD, strike, 1e24, vol, true);
        double resP1 = BlackFormulaRepository.price(FORWARD, strike, 1e-24, vol, false);
        double resP2 = BlackFormulaRepository.price(FORWARD, strike, 1e24, vol, false);
        double refC1 = BlackFormulaRepository.price(FORWARD, strike, 0., vol, true);
        double refC2 = BlackFormulaRepository.price(FORWARD, strike, inf, vol, true);
        double refP1 = BlackFormulaRepository.price(FORWARD, strike, 0., vol, false);
        double refP2 = BlackFormulaRepository.price(FORWARD, strike, inf, vol, false);
        assertThat(FORWARD > strike ? FORWARD - strike : 0.).isCloseTo(resC1, offset(EPS));
        assertThat(FORWARD).isCloseTo(resC2, offset(FORWARD * EPS));
        assertThat(strike).isCloseTo(resP2, offset(strike * EPS));
        assertThat(FORWARD > strike ? 0. : -FORWARD + strike).isCloseTo(resP1, offset(EPS));

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.price(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, true);
      double refC1 = BlackFormulaRepository.price(FORWARD, strike, TIME_TO_EXPIRY, 0., true);
      double resC2 = BlackFormulaRepository.price(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, true);
      double refC2 = BlackFormulaRepository.price(FORWARD, strike, TIME_TO_EXPIRY, inf, true);
      double resP1 = BlackFormulaRepository.price(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, false);
      double refP1 = BlackFormulaRepository.price(FORWARD, strike, TIME_TO_EXPIRY, 0., false);
      double resP2 = BlackFormulaRepository.price(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, false);
      double refP2 = BlackFormulaRepository.price(FORWARD, strike, TIME_TO_EXPIRY, inf, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.price(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, true);
      double resC2 = BlackFormulaRepository.price(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, true);
      double resC3 = BlackFormulaRepository.price(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, true);
      double resP1 = BlackFormulaRepository.price(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, false);
      double resP2 = BlackFormulaRepository.price(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, false);
      double resP3 = BlackFormulaRepository.price(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, false);
      double resC4 = BlackFormulaRepository.price(1.e12, 1.e12, TIME_TO_EXPIRY, vol, true);
      double resP4 = BlackFormulaRepository.price(1.e12, 1.e12, TIME_TO_EXPIRY, vol, false);

      double refC1 = BlackFormulaRepository.price(0., 0., TIME_TO_EXPIRY, vol, true);
      double refC2 = BlackFormulaRepository.price(0., inf, TIME_TO_EXPIRY, vol, true);
      double refC3 = BlackFormulaRepository.price(inf, 0., TIME_TO_EXPIRY, vol, true);
      double refP1 = BlackFormulaRepository.price(0., 0., TIME_TO_EXPIRY, vol, false);
      double refP2 = BlackFormulaRepository.price(0., inf, TIME_TO_EXPIRY, vol, false);
      double refP3 = BlackFormulaRepository.price(inf, 0., TIME_TO_EXPIRY, vol, false);
      double refC4 = BlackFormulaRepository.price(inf, inf, TIME_TO_EXPIRY, vol, true);
      double refP4 = BlackFormulaRepository.price(inf, inf, TIME_TO_EXPIRY, vol, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

      for (int k = 0; k < 8; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.price(1.e-12, strike, 1.e-12, vol, true);
        double resC2 = BlackFormulaRepository.price(1.e-12, strike, 1.e12, vol, true);
        double resC3 = BlackFormulaRepository.price(1.e12, strike, 1.e-12, vol, true);
        double resP1 = BlackFormulaRepository.price(1.e-12, strike, 1.e-12, vol, false);
        double resP2 = BlackFormulaRepository.price(1.e-12, strike, 1.e12, vol, false);
        double resP3 = BlackFormulaRepository.price(1.e12, strike, 1.e-12, vol, false);
        double resC4 = BlackFormulaRepository.price(1.e12, strike, 1.e24, vol, true);
        double resP4 = BlackFormulaRepository.price(1.e12, strike, 1.e24, vol, false);

        double refC1 = BlackFormulaRepository.price(0., strike, 0., vol, true);
        double refC2 = BlackFormulaRepository.price(0., strike, inf, vol, true);
        double refC3 = BlackFormulaRepository.price(inf, strike, 0., vol, true);
        double refP1 = BlackFormulaRepository.price(0., strike, 0., vol, false);
        double refP2 = BlackFormulaRepository.price(0., strike, inf, vol, false);
        double refP3 = BlackFormulaRepository.price(inf, strike, 0., vol, false);
        double refC4 = BlackFormulaRepository.price(inf, strike, inf, vol, true);
        double refP4 = BlackFormulaRepository.price(inf, strike, inf, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.price(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, true);
      double resC2 = BlackFormulaRepository.price(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, true);
      double resC3 = BlackFormulaRepository.price(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, true);
      double resP1 = BlackFormulaRepository.price(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, false);
      double resP2 = BlackFormulaRepository.price(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, false);
      double resP3 = BlackFormulaRepository.price(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, false);
      double resC4 = BlackFormulaRepository.price(1.e12, strike, TIME_TO_EXPIRY, 1.e12, true);
      double resP4 = BlackFormulaRepository.price(1.e12, strike, TIME_TO_EXPIRY, 1.e12, false);

      double refC1 = BlackFormulaRepository.price(0., strike, TIME_TO_EXPIRY, 0., true);
      double refC2 = BlackFormulaRepository.price(0., strike, TIME_TO_EXPIRY, inf, true);
      double refC3 = BlackFormulaRepository.price(inf, strike, TIME_TO_EXPIRY, 0., true);
      double refP1 = BlackFormulaRepository.price(0., strike, TIME_TO_EXPIRY, 0., false);
      double refP2 = BlackFormulaRepository.price(0., strike, TIME_TO_EXPIRY, inf, false);
      double refP3 = BlackFormulaRepository.price(inf, strike, TIME_TO_EXPIRY, 0., false);
      double refC4 = BlackFormulaRepository.price(inf, strike, TIME_TO_EXPIRY, inf, true);
      double refP4 = BlackFormulaRepository.price(inf, strike, TIME_TO_EXPIRY, inf, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

      for (int k = 0; k < 8; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.price(forward, 1.e-12, 1.e-12, vol, true);
        double resC2 = BlackFormulaRepository.price(forward, 1.e-12, 1.e12, vol, true);
        double resC3 = BlackFormulaRepository.price(forward, 1.e12, 1.e-12, vol, true);
        double resP1 = BlackFormulaRepository.price(forward, 1.e-12, 1.e-12, vol, false);
        double resP2 = BlackFormulaRepository.price(forward, 1.e-12, 1.e12, vol, false);
        double resP3 = BlackFormulaRepository.price(forward, 1.e12, 1.e-12, vol, false);
        double resC4 = BlackFormulaRepository.price(forward, 1.e12, 1.e24, vol, true);
        double resP4 = BlackFormulaRepository.price(forward, 1.e12, 1.e24, vol, false);

        double refC1 = BlackFormulaRepository.price(forward, 0., 0., vol, true);
        double refC2 = BlackFormulaRepository.price(forward, 0., inf, vol, true);
        double refC3 = BlackFormulaRepository.price(forward, inf, 0., vol, true);
        double refP1 = BlackFormulaRepository.price(forward, 0., 0., vol, false);
        double refP2 = BlackFormulaRepository.price(forward, 0., inf, vol, false);
        double refP3 = BlackFormulaRepository.price(forward, inf, 0., vol, false);
        double refC4 = BlackFormulaRepository.price(forward, inf, inf, vol, true);
        double refP4 = BlackFormulaRepository.price(forward, inf, inf, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.price(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true);
      double resC2 = BlackFormulaRepository.price(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, true);
      double resC3 = BlackFormulaRepository.price(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, true);
      double resP1 = BlackFormulaRepository.price(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false);
      double resP2 = BlackFormulaRepository.price(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, false);
      double resP3 = BlackFormulaRepository.price(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, false);
      double resC4 = BlackFormulaRepository.price(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, true);
      double resP4 = BlackFormulaRepository.price(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, false);

      double refC1 = BlackFormulaRepository.price(forward, 0., TIME_TO_EXPIRY, 0., true);
      double refC2 = BlackFormulaRepository.price(forward, 0., TIME_TO_EXPIRY, inf, true);
      double refC3 = BlackFormulaRepository.price(forward, inf, TIME_TO_EXPIRY, 0., true);
      double refP1 = BlackFormulaRepository.price(forward, 0., TIME_TO_EXPIRY, 0., false);
      double refP2 = BlackFormulaRepository.price(forward, 0., TIME_TO_EXPIRY, inf, false);
      double refP3 = BlackFormulaRepository.price(forward, inf, TIME_TO_EXPIRY, 0., false);
      double refC4 = BlackFormulaRepository.price(forward, inf, TIME_TO_EXPIRY, inf, true);
      double refP4 = BlackFormulaRepository.price(forward, inf, TIME_TO_EXPIRY, inf, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

      for (int k = 0; k < 8; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.price(1.e-12, 1.e-12, 1.e-24, vol, true);
      double resC2 = BlackFormulaRepository.price(1.e-12, 1.e-12, 1.e24, vol, true);
      double resC3 = BlackFormulaRepository.price(1.e-12, 1.e12, 1.e-24, vol, true);
      double resP1 = BlackFormulaRepository.price(1.e-12, 1.e-12, 1.e-24, vol, false);
      double resP2 = BlackFormulaRepository.price(1.e-12, 1.e-12, 1.e24, vol, false);
      double resP3 = BlackFormulaRepository.price(1.e-12, 1.e12, 1.e-24, vol, false);
      double resC4 = BlackFormulaRepository.price(1.e12, 1.e-12, 1.e-24, vol, true);
      double resP4 = BlackFormulaRepository.price(1.e12, 1.e-12, 1.e-24, vol, false);
      double resC5 = BlackFormulaRepository.price(FORWARD, FORWARD * (1. + 1.e-12), 1.e-24, vol, true);
      double resP5 = BlackFormulaRepository.price(FORWARD, FORWARD * (1. + 1.e-12), 1.e-24, vol, false);
      double resC6 = BlackFormulaRepository.price(1.e12, 1.e12, 1.e24, vol, true);
      double resP6 = BlackFormulaRepository.price(1.e12, 1.e12, 1.e24, vol, false);

      double refC1 = BlackFormulaRepository.price(0., 0., 0., vol, true);
      double refC2 = BlackFormulaRepository.price(0., 0., inf, vol, true);
      double refC3 = BlackFormulaRepository.price(0., inf, 0., vol, true);
      double refP1 = BlackFormulaRepository.price(0., 0., 0., vol, false);
      double refP2 = BlackFormulaRepository.price(0., 0., inf, vol, false);
      double refP3 = BlackFormulaRepository.price(0., inf, 0., vol, false);
      double refC4 = BlackFormulaRepository.price(inf, 0., 0., vol, true);
      double refP4 = BlackFormulaRepository.price(inf, 0., 0., vol, false);
      double refC5 = BlackFormulaRepository.price(FORWARD, FORWARD, 0., vol, true);
      double refP5 = BlackFormulaRepository.price(FORWARD, FORWARD, 0., vol, false);
      double refC6 = BlackFormulaRepository.price(inf, inf, inf, vol, true);
      double refP6 = BlackFormulaRepository.price(inf, inf, inf, vol, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6};

      for (int k = 0; k < 12; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.price(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true);
      double resC2 = BlackFormulaRepository.price(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12, true);
      double resC3 = BlackFormulaRepository.price(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12, true);
      double resP1 = BlackFormulaRepository.price(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false);
      double resP2 = BlackFormulaRepository.price(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12, false);
      double resP3 = BlackFormulaRepository.price(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12, false);
      double resC4 = BlackFormulaRepository.price(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true);
      double resP4 = BlackFormulaRepository.price(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false);
      double resC5 = BlackFormulaRepository.price(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12, true);
      double resP5 = BlackFormulaRepository
          .price(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12, false);

      double refC1 = BlackFormulaRepository.price(0., 0., TIME_TO_EXPIRY, 0., true);
      double refC2 = BlackFormulaRepository.price(0., 0., TIME_TO_EXPIRY, inf, true);
      double refC3 = BlackFormulaRepository.price(0., inf, TIME_TO_EXPIRY, 0., true);
      double refP1 = BlackFormulaRepository.price(0., 0., TIME_TO_EXPIRY, 0., false);
      double refP2 = BlackFormulaRepository.price(0., 0., TIME_TO_EXPIRY, inf, false);
      double refP3 = BlackFormulaRepository.price(0., inf, TIME_TO_EXPIRY, 0., false);
      double refC4 = BlackFormulaRepository.price(inf, 0., TIME_TO_EXPIRY, 0., true);
      double refP4 = BlackFormulaRepository.price(inf, 0., TIME_TO_EXPIRY, 0., false);
      double refC5 = BlackFormulaRepository.price(FORWARD, FORWARD, TIME_TO_EXPIRY, 1.e-12, true);
      double refP5 = BlackFormulaRepository.price(FORWARD, FORWARD, TIME_TO_EXPIRY, 1.e-12, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5};

      for (int k = 0; k < 10; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10)));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.price(strike, strike, inf, 0., true);
      double resP1 = BlackFormulaRepository.price(strike, strike, inf, 0., false);
      double refC1 = strike * NORMAL.getCDF(0.5) - strike * NORMAL.getCDF(-0.5);
      double refP1 = -strike * NORMAL.getCDF(-0.5) + strike * NORMAL.getCDF(0.5);

      double[] resVec = new double[] {resC1, resP1};
      double[] refVec = new double[] {refC1, refP1};
      for (int k = 0; k < 2; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10)));
            }
          }
        }
      }
    }
  }

  @Test
  public void negativeVolErrorPriceTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.price(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, true));
  }

  @Test
  public void negativeFwdErrorPriceTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.price(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], true));
  }

  @Test
  public void negativeStrikeErrorPriceTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.price(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], true));
  }

  @Test
  public void negativeTimeErrorPriceTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.price(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], true));
  }

  /*
   * Tests for "delta"
   */
  /**
   * Large/small value for delta
   */
  @Test
  public void exDeltaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.delta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, true);
        double refC1 = BlackFormulaRepository.delta(0., strike, TIME_TO_EXPIRY, vol, true);
        double resC2 = BlackFormulaRepository.delta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, true);
        double refC2 = BlackFormulaRepository.delta(inf, strike, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.delta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, false);
        double refP1 = BlackFormulaRepository.delta(0., strike, TIME_TO_EXPIRY, vol, false);
        double resP2 = BlackFormulaRepository.delta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, false);
        double refP2 = BlackFormulaRepository.delta(inf, strike, TIME_TO_EXPIRY, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-11).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-11));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.delta(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol, true);
        double resC2 = BlackFormulaRepository.delta(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.delta(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol, false);
        double resP2 = BlackFormulaRepository.delta(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol, false);
        double refC1 = BlackFormulaRepository.delta(forward, 0., TIME_TO_EXPIRY, vol, true);
        double refC2 = BlackFormulaRepository.delta(forward, inf, TIME_TO_EXPIRY, vol, true);
        double refP1 = BlackFormulaRepository.delta(forward, 0., TIME_TO_EXPIRY, vol, false);
        double refP2 = BlackFormulaRepository.delta(forward, inf, TIME_TO_EXPIRY, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.delta(FORWARD, strike, 1e-24, vol, true);
        double resC2 = BlackFormulaRepository.delta(FORWARD, strike, 1e24, vol, true);
        double resP1 = BlackFormulaRepository.delta(FORWARD, strike, 1e-24, vol, false);
        double resP2 = BlackFormulaRepository.delta(FORWARD, strike, 1e24, vol, false);
        double refC1 = BlackFormulaRepository.delta(FORWARD, strike, 0., vol, true);
        double refC2 = BlackFormulaRepository.delta(FORWARD, strike, inf, vol, true);
        double refP1 = BlackFormulaRepository.delta(FORWARD, strike, 0., vol, false);
        double refP2 = BlackFormulaRepository.delta(FORWARD, strike, inf, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, true);
      double refC1 = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, 0., true);
      double resC2 = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, true);
      double refC2 = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, inf, true);
      double resP1 = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, false);
      double refP1 = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, 0., false);
      double resP2 = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, false);
      double refP2 = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, inf, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.delta(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, true);
      double resC2 = BlackFormulaRepository.delta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, true);
      double resC3 = BlackFormulaRepository.delta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, true);
      double resP1 = BlackFormulaRepository.delta(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, false);
      double resP2 = BlackFormulaRepository.delta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, false);
      double resP3 = BlackFormulaRepository.delta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, false);
      double resC4 = BlackFormulaRepository.delta(1.e12, 1.e12, TIME_TO_EXPIRY, vol, true);
      double resP4 = BlackFormulaRepository.delta(1.e12, 1.e12, TIME_TO_EXPIRY, vol, false);

      double refC1 = BlackFormulaRepository.delta(0., 0., TIME_TO_EXPIRY, vol, true);
      double refC2 = BlackFormulaRepository.delta(0., inf, TIME_TO_EXPIRY, vol, true);
      double refC3 = BlackFormulaRepository.delta(inf, 0., TIME_TO_EXPIRY, vol, true);
      double refP1 = BlackFormulaRepository.delta(0., 0., TIME_TO_EXPIRY, vol, false);
      double refP2 = BlackFormulaRepository.delta(0., inf, TIME_TO_EXPIRY, vol, false);
      double refP3 = BlackFormulaRepository.delta(inf, 0., TIME_TO_EXPIRY, vol, false);
      double refC4 = BlackFormulaRepository.delta(inf, inf, TIME_TO_EXPIRY, vol, true);
      double refP4 = BlackFormulaRepository.delta(inf, inf, TIME_TO_EXPIRY, vol, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

      for (int k = 0; k < 8; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.delta(1.e-12, strike, 1.e-12, vol, true);
        double resC2 = BlackFormulaRepository.delta(1.e-12, strike, 1.e12, vol, true);
        double resC3 = BlackFormulaRepository.delta(1.e12, strike, 1.e-12, vol, true);
        double resP1 = BlackFormulaRepository.delta(1.e-12, strike, 1.e-12, vol, false);
        double resP2 = BlackFormulaRepository.delta(1.e-12, strike, 1.e12, vol, false);
        double resP3 = BlackFormulaRepository.delta(1.e12, strike, 1.e-12, vol, false);
        double resC4 = BlackFormulaRepository.delta(1.e12, strike, 1.e12, vol, true);
        double resP4 = BlackFormulaRepository.delta(1.e12, strike, 1.e12, vol, false);

        double refC1 = BlackFormulaRepository.delta(0., strike, 0., vol, true);
        double refC2 = BlackFormulaRepository.delta(0., strike, inf, vol, true);
        double refC3 = BlackFormulaRepository.delta(inf, strike, 0., vol, true);
        double refP1 = BlackFormulaRepository.delta(0., strike, 0., vol, false);
        double refP2 = BlackFormulaRepository.delta(0., strike, inf, vol, false);
        double refP3 = BlackFormulaRepository.delta(inf, strike, 0., vol, false);
        double refC4 = BlackFormulaRepository.delta(inf, strike, inf, vol, true);
        double refP4 = BlackFormulaRepository.delta(inf, strike, inf, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.delta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, true);
      double resC2 = BlackFormulaRepository.delta(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, true);
      double resC3 = BlackFormulaRepository.delta(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, true);
      double resP1 = BlackFormulaRepository.delta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, false);
      double resP2 = BlackFormulaRepository.delta(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, false);
      double resP3 = BlackFormulaRepository.delta(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, false);

      double refC1 = BlackFormulaRepository.delta(0., strike, TIME_TO_EXPIRY, 0., true);
      double refC2 = BlackFormulaRepository.delta(0., strike, TIME_TO_EXPIRY, inf, true);
      double refC3 = BlackFormulaRepository.delta(inf, strike, TIME_TO_EXPIRY, 0., true);
      double refP1 = BlackFormulaRepository.delta(0., strike, TIME_TO_EXPIRY, 0., false);
      double refP2 = BlackFormulaRepository.delta(0., strike, TIME_TO_EXPIRY, inf, false);
      double refP3 = BlackFormulaRepository.delta(inf, strike, TIME_TO_EXPIRY, 0., false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

      for (int k = 0; k < 6; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.delta(forward, 1.e-12, 1.e-12, vol, true);
        double resC2 = BlackFormulaRepository.delta(forward, 1.e-12, 1.e12, vol, true);
        double resC3 = BlackFormulaRepository.delta(forward, 1.e12, 1.e-12, vol, true);
        double resP1 = BlackFormulaRepository.delta(forward, 1.e-12, 1.e-12, vol, false);
        double resP2 = BlackFormulaRepository.delta(forward, 1.e-12, 1.e12, vol, false);
        double resP3 = BlackFormulaRepository.delta(forward, 1.e12, 1.e-12, vol, false);
        double resC4 = BlackFormulaRepository.delta(forward, 1.e12, 1.e12, vol, true);
        double resP4 = BlackFormulaRepository.delta(forward, 1.e12, 1.e12, vol, false);

        double refC1 = BlackFormulaRepository.delta(forward, 0., 0., vol, true);
        double refC2 = BlackFormulaRepository.delta(forward, 0., inf, vol, true);
        double refC3 = BlackFormulaRepository.delta(forward, inf, 0., vol, true);
        double refP1 = BlackFormulaRepository.delta(forward, 0., 0., vol, false);
        double refP2 = BlackFormulaRepository.delta(forward, 0., inf, vol, false);
        double refP3 = BlackFormulaRepository.delta(forward, inf, 0., vol, false);
        double refC4 = BlackFormulaRepository.delta(forward, inf, inf, vol, true);
        double refP4 = BlackFormulaRepository.delta(forward, inf, inf, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.delta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true);
      double resC2 = BlackFormulaRepository.delta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, true);
      double resC3 = BlackFormulaRepository.delta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, true);
      double resP1 = BlackFormulaRepository.delta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false);
      double resP2 = BlackFormulaRepository.delta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, false);
      double resP3 = BlackFormulaRepository.delta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, false);

      double refC1 = BlackFormulaRepository.delta(forward, 0., TIME_TO_EXPIRY, 0., true);
      double refC2 = BlackFormulaRepository.delta(forward, 0., TIME_TO_EXPIRY, inf, true);
      double refC3 = BlackFormulaRepository.delta(forward, inf, TIME_TO_EXPIRY, 0., true);
      double refP1 = BlackFormulaRepository.delta(forward, 0., TIME_TO_EXPIRY, 0., false);
      double refP2 = BlackFormulaRepository.delta(forward, 0., TIME_TO_EXPIRY, inf, false);
      double refP3 = BlackFormulaRepository.delta(forward, inf, TIME_TO_EXPIRY, 0., false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

      for (int k = 0; k < 6; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.delta(1.e-12, 1.e-12, 1.e-24, vol, true);
      double resC2 = BlackFormulaRepository.delta(1.e-12, 1.e-12, 1.e24, vol, true);
      double resC3 = BlackFormulaRepository.delta(1.e-12, 1.e12, 1.e-24, vol, true);
      double resP1 = BlackFormulaRepository.delta(1.e-12, 1.e-12, 1.e-24, vol, false);
      double resP2 = BlackFormulaRepository.delta(1.e-12, 1.e-12, 1.e24, vol, false);
      double resP3 = BlackFormulaRepository.delta(1.e-12, 1.e12, 1.e-24, vol, false);
      double resC4 = BlackFormulaRepository.delta(1.e12, 1.e-12, 1.e-24, vol, true);
      double resP4 = BlackFormulaRepository.delta(1.e12, 1.e-12, 1.e-24, vol, false);
      double resC5 = BlackFormulaRepository.delta(FORWARD, FORWARD * (1. + 1.e-12), 1.e-24, vol, true);
      double resP5 = BlackFormulaRepository.delta(FORWARD, FORWARD * (1. + 1.e-12), 1.e-24, vol, false);
      double resC6 = BlackFormulaRepository.delta(1.e12, 1.e12, 1.e24, vol, true);
      double resP6 = BlackFormulaRepository.delta(1.e12, 1.e12, 1.e24, vol, false);
      double resC7 = BlackFormulaRepository.delta(1.e12, 1.e12, 1.e-24, vol, true);
      double resP7 = BlackFormulaRepository.delta(1.e12, 1.e12, 1.e-24, vol, false);

      double refC1 = BlackFormulaRepository.delta(0., 0., 0., vol, true);
      double refC2 = BlackFormulaRepository.delta(0., 0., inf, vol, true);
      double refC3 = BlackFormulaRepository.delta(0., inf, 0., vol, true);
      double refP1 = BlackFormulaRepository.delta(0., 0., 0., vol, false);
      double refP2 = BlackFormulaRepository.delta(0., 0., inf, vol, false);
      double refP3 = BlackFormulaRepository.delta(0., inf, 0., vol, false);
      double refC4 = BlackFormulaRepository.delta(inf, 0., 0., vol, true);
      double refP4 = BlackFormulaRepository.delta(inf, 0., 0., vol, false);
      double refC5 = BlackFormulaRepository.delta(FORWARD, FORWARD, 0., vol, true);
      double refP5 = BlackFormulaRepository.delta(FORWARD, FORWARD, 0., vol, false);
      double refC6 = BlackFormulaRepository.delta(inf, inf, inf, vol, true);
      double refP6 = BlackFormulaRepository.delta(inf, inf, inf, vol, false);
      double refC7 = BlackFormulaRepository.delta(inf, inf, 0., vol, true);
      double refP7 = BlackFormulaRepository.delta(inf, inf, 0., vol, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7};

      for (int k = 0; k < 14; ++k) {
        if ((refVec[k] != 0.5) && (refVec[k] != -0.5)) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.delta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true);
      double resC2 = BlackFormulaRepository.delta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12, true);
      double resC3 = BlackFormulaRepository.delta(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12, true);
      double resP1 = BlackFormulaRepository.delta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false);
      double resP2 = BlackFormulaRepository.delta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12, false);
      double resP3 = BlackFormulaRepository.delta(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12, false);
      double resC4 = BlackFormulaRepository.delta(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true);
      double resP4 = BlackFormulaRepository.delta(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false);
      double resC5 = BlackFormulaRepository.delta(FORWARD, FORWARD * (1. + 1.e-13), TIME_TO_EXPIRY, 1.e-13, true);
      double resP5 = BlackFormulaRepository
          .delta(FORWARD, FORWARD * (1. + 1.e-13), TIME_TO_EXPIRY, 1.e-13, false);

      double refC1 = BlackFormulaRepository.delta(0., 0., TIME_TO_EXPIRY, 0., true);
      double refC2 = BlackFormulaRepository.delta(0., 0., TIME_TO_EXPIRY, inf, true);
      double refC3 = BlackFormulaRepository.delta(0., inf, TIME_TO_EXPIRY, 0., true);
      double refP1 = BlackFormulaRepository.delta(0., 0., TIME_TO_EXPIRY, 0., false);
      double refP2 = BlackFormulaRepository.delta(0., 0., TIME_TO_EXPIRY, inf, false);
      double refP3 = BlackFormulaRepository.delta(0., inf, TIME_TO_EXPIRY, 0., false);
      double refC4 = BlackFormulaRepository.delta(inf, 0., TIME_TO_EXPIRY, 0., true);
      double refP4 = BlackFormulaRepository.delta(inf, 0., TIME_TO_EXPIRY, 0., false);
      double refC5 = BlackFormulaRepository.delta(FORWARD, FORWARD, TIME_TO_EXPIRY, 0., true);
      double refP5 = BlackFormulaRepository.delta(FORWARD, FORWARD, TIME_TO_EXPIRY, 0., false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5};

      for (int k = 0; k < 10; ++k) {
        if ((refVec[k] != 0.5) && (refVec[k] != -0.5)) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.delta(strike, strike, inf, 0., true);
      double resP1 = BlackFormulaRepository.delta(strike, strike, inf, 0., false);
      double refC1 = NORMAL.getCDF(0.5);
      double refP1 = -NORMAL.getCDF(-0.5);

      double[] resVec = new double[] {resC1, resP1};
      double[] refVec = new double[] {refC1, refP1};
      for (int k = 0; k < 2; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10)));
            }
          }
        }
      }
    }
  }

  /**
   *
   */
  @Test
  public void parityDeltaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, vol, false);
        assertThat(1.).isCloseTo(resC1 - resP1, offset(EPS));
      }
    }
  }

  /**
   *
   */
  @Test
  public void negativeVolErrorDeltaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.delta(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, true));
  }

  @Test
  public void negativeFwdErrorDeltaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.delta(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], true));
  }

  @Test
  public void negativeStrikeErrorDeltaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.delta(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], true));
  }

  @Test
  public void negativeTimeErrorDeltaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.delta(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], true));
  }

  /*
   * Tests for "strikeForDelta"
   */
  @Test
  public void strikeForDeltaRecoveryTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, vol, false);
        double strRecovC1 = BlackFormulaRepository.strikeForDelta(FORWARD, resC1, TIME_TO_EXPIRY, vol, true);
        double strRecovP1 = BlackFormulaRepository.strikeForDelta(FORWARD, resP1, TIME_TO_EXPIRY, vol, false);
        assertThat(strike).isCloseTo(strRecovC1, offset(strike * EPS));
        assertThat(strike).isCloseTo(strRecovP1, offset(strike * EPS));
      }
    }
  }

  /**
   * Note that the inverse is not necessarily possible because \pm 1, 0 are not taken by strikeForDelta method
   */
  @Test
  public void exDeltaStrikeForDeltaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double fwd = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.strikeForDelta(fwd, 1. - 1.e-12, TIME_TO_EXPIRY, vol, true);
        double resC2 = BlackFormulaRepository.strikeForDelta(fwd, 1.e-12, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.strikeForDelta(fwd, -1. + 1.e-12, TIME_TO_EXPIRY, vol, false);
        double resP2 = BlackFormulaRepository.strikeForDelta(fwd, -1.e-12, TIME_TO_EXPIRY, vol, false);
        double strRecovC1 = BlackFormulaRepository.delta(fwd, resC1, TIME_TO_EXPIRY, vol, true);
        double strRecovC2 = BlackFormulaRepository.delta(fwd, resC2, TIME_TO_EXPIRY, vol, true);
        double strRecovP1 = BlackFormulaRepository.delta(fwd, resP1, TIME_TO_EXPIRY, vol, false);
        double strRecovP2 = BlackFormulaRepository.delta(fwd, resP2, TIME_TO_EXPIRY, vol, false);

        assertThat(1. - 1.e-12).isCloseTo(strRecovC1, offset(EPS));
        assertThat(1.e-12).isCloseTo(strRecovC2, offset(EPS));
        assertThat(-1. + 1.e-12).isCloseTo(strRecovP1, offset(EPS));
        assertThat(-1.e-12).isCloseTo(strRecovP2, offset(EPS));
      }
    }
  }

  @Test
  public void exFwdStrikeForDeltaTest() {
    int nVols = VOLS.length;

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.strikeForDelta(1.e12, 1. - 1.e-12, TIME_TO_EXPIRY, vol, true);
      double resC2 = BlackFormulaRepository.strikeForDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, true);
      double resP1 = BlackFormulaRepository.strikeForDelta(1.e12, -1.e-12, TIME_TO_EXPIRY, vol, false);
      double resP2 = BlackFormulaRepository.strikeForDelta(1.e-12, -1. + 1.e-12, TIME_TO_EXPIRY, vol, false);
      double strRecovC1 = BlackFormulaRepository.delta(1.e12, resC1, TIME_TO_EXPIRY, vol, true);
      double strRecovC2 = BlackFormulaRepository.delta(1.e-12, resC2, TIME_TO_EXPIRY, vol, true);
      double strRecovP1 = BlackFormulaRepository.delta(1.e12, resP1, TIME_TO_EXPIRY, vol, false);
      double strRecovP2 = BlackFormulaRepository.delta(1.e-12, resP2, TIME_TO_EXPIRY, vol, false);

      assertThat(1. - 1.e-12).isCloseTo(strRecovC1, offset(EPS));
      assertThat(1.e-12).isCloseTo(strRecovC2, offset(EPS));
      assertThat(-1.e-12).isCloseTo(strRecovP1, offset(EPS));
      assertThat(-1. + 1.e-12).isCloseTo(strRecovP2, offset(EPS));
    }
  }

  @Test
  public void exTimeStrikeForDeltaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;

    double red = Math.sqrt(1.e12);

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double fwd = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.strikeForDelta(fwd, 1. - 1.e-12, 1.e-12, vol, true);
        double resP1 = BlackFormulaRepository.strikeForDelta(fwd, -0.5, 1.e-12, vol, false);
        double strRecovC1 = BlackFormulaRepository.delta(fwd, resC1, 1.e-12, vol, true);
        double strRecovP1 = BlackFormulaRepository.delta(fwd, resP1, 1.e-12, vol, false);

        assertThat(1. - 1.e-12).isCloseTo(strRecovC1, offset(EPS * red));
        /*
         * This case is not correctly recovered because strike = infinity is obtained by strikeForDelta, coming from
         * exp( 1.e12 ), which always results in delta = 0
         */
        assertThat(-0.5).isCloseTo(strRecovP1, offset(EPS * red));
        /*
         * This case gives strike = infinity
         */
      }
    }
  }

  @Test
  public void exVolStrikeForDeltaTest() {
    double small = 1.e-12;
    double inf = Double.POSITIVE_INFINITY;

    double resC2 = BlackFormulaRepository.strikeForDelta(FORWARD, 1.e-12, TIME_TO_EXPIRY, small, true);
    double resP2 = BlackFormulaRepository.strikeForDelta(FORWARD, -1.e-12, TIME_TO_EXPIRY, small, false);
    double strRecovC2 = BlackFormulaRepository.delta(FORWARD, resC2, TIME_TO_EXPIRY, small, true);
    double strRecovP2 = BlackFormulaRepository.delta(FORWARD, resP2, TIME_TO_EXPIRY, small, false);
    double resC3 = BlackFormulaRepository.strikeForDelta(FORWARD, 0.5, inf, 0., true);
    double resP3 = BlackFormulaRepository.strikeForDelta(FORWARD, -0.5, inf, 0., false);
    double strRecovC3 = BlackFormulaRepository.delta(FORWARD, resC3, inf, 0., true);
    double strRecovP3 = BlackFormulaRepository.delta(FORWARD, resP3, inf, 0., false);

    assertThat(1.e-12).isCloseTo(strRecovC2, offset(EPS));
    assertThat(-1.e-12).isCloseTo(strRecovP2, offset(EPS));
    assertThat(0.5).isCloseTo(strRecovC3, offset(EPS));
    assertThat(-0.5).isCloseTo(strRecovP3, offset(EPS));
  }

  @Test
  public void outOfRangeStrikeForDeltaCall1Test() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.strikeForDelta(FORWARD, -0.1, TIME_TO_EXPIRY, VOLS[1], true));
  }

  @Test
  public void outOfRangeStrikeForDeltaCall2Test() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.strikeForDelta(FORWARD, 1.1, TIME_TO_EXPIRY, VOLS[1], true));
  }

  @Test
  public void outOfRangeStrikeForDeltaPut1Test() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.strikeForDelta(FORWARD, 0.5, TIME_TO_EXPIRY, VOLS[1], false));
  }

  @Test
  public void outOfRangeStrikeForDeltaPut2Test() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.strikeForDelta(FORWARD, -1.5, TIME_TO_EXPIRY, VOLS[1], false));
  }

  @Test
  public void negativeFwdStrikeForDeltaCall2Test() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.strikeForDelta(-FORWARD, 0.5, TIME_TO_EXPIRY, VOLS[1], true));
  }

  @Test
  public void negativeTimeStrikeForDeltaPut1Test() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.strikeForDelta(FORWARD, 0.5, -TIME_TO_EXPIRY, VOLS[1], true));
  }

  @Test
  public void negativeVolStrikeForDeltaPut2Test() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.strikeForDelta(FORWARD, 0.5, TIME_TO_EXPIRY, -VOLS[1], true));
  }

  /*
   * Tests for "dualDelta"
   */
  /**
   * large/small values for dual delta
   */
  @Test
  public void exDualDeltaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualDelta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, true);
        double refC1 = BlackFormulaRepository.dualDelta(0., strike, TIME_TO_EXPIRY, vol, true);
        double resC2 = BlackFormulaRepository.dualDelta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, true);
        double refC2 = BlackFormulaRepository.dualDelta(inf, strike, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.dualDelta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, false);
        double refP1 = BlackFormulaRepository.dualDelta(0., strike, TIME_TO_EXPIRY, vol, false);
        double resP2 = BlackFormulaRepository.dualDelta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, false);
        double refP2 = BlackFormulaRepository.dualDelta(inf, strike, TIME_TO_EXPIRY, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-11).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-11));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualDelta(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol, true);
        double resC2 = BlackFormulaRepository.dualDelta(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.dualDelta(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol, false);
        double resP2 = BlackFormulaRepository.dualDelta(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol, false);
        double refC1 = BlackFormulaRepository.dualDelta(forward, 0., TIME_TO_EXPIRY, vol, true);
        double refC2 = BlackFormulaRepository.dualDelta(forward, inf, TIME_TO_EXPIRY, vol, true);
        double refP1 = BlackFormulaRepository.dualDelta(forward, 0., TIME_TO_EXPIRY, vol, false);
        double refP2 = BlackFormulaRepository.dualDelta(forward, inf, TIME_TO_EXPIRY, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualDelta(FORWARD, strike, 1e-24, vol, true);
        double resC2 = BlackFormulaRepository.dualDelta(FORWARD, strike, 1e24, vol, true);
        double resP1 = BlackFormulaRepository.dualDelta(FORWARD, strike, 1e-24, vol, false);
        double resP2 = BlackFormulaRepository.dualDelta(FORWARD, strike, 1e24, vol, false);
        double refC1 = BlackFormulaRepository.dualDelta(FORWARD, strike, 0., vol, true);
        double refC2 = BlackFormulaRepository.dualDelta(FORWARD, strike, inf, vol, true);
        double refP1 = BlackFormulaRepository.dualDelta(FORWARD, strike, 0., vol, false);
        double refP2 = BlackFormulaRepository.dualDelta(FORWARD, strike, inf, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.dualDelta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, true);
      double refC1 = BlackFormulaRepository.dualDelta(FORWARD, strike, TIME_TO_EXPIRY, 0., true);
      double resC2 = BlackFormulaRepository.dualDelta(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, true);
      double refC2 = BlackFormulaRepository.dualDelta(FORWARD, strike, TIME_TO_EXPIRY, inf, true);
      double resP1 = BlackFormulaRepository.dualDelta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, false);
      double refP1 = BlackFormulaRepository.dualDelta(FORWARD, strike, TIME_TO_EXPIRY, 0., false);
      double resP2 = BlackFormulaRepository.dualDelta(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, false);
      double refP2 = BlackFormulaRepository.dualDelta(FORWARD, strike, TIME_TO_EXPIRY, inf, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.dualDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, true);
      double resC2 = BlackFormulaRepository.dualDelta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, true);
      double resC3 = BlackFormulaRepository.dualDelta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, true);
      double resP1 = BlackFormulaRepository.dualDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, false);
      double resP2 = BlackFormulaRepository.dualDelta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, false);
      double resP3 = BlackFormulaRepository.dualDelta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, false);
      double resC4 = BlackFormulaRepository.dualDelta(1.e12, 1.e12, TIME_TO_EXPIRY, vol, true);
      double resP4 = BlackFormulaRepository.dualDelta(1.e12, 1.e12, TIME_TO_EXPIRY, vol, false);

      double refC1 = BlackFormulaRepository.dualDelta(0., 0., TIME_TO_EXPIRY, vol, true);
      double refC2 = BlackFormulaRepository.dualDelta(0., inf, TIME_TO_EXPIRY, vol, true);
      double refC3 = BlackFormulaRepository.dualDelta(inf, 0., TIME_TO_EXPIRY, vol, true);
      double refP1 = BlackFormulaRepository.dualDelta(0., 0., TIME_TO_EXPIRY, vol, false);
      double refP2 = BlackFormulaRepository.dualDelta(0., inf, TIME_TO_EXPIRY, vol, false);
      double refP3 = BlackFormulaRepository.dualDelta(inf, 0., TIME_TO_EXPIRY, vol, false);
      double refC4 = BlackFormulaRepository.dualDelta(inf, inf, TIME_TO_EXPIRY, vol, true);
      double refP4 = BlackFormulaRepository.dualDelta(inf, inf, TIME_TO_EXPIRY, vol, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

      for (int k = 0; k < 8; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualDelta(1.e-12, strike, 1.e-12, vol, true);
        double resC2 = BlackFormulaRepository.dualDelta(1.e-12, strike, 1.e12, vol, true);
        double resC3 = BlackFormulaRepository.dualDelta(1.e12, strike, 1.e-12, vol, true);
        double resP1 = BlackFormulaRepository.dualDelta(1.e-12, strike, 1.e-12, vol, false);
        double resP2 = BlackFormulaRepository.dualDelta(1.e-12, strike, 1.e12, vol, false);
        double resP3 = BlackFormulaRepository.dualDelta(1.e12, strike, 1.e-12, vol, false);
        double resC4 = BlackFormulaRepository.dualDelta(1.e12, strike, 1.e12, vol, true);
        double resP4 = BlackFormulaRepository.dualDelta(1.e12, strike, 1.e12, vol, false);

        double refC1 = BlackFormulaRepository.dualDelta(0., strike, 0., vol, true);
        double refC2 = BlackFormulaRepository.dualDelta(0., strike, inf, vol, true);
        double refC3 = BlackFormulaRepository.dualDelta(inf, strike, 0., vol, true);
        double refP1 = BlackFormulaRepository.dualDelta(0., strike, 0., vol, false);
        double refP2 = BlackFormulaRepository.dualDelta(0., strike, inf, vol, false);
        double refP3 = BlackFormulaRepository.dualDelta(inf, strike, 0., vol, false);
        double refC4 = BlackFormulaRepository.dualDelta(inf, strike, inf, vol, true);
        double refP4 = BlackFormulaRepository.dualDelta(inf, strike, inf, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.dualDelta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, true);
      double resC2 = BlackFormulaRepository.dualDelta(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, true);
      double resC3 = BlackFormulaRepository.dualDelta(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, true);
      double resP1 = BlackFormulaRepository.dualDelta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, false);
      double resP2 = BlackFormulaRepository.dualDelta(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, false);
      double resP3 = BlackFormulaRepository.dualDelta(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, false);

      double refC1 = BlackFormulaRepository.dualDelta(0., strike, TIME_TO_EXPIRY, 0., true);
      double refC2 = BlackFormulaRepository.dualDelta(0., strike, TIME_TO_EXPIRY, inf, true);
      double refC3 = BlackFormulaRepository.dualDelta(inf, strike, TIME_TO_EXPIRY, 0., true);
      double refP1 = BlackFormulaRepository.dualDelta(0., strike, TIME_TO_EXPIRY, 0., false);
      double refP2 = BlackFormulaRepository.dualDelta(0., strike, TIME_TO_EXPIRY, inf, false);
      double refP3 = BlackFormulaRepository.dualDelta(inf, strike, TIME_TO_EXPIRY, 0., false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

      for (int k = 0; k < 6; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualDelta(forward, 1.e-12, 1.e-12, vol, true);
        double resC2 = BlackFormulaRepository.dualDelta(forward, 1.e-12, 1.e12, vol, true);
        double resC3 = BlackFormulaRepository.dualDelta(forward, 1.e12, 1.e-12, vol, true);
        double resP1 = BlackFormulaRepository.dualDelta(forward, 1.e-12, 1.e-12, vol, false);
        double resP2 = BlackFormulaRepository.dualDelta(forward, 1.e-12, 1.e12, vol, false);
        double resP3 = BlackFormulaRepository.dualDelta(forward, 1.e12, 1.e-12, vol, false);
        double resC4 = BlackFormulaRepository.dualDelta(forward, 1.e12, 1.e12, vol, true);
        double resP4 = BlackFormulaRepository.dualDelta(forward, 1.e12, 1.e12, vol, false);

        double refC1 = BlackFormulaRepository.dualDelta(forward, 0., 0., vol, true);
        double refC2 = BlackFormulaRepository.dualDelta(forward, 0., inf, vol, true);
        double refC3 = BlackFormulaRepository.dualDelta(forward, inf, 0., vol, true);
        double refP1 = BlackFormulaRepository.dualDelta(forward, 0., 0., vol, false);
        double refP2 = BlackFormulaRepository.dualDelta(forward, 0., inf, vol, false);
        double refP3 = BlackFormulaRepository.dualDelta(forward, inf, 0., vol, false);
        double refC4 = BlackFormulaRepository.dualDelta(forward, inf, inf, vol, true);
        double refP4 = BlackFormulaRepository.dualDelta(forward, inf, inf, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.dualDelta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true);
      double resC2 = BlackFormulaRepository.dualDelta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, true);
      double resC3 = BlackFormulaRepository.dualDelta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, true);
      double resP1 = BlackFormulaRepository.dualDelta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false);
      double resP2 = BlackFormulaRepository.dualDelta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, false);
      double resP3 = BlackFormulaRepository.dualDelta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, false);

      double refC1 = BlackFormulaRepository.dualDelta(forward, 0., TIME_TO_EXPIRY, 0., true);
      double refC2 = BlackFormulaRepository.dualDelta(forward, 0., TIME_TO_EXPIRY, inf, true);
      double refC3 = BlackFormulaRepository.dualDelta(forward, inf, TIME_TO_EXPIRY, 0., true);
      double refP1 = BlackFormulaRepository.dualDelta(forward, 0., TIME_TO_EXPIRY, 0., false);
      double refP2 = BlackFormulaRepository.dualDelta(forward, 0., TIME_TO_EXPIRY, inf, false);
      double refP3 = BlackFormulaRepository.dualDelta(forward, inf, TIME_TO_EXPIRY, 0., false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

      for (int k = 0; k < 6; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.dualDelta(1.e-12, 1.e-12, 1.e-24, vol, true);
      double resC2 = BlackFormulaRepository.dualDelta(1.e-12, 1.e-12, 1.e24, vol, true);
      double resC3 = BlackFormulaRepository.dualDelta(1.e-12, 1.e12, 1.e-24, vol, true);
      double resP1 = BlackFormulaRepository.dualDelta(1.e-12, 1.e-12, 1.e-24, vol, false);
      double resP2 = BlackFormulaRepository.dualDelta(1.e-12, 1.e-12, 1.e24, vol, false);
      double resP3 = BlackFormulaRepository.dualDelta(1.e-12, 1.e12, 1.e-24, vol, false);
      double resC4 = BlackFormulaRepository.dualDelta(1.e12, 1.e-12, 1.e-24, vol, true);
      double resP4 = BlackFormulaRepository.dualDelta(1.e12, 1.e-12, 1.e-24, vol, false);
      double resC5 = BlackFormulaRepository.dualDelta(FORWARD, FORWARD * (1. + 1.e-12), 1.e-24, vol, true);
      double resP5 = BlackFormulaRepository.dualDelta(FORWARD, FORWARD * (1. + 1.e-12), 1.e-24, vol, false);
      double resC6 = BlackFormulaRepository.dualDelta(1.e12, 1.e12, 1.e24, vol, true);
      double resP6 = BlackFormulaRepository.dualDelta(1.e12, 1.e12, 1.e24, vol, false);
      double resC7 = BlackFormulaRepository.dualDelta(1.e12, 1.e12, 1.e-24, vol, true);
      double resP7 = BlackFormulaRepository.dualDelta(1.e12, 1.e12, 1.e-24, vol, false);

      double refC1 = BlackFormulaRepository.dualDelta(0., 0., 0., vol, true);
      double refC2 = BlackFormulaRepository.dualDelta(0., 0., inf, vol, true);
      double refC3 = BlackFormulaRepository.dualDelta(0., inf, 0., vol, true);
      double refP1 = BlackFormulaRepository.dualDelta(0., 0., 0., vol, false);
      double refP2 = BlackFormulaRepository.dualDelta(0., 0., inf, vol, false);
      double refP3 = BlackFormulaRepository.dualDelta(0., inf, 0., vol, false);
      double refC4 = BlackFormulaRepository.dualDelta(inf, 0., 0., vol, true);
      double refP4 = BlackFormulaRepository.dualDelta(inf, 0., 0., vol, false);
      double refC5 = BlackFormulaRepository.dualDelta(FORWARD, FORWARD, 0., vol, true);
      double refP5 = BlackFormulaRepository.dualDelta(FORWARD, FORWARD, 0., vol, false);
      double refC6 = BlackFormulaRepository.dualDelta(inf, inf, inf, vol, true);
      double refP6 = BlackFormulaRepository.dualDelta(inf, inf, inf, vol, false);
      double refC7 = BlackFormulaRepository.dualDelta(inf, inf, 0., vol, true);
      double refP7 = BlackFormulaRepository.dualDelta(inf, inf, 0., vol, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7};

      for (int k = 0; k < 14; ++k) {

        if ((refVec[k] != 0.5) && (refVec[k] != -0.5)) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.dualDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true);
      double resC2 = BlackFormulaRepository.dualDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12, true);
      double resC3 = BlackFormulaRepository.dualDelta(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12, true);
      double resP1 = BlackFormulaRepository.dualDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false);
      double resP2 = BlackFormulaRepository.dualDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12, false);
      double resP3 = BlackFormulaRepository.dualDelta(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12, false);
      double resC4 = BlackFormulaRepository.dualDelta(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true);
      double resP4 = BlackFormulaRepository.dualDelta(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false);
      double resC5 = BlackFormulaRepository.dualDelta(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12,
          true);
      double resP5 = BlackFormulaRepository.dualDelta(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12,
          false);

      double refC1 = BlackFormulaRepository.dualDelta(0., 0., TIME_TO_EXPIRY, 0., true);
      double refC2 = BlackFormulaRepository.dualDelta(0., 0., TIME_TO_EXPIRY, inf, true);
      double refC3 = BlackFormulaRepository.dualDelta(0., inf, TIME_TO_EXPIRY, 0., true);
      double refP1 = BlackFormulaRepository.dualDelta(0., 0., TIME_TO_EXPIRY, 0., false);
      double refP2 = BlackFormulaRepository.dualDelta(0., 0., TIME_TO_EXPIRY, inf, false);
      double refP3 = BlackFormulaRepository.dualDelta(0., inf, TIME_TO_EXPIRY, 0., false);
      double refC4 = BlackFormulaRepository.dualDelta(inf, 0., TIME_TO_EXPIRY, 0., true);
      double refP4 = BlackFormulaRepository.dualDelta(inf, 0., TIME_TO_EXPIRY, 0., false);
      double refC5 = BlackFormulaRepository.dualDelta(FORWARD, FORWARD, TIME_TO_EXPIRY, 0., true);
      double refP5 = BlackFormulaRepository.dualDelta(FORWARD, FORWARD, TIME_TO_EXPIRY, 0., false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5};

      for (int k = 0; k < 10; ++k) {
        if ((refVec[k] != 0.5) && (refVec[k] != -0.5)) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.dualDelta(strike, strike, inf, 0., true);
      double resP1 = BlackFormulaRepository.dualDelta(strike, strike, inf, 0., false);
      double refC1 = -NORMAL.getCDF(-0.5);
      double refP1 = NORMAL.getCDF(0.5);

      double[] resVec = new double[] {resC1, resP1};
      double[] refVec = new double[] {refC1, refP1};
      for (int k = 0; k < 2; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10)));
            }
          }
        }
      }
    }
  }

  @Test
  public void parityDualDeltaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualDelta(FORWARD, strike, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.dualDelta(FORWARD, strike, TIME_TO_EXPIRY, vol, false);
        assertThat(-1.).isCloseTo(resC1 - resP1, offset(EPS));
      }
    }
  }

  @Test
  public void negativeVolErrorDualDeltaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.dualDelta(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, true));
  }

  @Test
  public void negativeFwdErrorDualDeltaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.dualDelta(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], true));
  }

  @Test
  public void negativeStrikeErrorDualDeltaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.dualDelta(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], true));
  }

  @Test
  public void negativeTimeErrorDualDeltaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.dualDelta(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], true));
  }

  /*
   * Tests for "simpleDelta"
   */
  /**
   * large/small values
   */
  @Test
  public void exSimpleDeltaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.simpleDelta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, true);
        double refC1 = BlackFormulaRepository.simpleDelta(0., strike, TIME_TO_EXPIRY, vol, true);
        double resC2 = BlackFormulaRepository.simpleDelta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, true);
        double refC2 = BlackFormulaRepository.simpleDelta(inf, strike, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.simpleDelta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, false);
        double refP1 = BlackFormulaRepository.simpleDelta(0., strike, TIME_TO_EXPIRY, vol, false);
        double resP2 = BlackFormulaRepository.simpleDelta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, false);
        double refP2 = BlackFormulaRepository.simpleDelta(inf, strike, TIME_TO_EXPIRY, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-11).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-11));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.simpleDelta(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol, true);
        double resC2 = BlackFormulaRepository.simpleDelta(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.simpleDelta(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol, false);
        double resP2 = BlackFormulaRepository.simpleDelta(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol, false);
        double refC1 = BlackFormulaRepository.simpleDelta(forward, 0., TIME_TO_EXPIRY, vol, true);
        double refC2 = BlackFormulaRepository.simpleDelta(forward, inf, TIME_TO_EXPIRY, vol, true);
        double refP1 = BlackFormulaRepository.simpleDelta(forward, 0., TIME_TO_EXPIRY, vol, false);
        double refP2 = BlackFormulaRepository.simpleDelta(forward, inf, TIME_TO_EXPIRY, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.simpleDelta(FORWARD, strike, 1e-24, vol, true);
        double resC2 = BlackFormulaRepository.simpleDelta(FORWARD, strike, 1e24, vol, true);
        double resP1 = BlackFormulaRepository.simpleDelta(FORWARD, strike, 1e-24, vol, false);
        double resP2 = BlackFormulaRepository.simpleDelta(FORWARD, strike, 1e24, vol, false);
        double refC1 = BlackFormulaRepository.simpleDelta(FORWARD, strike, 0., vol, true);
        double refC2 = BlackFormulaRepository.simpleDelta(FORWARD, strike, inf, vol, true);
        double refP1 = BlackFormulaRepository.simpleDelta(FORWARD, strike, 0., vol, false);
        double refP2 = BlackFormulaRepository.simpleDelta(FORWARD, strike, inf, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.simpleDelta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, true);
      double refC1 = BlackFormulaRepository.simpleDelta(FORWARD, strike, TIME_TO_EXPIRY, 0., true);
      double resC2 = BlackFormulaRepository.simpleDelta(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, true);
      double refC2 = BlackFormulaRepository.simpleDelta(FORWARD, strike, TIME_TO_EXPIRY, inf, true);
      double resP1 = BlackFormulaRepository.simpleDelta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, false);
      double refP1 = BlackFormulaRepository.simpleDelta(FORWARD, strike, TIME_TO_EXPIRY, 0., false);
      double resP2 = BlackFormulaRepository.simpleDelta(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, false);
      double refP2 = BlackFormulaRepository.simpleDelta(FORWARD, strike, TIME_TO_EXPIRY, inf, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, true);
      double resC2 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, true);
      double resC3 = BlackFormulaRepository.simpleDelta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, true);
      double resP1 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, false);
      double resP2 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, false);
      double resP3 = BlackFormulaRepository.simpleDelta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, false);
      double resC4 = BlackFormulaRepository.simpleDelta(1.e12, 1.e12, TIME_TO_EXPIRY, vol, true);
      double resP4 = BlackFormulaRepository.simpleDelta(1.e12, 1.e12, TIME_TO_EXPIRY, vol, false);

      double refC1 = BlackFormulaRepository.simpleDelta(0., 0., TIME_TO_EXPIRY, vol, true);
      double refC2 = BlackFormulaRepository.simpleDelta(0., inf, TIME_TO_EXPIRY, vol, true);
      double refC3 = BlackFormulaRepository.simpleDelta(inf, 0., TIME_TO_EXPIRY, vol, true);
      double refP1 = BlackFormulaRepository.simpleDelta(0., 0., TIME_TO_EXPIRY, vol, false);
      double refP2 = BlackFormulaRepository.simpleDelta(0., inf, TIME_TO_EXPIRY, vol, false);
      double refP3 = BlackFormulaRepository.simpleDelta(inf, 0., TIME_TO_EXPIRY, vol, false);
      double refC4 = BlackFormulaRepository.simpleDelta(inf, inf, TIME_TO_EXPIRY, vol, true);
      double refP4 = BlackFormulaRepository.simpleDelta(inf, inf, TIME_TO_EXPIRY, vol, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

      for (int k = 0; k < 8; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.simpleDelta(1.e-12, strike, 1.e-24, vol, true);
        double resC2 = BlackFormulaRepository.simpleDelta(1.e-12, strike, 1.e24, vol, true);
        double resC3 = BlackFormulaRepository.simpleDelta(1.e12, strike, 1.e-24, vol, true);
        double resP1 = BlackFormulaRepository.simpleDelta(1.e-12, strike, 1.e-24, vol, false);
        double resP2 = BlackFormulaRepository.simpleDelta(1.e-12, strike, 1.e24, vol, false);
        double resP3 = BlackFormulaRepository.simpleDelta(1.e12, strike, 1.e-24, vol, false);
        double resC4 = BlackFormulaRepository.simpleDelta(1.e12, strike, 1.e24, vol, true);
        double resP4 = BlackFormulaRepository.simpleDelta(1.e12, strike, 1.e24, vol, false);

        double refC1 = BlackFormulaRepository.simpleDelta(0., strike, 0., vol, true);
        double refC2 = BlackFormulaRepository.simpleDelta(0., strike, inf, vol, true);
        double refC3 = BlackFormulaRepository.simpleDelta(inf, strike, 0., vol, true);
        double refP1 = BlackFormulaRepository.simpleDelta(0., strike, 0., vol, false);
        double refP2 = BlackFormulaRepository.simpleDelta(0., strike, inf, vol, false);
        double refP3 = BlackFormulaRepository.simpleDelta(inf, strike, 0., vol, false);
        double refC4 = BlackFormulaRepository.simpleDelta(inf, strike, inf, vol, true);
        double refP4 = BlackFormulaRepository.simpleDelta(inf, strike, inf, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-9, 1.e-9)));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.simpleDelta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, true);
      double resC2 = BlackFormulaRepository.simpleDelta(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, true);
      double resC3 = BlackFormulaRepository.simpleDelta(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, true);
      double resP1 = BlackFormulaRepository.simpleDelta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, false);
      double resP2 = BlackFormulaRepository.simpleDelta(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, false);
      double resP3 = BlackFormulaRepository.simpleDelta(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, false);

      double refC1 = BlackFormulaRepository.simpleDelta(0., strike, TIME_TO_EXPIRY, 0., true);
      double refC2 = BlackFormulaRepository.simpleDelta(0., strike, TIME_TO_EXPIRY, inf, true);
      double refC3 = BlackFormulaRepository.simpleDelta(inf, strike, TIME_TO_EXPIRY, 0., true);
      double refP1 = BlackFormulaRepository.simpleDelta(0., strike, TIME_TO_EXPIRY, 0., false);
      double refP2 = BlackFormulaRepository.simpleDelta(0., strike, TIME_TO_EXPIRY, inf, false);
      double refP3 = BlackFormulaRepository.simpleDelta(inf, strike, TIME_TO_EXPIRY, 0., false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

      for (int k = 0; k < 6; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.simpleDelta(forward, 1.e-12, 1.e-24, vol, true);
        double resC2 = BlackFormulaRepository.simpleDelta(forward, 1.e-12, 1.e24, vol, true);
        double resC3 = BlackFormulaRepository.simpleDelta(forward, 1.e12, 1.e-24, vol, true);
        double resP1 = BlackFormulaRepository.simpleDelta(forward, 1.e-12, 1.e-24, vol, false);
        double resP2 = BlackFormulaRepository.simpleDelta(forward, 1.e-12, 1.e24, vol, false);
        double resP3 = BlackFormulaRepository.simpleDelta(forward, 1.e12, 1.e-24, vol, false);
        double resC4 = BlackFormulaRepository.simpleDelta(forward, 1.e12, 1.e24, vol, true);
        double resP4 = BlackFormulaRepository.simpleDelta(forward, 1.e12, 1.e24, vol, false);

        double refC1 = BlackFormulaRepository.simpleDelta(forward, 0., 0., vol, true);
        double refC2 = BlackFormulaRepository.simpleDelta(forward, 0., inf, vol, true);
        double refC3 = BlackFormulaRepository.simpleDelta(forward, inf, 0., vol, true);
        double refP1 = BlackFormulaRepository.simpleDelta(forward, 0., 0., vol, false);
        double refP2 = BlackFormulaRepository.simpleDelta(forward, 0., inf, vol, false);
        double refP3 = BlackFormulaRepository.simpleDelta(forward, inf, 0., vol, false);
        double refC4 = BlackFormulaRepository.simpleDelta(forward, inf, inf, vol, true);
        double refP4 = BlackFormulaRepository.simpleDelta(forward, inf, inf, vol, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-9, 1.e-9)));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.simpleDelta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true);
      double resC2 = BlackFormulaRepository.simpleDelta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, true);
      double resC3 = BlackFormulaRepository.simpleDelta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, true);
      double resP1 = BlackFormulaRepository.simpleDelta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false);
      double resP2 = BlackFormulaRepository.simpleDelta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, false);
      double resP3 = BlackFormulaRepository.simpleDelta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, false);

      double refC1 = BlackFormulaRepository.simpleDelta(forward, 0., TIME_TO_EXPIRY, 0., true);
      double refC2 = BlackFormulaRepository.simpleDelta(forward, 0., TIME_TO_EXPIRY, inf, true);
      double refC3 = BlackFormulaRepository.simpleDelta(forward, inf, TIME_TO_EXPIRY, 0., true);
      double refP1 = BlackFormulaRepository.simpleDelta(forward, 0., TIME_TO_EXPIRY, 0., false);
      double refP2 = BlackFormulaRepository.simpleDelta(forward, 0., TIME_TO_EXPIRY, inf, false);
      double refP3 = BlackFormulaRepository.simpleDelta(forward, inf, TIME_TO_EXPIRY, 0., false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

      for (int k = 0; k < 6; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e-12, 1.e-24, vol, true);
      double resC2 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e-12, 1.e24, vol, true);
      double resC3 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e12, 1.e-24, vol, true);
      double resP1 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e-12, 1.e-24, vol, false);
      double resP2 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e-12, 1.e24, vol, false);
      double resP3 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e12, 1.e-24, vol, false);
      double resC4 = BlackFormulaRepository.simpleDelta(1.e12, 1.e-12, 1.e-24, vol, true);
      double resP4 = BlackFormulaRepository.simpleDelta(1.e12, 1.e-12, 1.e-24, vol, false);
      double resC5 = BlackFormulaRepository.simpleDelta(FORWARD, FORWARD * (1. + 1.e-12), 1.e-24, vol, true);
      double resP5 = BlackFormulaRepository.simpleDelta(FORWARD, FORWARD * (1. + 1.e-12), 1.e-24, vol, false);
      double resC6 = BlackFormulaRepository.simpleDelta(1.e12, 1.e12, 1.e24, vol, true);
      double resP6 = BlackFormulaRepository.simpleDelta(1.e12, 1.e12, 1.e24, vol, false);
      double resC7 = BlackFormulaRepository.simpleDelta(1.e12, 1.e12, 1.e-24, vol, true);
      double resP7 = BlackFormulaRepository.simpleDelta(1.e12, 1.e12, 1.e-24, vol, false);

      double refC1 = BlackFormulaRepository.simpleDelta(0., 0., 0., vol, true);
      double refC2 = BlackFormulaRepository.simpleDelta(0., 0., inf, vol, true);
      double refC3 = BlackFormulaRepository.simpleDelta(0., inf, 0., vol, true);
      double refP1 = BlackFormulaRepository.simpleDelta(0., 0., 0., vol, false);
      double refP2 = BlackFormulaRepository.simpleDelta(0., 0., inf, vol, false);
      double refP3 = BlackFormulaRepository.simpleDelta(0., inf, 0., vol, false);
      double refC4 = BlackFormulaRepository.simpleDelta(inf, 0., 0., vol, true);
      double refP4 = BlackFormulaRepository.simpleDelta(inf, 0., 0., vol, false);
      double refC5 = BlackFormulaRepository.simpleDelta(FORWARD, FORWARD, 0., vol, true);
      double refP5 = BlackFormulaRepository.simpleDelta(FORWARD, FORWARD, 0., vol, false);
      double refC6 = BlackFormulaRepository.simpleDelta(inf, inf, inf, vol, true);
      double refP6 = BlackFormulaRepository.simpleDelta(inf, inf, inf, vol, false);
      double refC7 = BlackFormulaRepository.simpleDelta(inf, inf, 0., vol, true);
      double refP7 = BlackFormulaRepository.simpleDelta(inf, inf, 0., vol, false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7};

      for (int k = 0; k < 14; ++k) {

        if ((refVec[k] != 0.5) && (refVec[k] != -0.5)) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true);
      double resC2 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12, true);
      double resC3 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12, true);
      double resP1 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false);
      double resP2 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12, false);
      double resP3 = BlackFormulaRepository.simpleDelta(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12, false);
      double resC4 = BlackFormulaRepository.simpleDelta(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true);
      double resP4 = BlackFormulaRepository.simpleDelta(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false);
      double resC5 = BlackFormulaRepository.simpleDelta(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12,
          true);
      double resP5 = BlackFormulaRepository.simpleDelta(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12,
          false);

      double refC1 = BlackFormulaRepository.simpleDelta(0., 0., TIME_TO_EXPIRY, 0., true);
      double refC2 = BlackFormulaRepository.simpleDelta(0., 0., TIME_TO_EXPIRY, inf, true);
      double refC3 = BlackFormulaRepository.simpleDelta(0., inf, TIME_TO_EXPIRY, 0., true);
      double refP1 = BlackFormulaRepository.simpleDelta(0., 0., TIME_TO_EXPIRY, 0., false);
      double refP2 = BlackFormulaRepository.simpleDelta(0., 0., TIME_TO_EXPIRY, inf, false);
      double refP3 = BlackFormulaRepository.simpleDelta(0., inf, TIME_TO_EXPIRY, 0., false);
      double refC4 = BlackFormulaRepository.simpleDelta(inf, 0., TIME_TO_EXPIRY, 0., true);
      double refP4 = BlackFormulaRepository.simpleDelta(inf, 0., TIME_TO_EXPIRY, 0., false);
      double refC5 = BlackFormulaRepository.simpleDelta(FORWARD, FORWARD, TIME_TO_EXPIRY, 0., true);
      double refP5 = BlackFormulaRepository.simpleDelta(FORWARD, FORWARD, TIME_TO_EXPIRY, 0., false);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5};

      for (int k = 0; k < 10; ++k) {
        if ((refVec[k] != 0.5) && (refVec[k] != -0.5)) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.simpleDelta(strike, strike, inf, 0., true);
      double resP1 = BlackFormulaRepository.simpleDelta(strike, strike, inf, 0., false);
      double refC1 = NORMAL.getCDF(0.);
      double refP1 = -NORMAL.getCDF(0.);

      double[] resVec = new double[] {resC1, resP1};
      double[] refVec = new double[] {refC1, refP1};
      for (int k = 0; k < 2; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10)));
            }
          }
        }
      }
    }
  }

  @Test
  public void paritySimpleDeltaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.simpleDelta(FORWARD, strike, TIME_TO_EXPIRY, vol, true);
        double resP1 = BlackFormulaRepository.simpleDelta(FORWARD, strike, TIME_TO_EXPIRY, vol, false);
        assertThat(1.).isCloseTo(resC1 - resP1, offset(EPS));
      }
    }
  }

  @Test
  public void negativeVolErrorSimpleDeltaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.simpleDelta(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, true));
  }

  @Test
  public void negativeFwdErrorSimpleDeltaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.simpleDelta(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], true));
  }

  @Test
  public void negativeStrikeErrorSimpleDeltaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.simpleDelta(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], true));
  }

  @Test
  public void negativeTimeErrorSimpleDeltaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.simpleDelta(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], true));
  }

  /*
   * Tests for "gamma"
   */
  /**
   * large/small values
   */
  @Test
  public void exGammaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.gamma(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.gamma(0., strike, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.gamma(1.e12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.gamma(inf, strike, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.gamma(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.gamma(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.gamma(forward, 0., TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.gamma(forward, inf, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.gamma(FORWARD, strike, 1e-24, vol);
        double resC2 = BlackFormulaRepository.gamma(FORWARD, strike, 1e24, vol);
        double refC1 = BlackFormulaRepository.gamma(FORWARD, strike, 0., vol);
        double refC2 = BlackFormulaRepository.gamma(FORWARD, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.gamma(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12);
      double refC1 = BlackFormulaRepository.gamma(FORWARD, strike, TIME_TO_EXPIRY, 0.);
      double resC2 = BlackFormulaRepository.gamma(FORWARD, strike, TIME_TO_EXPIRY, 1.e12);
      double refC2 = BlackFormulaRepository.gamma(FORWARD, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2};
      double[] refVec = new double[] {refC1, refC2};

      for (int k = 0; k < 2; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.gamma(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resC2 = BlackFormulaRepository.gamma(1.e-12, 1.e12, TIME_TO_EXPIRY, vol);
      double resC3 = BlackFormulaRepository.gamma(1.e12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resP1 = BlackFormulaRepository.gamma(1.e12, 1.e12, TIME_TO_EXPIRY, vol);

      double refC1 = BlackFormulaRepository.gamma(0., 0., TIME_TO_EXPIRY, vol);
      double refC2 = BlackFormulaRepository.gamma(0., inf, TIME_TO_EXPIRY, vol);
      double refC3 = BlackFormulaRepository.gamma(inf, 0., TIME_TO_EXPIRY, vol);
      double refP1 = BlackFormulaRepository.gamma(inf, inf, TIME_TO_EXPIRY, vol);

      double[] resVec = new double[] {resC1, resP1, resC2, resC3};
      double[] refVec = new double[] {refC1, refP1, refC2, refC3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.gamma(1.e-12, strike, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.gamma(1.e-12, strike, 1.e24, vol);
        double resC3 = BlackFormulaRepository.gamma(1.e12, strike, 1.e-24, vol);
        double resP1 = BlackFormulaRepository.gamma(1.e12, strike, 1.e24, vol);

        double refC1 = BlackFormulaRepository.gamma(0., strike, 0., vol);
        double refC2 = BlackFormulaRepository.gamma(0., strike, inf, vol);
        double refC3 = BlackFormulaRepository.gamma(inf, strike, 0., vol);
        double refP1 = BlackFormulaRepository.gamma(inf, strike, inf, vol);

        double[] resVec = new double[] {resC1, resP1, resC2, resC3};
        double[] refVec = new double[] {refC1, refP1, refC2, refC3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.gamma(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.gamma(1.e-12, strike, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.gamma(1.e12, strike, TIME_TO_EXPIRY, 1.e-12);

      double refC1 = BlackFormulaRepository.gamma(0., strike, TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.gamma(0., strike, TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.gamma(inf, strike, TIME_TO_EXPIRY, 0.);

      double[] resVec = new double[] {resC1, resC2, resC3};
      double[] refVec = new double[] {refC1, refC2, refC3};

      for (int k = 0; k < 3; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.gamma(forward, 1.e-12, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.gamma(forward, 1.e-12, 1.e24, vol);
        double resC3 = BlackFormulaRepository.gamma(forward, 1.e12, 1.e-24, vol);
        double resP1 = BlackFormulaRepository.gamma(forward, 1.e12, 1.e24, vol);

        double refC1 = BlackFormulaRepository.gamma(forward, 0., 0., vol);
        double refC2 = BlackFormulaRepository.gamma(forward, 0., inf, vol);
        double refC3 = BlackFormulaRepository.gamma(forward, inf, 0., vol);
        double refP1 = BlackFormulaRepository.gamma(forward, inf, inf, vol);

        double[] resVec = new double[] {resC1, resP1, resC2, resC3};
        double[] refVec = new double[] {refC1, refP1, refC2, refC3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.gamma(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.gamma(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.gamma(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12);

      double refC1 = BlackFormulaRepository.gamma(forward, 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.gamma(forward, 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.gamma(forward, inf, TIME_TO_EXPIRY, 0.);

      double[] resVec = new double[] {resC1, resC2, resC3};
      double[] refVec = new double[] {refC1, refC2, refC3};

      for (int k = 0; k < 3; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.gamma(1.e-12, 1.e-12, 1.e-24, vol);
      double resC2 = BlackFormulaRepository.gamma(1.e-12, 1.e-12, 1.e24, vol);
      double resC3 = BlackFormulaRepository.gamma(1.e-12, 1.e12, 1.e-24, vol);
      double resP1 = BlackFormulaRepository.gamma(1.e12, 1.e12, 1.e24, vol);
      double resC4 = BlackFormulaRepository.gamma(1.e12, 1.e-12, 1.e-24, vol);
      double resC5 = BlackFormulaRepository.gamma(FORWARD, FORWARD, 1.e-24, vol); // / "* (1. + 1.e-12) " removed
      double resP2 = BlackFormulaRepository.gamma(1.e12, 1.e12, 1.e-24, vol);

      double refC1 = BlackFormulaRepository.gamma(0., 0., 0., vol);
      double refC2 = BlackFormulaRepository.gamma(0., 0., inf, vol);
      double refC3 = BlackFormulaRepository.gamma(0., inf, 0., vol);
      double refP1 = BlackFormulaRepository.gamma(inf, inf, inf, vol);
      double refC4 = BlackFormulaRepository.gamma(inf, 0., 0., vol);
      double refC5 = BlackFormulaRepository.gamma(FORWARD, FORWARD, 0., vol);
      double refP2 = BlackFormulaRepository.gamma(inf, inf, 0., vol);

      double[] resVec = new double[] {resC1, resP1, resC2, resC3, resC4, resC5, resP2};
      double[] refVec = new double[] {refC1, refP1, refC2, refC3, refC4, refC5, refP2};

      for (int k = 0; k < 6; ++k) {

        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e9).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.gamma(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.gamma(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.gamma(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resC4 = BlackFormulaRepository.gamma(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC5 = BlackFormulaRepository.gamma(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12);

      double refC1 = BlackFormulaRepository.gamma(0., 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.gamma(0., 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.gamma(0., inf, TIME_TO_EXPIRY, 0.);
      double refC4 = BlackFormulaRepository.gamma(inf, 0., TIME_TO_EXPIRY, 0.);
      double refC5 = BlackFormulaRepository.gamma(FORWARD, FORWARD, TIME_TO_EXPIRY, 0.);

      double[] resVec = new double[] {resC1, resC2, resC3, resC4, resC5};
      double[] refVec = new double[] {refC1, refC2, refC3, refC4, refC5};

      for (int k = 0; k < 5; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e9).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.gamma(strike, strike, inf, 0.);
      double refC1 = NORMAL.getPDF(0.5) / strike;
      double[] resVec = new double[] {resC1};
      double[] refVec = new double[] {refC1};
      for (int k = 0; k < 1; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10)));
            }
          }
        }
      }
    }
  }

  @Test
  public void negativeVolErrorGammaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.gamma(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5));
  }

  @Test
  public void negativeFwdErrorGammaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.gamma(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeStrikeErrorGammaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.gamma(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeTimeErrorGammaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.gamma(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1]));
  }

  /*
   * Tests for "dualGamma"
   */
  /**
   * large/small values
   */
  @Test
  public void exDualGammaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualGamma(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.dualGamma(0., strike, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.dualGamma(1.e12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.dualGamma(inf, strike, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualGamma(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.dualGamma(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.dualGamma(forward, 0., TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.dualGamma(forward, inf, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualGamma(FORWARD, strike, 1e-24, vol);
        double resC2 = BlackFormulaRepository.dualGamma(FORWARD, strike, 1e24, vol);
        double refC1 = BlackFormulaRepository.dualGamma(FORWARD, strike, 0., vol);
        double refC2 = BlackFormulaRepository.dualGamma(FORWARD, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.dualGamma(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12);
      double refC1 = BlackFormulaRepository.dualGamma(FORWARD, strike, TIME_TO_EXPIRY, 0.);
      double resC2 = BlackFormulaRepository.dualGamma(FORWARD, strike, TIME_TO_EXPIRY, 1.e12);
      double refC2 = BlackFormulaRepository.dualGamma(FORWARD, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2};
      double[] refVec = new double[] {refC1, refC2};

      for (int k = 0; k < 2; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.dualGamma(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resC2 = BlackFormulaRepository.dualGamma(1.e-12, 1.e12, TIME_TO_EXPIRY, vol);
      double resC3 = BlackFormulaRepository.dualGamma(1.e12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resP1 = BlackFormulaRepository.dualGamma(1.e12, 1.e12, TIME_TO_EXPIRY, vol);

      double refC1 = BlackFormulaRepository.dualGamma(0., 0., TIME_TO_EXPIRY, vol);
      double refC2 = BlackFormulaRepository.dualGamma(0., inf, TIME_TO_EXPIRY, vol);
      double refC3 = BlackFormulaRepository.dualGamma(inf, 0., TIME_TO_EXPIRY, vol);
      double refP1 = BlackFormulaRepository.dualGamma(inf, inf, TIME_TO_EXPIRY, vol);

      double[] resVec = new double[] {resC1, resP1, resC2, resC3};
      double[] refVec = new double[] {refC1, refP1, refC2, refC3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualGamma(1.e-12, strike, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.dualGamma(1.e-12, strike, 1.e24, vol);
        double resC3 = BlackFormulaRepository.dualGamma(1.e12, strike, 1.e-24, vol);
        double resP1 = BlackFormulaRepository.dualGamma(1.e12, strike, 1.e24, vol);

        double refC1 = BlackFormulaRepository.dualGamma(0., strike, 0., vol);
        double refC2 = BlackFormulaRepository.dualGamma(0., strike, inf, vol);
        double refC3 = BlackFormulaRepository.dualGamma(inf, strike, 0., vol);
        double refP1 = BlackFormulaRepository.dualGamma(inf, strike, inf, vol);

        double[] resVec = new double[] {resC1, resP1, resC2, resC3};
        double[] refVec = new double[] {refC1, refP1, refC2, refC3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.dualGamma(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.dualGamma(1.e-12, strike, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.dualGamma(1.e12, strike, TIME_TO_EXPIRY, 1.e-12);

      double refC1 = BlackFormulaRepository.dualGamma(0., strike, TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.dualGamma(0., strike, TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.dualGamma(inf, strike, TIME_TO_EXPIRY, 0.);

      double[] resVec = new double[] {resC1, resC2, resC3};
      double[] refVec = new double[] {refC1, refC2, refC3};

      for (int k = 0; k < 3; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualGamma(forward, 1.e-12, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.dualGamma(forward, 1.e-12, 1.e24, vol);
        double resC3 = BlackFormulaRepository.dualGamma(forward, 1.e12, 1.e-24, vol);
        double resP1 = BlackFormulaRepository.dualGamma(forward, 1.e12, 1.e24, vol);

        double refC1 = BlackFormulaRepository.dualGamma(forward, 0., 0., vol);
        double refC2 = BlackFormulaRepository.dualGamma(forward, 0., inf, vol);
        double refC3 = BlackFormulaRepository.dualGamma(forward, inf, 0., vol);
        double refP1 = BlackFormulaRepository.dualGamma(forward, inf, inf, vol);

        double[] resVec = new double[] {resC1, resP1, resC2, resC3};
        double[] refVec = new double[] {refC1, refP1, refC2, refC3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.dualGamma(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.dualGamma(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.dualGamma(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12);

      double refC1 = BlackFormulaRepository.dualGamma(forward, 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.dualGamma(forward, 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.dualGamma(forward, inf, TIME_TO_EXPIRY, 0.);

      double[] resVec = new double[] {resC1, resC2, resC3};
      double[] refVec = new double[] {refC1, refC2, refC3};

      for (int k = 0; k < 3; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.dualGamma(1.e-12, 1.e-12, 1.e-24, vol);
      double resC2 = BlackFormulaRepository.dualGamma(1.e-12, 1.e-12, 1.e24, vol);
      double resC3 = BlackFormulaRepository.dualGamma(1.e-12, 1.e12, 1.e-24, vol);
      double resP1 = BlackFormulaRepository.dualGamma(1.e12, 1.e12, 1.e24, vol);
      double resC4 = BlackFormulaRepository.dualGamma(1.e12, 1.e-12, 1.e-24, vol);
      double resC5 = BlackFormulaRepository.dualGamma(FORWARD, FORWARD, 1.e-24, vol); // / "* (1. + 1.e-12) "
                                                                                      // removed
      double resP2 = BlackFormulaRepository.dualGamma(1.e12, 1.e12, 1.e-24, vol);

      double refC1 = BlackFormulaRepository.dualGamma(0., 0., 0., vol);
      double refC2 = BlackFormulaRepository.dualGamma(0., 0., inf, vol);
      double refC3 = BlackFormulaRepository.dualGamma(0., inf, 0., vol);
      double refP1 = BlackFormulaRepository.dualGamma(inf, inf, inf, vol);
      double refC4 = BlackFormulaRepository.dualGamma(inf, 0., 0., vol);
      double refC5 = BlackFormulaRepository.dualGamma(FORWARD, FORWARD, 0., vol);
      double refP2 = BlackFormulaRepository.dualGamma(inf, inf, 0., vol);

      double[] resVec = new double[] {resC1, resP1, resC2, resC3, resC4, resC5, resP2};
      double[] refVec = new double[] {refC1, refP1, refC2, refC3, refC4, refC5, refP2};

      for (int k = 0; k < 6; ++k) {  // k=7 ref value is not accurate due to non-unity of vol

        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e9).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.dualGamma(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.dualGamma(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.dualGamma(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resC4 = BlackFormulaRepository.dualGamma(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC5 = BlackFormulaRepository.dualGamma(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12);

      double refC1 = BlackFormulaRepository.dualGamma(0., 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.dualGamma(0., 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.dualGamma(0., inf, TIME_TO_EXPIRY, 0.);
      double refC4 = BlackFormulaRepository.dualGamma(inf, 0., TIME_TO_EXPIRY, 0.);
      double refC5 = BlackFormulaRepository.dualGamma(FORWARD, FORWARD, TIME_TO_EXPIRY, 0.);

      double[] resVec = new double[] {resC1, resC2, resC3, resC4, resC5};
      double[] refVec = new double[] {refC1, refC2, refC3, refC4, refC5};

      for (int k = 0; k < 5; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e9).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.dualGamma(strike, strike, inf, 0.);
      double refC1 = NORMAL.getPDF(0.5) / strike;
      double[] resVec = new double[] {resC1};
      double[] refVec = new double[] {refC1};
      for (int k = 0; k < 1; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10)));
            }
          }
        }
      }
    }
  }

  @Test
  public void negativeVolErrorDualGammaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.dualGamma(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5));
  }

  @Test
  public void negativeFwdErrorDualGammaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.dualGamma(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeStrikeErrorDualGammaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.dualGamma(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeTimeErrorDualGammaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.dualGamma(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1]));
  }

  /*
   * crossGamma
   */
  /**
   * large/small value
   */
  @Test
  public void exCrossGammaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.crossGamma(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.crossGamma(0., strike, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.crossGamma(1.e12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.crossGamma(inf, strike, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.crossGamma(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.crossGamma(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.crossGamma(forward, 0., TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.crossGamma(forward, inf, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.crossGamma(FORWARD, strike, 1e-24, vol);
        double resC2 = BlackFormulaRepository.crossGamma(FORWARD, strike, 1e24, vol);
        double refC1 = BlackFormulaRepository.crossGamma(FORWARD, strike, 0., vol);
        double refC2 = BlackFormulaRepository.crossGamma(FORWARD, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.crossGamma(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12);
      double refC1 = BlackFormulaRepository.crossGamma(FORWARD, strike, TIME_TO_EXPIRY, 0.);
      double resC2 = BlackFormulaRepository.crossGamma(FORWARD, strike, TIME_TO_EXPIRY, 1.e12);
      double refC2 = BlackFormulaRepository.crossGamma(FORWARD, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2};
      double[] refVec = new double[] {refC1, refC2};

      for (int k = 0; k < 2; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.crossGamma(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resC2 = BlackFormulaRepository.crossGamma(1.e-12, 1.e12, TIME_TO_EXPIRY, vol);
      double resC3 = BlackFormulaRepository.crossGamma(1.e12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resP1 = BlackFormulaRepository.crossGamma(1.e12, 1.e12, TIME_TO_EXPIRY, vol);

      double refC1 = BlackFormulaRepository.crossGamma(0., 0., TIME_TO_EXPIRY, vol);
      double refC2 = BlackFormulaRepository.crossGamma(0., inf, TIME_TO_EXPIRY, vol);
      double refC3 = BlackFormulaRepository.crossGamma(inf, 0., TIME_TO_EXPIRY, vol);
      double refP1 = BlackFormulaRepository.crossGamma(inf, inf, TIME_TO_EXPIRY, vol);

      double[] resVec = new double[] {resC1, resP1, resC2, resC3};
      double[] refVec = new double[] {refC1, refP1, refC2, refC3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.crossGamma(1.e-12, strike, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.crossGamma(1.e-12, strike, 1.e24, vol);
        double resC3 = BlackFormulaRepository.crossGamma(1.e12, strike, 1.e-24, vol);
        double resP1 = BlackFormulaRepository.crossGamma(1.e12, strike, 1.e24, vol);

        double refC1 = BlackFormulaRepository.crossGamma(0., strike, 0., vol);
        double refC2 = BlackFormulaRepository.crossGamma(0., strike, inf, vol);
        double refC3 = BlackFormulaRepository.crossGamma(inf, strike, 0., vol);
        double refP1 = BlackFormulaRepository.crossGamma(inf, strike, inf, vol);

        double[] resVec = new double[] {resC1, resP1, resC2, resC3};
        double[] refVec = new double[] {refC1, refP1, refC2, refC3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.crossGamma(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.crossGamma(1.e-12, strike, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.crossGamma(1.e12, strike, TIME_TO_EXPIRY, 1.e-12);

      double refC1 = BlackFormulaRepository.crossGamma(0., strike, TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.crossGamma(0., strike, TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.crossGamma(inf, strike, TIME_TO_EXPIRY, 0.);

      double[] resVec = new double[] {resC1, resC2, resC3};
      double[] refVec = new double[] {refC1, refC2, refC3};

      for (int k = 0; k < 3; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.crossGamma(forward, 1.e-12, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.crossGamma(forward, 1.e-12, 1.e24, vol);
        double resC3 = BlackFormulaRepository.crossGamma(forward, 1.e12, 1.e-24, vol);
        double resP1 = BlackFormulaRepository.crossGamma(forward, 1.e12, 1.e24, vol);

        double refC1 = BlackFormulaRepository.crossGamma(forward, 0., 0., vol);
        double refC2 = BlackFormulaRepository.crossGamma(forward, 0., inf, vol);
        double refC3 = BlackFormulaRepository.crossGamma(forward, inf, 0., vol);
        double refP1 = BlackFormulaRepository.crossGamma(forward, inf, inf, vol);

        double[] resVec = new double[] {resC1, resP1, resC2, resC3};
        double[] refVec = new double[] {refC1, refP1, refC2, refC3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.crossGamma(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.crossGamma(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.crossGamma(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12);

      double refC1 = BlackFormulaRepository.crossGamma(forward, 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.crossGamma(forward, 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.crossGamma(forward, inf, TIME_TO_EXPIRY, 0.);

      double[] resVec = new double[] {resC1, resC2, resC3};
      double[] refVec = new double[] {refC1, refC2, refC3};

      for (int k = 0; k < 3; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.crossGamma(1.e-12, 1.e-12, 1.e-24, vol);
      double resC2 = BlackFormulaRepository.crossGamma(1.e-12, 1.e-12, 1.e24, vol);
      double resC3 = BlackFormulaRepository.crossGamma(1.e-12, 1.e12, 1.e-24, vol);
      double resP1 = BlackFormulaRepository.crossGamma(1.e12, 1.e12, 1.e24, vol);
      double resC4 = BlackFormulaRepository.crossGamma(1.e12, 1.e-12, 1.e-24, vol);
      double resC5 = BlackFormulaRepository.crossGamma(FORWARD, FORWARD, 1.e-24, vol); // / "* (1. + 1.e-12) "
                                                                                       // removed
      double resP2 = BlackFormulaRepository.crossGamma(1.e12, 1.e12, 1.e-24, vol);

      double refC1 = BlackFormulaRepository.crossGamma(0., 0., 0., vol);
      double refC2 = BlackFormulaRepository.crossGamma(0., 0., inf, vol);
      double refC3 = BlackFormulaRepository.crossGamma(0., inf, 0., vol);
      double refP1 = BlackFormulaRepository.crossGamma(inf, inf, inf, vol);
      double refC4 = BlackFormulaRepository.crossGamma(inf, 0., 0., vol);
      double refC5 = BlackFormulaRepository.crossGamma(FORWARD, FORWARD, 0., vol);
      double refP2 = BlackFormulaRepository.crossGamma(inf, inf, 0., vol);

      double[] resVec = new double[] {resC1, resP1, resC2, resC3, resC4, resC5, resP2};
      double[] refVec = new double[] {refC1, refP1, refC2, refC3, refC4, refC5, refP2};

      for (int k = 0; k < 6; ++k) {  // k=7 ref value is not accurate due to non-unity of vol

        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e9).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e9).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.crossGamma(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.crossGamma(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.crossGamma(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resC4 = BlackFormulaRepository.crossGamma(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC5 = BlackFormulaRepository.crossGamma(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12);

      double refC1 = BlackFormulaRepository.crossGamma(0., 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.crossGamma(0., 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.crossGamma(0., inf, TIME_TO_EXPIRY, 0.);
      double refC4 = BlackFormulaRepository.crossGamma(inf, 0., TIME_TO_EXPIRY, 0.);
      double refC5 = BlackFormulaRepository.crossGamma(FORWARD, FORWARD, TIME_TO_EXPIRY, 0.);

      double[] resVec = new double[] {resC1, resC2, resC3, resC4, resC5};
      double[] refVec = new double[] {refC1, refC2, refC3, refC4, refC5};

      for (int k = 0; k < 5; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e9).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e9).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.crossGamma(strike, strike, inf, 0.);
      double refC1 = -NORMAL.getPDF(0.5) / strike;
      double[] resVec = new double[] {resC1};
      double[] refVec = new double[] {refC1};
      for (int k = 0; k < 1; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10)));
            }
          }
        }
      }
    }
  }

  @Test
  public void negativeVolErrorCrossGammaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.crossGamma(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5));
  }

  @Test
  public void negativeFwdErrorCrossGammaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.crossGamma(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeStrikeErrorCrossGammaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.crossGamma(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeTimeErrorCrossGammaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.crossGamma(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1]));
  }

  /*
   * Theta tests
   */
  /**
   * large/small input
   */
  @Test
  public void exThetaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.theta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, true, 0.05);
        double refC1 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, vol, true, 0.05);
        double resC2 = BlackFormulaRepository.theta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, true, 0.05);
        double refC2 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, vol, true, 0.05);
        double resP1 = BlackFormulaRepository.theta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, false, 0.05);
        double refP1 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, vol, false, 0.05);
        double resP2 = BlackFormulaRepository.theta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, false, 0.05);
        double refP2 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, vol, false, 0.05);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.theta(forward, 1.e-14 * forward, TIME_TO_EXPIRY, vol, true, 0.05);
        double resC2 = BlackFormulaRepository.theta(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol, true, 0.05);
        double resP1 = BlackFormulaRepository.theta(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol, false, 0.05);
        double resP2 = BlackFormulaRepository.theta(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol, false, 0.05);
        double refC1 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, vol, true, 0.05);
        double refC2 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, vol, true, 0.05);
        double refP1 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, vol, false, 0.05);
        double refP2 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, vol, false, 0.05);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.theta(FORWARD, strike, 1e-12, vol, true, 0.05);
        double resC2 = BlackFormulaRepository.theta(FORWARD, strike, 1e12, vol, true, 0.05);
        double resP1 = BlackFormulaRepository.theta(FORWARD, strike, 1e-12, vol, false, 0.05);
        double resP2 = BlackFormulaRepository.theta(FORWARD, strike, 1e12, vol, false, 0.05);
        double refC1 = BlackFormulaRepository.theta(FORWARD, strike, 0., vol, true, 0.05);
        double refC2 = BlackFormulaRepository.theta(FORWARD, strike, inf, vol, true, 0.05);
        double refP1 = BlackFormulaRepository.theta(FORWARD, strike, 0., vol, false, 0.05);
        double refP2 = BlackFormulaRepository.theta(FORWARD, strike, inf, vol, false, 0.05);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-24, true, 0.05);
      double refC1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 0., true, 0.05);
      double resC2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 1.e24, true, 0.05);
      double refC2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, inf, true, 0.05);
      double resP1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-24, false, 0.05);
      double refP1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 0., false, 0.05);
      double resP2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 1.e24, false, 0.05);
      double refP2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, inf, false, 0.05);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, vol, true, 1.e-12);
        double resP1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, vol, false, 1.e-12);
        double refC1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, vol, true, 0.);
        double refP1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, vol, false, 0.);
        double resC2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, vol, true, 1.e12);
        double resP2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, vol, false, 1.e12);
        double refC2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, vol, true, inf);
        double refP2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, vol, false, inf);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e8).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e9).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-9, 1.e-9)));
              }
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.theta(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, true, 0.05);
      double resC2 = BlackFormulaRepository.theta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, true, 0.05);
      double resC3 = BlackFormulaRepository.theta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, true, 0.05);
      double resP1 = BlackFormulaRepository.theta(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, false, 0.05);
      double resP2 = BlackFormulaRepository.theta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, false, 0.05);
      double resP3 = BlackFormulaRepository.theta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, false, 0.05);
      double resC4 = BlackFormulaRepository.theta(1.e12, 1.e12, TIME_TO_EXPIRY, vol, true, 0.05);
      double resP4 = BlackFormulaRepository.theta(1.e12, 1.e12, TIME_TO_EXPIRY, vol, false, 0.05);
      double resC5 = BlackFormulaRepository.theta(1.e10, 1.e11, TIME_TO_EXPIRY, vol, true, 0.05);
      double resP5 = BlackFormulaRepository.theta(1.e11, 1.e10, TIME_TO_EXPIRY, vol, false, 0.05);

      double refC1 = BlackFormulaRepository.theta(0., 0., TIME_TO_EXPIRY, vol, true, 0.05);
      double refC2 = BlackFormulaRepository.theta(0., inf, TIME_TO_EXPIRY, vol, true, 0.05);
      double refC3 = BlackFormulaRepository.theta(inf, 0., TIME_TO_EXPIRY, vol, true, 0.05);
      double refP1 = BlackFormulaRepository.theta(0., 0., TIME_TO_EXPIRY, vol, false, 0.05);
      double refP2 = BlackFormulaRepository.theta(0., inf, TIME_TO_EXPIRY, vol, false, 0.05);
      double refP3 = BlackFormulaRepository.theta(inf, 0., TIME_TO_EXPIRY, vol, false, 0.05);
      double refC4 = BlackFormulaRepository.theta(inf, inf, TIME_TO_EXPIRY, vol, true, 0.05);
      double refP4 = BlackFormulaRepository.theta(inf, inf, TIME_TO_EXPIRY, vol, false, 0.05);
      double refC5 = BlackFormulaRepository.theta(1.e15, 1.e16, TIME_TO_EXPIRY, vol, true, 0.05);
      double refP5 = BlackFormulaRepository.theta(1.e16, 1.e15, TIME_TO_EXPIRY, vol, false, 0.05);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5};

      for (int k = 0; k < 6; ++k) {  // ref values
        if (k != 6 && k != 7) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e8).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e9).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.theta(1.e-12, strike, 1.e-24, vol, true, 0.05);
        double resC2 = BlackFormulaRepository.theta(1.e-12, strike, 1.e24, vol, true, 0.05);
        double resC3 = BlackFormulaRepository.theta(1.e12, strike, 1.e-24, vol, true, 0.05);
        double resP1 = BlackFormulaRepository.theta(1.e-12, strike, 1.e-24, vol, false, 0.05);
        double resP2 = BlackFormulaRepository.theta(1.e-12, strike, 1.e24, vol, false, 0.05);
        double resP3 = BlackFormulaRepository.theta(1.e12, strike, 1.e-24, vol, false, 0.05);
        double resC4 = BlackFormulaRepository.theta(1.e12, strike, 1.e24, vol, true, 0.05);
        double resP4 = BlackFormulaRepository.theta(1.e12, strike, 1.e24, vol, false, 0.05);

        double refC1 = BlackFormulaRepository.theta(0., strike, 0., vol, true, 0.05);
        double refC2 = BlackFormulaRepository.theta(0., strike, inf, vol, true, 0.05);
        double refC3 = BlackFormulaRepository.theta(inf, strike, 0., vol, true, 0.05);
        double refP1 = BlackFormulaRepository.theta(0., strike, 0., vol, false, 0.05);
        double refP2 = BlackFormulaRepository.theta(0., strike, inf, vol, false, 0.05);
        double refP3 = BlackFormulaRepository.theta(inf, strike, 0., vol, false, 0.05);
        double refC4 = BlackFormulaRepository.theta(inf, strike, inf, vol, true, 0.05);
        double refP4 = BlackFormulaRepository.theta(inf, strike, inf, vol, false, 0.05);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-24, true, 0.05);
      double resC2 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, 1.e24, true, 0.05);
      double resC3 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, 1.e-24, true, 0.05);
      double resP1 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-24, false, 0.05);
      double resP2 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, 1.e24, false, 0.05);
      double resP3 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, 1.e-24, false, 0.05);
      double resC4 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, 1.e24, true, 0.05);
      double resP4 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, 1.e24, false, 0.05);

      double refC1 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, 0., true, 0.05);
      double refC2 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, inf, true, 0.05);
      double refC3 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, 0., true, 0.05);
      double refP1 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, 0., false, 0.05);
      double refP2 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, inf, false, 0.05);
      double refP3 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, 0., false, 0.05);
      double refC4 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, inf, true, 0.05);
      double refP4 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, inf, false, 0.05);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

      for (int k = 0; k < 8; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.theta(forward, 1.e-12, 1.e-24, vol, true, 0.05);
        double resC2 = BlackFormulaRepository.theta(forward, 1.e-12, 1.e24, vol, true, 0.05);
        double resC3 = BlackFormulaRepository.theta(forward, 1.e12, 1.e-24, vol, true, 0.05);
        double resP1 = BlackFormulaRepository.theta(forward, 1.e-12, 1.e-24, vol, false, 0.05);
        double resP2 = BlackFormulaRepository.theta(forward, 1.e-12, 1.e24, vol, false, 0.05);
        double resP3 = BlackFormulaRepository.theta(forward, 1.e12, 1.e-24, vol, false, 0.05);
        double resC4 = BlackFormulaRepository.theta(forward, 1.e12, 1.e24, vol, true, 0.05);
        double resP4 = BlackFormulaRepository.theta(forward, 1.e12, 1.e24, vol, false, 0.05);

        double refC1 = BlackFormulaRepository.theta(forward, 0., 0., vol, true, 0.05);
        double refC2 = BlackFormulaRepository.theta(forward, 0., inf, vol, true, 0.05);
        double refC3 = BlackFormulaRepository.theta(forward, inf, 0., vol, true, 0.05);
        double refP1 = BlackFormulaRepository.theta(forward, 0., 0., vol, false, 0.05);
        double refP2 = BlackFormulaRepository.theta(forward, 0., inf, vol, false, 0.05);
        double refP3 = BlackFormulaRepository.theta(forward, inf, 0., vol, false, 0.05);
        double refC4 = BlackFormulaRepository.theta(forward, inf, inf, vol, true, 0.05);
        double refP4 = BlackFormulaRepository.theta(forward, inf, inf, vol, false, 0.05);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.theta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true, 0.05);
      double resC2 = BlackFormulaRepository.theta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, true, 0.05);
      double resC3 = BlackFormulaRepository.theta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, true, 0.05);
      double resP1 = BlackFormulaRepository.theta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false, 0.05);
      double resP2 = BlackFormulaRepository.theta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, false, 0.05);
      double resP3 = BlackFormulaRepository.theta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, false, 0.05);
      double resC4 = BlackFormulaRepository.theta(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, true, 0.05);
      double resP4 = BlackFormulaRepository.theta(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, false, 0.05);

      double refC1 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, 0., true, 0.05);
      double refC2 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, inf, true, 0.05);
      double refC3 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, 0., true, 0.05);
      double refP1 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, 0., false, 0.05);
      double refP2 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, inf, false, 0.05);
      double refP3 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, 0., false, 0.05);
      double refC4 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, inf, true, 0.05);
      double refP4 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, inf, false, 0.05);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

      for (int k = 0; k < 8; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.theta(FORWARD, strike, 1.e-12, vol, true, 1.e-12);
        double resC2 = BlackFormulaRepository.theta(FORWARD, strike, 1.e12, vol, true, 1.e-12);
        double resP1 = BlackFormulaRepository.theta(FORWARD, strike, 1.e-12, vol, false, 1.e-12);
        double resP2 = BlackFormulaRepository.theta(FORWARD, strike, 1.e12, vol, false, 1.e-12);
        double resC3 = BlackFormulaRepository.theta(FORWARD, strike, 1.e12, vol, true, 1.e12);
        double resP3 = BlackFormulaRepository.theta(FORWARD, strike, 1.e12, vol, false, 1.e12);
        double resC4 = BlackFormulaRepository.theta(FORWARD, strike, 1.e-12, vol, true, 1.e12);
        double resP4 = BlackFormulaRepository.theta(FORWARD, strike, 1.e-12, vol, false, 1.e12);

        double refC1 = BlackFormulaRepository.theta(FORWARD, strike, 0., vol, true, 0.);
        double refC2 = BlackFormulaRepository.theta(FORWARD, strike, inf, vol, true, 0.);
        double refP1 = BlackFormulaRepository.theta(FORWARD, strike, 0., vol, false, 0.);
        double refP2 = BlackFormulaRepository.theta(FORWARD, strike, inf, vol, false, 0.);
        double refC3 = BlackFormulaRepository.theta(FORWARD, strike, inf, vol, true, inf);
        double refP3 = BlackFormulaRepository.theta(FORWARD, strike, inf, vol, false, inf);
        double refC4 = BlackFormulaRepository.theta(FORWARD, strike, 0., vol, true, inf);
        double refP4 = BlackFormulaRepository.theta(FORWARD, strike, 0., vol, false, inf);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 6; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.theta(strike, 1.e-12, TIME_TO_EXPIRY, vol, true, 1.e-12);
        double resC2 = BlackFormulaRepository.theta(strike, 1.e12, TIME_TO_EXPIRY, vol, true, 1.e-12);
        double resP1 = BlackFormulaRepository.theta(strike, 1.e-12, TIME_TO_EXPIRY, vol, false, 1.e-12);
        double resP2 = BlackFormulaRepository.theta(strike, 1.e12, TIME_TO_EXPIRY, vol, false, 1.e-12);
        double resC3 = BlackFormulaRepository.theta(strike, 1.e12, TIME_TO_EXPIRY, vol, true, 1.e12);
        double resP3 = BlackFormulaRepository.theta(strike, 1.e12, TIME_TO_EXPIRY, vol, false, 1.e12);

        double refC1 = BlackFormulaRepository.theta(strike, 0., TIME_TO_EXPIRY, vol, true, 0.);
        double refC2 = BlackFormulaRepository.theta(strike, inf, TIME_TO_EXPIRY, vol, true, 0.);
        double refP1 = BlackFormulaRepository.theta(strike, 0., TIME_TO_EXPIRY, vol, false, 0.);
        double refP2 = BlackFormulaRepository.theta(strike, inf, TIME_TO_EXPIRY, vol, false, 0.);
        double refC3 = BlackFormulaRepository.theta(strike, inf, TIME_TO_EXPIRY, vol, true, inf);
        double refP3 = BlackFormulaRepository.theta(strike, inf, TIME_TO_EXPIRY, vol, false, inf);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

        for (int k = 0; k < 6; ++k) {
          if (k != 3) {
            if (refVec[k] > 1.e10) {
              assertThat(resVec[k] > 1.e10).isTrue();
            } else {
              if (refVec[k] < -1.e10) {
                assertThat(resVec[k] < -1.e10).isTrue();
              } else {
                if (refVec[k] == 0.) {
                  assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
                } else {
                  assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-9, 1.e-9)));
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, true, 1.e-12);
      double resC2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, true, 1.e-12);
      double resP1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, false, 1.e-12);
      double resP2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, false, 1.e-12);
      double resC3 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, true, 1.e12);
      double resP3 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, false, 1.e12);
      double resC4 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, true, 1.e12);
      double resP4 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, false, 1.e12);

      double refC1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 0., true, 0.);
      double refC2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, inf, true, 0.);
      double refP1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 0., false, 0.);
      double refP2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, inf, false, 0.);
      double refC3 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, inf, true, inf);
      double refP3 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, inf, false, inf);
      double refC4 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 0., true, inf);
      double refP4 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, 0., false, inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

      for (int k = 0; k < 6; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e9).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, vol, true, 1.e-12);
        double resC2 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, vol, true, 1.e-12);
        double resP1 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, vol, false, 1.e-12);
        double resP2 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, vol, false, 1.e-12);
        double resC3 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, vol, true, 1.e12);
        double resP3 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, vol, false, 1.e12);
        double resC4 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, vol, true, 1.e12);
        double resP4 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, vol, false, 1.e12);

        double refC1 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, vol, true, 0.);
        double refC2 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, vol, true, 0.);
        double refP1 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, vol, false, 0.);
        double refP2 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, vol, false, 0.);
        double refC3 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, vol, true, inf);
        double refP3 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, vol, false, inf);
        double refC4 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, vol, true, inf);
        double refP4 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, vol, false, inf);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (k != 2 && k != 7) {
            if (refVec[k] > 1.e10) {
              assertThat(resVec[k] > 1.e10).isTrue();
            } else {
              if (refVec[k] < -1.e10) {
                assertThat(resVec[k] < -1.e10).isTrue();
              } else {
                if (refVec[k] == 0.) {
                  assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
                } else {
                  assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-9, 1.e-9)));
                }
              }
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.theta(1.e-12, 1.e-12, 1.e-24, vol, true, 0.05);
      double resC2 = BlackFormulaRepository.theta(1.e-12, 1.e-12, 1.e24, vol, true, 0.05);
      double resC3 = BlackFormulaRepository.theta(1.e-12, 1.e12, 1.e-24, vol, true, 0.05);
      double resP1 = BlackFormulaRepository.theta(1.e-12, 1.e-12, 1.e-24, vol, false, 0.05);
      double resP2 = BlackFormulaRepository.theta(1.e-12, 1.e-12, 1.e24, vol, false, 0.05);
      double resP3 = BlackFormulaRepository.theta(1.e-12, 1.e12, 1.e-24, vol, false, 0.05);
      double resC4 = BlackFormulaRepository.theta(1.e12, 1.e-12, 1.e-24, vol, true, 0.05);
      double resP4 = BlackFormulaRepository.theta(1.e12, 1.e-12, 1.e-24, vol, false, 0.05);
      double resC5 = BlackFormulaRepository.theta(FORWARD, FORWARD * (1. + 1.e-12), 1.e-24, vol, true, 0.05);
      double resP5 = BlackFormulaRepository.theta(FORWARD, FORWARD * (1. + 1.e-12), 1.e-24, vol, false, 0.05);
      double resC6 = BlackFormulaRepository.theta(1.e12, 1.e12, 1.e24, vol, true, 0.05);
      double resP6 = BlackFormulaRepository.theta(1.e12, 1.e12, 1.e24, vol, false, 0.05);

      double refC1 = BlackFormulaRepository.theta(0., 0., 0., vol, true, 0.05);
      double refC2 = BlackFormulaRepository.theta(0., 0., inf, vol, true, 0.05);
      double refC3 = BlackFormulaRepository.theta(0., inf, 0., vol, true, 0.05);
      double refP1 = BlackFormulaRepository.theta(0., 0., 0., vol, false, 0.05);
      double refP2 = BlackFormulaRepository.theta(0., 0., inf, vol, false, 0.05);
      double refP3 = BlackFormulaRepository.theta(0., inf, 0., vol, false, 0.05);
      double refC4 = BlackFormulaRepository.theta(inf, 0., 0., vol, true, 0.05);
      double refP4 = BlackFormulaRepository.theta(inf, 0., 0., vol, false, 0.05);
      double refC5 = BlackFormulaRepository.theta(FORWARD, FORWARD, 0., vol, true, 0.05);
      double refP5 = BlackFormulaRepository.theta(FORWARD, FORWARD, 0., vol, false, 0.05);
      double refC6 = BlackFormulaRepository.theta(inf, inf, inf, vol, true, 0.05);
      double refP6 = BlackFormulaRepository.theta(inf, inf, inf, vol, false, 0.05);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6};
      for (int k = 0; k < 12; ++k) {

        if ((refVec[k] != -0.5 * vol) && (refVec[k] != -0.5 * FORWARD) && (refVec[k] != Double.NEGATIVE_INFINITY) &&
            k != 11) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-7).isTrue();  //should be rechecked
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.theta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true, 0.05);
      double resC2 = BlackFormulaRepository.theta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12, true, 0.05);
      double resC3 = BlackFormulaRepository.theta(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12, true, 0.05);
      double resP1 = BlackFormulaRepository.theta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false, 0.05);
      double resP2 = BlackFormulaRepository.theta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12, false, 0.05);
      double resP3 = BlackFormulaRepository.theta(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12, false, 0.05);
      double resC4 = BlackFormulaRepository.theta(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true, 0.05);
      double resP4 = BlackFormulaRepository.theta(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false, 0.05);
      double resC5 = BlackFormulaRepository.theta(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12, true,
          0.05);
      double resP5 = BlackFormulaRepository.theta(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12,
          false, 0.05);
      double resC6 = BlackFormulaRepository.theta(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e24, true, 0.05);
      double resP6 = BlackFormulaRepository.theta(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e24, false, 0.05);

      double refC1 = BlackFormulaRepository.theta(0., 0., TIME_TO_EXPIRY, 0., true, 0.05);
      double refC2 = BlackFormulaRepository.theta(0., 0., TIME_TO_EXPIRY, inf, true, 0.05);
      double refC3 = BlackFormulaRepository.theta(0., inf, TIME_TO_EXPIRY, 0., true, 0.05);
      double refP1 = BlackFormulaRepository.theta(0., 0., TIME_TO_EXPIRY, 0., false, 0.05);
      double refP2 = BlackFormulaRepository.theta(0., 0., TIME_TO_EXPIRY, inf, false, 0.05);
      double refP3 = BlackFormulaRepository.theta(0., inf, TIME_TO_EXPIRY, 0., false, 0.05);
      double refC4 = BlackFormulaRepository.theta(inf, 0., TIME_TO_EXPIRY, 0., true, 0.05);
      double refP4 = BlackFormulaRepository.theta(inf, 0., TIME_TO_EXPIRY, 0., false, 0.05);
      double refC5 = BlackFormulaRepository.theta(FORWARD, FORWARD, TIME_TO_EXPIRY, 0., true, 0.05);
      double refP5 = BlackFormulaRepository.theta(FORWARD, FORWARD, TIME_TO_EXPIRY, 0., false, 0.05);
      double refC6 = BlackFormulaRepository.theta(inf, inf, TIME_TO_EXPIRY, inf, true, 0.05);
      double refP6 = BlackFormulaRepository.theta(inf, inf, TIME_TO_EXPIRY, inf, false, 0.05);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC6, resP6,
          resC5, resP5};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC6, refP6,
          refC5, refP5};

      for (int k = 0; k < 10; ++k) {  // The last two cases return reference values
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.theta(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, true, 1.e-12);
      double resC2 = BlackFormulaRepository.theta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, true, 1.e-12);
      double resC3 = BlackFormulaRepository.theta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, true, 1.e-12);
      double resP1 = BlackFormulaRepository.theta(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, false, 1.e-12);
      double resP2 = BlackFormulaRepository.theta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, false, 1.e-12);
      double resP3 = BlackFormulaRepository.theta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, false, 1.e-12);
      double resC4 = BlackFormulaRepository.theta(1.e12, 1.e12, TIME_TO_EXPIRY, vol, true, 1.e12);
      double resP4 = BlackFormulaRepository.theta(1.e12, 1.e12, TIME_TO_EXPIRY, vol, false, 1.e12);
      double resC5 = BlackFormulaRepository.theta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, true, 1.e12);
      double resP5 = BlackFormulaRepository.theta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, false, 1.e12);
      double resC6 = BlackFormulaRepository.theta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, true, 1.e12);
      double resP6 = BlackFormulaRepository.theta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, false, 1.e12);
      double resC7 = BlackFormulaRepository.theta(1.e-12, 2.e-12, TIME_TO_EXPIRY, vol, true, 1.e12);
      double resP7 = BlackFormulaRepository.theta(1.e-12, 0.5e-12, TIME_TO_EXPIRY, vol, false, 1.e12);
      double resC8 = BlackFormulaRepository.theta(1.e12, 1.e12, TIME_TO_EXPIRY, vol, true, 1.e-12);
      double resP8 = BlackFormulaRepository.theta(1.e12, 1.e12, TIME_TO_EXPIRY, vol, false, 1.e-12);

      double refC1 = BlackFormulaRepository.theta(0., 0., TIME_TO_EXPIRY, vol, true, 0.);
      double refC2 = BlackFormulaRepository.theta(0., inf, TIME_TO_EXPIRY, vol, true, 0.);
      double refC3 = BlackFormulaRepository.theta(inf, 0., TIME_TO_EXPIRY, vol, true, 0.);
      double refP1 = BlackFormulaRepository.theta(0., 0., TIME_TO_EXPIRY, vol, false, 0.);
      double refP2 = BlackFormulaRepository.theta(0., inf, TIME_TO_EXPIRY, vol, false, 0.);
      double refP3 = BlackFormulaRepository.theta(inf, 0., TIME_TO_EXPIRY, vol, false, 0.);
      double refC4 = BlackFormulaRepository.theta(inf, inf, TIME_TO_EXPIRY, vol, true, inf);
      double refP4 = BlackFormulaRepository.theta(inf, inf, TIME_TO_EXPIRY, vol, false, inf);
      double refC5 = BlackFormulaRepository.theta(inf, 0., TIME_TO_EXPIRY, vol, true, inf);
      double refP5 = BlackFormulaRepository.theta(inf, 0., TIME_TO_EXPIRY, vol, false, inf);
      double refC6 = BlackFormulaRepository.theta(0., inf, TIME_TO_EXPIRY, vol, true, inf);
      double refP6 = BlackFormulaRepository.theta(0., inf, TIME_TO_EXPIRY, vol, false, inf);
      double refC7 = BlackFormulaRepository.theta(0., 0., TIME_TO_EXPIRY, vol, true, inf);
      double refP7 = BlackFormulaRepository.theta(0., 0., TIME_TO_EXPIRY, vol, false, inf);
      double refC8 = BlackFormulaRepository.theta(inf, inf, TIME_TO_EXPIRY, vol, true, 0.);
      double refP8 = BlackFormulaRepository.theta(inf, inf, TIME_TO_EXPIRY, vol, false, 0.);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7, resC8, resP8};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7, refC8, refP8};

      for (int k = 0; k < 14; ++k) {
        if (k != 3 && k != 4 && k != 7 && k != 11) {
          if (k != 12 && k != 13) {  // ref values are returned
            if (refVec[k] > 1.e10) {
              assertThat(resVec[k] > 1.e9).isTrue();
            } else {
              if (refVec[k] < -1.e10) {
                assertThat(resVec[k] < -1.e9).isTrue();
              } else {
                if (refVec[k] == 0.) {
                  assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
                } else {
                  assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.theta(1.e-12, strike, 1.e-24, vol, true, 1.e-12);
        double resC2 = BlackFormulaRepository.theta(1.e-12, strike, 1.e24, vol, true, 1.e-12);
        double resC3 = BlackFormulaRepository.theta(1.e12, strike, 1.e-24, vol, true, 1.e-12);
        double resP1 = BlackFormulaRepository.theta(1.e-12, strike, 1.e-24, vol, false, 1.e-12);
        double resP2 = BlackFormulaRepository.theta(1.e-12, strike, 1.e24, vol, false, 1.e-12);
        double resP3 = BlackFormulaRepository.theta(1.e12, strike, 1.e-24, vol, false, 1.e-12);
        double resC4 = BlackFormulaRepository.theta(1.e12, strike, 1.e24, vol, true, 1.e12);
        double resP4 = BlackFormulaRepository.theta(1.e12, strike, 1.e24, vol, false, 1.e12);
        double resC5 = BlackFormulaRepository.theta(1.e-12, strike, 1.e24, vol, true, 1.e12);
        double resP5 = BlackFormulaRepository.theta(1.e-12, strike, 1.e24, vol, false, 1.e12);
        double resC6 = BlackFormulaRepository.theta(1.e12, strike, 1.e-24, vol, true, 1.e12);
        double resP6 = BlackFormulaRepository.theta(1.e12, strike, 1.e-24, vol, false, 1.e12);
        double resC7 = BlackFormulaRepository.theta(1.e12, strike, 1.e24, vol, true, 1.e-12);
        double resP7 = BlackFormulaRepository.theta(1.e12, strike, 1.e24, vol, false, 1.e-12);
        double resC8 = BlackFormulaRepository.theta(1.e-12, strike, 1.e-24, vol, true, 1.e12);
        double resP8 = BlackFormulaRepository.theta(1.e-12, strike, 1.e-24, vol, false, 1.e12);

        double refC1 = BlackFormulaRepository.theta(0., strike, 0., vol, true, 0.);
        double refC2 = BlackFormulaRepository.theta(0., strike, inf, vol, true, 0.);
        double refC3 = BlackFormulaRepository.theta(inf, strike, 0., vol, true, 0.);
        double refP1 = BlackFormulaRepository.theta(0., strike, 0., vol, false, 0.);
        double refP2 = BlackFormulaRepository.theta(0., strike, inf, vol, false, 0.);
        double refP3 = BlackFormulaRepository.theta(inf, strike, 0., vol, false, 0.);
        double refC4 = BlackFormulaRepository.theta(inf, strike, inf, vol, true, inf);
        double refP4 = BlackFormulaRepository.theta(inf, strike, inf, vol, false, inf);
        double refC5 = BlackFormulaRepository.theta(0., strike, inf, vol, true, inf);
        double refP5 = BlackFormulaRepository.theta(0., strike, inf, vol, false, inf);
        double refC6 = BlackFormulaRepository.theta(inf, strike, 0., vol, true, inf);
        double refP6 = BlackFormulaRepository.theta(inf, strike, 0., vol, false, inf);
        double refC7 = BlackFormulaRepository.theta(inf, strike, inf, vol, true, 0.);
        double refP7 = BlackFormulaRepository.theta(inf, strike, inf, vol, false, 0.);
        double refC8 = BlackFormulaRepository.theta(0., strike, 0., vol, true, inf);
        double refP8 = BlackFormulaRepository.theta(0., strike, 0., vol, false, inf);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
            resC6, resP6, resC7, resP7, resC8, resP8};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
            refC6, refP6, refC7, refP7, refC8, refP8};

        for (int k = 0; k < 16; ++k) {
          if (k != 4 && k != 8 && k != 12) {
            if (refVec[k] > 1.e10) {
              assertThat(resVec[k] > 1.e10).isTrue();
            } else {
              if (refVec[k] < -1.e10) {
                assertThat(resVec[k] < -1.e10).isTrue();
              } else {
                if (refVec[k] == 0.) {
                  assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
                } else {
                  assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-9));
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, true, 1.e-12);
      double resC2 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, true, 1.e-12);
      double resC3 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, true, 1.e-12);
      double resP1 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, false, 1.e-12);
      double resP2 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, false, 1.e-12);
      double resP3 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, false, 1.e-12);
      double resC4 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, 1.e12, true, 1.e12);
      double resP4 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, 1.e12, false, 1.e12);
      double resC5 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, true, 1.e12);
      double resP5 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, false, 1.e12);
      double resC6 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, true, 1.e12);
      double resP6 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, false, 1.e12);
      double resC7 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, true, 1.e12);
      double resP7 = BlackFormulaRepository.theta(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, false, 1.e12);
      double resC8 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, 1.e12, true, 1.e-12);
      double resP8 = BlackFormulaRepository.theta(1.e12, strike, TIME_TO_EXPIRY, 1.e12, false, 1.e-12);

      double refC1 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, 0., true, 0.);
      double refC2 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, inf, true, 0.);
      double refC3 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, 0., true, 0.);
      double refP1 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, 0., false, 0.);
      double refP2 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, inf, false, 0.);
      double refP3 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, 0., false, 0.);
      double refC4 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, inf, true, inf);
      double refP4 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, inf, false, inf);
      double refC5 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, 0., true, inf);
      double refP5 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, 0., false, inf);
      double refC6 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, 0., true, inf);
      double refP6 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, 0., false, inf);
      double refC7 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, inf, true, inf);
      double refP7 = BlackFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, inf, false, inf);
      double refC8 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, inf, true, 0.);
      double refP8 = BlackFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, inf, false, 0.);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7, resC8, resP8};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7, refC8, refP8};

      for (int k = 0; k < 16; ++k) {
        if (k != 4 && k != 9 && k != 12 && k != 14) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-9));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.theta(forward, 1.e-12, 1.e-24, vol, true, 1.e-12);
        double resC2 = BlackFormulaRepository.theta(forward, 1.e-12, 1.e24, vol, true, 1.e-12);
        double resC3 = BlackFormulaRepository.theta(forward, 1.e12, 1.e-24, vol, true, 1.e-12);
        double resP1 = BlackFormulaRepository.theta(forward, 1.e-12, 1.e-24, vol, false, 1.e-12);
        double resP2 = BlackFormulaRepository.theta(forward, 1.e-12, 1.e24, vol, false, 1.e-12);
        double resP3 = BlackFormulaRepository.theta(forward, 1.e12, 1.e-24, vol, false, 1.e-12);
        double resC4 = BlackFormulaRepository.theta(forward, 1.e12, 1.e24, vol, true, 1.e12);
        double resP4 = BlackFormulaRepository.theta(forward, 1.e12, 1.e24, vol, false, 1.e12);
        double resC5 = BlackFormulaRepository.theta(forward, 1.e-12, 1.e24, vol, true, 1.e12);
        double resP5 = BlackFormulaRepository.theta(forward, 1.e-12, 1.e24, vol, false, 1.e12);
        double resC6 = BlackFormulaRepository.theta(forward, 1.e12, 1.e-12, vol, true, 1.e12);
        double resP6 = BlackFormulaRepository.theta(forward, 1.e12, 1.e-12, vol, false, 1.e12);
        double resC7 = BlackFormulaRepository.theta(forward, 1.e-12, 1.e-12, vol, true, 1.e12);
        double resP7 = BlackFormulaRepository.theta(forward, 1.e-12, 1.e-12, vol, false, 1.e12);
        double resC8 = BlackFormulaRepository.theta(forward, 1.e12, 1.e24, vol, true, 1.e-12);
        double resP8 = BlackFormulaRepository.theta(forward, 1.e12, 1.e24, vol, false, 1.e-12);

        double refC1 = BlackFormulaRepository.theta(forward, 0., 0., vol, true, 0.);
        double refC2 = BlackFormulaRepository.theta(forward, 0., inf, vol, true, 0.);
        double refC3 = BlackFormulaRepository.theta(forward, inf, 0., vol, true, 0.);
        double refP1 = BlackFormulaRepository.theta(forward, 0., 0., vol, false, 0.);
        double refP2 = BlackFormulaRepository.theta(forward, 0., inf, vol, false, 0.);
        double refP3 = BlackFormulaRepository.theta(forward, inf, 0., vol, false, 0.);
        double refC4 = BlackFormulaRepository.theta(forward, inf, inf, vol, true, inf);
        double refP4 = BlackFormulaRepository.theta(forward, inf, inf, vol, false, inf);
        double refC5 = BlackFormulaRepository.theta(forward, 0., inf, vol, true, inf);
        double refP5 = BlackFormulaRepository.theta(forward, 0., inf, vol, false, inf);
        double refC6 = BlackFormulaRepository.theta(forward, inf, 0., vol, true, inf);
        double refP6 = BlackFormulaRepository.theta(forward, inf, 0., vol, false, inf);
        double refC7 = BlackFormulaRepository.theta(forward, 0., inf, vol, true, inf);
        double refP7 = BlackFormulaRepository.theta(forward, 0., inf, vol, false, inf);
        double refC8 = BlackFormulaRepository.theta(forward, 0., 0., vol, true, inf);
        double refP8 = BlackFormulaRepository.theta(forward, 0., 0., vol, false, inf);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
            resC6, resP6, resC7, resP7, resC8, resP8};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
            refC6, refP6, refC7, refP7, refC8, refP8};

        for (int k = 0; k < 14; ++k) {  // some of ref values skipped
          if (k != 5 && k != 9) {
            if (refVec[k] > 1.e10) {
              assertThat(resVec[k] > 1.e10).isTrue();
            } else {
              if (refVec[k] < -1.e10) {
                assertThat(resVec[k] < -1.e10).isTrue();
              } else {
                if (refVec[k] == 0.) {
                  assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
                } else {
                  assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-9));
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.theta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true, 1.e-12);
      double resC2 = BlackFormulaRepository.theta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, true, 1.e-12);
      double resC3 = BlackFormulaRepository.theta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, true, 1.e-12);
      double resP1 = BlackFormulaRepository.theta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false, 1.e-12);
      double resP2 = BlackFormulaRepository.theta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, false, 1.e-12);
      double resP3 = BlackFormulaRepository.theta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, false, 1.e-12);
      double resC4 = BlackFormulaRepository.theta(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, true, 1.e12);
      double resP4 = BlackFormulaRepository.theta(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, false, 1.e12);
      double resC5 = BlackFormulaRepository.theta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, true, 1.e12);
      double resP5 = BlackFormulaRepository.theta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, false, 1.e12);
      double resC6 = BlackFormulaRepository.theta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, true, 1.e12);
      double resP6 = BlackFormulaRepository.theta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, false, 1.e12);
      double resC7 = BlackFormulaRepository.theta(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, true, 1.e-12);
      double resP7 = BlackFormulaRepository.theta(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, false, 1.e-12);
      double resC8 = BlackFormulaRepository.theta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true, 1.e12);
      double resP8 = BlackFormulaRepository.theta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false, 1.e12);

      double refC1 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, 0., true, 0.);
      double refC2 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, inf, true, 0.);
      double refC3 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, 0., true, 0.);
      double refP1 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, 0., false, 0.);
      double refP2 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, inf, false, 0.);
      double refP3 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, 0., false, 0.);
      double refC4 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, inf, true, inf);
      double refP4 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, inf, false, inf);
      double refC5 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, inf, true, inf);
      double refP5 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, inf, false, inf);
      double refC6 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, 0., true, inf);
      double refP6 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, 0., false, inf);
      double refC7 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, inf, true, 0.);
      double refP7 = BlackFormulaRepository.theta(forward, inf, TIME_TO_EXPIRY, inf, false, 0.);
      double refC8 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, 0., true, inf);
      double refP8 = BlackFormulaRepository.theta(forward, 0., TIME_TO_EXPIRY, 0., false, inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7, resC8, resP8};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7, refC8, refP8};

      for (int k = 0; k < 16; ++k) {
        if (k != 5 && k != 9 && k != 11 && k != 13) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-9));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.theta(strike, strike, inf, 0., true, 1.);
      double resP1 = BlackFormulaRepository.theta(strike, strike, inf, 0., false, 1.);
      double resC2 = BlackFormulaRepository.theta(strike, strike, inf, 0., true, 0.);
      double resP2 = BlackFormulaRepository.theta(strike, strike, inf, 0., false, 0.);
      double refC1 = strike * (NORMAL.getCDF(0.5));
      double refP1 = -strike * (NORMAL.getCDF(-0.5));

      double[] resVec = new double[] {resC1, resP1, resC2, resP2};
      double[] refVec = new double[] {refC1, refP1, 0., 0.};
      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10)));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, VOLS[0], true, -inf);
      double resP1 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, VOLS[1], false, -inf);
      double resC2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, VOLS[2], true, -inf);
      double resP2 = BlackFormulaRepository.theta(FORWARD, strike, TIME_TO_EXPIRY, VOLS[3], false, -inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2};
      double[] refVec = new double[] {0., 0., 0., 0.};
      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10)));
            }
          }
        }
      }
    }
  }

  @Test
  public void negativeVolErrorThetaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.theta(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, true, 0.1));
  }

  @Test
  public void negativeFwdErrorThetaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.theta(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], true, 0.1));
  }

  @Test
  public void negativeStrikeErrorThetaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.theta(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], true, 0.1));
  }

  @Test
  public void negativeTimeErrorThetaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.theta(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], true, 0.1));
  }

  /*
   * driftlessTheta
   */
  /**
   * large/small input
   */
  @Test
  public void exDriftlessThetaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.driftlessTheta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.driftlessTheta(0., strike, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.driftlessTheta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.driftlessTheta(inf, strike, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-11).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-11));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.driftlessTheta(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.driftlessTheta(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.driftlessTheta(forward, 0., TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.driftlessTheta(forward, inf, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.driftlessTheta(FORWARD, strike, 1e-24, vol);
        double resC2 = BlackFormulaRepository.driftlessTheta(FORWARD, strike, 1e24, vol);
        double refC1 = BlackFormulaRepository.driftlessTheta(FORWARD, strike, 0., vol);
        double refC2 = BlackFormulaRepository.driftlessTheta(FORWARD, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.driftlessTheta(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12);
      double refC1 = BlackFormulaRepository.driftlessTheta(FORWARD, strike, TIME_TO_EXPIRY, 0.);
      double resC2 = BlackFormulaRepository.driftlessTheta(FORWARD, strike, TIME_TO_EXPIRY, 1.e12);
      double refC2 = BlackFormulaRepository.driftlessTheta(FORWARD, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2};
      double[] refVec = new double[] {refC1, refC2};

      for (int k = 0; k < 2; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resC2 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e12, TIME_TO_EXPIRY, vol);
      double resC3 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resP3 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e12, TIME_TO_EXPIRY, vol);

      double refC1 = BlackFormulaRepository.driftlessTheta(0., 0., TIME_TO_EXPIRY, vol);
      double refC2 = BlackFormulaRepository.driftlessTheta(0., inf, TIME_TO_EXPIRY, vol);
      double refC3 = BlackFormulaRepository.driftlessTheta(inf, 0., TIME_TO_EXPIRY, vol);
      double refP3 = BlackFormulaRepository.driftlessTheta(inf, inf, TIME_TO_EXPIRY, vol);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e12) {
          assertThat(resVec[k] > 1.e9).isTrue();
        } else {
          if (refVec[k] < -1.e12) {
            assertThat(resVec[k] < -1.e9).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.driftlessTheta(1.e-12, strike, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.driftlessTheta(1.e-12, strike, 1.e24, vol);
        double resC3 = BlackFormulaRepository.driftlessTheta(1.e12, strike, 1.e-24, vol);
        double resP3 = BlackFormulaRepository.driftlessTheta(1.e12, strike, 1.e24, vol);

        double refC1 = BlackFormulaRepository.driftlessTheta(0., strike, 0., vol);
        double refC2 = BlackFormulaRepository.driftlessTheta(0., strike, inf, vol);
        double refC3 = BlackFormulaRepository.driftlessTheta(inf, strike, 0., vol);
        double refP3 = BlackFormulaRepository.driftlessTheta(inf, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2, resC3, resP3};
        double[] refVec = new double[] {refC1, refC2, refC3, refP3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.driftlessTheta(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.driftlessTheta(1.e-12, strike, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.driftlessTheta(1.e12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resP3 = BlackFormulaRepository.driftlessTheta(1.e12, strike, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.driftlessTheta(0., strike, TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.driftlessTheta(0., strike, TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.driftlessTheta(inf, strike, TIME_TO_EXPIRY, 0.);
      double refP3 = BlackFormulaRepository.driftlessTheta(inf, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.driftlessTheta(forward, 1.e-12, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.driftlessTheta(forward, 1.e-12, 1.e24, vol);
        double resC3 = BlackFormulaRepository.driftlessTheta(forward, 1.e12, 1.e-24, vol);
        double resP3 = BlackFormulaRepository.driftlessTheta(forward, 1.e12, 1.e24, vol);

        double refC1 = BlackFormulaRepository.driftlessTheta(forward, 0., 0., vol);
        double refC2 = BlackFormulaRepository.driftlessTheta(forward, 0., inf, vol);
        double refC3 = BlackFormulaRepository.driftlessTheta(forward, inf, 0., vol);
        double refP3 = BlackFormulaRepository.driftlessTheta(forward, inf, inf, vol);

        double[] resVec = new double[] {resC1, resC2, resC3, resP3};
        double[] refVec = new double[] {refC1, refC2, refC3, refP3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.driftlessTheta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.driftlessTheta(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.driftlessTheta(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP3 = BlackFormulaRepository.driftlessTheta(forward, 1.e12, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.driftlessTheta(forward, 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.driftlessTheta(forward, 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.driftlessTheta(forward, inf, TIME_TO_EXPIRY, 0.);
      double refP3 = BlackFormulaRepository.driftlessTheta(forward, inf, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.driftlessTheta(1.e-14, 1.e-14, 1.e-11, vol);
      double resC2 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e-12, 1.e24, vol);
      double resC3 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e12, 1.e-24, vol);
      double resP1 = BlackFormulaRepository.driftlessTheta(1.e-14, 1.e-14, 1.e-11, vol);
      double resP2 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e-12, 1.e24, vol);
      double resP3 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e12, 1.e-24, vol);
      double resC4 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e-12, 1.e-24, vol);
      double resP4 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e-12, 1.e-24, vol);
      double resC5 = BlackFormulaRepository.driftlessTheta(FORWARD, FORWARD * (1. + 1.e-14), 1.e-24, vol);
      double resP5 = BlackFormulaRepository.driftlessTheta(FORWARD, FORWARD * (1. + 1.e-14), 1.e-24, vol);
      double resC6 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e12, 1.e24, vol);
      double resP6 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e12, 1.e-24, vol);
      double resC7 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e-12, 1.e24, vol);
      double resP7 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e12, 1.e24, vol);
      double resP8 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e-12, 1.e-24, vol);

      double refC1 = BlackFormulaRepository.driftlessTheta(0., 0., 0., vol);
      double refC2 = BlackFormulaRepository.driftlessTheta(0., 0., inf, vol);
      double refC3 = BlackFormulaRepository.driftlessTheta(0., inf, 0., vol);
      double refP1 = BlackFormulaRepository.driftlessTheta(0., 0., 0., vol);
      double refP2 = BlackFormulaRepository.driftlessTheta(0., 0., inf, vol);
      double refP3 = BlackFormulaRepository.driftlessTheta(0., inf, 0., vol);
      double refC4 = BlackFormulaRepository.driftlessTheta(inf, 0., 0., vol);
      double refP4 = BlackFormulaRepository.driftlessTheta(inf, 0., 0., vol);
      double refC5 = BlackFormulaRepository.driftlessTheta(FORWARD, FORWARD, 0., vol);
      double refP5 = BlackFormulaRepository.driftlessTheta(FORWARD, FORWARD, 0., vol);
      double refC6 = BlackFormulaRepository.driftlessTheta(inf, inf, inf, vol);
      double refP6 = BlackFormulaRepository.driftlessTheta(inf, inf, 0., vol);
      double refC7 = BlackFormulaRepository.driftlessTheta(inf, 0., inf, vol);
      double refP7 = BlackFormulaRepository.driftlessTheta(0., inf, inf, vol);
      double refP8 = BlackFormulaRepository.driftlessTheta(0., 0., 0., vol);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7, resP8};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7, refP8};

      for (int k = 0; k < 15; ++k) {

        if ((refVec[k] != -0.5 * vol * NORMAL.getPDF(0.)) && (refVec[k] != -0.5 * FORWARD * NORMAL.getPDF(0.)) &&
            (refVec[k] != Double.NEGATIVE_INFINITY)) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP1 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resP2 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resP3 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resC4 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resP4 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC5 = BlackFormulaRepository.driftlessTheta(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY,
          1.e-12);
      double resP5 = BlackFormulaRepository.driftlessTheta(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY,
          1.e-12);
      double resC6 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e12);
      double resP6 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC7 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP7 = BlackFormulaRepository.driftlessTheta(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e12);
      double resP8 = BlackFormulaRepository.driftlessTheta(1.e12, 1.e12, 1.e-24, 1.e-12);

      double refC1 = BlackFormulaRepository.driftlessTheta(0., 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.driftlessTheta(0., 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.driftlessTheta(0., inf, TIME_TO_EXPIRY, 0.);
      double refP1 = BlackFormulaRepository.driftlessTheta(0., 0., TIME_TO_EXPIRY, 0.);
      double refP2 = BlackFormulaRepository.driftlessTheta(0., 0., TIME_TO_EXPIRY, inf);
      double refP3 = BlackFormulaRepository.driftlessTheta(0., inf, TIME_TO_EXPIRY, 0.);
      double refC4 = BlackFormulaRepository.driftlessTheta(inf, 0., TIME_TO_EXPIRY, 0.);
      double refP4 = BlackFormulaRepository.driftlessTheta(inf, 0., TIME_TO_EXPIRY, 0.);
      double refC5 = BlackFormulaRepository.driftlessTheta(FORWARD, FORWARD, TIME_TO_EXPIRY, 1.e-12);
      double refP5 = BlackFormulaRepository.driftlessTheta(FORWARD, FORWARD, TIME_TO_EXPIRY, 1.e-12);
      double refC6 = BlackFormulaRepository.driftlessTheta(inf, inf, TIME_TO_EXPIRY, inf);
      double refP6 = BlackFormulaRepository.driftlessTheta(inf, 0., TIME_TO_EXPIRY, inf);
      double refC7 = BlackFormulaRepository.driftlessTheta(inf, inf, TIME_TO_EXPIRY, 0.);
      double refP7 = BlackFormulaRepository.driftlessTheta(0., inf, TIME_TO_EXPIRY, inf);
      double refP8 = BlackFormulaRepository.driftlessTheta(inf, inf, 0., 0.);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7, resP8};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7, refP8};

      for (int k = 0; k < 15; ++k) {
        if (k != 12) {  // ref value
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (Math.abs(refVec[k]) < 1.e-10) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

  }

  @Test
  public void negativeVolErrorDriftlessThetaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.driftlessTheta(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5));
  }

  @Test
  public void negativeFwdErrorDriftlessThetaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.driftlessTheta(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeStrikeErrorDriftlessThetaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.driftlessTheta(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeTimeErrorDriftlessThetaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.driftlessTheta(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1]));
  }

  /*
   * thetaMod tests
   */
  /**
   * large/small input
   */
  @Test
  public void exthetaModTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.thetaMod(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, true, 0.05);
        double refC1 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, vol, true, 0.05);
        double resC2 = BlackFormulaRepository.thetaMod(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, true, 0.05);
        double refC2 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, vol, true, 0.05);
        double resP1 = BlackFormulaRepository.thetaMod(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, false, 0.05);
        double refP1 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, vol, false, 0.05);
        double resP2 = BlackFormulaRepository.thetaMod(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, false, 0.05);
        double refP2 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, vol, false, 0.05);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository
            .thetaMod(forward, 1.e-14 * forward, TIME_TO_EXPIRY, vol, true, 0.05);
        double resC2 = BlackFormulaRepository.thetaMod(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol, true, 0.05);
        double resP1 = BlackFormulaRepository.thetaMod(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol, false,
            0.05);
        double resP2 = BlackFormulaRepository
            .thetaMod(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol, false, 0.05);
        double refC1 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, vol, true, 0.05);
        double refC2 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, vol, true, 0.05);
        double refP1 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, vol, false, 0.05);
        double refP2 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, vol, false, 0.05);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.thetaMod(FORWARD, strike, 1e-12, vol, true, 0.05);
        double resC2 = BlackFormulaRepository.thetaMod(FORWARD, strike, 1e12, vol, true, 0.05);
        double resP1 = BlackFormulaRepository.thetaMod(FORWARD, strike, 1e-12, vol, false, 0.05);
        double resP2 = BlackFormulaRepository.thetaMod(FORWARD, strike, 1e12, vol, false, 0.05);
        double refC1 = BlackFormulaRepository.thetaMod(FORWARD, strike, 0., vol, true, 0.05);
        double refC2 = BlackFormulaRepository.thetaMod(FORWARD, strike, inf, vol, true, 0.05);
        double refP1 = BlackFormulaRepository.thetaMod(FORWARD, strike, 0., vol, false, 0.05);
        double refP2 = BlackFormulaRepository.thetaMod(FORWARD, strike, inf, vol, false, 0.05);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 1.e-24, true, 0.05);
      double refC1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 0., true, 0.05);
      double resC2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 1.e24, true, 0.05);
      double refC2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, inf, true, 0.05);
      double resP1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 1.e-24, false, 0.05);
      double refP1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 0., false, 0.05);
      double resP2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 1.e24, false, 0.05);
      double refP2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, inf, false, 0.05);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, vol, true, 1.e-12);
        double resP1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, vol, false, 1.e-12);
        double refC1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, vol, true, 0.);
        double refP1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, vol, false, 0.);
        double resC2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, vol, true, 1.e12);
        double resP2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, vol, false, 1.e12);
        double refC2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, vol, true, inf);
        double refP2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, vol, false, inf);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e8).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e9).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-9, 1.e-9)));
              }
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.thetaMod(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, true, 0.05);
      double resC2 = BlackFormulaRepository.thetaMod(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, true, 0.05);
      double resC3 = BlackFormulaRepository.thetaMod(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, true, 0.05);
      double resP1 = BlackFormulaRepository.thetaMod(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, false, 0.05);
      double resP2 = BlackFormulaRepository.thetaMod(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, false, 0.05);
      double resP3 = BlackFormulaRepository.thetaMod(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, false, 0.05);
      double resC4 = BlackFormulaRepository.thetaMod(1.e12, 1.e12, TIME_TO_EXPIRY, vol, true, 0.05);
      double resP4 = BlackFormulaRepository.thetaMod(1.e12, 1.e12, TIME_TO_EXPIRY, vol, false, 0.05);
      double resC5 = BlackFormulaRepository.thetaMod(1.e10, 1.e11, TIME_TO_EXPIRY, vol, true, 0.05);
      double resP5 = BlackFormulaRepository.thetaMod(1.e11, 1.e10, TIME_TO_EXPIRY, vol, false, 0.05);

      double refC1 = BlackFormulaRepository.thetaMod(0., 0., TIME_TO_EXPIRY, vol, true, 0.05);
      double refC2 = BlackFormulaRepository.thetaMod(0., inf, TIME_TO_EXPIRY, vol, true, 0.05);
      double refC3 = BlackFormulaRepository.thetaMod(inf, 0., TIME_TO_EXPIRY, vol, true, 0.05);
      double refP1 = BlackFormulaRepository.thetaMod(0., 0., TIME_TO_EXPIRY, vol, false, 0.05);
      double refP2 = BlackFormulaRepository.thetaMod(0., inf, TIME_TO_EXPIRY, vol, false, 0.05);
      double refP3 = BlackFormulaRepository.thetaMod(inf, 0., TIME_TO_EXPIRY, vol, false, 0.05);
      double refC4 = BlackFormulaRepository.thetaMod(inf, inf, TIME_TO_EXPIRY, vol, true, 0.05);
      double refP4 = BlackFormulaRepository.thetaMod(inf, inf, TIME_TO_EXPIRY, vol, false, 0.05);
      double refC5 = BlackFormulaRepository.thetaMod(1.e15, 1.e16, TIME_TO_EXPIRY, vol, true, 0.05);
      double refP5 = BlackFormulaRepository.thetaMod(1.e16, 1.e15, TIME_TO_EXPIRY, vol, false, 0.05);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5};

      for (int k = 0; k < 6; ++k) {  // ref values
        if (k != 6 && k != 7) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e8).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e9).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.thetaMod(1.e-12, strike, 1.e-24, vol, true, 0.05);
        double resC2 = BlackFormulaRepository.thetaMod(1.e-12, strike, 1.e24, vol, true, 0.05);
        double resC3 = BlackFormulaRepository.thetaMod(1.e12, strike, 1.e-24, vol, true, 0.05);
        double resP1 = BlackFormulaRepository.thetaMod(1.e-12, strike, 1.e-24, vol, false, 0.05);
        double resP2 = BlackFormulaRepository.thetaMod(1.e-12, strike, 1.e24, vol, false, 0.05);
        double resP3 = BlackFormulaRepository.thetaMod(1.e12, strike, 1.e-24, vol, false, 0.05);
        double resC4 = BlackFormulaRepository.thetaMod(1.e12, strike, 1.e24, vol, true, 0.05);
        double resP4 = BlackFormulaRepository.thetaMod(1.e12, strike, 1.e24, vol, false, 0.05);

        double refC1 = BlackFormulaRepository.thetaMod(0., strike, 0., vol, true, 0.05);
        double refC2 = BlackFormulaRepository.thetaMod(0., strike, inf, vol, true, 0.05);
        double refC3 = BlackFormulaRepository.thetaMod(inf, strike, 0., vol, true, 0.05);
        double refP1 = BlackFormulaRepository.thetaMod(0., strike, 0., vol, false, 0.05);
        double refP2 = BlackFormulaRepository.thetaMod(0., strike, inf, vol, false, 0.05);
        double refP3 = BlackFormulaRepository.thetaMod(inf, strike, 0., vol, false, 0.05);
        double refC4 = BlackFormulaRepository.thetaMod(inf, strike, inf, vol, true, 0.05);
        double refP4 = BlackFormulaRepository.thetaMod(inf, strike, inf, vol, false, 0.05);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, 1.e-24, true, 0.05);
      double resC2 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, 1.e24, true, 0.05);
      double resC3 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, 1.e-24, true, 0.05);
      double resP1 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, 1.e-24, false, 0.05);
      double resP2 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, 1.e24, false, 0.05);
      double resP3 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, 1.e-24, false, 0.05);
      double resC4 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, 1.e24, true, 0.05);
      double resP4 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, 1.e24, false, 0.05);

      double refC1 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, 0., true, 0.05);
      double refC2 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, inf, true, 0.05);
      double refC3 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, 0., true, 0.05);
      double refP1 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, 0., false, 0.05);
      double refP2 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, inf, false, 0.05);
      double refP3 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, 0., false, 0.05);
      double refC4 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, inf, true, 0.05);
      double refP4 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, inf, false, 0.05);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

      for (int k = 0; k < 8; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.thetaMod(forward, 1.e-12, 1.e-24, vol, true, 0.05);
        double resC2 = BlackFormulaRepository.thetaMod(forward, 1.e-12, 1.e24, vol, true, 0.05);
        double resC3 = BlackFormulaRepository.thetaMod(forward, 1.e12, 1.e-24, vol, true, 0.05);
        double resP1 = BlackFormulaRepository.thetaMod(forward, 1.e-12, 1.e-24, vol, false, 0.05);
        double resP2 = BlackFormulaRepository.thetaMod(forward, 1.e-12, 1.e24, vol, false, 0.05);
        double resP3 = BlackFormulaRepository.thetaMod(forward, 1.e12, 1.e-24, vol, false, 0.05);
        double resC4 = BlackFormulaRepository.thetaMod(forward, 1.e12, 1.e24, vol, true, 0.05);
        double resP4 = BlackFormulaRepository.thetaMod(forward, 1.e12, 1.e24, vol, false, 0.05);

        double refC1 = BlackFormulaRepository.thetaMod(forward, 0., 0., vol, true, 0.05);
        double refC2 = BlackFormulaRepository.thetaMod(forward, 0., inf, vol, true, 0.05);
        double refC3 = BlackFormulaRepository.thetaMod(forward, inf, 0., vol, true, 0.05);
        double refP1 = BlackFormulaRepository.thetaMod(forward, 0., 0., vol, false, 0.05);
        double refP2 = BlackFormulaRepository.thetaMod(forward, 0., inf, vol, false, 0.05);
        double refP3 = BlackFormulaRepository.thetaMod(forward, inf, 0., vol, false, 0.05);
        double refC4 = BlackFormulaRepository.thetaMod(forward, inf, inf, vol, true, 0.05);
        double refP4 = BlackFormulaRepository.thetaMod(forward, inf, inf, vol, false, 0.05);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.thetaMod(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true, 0.05);
      double resC2 = BlackFormulaRepository.thetaMod(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, true, 0.05);
      double resC3 = BlackFormulaRepository.thetaMod(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, true, 0.05);
      double resP1 = BlackFormulaRepository.thetaMod(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false, 0.05);
      double resP2 = BlackFormulaRepository.thetaMod(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, false, 0.05);
      double resP3 = BlackFormulaRepository.thetaMod(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, false, 0.05);
      double resC4 = BlackFormulaRepository.thetaMod(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, true, 0.05);
      double resP4 = BlackFormulaRepository.thetaMod(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, false, 0.05);

      double refC1 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, 0., true, 0.05);
      double refC2 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, inf, true, 0.05);
      double refC3 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, 0., true, 0.05);
      double refP1 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, 0., false, 0.05);
      double refP2 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, inf, false, 0.05);
      double refP3 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, 0., false, 0.05);
      double refC4 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, inf, true, 0.05);
      double refP4 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, inf, false, 0.05);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

      for (int k = 0; k < 8; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.thetaMod(FORWARD, strike, 1.e-12, vol, true, 1.e-12);
        double resC2 = BlackFormulaRepository.thetaMod(FORWARD, strike, 1.e12, vol, true, 1.e-12);
        double resP1 = BlackFormulaRepository.thetaMod(FORWARD, strike, 1.e-12, vol, false, 1.e-12);
        double resP2 = BlackFormulaRepository.thetaMod(FORWARD, strike, 1.e12, vol, false, 1.e-12);
        double resC3 = BlackFormulaRepository.thetaMod(FORWARD, strike, 1.e12, vol, true, 1.e12);
        double resP3 = BlackFormulaRepository.thetaMod(FORWARD, strike, 1.e12, vol, false, 1.e12);
        double resC4 = BlackFormulaRepository.thetaMod(FORWARD, strike, 1.e-12, vol, true, 1.e12);
        double resP4 = BlackFormulaRepository.thetaMod(FORWARD, strike, 1.e-12, vol, false, 1.e12);

        double refC1 = BlackFormulaRepository.thetaMod(FORWARD, strike, 0., vol, true, 0.);
        double refC2 = BlackFormulaRepository.thetaMod(FORWARD, strike, inf, vol, true, 0.);
        double refP1 = BlackFormulaRepository.thetaMod(FORWARD, strike, 0., vol, false, 0.);
        double refP2 = BlackFormulaRepository.thetaMod(FORWARD, strike, inf, vol, false, 0.);
        double refC3 = BlackFormulaRepository.thetaMod(FORWARD, strike, inf, vol, true, inf);
        double refP3 = BlackFormulaRepository.thetaMod(FORWARD, strike, inf, vol, false, inf);
        double refC4 = BlackFormulaRepository.thetaMod(FORWARD, strike, 0., vol, true, inf);
        double refP4 = BlackFormulaRepository.thetaMod(FORWARD, strike, 0., vol, false, inf);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 6; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.thetaMod(strike, 1.e-12, TIME_TO_EXPIRY, vol, true, 1.e-12);
        double resC2 = BlackFormulaRepository.thetaMod(strike, 1.e12, TIME_TO_EXPIRY, vol, true, 1.e-12);
        double resP1 = BlackFormulaRepository.thetaMod(strike, 1.e-12, TIME_TO_EXPIRY, vol, false, 1.e-12);
        double resP2 = BlackFormulaRepository.thetaMod(strike, 1.e12, TIME_TO_EXPIRY, vol, false, 1.e-12);
        double resC3 = BlackFormulaRepository.thetaMod(strike, 1.e12, TIME_TO_EXPIRY, vol, true, 1.e12);
        double resP3 = BlackFormulaRepository.thetaMod(strike, 1.e12, TIME_TO_EXPIRY, vol, false, 1.e12);

        double refC1 = BlackFormulaRepository.thetaMod(strike, 0., TIME_TO_EXPIRY, vol, true, 0.);
        double refC2 = BlackFormulaRepository.thetaMod(strike, inf, TIME_TO_EXPIRY, vol, true, 0.);
        double refP1 = BlackFormulaRepository.thetaMod(strike, 0., TIME_TO_EXPIRY, vol, false, 0.);
        double refP2 = BlackFormulaRepository.thetaMod(strike, inf, TIME_TO_EXPIRY, vol, false, 0.);
        double refC3 = BlackFormulaRepository.thetaMod(strike, inf, TIME_TO_EXPIRY, vol, true, inf);
        double refP3 = BlackFormulaRepository.thetaMod(strike, inf, TIME_TO_EXPIRY, vol, false, inf);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

        for (int k = 0; k < 6; ++k) {
          if (k != 3) {
            if (refVec[k] > 1.e10) {
              assertThat(resVec[k] > 1.e10).isTrue();
            } else {
              if (refVec[k] < -1.e10) {
                assertThat(resVec[k] < -1.e10).isTrue();
              } else {
                if (refVec[k] == 0.) {
                  assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
                } else {
                  assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-9, 1.e-9)));
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, true, 1.e-12);
      double resC2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, true, 1.e-12);
      double resP1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, false, 1.e-12);
      double resP2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, false, 1.e-12);
      double resC3 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, true, 1.e12);
      double resP3 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 1.e12, false, 1.e12);
      double resC4 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, true, 1.e12);
      double resP4 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12, false, 1.e12);

      double refC1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 0., true, 0.);
      double refC2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, inf, true, 0.);
      double refP1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 0., false, 0.);
      double refP2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, inf, false, 0.);
      double refC3 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, inf, true, inf);
      double refP3 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, inf, false, inf);
      double refC4 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 0., true, inf);
      double refP4 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, 0., false, inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

      for (int k = 0; k < 6; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e9).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, vol, true, 1.e-12);
        double resC2 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, vol, true, 1.e-12);
        double resP1 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, vol, false, 1.e-12);
        double resP2 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, vol, false, 1.e-12);
        double resC3 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, vol, true, 1.e12);
        double resP3 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, vol, false, 1.e12);
        double resC4 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, vol, true, 1.e12);
        double resP4 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, vol, false, 1.e12);

        double refC1 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, vol, true, 0.);
        double refC2 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, vol, true, 0.);
        double refP1 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, vol, false, 0.);
        double refP2 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, vol, false, 0.);
        double refC3 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, vol, true, inf);
        double refP3 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, vol, false, inf);
        double refC4 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, vol, true, inf);
        double refP4 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, vol, false, inf);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4};

        for (int k = 0; k < 8; ++k) {
          if (k != 2 && k != 7) {
            if (refVec[k] > 1.e10) {
              assertThat(resVec[k] > 1.e10).isTrue();
            } else {
              if (refVec[k] < -1.e10) {
                assertThat(resVec[k] < -1.e10).isTrue();
              } else {
                if (refVec[k] == 0.) {
                  assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
                } else {
                  assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-9, 1.e-9)));
                }
              }
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.thetaMod(1.e-12, 1.e-12, 1.e-24, vol, true, 0.05);
      double resC2 = BlackFormulaRepository.thetaMod(1.e-12, 1.e-12, 1.e24, vol, true, 0.05);
      double resC3 = BlackFormulaRepository.thetaMod(1.e-12, 1.e12, 1.e-24, vol, true, 0.05);
      double resP1 = BlackFormulaRepository.thetaMod(1.e-12, 1.e-12, 1.e-24, vol, false, 0.05);
      double resP2 = BlackFormulaRepository.thetaMod(1.e-12, 1.e-12, 1.e24, vol, false, 0.05);
      double resP3 = BlackFormulaRepository.thetaMod(1.e-12, 1.e12, 1.e-24, vol, false, 0.05);
      double resC4 = BlackFormulaRepository.thetaMod(1.e12, 1.e-12, 1.e-24, vol, true, 0.05);
      double resP4 = BlackFormulaRepository.thetaMod(1.e12, 1.e-12, 1.e-24, vol, false, 0.05);
      double resC5 = BlackFormulaRepository.thetaMod(FORWARD, FORWARD * (1. + 1.e-12), 1.e-24, vol, true, 0.05);
      double resP5 = BlackFormulaRepository.thetaMod(FORWARD, FORWARD * (1. + 1.e-12), 1.e-24, vol, false, 0.05);
      double resC6 = BlackFormulaRepository.thetaMod(1.e12, 1.e12, 1.e24, vol, true, 0.05);
      double resP6 = BlackFormulaRepository.thetaMod(1.e12, 1.e12, 1.e24, vol, false, 0.05);

      double refC1 = BlackFormulaRepository.thetaMod(0., 0., 0., vol, true, 0.05);
      double refC2 = BlackFormulaRepository.thetaMod(0., 0., inf, vol, true, 0.05);
      double refC3 = BlackFormulaRepository.thetaMod(0., inf, 0., vol, true, 0.05);
      double refP1 = BlackFormulaRepository.thetaMod(0., 0., 0., vol, false, 0.05);
      double refP2 = BlackFormulaRepository.thetaMod(0., 0., inf, vol, false, 0.05);
      double refP3 = BlackFormulaRepository.thetaMod(0., inf, 0., vol, false, 0.05);
      double refC4 = BlackFormulaRepository.thetaMod(inf, 0., 0., vol, true, 0.05);
      double refP4 = BlackFormulaRepository.thetaMod(inf, 0., 0., vol, false, 0.05);
      double refC5 = BlackFormulaRepository.thetaMod(FORWARD, FORWARD, 0., vol, true, 0.05);
      double refP5 = BlackFormulaRepository.thetaMod(FORWARD, FORWARD, 0., vol, false, 0.05);
      double refC6 = BlackFormulaRepository.thetaMod(inf, inf, inf, vol, true, 0.05);
      double refP6 = BlackFormulaRepository.thetaMod(inf, inf, inf, vol, false, 0.05);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6};
      for (int k = 0; k < 12; ++k) {

        if ((refVec[k] != -0.5 * vol) && (refVec[k] != -0.5 * FORWARD) && (refVec[k] != Double.NEGATIVE_INFINITY) &&
            k != 11) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-7).isTrue();  //should be rechecked
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.thetaMod(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true, 0.05);
      double resC2 = BlackFormulaRepository.thetaMod(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12, true, 0.05);
      double resC3 = BlackFormulaRepository.thetaMod(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12, true, 0.05);
      double resP1 = BlackFormulaRepository.thetaMod(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false, 0.05);
      double resP2 = BlackFormulaRepository.thetaMod(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12, false, 0.05);
      double resP3 = BlackFormulaRepository.thetaMod(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12, false, 0.05);
      double resC4 = BlackFormulaRepository.thetaMod(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true, 0.05);
      double resP4 = BlackFormulaRepository.thetaMod(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false, 0.05);
      double resC5 = BlackFormulaRepository.thetaMod(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12,
          true, 0.05);
      double resP5 = BlackFormulaRepository.thetaMod(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12,
          false, 0.05);
      double resC6 = BlackFormulaRepository.thetaMod(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e24, true, 0.05);
      double resP6 = BlackFormulaRepository.thetaMod(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e24, false, 0.05);

      double refC1 = BlackFormulaRepository.thetaMod(0., 0., TIME_TO_EXPIRY, 0., true, 0.05);
      double refC2 = BlackFormulaRepository.thetaMod(0., 0., TIME_TO_EXPIRY, inf, true, 0.05);
      double refC3 = BlackFormulaRepository.thetaMod(0., inf, TIME_TO_EXPIRY, 0., true, 0.05);
      double refP1 = BlackFormulaRepository.thetaMod(0., 0., TIME_TO_EXPIRY, 0., false, 0.05);
      double refP2 = BlackFormulaRepository.thetaMod(0., 0., TIME_TO_EXPIRY, inf, false, 0.05);
      double refP3 = BlackFormulaRepository.thetaMod(0., inf, TIME_TO_EXPIRY, 0., false, 0.05);
      double refC4 = BlackFormulaRepository.thetaMod(inf, 0., TIME_TO_EXPIRY, 0., true, 0.05);
      double refP4 = BlackFormulaRepository.thetaMod(inf, 0., TIME_TO_EXPIRY, 0., false, 0.05);
      double refC5 = BlackFormulaRepository.thetaMod(FORWARD, FORWARD, TIME_TO_EXPIRY, 0., true, 0.05);
      double refP5 = BlackFormulaRepository.thetaMod(FORWARD, FORWARD, TIME_TO_EXPIRY, 0., false, 0.05);
      double refC6 = BlackFormulaRepository.thetaMod(inf, inf, TIME_TO_EXPIRY, inf, true, 0.05);
      double refP6 = BlackFormulaRepository.thetaMod(inf, inf, TIME_TO_EXPIRY, inf, false, 0.05);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC6, resP6,
          resC5, resP5};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC6, refP6,
          refC5, refP5};

      for (int k = 0; k < 10; ++k) {  // The last two cases return reference values
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.thetaMod(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, true, 1.e-12);
      double resC2 = BlackFormulaRepository.thetaMod(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, true, 1.e-12);
      double resC3 = BlackFormulaRepository.thetaMod(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, true, 1.e-12);
      double resP1 = BlackFormulaRepository.thetaMod(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol, false, 1.e-12);
      double resP2 = BlackFormulaRepository.thetaMod(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, false, 1.e-12);
      double resP3 = BlackFormulaRepository.thetaMod(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, false, 1.e-12);
      double resC4 = BlackFormulaRepository.thetaMod(1.e12, 1.e12, TIME_TO_EXPIRY, vol, true, 1.e12);
      double resP4 = BlackFormulaRepository.thetaMod(1.e12, 1.e12, TIME_TO_EXPIRY, vol, false, 1.e12);
      double resC5 = BlackFormulaRepository.thetaMod(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, true, 1.e12);
      double resP5 = BlackFormulaRepository.thetaMod(1.e12, 1.e-12, TIME_TO_EXPIRY, vol, false, 1.e12);
      double resC6 = BlackFormulaRepository.thetaMod(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, true, 1.e12);
      double resP6 = BlackFormulaRepository.thetaMod(1.e-12, 1.e12, TIME_TO_EXPIRY, vol, false, 1.e12);
      double resC7 = BlackFormulaRepository.thetaMod(1.e-12, 2.e-12, TIME_TO_EXPIRY, vol, true, 1.e12);
      double resP7 = BlackFormulaRepository.thetaMod(1.e-12, 0.5e-12, TIME_TO_EXPIRY, vol, false, 1.e12);
      double resC8 = BlackFormulaRepository.thetaMod(1.e12, 1.e12, TIME_TO_EXPIRY, vol, true, 1.e-12);
      double resP8 = BlackFormulaRepository.thetaMod(1.e12, 1.e12, TIME_TO_EXPIRY, vol, false, 1.e-12);

      double refC1 = BlackFormulaRepository.thetaMod(0., 0., TIME_TO_EXPIRY, vol, true, 0.);
      double refC2 = BlackFormulaRepository.thetaMod(0., inf, TIME_TO_EXPIRY, vol, true, 0.);
      double refC3 = BlackFormulaRepository.thetaMod(inf, 0., TIME_TO_EXPIRY, vol, true, 0.);
      double refP1 = BlackFormulaRepository.thetaMod(0., 0., TIME_TO_EXPIRY, vol, false, 0.);
      double refP2 = BlackFormulaRepository.thetaMod(0., inf, TIME_TO_EXPIRY, vol, false, 0.);
      double refP3 = BlackFormulaRepository.thetaMod(inf, 0., TIME_TO_EXPIRY, vol, false, 0.);
      double refC4 = BlackFormulaRepository.thetaMod(inf, inf, TIME_TO_EXPIRY, vol, true, inf);
      double refP4 = BlackFormulaRepository.thetaMod(inf, inf, TIME_TO_EXPIRY, vol, false, inf);
      double refC5 = BlackFormulaRepository.thetaMod(inf, 0., TIME_TO_EXPIRY, vol, true, inf);
      double refP5 = BlackFormulaRepository.thetaMod(inf, 0., TIME_TO_EXPIRY, vol, false, inf);
      double refC6 = BlackFormulaRepository.thetaMod(0., inf, TIME_TO_EXPIRY, vol, true, inf);
      double refP6 = BlackFormulaRepository.thetaMod(0., inf, TIME_TO_EXPIRY, vol, false, inf);
      double refC7 = BlackFormulaRepository.thetaMod(0., 0., TIME_TO_EXPIRY, vol, true, inf);
      double refP7 = BlackFormulaRepository.thetaMod(0., 0., TIME_TO_EXPIRY, vol, false, inf);
      double refC8 = BlackFormulaRepository.thetaMod(inf, inf, TIME_TO_EXPIRY, vol, true, 0.);
      double refP8 = BlackFormulaRepository.thetaMod(inf, inf, TIME_TO_EXPIRY, vol, false, 0.);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7, resC8, resP8};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7, refC8, refP8};

      for (int k = 0; k < 14; ++k) {
        if (k != 3 && k != 8) {
          if (k != 12 && k != 13) {  // ref values are returned
            if (refVec[k] > 1.e10) {
              assertThat(resVec[k] > 1.e9).isTrue();
            } else {
              if (refVec[k] < -1.e10) {
                assertThat(resVec[k] < -1.e9).isTrue();
              } else {
                if (refVec[k] == 0.) {
                  assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
                } else {
                  assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.thetaMod(1.e-12, strike, 1.e-24, vol, true, 1.e-12);
        double resC2 = BlackFormulaRepository.thetaMod(1.e-12, strike, 1.e24, vol, true, 1.e-12);
        double resC3 = BlackFormulaRepository.thetaMod(1.e12, strike, 1.e-24, vol, true, 1.e-12);
        double resP1 = BlackFormulaRepository.thetaMod(1.e-12, strike, 1.e-24, vol, false, 1.e-12);
        double resP2 = BlackFormulaRepository.thetaMod(1.e-12, strike, 1.e24, vol, false, 1.e-12);
        double resP3 = BlackFormulaRepository.thetaMod(1.e12, strike, 1.e-24, vol, false, 1.e-12);
        double resC4 = BlackFormulaRepository.thetaMod(1.e12, strike, 1.e24, vol, true, 1.e12);
        double resP4 = BlackFormulaRepository.thetaMod(1.e12, strike, 1.e24, vol, false, 1.e12);
        double resC5 = BlackFormulaRepository.thetaMod(1.e-12, strike, 1.e24, vol, true, 1.e12);
        double resP5 = BlackFormulaRepository.thetaMod(1.e-12, strike, 1.e24, vol, false, 1.e12);
        double resC6 = BlackFormulaRepository.thetaMod(1.e12, strike, 1.e-24, vol, true, 1.e12);
        double resP6 = BlackFormulaRepository.thetaMod(1.e12, strike, 1.e-24, vol, false, 1.e12);
        double resC7 = BlackFormulaRepository.thetaMod(1.e12, strike, 1.e24, vol, true, 1.e-12);
        double resP7 = BlackFormulaRepository.thetaMod(1.e12, strike, 1.e24, vol, false, 1.e-12);
        double resC8 = BlackFormulaRepository.thetaMod(1.e-12, strike, 1.e-24, vol, true, 1.e12);
        double resP8 = BlackFormulaRepository.thetaMod(1.e-12, strike, 1.e-24, vol, false, 1.e12);

        double refC1 = BlackFormulaRepository.thetaMod(0., strike, 0., vol, true, 0.);
        double refC2 = BlackFormulaRepository.thetaMod(0., strike, inf, vol, true, 0.);
        double refC3 = BlackFormulaRepository.thetaMod(inf, strike, 0., vol, true, 0.);
        double refP1 = BlackFormulaRepository.thetaMod(0., strike, 0., vol, false, 0.);
        double refP2 = BlackFormulaRepository.thetaMod(0., strike, inf, vol, false, 0.);
        double refP3 = BlackFormulaRepository.thetaMod(inf, strike, 0., vol, false, 0.);
        double refC4 = BlackFormulaRepository.thetaMod(inf, strike, inf, vol, true, inf);
        double refP4 = BlackFormulaRepository.thetaMod(inf, strike, inf, vol, false, inf);
        double refC5 = BlackFormulaRepository.thetaMod(0., strike, inf, vol, true, inf);
        double refP5 = BlackFormulaRepository.thetaMod(0., strike, inf, vol, false, inf);
        double refC6 = BlackFormulaRepository.thetaMod(inf, strike, 0., vol, true, inf);
        double refP6 = BlackFormulaRepository.thetaMod(inf, strike, 0., vol, false, inf);
        double refC7 = BlackFormulaRepository.thetaMod(inf, strike, inf, vol, true, 0.);
        double refP7 = BlackFormulaRepository.thetaMod(inf, strike, inf, vol, false, 0.);
        double refC8 = BlackFormulaRepository.thetaMod(0., strike, 0., vol, true, inf);
        double refP8 = BlackFormulaRepository.thetaMod(0., strike, 0., vol, false, inf);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
            resC6, resP6, resC7, resP7, resC8, resP8};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
            refC6, refP6, refC7, refP7, refC8, refP8};

        for (int k = 0; k < 16; ++k) {
          if (k != 4 && k != 8 && k != 12) {
            if (refVec[k] > 1.e10) {
              assertThat(resVec[k] > 1.e10).isTrue();
            } else {
              if (refVec[k] < -1.e10) {
                assertThat(resVec[k] < -1.e10).isTrue();
              } else {
                if (refVec[k] == 0.) {
                  assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
                } else {
                  assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-9));
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, true, 1.e-12);
      double resC2 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, true, 1.e-12);
      double resC3 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, true, 1.e-12);
      double resP1 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, false, 1.e-12);
      double resP2 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, false, 1.e-12);
      double resP3 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, false, 1.e-12);
      double resC4 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, 1.e12, true, 1.e12);
      double resP4 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, 1.e12, false, 1.e12);
      double resC5 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, true, 1.e12);
      double resP5 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12, false, 1.e12);
      double resC6 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, true, 1.e12);
      double resP6 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, 1.e-12, false, 1.e12);
      double resC7 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, true, 1.e12);
      double resP7 = BlackFormulaRepository.thetaMod(1.e-12, strike, TIME_TO_EXPIRY, 1.e12, false, 1.e12);
      double resC8 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, 1.e12, true, 1.e-12);
      double resP8 = BlackFormulaRepository.thetaMod(1.e12, strike, TIME_TO_EXPIRY, 1.e12, false, 1.e-12);

      double refC1 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, 0., true, 0.);
      double refC2 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, inf, true, 0.);
      double refC3 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, 0., true, 0.);
      double refP1 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, 0., false, 0.);
      double refP2 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, inf, false, 0.);
      double refP3 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, 0., false, 0.);
      double refC4 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, inf, true, inf);
      double refP4 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, inf, false, inf);
      double refC5 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, 0., true, inf);
      double refP5 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, 0., false, inf);
      double refC6 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, 0., true, inf);
      double refP6 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, 0., false, inf);
      double refC7 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, inf, true, inf);
      double refP7 = BlackFormulaRepository.thetaMod(0., strike, TIME_TO_EXPIRY, inf, false, inf);
      double refC8 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, inf, true, 0.);
      double refP8 = BlackFormulaRepository.thetaMod(inf, strike, TIME_TO_EXPIRY, inf, false, 0.);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7, resC8, resP8};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7, refC8, refP8};

      for (int k = 0; k < 16; ++k) {
        if (k != 9 && k != 10) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-9));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.thetaMod(forward, 1.e-12, 1.e-24, vol, true, 1.e-12);
        double resC2 = BlackFormulaRepository.thetaMod(forward, 1.e-12, 1.e24, vol, true, 1.e-12);
        double resC3 = BlackFormulaRepository.thetaMod(forward, 1.e12, 1.e-24, vol, true, 1.e-12);
        double resP1 = BlackFormulaRepository.thetaMod(forward, 1.e-12, 1.e-24, vol, false, 1.e-12);
        double resP2 = BlackFormulaRepository.thetaMod(forward, 1.e-12, 1.e24, vol, false, 1.e-12);
        double resP3 = BlackFormulaRepository.thetaMod(forward, 1.e12, 1.e-24, vol, false, 1.e-12);
        double resC4 = BlackFormulaRepository.thetaMod(forward, 1.e12, 1.e24, vol, true, 1.e12);
        double resP4 = BlackFormulaRepository.thetaMod(forward, 1.e12, 1.e24, vol, false, 1.e12);
        double resC5 = BlackFormulaRepository.thetaMod(forward, 1.e-12, 1.e24, vol, true, 1.e12);
        double resP5 = BlackFormulaRepository.thetaMod(forward, 1.e-12, 1.e24, vol, false, 1.e12);
        double resC6 = BlackFormulaRepository.thetaMod(forward, 1.e12, 1.e-12, vol, true, 1.e12);
        double resP6 = BlackFormulaRepository.thetaMod(forward, 1.e12, 1.e-12, vol, false, 1.e12);
        double resC7 = BlackFormulaRepository.thetaMod(forward, 1.e-12, 1.e-12, vol, true, 1.e12);
        double resP7 = BlackFormulaRepository.thetaMod(forward, 1.e-12, 1.e-12, vol, false, 1.e12);
        double resC8 = BlackFormulaRepository.thetaMod(forward, 1.e12, 1.e24, vol, true, 1.e-12);
        double resP8 = BlackFormulaRepository.thetaMod(forward, 1.e12, 1.e24, vol, false, 1.e-12);

        double refC1 = BlackFormulaRepository.thetaMod(forward, 0., 0., vol, true, 0.);
        double refC2 = BlackFormulaRepository.thetaMod(forward, 0., inf, vol, true, 0.);
        double refC3 = BlackFormulaRepository.thetaMod(forward, inf, 0., vol, true, 0.);
        double refP1 = BlackFormulaRepository.thetaMod(forward, 0., 0., vol, false, 0.);
        double refP2 = BlackFormulaRepository.thetaMod(forward, 0., inf, vol, false, 0.);
        double refP3 = BlackFormulaRepository.thetaMod(forward, inf, 0., vol, false, 0.);
        double refC4 = BlackFormulaRepository.thetaMod(forward, inf, inf, vol, true, inf);
        double refP4 = BlackFormulaRepository.thetaMod(forward, inf, inf, vol, false, inf);
        double refC5 = BlackFormulaRepository.thetaMod(forward, 0., inf, vol, true, inf);
        double refP5 = BlackFormulaRepository.thetaMod(forward, 0., inf, vol, false, inf);
        double refC6 = BlackFormulaRepository.thetaMod(forward, inf, 0., vol, true, inf);
        double refP6 = BlackFormulaRepository.thetaMod(forward, inf, 0., vol, false, inf);
        double refC7 = BlackFormulaRepository.thetaMod(forward, 0., inf, vol, true, inf);
        double refP7 = BlackFormulaRepository.thetaMod(forward, 0., inf, vol, false, inf);
        double refC8 = BlackFormulaRepository.thetaMod(forward, 0., 0., vol, true, inf);
        double refP8 = BlackFormulaRepository.thetaMod(forward, 0., 0., vol, false, inf);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
            resC6, resP6, resC7, resP7, resC8, resP8};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
            refC6, refP6, refC7, refP7, refC8, refP8};

        for (int k = 0; k < 14; ++k) {  // some of ref values skipped
          if (k != 5 && k != 9 && k != 12) {
            if (refVec[k] > 1.e10) {
              assertThat(resVec[k] > 1.e10).isTrue();
            } else {
              if (refVec[k] < -1.e10) {
                assertThat(resVec[k] < -1.e10).isTrue();
              } else {
                if (refVec[k] == 0.) {
                  assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
                } else {
                  assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-9));
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.thetaMod(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true, 1.e-12);
      double resC2 = BlackFormulaRepository.thetaMod(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, true, 1.e-12);
      double resC3 = BlackFormulaRepository.thetaMod(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, true, 1.e-12);
      double resP1 = BlackFormulaRepository.thetaMod(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false, 1.e-12);
      double resP2 = BlackFormulaRepository.thetaMod(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, false, 1.e-12);
      double resP3 = BlackFormulaRepository.thetaMod(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, false, 1.e-12);
      double resC4 = BlackFormulaRepository.thetaMod(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, true, 1.e12);
      double resP4 = BlackFormulaRepository.thetaMod(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, false, 1.e12);
      double resC5 = BlackFormulaRepository.thetaMod(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, true, 1.e12);
      double resP5 = BlackFormulaRepository.thetaMod(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12, false, 1.e12);
      double resC6 = BlackFormulaRepository.thetaMod(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, true, 1.e12);
      double resP6 = BlackFormulaRepository.thetaMod(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12, false, 1.e12);
      double resC7 = BlackFormulaRepository.thetaMod(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, true, 1.e-12);
      double resP7 = BlackFormulaRepository.thetaMod(forward, 1.e12, TIME_TO_EXPIRY, 1.e12, false, 1.e-12);
      double resC8 = BlackFormulaRepository.thetaMod(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, true, 1.e12);
      double resP8 = BlackFormulaRepository.thetaMod(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12, false, 1.e12);

      double refC1 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, 0., true, 0.);
      double refC2 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, inf, true, 0.);
      double refC3 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, 0., true, 0.);
      double refP1 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, 0., false, 0.);
      double refP2 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, inf, false, 0.);
      double refP3 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, 0., false, 0.);
      double refC4 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, inf, true, inf);
      double refP4 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, inf, false, inf);
      double refC5 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, inf, true, inf);
      double refP5 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, inf, false, inf);
      double refC6 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, 0., true, inf);
      double refP6 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, 0., false, inf);
      double refC7 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, inf, true, 0.);
      double refP7 = BlackFormulaRepository.thetaMod(forward, inf, TIME_TO_EXPIRY, inf, false, 0.);
      double refC8 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, 0., true, inf);
      double refP8 = BlackFormulaRepository.thetaMod(forward, 0., TIME_TO_EXPIRY, 0., false, inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7, resC8, resP8};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7, refC8, refP8};

      for (int k = 0; k < 16; ++k) {
        if (k != 5 && k != 9 && k != 11 && k != 13 && k != 14) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-9));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.thetaMod(strike, strike, inf, 0., true, 1.);
      double resP1 = BlackFormulaRepository.thetaMod(strike, strike, inf, 0., false, 1.);
      double resC2 = BlackFormulaRepository.thetaMod(strike, strike, inf, 0., true, 0.);
      double resP2 = BlackFormulaRepository.thetaMod(strike, strike, inf, 0., false, 0.);
      double refC1 = strike * (NORMAL.getCDF(0.5));
      double refP1 = -strike * (NORMAL.getCDF(-0.5));

      double[] resVec = new double[] {resC1, resP1, resC2, resP2};
      double[] refVec = new double[] {refC1, refP1, 0., 0.};
      for (int k = 2; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10)));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, VOLS[0], true, -inf);
      double resP1 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, VOLS[1], false, -inf);
      double resC2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, VOLS[2], true, -inf);
      double resP2 = BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, VOLS[3], false, -inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2};
      double[] refVec = new double[] {0., 0., 0., 0.};
      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10)));
            }
          }
        }
      }
    }
  }

  @Test
  public void negativeVolErrorthetaModTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.thetaMod(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, true, 0.1));
  }

  @Test
  public void negativeFwdErrorthetaModTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.thetaMod(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], true, 0.1));
  }

  @Test
  public void negativeStrikeErrorthetaModTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.thetaMod(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], true, 0.1));
  }

  @Test
  public void negativeTimeErrorthetaModTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.thetaMod(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], true, 0.1));
  }

  @Test
  public void consistencyWithBlackScholestest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double interestRate = 0.02;
    double df = Math.exp(-interestRate * TIME_TO_EXPIRY);
    double spot = FORWARD * df;

    boolean[] tfSet = new boolean[] {true, false};
    for (boolean isCall : tfSet) {
      for (int i = 0; i < nStrikes; ++i) {
        for (int j = 0; j < nVols; ++j) {
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double price1 = df *
              BlackFormulaRepository.thetaMod(FORWARD, strike, TIME_TO_EXPIRY, vol, isCall, interestRate);
          double price2 = BlackScholesFormulaRepository.theta(spot, strike, TIME_TO_EXPIRY, vol, interestRate,
              interestRate, isCall);
          assertThat(price1).isCloseTo(price2, offset(1.e-14));
        }
      }
    }
  }

  /*
   * vega
   */
  /**
   * large/small input
   */
  @Test
  public void exVegaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vega(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.vega(0., strike, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.vega(1.e12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.vega(inf, strike, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-11).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-11));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vega(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.vega(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.vega(forward, 0., TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.vega(forward, inf, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vega(FORWARD, strike, 1e-24, vol);
        double resC2 = BlackFormulaRepository.vega(FORWARD, strike, 1e24, vol);
        double refC1 = BlackFormulaRepository.vega(FORWARD, strike, 0., vol);
        double refC2 = BlackFormulaRepository.vega(FORWARD, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.vega(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12);
      double refC1 = BlackFormulaRepository.vega(FORWARD, strike, TIME_TO_EXPIRY, 0.);
      double resC2 = BlackFormulaRepository.vega(FORWARD, strike, TIME_TO_EXPIRY, 1.e12);
      double refC2 = BlackFormulaRepository.vega(FORWARD, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2};
      double[] refVec = new double[] {refC1, refC2};

      for (int k = 0; k < 2; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.vega(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resC2 = BlackFormulaRepository.vega(1.e-12, 1.e12, TIME_TO_EXPIRY, vol);
      double resC3 = BlackFormulaRepository.vega(1.e12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resP3 = BlackFormulaRepository.vega(1.e12, 1.e12, TIME_TO_EXPIRY, vol);

      double refC1 = BlackFormulaRepository.vega(0., 0., TIME_TO_EXPIRY, vol);
      double refC2 = BlackFormulaRepository.vega(0., inf, TIME_TO_EXPIRY, vol);
      double refC3 = BlackFormulaRepository.vega(inf, 0., TIME_TO_EXPIRY, vol);
      double refP3 = BlackFormulaRepository.vega(inf, inf, TIME_TO_EXPIRY, vol);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e12) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e12) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vega(1.e-12, strike, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.vega(1.e-12, strike, 1.e24, vol);
        double resC3 = BlackFormulaRepository.vega(1.e12, strike, 1.e-24, vol);
        double resP3 = BlackFormulaRepository.vega(1.e12, strike, 1.e24, vol);

        double refC1 = BlackFormulaRepository.vega(0., strike, 0., vol);
        double refC2 = BlackFormulaRepository.vega(0., strike, inf, vol);
        double refC3 = BlackFormulaRepository.vega(inf, strike, 0., vol);
        double refP3 = BlackFormulaRepository.vega(inf, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2, resC3, resP3};
        double[] refVec = new double[] {refC1, refC2, refC3, refP3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.vega(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.vega(1.e-12, strike, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.vega(1.e12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resP3 = BlackFormulaRepository.vega(1.e12, strike, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.vega(0., strike, TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.vega(0., strike, TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.vega(inf, strike, TIME_TO_EXPIRY, 0.);
      double refP3 = BlackFormulaRepository.vega(inf, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vega(forward, 1.e-12, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.vega(forward, 1.e-12, 1.e24, vol);
        double resC3 = BlackFormulaRepository.vega(forward, 1.e12, 1.e-24, vol);
        double resP3 = BlackFormulaRepository.vega(forward, 1.e12, 1.e24, vol);

        double refC1 = BlackFormulaRepository.vega(forward, 0., 0., vol);
        double refC2 = BlackFormulaRepository.vega(forward, 0., inf, vol);
        double refC3 = BlackFormulaRepository.vega(forward, inf, 0., vol);
        double refP3 = BlackFormulaRepository.vega(forward, inf, inf, vol);

        double[] resVec = new double[] {resC1, resC2, resC3, resP3};
        double[] refVec = new double[] {refC1, refC2, refC3, refP3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.vega(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.vega(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.vega(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP3 = BlackFormulaRepository.vega(forward, 1.e12, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.vega(forward, 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.vega(forward, 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.vega(forward, inf, TIME_TO_EXPIRY, 0.);
      double refP3 = BlackFormulaRepository.vega(forward, inf, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.vega(1.e-12, 1.e-14, 1.e-24, vol);
      double resC2 = BlackFormulaRepository.vega(1.e-12, 1.e-12, 1.e24, vol);
      double resC3 = BlackFormulaRepository.vega(1.e-12, 1.e12, 1.e-24, vol);
      double resP1 = BlackFormulaRepository.vega(1.e-12, 1.e-14, 1.e-24, vol);
      double resP2 = BlackFormulaRepository.vega(1.e-12, 1.e-12, 1.e24, vol);
      double resP3 = BlackFormulaRepository.vega(1.e-12, 1.e12, 1.e-24, vol);
      double resC4 = BlackFormulaRepository.vega(1.e12, 1.e-12, 1.e-24, vol);
      double resP4 = BlackFormulaRepository.vega(1.e12, 1.e-12, 1.e-24, vol);
      double resC5 = BlackFormulaRepository.vega(FORWARD, FORWARD * (1. + 1.e-14), 1.e-24, vol);
      double resP5 = BlackFormulaRepository.vega(FORWARD, FORWARD * (1. + 1.e-14), 1.e-24, vol);
      double resC6 = BlackFormulaRepository.vega(1.e12, 1.e12, 1.e24, vol);
      double resP6 = BlackFormulaRepository.vega(1.e12, 1.e-12, 1.e24, vol);
      double resC7 = BlackFormulaRepository.vega(1.e12, 1.e12, 1.e-24, vol);
      double resP7 = BlackFormulaRepository.vega(1.e-12, 1.e12, 1.e24, vol);

      double refC1 = BlackFormulaRepository.vega(0., 0., 0., vol);
      double refC2 = BlackFormulaRepository.vega(0., 0., inf, vol);
      double refC3 = BlackFormulaRepository.vega(0., inf, 0., vol);
      double refP1 = BlackFormulaRepository.vega(0., 0., 0., vol);
      double refP2 = BlackFormulaRepository.vega(0., 0., inf, vol);
      double refP3 = BlackFormulaRepository.vega(0., inf, 0., vol);
      double refC4 = BlackFormulaRepository.vega(inf, 0., 0., vol);
      double refP4 = BlackFormulaRepository.vega(inf, 0., 0., vol);
      double refC5 = BlackFormulaRepository.vega(FORWARD, FORWARD, 0., vol);
      double refP5 = BlackFormulaRepository.vega(FORWARD, FORWARD, 0., vol);
      double refC6 = BlackFormulaRepository.vega(inf, inf, inf, vol);
      double refP6 = BlackFormulaRepository.vega(inf, 0., inf, vol);
      double refC7 = BlackFormulaRepository.vega(inf, inf, 0., vol);
      double refP7 = BlackFormulaRepository.vega(0., inf, inf, vol);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7};

      for (int k = 0; k < 14; ++k) {

        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.vega(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.vega(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.vega(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP1 = BlackFormulaRepository.vega(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resP2 = BlackFormulaRepository.vega(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resP3 = BlackFormulaRepository.vega(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resC4 = BlackFormulaRepository.vega(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resP4 = BlackFormulaRepository.vega(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC5 = BlackFormulaRepository.vega(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12);
      double resP5 = BlackFormulaRepository.vega(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12);
      double resC6 = BlackFormulaRepository.vega(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e12);
      double resP6 = BlackFormulaRepository.vega(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC7 = BlackFormulaRepository.vega(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP7 = BlackFormulaRepository.vega(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.vega(0., 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.vega(0., 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.vega(0., inf, TIME_TO_EXPIRY, 0.);
      double refP1 = BlackFormulaRepository.vega(0., 0., TIME_TO_EXPIRY, 0.);
      double refP2 = BlackFormulaRepository.vega(0., 0., TIME_TO_EXPIRY, inf);
      double refP3 = BlackFormulaRepository.vega(0., inf, TIME_TO_EXPIRY, 0.);
      double refC4 = BlackFormulaRepository.vega(inf, 0., TIME_TO_EXPIRY, 0.);
      double refP4 = BlackFormulaRepository.vega(inf, 0., TIME_TO_EXPIRY, 0.);
      double refC5 = BlackFormulaRepository.vega(FORWARD, FORWARD, TIME_TO_EXPIRY, 0.);
      double refP5 = BlackFormulaRepository.vega(FORWARD, FORWARD, TIME_TO_EXPIRY, 0.);
      double refC6 = BlackFormulaRepository.vega(inf, inf, TIME_TO_EXPIRY, inf);
      double refP6 = BlackFormulaRepository.vega(inf, 0., TIME_TO_EXPIRY, inf);
      double refC7 = BlackFormulaRepository.vega(inf, inf, TIME_TO_EXPIRY, 0.);
      double refP7 = BlackFormulaRepository.vega(0., inf, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7};

      for (int k = 0; k < 14; ++k) {
        if (refVec[k] != FORWARD * Math.sqrt(TIME_TO_EXPIRY) * NORMAL.getPDF(0.)) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (Math.abs(refVec[k]) < 1.e-10) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double resC1 = BlackFormulaRepository.vega(FORWARD, 1.e-12, 1.e-24, 1.e-12);
      double resC2 = BlackFormulaRepository.vega(FORWARD, 1.e-12, 1.e-24, 1.e12);
      double resC3 = BlackFormulaRepository.vega(FORWARD, 1.e-12, 1.e24, 1.e-12);
      double resP1 = BlackFormulaRepository.vega(FORWARD, 1.e-12, 1.e-24, 1.e-12);
      double resP2 = BlackFormulaRepository.vega(FORWARD, 1.e-12, 1.e-24, 1.e12);
      double resP3 = BlackFormulaRepository.vega(FORWARD, 1.e-12, 1.e24, 1.e-12);
      double resC4 = BlackFormulaRepository.vega(FORWARD, 1.e12, 1.e-24, 1.e-12);
      double resP4 = BlackFormulaRepository.vega(FORWARD, 1.e12, 1.e-24, 1.e-12);
      double resC6 = BlackFormulaRepository.vega(FORWARD, 1.e12, 1.e24, 1.e12);
      double resP6 = BlackFormulaRepository.vega(FORWARD, 1.e12, 1.e-24, 1.e12);
      double resC7 = BlackFormulaRepository.vega(FORWARD, 1.e12, 1.e24, 1.e-12);
      double resP7 = BlackFormulaRepository.vega(FORWARD, 1.e-12, 1.e24, 1.e12);

      double refC1 = BlackFormulaRepository.vega(FORWARD, 0., 0., 0.);
      double refC2 = BlackFormulaRepository.vega(FORWARD, 0., 0., inf);
      double refC3 = BlackFormulaRepository.vega(FORWARD, 0., inf, 0.);
      double refP1 = BlackFormulaRepository.vega(FORWARD, 0., 0., 0.);
      double refP2 = BlackFormulaRepository.vega(FORWARD, 0., 0., inf);
      double refP3 = BlackFormulaRepository.vega(FORWARD, 0., inf, 0.);
      double refC4 = BlackFormulaRepository.vega(FORWARD, inf, 0., 0.);
      double refP4 = BlackFormulaRepository.vega(FORWARD, inf, 0., 0.);
      double refC6 = BlackFormulaRepository.vega(FORWARD, inf, inf, inf);
      double refP6 = BlackFormulaRepository.vega(FORWARD, inf, 0., inf);
      double refC7 = BlackFormulaRepository.vega(FORWARD, inf, inf, 0.);
      double refP7 = BlackFormulaRepository.vega(FORWARD, 0., inf, inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC6, resP6,
          resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC6, refP6,
          refC7, refP7};

      for (int k = 0; k < 12; ++k) {

        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }
  }

  @Test
  public void negativeVolErrorVegaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.vega(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5));
  }

  @Test
  public void negativeFwdErrorVegaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.vega(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeStrikeErrorVegaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.vega(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeTimeErrorVegaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.vega(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1]));
  }

  /*
   * vanna
   */
  @Test
  public void exVannaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vanna(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.vanna(0., strike, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.vanna(1.e12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.vanna(inf, strike, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-11).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-11));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vanna(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.vanna(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.vanna(forward, 0., TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.vanna(forward, inf, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vanna(FORWARD, strike, 1e-24, vol);
        double resC2 = BlackFormulaRepository.vanna(FORWARD, strike, 1e24, vol);
        double refC1 = BlackFormulaRepository.vanna(FORWARD, strike, 0., vol);
        double refC2 = BlackFormulaRepository.vanna(FORWARD, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.vanna(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12);
      double refC1 = BlackFormulaRepository.vanna(FORWARD, strike, TIME_TO_EXPIRY, 0.);
      double resC2 = BlackFormulaRepository.vanna(FORWARD, strike, TIME_TO_EXPIRY, 1.e12);
      double refC2 = BlackFormulaRepository.vanna(FORWARD, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2};
      double[] refVec = new double[] {refC1, refC2};

      for (int k = 0; k < 2; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.vanna(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resC2 = BlackFormulaRepository.vanna(1.e-12, 1.e12, TIME_TO_EXPIRY, vol);
      double resC3 = BlackFormulaRepository.vanna(1.e12, 1.e12, TIME_TO_EXPIRY, vol);
      double resP3 = BlackFormulaRepository.vanna(1.e12, 1.e12, TIME_TO_EXPIRY, vol);

      double refC1 = BlackFormulaRepository.vanna(0., 0., TIME_TO_EXPIRY, vol);
      double refC2 = BlackFormulaRepository.vanna(0., inf, TIME_TO_EXPIRY, vol);
      double refC3 = BlackFormulaRepository.vanna(inf, inf, TIME_TO_EXPIRY, vol);
      double refP3 = BlackFormulaRepository.vanna(inf, inf, TIME_TO_EXPIRY, vol);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e12) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e12) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vanna(1.e-12, strike, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.vanna(1.e-12, strike, 1.e24, vol);
        double resC3 = BlackFormulaRepository.vanna(1.e12, strike, 1.e-24, vol);
        double resP3 = BlackFormulaRepository.vanna(1.e12, strike, 1.e24, vol);

        double refC1 = BlackFormulaRepository.vanna(0., strike, 0., vol);
        double refC2 = BlackFormulaRepository.vanna(0., strike, inf, vol);
        double refC3 = BlackFormulaRepository.vanna(inf, strike, 0., vol);
        double refP3 = BlackFormulaRepository.vanna(inf, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2, resC3, resP3};
        double[] refVec = new double[] {refC1, refC2, refC3, refP3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.vanna(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.vanna(1.e-12, strike, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.vanna(1.e12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resP3 = BlackFormulaRepository.vanna(1.e12, strike, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.vanna(0., strike, TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.vanna(0., strike, TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.vanna(inf, strike, TIME_TO_EXPIRY, 0.);
      double refP3 = BlackFormulaRepository.vanna(inf, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vanna(forward, 1.e-12, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.vanna(forward, 1.e-12, 1.e24, vol);
        double resC3 = BlackFormulaRepository.vanna(forward, 1.e12, 1.e-24, vol);
        double resP3 = BlackFormulaRepository.vanna(forward, 1.e12, 1.e24, vol);

        double refC1 = BlackFormulaRepository.vanna(forward, 0., 0., vol);
        double refC2 = BlackFormulaRepository.vanna(forward, 0., inf, vol);
        double refC3 = BlackFormulaRepository.vanna(forward, inf, 0., vol);
        double refP3 = BlackFormulaRepository.vanna(forward, inf, inf, vol);

        double[] resVec = new double[] {resC1, resC2, resC3, resP3};
        double[] refVec = new double[] {refC1, refC2, refC3, refP3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.vanna(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.vanna(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.vanna(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP3 = BlackFormulaRepository.vanna(forward, 1.e12, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.vanna(forward, 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.vanna(forward, 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.vanna(forward, inf, TIME_TO_EXPIRY, 0.);
      ;
      double refP3 = BlackFormulaRepository.vanna(forward, inf, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.vanna(1.e-12, 1.e-12, 1.e-24, vol);
      double resC2 = BlackFormulaRepository.vanna(1.e-12, 1.e-12, 1.e24, vol);
      double resC3 = BlackFormulaRepository.vanna(1.e-12, 1.e12, 1.e-24, vol);
      double resP1 = BlackFormulaRepository.vanna(1.e-12, 1.e-14, 1.e-24, vol);
      double resP2 = BlackFormulaRepository.vanna(1.e-12, 1.e-12, 1.e24, vol);
      double resP3 = BlackFormulaRepository.vanna(1.e-12, 1.e12, 1.e-24, vol);
      double resC4 = BlackFormulaRepository.vanna(1.e12, 1.e-12, 1.e-24, vol);
      double resP4 = BlackFormulaRepository.vanna(1.e12, 1.e-12, 1.e-24, vol);
      double resC5 = BlackFormulaRepository.vanna(FORWARD, FORWARD * (1. + 1.e-14), 1.e-24, vol);
      double resP5 = BlackFormulaRepository.vanna(FORWARD, FORWARD * (1. + 1.e-14), 1.e-24, vol);
      double resC6 = BlackFormulaRepository.vanna(1.e12, 1.e12, 1.e24, vol);
      double resP6 = BlackFormulaRepository.vanna(1.e12, 1.e-12, 1.e24, vol);
      double resC7 = BlackFormulaRepository.vanna(1.e12, 1.e12, 1.e-24, vol);
      double resP7 = BlackFormulaRepository.vanna(1.e-12, 1.e12, 1.e24, vol);

      double refC1 = BlackFormulaRepository.vanna(0., 0., 0., vol);
      double refC2 = BlackFormulaRepository.vanna(0., 0., inf, vol);
      double refC3 = BlackFormulaRepository.vanna(0., inf, 0., vol);
      double refP1 = BlackFormulaRepository.vanna(0., 0., 0., vol);
      double refP2 = BlackFormulaRepository.vanna(0., 0., inf, vol);
      double refP3 = BlackFormulaRepository.vanna(0., inf, 0., vol);
      double refC4 = BlackFormulaRepository.vanna(inf, 0., 0., vol);
      double refP4 = BlackFormulaRepository.vanna(inf, 0., 0., vol);
      double refC5 = BlackFormulaRepository.vanna(FORWARD, FORWARD, 0., vol);
      double refP5 = BlackFormulaRepository.vanna(FORWARD, FORWARD, 0., vol);
      double refC6 = BlackFormulaRepository.vanna(inf, inf, inf, vol);
      double refP6 = BlackFormulaRepository.vanna(inf, 0., inf, vol);
      double refC7 = BlackFormulaRepository.vanna(inf, inf, 0., vol);
      double refP7 = BlackFormulaRepository.vanna(0., inf, inf, vol);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7};

      for (int k = 0; k < 14; ++k) {
        // refC5 and refP5 are ambiguous cases
        if (k != 8 && k != 9) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.vanna(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.vanna(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.vanna(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP1 = BlackFormulaRepository.vanna(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resP2 = BlackFormulaRepository.vanna(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resP3 = BlackFormulaRepository.vanna(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resC4 = BlackFormulaRepository.vanna(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resP4 = BlackFormulaRepository.vanna(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC5 = BlackFormulaRepository.vanna(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12);
      double resP5 = BlackFormulaRepository.vanna(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12);
      double resC6 = BlackFormulaRepository.vanna(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e12);
      double resP6 = BlackFormulaRepository.vanna(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC7 = BlackFormulaRepository.vanna(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP7 = BlackFormulaRepository.vanna(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.vanna(0., 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.vanna(0., 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.vanna(0., inf, TIME_TO_EXPIRY, 0.);
      double refP1 = BlackFormulaRepository.vanna(0., 0., TIME_TO_EXPIRY, 0.);
      double refP2 = BlackFormulaRepository.vanna(0., 0., TIME_TO_EXPIRY, inf);
      double refP3 = BlackFormulaRepository.vanna(0., inf, TIME_TO_EXPIRY, 0.);
      double refC4 = BlackFormulaRepository.vanna(inf, 0., TIME_TO_EXPIRY, 0.);
      double refP4 = BlackFormulaRepository.vanna(inf, 0., TIME_TO_EXPIRY, 0.);
      double refC5 = BlackFormulaRepository.vanna(FORWARD, FORWARD, TIME_TO_EXPIRY, 0.);
      double refP5 = BlackFormulaRepository.vanna(FORWARD, FORWARD, TIME_TO_EXPIRY, 0.);
      double refC6 = BlackFormulaRepository.vanna(inf, inf, TIME_TO_EXPIRY, inf);
      double refP6 = BlackFormulaRepository.vanna(inf, 0., TIME_TO_EXPIRY, inf);
      double refC7 = BlackFormulaRepository.vanna(inf, inf, TIME_TO_EXPIRY, 0.);
      double refP7 = BlackFormulaRepository.vanna(0., inf, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7};

      for (int k = 2; k < 12; ++k) {
        if (k != 8 && k != 9) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (Math.abs(refVec[k]) < 1.e-10) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.vanna(FORWARD, 1.e-12, 1.e-24, 1.e-12);
      double resC2 = BlackFormulaRepository.vanna(FORWARD, 1.e-12, 1.e-24, 1.e12);
      double resC3 = BlackFormulaRepository.vanna(FORWARD, 1.e-12, 1.e24, 1.e-12);
      double resP1 = BlackFormulaRepository.vanna(FORWARD, 1.e-12, 1.e-24, 1.e-12);
      double resP2 = BlackFormulaRepository.vanna(FORWARD, 1.e-12, 1.e-24, 1.e12);
      double resP3 = BlackFormulaRepository.vanna(FORWARD, 1.e-12, 1.e24, 1.e-12);
      double resC4 = BlackFormulaRepository.vanna(FORWARD, 1.e12, 1.e-24, 1.e-12);
      double resP4 = BlackFormulaRepository.vanna(FORWARD, 1.e12, 1.e-24, 1.e-12);
      double resC6 = BlackFormulaRepository.vanna(FORWARD, 1.e12, 1.e24, 1.e12);
      double resP6 = BlackFormulaRepository.vanna(FORWARD, 1.e12, 1.e-24, 1.e12);
      double resC7 = BlackFormulaRepository.vanna(FORWARD, 1.e12, 1.e24, 1.e-12);
      double resP7 = BlackFormulaRepository.vanna(FORWARD, 1.e-12, 1.e24, 1.e12);

      double refC1 = BlackFormulaRepository.vanna(FORWARD, 0., 0., 0.);
      double refC2 = BlackFormulaRepository.vanna(FORWARD, 0., 0., inf);
      double refC3 = BlackFormulaRepository.vanna(FORWARD, 0., inf, 0.);
      double refP1 = BlackFormulaRepository.vanna(FORWARD, 0., 0., 0.);
      double refP2 = BlackFormulaRepository.vanna(FORWARD, 0., 0., inf);
      double refP3 = BlackFormulaRepository.vanna(FORWARD, 0., inf, 0.);
      double refC4 = BlackFormulaRepository.vanna(FORWARD, inf, 0., 0.);
      double refP4 = BlackFormulaRepository.vanna(FORWARD, inf, 0., 0.);
      double refC6 = BlackFormulaRepository.vanna(FORWARD, inf, inf, inf);
      double refP6 = BlackFormulaRepository.vanna(FORWARD, inf, 0., inf);
      double refC7 = BlackFormulaRepository.vanna(FORWARD, inf, inf, 0.);
      double refP7 = BlackFormulaRepository.vanna(FORWARD, 0., inf, inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC6, resP6,
          resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC6, refP6,
          refC7, refP7};

      for (int k = 0; k < 12; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (Math.abs(refVec[k]) < 1.e-10) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }
  }

  @Test
  public void negativeVolErrorVannaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.vanna(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5));
  }

  @Test
  public void negativeFwdErrorVannaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.vanna(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeStrikeErrorVannaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.vanna(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeTimeErrorVannaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.vanna(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1]));
  }

  /*
   * dualVanna
   */
  /**
   * large/small input
   */
  @Test
  public void exDualVannaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualVanna(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.dualVanna(0., strike, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.dualVanna(1.e12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.dualVanna(inf, strike, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-11).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-11));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualVanna(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.dualVanna(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.dualVanna(forward, 0., TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.dualVanna(forward, inf, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualVanna(FORWARD, strike, 1e-24, vol);
        double resC2 = BlackFormulaRepository.dualVanna(FORWARD, strike, 1e24, vol);
        double refC1 = BlackFormulaRepository.dualVanna(FORWARD, strike, 0., vol);
        double refC2 = BlackFormulaRepository.dualVanna(FORWARD, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.dualVanna(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12);
      double refC1 = BlackFormulaRepository.dualVanna(FORWARD, strike, TIME_TO_EXPIRY, 0.);
      double resC2 = BlackFormulaRepository.dualVanna(FORWARD, strike, TIME_TO_EXPIRY, 1.e12);
      double refC2 = BlackFormulaRepository.dualVanna(FORWARD, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2};
      double[] refVec = new double[] {refC1, refC2};

      for (int k = 0; k < 2; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.dualVanna(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resC2 = BlackFormulaRepository.dualVanna(1.e-12, 1.e12, TIME_TO_EXPIRY, vol);
      double resC3 = BlackFormulaRepository.dualVanna(1.e12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resP3 = BlackFormulaRepository.dualVanna(1.e12, 1.e12, TIME_TO_EXPIRY, vol);

      double refC1 = BlackFormulaRepository.dualVanna(0., 0., TIME_TO_EXPIRY, vol);
      double refC2 = BlackFormulaRepository.dualVanna(0., inf, TIME_TO_EXPIRY, vol);
      double refC3 = BlackFormulaRepository.dualVanna(inf, 0., TIME_TO_EXPIRY, vol);
      double refP3 = BlackFormulaRepository.dualVanna(inf, inf, TIME_TO_EXPIRY, vol);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e12) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e12) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualVanna(1.e-12, strike, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.dualVanna(1.e-12, strike, 1.e24, vol);
        double resC3 = BlackFormulaRepository.dualVanna(1.e12, strike, 1.e-24, vol);
        double resP3 = BlackFormulaRepository.dualVanna(1.e12, strike, 1.e24, vol);

        double refC1 = BlackFormulaRepository.dualVanna(0., strike, 0., vol);
        double refC2 = BlackFormulaRepository.dualVanna(0., strike, inf, vol);
        double refC3 = BlackFormulaRepository.dualVanna(inf, strike, 0., vol);
        double refP3 = BlackFormulaRepository.dualVanna(inf, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2, resC3, resP3};
        double[] refVec = new double[] {refC1, refC2, refC3, refP3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.dualVanna(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.dualVanna(1.e-12, strike, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.dualVanna(1.e12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resP3 = BlackFormulaRepository.dualVanna(1.e12, strike, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.dualVanna(0., strike, TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.dualVanna(0., strike, TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.dualVanna(inf, strike, TIME_TO_EXPIRY, 0.);
      double refP3 = BlackFormulaRepository.dualVanna(inf, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.dualVanna(forward, 1.e-12, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.dualVanna(forward, 1.e-12, 1.e24, vol);
        double resC3 = BlackFormulaRepository.dualVanna(forward, 1.e12, 1.e-24, vol);
        double resP3 = BlackFormulaRepository.dualVanna(forward, 1.e12, 1.e24, vol);

        double refC1 = BlackFormulaRepository.dualVanna(forward, 0., 0., vol);
        double refC2 = BlackFormulaRepository.dualVanna(forward, 0., inf, vol);
        double refC3 = BlackFormulaRepository.dualVanna(forward, inf, 0., vol);
        double refP3 = BlackFormulaRepository.dualVanna(forward, inf, inf, vol);

        double[] resVec = new double[] {resC1, resC2, resC3, resP3};
        double[] refVec = new double[] {refC1, refC2, refC3, refP3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.dualVanna(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.dualVanna(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.dualVanna(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP3 = BlackFormulaRepository.dualVanna(forward, 1.e12, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.dualVanna(forward, 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.dualVanna(forward, 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.dualVanna(forward, inf, TIME_TO_EXPIRY, 0.);
      double refP3 = BlackFormulaRepository.dualVanna(forward, inf, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.dualVanna(1.e-12, 1.e-12, 1.e-24, vol);
      double resC2 = BlackFormulaRepository.dualVanna(1.e-12, 1.e-12, 1.e24, vol);
      double resC3 = BlackFormulaRepository.dualVanna(1.e-12, 1.e12, 1.e-24, vol);
      double resP1 = BlackFormulaRepository.dualVanna(1.e-12, 1.e-14, 1.e-24, vol);
      double resP2 = BlackFormulaRepository.dualVanna(1.e-12, 1.e-12, 1.e24, vol);
      double resP3 = BlackFormulaRepository.dualVanna(1.e-12, 1.e12, 1.e-24, vol);
      double resC4 = BlackFormulaRepository.dualVanna(1.e12, 1.e-12, 1.e-24, vol);
      double resP4 = BlackFormulaRepository.dualVanna(1.e12, 1.e-12, 1.e-24, vol);
      double resC5 = BlackFormulaRepository.dualVanna(FORWARD, FORWARD * (1. + 1.e-14), 1.e-24, vol);
      double resP5 = BlackFormulaRepository.dualVanna(FORWARD, FORWARD * (1. + 1.e-14), 1.e-24, vol);
      double resC6 = BlackFormulaRepository.dualVanna(1.e12, 1.e12, 1.e24, vol);
      double resP6 = BlackFormulaRepository.dualVanna(1.e12, 1.e-12, 1.e24, vol);
      double resC7 = BlackFormulaRepository.dualVanna(1.e12, 1.e12, 1.e-24, vol);
      double resP7 = BlackFormulaRepository.dualVanna(1.e-12, 1.e12, 1.e24, vol);

      double refC1 = BlackFormulaRepository.dualVanna(0., 0., 0., vol);
      double refC2 = BlackFormulaRepository.dualVanna(0., 0., inf, vol);
      double refC3 = BlackFormulaRepository.dualVanna(0., inf, 0., vol);
      double refP1 = BlackFormulaRepository.dualVanna(0., 0., 0., vol);
      double refP2 = BlackFormulaRepository.dualVanna(0., 0., inf, vol);
      double refP3 = BlackFormulaRepository.dualVanna(0., inf, 0., vol);
      double refC4 = BlackFormulaRepository.dualVanna(inf, 0., 0., vol);
      double refP4 = BlackFormulaRepository.dualVanna(inf, 0., 0., vol);
      double refC5 = BlackFormulaRepository.dualVanna(FORWARD, FORWARD, 0., vol);
      double refP5 = BlackFormulaRepository.dualVanna(FORWARD, FORWARD, 0., vol);
      double refC6 = BlackFormulaRepository.dualVanna(inf, inf, inf, vol);
      double refP6 = BlackFormulaRepository.dualVanna(inf, 0., inf, vol);
      double refC7 = BlackFormulaRepository.dualVanna(inf, inf, 0., vol);
      double refP7 = BlackFormulaRepository.dualVanna(0., inf, inf, vol);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7};

      for (int k = 0; k < 14; ++k) {
        if (k != 8 && k != 9) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.dualVanna(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.dualVanna(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.dualVanna(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP1 = BlackFormulaRepository.dualVanna(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resP2 = BlackFormulaRepository.dualVanna(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resP3 = BlackFormulaRepository.dualVanna(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resC4 = BlackFormulaRepository.dualVanna(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resP4 = BlackFormulaRepository.dualVanna(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC5 = BlackFormulaRepository.dualVanna(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12);
      double resP5 = BlackFormulaRepository.dualVanna(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12);
      double resC6 = BlackFormulaRepository.dualVanna(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e12);
      double resP6 = BlackFormulaRepository.dualVanna(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC7 = BlackFormulaRepository.dualVanna(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP7 = BlackFormulaRepository.dualVanna(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.dualVanna(0., 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.dualVanna(0., 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.dualVanna(0., inf, TIME_TO_EXPIRY, 0.);
      double refP1 = BlackFormulaRepository.dualVanna(0., 0., TIME_TO_EXPIRY, 0.);
      double refP2 = BlackFormulaRepository.dualVanna(0., 0., TIME_TO_EXPIRY, inf);
      double refP3 = BlackFormulaRepository.dualVanna(0., inf, TIME_TO_EXPIRY, 0.);
      double refC4 = BlackFormulaRepository.dualVanna(inf, 0., TIME_TO_EXPIRY, 0.);
      double refP4 = BlackFormulaRepository.dualVanna(inf, 0., TIME_TO_EXPIRY, 0.);
      double refC5 = BlackFormulaRepository.dualVanna(FORWARD, FORWARD, TIME_TO_EXPIRY, 0.);
      double refP5 = BlackFormulaRepository.dualVanna(FORWARD, FORWARD, TIME_TO_EXPIRY, 0.);
      double refC6 = BlackFormulaRepository.dualVanna(inf, inf, TIME_TO_EXPIRY, inf);
      double refP6 = BlackFormulaRepository.dualVanna(inf, 0., TIME_TO_EXPIRY, inf);
      double refC7 = BlackFormulaRepository.dualVanna(inf, inf, TIME_TO_EXPIRY, 0.);
      double refP7 = BlackFormulaRepository.dualVanna(0., inf, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7};

      for (int k = 2; k < 12; ++k) {
        if (k != 8 && k != 9) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (Math.abs(refVec[k]) < 1.e-10) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.dualVanna(FORWARD, 1.e-12, 1.e-12, 1.e-12);
      double resC2 = BlackFormulaRepository.dualVanna(FORWARD, 1.e-12, 1.e-12, 1.e12);
      double resC3 = BlackFormulaRepository.dualVanna(FORWARD, 1.e-12, 1.e12, 1.e-12);
      double resP1 = BlackFormulaRepository.dualVanna(FORWARD, 1.e-12, 1.e-12, 1.e-12);
      double resP2 = BlackFormulaRepository.dualVanna(FORWARD, 1.e-12, 1.e-12, 1.e12);
      double resP3 = BlackFormulaRepository.dualVanna(FORWARD, 1.e-12, 1.e12, 1.e-12);
      double resC4 = BlackFormulaRepository.dualVanna(FORWARD, 1.e12, 1.e-12, 1.e-12);
      double resP4 = BlackFormulaRepository.dualVanna(FORWARD, 1.e12, 1.e-12, 1.e-12);
      double resC6 = BlackFormulaRepository.dualVanna(FORWARD, 1.e12, 1.e12, 1.e12);
      double resP6 = BlackFormulaRepository.dualVanna(FORWARD, 1.e12, 1.e-12, 1.e12);
      double resC7 = BlackFormulaRepository.dualVanna(FORWARD, 1.e12, 1.e12, 1.e-12);
      double resP7 = BlackFormulaRepository.dualVanna(FORWARD, 1.e-12, 1.e12, 1.e12);

      double refC1 = BlackFormulaRepository.dualVanna(FORWARD, 0., 0., 0.);
      double refC2 = BlackFormulaRepository.dualVanna(FORWARD, 0., 0., inf);
      double refC3 = BlackFormulaRepository.dualVanna(FORWARD, 0., inf, 0.);
      double refP1 = BlackFormulaRepository.dualVanna(FORWARD, 0., 0., 0.);
      double refP2 = BlackFormulaRepository.dualVanna(FORWARD, 0., 0., inf);
      double refP3 = BlackFormulaRepository.dualVanna(FORWARD, 0., inf, 0.);
      double refC4 = BlackFormulaRepository.dualVanna(FORWARD, inf, 0., 0.);
      double refP4 = BlackFormulaRepository.dualVanna(FORWARD, inf, 0., 0.);
      double refC6 = BlackFormulaRepository.dualVanna(FORWARD, inf, inf, inf);
      double refP6 = BlackFormulaRepository.dualVanna(FORWARD, inf, 0., inf);
      double refC7 = BlackFormulaRepository.dualVanna(FORWARD, inf, inf, 0.);
      double refP7 = BlackFormulaRepository.dualVanna(FORWARD, 0., inf, inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC6, resP6,
          resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC6, refP6,
          refC7, refP7};

      for (int k = 0; k < 12; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (Math.abs(refVec[k]) < 1.e-10) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }
  }

  @Test
  public void negativeVolErrorDualVannaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.dualVanna(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5));
  }

  @Test
  public void negativeFwdErrorDualVannaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.dualVanna(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeStrikeErrorDualVannaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.dualVanna(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeTimeErrorDualVannaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.dualVanna(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1]));
  }

  /*
   * vomma
   */
  /**
   * large/small input
   */
  @Test
  public void exVommaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vomma(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.vomma(0., strike, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.vomma(1.e12 * strike, strike, TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.vomma(inf, strike, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-11).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-11));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vomma(forward, 1.e-12 * forward, TIME_TO_EXPIRY, vol);
        double resC2 = BlackFormulaRepository.vomma(forward, 1.e12 * forward, TIME_TO_EXPIRY, vol);
        double refC1 = BlackFormulaRepository.vomma(forward, 0., TIME_TO_EXPIRY, vol);
        double refC2 = BlackFormulaRepository.vomma(forward, inf, TIME_TO_EXPIRY, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vomma(FORWARD, strike, 1e-24, vol);
        double resC2 = BlackFormulaRepository.vomma(FORWARD, strike, 1e24, vol);
        double refC1 = BlackFormulaRepository.vomma(FORWARD, strike, 0., vol);
        double refC2 = BlackFormulaRepository.vomma(FORWARD, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e12).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.vomma(FORWARD, strike, TIME_TO_EXPIRY, 1.e-12);
      double refC1 = BlackFormulaRepository.vomma(FORWARD, strike, TIME_TO_EXPIRY, 0.);
      double resC2 = BlackFormulaRepository.vomma(FORWARD, strike, TIME_TO_EXPIRY, 1.e12);
      double refC2 = BlackFormulaRepository.vomma(FORWARD, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2};
      double[] refVec = new double[] {refC1, refC2};

      for (int k = 0; k < 2; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e12).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.vomma(1.e-12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resC2 = BlackFormulaRepository.vomma(1.e-12, 1.e12, TIME_TO_EXPIRY, vol);
      double resC3 = BlackFormulaRepository.vomma(1.e12, 1.e-12, TIME_TO_EXPIRY, vol);
      double resP3 = BlackFormulaRepository.vomma(1.e12, 1.e12, TIME_TO_EXPIRY, vol);

      double refC1 = BlackFormulaRepository.vomma(0., 0., TIME_TO_EXPIRY, vol);
      double refC2 = BlackFormulaRepository.vomma(0., inf, TIME_TO_EXPIRY, vol);
      double refC3 = BlackFormulaRepository.vomma(inf, 0., TIME_TO_EXPIRY, vol);
      double refP3 = BlackFormulaRepository.vomma(inf, inf, TIME_TO_EXPIRY, vol);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e12) {
          assertThat(resVec[k] > 1.e12).isTrue();
        } else {
          if (refVec[k] < -1.e12) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-12).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-12));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vomma(1.e-12, strike, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.vomma(1.e-12, strike, 1.e24, vol);
        double resC3 = BlackFormulaRepository.vomma(1.e12, strike, 1.e-24, vol);
        double resP3 = BlackFormulaRepository.vomma(1.e12, strike, 1.e24, vol);

        double refC1 = BlackFormulaRepository.vomma(0., strike, 0., vol);
        double refC2 = BlackFormulaRepository.vomma(0., strike, inf, vol);
        double refC3 = BlackFormulaRepository.vomma(inf, strike, 0., vol);
        double refP3 = BlackFormulaRepository.vomma(inf, strike, inf, vol);

        double[] resVec = new double[] {resC1, resC2, resC3, resP3};
        double[] refVec = new double[] {refC1, refC2, refC3, refP3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double strike = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.vomma(1.e-12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.vomma(1.e-12, strike, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.vomma(1.e12, strike, TIME_TO_EXPIRY, 1.e-12);
      double resP3 = BlackFormulaRepository.vomma(1.e12, strike, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.vomma(0., strike, TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.vomma(0., strike, TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.vomma(inf, strike, TIME_TO_EXPIRY, 0.);
      double refP3 = BlackFormulaRepository.vomma(inf, strike, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double forward = STRIKES_INPUT[i];
        double vol = VOLS[j];
        double resC1 = BlackFormulaRepository.vomma(forward, 1.e-12, 1.e-24, vol);
        double resC2 = BlackFormulaRepository.vomma(forward, 1.e-12, 1.e24, vol);
        double resC3 = BlackFormulaRepository.vomma(forward, 1.e12, 1.e-24, vol);
        double resP3 = BlackFormulaRepository.vomma(forward, 1.e12, 1.e24, vol);

        double refC1 = BlackFormulaRepository.vomma(forward, 0., 0., vol);
        double refC2 = BlackFormulaRepository.vomma(forward, 0., inf, vol);
        double refC3 = BlackFormulaRepository.vomma(forward, inf, 0., vol);
        double refP3 = BlackFormulaRepository.vomma(forward, inf, inf, vol);

        double[] resVec = new double[] {resC1, resC2, resC3, resP3};
        double[] refVec = new double[] {refC1, refC2, refC3, refP3};

        for (int k = 0; k < 4; ++k) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      double forward = STRIKES_INPUT[i];
      double resC1 = BlackFormulaRepository.vomma(forward, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.vomma(forward, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.vomma(forward, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      ;
      double resP3 = BlackFormulaRepository.vomma(forward, 1.e12, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.vomma(forward, 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.vomma(forward, 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.vomma(forward, inf, TIME_TO_EXPIRY, 0.);
      double refP3 = BlackFormulaRepository.vomma(forward, inf, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resC2, resC3, resP3};
      double[] refVec = new double[] {refC1, refC2, refC3, refP3};

      for (int k = 0; k < 4; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (refVec[k] == 0.) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

    for (int j = 0; j < nVols; ++j) {
      double vol = VOLS[j];
      double resC1 = BlackFormulaRepository.vomma(1.e-12, 1.e-12, 1.e-24, vol);
      double resC2 = BlackFormulaRepository.vomma(1.e-12, 1.e-12, 1.e24, vol);
      double resC3 = BlackFormulaRepository.vomma(1.e-12, 1.e12, 1.e-24, vol);
      double resP1 = BlackFormulaRepository.vomma(1.e-12, 1.e-14, 1.e-24, vol);
      double resP2 = BlackFormulaRepository.vomma(1.e-12, 1.e-12, 1.e24, vol);
      double resP3 = BlackFormulaRepository.vomma(1.e-12, 1.e12, 1.e-24, vol);
      double resC4 = BlackFormulaRepository.vomma(1.e12, 1.e-12, 1.e-24, vol);
      double resP4 = BlackFormulaRepository.vomma(1.e12, 1.e-12, 1.e-24, vol);
      double resC5 = BlackFormulaRepository.vomma(FORWARD, FORWARD * (1. + 1.e-14), 1.e-24, vol);
      double resP5 = BlackFormulaRepository.vomma(FORWARD, FORWARD * (1. + 1.e-14), 1.e-24, vol);
      double resC6 = BlackFormulaRepository.vomma(1.e12, 1.e12, 1.e24, vol);
      double resP6 = BlackFormulaRepository.vomma(1.e12, 1.e-12, 1.e24, vol);
      double resC7 = BlackFormulaRepository.vomma(1.e12, 1.e12, 1.e-24, vol);
      double resP7 = BlackFormulaRepository.vomma(1.e-12, 1.e12, 1.e24, vol);

      double refC1 = BlackFormulaRepository.vomma(0., 0., 0., vol);
      double refC2 = BlackFormulaRepository.vomma(0., 0., inf, vol);
      double refC3 = BlackFormulaRepository.vomma(0., inf, 0., vol);
      double refP1 = BlackFormulaRepository.vomma(0., 0., 0., vol);
      double refP2 = BlackFormulaRepository.vomma(0., 0., inf, vol);
      double refP3 = BlackFormulaRepository.vomma(0., inf, 0., vol);
      double refC4 = BlackFormulaRepository.vomma(inf, 0., 0., vol);
      double refP4 = BlackFormulaRepository.vomma(inf, 0., 0., vol);
      double refC5 = BlackFormulaRepository.vomma(FORWARD, FORWARD, 0., vol);
      double refP5 = BlackFormulaRepository.vomma(FORWARD, FORWARD, 0., vol);
      double refC6 = BlackFormulaRepository.vomma(inf, inf, inf, vol);
      double refP6 = BlackFormulaRepository.vomma(inf, 0., inf, vol);
      double refC7 = BlackFormulaRepository.vomma(inf, inf, 0., vol);
      double refP7 = BlackFormulaRepository.vomma(0., inf, inf, vol);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7};

      for (int k = 0; k < 14; ++k) {
        if (k != 12) {  // ref val
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e12).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (refVec[k] == 0.) {
                assertThat(Math.abs(resVec[k]) < 1.e-9).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.vomma(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC2 = BlackFormulaRepository.vomma(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC3 = BlackFormulaRepository.vomma(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP1 = BlackFormulaRepository.vomma(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resP2 = BlackFormulaRepository.vomma(1.e-12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resP3 = BlackFormulaRepository.vomma(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resC4 = BlackFormulaRepository.vomma(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resP4 = BlackFormulaRepository.vomma(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e-12);
      double resC5 = BlackFormulaRepository.vomma(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12);
      double resP5 = BlackFormulaRepository.vomma(FORWARD, FORWARD * (1. + 1.e-12), TIME_TO_EXPIRY, 1.e-12);
      double resC6 = BlackFormulaRepository.vomma(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e12);
      double resP6 = BlackFormulaRepository.vomma(1.e12, 1.e-12, TIME_TO_EXPIRY, 1.e12);
      double resC7 = BlackFormulaRepository.vomma(1.e12, 1.e12, TIME_TO_EXPIRY, 1.e-12);
      double resP7 = BlackFormulaRepository.vomma(1.e-12, 1.e12, TIME_TO_EXPIRY, 1.e12);

      double refC1 = BlackFormulaRepository.vomma(0., 0., TIME_TO_EXPIRY, 0.);
      double refC2 = BlackFormulaRepository.vomma(0., 0., TIME_TO_EXPIRY, inf);
      double refC3 = BlackFormulaRepository.vomma(0., inf, TIME_TO_EXPIRY, 0.);
      double refP1 = BlackFormulaRepository.vomma(0., 0., TIME_TO_EXPIRY, 0.);
      double refP2 = BlackFormulaRepository.vomma(0., 0., TIME_TO_EXPIRY, inf);
      double refP3 = BlackFormulaRepository.vomma(0., inf, TIME_TO_EXPIRY, 0.);
      double refC4 = BlackFormulaRepository.vomma(inf, 0., TIME_TO_EXPIRY, 0.);
      double refP4 = BlackFormulaRepository.vomma(inf, 0., TIME_TO_EXPIRY, 0.);
      double refC5 = BlackFormulaRepository.vomma(FORWARD, FORWARD, TIME_TO_EXPIRY, 0.);
      double refP5 = BlackFormulaRepository.vomma(FORWARD, FORWARD, TIME_TO_EXPIRY, 0.);
      double refC6 = BlackFormulaRepository.vomma(inf, inf, TIME_TO_EXPIRY, inf);
      double refP6 = BlackFormulaRepository.vomma(inf, 0., TIME_TO_EXPIRY, inf);
      double refC7 = BlackFormulaRepository.vomma(inf, inf, TIME_TO_EXPIRY, 0.);
      double refP7 = BlackFormulaRepository.vomma(0., inf, TIME_TO_EXPIRY, inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC5, resP5,
          resC6, resP6, resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC5, refP5,
          refC6, refP6, refC7, refP7};

      for (int k = 2; k < 12; ++k) {
        if (k != 8 && k != 9) {
          if (refVec[k] > 1.e10) {
            assertThat(resVec[k] > 1.e10).isTrue();
          } else {
            if (refVec[k] < -1.e10) {
              assertThat(resVec[k] < -1.e10).isTrue();
            } else {
              if (Math.abs(refVec[k]) < 1.e-10) {
                assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
              } else {
                assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
              }
            }
          }
        }
      }
    }

    {
      double resC1 = BlackFormulaRepository.vomma(FORWARD, 1.e-12, 1.e-12, 1.e-12);
      double resC2 = BlackFormulaRepository.vomma(FORWARD, 1.e-12, 1.e-12, 1.e12);
      double resC3 = BlackFormulaRepository.vomma(FORWARD, 1.e-12, 1.e12, 1.e-12);
      double resP1 = BlackFormulaRepository.vomma(FORWARD, 1.e-12, 1.e-12, 1.e-12);
      double resP2 = BlackFormulaRepository.vomma(FORWARD, 1.e-12, 1.e-12, 1.e12);
      double resP3 = BlackFormulaRepository.vomma(FORWARD, 1.e-12, 1.e12, 1.e-12);
      double resC4 = BlackFormulaRepository.vomma(FORWARD, 1.e12, 1.e-12, 1.e-12);
      double resP4 = BlackFormulaRepository.vomma(FORWARD, 1.e12, 1.e-12, 1.e-12);
      double resC6 = BlackFormulaRepository.vomma(FORWARD, 1.e12, 1.e12, 1.e12);
      double resP6 = BlackFormulaRepository.vomma(FORWARD, 1.e12, 1.e-12, 1.e12);
      double resC7 = BlackFormulaRepository.vomma(FORWARD, 1.e12, 1.e12, 1.e-12);
      double resP7 = BlackFormulaRepository.vomma(FORWARD, 1.e-12, 1.e12, 1.e12);

      double refC1 = BlackFormulaRepository.vomma(FORWARD, 0., 0., 0.);
      double refC2 = BlackFormulaRepository.vomma(FORWARD, 0., 0., inf);
      double refC3 = BlackFormulaRepository.vomma(FORWARD, 0., inf, 0.);
      double refP1 = BlackFormulaRepository.vomma(FORWARD, 0., 0., 0.);
      double refP2 = BlackFormulaRepository.vomma(FORWARD, 0., 0., inf);
      double refP3 = BlackFormulaRepository.vomma(FORWARD, 0., inf, 0.);
      double refC4 = BlackFormulaRepository.vomma(FORWARD, inf, 0., 0.);
      double refP4 = BlackFormulaRepository.vomma(FORWARD, inf, 0., 0.);
      double refC6 = BlackFormulaRepository.vomma(FORWARD, inf, inf, inf);
      double refP6 = BlackFormulaRepository.vomma(FORWARD, inf, 0., inf);
      double refC7 = BlackFormulaRepository.vomma(FORWARD, inf, inf, 0.);
      double refP7 = BlackFormulaRepository.vomma(FORWARD, 0., inf, inf);

      double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3, resC4, resP4, resC6, resP6,
          resC7, resP7};
      double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3, refC4, refP4, refC6, refP6,
          refC7, refP7};

      for (int k = 2; k < 12; ++k) {
        if (refVec[k] > 1.e10) {
          assertThat(resVec[k] > 1.e10).isTrue();
        } else {
          if (refVec[k] < -1.e10) {
            assertThat(resVec[k] < -1.e10).isTrue();
          } else {
            if (Math.abs(refVec[k]) < 1.e-10) {
              assertThat(Math.abs(resVec[k]) < 1.e-10).isTrue();
            } else {
              assertThat(refVec[k]).isCloseTo(resVec[k], offset(Math.abs(refVec[k]) * 1.e-10));
            }
          }
        }
      }
    }

  }

  @Test
  public void negativeVolErrorVommaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.vomma(FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5));
  }

  @Test
  public void negativeFwdErrorVommaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.vomma(-FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeStrikeErrorVommaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.vomma(FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1]));
  }

  @Test
  public void negativeTimeErrorVommaTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.vomma(FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1]));
  }

  /*
   * Volga test
   */
  @Test
  public void volgaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];

        double volga = BlackFormulaRepository.volga(FORWARD, strike, TIME_TO_EXPIRY, vol);
        double vomma = BlackFormulaRepository.vomma(strike, FORWARD, TIME_TO_EXPIRY, vol);
        assertThat(vomma).isCloseTo(volga, offset(Math.abs(vomma) * 1.e-8));

      }
    }
  }

  /*
   * Implied vol tests
   */
  @Test
  public void volRecoveryTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];

        double cPrice = BlackFormulaRepository.price(FORWARD, strike, TIME_TO_EXPIRY, vol, true);
        double pPrice = BlackFormulaRepository.price(FORWARD, strike, TIME_TO_EXPIRY, vol, false);
        double cRes = BlackFormulaRepository.impliedVolatility(cPrice, FORWARD, strike, TIME_TO_EXPIRY, true);
        double pRes = BlackFormulaRepository.impliedVolatility(pPrice, FORWARD, strike, TIME_TO_EXPIRY, false);
        assertThat(vol).isCloseTo(cRes, offset(Math.abs(vol) * 1.e-8));
        assertThat(vol).isCloseTo(pRes, offset(Math.abs(vol) * 1.e-8));

      }
    }
  }

  @Test
  public void impliedVolTest() {
    double vol = 0.4342; // Deliberately picked an arbitrary vol
    double t = 0.1;
    double f = 0.01;
    double p = 4.1;
    double ivCall = 0;
    double ivPut = 0;
    double iv = 0;

    for (int i = 0; i < 100; i++) {
      double k = 0.004 + 0.022 * i / 100.;
      double cPrice = p * BlackFormulaRepository.price(f, k, t, vol, true);
      double pPrice = p * BlackFormulaRepository.price(f, k, t, vol, false);

      ivCall = BlackFormulaRepository.impliedVolatility(cPrice / p, f, k, t, true);
      ivPut = BlackFormulaRepository.impliedVolatility(pPrice / p, f, k, t, false);
      boolean isCall = k > f;
      double otmP = (isCall ? cPrice : pPrice) / p;
      iv = BlackFormulaRepository.impliedVolatility(otmP, f, k, t, isCall);

      // this is why we should compute OTM prices if an implied vol is required
      assertThat(vol).isCloseTo(ivCall, offset(5e-4));
      assertThat(vol).isCloseTo(ivPut, offset(2e-3));
      assertThat(vol).isCloseTo(iv, offset(1e-9));
    }
  }

  /* Implied vol computation with limit cases option price near intrinsic price */
  @Test
  public void impliedVol_near_intrinsic() {
    double optionPrice = 19.98d;
    double forward = 79.98d;
    double strike = 60.00d;
    double timeToExpiry = 0.0d;
    double iv = BlackFormulaRepository.impliedVolatility(optionPrice, forward, strike, timeToExpiry, true);
    assertThat(iv).isCloseTo(0.0, offset(1e-12));
    ValueDerivatives ivAdj = BlackFormulaRepository
        .impliedVolatilityAdjoint(optionPrice, forward, strike, timeToExpiry, true);
    assertThat(ivAdj.getValue()).isCloseTo(0.0, offset(1e-12));
    assertThat(ivAdj.getDerivatives().size()).isEqualTo(1);
    assertThat(ivAdj.getDerivative(0)).isCloseTo(0.0, offset(1e-12));
  }

  @Test
  public void implied_volatility_adjoint() {
    double vol = 0.4342; // Deliberately picked an arbitrary vol
    double t = 0.1;
    double f = 10.0d;
    double shiftFd = 1.0E-6;
    double toleranceVol = 1.0E-3;
    double toleranceVolDelta = 1.0E-3;
    int nbPoints = 25;
    for (int i = 0; i <= nbPoints; i++) {
      double k = 0.75 * f + i * 0.5 * f / 25;
      double cPrice = BlackFormulaRepository.price(f, k, t, vol, true);
      double pPrice = BlackFormulaRepository.price(f, k, t, vol, false);
      ValueDerivatives ivCallAdj = BlackFormulaRepository.impliedVolatilityAdjoint(cPrice, f, k, t, true);
      ValueDerivatives ivPutAdj = BlackFormulaRepository.impliedVolatilityAdjoint(pPrice, f, k, t, false);
      assertThat(ivCallAdj.getValue()).isCloseTo(vol, offset(toleranceVol));
      assertThat(ivPutAdj.getValue()).isCloseTo(vol, offset(toleranceVol));
      double ivCallP = BlackFormulaRepository.impliedVolatility(cPrice + shiftFd, f, k, t, true);
      double ivCallM = BlackFormulaRepository.impliedVolatility(cPrice - shiftFd, f, k, t, true);
      double ivCallDerivative = (ivCallP - ivCallM) / (2 * shiftFd);
      assertThat(ivCallAdj.getDerivative(0)).isCloseTo(ivCallDerivative, offset(toleranceVolDelta));
      assertThat(ivPutAdj.getDerivative(0)).isCloseTo(ivCallAdj.getDerivative(0), offset(toleranceVolDelta));
      // Vega and its inverse are the same for call and puts
    }

  }

  @Test
  public void negativePriceErrorImpliedVolatilityTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.impliedVolatility(-10., FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, true));
  }

  @Test
  public void negativeFwdErrorImpliedVolatilityTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.impliedVolatility(10., -FORWARD, STRIKES_INPUT[1], TIME_TO_EXPIRY, true));
  }

  @Test
  public void negativeStrikeErrorImpliedVolatilityTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.impliedVolatility(10., FORWARD, -STRIKES_INPUT[1], TIME_TO_EXPIRY, true));
  }

  @Test
  public void negativeTimeErrorImpliedVolatilityTest() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.impliedVolatility(10., FORWARD, STRIKES_INPUT[1], -TIME_TO_EXPIRY, true));
  }

  @Test
  public void volInitialGuessTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];

        double zero = BlackFormulaRepository.impliedVolatility(0., FORWARD, strike, TIME_TO_EXPIRY, vol);
        double atm = BlackFormulaRepository.impliedVolatility(Math.pow(strike, 0.6), strike, strike,
            TIME_TO_EXPIRY, vol);
        assertThat(0.).isCloseTo(zero, offset(Math.abs(vol) * 1.e-13));
        assertThat(NORMAL.getInverseCDF(0.5 * (Math.pow(strike, 0.6) / strike + 1)) * 2 / Math.sqrt(TIME_TO_EXPIRY)).isCloseTo(atm, offset(1.e-13));

      }
    }
  }

  /*
   * Implied strike tests
   */
  @Test
  public void strikeRecoveryTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        double strike = STRIKES_INPUT[i];
        double vol = VOLS[j];

        double cDelta = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, vol, true);
        double pdelta = BlackFormulaRepository.delta(FORWARD, strike, TIME_TO_EXPIRY, vol, false);
        double cRes = BlackFormulaRepository.impliedStrike(cDelta, true, FORWARD, TIME_TO_EXPIRY, vol);
        double pRes = BlackFormulaRepository.impliedStrike(pdelta, false, FORWARD, TIME_TO_EXPIRY, vol);
        assertThat(strike).isCloseTo(cRes, offset(Math.abs(strike) * 1.e-8));
        assertThat(strike).isCloseTo(pRes, offset(Math.abs(strike) * 1.e-8));
      }
    }
  }

  /*
   * Tests below are for debugging
   */
  @Disabled
  public void sampleTest() {
    double inf = Double.POSITIVE_INFINITY;
    double resC0 = BlackFormulaRepository.crossGamma(inf, FORWARD, 0.01, VOLS[2]);
    double resC00 = BlackFormulaRepository.crossGamma(1.e12, FORWARD, 0.01, VOLS[2]);
    System.out.println(resC0 + "\t" + resC00);
    System.out.println("\n");
    double resP0 = BlackFormulaRepository.crossGamma(inf, FORWARD, 0.01, VOLS[2]);
    double resP00 = BlackFormulaRepository.crossGamma(1.e12, FORWARD, 0.01, VOLS[2]);
    System.out.println(resP0 + "\t" + resP00);
    System.out.println("\n");
    double resC1 = BlackFormulaRepository.crossGamma(FORWARD * 0.9, inf, 0.001, VOLS[2]);
    double resC2 = BlackFormulaRepository.crossGamma(FORWARD * 0.9, 1.e12, 0.01, VOLS[2]);
    System.out.println(resC1 + "\t" + resC2);
    System.out.println("\n");
    double resP1 = BlackFormulaRepository.crossGamma(FORWARD * 0.9, inf, 0.01, VOLS[2]);
    double resP2 = BlackFormulaRepository.crossGamma(FORWARD * 0.9, 1.e12, 0.01, VOLS[2]);
    System.out.println(resP1 + "\t" + resP2);
    System.out.println("\n");
    double resC3 = BlackFormulaRepository.crossGamma(FORWARD * 0.9, FORWARD, inf, VOLS[2]);
    double resC4 = BlackFormulaRepository.crossGamma(FORWARD * 0.9, FORWARD, 1.e12, VOLS[2]);
    System.out.println(resC3 + "\t" + resC4);
    System.out.println("\n");
    double resP3 = BlackFormulaRepository.crossGamma(FORWARD * 0.9, FORWARD, inf, VOLS[2]);
    double resP4 = BlackFormulaRepository.crossGamma(FORWARD * 0.9, FORWARD, 1.e12, VOLS[2]);
    System.out.println(resP3 + "\t" + resP4);
    System.out.println("\n");
    double resC5 = BlackFormulaRepository.crossGamma(FORWARD * 0.9, FORWARD, 1.e-12, VOLS[2]);
    double resC6 = BlackFormulaRepository.crossGamma(FORWARD * 0.9, FORWARD, 1.e-11, VOLS[2]);
    System.out.println(resC5 + "\t" + resC6);
    System.out.println("\n");
    double resP5 = BlackFormulaRepository.crossGamma(0., FORWARD, 0.01, VOLS[2]);
    double resP6 = BlackFormulaRepository.crossGamma(1.e-12, FORWARD, 0.01, VOLS[2]);
    System.out.println(resP5 + "\t" + resP6);
    System.out.println("\n");
    double resC7 = BlackFormulaRepository.crossGamma(0., 0., 0.01, VOLS[2]);
    double resC8 = BlackFormulaRepository.crossGamma(1.e-12, 1.e-12, 0.01, VOLS[2]);
    System.out.println(resC7 + "\t" + resC8);
    System.out.println("\n");
    double resP7 = BlackFormulaRepository.crossGamma(FORWARD, FORWARD, 0., VOLS[2]);
    double resP8 = BlackFormulaRepository.crossGamma(FORWARD, FORWARD, 1.e-60, VOLS[2]);
    System.out.println(resP7 + "\t" + resP8);
    System.out.println("\n");
    double resP9 = BlackFormulaRepository.crossGamma(FORWARD, 0., 0.01, VOLS[2]);
    double resP10 = BlackFormulaRepository.crossGamma(FORWARD, 1.e-12, 0.01, VOLS[2]);
    System.out.println(resP9 + "\t" + resP10);
    System.out.println("\n");
    double resC11 = BlackFormulaRepository.crossGamma(0., 0., 0., VOLS[2]);
    double resC12 = BlackFormulaRepository.crossGamma(1.e-12, 1.e-12, 1.e-20, VOLS[2]);
    System.out.println(resC11 + "\t" + resC12);
    System.out.println("\n");
    double resC13 = BlackFormulaRepository.crossGamma(FORWARD, 0., 0., VOLS[2]);
    double resC14 = BlackFormulaRepository.crossGamma(FORWARD, 1.e-12, 1.e-20, VOLS[2]);
    System.out.println(resC13 + "\t" + resC14);
    System.out.println("\n");
    double resC15 = BlackFormulaRepository.crossGamma(0., FORWARD, 0., VOLS[2]);
    double resC16 = BlackFormulaRepository.crossGamma(1.e-12, FORWARD, 1.e-20, VOLS[2]);
    System.out.println(resC15 + "\t" + resC16);
    System.out.println("\n");
  }

  @Disabled
  public void sample2Test() {
    double inf = Double.POSITIVE_INFINITY;
    double resC0 = BlackFormulaRepository.price(inf, FORWARD, 0.01, VOLS[2], true);
    double resC00 = BlackFormulaRepository.price(1.e14, FORWARD, 0.01, VOLS[2], true);
    System.out.println(resC0 + "\t" + resC00);
    System.out.println("\n");
    double resP0 = BlackFormulaRepository.price(inf, FORWARD, 0.01, VOLS[2], false);
    double resP00 = BlackFormulaRepository.price(1.e12, FORWARD, 0.01, VOLS[2], false);
    System.out.println(resP0 + "\t" + resP00);
    System.out.println("\n");
    double resC1 = BlackFormulaRepository.price(FORWARD * 0.9, inf, 0.001, VOLS[2], true);
    double resC2 = BlackFormulaRepository.price(FORWARD * 0.9, 1.e12, 0.01, VOLS[2], true);
    System.out.println(resC1 + "\t" + resC2);
    System.out.println("\n");
    double resP1 = BlackFormulaRepository.price(FORWARD * 0.9, inf, 0.01, VOLS[2], false);
    double resP2 = BlackFormulaRepository.price(FORWARD * 0.9, 1.e12, 0.01, VOLS[2], false);
    System.out.println(resP1 + "\t" + resP2);
    System.out.println("\n");
    double resC3 = BlackFormulaRepository.price(FORWARD * 0.9, FORWARD, inf, VOLS[2], true);
    double resC4 = BlackFormulaRepository.price(FORWARD * 0.9, FORWARD, 1.e12, VOLS[2], true);
    System.out.println(resC3 + "\t" + resC4);
    System.out.println("\n");
    double resP3 = BlackFormulaRepository.price(FORWARD * 0.9, FORWARD, inf, VOLS[2], false);
    double resP4 = BlackFormulaRepository.price(FORWARD * 0.9, FORWARD, 1.e12, VOLS[2], false);
    System.out.println(resP3 + "\t" + resP4);
    System.out.println("\n");
    double resC5 = BlackFormulaRepository.price(FORWARD * 0.9, FORWARD, 1.e-12, VOLS[2], true);
    double resC6 = BlackFormulaRepository.price(FORWARD * 0.9, FORWARD, 1.e-11, VOLS[2], true);
    System.out.println(resC5 + "\t" + resC6);
    System.out.println("\n");
    double resP5 = BlackFormulaRepository.price(0., FORWARD, 0.01, VOLS[2], false);
    double resP6 = BlackFormulaRepository.price(1.e-12, FORWARD, 0.01, VOLS[2], false);
    System.out.println(resP5 + "\t" + resP6);
    System.out.println("\n");
    double resC7 = BlackFormulaRepository.price(0., 0., 0.01, VOLS[2], true);
    double resC8 = BlackFormulaRepository.price(1.e-12, 1.e-12, 0.01, VOLS[2], true);
    System.out.println(resC7 + "\t" + resC8);
    System.out.println("\n");
    double resP7 = BlackFormulaRepository.price(FORWARD, FORWARD, 0., VOLS[2], false);
    double resP8 = BlackFormulaRepository.price(FORWARD, FORWARD, 1.e-60, VOLS[2], false);
    System.out.println(resP7 + "\t" + resP8);
    System.out.println("\n");
    double resP9 = BlackFormulaRepository.price(FORWARD, 0., 0.01, VOLS[2], true);
    double resP10 = BlackFormulaRepository.price(FORWARD, 1.e-12, 0.01, VOLS[2], true);
    System.out.println(resP9 + "\t" + resP10);
    System.out.println("\n");
    double resC11 = BlackFormulaRepository.price(0., 0., 0., VOLS[2], false);
    double resC12 = BlackFormulaRepository.price(1.e-12, 1.e-12, 1.e-20, VOLS[2], false);
    System.out.println(resC11 + "\t" + resC12);
    System.out.println("\n");
    double resC13 = BlackFormulaRepository.price(FORWARD, 0., 0., VOLS[2], true);
    double resC14 = BlackFormulaRepository.price(FORWARD, 1.e-12, 1.e-20, VOLS[2], true);
    System.out.println(resC13 + "\t" + resC14);
    System.out.println("\n");
    double resC15 = BlackFormulaRepository.price(0., FORWARD, 0., VOLS[2], false);
    double resC16 = BlackFormulaRepository.price(1.e-12, FORWARD, 1.e-20, VOLS[2], false);
    System.out.println(resC15 + "\t" + resC16);
    System.out.println("\n");
    double resP17 = BlackFormulaRepository.price(FORWARD, 0., 0.01, VOLS[2], false);
    double resP18 = BlackFormulaRepository.price(FORWARD, 1.e-12, 0.01, VOLS[2], false);
    System.out.println(resP17 + "\t" + resP18);
    System.out.println("\n");
  }

  @Disabled
  public void sample3Test() {
    double inf = Double.POSITIVE_INFINITY;
    double resC0 = BlackFormulaRepository.theta(inf, FORWARD, 0.01, VOLS[2], true, 0.05);
    double resC00 = BlackFormulaRepository.theta(1.e14, FORWARD, 0.01, VOLS[2], true, 0.05);
    System.out.println(resC0 + "\t" + resC00);
    System.out.println("\n");
    double resP0 = BlackFormulaRepository.theta(inf, FORWARD, 0.01, VOLS[2], false, 0.05);
    double resP00 = BlackFormulaRepository.theta(1.e12, FORWARD, 0.01, VOLS[2], false, 0.05);
    System.out.println(resP0 + "\t" + resP00);
    System.out.println("\n");
    double resC1 = BlackFormulaRepository.theta(FORWARD * 0.9, inf, 0.001, VOLS[2], true, 0.05);
    double resC2 = BlackFormulaRepository.theta(FORWARD * 0.9, 1.e12, 0.01, VOLS[2], true, 0.05);
    System.out.println(resC1 + "\t" + resC2);
    System.out.println("\n");
    double resP1 = BlackFormulaRepository.theta(FORWARD * 0.9, inf, 0.01, VOLS[2], false, 0.05);
    double resP2 = BlackFormulaRepository.theta(FORWARD * 0.9, 1.e12, 0.01, VOLS[2], false, 0.05);
    System.out.println(resP1 + "\t" + resP2);
    System.out.println("\n");
    double resC3 = BlackFormulaRepository.theta(FORWARD * 0.9, FORWARD, inf, VOLS[2], true, 0.05);
    double resC4 = BlackFormulaRepository.theta(FORWARD * 0.9, FORWARD, 1.e12, VOLS[2], true, 0.05);
    System.out.println(resC3 + "\t" + resC4);
    System.out.println("\n");
    double resP3 = BlackFormulaRepository.theta(FORWARD * 0.9, FORWARD, inf, VOLS[2], false, 0.05);
    double resP4 = BlackFormulaRepository.theta(FORWARD * 0.9, FORWARD, 1.e12, VOLS[2], false, 0.05);
    System.out.println(resP3 + "\t" + resP4);
    System.out.println("\n");
    double resC5 = BlackFormulaRepository.theta(FORWARD * 0.9, FORWARD, 1.e-12, VOLS[2], true, 0.05);
    double resC6 = BlackFormulaRepository.theta(FORWARD * 0.9, FORWARD, 1.e-11, VOLS[2], true, 0.05);
    System.out.println(resC5 + "\t" + resC6);
    System.out.println("\n");
    double resP5 = BlackFormulaRepository.theta(0., FORWARD, 0.01, VOLS[2], false, 0.05);
    double resP6 = BlackFormulaRepository.theta(1.e-12, FORWARD, 0.01, VOLS[2], false, 0.05);
    System.out.println(resP5 + "\t" + resP6);
    System.out.println("\n");
    double resC7 = BlackFormulaRepository.theta(0., 0., 0.01, VOLS[2], true, 0.05);
    double resC8 = BlackFormulaRepository.theta(1.e-12, 1.e-12, 0.01, VOLS[2], true, 0.05);
    System.out.println(resC7 + "\t" + resC8);
    System.out.println("\n");
    double resP7 = BlackFormulaRepository.theta(FORWARD, FORWARD, 0., VOLS[2], false, 0.05);
    double resP8 = BlackFormulaRepository.theta(FORWARD, FORWARD, 1.e-60, VOLS[2], false, 0.05);
    System.out.println(resP7 + "\t" + resP8);
    System.out.println("\n");
    double resP9 = BlackFormulaRepository.theta(FORWARD, 0., 0.01, VOLS[2], true, 0.05);
    double resP10 = BlackFormulaRepository.theta(FORWARD, 1.e-12, 0.01, VOLS[2], true, 0.05);
    System.out.println(resP9 + "\t" + resP10);
    System.out.println("\n");
    double resC11 = BlackFormulaRepository.theta(0., 0., 0., VOLS[2], false, 0.05);
    double resC12 = BlackFormulaRepository.theta(1.e-12, 1.e-12, 1.e-24, VOLS[2], false, 0.05);
    System.out.println(resC11 + "\t" + resC12);
    System.out.println("\n");
    double resC13 = BlackFormulaRepository.theta(FORWARD, 0., 0., VOLS[2], true, 0.05);
    double resC14 = BlackFormulaRepository.theta(FORWARD, 1.e-12, 1.e-20, VOLS[2], true, 0.05);
    System.out.println(resC13 + "\t" + resC14);
    System.out.println("\n");
    double resC15 = BlackFormulaRepository.theta(0., FORWARD, 0., VOLS[2], false, 0.05);
    double resC16 = BlackFormulaRepository.theta(1.e-12, FORWARD, 1.e-20, VOLS[2], false, 0.05);
    System.out.println(resC15 + "\t" + resC16);
    System.out.println("\n");
    double resC17 = BlackFormulaRepository.theta(FORWARD, inf, 1., VOLS[2], false, 0.05);
    double resC18 = BlackFormulaRepository.theta(FORWARD, 1.e12, 1., VOLS[2], false, 0.05);
    System.out.println(resC17 + "\t" + resC18);
    System.out.println("\n");
    double resC19 = BlackFormulaRepository.theta(FORWARD * 0.9, FORWARD, 1., inf, false, 0.05);
    double resC20 = BlackFormulaRepository.theta(FORWARD * 0.9, FORWARD, 1., 1.e15, false, 0.05);
    System.out.println(resC19 + "\t" + resC20);
    System.out.println("\n");
  }

  @Disabled
  public void sample4Test() {
    double inf = Double.POSITIVE_INFINITY;
    double resC0 = BlackFormulaRepository.vomma(inf, FORWARD, 0.01, VOLS[2]);
    double resC00 = BlackFormulaRepository.vomma(1.e14, FORWARD, 0.01, VOLS[2]);
    System.out.println(resC0 + "\t" + resC00);
    System.out.println("\n");
    double resP0 = BlackFormulaRepository.vomma(inf, FORWARD, 0.01, VOLS[2]);
    double resP00 = BlackFormulaRepository.vomma(1.e12, FORWARD, 0.01, VOLS[2]);
    System.out.println(resP0 + "\t" + resP00);
    System.out.println("\n");
    double resC1 = BlackFormulaRepository.vomma(FORWARD * 0.9, inf, 0.001, VOLS[2]);
    double resC2 = BlackFormulaRepository.vomma(FORWARD * 0.9, 1.e12, 0.01, VOLS[2]);
    System.out.println(resC1 + "\t" + resC2);
    System.out.println("\n");
    double resP1 = BlackFormulaRepository.vomma(FORWARD * 0.9, inf, 0.01, VOLS[2]);
    double resP2 = BlackFormulaRepository.vomma(FORWARD * 0.9, 1.e12, 0.01, VOLS[2]);
    System.out.println(resP1 + "\t" + resP2);
    System.out.println("\n");
    double resC3 = BlackFormulaRepository.vomma(FORWARD * 0.9, FORWARD, inf, VOLS[2]);
    double resC4 = BlackFormulaRepository.vomma(FORWARD * 0.9, FORWARD, 1.e12, VOLS[2]);
    System.out.println(resC3 + "\t" + resC4);
    System.out.println("\n");
    double resP3 = BlackFormulaRepository.vomma(FORWARD * 0.9, FORWARD, inf, VOLS[2]);
    double resP4 = BlackFormulaRepository.vomma(FORWARD * 0.9, FORWARD, 1.e12, VOLS[2]);
    System.out.println(resP3 + "\t" + resP4);
    System.out.println("\n");
    double resC5 = BlackFormulaRepository.vomma(FORWARD * 0.9, FORWARD, 1.e-12, VOLS[2]);
    double resC6 = BlackFormulaRepository.vomma(FORWARD * 0.9, FORWARD, 1.e-11, VOLS[2]);
    System.out.println(resC5 + "\t" + resC6);
    System.out.println("\n");
    double resP5 = BlackFormulaRepository.vomma(0., FORWARD, 0.01, VOLS[2]);
    double resP6 = BlackFormulaRepository.vomma(1.e-12, FORWARD, 0.01, VOLS[2]);
    System.out.println(resP5 + "\t" + resP6);
    System.out.println("\n");
    double resC7 = BlackFormulaRepository.vomma(0., 0., 0.01, VOLS[2]);
    double resC8 = BlackFormulaRepository.vomma(1.e-12, 1.e-12, 0.01, VOLS[2]);
    System.out.println(resC7 + "\t" + resC8);
    System.out.println("\n");
    double resP7 = BlackFormulaRepository.vomma(FORWARD, FORWARD, 0., VOLS[2]);
    double resP8 = BlackFormulaRepository.vomma(FORWARD, FORWARD, 1.e-60, VOLS[2]);
    System.out.println(resP7 + "\t" + resP8);
    System.out.println("\n");
    double resP9 = BlackFormulaRepository.vomma(FORWARD, 0., 0.01, VOLS[2]);
    double resP10 = BlackFormulaRepository.vomma(FORWARD, 1.e-12, 0.01, VOLS[2]);
    System.out.println(resP9 + "\t" + resP10);
    System.out.println("\n");
    double resC11 = BlackFormulaRepository.vomma(0., 0., 0., VOLS[2]);
    double resC12 = BlackFormulaRepository.vomma(1.e-12, 1.e-12, 1.e-60, VOLS[2]);
    System.out.println(resC11 + "\t" + resC12);
    System.out.println("\n");
    double resC13 = BlackFormulaRepository.vomma(FORWARD, 0., 0., VOLS[2]);
    double resC14 = BlackFormulaRepository.vomma(FORWARD, 1.e-12, 1.e-12, VOLS[2]);
    System.out.println(resC13 + "\t" + resC14);
    System.out.println("\n");
    double resC15 = BlackFormulaRepository.vomma(0., FORWARD, 0., VOLS[2]);
    double resC16 = BlackFormulaRepository.vomma(1.e-12, FORWARD, 1.e-20, VOLS[2]);
    System.out.println(resC15 + "\t" + resC16);
    System.out.println("\n");
    double resC17 = BlackFormulaRepository.vomma(FORWARD, inf, 1., VOLS[2]);
    double resC18 = BlackFormulaRepository.vomma(FORWARD, 1.e12, 1., VOLS[2]);
    System.out.println(resC17 + "\t" + resC18);
    System.out.println("\n");
    double resC19 = BlackFormulaRepository.vomma(FORWARD * 0.9, FORWARD, 1., inf);
    double resC20 = BlackFormulaRepository.vomma(FORWARD * 0.9, FORWARD, 1., 1.e15);
    System.out.println(resC19 + "\t" + resC20);
    System.out.println("\n");
  }

  @Disabled
  public void sTest() {

    double forward = 140.;
    double strike = 140 + 1.e-10;
    double lognormalVol = 1.e-26;
    double rootT = 2.;

    double d1 = Math.log(forward / strike) / lognormalVol / rootT + 0.5 * lognormalVol * rootT;
    double d2 = Math.log(forward / strike) / lognormalVol / rootT - 0.5 * lognormalVol * rootT;
    System.out.println((-d2 * NORMAL.getPDF(d1) / lognormalVol));

    forward = 140.;
    strike = 140.;
    lognormalVol = 0.;
    d1 = Math.log(forward / strike) / lognormalVol / rootT + 0.5 * lognormalVol * rootT;
    System.out.println((-d2 * NORMAL.getPDF(d1) / lognormalVol));
  }

  //-------------------------------------------------------------------------
  // This test demonstrates why it is a bad idea to use quadrature methods for non-smooth functions
  // Test was originally in GaussianQuadratureIntegrator1DTest but moved here due to BlackFormulaRepository dependency
  @Test
  public void testBlackFormula() {
    double fwd = 5;
    double strike = 6.5;
    double t = 1.5;
    double vol = 0.35;
    double expected = BlackFormulaRepository.price(fwd, strike, t, vol, true);

    Function<Double, Double> func = getBlackIntergrand(fwd, strike, t, vol);

    Function<Double, Double> fullIntergrand = new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        return func.apply(x) * Math.exp(-x * x);
      }
    };

    RungeKuttaIntegrator1D rk = new RungeKuttaIntegrator1D(1e-15);
    double resRK = rk.integrate(fullIntergrand, 0., 10.); //The strike > fwd, so can start the integration at z=0 (i.e. s = fwd)
    assertThat(expected).as("Runge Kutta").isCloseTo(resRK, offset(1e-15));

    GaussHermiteQuadratureIntegrator1D gh = new GaussHermiteQuadratureIntegrator1D(40);
    double resGH = gh.integrateFromPolyFunc(func);
    assertThat(expected).as("Gauss Hermite").isCloseTo(resGH, offset(1e-2)); //terrible accuracy even with 40 points
  }

  private Function<Double, Double> getBlackIntergrand(double fwd, double k, double t, double vol) {
    double rootPI = Math.sqrt(Math.PI);
    double sigmaSqrTO2 = vol * vol * t / 2;
    double sigmaRoot2T = vol * Math.sqrt(2 * t);

    return new Function<Double, Double>() {
      @Override
      public Double apply(Double x) {
        double s = fwd * Math.exp(-sigmaSqrTO2 + sigmaRoot2T * x);
        return Math.max(s - k, 0) / rootPI;
      }
    };
  }

  private static final int N = 10;
  private static final double[] STRIKES = new double[N];
  private static final double[] STRIKES_ATM = new double[N];
  private static final double[] SIGMA_NORMAL = new double[N];
  static {
    for (int i = 0; i < 10; i++) {
      STRIKES[i] = FORWARD - 40.0d * (1.0d - 2.0d / N * i);
      STRIKES_ATM[i] = FORWARD + (-0.5d * N + i) / 100.0d;
      SIGMA_NORMAL[i] = 15.0 + i / 10.0d;
    }
  }
  private static final double TOLERANCE_PRICE = 1.0E-6;
  private static final double TOLERANCE_VOL_DELTA = 1.0E-8;

  @Test
  public void wrong_strike() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.impliedVolatilityFromNormalApproximated(FORWARD, -1.0d, TIME_TO_EXPIRY, 0.20d));
  }

  @Test
  public void wrong_forward() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.impliedVolatilityFromNormalApproximated(-1.0d, FORWARD, TIME_TO_EXPIRY, 0.20d));
  }

  @Test
  public void wrong_strike2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.impliedVolatilityFromNormalApproximated2(FORWARD, -1.0d, TIME_TO_EXPIRY, 0.20d));
  }

  @Test
  public void wrong_forward2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BlackFormulaRepository.impliedVolatilityFromNormalApproximated2(-1.0d, FORWARD, TIME_TO_EXPIRY, 0.20d));
  }

  @Test
  public void price_comparison_normal() {
    priceCheck(STRIKES);
    priceCheck(STRIKES_ATM);
  }

  private void priceCheck(double[] strikes) {
    for (int i = 0; i < N; i++) {
      double ivBlackComputed = BlackFormulaRepository
          .impliedVolatilityFromNormalApproximated(FORWARD, strikes[i], TIME_TO_EXPIRY, SIGMA_NORMAL[i]);
      double priceBlackComputed = BlackFormulaRepository
          .price(FORWARD, strikes[i], TIME_TO_EXPIRY, ivBlackComputed, true);
      double priceNormal = NormalFormulaRepository
          .price(FORWARD, strikes[i], TIME_TO_EXPIRY, SIGMA_NORMAL[i], CALL);
      assertThat(priceNormal).isCloseTo(priceBlackComputed, offset(TOLERANCE_PRICE));
    }
  }

  @Test
  public void implied_volatility_from_normal_adjoint() {
    double shiftFd = 1.0E-6;
    for (int i = 0; i < N; i++) {
      double ivBlackComputed = BlackFormulaRepository
          .impliedVolatilityFromNormalApproximated(FORWARD, STRIKES[i], TIME_TO_EXPIRY, SIGMA_NORMAL[i]);
      ValueDerivatives ivBlackAdj = BlackFormulaRepository
          .impliedVolatilityFromNormalApproximatedAdjoint(FORWARD, STRIKES[i], TIME_TO_EXPIRY, SIGMA_NORMAL[i]);
      assertThat(ivBlackComputed).isCloseTo(ivBlackAdj.getValue(), offset(TOLERANCE_1));
      assertThat(1).isEqualTo(ivBlackAdj.getDerivatives().size());
      double ivBlackComputedP = BlackFormulaRepository
          .impliedVolatilityFromNormalApproximated(FORWARD, STRIKES[i], TIME_TO_EXPIRY, SIGMA_NORMAL[i] + shiftFd);
      double ivBlackComputedM = BlackFormulaRepository
          .impliedVolatilityFromNormalApproximated(FORWARD, STRIKES[i], TIME_TO_EXPIRY, SIGMA_NORMAL[i] - shiftFd);
      double derivativeApproximated = (ivBlackComputedP - ivBlackComputedM) / (2 * shiftFd);
      assertThat(derivativeApproximated).isCloseTo(ivBlackAdj.getDerivative(0), offset(TOLERANCE_VOL_DELTA));
    }
  }
    
  private static final double T = 4.5;
  private static final double F = 104;
  private static final double DELTA_F = 10;
  private static final double SIGMA = 0.5;
  private static final double TOLERANCE_PRICE2 = 1.0E-8;
  private static final double TOLERANCE_PRICE_DELTA = 1.0E-6;

  @Test
  public void priceAdjoint() {
    // Price
    double price = BlackFormulaRepository.price(F, F - DELTA_F, T, SIGMA, true);
    ValueDerivatives priceAdjoint = BlackFormulaRepository.priceAdjoint(F, F - DELTA_F, T, SIGMA, true);
    assertThat(price).isCloseTo(priceAdjoint.getValue(), offset(TOLERANCE_PRICE2));
    // Price with 0 volatility
    double price0 = BlackFormulaRepository.price(F, F - DELTA_F, T, 0.0d, true);
    ValueDerivatives price0Adjoint = BlackFormulaRepository.priceAdjoint(F, F - DELTA_F, T, 0.0d, true);
    assertThat(price0).isCloseTo(price0Adjoint.getValue(), offset(TOLERANCE_PRICE2));
    // Derivative forward.
    double deltaF = 0.01;
    double priceFP = BlackFormulaRepository.price(F + deltaF, F - DELTA_F, T, SIGMA, true);
    double priceFM = BlackFormulaRepository.price(F - deltaF, F - DELTA_F, T, SIGMA, true);
    double derivativeFxFD = (priceFP - priceFM) / (2 * deltaF);
    assertThat(derivativeFxFD).isCloseTo(priceAdjoint.getDerivative(0), offset(TOLERANCE_PRICE_DELTA));
    // Derivative strike.
    double deltaK = 0.01;
    double priceKP = BlackFormulaRepository.price(F, F - DELTA_F + deltaK, T, SIGMA, true);
    double priceKM = BlackFormulaRepository.price(F, F - DELTA_F - deltaK, T, SIGMA, true);
    double derivativeKxFD = (priceKP - priceKM) / (2 * deltaK);
    assertThat(derivativeKxFD).isCloseTo(priceAdjoint.getDerivative(1), offset(TOLERANCE_PRICE_DELTA));
    // Derivative time.
    double deltaT = 1.0 / 365.0;
    double priceTP = BlackFormulaRepository.price(F, F - DELTA_F, T + deltaT, SIGMA, true);
    double priceTM = BlackFormulaRepository.price(F, F - DELTA_F, T - deltaT, SIGMA, true);
    double derivativeTxFD = (priceTP - priceTM) / (2 * deltaT);
    assertThat(derivativeTxFD).isCloseTo(priceAdjoint.getDerivative(2), offset(TOLERANCE_PRICE_DELTA));
    // Derivative volatility.
    double deltaV = 0.0001;
    double priceVP = BlackFormulaRepository.price(F, F - DELTA_F, T, SIGMA + deltaV, true);
    double priceVM = BlackFormulaRepository.price(F, F - DELTA_F, T, SIGMA - deltaV, true);
    double derivativeVxFD = (priceVP - priceVM) / (2 * deltaV);
    assertThat(derivativeVxFD).isCloseTo(priceAdjoint.getDerivative(3), offset(TOLERANCE_PRICE_DELTA));
  }

  private static final double TOLERANCE_1 = 1.0E-10;
  private static final double TOLERANCE_2_FWD_FWD = 1.0E-6;
  private static final double TOLERANCE_2_VOL_VOL = 1.0E-6;
  private static final double TOLERANCE_2_STR_STR = 1.0E-6;
  private static final double TOLERANCE_2_FWD_VOL = 1.0E-7;
  private static final double TOLERANCE_2_FWD_STR = 1.0E-6;
  private static final double TOLERANCE_2_STR_VOL = 1.0E-6;

  /** Tests second order Algorithmic Differentiation version of BlackFunction with several data sets. */
  @Test
  public void testPriceAdjoint2() {
    // forward, numeraire, sigma, strike, time
    double[][] testData = {
        {104.0d, 0.9d, 0.50d, 94.0d, 4.5d},
        {104.0d, 0.9d, 0.50d, 124.0d, 4.5d},
        {104.0d, 0.9d, 0.50d, 104.0d, 4.5d},
        {0.0250d, 1000.0d, 0.25d, 0.0150d, 10.0d},
        {0.0250d, 1000.0d, 0.25d, 0.0400d, 10.0d},
        {1700.0d, 0.9d, 1.00d, 1500.0d, 0.01d},
        {1700.0d, 0.9d, 1.00d, 1900.0d, 20.0d}
    };
    int nbTest = testData.length;
    for (int i = 0; i < nbTest; i++) {
      testPriceAdjointSecondOrder(testData[i][0], testData[i][1], testData[i][2], testData[i][3], testData[i][4], CALL, i);
      testPriceAdjointSecondOrder(testData[i][0], testData[i][1], testData[i][2], testData[i][3], testData[i][4], PUT, i);
    }
  }

  private void testPriceAdjointSecondOrder(double forward, double numeraire, double sigma, double strike, double time,
      PutCall putCall, int i) {
    // Price
    ValueDerivatives priceAdjoint =
        BlackFormulaRepository.priceAdjoint(forward, strike, time, sigma, putCall.equals(PutCall.CALL));
    Pair<ValueDerivatives, double[][]> bs =
        BlackFormulaRepository.priceAdjoint2(forward, strike, time, sigma, putCall.equals(PutCall.CALL));
    double[][] bsD2 = bs.getSecond();
    assertThat(priceAdjoint.getValue()).as("AD Second order: price").isCloseTo(bs.getFirst().getValue(), offset(TOLERANCE_1));
    // First derivative
    for (int loopder = 0; loopder < 3; loopder++) {
      assertThat(priceAdjoint.getDerivatives().get(loopder)).as("AD Second order: 1st").isCloseTo(bs.getFirst().getDerivative(loopder), offset(TOLERANCE_1));
    }
    // Second derivative
    // Derivative forward-forward.
    double deltaF = 1.0E-3 * forward;
    ValueDerivatives priceAdjointFP = BlackFormulaRepository
        .priceAdjoint(forward + deltaF, strike, time, sigma, putCall.equals(PutCall.CALL));
    ValueDerivatives priceAdjointFM = BlackFormulaRepository
        .priceAdjoint(forward - deltaF, strike, time, sigma, putCall.equals(PutCall.CALL));
    double derivativeFFxFD = (priceAdjointFP.getDerivative(0) - priceAdjointFM.getDerivative(0)) / (2 * deltaF);
    assertThat(derivativeFFxFD).as("AD Second order: 2nd - fwd-fwd " + i).isCloseTo(bs.getSecond()[0][0],
        offset(TOLERANCE_2_FWD_FWD * Math.abs(bs.getFirst().getValue() / (deltaF * deltaF))));
    // Derivative volatility-volatility.
    double deltaV = 0.00001;
    double deltaV2 = (deltaV * deltaV);
    ValueDerivatives priceAdjointVP = BlackFormulaRepository
        .priceAdjoint(forward, strike, time, sigma + deltaV, putCall.equals(PutCall.CALL));
    ValueDerivatives priceAdjointVM = BlackFormulaRepository
        .priceAdjoint(forward, strike, time, sigma - deltaV, putCall.equals(PutCall.CALL));
    double derivativeVVxFD = (priceAdjointVP.getDerivative(3) - priceAdjointVM.getDerivative(3)) / (2 * deltaV);
    assertThat(derivativeVVxFD).as("AD Second order: 2nd - vol-vol " + i).isCloseTo(bsD2[2][2],
        offset(TOLERANCE_2_VOL_VOL * Math.abs(bs.getFirst().getValue() / deltaV2)));
    // Derivative forward-volatility.
    double derivativeFVxFD = (priceAdjointVP.getDerivative(0) - priceAdjointVM.getDerivative(0)) / (2 * deltaV);
    assertThat(derivativeFVxFD).as("AD Second order: 2nd - fwd-vol " + i).isCloseTo(bsD2[2][0],
        offset(TOLERANCE_2_FWD_VOL * Math.abs(bs.getFirst().getValue() / (deltaF * deltaV))));
    assertThat(bsD2[0][2]).as("AD Second order: 2nd - fwd-vol").isCloseTo(bsD2[2][0], offset(TOLERANCE_1));
    // Derivative strike-strike.
    double deltaK = 1.0E-4 * strike;
    ValueDerivatives priceAdjointKP = BlackFormulaRepository
        .priceAdjoint(forward, strike + deltaK, time, sigma, putCall.equals(PutCall.CALL));
    ValueDerivatives priceAdjointKM = BlackFormulaRepository
        .priceAdjoint(forward, strike - deltaK, time, sigma, putCall.equals(PutCall.CALL));
    double derivativeKKxFD = (priceAdjointKP.getDerivative(1) - priceAdjointKM.getDerivative(1)) / (2 * deltaK);
    assertThat(derivativeKKxFD).as("AD Second order: 2nd - strike-strike " + i)
        .isCloseTo(bsD2[1][1], offset(TOLERANCE_2_STR_STR * Math.abs(derivativeKKxFD)));
    // Derivative forward-strike.
    double derivativeFKxFD = (priceAdjointKP.getDerivative(0) - priceAdjointKM.getDerivative(0)) / (2 * deltaK);
    assertThat(derivativeFKxFD).as("AD Second order: 2nd - fwd-str " + i)
        .isCloseTo(bsD2[1][0], offset(TOLERANCE_2_FWD_STR * Math.abs(bs.getFirst().getValue() / (deltaF * deltaK))));
    assertThat(bsD2[0][1]).as("AD Second order: 2nd - fwd-str")
        .isCloseTo(bsD2[1][0], offset(TOLERANCE_1));
    // Derivative strike-volatility.
    double derivativeKVxFD = (priceAdjointVP.getDerivative(1) - priceAdjointVM.getDerivative(1)) / (2 * deltaV);
    assertThat(derivativeKVxFD).as("AD Second order: 2nd - str-vol " + i)
        .isCloseTo(bsD2[2][1], offset(TOLERANCE_2_STR_VOL * Math.abs(bs.getFirst().getValue())));
    assertThat(bsD2[1][2]).as("AD Second order: 2nd - str-vol").isCloseTo(bsD2[2][1], offset(TOLERANCE_1));
  }

}
