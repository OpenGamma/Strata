Strata releases
===============

The release process for Strata is as follows:

1. Ensure all required changes have been merged

1. Check out the master branch and ensure there are no local changes

1. Change version of Strata, *{major}.{minor}.{patch}*, e.g. 1.0.0:  
`mvn versions:set -DgenerateBackupPoms=false -DartifactId=* -DgroupId=com.opengamma.strata`

1. Create Git commit, with message such as "Release v1.0.0", and push

1. Add Git tag (beginning with `v`):  
`git tag -a v1.0.0 -m "Release v1.0.0"`

1. Push the tag: `git push --follow-tags`

1. Travis will automatically detect the new tag and perform a build.
Being a release tag, beginning with `v`, additional operations are triggered during the build which 
will perform the deployment to Bintray and upload the build artifacts to the GitHub Releases page.
Note that there will be a concurrent build for the earlier push to master which will behave normally.

1. Bump version of Strata to SNAPSHOT, e.g. 1.1.0-SNAPSHOT:  
`mvn versions:set -DgenerateBackupPoms=false -DartifactId=* -DgroupId=com.opengamma.strata`

1. Create Git commit, with message such as "Bump version", and push

1. Update docs website

1. Add Javadoc to docs website  
(delete old Javadoc, copy in new Javadoc)

1. Add forum post

1. Party!


Milestone releases
------------------

1. Ensure all required changes have been merged

1. Check out the master branch of and ensure there are no local changes

1. Change version of Strata, *{major}.{minor}.{patch}*, eg. 1.1.0-M1:  
`mvn versions:set -DgenerateBackupPoms=false -DartifactId=* -DgroupId=com.opengamma.strata`

1. Create git commit, with message such as "Release v1.1.0-M1"

1. Release:  
`mvn clean deploy -Doss.jars -Ddist`

1. Add git tag and push commit to GitHub  
`git tag -a m1.1.0-M1 -m "Release v1.1.0-M1"`

1. Bump version of Strata back to original SNAPSHOT, eg. 1.1.0-SNAPSHOT:  
`mvn versions:set -DgenerateBackupPoms=false -DartifactId=* -DgroupId=com.opengamma.strata`

1. Create git commit, with message such as "Bump version"

1. Push the commits and tag:  
`git push --follow-tags`
