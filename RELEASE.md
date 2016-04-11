Strata releases
===============

The release process for Strata is as follows:

1. Ensure all required changes have been merged

1. Check out the master branch of and ensure there are no local changes

1. Change version of Strata, *{major}.{minor}.{patch}*, eg. 0.11.0:  
`mvn versions:set -DgenerateBackupPoms=false -DartifactId=* -DgroupId=com.opengamma.strata`

1. Create git commit, with message such as "Release v0.11.0"

1. Release:  
`mvn clean deploy -Doss.jars -Ddist`

1. Add git tag and push commit to GitHub  
`git tag -a v0.11.0 -m "Release v0.11.0"`

1. Add targets to GitHub releases page  
(targets must be renamed to `strata-dist-0.11.0.zip` and `strata-report-tool-0.11.0.zip`)

1. Bump version of Strata to SNAPSHOT, eg. 0.11.0-SNAPSHOT:  
`mvn versions:set -DgenerateBackupPoms=false -DartifactId=* -DgroupId=com.opengamma.strata`

1. Create git commit, with message such as "Bump version", and push to GitHub

1. Update docs website

1. Add Javadoc to docs website  
(delete old Javadoc, copy in new Javadoc)

1. Add forum post

1. Party!
