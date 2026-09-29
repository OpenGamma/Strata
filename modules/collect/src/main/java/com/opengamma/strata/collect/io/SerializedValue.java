/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;

import org.joda.beans.Bean;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaBean;
import org.joda.beans.TypedMetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;
import org.joda.beans.impl.light.LightMetaBean;
import org.joda.beans.ser.JodaBeanSer;
import org.joda.beans.ser.SerDeserializers;

import com.opengamma.strata.collect.UncheckedReflectiveOperationException;

/**
 * A serialized value.
 * <p>
 * This bean allows a Java object to be passed around in serialized form.
 * The object can be a Joda-Convert type, a Joda-Bean or implement {@code Serializable}.
 * This is useful where an object needs to be transported to another Java process where
 * the object's class may not be on the classpath.
 */
@BeanDefinition(style = "light")
public final class SerializedValue implements ImmutableBean {

  /**
   * The value, as a serialized Joda-Convert string.
   */
  @PropertyDefinition(get = "field")
  private final String convert;
  /**
   * The value, as a serialized Joda-Bean.
   */
  @PropertyDefinition(get = "field")
  private final byte[] bean;
  /**
   * The value, serialized by Java serialization.
   */
  @PropertyDefinition(get = "field")
  private final byte[] java;

  //-------------------------------------------------------------------------
  @ImmutableValidator
  private void validate() {
    int count = (convert != null ? 1 : 0) + (bean != null ? 1 : 0) + (java != null ? 1 : 0);
    if (count > 1) {
      throw new IllegalArgumentException("Value must be present in one form only");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance by serializing the value based on the declared Java type.
   * <p>
   * The declared Java type is not necessarily the same as the actual type of the value.
   * For example, the declared type might be an interface and the actual type the implementation class.
   * 
   * @param <T> the type of the value
   * @param javaType the declared Java type
   * @param value the value, may be null, a Joda-Convert type, Joda-Bean, or Serializable
   * @return the value object
   * @throws RuntimeException if the value cannot be stored
   */
  public static <T> SerializedValue serialize(Class<T> javaType, T value) {
    if (value == null) {
      return new SerializedValue(null, null, null);
    }
    if (JodaBeanUtils.stringConverter().isConvertible(javaType)) {
      String str = JodaBeanUtils.stringConverter().convertToString(javaType, value);
      return new SerializedValue(str, null, null);
    }
    if (value instanceof Bean) {
      byte[] bytes = JodaBeanSer.COMPACT.binWriter().write((Bean) value, false);
      return new SerializedValue(null, bytes, null);
    }
    if (value instanceof Serializable) {
      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
          oos.writeObject(value);
          oos.flush();
        }
        return new SerializedValue(null, null, baos.toByteArray());
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    } else {
      throw new IllegalArgumentException("Value must be a Joda-Convert type, Joda-Bean or Serializable");
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Deserializes the value.
   * <p>
   * This converts the value from the underlying string or binary form.
   * As such, it might throw an exception.
   * The intention is that deserialization uses the same Java type as serialization.
   * 
   * @param <T> the type of the value
   * @param javaType the declared Java type
   * @return the value, null if the serialized value represents null
   * @throws RuntimeException if the value cannot be deserialized
   */
  public <T> T deserialize(Class<T> javaType) {
    if (convert != null) {
      return JodaBeanUtils.stringConverter().convertFromString(javaType, convert);
    } else if (bean != null) {
      return JodaBeanSer.COMPACT.withDeserializers(SerDeserializers.LENIENT).binReader().read(bean, javaType);
    } else if (java != null) {
      try (ByteArrayInputStream bais = new ByteArrayInputStream(java);
          ObjectInputStream ois = new ObjectInputStream(bais)) {
        return javaType.cast(ois.readObject());
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      } catch (ClassNotFoundException ex) {
        throw new UncheckedReflectiveOperationException(ex);
      }
    } else {
      return null;
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code SerializedValue}.
   */
  private static final TypedMetaBean<SerializedValue> META_BEAN =
      LightMetaBean.of(
          SerializedValue.class,
          MethodHandles.lookup(),
          new String[] {
              "convert",
              "bean",
              "java"},
          new Object[0]);

  /**
   * The meta-bean for {@code SerializedValue}.
   * @return the meta-bean, not null
   */
  public static TypedMetaBean<SerializedValue> meta() {
    return META_BEAN;
  }

  static {
    MetaBean.register(META_BEAN);
  }

  private SerializedValue(
      String convert,
      byte[] bean,
      byte[] java) {
    this.convert = convert;
    this.bean = (bean != null ? bean.clone() : null);
    this.java = (java != null ? java.clone() : null);
    validate();
  }

  @Override
  public TypedMetaBean<SerializedValue> metaBean() {
    return META_BEAN;
  }

  //-----------------------------------------------------------------------
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      SerializedValue other = (SerializedValue) obj;
      return JodaBeanUtils.equal(convert, other.convert) &&
          JodaBeanUtils.equal(bean, other.bean) &&
          JodaBeanUtils.equal(java, other.java);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(convert);
    hash = hash * 31 + JodaBeanUtils.hashCode(bean);
    hash = hash * 31 + JodaBeanUtils.hashCode(java);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(128);
    buf.append("SerializedValue{");
    buf.append("convert").append('=').append(JodaBeanUtils.toString(convert)).append(',').append(' ');
    buf.append("bean").append('=').append(JodaBeanUtils.toString(bean)).append(',').append(' ');
    buf.append("java").append('=').append(JodaBeanUtils.toString(java));
    buf.append('}');
    return buf.toString();
  }

  //-------------------------- AUTOGENERATED END --------------------------
}
