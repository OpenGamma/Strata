/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableConstructor;
import org.joda.beans.PropertyDefinition;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataId;
import com.opengamma.strata.data.MarketDataName;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.DiscountFactorsDecoratedForward;
import com.opengamma.strata.pricer.fx.FxForwardRates;
import com.opengamma.strata.pricer.fx.FxForwardRatesDecoratedForward;
import com.opengamma.strata.pricer.fx.FxIndexRates;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.OvernightIndexRates;
import com.opengamma.strata.pricer.rate.PriceIndexValues;
import com.opengamma.strata.pricer.rate.RatesProvider;
import java.util.Map;
import java.util.NoSuchElementException;
import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

/**
 * Rates provider build as an existing provider brought at a forward date by implied forward rates and discount factors.
 * <p>
 * Only the methods used for direct valuation are implemented. The methods with spread and the methods related
 * to sensitivities are not implemented.
 */
@BeanDefinition(builderScope = "private")
public class RatesProviderDecoratedForward 
    implements RatesProvider, ImmutableBean, Serializable {
  
  /** Underlying provider. */
  @PropertyDefinition(validate = "notNull")
  private final RatesProvider underlying;
  /** The forward rate. */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final LocalDate valuationDate;

  //-------------------------------------------------------------------------
  /**
   * Create a new {@link DiscountFactors} from an existing one and a forward date. 
   * <p>
   * The provider created as a valuation date at the forward date. The discount factors at a given date are the 
   * forward discount factors, i.e the ratio of the original discount factor at the date and the discount 
   * factor at the forward date.
   * 
   * @param underlying  the underlying rates provider
   * @param valuationDate  the valuation date for which the curve is valid
   * @return the discount factors
   */
  public static RatesProviderDecoratedForward of(RatesProvider underlying, LocalDate valuationDate) {
    return new RatesProviderDecoratedForward(underlying, valuationDate);
  }

  @ImmutableConstructor
  private RatesProviderDecoratedForward(RatesProvider underlying, LocalDate valuationDate) {
    this.underlying = ArgChecker.notNull(underlying, "underlying");
    this.valuationDate = ArgChecker.notNull(valuationDate, "valuation date");
  }

  @Override
  public double fxRate(Currency baseCurrency, Currency counterCurrency) {
    return underlying.fxForwardRates(CurrencyPair.of(baseCurrency, counterCurrency)).rate(baseCurrency, valuationDate);
  }

  @Override
  public DiscountFactors discountFactors(Currency currency) {
    return DiscountFactorsDecoratedForward.of(underlying.discountFactors(currency), valuationDate);
  }

  @Override
  public FxForwardRates fxForwardRates(CurrencyPair currencyPair) {
    return FxForwardRatesDecoratedForward.of(underlying.fxForwardRates(currencyPair), valuationDate);
  }

  @Override
  public IborIndexRates iborIndexRates(IborIndex index) {
    return underlying.iborIndexRates(index);
  }

  @Override
  public OvernightIndexRates overnightIndexRates(OvernightIndex index) {
    return underlying.overnightIndexRates(index);
  }

  @Override
  public Set<Currency> getDiscountCurrencies() {
    return underlying.getDiscountCurrencies();
  }

  @Override
  public Set<IborIndex> getIborIndices() {
    return underlying.getIborIndices();
  }

  @Override
  public Set<OvernightIndex> getOvernightIndices() {
    return underlying.getOvernightIndices();
  }

  @Override
  public Set<PriceIndex> getPriceIndices() {
    return underlying.getPriceIndices();
  }

  @Override
  public FxIndexRates fxIndexRates(FxIndex index) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public PriceIndexValues priceIndexValues(PriceIndex index) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public MultiCurrencyAmount currencyExposure(PointSensitivities pointSensitivities) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public LocalDateDoubleTimeSeries timeSeries(Index index) {
    throw new UnsupportedOperationException("Not implemented");
    // Do we need to add fixing of the days between original date and new date?
  }

  @Override
  public <T> T data(MarketDataId<T> id) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public <T> Optional<T> findData(MarketDataName<T> name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public ImmutableRatesProvider toImmutableRatesProvider() {
    throw new UnsupportedOperationException("Not implemented");
  }
  
  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code RatesProviderDecoratedForward}.
   * @return the meta-bean, not null
   */
  public static RatesProviderDecoratedForward.Meta meta() {
    return RatesProviderDecoratedForward.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(RatesProviderDecoratedForward.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  @Override
  public RatesProviderDecoratedForward.Meta metaBean() {
    return RatesProviderDecoratedForward.Meta.INSTANCE;
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
   * Gets underlying provider.
   * @return the value of the property, not null
   */
  public RatesProvider getUnderlying() {
    return underlying;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the forward rate.
   * @return the value of the property, not null
   */
  @Override
  public LocalDate getValuationDate() {
    return valuationDate;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      RatesProviderDecoratedForward other = (RatesProviderDecoratedForward) obj;
      return JodaBeanUtils.equal(underlying, other.underlying) &&
          JodaBeanUtils.equal(valuationDate, other.valuationDate);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(underlying);
    hash = hash * 31 + JodaBeanUtils.hashCode(valuationDate);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(96);
    buf.append("RatesProviderDecoratedForward{");
    int len = buf.length();
    toString(buf);
    if (buf.length() > len) {
      buf.setLength(buf.length() - 2);
    }
    buf.append('}');
    return buf.toString();
  }

  protected void toString(StringBuilder buf) {
    buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying)).append(',').append(' ');
    buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code RatesProviderDecoratedForward}.
   */
  public static class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code underlying} property.
     */
    private final MetaProperty<RatesProvider> underlying = DirectMetaProperty.ofImmutable(
        this, "underlying", RatesProviderDecoratedForward.class, RatesProvider.class);
    /**
     * The meta-property for the {@code valuationDate} property.
     */
    private final MetaProperty<LocalDate> valuationDate = DirectMetaProperty.ofImmutable(
        this, "valuationDate", RatesProviderDecoratedForward.class, LocalDate.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "underlying",
        "valuationDate");

    /**
     * Restricted constructor.
     */
    protected Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          return underlying;
        case 113107279:  // valuationDate
          return valuationDate;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends RatesProviderDecoratedForward> builder() {
      return new RatesProviderDecoratedForward.Builder();
    }

    @Override
    public Class<? extends RatesProviderDecoratedForward> beanType() {
      return RatesProviderDecoratedForward.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code underlying} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<RatesProvider> underlying() {
      return underlying;
    }

    /**
     * The meta-property for the {@code valuationDate} property.
     * @return the meta-property, not null
     */
    public final MetaProperty<LocalDate> valuationDate() {
      return valuationDate;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          return ((RatesProviderDecoratedForward) bean).getUnderlying();
        case 113107279:  // valuationDate
          return ((RatesProviderDecoratedForward) bean).getValuationDate();
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
   * The bean-builder for {@code RatesProviderDecoratedForward}.
   */
  private static class Builder extends DirectFieldsBeanBuilder<RatesProviderDecoratedForward> {

    private RatesProvider underlying;
    private LocalDate valuationDate;

    /**
     * Restricted constructor.
     */
    protected Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          return underlying;
        case 113107279:  // valuationDate
          return valuationDate;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -1770633379:  // underlying
          this.underlying = (RatesProvider) newValue;
          break;
        case 113107279:  // valuationDate
          this.valuationDate = (LocalDate) newValue;
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
    public RatesProviderDecoratedForward build() {
      return new RatesProviderDecoratedForward(
          underlying,
          valuationDate);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(96);
      buf.append("RatesProviderDecoratedForward.Builder{");
      int len = buf.length();
      toString(buf);
      if (buf.length() > len) {
        buf.setLength(buf.length() - 2);
      }
      buf.append('}');
      return buf.toString();
    }

    protected void toString(StringBuilder buf) {
      buf.append("underlying").append('=').append(JodaBeanUtils.toString(underlying)).append(',').append(' ');
      buf.append("valuationDate").append('=').append(JodaBeanUtils.toString(valuationDate)).append(',').append(' ');
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
