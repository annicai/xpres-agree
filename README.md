# xpres-agree



AGREE is a software to create instance diagrams for STEP files. 

AGREE was developed for KTH, starting in 2011. The libraries used is therefore quite old, and some deprecated. There has been no active development the last years, except for smaller fixes, but since there is still interests in maintaining it, it is now made available as an open source project.

## Build project

Project is built using Maven.

`mvn clean compile assembly:single`

There are two dependencies that are served from a local Nexus instance, [JSDAI runtime](https://www.jsdai.net/download) and *stepmodules*. Only the first one is neccessary, and can be downloaded from the link above. Stepmodules contains a set of common STEP schemas. Not including it will only make these schemas not being available in AGREE, instead you can import custom jars containing schemas from the UI.
Both of these dependencies will be added to a public maven repository in the near future.
