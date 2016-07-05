Download
========

Clone or download latest sourcecode from GitHub repository:

[https://github.com/cyberborean/rdfbeans](https://github.com/cyberborean/rdfbeans)

Using Maven artifact
--------------------

To use RDFBeans as a dependency of your Maven project, add it to your project POM:

```
<repositories>
        <repository>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
            <id>rdfbeans-repo</id>
            <name>RDFBeans</name>
            <url>https://raw.githubusercontent.com/cyberborean/rdfbeans/mvn-repo</url>
        </repository>
        ...
</repositories>

<dependencies>
        <dependency>
            <groupId>org.cyberborean</groupId>
            <artifactId>rdfbeans</artifactId>
            <version>2.2-SNAPSHOT</version>
        </dependency>
        ...
</dependencies>
```
