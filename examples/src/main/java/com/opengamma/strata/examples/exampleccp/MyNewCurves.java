//package com.opengamma.strata.examples.exampleccp;
//
//import com.google.common.collect.ImmutableMap;
//import com.opengamma.strata.basics.date.Tenor;
//import com.opengamma.strata.curve.SimpleCurveCalibrator;
//import com.opengamma.strata.curve.YieldCurve;
//
//import java.time.LocalDate;
//
//import static com.opengamma.strata.basics.date.Tenor.*;
//import static com.opengamma.strata.curve.InterpolationMethod.LINEAR;
//
//public class MyNewCurves {
//
//  public static YieldCurve oisCurveDiscount() {
//    return ois();
//  }
//
//  public static YieldCurve oisCurve() {
//    return ois();
//  }
//
//  private static YieldCurve ois() {
//    SimpleCurveCalibrator curveCalibrator = new SimpleCurveCalibrator().withInterpolation(LINEAR);
//
//    LocalDate start = LocalDate.of(2014, 7, 23);
//
//    ImmutableMap<Tenor, Double> rates = ImmutableMap.of(
//        TENOR_1M, 0.0015,
//        TENOR_2M, 0.0019,
//        TENOR_3M, 0.0023,
//        TENOR_6M, 0.0032,
//        TENOR_9M, 0.0042);
//
//    return curveCalibrator.buildYieldCurve(rates, start);
//  }
//}
