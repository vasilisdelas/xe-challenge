## 🏡 XE.GR Real Estate Search Automation (Java + Playwright)

### 📌 Overview

This project automates the search functionality for rental properties on [xe.gr](https://www.xe.gr/) using **Playwright**. The script ensures that key search features work correctly and can be integrated into a smoke test suite for daily execution.

### 🚀 Setup & Installation

#### **Clone the Repository**

```
git clone git@github.com:vasilisdelas/xe-challenge.git
```

#### **Navigate to the project's folder**

```
cd /xe-challenge
```

#### **Switch to the branch for the Java version**

```
git switch java-version
```

#### **Install the dependencies**

```
mvn clean install
```

#### **Install Playwright browsers**

```
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

#### **Run the test using Maven**

```
mvn test
```
#### **Run the test via TestNG XML**

```
mvn test -DsuiteXmlFile=testng.xml
```

#### **Generate an HTML report**

```
mvn surefire-report:report
```
