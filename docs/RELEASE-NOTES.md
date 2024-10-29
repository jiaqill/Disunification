---
- - summary:
    - v1.4.1 (2024-01-06), Java 11, OWL API 4.5.26, Protege 5.5.0
    - v1.4.0 (2016-04-11), Java 8 , OWL API 4.1.3 , Protege 5.0.0-beta-23
    - v1.3.1 (2015-09-09), Java 7 , OWL API 3.5.1 , Protege 5.0.0-beta-17
    - v1.3.0 (2015-04-15), Java 7 , OWL API 3.5.0 , Protege 4.3
    - v1.2.0 (2012-04-30), Java 6 , OWL API 3.2.4 , Protege 4.1
    - v1.1.0 (2012-03-09), Java 6 , OWL API 3.2.4 , Protege 4.1
    - v1.0.0 (2012-01-27), Java 6 , OWL API 3.2.4 , Protege 4.1
- - version: v1.4.1
  - date: '2024-01-06'
  - features:
    - uses the OWL API 4.5.26
    - can be used as a plug-in for Protege 5.5.0, tested with Protege 5.6.3
    - runs on Java 11, tested with Java 21
  - build: $ `mvn clean install`
  - release: target/uel-1.4.1.zip
- - version: v1.4.0
  - date: '2016-04-11'
  - features:
    - new user interface for adding dissubsumptions
    - undo button for new dissubsumptions
    - some performance improvements
    - uses the OWL API 4.1.3
    - can be used as a plug-in for Protege 5.0.0-beta-23
    - runs on Java 8
  - build: $ `mvn clean install`
  - release: target/uel-1.4.0.zip
- - version: v1.3.1
  - date: '2015-09-09'
  - features:
    - has new icons
    - does not have [Apache Ant + Apache Ivy](https://ant.apache.org/ivy/) build files
    - includes Maven POM files to be deployed in [Sonatype](https://oss.sonatype.org/)
    - is available at [The Central Repository](https://repo1.maven.org/maven2/de/tu-dresden/inf/lat/uel/)
  - build: $ `mvn clean install`
  - release: target/uel-1.3.1.zip
- - version: v1.3.0
  - date: '2015-04-15'
  - features:
    - added capability to handle dissubsumptions to SAT-based algorithm
    - added ASP-based algorithm (experimental)
  - build:
    - $ `cd uel`
    - if using Apache Ant (1.8.3+)
    - $ `ant`
    - if using Apache Maven (3.0.3+)
    - $ `mvn clean package`
    - release: uel/uel-build/target/uel-1.3.0.zip
- - version: v1.2.0
  - date: '2012-04-30'
  - features:
    - added possibility to restrict SAT-based algorithm to minimal assignments
  - build:
    - $ `cd uel/uel.distribution`
    - if using Apache Ant (1.8.3+)
    - $ `ant`
    - if using Apache Maven (3.0.3+)
    - $ `mvn clean package javadoc:javadoc source:jar`
  - release: uel/uel.distribution/target/uel-1.2.0.zip
- - version: v1.1.0
  - date: '2012-03-09'
  - features:
    - added Rule-based algorithm
  - build:
    - $ `cd uel/uel.plugin`
    - $ `ant -buildfile build-bundle.xml`
  - release: uel/uel.plugin/target/de.tudresden.inf.lat.uel.jar
- - version: v1.0.0
  - date: '2012-01-27'
  - features:
    - basic SAT-based algorithm
  - build:
    - $ `cd uel/uel.plugin`
    - $ `ant -buildfile build-bundle.xml`
  - release: uel/uel.plugin/target/de.tudresden.inf.lat.uel.jar

