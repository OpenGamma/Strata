/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
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

import com.opengamma.strata.basics.PutCall;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivities;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.market.surface.NodalSurface;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivities;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceUnitParameterSensitivity;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.impl.option.SabrInterestRateParameters;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Volatility environment for swaptions in the SABR model. 
 * <p>
 * The volatility is represented in terms of SABR model parameters.
 * <p>
 * The {@code surfaceCurrencyParameterSensitivity()}, {@code priceGamma()} and
 * {@code priceTheta()} methods are not implemented.
 */
@BeanDefinition(builderScope = "private")
public final class SabrParametersSwaptionVolatilities
    implements SabrSwaptionVolatilities, ImmutableBean {

  /** 
   * The SABR model parameters. 
   * <p>
   * Each model parameter of SABR model is a surface in the expiry/swap tenor dimensions. 
   */
  @PropertyDefinition(validate = "notNull")
  private final SabrInterestRateParameters parameters;
  /** 
   * The swap convention. 
   * <p>
   * The data must valid in terms of this swap convention. 
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FixedIborSwapConvention convention;
  /** 
   * The valuation date-time. 
   * <p>
   * All data items in this environment are calibrated for this date-time. 
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ZonedDateTime valuationDateTime;
  /** 
   * The day count applicable to the model. 
   */
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the SABR model parameters and the date-time for which it is valid.
   * 
   * @param parameters  the SABR model parameters
   * @param convention  the swap convention for which the data is valid
   * @param valuationDateTime  the valuation date-time
   * @param dayCount  the day count applicable to the model
   * @return the volatilities
   */
  public static SabrParametersSwaptionVolatilities of(
      SabrInterestRateParameters parameters,
      FixedIborSwapConvention convention,
      ZonedDateTime valuationDateTime,
      DayCount dayCount) {

    return new SabrParametersSwaptionVolatilities(parameters, convention, valuationDateTime, dayCount);
  }

  /**
   * Obtains an instance from the SABR model parameters and the date, time and zone for which it is valid.
   * 
   * @param parameters  the SABR model parameters
   * @param convention  the swap convention for which the data is valid
   * @param valuationDate  the valuation date
   * @param valuationTime  the valuation time
   * @param valuationZone  the valuation time zone
   * @param dayCount  the day count applicable to the model
   * @return the volatilities
   */
  public static SabrParametersSwaptionVolatilities of(
      SabrInterestRateParameters parameters,
      FixedIborSwapConvention convention,
      LocalDate valuationDate,
      LocalTime valuationTime,
      ZoneId valuationZone,
      DayCount dayCount) {

    return of(parameters, convention, valuationDate.atTime(valuationTime).atZone(valuationZone), dayCount);
  }

  //-------------------------------------------------------------------------
  @Override
  public double volatility(double expiry, double tenor, double strike, double forwardRate) {
    return parameters.volatility(expiry, tenor, strike, forwardRate);
  }

  @Override
  public ValueDerivatives volatilityAdjoint(double expiry, double tenor, double strike, double forward) {
    return parameters.volatilityAdjoint(expiry, tenor, strike, forward);
  }

  @Override
  public double shift(double expiry, double tenor) {
    return parameters.shift(expiry, tenor);
  }

  @Override
  public SurfaceCurrencyParameterSensitivity surfaceCurrencyParameterSensitivity(SwaptionSensitivity pointSensitivity) {
    throw new UnsupportedOperationException("Sensitivity is based on SwaptionSabrSensitivity, not SwaptionSensitivity");
  }

  /**
   * Calculates the surface parameter sensitivities from the point sensitivities.
   * <p>
   * This is used to convert a set of point sensitivities to surface parameter sensitivities.
   * 
   * @param pointSensitivities  the point sensitivities to convert
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  public SurfaceCurrencyParameterSensitivities surfaceCurrencyParameterSensitivity(
      SwaptionSabrSensitivities pointSensitivities) {

    List<SurfaceCurrencyParameterSensitivity> sensitivitiesTotal =
        pointSensitivities.getSensitivities()
            .stream()
            .map(pointSensitivity -> surfaceCurrencyParameterSensitivity(pointSensitivity).getSensitivities())
            .flatMap(list -> list.stream())
            .collect(Collectors.toList());
    return SurfaceCurrencyParameterSensitivities.of(sensitivitiesTotal);
  }

  /**
   * Calculates the surface parameter sensitivities from the point sensitivity.
   * <p>
   * This is used to convert a single point sensitivity to surface parameter sensitivities.
   * 
   * @param pointSensitivity  the point sensitivity to convert
   * @return the parameter sensitivity
   * @throws RuntimeException if the result cannot be calculated
   */
  public SurfaceCurrencyParameterSensitivities surfaceCurrencyParameterSensitivity(SwaptionSabrSensitivity pointSensitivity) {
    ArgChecker.isTrue(pointSensitivity.getConvention().equals(convention),
        "Swap convention of provider must be the same as swap convention of swaption sensitivity");
    double expiry = relativeTime(pointSensitivity.getExpiry());
    double tenor = pointSensitivity.getTenor();
    SurfaceCurrencyParameterSensitivity alphaSensi = surfaceCurrencyParameterSensitivity(
        parameters.getAlphaSurface(), pointSensitivity.getCurrency(), pointSensitivity.getAlphaSensitivity(), expiry, tenor);
    SurfaceCurrencyParameterSensitivity betaSensi = surfaceCurrencyParameterSensitivity(
        parameters.getBetaSurface(), pointSensitivity.getCurrency(), pointSensitivity.getBetaSensitivity(), expiry, tenor);
    SurfaceCurrencyParameterSensitivity rhoSensi = surfaceCurrencyParameterSensitivity(
        parameters.getRhoSurface(), pointSensitivity.getCurrency(), pointSensitivity.getRhoSensitivity(), expiry, tenor);
    SurfaceCurrencyParameterSensitivity nuSensi = surfaceCurrencyParameterSensitivity(
        parameters.getNuSurface(), pointSensitivity.getCurrency(), pointSensitivity.getNuSensitivity(), expiry, tenor);
    return SurfaceCurrencyParameterSensitivities.of(alphaSensi, betaSensi, rhoSensi, nuSensi);
  }

  private SurfaceCurrencyParameterSensitivity surfaceCurrencyParameterSensitivity(
      NodalSurface surface,
      Currency currency,
      double factor,
      double expiry,
      double tenor) {

    SurfaceUnitParameterSensitivity unitSens = surface.zValueParameterSensitivity(expiry, tenor);
    return unitSens.multipliedBy(currency, factor);
  }

  //-------------------------------------------------------------------------
  @Override
  public double price(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    double shift = parameters.shift(expiry, tenor);
    return BlackFormulaRepository.price(forward + shift, strike + shift, expiry, volatility, putCall.isCall());
  }

  @Override
  public double priceDelta(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    double shift = parameters.shift(expiry, tenor);
    return BlackFormulaRepository.delta(forward + shift, strike + shift, expiry, volatility, putCall.isCall());
  }

  @Override
  public double priceGamma(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    throw new UnsupportedOperationException("SABR model does not support this method");
  }

  @Override
  public double priceTheta(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    throw new UnsupportedOperationException("SABR model does not support this method");
  }

  @Override
  public double priceVega(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    double shift = parameters.shift(expiry, tenor);
    return BlackFormulaRepository.vega(forward + shift, strike + shift, expiry, volatility);
  }

  //-------------------------------------------------------------------------
  @Override
  public double relativeTime(ZonedDateTime dateTime) {
    ArgChecker.notNull(dateTime, "dateTime");
    LocalDate valuationDate = valuationDateTime.toLocalDate();
    LocalDate date = dateTime.toLocalDate();
    return dayCount.relativeYearFraction(valuationDate, date);
  }

  @Override
  public double tenor(LocalDate startDate, LocalDate endDate) {
    // rounded number of months. the rounding is to ensure that an integer number of year even with holidays/leap year
    return Math.round((endDate.toEpochDay() - startDate.toEpochDay()) / 365.25 * 12) / 12;
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SabrParametersSwaptionVolatilities}.
   * @return the meta-bean, not null
   */
  public static SabrParametersSwaptionVolatilities.Meta meta() {
    return SabrParametersSwaptionVolatilities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SabrParametersSwaptionVolatilities.Meta.INSTANCE);
  }

  private SabrParametersSwaptionVolatilities(
      SabrInterestRateParameters parameters,
      FixedIborSwapConvention convention,
      ZonedDateTime valuationDateTime,
      DayCount dayCount) {
    JodaBeanUtils.notNull(parameters, "parameters");
    JodaBeanUtils.notNull(convention, "convention");
    JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    this.parameters = parameters;
    this.convention = convention;
    this.valuationDateTime = valuationDateTime;
    this.dayCount = dayCount;
  }

  @Override
  public SabrParametersSwaptionVolatilities.Meta metaBean() {
    return SabrParametersSwaptionVolatilities.Meta.INSTANCE;
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
   * Gets the SABR model parameters.
   * <p>
   * Each model parameter of SABR model is a surface in the expiry/swap tenor dimensions.
   * @return the value of the property, not null
   */
  public SabrInterestRateParameters getParameters() {
    return parameters;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the swap convention.
   * <p>
   * The data must valid in terms of this swap convention.
   * @return the value of the property, not null
   */
  @Override
  public FixedIborSwapConvention getConvention() {
    return convention;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the valuation date-time.
   * <p>
   * All data items in this environment are calibrated for this date-time.
   * @return the value of the property, not null
   */
  @Override
  public ZonedDateTime getValuationDateTime() {
    return valuationDateTime;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count applicable to the model.
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SabrParametersSwaptionVolatilities other = (SabrParametersSwaptionVolatilities) obj;
      return JodaBeanUtils.equal(parameters, other.parameters) &&
          JodaBeanUtils.equal(convention, other.convention) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime) &&
          JodaBeanUtils.equal(dayCount, other.dayCount);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(parameters);
    hash = hash * 31 + JodaBeanUtils.hashCode(convention);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("SabrParametersSwaptionVolatilities{");
    buf.append("parameters").append('=').append(parameters).append(',').append(' ');
    buf.append("convention").append('=').append(convention).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(valuationDateTime).append(',').append(' ');
    buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SabrParametersSwaptionVolatilities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code parameters} property.
     */
    private final MetaProperty<SabrInterestRateParameters> parameters = DirectMetaProperty.ofImmutable(
        this, "parameters", SabrParametersSwaptionVolatilities.class, SabrInterestRateParameters.class);
    /**
     * The meta-property for the {@code convention} property.
     */
    private final MetaProperty<FixedIborSwapConvention> convention = DirectMetaProperty.ofImmutable(
        this, "convention", SabrParametersSwaptionVolatilities.class, FixedIborSwapConvention.class);
    /**
     * The meta-property for the {@code valuationDateTime} property.
     */
    private final MetaProperty<ZonedDateTime> valuationDateTime = DirectMetaProperty.ofImmutable(
        this, "valuationDateTime", SabrParametersSwaptionVolatilities.class, ZonedDateTime.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", SabrParametersSwaptionVolatilities.class, DayCount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "parameters",
        "convention",
        "valuationDateTime",
        "dayCount");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 458736106:  // parameters
          return parameters;
        case 2039569265:  // convention
          return convention;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case 1905311443:  // dayCount
          return dayCount;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SabrParametersSwaptionVolatilities> builder() {
      return new SabrParametersSwaptionVolatilities.Builder();
    }

    @Override
    public Class<? extends SabrParametersSwaptionVolatilities> beanType() {
      return SabrParametersSwaptionVolatilities.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code parameters} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SabrInterestRateParameters> parameters() {
      return parameters;
    }

    /**
     * The meta-property for the {@code convention} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FixedIborSwapConvention> convention() {
      return convention;
    }

    /**
     * The meta-property for the {@code valuationDateTime} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ZonedDateTime> valuationDateTime() {
      return valuationDateTime;
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
        case 458736106:  // parameters
          return ((SabrParametersSwaptionVolatilities) bean).getParameters();
        case 2039569265:  // convention
          return ((SabrParametersSwaptionVolatilities) bean).getConvention();
        case -949589828:  // valuationDateTime
          return ((SabrParametersSwaptionVolatilities) bean).getValuationDateTime();
        case 1905311443:  // dayCount
          return ((SabrParametersSwaptionVolatilities) bean).getDayCount();
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
   * The bean-builder for {@code SabrParametersSwaptionVolatilities}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SabrParametersSwaptionVolatilities> {

    private SabrInterestRateParameters parameters;
    private FixedIborSwapConvention convention;
    private ZonedDateTime valuationDateTime;
    private DayCount dayCount;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 458736106:  // parameters
          return parameters;
        case 2039569265:  // convention
          return convention;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case 1905311443:  // dayCount
          return dayCount;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 458736106:  // parameters
          this.parameters = (SabrInterestRateParameters) newValue;
          break;
        case 2039569265:  // convention
          this.convention = (FixedIborSwapConvention) newValue;
          break;
        case -949589828:  // valuationDateTime
          this.valuationDateTime = (ZonedDateTime) newValue;
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
    public SabrParametersSwaptionVolatilities build() {
      return new SabrParametersSwaptionVolatilities(
          parameters,
          convention,
          valuationDateTime,
          dayCount);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("SabrParametersSwaptionVolatilities.Builder{");
      buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters)).append(',').append(' ');
      buf.append("convention").append('=').append(JodaBeanUtils.toString(convention)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
