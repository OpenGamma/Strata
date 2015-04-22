/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableDefaults;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.ForwardSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.SimplyCompoundedForwardSensitivity;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxMatrix;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.pricer.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.pricer.sensitivity.IborRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.IndexCurrencySensitivityKey;
import com.opengamma.strata.pricer.sensitivity.NameCurrencySensitivityKey;
import com.opengamma.strata.pricer.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.sensitivity.PointSensitivity;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.sensitivity.SensitivityKey;
import com.opengamma.strata.pricer.sensitivity.ZeroRateSensitivity;

/**
 * The default immutable rates provider, used to calculate analytic measures.
 * <p>
 * This provides the environmental information against which pricing occurs.
 * This includes FX rates, discount factors and forward curves.
 */
@BeanDefinition
public final class ImmutableRatesProvider
    implements RatesProvider, ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The valuation date.
   * All curves and other data items in this provider are calibrated for this date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /**
   * The matrix of foreign exchange rates, defaulted to an empty matrix.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final FxMatrix fxMatrix;
  /**
   * The discount curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each currency.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<Currency, YieldAndDiscountCurve> discountCurves;
  /**
   * The forward curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each index.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<Index, YieldAndDiscountCurve> indexCurves;
  /**
   * The time-series, defaulted to an empty map.
   * The historic data associated with each index.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final ImmutableMap<Index, LocalDateDoubleTimeSeries> timeSeries;
  /**
   * The day count applicable to the models.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final DayCount dayCount;

  //-------------------------------------------------------------------------
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.fxMatrix = FxMatrix.empty();
    builder.discountCurves = ImmutableMap.of();
    builder.indexCurves = ImmutableMap.of();
    builder.timeSeries = ImmutableMap.of();
  }

  //-------------------------------------------------------------------------
  @Override
  public LocalDateDoubleTimeSeries timeSeries(Index index) {
    ArgChecker.notNull(index, "index");
    LocalDateDoubleTimeSeries series = timeSeries.get(index);
    if (series == null) {
      throw new IllegalArgumentException("Unknown index: " + index.getName());
    }
    return series;
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    ArgChecker.notNull(baseCurrency, "baseCurrency");
    ArgChecker.notNull(counterCurrency, "counterCurrency");
    if (baseCurrency.equals(counterCurrency)) {
      return 1d;
    }
    return fxMatrix.rate(baseCurrency, counterCurrency);
  }

  //-------------------------------------------------------------------------
  @Override
  public double discountFactor(Currency currency, LocalDate date) {
    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(date, "date");
    return discountCurve(currency).getDiscountFactor(relativeTime(date));
  }

  @Override
  public PointSensitivityBuilder discountFactorZeroRateSensitivity(Currency currency, LocalDate date) {
    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(date, "date");
    double relativeTime = relativeTime(date);
    double discountFactor = discountCurve(currency).getDiscountFactor(relativeTime);
    return ZeroRateSensitivity.of(currency, date, -discountFactor * relativeTime);
  }

  // lookup the discount curve for the currency
  private YieldAndDiscountCurve discountCurve(Currency currency) {
    YieldAndDiscountCurve curve = discountCurves.get(currency);
    if (curve == null) {
      throw new IllegalArgumentException("Unable to find discount curve: " + currency);
    }
    return curve;
  }

  //-------------------------------------------------------------------------
  @Override
  public double fxIndexRate(FxIndex index, Currency baseCurrency, LocalDate fixingDate) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(baseCurrency, "baseCurrency");
    ArgChecker.notNull(fixingDate, "fixingDate");
    ArgChecker.isTrue(
        index.getCurrencyPair().contains(baseCurrency),
        "Currency {} invalid for FxIndex {}", baseCurrency, index);
    boolean inverse = baseCurrency.equals(index.getCurrencyPair().getCounter());
    if (!fixingDate.isAfter(valuationDate)) {
      return fxIndexHistoricRate(index, fixingDate, inverse);
    }
    return fxIndexForwardRate(index, fixingDate, inverse);
  }

  // historic rate
  private double fxIndexHistoricRate(FxIndex index, LocalDate fixingDate, boolean inverse) {
    OptionalDouble fixedRate = timeSeries(index).get(fixingDate);
    if (fixedRate.isPresent()) {
      // if the index is the inverse of the desired pair, then invert it
      double fxIndexRate = fixedRate.getAsDouble();
      return (inverse ? 1d / fxIndexRate : fxIndexRate);
    } else if (fixingDate.isBefore(valuationDate)) { // the fixing is required
      throw new PricingException(Messages.format("Unable to get fixing for {} on date {}", index, fixingDate));
    } else {
      return fxIndexForwardRate(index, fixingDate, inverse);
    }
  }

  // forward rate
  private double fxIndexForwardRate(FxIndex index, LocalDate fixingDate, boolean inverse) {
    // use the specified base currency to determine the desired currency pair
    // then derive rate from discount factors based off desired currency pair, not that of the index
    CurrencyPair pair = inverse ? index.getCurrencyPair().inverse() : index.getCurrencyPair();
    double maturity = relativeTime(index.calculateMaturityFromFixing(fixingDate));
    double dfCcyBaseAtMaturity = discountCurve(pair.getBase()).getDiscountFactor(maturity);
    double dfCcyCounterAtMaturity = discountCurve(pair.getCounter()).getDiscountFactor(maturity);
    return fxRate(pair) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
  }

  //-------------------------------------------------------------------------
  @Override
  public double iborIndexRate(IborIndex index, LocalDate fixingDate) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(fixingDate, "fixingDate");
    if (!fixingDate.isAfter(valuationDate)) {
      return iborIndexHistoricRate(index, fixingDate);
    }
    return iborIndexForwardRate(index, fixingDate);
  }

  // historic rate
  private double iborIndexHistoricRate(IborIndex index, LocalDate fixingDate) {
    OptionalDouble fixedRate = timeSeries(index).get(fixingDate);
    if (fixedRate.isPresent()) {
      return fixedRate.getAsDouble();
    } else if (fixingDate.isBefore(valuationDate)) { // the fixing is required
      throw new PricingException(Messages.format("Unable to get fixing for {} on date {}", index, fixingDate));
    } else {
      return iborIndexForwardRate(index, fixingDate);
    }
  }

  // forward rate
  private double iborIndexForwardRate(IborIndex index, LocalDate fixingDate) {
    LocalDate fixingStartDate = index.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
    double fixingYearFraction = index.getDayCount().yearFraction(fixingStartDate, fixingEndDate);
    return indexCurve(index).getSimplyCompoundForwardRate(
        relativeTime(fixingStartDate), relativeTime(fixingEndDate), fixingYearFraction);
  }

  @Override
  public PointSensitivityBuilder iborIndexRateSensitivity(IborIndex index, LocalDate fixingDate) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(fixingDate, "fixingDate");
    if (fixingDate.isBefore(valuationDate) ||
        (fixingDate.equals(valuationDate) && timeSeries(index).get(fixingDate).isPresent())) {
      return PointSensitivityBuilder.none();
    }
    return IborRateSensitivity.of(index, fixingDate, 1d);
  }

  // lookup the discount curve for the currency
  private YieldAndDiscountCurve indexCurve(Index index) {
    YieldAndDiscountCurve curve = indexCurves.get(index);
    if (curve == null) {
      throw new IllegalArgumentException("Unable to find index curve: " + index);
    }
    return curve;
  }

  //-------------------------------------------------------------------------
  @Override
  public double overnightIndexRate(OvernightIndex index, LocalDate fixingDate) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(fixingDate, "fixingDate");
    LocalDate publicationDate = index.calculatePublicationFromFixing(fixingDate);
    if (!publicationDate.isAfter(valuationDate)) {
      return overnightIndexHistoricRate(index, fixingDate, publicationDate);
    }
    return overnightIndexForwardRate(index, fixingDate);
  }

  // historic rate
  private double overnightIndexHistoricRate(OvernightIndex index, LocalDate fixingDate, LocalDate publicationDate) {
    OptionalDouble fixedRate = timeSeries(index).get(fixingDate);
    if (fixedRate.isPresent()) {
      return fixedRate.getAsDouble();
    } else if (publicationDate.isBefore(valuationDate)) { // the fixing is required
      throw new PricingException(Messages.format("Unable to get fixing for {} on date {}", index, fixingDate));
    } else {
      return overnightIndexForwardRate(index, fixingDate);
    }
  }

  // forward rate
  private double overnightIndexForwardRate(OvernightIndex index, LocalDate fixingDate) {
    LocalDate fixingStartDate = index.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
    double fixingYearFraction = index.getDayCount().yearFraction(fixingStartDate, fixingEndDate);
    return indexCurve(index).getSimplyCompoundForwardRate(
        relativeTime(fixingStartDate), relativeTime(fixingEndDate), fixingYearFraction);
  }

  @Override
  public PointSensitivityBuilder overnightIndexRateSensitivity(OvernightIndex index, LocalDate fixingDate) {
    ArgChecker.notNull(index, "index");
    ArgChecker.notNull(fixingDate, "fixingDate");
    LocalDate publicationDate = index.calculatePublicationFromFixing(fixingDate);
    if (publicationDate.isBefore(valuationDate) ||
        (publicationDate.equals(valuationDate) && timeSeries(index).get(fixingDate).isPresent())) {
      return PointSensitivityBuilder.none();
    }
    LocalDate fixingStartDate = index.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate = index.calculateMaturityFromEffective(fixingStartDate);
    return OvernightRateSensitivity.of(index, index.getCurrency(), fixingDate, fixingEndDate, 1d);
  }

  //-------------------------------------------------------------------------
  @Override
  public double overnightIndexRatePeriod(OvernightIndex index, LocalDate startDate, LocalDate endDate) {
    ArgChecker.notNull(index, "index");
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    ArgChecker.inOrderOrEqual(valuationDate, startDate, "valuationDate", "startDate");
    double fixingYearFraction = index.getDayCount().yearFraction(startDate, endDate);
    return indexCurve(index).getSimplyCompoundForwardRate(
        relativeTime(startDate), relativeTime(endDate), fixingYearFraction);
  }

  @Override
  public PointSensitivityBuilder overnightIndexRatePeriodSensitivity(
      OvernightIndex index,
      LocalDate startDate,
      LocalDate endDate) {

    ArgChecker.notNull(index, "index");
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    ArgChecker.inOrderOrEqual(valuationDate, startDate, "valuationDate", "startDate");
    return OvernightRateSensitivity.of(index, index.getCurrency(), startDate, endDate, 1d);
  }

  //-------------------------------------------------------------------------
  @Override
  public CurveParameterSensitivity parameterSensitivity(PointSensitivities sensitivities) {
    Map<SensitivityKey, double[]> map = new HashMap<>();
    paramSensitivityZeroRate(sensitivities, map);
    parameterSensitivityIbor(sensitivities, map);
    parameterSensitivityOvernight(sensitivities, map);
    return CurveParameterSensitivity.of(map);
  }

  // handle zero rate sensitivities
  private void paramSensitivityZeroRate(PointSensitivities sensitivities, Map<SensitivityKey, double[]> mutableMap) {
    // group by currency
    ListMultimap<Currency, DoublesPair> grouped = ArrayListMultimap.create();
    for (PointSensitivity point : sensitivities.getSensitivities()) {
      if (point instanceof ZeroRateSensitivity) {
        grouped.put(point.getCurrency(), DoublesPair.of(relativeTime(point.getDate()), point.getSensitivity()));
      }
    }
    // calculate per currency
    for (Currency ccy : grouped.keySet()) {
      YieldAndDiscountCurve curve = discountCurve(ccy);
      SensitivityKey keyParam = NameCurrencySensitivityKey.of(curve.getName(), ccy);
      double[] sensiParam = parameterSensitivityZeroRate(curve, grouped.get(ccy));
      mutableMap.put(keyParam, sensiParam);
    }
  }

  // sensitivity, copied from MulticurveProviderDiscount
  private double[] parameterSensitivityZeroRate(YieldAndDiscountCurve curve, List<DoublesPair> pointSensitivity) {
    int nbParameters = curve.getNumberOfParameters();
    double[] result = new double[nbParameters];
    for (DoublesPair timeAndS : pointSensitivity) {
      double[] sensi1Point = curve.getInterestRateParameterSensitivity(timeAndS.getFirst());
      for (int i = 0; i < nbParameters; i++) {
        result[i] += timeAndS.getSecond() * sensi1Point[i];
      }
    }
    return result;
  }

  // handle ibor rate sensitivities
  private void parameterSensitivityIbor(PointSensitivities sensitivities, Map<SensitivityKey, double[]> mutableMap) {
    // group by currency
    ListMultimap<IndexCurrencySensitivityKey, ForwardSensitivity> grouped = ArrayListMultimap.create();
    for (PointSensitivity point : sensitivities.getSensitivities()) {
      if (point instanceof IborRateSensitivity) {
        IborRateSensitivity pt = (IborRateSensitivity) point;
        IborIndex index = pt.getIndex();
        LocalDate startDate = index.calculateEffectiveFromFixing(pt.getDate());
        LocalDate endDate = index.calculateMaturityFromEffective(startDate);
        double startTime = relativeTime(startDate);
        double endTime = relativeTime(endDate);
        double accrualFactor = index.getDayCount().yearFraction(startDate, endDate);
        IndexCurrencySensitivityKey key = IndexCurrencySensitivityKey.of(index, pt.getCurrency());
        grouped.put(key, new SimplyCompoundedForwardSensitivity(startTime, endTime, accrualFactor, pt.getSensitivity()));
      }
    }
    // calculate per currency
    for (IndexCurrencySensitivityKey key : grouped.keySet()) {
      YieldAndDiscountCurve curve = indexCurve(key.getIndex());
      SensitivityKey keyParam = NameCurrencySensitivityKey.of(curve.getName(), key.getCurrency());
      double[] sensiParam = parameterSensitivityIndex(curve, grouped.get(key));
      mutableMap.merge(keyParam, sensiParam, ImmutableRatesProvider::combineArrays);
    }
  }

  // handle overnight rate sensitivities
  private void parameterSensitivityOvernight(PointSensitivities sensitivities, Map<SensitivityKey, double[]> mutableMap) {
    // group by currency
    ListMultimap<IndexCurrencySensitivityKey, ForwardSensitivity> grouped = ArrayListMultimap.create();
    for (PointSensitivity point : sensitivities.getSensitivities()) {
      if (point instanceof OvernightRateSensitivity) {
        OvernightRateSensitivity pt = (OvernightRateSensitivity) point;
        OvernightIndex index = pt.getIndex();
        LocalDate fixingDate = pt.getFixingDate();
        LocalDate endDate = pt.getEndDate();
        LocalDate startDate = index.calculateEffectiveFromFixing(fixingDate);
        double startTime = relativeTime(startDate);
        double endTime = relativeTime(endDate);
        double accrualFactor = index.getDayCount().yearFraction(startDate, endDate);
        IndexCurrencySensitivityKey key = IndexCurrencySensitivityKey.of(index, pt.getCurrency());
        grouped.put(key, new SimplyCompoundedForwardSensitivity(startTime, endTime, accrualFactor, pt.getSensitivity()));
      }
    }
    // calculate per currency
    for (IndexCurrencySensitivityKey key : grouped.keySet()) {
      YieldAndDiscountCurve curve = indexCurve(key.getIndex());
      SensitivityKey keyParam = NameCurrencySensitivityKey.of(curve.getName(), key.getCurrency());
      double[] sensiParam = parameterSensitivityIndex(curve, grouped.get(key));
      mutableMap.merge(keyParam, sensiParam, ImmutableRatesProvider::combineArrays);
    }
  }

  // sensitivity, copied from MulticurveProviderDiscount
  private double[] parameterSensitivityIndex(YieldAndDiscountCurve curve, List<ForwardSensitivity> pointSensitivity) {
    int nbParameters = curve.getNumberOfParameters();
    double[] result = new double[nbParameters];
    for (ForwardSensitivity timeAndS : pointSensitivity) {
      double startTime = timeAndS.getStartTime();
      double endTime = timeAndS.getEndTime();
      double forwardBar = timeAndS.getValue();
      // Implementation note: only the sensitivity to the forward is available.
      // The sensitivity to the pseudo-discount factors need to be computed.
      double dfForwardStart = curve.getDiscountFactor(startTime);
      double dfForwardEnd = curve.getDiscountFactor(endTime);
      double dFwddyStart = timeAndS.derivativeToYieldStart(dfForwardStart, dfForwardEnd);
      double dFwddyEnd = timeAndS.derivativeToYieldEnd(dfForwardStart, dfForwardEnd);
      double[] sensiPtStart = curve.getInterestRateParameterSensitivity(startTime);
      double[] sensiPtEnd = curve.getInterestRateParameterSensitivity(endTime);
      for (int i = 0; i < nbParameters; i++) {
        result[i] += dFwddyStart * sensiPtStart[i] * forwardBar;
        result[i] += dFwddyEnd * sensiPtEnd[i] * forwardBar;
      }
    }
    return result;
  }

  // add two arrays
  private static double[] combineArrays(double[] a, double[] b) {
    ArgChecker.isTrue(a.length == b.length, "Sensitivity arrays must have same length");
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] + b[i];
    }
    return result;
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(LocalDate date) {
    ArgChecker.notNull(date, "date");
    return dayCount.relativeYearFraction(valuationDate, date);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableRatesProvider}.
   * @return the meta-bean, not null
   */
  public static ImmutableRatesProvider.Meta meta() {
    return ImmutableRatesProvider.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableRatesProvider.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableRatesProvider.Builder builder() {
    return new ImmutableRatesProvider.Builder();
  }

  private ImmutableRatesProvider(
      LocalDate valuationDate,
      FxMatrix fxMatrix,
      Map<Currency, YieldAndDiscountCurve> discountCurves,
      Map<Index, YieldAndDiscountCurve> indexCurves,
      Map<Index, LocalDateDoubleTimeSeries> timeSeries,
      DayCount dayCount) {
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(fxMatrix, "fxMatrix");
    JodaBeanUtils.notNull(discountCurves, "discountCurves");
    JodaBeanUtils.notNull(indexCurves, "indexCurves");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.valuationDate = valuationDate;
    this.fxMatrix = fxMatrix;
    this.discountCurves = ImmutableMap.copyOf(discountCurves);
    this.indexCurves = ImmutableMap.copyOf(indexCurves);
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
    this.dayCount = dayCount;
  }

  @Override
  public ImmutableRatesProvider.Meta metaBean() {
    return ImmutableRatesProvider.Meta.INSTANCE;
  }

  @Override
  public <R> Property<R> property(String propertyName) {
    return metaBean().<R>metaProperty(propertyName).createProperty(this);
  }

  @Override
  public Set<String> propertyNames() {
    return metaBean().metaPropertyMap().keySet();
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date.
   * All curves and other data items in this provider are calibrated for this date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the matrix of foreign exchange rates, defaulted to an empty matrix.
   * @return the value of the property, not null
   */
  private FxMatrix getFxMatrix() {
    return fxMatrix;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the discount curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each currency.
   * @return the value of the property, not null
   */
  private ImmutableMap<Currency, YieldAndDiscountCurve> getDiscountCurves() {
    return discountCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the forward curves, defaulted to an empty map.
   * The curve data, predicting the future, associated with each index.
   * @return the value of the property, not null
   */
  private ImmutableMap<Index, YieldAndDiscountCurve> getIndexCurves() {
    return indexCurves;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-series, defaulted to an empty map.
   * The historic data associated with each index.
   * @return the value of the property, not null
   */
  private ImmutableMap<Index, LocalDateDoubleTimeSeries> getTimeSeries() {
    return timeSeries;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count applicable to the models.
   * @return the value of the property, not null
   */
  private DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      ImmutableRatesProvider other = (ImmutableRatesProvider) obj;
      return JodaBeanUtils.equal(getValuationDate(), other.getValuationDate()) &&
          JodaBeanUtils.equal(getFxMatrix(), other.getFxMatrix()) &&
          JodaBeanUtils.equal(getDiscountCurves(), other.getDiscountCurves()) &&
          JodaBeanUtils.equal(getIndexCurves(), other.getIndexCurves()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getFxMatrix());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDiscountCurves());
    hash = hash * 31 + JodaBeanUtils.hashCode(getIndexCurves());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(224);
    buf.append("ImmutableRatesProvider{");
    buf.append("valuationDate").append('=').append(getValuationDate()).append(',').append(' ');
    buf.append("fxMatrix").append('=').append(getFxMatrix()).append(',').append(' ');
    buf.append("discountCurves").append('=').append(getDiscountCurves()).append(',').append(' ');
    buf.append("indexCurves").append('=').append(getIndexCurves()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(getTimeSeries()).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(getDayCount()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableRatesProvider}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", ImmutableRatesProvider.class, LocalDate.class);
    /**
     * The meta-property for the {@code fxMatrix} property.
     */
    private final MetaProperty<FxMatrix> fxMatrix = DirectMetaProperty.ofImmutable(
        this, "fxMatrix", ImmutableRatesProvider.class, FxMatrix.class);
    /**
     * The meta-property for the {@code discountCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Currency, YieldAndDiscountCurve>> discountCurves = DirectMetaProperty.ofImmutable(
        this, "discountCurves", ImmutableRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code indexCurves} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Index, YieldAndDiscountCurve>> indexCurves = DirectMetaProperty.ofImmutable(
        this, "indexCurves", ImmutableRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Index, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", ImmutableRatesProvider.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ImmutableRatesProvider.class, DayCount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationDate",
        "fxMatrix",
        "discountCurves",
        "indexCurves",
        "timeSeries",
        "dayCount");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case -1198118093:  // fxMatrix
          return fxMatrix;
        case -624113147:  // discountCurves
          return discountCurves;
        case 886361302:  // indexCurves
          return indexCurves;
        case 779431844:  // timeSeries
          return timeSeries;
        case 1905311443:  // dayCount
          return dayCount;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableRatesProvider.Builder builder() {
      return new ImmutableRatesProvider.Builder();
    }

    @Override
    public Class<? extends ImmutableRatesProvider> beanType() {
      return ImmutableRatesProvider.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
    }

    /**
     * The meta-property for the {@code fxMatrix} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FxMatrix> fxMatrix() {
      return fxMatrix;
    }

    /**
     * The meta-property for the {@code discountCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Currency, YieldAndDiscountCurve>> discountCurves() {
      return discountCurves;
    }

    /**
     * The meta-property for the {@code indexCurves} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Index, YieldAndDiscountCurve>> indexCurves() {
      return indexCurves;
    }

    /**
     * The meta-property for the {@code timeSeries} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableMap<Index, LocalDateDoubleTimeSeries>> timeSeries() {
      return timeSeries;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return dayCount;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return ((ImmutableRatesProvider) bean).getValuationDate();
        case -1198118093:  // fxMatrix
          return ((ImmutableRatesProvider) bean).getFxMatrix();
        case -624113147:  // discountCurves
          return ((ImmutableRatesProvider) bean).getDiscountCurves();
        case 886361302:  // indexCurves
          return ((ImmutableRatesProvider) bean).getIndexCurves();
        case 779431844:  // timeSeries
          return ((ImmutableRatesProvider) bean).getTimeSeries();
        case 1905311443:  // dayCount
          return ((ImmutableRatesProvider) bean).getDayCount();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code ImmutableRatesProvider}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableRatesProvider> {

    private LocalDate valuationDate;
    private FxMatrix fxMatrix;
    private Map<Currency, YieldAndDiscountCurve> discountCurves = ImmutableMap.of();
    private Map<Index, YieldAndDiscountCurve> indexCurves = ImmutableMap.of();
    private Map<Index, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();
    private DayCount dayCount;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ImmutableRatesProvider beanToCopy) {
      this.valuationDate = beanToCopy.getValuationDate();
      this.fxMatrix = beanToCopy.getFxMatrix();
      this.discountCurves = beanToCopy.getDiscountCurves();
      this.indexCurves = beanToCopy.getIndexCurves();
      this.timeSeries = beanToCopy.getTimeSeries();
      this.dayCount = beanToCopy.getDayCount();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case -1198118093:  // fxMatrix
          return fxMatrix;
        case -624113147:  // discountCurves
          return discountCurves;
        case 886361302:  // indexCurves
          return indexCurves;
        case 779431844:  // timeSeries
          return timeSeries;
        case 1905311443:  // dayCount
          return dayCount;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
          break;
        case -1198118093:  // fxMatrix
          this.fxMatrix = (FxMatrix) newValue;
          break;
        case -624113147:  // discountCurves
          this.discountCurves = (Map<Currency, YieldAndDiscountCurve>) newValue;
          break;
        case 886361302:  // indexCurves
          this.indexCurves = (Map<Index, YieldAndDiscountCurve>) newValue;
          break;
        case 779431844:  // timeSeries
          this.timeSeries = (Map<Index, LocalDateDoubleTimeSeries>) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Builder setString(String propertyName, String value) {
      setString(meta().metaProperty(propertyName), value);
      return this;
    }

    @Override
    public Builder setString(MetaProperty<?> property, String value) {
      super.setString(property, value);
      return this;
    }

    @Override
    public Builder setAll(Map<String, ? extends Object> propertyValueMap) {
      super.setAll(propertyValueMap);
      return this;
    }

    @Override
    public ImmutableRatesProvider build() {
      return new ImmutableRatesProvider(
          valuationDate,
          fxMatrix,
          discountCurves,
          indexCurves,
          timeSeries,
          dayCount);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code valuationDate} property in the builder.
     * @param valuationDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDate(LocalDate valuationDate) {
      JodaBeanUtils.notNull(valuationDate, "valuationDate");
      this.valuationDate = valuationDate;
      return this;
    }

    /**
     * Sets the {@code fxMatrix} property in the builder.
     * @param fxMatrix  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder fxMatrix(FxMatrix fxMatrix) {
      JodaBeanUtils.notNull(fxMatrix, "fxMatrix");
      this.fxMatrix = fxMatrix;
      return this;
    }

    /**
     * Sets the {@code discountCurves} property in the builder.
     * @param discountCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder discountCurves(Map<Currency, YieldAndDiscountCurve> discountCurves) {
      JodaBeanUtils.notNull(discountCurves, "discountCurves");
      this.discountCurves = discountCurves;
      return this;
    }

    /**
     * Sets the {@code indexCurves} property in the builder.
     * @param indexCurves  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder indexCurves(Map<Index, YieldAndDiscountCurve> indexCurves) {
      JodaBeanUtils.notNull(indexCurves, "indexCurves");
      this.indexCurves = indexCurves;
      return this;
    }

    /**
     * Sets the {@code timeSeries} property in the builder.
     * @param timeSeries  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder timeSeries(Map<Index, LocalDateDoubleTimeSeries> timeSeries) {
      JodaBeanUtils.notNull(timeSeries, "timeSeries");
      this.timeSeries = timeSeries;
      return this;
    }

    /**
     * Sets the {@code dayCount} property in the builder.
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(224);
      buf.append("ImmutableRatesProvider.Builder{");
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("fxMatrix").append('=').append(JodaBeanUtils.toString(fxMatrix)).append(',').append(' ');
      buf.append("discountCurves").append('=').append(JodaBeanUtils.toString(discountCurves)).append(',').append(' ');
      buf.append("indexCurves").append('=').append(JodaBeanUtils.toString(indexCurves)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
