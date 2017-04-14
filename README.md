ossindex-maven-plugin
=====================

Audits a [maven](https://maven.apache.org/) project using the [OSS Index REST API v2.0](https://ossindex.net) to identify known vulnerabilities in its dependencies.

This is a rewrite from previous versions to use the much simplified v2.0 API.

Requirements
-------------

* Maven 3.1
* An internet connection with access to https://ossindex.net 

[How To Install Apache Maven 3.2.1 On Ubuntu 14.04, Linux Mint 17 And Their Derivative Systems](http://linuxg.net/how-to-install-apache-maven-3-2-1-on-ubuntu-14-04-linux-mint-17-and-their-derivative-systems/)

Depending on your platform and installation details, you may also want to symbolically link maven3 like so:
```
sudo ln -s /usr/bin/mvn3 /usr/bin/mvn
sudo ln -s /usr/share/maven3 /usr/share/maven
```

Usage
-----

```
mvn <phase> net.ossindex:ossindex-maven-plugin:audit
```

A phase is required to ensure that all dependencies are correctly identified. For example:

```
mvn <compile> net.ossindex:ossindex-maven-plugin:audit
```

### Success output
This will run the OSS Index Auditor against the applicable maven project. A successfull
scan finding no errors will look something like this:

```
[INFO] Scanning for projects...
[INFO]
[INFO] Using the builder org.apache.maven.lifecycle.internal.builder.singlethreaded.SingleThreadedBuilder with a thread count of 1
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Building net.ossindex:heuristic-version 0.0.7-SNAPSHOT
[INFO] ------------------------------------------------------------------------
[INFO]
[INFO] --- ossindex-maven-plugin:0.0.10:audit (default-cli) @ heuristic-version ---
[INFO] OSS Index dependency audit
[INFO] com.github.zafarkhaja:java-semver:0.9.0  No known vulnerabilities
[INFO] org.slf4j:slf4j-api:1.7.12  No known vulnerabilities
[INFO] org.slf4j:slf4j-simple:1.7.12  No known vulnerabilities
[INFO] org.slf4j:slf4j-api:1.7.12  No known vulnerabilities
[INFO] junit:junit:4.12  No known vulnerabilities
[INFO] org.hamcrest:hamcrest-core:1.3  No known vulnerabilities
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Error output
If a vulnerability is found that might impact your project, the output will resemble the
following, where the package and vulnerability details depends on what is identified.

```
...
[INFO] org.eclipse.sisu:org.eclipse.sisu.inject:0.3.0  Unknown source for package
[INFO] org.codehaus.plexus:plexus-component-annotations:1.5.5  No known vulnerabilities
[INFO] org.codehaus.plexus:plexus-classworlds:2.5.2  No known vulnerabilities
[INFO] org.apache.maven.plugin-tools:maven-plugin-annotations:3.4  Unknown source for package
[ERROR] 
[ERROR] --------------------------------------------------------------
[ERROR] org.apache.maven:maven-artifact:3.0  [VULNERABLE]
[ERROR] 1 known vulnerabilities, 1 affecting installed version
[ERROR] 
[ERROR] cve:/CVE-2013-0253
[ERROR] The default configuration of Apache Maven 3.0.4, when using Maven Wagon 2.1, disables SSL certificate checks, which allows remote attackers to spoof servers via a man-in-the-middle (MITM) attack.
[ERROR] 
[ERROR] --------------------------------------------------------------
[ERROR] 
[INFO] org.codehaus.plexus:plexus-utils:3.0.20  No known vulnerabilities
[INFO] org.apache.maven:maven-project:3.0-alpha-2  No known vulnerabilities
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 0.651 s
[INFO] Finished at: 2015-11-02T21:24:19-05:00
[INFO] Final Memory: 7M/240M
[INFO] ------------------------------------------------------------------------
[ERROR] Failed to execute goal net.ossindex:ossindex-maven-plugin:0.0.11-SNAPSHOT:audit (default-cli) on project ossindex-maven-plugin: 1 known vulnerabilities affecting project dependencies -> [Help 1]
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] \[Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
Build step 'Invoke top-level Maven targets' marked build as failure
Finished: FAILURE
```

Report output
-------------

Use the `-Daudit.output=<path>` argument on the maven command line export the results in a variety of formats, depending on the file extension. You can output in multiple formats by providing a comma separated list of file paths (either relative or absolute paths will do). For example:

mvn install net.ossindex:ossindex-maven-plugin:audit -Daudit.failOnError=false -Daudit.output=test.json,test.xml,test.txt
You will get three different files output, each with a different format.

The structure of the JSON and XML formats follow the internal representation of the data in the maven plugin itself (I simply serialize the objects to disk). You will see every requested package, including their transitive dependencies, regardless whether there are vulnerabilities or not. If a package has known vulnerabilities then it will have the "vulnerability-total" field assigned a value greater then 0. If there are vulnerabilities that affect the requested version of the package, then the "vulnerability-matches" matches field assigned accordingly, and the "vulnerabilities" list will be filled with the details. Here is a sample with one non-vulnerable package and one vulnerable package in JSON:

```
[
  {
    "vulnerability-total": 0,
    "vulnerability-matches": 0,
    "groupId": "org.apache.commons",
    "artifactId": "commons-dbcp2",
    "version": "2.1.1"
  },
  {
    "parent": {
      "groupId": "org.apache.commons",
      "artifactId": "commons-dbcp2",
      "version": "2.1.1"
    },
    "vulnerability-total": 39,
    "vulnerability-matches": 1,
    "vulnerabilities": [
      {
        "id": 348558,
        "title": "[CVE-2011-5034]  Improper Input Validation",
        "description": "Apache Geronimo 2.2.1 and earlier computes hash values for form parameters without restricting the ability to trigger hash collisions predictably, which allows remote attackers to cause a denial of service (CPU consumption) by sending many crafted parameters.  NOTE: this might overlap CVE-2011-4461.",
        "versions": [
          "1.0",
          "1.1",
          "1.1.1",
          "1.2",
          "2.0.1",
          "2.0.2",
          "2.1",
          "2.1.1",
          "2.1.2",
          "2.1.3",
          "2.1.4",
          "2.1.5",
          "2.1.6",
          "2.1.7",
          "2.1.8",
          "2.2",
          "2.2.1"
        ],
        "references": [
          "http://archives.neohapsis.com/archives/bugtraq/2011-12/0181.html",
          "http://cve.mitre.org/cgi-bin/cvename.cgi?name=2011-5034",
          "http://secunia.com/advisories/47412",
          "http://www.cvedetails.com/cve-details.php?t=1&cve_id=CVE-2011-5034",
          "http://www.kb.cert.org/vuls/id/903934",
          "http://www.nruns.com/_downloads/advisory28122011.pdf",
          "http://www.ocert.org/advisories/ocert-2011-003.html",
          "https://github.com/FireFart/HashCollision-DOS-POC/blob/master/HashtablePOC.py",
          "https://web.nvd.nist.gov/view/vuln/detail?vulnId=2011-5034"
        ],
        "published": 1325210101610,
        "updated": 1352178300500,
        "cve": "CVE-2011-5034"
      }
    ],
    "groupId": "org.apache.geronimo.specs",
    "artifactId": "geronimo-jta_1.1_spec",
    "version": "1.1.1"
  },
]
```

You'll note that the vulnerable package's "parent" is commons-dbcp2, this is not necessarily the direct parent but does indicate that geronimo-jta_1.1_spec is a downstream dependency of commons-dbcp2.

Disable fail on error
------------------------

In the current version the build will fail on an error. This default will be changing in the next release. In the meanwhile, you can disable this behaviour by specifying `-Daudit.failOnError=false` on the maven command line

Integration into Jenkins
------------------------

A simple way to integrate with Jenkins is as "post-build step", specifically
under the "Invoke top-level Maven targets".

![Jenkins config](docs/jenkins1.png)

* Go to the configuration for your Jenkins project
* Find and click the "Add post-build step" drop down
* Select "Invoke top-level Maven targets"
* Enter "net.ossindex:ossindex-maven-plugin:audit" as the goal
* Select Apply/Save and re-run your build

![Jenkins config](docs/jenkins2.png)

If a vulnerability is detected in a dependency the build will indicate a failure and
log output for the build will contain something like this:

![Jenkins error](docs/jenkins-err.png)
