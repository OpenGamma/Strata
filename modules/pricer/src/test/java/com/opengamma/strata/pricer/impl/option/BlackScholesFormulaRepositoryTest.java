/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.option;

import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link BlackScholesFormulaRepository}.
 */
@Test
public class BlackScholesFormulaRepositoryTest {

  private static final double EPS = 1.e-14;
  private static final double DELTA = 1.e-6;
  private static final double NAN = Double.NaN;
  private static final double INF = Double.POSITIVE_INFINITY;

  private static final double TIME_TO_EXPIRY = 4.5;
  private static final double SPOT = 100.;
  private static final double[] STRIKES_INPUT = new double[] {85.0, 90.0, 95.0, 100.0, 103.0, 108.0, 120.0, 150.0, 250.0};
  private static final double[] VOLS = new double[] {0.1, 0.12, 0.15, 0.2, 0.3, 0.5, 0.8};
  private static final double[] INTEREST_RATES = new double[] {-0.01, -0.005, 0, 0.008, 0.032, 0.062, 0.1};
  private static final double COST_OF_CARRY = 0.05;

  private static final double[] TIME_TO_EXPIRY_EX = {4.5, 0., 1.e-12, 1.e12, INF};
  private static final double[] SPOT_EX = {100., 0., 1.e-12, 1.e12, INF};
  private static final double[] STRIKES_INPUT_EX = new double[] {85.0, 90.0, 95.0, 100.0, 103.0, 108.0, 120.0, 150.0, 250.0, 0.,
      1.e-12, 1.e12, INF};
  private static final double[] VOLS_EX = new double[] {0.1, 0.12, 0.15, 0.2, 0.3, 0.5, 0.8, 0., 1.e-12, 1.e12, INF};
  private static final double[] INTEREST_RATES_EX = new double[] {-0.01, -0.005, 0, 0.008, 0.032, 0.062, 0.1, 0., 1.e-12, 1.e12,
      INF, 1.e-12, -INF};
  private static final double[] COST_OF_CARRY_EX = {0.05, 0., 1.e-12, 1.e12, INF, 1.e-12, -INF};

  private static final double[][][] PrecomputedCallPrice = new double[][][] {
      {
      {42.388192240722034, 41.445107405754896, 40.523005041587581, 39.090123476135133, 35.088373580052092,
          30.657270312145542, 25.838608802827295},
      {42.844635277413687, 41.891395149588462, 40.959363435265075, 39.511052365075997, 35.466210966900221,
          30.987392849065387, 26.116843198832520},
      {43.925335617747066, 42.948051244384672, 41.992510239239948, 40.507667401273466, 36.360800126408598,
          31.769009632151473, 26.775606685804973},
      {46.509094773672501, 45.474324955700368, 44.462577485967870, 42.890393795080151, 38.499601092232645,
          33.637714067925671, 28.350591098717572},
      {53.193986053605883, 52.010485675158193, 50.853316715912236, 49.055158361427800, 44.033263892482793,
          38.472563306420405, 32.425506341403754},
      {68.103724051366044, 66.588500448696692, 65.106988696562240, 62.804824684054182, 56.375343825382409,
          49.256034927120396, 41.514048860248678},
      {88.766116738864042, 86.791180462272393, 84.860184074456981, 81.859552870616028, 73.479393688121178,
          64.200115446500774, 54.109242317679445}},
      {
      {37.456487390776388, 36.623126887248162, 35.808307623895459, 34.542136375448479, 31.005974850266114,
          27.090413584076515, 22.832385002928326},
      {38.139841849279193, 37.291277554490861, 36.461592765423561, 35.172321546100306, 31.571646450304428,
          27.584649861820012, 23.248938000202855},
      {39.567718773636088, 38.687385983840642, 37.826639509476351, 36.489100637901586, 32.753623701507848,
          28.617362193449722, 24.119330232490562},
      {42.656781946566326, 41.707721322941552, 40.779776124021012, 39.337815208451701, 35.310708514387102,
          30.851527881998159, 26.002333273509258},
      {50.072156744232956, 48.958113205612392, 47.868855757438943, 46.176227066557196, 41.449055713979739,
          36.214699501747923, 30.522530016865090},
      {65.887130303467842, 64.421223169334212, 62.987930655324554, 60.760695913246877, 54.540477430048355,
          47.652882961695354, 40.162877798235037},
      {87.395751286588535, 85.451303945903021, 83.550118152890519, 80.595810495553920, 72.345023657486877,
          63.208998300993564, 53.273907405608348}},
      {
      {32.715707145094910, 31.987823136771382, 31.276133647099869, 30.170218740355807, 27.081620931686061,
          23.661643122317834, 19.942543287275861},
      {33.664230469455035, 32.915242990671231, 32.182919556648116, 31.044940966268769, 27.866795740876597,
          24.347662846564219, 20.520735510697762},
      {35.457091533184425, 34.668215113848404, 33.896890218850032, 32.698306128876695, 29.350901937810157,
          25.644350045459888, 21.613611455971515},
      {39.036835496346015, 38.168314202665016, 37.319116433239728, 35.999523428691212, 32.314165687937233,
          28.233400734474280, 23.795719231484398},
      {47.128774909821658, 46.080217975484366, 45.054990135668476, 43.461858907350447, 39.012563947403692,
          34.085897876593542, 28.728330081543525},
      {63.777287309365754, 62.358321571567203, 60.970926065899761, 58.815012014147349, 52.793977868286255,
          46.126938504827613, 38.876778890067470},
      {86.080586862737633, 84.165400303370134, 82.292824275492563, 79.382974160666237, 71.256348293338164,
          62.257805312674051, 52.472221434517728}},
      {
      {28.227691366172778, 27.599660156387031, 26.985601864023081, 26.031398901929947, 23.366502028061248,
          20.415684622407838, 17.206779436958136},
      {29.460572920194522, 28.805111621144633, 28.164233525066351, 27.168354493347223, 24.387064742833289,
          21.307366505179694, 17.958308164432047},
      {31.614253168446297, 30.910875148391668, 30.223146419068740, 29.154464831673863, 26.169852192071076,
          22.865016266736468, 19.271128987385019},
      {35.651639079672989, 34.858434218120514, 34.082877183389897, 32.877716648934467, 29.511945771703950,
          25.785056605612148, 21.732201980396823},
      {44.356758683618423, 43.369875680794145, 42.404949603817691, 40.905523030963742, 36.717926319013792,
          32.081036469111204, 27.038589635496734},
      {61.767323908809182, 60.393077369959677, 59.049406116390983, 56.961436446119521, 51.130157286355242,
          44.673231988200470, 37.651563676357824},
      {84.816610871615353, 82.929546214267660, 81.084466410882186, 78.217343475515833, 70.210046023101043,
          61.343634370732559, 51.701738442808583}},
      {
      {25.680774107575076, 25.109408655764113, 24.550755378366432, 23.682647873419768, 21.258198287726145,
          18.573626097805651, 15.654252772826112},
      {27.083882968770851, 26.481300080715201, 25.892123916407286, 24.976586013624527, 22.419672870447798,
          19.588424921750786, 16.509547114380702},
      {29.443114295640029, 28.788041429386809, 28.147543123955984, 27.152254259921776, 24.372612729001617,
          21.294739550739045, 17.947665894822222},
      {33.732817530354083, 32.982304074320730, 32.248488614153935, 31.108191521144597, 27.923571184422670,
          24.397268454983575, 20.562544187638785},
      {42.772673931596117, 41.821034854685237, 40.890568569874155, 39.444690065935177, 35.406642335828842,
          30.935346788260858, 26.072977655972238},
      {60.606631963610560, 59.258209384541409, 57.939787539597141, 55.891053656546795, 50.169352155690575,
          43.833761257507241, 36.944039634231586},
      {84.081289897742295, 82.210585222329073, 80.381501411520475, 77.539235112223196, 69.601357242839285,
          60.811813298151890, 51.253508169610768}},
      {
      {21.714207592329316, 21.231093338072895, 20.758727778267996, 20.024705256364712, 17.974728048498548,
          15.704805904238377, 13.236349223276591},
      {23.383992396693486, 22.863727496386588, 22.355037846443309, 21.564570269019399, 19.356953378635112,
          16.912478168716945, 14.254201461462443},
      {26.054446449835140, 25.474767250713811, 24.907985196597735, 24.027246150119325, 21.567519210574041,
          18.843884709915876, 15.882032561529108},
      {30.719333503473834, 30.035866338776202, 29.367605472912139, 28.329175562286544, 25.429049768831092,
          22.217765402134638, 18.725611995250446},
      {40.259006031733684, 39.363292955701560, 38.487508387440954, 37.126601386295221, 33.325861966011026,
          29.117335870422906, 24.540718833611479},
      {58.743506466504932, 57.436536124699913, 56.158644256025113, 54.172891076746374, 48.627082008907607,
          42.486255290141514, 35.808332534554012},
      {82.891922555376681, 81.047679831853728, 79.244469227244423, 76.442408052283383, 68.616815005325648,
          59.951603079507294, 50.528504439616363}},
      {
      {13.788335878907276, 13.481562464438824, 13.181614378904314, 12.715516363047620, 11.413798390334314, 9.972417266434356,
          8.404968411811751},
      {15.907916865834139, 15.553985396738028, 15.207928464947720, 14.670180577697515, 13.168358931159062,
          11.505404728972913, 9.697003317100766},
      {19.094139183282898, 18.669318209596582, 18.253948976989157, 17.608494698388782, 15.805870772872780,
          13.809840792343792, 11.639231745995765},
      {24.390979675271552, 23.848310553854262, 23.317715149001913, 22.493207584689088, 20.190523860255283,
          17.640781962021954, 14.868031610501113},
      {34.828165689108474, 34.053282091700282, 33.295638695653658, 32.118314683083376, 28.830285610372485,
          25.189479276293007, 21.230236558576035},
      {54.606702067923010, 53.391770505959599, 52.203869668144705, 50.357956157633822, 45.202691146926071,
          39.494310506189137, 33.286656924003069},
      {80.206222389649881, 78.421733172084004, 76.676946631850868, 73.965672302338390, 66.393628647109210,
          58.009169783685806, 48.891379752868431}},
      {
      {3.328906490728528, 3.254842439810734, 3.182426222394327, 3.069896565158853, 2.755623875073592, 2.407632426279232,
          2.029204549857958},
      {5.112418288611877, 4.998673306740088, 4.887459009990433, 4.714639893783904, 4.231990875892294, 3.697557766385675,
          3.116381454667383},
      {8.059806552432782, 7.880485828180092, 7.705154768187647, 7.432702756917415, 6.671799110673657, 5.829257042581645,
          4.913023592799185},
      {13.345242982640791, 13.048327836922702, 12.758018685855426, 12.306898889386751, 11.047012069592625, 9.651950221812832,
          8.134871873018049},
      {24.368455680587459, 23.826287690185346, 23.296182266803100, 22.472436098885268, 20.171878801378924,
          17.624491477407318, 14.854301638625593},
      {45.980465197347087, 44.957456733431215, 43.957208942178283, 42.402894934681818, 38.062008661910710,
          33.255382598719173, 28.028353888253228},
      {74.354192706245044, 72.699903909042803, 71.082420990903273, 68.568967446155852, 61.549397438409613,
          53.776687896749209, 45.324152709230404}},
      {
      {0.005810696974459, 0.005681416155733, 0.005555011675278, 0.005358588092742, 0.004810016549949, 0.004202587995173,
          0.003542031826745},
      {0.047125603795891, 0.046077117414915, 0.045051958558736, 0.043458934526089, 0.039009938942081, 0.034083604367675,
          0.028726397062748},
      {0.307974608501107, 0.301122554486089, 0.294422950195592, 0.284012241084843, 0.254937225321809, 0.222742710245262,
          0.187732361528245},
      {1.674165161621819, 1.636917058041711, 1.600497678683162, 1.543904550483984, 1.385851330768950, 1.210840358926868,
          1.020522376533685},
      {8.035294567394008, 7.856519204482371, 7.681721372215851, 7.410097958949876, 6.651508420206252, 5.811528743461139,
          4.898081799839467},
      {28.157055740368474, 27.530596085866989, 26.918074383626475, 25.966259173421985, 23.308030810128514,
          20.364597388924221, 17.163722014449160},
      {60.479193171039057, 59.133605947438191, 57.817956374810940, 55.773530405992034, 50.063860042770415,
          43.741591119891297, 36.866356653819025}}};

  /**
   * Check call price against the pre-computed values and put price via call-put parity
   */
  public void priceTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int k = 0; k < nInt; ++k) {
          double price =
              BlackScholesFormulaRepository.price(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true);
          assertEquals(PrecomputedCallPrice[i][j][k], price, Math.max(PrecomputedCallPrice[i][j][k] * EPS, EPS));
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int k = 0; k < nInt; ++k) {
          double call =
              BlackScholesFormulaRepository.price(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true);
          double put =
              BlackScholesFormulaRepository.price(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, false);
          double ref =
              SPOT * Math.exp((COST_OF_CARRY - INTEREST_RATES[k]) * TIME_TO_EXPIRY) - STRIKES_INPUT[i] *
                  Math.exp(-INTEREST_RATES[k] * TIME_TO_EXPIRY);
          assertEquals(ref, call - put, Math.max(EPS * Math.abs(ref), EPS));
        }
      }
    }
  }

  /**
   * Test all of the greeks by the finite difference method
   */
  public void greeksFiniteDiffTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;

    double[] upStrikes = new double[nStrikes];
    double[] dwStrikes = new double[nStrikes];
    double upSpot = SPOT * (1. + DELTA);
    double dwSpot = SPOT * (1. - DELTA);
    double upTime = TIME_TO_EXPIRY * (1. + DELTA);
    double dwTime = TIME_TO_EXPIRY * (1. - DELTA);
    double[] upVOLS = new double[nVols];
    double[] dwVOLS = new double[nVols];
    double[] upInt = new double[nInt];
    double[] dwInt = new double[nInt];
    double upCost = COST_OF_CARRY * (1. + DELTA);
    double dwCost = COST_OF_CARRY * (1. - DELTA);

    for (int i = 0; i < nStrikes; ++i) {
      upStrikes[i] = STRIKES_INPUT[i] * (1. + DELTA);
      dwStrikes[i] = STRIKES_INPUT[i] * (1. - DELTA);
    }
    for (int i = 0; i < nVols; ++i) {
      upVOLS[i] = VOLS[i] * (1. + DELTA);
      dwVOLS[i] = VOLS[i] * (1. - DELTA);
    }
    for (int i = 0; i < nVols; ++i) {
      upInt[i] = INTEREST_RATES[i] * (1. + DELTA);
      dwInt[i] = INTEREST_RATES[i] * (1. - DELTA);
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int k = 0; k < nInt; ++k) {

          double finDeltaC =
              (BlackScholesFormulaRepository.price(upSpot, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true) - BlackScholesFormulaRepository
                  .price(dwSpot, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                      true)) / 2. / SPOT / DELTA;
          double finDeltaP =
              (BlackScholesFormulaRepository.price(upSpot, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, false) - BlackScholesFormulaRepository
                  .price(dwSpot, STRIKES_INPUT[i], TIME_TO_EXPIRY,
                      VOLS[j], INTEREST_RATES[k], COST_OF_CARRY, false)) / 2. / SPOT / DELTA;
          assertEquals(finDeltaC, BlackScholesFormulaRepository.delta(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY, true), Math.abs(finDeltaC) * DELTA);
          assertEquals(finDeltaP, BlackScholesFormulaRepository.delta(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY, false), Math.abs(finDeltaP) * DELTA);

          double finDualDeltaC =
              (BlackScholesFormulaRepository.price(SPOT, upStrikes[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                  true) - BlackScholesFormulaRepository.price(
                  SPOT, dwStrikes[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                  true)) / 2. / STRIKES_INPUT[i] / DELTA;
          double finDualDeltaP =
              (BlackScholesFormulaRepository.price(SPOT, upStrikes[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                  false) - BlackScholesFormulaRepository
                  .price(SPOT, dwStrikes[i], TIME_TO_EXPIRY,
                      VOLS[j], INTEREST_RATES[k], COST_OF_CARRY, false)) / 2. / STRIKES_INPUT[i] / DELTA;
          assertEquals(finDualDeltaC, BlackScholesFormulaRepository.dualDelta(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY, true), Math.abs(finDualDeltaC) * DELTA);
          assertEquals(finDualDeltaP, BlackScholesFormulaRepository.dualDelta(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY, false), Math.abs(finDualDeltaP) *
              DELTA);

          double finGamma =
              (BlackScholesFormulaRepository.delta(upSpot, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true) - BlackScholesFormulaRepository
                  .delta(dwSpot, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                      true)) / 2. / SPOT / DELTA;
          assertEquals(finGamma, BlackScholesFormulaRepository.gamma(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY), Math.abs(finGamma) * DELTA);

          double finDualGamma =
              (BlackScholesFormulaRepository.dualDelta(SPOT, upStrikes[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true) - BlackScholesFormulaRepository
                  .dualDelta(SPOT, dwStrikes[i],
                      TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                      true)) / 2. / STRIKES_INPUT[i] / DELTA;
          assertEquals(finDualGamma, BlackScholesFormulaRepository.dualGamma(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY), Math.abs(finDualGamma) * DELTA);

          double finCrossGamma =
              (BlackScholesFormulaRepository.dualDelta(upSpot, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true) - BlackScholesFormulaRepository
                  .dualDelta(dwSpot, STRIKES_INPUT[i],
                      TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                      true)) /
                  2. / SPOT / DELTA;
          assertEquals(finCrossGamma, BlackScholesFormulaRepository.crossGamma(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY), Math.abs(finCrossGamma) * DELTA);

          double finThetaC =
              -(BlackScholesFormulaRepository.price(SPOT, STRIKES_INPUT[i], upTime, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                  true) - BlackScholesFormulaRepository.price(SPOT,
                  STRIKES_INPUT[i], dwTime, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                  true)) / 2. / TIME_TO_EXPIRY / DELTA;
          double finThetaP =
              -(BlackScholesFormulaRepository.price(SPOT, STRIKES_INPUT[i], upTime, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                  false) - BlackScholesFormulaRepository.price(SPOT,
                  STRIKES_INPUT[i], dwTime, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                  false)) / 2. / TIME_TO_EXPIRY / DELTA;
          assertEquals(finThetaC, BlackScholesFormulaRepository.theta(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY, true), Math.abs(finThetaC) * DELTA);
          assertEquals(finThetaP, BlackScholesFormulaRepository.theta(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY, false),
              Math.max(Math.abs(finThetaP) * DELTA, DELTA));

          double finCharmC =
              -(BlackScholesFormulaRepository.delta(SPOT, STRIKES_INPUT[i], upTime, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                  true) - BlackScholesFormulaRepository.delta(SPOT,
                  STRIKES_INPUT[i], dwTime, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                  true)) / 2. / TIME_TO_EXPIRY / DELTA;
          double finCharmP =
              -(BlackScholesFormulaRepository.delta(SPOT, STRIKES_INPUT[i], upTime, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                  false) - BlackScholesFormulaRepository.delta(SPOT,
                  STRIKES_INPUT[i], dwTime, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                  false)) / 2. / TIME_TO_EXPIRY / DELTA;
          assertEquals(finCharmC, BlackScholesFormulaRepository.charm(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY, true), Math.abs(finCharmC) * DELTA);
          assertEquals(finCharmP, BlackScholesFormulaRepository.charm(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY, false), Math.abs(finCharmP) * DELTA);

          double finDualCharmC =
              -(BlackScholesFormulaRepository.dualDelta(SPOT, STRIKES_INPUT[i], upTime, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true) - BlackScholesFormulaRepository
                  .dualDelta(SPOT,
                      STRIKES_INPUT[i], dwTime, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                      true)) / 2. / TIME_TO_EXPIRY / DELTA;
          double finDualCharmP =
              -(BlackScholesFormulaRepository.dualDelta(SPOT, STRIKES_INPUT[i], upTime, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, false) - BlackScholesFormulaRepository
                  .dualDelta(SPOT,
                      STRIKES_INPUT[i], dwTime, VOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                      false)) / 2. / TIME_TO_EXPIRY / DELTA;
          assertEquals(finDualCharmC, BlackScholesFormulaRepository.dualCharm(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY, true), Math.abs(finDualCharmC) * DELTA);
          assertEquals(finDualCharmP, BlackScholesFormulaRepository.dualCharm(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY, false), Math.abs(finDualCharmP) *
              DELTA);

          double finVega =
              (BlackScholesFormulaRepository.price(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, upVOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true) - BlackScholesFormulaRepository.price(
                  SPOT, STRIKES_INPUT[i],
                  TIME_TO_EXPIRY, dwVOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                  true)) / 2. / VOLS[j] / DELTA;
          assertEquals(finVega, BlackScholesFormulaRepository.vega(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY), Math.abs(finVega) * DELTA);

          double finVanna =
              (BlackScholesFormulaRepository.delta(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, upVOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true) - BlackScholesFormulaRepository
                  .delta(SPOT, STRIKES_INPUT[i],
                      TIME_TO_EXPIRY, dwVOLS[j], INTEREST_RATES[k], COST_OF_CARRY, true)) / 2. / VOLS[j] / DELTA;
          assertEquals(finVanna, BlackScholesFormulaRepository.vanna(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY), Math.abs(finVanna) * DELTA);

          double finDualVanna =
              (BlackScholesFormulaRepository.dualDelta(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, upVOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true) - BlackScholesFormulaRepository
                  .dualDelta(SPOT, STRIKES_INPUT[i],
                      TIME_TO_EXPIRY, dwVOLS[j], INTEREST_RATES[k], COST_OF_CARRY, true)) /
                  2. / VOLS[j] / DELTA;
          assertEquals(finDualVanna, BlackScholesFormulaRepository.dualVanna(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY), Math.abs(finDualVanna) * DELTA);

          double finVomma =
              (BlackScholesFormulaRepository.vega(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, upVOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY) - BlackScholesFormulaRepository.vega(SPOT,
                  STRIKES_INPUT[i],
                  TIME_TO_EXPIRY, dwVOLS[j], INTEREST_RATES[k], COST_OF_CARRY)) / 2. / VOLS[j] / DELTA;
          assertEquals(finVomma, BlackScholesFormulaRepository.vomma(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY), Math.abs(finVomma) * DELTA);

          double finRhoC =
              (BlackScholesFormulaRepository.price(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], upInt[k], COST_OF_CARRY +
                  INTEREST_RATES[k] * DELTA, true) - BlackScholesFormulaRepository
                    .price(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], dwInt[k], COST_OF_CARRY - INTEREST_RATES[k] * DELTA,
                        true)) /
                  2. / INTEREST_RATES[k] / DELTA;
          double finRhoP =
              (BlackScholesFormulaRepository.price(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], upInt[k], COST_OF_CARRY +
                  INTEREST_RATES[k] * DELTA, false) - BlackScholesFormulaRepository
                    .price(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], dwInt[k], COST_OF_CARRY - INTEREST_RATES[k] * DELTA,
                        false)) /
                  2. / INTEREST_RATES[k] / DELTA;
          if (INTEREST_RATES[k] != 0.) {
            assertEquals(finRhoC, BlackScholesFormulaRepository.rho(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
                INTEREST_RATES[k], COST_OF_CARRY, true), Math.abs(finRhoC) * DELTA);
            assertEquals(finRhoP, BlackScholesFormulaRepository.rho(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
                INTEREST_RATES[k], COST_OF_CARRY, false), Math.abs(finRhoP) * DELTA);
          }

          double finCarryRhoC =
              (BlackScholesFormulaRepository.price(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k], upCost,
                  true) - BlackScholesFormulaRepository
                  .price(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k], dwCost,
                      true)) /
                  2. / COST_OF_CARRY / DELTA;
          double finCarryRhoP =
              (BlackScholesFormulaRepository.price(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k], upCost,
                  false) - BlackScholesFormulaRepository
                  .price(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k], dwCost,
                      false)) /
                  2. / COST_OF_CARRY / DELTA;
          assertEquals(finCarryRhoC, BlackScholesFormulaRepository.carryRho(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY, true), Math.abs(finCarryRhoC) * DELTA);
          assertEquals(finCarryRhoP, BlackScholesFormulaRepository.carryRho(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY, false), Math.abs(finCarryRhoP) * DELTA);

          double finVegaBleed =
              (BlackScholesFormulaRepository.theta(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, upVOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true) - BlackScholesFormulaRepository
                  .theta(
                      SPOT, STRIKES_INPUT[i],
                      TIME_TO_EXPIRY, dwVOLS[j], INTEREST_RATES[k], COST_OF_CARRY,
                      true)) / 2. / VOLS[j] / DELTA;
          assertEquals(finVegaBleed, BlackScholesFormulaRepository.vegaBleed(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j],
              INTEREST_RATES[k], COST_OF_CARRY), Math.abs(finVegaBleed) * DELTA);
        }
      }
    }

  }

  /**
   *
   */
  public void strikeForDeltaRecoveryTest() {

    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int k = 0; k < nInt; ++k) {
          double deltaC =
              BlackScholesFormulaRepository.delta(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true);
          double deltaP =
              BlackScholesFormulaRepository.delta(SPOT, STRIKES_INPUT[i], TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, false);
          double resC =
              BlackScholesFormulaRepository.strikeForDelta(SPOT, deltaC, TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, true);
          double resP =
              BlackScholesFormulaRepository.strikeForDelta(SPOT, deltaP, TIME_TO_EXPIRY, VOLS[j], INTEREST_RATES[k],
                  COST_OF_CARRY, false);
          assertEquals(STRIKES_INPUT[i], resC, STRIKES_INPUT[i] * EPS);
          assertEquals(STRIKES_INPUT[i], resP, STRIKES_INPUT[i] * EPS);
        }
      }
    }
  }

  /*
   *
   * Tests below are checking that limiting case is properly realized
   * Since the formula typically becomes ambiguous when several inputs are very large/small at the same time,
   * we just check that a reference value (rather than NaN) is returned for that case.
   *
   *
   */

  /**
   * Large/small values for price
   */
  public void exPriceTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.price(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC1 = BlackScholesFormulaRepository.price(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.price(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.price(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.price(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP1 = BlackScholesFormulaRepository.price(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.price(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.price(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-11);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.price(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.price(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.price(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.price(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.price(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.price(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.price(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.price(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.price(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, true);
          double resC2 = BlackScholesFormulaRepository.price(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, true);
          double resP1 = BlackScholesFormulaRepository.price(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, false);
          double resP2 = BlackScholesFormulaRepository.price(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.price(SPOT, strike, 0., vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.price(SPOT, strike, inf, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.price(SPOT, strike, 0., vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.price(SPOT, strike, inf, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, true);
        double resC2 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, true);
        double resP1 =
            BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, false);
        double resP2 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, false);
        double refP2 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, true);
        double resC2 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, true);
        double resP1 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, false);
        double resP2 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, false);
        double refP2 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, false);

        double resC3 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, true);
        double refC3 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, true);
        double resP3 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, false);
        double refP3 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

        for (int k = 0; k < 6; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-11);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, true);
          double refC1 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., true);
          double resC2 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, true);
          double refC2 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, true);
          double resP1 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, false);
          double refP1 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., false);
          double resP2 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, false);
          double refP2 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, false);

          double resC3 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, true);
          double refC3 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, true);
          double resP3 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, false);
          double refP3 = BlackScholesFormulaRepository.price(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

          for (int k = 0; k < 6; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or negative value is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.price(spot, strike, time, vol, rate, cost, true);
                double resP1 = BlackScholesFormulaRepository.price(spot, strike, time, vol, rate, cost, false);

                assertTrue(!(Double.isNaN(resC1)));
                assertTrue(!(Double.isNaN(resP1)));
                assertTrue(resC1 >= 0.);
                assertTrue(resP1 >= 0.);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for delta
   */
  public void exDeltaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.delta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC1 = BlackScholesFormulaRepository.delta(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.delta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.delta(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.delta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP1 = BlackScholesFormulaRepository.delta(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.delta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.delta(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-11);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.delta(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.delta(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.delta(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.delta(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.delta(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.delta(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.delta(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.delta(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.delta(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, true);
          double resC2 = BlackScholesFormulaRepository.delta(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, true);
          double resP1 = BlackScholesFormulaRepository.delta(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, false);
          double resP2 = BlackScholesFormulaRepository.delta(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.delta(SPOT, strike, 0., vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.delta(SPOT, strike, inf, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.delta(SPOT, strike, 0., vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.delta(SPOT, strike, inf, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, true);
        double resC2 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, true);
        double resP1 =
            BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, false);
        double resP2 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, false);
        double refP2 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, true);
        double resC2 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, true);
        double resP1 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, false);
        double resP2 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, false);
        double refP2 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, false);

        double resC3 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, true);
        double refC3 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, true);
        double resP3 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, false);
        double refP3 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

        for (int k = 0; k < 6; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-11);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, true);
          double refC1 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., true);
          double resC2 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, true);
          double refC2 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, true);
          double resP1 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, false);
          double refP1 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., false);
          double resP2 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, false);
          double refP2 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, false);

          double resC3 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, true);
          double refC3 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, true);
          double resP3 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, false);
          double refP3 = BlackScholesFormulaRepository.delta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

          for (int k = 0; k < 6; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.delta(spot, strike, time, vol, rate, cost, true);
                double resP1 = BlackScholesFormulaRepository.delta(spot, strike, time, vol, rate, cost, false);

                assertTrue(!(Double.isNaN(resC1)));
                assertTrue(!(Double.isNaN(resP1)));
                assertTrue(resC1 >= 0.);
                assertTrue(resP1 <= 0.);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for dualDelta
   */
  public void exDualDeltaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.dualDelta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC1 =
              BlackScholesFormulaRepository.dualDelta(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.dualDelta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 =
              BlackScholesFormulaRepository.dualDelta(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.dualDelta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP1 =
              BlackScholesFormulaRepository.dualDelta(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.dualDelta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 =
              BlackScholesFormulaRepository.dualDelta(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-11);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.dualDelta(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.dualDelta(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.dualDelta(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.dualDelta(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.dualDelta(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.dualDelta(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.dualDelta(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 =
              BlackScholesFormulaRepository.dualDelta(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, true);
          double resC2 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, true);
          double resP1 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, false);
          double resP2 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, 0., vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, inf, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, 0., vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, inf, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, true);
        double resC2 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, true);
        double refC2 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, true);
        double resP1 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, false);
        double refP1 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, false);
        double resP2 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, false);
        double refP2 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, true);
        double resC2 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, true);
        double resP1 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, false);
        double resP2 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, false);
        double refP2 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, false);

        double resC3 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, true);
        double refC3 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, true);
        double resP3 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, false);
        double refP3 =
            BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

        for (int k = 0; k < 6; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-11);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, true);
          double refC1 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., true);
          double resC2 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, true);
          double refC2 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, true);
          double resP1 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, false);
          double refP1 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., false);
          double resP2 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, false);
          double refP2 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, false);

          double resC3 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, true);
          double refC3 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, true);
          double resP3 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, false);
          double refP3 = BlackScholesFormulaRepository.dualDelta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

          for (int k = 0; k < 6; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];
                double resC1 = BlackScholesFormulaRepository.dualDelta(spot, strike, time, vol, rate, cost, true);
                double resP1 = BlackScholesFormulaRepository.dualDelta(spot, strike, time, vol, rate, cost, false);

                assertTrue(!(Double.isNaN(resC1)));
                assertTrue(!(Double.isNaN(resP1)));
                assertTrue(resC1 <= 0.);
                assertTrue(resP1 >= 0.);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for gamma
   */
  public void exGammaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.gamma(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.gamma(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 =
              BlackScholesFormulaRepository.gamma(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.gamma(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-11);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.gamma(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 = BlackScholesFormulaRepository.gamma(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.gamma(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.gamma(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.gamma(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY);
          double resC2 = BlackScholesFormulaRepository.gamma(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.gamma(SPOT, strike, 0., vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.gamma(SPOT, strike, inf, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e9);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY);

        double resC3 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY);
        double refC3 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2, resC3};
        double[] refVec = new double[] {refC1, refC2, refC3};

        for (int k = 0; k < 3; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-11);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12);
          double refC1 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0.);
          double resC2 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12);
          double refC2 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf);

          double resC3 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12);
          double refC3 = BlackScholesFormulaRepository.gamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf);

          double[] resVec = new double[] {resC1, resC2, resC3};
          double[] refVec = new double[] {refC1, refC2, refC3};

          for (int k = 0; k < 3; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.gamma(spot, strike, time, vol, rate, cost);

                assertTrue(!(Double.isNaN(resC1)));
                assertTrue(resC1 >= 0.);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for DualGamma
   */
  public void exDualGammaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.dualGamma(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.dualGamma(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 =
              BlackScholesFormulaRepository.dualGamma(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.dualGamma(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-11);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.dualGamma(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 =
              BlackScholesFormulaRepository.dualGamma(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.dualGamma(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.dualGamma(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY);
          double resC2 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, 0., vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, inf, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e9);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY);

        double resC3 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY);
        double refC3 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2, resC3};
        double[] refVec = new double[] {refC1, refC2, refC3};

        for (int k = 0; k < 3; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-11);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12);
          double refC1 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0.);
          double resC2 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12);
          double refC2 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf);

          double resC3 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12);
          double refC3 = BlackScholesFormulaRepository.dualGamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf);

          double[] resVec = new double[] {resC1, resC2, resC3};
          double[] refVec = new double[] {refC1, refC2, refC3};

          for (int k = 0; k < 3; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];
                double resC1 = BlackScholesFormulaRepository.dualGamma(spot, strike, time, vol, rate, cost);

                assertTrue(!(Double.isNaN(resC1)));
                assertTrue(resC1 >= 0.);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for crossGamma
   */
  public void excrossGammaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.crossGamma(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.crossGamma(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 =
              BlackScholesFormulaRepository.crossGamma(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.crossGamma(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-11);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.crossGamma(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 =
              BlackScholesFormulaRepository.crossGamma(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.crossGamma(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.crossGamma(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY);
          double resC2 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, 0., vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, inf, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e9);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e9);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY);

        double resC3 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY);
        double refC3 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2, resC3};
        double[] refVec = new double[] {refC1, refC2, refC3};

        for (int k = 0; k < 3; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-11);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12);
          double refC1 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0.);
          double resC2 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12);
          double refC2 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf);

          double resC3 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12);
          double refC3 = BlackScholesFormulaRepository.crossGamma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf);

          double[] resVec = new double[] {resC1, resC2, resC3};
          double[] refVec = new double[] {refC1, refC2, refC3};

          for (int k = 0; k < 3; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];
                double resC1 = BlackScholesFormulaRepository.crossGamma(spot, strike, time, vol, rate, cost);

                assertTrue(!(Double.isNaN(resC1)));
                assertTrue(resC1 <= 0.);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for theta
   */
  public void exthetaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.theta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC1 = BlackScholesFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.theta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.theta(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP1 = BlackScholesFormulaRepository.theta(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.theta(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.theta(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.theta(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.theta(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.theta(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.theta(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.theta(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.theta(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.theta(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.theta(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (rate != 0.) {
              if (refVec[k] > 1.e10) {
                assertTrue(resVec[k] > 1.e10);
              } else {
                if (refVec[k] < -1.e10) {
                  assertTrue(resVec[k] < -1.e10);
                } else {
                  if (refVec[k] == 0.) {
                    assertTrue(Math.abs(resVec[k]) < 1.e-10);
                  } else {
                    assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                  }
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.theta(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, true);
          double resC2 = BlackScholesFormulaRepository.theta(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, true);
          double resP1 = BlackScholesFormulaRepository.theta(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, false);
          double resP2 = BlackScholesFormulaRepository.theta(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.theta(SPOT, strike, 0., vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.theta(SPOT, strike, inf, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.theta(SPOT, strike, 0., vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.theta(SPOT, strike, inf, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, true);
        double resC2 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, true);
        double resP1 =
            BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, false);
        double resP2 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, false);
        double refP2 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {

          if (rate != 0.) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, true);
        double resC2 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, true);
        double resP1 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, false);
        double resP2 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, false);
        double refP2 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, false);

        double resC3 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, true);
        double refC3 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, true);
        double resP3 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, false);
        double refP3 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

        for (int k = 0; k < 6; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e10);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e10);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-10);
              } else {
                assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, true);
          double refC1 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., true);
          double resC2 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, true);
          double refC2 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, true);
          double resP1 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, false);
          double refP1 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., false);
          double resP2 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, false);
          double refP2 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, false);

          double resC3 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, true);
          double refC3 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, true);
          double resP3 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, false);
          double refP3 = BlackScholesFormulaRepository.theta(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

          for (int k = 0; k < 6; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-9, 1.e-9));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.theta(spot, strike, time, vol, rate, cost, true);
                double resP1 = BlackScholesFormulaRepository.theta(spot, strike, time, vol, rate, cost, false);

                assertTrue(!(Double.isNaN(resC1)));
                assertTrue(!(Double.isNaN(resP1)));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for charm
   */
  public void excharmTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.charm(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC1 = BlackScholesFormulaRepository.charm(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.charm(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.charm(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.charm(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP1 = BlackScholesFormulaRepository.charm(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.charm(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.charm(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.charm(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.charm(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.charm(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.charm(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.charm(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.charm(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.charm(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.charm(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (rate != 0.) {
              if (refVec[k] > 1.e10) {
                assertTrue(resVec[k] > 1.e10);
              } else {
                if (refVec[k] < -1.e10) {
                  assertTrue(resVec[k] < -1.e10);
                } else {
                  if (refVec[k] == 0.) {
                    assertTrue(Math.abs(resVec[k]) < 1.e-10);
                  } else {
                    assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                  }
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.charm(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, true);
          double resC2 = BlackScholesFormulaRepository.charm(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, true);
          double resP1 = BlackScholesFormulaRepository.charm(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, false);
          double resP2 = BlackScholesFormulaRepository.charm(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.charm(SPOT, strike, 0., vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.charm(SPOT, strike, inf, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.charm(SPOT, strike, 0., vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.charm(SPOT, strike, inf, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, true);
        double resC2 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, true);
        double resP1 =
            BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, false);
        double resP2 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, false);
        double refP2 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {

          if (rate != 0.) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, true);
        double resC2 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, true);
        double resP1 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, false);
        double resP2 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, false);
        double refP2 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, false);

        double resC3 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, true);
        double refC3 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, true);
        double resP3 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, false);
        double refP3 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

        for (int k = 0; k < 6; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e10);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e10);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-10);
              } else {
                assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, true);
          double refC1 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., true);
          double resC2 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, true);
          double refC2 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, true);
          double resP1 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, false);
          double refP1 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., false);
          double resP2 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, false);
          double refP2 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, false);

          double resC3 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, true);
          double refC3 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, true);
          double resP3 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, false);
          double refP3 = BlackScholesFormulaRepository.charm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

          for (int k = 0; k < 6; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-9, 1.e-9));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.charm(spot, strike, time, vol, rate, cost, true);
                double resP1 = BlackScholesFormulaRepository.charm(spot, strike, time, vol, rate, cost, false);

                assertTrue(!(Double.isNaN(resC1)));
                assertTrue(!(Double.isNaN(resP1)));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for dualCharm
   */
  public void exdualCharmTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.dualCharm(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC1 =
              BlackScholesFormulaRepository.dualCharm(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.dualCharm(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 =
              BlackScholesFormulaRepository.dualCharm(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.dualCharm(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP1 =
              BlackScholesFormulaRepository.dualCharm(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.dualCharm(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 =
              BlackScholesFormulaRepository.dualCharm(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.dualCharm(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.dualCharm(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.dualCharm(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.dualCharm(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.dualCharm(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.dualCharm(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.dualCharm(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 =
              BlackScholesFormulaRepository.dualCharm(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (rate != 0.) {
              if (refVec[k] > 1.e10) {
                assertTrue(resVec[k] > 1.e10);
              } else {
                if (refVec[k] < -1.e10) {
                  assertTrue(resVec[k] < -1.e10);
                } else {
                  if (refVec[k] == 0.) {
                    assertTrue(Math.abs(resVec[k]) < 1.e-10);
                  } else {
                    assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                  }
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, true);
          double resC2 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, true);
          double resP1 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, false);
          double resP2 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, 0., vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, inf, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, 0., vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, inf, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e9);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, true);
        double resC2 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, true);
        double refC2 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, true);
        double resP1 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, false);
        double refP1 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, false);
        double resP2 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, false);
        double refP2 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {

          if (rate != 0.) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, true);
        double resC2 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, true);
        double resP1 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, false);
        double resP2 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, false);
        double refP2 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, false);

        double resC3 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, true);
        double refC3 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, true);
        double resP3 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, false);
        double refP3 =
            BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

        for (int k = 0; k < 6; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e10);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e10);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-10);
              } else {
                assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, true);
          double refC1 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., true);
          double resC2 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, true);
          double refC2 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, true);
          double resP1 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, false);
          double refP1 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., false);
          double resP2 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, false);
          double refP2 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, false);

          double resC3 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, true);
          double refC3 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, true);
          double resP3 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, false);
          double refP3 = BlackScholesFormulaRepository.dualCharm(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

          for (int k = 0; k < 6; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-9, 1.e-9));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.dualCharm(spot, strike, time, vol, rate, cost, true);
                double resP1 = BlackScholesFormulaRepository.dualCharm(spot, strike, time, vol, rate, cost, false);

                assertTrue(!(Double.isNaN(resC1)));
                assertTrue(!(Double.isNaN(resP1)));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for vega
   */
  public void exVegaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.vega(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.vega(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 =
              BlackScholesFormulaRepository.vega(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.vega(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-11);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.vega(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 = BlackScholesFormulaRepository.vega(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.vega(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.vega(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.vega(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY);
          double resC2 = BlackScholesFormulaRepository.vega(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.vega(SPOT, strike, 0., vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.vega(SPOT, strike, inf, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e9);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e9);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY);

        double resC3 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY);
        double refC3 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2, resC3};
        double[] refVec = new double[] {refC1, refC2, refC3};

        for (int k = 0; k < 3; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-11);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12);
          double refC1 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0.);
          double resC2 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12);
          double refC2 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf);

          double resC3 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12);
          double refC3 = BlackScholesFormulaRepository.vega(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf);

          double[] resVec = new double[] {resC1, resC2, resC3};
          double[] refVec = new double[] {refC1, refC2, refC3};

          for (int k = 0; k < 3; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.vega(spot, strike, time, vol, rate, cost);

                assertTrue(!(Double.isNaN(resC1)));
                assertTrue(resC1 >= 0.);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for vanna
   */
  public void exVannaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.vanna(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.vanna(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 =
              BlackScholesFormulaRepository.vanna(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.vanna(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-11);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.vanna(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 = BlackScholesFormulaRepository.vanna(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.vanna(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.vanna(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.vanna(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY);
          double resC2 = BlackScholesFormulaRepository.vanna(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.vanna(SPOT, strike, 0., vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.vanna(SPOT, strike, inf, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e9);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e9);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY);

        double resC3 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY);
        double refC3 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2, resC3};
        double[] refVec = new double[] {refC1, refC2, refC3};

        for (int k = 0; k < 3; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e10);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e10);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-10);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12);
          double refC1 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0.);
          double resC2 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12);
          double refC2 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf);

          double resC3 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12);
          double refC3 = BlackScholesFormulaRepository.vanna(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf);

          double[] resVec = new double[] {resC1, resC2, resC3};
          double[] refVec = new double[] {refC1, refC2, refC3};

          for (int k = 0; k < 3; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.vanna(spot, strike, time, vol, rate, cost);

                assertTrue(!(Double.isNaN(resC1)));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for dualVanna
   */
  public void exDualVannaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.dualVanna(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.dualVanna(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 =
              BlackScholesFormulaRepository.dualVanna(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.dualVanna(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-11);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.dualVanna(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 =
              BlackScholesFormulaRepository.dualVanna(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.dualVanna(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.dualVanna(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY);
          double resC2 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, 0., vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, inf, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e9);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e9);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY);

        double resC3 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY);
        double refC3 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2, resC3};
        double[] refVec = new double[] {refC1, refC2, refC3};

        for (int k = 0; k < 3; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e10);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e10);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-10);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12);
          double refC1 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0.);
          double resC2 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12);
          double refC2 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf);

          double resC3 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12);
          double refC3 = BlackScholesFormulaRepository.dualVanna(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf);

          double[] resVec = new double[] {resC1, resC2, resC3};
          double[] refVec = new double[] {refC1, refC2, refC3};

          for (int k = 0; k < 3; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.dualVanna(spot, strike, time, vol, rate, cost);

                assertTrue(!(Double.isNaN(resC1)));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for vomma
   */
  public void exvommaTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.vomma(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.vomma(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 =
              BlackScholesFormulaRepository.vomma(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.vomma(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-11);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.vomma(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 = BlackScholesFormulaRepository.vomma(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.vomma(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.vomma(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.vomma(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY);
          double resC2 = BlackScholesFormulaRepository.vomma(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.vomma(SPOT, strike, 0., vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.vomma(SPOT, strike, inf, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e9);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e9);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY);

        double resC3 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY);
        double refC3 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2, resC3};
        double[] refVec = new double[] {refC1, refC2, refC3};

        for (int k = 0; k < 3; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e10);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e10);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-10);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12);
          double refC1 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0.);
          double resC2 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12);
          double refC2 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf);

          double resC3 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12);
          double refC3 = BlackScholesFormulaRepository.vomma(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf);

          double[] resVec = new double[] {resC1, resC2, resC3};
          double[] refVec = new double[] {refC1, refC2, refC3};

          for (int k = 0; k < 3; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-8, 1.e-8));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.vomma(spot, strike, time, vol, rate, cost);

                assertTrue(!(Double.isNaN(resC1)));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for vegaBleed
   */
  public void exVegaBleedTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.vegaBleed(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.vegaBleed(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 =
              BlackScholesFormulaRepository.vegaBleed(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.vegaBleed(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-11);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.vegaBleed(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double resC2 =
              BlackScholesFormulaRepository.vegaBleed(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.vegaBleed(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.vegaBleed(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {
            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-12);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, 1e-28, vol, rate, COST_OF_CARRY);
          double resC2 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, 1e28, vol, rate, COST_OF_CARRY);
          double refC1 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, 0., vol, rate, COST_OF_CARRY);
          double refC2 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, inf, vol, rate, COST_OF_CARRY);

          double[] resVec = new double[] {resC1, resC2};
          double[] refVec = new double[] {refC1, refC2};

          for (int k = 0; k < 2; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e9);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e9);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2};
        double[] refVec = new double[] {refC1, refC2};

        for (int k = 0; k < 2; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY);
        double refC1 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY);
        double resC2 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY);
        double refC2 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY);

        double resC3 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY);
        double refC3 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY);

        double[] resVec = new double[] {resC1, resC2, resC3};
        double[] refVec = new double[] {refC1, refC2, refC3};

        for (int k = 0; k < 3; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e10);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e10);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-10);
              } else {
                assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12);
          double refC1 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0.);
          double resC2 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12);
          double refC2 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf);

          double resC3 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12);
          double refC3 = BlackScholesFormulaRepository.vegaBleed(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf);

          double[] resVec = new double[] {resC1, resC2, resC3};
          double[] refVec = new double[] {refC1, refC2, refC3};

          for (int k = 0; k < 3; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-8, 1.e-8));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.vegaBleed(spot, strike, time, vol, rate, cost);

                assertTrue(!(Double.isNaN(resC1)));
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for rho
   */
  public void exRhoTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.rho(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC1 = BlackScholesFormulaRepository.rho(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.rho(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.rho(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.rho(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP1 = BlackScholesFormulaRepository.rho(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.rho(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.rho(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e12);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e12);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-11);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.rho(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.rho(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.rho(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.rho(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.rho(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.rho(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.rho(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.rho(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e9);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e9);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-8);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-9);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.rho(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, true);
          double resC2 = BlackScholesFormulaRepository.rho(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, true);
          double resP1 = BlackScholesFormulaRepository.rho(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, false);
          double resP2 = BlackScholesFormulaRepository.rho(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.rho(SPOT, strike, 0., vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.rho(SPOT, strike, inf, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.rho(SPOT, strike, 0., vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.rho(SPOT, strike, inf, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, true);
        double resC2 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, true);
        double resP1 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, false);
        double resP2 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, false);
        double refP2 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, true);
        double resC2 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, true);
        double resP1 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, false);
        double resP2 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, false);
        double refP2 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, false);

        double resC3 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, true);
        double refC3 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, true);
        double resP3 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, false);
        double refP3 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

        for (int k = 0; k < 6; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-11);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, true);
          double refC1 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., true);
          double resC2 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, true);
          double refC2 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, true);
          double resP1 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, false);
          double refP1 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., false);
          double resP2 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, false);
          double refP2 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, false);

          double resC3 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, true);
          double refC3 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, true);
          double resP3 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, false);
          double refP3 = BlackScholesFormulaRepository.rho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

          for (int k = 0; k < 6; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.rho(spot, strike, time, vol, rate, cost, true);
                double resP1 = BlackScholesFormulaRepository.rho(spot, strike, time, vol, rate, cost, false);

                assertTrue(!(Double.isNaN(resC1)));
                assertTrue(!(Double.isNaN(resP1)));
                assertTrue(resC1 >= 0.);
                assertTrue(resP1 <= 0.);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Large/small values for carryRho
   */
  public void excarryRhoTest() {
    int nStrikes = STRIKES_INPUT.length;
    int nVols = VOLS.length;
    int nInt = INTEREST_RATES.length;
    double inf = Double.POSITIVE_INFINITY;

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.carryRho(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC1 = BlackScholesFormulaRepository.carryRho(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.carryRho(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 =
              BlackScholesFormulaRepository.carryRho(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.carryRho(1.e-12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP1 =
              BlackScholesFormulaRepository.carryRho(0., strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.carryRho(1.e12 * strike, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 =
              BlackScholesFormulaRepository.carryRho(inf, strike, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e9);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e9);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-8);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-9);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double vol = VOLS[j];
          double resC1 =
              BlackScholesFormulaRepository.carryRho(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resC2 =
              BlackScholesFormulaRepository.carryRho(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double resP1 =
              BlackScholesFormulaRepository.carryRho(SPOT, 1.e-12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double resP2 =
              BlackScholesFormulaRepository.carryRho(SPOT, 1.e12 * SPOT, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.carryRho(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.carryRho(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.carryRho(SPOT, 0., TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.carryRho(SPOT, inf, TIME_TO_EXPIRY, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e9);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e9);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-8);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-9);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.carryRho(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, true);
          double resC2 = BlackScholesFormulaRepository.carryRho(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, true);
          double resP1 = BlackScholesFormulaRepository.carryRho(SPOT, strike, 1e-24, vol, rate, COST_OF_CARRY, false);
          double resP2 = BlackScholesFormulaRepository.carryRho(SPOT, strike, 1e24, vol, rate, COST_OF_CARRY, false);
          double refC1 = BlackScholesFormulaRepository.carryRho(SPOT, strike, 0., vol, rate, COST_OF_CARRY, true);
          double refC2 = BlackScholesFormulaRepository.carryRho(SPOT, strike, inf, vol, rate, COST_OF_CARRY, true);
          double refP1 = BlackScholesFormulaRepository.carryRho(SPOT, strike, 0., vol, rate, COST_OF_CARRY, false);
          double refP2 = BlackScholesFormulaRepository.carryRho(SPOT, strike, inf, vol, rate, COST_OF_CARRY, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2};

          for (int k = 0; k < 4; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-10);
                }
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int l = 0; l < nInt; ++l) {
        double rate = INTEREST_RATES[l];
        double strike = STRIKES_INPUT[i];
        double resC1 =
            BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, true);
        double resC2 =
            BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, true);
        double resP1 =
            BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, 1.e-12, rate, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, 0., rate, COST_OF_CARRY, false);
        double resP2 =
            BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, 1.e12, rate, COST_OF_CARRY, false);
        double refP2 =
            BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, inf, rate, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2};

        for (int k = 0; k < 4; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-12);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-12);
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
        double resC1 =
            BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, true);
        double refC1 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, true);
        double resC2 =
            BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, true);
        double refC2 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, true);
        double resP1 =
            BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e-12, COST_OF_CARRY, false);
        double refP1 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, 0., COST_OF_CARRY, false);
        double resP2 =
            BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, 1.e12, COST_OF_CARRY, false);
        double refP2 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, inf, COST_OF_CARRY, false);

        double resC3 =
            BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, true);
        double refC3 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, true);
        double resP3 =
            BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, -1.e12, COST_OF_CARRY, false);
        double refP3 =
            BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, -inf, COST_OF_CARRY, false);

        double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
        double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

        for (int k = 0; k < 6; ++k) {

          if (refVec[k] > 1.e10) {
            assertTrue(resVec[k] > 1.e12);
          } else {
            if (refVec[k] < -1.e10) {
              assertTrue(resVec[k] < -1.e12);
            } else {
              if (refVec[k] == 0.) {
                assertTrue(Math.abs(resVec[k]) < 1.e-11);
              } else {
                assertEquals(refVec[k], resVec[k], Math.abs(refVec[k]) * 1.e-11);
              }
            }
          }
        }
      }
    }

    for (int i = 0; i < nStrikes; ++i) {
      for (int j = 0; j < nVols; ++j) {
        for (int l = 0; l < nInt; ++l) {
          double rate = INTEREST_RATES[l];
          double strike = STRIKES_INPUT[i];
          double vol = VOLS[j];
          double resC1 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, true);
          double refC1 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., true);
          double resC2 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, true);
          double refC2 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, true);
          double resP1 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e-12, false);
          double refP1 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 0., false);
          double resP2 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, 1.e12, false);
          double refP2 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, inf, false);

          double resC3 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, true);
          double refC3 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, true);
          double resP3 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -1.e12, false);
          double refP3 = BlackScholesFormulaRepository.carryRho(SPOT, strike, TIME_TO_EXPIRY, vol, rate, -inf, false);

          double[] resVec = new double[] {resC1, resP1, resC2, resP2, resC3, resP3};
          double[] refVec = new double[] {refC1, refP1, refC2, refP2, refC3, refP3};

          for (int k = 0; k < 6; ++k) {

            if (refVec[k] > 1.e10) {
              assertTrue(resVec[k] > 1.e10);
            } else {
              if (refVec[k] < -1.e10) {
                assertTrue(resVec[k] < -1.e10);
              } else {
                if (refVec[k] == 0.) {
                  assertTrue(Math.abs(resVec[k]) < 1.e-10);
                } else {
                  assertEquals(refVec[k], resVec[k], Math.max(Math.abs(refVec[k]) * 1.e-10, 1.e-10));
                }
              }
            }
          }
        }
      }
    }

    /*
     *
     * Since the reference value is as descent as BlackFormulaRepository, below just check NaN or wrong signature is not produced
     *
     */
    int nSpt = SPOT_EX.length;
    int nStr = STRIKES_INPUT_EX.length;
    int nTm = TIME_TO_EXPIRY_EX.length;
    int nVlt = VOLS_EX.length;
    int nRt = INTEREST_RATES_EX.length;
    int nCst = COST_OF_CARRY_EX.length;

    for (int i = 0; i < nSpt; ++i) {
      double spot = SPOT_EX[i];
      for (int j = 0; j < nStr; ++j) {
        double strike = STRIKES_INPUT_EX[j];
        for (int l = 0; l < nTm; ++l) {
          double time = TIME_TO_EXPIRY_EX[l];
          for (int m = 0; m < nVlt; ++m) {
            double vol = VOLS_EX[m];
            for (int n = 0; n < nRt; ++n) {
              double rate = INTEREST_RATES_EX[n];
              for (int ii = 0; ii < nCst; ++ii) {
                double cost = COST_OF_CARRY_EX[ii];

                double resC1 = BlackScholesFormulaRepository.carryRho(spot, strike, time, vol, rate, cost, true);
                double resP1 = BlackScholesFormulaRepository.carryRho(spot, strike, time, vol, rate, cost, false);

                assertTrue(!(Double.isNaN(resC1)));
                assertTrue(!(Double.isNaN(resP1)));
                assertTrue(resC1 >= 0.);
                assertTrue(resP1 <= 0.);
              }
            }
          }
        }
      }
    }
  }

  /*
   *
   * Error tests
   *
   *
   */

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrorstrikeForDeltaTest() {
    BlackScholesFormulaRepository.strikeForDelta(
        SPOT, 0.1, TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrorstrikeForDeltaTest() {
    BlackScholesFormulaRepository.strikeForDelta(
        -SPOT, 0.1, TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrorstrikeForDeltaTest() {
    BlackScholesFormulaRepository.strikeForDelta(
        SPOT, 0.1, -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void rangeErrorstrikeForDelta1Test() {
    BlackScholesFormulaRepository.strikeForDelta(
        SPOT, 100., TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void rangeErrorstrikeForDelta2Test() {
    BlackScholesFormulaRepository.strikeForDelta(
        SPOT, -100., TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void rangeErrorstrikeForDelta3Test() {
    BlackScholesFormulaRepository.strikeForDelta(
        SPOT, 100., TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, false);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void rangeErrorstrikeForDelta4Test() {
    BlackScholesFormulaRepository.strikeForDelta(
        SPOT, -100., TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY, false);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrorPriceTest() {
    BlackScholesFormulaRepository.price(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrorPriceTest() {
    BlackScholesFormulaRepository.price(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrorPriceTest() {
    BlackScholesFormulaRepository.price(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrorPriceTest() {
    BlackScholesFormulaRepository.price(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrorPriceTest() {
    BlackScholesFormulaRepository.price(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrorPriceTest() {
    BlackScholesFormulaRepository.price(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrordeltaTest() {
    BlackScholesFormulaRepository.delta(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrordeltaTest() {
    BlackScholesFormulaRepository.delta(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrordeltaTest() {
    BlackScholesFormulaRepository.delta(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrordeltaTest() {
    BlackScholesFormulaRepository.delta(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrordeltaTest() {
    BlackScholesFormulaRepository.delta(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrordeltaTest() {
    BlackScholesFormulaRepository.delta(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrordualDeltaTest() {
    BlackScholesFormulaRepository.dualDelta(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrordualDeltaTest() {
    BlackScholesFormulaRepository.dualDelta(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrordualDeltaTest() {
    BlackScholesFormulaRepository.dualDelta(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrordualDeltaTest() {
    BlackScholesFormulaRepository.dualDelta(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrordualDeltaTest() {
    BlackScholesFormulaRepository.dualDelta(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrordualDeltaTest() {
    BlackScholesFormulaRepository.dualDelta(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrorgammaTest() {
    BlackScholesFormulaRepository.gamma(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrorgammaTest() {
    BlackScholesFormulaRepository.gamma(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrorgammaTest() {
    BlackScholesFormulaRepository.gamma(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrorgammaTest() {
    BlackScholesFormulaRepository.gamma(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrorgammaTest() {
    BlackScholesFormulaRepository.gamma(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrorgammaTest() {
    BlackScholesFormulaRepository.gamma(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrordualGammaTest() {
    BlackScholesFormulaRepository.dualGamma(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrordualGammaTest() {
    BlackScholesFormulaRepository.dualGamma(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrordualGammaTest() {
    BlackScholesFormulaRepository.dualGamma(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrordualGammaTest() {
    BlackScholesFormulaRepository.dualGamma(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrordualGammaTest() {
    BlackScholesFormulaRepository.dualGamma(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrordualGammaTest() {
    BlackScholesFormulaRepository.dualGamma(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrorcrossGammaTest() {
    BlackScholesFormulaRepository.crossGamma(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrorcrossGammaTest() {
    BlackScholesFormulaRepository.crossGamma(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrorcrossGammaTest() {
    BlackScholesFormulaRepository.crossGamma(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrorcrossGammaTest() {
    BlackScholesFormulaRepository.crossGamma(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrorcrossGammaTest() {
    BlackScholesFormulaRepository.crossGamma(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrorcrossGammaTest() {
    BlackScholesFormulaRepository.crossGamma(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrorthetaTest() {
    BlackScholesFormulaRepository.theta(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrorthetaTest() {
    BlackScholesFormulaRepository.theta(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrorthetaTest() {
    BlackScholesFormulaRepository.theta(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrorthetaTest() {
    BlackScholesFormulaRepository.theta(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrorthetaTest() {
    BlackScholesFormulaRepository.theta(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrorthetaTest() {
    BlackScholesFormulaRepository.theta(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrorcharmTest() {
    BlackScholesFormulaRepository.charm(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrorcharmTest() {
    BlackScholesFormulaRepository.charm(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrorcharmTest() {
    BlackScholesFormulaRepository.charm(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrorcharmTest() {
    BlackScholesFormulaRepository.charm(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrorcharmTest() {
    BlackScholesFormulaRepository.charm(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrorcharmTest() {
    BlackScholesFormulaRepository.charm(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrordualCharmTest() {
    BlackScholesFormulaRepository.dualCharm(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrordualCharmTest() {
    BlackScholesFormulaRepository.dualCharm(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrordualCharmTest() {
    BlackScholesFormulaRepository.dualCharm(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrordualCharmTest() {
    BlackScholesFormulaRepository.dualCharm(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrordualCharmTest() {
    BlackScholesFormulaRepository.dualCharm(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrordualCharmTest() {
    BlackScholesFormulaRepository.dualCharm(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrorvegaTest() {
    BlackScholesFormulaRepository.vega(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrorvegaTest() {
    BlackScholesFormulaRepository.vega(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrorvegaTest() {
    BlackScholesFormulaRepository.vega(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrorvegaTest() {
    BlackScholesFormulaRepository.vega(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrorvegaTest() {
    BlackScholesFormulaRepository.vega(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrorvegaTest() {
    BlackScholesFormulaRepository.vega(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrorvannaTest() {
    BlackScholesFormulaRepository.vanna(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrorvannaTest() {
    BlackScholesFormulaRepository.vanna(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrorvannaTest() {
    BlackScholesFormulaRepository.vanna(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrorvannaTest() {
    BlackScholesFormulaRepository.vanna(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrorvannaTest() {
    BlackScholesFormulaRepository.vanna(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrorvannaTest() {
    BlackScholesFormulaRepository.vanna(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrordualVannaTest() {
    BlackScholesFormulaRepository.dualVanna(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrordualVannaTest() {
    BlackScholesFormulaRepository.dualVanna(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrordualVannaTest() {
    BlackScholesFormulaRepository.dualVanna(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrordualVannaTest() {
    BlackScholesFormulaRepository.dualVanna(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrordualVannaTest() {
    BlackScholesFormulaRepository.dualVanna(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrordualVannaTest() {
    BlackScholesFormulaRepository.dualVanna(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrorvommaTest() {
    BlackScholesFormulaRepository.vomma(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrorvommaTest() {
    BlackScholesFormulaRepository.vomma(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrorvommaTest() {
    BlackScholesFormulaRepository.vomma(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrorvommaTest() {
    BlackScholesFormulaRepository.vomma(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrorvommaTest() {
    BlackScholesFormulaRepository.vomma(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrorvommaTest() {
    BlackScholesFormulaRepository.vomma(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrorvegaBleedTest() {
    BlackScholesFormulaRepository.vegaBleed(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrorvegaBleedTest() {
    BlackScholesFormulaRepository.vegaBleed(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrorvegaBleedTest() {
    BlackScholesFormulaRepository.vegaBleed(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrorvegaBleedTest() {
    BlackScholesFormulaRepository.vegaBleed(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrorvegaBleedTest() {
    BlackScholesFormulaRepository.vegaBleed(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrorvegaBleedTest() {
    BlackScholesFormulaRepository.vegaBleed(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrorrhoTest() {
    BlackScholesFormulaRepository.rho(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrorrhoTest() {
    BlackScholesFormulaRepository.rho(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrorrhoTest() {
    BlackScholesFormulaRepository.rho(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrorrhoTest() {
    BlackScholesFormulaRepository.rho(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrorrhoTest() {
    BlackScholesFormulaRepository.rho(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrorrhoTest() {
    BlackScholesFormulaRepository.rho(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeVolErrorcarryRhoTest() {
    BlackScholesFormulaRepository.carryRho(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, -0.5, INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeSpotErrorcarryRhoTest() {
    BlackScholesFormulaRepository.carryRho(
        -SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeStrikeErrorcarryRhoTest() {
    BlackScholesFormulaRepository.carryRho(
        SPOT, -STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeTimeErrorcarryRhoTest() {
    BlackScholesFormulaRepository.carryRho(
        SPOT, STRIKES_INPUT[1], -TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanIntErrorcarryRhoTest() {
    BlackScholesFormulaRepository.carryRho(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], NAN, COST_OF_CARRY, true);
  }

  /**
   *
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanCarryErrorcarryRhoTest() {
    BlackScholesFormulaRepository.carryRho(
        SPOT, STRIKES_INPUT[1], TIME_TO_EXPIRY, VOLS[1], INTEREST_RATES[1], NAN, true);
  }

}
