OG-Identifier
-------------
This directory contains the `og-identifier` project.

### Overview

This project provides classes that manage identifiers and links between entities.
There are two main types of identifier, Internal and External.

Internal identifiers are guaranteed to be unique within your codebase.
They include a scheme to identify the system providing the identifier.
Each entity (logical object) is represented by a single `ObjectId`.
Each version of the entity is represented by a `UniqueId`, which simply adds a version to the `ObjectId`.

External identifiers are provided by other systems.
They include a scheme to identify the system providing the identifier.
There is an expectation of uniqueness, however your system must allow for the external system to misbehave.

Links provide a mechanism for one entity to link to another.
A link may consist of just an identifier, or it may be fully resolved.


### Source code

OG-Identifier is released as Open Source Software using the
[Apache v2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).  
Commercial support is [available](http://www.opengamma.com/) from the authors.

Code in this directory is maintained with backwards compatibility in mind.

[![OpenGamma](http://developers.opengamma.com/res/display/default/chrome/masthead_logo.png "OpenGamma")](http://developers.opengamma.com)
