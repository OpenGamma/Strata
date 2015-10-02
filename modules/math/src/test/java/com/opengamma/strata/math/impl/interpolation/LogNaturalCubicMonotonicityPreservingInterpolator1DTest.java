/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test.
 */
@Test
public class LogNaturalCubicMonotonicityPreservingInterpolator1DTest {

  /**
   * Parameter for centered finite difference method
   */
  private static final double EPS = 1.e-7;

  /**
   * Data contain minima
   */
  @Test
  public void localMinimumTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6., 7., 8. };
    final double[] yValues = new double[] {11., 8., 5., 1.001, 1.001, 5., 8., 11. };

    final double[] xKeys = new double[] {1., 1.3, 1.6, 2., 2.3, 2.6, 3., 3.3, 3.6, 4., 4.3, 4.7, 5., 5.3, 5.7, 6., 6.3, 6.7, 7., 7.3, 7.7, 8. };
    final int nKeys = xKeys.length;
    final double[] expected = new double[] {11.000000000000002, 9.8292188543753305, 8.8720987912733342, 7.9999999999999982, 7.5805212645035995, 6.8919800777834626, 4.9999999999999991,
      3.1906863006920174, 1.8676668553236566, 1.0009999999999999, 0.77713354870874862, 0.77713354870874862, 1.0009999999999999, 1.5674583835931175, 3.1906863006920183, 4.9999999999999991,
      6.5303730578459076, 7.5805212645035995, 7.9999999999999982, 8.6084760049832294, 9.8292188543753305, 11.000000000000002 };
    final Interpolator1D interpWrap = Interpolator1DFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_MONOTONE);
    final Interpolator1DDataBundle bundle = interpWrap.getDataBundle(xValues, yValues);
    for (int i = 0; i < nKeys; ++i) {
      final double res = interpWrap.interpolate(bundle, xKeys[i]);
      assertEquals(res, expected[i], expected[i] * 1.e-15);
    }

    /*
     * Test sensitivity
     */
    final int nData = 8;
    final double[] yValues1Up = new double[nData];
    final double[] yValues1Dw = new double[nData];
    for (int i = 0; i < nData; ++i) {
      yValues1Up[i] = yValues[i];
      yValues1Dw[i] = yValues[i];
    }
    for (int j = 0; j < nData; ++j) {
      yValues1Up[j] = yValues[j] * (1. + EPS);
      yValues1Dw[j] = yValues[j] * (1. - EPS);
      Interpolator1DDataBundle dataBund1Up = interpWrap.getDataBundleFromSortedArrays(xValues, yValues1Up);
      Interpolator1DDataBundle dataBund1Dw = interpWrap.getDataBundleFromSortedArrays(xValues, yValues1Dw);
      for (int i = 0; i < nKeys; ++i) {
        double res1 = 0.5 * (interpWrap.interpolate(dataBund1Up, xKeys[i]) - interpWrap.interpolate(dataBund1Dw, xKeys[i])) / EPS / yValues[j];
        assertEquals(res1, interpWrap.getNodeSensitivitiesForValue(bundle, xKeys[i])[j], Math.max(Math.abs(yValues[j]) * EPS, EPS));
      }
      yValues1Up[j] = yValues[j];
      yValues1Dw[j] = yValues[j];
    }

  }

  /**
   * First and last intervals are flat
   */
  @Test
  public void correctedEndIntervalsTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6., 7., 8. };
    final double[] yValues = new double[] {1.001, 1.001, 5., 8., 11., 12., 18., 18. };

    final double[] xKeys = new double[] {1., 1.3, 1.6, 2., 2.3, 2.6, 3., 3.3, 3.6, 4., 4.3, 4.7, 5., 5.3, 5.7, 6., 6.3, 6.7, 7., 7.3, 7.7, 8. };
    final int nKeys = xKeys.length;
    final double[] expected = new double[] {1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.3100031054806665, 2.3728322906920676, 4.9999999999999991,
      6.547464969992709, 7.3863523497161641, 7.9999999999999982, 8.7698200360758953, 10.147409335950455, 11.000000000000002, 11.32816743116623, 11.473242089855484, 12, 13.599314796416568,
      16.75801353957614, 17.999999999999996, 17.999999999999996, 17.999999999999996, 17.999999999999996 };
    final PiecewisePolynomialInterpolator1D interpWrap = new LogNaturalCubicMonotonicityPreservingInterpolator1D();
    final Interpolator1DDataBundle bundle = interpWrap.getDataBundle(xValues, yValues);
    for (int i = 0; i < nKeys; ++i) {
      final double res = interpWrap.interpolate(bundle, xKeys[i]);
      assertEquals(res, expected[i], expected[i] * 1.e-15);
    }

    /*
     * Test sensitivity
     */
    final int nData = 8;
    final double[] yValues1Up = new double[nData];
    final double[] yValues1Dw = new double[nData];
    for (int i = 0; i < nData; ++i) {
      yValues1Up[i] = yValues[i];
      yValues1Dw[i] = yValues[i];
    }
    for (int j = 0; j < nData; ++j) {
      yValues1Up[j] = yValues[j] * (1. + EPS);
      yValues1Dw[j] = yValues[j] * (1. - EPS);
      Interpolator1DDataBundle dataBund1Up = interpWrap.getDataBundleFromSortedArrays(xValues, yValues1Up);
      Interpolator1DDataBundle dataBund1Dw = interpWrap.getDataBundleFromSortedArrays(xValues, yValues1Dw);
      for (int i = 0; i < nKeys; ++i) {
        double res1 = 0.5 * (interpWrap.interpolate(dataBund1Up, xKeys[i]) - interpWrap.interpolate(dataBund1Dw, xKeys[i])) / EPS / yValues[j];
        assertEquals(res1, interpWrap.getNodeSensitivitiesForValue(bundle, xKeys[i])[j], Math.max(Math.abs(yValues[j]) * EPS, EPS));
      }
      yValues1Up[j] = yValues[j];
      yValues1Dw[j] = yValues[j];
    }
  }

  /**
   * Data are totally flat
   */
  @Test
  public void constantTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6., 7., 8. };
    final double[] yValues = new double[] {1.001, 1.001, 1.001, 1.001, 1.001, 1.001, 1.001, 1.001 };

    final double[] xKeys = new double[] {1., 1.3, 1.6, 2., 2.3, 2.6, 3., 3.3, 3.6, 4., 4.3, 4.7, 5., 5.3, 5.7, 6., 6.3, 6.7, 7., 7.3, 7.7, 8. };
    final int nKeys = xKeys.length;
    final double[] expected = new double[] {1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999,
      1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999,
      1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999, 1.0009999999999999 };
    final PiecewisePolynomialInterpolator1D interpWrap = new LogNaturalCubicMonotonicityPreservingInterpolator1D();
    final Interpolator1DDataBundle bundle = interpWrap.getDataBundle(xValues, yValues);
    for (int i = 0; i < nKeys; ++i) {
      final double res = interpWrap.interpolate(bundle, xKeys[i]);
      assertEquals(expected[i], res, expected[i] * 1.e-15);
    }

    /*
     * Test sensitivity
     */
    final int nData = 8;
    final double[] yValues1Up = new double[nData];
    final double[] yValues1Dw = new double[nData];
    for (int i = 0; i < nData; ++i) {
      yValues1Up[i] = yValues[i];
      yValues1Dw[i] = yValues[i];
    }
    for (int j = 0; j < nData; ++j) {
      yValues1Up[j] = yValues[j] * (1. + EPS);
      yValues1Dw[j] = yValues[j] * (1. - EPS);
      Interpolator1DDataBundle dataBund1Up = interpWrap.getDataBundleFromSortedArrays(xValues, yValues1Up);
      Interpolator1DDataBundle dataBund1Dw = interpWrap.getDataBundleFromSortedArrays(xValues, yValues1Dw);
      for (int i = 0; i < nKeys; ++i) {
        double res1 = 0.5 * (interpWrap.interpolate(dataBund1Up, xKeys[i]) - interpWrap.interpolate(dataBund1Dw, xKeys[i])) / EPS / yValues[j];
        assertEquals(res1, interpWrap.getNodeSensitivitiesForValue(bundle, xKeys[i])[j], Math.max(Math.abs(yValues[j]) * EPS, EPS));
      }
      yValues1Up[j] = yValues[j];
      yValues1Dw[j] = yValues[j];
    }
  }

  /**
   * Intervals are random
   */
  @Test
  public void differentIntervalsTest() {
    final double[] xValues = new double[] {1.0328724558967068, 1.2692381049172323, 2.8611430465380905, 4.296118458251132, 7.011992052151352, 7.293354144919639, 8.557971037612713, 8.77306861567384,
      10.572470371584489, 12.96945799507056 };
    final double[][] yValues = new double[][] {
      {1.1593075755231343, 2.794957672828094, 4.674733634811079, 5.517689918508841, 6.138447304104604, 6.264375977142906, 6.581666492568779, 8.378685055774037,
        10.005246918325483, 10.468304334744241 },
      {9.95780079114617, 8.733013195721913, 8.192165283188197, 6.539369493529048, 6.3868683960757515, 4.700471352238411, 4.555354921077598, 3.780781869340659, 2.299369456202763, 0.9182441378327986 } };
    final double[] xKeys = new double[] {1.183889983084374, 1.2385908948332678, 1.9130960889984017, 2.6751399625052708, 3.061475076285611, 3.8368242544942768, 5.18374791977202, 6.237315353617813,
      7.165363988178849, 7.292538274335414, 8.555769928347884, 8.556400741450425, 8.743446029721234, 8.758174808226803, 10.354819113659708, 10.54773504143382, 12.665952152847833, 12.720941630811579 };
    final int nKeys = xKeys.length;
    final double[][] expected = new double[][] {
      {2.2498175275276355, 2.6604493559730114, 4.1934731104936809, 4.6708996453788716, 4.7026667337466561, 5.1700040449097751, 5.8211261852832061,
        5.8734369002551237, 6.230372939183983, 6.2643743298420214, 6.5799716327905839, 6.5804567123216771, 8.2244714149632099, 8.320106237623623, 10.002105989812062, 10.005242307319149,
        10.311404607585567, 10.336575152039961 },
      {9.0115183927982674, 8.7923765265015685, 8.3742614861844871, 8.2582572028821648, 8.0294763379975009, 6.8678544068867886, 6.53398463318387, 6.4832650514113164, 5.3777446119604493,
        4.7007909964955576, 4.5560983095534917, 4.5558857802921331, 3.886702136938859, 3.8309544446032437, 2.3465700101454354, 2.3057759170296173, 1.0734291352697389, 1.0436689951231841, } };
    final Interpolator1D interpWrap = Interpolator1DFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_MONOTONE);

    final int yDim = yValues.length;
    for (int k = 0; k < yDim; ++k) {
      final Interpolator1DDataBundle bundle = interpWrap.getDataBundle(xValues, yValues[k]);
      for (int i = 0; i < nKeys; ++i) {
        final double res = interpWrap.interpolate(bundle, xKeys[i]);
        assertEquals(res, expected[k][i], expected[k][i] * 1.e-15);
      }

      /*
       * Test sensitivity
       */
      final int nData = 10;
      final double[] yValues1Up = new double[nData];
      final double[] yValues1Dw = new double[nData];
      for (int i = 0; i < nData; ++i) {
        yValues1Up[i] = yValues[k][i];
        yValues1Dw[i] = yValues[k][i];
      }
      for (int j = 0; j < nData; ++j) {
        yValues1Up[j] = yValues[k][j] * (1. + EPS);
        yValues1Dw[j] = yValues[k][j] * (1. - EPS);
        Interpolator1DDataBundle dataBund1Up = interpWrap.getDataBundleFromSortedArrays(xValues, yValues1Up);
        Interpolator1DDataBundle dataBund1Dw = interpWrap.getDataBundleFromSortedArrays(xValues, yValues1Dw);
        for (int i = 0; i < nKeys; ++i) {
          double res1 = 0.5 * (interpWrap.interpolate(dataBund1Up, xKeys[i]) - interpWrap.interpolate(dataBund1Dw, xKeys[i])) / EPS / yValues[k][j];
          assertEquals(res1, interpWrap.getNodeSensitivitiesForValue(bundle, xKeys[i])[j], Math.max(Math.abs(yValues[k][j]) * EPS, EPS));
        }
        yValues1Up[j] = yValues[k][j];
        yValues1Dw[j] = yValues[k][j];
      }
    }
  }

  /**
   * Tests below are for checking remaining branches, 
   * where finite difference method is not necessarily a nice reference value to the node sensitivity
   */
  @Test
  public void branch1Test() {
    final double[] xValues = new double[] {2., 3., 4., 6., 7. };
    final int nData = xValues.length;
    /*
     * yValues must be exponentiated to plug them into the interpolator
     */
    final double[] yValues = new double[] {1., 3., 4., 0., 1. };
    final double[] xKeys = new double[] {2.0, 2.983627751020256, 2.998717035252617, 3.0, 3.9330838028795982, 3.9594906722658414, 4.0, 4.266178673839306, 5.449091282737356, 6.0, 6.747051011498387,
      6.901100999729749, 7.0 };
    final int nKeys = xKeys.length;
    final double[] expected = new double[] {2.7182818284590451, 19.479258615895358, 20.037429599906989, 20.085536923187668, 54.324442814355635, 54.49737300048465, 54.598150033144236,
      44.98395132776092, 2.1028639894359609, 1, 1.8111382952489585, 2.2988022001894772, 2.7182818284590451 };

    final Interpolator1D interpWrap = Interpolator1DFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_MONOTONE);

    final double[] expY = new double[nData];
    for (int j = 0; j < nData; ++j) {
      expY[j] = Math.exp(yValues[j]);
    }
    final Interpolator1DDataBundle bundle = interpWrap.getDataBundle(xValues, expY);
    for (int i = 0; i < nKeys; ++i) {
      final double res = interpWrap.interpolate(bundle, xKeys[i]);
      assertEquals(res, expected[i], expected[i] * 1.e-15);
    }

  }

  /**
   * 
   */
  @Test
  public void branch2Test() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6., 7., 8., 9., 10. };
    final int nData = xValues.length;
    /*
     * yValues must be exponentiated to plug them into the interpolator
     */
    final double[][] yValues = new double[][] { {2.0, 2.0, 1.0, 1.0, 0.0, 3.0, 4.0, 4.0, 0.0, 2.0 }, {4.0, 4.0, 1.0, 0.0, 4.0, 1.0, 0.0, 0.0, 1.0, 3.0 },
      {1.0, 4.0, 4.0, 3.0, 0.0, 0.0, 1.0, 4.0, 3.0, 3.0 } };
    final double[] xKeys = new double[] {1.0, 1.9050850580810552, 1.9158926383758192, 2.0, 2.4994761110044155, 2.968553598403417, 3.0, 3.8484792403070083, 3.9221624514312947, 4.0, 4.979503746614516,
      4.993668628571749, 5.0, 5.18354454926604, 5.846196442887777, 6.0, 6.867629604698587, 6.914840435246079, 7.0, 7.015305676304899, 7.280065439914241, 8.0, 8.49326697367971, 8.708063885779406,
      9.0, 9.131229570982494, 9.716349521529969, 10.0 };
    final int nKeys = xKeys.length;
    final double[][] expected = new double[][] {
      {7.3890560989306504, 7.3890560989306504, 7.3890560989306504, 7.3890560989306504, 4.4852123145874767, 2.7261883787002428, 2.7182818284590451, 2.7182818284590451, 2.7182818284590451,
        2.7182818284590451, 0.97969932627769529, 0.99322756673779766, 1, 1.3938793248551158, 12.850759166959309, 20.085536923187668, 50.182597030452385, 51.560280849294088, 54.598150033144236,
        55.218804661804363, 63.511538113015853, 54.598150033144236, 8.7176590694268565, 2.8278567327356612, 1, 0.86880565555798062, 2.5579817875757369, 7.3890560989306469 },
      {54.598150033144236, 54.598150033144236, 54.598150033144236, 54.598150033144236, 17.760274110817374, 2.9958037021087414, 2.7182818284590451, 0.73376944290077917, 0.82738742180704639, 1,
        53.508785496466743, 54.295076296433677, 54.598150033144236, 50.311063229889783, 4.6558218627733252, 2.7182818284590451, 1.0062934843063123, 1.0034519476935224, 1, 0.99932896631361012,
        0.97442187215373077, 1, 1.3997684211639461, 1.7853752841837764, 2.7182818284590451, 3.3911250692786412, 10.828067231360166, 20.085536923187668 },
      {2.7182818284590451, 49.235006411966012, 49.974656963120772, 54.598150033144236, 63.830736342838208, 55.448625761619375, 54.598150033144236, 27.584526525055619, 23.933685337017916,
        20.085536923187668, 1.0176314315192936, 1.0049633272587422, 1, 0.89858270072467672, 0.92703169141746544, 1, 2.0218090429458204, 2.2248687835027874, 2.7182818284590451, 2.8292669718329302,
        6.5706545957915896, 54.598150033144236, 38.712600828630933, 26.456722390796571, 20.085536923187668, 20.085536923187668, 20.085536923187668, 20.085536923187668 } };
    final Interpolator1D interpWrap = Interpolator1DFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_MONOTONE);

    final int dim = yValues.length;
    final double[] expY = new double[nData];

    for (int k = 0; k < dim; ++k) {
      for (int j = 0; j < nData; ++j) {
        expY[j] = Math.exp(yValues[k][j]);
      }
      final Interpolator1DDataBundle bundle = interpWrap.getDataBundle(xValues, expY);
      for (int i = 0; i < nKeys; ++i) {
        final double res = interpWrap.interpolate(bundle, xKeys[i]);
        assertEquals(res, expected[k][i], expected[k][i] * 1.e-14);
      }
    }
  }

  /**
   * 
   */
  @Test
  public void equalBranchTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6., 7., 8. };
    final int nData = xValues.length;
    /*
     * yValues must be exponentiated to plug them into the interpolator
     */
    final double[][] yValues = new double[][] {
      {0.0, 4.0, 1.0, 0.0, 0.0, 1.0, 4.0, 0.0 },
      {7.0, 9.0, 0.0, 5.0, 7.0, 6.0, 2.0, 1.0 },
      {1.0, 2.0, 6.0, 7.0, 5.0, 0.0, 9.0, 7.0 },
      {1.0, 7.0, 3.0, 3.0, 1.0, 1.0, 9.0, 4.0 },
      {6.999999999969613, 8.999999999970079, -4.257663991276979E-11, 4.999999999946146, 7.000000000012074, 5.999999999913111, 2.000000000016158, 0.9999999999261457 },
      {0.9999999999261457, 2.000000000016158, 5.999999999913111, 7.000000000012074, 4.999999999946146, -4.257663991276979E-11, 8.999999999970079, 6.999999999969613 } };
    final double[] xKeys = new double[] {1.0, 1.2779433997630072, 1.656609533463786, 2.0, 2.2956786700005267, 2.8659980081803678, 3.0, 3.9898449811216867, 3.9938359664856375, 4.0, 4.188109260626712,
      4.779284458437244, 5.0, 5.869285729959244, 5.95922678837249, 6.0, 6.460023409907294, 6.75536182298159, 7.0, 7.183119556298179, 7.1955552746258356, 8.0 };
    final int nKeys = xKeys.length;
    final double[][] expected = new double[][] {
      {1, 5.0769807024909674, 29.17992815113557, 54.598150033144236, 34.917727263585803, 4.2573927525768376, 2.7182818284590451, 1.0000010472308627, 1.0000002342043866, 1, 1, 1, 1,
        1.9287698923294074, 2.4171760706860161, 2.7182818284590451, 15.224138974744141, 39.838595228973716, 54.598150033144236, 45.199425710161854, 44.057940816613353, 1 },
      {1096.6331584284585, 5333.5539184302479, 18165.963065113076, 8103.0839275753842, 607.56986313464984, 1.9149625780643504, 1, 139.5549556354064, 142.99211179838267, 148.4131591025766,
        376.02571344490883, 1073.3024006209625, 1096.6331584284585, 568.56609115981007, 453.68360696919558, 403.42879349273511, 65.350616903680475, 17.888819561830854, 7.3890560989306504,
        4.6884364178676323, 4.5748841405062377, 2.7182818284590451 },
      {2.7182818284590451, 2.7772794454320069, 3.6077635314095331, 7.3890560989306504, 22.145043556838431, 256.96421801803592, 403.42879349273511, 1096.632010001573, 1096.6329015922227,
        1096.6331584284585, 1082.1309438583924, 425.60241718811955, 148.4131591025766, 0.85943215386562299, 0.90677829320593184, 1, 39.576501369696395, 1038.7428669102387, 8103.0839275753842,
        16662.00239456903, 17057.458854736062, 1096.6331584284585 },
      {2.7182818284590451, 31.095872784557542, 428.47038447618979, 1096.6331584284585, 472.33535110791058, 24.440185281825144, 20.085536923187668, 20.085536923187668, 20.085536923187668,
        20.085536923187668, 17.41632068392553, 4.2645590003046863, 2.7182818284590451, 2.2923051466497801, 2.5633983656402046, 2.7182818284590451, 79.81311881224741, 1714.6571130287196,
        8103.0839275753842, 9619.3063046375883, 9486.9934849509755, 54.598150033144236 },
      {1096.6331583951348, 5333.5539182701423, 18165.963064572952, 8103.083927332932, 607.5698631159371, 1.9149625779901842, 0.99999999995742339, 139.55495562780879, 142.99211179063042,
        148.41315909458399, 376.02571342971049, 1073.3024006275059, 1096.6331584416992, 568.56609111300827, 453.68360692998351, 403.42879345768171, 65.350616900601295, 17.888819561719593,
        7.3890560990500438, 4.6884364179550468, 4.5748841405902736, 2.7182818282582883 },
      {2.7182818282582888, 2.7772794453460929, 3.6077635314503724, 7.3890560990500438, 22.145043556555468, 256.96421799682423, 403.42879345768171, 1096.6320100147796, 1096.6329016054508,
        1096.6331584416992, 1082.1309438666685, 425.60241717206202, 148.41315909458399, 0.85943215382507876, 0.90677829316608805, 0.99999999995742339, 39.576501368397217, 1038.7428668785005,
        8103.083927332932, 16662.002394073297, 17057.458854228626, 1096.6331583951348 } };

    final Interpolator1D interpWrap = Interpolator1DFactory.getInterpolator(Interpolator1DFactory.LOG_NATURAL_CUBIC_MONOTONE);

    final double[] expY = new double[nData];
    final int dim = yValues.length;

    for (int k = 0; k < dim; ++k) {
      for (int j = 0; j < nData; ++j) {
        expY[j] = Math.exp(yValues[k][j]);
      }
      final Interpolator1DDataBundle bundle = interpWrap.getDataBundle(xValues, expY);
      for (int i = 0; i < nKeys; ++i) {
        final double res = interpWrap.interpolate(bundle, xKeys[i]);
        assertEquals(res, expected[k][i], expected[k][i] * 1.e-14);
      }
    }
  }

}
