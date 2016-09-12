/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * A market convention for Ibor-Ibor swap trades.
 * <p>
 * This defines the market convention for a Ibor-Ibor single currency swap.
 * The convention is formed by combining two swap leg conventions in the same currency.
 * <p>
 * The market price is for the difference (spread) between the values of the two legs.
 * This convention has two legs, the "spread leg" and the "flat leg". The spread will be
 * added to the "spread leg", which is typically the leg with the shorter underlying tenor.
 * The payment frequency is typically determined by the longer underlying tenor, with
 * compounding applied.
 * <p>
 * For example, a 'USD 3s1s' basis swap has 'USD-LIBOR-1M' as the spread leg and 'USD-LIBOR-3M'
 * as the flat leg. Payment is every 3 months, with the one month leg compounded.
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
public final class ImmutableIborIborSwapConvention
    implements IborIborSwapConvention, ImmutableBean, Serializable {

  /**
   * The convention name, such as 'USD-LIBOR-3M-LIBOR-6M'.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final String name;
  /**
   * The market convention of the floating leg that has the spread applied.
   * <p>
   * The spread is the market price of the instrument.
   * It is added to the observed interest rate.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborRateSwapLegConvention spreadLeg;
  /**
   * The market convention of the floating leg that does not have the spread applied.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final IborRateSwapLegConvention flatLeg;
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
   * The spot date offset is set to be the effective date offset of the index of the spread leg.
   * 
   * @param name  the unique name of the convention 
   * @param spreadLeg  the market convention for the leg that the spread is added to
   * @param flatLeg  the market convention for the other leg, known as the flat leg
   * @return the convention
   */
  public static ImmutableIborIborSwapConvention of(
      String name,
      IborRateSwapLegConvention spreadLeg,
      IborRateSwapLegConvention flatLeg) {

    return of(name, spreadLeg, flatLeg, spreadLeg.getIndex().getEffectiveDateOffset());
  }

  /**
   * Obtains a convention based on the specified name and leg conventions.
   * <p>
   * The two leg conventions must be in the same currency.
   * 
   * @param name  the unique name of the convention 
   * @param spreadLeg  the market convention for the leg that the spread is added to
   * @param flatLeg  the market convention for the other leg, known as the flat leg
   * @param spotDateOffset  the offset of the spot value date from the trade date
   * @return the convention
   */
  public static ImmutableIborIborSwapConvention of(
      String name,
      IborRateSwapLegConvention spreadLeg,
      IborRateSwapLegConvention flatLeg,
      DaysAdjustment spotDateOffset) {

    return new ImmutableIborIborSwapConvention(name, spreadLeg, flatLeg, spotDateOffset);
  }

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(spreadLeg.getCurrency().equals(flatLeg.getCurrency()), "Conventions must have same currency");
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
    SwapLeg leg1 = spreadLeg.toLeg(startDate, endDate, PayReceive.ofPay(buySell.isBuy()), notional, spread);
    SwapLeg leg2 = flatLeg.toLeg(startDate, endDate, PayReceive.ofPay(buySell.isSell()), notional);
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
   * The meta-bean for {@code ImmutableIborIborSwapConvention}.
   * @return the meta-bean, not null
   */
  public static ImmutableIborIborSwapConvention.Meta meta() {
    return ImmutableIborIborSwapConvention.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(ImmutableIborIborSwapConvention.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ImmutableIborIborSwapConvention.Builder builder() {
    return new ImmutableIborIborSwapConvention.Builder();
  }

  private ImmutableIborIborSwapConvention(
      String name,
      IborRateSwapLegConvention spreadLeg,
      IborRateSwapLegConvention flatLeg,
      DaysAdjustment spotDateOffset) {
    JodaBeanUtils.notNull(name, "name");
    JodaBeanUtils.notNull(spreadLeg, "spreadLeg");
    JodaBeanUtils.notNull(flatLeg, "flatLeg");
    JodaBeanUtils.notNull(spotDateOffset, "spotDateOffset");
    this.name = name;
    this.spreadLeg = spreadLeg;
    this.flatLeg = flatLeg;
    this.spotDateOffset = spotDateOffset;
    validate();
  }

  @Override
  public ImmutableIborIborSwapConvention.Meta metaBean() {
    return ImmutableIborIborSwapConvention.Meta.INSTANCE;
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
   * Gets the convention name, such as 'USD-LIBOR-3M-LIBOR-6M'.
   * @return the value of the property, not null
   */
  @Override
  public String getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the floating leg that has the spread applied.
   * <p>
   * The spread is the market price of the instrument.
   * It is added to the observed interest rate.
   * @return the value of the property, not null
   */
  @Override
  public IborRateSwapLegConvention getSpreadLeg() {
    return spreadLeg;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the market convention of the floating leg that does not have the spread applied.
   * @return the value of the property, not null
   */
  @Override
  public IborRateSwapLegConvention getFlatLeg() {
    return flatLeg;
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
      ImmutableIborIborSwapConvention other = (ImmutableIborIborSwapConvention) obj;
      return JodaBeanUtils.equal(name, other.name) &&
          JodaBeanUtils.equal(spreadLeg, other.spreadLeg) &&
          JodaBeanUtils.equal(flatLeg, other.flatLeg) &&
          JodaBeanUtils.equal(spotDateOffset, other.spotDateOffset);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(name);
    hash = hash * 31 + JodaBeanUtils.hashCode(spreadLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(flatLeg);
    hash = hash * 31 + JodaBeanUtils.hashCode(spotDateOffset);
    return hash;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ImmutableIborIborSwapConvention}.
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
        this, "name", ImmutableIborIborSwapConvention.class, String.class);
    /**
     * The meta-property for the {@code spreadLeg} property.
     */
    private final MetaProperty<IborRateSwapLegConvention> spreadLeg = DirectMetaProperty.ofImmutable(
        this, "spreadLeg", ImmutableIborIborSwapConvention.class, IborRateSwapLegConvention.class);
    /**
     * The meta-property for the {@code flatLeg} property.
     */
    private final MetaProperty<IborRateSwapLegConvention> flatLeg = DirectMetaProperty.ofImmutable(
        this, "flatLeg", ImmutableIborIborSwapConvention.class, IborRateSwapLegConvention.class);
    /**
     * The meta-property for the {@code spotDateOffset} property.
     */
    private final MetaProperty<DaysAdjustment> spotDateOffset = DirectMetaProperty.ofImmutable(
        this, "spotDateOffset", ImmutableIborIborSwapConvention.class, DaysAdjustment.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "spreadLeg",
        "flatLeg",
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
        case 1302781851:  // spreadLeg
          return spreadLeg;
        case -778843179:  // flatLeg
          return flatLeg;
        case 746995843:  // spotDateOffset
          return spotDateOffset;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ImmutableIborIborSwapConvention.Builder builder() {
      return new ImmutableIborIborSwapConvention.Builder();
    }

    @Override
    public Class<? extends ImmutableIborIborSwapConvention> beanType() {
      return ImmutableIborIborSwapConvention.class;
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
     * The meta-property for the {@code spreadLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateSwapLegConvention> spreadLeg() {
      return spreadLeg;
    }

    /**
     * The meta-property for the {@code flatLeg} property.
     * @return the meta-property, not null
     */
    public MetaProperty<IborRateSwapLegConvention> flatLeg() {
      return flatLeg;
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
          return ((ImmutableIborIborSwapConvention) bean).getName();
        case 1302781851:  // spreadLeg
          return ((ImmutableIborIborSwapConvention) bean).getSpreadLeg();
        case -778843179:  // flatLeg
          return ((ImmutableIborIborSwapConvention) bean).getFlatLeg();
        case 746995843:  // spotDateOffset
          return ((ImmutableIborIborSwapConvention) bean).getSpotDateOffset();
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
   * The bean-builder for {@code ImmutableIborIborSwapConvention}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ImmutableIborIborSwapConvention> {

    private String name;
    private IborRateSwapLegConvention spreadLeg;
    private IborRateSwapLegConvention flatLeg;
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
    private Builder(ImmutableIborIborSwapConvention beanToCopy) {
      this.name = beanToCopy.getName();
      this.spreadLeg = beanToCopy.getSpreadLeg();
      this.flatLeg = beanToCopy.getFlatLeg();
      this.spotDateOffset = beanToCopy.getSpotDateOffset();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case 1302781851:  // spreadLeg
          return spreadLeg;
        case -778843179:  // flatLeg
          return flatLeg;
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
        case 1302781851:  // spreadLeg
          this.spreadLeg = (IborRateSwapLegConvention) newValue;
          break;
        case -778843179:  // flatLeg
          this.flatLeg = (IborRateSwapLegConvention) newValue;
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
    public ImmutableIborIborSwapConvention build() {
      return new ImmutableIborIborSwapConvention(
          name,
          spreadLeg,
          flatLeg,
          spotDateOffset);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the convention name, such as 'USD-LIBOR-3M-LIBOR-6M'.
     * @param name  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder name(String name) {
      JodaBeanUtils.notNull(name, "name");
      this.name = name;
      return this;
    }

    /**
     * Sets the market convention of the floating leg that has the spread applied.
     * <p>
     * The spread is the market price of the instrument.
     * It is added to the observed interest rate.
     * @param spreadLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder spreadLeg(IborRateSwapLegConvention spreadLeg) {
      JodaBeanUtils.notNull(spreadLeg, "spreadLeg");
      this.spreadLeg = spreadLeg;
      return this;
    }

    /**
     * Sets the market convention of the floating leg that does not have the spread applied.
     * @param flatLeg  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder flatLeg(IborRateSwapLegConvention flatLeg) {
      JodaBeanUtils.notNull(flatLeg, "flatLeg");
      this.flatLeg = flatLeg;
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
      buf.append("ImmutableIborIborSwapConvention.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("spreadLeg").append('=').append(JodaBeanUtils.toString(spreadLeg)).append(',').append(' ');
      buf.append("flatLeg").append('=').append(JodaBeanUtils.toString(flatLeg)).append(',').append(' ');
      buf.append("spotDateOffset").append('=').append(JodaBeanUtils.toString(spotDateOffset));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
