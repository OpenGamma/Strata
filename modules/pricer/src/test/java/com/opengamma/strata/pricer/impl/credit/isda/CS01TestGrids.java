/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getIMMDateSet;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getNextIMMDate;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.getPrevIMMDate;
import static com.opengamma.strata.pricer.impl.credit.isda.ImmDateLogic.isIMMDate;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.ShiftType;

/**
 * Test.
 */
@Test
public class CS01TestGrids extends IsdaBaseTest {

  private static final MarketQuoteConverter QUOTE_CONVERTER = new MarketQuoteConverter();

  private static final Period[] PILLARS = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5), Period.ofYears(7),
    Period.ofYears(10) };
  private static final Period[] BUCKETS = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(2), Period.ofYears(3), Period.ofYears(4), Period.ofYears(5), Period.ofYears(6),
    Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10) };

  private static final double NOTIONAL = 1e6;
  private static final double COUPON = 0.01;
  private static final double RECOVERY = 0.40;
  private static final Period NON_IMM_TENOR = Period.ofMonths(6);
  private static final CdsAnalyticFactory IMM_CDS_FACTORY = new CdsAnalyticFactory(RECOVERY);
  private static final CdsAnalyticFactory NON_IMM_CDS_FACTORY = IMM_CDS_FACTORY.with(NON_IMM_TENOR);

  @Test(enabled = false)
  public void gbpTest() {
    final LocalDate tradeDate = LocalDate.of(2013, Month.SEPTEMBER, 5);
    final CdsAnalytic cds = IMM_CDS_FACTORY.makeImmCds(tradeDate, Period.ofYears(10));
    final LocalDate spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 1);
    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    final double[] rates = new double[] {0.004919, 0.005006, 0.00515, 0.005906, 0.008813, 0.0088, 0.01195, 0.01534, 0.01836, 0.02096, 0.02322, 0.02514, 0.02673, 0.02802, 0.02997, 0.0318, 0.03331,
      0.03383, 0.034 };
    final IsdaCompliantYieldCurve yieldCurve = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT_ACT_ISDA, ACT_ACT_ISDA, Period.ofMonths(6));
    final double tradelevel = 130.5 * ONE_BP;
    System.out.println("Accrued days: " + cds.getAccuredDays());

    final double cs01 = NOTIONAL * ONE_BP * CS01_CAL.parallelCS01(cds, new CdsQuotedSpread(COUPON, tradelevel), yieldCurve, ONE_BP);
    System.out.println("CS01: " + cs01);

    final int day = 5;
    final Month month = Month.SEPTEMBER;
    for (int i = 0; i < 10; i++) {
      final int year = 2014 + i;
      final LocalDate mat = LocalDate.of(year, month, day);
      final double t = ACT365F.yearFraction(tradeDate, mat);
      final double df = yieldCurve.getDiscountFactor(t);
      System.out.println(mat + "\t" + t + "\t" + df);
    }

  }

  @Test(enabled = false)
  public void aegonTest() {
    final String name = "aegon";

    final LocalDate tradeDate = LocalDate.of(2013, Month.SEPTEMBER, 2);
    final LocalDate firstMaturity = getNextIMMDate(tradeDate);
    final LocalDate[] maturities = getIMMDateSet(firstMaturity, 41);

    final double[] spreads = new double[] {0.003372, 0.003751, 0.004117, 0.004892, 0.00557, 0.006765, 0.007629, 0.008325, 0.008907, 0.009919, 0.010759, 0.011492, 0.012135, 0.012951, 0.013681,
      0.014336, 0.014927, 0.015745, 0.016472, 0.017147, 0.017754, 0.018181, 0.018569, 0.018953, 0.019306, 0.019671, 0.02001, 0.020339, 0.020647, 0.020829, 0.021013, 0.021182, 0.021351, 0.021499,
      0.021649, 0.021786, 0.022041, 0.022181, 0.022328, 0.02246, 0.022601 };

    //yield curve
    final LocalDate spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 3);
    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "9M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "30Y" };
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    final double[] rates = new double[] {0.00129, 0.00175, 0.00224, 0.00343, 0.0045, 0.00545, 0.00628, 0.00862, 0.01121, 0.01365, 0.01576, 0.01758, 0.01917, 0.0206, 0.02188, 0.02395, 0.02595, 0.0271,
      0.0271 };
    final IsdaCompliantYieldCurve yieldCurve = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofYears(1));

    runGrid(name, tradeDate, maturities, COUPON, RECOVERY, spreads, yieldCurve);
  }

  @Test(enabled = false)
  public void russiaTest() {
    final String name = "Russia";

    final LocalDate tradeDate = LocalDate.of(2013, Month.AUGUST, 7);
    final LocalDate firstMaturity = LocalDate.of(2013, Month.AUGUST, 20);
    final LocalDate lastMaturity = LocalDate.of(2023, Month.SEPTEMBER, 20);
    final LocalDate[] maturities = getAllMaturities(firstMaturity, lastMaturity);

    final double[] spreads = new double[] {0.006345, 0.006374, 0.006613, 0.006754, 0.006846, 0.006919, 0.006991, 0.00706, 0.007292, 0.007483, 0.007652, 0.007897, 0.008114, 0.008307, 0.008571,
      0.008812, 0.009032, 0.009242, 0.009435, 0.009603, 0.009769, 0.009918, 0.010065, 0.010204, 0.010339, 0.010467, 0.010664, 0.010854, 0.011028, 0.011202, 0.011367, 0.011515, 0.011669, 0.011811,
      0.011952, 0.012092, 0.01223, 0.012363, 0.012583, 0.012801, 0.013005, 0.013206, 0.013402, 0.013579, 0.013761, 0.013935, 0.014113, 0.014274, 0.014438, 0.014604, 0.014823, 0.015052, 0.015284,
      0.015486, 0.015697, 0.015905, 0.01609, 0.016284, 0.016492, 0.016649, 0.016817, 0.017014, 0.01716, 0.017327, 0.017517, 0.017645, 0.017802, 0.017972, 0.018089, 0.018236, 0.018419, 0.018504,
      0.018633, 0.018806, 0.018873, 0.018989, 0.019149, 0.019206, 0.019311, 0.019464, 0.019513, 0.019611, 0.019769, 0.019803, 0.019896, 0.02005, 0.020068, 0.020148, 0.020291, 0.020305, 0.020383,
      0.020522, 0.020526, 0.020596, 0.020737, 0.02073, 0.020796, 0.020933, 0.02092, 0.020981, 0.021115, 0.021099, 0.021157, 0.021286, 0.021266, 0.02132, 0.021455, 0.021428, 0.021481, 0.021615,
      0.021581, 0.02163, 0.02176, 0.021724, 0.02177, 0.021897, 0.021858, 0.021902, 0.022033, 0.021981, 0.022025, 0.022122 };

    //yield curve
    final LocalDate spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 3);
    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    final double[] rates = new double[] {0.00185, 0.00227, 0.002664, 0.003955, 0.006654, 0.004845, 0.00784, 0.011725, 0.0157, 0.01919, 0.02219, 0.024565, 0.02657, 0.02825, 0.03095, 0.033495,
      0.035505, 0.036425, 0.03691500 };
    final IsdaCompliantYieldCurve yieldCurve = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofMonths(6));

    runGrid(name, tradeDate, maturities, COUPON, RECOVERY, spreads, yieldCurve);
  }

  @Test(enabled = false)
  public void gazpromTest() {
    final String name = "Gazprom";

    final LocalDate tradeDate = LocalDate.of(2013, Month.AUGUST, 7);
    final LocalDate firstMaturity = LocalDate.of(2013, Month.AUGUST, 20);
    final LocalDate lastMaturity = LocalDate.of(2023, Month.SEPTEMBER, 20);
    final LocalDate[] maturities = getAllMaturities(firstMaturity, lastMaturity);

    final double[] spreads = new double[] {0.008249, 0.008249, 0.008243, 0.008239, 0.008235, 0.008398, 0.008548, 0.008681, 0.00898, 0.009229, 0.009454, 0.009814, 0.010136, 0.01043, 0.010901,
      0.011348, 0.011739, 0.012099, 0.01244, 0.012743, 0.013042, 0.013325, 0.013613, 0.013844, 0.014074, 0.014307, 0.014627, 0.014943, 0.015254, 0.015522, 0.015797, 0.01607, 0.016302, 0.016535,
      0.016796, 0.016994, 0.017215, 0.017459, 0.017736, 0.018038, 0.01836, 0.018602, 0.018873, 0.019163, 0.019366, 0.019611, 0.019907, 0.020078, 0.020306, 0.020591, 0.020811, 0.021096, 0.021445,
      0.021633, 0.021895, 0.022227, 0.02238, 0.022617, 0.022968, 0.023063, 0.023273, 0.023606, 0.023641, 0.023806, 0.024101, 0.024108, 0.02426, 0.024558, 0.024546, 0.024688, 0.025002, 0.024957,
      0.025085, 0.025391, 0.025318, 0.025426, 0.025718, 0.025634, 0.025738, 0.026032, 0.025936, 0.026033, 0.026337, 0.026221, 0.026312, 0.026614, 0.026482, 0.026564, 0.026863, 0.026723, 0.026801,
      0.0271, 0.02695, 0.027024, 0.027334, 0.027166, 0.027233, 0.027542, 0.027364, 0.027427, 0.027736, 0.027551, 0.027613, 0.027922, 0.02773, 0.027788, 0.028107, 0.027903, 0.027958, 0.028277,
      0.028065, 0.028118, 0.028437, 0.02822, 0.028272, 0.028591, 0.028368, 0.028417, 0.028744, 0.028548, 0.028581, 0.028847 };

    //yield curve
    final LocalDate spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 3);
    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    final double[] rates = new double[] {0.00185, 0.00227, 0.002664, 0.003955, 0.006654, 0.004845, 0.00784, 0.011725, 0.0157, 0.01919, 0.02219, 0.024565, 0.02657, 0.02825, 0.03095, 0.033495,
      0.035505, 0.036425, 0.03691500 };
    final IsdaCompliantYieldCurve yieldCurve = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofMonths(6));

    runGrid(name, tradeDate, maturities, COUPON, 0.25, spreads, yieldCurve);
  }

  @Test(enabled = false)
  public void brazilTest() {
    final String name = "Brazil";

    final LocalDate tradeDate = LocalDate.of(2013, Month.AUGUST, 7);
    final LocalDate firstMaturity = LocalDate.of(2013, Month.AUGUST, 20);
    final LocalDate lastMaturity = LocalDate.of(2023, Month.SEPTEMBER, 20);
    final LocalDate[] maturities = getAllMaturities(firstMaturity, lastMaturity);

    final double[] spreads = new double[] {0.004537, 0.004538, 0.004795, 0.004926, 0.004998, 0.005168, 0.005286, 0.005362, 0.00581, 0.006139, 0.006414, 0.006798, 0.007124, 0.007399, 0.007853,
      0.008263, 0.008613, 0.008934, 0.009221, 0.009451, 0.009688, 0.009894, 0.010091, 0.010261, 0.010424, 0.010578, 0.010802, 0.01102, 0.011223, 0.011405, 0.011584, 0.011746, 0.011892, 0.012034,
      0.012183, 0.012297, 0.012422, 0.012551, 0.012746, 0.012951, 0.013157, 0.013321, 0.013497, 0.013673, 0.013807, 0.013957, 0.014132, 0.014236, 0.014371, 0.014535, 0.0147, 0.0149, 0.015133,
      0.015273, 0.015448, 0.01566, 0.015767, 0.015921, 0.016141, 0.016217, 0.016359, 0.016575, 0.016639, 0.016778, 0.016998, 0.017042, 0.017168, 0.017378, 0.017401, 0.017511, 0.017732, 0.017731,
      0.017836, 0.018057, 0.018043, 0.018147, 0.018374, 0.018345, 0.018441, 0.018667, 0.01862, 0.018705, 0.018941, 0.018874, 0.018956, 0.019194, 0.019102, 0.019168, 0.019394, 0.019293, 0.019355,
      0.019579, 0.019468, 0.019523, 0.019755, 0.019633, 0.019686, 0.019919, 0.019789, 0.01984, 0.020074, 0.019937, 0.019985, 0.020219, 0.020073, 0.020116, 0.020357, 0.020202, 0.020245, 0.020487,
      0.020326, 0.020367, 0.020612, 0.020445, 0.020484, 0.020728, 0.020554, 0.02059, 0.020841, 0.020659, 0.020694, 0.020947 };

    //yield curve
    final LocalDate spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 3);
    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    final double[] rates = new double[] {0.00185, 0.00227, 0.002664, 0.003955, 0.006654, 0.004845, 0.00784, 0.011725, 0.0157, 0.01919, 0.02219, 0.024565, 0.02657, 0.02825, 0.03095, 0.033495,
      0.035505, 0.036425, 0.03691500 };
    final IsdaCompliantYieldCurve yieldCurve = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofMonths(6));

    runGrid(name, tradeDate, maturities, COUPON, RECOVERY, spreads, yieldCurve);
  }

  /**
   * This test data given as PUF 
   */
  @Test(enabled = false)
  public void argentTest() {
    final String name = "Argent";
    final double coupon = 0.05;
    final double rr = 0.25;

    final LocalDate tradeDate = LocalDate.of(2013, Month.OCTOBER, 17);
    final LocalDate firstMaturity = LocalDate.of(2013, Month.OCTOBER, 20);
    final LocalDate lastMaturity = LocalDate.of(2023, Month.DECEMBER, 20);
    final LocalDate[] maturities = getAllMaturities(firstMaturity, lastMaturity);

    final double[] puf = new double[] {0.0017, 0.0155, 0.0286, 0.0417, 0.0546, 0.0659, 0.0882, 0.109, 0.1296, 0.146, 0.1624, 0.1782, 0.1965, 0.2145, 0.2313, 0.2458, 0.2597, 0.2717, 0.2846, 0.2967,
      0.3086, 0.3196, 0.3306, 0.3411, 0.3507, 0.3603, 0.3691, 0.3723, 0.3754, 0.3782, 0.3806, 0.3828, 0.3851, 0.387, 0.3889, 0.3908, 0.3927, 0.3946, 0.3964, 0.3981, 0.3998, 0.4012, 0.4029, 0.4044,
      0.406, 0.4076, 0.4091, 0.4106, 0.4122, 0.4137, 0.415, 0.4167, 0.4182, 0.4195, 0.4212, 0.4226, 0.4241, 0.4256, 0.4271, 0.4284, 0.4298, 0.4311, 0.4323, 0.4332, 0.434, 0.4346, 0.4354, 0.4361,
      0.4367, 0.4374, 0.4381, 0.4386, 0.4394, 0.44, 0.4405, 0.4412, 0.4417, 0.4421, 0.4427, 0.4433, 0.4437, 0.4443, 0.4448, 0.4452, 0.4457, 0.4462, 0.4466, 0.4471, 0.4475, 0.4478, 0.4483, 0.4486,
      0.4489, 0.4493, 0.4497, 0.4499, 0.4504, 0.4508, 0.451, 0.4513, 0.4516, 0.4516, 0.452, 0.4523, 0.4524, 0.4527, 0.4529, 0.453, 0.4534, 0.4536, 0.4537, 0.454, 0.4542, 0.4542, 0.4545, 0.4546,
      0.4547, 0.455, 0.4551, 0.4551, 0.4554, 0.4555, 0.4555 };

    //yield curve
    final LocalDate spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 3);
    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    final double[] rates = new double[] {0.001755, 0.002163, 0.002461, 0.003644, 0.006296, 0.004685, 0.00757, 0.01152, 0.015555, 0.019, 0.021885, 0.024305, 0.026315, 0.02804, 0.03069, 0.03322,
      0.035355, 0.03643, 0.03697 };
    final IsdaCompliantYieldCurve yieldCurve = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofMonths(6));

    runPUFGrid(name, tradeDate, maturities, coupon, rr, puf, yieldCurve);

  }

  @Test(enabled = false)
  public void argentTest2() {
    final String name = "Argent";
    final double coupon = 0.05;
    final double rr = 0.25;

    final LocalDate tradeDate = LocalDate.of(2013, Month.OCTOBER, 17);
    final LocalDate firstMaturity = LocalDate.of(2013, Month.OCTOBER, 20);
    final LocalDate lastMaturity = LocalDate.of(2023, Month.DECEMBER, 20);
    final LocalDate[] maturities = getAllMaturities(firstMaturity, lastMaturity);

    final double[] qSpreads = new double[] {0.20, 0.216339819827789, 0.215026975292814, 0.2140784051537, 0.213986209900337, 0.213733114478896, 0.236019328256488, 0.251607596246203, 0.26362232382798,
      0.268042726166823, 0.271780439052978, 0.274797009215456, 0.282095157839183, 0.288300465146796, 0.293561155538839, 0.295591514818445, 0.29729057005055, 0.298517390580295, 0.299803497765076,
      0.300993122482366, 0.301936387699877, 0.302639692320837, 0.303343347699392, 0.303880329866993, 0.304138762809806, 0.304430335706418, 0.304527338232278, 0.299061023763049, 0.293974349695976,
      0.289496800367118, 0.284522691790816, 0.279961135583844, 0.275603109124033, 0.271387082299889, 0.267275587121691, 0.263430343128684, 0.259988447239805, 0.256620429569045, 0.253528850163382,
      0.250395611588397, 0.24746221669694, 0.244855071581835, 0.242231733937488, 0.239740525695391, 0.237339961842982, 0.235175168343644, 0.232961310022416, 0.230862732649384, 0.229036482110206,
      0.227157070953311, 0.22532597412706, 0.223767825467926, 0.222170697637686, 0.220761519883318, 0.219447432039878, 0.218093713278725, 0.216798529925898, 0.215636968947012, 0.214478293265545,
      0.21324943813095, 0.21220501810934, 0.211097614146918, 0.210040091113559, 0.208779319222011, 0.207518649012888, 0.206345676987996, 0.20517599772974, 0.204051722843812, 0.202857480006609,
      0.201813591489336, 0.200770647273373, 0.199645293132785, 0.1987811426371, 0.197796948605989, 0.196829332793193, 0.195969669292786, 0.195033278349195, 0.194145290528741, 0.193324674700498,
      0.192578433887757, 0.191705833636468, 0.19100815408059, 0.190252430070042, 0.189463716602488, 0.188786025531258, 0.188108350903912, 0.18742568528085, 0.186788889440548, 0.18612527281304,
      0.185508037546637, 0.184936293649623, 0.184305735541621, 0.183666614884945, 0.183124570127554, 0.18258230910081, 0.18194485032407, 0.181512245858378, 0.181023830671359, 0.180461170291072,
      0.179944609474692, 0.179450226660242, 0.178866018589262, 0.178450528319099, 0.1780202969245, 0.177472840937738, 0.177064341863839, 0.176601428646639, 0.176095220061246, 0.175779436454306,
      0.17535517129127, 0.174903632782417, 0.174552288005534, 0.174163744280323, 0.173723383428186, 0.173405169706597, 0.173010120198239, 0.172608427416311, 0.172335663591248, 0.171956095953899,
      0.171530931604331, 0.17128614937871, 0.170935016451065, 0.170550536934353 };

    //yield curve
    final LocalDate spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 3);
    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    final double[] rates = new double[] {0.001755, 0.002163, 0.002461, 0.003644, 0.006296, 0.004685, 0.00757, 0.01152, 0.015555, 0.019, 0.021885, 0.024305, 0.026315, 0.02804, 0.03069, 0.03322,
      0.035355, 0.03643, 0.03697 };
    final IsdaCompliantYieldCurve yieldCurve = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofMonths(6));

    runGrid(name, tradeDate, maturities, coupon, rr, qSpreads, yieldCurve);

  }

  private void runPUFGrid(final String name, final LocalDate tradeDate, final LocalDate[] maturities, final double coupon, final double recovery, final double[] puf,
      final IsdaCompliantYieldCurve yieldCurve) {
    final int nMat = maturities.length;
    ArgChecker.isTrue(nMat == puf.length, "wrong number of spreads");
    final CdsAnalyticFactory immFactory = IMM_CDS_FACTORY.withRecoveryRate(recovery);
    final CdsAnalyticFactory nonImmFactory = NON_IMM_CDS_FACTORY.withRecoveryRate(recovery);
    final LocalDate startDate = getPrevIMMDate(tradeDate);
    final LocalDate nextIMM = getNextIMMDate(tradeDate);
    final LocalDate[] pillarDates = getIMMDateSet(nextIMM, PILLARS);
    final double[] pillarPUF = getSpreadsAtDates(maturities, puf, pillarDates);

    //NOTE the curve is build with SA coupons and T+1 accrual for non-IMM - is this correct?
    final CdsAnalytic[] pillarCDSsIMM = immFactory.makeCds(tradeDate, startDate, pillarDates);
    // CDSAnalytic[] pillarCDSsNonIMM = nonImmFactory.makeCDS(tradeDate, tradeDate.plusDays(1), pillarDates);
    final LocalDate[] bucketDates = getIMMDateSet(nextIMM, BUCKETS);
    final CdsAnalytic[] bucketCDSsIMM = immFactory.makeImmCds(tradeDate, BUCKETS);
    final CdsAnalytic[] bucketCDSsNonIMM = nonImmFactory.makeCds(tradeDate, tradeDate.plusDays(1), bucketDates);
    final int nPillars = pillarDates.length;
    final double[] premiums = new double[nPillars];
    Arrays.fill(premiums, coupon);

    final double[] upfrontAmount = new double[nMat];
    final double[] parellelCS01_A = new double[nMat];
    final double[] parellelCS01_B = new double[nMat];
    final double[][] bucketedCS01 = new double[nMat][];
    final int[] accDays = new int[nMat];

    for (int i = 0; i < nMat; i++) {
      if (isIMMDate(maturities[i])) {
        final CdsAnalytic pricingCDS = immFactory.makeCds(tradeDate, startDate, maturities[i]);
        System.out.println(QUOTE_CONVERTER.pufToQuotedSpread(pricingCDS, coupon, yieldCurve, puf[i]));
        accDays[i] = pricingCDS.getAccuredDays();
        parellelCS01_A[i] = CS01_CAL.parallelCS01FromPUF(pricingCDS, coupon, yieldCurve, puf[i], ONE_BP);
        bucketedCS01[i] = CS01_CAL.bucketedCS01FromPUF(pricingCDS, new PointsUpFront(coupon, puf[i]), yieldCurve, bucketCDSsIMM, ONE_BP);
      } else {
        final CdsAnalytic pricingCDS = immFactory.makeCds(tradeDate, tradeDate.plusDays(1), maturities[i]); //nonImmFactory.makeCDS(tradeDate, tradeDate.plusDays(1), maturities[i]);

        accDays[i] = pricingCDS.getAccuredDays();

        parellelCS01_A[i] = CS01_CAL.parallelCS01FromPUF(pricingCDS, coupon, yieldCurve, puf[i], ONE_BP);

        final double[] pillarQS = QUOTE_CONVERTER.pufToQuotedSpreads(pillarCDSsIMM, coupon, yieldCurve, pillarPUF);
        final IsdaCompliantCreditCurve creditCurve = CREDIT_CURVE_BUILDER.calibrateCreditCurve(pillarCDSsIMM, premiums, yieldCurve, pillarPUF);
        final double qSpread = QUOTE_CONVERTER.pufToQuotedSpread(pricingCDS, coupon, yieldCurve, puf[i]);
        System.out.println(QUOTE_CONVERTER.pufToQuotedSpread(pricingCDS, coupon, yieldCurve, puf[i]));
        parellelCS01_B[i] = CS01_CAL.parallelCS01FromParSpreads(
            pricingCDS, qSpread, yieldCurve, pillarCDSsIMM, pillarQS, ONE_BP, ShiftType.ABSOLUTE);
        bucketedCS01[i] = CS01_CAL.bucketedCS01FromCreditCurve(
            pricingCDS, coupon, bucketCDSsNonIMM, yieldCurve, creditCurve, ONE_BP);
      }
    }
    final double scale = ONE_BP * NOTIONAL;
    outputPUF(name, maturities, BUCKETS, puf, upfrontAmount, bucketedCS01, parellelCS01_A, parellelCS01_B, scale, accDays);
  }

  private void runGrid(final String name, final LocalDate tradeDate, final LocalDate[] maturities, final double coupon, final double recovery, final double[] spreads,
      final IsdaCompliantYieldCurve yieldCurve) {
    final int nMat = maturities.length;
    ArgChecker.isTrue(nMat == spreads.length, "wrong number of spreads");

    final double scale = ONE_BP * NOTIONAL;
    final CdsAnalyticFactory immFactory = IMM_CDS_FACTORY.withRecoveryRate(recovery);
    final CdsAnalyticFactory nonImmFactory = NON_IMM_CDS_FACTORY.withRecoveryRate(recovery);

    final LocalDate startDate = getPrevIMMDate(tradeDate);
    final LocalDate nextIMM = getNextIMMDate(tradeDate);
    final LocalDate[] pillarDates = getIMMDateSet(nextIMM, PILLARS);

    final double[] pillarSpreads = getSpreadsAtDates(maturities, spreads, pillarDates);

    final CdsAnalytic[] pillarCDSsNonIMM = nonImmFactory.makeCds(tradeDate, tradeDate.plusDays(1), pillarDates); //TODO check this start date
    final LocalDate[] bucketDates = getIMMDateSet(nextIMM, BUCKETS);
    final CdsAnalytic[] bucketCDSsIMM = immFactory.makeCds(tradeDate, startDate, bucketDates);
    final CdsAnalytic[] bucketCDSsNonIMM = nonImmFactory.makeCds(tradeDate, tradeDate.plusDays(1), bucketDates);

    final double[] puf = new double[nMat];
    final double[] upfrontAmount = new double[nMat];
    final double[] parellelCS01 = new double[nMat];
    final double[][] bucketedCS01 = new double[nMat][];
    final int[] accDays = new int[nMat];

    for (int i = 0; i < nMat; i++) {
      if (isIMMDate(maturities[i])) {
        final CdsAnalytic pricingCDS = immFactory.makeCds(tradeDate, startDate, maturities[i]);
        accDays[i] = pricingCDS.getAccuredDays();
        final CdsQuotedSpread quote = new CdsQuotedSpread(coupon, spreads[i]);
        puf[i] = QUOTE_CONVERTER.convert(pricingCDS, quote, yieldCurve).getPointsUpFront();
        upfrontAmount[i] = (puf[i] - pricingCDS.getAccruedPremium(coupon)) * NOTIONAL;
        parellelCS01[i] = CS01_CAL.parallelCS01(pricingCDS, quote, yieldCurve, ONE_BP);
        //a flat (constant) hazard rate does not give a completely flat spread term structure 
        //TODO repeat calculation (already done for PUF) and parallelCS01
        final IsdaCompliantCreditCurve creditCurve = CREDIT_CURVE_BUILDER.calibrateCreditCurve(pricingCDS, quote.getQuotedSpread(), yieldCurve);
        bucketedCS01[i] = CS01_CAL.bucketedCS01FromCreditCurve(pricingCDS, coupon, bucketCDSsIMM, yieldCurve, creditCurve, ONE_BP);
      } else {
        final CdsAnalytic pricingCDS = nonImmFactory.makeCds(tradeDate, tradeDate.plusDays(1), maturities[i]);
        accDays[i] = pricingCDS.getAccuredDays();
        final IsdaCompliantCreditCurve creditCurve = CREDIT_CURVE_BUILDER.calibrateCreditCurve(pillarCDSsNonIMM, pillarSpreads, yieldCurve);
        puf[i] = PRICER.pv(pricingCDS, yieldCurve, creditCurve, spreads[i]);
        upfrontAmount[i] = (puf[i] - pricingCDS.getAccruedPremium(spreads[i])) * NOTIONAL;
        parellelCS01[i] = CS01_CAL.parallelCS01FromParSpreads(
            pricingCDS, spreads[i], yieldCurve, pillarCDSsNonIMM, pillarSpreads, ONE_BP, ShiftType.ABSOLUTE);
        bucketedCS01[i] = CS01_CAL.bucketedCS01FromCreditCurve(
            pricingCDS, spreads[i], bucketCDSsNonIMM, yieldCurve, creditCurve, ONE_BP);
      }
    }

    output(name, maturities, BUCKETS, puf, upfrontAmount, bucketedCS01, parellelCS01, scale, accDays);
  }

  /**
   * @param maturities
   * @param spreads
   * @param dates
   * @param nPillars
   * @param pillarSpreads
   */
  private double[] getSpreadsAtDates(final LocalDate[] maturities, final double[] spreads, final LocalDate[] dates) {
    final int n = dates.length;
    final double[] res = new double[n];
    for (int i = 0; i < n; i++) {
      final int index = Arrays.binarySearch(maturities, dates[i]);
      if (index < 0) {
        final int insPoint = -(index + 1);
        res[i] = spreads[insPoint - 1];
      } else {
        res[i] = spreads[index];
      }
    }
    return res;
  }

  private static LocalDate[] getAllMaturities(final LocalDate start, final LocalDate end) {
    ArgChecker.notNull(start, "start");
    ArgChecker.notNull(end, "end");
    ArgChecker.isTrue(end.isAfter(start), "need end after start");
    final int day = start.getDayOfMonth();
    ArgChecker.isTrue(day == end.getDayOfMonth(), "start and end must be on same day");
    final int n = (end.getYear() - start.getYear()) * 12 + end.getMonthValue() - start.getMonthValue() + 1;
    final LocalDate[] res = new LocalDate[n];
    res[0] = start;
    for (int i = 1; i < n; i++) {
      res[i] = res[i - 1].plusMonths(1);
    }
    ArgChecker.isTrue(res[n - 1].isEqual(end), "error");
    return res;
  }

  private void output(final String name, final LocalDate[] maturities, final Period[] buckets, final double[] puf, final double[] upfrontAmount, final double[][] bCS01, final double[] pCS01,
      final double scale, final int[] accDays) {
    ArgChecker.notNull(name, "name");
    ArgChecker.noNulls(maturities, "maturities");
    ArgChecker.noNulls(buckets, "pillars");
    ArgChecker.notEmpty(puf, "puf");
    ArgChecker.notEmpty(upfrontAmount, "upfrontAmount");
    ArgChecker.noNulls(bCS01, "bCS01");
    ArgChecker.notEmpty(pCS01, "pCS01");
    final int rows = maturities.length;
    ArgChecker.isTrue(rows == bCS01.length, "bCS01 length wrong");
    ArgChecker.isTrue(rows == pCS01.length, "pCS01 length wrong");
    ArgChecker.isTrue(rows == puf.length, "puf length wrong");
    ArgChecker.isTrue(rows == upfrontAmount.length, "upfrontAmount length wrong");
    final int columns = buckets.length;
    ArgChecker.isTrue(columns == bCS01[0].length, "bCS01 width wrong");

    System.out.println(name);
    System.out.print("Maturity");
    for (int j = 0; j < columns; j++) {
      System.out.print("\t" + buckets[j]);
    }
    System.out.print("\t\tSum\tParallel\tPUF\tUpFront Amount\tAccured Days\n");

    for (int i = 0; i < rows; i++) {
      System.out.print(maturities[i]);
      double sum = 0.0;
      for (int j = 0; j < columns; j++) {
        final double temp = bCS01[i][j] * scale;
        sum += temp;
        System.out.print("\t" + temp);
      }
      System.out.print("\t\t" + sum + "\t" + scale * pCS01[i] + "\t" + ONE_HUNDRED * puf[i] + "\t" + upfrontAmount[i] + "\t" + accDays[i] + "\n");
    }
    System.out.print("\n");
  }

  private void outputPUF(final String name, final LocalDate[] maturities, final Period[] buckets, final double[] puf, final double[] upfrontAmount, final double[][] bCS01, final double[] pCS01A,
      final double[] pCS01B, final double scale, final int[] accDays) {
    ArgChecker.notNull(name, "name");
    ArgChecker.noNulls(maturities, "maturities");
    ArgChecker.noNulls(buckets, "pillars");
    ArgChecker.notEmpty(puf, "puf");
    ArgChecker.notEmpty(upfrontAmount, "upfrontAmount");
    ArgChecker.noNulls(bCS01, "bCS01");
    ArgChecker.notEmpty(pCS01A, "pCS01");
    final int rows = maturities.length;
    ArgChecker.isTrue(rows == bCS01.length, "bCS01 length wrong");
    ArgChecker.isTrue(rows == pCS01A.length, "pCS01 length wrong");
    ArgChecker.isTrue(rows == puf.length, "puf length wrong");
    ArgChecker.isTrue(rows == upfrontAmount.length, "upfrontAmount length wrong");
    final int columns = buckets.length;
    ArgChecker.isTrue(columns == bCS01[0].length, "bCS01 width wrong");

    System.out.println(name);
    System.out.print("Maturity");
    for (int j = 0; j < columns; j++) {
      System.out.print("\t" + buckets[j]);
    }
    System.out.print("\t\tSum\tParallel A\tParallel B \tPUF\tUpFront Amount\tAccured Days\n");

    for (int i = 0; i < rows; i++) {
      System.out.print(maturities[i]);
      double sum = 0.0;
      for (int j = 0; j < columns; j++) {
        final double temp = bCS01[i][j] * scale;
        sum += temp;
        System.out.print("\t" + temp);
      }
      System.out.print("\t\t" + sum + "\t" + scale * pCS01A[i] + "\t" + scale * pCS01B[i] + "\t" + ONE_HUNDRED * puf[i] + "\t" + upfrontAmount[i] + "\t" + accDays[i] + "\n");
    }
    System.out.print("\n");
  }
}
