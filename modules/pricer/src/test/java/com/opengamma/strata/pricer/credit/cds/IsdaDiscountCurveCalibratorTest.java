package com.opengamma.strata.pricer.credit.cds;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.product.deposit.type.TermDepositConvention;
import com.opengamma.strata.product.deposit.type.TermDepositConventions;
import com.opengamma.strata.product.deposit.type.TermDepositTemplate;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;

/**
 * Test {@link IsdaDiscountCurveCalibrator}.
 */
@Test
public class IsdaDiscountCurveCalibratorTest {

  private static final DayCount ACT365 = DayCounts.ACT_365F;
  private static final DayCount ACT360 = DayCounts.ACT_360;
  private static final DayCount D30360 = DayCounts.THIRTY_U_360;
  private static final DayCount ACT_ACT = DayCounts.ACT_ACT_ISDA;

//  private static ProbabilityDistribution<Double> NORMAL = new NormalDistribution(0, 1, new MersenneTwister(MersenneTwister.DEFAULT_SEED));

  private static final BusinessDayConvention FOLLOWING = BusinessDayConventions.FOLLOWING;
  private static final BusinessDayConvention MOD_FOLLOWING = BusinessDayConventions.MODIFIED_FOLLOWING;

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendarId CALENDAR = HolidayCalendarIds.SAT_SUN;

  public void test() {
    final boolean print = false;
    if (print) {
      System.out.println("ISDACompliantYieldCurveBuildTest: print should be false for commit");
    }

    // date from ISDA excel
    final double[] sampleTimes = new double[] {0.0767123287671233, 0.167123287671233, 0.249315068493151, 0.498630136986301,
        0.747945205479452, 0.997260273972603, 1.4958904109589, 1.99452054794521,
        2.5013698630137, 3.0027397260274, 3.5041095890411, 4.0027397260274, 4.5041095890411, 5.0027397260274, 5.5041095890411,
        6.0027397260274, 6.5013698630137, 7, 7.50684931506849, 8.00547945205479,
        8.50684931506849, 9.00547945205479, 9.50684931506849, 10.0054794520548, 10.5068493150685, 11.0082191780822,
        11.5068493150685, 12.0054794520548, 12.5041095890411, 13.0027397260274,
        13.5095890410959, 14.0082191780822, 14.5095890410959, 15.0109589041096, 15.5123287671233, 16.0109589041096,
        16.5123287671233, 17.0109589041096, 17.5095890410959, 18.0082191780822,
        18.5068493150685, 19.013698630137, 19.5150684931507, 20.013698630137, 20.5150684931507, 21.013698630137, 21.5150684931507,
        22.013698630137, 22.5150684931507, 23.013698630137, 23.5123287671233,
        24.0109589041096, 24.5178082191781, 25.0164383561644, 25.5178082191781, 26.0164383561644, 26.5178082191781,
        27.0191780821918, 27.5205479452055, 28.0191780821918, 28.5178082191781,
        29.0164383561644, 29.5150684931507, 30.013698630137};
    final double[] zeroRates = new double[] {0.00344732957665484, 0.00645427070262317, 0.010390833731528, 0.0137267241507424,
        0.016406009142171, 0.0206548075787697, 0.0220059788254565,
        0.0226815644487997, 0.0241475224808774, 0.0251107341245228, 0.0263549710022889, 0.0272832610741453, 0.0294785565070328,
        0.0312254350680597, 0.0340228731758456, 0.0363415444446394,
        0.0364040719835966, 0.0364576914896066, 0.0398713425199977, 0.0428078389323812, 0.0443206903065534, 0.0456582004054368,
        0.0473373527805339, 0.0488404232471453, 0.0496433764260127,
        0.0503731885238783, 0.0510359350109291, 0.0516436290741354, 0.0526405492486405, 0.0535610094687589, 0.05442700569164,
        0.0552178073994544, 0.0559581527041068, 0.0566490425640605,
        0.0572429526830672, 0.0577967261153023, 0.0583198210222109, 0.0588094750567186, 0.0592712408001043, 0.0597074348516306,
        0.0601201241459759, 0.0605174325075768, 0.0608901411604128,
        0.0612422922398251, 0.0618707980423834, 0.0624661234885966, 0.0630368977571603, 0.0635787665840882, 0.064099413535239,
        0.0645947156962813, 0.0650690099353217, 0.0655236050526131,
        0.0659667431709796, 0.0663851731522577, 0.0668735344788778, 0.0673405584796377, 0.0677924400667054, 0.0682275513575991,
        0.0686468089170376, 0.0690488939824011, 0.0694369182384849,
        0.06981160656508, 0.0701736348572483, 0.0705236340943412};

    final LocalDate spotDate = LocalDate.of(2013, 5, 31);

    final int nMoneyMarket = 6;
    final int nSwaps = 14;
    final int nInstruments = nMoneyMarket + nSwaps;

    final CurveNode[] types = new CurveNode[nInstruments];
    final Period[] tenors = new Period[nInstruments];
    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12};
    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30};
//    // check
//    ArgumentChecker.isTrue(mmMonths.length == nMoneyMarket, "mmMonths");
//    ArgumentChecker.isTrue(swapYears.length == nSwaps, "swapYears");

    for (int i = 0; i < nMoneyMarket; i++) {
      tenors[i] = Period.ofMonths(mmMonths[i]);
      TermDepositConvention convention = TermDepositConventions.USD_SHORT_DEPOSIT_T2;
      types[i] = TermDepositCurveNode.of(TermDepositTemplate.of(tenors[i], convention), QuoteId.of(StandardId.of("OG", "test")));

    }
    for (int i = nMoneyMarket; i < nInstruments; i++) {
      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
      FixedIborSwapConvention convention = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
      types[i] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Tenor.of(tenors[i]), convention),
          QuoteId.of(StandardId.of("OG", "test")));
    }

    final double[] rates = new double[] {0.00340055550701297, 0.00636929056400781, 0.0102617798438113, 0.0135851258907251,
        0.0162809551414651, 0.020583125112332, 0.0227369218210212,
        0.0251978805237614, 0.0273223815467694, 0.0310882447627048, 0.0358397743454067, 0.036047665095421, 0.0415916567616181,
        0.044066373237682, 0.046708518178509, 0.0491196954851753,
        0.0529297239911766, 0.0562025436376854, 0.0589772202773522, 0.0607471217692999};

    final DayCount moneyMarketDCC = ACT360;
    final DayCount swapDCC = D30360;
    final DayCount curveDCC = ACT365;
    final Period swapInterval = Period.ofMonths(6);

    final IsdaDiscountCurveCalibrator bob =
        new IsdaDiscountCurveCalibrator(spotDate, spotDate, types, tenors, moneyMarketDCC, swapDCC, swapInterval, curveDCC,
            MOD_FOLLOWING, CALENDAR, REF_DATA);
    final IsdaCompliantZeroRateDiscountFactors hc = bob.build(rates);

    final int nCurvePoints = hc.getParameterCount();
    assertEquals(nInstruments, nCurvePoints);
    final int nSamplePoints = sampleTimes.length;
    for (int i = 0; i < nSamplePoints; i++) {
      final double time = sampleTimes[i];
      final double zr = hc.getCurve().yValue(time);
      assertEquals(zeroRates[i], zr, 1e-10);
    }

//    if (print) {
//      for (int i = 0; i < nCurvePoints; i++) {
//        System.out.println(hc.getTimeAtIndex(i) + "\t" + hc.getZeroRateAtIndex(i));
//      }
//    }

  }

  @Test
  public void offsetTest() {
    final boolean print = false;
    if (print) {
      System.out.println("ISDACompliantYieldCurveBuildTest: print should be false for commit");
    }

    // date from ISDA excel
    final double[] zeroRates = new double[] {0.00344732957670444, 0.00344732957670444, 0.00344732957665564, 0.00573603521085939,
        0.0084176643849198, 0.010781796487941, 0.0120279905518332,
        0.0128615375747012, 0.0134582814660727, 0.0143042601975818, 0.0151295167161045, 0.0157903795058379, 0.0163736824559949,
        0.0177879283390989, 0.0189852653444261, 0.0200120415227836,
        0.0206710846569517, 0.0209227827032035, 0.0211449387252473, 0.0213424654765546, 0.0215192436195312, 0.0216783792332686,
        0.0218223878875253, 0.0219533286058241, 0.0220729029423261,
        0.0221825293306998, 0.0222833996177656, 0.0223765225709555, 0.0224627577317496, 0.0225428420251548, 0.0226174108713557,
        0.0228326433450198, 0.0230608528197144, 0.0232748170088476,
        0.0234758293705513, 0.0236650313702278, 0.0238434341690096, 0.0240119367014882, 0.0241713408250857, 0.0243223640799347,
        0.0244656504877115, 0.0246017797322627, 0.0247312749980345,
        0.0248546096897925, 0.0249722132155767, 0.0250950768771892, 0.0252884986617019, 0.0254735181097892, 0.0256506710847436,
        0.0258204488317648, 0.025983302527113, 0.0261396472817999,
        0.0262898656746259, 0.0264343108778737, 0.0265733094294154, 0.0267071636970361, 0.0268361540741183, 0.0269605409402411,
        0.0270805664155462, 0.0271964559337422, 0.0274246634134451,
        0.0277507838016358, 0.0280662187249447, 0.028371484888637, 0.0286670662121106, 0.0289534163886924, 0.0292309612092909,
        0.0295001006749348, 0.0297612109202432, 0.0300146459672769,
        0.0302607393269689, 0.0304998054633688, 0.0307321411342168, 0.0309580266198665, 0.0311777268512541, 0.0315770230032083,
        0.0319815950322907, 0.0323755260295719, 0.0327592303654492,
        0.0331331011714474, 0.0334975116837716, 0.0338528164861427, 0.0341993526606172, 0.0345374408542415, 0.034867386268636,
        0.0351894795789277, 0.0355039977878485, 0.0358112050202719,
        0.0361113532629797, 0.036314617444893, 0.0363240760018509, 0.0363333244620306, 0.0363423697486562, 0.036351218484073,
        0.0363598770059169, 0.0363683513822515, 0.0363766474257508,
        0.0363847707069948, 0.0363927265669436, 0.0364005201286479, 0.0364081563082498, 0.0364156398253248, 0.0364229752126081,
        0.0364301668251504, 0.0368049737599067, 0.0372875925345602,
        0.037761022071311, 0.0382255223448261, 0.0386813436147256, 0.0391287268751859, 0.0395679042798051, 0.0399990995433007,
        0.0404225283215028, 0.0408383985709972, 0.0412469108896797,
        0.0416482588393919, 0.0420426292517308, 0.0424302025180453, 0.0427984803283626, 0.0430160187075607, 0.0432299022080761,
        0.0434402221714286, 0.0436470669205816, 0.0438505218836128,
        0.0440506697113542, 0.0442475903893416, 0.0444413613443925, 0.0446320575461104, 0.0448197516035947, 0.0450045138576194,
        0.0451864124685252, 0.045365513500057, 0.0455418809993614,
        0.0457483713358813, 0.0459870175650164, 0.0462221029730662, 0.0464537066659494, 0.0466819054236241, 0.0469067737849521,
        0.0471283841288746, 0.0473468067520848, 0.0475621099433725,
        0.0477743600548064, 0.0479836215699092, 0.0481899571689731, 0.0483934277916546, 0.0485940926969798, 0.0487920095208844,
        0.0489164313924592, 0.0490295826139708};

    final LocalDate spotDate = LocalDate.of(2013, 5, 31);
    final LocalDate tradeDate = LocalDate.of(2013, 5, 29);

    final int nMoneyMarket = 6;
    final int nSwaps = 14;
    final int nInstruments = nMoneyMarket + nSwaps;

    final CurveNode[] types = new CurveNode[nInstruments];
    final Period[] tenors = new Period[nInstruments];
    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12};
    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30};
//    // check
//    ArgumentChecker.isTrue(mmMonths.length == nMoneyMarket, "mmMonths");
//    ArgumentChecker.isTrue(swapYears.length == nSwaps, "swapYears");

    for (int i = 0; i < nMoneyMarket; i++) {
      tenors[i] = Period.ofMonths(mmMonths[i]);
      TermDepositConvention convention = TermDepositConventions.USD_SHORT_DEPOSIT_T2;
      types[i] = TermDepositCurveNode.of(TermDepositTemplate.of(tenors[i], convention), QuoteId.of(StandardId.of("OG", "test")));
    }
    for (int i = nMoneyMarket; i < nInstruments; i++) {
      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
      FixedIborSwapConvention convention = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
      types[i] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Tenor.of(tenors[i]), convention),
          QuoteId.of(StandardId.of("OG", "test")));
    }

    final double[] rates = new double[] {0.00340055550701297, 0.00636929056400781, 0.0102617798438113, 0.0135851258907251,
        0.0162809551414651, 0.020583125112332, 0.0227369218210212,
        0.0251978805237614, 0.0273223815467694, 0.0310882447627048, 0.0358397743454067, 0.036047665095421, 0.0415916567616181,
        0.044066373237682, 0.046708518178509, 0.0491196954851753,
        0.0529297239911766, 0.0562025436376854, 0.0589772202773522, 0.0607471217692999};

    final DayCount moneyMarketDCC = ACT360;
    final DayCount swapDCC = D30360;
    final DayCount curveDCC = ACT365;
    final Period swapInterval = Period.ofMonths(6);

    final IsdaDiscountCurveCalibrator bob = new IsdaDiscountCurveCalibrator(tradeDate, spotDate, types, tenors, moneyMarketDCC,
        swapDCC, swapInterval, curveDCC, MOD_FOLLOWING, CALENDAR, REF_DATA);
    final IsdaCompliantZeroRateDiscountFactors yc = bob.build(rates);

    final int nCurvePoints = yc.getParameterCount();
    assertEquals(nInstruments, nCurvePoints);
    final int nSamplePoints = zeroRates.length;
    final double[] times = new double[nSamplePoints];
    times[0] = 0.0;
    LocalDate tDate = tradeDate.plusDays(1);
    times[1] = curveDCC.relativeYearFraction(tradeDate, tDate);
    for (int i = 2; i < nSamplePoints; i++) {
      tDate = tradeDate.plusDays(25 * (i - 1) + 1);
      times[i] = curveDCC.relativeYearFraction(tradeDate, tDate);
    }

//    if (print) {
//      for (int i = 0; i < nSamplePoints; i++) {
//        final double zr = yc.getZeroRate(times[i]);
//        System.out.println(times[i] + "\t" + zr);
//      }
//    }

    for (int i = 0; i < nSamplePoints; i++) {
      final double zr = yc.getCurve().yValue(times[i]);
      assertEquals(zeroRates[i], zr, 1e-10); //see Javadocs
//      System.out.println(zeroRates[i] + "\t" + zr + "\t" + (zeroRates[i] - zr));
    }

  }

//
//  @Test(enabled = false)
//  public void timingTest() {
//    //TODO extract timing to another test
//    final int warmup = 200;
//    final int hotSpot = 1000;
//    final double vol = 0.05;
//
//    final double[] rates = new double[] {0.00340055550701297, 0.00636929056400781, 0.0102617798438113, 0.0135851258907251,
//        0.0162809551414651, 0.020583125112332, 0.0227369218210212,
//        0.0251978805237614, 0.0273223815467694, 0.0310882447627048, 0.0358397743454067, 0.036047665095421, 0.0415916567616181,
//        0.044066373237682, 0.046708518178509, 0.0491196954851753,
//        0.0529297239911766, 0.0562025436376854, 0.0589772202773522, 0.0607471217692999};
//
//    final LocalDate spotDate = LocalDate.of(2013, 5, 31);
//
//    final int nMoneyMarket = 6;
//    final int nSwaps = 14;
//    final int nInstruments = nMoneyMarket + nSwaps;
//
//    final ISDAInstrumentTypes[] types = new ISDAInstrumentTypes[nInstruments];
//    final Period[] tenors = new Period[nInstruments];
//    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12};
//    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30};
//
//    final DayCount moneyMarketDCC = ACT360;
//    final DayCount swapDCC = D30360;
//    // final DayCount curveDCC = ACT365;
//    final Period swapInterval = Period.ofMonths(6);
//
//    for (int i = 0; i < nMoneyMarket; i++) {
//      types[i] = ISDAInstrumentTypes.MoneyMarket;
//      tenors[i] = Period.ofMonths(mmMonths[i]);
//    }
//    for (int i = nMoneyMarket; i < nInstruments; i++) {
//      types[i] = ISDAInstrumentTypes.Swap;
//      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
//    }
//
//    final ISDACompliantYieldCurveBuild bob =
//        new ISDACompliantYieldCurveBuild(spotDate, types, tenors, moneyMarketDCC, swapDCC, swapInterval, MOD_FOLLOWING);
//
//    for (int i = 0; i < warmup; i++) {
//      @SuppressWarnings("unused")
//      final ISDACompliantCurve hc1 = bob.build(bumpRates(rates, vol));
//    }
//
//    if (hotSpot > 0) {
//      final long t0 = System.nanoTime();
//      for (int i = 0; i < hotSpot; i++) {
//        @SuppressWarnings("unused")
//        final ISDACompliantCurve hc1 = bob.build(bumpRates(rates, vol));
//      }
//      System.out.println("time to build yield curve: " + (System.nanoTime() - t0) / 1e6 / hotSpot + "ms");
//    }
//  }
//
//  private double[] bumpRates(final double[] rates, final double vol) {
//    final int n = rates.length;
//    final double[] res = new double[n];
//    for (int i = 0; i < n; i++) {
//      res[i] = rates[i] * Math.exp(vol * NORMAL.nextRandom());
//    }
//    return res;
//  }

  @Test
  public void anotherTest() {
    final boolean print = false;
    if (print) {
      System.out.println("ISDACompliantYieldCurveBuildTest: print should be false for commit");
    }

    // date from ISDA excel
    final double[] sampleTimes = new double[] {0.0876712328767123, 0.167123287671233, 0.252054794520548, 0.495890410958904,
        0.747945205479452, 1, 1.4958904109589, 2.00547945205479, 2.5041095890411,
        3.0027397260274, 3.5013698630137, 4.0027397260274, 4.4986301369863, 5.0027397260274, 5.4986301369863, 6.0027397260274,
        6.5013698630137, 7.01095890410959, 7.5013698630137, 8.00821917808219,
        8.50684931506849, 9.00547945205479, 9.5041095890411, 10.0054794520548, 10.5041095890411, 11.0082191780822,
        11.5041095890411, 12.0082191780822, 12.5041095890411, 13.013698630137,
        13.5041095890411, 14.0109589041096, 14.5095890410959, 15.0109589041096, 15.5068493150685, 16.0109589041096,
        16.5068493150685, 17.0109589041096, 17.5068493150685, 18.0109589041096,
        18.5095890410959, 19.0164383561644, 19.5150684931507, 20.013698630137, 20.5123287671233, 21.013698630137,
        21.5095890410959, 22.013698630137, 22.5123287671233, 23.0164383561644,
        23.5123287671233, 24.0219178082192, 24.5123287671233, 25.0191780821918, 25.5178082191781, 26.0164383561644,
        26.5150684931507, 27.0191780821918, 27.5150684931507, 28.0191780821918,
        28.5150684931507, 29.0191780821918, 29.5150684931507, 30.0246575342466};
    final double[] zeroRates = new double[] {0.00451091345592003, 0.0096120532508373, 0.0124886704800469, 0.0179287581253996,
        0.019476202462918, 0.0209073273478429, 0.0180925538740485,
        0.0166502405937304, 0.0189037116841984, 0.0204087671935255, 0.0220943506849952, 0.0233657744039486, 0.0246460468575126,
        0.0256873833598965, 0.026666390851819, 0.0274958283375808,
        0.028228774560615, 0.0288701107678566, 0.0294694929454103, 0.0300118234002438, 0.0305061047348909, 0.0309456497124306,
        0.0313781991283657, 0.0317696564018493, 0.0321646717802045,
        0.0325276505922571, 0.0329486243843157, 0.0333409374474117, 0.0336496168922921, 0.0339423150176603, 0.0342031385938489,
        0.034453517898306, 0.0346827676795623, 0.0348979210010215,
        0.0349547278282821, 0.0350088694020237, 0.0350589017641339, 0.0351067734588913, 0.035151174765217, 0.0351938059061586,
        0.0352336892661124, 0.0352720864818463, 0.0353079147726051,
        0.0353419577796079, 0.0353037376607363, 0.0352671363539399, 0.0352326134807957, 0.0351991126433607, 0.035167451913752,
        0.0351368377606211, 0.0351080035690964, 0.0350796130984763,
        0.035053405709698, 0.0350273994983831, 0.0350148748938213, 0.0350028303815154, 0.0349912388762854, 0.0349799549048451,
        0.0349692583262832, 0.0349587725430485, 0.0349488194559029,
        0.0349390500683469, 0.0349297655642079, 0.0349205440948243};

    final LocalDate spotDate = LocalDate.of(2009, 11, 12);

    final int nMoneyMarket = 6;
    final int nSwaps = 15;
    final int nInstruments = nMoneyMarket + nSwaps;

    final CurveNode[] types = new CurveNode[nInstruments];
    final Period[] tenors = new Period[nInstruments];
    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12};
    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25, 30};
    // check
//    ArgumentChecker.isTrue(mmMonths.length == nMoneyMarket, "mmMonths");
//    ArgumentChecker.isTrue(swapYears.length == nSwaps, "swapYears");

    for (int i = 0; i < nMoneyMarket; i++) {
      tenors[i] = Period.ofMonths(mmMonths[i]);
      TermDepositConvention convention = TermDepositConventions.USD_SHORT_DEPOSIT_T2;
      types[i] = TermDepositCurveNode.of(TermDepositTemplate.of(tenors[i], convention), QuoteId.of(StandardId.of("OG", "test")));
    }
    for (int i = nMoneyMarket; i < nInstruments; i++) {
      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
      FixedIborSwapConvention convention = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
      types[i] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Tenor.of(tenors[i]), convention),
          QuoteId.of(StandardId.of("OG", "test")));
    }

    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033,
        0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
        0.03367, 0.03419, 0.03411, 0.03412};

    final DayCount moneyMarketDCC = ACT360;
    final DayCount swapDCC = ACT360;
    final Period swapInterval = Period.ofMonths(6);
    final IsdaDiscountCurveCalibrator bob =
        new IsdaDiscountCurveCalibrator(spotDate, spotDate, types, tenors, moneyMarketDCC, swapDCC, swapInterval, ACT365,
            FOLLOWING, CALENDAR, REF_DATA);
    final IsdaCompliantZeroRateDiscountFactors hc = bob.build(rates);

    final int nCurvePoints = hc.getParameterCount();
    assertEquals(nInstruments, nCurvePoints);
    final int nSamplePoints = sampleTimes.length;
    for (int i = 0; i < nSamplePoints; i++) {
      final double time = sampleTimes[i];
      final double zr = hc.getCurve().yValue(time);
      assertEquals(zeroRates[i], zr, 1e-10); //see Javadocs
    }

//    if (print) {
//      for (int i = 0; i < nCurvePoints; i++) {
//        System.out.println(hc.getTimeAtIndex(i) + "\t" + hc.getZeroRateAtIndex(i));
//      }
//    }

  }

  /**
   * 
   */
  @Test
  public void differentSpotDatesTest() {
    final double[][] sampleTimes = new double[][] {
        {0.0849315068493151, 0.164383561643836, 0.252054794520548, 0.504109589041096, 0.747945205479452, 1, 1.5041095890411, 2,
            2.5041095890411, 3, 3.5013698630137, 4, 4.50684931506849,
            4.9972602739726, 5.50684931506849, 6.0027397260274, 6.50684931506849, 7.0027397260274, 7.50684931506849,
            8.00547945205479, 8.50684931506849, 9.00547945205479, 9.5041095890411,
            10.0027397260274, 10.5095890410959, 11, 11.5095890410959, 12.0082191780822, 12.5123287671233, 13.0082191780822,
            13.5123287671233, 14.0082191780822, 14.5095890410959, 15.0082191780822,
            15.5068493150685, 16.0054794520548, 16.5150684931507, 17.0109589041096, 17.5150684931507, 18.0109589041096,
            18.5150684931507, 19.0109589041096, 19.5150684931507, 20.013698630137,
            20.5123287671233, 21.0109589041096, 21.5178082191781, 22.0082191780822, 22.5178082191781, 23.013698630137,
            23.5178082191781, 24.0164383561644, 24.5205479452055, 25.0164383561644,
            25.5178082191781, 26.0164383561644, 26.5150684931507, 27.013698630137, 27.5205479452055, 28.0191780821918,
            28.5232876712329, 29.0191780821918, 29.5232876712329, 30.0191780821918,},
        {0.0767123287671233, 0.167123287671233, 0.249315068493151, 0.498630136986301, 0.747945205479452, 0.997260273972603,
            1.4958904109589, 1.99452054794521, 2.5013698630137, 3.0027397260274,
            3.5041095890411, 4.0027397260274, 4.5041095890411, 5.0027397260274, 5.5041095890411, 6.0027397260274, 6.5013698630137,
            7, 7.50684931506849, 8.00547945205479, 8.50684931506849,
            9.00547945205479, 9.50684931506849, 10.0054794520548, 10.5068493150685, 11.0082191780822, 11.5068493150685,
            12.0054794520548, 12.5041095890411, 13.0027397260274, 13.5095890410959,
            14.0082191780822, 14.5095890410959, 15.0109589041096, 15.5123287671233, 16.0109589041096, 16.5123287671233,
            17.0109589041096, 17.5095890410959, 18.0082191780822, 18.5068493150685,
            19.013698630137, 19.5150684931507, 20.013698630137, 20.5150684931507, 21.013698630137, 21.5150684931507,
            22.013698630137, 22.5150684931507, 23.013698630137, 23.5123287671233,
            24.0109589041096, 24.5178082191781, 25.0164383561644, 25.5178082191781, 26.0164383561644, 26.5178082191781,
            27.0191780821918, 27.5205479452055, 28.0191780821918, 28.5178082191781,
            29.0164383561644, 29.5150684931507, 30.013698630137,},
        {0.0767123287671233, 0.161643835616438, 0.246575342465753, 0.495890410958904, 0.747945205479452, 0.997260273972603,
            1.4958904109589, 2.0027397260274, 2.5013698630137, 3.0027397260274,
            3.4986301369863, 4.0027397260274, 4.4986301369863, 5.0027397260274, 5.5013698630137, 6.0027397260274, 6.5013698630137,
            7.00821917808219, 7.4986301369863, 8.00547945205479, 8.5041095890411,
            9.00547945205479, 9.5041095890411, 10.0082191780822, 10.5041095890411, 11.0082191780822, 11.5041095890411,
            12.0054794520548, 12.5041095890411, 13.0109589041096, 13.5095890410959,
            14.0109589041096, 14.5068493150685, 15.0109589041096, 15.5068493150685, 16.0109589041096, 16.5068493150685,
            17.0109589041096, 17.5095890410959, 18.0164383561644, 18.5068493150685,
            19.013698630137, 19.5123287671233, 20.013698630137, 20.5095890410959, 21.013698630137, 21.5123287671233,
            22.0164383561644, 22.5123287671233, 23.013698630137, 23.5123287671233,
            24.0191780821918, 24.5095890410959, 25.0164383561644, 25.5150684931507, 26.0191780821918, 26.5150684931507,
            27.0191780821918, 27.5150684931507, 28.0191780821918, 28.5150684931507,
            29.0164383561644, 29.5150684931507, 30.0219178082192,},
        {0.0821917808219178, 0.16986301369863, 0.249315068493151, 0.495890410958904, 0.747945205479452, 1, 1.4958904109589, 2,
            2.4986301369863, 3, 3.4986301369863, 4.00547945205479, 4.5041095890411,
            5.0027397260274, 5.5013698630137, 6.0027397260274, 6.5013698630137, 7.00547945205479, 7.5013698630137,
            8.00547945205479, 8.5013698630137, 9.0027397260274, 9.5013698630137, 10.0082191780822,
            10.5068493150685, 11.0082191780822, 11.5041095890411, 12.0082191780822, 12.5041095890411, 13.0082191780822,
            13.5041095890411, 14.0082191780822, 14.5068493150685, 15.013698630137,
            15.5123287671233, 16.0109589041096, 16.5095890410959, 17.0109589041096, 17.5068493150685, 18.0109589041096,
            18.5095890410959, 19.013698630137, 19.5095890410959, 20.0109589041096,
            20.5095890410959, 21.0164383561644, 21.5150684931507, 22.013698630137, 22.5123287671233, 23.0164383561644,
            23.5123287671233, 24.0164383561644, 24.5123287671233, 25.0164383561644,
            25.5123287671233, 26.013698630137, 26.5205479452055, 27.0191780821918, 27.5178082191781, 28.0191780821918,
            28.5150684931507, 29.0191780821918, 29.5150684931507, 30.0191780821918,}};

    final double[][] zeroRates = new double[][] {
        {0.00451094132691394, 0.00961217974910455, 0.0124886704800469, 0.0179274411726332, 0.019476202462918, 0.0209073273478429,
            0.0179000772579215, 0.0164209678386938, 0.0186592679052116,
            0.0201271386010079, 0.021797188562087, 0.0230428815658429, 0.0243324704904329, 0.025331229290671, 0.0263158588596771,
            0.0271135246391119, 0.0278487151164177, 0.0284686444021044,
            0.0290661710399708, 0.0295831721479864, 0.0300718446799346, 0.0305038794836742, 0.0309359080120768,
            0.0313248638523561, 0.0317220181775318, 0.0320714535637223, 0.0324894654570969,
            0.0328641460176763, 0.0331733043925884, 0.0334540432499793, 0.0337183143788277, 0.0339597188942556,
            0.0341870155236588, 0.0343980080434021, 0.0344540834934504, 0.0345066650263785,
            0.0345571216268994, 0.0346033196626196, 0.0346476020776184, 0.0346887439459186, 0.0347283088188042,
            0.0347651813828698, 0.0348007443370411, 0.0348341583058066, 0.0347972170482594,
            0.0347620291637398, 0.034727932613995, 0.034696436809352, 0.0346651627297138, 0.034636058999943, 0.0346077309180707,
            0.0345808806544743, 0.034554845409868, 0.0345302584100671,
            0.0345178592427287, 0.0345060018158178, 0.0344945903594994, 0.0344836001780392, 0.0344728369911756,
            0.0344626283203863, 0.034452670297307, 0.0344432121915854, 0.0344339229923854,
            0.0344250896444241,},
        {0.00451102494265255, 0.0096120532508373, 0.0124888839141788, 0.0179283191125014, 0.019476202462918, 0.0209079220085484,
            0.0179318202997679, 0.0164437694453777, 0.0186558416869182,
            0.0201092896663371, 0.021780621447333, 0.023027554990938, 0.0243031746704984, 0.0253182328885705, 0.0262937569866255,
            0.0271023252591034, 0.0278325578499245, 0.0284587573045192,
            0.0290582636990626, 0.0295739720597311, 0.0300630229304215, 0.0304953922229926, 0.0309244291338963,
            0.0313084757037189, 0.031696115280366, 0.0320484447313153, 0.0324706505903697,
            0.032857785036212, 0.0331610176269445, 0.0334409934408067, 0.0337044029744327, 0.033944940639081, 0.0341701310108955,
            0.0343802785507912, 0.0344378289317298, 0.0344914900345785,
            0.0345421783629908, 0.0345896262432547, 0.0346343717250805, 0.0346766392888501, 0.0347166292222404,
            0.0347551287305473, 0.0347912445067621, 0.0348253682371375, 0.0347880042568088,
            0.0347526128122574, 0.0347186809082781, 0.0346864674010876, 0.0346555155111259, 0.0346260703411265,
            0.0345978740690391, 0.0345708488881244, 0.0345445048132247, 0.0345196296099635,
            0.0345077802995578, 0.0344964487054076, 0.034485484517828, 0.0344749272347962, 0.0344647546173287, 0.0344549986631012,
            0.0344455838720343, 0.0344364926560736, 0.0344277086157007,
            0.0344192164412062,},
        {0.00451102494265255, 0.00961230625181013, 0.0124890973580346, 0.0179287581253996, 0.019476202462918, 0.0209079220085484,
            0.0179146548564806, 0.0163995948752698, 0.0186206551386126,
            0.0201101769492334, 0.0217660475691097, 0.02302882260281, 0.0242922206616739, 0.025319832020548, 0.0262838878075548,
            0.0270917555423806, 0.0278116612226671, 0.0284384450444177,
            0.0290358978669123, 0.0295764562951511, 0.0300629812643314, 0.0304978571668544, 0.030919172038549, 0.0313024406525135,
            0.0316914861635501, 0.0320510528074843, 0.0324673389902428,
            0.0328532632414678, 0.0331579238389174, 0.0334436742300171, 0.0337038681393345, 0.0339468189060175,
            0.0341705955710052, 0.034382926602007, 0.0344392632577323, 0.0344929567193703,
            0.0345425749799782, 0.0345900504563705, 0.0346343206202881, 0.0346768091131062, 0.0347157043205886,
            0.0347537948601011, 0.0347893367213927, 0.0348232882333615, 0.0347864043700377,
            0.0347506934936443, 0.0347170172529163, 0.0346845215843763, 0.0346539756628263, 0.0346244304999217, 0.034596296501164,
            0.0345688959004665, 0.0345434627006801, 0.0345182248501461,
            0.0345058261111373, 0.0344937742017407, 0.0344823659106201, 0.0344711977594699, 0.034460610972114, 0.0344502328164517,
            0.0344403818892104, 0.0344307644175739, 0.0344215235693735,
            0.034412444990971,},
        {0.0045109691983703, 0.0096119267570081, 0.0124888839141788, 0.0179287581253996, 0.019476202462918, 0.0209073273478429,
            0.0179335278202602, 0.0164219833090028, 0.0186299845381473,
            0.0201100732741673, 0.0217671886968912, 0.0230287833690935, 0.0243010199689606, 0.0253196453298618,
            0.0262904854717679, 0.0271040384184857, 0.0278249286240857, 0.0284494210990554,
            0.0290457465231719, 0.0295762315011855, 0.0300615703697837, 0.0304979156078269, 0.0309225851220435, 0.031310886840618,
            0.0316975517117297, 0.0320510230134998, 0.0324662006814878,
            0.0328531122321117, 0.0331562801979802, 0.0334407808842474, 0.0336999187560647, 0.0339445461301522,
            0.0341697892178598, 0.0343834117442073, 0.0344401400378486, 0.0344933349470126,
            0.0345433166223671, 0.0345906185738692, 0.0346347385036731, 0.0346770994858479, 0.0347167301000603,
            0.0347546832186601, 0.0347901038970396, 0.034824131244467, 0.0347870490937025,
            0.0347511590458418, 0.034717501075579, 0.0346853678721082, 0.0346546581183228, 0.0346249635055764, 0.0345969953907137,
            0.0345697475582014, 0.0345440374570194, 0.0345189459840149,
            0.0345073236854629, 0.0344960234389951, 0.0344850339837525, 0.0344746250607718, 0.0344645933627979,
            0.0344548665425021, 0.0344455824855072, 0.0344364697801176, 0.0344278093650792,
            0.0344192986850272,}};

    final LocalDate[] spotDate = new LocalDate[] {LocalDate.of(2000, 7, 31), LocalDate.of(2013, 5, 31), LocalDate.of(2015, 1, 30),
        LocalDate.of(2033, 11, 29)};
    final int nDates = spotDate.length;

    final int nMoneyMarket = 6;
    final int nSwaps = 15;
    final int nInstruments = nMoneyMarket + nSwaps;

    final CurveNode[] types = new CurveNode[nInstruments];
    final Period[] tenors = new Period[nInstruments];
    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12};
    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25, 30};
    // check
//    ArgumentChecker.isTrue(mmMonths.length == nMoneyMarket, "mmMonths");
//    ArgumentChecker.isTrue(swapYears.length == nSwaps, "swapYears");

    for (int i = 0; i < nMoneyMarket; i++) {
      tenors[i] = Period.ofMonths(mmMonths[i]);
      TermDepositConvention convention = TermDepositConventions.USD_SHORT_DEPOSIT_T2;
      types[i] = TermDepositCurveNode.of(TermDepositTemplate.of(tenors[i], convention), QuoteId.of(StandardId.of("OG", "test")));
    }
    for (int i = nMoneyMarket; i < nInstruments; i++) {
      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
      FixedIborSwapConvention convention = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
      types[i] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Tenor.of(tenors[i]), convention),
          QuoteId.of(StandardId.of("OG", "test")));
    }

    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033,
        0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
        0.03367, 0.03419, 0.03411, 0.03412,};

    final DayCount moneyMarketDCC = ACT360;
    final DayCount swapDCC = D30360;
    final Period swapInterval = Period.ofMonths(6);
    final int nSamplePoints = sampleTimes.length;

    for (int k = 0; k < nDates; ++k) {
      final IsdaDiscountCurveCalibrator bob =
          new IsdaDiscountCurveCalibrator(spotDate[k], spotDate[k], types, tenors, moneyMarketDCC, swapDCC, swapInterval, ACT365,
              MOD_FOLLOWING, CALENDAR, REF_DATA);
      final IsdaCompliantZeroRateDiscountFactors hc = bob.build(rates);

      final int nCurvePoints = hc.getParameterCount();
      assertEquals(nInstruments, nCurvePoints);
      for (int i = 0; i < nSamplePoints; i++) {
        final double time = sampleTimes[k][i];
        final double zr = hc.getCurve().yValue(time);
        assertEquals(zeroRates[k][i], zr, 1.e-10);
      }
    }
  }

//  /**
//   * 
//   */
//  @Test(expectedExceptions = IllegalArgumentException.class)
//  public void OverlappingInstrumentsTest() {
//
//    final LocalDate spotDate = LocalDate.of(2013, 5, 31);
//
//    final int nMoneyMarket = 6;
//    final int nSwaps = 14;
//    final int nInstruments = nMoneyMarket + nSwaps;
//
//    final ISDAInstrumentTypes[] types = new ISDAInstrumentTypes[nInstruments];
//    final Period[] tenors = new Period[nInstruments];
//    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12};
//    final int[] swapYears = new int[] {1, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30};
//    // check
//    ArgumentChecker.isTrue(mmMonths.length == nMoneyMarket, "mmMonths");
//    ArgumentChecker.isTrue(swapYears.length == nSwaps, "swapYears");
//
//    for (int i = 0; i < nMoneyMarket; i++) {
//      types[i] = ISDAInstrumentTypes.MoneyMarket;
//      tenors[i] = Period.ofMonths(mmMonths[i]);
//    }
//    for (int i = nMoneyMarket; i < nInstruments; i++) {
//      types[i] = ISDAInstrumentTypes.Swap;
//      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
//    }
//
//    final double[] rates = new double[] {0.00340055550701297, 0.00636929056400781, 0.0102617798438113, 0.0135851258907251,
//        0.0162809551414651, 0.020583125112332, 0.0227369218210212,
//        0.0251978805237614, 0.0273223815467694, 0.0310882447627048, 0.0358397743454067, 0.036047665095421, 0.0415916567616181,
//        0.044066373237682, 0.046708518178509, 0.0491196954851753,
//        0.0529297239911766, 0.0562025436376854, 0.0589772202773522, 0.0607471217692999};
//
//    final DayCount moneyMarketDCC = ACT360;
//    final DayCount swapDCC = D30360;
//    final Period swapInterval = Period.ofMonths(6);
//
//    ISDACompliantYieldCurveBuild.build(spotDate, types, tenors, rates, moneyMarketDCC, swapDCC, swapInterval, MOD_FOLLOWING);
//  }

  /**
   * 
   */
  @Test
  public void dayCountTest() {

    final LocalDate spotDate = LocalDate.of(2009, 11, 13);

    final int nMoneyMarket = 6;
    final int nSwaps = 15;
    final int nInstruments = nMoneyMarket + nSwaps;

    final CurveNode[] types = new CurveNode[nInstruments];
    final Period[] tenors = new Period[nInstruments];
    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12};
    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25, 30};
    // check
//    ArgumentChecker.isTrue(mmMonths.length == nMoneyMarket, "mmMonths");
//    ArgumentChecker.isTrue(swapYears.length == nSwaps, "swapYears");


    for (int i = 0; i < nMoneyMarket; i++) {
      tenors[i] = Period.ofMonths(mmMonths[i]);
      TermDepositConvention convention = TermDepositConventions.USD_SHORT_DEPOSIT_T2;
      types[i] = TermDepositCurveNode.of(TermDepositTemplate.of(tenors[i], convention), QuoteId.of(StandardId.of("OG", "test")));
    }
    for (int i = nMoneyMarket; i < nInstruments; i++) {
      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
      FixedIborSwapConvention convention = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
      types[i] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Tenor.of(tenors[i]), convention),
          QuoteId.of(StandardId.of("OG", "test")));
    }

    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033,
        0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
        0.03367, 0.03419, 0.03411, 0.03412,};

    final DayCount[] moneyMarketDCC = new DayCount[] {D30360, ACT_ACT, ACT360};
    final DayCount[] swapDCC = new DayCount[] {D30360, ACT_ACT, ACT360};
    final Period swapInterval = Period.ofMonths(6);
    final double[][] zeroRates = new double[][] {
        {0.00451094132691394, 0.00945460303190611, 0.0122229540868544, 0.0178301408290063, 0.0192637126928983, 0.0206242461862328,
            0.0178588147430989, 0.0164238861778704, 0.018631344987978,
            0.0201117451815117, 0.0217674952346848, 0.0230301783236276, 0.0242934910735128, 0.0253210330445365,
            0.0262869428224258, 0.0271052836260002, 0.0278243965983341, 0.0284504902511728,
            0.0290513485359989, 0.0295773565758978, 0.0300639368228416, 0.0304988621348888, 0.0309234981122104,
            0.0313120284810566, 0.0316976136595328, 0.0320519270804712, 0.0324648779855183,
            0.0328537199479907, 0.0331543892482607, 0.0334413187554564, 0.0337025863249896, 0.0339452539736723,
            0.0341712396536822, 0.0343844468442421, 0.0344407813129133, 0.0344944726901091,
            0.0345440890244785, 0.0345915626578179, 0.0346355947596053, 0.0346783179464823, 0.0347178459386941,
            0.0347553007048015, 0.034790841186317, 0.0348247913802487, 0.0347878927806037,
            0.034752167636536, 0.0347186587816984, 0.0346861419429677, 0.0346554111637513, 0.0346253803814125, 0.0345977089573594,
            0.0345702974092172, 0.0345444362953857, 0.0345196061141392,
            0.034507535995373, 0.0344958660484172, 0.0344846975917001, 0.0344738254448885, 0.0344635192532148, 0.034453416163592,
            0.0344438263285551, 0.0344343129715817, 0.0344254678358085,
            0.0344166298790237,},
        {0.00444915928374197, 0.00948048554422632, 0.0123174428554907, 0.017684232421227, 0.0192113127644758, 0.0206227013525493,
            0.0178578803162074, 0.0164232684791861, 0.0186322669831205,
            0.020113699749959, 0.0217678904337317, 0.0230293843399428, 0.0242925501835449, 0.0253199726652561, 0.0262857612109845,
            0.0271039993039063, 0.0278237617886532, 0.0284504209376168,
            0.0290505062276185, 0.0295758375663676, 0.0300623424281604, 0.0304972003578786, 0.0309217696564316,
            0.0313102390160184, 0.0316963497583253, 0.032051146118778, 0.0324633901436513,
            0.0328515664951015, 0.0331523890961102, 0.0334394648986057, 0.033700865679316, 0.0339436570557279, 0.0341697579579161,
            0.0343830738553501, 0.0344392890347298, 0.0344928667194298,
            0.034542377990283, 0.0345897510973155, 0.0346336899603035, 0.0346763226800328, 0.0347157669709816, 0.0347531424259208,
            0.0347886076498017, 0.0348224859535626, 0.0347855935225375,
            0.0347498743509143, 0.0347163710980069, 0.0346838596953639, 0.0346531340536459, 0.0346231082917816,
            0.0345954414937712, 0.0345680345282262, 0.0345421777377941, 0.0345173517075981,
            0.0345052793810392, 0.0344936072994881, 0.0344824367999049, 0.0344715626644267, 0.0344612545876073,
            0.0344511496499889, 0.0344415580608378, 0.0344320429637392, 0.0344231962100678,
            0.0344143566366979,},
        {0.00451094132691394, 0.0096120532508373, 0.0124882436409548, 0.0179287581253996, 0.019476202462918, 0.0209061381614906,
            0.0181040003596387, 0.0166500255791151, 0.0188996867376634,
            0.020408389323302, 0.0220860526785435, 0.0233654469588231, 0.0246457399933126, 0.0256870932356245, 0.0266661166481783,
            0.0274955676222623, 0.0282303573824345, 0.028870099985772,
            0.0294788395706417, 0.030011747119614, 0.0305046822913288, 0.0309452878948724, 0.0313755978170794, 0.0317693196911606,
            0.0321643461543265, 0.0325273351521884, 0.0329459710324609,
            0.0333401660693457, 0.0336461396134401, 0.0339381309745242, 0.0342040076840362, 0.0344509563430373,
            0.0346809287391722, 0.0348978972147147, 0.0349547044083665, 0.0350088463313092,
            0.0350588790161168, 0.035106751019636, 0.03515115261234, 0.035194234306507, 0.0352340939938567, 0.0352718630578354,
            0.0353077017736973, 0.0353419368572384, 0.035303919264229,
            0.0352671107132153, 0.0352325856630051, 0.0351990827129553, 0.0351674199867719, 0.0351364784856934,
            0.0351079678932113, 0.0350797250576823, 0.035053079675205, 0.0350274964895541,
            0.0350149676071521, 0.0350028541063757, 0.0349912611565855, 0.0349799757789108, 0.0349692778673166,
            0.0349587907773192, 0.0349488364497969, 0.0349389615071325, 0.034929780183562,
            0.0349206063118399,}};
    final double[] sampleTimes = new double[] {0.0849315068493151, 0.167123287671233, 0.257534246575342, 0.495890410958904,
        0.747945205479452, 1.00547945205479, 1.4958904109589, 2.0027397260274,
        2.5013698630137, 3.0027397260274, 3.4986301369863, 4.0027397260274, 4.4986301369863, 5.0027397260274, 5.4986301369863,
        6.0027397260274, 6.5013698630137, 7.00821917808219, 7.50684931506849,
        8.00547945205479, 8.5041095890411, 9.00547945205479, 9.5013698630137, 10.0054794520548, 10.5041095890411,
        11.0082191780822, 11.5041095890411, 12.013698630137, 12.5041095890411,
        13.0109589041096, 13.5095890410959, 14.0082191780822, 14.5068493150685, 15.0109589041096, 15.5068493150685,
        16.0109589041096, 16.5068493150685, 17.0109589041096, 17.5068493150685,
        18.0164383561644, 18.5150684931507, 19.013698630137, 19.5123287671233, 20.013698630137, 20.5095890410959, 21.013698630137,
        21.5095890410959, 22.013698630137, 22.5123287671233, 23.0219178082192,
        23.5123287671233, 24.0191780821918, 24.5178082191781, 25.0164383561644, 25.5150684931507, 26.0164383561644,
        26.5150684931507, 27.0191780821918, 27.5150684931507, 28.0191780821918,
        28.5150684931507, 29.0246575342466, 29.5150684931507, 30.0219178082192,};

    for (int ii = 0; ii < 3; ++ii) {
      //      System.out.println(ii);
      final IsdaCompliantZeroRateDiscountFactors hc =
          (new IsdaDiscountCurveCalibrator(spotDate, spotDate, types, tenors, moneyMarketDCC[ii], swapDCC[ii], swapInterval,
              ACT365, BusinessDayConventions.FOLLOWING, CALENDAR, REF_DATA)).build(rates);

      final int nCurvePoints = hc.getParameterCount();
      assertEquals(nInstruments, nCurvePoints);
      final int nSamplePoints = sampleTimes.length;
      //      double ref = 0.;
      for (int i = 0; i < nSamplePoints; i++) {
        final double time = sampleTimes[i];
        final double zr = hc.getCurve().yValue(time);
        //        assertTrue(zr >= ref);
        //        ref = zr;
        assertEquals(zeroRates[ii][i], zr, 1.e-10);
      }
    }
  }

  /**
   * 
   */
  @Test
  public void onlyMoneyOrSwapTest() {

    // date from ISDA excel
    final double[] sampleTimes = new double[] {0.0767123287671233, 0.167123287671233, 0.249315068493151, 0.498630136986301,
        0.747945205479452, 0.997260273972603, 1.4958904109589, 1.99452054794521,
        2.5013698630137, 3.0027397260274, 3.5041095890411, 4.0027397260274, 4.5041095890411, 5.0027397260274, 5.5041095890411,
        6.0027397260274, 6.5013698630137, 7, 7.50684931506849, 8.00547945205479,
        8.50684931506849, 9.00547945205479, 9.50684931506849, 10.0054794520548, 10.5068493150685, 11.0082191780822,
        11.5068493150685, 12.0054794520548, 12.5041095890411, 13.0027397260274,
        13.5095890410959, 14.0082191780822, 14.5095890410959, 15.0109589041096, 15.5123287671233, 16.0109589041096,
        16.5123287671233, 17.0109589041096, 17.5095890410959, 18.0082191780822,
        18.5068493150685, 19.013698630137, 19.5150684931507, 20.013698630137, 20.5150684931507, 21.013698630137, 21.5150684931507,
        22.013698630137, 22.5150684931507, 23.013698630137, 23.5123287671233,
        24.0109589041096, 24.5178082191781, 25.0164383561644, 25.5178082191781, 26.0164383561644, 26.5178082191781,
        27.0191780821918, 27.5205479452055, 28.0191780821918, 28.5178082191781,
        29.0164383561644, 29.5150684931507, 30.013698630137};
    final double[] zeroRates = new double[] {0.00344732957665484, 0.00645427070262317, 0.010390833731528, 0.0137267241507424,
        0.016406009142171, 0.0206548075787697, 0.0220059788254565,
        0.0226815644487997, 0.0241475224808774, 0.0251107341245228, 0.0263549710022889, 0.0272832610741453, 0.0294785565070328,
        0.0312254350680597, 0.0340228731758456, 0.0363415444446394,
        0.0364040719835966, 0.0364576914896066, 0.0398713425199977, 0.0428078389323812, 0.0443206903065534, 0.0456582004054368,
        0.0473373527805339, 0.0488404232471453, 0.0496433764260127,
        0.0503731885238783, 0.0510359350109291, 0.0516436290741354, 0.0526405492486405, 0.0535610094687589, 0.05442700569164,
        0.0552178073994544, 0.0559581527041068, 0.0566490425640605,
        0.0572429526830672, 0.0577967261153023, 0.0583198210222109, 0.0588094750567186, 0.0592712408001043, 0.0597074348516306,
        0.0601201241459759, 0.0605174325075768, 0.0608901411604128,
        0.0612422922398251, 0.0618707980423834, 0.0624661234885966, 0.0630368977571603, 0.0635787665840882, 0.064099413535239,
        0.0645947156962813, 0.0650690099353217, 0.0655236050526131,
        0.0659667431709796, 0.0663851731522577, 0.0668735344788778, 0.0673405584796377, 0.0677924400667054, 0.0682275513575991,
        0.0686468089170376, 0.0690488939824011, 0.0694369182384849,
        0.06981160656508, 0.0701736348572483, 0.0705236340943412};

    final LocalDate spotDate = LocalDate.of(2013, 5, 31);
    final DayCount moneyMarketDCC = ACT360;
    final DayCount swapDCC = D30360;
    final Period swapInterval = Period.ofMonths(6);

    final int nMoneyMarket1 = 0;
    final int nSwaps1 = 14;
    final int nInstruments1 = nMoneyMarket1 + nSwaps1;

    final CurveNode[] types1 = new CurveNode[nInstruments1];
    final Period[] tenors1 = new Period[nInstruments1];
    final int[] mmMonths1 = new int[] {};
    final int[] swapYears1 = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30};
    // check
//    ArgumentChecker.isTrue(mmMonths1.length == nMoneyMarket1, "mmMonths");
//    ArgumentChecker.isTrue(swapYears1.length == nSwaps1, "swapYears");

    for (int i = 0; i < nMoneyMarket1; i++) {
      tenors1[i] = Period.ofMonths(mmMonths1[i]);
      TermDepositConvention convention = TermDepositConventions.USD_SHORT_DEPOSIT_T2;
      types1[i] =
          TermDepositCurveNode.of(TermDepositTemplate.of(tenors1[i], convention), QuoteId.of(StandardId.of("OG", "test")));
    }
    for (int i = nMoneyMarket1; i < nInstruments1; i++) {
      tenors1[i] = Period.ofYears(swapYears1[i - nMoneyMarket1]);
      FixedIborSwapConvention convention = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
      types1[i] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Tenor.of(tenors1[i]), convention),
          QuoteId.of(StandardId.of("OG", "test")));
    }

    final int nMoneyMarket2 = 6;
    final int nSwaps2 = 0;
    final int nInstruments2 = nMoneyMarket2 + nSwaps2;

    final CurveNode[] types2 = new CurveNode[nInstruments2];
    final Period[] tenors2 = new Period[nInstruments2];
    final int[] mmMonths2 = new int[] {1, 2, 3, 6, 9, 12};
    final int[] swapYears2 = new int[] {};
    // check
//    ArgumentChecker.isTrue(mmMonths2.length == nMoneyMarket2, "mmMonths");
//    ArgumentChecker.isTrue(swapYears2.length == nSwaps2, "swapYears");
    for (int i = 0; i < nMoneyMarket2; i++) {
      tenors2[i] = Period.ofMonths(mmMonths2[i]);
      TermDepositConvention convention = TermDepositConventions.USD_SHORT_DEPOSIT_T2;
      types2[i] =
          TermDepositCurveNode.of(TermDepositTemplate.of(tenors2[i], convention), QuoteId.of(StandardId.of("OG", "test")));
    }
    for (int i = nMoneyMarket2; i < nInstruments2; i++) {
      tenors2[i] = Period.ofYears(swapYears2[i - nMoneyMarket2]);
      FixedIborSwapConvention convention = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M;
      types2[i] = FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Tenor.of(tenors2[i]), convention),
          QuoteId.of(StandardId.of("OG", "test")));
    }

    final double[] rates1 = new double[] {0.0227369218210212, 0.0251978805237614, 0.0273223815467694, 0.0310882447627048,
        0.0358397743454067, 0.036047665095421, 0.0415916567616181, 0.044066373237682,
        0.046708518178509, 0.0491196954851753, 0.0529297239911766, 0.0562025436376854, 0.0589772202773522, 0.0607471217692999};
    final double[] rates2 = new double[] {0.00340055550701297, 0.00636929056400781, 0.0102617798438113, 0.0135851258907251,
        0.0162809551414651, 0.020583125112332};

    final IsdaCompliantZeroRateDiscountFactors hc1 =
        (new IsdaDiscountCurveCalibrator(spotDate, spotDate, types1, tenors1, moneyMarketDCC, swapDCC,
            swapInterval, ACT365, MOD_FOLLOWING, CALENDAR, REF_DATA)).build(rates1);
    final int nCurvePoints1 = hc1.getParameterCount();
    assertEquals(nInstruments1, nCurvePoints1);
    final IsdaCompliantZeroRateDiscountFactors hc2 =
        (new IsdaDiscountCurveCalibrator(spotDate, spotDate, types2, tenors2, moneyMarketDCC, swapDCC,
            swapInterval, ACT365, MOD_FOLLOWING, CALENDAR, REF_DATA)).build(rates2);
    final int nCurvePoints2 = hc2.getParameterCount();
    assertEquals(nInstruments2, nCurvePoints2);

    double ref1 = 0.;
    double ref2 = 0.;
    final int nSamplePoints = sampleTimes.length;
    for (int i = 0; i < nSamplePoints; i++) {
      final double time = sampleTimes[i];
      final double zr1 = hc1.getCurve().yValue(time);
      final double zr2 = hc2.getCurve().yValue(time);
      if (time < 1.) {
        assertEquals(zeroRates[i], zr2, 1e-10);
        if (i > 0) {
          assertTrue(zr1 == ref1);
        }
      }
      assertTrue(zr1 >= ref1);
      assertTrue(zr2 >= ref2);
      ref1 = zr1;
      ref2 = zr2;
    }
  }
//
//  /**
//   * convention == following
//   */
//  @Test
//  public void anotherConventionTest() {
//    final BusinessDayConvention conv = BusinessDayConventions.FOLLOWING;
//
//    // date from ISDA excel
//    final double[] sampleTimes = new double[] {0.0849315068493151, 0.167123287671233, 0.257534246575342, 0.495890410958904,
//        0.747945205479452, 1.00547945205479, 1.4958904109589, 2.0027397260274,
//        2.5013698630137, 3.0027397260274, 3.4986301369863, 4.0027397260274, 4.4986301369863, 5.0027397260274, 5.4986301369863,
//        6.0027397260274, 6.5013698630137, 7.00821917808219, 7.50684931506849,
//        8.00547945205479, 8.5041095890411, 9.00547945205479, 9.5013698630137, 10.0054794520548, 10.5041095890411,
//        11.0082191780822, 11.5041095890411, 12.013698630137, 12.5041095890411,
//        13.0109589041096, 13.5095890410959, 14.0082191780822, 14.5068493150685, 15.0109589041096, 15.5068493150685,
//        16.0109589041096, 16.5068493150685, 17.0109589041096, 17.5068493150685,
//        18.0164383561644, 18.5150684931507, 19.013698630137, 19.5123287671233, 20.013698630137, 20.5095890410959, 21.013698630137,
//        21.5095890410959, 22.013698630137, 22.5123287671233, 23.0219178082192,
//        23.5123287671233, 24.0191780821918, 24.5178082191781, 25.0164383561644, 25.5150684931507, 26.0164383561644,
//        26.5150684931507, 27.0191780821918, 27.5150684931507, 28.0191780821918,
//        28.5150684931507, 29.0246575342466, 29.5150684931507, 30.0219178082192,};
//    final double[] zeroRates = new double[] {0.00451094132691394, 0.0096120532508373, 0.0124882436409548, 0.0179287581253996,
//        0.019476202462918, 0.0209061381614906, 0.0181040003596387,
//        0.0166500255791151, 0.0188996867376634, 0.020408389323302, 0.0220860526785435, 0.0233654469588231, 0.0246457399933126,
//        0.0256870932356245, 0.0266661166481783, 0.0274955676222623,
//        0.0282303573824345, 0.028870099985772, 0.0294788395706417, 0.030011747119614, 0.0305046822913288, 0.0309452878948724,
//        0.0313755978170794, 0.0317693196911606, 0.0321643461543265,
//        0.0325273351521884, 0.0329459710324609, 0.0333401660693457, 0.0336461396134401, 0.0339381309745242, 0.0342040076840362,
//        0.0344509563430373, 0.0346809287391722, 0.0348978972147147,
//        0.0349547044083665, 0.0350088463313092, 0.0350588790161168, 0.035106751019636, 0.03515115261234, 0.035194234306507,
//        0.0352340939938567, 0.0352718630578354, 0.0353077017736973,
//        0.0353419368572384, 0.035303919264229, 0.0352671107132153, 0.0352325856630051, 0.0351990827129553, 0.0351674199867719,
//        0.0351364784856934, 0.0351079678932113, 0.0350797250576823,
//        0.035053079675205, 0.0350274964895541, 0.0350149676071521, 0.0350028541063757, 0.0349912611565855, 0.0349799757789108,
//        0.0349692778673166, 0.0349587907773192, 0.0349488364497969,
//        0.0349389615071325, 0.034929780183562, 0.0349206063118399,};
//
//    final LocalDate spotDate = LocalDate.of(2009, 11, 13);
//
//    final int nMoneyMarket = 6;
//    final int nSwaps = 15;
//    final int nInstruments = nMoneyMarket + nSwaps;
//
//    final ISDAInstrumentTypes[] types = new ISDAInstrumentTypes[nInstruments];
//    final Period[] tenors = new Period[nInstruments];
//    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12};
//    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25, 30};
//    // check
//    ArgumentChecker.isTrue(mmMonths.length == nMoneyMarket, "mmMonths");
//    ArgumentChecker.isTrue(swapYears.length == nSwaps, "swapYears");
//
//    for (int i = 0; i < nMoneyMarket; i++) {
//      types[i] = ISDAInstrumentTypes.MoneyMarket;
//      tenors[i] = Period.ofMonths(mmMonths[i]);
//    }
//    for (int i = nMoneyMarket; i < nInstruments; i++) {
//      types[i] = ISDAInstrumentTypes.Swap;
//      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
//    }
//
//    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033,
//        0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
//        0.03367, 0.03419, 0.03411, 0.03412,};
//
//    final DayCount moneyMarketDCC = ACT360;
//    final DayCount swapDCC = ACT360;
//    final Period swapInterval = Period.ofMonths(6);
//
//    final ISDACompliantCurve hc =
//        ISDACompliantYieldCurveBuild.build(spotDate, types, tenors, rates, moneyMarketDCC, swapDCC, swapInterval, conv);
//
//    final int nCurvePoints = hc.getNumberOfKnots();
//    assertEquals(nInstruments, nCurvePoints);
//    final int nSamplePoints = sampleTimes.length;
//    for (int i = 0; i < nSamplePoints; i++) {
//      final double time = sampleTimes[i];
//      final double zr = hc.getZeroRate(time);
//      //        System.out.println(time + "\t" + zr);
//      assertEquals(zeroRates[i], zr, 1.e-10);
//      //      assertEquals("time:" + time, zeroRates[i], zr, 1e-10);
//    }
//  }
//
//  /**
//   * BAD_DAY_PRIVIOUS case
//   */
//  @Test(enabled = false)
//  public void ConventionTest() {
//    final BusinessDayConvention conv = BusinessDayConventions.PRECEDING;
//
//    // date from ISDA excel
//    final double[] sampleTimes = new double[] {0.0849315068493151, 0.167123287671233, 0.257534246575342, 0.495890410958904,
//        0.747945205479452, 0.997260273972603, 1.00547945205479, 1.4958904109589,
//        1.99452054794521, 2.49315068493151, 3.0027397260274, 3.4986301369863, 4.0027397260274, 4.4986301369863, 5.0027397260274,
//        5.4986301369863, 6.0027397260274, 6.5013698630137, 7, 7.4986301369863,
//        8.00547945205479, 8.4958904109589, 9.00547945205479, 9.5013698630137, 10.0054794520548, 10.5041095890411,
//        11.0082191780822, 11.5041095890411, 12.0054794520548, 12.5041095890411,
//        13.0027397260274, 13.5013698630137, 14.0082191780822, 14.5068493150685, 15.0109589041096, 15.5068493150685,
//        16.0109589041096, 16.5068493150685, 17.0109589041096, 17.5068493150685,
//        18.0082191780822, 18.5068493150685, 19.013698630137, 19.5041095890411, 20.013698630137, 20.5095890410959, 21.013698630137,
//        21.5095890410959, 22.013698630137, 22.5123287671233, 23.013698630137,
//        23.5123287671233, 24.0109589041096, 24.5095890410959, 25.0164383561644, 25.5068493150685, 26.0164383561644,
//        26.5150684931507, 27.0191780821918, 27.5150684931507, 28.0191780821918,
//        28.5150684931507, 29.0164383561644, 29.5150684931507,};
//    final double[] zeroRates = new double[] {0.00451094132691394, 0.00945460303190611, 0.0122229540868544, 0.0178301408290063,
//        0.0192637126928983, 0.020591680224956, 0.0206242461862328,
//        0.0178470900581182, 0.0164236036069756, 0.0186204134485963, 0.0201116785235652, 0.0217674336981491, 0.023030120692705,
//        0.0242934370719117, 0.0253209819949203, 0.0262868945777896,
//        0.0271052377578073, 0.0278295229983663, 0.0284506223964347, 0.0290469851950165, 0.0295770409783873, 0.0300560714845454,
//        0.0304985616189548, 0.0309232085868126, 0.0313117490117008,
//        0.0316973434100398, 0.0320516653030913, 0.0324679345680479, 0.0328538431353751, 0.0331585855645591, 0.0334399554180535,
//        0.0337005422710057, 0.0339464118415257, 0.0341715292999348,
//        0.0343839173629805, 0.0344402664789812, 0.0344939718162856, 0.034543601051226, 0.0345910870280201, 0.0346351305784413,
//        0.0346771948006828, 0.0347167686837666, 0.0347548676479646,
//        0.0347898461197856, 0.0348243763914412, 0.0347874879780581, 0.0347517726963068, 0.0347182730919548, 0.034685765229853,
//        0.034655042934204, 0.0346254941265551, 0.0345973566571368,
//        0.0345703878366083, 0.0345445163389807, 0.0345192753750602, 0.0345074031846624, 0.0344955408983266, 0.0344843750709356,
//        0.0344735054836918, 0.0344632017183462, 0.034453101007236,
//        0.0344435134298795, 0.0344341530670394, 0.0344251592591684,};
//
//    final LocalDate spotDate = LocalDate.of(2009, 11, 13);
//
//    final int nMoneyMarket = 6;
//    final int nSwaps = 15;
//    final int nInstruments = nMoneyMarket + nSwaps;
//
//    final ISDAInstrumentTypes[] types = new ISDAInstrumentTypes[nInstruments];
//    final Period[] tenors = new Period[nInstruments];
//    final int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12};
//    final int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25, 30};
//    // check
//    ArgumentChecker.isTrue(mmMonths.length == nMoneyMarket, "mmMonths");
//    ArgumentChecker.isTrue(swapYears.length == nSwaps, "swapYears");
//
//    for (int i = 0; i < nMoneyMarket; i++) {
//      types[i] = ISDAInstrumentTypes.MoneyMarket;
//      tenors[i] = Period.ofMonths(mmMonths[i]);
//    }
//    for (int i = nMoneyMarket; i < nInstruments; i++) {
//      types[i] = ISDAInstrumentTypes.Swap;
//      tenors[i] = Period.ofYears(swapYears[i - nMoneyMarket]);
//    }
//
//    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033,
//        0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
//        0.03367, 0.03419, 0.03411, 0.03412,};
//
//    final DayCount moneyMarketDCC = ACT360;
//    final DayCount swapDCC = ACT360;
//    final Period swapInterval = Period.ofMonths(6);
//
//    final ISDACompliantCurve hc =
//        ISDACompliantYieldCurveBuild.build(spotDate, types, tenors, rates, moneyMarketDCC, swapDCC, swapInterval, conv);
//
//    final int nCurvePoints = hc.getNumberOfKnots();
//    assertEquals(nInstruments, nCurvePoints);
//    final int nSamplePoints = sampleTimes.length;
//    for (int i = 0; i < nSamplePoints; i++) {
//      final double time = sampleTimes[i];
//      final double zr = hc.getZeroRate(time);
//      //        System.out.println(time + "\t" + zr);
//      assertEquals(zeroRates[i], zr, 1.e-10);
//      //      assertEquals("time:" + time, zeroRates[i], zr, 1e-10);
//    }
//  }

}
