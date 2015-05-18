/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static org.testng.Assert.assertEquals;

import org.joda.beans.ser.JodaBeanSer;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test serialization.
 */
@Test
public class SerializeTest {

  public void test_serialize() {
    MockSerBean bean = new MockSerBean();
    bean.setBdConvention(BusinessDayConventions.MODIFIED_FOLLOWING);
    bean.setHolidayCalendar(HolidayCalendars.NO_HOLIDAYS);
    bean.setDayCount(DayCounts.ACT_360);
    bean.setObjects(ImmutableList.of(
        BusinessDayConventions.MODIFIED_FOLLOWING, HolidayCalendars.NO_HOLIDAYS, DayCounts.ACT_360));

    String xml = JodaBeanSer.PRETTY.xmlWriter().write(bean);
    MockSerBean test = JodaBeanSer.COMPACT.xmlReader().read(xml, MockSerBean.class);
    assertEquals(test, bean);
  }

}
