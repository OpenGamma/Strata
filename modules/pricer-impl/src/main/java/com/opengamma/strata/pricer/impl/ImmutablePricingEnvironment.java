/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
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
import com.opengamma.analytics.financial.provider.description.interestrate.MulticurveProviderInterface;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.ForwardSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.SimplyCompoundedForwardSensitivity;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.pricer.PricingEnvironment;
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
 * The default immutable pricing environment used to calculate analytic measures.
 * <p>
 * This provides the environmental information against which pricing occurs.
 * This includes FX rates, discount factors and forward curves.
 */
@BeanDefinition
public final class ImmutablePricingEnvironment
    implements PricingEnvironment, ImmutableBean, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The valuation date.
   * All curves and other data items in this environment are calibrated for this date.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;
  /**
   * The multi-curve bundle.
   */
  @PropertyDefinition(validate = "notNull", get = "private")
  private final MulticurveProviderInterface multicurve;
  /**
   * The time-series.
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
  @Override
  @SuppressWarnings("unchecked")
  public <T> T rawData(Class<T> cls) {
    ArgChecker.notNull(cls, "cls");
    if (cls == MulticurveProviderInterface.class) {
      return (T) multicurve;
    }
    throw new IllegalArgumentException("No raw data available for type: " + cls.getName());
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
    return multicurve.getFxRate(baseCurrency, counterCurrency);
  }

  //-------------------------------------------------------------------------
  @Override
  public double discountFactor(Currency currency, LocalDate date) {
    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(date, "date");
    return multicurve.getDiscountFactor(currency, relativeTime(date));
  }

  @Override
  public PointSensitivityBuilder discountFactorZeroRateSensitivity(Currency currency, LocalDate date) {
    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(date, "date");
    double relativeTime = relativeTime(date);
    double discountFactor = multicurve.getDiscountFactor(currency, relativeTime);
    return ZeroRateSensitivity.of(currency, date, -discountFactor * relativeTime);
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
    double dfCcyBaseAtMaturity = multicurve.getDiscountFactor(pair.getBase(), maturity);
    double dfCcyCounterAtMaturity = multicurve.getDiscountFactor(pair.getCounter(), maturity);
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
    return multicurve.getSimplyCompoundForwardRate(
        Legacy.iborIndex(index), relativeTime(fixingStartDate), relativeTime(fixingEndDate), fixingYearFraction);
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
    return multicurve.getSimplyCompoundForwardRate(
        Legacy.overnightIndex(index), relativeTime(fixingStartDate), relativeTime(fixingEndDate), fixingYearFraction);
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
    return multicurve.getSimplyCompoundForwardRate(
        Legacy.overnightIndex(index), relativeTime(startDate), relativeTime(endDate), fixingYearFraction);
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
    paramSensitivityIborRate(sensitivities, map);
    paramSensitivityOvernightRate(sensitivities, map);
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
      String curveName = multicurve.getName(ccy);
      SensitivityKey keyParam = NameCurrencySensitivityKey.of(curveName, ccy);
      double[] sensiParam = multicurve.parameterSensitivity(curveName, grouped.get(ccy));
      mutableMap.put(keyParam, sensiParam);
    }
  }

  // handle ibor rate sensitivities
  private void paramSensitivityIborRate(PointSensitivities sensitivities, Map<SensitivityKey, double[]> mutableMap) {
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
      String curveName = multicurve.getName(Legacy.iborIndex((IborIndex) key.getIndex()));
      SensitivityKey keyParam = NameCurrencySensitivityKey.of(curveName, key.getCurrency());
      double[] sensiParam = multicurve.parameterForwardSensitivity(curveName, grouped.get(key));
      mutableMap.merge(keyParam, sensiParam, ImmutablePricingEnvironment::combineArrays);
    }
  }

  // handle overnight rate sensitivities
  private void paramSensitivityOvernightRate(PointSensitivities sensitivities, Map<SensitivityKey, double[]> mutableMap) {
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
      String curveName = multicurve.getName(Legacy.overnightIndex((OvernightIndex) key.getIndex()));
      SensitivityKey keyParam = NameCurrencySensitivityKey.of(curveName, key.getCurrency());
      double[] sensiParam = multicurve.parameterForwardSensitivity(curveName, grouped.get(key));
      mutableMap.merge(keyParam, sensiParam, ImmutablePricingEnvironment::combineArrays);
    }
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
   * The meta-bean for {@code ImmutablePricingEnvironment}.
   * @return the meta-bean, not null
   */
  public static ImmutablePricingEnvironment.Meta meta() {
    return ImmutablePricingEnvironment.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutablePricingEnvironment.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutablePricingEnvironment.Builder builder() {
    return new ImmutablePricingEnvironment.Builder();
  }

  private ImmutablePricingEnvironment(
      LocalDate valuationDate,
      MulticurveProviderInterface multicurve,
      Map<Index, LocalDateDoubleTimeSeries> timeSeries,
      DayCount dayCount) {
    JodaBeanUtils.notNull(valuationDate, "valuationDate");
    JodaBeanUtils.notNull(multicurve, "multicurve");
    JodaBeanUtils.notNull(timeSeries, "timeSeries");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.valuationDate = valuationDate;
    this.multicurve = multicurve;
    this.timeSeries = ImmutableMap.copyOf(timeSeries);
    this.dayCount = dayCount;
  }

  @Override
  public ImmutablePricingEnvironment.Meta metaBean() {
    return ImmutablePricingEnvironment.Meta.INSTANCE;
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
   * All curves and other data items in this environment are calibrated for this date.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the multi-curve bundle.
   * @return the value of the property, not null
   */
  private MulticurveProviderInterface getMulticurve() {
    return multicurve;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the time-series.
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
      ImmutablePricingEnvironment other = (ImmutablePricingEnvironment) obj;
      return JodaBeanUtils.equal(getValuationDate(), other.getValuationDate()) &&
          JodaBeanUtils.equal(getMulticurve(), other.getMulticurve()) &&
          JodaBeanUtils.equal(getTimeSeries(), other.getTimeSeries()) &&
          JodaBeanUtils.equal(getDayCount(), other.getDayCount());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(getValuationDate());
    hash = hash * 31 + JodaBeanUtils.hashCode(getMulticurve());
    hash = hash * 31 + JodaBeanUtils.hashCode(getTimeSeries());
    hash = hash * 31 + JodaBeanUtils.hashCode(getDayCount());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("ImmutablePricingEnvironment{");
    buf.append("valuationDate").append('=').append(getValuationDate()).append(',').append(' ');
    buf.append("multicurve").append('=').append(getMulticurve()).append(',').append(' ');
    buf.append("timeSeries").append('=').append(getTimeSeries()).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(getDayCount()));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutablePricingEnvironment}.
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
        this, "valuationDate", ImmutablePricingEnvironment.class, LocalDate.class);
    /**
     * The meta-property for the {@code multicurve} property.
     */
    private final MetaProperty<MulticurveProviderInterface> multicurve = DirectMetaProperty.ofImmutable(
        this, "multicurve", ImmutablePricingEnvironment.class, MulticurveProviderInterface.class);
    /**
     * The meta-property for the {@code timeSeries} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableMap<Index, LocalDateDoubleTimeSeries>> timeSeries = DirectMetaProperty.ofImmutable(
        this, "timeSeries", ImmutablePricingEnvironment.class, (Class) ImmutableMap.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", ImmutablePricingEnvironment.class, DayCount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "valuationDate",
        "multicurve",
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
        case 1253345110:  // multicurve
          return multicurve;
        case 779431844:  // timeSeries
          return timeSeries;
        case 1905311443:  // dayCount
          return dayCount;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutablePricingEnvironment.Builder builder() {
      return new ImmutablePricingEnvironment.Builder();
    }

    @Override
    public Class<? extends ImmutablePricingEnvironment> beanType() {
      return ImmutablePricingEnvironment.class;
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
     * The meta-property for the {@code multicurve} property.
     * @return the meta-property, not null
     */
    public MetaProperty<MulticurveProviderInterface> multicurve() {
      return multicurve;
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
          return ((ImmutablePricingEnvironment) bean).getValuationDate();
        case 1253345110:  // multicurve
          return ((ImmutablePricingEnvironment) bean).getMulticurve();
        case 779431844:  // timeSeries
          return ((ImmutablePricingEnvironment) bean).getTimeSeries();
        case 1905311443:  // dayCount
          return ((ImmutablePricingEnvironment) bean).getDayCount();
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
   * The bean-builder for {@code ImmutablePricingEnvironment}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutablePricingEnvironment> {

    private LocalDate valuationDate;
    private MulticurveProviderInterface multicurve;
    private Map<Index, LocalDateDoubleTimeSeries> timeSeries = ImmutableMap.of();
    private DayCount dayCount;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ImmutablePricingEnvironment beanToCopy) {
      this.valuationDate = beanToCopy.getValuationDate();
      this.multicurve = beanToCopy.getMulticurve();
      this.timeSeries = beanToCopy.getTimeSeries();
      this.dayCount = beanToCopy.getDayCount();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 113107279:  // valuationDate
          return valuationDate;
        case 1253345110:  // multicurve
          return multicurve;
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
        case 1253345110:  // multicurve
          this.multicurve = (MulticurveProviderInterface) newValue;
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
    public ImmutablePricingEnvironment build() {
      return new ImmutablePricingEnvironment(
          valuationDate,
          multicurve,
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
     * Sets the {@code multicurve} property in the builder.
     * @param multicurve  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder multicurve(MulticurveProviderInterface multicurve) {
      JodaBeanUtils.notNull(multicurve, "multicurve");
      this.multicurve = multicurve;
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
      StringBuilder buf = new StringBuilder(160);
      buf.append("ImmutablePricingEnvironment.Builder{");
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
      buf.append("multicurve").append('=').append(JodaBeanUtils.toString(multicurve)).append(',').append(' ');
      buf.append("timeSeries").append('=').append(JodaBeanUtils.toString(timeSeries)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
