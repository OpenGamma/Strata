/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */

package com.opengamma.tools.gradle.release

import com.github.zafarkhaja.semver.Version
import spock.lang.Specification

class SnapshotVersionDeriverSpec extends Specification
{
	void setup()
	{
		ClassEnhancer.enhanceVersion()
	}

	//	@Unroll
    void "Test version deriver"(
		    String baseTagDescription,
		    String commitDescription,
		    String releaseTagTemplate,
		    Integer buildNumber,
		    String expected)
    {
        setup:
        SnapshotVersionDeriver versionDeriver = new SnapshotVersionDeriver(
		        baseTagDescription,
		        commitDescription,
		        releaseTagTemplate,
		        buildNumber)

        when:
        Version v = versionDeriver.deriveSnapshot()

	    then:
	    v == Version.valueOf(expected)

	    where:
	    baseTagDescription  | commitDescription         | releaseTagTemplate   | buildNumber | expected
	    "v1.0.0"            | "v1.0.0-10-g123abc"       | "v@version@"         | null        | "1.0.0+10.g123abc"
	    "v1.0.0"            | "v1.0.0-10-g123abc"       | "v@version@"         | 120         | "1.0.0+10.g123abc.b120"
	    "rel/test-2.2.1"    | "rel/test-2.2.1"          | "rel/test-@version@" | null        | "2.2.1"
	    "rel/test-2.2.1"    | "rel/test-2.2.1"          | "rel/test-@version@" | 201         | "2.2.1+b201"
	    "v1.0.0-beta2"      | "v1.0.0-beta2-10-g123abc" | "v@version@"         | null        | "1.0.0-beta2+10.g123abc"
	    "v1.0.0-beta2"      | "v1.0.0-beta2-10-g123abc" | "v@version@"         | 120         | "1.0.0-beta2+10.g123abc.b120"
	    "rel/test-2.2.1-M3" | "rel/test-2.2.1-M3"       | "rel/test-@version@" | null        | "2.2.1-M3"
	    "rel/test-2.2.1-M3" | "rel/test-2.2.1-M3"       | "rel/test-@version@" | 201         | "2.2.1-M3+b201"
    }

	void "Test configurable metadata"(
			Integer buildNumber,
			boolean includeCommitCount,
			boolean includeObjectName,
			boolean includeBuildNumber,
			String expected)
	{
		setup:
		SnapshotVersionDeriver versionDeriver = new SnapshotVersionDeriver(
				"v1.0.0",
				"v1.0.0-10-g123abc",
				"v@version@",
				buildNumber,
				includeCommitCount,
				includeObjectName,
				includeBuildNumber
		)

		when:
		Version v = versionDeriver.deriveSnapshot()

		then:
		v == Version.valueOf(expected)

		where:
		buildNumber | includeCommitCount | includeObjectName | includeBuildNumber | expected
		null        | true               | true              | true               | "1.0.0+10.g123abc"
		120         | true               | true              | true               | "1.0.0+10.g123abc.b120"
		null        | true               | true              | false              | "1.0.0+10.g123abc"
		120         | true               | true              | false              | "1.0.0+10.g123abc"
		null        | true               | false             | true               | "1.0.0+10.b120"
		120         | true               | false             | true               | "1.0.0+10.b120"
		null        | true               | false             | false              | "1.0.0+10"
		120         | true               | false             | false              | "1.0.0+10"
		null        | false              | true              | true               | "1.0.0+g123abc"
		120         | false              | true              | true               | "1.0.0+g123abc.b120"
		null        | false              | true              | false              | "1.0.0+g123abc"
		120         | false              | true              | false              | "1.0.0+g123abc"
		null        | false              | false             | true               | "1.0.0"
		120         | false              | false             | true               | "1.0.0+b120"
		null        | false              | false             | false              | "1.0.0"
		120         | false              | false             | false              | "1.0.0"
	}
}
