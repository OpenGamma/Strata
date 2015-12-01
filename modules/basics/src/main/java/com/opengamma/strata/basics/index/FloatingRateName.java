/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import java.io.Serializable;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Logger;

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
import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.collect.io.ResourceConfig;
import com.opengamma.strata.collect.named.Named;

/**
 * A floating rate index name, such as Libor, Euribor or US Fed Fund.
 * <p>
 * An index represented by this class relates to some form of floating rate.
 * This can include {@link IborIndex} and {@link OvernightIndex} values.
 * <p>
 * This class is designed to match the FpML/ISDA floating rate index concept.
 * The FpML concept provides a single key for floating rates of a variety of
 * types, mixing  Ibor, Overnight and Swap indices.
 * It also sometimes includes a source, such as 'Bloomberg' or 'Reuters'.
 * This class matches the single concept and provided a bridge the the more
 * specific index implementations used for pricing.
 * <p>
 * The most common implementations are provided in {@link FloatingRateNames}.
 * <p>
 * The set of supported values, and their mapping to {@code IborIndex} and
 * {@code OvernightIndex}, is defined in the {@code FloatingRateName.ini}
 * config file.
 */
@BeanDefinition(builderScope = "private")
public final class FloatingRateName
    implements ImmutableBean, Named, Serializable {

  /**
   * INI file for floating rate names.
   */
  private static final String FLOATING_RATE_NAME_INI = "FloatingRateName.ini";
  /**
   * The map of known instances.
   */
  static final ImmutableMap<String, FloatingRateName> DATA_MAP = loadIndices();

  /**
   * The external name, typically from FpML, such as 'GBP-LIBOR-BBA'.
   */
  @PropertyDefinition(validate = "notEmpty", overrideGet = true)
  private final String name;
  /**
   * The root of the name of the index, such as 'GBP-LIBOR', to which the tenor is appended.
   * This name matches that used by {@link IborIndex} or {@link OvernightIndex}.
   * Typically, multiple {@code FloatingRateName} names map to one Ibor or Overnight index.
   */
  @PropertyDefinition(validate = "notEmpty")
  private final String indexName;
  /**
   * The type of the index.
   */
  @PropertyDefinition(validate = "notNull")
  private final FloatingRateType type;

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code FloatingRateName} from a unique name.
   * 
   * @param name  the unique name
   * @return the name
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FloatingRateName of(String name) {
    ArgChecker.notNull(name, "uniqueName");
    FloatingRateName index = DATA_MAP.get(name);
    if (index == null) {
      throw new IllegalArgumentException("Unknown FpML Floating Rate Index: " + name);
    }
    return index;
  }

  //-------------------------------------------------------------------------
  /**
   * Loads the available indices.
   * 
   * @return the map of known indices
   */
  static ImmutableMap<String, FloatingRateName> loadIndices() {
    try {
      IniFile ini = ResourceConfig.combinedIniFile(FLOATING_RATE_NAME_INI);
      return parseIndices(ini);

    } catch (RuntimeException ex) {
      // logging used because this is loaded in a static variable
      Logger logger = Logger.getLogger(FloatingRateName.class.getName());
      logger.severe(Throwables.getStackTraceAsString(ex));
      // return an empty instance to avoid ExceptionInInitializerError
      return ImmutableMap.of();
    }
  }

  // parse the config file FloatingRateName.ini
  private static ImmutableMap<String, FloatingRateName> parseIndices(IniFile ini) {
    ImmutableMap.Builder<String, FloatingRateName> builder = ImmutableMap.builder();
    PropertySet iborSection = ini.section("ibor");
    for (String key : iborSection.keys()) {
      builder.put(key, new FloatingRateName(key, iborSection.value(key) + "-", FloatingRateType.IBOR));
    }
    PropertySet onCompoundedSection = ini.section("overnightCompounded");
    for (String key : onCompoundedSection.keys()) {
      builder.put(key, new FloatingRateName(key, onCompoundedSection.value(key), FloatingRateType.OVERNIGHT_COMPOUNDED));
    }
    PropertySet onAveragedSection = ini.section("overnightAveraged");
    for (String key : onAveragedSection.keys()) {
      builder.put(key, new FloatingRateName(key, onAveragedSection.value(key), FloatingRateType.OVERNIGHT_AVERAGED));
    }
    return builder.build();
  }

  //-------------------------------------------------------------------------
  /**
   * Checks and returns an Ibor index.
   * <p>
   * If this is an Ibor index, then this returns the matching {@link IborIndex}.
   * If not, an exception is thrown.
   * 
   * @param tenor  the tenor of the index
   * @return the index
   * @throws IllegalStateException if the type is not an Ibor index type
   * @see #getType()
   */
  public IborIndex toIborIndex(Tenor tenor) {
    if (!type.isIbor()) {
      throw new IllegalStateException("Incorrect index type, expected Ibor: " + name);
    }
    return IborIndex.of(indexName + tenor.toString());
  }

  /**
   * Converts to an {@link OvernightIndex}.
   * <p>
   * If this is an Overnight index, then this returns the matching {@link OvernightIndex}.
   * If not, an exception is thrown.
   * 
   * @return the index
   * @throws IllegalStateException if the type is not an Overnight index type
   * @see #getType()
   */
  public OvernightIndex toOvernightIndex() {
    if (!type.isOvernight()) {
      throw new IllegalStateException("Incorrect index type, expected Overnight: " + name);
    }
    return OvernightIndex.of(indexName);
  }

  //-------------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof FloatingRateName) {
      return name.equals(((FloatingRateName) obj).name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the name of the index.
   * 
   * @return the name of the index
   */
  @Override
  @ToString
  public String toString() {
    return getName();
  }

  //------------------------- AUTOGENERATED START -------------------------
  ///CLOVER:OFF
  /**
   * The meta-bean for {@code FloatingRateName}.
   * @return the meta-bean, not null
   */
  public static FloatingRateName.Meta meta() {
    return FloatingRateName.Meta.INSTANCE;
  }

  static {
    JodaBeanUtils.registerMetaBean(FloatingRateName.Meta.INSTANCE);
  }

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;

  private FloatingRateName(
      String name,
      String indexName,
      FloatingRateType type) {
    JodaBeanUtils.notEmpty(name, "name");
    JodaBeanUtils.notEmpty(indexName, "indexName");
    JodaBeanUtils.notNull(type, "type");
    this.name = name;
    this.indexName = indexName;
    this.type = type;
  }

  @Override
  public FloatingRateName.Meta metaBean() {
    return FloatingRateName.Meta.INSTANCE;
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
   * Gets the external name, typically from FpML, such as 'GBP-LIBOR-BBA'.
   * @return the value of the property, not empty
   */
  @Override
  public String getName() {
    return name;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the root of the name of the index, such as 'GBP-LIBOR', to which the tenor is appended.
   * This name matches that used by {@link IborIndex} or {@link OvernightIndex}.
   * Typically, multiple {@code FloatingRateName} names map to one Ibor or Overnight index.
   * @return the value of the property, not empty
   */
  public String getIndexName() {
    return indexName;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of the index.
   * @return the value of the property, not null
   */
  public FloatingRateType getType() {
    return type;
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FloatingRateName}.
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
        this, "name", FloatingRateName.class, String.class);
    /**
     * The meta-property for the {@code indexName} property.
     */
    private final MetaProperty<String> indexName = DirectMetaProperty.ofImmutable(
        this, "indexName", FloatingRateName.class, String.class);
    /**
     * The meta-property for the {@code type} property.
     */
    private final MetaProperty<FloatingRateType> type = DirectMetaProperty.ofImmutable(
        this, "type", FloatingRateName.class, FloatingRateType.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "name",
        "indexName",
        "type");

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
        case -807707011:  // indexName
          return indexName;
        case 3575610:  // type
          return type;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public BeanBuilder<? extends FloatingRateName> builder() {
      return new FloatingRateName.Builder();
    }

    @Override
    public Class<? extends FloatingRateName> beanType() {
      return FloatingRateName.class;
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
     * The meta-property for the {@code indexName} property.
     * @return the meta-property, not null
     */
    public MetaProperty<String> indexName() {
      return indexName;
    }

    /**
     * The meta-property for the {@code type} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FloatingRateType> type() {
      return type;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return ((FloatingRateName) bean).getName();
        case -807707011:  // indexName
          return ((FloatingRateName) bean).getIndexName();
        case 3575610:  // type
          return ((FloatingRateName) bean).getType();
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
   * The bean-builder for {@code FloatingRateName}.
   */
  private static final class Builder extends DirectFieldsBeanBuilder<FloatingRateName> {

    private String name;
    private String indexName;
    private FloatingRateType type;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3373707:  // name
          return name;
        case -807707011:  // indexName
          return indexName;
        case 3575610:  // type
          return type;
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
        case -807707011:  // indexName
          this.indexName = (String) newValue;
          break;
        case 3575610:  // type
          this.type = (FloatingRateType) newValue;
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
    public FloatingRateName build() {
      return new FloatingRateName(
          name,
          indexName,
          type);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(128);
      buf.append("FloatingRateName.Builder{");
      buf.append("name").append('=').append(JodaBeanUtils.toString(name)).append(',').append(' ');
      buf.append("indexName").append('=').append(JodaBeanUtils.toString(indexName)).append(',').append(' ');
      buf.append("type").append('=').append(JodaBeanUtils.toString(type));
      buf.append('}');
      return buf.toString();
    }

  }

  ///CLOVER:ON
  //-------------------------- AUTOGENERATED END --------------------------
}
