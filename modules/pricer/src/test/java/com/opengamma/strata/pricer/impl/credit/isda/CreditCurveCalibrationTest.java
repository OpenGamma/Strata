/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.credit.isda;

import static org.testng.AssertJUnit.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Arrays;

import com.opengamma.strata.basics.schedule.StubConvention;

/**
 * Test.
 */
public class CreditCurveCalibrationTest extends IsdaBaseTest {

  private static final CdsAnalytic[][] PILLAR_CDS;
  private static final IsdaCompliantYieldCurve[] YIELD_CURVES;
  private static final double[][] SPREADS;
  private static final double[][] SUR_PROB_ISDA;
  private static final double[][] SUR_PROB_MARKIT_FIX;
  private static final double[] OBS_TIMES = new double[] {30 / 365., 90 / 365., 180. / 365., 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };
  private static final int N_OBS = OBS_TIMES.length;

  static {

    final CdsAnalyticFactory factory = new CdsAnalyticFactory();

    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "9M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "11Y", "12Y", "15Y", "20Y", "25Y", "30Y" };
    final String[] yieldCurveInstruments = new String[] {"M", "M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S" };
    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033, 0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
      0.03367, 0.03419, 0.03411, 0.03412 };

    final int nCases = 5;
    PILLAR_CDS = new CdsAnalytic[nCases][];
    SPREADS = new double[nCases][];
    YIELD_CURVES = new IsdaCompliantYieldCurve[nCases];
    SUR_PROB_MARKIT_FIX = new double[nCases][];
    SUR_PROB_ISDA = new double[nCases][];

    //case1
    LocalDate tradeDate = LocalDate.of(2011, Month.JUNE, 19);
    LocalDate spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 3);
    Period[] tenors = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5), Period.ofYears(7), Period.ofYears(10) };
    PILLAR_CDS[0] = factory.makeImmCds(tradeDate, tenors);
    SPREADS[0] = new double[] {0.00886315689995649, 0.00886315689995649, 0.0133044689825873, 0.0171490070952563, 0.0183903639181293, 0.0194721890639724 };
    YIELD_CURVES[0] = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofYears(1));
    SUR_PROB_ISDA[0] = new double[] {0.998772746815168, 0.996322757048216, 0.992659036212158, 0.985174753005029, 0.959541054444166, 0.9345154655283, 0.897874320219939, 0.862605325653124,
      0.830790530716993, 0.80016566917562, 0.76968842828467, 0.740364207356242, 0.71215720464425, 0.685024855452902, 0.658926216751093 };
    SUR_PROB_MARKIT_FIX[0] = new double[] {0.998773616100865, 0.996325358510497, 0.992664220011069, 0.985181033285486, 0.959551128356433, 0.934529141029508, 0.897893062747179, 0.862628725130658,
      0.830817532293803, 0.800195970143901, 0.76972190245315, 0.740400570243092, 0.712196187570045, 0.685066206017066, 0.658969697981512 };

    //case2
    tradeDate = LocalDate.of(2011, Month.MARCH, 21);
    LocalDate effDate = LocalDate.of(2011, Month.MARCH, 20); //not this is a Sunday - for a standard CDS this would roll to the Monday.
    spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 3);
    tenors = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5), Period.ofYears(7), Period.ofYears(10) };
    PILLAR_CDS[1] = factory.makeImmCds(tradeDate, effDate, tenors);
    SPREADS[1] = new double[] {0.027, 0.018, 0.012, 0.009, 0.007, 0.006 };
    YIELD_CURVES[1] = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofYears(1));
    SUR_PROB_ISDA[1] = new double[] {0.996266535762958, 0.988841371514657, 0.977807258018988, 0.96475844963628, 0.953395823781617, 0.940590393592274, 0.933146036536171, 0.927501935763199,
      0.924978347338877, 0.923516383873675, 0.919646843289677, 0.914974439245307, 0.91032577405212, 0.905700727101315, 0.901099178396858 };
    SUR_PROB_MARKIT_FIX[1] = new double[] {0.996272873932676, 0.988860244428938, 0.977844583012059, 0.964805380714707, 0.953429040991605, 0.940617833909825, 0.933169293548597, 0.927521552929219,
      0.924995002753253, 0.923530307620416, 0.91965942070523, 0.914986149602945, 0.910336625838319, 0.905710728738695, 0.901108338244616, };

    //case3
    tradeDate = LocalDate.of(2011, Month.MAY, 30);
    spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 3);
    LocalDate[] maDates = new LocalDate[] {LocalDate.of(2011, Month.JUNE, 20), LocalDate.of(2012, Month.MAY, 30), LocalDate.of(2014, Month.JUNE, 20), LocalDate.of(2016, Month.JUNE, 20),
      LocalDate.of(2018, Month.JUNE, 20) };
    PILLAR_CDS[2] = factory.withRecoveryRate(0.25).withAccrualDCC(D30360).makeCds(tradeDate, tradeDate.plusDays(1), maDates);
    SPREADS[2] = new double[] {0.05, 0.05, 0.05, 0.05, 0.05 };
    YIELD_CURVES[2] = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofYears(1));
    SUR_PROB_ISDA[2] = new double[] {0.994488823839325, 0.983690222697363, 0.967711777571664, 0.935677618299157, 0.875533583554252, 0.819255475760025, 0.7666904278069, 0.717503794565525,
      0.671435362513808, 0.628322474825315, 0.587977867136078, 0.550223788092265, 0.514893899760578, 0.481832544772127, 0.450894060523028 };
    SUR_PROB_MARKIT_FIX[2] = new double[] {0.994492541382389, 0.983716053360113, 0.967769880036333, 0.935798775210736, 0.875741081454824, 0.819537657320969, 0.766996460740263, 0.717827034617184,
      0.671770999435863, 0.628667500941574, 0.588329694303598, 0.550580121735183, 0.515252711846802, 0.482192049049632, 0.451252689837008 };

    //case4
    tradeDate = LocalDate.of(2011, Month.MAY, 30);
    effDate = LocalDate.of(2011, Month.JULY, 31);
    spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 3);
    maDates = new LocalDate[] {LocalDate.of(2011, Month.NOVEMBER, 30), LocalDate.of(2012, Month.MAY, 30), LocalDate.of(2014, Month.MAY, 30), LocalDate.of(2016, Month.MAY, 30),
      LocalDate.of(2018, Month.MAY, 30), LocalDate.of(2021, Month.MAY, 30) };
    PILLAR_CDS[3] =
        factory.withRecoveryRate(0.25).withAccrualDCC(ACT365F).with(Period.ofMonths(6)).with(StubConvention.LONG_INITIAL)
            .makeCds(tradeDate, effDate, maDates);
    SPREADS[3] = new double[] {0.07, 0.06, 0.05, 0.055, 0.06, 0.065 };
    YIELD_CURVES[3] = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofYears(1));
    SUR_PROB_ISDA[3] = new double[] {0.99238650617037, 0.977332973057625, 0.955179740225657, 0.92187587198518, 0.868032006457467, 0.817353939709416, 0.751100020583073, 0.690170357851426,
      0.622562049244094, 0.561519352597547, 0.500515112466997, 0.44610942528539, 0.397617603088025, 0.354396812361283, 0.315874095202052 };
    SUR_PROB_MARKIT_FIX[3] = new double[] {0.992434753056402, 0.977475525071675, 0.955458402114146, 0.923257693140384, 0.86924227242564, 0.818402338488625, 0.752150342806546, 0.691215773405857,
      0.623608833084194, 0.562557270491733, 0.50153493334764, 0.447102836461508, 0.3985783104631, 0.355320200669978, 0.316756937570093 };

    //case5 This is designed to trip the low rates/low spreads branch
    tradeDate = LocalDate.of(2014, Month.JANUARY, 14);
    spotDate = DEFAULT_CALENDAR.shift(tradeDate.minusDays(1), 3);
    PILLAR_CDS[4] = factory.makeImmCds(tradeDate, tenors);
    SPREADS[4] = new double[6];
    Arrays.fill(SPREADS[4], ONE_BP);
    final int n = rates.length;
    final double[] lowRates = new double[n];
    for (int i = 0; i < n; i++) {
      lowRates[i] = rates[i] / 1000;
    }
    YIELD_CURVES[4] = makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, lowRates, ACT360, D30360, Period.ofYears(1));
    SUR_PROB_ISDA[4] = new double[] {0.999986111241871, 0.999958334304303, 0.999916670344636, 0.999831033196934, 0.999662094963152, 0.999493185285761, 0.999324304350342, 0.999155451994703,
      0.998986628218491, 0.998817832978659, 0.998649066279251, 0.998480328100177, 0.998311618432194, 0.998142937270482, 0.997974284610226 };
    SUR_PROB_MARKIT_FIX[4] = new double[] {0.999986111408132, 0.999958334803071, 0.999916671342131, 0.999831035053453, 0.999662097437689, 0.999493188187127, 0.999324307673036, 0.9991554557374,
      0.998986632383511, 0.998817837566403, 0.998649071291, 0.998480333536109, 0.998311624292164, 0.998142943554348, 0.997974291317845 };
  }

  protected void testCalibrationAgainstISDA(final IsdaCompliantCreditCurveBuilder builder, final double tol) {

    final int n = YIELD_CURVES.length;
    final String text = builder.getAccOnDefaultFormula().toString();
    final AnalyticCdsPricer pricer = new AnalyticCdsPricer(builder.getAccOnDefaultFormula());
    for (int i = 0; i < n; i++) {
      final IsdaCompliantCreditCurve creditCurve = builder.calibrateCreditCurve(PILLAR_CDS[i], SPREADS[i], YIELD_CURVES[i]);
      final double[] expected = builder.getAccOnDefaultFormula() == MARKIT_FIX ? SUR_PROB_MARKIT_FIX[i] : SUR_PROB_ISDA[i];

      for (int k = 0; k < N_OBS; k++) {
        assertEquals("failed test case " + (i + 1) + " (" + text + "), node " + k, expected[k], creditCurve.getSurvivalProbability(OBS_TIMES[k]), tol);
      }

      final CdsAnalytic[] cds = PILLAR_CDS[i];
      final int m = cds.length;
      for (int j = 0; j < m; j++) {
        final double p = pricer.pv(cds[j], YIELD_CURVES[i], creditCurve, SPREADS[i][j]);
        assertEquals("failed test case " + (i + 1) + " (" + text + "), cds: " + j + 1, 0.0, p, 5e-16);
      }

    }

  }

}
