Strata-Calc
-----------
This directory contains the `strata-calc` module.

### Overview

This module provides the classes used by Strata when performing calculations.
It aims to make it easy to calculate measures, such as present value,
on a list of targets, typically trades.
To achieve this, the system manages market data, such as quotes, curves and surfaces.
If desired, calculations can run for scenarios, where the market data is manipulated to simulate
potential future changes, such as a rise or fall in interest rates.

The interface, `CalculationRunner` provides the main API.


### Source code

This module is released as Open Source Software using the
[Apache v2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).  
Commercial support is [available](http://www.opengamma.com/) from the authors.

Code in this module will be maintained with backwards compatibility in mind.

[![OpenGamma](http://developers.opengamma.com/res/display/default/chrome/masthead_logo.png "OpenGamma")](http://www.opengamma.com)
