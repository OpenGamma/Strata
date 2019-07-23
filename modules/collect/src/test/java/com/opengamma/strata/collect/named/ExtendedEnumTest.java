/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.named;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.collect.named.ExtendedEnum.ExternalEnumNames;

/**
 * Test {@link ExtendedEnum}.
 */
public class ExtendedEnumTest {

  @Test
  public void test_enum_SampleNamed() {
    ExtendedEnum<SampleNamed> test = ExtendedEnum.of(SampleNamed.class);
    assertThat(test.lookupAll())
        .isEqualTo(ImmutableMap.builder()
            .put("Standard", SampleNameds.STANDARD)
            .put("STANDARD", SampleNameds.STANDARD)
            .put("More", MoreSampleNameds.MORE)
            .put("MORE", MoreSampleNameds.MORE)
            .put("Other", OtherSampleNameds.OTHER)
            .put("Another1", SampleNamedInstanceLookup1.ANOTHER1)
            .put("ANOTHER1", SampleNamedInstanceLookup1.ANOTHER1)
            .put("Another2", SampleNamedInstanceLookup2.ANOTHER2)
            .put("ANOTHER2", SampleNamedInstanceLookup2.ANOTHER2)
            .build());
    assertThat(test.lookupAllNormalized())
        .isEqualTo(ImmutableMap.builder()
            .put("Standard", SampleNameds.STANDARD)
            .put("More", MoreSampleNameds.MORE)
            .put("Other", OtherSampleNameds.OTHER)
            .put("Another1", SampleNamedInstanceLookup1.ANOTHER1)
            .put("Another2", SampleNamedInstanceLookup2.ANOTHER2)
            .build());
    assertThat(test.alternateNames()).isEqualTo(ImmutableMap.of("Alternate", "Standard", "ALTERNATE", "Standard"));
    assertThat(test.getType()).isEqualTo(SampleNamed.class);
    assertThat(test.find("Standard")).isEqualTo(Optional.of(SampleNameds.STANDARD));
    assertThat(test.find("STANDARD")).isEqualTo(Optional.of(SampleNameds.STANDARD));
    assertThat(test.find("Rubbish")).isEqualTo(Optional.empty());
    assertThat(test.lookup("Standard")).isEqualTo(SampleNameds.STANDARD);
    assertThat(test.lookup("Alternate")).isEqualTo(SampleNameds.STANDARD);
    assertThat(test.lookup("ALTERNATE")).isEqualTo(SampleNameds.STANDARD);
    assertThat(test.lookup("More")).isEqualTo(MoreSampleNameds.MORE);
    assertThat(test.lookup("More", MoreSampleNameds.class)).isEqualTo(MoreSampleNameds.MORE);
    assertThat(test.lookup("Other")).isEqualTo(OtherSampleNameds.OTHER);
    assertThat(test.lookup("Other", OtherSampleNameds.class)).isEqualTo(OtherSampleNameds.OTHER);
    assertThat(test.lookup("Another1")).isEqualTo(SampleNamedInstanceLookup1.ANOTHER1);
    assertThat(test.lookup("Another2")).isEqualTo(SampleNamedInstanceLookup2.ANOTHER2);
    assertThatIllegalArgumentException().isThrownBy(() -> test.lookup("Rubbish"));
    assertThatIllegalArgumentException().isThrownBy(() -> test.lookup(null));
    assertThatIllegalArgumentException().isThrownBy(() -> test.lookup("Other", MoreSampleNameds.class));
    assertThat(test.toString()).isEqualTo("ExtendedEnum[SampleNamed]");
  }

  @Test
  public void test_enum_SampleNamed_externals() {
    ExtendedEnum<SampleNamed> test = ExtendedEnum.of(SampleNamed.class);
    assertThat(test.externalNameGroups()).isEqualTo(ImmutableSet.of("Foo", "Bar"));
    assertThatIllegalArgumentException().isThrownBy(() -> test.externalNames("Rubbish"));
    ExternalEnumNames<SampleNamed> fooExternals = test.externalNames("Foo");
    assertThat(fooExternals.lookup("Foo1")).isEqualTo(SampleNameds.STANDARD);
    assertThat(fooExternals.lookup("Foo1", SampleNamed.class)).isEqualTo(SampleNameds.STANDARD);
    assertThat(fooExternals.lookup("Foo1", SampleNamed.class)).isEqualTo(SampleNameds.STANDARD);
    assertThat(fooExternals.externalNames()).isEqualTo(ImmutableMap.of("Foo1", "Standard"));
    assertThatIllegalArgumentException().isThrownBy(() -> fooExternals.lookup("Rubbish"));
    assertThatIllegalArgumentException().isThrownBy(() -> fooExternals.lookup(null));
    assertThatIllegalArgumentException().isThrownBy(() -> fooExternals.lookup("Other", MoreSampleNameds.class));
    assertThat(fooExternals.toString()).isEqualTo("ExternalEnumNames[SampleNamed:Foo]");

    ExternalEnumNames<SampleNamed> barExternals = test.externalNames("Bar");
    assertThat(barExternals.lookup("Foo1")).isEqualTo(MoreSampleNameds.MORE);
    assertThat(barExternals.lookup("Foo2")).isEqualTo(SampleNameds.STANDARD);
    assertThat(barExternals.reverseLookup(MoreSampleNameds.MORE)).isEqualTo("Foo1");
    assertThat(barExternals.reverseLookup(SampleNameds.STANDARD)).isEqualTo("Foo2");
    assertThatIllegalArgumentException().isThrownBy(() -> barExternals.reverseLookup(OtherSampleNameds.OTHER));
    assertThat(barExternals.externalNames()).isEqualTo(ImmutableMap.of("Foo1", "More", "Foo2", "Standard"));
    assertThat(barExternals.toString()).isEqualTo("ExternalEnumNames[SampleNamed:Bar]");
  }

  @Test
  public void test_enum_SampleOther() {
    ExtendedEnum<SampleOther> test = ExtendedEnum.of(SampleOther.class);
    assertThat(test.lookupAll()).isEqualTo(ImmutableMap.of());
    assertThat(test.alternateNames()).isEqualTo(ImmutableMap.of());
    assertThat(test.externalNameGroups()).isEqualTo(ImmutableSet.of());
    assertThatIllegalArgumentException().isThrownBy(() -> test.lookup("Rubbish"));
    assertThatIllegalArgumentException().isThrownBy(() -> test.lookup(null));
    assertThat(test.toString()).isEqualTo("ExtendedEnum[SampleOther]");
  }

  @Test
  public void test_enum_lenient() {
    ExtendedEnum<SampleNamed> test = ExtendedEnum.of(SampleNamed.class);
    assertThat(test.findLenient("Standard")).isEqualTo(Optional.of(SampleNameds.STANDARD));
    assertThat(test.findLenient("A1")).isEqualTo(Optional.of(SampleNameds.STANDARD));
    assertThat(test.findLenient("A2")).isEqualTo(Optional.of(MoreSampleNameds.MORE));
  }

  @Test
  public void test_enum_invalid() {
    Logger logger = Logger.getLogger(ExtendedEnum.class.getName());
    Level level = logger.getLevel();
    try {
      logger.setLevel(Level.OFF);
      // these return empty instances to avoid ExceptionInInitializerError
      assertThat(ExtendedEnum.of(SampleInvalid1.class).lookupAll().isEmpty()).isEqualTo(true);
      assertThat(ExtendedEnum.of(SampleInvalid2.class).lookupAll().isEmpty()).isEqualTo(true);
      assertThat(ExtendedEnum.of(SampleInvalid3.class).lookupAll().isEmpty()).isEqualTo(true);
      assertThat(ExtendedEnum.of(SampleInvalid4.class).lookupAll().isEmpty()).isEqualTo(true);
      assertThat(ExtendedEnum.of(SampleInvalid5.class).lookupAll().isEmpty()).isEqualTo(true);
      assertThat(ExtendedEnum.of(SampleInvalid6.class).lookupAll().isEmpty()).isEqualTo(true);
      assertThat(ExtendedEnum.of(SampleInvalid7.class).lookupAll().isEmpty()).isEqualTo(true);
    } finally {
      logger.setLevel(level);
    }
  }

}
