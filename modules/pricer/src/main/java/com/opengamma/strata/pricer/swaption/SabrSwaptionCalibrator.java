/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.interpolator.SurfaceInterpolator;
import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.rootfinding.NewtonRaphsonSingleRootFinder;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResultsWithTransform;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.volatility.smile.SabrFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.SabrModelFitter;
import com.opengamma.strata.pricer.model.SabrInterestRateParameters;
import com.opengamma.strata.pricer.model.SabrVolatilityFormula;
import com.opengamma.strata.pricer.option.RawOptionData;
import com.opengamma.strata.pricer.option.TenorRawOptionData;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Swaption SABR calibrator.
 * <p>
 * This calibrator takes raw data and produces calibrated SABR parameters.
 */
public final class SabrSwaptionCalibrator {

  /**
   * The SABR implied volatility formula.
   */
  private final SabrVolatilityFormula sabrVolatilityFormula;
  /**
   * The swap pricer.
   * Required for forward rate computation.
   */
  private final DiscountingSwapProductPricer swapPricer;
  /**
   * The reference data.
   */
  private final ReferenceData refData;

  /** The root-finder used in the Alpha calibration to ATM volatility. */
  private static final NewtonRaphsonSingleRootFinder ROOT_FINDER = new NewtonRaphsonSingleRootFinder();

  /**
   * The default instance of the class.
   */
  public static final SabrSwaptionCalibrator DEFAULT =
      new SabrSwaptionCalibrator(
          SabrVolatilityFormula.hagan(), DiscountingSwapProductPricer.DEFAULT, ReferenceData.standard());

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a SABR volatility function provider and a swap pricer.
   * <p>
   * The swap pricer is used to compute the forward rate required for calibration.
   * 
   * @param sabrVolatilityFormula  the SABR implied volatility formula
   * @param swapPricer  the swap pricer
   * @return the calibrator
   */
  public static SabrSwaptionCalibrator of(
      SabrVolatilityFormula sabrVolatilityFormula,
      DiscountingSwapProductPricer swapPricer) {

    return new SabrSwaptionCalibrator(sabrVolatilityFormula, swapPricer, ReferenceData.standard());
  }

  /**
   * Obtains an instance from a SABR volatility function provider and a swap pricer.
   * <p>
   * The swap pricer is used to compute the forward rate required for calibration.
   * 
   * @param sabrVolatilityFormula  the SABR implied volatility formula
   * @param swapPricer  the swap pricer
   * @param refData  the reference data
   * @return the calibrator
   */
  public static SabrSwaptionCalibrator of(
      SabrVolatilityFormula sabrVolatilityFormula,
      DiscountingSwapProductPricer swapPricer,
      ReferenceData refData) {

    return new SabrSwaptionCalibrator(sabrVolatilityFormula, swapPricer, refData);
  }

  private SabrSwaptionCalibrator(
      SabrVolatilityFormula sabrVolatilityFormula,
      DiscountingSwapProductPricer swapPricer,
      ReferenceData refData) {

    this.sabrVolatilityFormula = ArgChecker.notNull(sabrVolatilityFormula, "sabrVolatilityFormula");
    this.swapPricer = ArgChecker.notNull(swapPricer, "swapPricer");
    this.refData = ArgChecker.notNull(refData, "refData");
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrate SABR parameters to a set of raw swaption data.
   * <p>
   * The SABR parameters are calibrated with fixed beta and fixed shift surfaces.
   * The raw data can be (shifted) log-normal volatilities, normal volatilities or option prices
   * 
   * @param definition  the definition of the calibration to be performed
   * @param calibrationDateTime  the data and time of the calibration
   * @param data  the map of raw option data, keyed by tenor
   * @param ratesProvider  the rate provider used to compute the swap forward rates
   * @param betaSurface  the beta surface
   * @param shiftSurface  the shift surface
   * @return the SABR volatility object
   */
  @SuppressWarnings("null")
  public SabrParametersSwaptionVolatilities calibrateWithFixedBetaAndShift(
      SabrSwaptionDefinition definition,
      ZonedDateTime calibrationDateTime,
      TenorRawOptionData data,
      RatesProvider ratesProvider,
      Surface betaSurface,
      Surface shiftSurface) {

    // If a MathException is thrown by a calibration for a specific expiry/tenor, an exception is thrown by the method
    return calibrateWithFixedBetaAndShift(
        definition,
        calibrationDateTime,
        data,
        ratesProvider,
        betaSurface,
        shiftSurface,
        true);
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrate SABR parameters to a set of raw swaption data.
   * <p>
   * The SABR parameters are calibrated with fixed beta and fixed shift surfaces.
   * The raw data can be (shifted) log-normal volatilities, normal volatilities or option prices
   * <p>
   * This method offers the flexibility to skip the data sets that throw a MathException (stopOnMathException = false).
   * The option to skip those data sets should be use with care, as part of the input data may be unused in the output.
   * 
   * @param definition  the definition of the calibration to be performed
   * @param calibrationDateTime  the data and time of the calibration
   * @param data  the map of raw option data, keyed by tenor
   * @param ratesProvider  the rate provider used to compute the swap forward rates
   * @param betaSurface  the beta surface
   * @param shiftSurface  the shift surface
   * @param stopOnMathException  flag indicating if the calibration should stop on math exceptions or skip the 
   *   expiries/tenors which throw MathException
   * @return the SABR volatility object
   */
  @SuppressWarnings("null")
  public SabrParametersSwaptionVolatilities calibrateWithFixedBetaAndShift(
      SabrSwaptionDefinition definition,
      ZonedDateTime calibrationDateTime,
      TenorRawOptionData data,
      RatesProvider ratesProvider,
      Surface betaSurface,
      Surface shiftSurface,
      boolean stopOnMathException) {

    SwaptionVolatilitiesName name = definition.getName();
    FixedIborSwapConvention convention = definition.getConvention();
    DayCount dayCount = definition.getDayCount();
    SurfaceInterpolator interpolator = definition.getInterpolator();

    BitSet fixed = new BitSet();
    fixed.set(1); // Beta fixed
    BusinessDayAdjustment bda = convention.getFloatingLeg().getStartDateBusinessDayAdjustment();
    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    // Sorted maps to obtain the surfaces nodes in standard order
    TreeMap<Double, TreeMap<Double, ParameterMetadata>> parameterMetadataTmp = new TreeMap<>();
    TreeMap<Double, TreeMap<Double, DoubleArray>> dataSensitivityAlphaTmp = new TreeMap<>(); // Sensitivity to the calibrating data
    TreeMap<Double, TreeMap<Double, DoubleArray>> dataSensitivityRhoTmp = new TreeMap<>();
    TreeMap<Double, TreeMap<Double, DoubleArray>> dataSensitivityNuTmp = new TreeMap<>();
    TreeMap<Double, TreeMap<Double, SabrFormulaData>> sabrPointTmp = new TreeMap<>();
    for (Tenor tenor : data.getTenors()) {
      RawOptionData tenorData = data.getData(tenor);
      double timeTenor = tenor.getPeriod().getYears() + tenor.getPeriod().getMonths() / 12;
      List<Period> expiries = tenorData.getExpiries();
      int nbExpiries = expiries.size();
      for (int loopexpiry = 0; loopexpiry < nbExpiries; loopexpiry++) {
        Pair<DoubleArray, DoubleArray> availableSmile = tenorData.availableSmileAtExpiry(expiries.get(loopexpiry));
        if (availableSmile.getFirst().size() == 0) { // If not data is available, no calibration possible
          continue;
        }
        LocalDate exerciseDate = expirationDate(bda, calibrationDate, expiries.get(loopexpiry));
        LocalDate effectiveDate = convention.calculateSpotDateFromTradeDate(exerciseDate, refData);
        double timeToExpiry = dayCount.relativeYearFraction(calibrationDate, exerciseDate);
        double beta = betaSurface.zValue(timeToExpiry, timeTenor);
        double shift = shiftSurface.zValue(timeToExpiry, timeTenor);
        LocalDate endDate = effectiveDate.plus(tenor);
        SwapTrade swap0 = convention.toTrade(calibrationDate, effectiveDate, endDate, BuySell.BUY, 1.0, 0.0);
        double forward = swapPricer.parRate(swap0.getProduct().resolve(refData), ratesProvider);
        SabrFormulaData sabrPoint = null;
        DoubleMatrix inverseJacobian = null;
        boolean error = false;
        try {
          Pair<SabrFormulaData, DoubleMatrix> calibrationResult =
              calibration(forward, shift, beta, fixed, bda, calibrationDateTime, dayCount,
                  availableSmile.getFirst(), availableSmile.getSecond(), expiries.get(loopexpiry), tenorData);
          sabrPoint = calibrationResult.getFirst();
          inverseJacobian = calibrationResult.getSecond();
        } catch (MathException e) {
          error = true;
          if (stopOnMathException) {
            String message = Messages.format("{} at expiry {} and tenor {}", e.getMessage(),
                expiries.get(loopexpiry), tenor);
            throw new MathException(message, e);
          }
        }
        if (!error) {
          if (!parameterMetadataTmp.containsKey(timeToExpiry)) {
            parameterMetadataTmp.put(timeToExpiry, new TreeMap<>());
            dataSensitivityAlphaTmp.put(timeToExpiry, new TreeMap<>());
            dataSensitivityRhoTmp.put(timeToExpiry, new TreeMap<>());
            dataSensitivityNuTmp.put(timeToExpiry, new TreeMap<>());
            sabrPointTmp.put(timeToExpiry, new TreeMap<>());
          }
          TreeMap<Double, ParameterMetadata> parameterMetadataExpiryMap = parameterMetadataTmp.get(timeToExpiry);
          TreeMap<Double, DoubleArray> dataSensitivityAlphaExpiryMap = dataSensitivityAlphaTmp.get(timeToExpiry);
          TreeMap<Double, DoubleArray> dataSensitivityRhoExpiryMap = dataSensitivityRhoTmp.get(timeToExpiry);
          TreeMap<Double, DoubleArray> dataSensitivityNuExpiryMap = dataSensitivityNuTmp.get(timeToExpiry);
          TreeMap<Double, SabrFormulaData> sabrPointExpiryMap = sabrPointTmp.get(timeToExpiry);
          parameterMetadataExpiryMap.put(timeTenor, SwaptionSurfaceExpiryTenorParameterMetadata.of(
              timeToExpiry,
              timeTenor,
              expiries.get(loopexpiry).toString() + "x" + tenor.toString()));
          dataSensitivityAlphaExpiryMap.put(timeTenor, inverseJacobian.row(0));
          dataSensitivityRhoExpiryMap.put(timeTenor, inverseJacobian.row(2));
          dataSensitivityNuExpiryMap.put(timeTenor, inverseJacobian.row(3));
          sabrPointExpiryMap.put(timeTenor, sabrPoint);
        }
      }
    }
    DoubleArray timeToExpiryArray = DoubleArray.EMPTY;
    DoubleArray timeTenorArray = DoubleArray.EMPTY;
    DoubleArray alphaArray = DoubleArray.EMPTY;
    DoubleArray rhoArray = DoubleArray.EMPTY;
    DoubleArray nuArray = DoubleArray.EMPTY;
    List<ParameterMetadata> parameterMetadata = new ArrayList<>();
    List<DoubleArray> dataSensitivityAlpha = new ArrayList<>(); // Sensitivity to the calibrating data
    List<DoubleArray> dataSensitivityRho = new ArrayList<>();
    List<DoubleArray> dataSensitivityNu = new ArrayList<>();
    for (Double timeToExpiry : parameterMetadataTmp.keySet()) {
      TreeMap<Double, ParameterMetadata> parameterMetadataExpiryMap = parameterMetadataTmp.get(timeToExpiry);
      TreeMap<Double, DoubleArray> dataSensitivityAlphaExpiryMap = dataSensitivityAlphaTmp.get(timeToExpiry);
      TreeMap<Double, DoubleArray> dataSensitivityRhoExpiryMap = dataSensitivityRhoTmp.get(timeToExpiry);
      TreeMap<Double, DoubleArray> dataSensitivityNuExpiryMap = dataSensitivityNuTmp.get(timeToExpiry);
      TreeMap<Double, SabrFormulaData> sabrPointExpiryMap = sabrPointTmp.get(timeToExpiry);
      for (Double timeTenor : parameterMetadataExpiryMap.keySet()) {
        parameterMetadata.add(parameterMetadataExpiryMap.get(timeTenor));
        dataSensitivityAlpha.add(dataSensitivityAlphaExpiryMap.get(timeTenor));
        dataSensitivityRho.add(dataSensitivityRhoExpiryMap.get(timeTenor));
        dataSensitivityNu.add(dataSensitivityNuExpiryMap.get(timeTenor));
        timeToExpiryArray = timeToExpiryArray.concat(timeToExpiry);
        timeTenorArray = timeTenorArray.concat(timeTenor);
        SabrFormulaData sabrPt = sabrPointExpiryMap.get(timeTenor);
        alphaArray = alphaArray.concat(sabrPt.getAlpha());
        rhoArray = rhoArray.concat(sabrPt.getRho());
        nuArray = nuArray.concat(sabrPt.getNu());
      }
    }
    SurfaceMetadata metadataAlpha = Surfaces.sabrParameterByExpiryTenor(
        name.getName() + "-Alpha", dayCount, ValueType.SABR_ALPHA)
        .withParameterMetadata(parameterMetadata);
    SurfaceMetadata metadataRho = Surfaces.sabrParameterByExpiryTenor(
        name.getName() + "-Rho", dayCount, ValueType.SABR_RHO)
        .withParameterMetadata(parameterMetadata);
    SurfaceMetadata metadataNu = Surfaces.sabrParameterByExpiryTenor(
        name.getName() + "-Nu", dayCount, ValueType.SABR_NU)
        .withParameterMetadata(parameterMetadata);
    InterpolatedNodalSurface alphaSurface = InterpolatedNodalSurface
        .of(metadataAlpha, timeToExpiryArray, timeTenorArray, alphaArray, interpolator);
    InterpolatedNodalSurface rhoSurface = InterpolatedNodalSurface
        .of(metadataRho, timeToExpiryArray, timeTenorArray, rhoArray, interpolator);
    InterpolatedNodalSurface nuSurface = InterpolatedNodalSurface
        .of(metadataNu, timeToExpiryArray, timeTenorArray, nuArray, interpolator);
    SabrInterestRateParameters params = SabrInterestRateParameters.of(
        alphaSurface, betaSurface, rhoSurface, nuSurface, shiftSurface, sabrVolatilityFormula);
    return SabrParametersSwaptionVolatilities.builder()
        .name(name)
        .convention(convention)
        .valuationDateTime(calibrationDateTime)
        .parameters(params)
        .dataSensitivityAlpha(dataSensitivityAlpha)
        .dataSensitivityRho(dataSensitivityRho)
        .dataSensitivityNu(dataSensitivityNu).build();
  }

  // The main part of the calibration. The calibration is done 4 times with different starting points: low and high
  // volatilities and high and low vol of vol. The best result (in term of chi^2) is returned.
  private Pair<SabrFormulaData, DoubleMatrix> calibration(
      double forward,
      double shift,
      double beta,
      BitSet fixed,
      BusinessDayAdjustment bda,
      ZonedDateTime calibrationDateTime,
      DayCount dayCount,
      DoubleArray strike,
      DoubleArray data,
      Period expiry,
      RawOptionData rawData) {

    double rhoStart = -0.50 * beta + 0.50 * (1 - beta);
    // Correlation is usually positive for normal and negative for log-normal;.
    double[] alphaStart = new double[4];
    alphaStart[0] = 0.0025 / Math.pow(forward + shift, beta); // Low vol
    alphaStart[1] = alphaStart[0];
    alphaStart[2] = 4 * alphaStart[0]; // High vol
    alphaStart[3] = alphaStart[2];
    double[] nuStart = new double[4];
    nuStart[0] = 0.10; // Low vol of vol
    nuStart[1] = 0.50; // High vol of vol
    nuStart[2] = 0.10;
    nuStart[3] = 0.50;
    double chi2 = 1.0E+12; // Large number 
    Pair<LeastSquareResultsWithTransform, DoubleArray> sabrCalibrationResult = null;
    for (int i = 0; i < 4; i++) { // Try different starting points and take the best
      DoubleArray startParameters = DoubleArray.of(alphaStart[i], beta, rhoStart, nuStart[i]);
      Pair<LeastSquareResultsWithTransform, DoubleArray> r = null;
      if (rawData.getDataType().equals(ValueType.NORMAL_VOLATILITY)) {
        r = calibrateLsShiftedFromNormalVolatilities(bda, calibrationDateTime, dayCount,
            expiry, forward, strike, rawData.getStrikeType(),
            data, startParameters, fixed, shift);
      } else {
        if (rawData.getDataType().equals(ValueType.PRICE)) {
          r = calibrateLsShiftedFromPrices(bda, calibrationDateTime, dayCount,
              expiry, forward, strike, rawData.getStrikeType(),
              data, startParameters, fixed, shift);
        } else {
          if (rawData.getDataType().equals(ValueType.BLACK_VOLATILITY)) {
            r = calibrateLsShiftedFromBlackVolatilities(bda, calibrationDateTime, dayCount,
                expiry, forward, strike, rawData.getStrikeType(),
                data, rawData.getShift().orElse(0d), startParameters, fixed, shift);
          } else {
            throw new IllegalArgumentException("Data type not supported");
          }
        }
      }
      if (r.getFirst().getChiSq() < chi2) { // Keep best calibration
        sabrCalibrationResult = r;
        chi2 = r.getFirst().getChiSq();
      }
    }
    @SuppressWarnings("null")
    SabrFormulaData sabrParameters =
        SabrFormulaData.of(sabrCalibrationResult.getFirst().getModelParameters().toArrayUnsafe());
    DoubleMatrix parameterSensitivityToBlackShifted =
        sabrCalibrationResult.getFirst().getModelParameterSensitivityToData();
    DoubleArray blackVolSensitivitytoRawData = sabrCalibrationResult.getSecond();
    // Multiply the sensitivity to the intermediary (shifted) log-normal vol by its sensitivity to the raw data
    double[][] parameterSensitivityToDataArray = new double[4][blackVolSensitivitytoRawData.size()];
    for (int loopsabr = 0; loopsabr < 4; loopsabr++) {
      for (int loopdata = 0; loopdata < blackVolSensitivitytoRawData.size(); loopdata++) {
        parameterSensitivityToDataArray[loopsabr][loopdata] =
            parameterSensitivityToBlackShifted.get(loopsabr, loopdata) * blackVolSensitivitytoRawData.get(loopdata);
      }
    }
    DoubleMatrix parameterSensitivityToData = DoubleMatrix.ofUnsafe(parameterSensitivityToDataArray);
    return Pair.of(sabrParameters, parameterSensitivityToData);
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrate SABR alpha parameters to a set of ATM swaption volatilities.
   * <p>
   * The SABR parameters are calibrated with all the parameters other than alpha (beta, rhu, nu, shift) fixed.
   * The at-the-money volatilities can be log-normal or normal volatilities.
   * 
   * @param name  the name
   * @param sabr  the SABR parameters from which the beta, rho, nu and shift are extracted
   * @param ratesProvider  the rate provider used to compute the swap forward rates
   * @param atmVolatilities  the swaption volatilities containing the ATM volatilities to be calibrated
   * @param tenors  the tenors for which the alpha parameter should be calibrated
   * @param expiries  the expiries for which the alpha parameter should be calibrated
   * @param interpolator  the interpolator for the alpha surface
   * @return the SABR volatility object
   */
  public SabrParametersSwaptionVolatilities calibrateAlphaWithAtm(
      SwaptionVolatilitiesName name,
      SabrParametersSwaptionVolatilities sabr,
      RatesProvider ratesProvider,
      SwaptionVolatilities atmVolatilities,
      List<Tenor> tenors,
      List<Period> expiries,
      SurfaceInterpolator interpolator) {

    int nbTenors = tenors.size();
    FixedIborSwapConvention convention = sabr.getConvention();
    DayCount dayCount = sabr.getDayCount();
    BusinessDayAdjustment bda = convention.getFloatingLeg().getStartDateBusinessDayAdjustment();
    LocalDate calibrationDate = sabr.getValuationDate();
    DoubleArray timeToExpiryArray = DoubleArray.EMPTY;
    DoubleArray timeTenorArray = DoubleArray.EMPTY;
    DoubleArray alphaArray = DoubleArray.EMPTY;
    List<ParameterMetadata> parameterMetadata = new ArrayList<>();
    List<DoubleArray> dataSensitivityAlpha = new ArrayList<>(); // Sensitivity to the calibrating data
    int nbExpiries = expiries.size();
    for (int loopexpiry = 0; loopexpiry < nbExpiries; loopexpiry++) {
      for (int looptenor = 0; looptenor < nbTenors; looptenor++) {
        double timeTenor = tenors.get(looptenor).getPeriod().getYears() + tenors.get(looptenor).getPeriod().getMonths() / 12;
        LocalDate exerciseDate = expirationDate(bda, calibrationDate, expiries.get(loopexpiry));
        LocalDate effectiveDate = convention.calculateSpotDateFromTradeDate(exerciseDate, refData);
        double timeToExpiry = dayCount.relativeYearFraction(calibrationDate, exerciseDate);
        LocalDate endDate = effectiveDate.plus(tenors.get(looptenor));
        SwapTrade swap0 = convention.toTrade(calibrationDate, effectiveDate, endDate, BuySell.BUY, 1.0, 0.0);
        double forward = swapPricer.parRate(swap0.getProduct().resolve(refData), ratesProvider);
        double atmVolatility = atmVolatilities.volatility(timeToExpiry, timeTenor, forward, forward);
        ValueType volatilityType = atmVolatilities.getVolatilityType();
        // Currently there is no 'SwaptionVolatilities' with Black shifted.
        double beta = sabr.getParameters().beta(timeToExpiry, timeTenor);
        double rho = sabr.getParameters().rho(timeToExpiry, timeTenor);
        double nu = sabr.getParameters().nu(timeToExpiry, timeTenor);
        double shift = sabr.getParameters().shift(timeToExpiry, timeTenor);
        Pair<Double, Double> calibrationResult = calibrationAtm(forward, shift, beta, rho, nu, bda,
            sabr.getValuationDateTime(), dayCount, expiries.get(loopexpiry), atmVolatility, volatilityType);
        timeToExpiryArray = timeToExpiryArray.concat(timeToExpiry);
        timeTenorArray = timeTenorArray.concat(timeTenor);
        alphaArray = alphaArray.concat(calibrationResult.getFirst());
        parameterMetadata.add(
            SwaptionSurfaceExpiryTenorParameterMetadata.of(
                timeToExpiry,
                timeTenor,
                expiries.get(loopexpiry).toString() + "x" + tenors.get(looptenor).toString()));
        dataSensitivityAlpha.add(DoubleArray.of(calibrationResult.getSecond()));
      }
    }
    SurfaceMetadata metadataAlpha = Surfaces.sabrParameterByExpiryTenor(
        name.getName() + "-Alpha", dayCount, ValueType.SABR_ALPHA)
        .withParameterMetadata(parameterMetadata);
    InterpolatedNodalSurface alphaSurface = InterpolatedNodalSurface
        .of(metadataAlpha, timeToExpiryArray, timeTenorArray, alphaArray, interpolator);
    SabrInterestRateParameters params = SabrInterestRateParameters.of(
        alphaSurface, sabr.getParameters().getBetaSurface(), sabr.getParameters().getRhoSurface(),
        sabr.getParameters().getNuSurface(), sabr.getParameters().getShiftSurface(), sabrVolatilityFormula);
    return SabrParametersSwaptionVolatilities.builder()
        .name(name)
        .convention(convention)
        .valuationDateTime(sabr.getValuationDateTime())
        .parameters(params)
        .dataSensitivityAlpha(dataSensitivityAlpha).build();
  }

  // Calibration for one option. Distribute the calculation according to the type of volatility (Black/Normal)
  private Pair<Double, Double> calibrationAtm(
      double forward,
      double shift,
      double beta,
      double rho,
      double nu,
      BusinessDayAdjustment bda,
      ZonedDateTime calibrationDateTime,
      DayCount dayCount,
      Period expiry,
      double volatility,
      ValueType volatilityType) {
    double alphaStart = volatility / Math.pow(forward + shift, beta);
    DoubleArray startParameters = DoubleArray.of(alphaStart, beta, rho, nu);
    Pair<Double, Double> r = null;
    if (volatilityType.equals(ValueType.NORMAL_VOLATILITY)) {
      r = calibrateAtmShiftedFromNormalVolatilities(
          bda, calibrationDateTime, dayCount, expiry, forward, volatility, startParameters, shift);
    } else {
      if (volatilityType.equals(ValueType.BLACK_VOLATILITY)) {
        r = calibrateAtmShiftedFromBlackVolatilities(
            bda, calibrationDateTime, dayCount, expiry, forward, volatility, 0.0, startParameters, shift);
      } else {
        throw new IllegalArgumentException("Data type not supported");
      }
    }
    return r;
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrate the SABR parameters to a set of Black volatilities at given moneyness by least square.
   * <p>
   * All the associated swaptions have the same expiration date, given by a period
   * from calibration time, and the same tenor.
   * 
   * @param bda  the business day adjustment for the exercise date adjustment
   * @param calibrationDateTime  the calibration date and time
   * @param dayCount  the day count for the computation of the time to exercise
   * @param periodToExpiry  the period to expiry
   * @param forward  the forward price/rate
   * @param strikesLike  the options strike-like dimension
   * @param strikeType  the strike type
   * @param blackVolatilitiesInput  the option (call/payer) implied volatilities in shifted Black model
   * @param shiftInput  the shift used to computed the input implied shifted Black volatilities
   * @param startParameters  the starting parameters for the calibration. If one or more of the parameters are fixed,
   * the starting value will be used as the fixed parameter.
   * @param fixedParameters  the flag for the fixed parameters that are not calibrated
   * @param shiftOutput  the shift to calibrate the shifted SABR
   * @return the least square results and the derivative of the shifted log-normal used for calibration with respect 
   * to the raw data
   */
  public Pair<LeastSquareResultsWithTransform, DoubleArray> calibrateLsShiftedFromBlackVolatilities(
      BusinessDayAdjustment bda,
      ZonedDateTime calibrationDateTime,
      DayCount dayCount,
      Period periodToExpiry,
      double forward,
      DoubleArray strikesLike,
      ValueType strikeType,
      DoubleArray blackVolatilitiesInput,
      double shiftInput,
      DoubleArray startParameters,
      BitSet fixedParameters,
      double shiftOutput) {

    int nbStrikes = strikesLike.size();
    ArgChecker.isTrue(nbStrikes == blackVolatilitiesInput.size(), "size of strikes must be the same as size of volatilities");
    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    LocalDate exerciseDate = expirationDate(bda, calibrationDate, periodToExpiry);
    double timeToExpiry = dayCount.relativeYearFraction(calibrationDate, exerciseDate);
    DoubleArray errors = DoubleArray.filled(nbStrikes, 1e-4);
    DoubleArray strikes = strikesShifted(forward, 0.0, strikesLike, strikeType);
    Pair<DoubleArray, DoubleArray> volAndDerivatives = blackVolatilitiesShiftedFromBlackVolatilitiesShifted(
        forward, shiftOutput, timeToExpiry, strikes, blackVolatilitiesInput, shiftInput);
    DoubleArray blackVolatilitiesTransformed = volAndDerivatives.getFirst();
    DoubleArray strikesShifted = strikesShifted(forward, shiftOutput, strikesLike, strikeType);
    SabrModelFitter fitter = new SabrModelFitter(
        forward + shiftOutput,
        strikesShifted,
        timeToExpiry,
        blackVolatilitiesTransformed,
        errors,
        sabrVolatilityFormula);
    LeastSquareResultsWithTransform result = fitter.solve(startParameters, fixedParameters);
    return Pair.of(result, volAndDerivatives.getSecond());
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrate the SABR alpha parameter to an ATM Black volatility and compute the derivative of the result with 
   * respect to the input volatility.
   * 
   * @param bda  the business day adjustment for the exercise date adjustment
   * @param calibrationDateTime  the calibration date and time
   * @param dayCount  the day count for the computation of the time to exercise
   * @param periodToExpiry  the period to expiry
   * @param forward  the forward price/rate
   * @param blackVolatility  the option (call/payer) Black implied volatility
   * @param shiftInput  the shift used to computed the input implied shifted Black volatilities
   * @param startParameters  the starting parameters for the calibration. The alpha parameter is used as a starting
   * point for the root-finding, the other parameters are fixed.
   * @param shiftOutput  the shift to calibrate the shifted SABR
   * @return the alpha calibrated and its derivative with respect to the volatility
   */
  public Pair<Double, Double> calibrateAtmShiftedFromBlackVolatilities(
      BusinessDayAdjustment bda,
      ZonedDateTime calibrationDateTime,
      DayCount dayCount,
      Period periodToExpiry,
      double forward,
      double blackVolatility,
      double shiftInput,
      DoubleArray startParameters,
      double shiftOutput) {

    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    LocalDate exerciseDate = expirationDate(bda, calibrationDate, periodToExpiry);
    double timeToExpiry = dayCount.relativeYearFraction(calibrationDate, exerciseDate);
    Pair<DoubleArray, DoubleArray> volAndDerivatives = blackVolatilitiesShiftedFromBlackVolatilitiesShifted(
        forward, shiftOutput, timeToExpiry, DoubleArray.of(forward), DoubleArray.of(blackVolatility), shiftInput);
    DoubleArray blackVolatilitiesTransformed = volAndDerivatives.getFirst();
    Function<Double, Double> volFunction =
        (a) -> sabrVolatilityFormula.volatility(forward + shiftOutput, forward + shiftOutput, timeToExpiry, a,
            startParameters.get(1), startParameters.get(2), startParameters.get(3)) - blackVolatilitiesTransformed.get(0);
    double alphaCalibrated = ROOT_FINDER.getRoot(volFunction, startParameters.get(0));
    double dAlphadBlack = 1.0d / sabrVolatilityFormula.volatilityAdjoint(forward + shiftOutput, forward + shiftOutput,
        timeToExpiry, alphaCalibrated, startParameters.get(1), startParameters.get(2), startParameters.get(3))
        .getDerivative(2);
    return Pair.of(alphaCalibrated, dAlphadBlack * volAndDerivatives.getSecond().get(0));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an array of shifted Black volatilities from shifted Black volatilities with a different shift and 
   * the sensitivities of the Black volatilities outputs with respect to the normal volatilities inputs.
   * 
   * @param forward  the forward rate
   * @param shiftOutput  the shift required in the output
   * @param timeToExpiry  the time to expiration
   * @param strikes  the option strikes
   * @param blackVolatilities  the shifted implied Black volatilities
   * @param shiftInput  the shift used in the input Black implied volatilities
   * @return the shifted black volatilities and their derivatives
   */
  public Pair<DoubleArray, DoubleArray> blackVolatilitiesShiftedFromBlackVolatilitiesShifted(
      double forward,
      double shiftOutput,
      double timeToExpiry,
      DoubleArray strikes,
      DoubleArray blackVolatilities,
      double shiftInput) {

    if (shiftInput == shiftOutput) {
      return Pair.of(blackVolatilities, DoubleArray.filled(blackVolatilities.size(), 1.0d));
      // No change required if shifts are the same
    }
    int nbStrikes = strikes.size();
    double[] impliedVolatility = new double[nbStrikes];
    double[] impliedVolatilityDerivatives = new double[nbStrikes];
    for (int i = 0; i < nbStrikes; i++) {
      ValueDerivatives price = BlackFormulaRepository.priceAdjoint(
          forward + shiftInput, strikes.get(i) + shiftInput, timeToExpiry, blackVolatilities.get(i), true); // vega-[3]
      ValueDerivatives iv = BlackFormulaRepository.impliedVolatilityAdjoint(
          price.getValue(), forward + shiftOutput, strikes.get(i) + shiftOutput, timeToExpiry, true);
      impliedVolatility[i] = iv.getValue();
      impliedVolatilityDerivatives[i] = iv.getDerivative(0) * price.getDerivative(3);
    }
    return Pair.of(DoubleArray.ofUnsafe(impliedVolatility), DoubleArray.ofUnsafe(impliedVolatilityDerivatives));
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrate the SABR parameters to a set of option prices at given moneyness.
   * <p>
   * All the associated swaptions have the same expiration date, given by a period
   * from calibration time, and the same tenor.
   * 
   * @param bda  the business day adjustment for the exercise date adjustment
   * @param calibrationDateTime  the calibration date and time
   * @param dayCount  the day count for the computation of the time to exercise
   * @param periodToExpiry  the period to expiry
   * @param forward  the forward price/rate
   * @param strikesLike  the options strike-like dimension
   * @param strikeType  the strike type
   * @param prices  the option (call/payer) prices
   * @param startParameters  the starting parameters for the calibration. If one or more of the parameters are fixed,
   * the starting value will be used as the fixed parameter.
   * @param fixedParameters  the flag for the fixed parameters that are not calibrated
   * @param shiftOutput  the shift to calibrate the shifted SABR
   * @return SABR parameters
   */
  public Pair<LeastSquareResultsWithTransform, DoubleArray> calibrateLsShiftedFromPrices(
      BusinessDayAdjustment bda,
      ZonedDateTime calibrationDateTime,
      DayCount dayCount,
      Period periodToExpiry,
      double forward,
      DoubleArray strikesLike,
      ValueType strikeType,
      DoubleArray prices,
      DoubleArray startParameters,
      BitSet fixedParameters,
      double shiftOutput) {

    int nbStrikes = strikesLike.size();
    ArgChecker.isTrue(nbStrikes == prices.size(), "size of strikes must be the same as size of prices");
    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    LocalDate exerciseDate = expirationDate(bda, calibrationDate, periodToExpiry);
    double timeToExpiry = dayCount.relativeYearFraction(calibrationDate, exerciseDate);
    DoubleArray errors = DoubleArray.filled(nbStrikes, 1e-4);
    DoubleArray strikes = strikesShifted(forward, 0.0, strikesLike, strikeType);
    Pair<DoubleArray, DoubleArray> volAndDerivatives = blackVolatilitiesShiftedFromPrices(
        forward, shiftOutput, timeToExpiry, strikes, prices);
    DoubleArray blackVolatilitiesTransformed = volAndDerivatives.getFirst();
    DoubleArray strikesShifted = strikesShifted(forward, shiftOutput, strikesLike, strikeType);
    SabrModelFitter fitter = new SabrModelFitter(
        forward + shiftOutput,
        strikesShifted,
        timeToExpiry,
        blackVolatilitiesTransformed,
        errors,
        sabrVolatilityFormula);
    return Pair.of(fitter.solve(startParameters, fixedParameters), volAndDerivatives.getSecond());
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an array of shifted Black volatilities from option prices and the sensitivities of the 
   * Black volatilities with respect to the price inputs.
   * 
   * @param forward  the forward rate
   * @param shiftOutput  the shift required in the output
   * @param timeToExpiry  the time to expiration
   * @param strikes  the option strikes
   * @param prices  the option prices
   * @return the shifted black volatilities and their derivatives
   */
  public Pair<DoubleArray, DoubleArray> blackVolatilitiesShiftedFromPrices(
      double forward,
      double shiftOutput,
      double timeToExpiry,
      DoubleArray strikes,
      DoubleArray prices) {

    int nbStrikes = strikes.size();
    double[] impliedVolatility = new double[nbStrikes];
    double[] impliedVolatilityDerivatives = new double[nbStrikes];
    for (int i = 0; i < nbStrikes; i++) {
      ValueDerivatives iv = BlackFormulaRepository.impliedVolatilityAdjoint(
          prices.get(i), forward + shiftOutput, strikes.get(i) + shiftOutput, timeToExpiry, true);
      impliedVolatility[i] = iv.getValue();
      impliedVolatilityDerivatives[i] = iv.getDerivative(0);
    }
    return Pair.of(DoubleArray.ofUnsafe(impliedVolatility), DoubleArray.ofUnsafe(impliedVolatilityDerivatives));
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrate the SABR parameters to a set of normal volatilities at given moneyness.
   * <p>
   * All the associated swaptions have the same expiration date, given by a period
   * from calibration time, and the same tenor.
   * 
   * @param bda  the business day adjustment for the exercise date adjustment
   * @param calibrationDateTime  the calibration date and time
   * @param dayCount  the day count for the computation of the time to exercise
   * @param periodToExpiry  the period to expiry
   * @param forward  the forward price/rate
   * @param strikesLike  the options strike-like dimension
   * @param strikeType  the strike type
   * @param normalVolatilities  the option (call/payer) normal model implied volatilities
   * @param startParameters  the starting parameters for the calibration. If one or more of the parameters are fixed,
   * the starting value will be used as the fixed parameter.
   * @param fixedParameters  the flag for the fixed parameters that are not calibrated
   * @param shiftOutput  the shift to calibrate the shifted SABR
   * @return the least square results and the derivative of the shifted log-normal used for calibration with respect 
   * to the raw data
   */
  public Pair<LeastSquareResultsWithTransform, DoubleArray> calibrateLsShiftedFromNormalVolatilities(
      BusinessDayAdjustment bda,
      ZonedDateTime calibrationDateTime,
      DayCount dayCount,
      Period periodToExpiry,
      double forward,
      DoubleArray strikesLike,
      ValueType strikeType,
      DoubleArray normalVolatilities,
      DoubleArray startParameters,
      BitSet fixedParameters,
      double shiftOutput) {

    int nbStrikes = strikesLike.size();
    ArgChecker.isTrue(nbStrikes == normalVolatilities.size(), "size of strikes must be the same as size of prices");
    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    LocalDate exerciseDate = expirationDate(bda, calibrationDate, periodToExpiry);
    double timeToExpiry = dayCount.relativeYearFraction(calibrationDate, exerciseDate);
    DoubleArray errors = DoubleArray.filled(nbStrikes, 1e-4);
    DoubleArray strikes = strikesShifted(forward, 0.0, strikesLike, strikeType);
    Pair<DoubleArray, DoubleArray> volAndDerivatives = blackVolatilitiesShiftedFromNormalVolatilities(
        forward, shiftOutput, timeToExpiry, strikes, normalVolatilities);
    DoubleArray blackVolatilitiesTransformed = volAndDerivatives.getFirst();
    DoubleArray strikesShifted = strikesShifted(forward, shiftOutput, strikesLike, strikeType);
    SabrModelFitter fitter = new SabrModelFitter(
        forward + shiftOutput,
        strikesShifted,
        timeToExpiry,
        blackVolatilitiesTransformed,
        errors,
        sabrVolatilityFormula);
    LeastSquareResultsWithTransform result = fitter.solve(startParameters, fixedParameters);
    return Pair.of(result, volAndDerivatives.getSecond());
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrate the SABR alpha parameter to an ATM normal volatility and compute the derivative of the result 
   * with respect to the input volatility.
   * 
   * @param bda  the business day adjustment for the exercise date adjustment
   * @param calibrationDateTime  the calibration date and time
   * @param dayCount  the day count for the computation of the time to exercise
   * @param periodToExpiry  the period to expiry
   * @param forward  the forward price/rate
   * @param normalVolatility  the option (call/payer) normal model implied volatility
   * @param startParameters  the starting parameters for the calibration. The alpha parameter is used as a starting
   * point for the root-finding, the other parameters are fixed.
   * @param shiftOutput  the shift to calibrate the shifted SABR
   * @return the alpha calibrated and its derivative with respect to the volatility
   */
  public Pair<Double, Double> calibrateAtmShiftedFromNormalVolatilities(
      BusinessDayAdjustment bda,
      ZonedDateTime calibrationDateTime,
      DayCount dayCount,
      Period periodToExpiry,
      double forward,
      double normalVolatility,
      DoubleArray startParameters,
      double shiftOutput) {

    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    LocalDate exerciseDate = expirationDate(bda, calibrationDate, periodToExpiry);
    double timeToExpiry = dayCount.relativeYearFraction(calibrationDate, exerciseDate);
    Pair<DoubleArray, DoubleArray> volAndDerivatives = blackVolatilitiesShiftedFromNormalVolatilities(
        forward, shiftOutput, timeToExpiry, DoubleArray.of(forward), DoubleArray.of(normalVolatility));
    DoubleArray blackVolatilitiesTransformed = volAndDerivatives.getFirst();
    Function<Double, Double> volFunction =
        (a) -> sabrVolatilityFormula.volatility(forward + shiftOutput, forward + shiftOutput, timeToExpiry, a,
            startParameters.get(1), startParameters.get(2), startParameters.get(3)) - blackVolatilitiesTransformed.get(0);
    double alphaCalibrated = ROOT_FINDER.getRoot(volFunction, startParameters.get(0));
    double dAlphadBlack = 1.0d / sabrVolatilityFormula.volatilityAdjoint(forward + shiftOutput, forward + shiftOutput,
        timeToExpiry, alphaCalibrated, startParameters.get(1), startParameters.get(2), startParameters.get(3))
        .getDerivative(2);
    return Pair.of(alphaCalibrated, dAlphadBlack * volAndDerivatives.getSecond().get(0));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an array of shifted Black volatilities from Normal volatilities and the sensitivities of the 
   * Black volatilities with respect to the normal volatilities inputs.
   * <p>
   * The transformation between normal and Black volatility is done using 
   * {@link BlackFormulaRepository#impliedVolatilityFromNormalApproximated}.
   * 
   * @param forward  the forward rate
   * @param shiftOutput  the shift required in the output
   * @param timeToExpiry  the time to expiration
   * @param strikes  the option strikes
   * @param normalVolatilities  the normal volatilities
   * @return the shifted black volatilities and their derivatives
   */
  public Pair<DoubleArray, DoubleArray> blackVolatilitiesShiftedFromNormalVolatilities(
      double forward,
      double shiftOutput,
      double timeToExpiry,
      DoubleArray strikes,
      DoubleArray normalVolatilities) {

    int nbStrikes = strikes.size();
    double[] impliedVolatility = new double[nbStrikes];
    double[] impliedVolatilityDerivatives = new double[nbStrikes];
    for (int i = 0; i < nbStrikes; i++) {
      ValueDerivatives iv = BlackFormulaRepository.impliedVolatilityFromNormalApproximatedAdjoint(
          forward + shiftOutput, strikes.get(i) + shiftOutput, timeToExpiry, normalVolatilities.get(i));
      impliedVolatility[i] = iv.getValue();
      impliedVolatilityDerivatives[i] = iv.getDerivative(0);
    }
    return Pair.of(DoubleArray.ofUnsafe(impliedVolatility), DoubleArray.ofUnsafe(impliedVolatilityDerivatives));
  }

  //-------------------------------------------------------------------------
  /**
   * Compute shifted strikes from forward and strike-like value type.
   * 
   * @param forward  the forward rate
   * @param shiftOutput  the shift for the output
   * @param strikesLike  the strike-like values 
   * @param strikeType  the strike type
   * @return the strikes
   */
  private DoubleArray strikesShifted(double forward, double shiftOutput, DoubleArray strikesLike, ValueType strikeType) {
    int nbStrikes = strikesLike.size();
    if (strikeType.equals(ValueType.STRIKE)) {
      return DoubleArray.of(nbStrikes, i -> strikesLike.get(i) + shiftOutput);
    }
    if (strikeType.equals(ValueType.SIMPLE_MONEYNESS)) {
      return DoubleArray.of(nbStrikes, i -> forward + strikesLike.get(i) + shiftOutput);
    }
    if (strikeType.equals(ValueType.LOG_MONEYNESS)) {
      return DoubleArray.of(nbStrikes, i -> forward * Math.exp(strikesLike.get(i)) + shiftOutput);
    }
    throw new IllegalArgumentException("Strike type not supported");
  }

  /**
   * Calculates the expiration date of a swaption from the calibration date and the underlying swap convention.
   * 
   * @param convention  the underlying swap convention
   * @param calibrationDate  the calibration date
   * @param expiry  the period to expiry
   * @return the date
   */
  private LocalDate expirationDate(BusinessDayAdjustment bda, LocalDate calibrationDate, Period expiry) {
    return bda.adjust(calibrationDate.plus(expiry), refData);
  }

}
