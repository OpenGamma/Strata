/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.sabr;

import static com.opengamma.sesame.sabr.SabrSurfaceSelector.SabrSurfaceName.ALPHA;
import static com.opengamma.sesame.sabr.SabrSurfaceSelector.SabrSurfaceName.BETA;
import static com.opengamma.sesame.sabr.SabrSurfaceSelector.SabrSurfaceName.NU;
import static com.opengamma.sesame.sabr.SabrSurfaceSelector.SabrSurfaceName.RHO;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import org.threeten.bp.Period;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.opengamma.analytics.financial.instrument.index.GeneratorSwapFixedIbor;
import com.opengamma.analytics.financial.instrument.index.IborIndex;
import com.opengamma.analytics.financial.model.option.definition.SABRInterestRateParameters;
import com.opengamma.analytics.math.interpolation.GridInterpolator2D;
import com.opengamma.analytics.math.interpolation.Interpolator1D;
import com.opengamma.analytics.math.interpolation.Interpolator2D;
import com.opengamma.analytics.math.surface.InterpolatedDoublesSurface;
import com.opengamma.core.link.ConfigLink;
import com.opengamma.financial.convention.SwapFixedLegConvention;
import com.opengamma.financial.convention.businessday.BusinessDayConventions;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.financial.convention.daycount.DayCounts;
import com.opengamma.financial.security.option.SwaptionSecurity;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.tuple.Triple;

/**
 * Config object holding links to other configuration
 * objects which define the data required for pricing
 * a swaption using SABR data.
 */
@BeanDefinition
public class SabrSwaptionConfig implements ImmutableBean {

  /**
   * Link pointing to a config object holding the SABR surface
   * data required for pricing swaptions.
   */
  @PropertyDefinition(validate = "notNull")
  private final ConfigLink<SabrSwaptionDataConfig> _sabrDataConfig;

  /**
   * Link pointing to a config object holding the interpolation
   * required for SABR surfaces.
   */
  @PropertyDefinition(validate = "notNull")
  private final ConfigLink<SabrSwaptionInterpolationConfig> _sabrInterpolationConfig;

  /**
   * Create the SABR configuration required to enable pricing
   * of the supplied swaption security.
   *
   * @param security the swaption to get SABR data for, not null
   * @return a result containing a the SABR configuration if
   * successful, a failure otherwise
   */
  public Result<SabrParametersConfiguration> createSABRParametersConfig(SwaptionSecurity security) {

    Interpolator2D interpolator = create2dInterpolator();

    Currency currency = security.getCurrency();
    SabrSwaptionDataConfig dataConfig = _sabrDataConfig.resolve();
    Result<SabrSurfaceSelector<SwapFixedLegConvention, SabrExpiryTenorSurface>> selectorResult =
        dataConfig.findSurfaceSelector(currency);

    if (selectorResult.isSuccess()) {

      SabrSurfaceSelector<SwapFixedLegConvention, SabrExpiryTenorSurface> selector = selectorResult.getValue();
      Result<SabrExpiryTenorSurface> alphaSurface = selector.findSabrSurface(ALPHA);
      Result<SabrExpiryTenorSurface> betaSurface = selector.findSabrSurface(BETA);
      Result<SabrExpiryTenorSurface> rhoSurface = selector.findSabrSurface(RHO);
      Result<SabrExpiryTenorSurface> nuSurface = selector.findSabrSurface(NU);

      if (Result.allSuccessful(alphaSurface, betaSurface, rhoSurface, nuSurface)) {

        InterpolatedDoublesSurface alpha = createSurface(alphaSurface.getValue(), interpolator);
        InterpolatedDoublesSurface beta = createSurface(betaSurface.getValue(), interpolator);
        InterpolatedDoublesSurface rho = createSurface(rhoSurface.getValue(), interpolator);
        InterpolatedDoublesSurface nu = createSurface(nuSurface.getValue(), interpolator);
        SABRInterestRateParameters params = new SABRInterestRateParameters(alpha, beta, rho, nu);

        SwapFixedLegConvention convention = selector.getConvention().resolve();

        return Result.success(new SabrParametersConfiguration(params, createSwapGenerator(convention)));
      } else {
        return Result.failure(alphaSurface, betaSurface, rhoSurface, nuSurface);
      }
    } else {
      return Result.failure(selectorResult);
    }
  }

  private Interpolator2D create2dInterpolator() {
    SabrSwaptionInterpolationConfig interpolationConfig = _sabrInterpolationConfig.resolve();

    Interpolator1D expiryInterpolator = interpolationConfig.createCombinedExpiryInterpolator();
    Interpolator1D tenorInterpolator = interpolationConfig.createCombinedTenorInterpolator();

    return new GridInterpolator2D(expiryInterpolator, tenorInterpolator);
  }

  private GeneratorSwapFixedIbor createSwapGenerator(SwapFixedLegConvention convention) {

    // This is ugly in the extreme. The swap generator we are creating is only needed
    // so that analytics code can read the fixed leg day count from it. Unfortunately,
    // we need a whole load of scaffolding to create an instance successfully. This
    // method provides the scaffolding.
    // TODO - PLAT-6441 should provide utilities to alleviate this

    // None of the inputs here matter as this index is never
    // actually used for anything apart from constructing the generator
    IborIndex dummyIndex = new IborIndex(Currency.USD, Period.ofMonths(6), 0,
                                         DayCounts.ACT_360, BusinessDayConventions.NONE, false, "dummy");

    // Similar to above, the only input that is actually used is the day count
    return new GeneratorSwapFixedIbor("dummy", Period.ofDays(0), convention.getDayCount(),
                                      dummyIndex, new MondayToFridayCalendar("dummy"));
  }

  private InterpolatedDoublesSurface createSurface(SabrExpiryTenorSurface sabrExpiryTenorSurface,
                                                   Interpolator2D interpolator) {

    List<Triple<Double, Double, Double>> xyzData = Lists.transform(sabrExpiryTenorSurface.getSabrData(),
        new Function<SabrNode, Triple<Double, Double, Double>>() {
          @Override
          public Triple<Double, Double, Double> apply(SabrNode input) {
            return Triple.of(input.getX(), input.getY(), input.getZ());
          }
        });
    return new InterpolatedDoublesSurface(xyzData, interpolator);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SabrSwaptionConfig}.
   * @return the meta-bean, not null
   */
  public static SabrSwaptionConfig.Meta meta() {
    return SabrSwaptionConfig.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SabrSwaptionConfig.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static SabrSwaptionConfig.Builder builder() {
    return new SabrSwaptionConfig.Builder();
  }

  /**
   * Restricted constructor.
   * @param builder  the builder to copy from, not null
   */
  protected SabrSwaptionConfig(SabrSwaptionConfig.Builder builder) {
    JodaBeanUtils.notNull(builder._sabrDataConfig, "sabrDataConfig");
    JodaBeanUtils.notNull(builder._sabrInterpolationConfig, "sabrInterpolationConfig");
    this._sabrDataConfig = builder._sabrDataConfig;
    this._sabrInterpolationConfig = builder._sabrInterpolationConfig;
  }

  @Override
  public SabrSwaptionConfig.Meta metaBean() {
    return SabrSwaptionConfig.Meta.INSTANCE;
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
   * Gets link pointing to a config object holding the SABR surface
   * data required for pricing swaptions.
   * @return the value of the property, not null
   */
  public ConfigLink<SabrSwaptionDataConfig> getSabrDataConfig() {
    return _sabrDataConfig;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets link pointing to a config object holding the interpolation
   * required for SABR surfaces.
   * @return the value of the property, not null
   */
  public ConfigLink<SabrSwaptionInterpolationConfig> getSabrInterpolationConfig() {
    return _sabrInterpolationConfig;
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
      SabrSwaptionConfig other = (SabrSwaptionConfig) obj;
      return JodaBeanUtils.equal(getSabrDataConfig(), other.getSabrDataConfig()) &&
          JodaBeanUtils.equal(getSabrInterpolationConfig(), other.getSabrInterpolationConfig());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash += hash * 31 + JodaBeanUtils.hashCode(getSabrDataConfig());
    hash += hash * 31 + JodaBeanUtils.hashCode(getSabrInterpolationConfig());
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("SabrSwaptionConfig{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("sabrDataConfig").append('=').append(JodaBeanUtils.toString(getSabrDataConfig())).append(',').append(' ');
    buf.append("sabrInterpolationConfig").append('=').append(JodaBeanUtils.toString(getSabrInterpolationConfig())).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SabrSwaptionConfig}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code sabrDataConfig} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ConfigLink<SabrSwaptionDataConfig>> _sabrDataConfig = DirectMetaProperty.ofImmutable(
        this, "sabrDataConfig", SabrSwaptionConfig.class, (Class) ConfigLink.class);
    /**
     * The meta-property for the {@code sabrInterpolationConfig} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ConfigLink<SabrSwaptionInterpolationConfig>> _sabrInterpolationConfig = DirectMetaProperty.ofImmutable(
        this, "sabrInterpolationConfig", SabrSwaptionConfig.class, (Class) ConfigLink.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "sabrDataConfig",
        "sabrInterpolationConfig");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1116125878:  // sabrDataConfig
          return _sabrDataConfig;
        case -1777261048:  // sabrInterpolationConfig
          return _sabrInterpolationConfig;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public SabrSwaptionConfig.Builder builder() {
      return new SabrSwaptionConfig.Builder();
    }

    @Override
    public Class<? extends SabrSwaptionConfig> beanType() {
      return SabrSwaptionConfig.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code sabrDataConfig} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ConfigLink<SabrSwaptionDataConfig>> sabrDataConfig() {
      return _sabrDataConfig;
    }

    /**
     * The meta-property for the {@code sabrInterpolationConfig} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<ConfigLink<SabrSwaptionInterpolationConfig>> sabrInterpolationConfig() {
      return _sabrInterpolationConfig;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1116125878:  // sabrDataConfig
          return ((SabrSwaptionConfig) bean).getSabrDataConfig();
        case -1777261048:  // sabrInterpolationConfig
          return ((SabrSwaptionConfig) bean).getSabrInterpolationConfig();
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
   * The bean-builder for {@code SabrSwaptionConfig}.
   */
  public static class Builder extends DirectFieldsBeanBuilder<SabrSwaptionConfig> {

    private ConfigLink<SabrSwaptionDataConfig> _sabrDataConfig;
    private ConfigLink<SabrSwaptionInterpolationConfig> _sabrInterpolationConfig;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    protected Builder(SabrSwaptionConfig beanToCopy) {
      this._sabrDataConfig = beanToCopy.getSabrDataConfig();
      this._sabrInterpolationConfig = beanToCopy.getSabrInterpolationConfig();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1116125878:  // sabrDataConfig
          return _sabrDataConfig;
        case -1777261048:  // sabrInterpolationConfig
          return _sabrInterpolationConfig;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1116125878:  // sabrDataConfig
          this._sabrDataConfig = (ConfigLink<SabrSwaptionDataConfig>) newValue;
          break;
        case -1777261048:  // sabrInterpolationConfig
          this._sabrInterpolationConfig = (ConfigLink<SabrSwaptionInterpolationConfig>) newValue;
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
    public SabrSwaptionConfig build() {
      return new SabrSwaptionConfig(this);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the {@code sabrDataConfig} property in the builder.
     * @param sabrDataConfig  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder sabrDataConfig(ConfigLink<SabrSwaptionDataConfig> sabrDataConfig) {
      JodaBeanUtils.notNull(sabrDataConfig, "sabrDataConfig");
      this._sabrDataConfig = sabrDataConfig;
      return this;
    }

    /**
     * Sets the {@code sabrInterpolationConfig} property in the builder.
     * @param sabrInterpolationConfig  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder sabrInterpolationConfig(ConfigLink<SabrSwaptionInterpolationConfig> sabrInterpolationConfig) {
      JodaBeanUtils.notNull(sabrInterpolationConfig, "sabrInterpolationConfig");
      this._sabrInterpolationConfig = sabrInterpolationConfig;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("SabrSwaptionConfig.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("sabrDataConfig").append('=').append(JodaBeanUtils.toString(_sabrDataConfig)).append(',').append(' ');
      buf.append("sabrInterpolationConfig").append('=').append(JodaBeanUtils.toString(_sabrInterpolationConfig)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
