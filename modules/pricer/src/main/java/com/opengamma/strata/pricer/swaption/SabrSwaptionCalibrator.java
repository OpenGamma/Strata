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

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.surface.Surfaces;
import com.opengamma.strata.market.surface.meta.SwaptionSurfaceExpiryTenorNodeMetadata;
import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.interpolation.GridInterpolator2D;
import com.opengamma.strata.math.impl.statistics.leastsquare.LeastSquareResultsWithTransform;
import com.opengamma.strata.pricer.calibration.RawOptionData;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.option.SabrInterestRateParameters;
import com.opengamma.strata.pricer.impl.volatility.smile.fitting.SabrModelFitter;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrFormulaData;
import com.opengamma.strata.pricer.impl.volatility.smile.function.SabrHaganVolatilityFunctionProvider;
import com.opengamma.strata.pricer.impl.volatility.smile.function.VolatilityFunctionProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.SwapTrade;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Utilities to calibrate SABR parameters to swaptions.
 */
public class SabrSwaptionCalibrator {

  /**
   * The SABR implied volatility function.
   */
  private final VolatilityFunctionProvider<SabrFormulaData> sabrFunctionProvider;
  /**
   * The swap pricer.
   * Required for forward rate computation.
   */
  private final DiscountingSwapProductPricer swapPricer;
  /**
   * The reference data.
   */
  private final ReferenceData refData;

  /**
   * The default instance of the class.
   */
  public static final SabrSwaptionCalibrator DEFAULT =
      new SabrSwaptionCalibrator(
          SabrHaganVolatilityFunctionProvider.DEFAULT, DiscountingSwapProductPricer.DEFAULT, ReferenceData.standard());

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a SABR volatility function provider and a swap pricer.
   * <p>
   * The swap pricer is used to compute the forward rate required for calibration.
   * 
   * @param sabrFunctionProvider  the SABR implied volatility formula provider
   * @param swapPricer  the swap pricer
   * @return the calibrator
   */
  public static SabrSwaptionCalibrator of(
      VolatilityFunctionProvider<SabrFormulaData> sabrFunctionProvider,
      DiscountingSwapProductPricer swapPricer) {

    return new SabrSwaptionCalibrator(sabrFunctionProvider, swapPricer, ReferenceData.standard());
  }

  /**
   * Obtains an instance from a SABR volatility function provider and a swap pricer.
   * <p>
   * The swap pricer is used to compute the forward rate required for calibration.
   * 
   * @param sabrFunctionProvider  the SABR implied volatility formula provider
   * @param swapPricer  the swap pricer
   * @param refData  the reference data
   * @return the calibrator
   */
  public static SabrSwaptionCalibrator of(
      VolatilityFunctionProvider<SabrFormulaData> sabrFunctionProvider,
      DiscountingSwapProductPricer swapPricer,
      ReferenceData refData) {

    return new SabrSwaptionCalibrator(sabrFunctionProvider, swapPricer, refData);
  }

  private SabrSwaptionCalibrator(
      VolatilityFunctionProvider<SabrFormulaData> sabrFunctionProvider,
      DiscountingSwapProductPricer swapPricer,
      ReferenceData refData) {

    this.sabrFunctionProvider = sabrFunctionProvider;
    this.swapPricer = swapPricer;
    this.refData = refData;
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrate SABR parameters to a set of raw swaption data. 
   * <p>
   * The SABR parameters are calibrated with fixed beta and fixed shift surfaces.
   * The raw data can be (shifted) log-normal volatilities, normal volatilities or option prices
   * 
   * @param convention  the swaption underlying convention
   * @param calibrationDateTime  the data and time of the calibration
   * @param dayCount  the day-count used for expiry time computation
   * @param tenors  the tenors associated to the different raw option data
   * @param data  the list of raw option data
   * @param ratesProvider  the rate provider used to compute the swap forward rates
   * @param betaSurface  the beta surface
   * @param shiftSurface  the shift surface
   * @param interpolator  the interpolator for the alpha, rho and nu surfaces
   * @return the SABR volatility object
   */
  @SuppressWarnings("null")
  public SabrParametersSwaptionVolatilities calibrateWithFixedBetaAndShift(
      FixedIborSwapConvention convention,
      ZonedDateTime calibrationDateTime,
      DayCount dayCount,
      List<Tenor> tenors,
      List<RawOptionData> data,
      RatesProvider ratesProvider,
      NodalSurface betaSurface,
      NodalSurface shiftSurface,
      GridInterpolator2D interpolator) {

    // If a MathException is thrown by a calibration for a specific expiry/tenor, an exception is thrown by the method
    return calibrateWithFixedBetaAndShift(
        convention,
        calibrationDateTime,
        dayCount,
        tenors,
        data,
        ratesProvider,
        betaSurface,
        shiftSurface,
        interpolator,
        true);
  }

  /**
   * Calibrate SABR parameters to a set of raw swaption data. 
   * <p>
   * The SABR parameters are calibrated with fixed beta and fixed shift surfaces.
   * The raw data can be (shifted) log-normal volatilities, normal volatilities or option prices
   * <p>
   * This method offers the flexibility to skip the data sets that throw a MathException (stopOnMathException = false).
   * The option to skip those data sets should be use with care, as part of the input data may be unused in the output.
   * 
   * @param convention  the swaption underlying convention
   * @param calibrationDateTime  the data and time of the calibration
   * @param dayCount  the day-count used for expiry time computation
   * @param tenors  the tenors associated to the different raw option data
   * @param data  the list of raw option data
   * @param ratesProvider  the rate provider used to compute the swap forward rates
   * @param betaSurface  the beta surface
   * @param shiftSurface  the shift surface
   * @param interpolator  the interpolator for the alpha, rho and nu surfaces
   * @param stopOnMathException  flag indicating if the calibration should stop on math exceptions or skip the 
   *   expiries/tenors which throw MathException
   * @return the SABR volatility object
   */
  @SuppressWarnings("null")
  public SabrParametersSwaptionVolatilities calibrateWithFixedBetaAndShift(
      FixedIborSwapConvention convention,
      ZonedDateTime calibrationDateTime,
      DayCount dayCount,
      List<Tenor> tenors,
      List<RawOptionData> data,
      RatesProvider ratesProvider,
      NodalSurface betaSurface,
      NodalSurface shiftSurface,
      GridInterpolator2D interpolator,
      boolean stopOnMathException) {

    BitSet fixed = new BitSet();
    fixed.set(1); // Beta fixed
    int nbTenors = tenors.size();
    BusinessDayAdjustment bda = convention.getFloatingLeg().getStartDateBusinessDayAdjustment();
    LocalDate calibrationDate = calibrationDateTime.toLocalDate();
    DoubleArray timeToExpiryArray = DoubleArray.EMPTY;
    DoubleArray timeTenorArray = DoubleArray.EMPTY;
    DoubleArray alphaArray = DoubleArray.EMPTY;
    DoubleArray rhoArray = DoubleArray.EMPTY;
    DoubleArray nuArray = DoubleArray.EMPTY;
    List<SurfaceParameterMetadata> parameterMetadata = new ArrayList<>();
    for (int looptenor = 0; looptenor < nbTenors; looptenor++) {
      double timeTenor = tenors.get(looptenor).getPeriod().getYears() + tenors.get(looptenor).getPeriod().getMonths() / 12;
      List<Period> expiries = data.get(looptenor).getExpiries();
      int nbExpiries = expiries.size();
      for (int loopexpiry = 0; loopexpiry < nbExpiries; loopexpiry++) {
        Pair<DoubleArray, DoubleArray> availableSmile = data.get(looptenor).availableSmileAtExpiry(expiries.get(loopexpiry));
        if (availableSmile.getFirst().size() == 0) { // If not data is available, no calibration possible
          continue;
        }
        LocalDate exerciseDate = expirationDate(bda, calibrationDate, expiries.get(loopexpiry));
        LocalDate effectiveDate = convention.calculateSpotDateFromTradeDate(exerciseDate, refData);
        double timeToExpiry = dayCount.relativeYearFraction(calibrationDate, exerciseDate);
        double beta = betaSurface.zValue(timeToExpiry, timeTenor);
        double shift = shiftSurface.zValue(timeToExpiry, timeTenor);
        LocalDate endDate = effectiveDate.plus(tenors.get(looptenor));
        SwapTrade swap0 = convention.toTrade(calibrationDate, effectiveDate, endDate, BuySell.BUY, 1.0, 0.0);
        double forward = swapPricer.parRate(swap0.getProduct().resolve(refData), ratesProvider);
        SabrFormulaData sabrPoint = null;
        boolean error = false;
        try {
          sabrPoint = calibration(forward, shift, beta, fixed, bda, calibrationDateTime, dayCount,
              availableSmile.getFirst(), availableSmile.getSecond(), expiries.get(loopexpiry), data.get(looptenor));
        } catch (MathException e) {
          error = true;
          if (stopOnMathException) {
            String message = Messages.format("{} at expiry {} and tenor {}", e.getMessage(),
                expiries.get(loopexpiry), tenors.get(looptenor));
            throw new MathException(message, e);
          }
        }
        if (!error) {
          timeToExpiryArray = timeToExpiryArray.concat(new double[] {timeToExpiry});
          timeTenorArray = timeTenorArray.concat(new double[] {timeTenor});
          alphaArray = alphaArray.concat(new double[] {sabrPoint.getAlpha()});
          rhoArray = rhoArray.concat(new double[] {sabrPoint.getRho()});
          nuArray = nuArray.concat(new double[] {sabrPoint.getNu()});
          parameterMetadata.add(SwaptionSurfaceExpiryTenorNodeMetadata.of(timeToExpiry, timeTenor,
              expiries.get(loopexpiry).toString() + "x" + tenors.get(looptenor).toString()));
        }
      }
    }
    SurfaceMetadata metadataAlpha = Surfaces.swaptionSabrExpiryTenor(
        "Swaption-SABR-Alpha", dayCount, convention, ValueType.SABR_ALPHA).withParameterMetadata(parameterMetadata);
    SurfaceMetadata metadataBeta = Surfaces.swaptionSabrExpiryTenor(
        "Swaption-SABR-Beta", dayCount, convention, ValueType.SABR_BETA).withParameterMetadata(parameterMetadata);
    SurfaceMetadata metadataRho = Surfaces.swaptionSabrExpiryTenor(
        "Swaption-SABR-Rho", dayCount, convention, ValueType.SABR_RHO).withParameterMetadata(parameterMetadata);
    SurfaceMetadata metadataNu = Surfaces.swaptionSabrExpiryTenor(
        "Swaption-SABR-Nu", dayCount, convention, ValueType.SABR_NU).withParameterMetadata(parameterMetadata);
    InterpolatedNodalSurface alphaSurface = InterpolatedNodalSurface
        .of(metadataAlpha, timeToExpiryArray, timeTenorArray, alphaArray, interpolator);
    InterpolatedNodalSurface rhoSurface = InterpolatedNodalSurface
        .of(metadataRho, timeToExpiryArray, timeTenorArray, rhoArray, interpolator);
    InterpolatedNodalSurface nuSurface = InterpolatedNodalSurface
        .of(metadataNu, timeToExpiryArray, timeTenorArray, nuArray, interpolator);
    SabrInterestRateParameters params = SabrInterestRateParameters.of(
        alphaSurface, betaSurface.withMetadata(metadataBeta), rhoSurface, nuSurface, shiftSurface, sabrFunctionProvider);
    return SabrParametersSwaptionVolatilities.of(params, calibrationDateTime);
  }

  // The main part of the calibration. The calibration is done 4 times with different starting points: low and high
  // volatilities and high and low vol of vol. The best result (in term of chi^2) is returned.
  private SabrFormulaData calibration(
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
    SabrFormulaData sabrPoint = null;
    for (int i = 0; i < 4; i++) { // Try different starting points and take the best
      DoubleArray startParameters = DoubleArray.of(alphaStart[i], beta, rhoStart, nuStart[i]);
      LeastSquareResultsWithTransform r = null;
      if (rawData.getDataType().equals(ValueType.NORMAL_VOLATILITY)) {
        r = calibrateShiftedFromNormalVolatilities(bda, calibrationDateTime, dayCount,
            expiry, forward, strike, rawData.getStrikeType(),
            data, startParameters, fixed, shift);
      } else {
        if (rawData.getDataType().equals(ValueType.PRICE)) {
          r = calibrateShiftedFromPrices(bda, calibrationDateTime, dayCount,
              expiry, forward, strike, rawData.getStrikeType(),
              data, startParameters, fixed, shift);
        } else {
          if (rawData.getDataType().equals(ValueType.BLACK_VOLATILITY)) {
            r = calibrateShiftedFromBlackVolatilities(bda, calibrationDateTime, dayCount,
                expiry, forward, strike, rawData.getStrikeType(),
                data, rawData.getShift().orElse(0d), startParameters, fixed, shift);
          } else {
            throw new IllegalArgumentException("Data type not supported");
          }
        }
      }
      if (r.getChiSq() < chi2) { // Better calibration
        sabrPoint = SabrFormulaData.of(r.getModelParameters().toArrayUnsafe());
        chi2 = r.getChiSq();
      }
    }
    return sabrPoint;
  }

  //-------------------------------------------------------------------------
  /**
   * Calibrate the SABR parameters to a set of Black volatilities at given moneyness.
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
   * @return SABR parameters
   */
  public LeastSquareResultsWithTransform calibrateShiftedFromBlackVolatilities(
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
    DoubleArray blackVolatilitiesTransformed = blackVolatilitiesShiftedFromBlackVolatilitiesShifted(
        forward, shiftOutput, timeToExpiry, strikes, blackVolatilitiesInput, shiftInput);
    DoubleArray strikesShifted = strikesShifted(forward, shiftOutput, strikesLike, strikeType);
    SabrModelFitter fitter = new SabrModelFitter(forward + shiftOutput, strikesShifted, timeToExpiry,
        blackVolatilitiesTransformed, errors, sabrFunctionProvider);
    return fitter.solve(startParameters, fixedParameters);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an array of shifted Black volatilities from shifted Black volatilities with a different shift.
   * 
   * @param forward  the forward rate
   * @param shiftOutput  the shift required in the output
   * @param timeToExpiry  the time to expiration
   * @param strikes  the option strikes
   * @param blackVolatilities  the shifted implied Black volatilities
   * @param shiftInput  the shift used in the input Black implied volatilities
   * @return the shifted black volatilities
   */
  public DoubleArray blackVolatilitiesShiftedFromBlackVolatilitiesShifted(
      double forward,
      double shiftOutput,
      double timeToExpiry,
      DoubleArray strikes,
      DoubleArray blackVolatilities,
      double shiftInput) {

    if (shiftInput == shiftOutput) {
      return blackVolatilities; // No change required if shifts are the same
    }
    return DoubleArray.of(strikes.size(), i -> {
      double price = BlackFormulaRepository.price(
          forward + shiftInput, strikes.get(i) + shiftInput, timeToExpiry, blackVolatilities.get(i), true);
      return BlackFormulaRepository.impliedVolatility(
          price, forward + shiftOutput, strikes.get(i) + shiftOutput, timeToExpiry, true);
    });
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
  public LeastSquareResultsWithTransform calibrateShiftedFromPrices(
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
    DoubleArray blackVolatilitiesTransformed = blackVolatilitiesShiftedFromPrices(
        forward, shiftOutput, timeToExpiry, strikes, prices);
    DoubleArray strikesShifted = strikesShifted(forward, shiftOutput, strikesLike, strikeType);
    SabrModelFitter fitter = new SabrModelFitter(forward + shiftOutput, strikesShifted, timeToExpiry,
        blackVolatilitiesTransformed, errors, sabrFunctionProvider);
    return fitter.solve(startParameters, fixedParameters);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an array of shifted Black volatilities from option prices.
   * 
   * @param forward  the forward rate
   * @param shiftOutput  the shift required in the output
   * @param timeToExpiry  the time to expiration
   * @param strikes  the option strikes
   * @param prices  the option prices
   * @return the shifted black volatilities
   */
  public DoubleArray blackVolatilitiesShiftedFromPrices(
      double forward,
      double shiftOutput,
      double timeToExpiry,
      DoubleArray strikes,
      DoubleArray prices) {

    return DoubleArray.of(strikes.size(), i -> {
      return BlackFormulaRepository.impliedVolatility(
          prices.get(i), forward + shiftOutput, strikes.get(i) + shiftOutput, timeToExpiry, true);
    });
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
   * @return SABR parameters
   */
  public LeastSquareResultsWithTransform calibrateShiftedFromNormalVolatilities(
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
    DoubleArray blackVolatilitiesTransformed = blackVolatilitiesShiftedFromNormalVolatilities(
        forward, shiftOutput, timeToExpiry, strikes, normalVolatilities);
    DoubleArray strikesShifted = strikesShifted(forward, shiftOutput, strikesLike, strikeType);
    SabrModelFitter fitter = new SabrModelFitter(
        forward + shiftOutput, strikesShifted, timeToExpiry, blackVolatilitiesTransformed, errors, sabrFunctionProvider);
    LeastSquareResultsWithTransform result = fitter.solve(startParameters, fixedParameters);
    return result;
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an array of shifted Black volatilities from Normal volatilities.
   * <p>
   * The transformation between normal and Black volatility is done using 
   * {@link BlackFormulaRepository#impliedVolatilityFromNormalApproximated}.
   * 
   * @param forward  the forward rate
   * @param shiftOutput  the shift required in the output
   * @param timeToExpiry  the time to expiration
   * @param strikes  the option strikes
   * @param normalVolatilities  the normal volatilities
   * @return the shifted black volatilities
   */
  public DoubleArray blackVolatilitiesShiftedFromNormalVolatilities(
      double forward,
      double shiftOutput,
      double timeToExpiry,
      DoubleArray strikes,
      DoubleArray normalVolatilities) {

    return DoubleArray.of(strikes.size(), i -> {
      return BlackFormulaRepository.impliedVolatilityFromNormalApproximated(
          forward + shiftOutput, strikes.get(i) + shiftOutput, timeToExpiry, normalVolatilities.get(i));
    });
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
