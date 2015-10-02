/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * 
 */
public class ProductPiecewisePolynomialInterpolator1DTest {
  private static final PiecewisePolynomialInterpolator[] INTERP_SENSE;
  static {
    PiecewisePolynomialInterpolator cubic = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator natural = new NaturalSplineInterpolator();
    PiecewiseCubicHermiteSplineInterpolatorWithSensitivity pchip =
        new PiecewiseCubicHermiteSplineInterpolatorWithSensitivity();
    PiecewisePolynomialInterpolator hymanNat = new MonotonicityPreservingCubicSplineInterpolator(natural);
    INTERP_SENSE = new PiecewisePolynomialInterpolator[] {cubic, natural, pchip, hymanNat };
  }
  private static final PiecewisePolynomialWithSensitivityFunction1D FUNC =
      new PiecewisePolynomialWithSensitivityFunction1D();
  private static final double EPS = 1.0e-12;
  private static final double DELTA = 1.0e-6;

  /**
   * No clamped points added
   */
  @Test
  public void notClampedTest() {
    double[][] xValuesSet = new double[][] { {-5.0, -1.4, 3.2, 3.5, 7.6 }, {1., 2., 4.5, 12.1, 14.2 },
      {-5.2, -3.4, -3.2, -0.9, -0.2 } };
    double[][] yValuesSet = new double[][] { {-2.2, 1.1, 1.9, 2.3, -0.1 }, {3.4, 5.2, 4.3, 1.1, 0.2 },
      {1.4, 2.2, 4.1, 1.9, 0.99 } };

    for (int k = 0; k < xValuesSet.length; ++k) {
      double[] xValues = Arrays.copyOf(xValuesSet[k], xValuesSet[k].length);
      double[] yValues = Arrays.copyOf(yValuesSet[k], yValuesSet[k].length);
      int nData = xValues.length;
      int nKeys = 100;
      double interval = (xValues[nData - 1] - xValues[0]) / (nKeys - 1.0);

      int n = INTERP_SENSE.length;
      for (int i = 0; i < n; ++i) {
        ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP_SENSE[i]);
        PiecewisePolynomialResult result = interp.interpolateWithSensitivity(xValues, yValues);
        ProductPiecewisePolynomialInterpolator1D interp1D = new ProductPiecewisePolynomialInterpolator1D(
            INTERP_SENSE[i]);
        Interpolator1DDataBundle data = interp1D.getDataBundle(xValues, yValues);
        InterpolatorTestUtil.assertArrayRelative("notClampedTest", xValues, data.getKeys(), EPS);
        InterpolatorTestUtil.assertArrayRelative("notClampedTest", yValues, data.getValues(), EPS);

        for (int j = 0; j < nKeys; ++j) {
          double key = xValues[0] + interval * j;
          InterpolatorTestUtil.assertRelative("notClampedTest", FUNC.evaluate(result, key).getEntry(0) / key,
              interp1D.interpolate(data, key), EPS);
          double keyUp = key + DELTA;
          double keyDw = key - DELTA;
          double refDeriv = 0.5 * (interp1D.interpolate(data, keyUp) - interp1D.interpolate(data, keyDw)) / DELTA;
          InterpolatorTestUtil.assertRelative("notClampedTest", refDeriv,
              interp1D.firstDerivative(data, key), DELTA);
          double[] refSense = new double[nData];
          for (int l = 0; l < nData; ++l) {
            double[] yValuesUp = Arrays.copyOf(yValues, nData);
            double[] yValuesDw = Arrays.copyOf(yValues, nData);
            yValuesUp[l] += DELTA;
            yValuesDw[l] -= DELTA;
            Interpolator1DDataBundle dataUp = interp1D.getDataBundle(xValues, yValuesUp);
            Interpolator1DDataBundle dataDw = interp1D.getDataBundle(xValues, yValuesDw);
            refSense[l] = 0.5 * (interp1D.interpolate(dataUp, key) - interp1D.interpolate(dataDw, key)) / DELTA;
          }
          InterpolatorTestUtil.assertArrayRelative("notClampedTest", interp1D.getNodeSensitivitiesForValue(data, key),
              refSense, DELTA * 10.0);
        }
      }
    }
  }

  /**
   * Clamped points 
   */
  @Test
  public void clampedTest() {
    double[] xValues = new double[] {-5.0, -1.4, 3.2, 3.5, 7.6 };
    double[] yValues = new double[] {-2.2, 1.1, 1.9, 2.3, -0.1 };
    double[][] xValuesClampedSet = new double[][] { {0.0 }, {-7.2, -2.5, 8.45 }, {} };
    double[][] yValuesClampedSet = new double[][] { {0.0 }, {-1.2, -1.4, 2.2 }, {} };

    for (int k = 0; k < xValuesClampedSet.length; ++k) {
      double[] xValuesClamped = Arrays.copyOf(xValuesClampedSet[k], xValuesClampedSet[k].length);
      double[] yValuesClamped = Arrays.copyOf(yValuesClampedSet[k], yValuesClampedSet[k].length);
      int nData = xValues.length;
      int nKeys = 100;
      double interval = (xValues[nData - 1] - xValues[0]) / (nKeys - 1.0);

      int n = INTERP_SENSE.length;
      for (int i = 0; i < n; ++i) {
        ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP_SENSE[i],
            xValuesClamped, yValuesClamped);
        PiecewisePolynomialResultsWithSensitivity result = interp.interpolateWithSensitivity(xValues, yValues);
        ProductPiecewisePolynomialInterpolator1D interp1D = new ProductPiecewisePolynomialInterpolator1D(
            INTERP_SENSE[i], xValuesClamped, yValuesClamped);
        Interpolator1DDataBundle data = interp1D.getDataBundleFromSortedArrays(xValues, yValues);
        InterpolatorTestUtil.assertArrayRelative("notClampedTest", xValues, data.getKeys(), EPS);
        InterpolatorTestUtil.assertArrayRelative("notClampedTest", yValues, data.getValues(), EPS);

        for (int j = 0; j < nKeys; ++j) {
          double key = xValues[0] + interval * j;
          InterpolatorTestUtil.assertRelative("notClampedTest", FUNC.evaluate(result, key).getEntry(0) / key,
              interp1D.interpolate(data, key), EPS);
          double keyUp = key + DELTA;
          double keyDw = key - DELTA;
          double refDeriv = 0.5 * (interp1D.interpolate(data, keyUp) - interp1D.interpolate(data, keyDw)) / DELTA;
          InterpolatorTestUtil.assertRelative("notClampedTest", refDeriv,
              interp1D.firstDerivative(data, key), DELTA * 10.0);
          double[] refSense = new double[nData];
          for (int l = 0; l < nData; ++l) {
            double[] yValuesUp = Arrays.copyOf(yValues, nData);
            double[] yValuesDw = Arrays.copyOf(yValues, nData);
            yValuesUp[l] += DELTA;
            yValuesDw[l] -= DELTA;
            Interpolator1DDataBundle dataUp = interp1D.getDataBundle(xValues, yValuesUp);
            Interpolator1DDataBundle dataDw = interp1D.getDataBundle(xValues, yValuesDw);
            refSense[l] = 0.5 * (interp1D.interpolate(dataUp, key) - interp1D.interpolate(dataDw, key)) / DELTA;
          }
          InterpolatorTestUtil.assertArrayRelative("notClampedTest", interp1D.getNodeSensitivitiesForValue(data, key),
              refSense, DELTA * 10.0);
        }
      }
    }
  }

  /**
   * Check Math.abs(value) < SMALL is smoothly connected to general cases
   */
  @Test
  public void closeToZeroTest() {
    double[] xValues = new double[] {-5.0, -2.4, 3.2, 3.5, 7.6 };
    double[] yValues = new double[] {-2.2, 1.1, 1.9, 2.3, -0.1 };
    int n = INTERP_SENSE.length;
    for (int i = 0; i < n; ++i) {
      ProductPiecewisePolynomialInterpolator1D interp1D = new ProductPiecewisePolynomialInterpolator1D(
          INTERP_SENSE[i], new double[] {0.0 }, new double[] {0.0 });
      Interpolator1DDataBundle data = interp1D.getDataBundle(xValues, yValues);
      double eps = 1.0e-5;
      InterpolatorTestUtil.assertRelative("closeToZeroTest", interp1D.interpolate(data, eps),
          interp1D.interpolate(data, 0.0), eps);
      InterpolatorTestUtil.assertRelative("closeToZeroTest", interp1D.firstDerivative(data, eps),
          interp1D.firstDerivative(data, 0.0), eps);
      InterpolatorTestUtil.assertArrayRelative("closeToZeroTest ", interp1D.getNodeSensitivitiesForValue(data, eps),
          interp1D.getNodeSensitivitiesForValue(data, 0.0), eps);
    }
  }

  /**
   * method not to be used
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notImplementedTest() {
    double[] xValues = new double[] {-5.0, -2.4, 3.2, 3.5, 7.6 };
    double[] yValues = new double[] {-2.2, 1.1, 1.9, 2.3, -0.1 };
    ProductPiecewisePolynomialInterpolator1D interp1D = new ProductPiecewisePolynomialInterpolator1D(
        INTERP_SENSE[1], new double[] {0.0 }, new double[] {0.0 });
    Interpolator1DDataBundle data = interp1D.getDataBundle(xValues, yValues);
    interp1D.getFiniteDifferenceSensitivities(data, 0.2);
  }

  /**
   * Regression to the legacy Product Interpolator via yield curve
   */
  @Test
  public void regressionYieldCurveTest() {
    double[] time1 = new double[] {0.002739726027397, 0.019178082191781, 0.038356164383562, 0.084931506849315,
      0.172602739726027, 0.252054794520548, 0.336986301369863, 0.413698630136986, 0.501369863013699,
      0.580821917808219, 0.671232876712329, 0.747945205479452, 0.832876712328767, 0.920547945205479, 1,
      1.25205479452055, 1.5013698630137, 1.75068493150685, 2.0027397260274, 3.0027397260274, 4.00821917808219,
      5.00547945205479, 6.00547945205479, 7.00547945205479, 8.00547945205479, 9.00547945205479, 10.0109589041096,
      12.0082191780822, 15.0164383561644, 20.013698630137, 25.0164383561644, 30.0219178082192, 35.0246575342466,
      40.027397260274, 45.0301369863014, 50.0356164383562 };
    double[] rate1 = new double[] {0.00430997455348493, 0.00431882113819674, 0.00431314320653989, 0.00431970749752497,
      0.00444129725993973, 0.00447764229051919, 0.00454934099818509, 0.00480522065276289, 0.00503962777637656,
      0.00520911179002989, 0.00543690213062051, 0.00563610380093184, 0.00583579456204562, 0.00612718766319607,
      0.00636519907010272, 0.00717310918750252, 0.00795749811802235, 0.00870119236161661, 0.00946155387022128,
      0.0123334511255753, 0.0143921749914485, 0.0159592596727554, 0.0172361877785297, 0.0183436791467994,
      0.0192793358953591, 0.0201222559973813, 0.0208986383710833, 0.0222510994961065, 0.023877100242259,
      0.0256026136259111, 0.026288001664418, 0.0266311744778153, 0.0264014166694374, 0.0263527169318637,
      0.0262906111905873, 0.0263674066536486 };
    double[] time2 = new double[] {0.084931506849315, 0.172602739726027, 0.252054794520548, 0.501369863013699,
      0.747945205479452, 1, 2.0027397260274, 3.0027397260274, 4.00821917808219, 5.00547945205479, 6.00547945205479,
      7.00547945205479, 8.00547945205479, 9.00547945205479, 10.0109589041096, 12.0082191780822, 15.0164383561644,
      20.013698630137, 25.0164383561644, 30.0219178082192, 35.0246575342466, 40.027397260274, 45.0301369863014,
      50.0356164383562 };
    double[] rate2 = new double[] {0.00505581436649715, 0.00503291190352002, 0.00514256786002282, 0.00569033898394734,
      0.00639149506676498, 0.00723232237139851, 0.0105298436453575, 0.0135410574559515, 0.0158030551363141,
      0.0174456995488895, 0.0187732338667046, 0.0199055537021988, 0.0208531180033394, 0.0216810024002466,
      0.0224286195281712, 0.0237374945104143, 0.0252160567356682, 0.0267142361493482, 0.0272605000077873,
      0.0275097501922553, 0.0272973928154239, 0.02732399981293, 0.0272679024586238, 0.0273500955580994 };
    double[] time3 = new double[] {0.252054794520548, 0.446575342465753, 0.695890410958904, 0.945205479452055,
      1.19452054794521, 1.44383561643836, 1.69315068493151, 1.96164383561644, 2.21095890410959, 3.0027397260274,
      4.00821917808219, 5.00547945205479, 6.00547945205479, 7.00547945205479, 8.00547945205479, 9.00547945205479,
      10.0109589041096, 12.0082191780822, 15.0164383561644, 20.013698630137, 25.0164383561644, 30.0219178082192,
      35.0246575342466, 40.027397260274, 45.0301369863014, 50.0356164383562 };
    double[] rate3 = new double[] {0.00562351263918076, 0.00605140841711946, 0.00674163439039939, 0.00751172986806733,
      0.00835257242596894, 0.00924368840059903, 0.0101182341187527, 0.0110392349927795, 0.0118815761688227,
      0.0141891783018791, 0.0164362685797301, 0.0180908568757903, 0.0194306164834304, 0.0205622948941999,
      0.021509365473766, 0.0223369044043144, 0.0230980379156519, 0.0243918201709714, 0.0258555923419827,
      0.0273230838882434, 0.0278368027650289, 0.0280342598539863, 0.0278259808180962, 0.0277923406323035,
      0.0277421859831982, 0.0278284728253182 };
    double[] time4 = new double[] {0.501369863013699, 1, 1.5013698630137, 2.0027397260274, 3.0027397260274,
      4.00821917808219, 5.00547945205479, 6.00547945205479, 7.00547945205479, 8.00547945205479, 9.00547945205479,
      10.0109589041096, 12.0082191780822, 15.0164383561644, 20.013698630137, 25.0164383561644, 30.0219178082192,
      35.0246575342466, 40.027397260274, 45.0301369863014, 50.0356164383562 };
    double[] rate4 = new double[] {0.00710174175249047, 0.00860451137005419, 0.0103551375418353, 0.0121086290753976,
      0.015178756626544, 0.0175003704137641, 0.0192427630940745, 0.0206581597595687, 0.0218403443388774,
      0.0228520936339576, 0.0237315739821965, 0.0245039864998691, 0.025779797672448, 0.0271525952264722,
      0.0284196247149596, 0.0287909199539885, 0.0288932996886611, 0.0286980928245931, 0.0285278998759854,
      0.0284929581122082, 0.0284672034337625 };

    int n = 30;
    double tol = 1.e-6;
    double interval;
    double[] expectedLeft1 = new double[] {0.00432241885364084, 0.003961534149119456, 0.004141976501380142,
      0.00420212395213371, 0.004232197677510493, 0.0042502419127365635, 0.0042622714028872745, 0.00427086389585207,
      0.004277308265575667, 0.004282320553138462, 0.004286330383188701, 0.004289611153229805, 0.004292345128264058,
      0.00429465849175458, 0.004296641374746455, 0.0042983598733394145, 0.004299863559608253, 0.004301190341610171,
      0.0043023697033896515, 0.004303424921823924, 0.004304374618414771, 0.0043052338677112495, 0.004306015003435322,
      0.004306728214313824, 0.0043073819909524485, 0.004307983465459984, 0.00430853867269771, 0.004309052753473382,
      0.004309530114193647, 0.00430997455348493 };
    double[] expectedLeft2 = new double[] {0.004930523545038513, 0.008563957367338998, 0.006747240456188747,
      0.006141668152472008, 0.005838882000613631, 0.005657210309498608, 0.005536095848755262, 0.005449585519652867,
      0.005384702772826073, 0.005334238414183011, 0.00529386692726856, 0.005260835710702192, 0.005233309696896886,
      0.005210018454446241, 0.00519005453234569, 0.005172752466525211, 0.005157613158932293, 0.005144254946350305,
      0.005132380979610762, 0.005121756904106959, 0.005112195236153537, 0.005103544203243298, 0.005095679627870353,
      0.005088498928616794, 0.0050819166209677, 0.005075860897930532, 0.005070270999742377, 0.005065095168086679,
      0.0050602890386921015, 0.00505581436649715 };
    double[] expectedLeft3 = new double[] {0.0063629856998657, -0.015081733059997554, -0.004359373680065933,
      -7.852538867553844E-4, 0.00100180600989989, 0.0020740419478930496, 0.002788865906555156, 0.003299454448456664,
      0.0036823958548827954, 0.003980239170992006, 0.004218513823879376, 0.004413465812605405, 0.0045759258032104285,
      0.004713391949106987, 0.004831220074161181, 0.004933337782541483, 0.005022690777374247, 0.005101531655167861,
      0.005171612435428853, 0.005234316291451845, 0.005290749761872537, 0.005341808616062689, 0.0053882257562355524,
      0.005430606623349906, 0.005469455751538065, 0.00550519694947117, 0.005538188824486344, 0.005568736856907801,
      0.005597102887013441, 0.00562351263918076 };
    double[] expectedLeft4 = new double[] {0.009323892257929966, -0.05511847239981542, -0.02289729007094271,
      -0.01215689596131849, -0.006786698906506367, -0.0035645806736191036, -0.0014165018516942615,
      1.1784016396634015E-4, 0.0012685966757117946, 0.002163629518180481, 0.0028796557921554303,
      0.003465495470862203, 0.003953695203117851, 0.004366787284257245, 0.0047208662109481546, 0.0050277346140802735,
      0.00529624446682088, 0.005533164925121415, 0.005743760888055224, 0.005932188854890736, 0.006101774025042697,
      0.006255208226608758, 0.006394693864396085, 0.006522050316288864, 0.006638793730523908, 0.006746197671620151,
      0.006845339771093605, 0.006937138011346804, 0.00702237923443906, 0.00710174175249047 };
    interval = time1[0] / (n - 1);
    assertCurveInterpolation("curve1, left extrapolation", expectedLeft1, time1, rate1, 0.0, interval, n, tol);
    interval = time2[0] / (n - 1);
    assertCurveInterpolation("curve2, left extrapolation", expectedLeft2, time2, rate2, 0.0, interval, n, tol);
    interval = time3[0] / (n - 1);
    assertCurveInterpolation("curve3, left extrapolation", expectedLeft3, time3, rate3, 0.0, interval, n, tol);
    interval = time4[0] / (n - 1);
    assertCurveInterpolation("curve4, left extrapolation", expectedLeft4, time4, rate4, 0.0, interval, n, tol);

    double[] expectedInterp1 = new double[] {0.00430997455348493, 0.008633736145209193, 0.013342661423362075,
      0.01619690305470609, 0.018239472342514093, 0.019813481623031277, 0.021148488603245606, 0.022295129127299116,
      0.0232760410722643, 0.024111570540299283, 0.024806838503060705, 0.025351737313310033, 0.025739317788841698,
      0.02599988135829853, 0.026197286469297604, 0.026378953913944004, 0.026536993204815812, 0.026624638165813695,
      0.026607951231641776, 0.02651814561956947, 0.026422578076638413, 0.02637196808365516, 0.02636017718578407,
      0.0263552990365331, 0.026334395081679077, 0.02630635647174156, 0.026290810081083863, 0.026301055806632525,
      0.026330445866155728, 0.0263674066536486 };
    double[] expectedInterp2 = new double[] {0.00505581436649715, 0.009895784888332884, 0.014819869256340384,
      0.017795352780938436, 0.019873631605821673, 0.021436035701199795, 0.022716195364051613, 0.023815268413068243,
      0.02470916929393918, 0.025445091083672374, 0.026048701645762295, 0.02651364340131122, 0.026834958717271002,
      0.027042423157990946, 0.027194383766823444, 0.02733365912995944, 0.027452704721998362, 0.027510783204702596,
      0.027479153572030988, 0.027390814729589083, 0.02731089329719233, 0.02728723165463167, 0.02730517776811378,
      0.027323455271577174, 0.02731275214507357, 0.027285611618745393, 0.02726820729595548, 0.027278589154227453,
      0.027310138832208883, 0.0273500955580994 };
    double[] expectedInterp3 = new double[] {0.00562351263918076, 0.011063492702706344, 0.015788399300860928,
      0.018650952326454865, 0.0206779530639229, 0.02220176178807306, 0.023477373313433858, 0.02453962212881623,
      0.025408711275890988, 0.026125737329425684, 0.026707539275361934, 0.027150860002250272, 0.027454073660366327,
      0.027647988781915416, 0.027785295519440416, 0.027903896659411603, 0.02799828895796063, 0.028037866458427044,
      0.02800197601659675, 0.02791912646971664, 0.02784027635535035, 0.027802514907529146, 0.02779637762405553,
      0.02779408657189797, 0.027776321495836885, 0.027752790087081092, 0.027741994847386625, 0.027756257243025217,
      0.027788819016457814, 0.0278284728253182 };
    double[] expectedInterp4 = new double[] {0.00710174175249047, 0.012799015779984229, 0.017318198072358112,
      0.020151722545359834, 0.0221888202830137, 0.023761250303376752, 0.025014118326630412, 0.026022702192962308,
      0.026819183698792878, 0.027452383888520546, 0.027945855738154994, 0.028306137851051752, 0.02853915311217904,
      0.028677264144043577, 0.02876527218987293, 0.028834726622994678, 0.02888455275987619, 0.028898012337487098,
      0.02886409176538647, 0.028795751469241702, 0.028715013291823743, 0.028639130076908027, 0.028576714157654476,
      0.028532440721874056, 0.028508701455636148, 0.028498674419406414, 0.028493356885492215, 0.028486199768680682,
      0.02847700252606377, 0.0284672034337625 };
    interval = (time1[time1.length - 1] - time1[0]) / (n - 1);
    assertCurveInterpolation("curve1, interpolation", expectedInterp1, time1, rate1, time1[0], interval, n, tol);
    interval = (time2[time2.length - 1] - time2[0]) / (n - 1);
    assertCurveInterpolation("curve2, interpolation", expectedInterp2, time2, rate2, time2[0], interval, n, tol);
    interval = (time3[time3.length - 1] - time3[0]) / (n - 1);
    assertCurveInterpolation("curve3, interpolation", expectedInterp3, time3, rate3, time3[0], interval, n, tol);
    interval = (time4[time4.length - 1] - time4[0]) / (n - 1);
    assertCurveInterpolation("curve4, interpolation", expectedInterp4, time4, rate4, time4[0], interval, n, tol);

    n = 15;
    double[] expectedRight1 = new double[] {0.0263674066536486, 0.026382572814350247, 0.026397319442305188,
      0.026411663707963506, 0.026425621857393427, 0.02643920927366107, 0.026452440533383473, 0.02646532945889201,
      0.02647788916639863, 0.026490132110517765, 0.026502070125461533, 0.026513714463194785, 0.026525075828808434,
      0.02653616441334495, 0.026546989924287463 };
    double[] expectedRight2 = new double[] {0.0273500955580994, 0.027366532781822845, 0.027382515312117106,
      0.027398061758473074, 0.027413189728528138, 0.027427915894590097, 0.027442256054929854, 0.027456225190316607,
      0.027469837516220872, 0.027483106531067714, 0.02749604506088451, 0.02750866530065369, 0.027520978852650743,
      0.027532996762020718, 0.02754472954982267 };
    double[] expectedRight3 = new double[] {0.0278284728253182, 0.027844762177093575, 0.027860600925936094,
      0.02787600751392262, 0.027890999390289366, 0.02790559307735732, 0.027919804231273562, 0.02793364769803786,
      0.027947137565236018, 0.02796028720985907, 0.027973109342549363, 0.027985616048581272, 0.02799781882585432,
      0.02800972862014963, 0.028021355857877083 };
    double[] expectedRight4 = new double[] {0.0284672034337625, 0.028463230722600225, 0.028459367906253023,
      0.028455610486994683, 0.028451954209236862, 0.028448395043450942, 0.028444929171354227, 0.028441552972245907,
      0.02843826301039016, 0.028435056023353823, 0.02843192891121545, 0.028428878726570815, 0.028425902665266973,
      0.028422998057803774, 0.02842016236134738 };
    interval = (60.0 - time1[time1.length - 1]) / (n - 1);
    assertCurveInterpolation("curve1, right extrapolation", expectedRight1, time1, rate1, time1[time1.length - 1], interval,
        n, tol);
    interval = (60.0 - time2[time2.length - 1]) / (n - 1);
    assertCurveInterpolation("curve2, right extrapolation", expectedRight2, time2, rate2, time2[time2.length - 1], interval,
        n, tol);
    interval = (60.0 - time3[time3.length - 1]) / (n - 1);
    assertCurveInterpolation("curve3, right extrapolation", expectedRight3, time3, rate3, time3[time3.length - 1], interval,
        n, tol);
    interval = (60.0 - time4[time4.length - 1]) / (n - 1);
    assertCurveInterpolation("curve4, right extrapolation", expectedRight4, time4, rate4, time4[time4.length - 1], interval,
        n, tol);
  }

  private void assertCurveInterpolation(
      String message, double[] expected, double[] times, double[] rates, double rebate,
      double interval, int nKeys, double relativeTol) {

    Interpolator1D interp = new ProductPiecewisePolynomialInterpolator1D(new NaturalSplineInterpolator());
    CombinedInterpolatorExtrapolator combInterp = new CombinedInterpolatorExtrapolator(interp,
        new ReciprocalExtrapolator1D());
    Interpolator1DDataBundle dataBundle = combInterp.getDataBundle(times, rates);

    for (int i = 0; i < nKeys; ++i) {
      double key = rebate + interval * i;
      double res = combInterp.interpolate(dataBundle, key);
      InterpolatorTestUtil.assertRelative(message, expected[i], res, relativeTol);
    }
  }

  //-------------------------------------------------------------------------
  private static final double[] S_ARR = new double[] {1d, 2d, 3d, 4d};
  private static final ProductPiecewisePolynomialInterpolator1D S_INTERP = new ProductPiecewisePolynomialInterpolator1D(
      INTERP_SENSE[0]);
  private static final Interpolator1DDataBundle S_DATA = S_INTERP.getDataBundle(S_ARR, S_ARR);

  /**
   * base interpolator is null
   */
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullBaseInterpTest1() {
    new ProductPiecewisePolynomialInterpolator1D(null);
  }

  /**
   * base interpolator is null
   */
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullBaseInterpTest2() {
    new ProductPiecewisePolynomialInterpolator1D(null, S_ARR, S_ARR);
  }

  /**
   * clamped point is null
   */
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullXClampedInterpTest() {
    new ProductPiecewisePolynomialInterpolator1D(INTERP_SENSE[1], null, S_ARR);
  }

  /**
   * clamped point is null
   */
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullYClampedInterpTest() {
    new ProductPiecewisePolynomialInterpolator1D(INTERP_SENSE[1], S_ARR, null);
  }

  /**
   * data bundle is null
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullDataInterpTest() {
    S_INTERP.interpolate(null, 1.5);
  }

  /**
   * Double value is null
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullValueInterpTest() {
    S_INTERP.interpolate(S_DATA, null);
  }

  /**
   * data bundle is null
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullDataDerivTest() {
    S_INTERP.firstDerivative(null, 1.5);
  }

  /**
   * Double value is null
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullValueDerivTest() {
    S_INTERP.firstDerivative(S_DATA, null);
  }

  /**
   * data bundle is null
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullDataSenseTest() {
    S_INTERP.getNodeSensitivitiesForValue(null, 1.5);
  }

  /**
   * Double value is null
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullValueSenseTest() {
    S_INTERP.getNodeSensitivitiesForValue(S_DATA, null);
  }

}
