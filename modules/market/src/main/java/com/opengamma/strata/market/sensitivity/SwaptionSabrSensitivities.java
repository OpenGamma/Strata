/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.sensitivity;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.FxConvertible;
import com.opengamma.strata.basics.currency.FxRateProvider;

/**
 * Sensitivities of swaptions to SABR model parameters.
 * <p>
 * Contains a list of {@link SwaptionSabrSensitivity} each of which holds sensitivity values
 * to the grid points of the SABR parameters.
 */
@BeanDefinition(builderScope = "private")
public final class SwaptionSabrSensitivities
    implements FxConvertible<SwaptionSabrSensitivities>, ImmutableBean, Serializable {

  /**
   * The swaption SABR sensitivities.
   * <p>
   * Each entry includes details of the surface it relates to.
   */
  @PropertyDefinition(validate = "notNull")
  private final ImmutableList<SwaptionSabrSensitivity> sensitivities;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance with the specified sensitivities.
   * 
   * @param sensitivities  the list of sensitivities
   * @return the instance with the sensitivities
   */
  public static SwaptionSabrSensitivities of(List<SwaptionSabrSensitivity> sensitivities) {
    List<SwaptionSabrSensitivity> mutableList = new ArrayList<SwaptionSabrSensitivity>(sensitivities);
    return new SwaptionSabrSensitivities(mutableList);
  }

  /**
   * Obtains an instance with the specified sensitivity.
   * 
   * @param sensitivity  the sensitivity to add
   * @return the instance with the sensitivity
   */
  public static SwaptionSabrSensitivities of(SwaptionSabrSensitivity sensitivity) {
    return new SwaptionSabrSensitivities(Arrays.asList(sensitivity));
  }

  /**
   * Obtains an empty instance.
   * <p>
   * @return the empty instance
   */
  public static SwaptionSabrSensitivities empty() {
    return new SwaptionSabrSensitivities(ImmutableList.of());
  }

  //-------------------------------------------------------------------------
  /**
   * Adds a swaption SABR sensitivity.
   * <p>
   * A list will be created with the new sensitivity added.
   * The created list will be not normalized. Use {@code normalizeSensitivities(List)} if necessary.
   * 
   * @param sensitivity  the sensitivity to add
   * @return the instance with the new sensitivity added.
   */
  public SwaptionSabrSensitivities add(SwaptionSabrSensitivity sensitivity) {
    List<SwaptionSabrSensitivity> mutableList = new ArrayList<SwaptionSabrSensitivity>(sensitivities);
    mutableList.add(sensitivity);
    return new SwaptionSabrSensitivities(mutableList);
  }

  /**
   * Combines with swaption SABR sensitivities.
   * <p>
   * A list will be created with the new sensitivities added.
   * The created list will be not normalized. Use {@code normalizeSensitivities(List)} if necessary.
   * 
   * @param other  the sensitivities to add
   * @return the instance with the new sensitivities added.
   */
  public SwaptionSabrSensitivities combine(SwaptionSabrSensitivities other) {
    List<SwaptionSabrSensitivity> mutableList =
        Stream.concat(sensitivities.stream(), other.getSensitivities().stream()).collect(Collectors.toList());
    return new SwaptionSabrSensitivities(mutableList);
  }

  /**
   * Normalizes the sensitivities. 
   * <p>
   * Swaption SABR sensitivity objects are combined into a single object if they have the same key. 
   * 
   * @return the instance with the sensitivities normalized.
   */
  public SwaptionSabrSensitivities normalize() {
    List<SwaptionSabrSensitivity> mutableList = new ArrayList<SwaptionSabrSensitivity>(sensitivities);
    normalizeSensitivities(mutableList);
    return new SwaptionSabrSensitivities(mutableList);
  }

  // combines entries with the same key
  private static void normalizeSensitivities(List<SwaptionSabrSensitivity> mutableList) {
    mutableList.sort(SwaptionSabrSensitivity::compareKey);
    SwaptionSabrSensitivity previous = mutableList.get(0);
    for (int i = 1; i < mutableList.size(); i++) {
      SwaptionSabrSensitivity current = mutableList.get(i);
      if (current.compareKey(previous) == 0) {
        mutableList.set(i - 1, previous.withSensitivities(
            previous.getAlphaSensitivity() + current.getAlphaSensitivity(),
            previous.getBetaSensitivity() + current.getBetaSensitivity(),
            previous.getRhoSensitivity() + current.getRhoSensitivity(),
            previous.getNuSensitivity() + current.getNuSensitivity()));
        mutableList.remove(i);
        i--;
      }
      previous = current;
    }
  }

  //-------------------------------------------------------------------------
  @Override
  public SwaptionSabrSensitivities convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    List<SwaptionSabrSensitivity> converted = sensitivities
        .stream()
        .map(sensitivity -> sensitivity.convertedTo(resultCurrency, rateProvider))
        .collect(toImmutableList());
    return SwaptionSabrSensitivities.of(converted);
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code SwaptionSabrSensitivities}.
   * @return the meta-bean, not null
   */
  public static SwaptionSabrSensitivities.Meta meta() {
    return SwaptionSabrSensitivities.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(SwaptionSabrSensitivities.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private SwaptionSabrSensitivities(
      List<SwaptionSabrSensitivity> sensitivities) {
    JodaBeanUtils.notNull(sensitivities, "sensitivities");
    this.sensitivities = ImmutableList.copyOf(sensitivities);
  }

  @Override
  public SwaptionSabrSensitivities.Meta metaBean() {
    return SwaptionSabrSensitivities.Meta.INSTANCE;
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
   * Gets the swaption SABR sensitivities.
   * <p>
   * Each entry includes details of the surface it relates to.
   * @return the value of the property, not null
   */
  public ImmutableList<SwaptionSabrSensitivity> getSensitivities() {
    return sensitivities;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SwaptionSabrSensitivities other = (SwaptionSabrSensitivities) obj;
      return JodaBeanUtils.equal(sensitivities, other.sensitivities);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(sensitivities);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(64);
    buf.append("SwaptionSabrSensitivities{");
    buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code SwaptionSabrSensitivities}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code sensitivities} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<SwaptionSabrSensitivity>> sensitivities = DirectMetaProperty.ofImmutable(
        this, "sensitivities", SwaptionSabrSensitivities.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "sensitivities");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return sensitivities;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends SwaptionSabrSensitivities> builder() {
      return new SwaptionSabrSensitivities.Builder();
    }

    @Override
    public Class<? extends SwaptionSabrSensitivities> beanType() {
      return SwaptionSabrSensitivities.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code sensitivities} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<SwaptionSabrSensitivity>> sensitivities() {
      return sensitivities;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return ((SwaptionSabrSensitivities) bean).getSensitivities();
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
   * The bean-builder for {@code SwaptionSabrSensitivities}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<SwaptionSabrSensitivities> {

    private List<SwaptionSabrSensitivity> sensitivities = ImmutableList.of();

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          return sensitivities;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 1226228605:  // sensitivities
          this.sensitivities = (List<SwaptionSabrSensitivity>) newValue;
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
    public SwaptionSabrSensitivities build() {
      return new SwaptionSabrSensitivities(
          sensitivities);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(64);
      buf.append("SwaptionSabrSensitivities.Builder{");
      buf.append("sensitivities").append('=').append(JodaBeanUtils.toString(sensitivities));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
