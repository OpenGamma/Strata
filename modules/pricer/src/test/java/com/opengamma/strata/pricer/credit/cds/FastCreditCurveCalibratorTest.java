package com.opengamma.strata.pricer.credit.cds;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConvention;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.data.ImmutableMarketData;
import com.opengamma.strata.data.ImmutableMarketDataBuilder;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveNode;
import com.opengamma.strata.market.curve.node.FixedIborSwapCurveNode;
import com.opengamma.strata.market.curve.node.TermDepositCurveNode;
import com.opengamma.strata.market.observable.QuoteId;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.credit.cds.Cds;
import com.opengamma.strata.product.credit.cds.CdsTrade;
import com.opengamma.strata.product.credit.cds.PaymentOnDefault;
import com.opengamma.strata.product.credit.cds.ProtectionStartOfDay;
import com.opengamma.strata.product.credit.cds.ResolvedCdsTrade;
import com.opengamma.strata.product.deposit.type.ImmutableTermDepositConvention;
import com.opengamma.strata.product.deposit.type.TermDepositConvention;
import com.opengamma.strata.product.deposit.type.TermDepositTemplate;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;
import com.opengamma.strata.product.swap.type.FixedIborSwapTemplate;
import com.opengamma.strata.product.swap.type.FixedRateSwapLegConvention;
import com.opengamma.strata.product.swap.type.IborRateSwapLegConvention;
import com.opengamma.strata.product.swap.type.ImmutableFixedIborSwapConvention;

public class FastCreditCurveCalibratorTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendarId DEFAULT_CALENDAR = HolidayCalendarIds.SAT_SUN;
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG", "ABC");
  private static final ResolvedCdsTrade[][] PILLAR_CDS;
  private static final CreditRatesProvider[] YIELD_CURVES;
  private static final double[][] SPREADS;
  private static final double[][] SUR_PROB_ISDA;
  private static final double[][] SUR_PROB_MARKIT_FIX;
  private static final double[] OBS_TIMES =
      new double[] {30 / 365., 90 / 365., 180. / 365., 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
  private static final int N_OBS = OBS_TIMES.length;
  private static final double ONE_BP = 1.0e-4;

  private static final BusinessDayConvention MOD_FOLLOWING = BusinessDayConventions.MODIFIED_FOLLOWING;
  private static final BusinessDayAdjustment BUS_ADJ = BusinessDayAdjustment.of(MOD_FOLLOWING, DEFAULT_CALENDAR);
  private static final DaysAdjustment ADJ_2D = DaysAdjustment.ofBusinessDays(2, DEFAULT_CALENDAR);
  private static final DaysAdjustment ADJ_3D = DaysAdjustment.ofBusinessDays(3, DEFAULT_CALENDAR);
  private static final TermDepositConvention TERM_2 = ImmutableTermDepositConvention.builder()
      .businessDayAdjustment(BUS_ADJ)
      .currency(EUR)
      .dayCount(ACT_360)
      .name("standar_eur")
      .spotDateOffset(ADJ_2D)
      .build();
  private static final TermDepositConvention TERM_3 = ImmutableTermDepositConvention.builder()
      .businessDayAdjustment(BUS_ADJ)
      .currency(EUR)
      .dayCount(ACT_360)
      .name("standar_eur")
      .spotDateOffset(ADJ_3D)
      .build();
  private static final FixedRateSwapLegConvention FIXED_LEG =
      FixedRateSwapLegConvention.of(EUR, THIRTY_U_360, Frequency.P12M, BUS_ADJ);
  private static final IborRateSwapLegConvention FLOATING_LEG = IborRateSwapLegConvention.of(IborIndices.EUR_LIBOR_3M);
  private static final FixedIborSwapConvention SWAP_2 =
      ImmutableFixedIborSwapConvention.of("standar_eur", FIXED_LEG, FLOATING_LEG, ADJ_2D);
  private static final FixedIborSwapConvention SWAP_3 =
      ImmutableFixedIborSwapConvention.of("standar_eur", FIXED_LEG, FLOATING_LEG, ADJ_3D);

  static {
    double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033,
        0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231, 0.03367, 0.03419, 0.03411, 0.03412};
    int nMoneyMarket = 6;
    int nSwaps = 15;
    int nInstruments = nMoneyMarket + nSwaps;
    List<CurveNode> types2 = new ArrayList(nInstruments);
    List<CurveNode> types3 = new ArrayList(nInstruments);
    int[] mmMonths = new int[] {1, 2, 3, 6, 9, 12};
    int[] swapYears = new int[] {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25, 30};
    String[] idValues = new String[] {"mm1M", "mm2M", "mm3M", "mm6M", "mm9M", "mm12M", "swap2Y", "swap3Y", "swap4Y", "swap5Y",
        "swap6Y", "swap7Y", "swap8Y", "swap9Y", "swap10Y", "swap11Y", "swap12Y", "swap15Y", "swap20Y", "swap25Y", "swap30Y"};
    for (int i = 0; i < nMoneyMarket; i++) {
      Period period = Period.ofMonths(mmMonths[i]);
      types2.add(TermDepositCurveNode.of(TermDepositTemplate.of(period, TERM_2), QuoteId.of(StandardId.of("OG", idValues[i]))));
      types3.add(TermDepositCurveNode.of(TermDepositTemplate.of(period, TERM_3), QuoteId.of(StandardId.of("OG", idValues[i]))));
    }
    for (int i = nMoneyMarket; i < nInstruments; i++) {
      Period period = Period.ofYears(swapYears[i - nMoneyMarket]);
      types2.add(FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Tenor.of(period), SWAP_2), QuoteId.of(StandardId.of("OG", idValues[i]))));
      types3.add(FixedIborSwapCurveNode.of(
          FixedIborSwapTemplate.of(Tenor.of(period), SWAP_3), QuoteId.of(StandardId.of("OG", idValues[i]))));
    }
    int nCases = 5;
    PILLAR_CDS = new ResolvedCdsTrade[nCases][];
    SPREADS = new double[nCases][];
    YIELD_CURVES = new CreditRatesProvider[nCases];
    SUR_PROB_MARKIT_FIX = new double[nCases][];
    SUR_PROB_ISDA = new double[nCases][];

    // case0
    LocalDate tradeDate0 = LocalDate.of(2011, 6, 19);
//  LocalDate spotDate = addWorkDays(tradeDate.minusDays(1), 3, DEFAULT_CALENDAR);
    LocalDate startDate0 = LocalDate.of(2011, 3, 21);
    ImmutableMarketDataBuilder builder0 = ImmutableMarketData.builder(tradeDate0);
    for (int j = 0; j < nInstruments; j++) {
      builder0.addValue(QuoteId.of(StandardId.of("OG", idValues[j])), rates[j]);
    }
    ImmutableMarketData quotes0 = builder0.build();
    IsdaCompliantZeroRateDiscountFactors hc0 =
        IsdaCompliantDiscountCurveCalibrator.DEFAULT.calibrate(types3, ACT_365F, CurveName.of("yield"), EUR, quotes0, REF_DATA);
    YIELD_CURVES[0] = CreditRatesProvider.builder()
        .valuationDate(tradeDate0)
        .discountCurves(ImmutableMap.of(EUR, hc0))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, ConstantRecoveryRates.of(LEGAL_ENTITY, tradeDate0, 0.4)))
        .creditCurves(ImmutableMap.of())
        .build();
    Period[] tenors = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5),
        Period.ofYears(7), Period.ofYears(10)};
    int nTenors = tenors.length;
    PILLAR_CDS[0] = new ResolvedCdsTrade[nTenors];
    SPREADS[0] = new double[] {0.00886315689995649, 0.00886315689995649, 0.0133044689825873, 0.0171490070952563,
        0.0183903639181293, 0.0194721890639724};
    SUR_PROB_ISDA[0] = new double[] {0.998772746815168, 0.996322757048216, 0.992659036212158, 0.985174753005029,
        0.959541054444166, 0.9345154655283, 0.897874320219939, 0.862605325653124, 0.830790530716993, 0.80016566917562,
        0.76968842828467, 0.740364207356242, 0.71215720464425, 0.685024855452902, 0.658926216751093};
    SUR_PROB_MARKIT_FIX[0] = new double[] {0.998773616100865, 0.996325358510497, 0.992664220011069, 0.985181033285486,
        0.959551128356433, 0.934529141029508, 0.897893062747179, 0.862628725130658, 0.830817532293803, 0.800195970143901,
        0.76972190245315, 0.740400570243092, 0.712196187570045, 0.685066206017066, 0.658969697981512};
    for (int i = 0; i < nTenors; ++i) {
      Cds product = Cds.of(
          BUY, LEGAL_ENTITY, EUR, 1d, startDate0, LocalDate.of(2011, 6, 20).plus(tenors[i]), DEFAULT_CALENDAR, SPREADS[0][i]);
      PILLAR_CDS[0][i] = CdsTrade.builder()
          .info(TradeInfo.builder().settlementDate(product.getSettlementDateOffset().adjust(tradeDate0, REF_DATA)).build())
          .product(product)
          .build()
          .resolve(REF_DATA);
    }

    // case1
    LocalDate tradeDate1 = LocalDate.of(2011, 3, 21);
    LocalDate effDate1 = LocalDate.of(2011, 3, 20); //note this is a Sunday - for a standard CDS this would roll to the Monday.
//    spotDate = addWorkDays(tradeDate.minusDays(1), 3, DEFAULT_CALENDAR);
    ImmutableMarketDataBuilder builder1 = ImmutableMarketData.builder(tradeDate1);
    for (int j = 0; j < nInstruments; j++) {
      builder1.addValue(QuoteId.of(StandardId.of("OG", idValues[j])), rates[j]);
    }
    ImmutableMarketData quotes1 = builder1.build();
    IsdaCompliantZeroRateDiscountFactors hc1 =
        IsdaCompliantDiscountCurveCalibrator.DEFAULT.calibrate(types2, ACT_365F, CurveName.of("yield"), EUR, quotes1, REF_DATA);
    YIELD_CURVES[1] = CreditRatesProvider.builder()
        .valuationDate(tradeDate1)
        .discountCurves(ImmutableMap.of(EUR, hc1))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, ConstantRecoveryRates.of(LEGAL_ENTITY, tradeDate1, 0.4)))
        .creditCurves(ImmutableMap.of())
        .build();
    tenors = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5), Period.ofYears(7),
        Period.ofYears(10)};
    nTenors = tenors.length;
    PILLAR_CDS[1] = new ResolvedCdsTrade[nTenors];
    SPREADS[1] = new double[] {0.027, 0.018, 0.012, 0.009, 0.007, 0.006};
    SUR_PROB_ISDA[1] = new double[] {0.996266535762958, 0.988841371514657, 0.977807258018988, 0.96475844963628, 0.953395823781617,
        0.940590393592274, 0.933146036536171, 0.927501935763199,
        0.924978347338877, 0.923516383873675, 0.919646843289677, 0.914974439245307, 0.91032577405212, 0.905700727101315,
        0.901099178396858};
    SUR_PROB_MARKIT_FIX[1] = new double[] {0.996272873932676, 0.988860244428938, 0.977844583012059, 0.964805380714707,
        0.953429040991605, 0.940617833909825, 0.933169293548597, 0.927521552929219,
        0.924995002753253, 0.923530307620416, 0.91965942070523, 0.914986149602945, 0.910336625838319, 0.905710728738695,
        0.901108338244616,};
    for (int i = 0; i < nTenors; ++i) {
      Cds product = Cds.of(
          BUY, LEGAL_ENTITY, EUR, 1d, effDate1, LocalDate.of(2011, 6, 20).plus(tenors[i]), DEFAULT_CALENDAR, SPREADS[1][i]);
      PILLAR_CDS[1][i] = CdsTrade.builder()
          .info(TradeInfo.builder().settlementDate(product.getSettlementDateOffset().adjust(tradeDate1, REF_DATA)).build())
          .product(product)
          .build()
          .resolve(REF_DATA);
    }

    // case2
    LocalDate tradeDate2 = LocalDate.of(2011, 5, 30);
//    spotDate = addWorkDays(tradeDate.minusDays(1), 3, DEFAULT_CALENDAR); // TODO replicate this logic??
    ImmutableMarketDataBuilder builder2 = ImmutableMarketData.builder(tradeDate2);
    for (int j = 0; j < nInstruments; j++) {
      builder2.addValue(QuoteId.of(StandardId.of("OG", idValues[j])), rates[j]);
    }
    ImmutableMarketData quotes2 = builder2.build();
    IsdaCompliantZeroRateDiscountFactors hc2 =
        IsdaCompliantDiscountCurveCalibrator.DEFAULT.calibrate(types2, ACT_365F, CurveName.of("yield"), EUR, quotes2, REF_DATA);
    YIELD_CURVES[2] = CreditRatesProvider.builder()
        .valuationDate(tradeDate2)
        .discountCurves(ImmutableMap.of(EUR, hc2))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, ConstantRecoveryRates.of(LEGAL_ENTITY, tradeDate2, 0.25)))
        .creditCurves(ImmutableMap.of())
        .build();
    LocalDate[] maDates = new LocalDate[] {LocalDate.of(2011, 6, 20), LocalDate.of(2012, 5, 30), LocalDate.of(2014, 6, 20),
        LocalDate.of(2016, 6, 20), LocalDate.of(2018, 6, 20)};
    PILLAR_CDS[2] = new ResolvedCdsTrade[maDates.length];
    SPREADS[2] = new double[] {0.05, 0.05, 0.05, 0.05, 0.05};
    SUR_PROB_ISDA[2] = new double[] {0.994488823839325, 0.983690222697363, 0.967711777571664, 0.935677618299157,
        0.875533583554252, 0.819255475760025, 0.7666904278069, 0.717503794565525, 0.671435362513808, 0.628322474825315,
        0.587977867136078, 0.550223788092265, 0.514893899760578, 0.481832544772127, 0.450894060523028};
    SUR_PROB_MARKIT_FIX[2] = new double[] {0.994492541382389, 0.983716053360113, 0.967769880036333, 0.935798775210736,
        0.875741081454824, 0.819537657320969, 0.766996460740263, 0.717827034617184, 0.671770999435863, 0.628667500941574,
        0.588329694303598, 0.550580121735183, 0.515252711846802, 0.482192049049632, 0.451252689837008};
    for (int i = 0; i < maDates.length; ++i) {
      Cds product = Cds.of(BUY, LEGAL_ENTITY, EUR, 1d, tradeDate2.plusDays(1), maDates[i], DEFAULT_CALENDAR, SPREADS[2][i])
              .toBuilder().dayCount(THIRTY_U_360).build();
      PILLAR_CDS[2][i] = CdsTrade.builder()
          .info(TradeInfo.builder().settlementDate(product.getSettlementDateOffset().adjust(tradeDate2, REF_DATA)).build())
          .product(product)
          .build()
          .resolve(REF_DATA);
    }

    // case3
    LocalDate tradeDate3 = LocalDate.of(2011, 5, 30);
    LocalDate effDate3 = LocalDate.of(2011, 7, 31);
    ImmutableMarketDataBuilder builder3 = ImmutableMarketData.builder(tradeDate3);
    for (int j = 0; j < nInstruments; j++) {
      builder3.addValue(QuoteId.of(StandardId.of("OG", idValues[j])), rates[j]);
    }
    ImmutableMarketData quotes3 = builder3.build();
    IsdaCompliantZeroRateDiscountFactors hc3 =
        IsdaCompliantDiscountCurveCalibrator.DEFAULT.calibrate(types2, ACT_365F, CurveName.of("yield"), EUR, quotes3, REF_DATA);
    YIELD_CURVES[3] = CreditRatesProvider.builder()
        .valuationDate(tradeDate3)
        .discountCurves(ImmutableMap.of(EUR, hc3))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, ConstantRecoveryRates.of(LEGAL_ENTITY, tradeDate3, 0.25)))
        .creditCurves(ImmutableMap.of())
        .build();
//    spotDate = addWorkDays(tradeDate.minusDays(1), 3, DEFAULT_CALENDAR);
    maDates = new LocalDate[] {LocalDate.of(2011, 11, 30), LocalDate.of(2012, 5, 30), LocalDate.of(2014, 5, 30),
        LocalDate.of(2016, 5, 30), LocalDate.of(2018, 5, 30), LocalDate.of(2021, 5, 30)};
    PILLAR_CDS[3] = new ResolvedCdsTrade[maDates.length];
    SPREADS[3] = new double[] {0.07, 0.06, 0.05, 0.055, 0.06, 0.065};
    SUR_PROB_ISDA[3] = new double[] {0.99238650617037, 0.977332973057625, 0.955179740225657, 0.92187587198518, 0.868032006457467,
        0.817353939709416, 0.751100020583073, 0.690170357851426, 0.622562049244094, 0.561519352597547, 0.500515112466997,
        0.44610942528539, 0.397617603088025, 0.354396812361283, 0.315874095202052};
    SUR_PROB_MARKIT_FIX[3] = new double[] {0.992434753056402, 0.977475525071675, 0.955458402114146, 0.923257693140384,
        0.86924227242564, 0.818402338488625, 0.752150342806546, 0.691215773405857, 0.623608833084194, 0.562557270491733,
        0.50153493334764, 0.447102836461508, 0.3985783104631, 0.355320200669978, 0.316756937570093};
    for (int i = 0; i < maDates.length; ++i) {
      Cds product = Cds.of(BuySell.BUY, LEGAL_ENTITY, EUR, 1d, effDate3, maDates[i], Frequency.P6M,
              BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, DEFAULT_CALENDAR), StubConvention.LONG_INITIAL,
              SPREADS[3][i], ACT_365F, PaymentOnDefault.ACCRUED_PREMIUM, ProtectionStartOfDay.BEGINNING,
              DaysAdjustment.ofCalendarDays(1), DaysAdjustment.ofBusinessDays(3, DEFAULT_CALENDAR));
      PILLAR_CDS[3][i] = CdsTrade.builder()
          .info(TradeInfo.builder().settlementDate(product.getSettlementDateOffset().adjust(tradeDate3, REF_DATA)).build())
          .product(product)
          .build()
          .resolve(REF_DATA);
    }

    // case4: designed to trip the low rates/low spreads branch
    LocalDate tradeDate4 = LocalDate.of(2014, 1, 14);
//    spotDate = addWorkDays(tradeDate.minusDays(1), 3, DEFAULT_CALENDAR);
    ImmutableMarketDataBuilder builder4 = ImmutableMarketData.builder(tradeDate4);
    for (int j = 0; j < rates.length; j++) {
      builder4.addValue(QuoteId.of(StandardId.of("OG", idValues[j])), rates[j] / 1000);
    }
    ImmutableMarketData quotes4 = builder4.build();
    IsdaCompliantZeroRateDiscountFactors hc4 =
        IsdaCompliantDiscountCurveCalibrator.DEFAULT.calibrate(types2, ACT_365F, CurveName.of("yield"), EUR, quotes4, REF_DATA);
    YIELD_CURVES[4] = CreditRatesProvider.builder()
        .valuationDate(tradeDate4)
        .discountCurves(ImmutableMap.of(EUR, hc4))
        .recoveryRateCurves(ImmutableMap.of(LEGAL_ENTITY, ConstantRecoveryRates.of(LEGAL_ENTITY, tradeDate4, 0.4)))
        .creditCurves(ImmutableMap.of())
        .build();
    SPREADS[4] = new double[6];
    Arrays.fill(SPREADS[4], ONE_BP);
    PILLAR_CDS[4] = new ResolvedCdsTrade[nTenors];
    for (int i = 0; i < nTenors; ++i) {
      Cds product = Cds.of(BuySell.BUY, LEGAL_ENTITY, EUR, 1d, LocalDate.of(2013, 12, 20),
          LocalDate.of(2014, 3, 20).plus(tenors[i]), DEFAULT_CALENDAR, SPREADS[4][i]);
      PILLAR_CDS[4][i] = CdsTrade.builder()
          .info(TradeInfo.builder().settlementDate(product.getSettlementDateOffset().adjust(tradeDate4, REF_DATA)).build())
          .product(product)
          .build()
          .resolve(REF_DATA);
    SUR_PROB_ISDA[4] = new double[] {0.999986111241871, 0.999958334304303, 0.999916670344636, 0.999831033196934,
          0.999662094963152, 0.999493185285761, 0.999324304350342, 0.999155451994703, 0.998986628218491, 0.998817832978659,
          0.998649066279251, 0.998480328100177, 0.998311618432194, 0.998142937270482, 0.997974284610226};
    SUR_PROB_MARKIT_FIX[4] = new double[] {0.999986111408132, 0.999958334803071, 0.999916671342131, 0.999831035053453,
          0.999662097437689, 0.999493188187127, 0.999324307673036, 0.9991554557374, 0.998986632383511, 0.998817837566403,
          0.998649071291, 0.998480333536109, 0.998311624292164, 0.998142943554348, 0.997974291317845};
    }
  }


  private void testCalibrationAgainstISDA(IsdaCompliantCreditCurveCalibrator builder, double tol) {
    int n = YIELD_CURVES.length;
    IsdaCdsProductPricer pricer = new IsdaCdsProductPricer(builder.getAccOnDefaultFormula());
    for (int i = 0; i < n; i++) {
      LegalEntitySurvivalProbabilities creditCurve =
          builder.calibrate(PILLAR_CDS[i], SPREADS[i], YIELD_CURVES[i], new double[SPREADS[i].length], REF_DATA);
      ResolvedCdsTrade[] cds = PILLAR_CDS[i];
      CreditRatesProvider provider = YIELD_CURVES[i].toBuilder()
          .creditCurves(ImmutableMap.of(Pair.of(LEGAL_ENTITY, EUR), creditCurve))
          .build();
      double[] expected =
          builder.getAccOnDefaultFormula() == AccrualOnDefaultFormulae.MARKIT_FIX ? SUR_PROB_MARKIT_FIX[i] : SUR_PROB_ISDA[i];
      for (int k = 0; k < N_OBS; k++) {
        assertEquals(creditCurve.getSurvivalProbabilities().discountFactor(OBS_TIMES[k]), expected[k], tol);
      }
      int m = cds.length;
      for (int j = 0; j < m; j++) {
        double price = pricer.price(
            cds[j].getProduct(), provider, SPREADS[i][j], cds[j].getInfo().getSettlementDate().get(), PriceType.CLEAN, REF_DATA);
        assertEquals(price, 0.0, 5e-16);
      }
    }
  }

  private static final FastCreditCurveCalibrator BUILDER_ISDA =
      new FastCreditCurveCalibrator(AccrualOnDefaultFormulae.ORIGINAL_ISDA);
  private static final FastCreditCurveCalibrator BUILDER_MARKIT =
      new FastCreditCurveCalibrator(AccrualOnDefaultFormulae.MARKIT_FIX);

  @Test
  public void test() {
    testCalibrationAgainstISDA(BUILDER_ISDA, 1e-14);
    testCalibrationAgainstISDA(BUILDER_MARKIT, 1e-14);
  }

//  /** TODO after simple builder is implemented
//   * 
//   */
//  @SuppressWarnings("deprecation")
//  @Test
//  public void noAccOnDefaultTest() {
//    final FastCreditCurveBuilder fastOg = new FastCreditCurveBuilder(OG_FIX, ArbitrageHandling.Ignore);
//
//    final SimpleCreditCurveBuilder simpleISDA = new SimpleCreditCurveBuilder(ORIGINAL_ISDA);
//    final SimpleCreditCurveBuilder simpleFix = new SimpleCreditCurveBuilder(MARKIT_FIX);
//    final SimpleCreditCurveBuilder simpleOg = new SimpleCreditCurveBuilder(OG_FIX);
//
//    final LocalDate tradeDate = LocalDate.of(2013, Month.APRIL, 25);
//
//    final CDSAnalyticFactory baseFactory = new CDSAnalyticFactory();
//    final CDSAnalyticFactory noAccFactory = baseFactory.withPayAccOnDefault(false);
//    final Period[] tenors = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(3), Period.ofYears(5),
//        Period.ofYears(7), Period.ofYears(10)};
//    final CDSAnalytic[] pillar = noAccFactory.makeIMMCDS(tradeDate, tenors);
//    final double[] spreads = new double[] {0.027, 0.017, 0.012, 0.009, 0.008, 0.005};
//
//    final LocalDate spotDate = addWorkDays(tradeDate.minusDays(1), 3, DEFAULT_CALENDAR);
//    final String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "9M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y",
//        "9Y", "10Y", "11Y", "12Y", "15Y", "20Y", "25Y", "30Y"};
//    final String[] yieldCurveInstruments =
//        new String[] {"M", "M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S"};
//    final double[] rates = new double[] {0.00445, 0.009488, 0.012337, 0.017762, 0.01935, 0.020838, 0.01652, 0.02018, 0.023033,
//        0.02525, 0.02696, 0.02825, 0.02931, 0.03017, 0.03092, 0.0316, 0.03231,
//        0.03367, 0.03419, 0.03411, 0.03412};
//    final ISDACompliantYieldCurve yc =
//        makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofYears(1));
//
//    final ISDACompliantCreditCurve curveFastISDA = BUILDER_ISDA.calibrateCreditCurve(pillar, spreads, yc);
//    final ISDACompliantCreditCurve curveFastFix = BUILDER_MARKIT.calibrateCreditCurve(pillar, spreads, yc);
//    final ISDACompliantCreditCurve curveFastOriginal = fastOg.calibrateCreditCurve(pillar, spreads, yc);
//    final ISDACompliantCreditCurve curveSimpleISDA = simpleISDA.calibrateCreditCurve(pillar, spreads, yc);
//    final ISDACompliantCreditCurve curveSimpleFix = simpleFix.calibrateCreditCurve(pillar, spreads, yc);
//    final ISDACompliantCreditCurve curveSimpleOriginal = simpleOg.calibrateCreditCurve(pillar, spreads, yc);
//
//    final double[] sampleTime = new double[] {30 / 365., 90 / 365., 180. / 365., 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
//    final int num = sampleTime.length;
//    for (int i = 0; i < num; ++i) {
//      assertEquals(curveSimpleISDA.getHazardRate(sampleTime[i]), curveFastISDA.getHazardRate(sampleTime[i]), 1.e-6);
//      assertEquals(curveSimpleFix.getHazardRate(sampleTime[i]), curveFastFix.getHazardRate(sampleTime[i]), 1.e-6);
//      assertEquals(curveSimpleOriginal.getHazardRate(sampleTime[i]), curveFastOriginal.getHazardRate(sampleTime[i]), 1.e-6);
//    }
//

//  
//    /*
//     * Flat hazard rate case
//     */
//    final double coupon = 0.025;
//    final MarketQuoteConverter conv = new MarketQuoteConverter();
//    final double[] pufs = conv.parSpreadsToPUF(new CDSAnalytic[] {pillar[3]}, coupon, yc, new double[] {spreads[3]});
//    final double[] qsps = conv.quotedSpreadToParSpreads(new CDSAnalytic[] {pillar[3]}, coupon, yc, new double[] {spreads[3]});
//
//    final PointsUpFront puf = new PointsUpFront(coupon, pufs[0]);
//    final QuotedSpread qsp = new QuotedSpread(coupon, qsps[0]);
//    final ParSpread psp = new ParSpread(spreads[3]);
//
//    final ISDACompliantCreditCurve curveFastPuf = BUILDER_ISDA.calibrateCreditCurve(pillar[3], puf, yc);
//    final ISDACompliantCreditCurve curveFastQsp = BUILDER_ISDA.calibrateCreditCurve(pillar[3], qsp, yc);
//    final ISDACompliantCreditCurve curveFastPsp = BUILDER_ISDA.calibrateCreditCurve(pillar[3], psp, yc);
//    final ISDACompliantCreditCurve curveSimplePuf = simpleISDA.calibrateCreditCurve(pillar[3], puf, yc);
//
//    final LocalDate stepinDate = tradeDate.plusDays(1);
//    final LocalDate valueDate = BusinessDayDateUtils.addWorkDays(tradeDate, 3, DEFAULT_CALENDAR);
//    final LocalDate startDate = IMMDateLogic.getPrevIMMDate(tradeDate);
//    final LocalDate endDate = IMMDateLogic.getNextIMMDate(tradeDate.plus(tenors[3]));
//    final ISDACompliantCreditCurve curveFastElem = BUILDER_ISDA.calibrateCreditCurve(tradeDate, stepinDate, valueDate, startDate,
//        endDate, spreads[3], false, Period.ofMonths(3), StubType.FRONTSHORT,
//        true, yc, 0.4);
//
//    assertEquals(1, curveFastPuf.getNumberOfKnots());
//    assertEquals(1, curveFastQsp.getNumberOfKnots());
//    assertEquals(1, curveFastPsp.getNumberOfKnots());
//
//    for (int i = 0; i < num; ++i) {
//      assertEquals(curveFastPuf.getForwardRate(sampleTime[i]), curveFastQsp.getForwardRate(sampleTime[i]), 1.e-12);
//      assertEquals(curveFastPuf.getForwardRate(sampleTime[i]), curveFastPsp.getForwardRate(sampleTime[i]), 1.e-12);
//      assertEquals(curveFastPuf.getForwardRate(sampleTime[i]), curveFastElem.getForwardRate(sampleTime[i]), 1.e-12);
//      assertEquals(curveSimplePuf.getForwardRate(sampleTime[i]), curveFastPuf.getForwardRate(sampleTime[i]), 1.e-6);
//    }
//
//    /*
//     * Consistency
//     */
//
//    final FastCreditCurveBuilder fastOriginalFail =
//        new FastCreditCurveBuilder(AccrualOnDefaultFormulae.OrignalISDA, ArbitrageHandling.Fail);
//    /*
//     * Fail with zero pufs
//     */
//    try {
//      fastOriginalFail.calibrateCreditCurve(pillar, spreads, yc);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//
//    /*
//     * Fail with nonzero pufs
//     */
//    final int nSpreads = spreads.length;
//    final PointsUpFront[] pufsFail = new PointsUpFront[nSpreads];
//    final double[] pufValues = conv.parSpreadsToPUF(pillar, coupon, yc, spreads);
//    for (int i = 0; i < nSpreads; ++i) {
//      pufsFail[i] = new PointsUpFront(coupon, pufValues[i]);
//    }
//    try {
//      fastOriginalFail.calibrateCreditCurve(pillar, pufsFail, yc);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//
//    /*
//     * ArgumentChecker hit 
//     */
//    final double[] prems = new double[nSpreads];
//    Arrays.fill(prems, coupon);
//    final double[] shortPufs = Arrays.copyOf(pufValues, nSpreads - 1);
//    try {
//      fastOriginalFail.calibrateCreditCurve(pillar, prems, yc, shortPufs);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//    final double[] shortPrems = Arrays.copyOf(prems, nSpreads - 1);
//    try {
//      fastOriginalFail.calibrateCreditCurve(pillar, shortPrems, yc, pufValues);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//
//    final CDSAnalytic[] pillarCopy = Arrays.copyOf(pillar, nSpreads);
//    pillarCopy[2] = pillarCopy[2].withOffset(0.5);
//    try {
//      fastOriginalFail.calibrateCreditCurve(pillarCopy, prems, yc, pufValues);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//    pillarCopy[2] = pillar[3];
//    pillarCopy[3] = pillar[2];
//    try {
//      fastOriginalFail.calibrateCreditCurve(pillarCopy, prems, yc, pufValues);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//
//    try {
//      BUILDER_ISDA.calibrateCreditCurve(tradeDate, stepinDate, valueDate, startDate, new LocalDate[] {endDate}, spreads, false,
//          Period.ofMonths(3), StubType.FRONTSHORT,
//          true, yc, 0.4);
//      throw new RuntimeException();
//    } catch (final Exception e) {
//      assertTrue(e instanceof IllegalArgumentException);
//    }
//
//  }

//  /**
//   * 
//   */
//  public void viaConverterTest() {
//    LocalDate tradeDate = LocalDate.of(2014, 5, 22);
//    double recovery = 0.25;
//    CDSAnalyticFactory immCDSFact = new CDSAnalyticFactory(recovery);
//
//    LocalDate spotDate = LocalDate.of(2014, 5, 27);
//    String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y",
//        "12Y", "15Y", "20Y", "25Y", "30Y"};
//    String[] yieldCurveInstruments =
//        new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S"};
//    double[] rates = new double[] {0.001, 0.002, 0.0025, 0.003, 0.0052, 0.0053, 0.00851, 0.0125, 0.016, 0.02, 0.02, 0.022, 0.024,
//        0.025, 0.02, 0.031,
//        0.030, 0.031, 0.0323};
//    ISDACompliantYieldCurve yieldCurve =
//        makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofMonths(6));
//    LocalDate end = LocalDate.of(2014, 6, 20);
//    double spF = 6.726 * 1.e-4;
//    double spS = 6.727 * 1.e-4;
//
//    CDSAnalytic cds = immCDSFact.makeCDS(tradeDate, getPrevIMMDate(tradeDate), end);
//    double coupon = 500. * 1.e-4;
//    MarketQuoteConverter conv = new MarketQuoteConverter();
//    double[] resS = conv.parSpreadsToQuotedSpreads(new CDSAnalytic[] {cds}, coupon, yieldCurve, new double[] {spS});
//    double[] resF = conv.parSpreadsToQuotedSpreads(new CDSAnalytic[] {cds}, coupon, yieldCurve, new double[] {spF});
//    assertEquals(resS[0], resF[0], 1.e-5);
//  }

//  /**
//   * 
//   */
//  public void viaImpliedSpreadTest() {
//
//    LocalDate tradeDate = LocalDate.of(2014, 5, 16);
//    double recoveryF = 0.248;
//    double recoveryS = 0.247;
//    CDSAnalyticFactory nonImmCDSFactS = new CDSAnalyticFactory(recoveryS, Period.ofMonths(6));
//    CDSAnalyticFactory nonImmCDSFactF = new CDSAnalyticFactory(recoveryF, Period.ofMonths(6));
//
//    LocalDate spotDate = LocalDate.of(2014, Month.MAY, 20);
//    String[] yieldCurvePoints = new String[] {"1M", "2M", "3M", "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y",
//        "12Y", "15Y", "20Y", "25Y", "30Y"};
//    String[] yieldCurveInstruments =
//        new String[] {"M", "M", "M", "M", "M", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S", "S"};
//    double[] rates = new double[] {0.00151, 0.0018, 0.0026, 0.0031, 0.0052, 0.0053, 0.00851, 0.0125, 0.016, 0.02, 0.02, 0.022,
//        0.024, 0.025, 0.02, 0.031,
//        0.030, 0.031, 0.0323};
//    ISDACompliantYieldCurve yieldCurve =
//        makeYieldCurve(tradeDate, spotDate, yieldCurvePoints, yieldCurveInstruments, rates, ACT360, D30360, Period.ofMonths(6));
//
//    LocalDate end = LocalDate.of(2014, 5, 20);
//    double puf = 1.101e-2;
//    double coupon = 500.0 * 1.0e-4;
//
//    Period[] buckets = new Period[] {Period.ofMonths(6), Period.ofYears(1), Period.ofYears(2), Period.ofYears(3),
//        Period.ofYears(4), Period.ofYears(5), Period.ofYears(6),
//        Period.ofYears(7), Period.ofYears(8), Period.ofYears(9), Period.ofYears(10), Period.ofYears(12), Period.ofYears(15),
//        Period.ofYears(20), Period.ofYears(25), Period.ofYears(30)};
//    CDSAnalytic cdsS = nonImmCDSFactS.makeCDS(tradeDate, getPrevIMMDate(tradeDate), end);
//    CDSAnalytic[] bucketCDSS = nonImmCDSFactS.makeIMMCDS(tradeDate, buckets);
//    CDSAnalytic cdsF = nonImmCDSFactF.makeCDS(tradeDate, getPrevIMMDate(tradeDate), end);
//    CDSAnalytic[] bucketCDSF = nonImmCDSFactF.makeIMMCDS(tradeDate, buckets);
//    double bump = 1.e-4;
//    FiniteDifferenceSpreadSensitivityCalculator cs01Cal = new FiniteDifferenceSpreadSensitivityCalculator();
//
//    PointsUpFront pufC = new PointsUpFront(coupon, puf);
//    double[] resS = cs01Cal.bucketedCS01FromPUF(cdsS, pufC, yieldCurve, bucketCDSS, bump);
//    double[] resF = cs01Cal.bucketedCS01FromPUF(cdsF, pufC, yieldCurve, bucketCDSF, bump);
//
//    for (int i = 0; i < resS.length; ++i) {
//      assertEquals(resS[i], resF[i], 1.e-5);
//    }
//  }

}
