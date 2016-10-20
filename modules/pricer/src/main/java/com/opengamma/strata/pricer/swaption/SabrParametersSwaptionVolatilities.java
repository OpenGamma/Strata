/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.model.SabrParameterType;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.market.param.ParameterPerturbation;
import com.opengamma.strata.market.param.UnitParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.surface.Surface;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.model.SabrInterestRateParameters;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Volatility environment for swaptions in the SABR model.
 * <p>
 * The volatility is represented in terms of SABR model parameters.
 * <p>
 * The {@code parameterSensitivity()}, {@code priceGamma()} and {@code priceTheta()} methods are not implemented.
 */
@BeanDefinition
public final class SabrParametersSwaptionVolatilities
    implements SabrSwaptionVolatilities, ImmutableBean, Serializable {
  // To represent the data sensitivities (dataSensitivityAlpha, ...), DoubleMatrix does not work as the
  // number of raw data can be different for each element of the list.

  /**
   * The name.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final SwaptionVolatilitiesName name;
  /**
   * The swap convention that the volatilities are to be used for.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final FixedIborSwapConvention convention;
  /**
   * The valuation date-time.
   * <p>
   * The volatilities are calibrated for this date-time.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final ZonedDateTime valuationDateTime;
  /**
   * The SABR model parameters.
   * <p>
   * Each model parameter of SABR model is a surface.
   * The x-value of the surface is the expiry, as a year fraction.
   * The y-value of the surface is the swap tenor, as a year fraction rounded to the month.
   */
  @PropertyDefinition(validate = "notNull")
  private final SabrInterestRateParameters parameters;
  /**
   * The sensitivity of the Alpha parameters to the raw data used for calibration.
   * <p>
   * The order of the sensitivities have to be coherent with the surface parameter metadata.
   */
  @PropertyDefinition(get = "optional")
  private final ImmutableList<DoubleArray> dataSensitivityAlpha;
  /**
   * The sensitivity of the Beta parameters to the raw data used for calibration.
   * <p>
   * The order of the sensitivities have to be coherent with the surface parameter metadata.
   */
  @PropertyDefinition(get = "optional")
  private final ImmutableList<DoubleArray> dataSensitivityBeta;
  /**
   * The sensitivity of the Rho parameters to the raw data used for calibration.
   * <p>
   * The order of the sensitivities have to be coherent with the surface parameter metadata.
   */
  @PropertyDefinition(get = "optional")
  private final ImmutableList<DoubleArray> dataSensitivityRho;
  /**
   * The sensitivity of the Nu parameters to the raw data used for calibration.
   * <p>
   * The order of the sensitivities have to be coherent with the surface parameter metadata.
   */
  @PropertyDefinition(get = "optional")
  private final ImmutableList<DoubleArray> dataSensitivityNu;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the SABR model parameters and the date-time for which it is valid.
   * 
   * @param name  the name
   * @param convention  the swap convention that the volatilities are to be used for
   * @param valuationDateTime  the valuation date-time
   * @param parameters  the SABR model parameters
   * @return the volatilities
   */
  public static SabrParametersSwaptionVolatilities of(
      SwaptionVolatilitiesName name,
      FixedIborSwapConvention convention,
      ZonedDateTime valuationDateTime,
      SabrInterestRateParameters parameters) {

    return new SabrParametersSwaptionVolatilities(name, convention, valuationDateTime, parameters, null, null, null, null);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the day count used to calculate the expiry year fraction.
   * 
   * @return the day count
   */
  public DayCount getDayCount() {
    return getParameters().getDayCount();
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    if (parameters.getAlphaSurface().getName().equals(name)) {
      return Optional.of(name.getMarketDataType().cast(parameters.getAlphaSurface()));
    }
    if (parameters.getBetaSurface().getName().equals(name)) {
      return Optional.of(name.getMarketDataType().cast(parameters.getBetaSurface()));
    }
    if (parameters.getRhoSurface().getName().equals(name)) {
      return Optional.of(name.getMarketDataType().cast(parameters.getRhoSurface()));
    }
    if (parameters.getNuSurface().getName().equals(name)) {
      return Optional.of(name.getMarketDataType().cast(parameters.getNuSurface()));
    }
    if (parameters.getShiftSurface().getName().equals(name)) {
      return Optional.of(name.getMarketDataType().cast(parameters.getShiftSurface()));
    }
    return Optional.empty();
  }

  @Override
  public int getParameterCount() {
    return parameters.getParameterCount();
  }

  @Override
  public double getParameter(int parameterIndex) {
    return parameters.getParameter(parameterIndex);
  }

  @Override
  public ParameterMetadata getParameterMetadata(int parameterIndex) {
    return parameters.getParameterMetadata(parameterIndex);
  }

  @Override
  public SabrParametersSwaptionVolatilities withParameter(int parameterIndex, double newValue) {
    SabrInterestRateParameters updated = parameters.withParameter(parameterIndex, newValue);
    return new SabrParametersSwaptionVolatilities(
        name,
        convention,
        valuationDateTime,
        updated,
        dataSensitivityAlpha,
        dataSensitivityBeta,
        dataSensitivityRho,
        dataSensitivityNu);
  }

  @Override
  public SabrParametersSwaptionVolatilities withPerturbation(ParameterPerturbation perturbation) {
    SabrInterestRateParameters updated = parameters.withPerturbation(perturbation);
    return new SabrParametersSwaptionVolatilities(
        name,
        convention,
        valuationDateTime,
        updated,
        dataSensitivityAlpha,
        dataSensitivityBeta,
        dataSensitivityRho,
        dataSensitivityNu);
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
  public double alpha(double expiry, double tenor) {
    return parameters.alpha(expiry, tenor);
  }

  @Override
  public double beta(double expiry, double tenor) {
    return parameters.beta(expiry, tenor);
  }

  @Override
  public double rho(double expiry, double tenor) {
    return parameters.rho(expiry, tenor);
  }

  @Override
  public double nu(double expiry, double tenor) {
    return parameters.nu(expiry, tenor);
  }

  @Override
  public double shift(double expiry, double tenor) {
    return parameters.shift(expiry, tenor);
  }

  @Override
  public CurrencyParameterSensitivities parameterSensitivity(PointSensitivities pointSensitivities) {
    CurrencyParameterSensitivities sens = CurrencyParameterSensitivities.empty();
    for (PointSensitivity point : pointSensitivities.getSensitivities()) {
      if (point instanceof SwaptionSabrSensitivity) {
        SwaptionSabrSensitivity pt = (SwaptionSabrSensitivity) point;
        if (pt.getVolatilitiesName().equals(getName())) {
          sens = sens.combinedWith(parameterSensitivity(pt));
        }
      }
    }
    return sens;
  }

  // convert a single point sensitivity
  private CurrencyParameterSensitivity parameterSensitivity(SwaptionSabrSensitivity point) {
    Surface surface = getSurface(point.getSensitivityType());
    double expiry = point.getExpiry();
    UnitParameterSensitivity unitSens = surface.zValueParameterSensitivity(expiry, point.getTenor());
    return unitSens.multipliedBy(point.getCurrency(), point.getSensitivity());
  }

  // find surface
  private Surface getSurface(SabrParameterType type) {
    switch (type) {
      case ALPHA:
        return parameters.getAlphaSurface();
      case BETA:
        return parameters.getBetaSurface();
      case RHO:
        return parameters.getRhoSurface();
      case NU:
        return parameters.getNuSurface();
      case SHIFT:
        return parameters.getShiftSurface();
      default:
        throw new IllegalStateException("Invalid enum value");
    }
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
    double shift = parameters.shift(expiry, tenor);
    return BlackFormulaRepository.gamma(forward + shift, strike + shift, expiry, volatility);
  }

  @Override
  public double priceTheta(double expiry, double tenor, PutCall putCall, double strike, double forward, double volatility) {
    double shift = parameters.shift(expiry, tenor);
    return BlackFormulaRepository.driftlessTheta(forward + shift, strike + shift, expiry, volatility);
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
    return getDayCount().relativeYearFraction(valuationDate, date);
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

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static SabrParametersSwaptionVolatilities.Builder builder() {
    return new SabrParametersSwaptionVolatilities.Builder();
  }

  private SabrParametersSwaptionVolatilities(
      SwaptionVolatilitiesName name,
      FixedIborSwapConvention convention,
      ZonedDateTime valuationDateTime,
      SabrInterestRateParameters parameters,
      List<DoubleArray> dataSensitivityAlpha,
      List<DoubleArray> dataSensitivityBeta,
      List<DoubleArray> dataSensitivityRho,
      List<DoubleArray> dataSensitivityNu) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(convention, "convention");
    JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
    JodaBeanUtils.notNull(parameters, "parameters");
    this.name = name;
    this.convention = convention;
    this.valuationDateTime = valuationDateTime;
    this.parameters = parameters;
    this.dataSensitivityAlpha = (dataSensitivityAlpha != null ? ImmutableList.copyOf(dataSensitivityAlpha) : null);
    this.dataSensitivityBeta = (dataSensitivityBeta != null ? ImmutableList.copyOf(dataSensitivityBeta) : null);
    this.dataSensitivityRho = (dataSensitivityRho != null ? ImmutableList.copyOf(dataSensitivityRho) : null);
    this.dataSensitivityNu = (dataSensitivityNu != null ? ImmutableList.copyOf(dataSensitivityNu) : null);
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
   * Gets the name.
   * @return the value of the property, not null
   */
  @Override
  public SwaptionVolatilitiesName getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the swap convention that the volatilities are to be used for.
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
   * The volatilities are calibrated for this date-time.
   * @return the value of the property, not null
   */
  @Override
  public ZonedDateTime getValuationDateTime() {
    return valuationDateTime;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the SABR model parameters.
   * <p>
   * Each model parameter of SABR model is a surface.
   * The x-value of the surface is the expiry, as a year fraction.
   * The y-value of the surface is the swap tenor, as a year fraction rounded to the month.
   * @return the value of the property, not null
   */
  public SabrInterestRateParameters getParameters() {
    return parameters;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the sensitivity of the Alpha parameters to the raw data used for calibration.
   * <p>
   * The order of the sensitivities have to be coherent with the surface parameter metadata.
   * @return the optional value of the property, not null
   */
  public Optional<ImmutableList<DoubleArray>> getDataSensitivityAlpha() {
    return Optional.ofNullable(dataSensitivityAlpha);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the sensitivity of the Beta parameters to the raw data used for calibration.
   * <p>
   * The order of the sensitivities have to be coherent with the surface parameter metadata.
   * @return the optional value of the property, not null
   */
  public Optional<ImmutableList<DoubleArray>> getDataSensitivityBeta() {
    return Optional.ofNullable(dataSensitivityBeta);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the sensitivity of the Rho parameters to the raw data used for calibration.
   * <p>
   * The order of the sensitivities have to be coherent with the surface parameter metadata.
   * @return the optional value of the property, not null
   */
  public Optional<ImmutableList<DoubleArray>> getDataSensitivityRho() {
    return Optional.ofNullable(dataSensitivityRho);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the sensitivity of the Nu parameters to the raw data used for calibration.
   * <p>
   * The order of the sensitivities have to be coherent with the surface parameter metadata.
   * @return the optional value of the property, not null
   */
  public Optional<ImmutableList<DoubleArray>> getDataSensitivityNu() {
    return Optional.ofNullable(dataSensitivityNu);
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
      SabrParametersSwaptionVolatilities other = (SabrParametersSwaptionVolatilities) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(convention, other.convention) &&
          JodaBeanUtils.equal(valuationDateTime, other.valuationDateTime) &&
          JodaBeanUtils.equal(parameters, other.parameters) &&
          JodaBeanUtils.equal(dataSensitivityAlpha, other.dataSensitivityAlpha) &&
          JodaBeanUtils.equal(dataSensitivityBeta, other.dataSensitivityBeta) &&
          JodaBeanUtils.equal(dataSensitivityRho, other.dataSensitivityRho) &&
          JodaBeanUtils.equal(dataSensitivityNu, other.dataSensitivityNu);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(convention);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDateTime);
    hash = hash * 31 + JodaBeanUtils.hashCode(parameters);
    hash = hash * 31 + JodaBeanUtils.hashCode(dataSensitivityAlpha);
    hash = hash * 31 + JodaBeanUtils.hashCode(dataSensitivityBeta);
    hash = hash * 31 + JodaBeanUtils.hashCode(dataSensitivityRho);
    hash = hash * 31 + JodaBeanUtils.hashCode(dataSensitivityNu);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(288);
    buf.append("SabrParametersSwaptionVolatilities{");
    buf.append("name").append('=').append(name).append(',').append(' ');
    buf.append("convention").append('=').append(convention).append(',').append(' ');
    buf.append("valuationDateTime").append('=').append(valuationDateTime).append(',').append(' ');
    buf.append("parameters").append('=').append(parameters).append(',').append(' ');
    buf.append("dataSensitivityAlpha").append('=').append(dataSensitivityAlpha).append(',').append(' ');
    buf.append("dataSensitivityBeta").append('=').append(dataSensitivityBeta).append(',').append(' ');
    buf.append("dataSensitivityRho").append('=').append(dataSensitivityRho).append(',').append(' ');
    buf.append("dataSensitivityNu").append('=').append(JodaBeanUtils.toString(dataSensitivityNu));
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
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<SwaptionVolatilitiesName> name = DirectMetaProperty.ofImmutable(
        this, "name", SabrParametersSwaptionVolatilities.class, SwaptionVolatilitiesName.class);
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
     * The meta-property for the {@code parameters} property.
     */
    private final MetaProperty<SabrInterestRateParameters> parameters = DirectMetaProperty.ofImmutable(
        this, "parameters", SabrParametersSwaptionVolatilities.class, SabrInterestRateParameters.class);
    /**
     * The meta-property for the {@code dataSensitivityAlpha} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<DoubleArray>> dataSensitivityAlpha = DirectMetaProperty.ofImmutable(
        this, "dataSensitivityAlpha", SabrParametersSwaptionVolatilities.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code dataSensitivityBeta} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<DoubleArray>> dataSensitivityBeta = DirectMetaProperty.ofImmutable(
        this, "dataSensitivityBeta", SabrParametersSwaptionVolatilities.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code dataSensitivityRho} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<DoubleArray>> dataSensitivityRho = DirectMetaProperty.ofImmutable(
        this, "dataSensitivityRho", SabrParametersSwaptionVolatilities.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code dataSensitivityNu} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<DoubleArray>> dataSensitivityNu = DirectMetaProperty.ofImmutable(
        this, "dataSensitivityNu", SabrParametersSwaptionVolatilities.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "convention",
        "valuationDateTime",
        "parameters",
        "dataSensitivityAlpha",
        "dataSensitivityBeta",
        "dataSensitivityRho",
        "dataSensitivityNu");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 2039569265:  // convention
          return convention;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case 458736106:  // parameters
          return parameters;
        case 1650101705:  // dataSensitivityAlpha
          return dataSensitivityAlpha;
        case -85295067:  // dataSensitivityBeta
          return dataSensitivityBeta;
        case 967095332:  // dataSensitivityRho
          return dataSensitivityRho;
        case -1077182148:  // dataSensitivityNu
          return dataSensitivityNu;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public SabrParametersSwaptionVolatilities.Builder builder() {
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
     * The meta-property for the {@code name} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SwaptionVolatilitiesName> name() {
      return name;
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
     * The meta-property for the {@code parameters} property.
     * @return the meta-property, not null
     */
    public MetaProperty<SabrInterestRateParameters> parameters() {
      return parameters;
    }

    /**
     * The meta-property for the {@code dataSensitivityAlpha} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<DoubleArray>> dataSensitivityAlpha() {
      return dataSensitivityAlpha;
    }

    /**
     * The meta-property for the {@code dataSensitivityBeta} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<DoubleArray>> dataSensitivityBeta() {
      return dataSensitivityBeta;
    }

    /**
     * The meta-property for the {@code dataSensitivityRho} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<DoubleArray>> dataSensitivityRho() {
      return dataSensitivityRho;
    }

    /**
     * The meta-property for the {@code dataSensitivityNu} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<DoubleArray>> dataSensitivityNu() {
      return dataSensitivityNu;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((SabrParametersSwaptionVolatilities) bean).getName();
        case 2039569265:  // convention
          return ((SabrParametersSwaptionVolatilities) bean).getConvention();
        case -949589828:  // valuationDateTime
          return ((SabrParametersSwaptionVolatilities) bean).getValuationDateTime();
        case 458736106:  // parameters
          return ((SabrParametersSwaptionVolatilities) bean).getParameters();
        case 1650101705:  // dataSensitivityAlpha
          return ((SabrParametersSwaptionVolatilities) bean).dataSensitivityAlpha;
        case -85295067:  // dataSensitivityBeta
          return ((SabrParametersSwaptionVolatilities) bean).dataSensitivityBeta;
        case 967095332:  // dataSensitivityRho
          return ((SabrParametersSwaptionVolatilities) bean).dataSensitivityRho;
        case -1077182148:  // dataSensitivityNu
          return ((SabrParametersSwaptionVolatilities) bean).dataSensitivityNu;
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
  public static final class Builder extends DirectFieldsBeanBuilder<SabrParametersSwaptionVolatilities> {

    private SwaptionVolatilitiesName name;
    private FixedIborSwapConvention convention;
    private ZonedDateTime valuationDateTime;
    private SabrInterestRateParameters parameters;
    private List<DoubleArray> dataSensitivityAlpha;
    private List<DoubleArray> dataSensitivityBeta;
    private List<DoubleArray> dataSensitivityRho;
    private List<DoubleArray> dataSensitivityNu;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(SabrParametersSwaptionVolatilities beanToCopy) {
      this.name = beanToCopy.getName();
      this.convention = beanToCopy.getConvention();
      this.valuationDateTime = beanToCopy.getValuationDateTime();
      this.parameters = beanToCopy.getParameters();
      this.dataSensitivityAlpha = beanToCopy.dataSensitivityAlpha;
      this.dataSensitivityBeta = beanToCopy.dataSensitivityBeta;
      this.dataSensitivityRho = beanToCopy.dataSensitivityRho;
      this.dataSensitivityNu = beanToCopy.dataSensitivityNu;
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 2039569265:  // convention
          return convention;
        case -949589828:  // valuationDateTime
          return valuationDateTime;
        case 458736106:  // parameters
          return parameters;
        case 1650101705:  // dataSensitivityAlpha
          return dataSensitivityAlpha;
        case -85295067:  // dataSensitivityBeta
          return dataSensitivityBeta;
        case 967095332:  // dataSensitivityRho
          return dataSensitivityRho;
        case -1077182148:  // dataSensitivityNu
          return dataSensitivityNu;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (SwaptionVolatilitiesName) newValue;
          break;
        case 2039569265:  // convention
          this.convention = (FixedIborSwapConvention) newValue;
          break;
        case -949589828:  // valuationDateTime
          this.valuationDateTime = (ZonedDateTime) newValue;
          break;
        case 458736106:  // parameters
          this.parameters = (SabrInterestRateParameters) newValue;
          break;
        case 1650101705:  // dataSensitivityAlpha
          this.dataSensitivityAlpha = (List<DoubleArray>) newValue;
          break;
        case -85295067:  // dataSensitivityBeta
          this.dataSensitivityBeta = (List<DoubleArray>) newValue;
          break;
        case 967095332:  // dataSensitivityRho
          this.dataSensitivityRho = (List<DoubleArray>) newValue;
          break;
        case -1077182148:  // dataSensitivityNu
          this.dataSensitivityNu = (List<DoubleArray>) newValue;
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
          name,
          convention,
          valuationDateTime,
          parameters,
          dataSensitivityAlpha,
          dataSensitivityBeta,
          dataSensitivityRho,
          dataSensitivityNu);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the name.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(SwaptionVolatilitiesName name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the swap convention that the volatilities are to be used for.
     * @param convention  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder convention(FixedIborSwapConvention convention) {
      JodaBeanUtils.notNull(convention, "convention");
      this.convention = convention;
      return this;
    }

    /**
     * Sets the valuation date-time.
     * <p>
     * The volatilities are calibrated for this date-time.
     * @param valuationDateTime  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder valuationDateTime(ZonedDateTime valuationDateTime) {
      JodaBeanUtils.notNull(valuationDateTime, "valuationDateTime");
      this.valuationDateTime = valuationDateTime;
      return this;
    }

    /**
     * Sets the SABR model parameters.
     * <p>
     * Each model parameter of SABR model is a surface.
     * The x-value of the surface is the expiry, as a year fraction.
     * The y-value of the surface is the swap tenor, as a year fraction rounded to the month.
     * @param parameters  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder parameters(SabrInterestRateParameters parameters) {
      JodaBeanUtils.notNull(parameters, "parameters");
      this.parameters = parameters;
      return this;
    }

    /**
     * Sets the sensitivity of the Alpha parameters to the raw data used for calibration.
     * <p>
     * The order of the sensitivities have to be coherent with the surface parameter metadata.
     * @param dataSensitivityAlpha  the new value
     * @return this, for chaining, not null
     */
    public Builder dataSensitivityAlpha(List<DoubleArray> dataSensitivityAlpha) {
      this.dataSensitivityAlpha = dataSensitivityAlpha;
      return this;
    }

    /**
     * Sets the {@code dataSensitivityAlpha} property in the builder
     * from an array of objects.
     * @param dataSensitivityAlpha  the new value
     * @return this, for chaining, not null
     */
    public Builder dataSensitivityAlpha(DoubleArray... dataSensitivityAlpha) {
      return dataSensitivityAlpha(ImmutableList.copyOf(dataSensitivityAlpha));
    }

    /**
     * Sets the sensitivity of the Beta parameters to the raw data used for calibration.
     * <p>
     * The order of the sensitivities have to be coherent with the surface parameter metadata.
     * @param dataSensitivityBeta  the new value
     * @return this, for chaining, not null
     */
    public Builder dataSensitivityBeta(List<DoubleArray> dataSensitivityBeta) {
      this.dataSensitivityBeta = dataSensitivityBeta;
      return this;
    }

    /**
     * Sets the {@code dataSensitivityBeta} property in the builder
     * from an array of objects.
     * @param dataSensitivityBeta  the new value
     * @return this, for chaining, not null
     */
    public Builder dataSensitivityBeta(DoubleArray... dataSensitivityBeta) {
      return dataSensitivityBeta(ImmutableList.copyOf(dataSensitivityBeta));
    }

    /**
     * Sets the sensitivity of the Rho parameters to the raw data used for calibration.
     * <p>
     * The order of the sensitivities have to be coherent with the surface parameter metadata.
     * @param dataSensitivityRho  the new value
     * @return this, for chaining, not null
     */
    public Builder dataSensitivityRho(List<DoubleArray> dataSensitivityRho) {
      this.dataSensitivityRho = dataSensitivityRho;
      return this;
    }

    /**
     * Sets the {@code dataSensitivityRho} property in the builder
     * from an array of objects.
     * @param dataSensitivityRho  the new value
     * @return this, for chaining, not null
     */
    public Builder dataSensitivityRho(DoubleArray... dataSensitivityRho) {
      return dataSensitivityRho(ImmutableList.copyOf(dataSensitivityRho));
    }

    /**
     * Sets the sensitivity of the Nu parameters to the raw data used for calibration.
     * <p>
     * The order of the sensitivities have to be coherent with the surface parameter metadata.
     * @param dataSensitivityNu  the new value
     * @return this, for chaining, not null
     */
    public Builder dataSensitivityNu(List<DoubleArray> dataSensitivityNu) {
      this.dataSensitivityNu = dataSensitivityNu;
      return this;
    }

    /**
     * Sets the {@code dataSensitivityNu} property in the builder
     * from an array of objects.
     * @param dataSensitivityNu  the new value
     * @return this, for chaining, not null
     */
    public Builder dataSensitivityNu(DoubleArray... dataSensitivityNu) {
      return dataSensitivityNu(ImmutableList.copyOf(dataSensitivityNu));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(288);
      buf.append("SabrParametersSwaptionVolatilities.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("convention").append('=').append(JodaBeanUtils.toString(convention)).append(',').append(' ');
      buf.append("valuationDateTime").append('=').append(JodaBeanUtils.toString(valuationDateTime)).append(',').append(' ');
      buf.append("parameters").append('=').append(JodaBeanUtils.toString(parameters)).append(',').append(' ');
      buf.append("dataSensitivityAlpha").append('=').append(JodaBeanUtils.toString(dataSensitivityAlpha)).append(',').append(' ');
      buf.append("dataSensitivityBeta").append('=').append(JodaBeanUtils.toString(dataSensitivityBeta)).append(',').append(' ');
      buf.append("dataSensitivityRho").append('=').append(JodaBeanUtils.toString(dataSensitivityRho)).append(',').append(' ');
      buf.append("dataSensitivityNu").append('=').append(JodaBeanUtils.toString(dataSensitivityNu));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
