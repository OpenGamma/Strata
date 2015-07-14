Strata-Engine
--------------
This directory contains the `strata-engine` module.

### Overview

This module provides the main calculation engine provided by Strata.
The engine exists to make it easy to calculate measures, such as present value,
on a list of targets, typically trades.
To achieve this, the engine manages market data, such as quotes, curves and surfaces.
If desired, the engine can run scenarios, where the market data is manipulated to simulate
potential future changes, such as a rise or fall in interest rates.

The interface, `CalculationEngine` provides the main API.


### Source code

This module is released as Open Source Software using the
[Apache v2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).  
Commercial support is [available](http://www.opengamma.com/) from the authors.

Code in this directory is not currently released.
Classes and Methods may change at any time.
Once released it will be maintained with backwards compatibility in mind.

[![OpenGamma](http://developers.opengamma.com/res/display/default/chrome/masthead_logo.png "OpenGamma")](http://www.opengamma.com)
