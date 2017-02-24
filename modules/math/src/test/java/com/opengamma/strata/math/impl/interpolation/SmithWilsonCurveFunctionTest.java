/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.differentiation.ScalarFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.differentiation.ScalarFirstOrderDifferentiator;

/**
 * Test {@link SmithWilsonCurveFunction}.
 */
@Test
public class SmithWilsonCurveFunctionTest {

  private static final double TOL = 1.0e-13;
  private static final double ONE_BP = 1.0e-4;
  private static final double EPS = 1.0e-7;
  private static final ScalarFirstOrderDifferentiator DERIVATIVE =
      new ScalarFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);
  private static final ScalarFieldFirstOrderDifferentiator PARAM_SENSI =
      new ScalarFieldFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);
  private static final SmithWilsonCurveFunction SW_FUNCTION = SmithWilsonCurveFunction.DEFAULT;

  public void test_function() {
    /* Source: EIOPA - European Insurance and Occupational Pensions Authority */
    DoubleArray weights = DoubleArray.of(2.66199573146653, -2.69394271301803, 0.484066828515597, 0.690098457608931,
        -0.380409008626438, 0.228249649568978, -0.160902407241201);
    DoubleArray nodes = DoubleArray.of(3d, 5d, 7d, 10d, 15d, 20d, 30d);
    double alpha = 0.162009;
    double[] dfExp = new double[] {
        1.0, 0.9784491887550640, 0.9542301229696670, 0.9249659993836340, 0.8901581292246130, 0.8554401454007420,
        0.8255482242362790, 0.7988075218816840, 0.7726983322166260, 0.7460275162373400, 0.7179860712887310, 0.6881599086848680,
        0.6573456807993880, 0.6264650351287230, 0.5962704115599910, 0.5673812725224810, 0.5401743074526890, 0.5144298960128170,
        0.4898295198849120, 0.4660960306958390, 0.4429840495494910, 0.4203401447244430, 0.3982807994009170, 0.3769536975722210,
        0.3564726090143710, 0.3369247816853740, 0.3183770246642050, 0.3008807334361760, 0.2844760642424350, 0.2691954275764280,
        0.2550664411957300, 0.2420825656824480, 0.2301189446273790, 0.2190430997506780, 0.2087446877039010, 0.1991315201776450,
        0.1901263120847230, 0.1816640241112870, 0.1736896905038690, 0.1661566430181050, 0.1590250583253540, 0.1522607695344190,
        0.1458342933905510, 0.1397200336141430, 0.1338956281057440, 0.1283414136729280, 0.1230399867736360, 0.1179758427202050,
        0.1131350790119630, 0.1085051510954450, 0.1040746709988320, 0.0998332410401614, 0.0957713162396188, 0.0918800902340929,
        0.0881514004454756, 0.0845776490323465, 0.0811517367898836, 0.0778670076814032, 0.0747172021082445, 0.0716964173703138,
        0.0687990740517746, 0.0660198872967402, 0.0633538421279633, 0.0607961721151252, 0.0583423408248006, 0.0559880255866578,
        0.0537291031941595, 0.0515616372264417, 0.0494818667339434, 0.0474861960760429, 0.0455711857363243, 0.0437335439716375,
        0.0419701191761172, 0.0402778928617874, 0.0386539731741227, 0.0370955888746717, 0.0356000837341027, 0.0341649112882632,
        0.0327876299174312, 0.0314658982151756, 0.0301974706183710, 0.0289801932741481, 0.0278120001230513, 0.0266909091805686,
        0.0256150190015920, 0.0245825053143602, 0.0235916178120912, 0.0226406770919034, 0.0217280717317838, 0.0208522554973446,
        0.0200117446709340, 0.0192051154963766, 0.0184310017332138, 0.0176880923148379, 0.0169751291053533, 0.0162909047503889,
        0.0156342606174260, 0.0150040848215014, 0.0143993103324119, 0.0138189131597808, 0.0132619106125611, 0.0127273596297376,
        0.0122143551791677, 0.0117220287216560, 0.0112495467375058, 0.0107961093129258, 0.0103609487837926, 0.0099433284343883,
        0.0095425412488404, 0.0091579087130933, 0.0087887796653373, 0.0084345291929100, 0.0080945575737721, 0.0077682892607414,
        0.0074551719067439, 0.0071546754294138, 0.0068662911134475, 0.0065895307491796, 0.0063239258059133, 0.0060690266385980,
        0.0058244017265059};
    int years = 121;
    Function<Double, Double> derivFunc = DERIVATIVE.differentiate(
        x -> SW_FUNCTION.value(x, alpha, nodes, weights), x -> (x >= 0d));
    double dfPrev = 1d;
    for (int i = 0; i < years; ++i) {
      double t = (double) i;
      // value
      double dfCmp = SW_FUNCTION.value(t, alpha, nodes, weights);
      assertEquals(dfCmp, dfExp[i], TOL);
      // first derivative
      double derivCmp = SW_FUNCTION.firstDerivative(t, alpha, nodes, weights);
      double derivExp = derivFunc.apply(t);
      assertEquals(derivCmp, derivExp, EPS);
      // parameter sensitivity
      DoubleArray paramSensiCmp = SW_FUNCTION.parameterSensitivity(t, alpha, nodes);
      Function<DoubleArray, DoubleArray> paramSensiFunc = PARAM_SENSI.differentiate(w -> SW_FUNCTION.value(t, alpha, nodes, w));
      DoubleArray paramSensiExp = paramSensiFunc.apply(weights);
      assertTrue(DoubleArrayMath.fuzzyEquals(paramSensiCmp.toArray(), paramSensiExp.toArray(), EPS));
      // forward rate
      if (i > 60) {
        double fwd = dfPrev / dfCmp - 1d;
        assertEquals(fwd, 0.042, ONE_BP);
      }
      dfPrev = dfCmp;
    }
  }

  public void test_gap() {
    DoubleArray nodes = DoubleArray.of(3d, 5d, 7d, 10d, 15d, 20d, 30d);
    double testAlpha = 0.05;
    DoubleArray weights = DoubleArray.of(87.2581459896248, -90.049408220743, 16.5225834204242, 20.401521178462, -12.3472386476547,
        5.04900954653571, -1.82365099870708);
    double gapExp = 4.27089481442022E-03 + ONE_BP;
    double gapCmp = SmithWilsonCurveFunction.gap(60d, testAlpha, nodes, weights);
    assertEquals(gapCmp, gapExp, TOL);
  }

}
