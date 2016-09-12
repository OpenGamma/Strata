/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.joda.beans.Bean;
import org.joda.beans.BeanDefinition;
import org.joda.beans.ImmutableBean;
import org.joda.beans.ImmutableValidator;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.Property;
import org.joda.beans.PropertyDefinition;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PayReceive;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLeg;
import com.opengamma.strata.product.swap.SwapTrade;

/**
 * A market convention for Fixed-Overnight swap trades.
 * <p>
 * This defines the market convention for a Fixed-Overnight single currency swap.
 * This is often known as an <i>OIS swap</i>, although <i>Fed Fund swaps</i> are also covered.
 * The convention is formed by combining two swap leg conventions in the same currency.
 * <p>
 * The convention is defined by four key dates.
 * <ul>
 * <li>Trade date, the date that the trade is agreed
 * <li>Spot date, the base for date calculations, typically 2 business days after the trade date
 * <li>Start date, the date on which the interest calculation starts, often the same as the spot date
 * <li>End date, the date on which the interest calculation ends, typically a number of years after the start date
 * </ul>
 */
@BeanDefinition
public final class ImmutableOvernightIborSwapConvention
    implements OvernightIborSwapConvention, ImmutableBean, Serializable {

  /**
   * The convention name, such as 'USD-FED-FUND-AA-LIBOR-3M'.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final String name;
  /**
   * The market convention of the floating leg.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final OvernightRateSwapLegConvention overnightLeg;
  /**
   * The market convention of the floating leg.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborRateSwapLegConvention iborLeg;
  /**
   * The offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date to find the start date.
   * A typical value is "plus 2 business days".
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final DaysAdjustment spotDateOffset;

  //-------------------------------------------------------------------------
  /**
   * Obtains a convention based on the specified name and leg conventions.
   * <p>
   * The two leg conventions must be in the same currency.
   * 
   * @param name  the unique name of the convention 
   * @param overnightLeg  the market convention for the overnight leg
   * @param iborLeg  the market convention for the ibor leg
   * @param spotDateOffset  the offset of the spot value date from the trade date
   * @return the convention
   */
  public static ImmutableOvernightIborSwapConvention of(
      String name,
      OvernightRateSwapLegConvention overnightLeg,
      IborRateSwapLegConvention iborLeg,
      DaysAdjustment spotDateOffset) {

    return new ImmutableOvernightIborSwapConvention(name, overnightLeg, iborLeg, spotDateOffset);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(overnightLeg.getCurrency().equals(iborLeg.getCurrency()), "Conventions must have same currency");
  }

  //-------------------------------------------------------------------------
  @Override
  public SwapTrade toTrade(
      TradeInfo tradeInfo,
      LocalDate startDate,
      LocalDate endDate,
      BuySell buySell,
      double notional,
      double spread) {

    Optional<LocalDate> tradeDate = tradeInfo.getTradeDate();
    if (tradeDate.isPresent()) {
      ArgChecker.inOrderOrEqual(tradeDate.get(), startDate, "tradeDate", "startDate");
    }
    SwapLeg leg1 = overnightLeg.toLeg(startDate, endDate, PayReceive.ofPay(buySell.isBuy()), notional, spread);
    SwapLeg leg2 = iborLeg.toLeg(startDate, endDate, PayReceive.ofPay(buySell.isSell()), notional);
    return SwapTrade.builder()
        .info(tradeInfo)
        .product(Swap.of(leg1, leg2))
        .build();
  }

  //-------------------------------------------------------------------------
  @Override
  public String toString() {
    return getName();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code ImmutableOvernightIborSwapConvention}.
   * @return the meta-bean, not null
   */
  public static ImmutableOvernightIborSwapConvention.Meta meta() {
    return ImmutableOvernightIborSwapConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableOvernightIborSwapConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableOvernightIborSwapConvention.Builder builder() {
    return new ImmutableOvernightIborSwapConvention.Builder();
  }

  private ImmutableOvernightIborSwapConvention(
      String name,
      OvernightRateSwapLegConvention overnightLeg,
      IborRateSwapLegConvention iborLeg,
      DaysAdjustment spotDateOffset) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(overnightLeg, "overnightLeg");
    JodaBeanUtils.notNull(iborLeg, "iborLeg");
    JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
    this.name = name;
    this.overnightLeg = overnightLeg;
    this.iborLeg = iborLeg;
    this.spotDateOffset = spotDateOffset;
    validate();
  }

  @Override
  public ImmutableOvernightIborSwapConvention.Meta metaBean() {
    return ImmutableOvernightIborSwapConvention.Meta.INSTANCE;
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
   * Gets the convention name, such as 'USD-FED-FUND-AA-LIBOR-3M'.
   * @return the value of the property, not null
   */
  @Override
  public String getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the floating leg.
   * @return the value of the property, not null
   */
  @Override
  public OvernightRateSwapLegConvention getOvernightLeg() {
    return overnightLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the floating leg.
   * @return the value of the property, not null
   */
  @Override
  public IborRateSwapLegConvention getIborLeg() {
    return iborLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the offset of the spot value date from the trade date.
   * <p>
   * The offset is applied to the trade date to find the start date.
   * A typical value is "plus 2 business days".
   * @return the value of the property, not null
   */
  @Override
  public DaysAdjustment getSpotDateOffset() {
    return spotDateOffset;
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
      ImmutableOvernightIborSwapConvention other = (ImmutableOvernightIborSwapConvention) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(overnightLeg, other.overnightLeg) &&
          JodaBeanUtils.equal(iborLeg, other.iborLeg) &&
          JodaBeanUtils.equal(spotDateOffset, other.spotDateOffset);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(overnightLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(iborLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(spotDateOffset);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableOvernightIborSwapConvention}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code name} property.
     */
    private final MetaProperty<String> name = DirectMetaProperty.ofImmutable(
        this, "name", ImmutableOvernightIborSwapConvention.class, String.class);
    /**
     * The meta-property for the {@code overnightLeg} property.
     */
    private final MetaProperty<OvernightRateSwapLegConvention> overnightLeg = DirectMetaProperty.ofImmutable(
        this, "overnightLeg", ImmutableOvernightIborSwapConvention.class, OvernightRateSwapLegConvention.class);
    /**
     * The meta-property for the {@code iborLeg} property.
     */
    private final MetaProperty<IborRateSwapLegConvention> iborLeg = DirectMetaProperty.ofImmutable(
        this, "iborLeg", ImmutableOvernightIborSwapConvention.class, IborRateSwapLegConvention.class);
    /**
     * The meta-property for the {@code spotDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> spotDateOffset = DirectMetaProperty.ofImmutable(
        this, "spotDateOffset", ImmutableOvernightIborSwapConvention.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "overnightLeg",
        "iborLeg",
        "spotDateOffset");

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
        case 1774606250:  // overnightLeg
          return overnightLeg;
        case 1610246066:  // iborLeg
          return iborLeg;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableOvernightIborSwapConvention.Builder builder() {
      return new ImmutableOvernightIborSwapConvention.Builder();
    }

    @Override
    public Class<? extends ImmutableOvernightIborSwapConvention> beanType() {
      return ImmutableOvernightIborSwapConvention.class;
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
    public MetaProperty<String> name() {
      return name;
    }

    /**
     * The meta-property for the {@code overnightLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<OvernightRateSwapLegConvention> overnightLeg() {
      return overnightLeg;
    }

    /**
     * The meta-property for the {@code iborLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateSwapLegConvention> iborLeg() {
      return iborLeg;
    }

    /**
     * The meta-property for the {@code spotDateOffset} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DaysAdjustment> spotDateOffset() {
      return spotDateOffset;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((ImmutableOvernightIborSwapConvention) bean).getName();
        case 1774606250:  // overnightLeg
          return ((ImmutableOvernightIborSwapConvention) bean).getOvernightLeg();
        case 1610246066:  // iborLeg
          return ((ImmutableOvernightIborSwapConvention) bean).getIborLeg();
        case 746995843:  // spotDateOffset
          return ((ImmutableOvernightIborSwapConvention) bean).getSpotDateOffset();
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
   * The bean-builder for {@code ImmutableOvernightIborSwapConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableOvernightIborSwapConvention> {

    private String name;
    private OvernightRateSwapLegConvention overnightLeg;
    private IborRateSwapLegConvention iborLeg;
    private DaysAdjustment spotDateOffset;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ImmutableOvernightIborSwapConvention beanToCopy) {
      this.name = beanToCopy.getName();
      this.overnightLeg = beanToCopy.getOvernightLeg();
      this.iborLeg = beanToCopy.getIborLeg();
      this.spotDateOffset = beanToCopy.getSpotDateOffset();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 1774606250:  // overnightLeg
          return overnightLeg;
        case 1610246066:  // iborLeg
          return iborLeg;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          this.name = (String) newValue;
          break;
        case 1774606250:  // overnightLeg
          this.overnightLeg = (OvernightRateSwapLegConvention) newValue;
          break;
        case 1610246066:  // iborLeg
          this.iborLeg = (IborRateSwapLegConvention) newValue;
          break;
        case 746995843:  // spotDateOffset
          this.spotDateOffset = (DaysAdjustment) newValue;
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
    public ImmutableOvernightIborSwapConvention build() {
      return new ImmutableOvernightIborSwapConvention(
          name,
          overnightLeg,
          iborLeg,
          spotDateOffset);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the convention name, such as 'USD-FED-FUND-AA-LIBOR-3M'.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the market convention of the floating leg.
     * @param overnightLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder overnightLeg(OvernightRateSwapLegConvention overnightLeg) {
      JodaBeanUtils.notNull(overnightLeg, "overnightLeg");
      this.overnightLeg = overnightLeg;
      return this;
    }

    /**
     * Sets the market convention of the floating leg.
     * @param iborLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder iborLeg(IborRateSwapLegConvention iborLeg) {
      JodaBeanUtils.notNull(iborLeg, "iborLeg");
      this.iborLeg = iborLeg;
      return this;
    }

    /**
     * Sets the offset of the spot value date from the trade date.
     * <p>
     * The offset is applied to the trade date to find the start date.
     * A typical value is "plus 2 business days".
     * @param spotDateOffset  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spotDateOffset(DaysAdjustment spotDateOffset) {
      JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
      this.spotDateOffset = spotDateOffset;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("ImmutableOvernightIborSwapConvention.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("overnightLeg").append('=').append(JodaBeanUtils.toString(overnightLeg)).append(',').append(' ');
      buf.append("iborLeg").append('=').append(JodaBeanUtils.toString(iborLeg)).append(',').append(' ');
      buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
