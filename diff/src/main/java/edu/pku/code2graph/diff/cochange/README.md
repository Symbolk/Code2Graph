## Graph-based cross-language co-change detection

## Environments

## Instructions

Execution order: Config --> ChangesCollector --> ChangeLint

### Configuration

1. (Config.java) Config the repo to test, and the input/output folder if specified differently:

```java
public static final String rootDir = System.getProperty("user.home") + "/coding/changelint";
public static String repoName = "AntennaPod-AntennaPod";
public static String repoPath = rootDir + "/repos/" + repoName;

// input: commits to test
public static String commitsListDir = rootDir + "/input";

// intermediate results: original changes in the commits
public static final String tempDir = rootDir + "/changes";

// output: suggested co-changes and comparison with ground truth
public static final String outputDir = rootDir + "/output";
```

### Preprocessing

2. (ChangesCollector.java) Collect xml and java changes/diffs in each commit as the Ground Truth:

```java
  public static void main(String[] args) {
    PropertyConfigurator.configure(
        System.getProperty("user.dir") + File.separator + "log4j.properties");
    repoName = Config.repoName;
    repoPath = Config.repoPath;
    try {
      collectChangesForRepo();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

```

### Co-change Suggestion 

3. (ChangeLint.java) Run main() to invoke the co-change suggester and compare with the Ground Truth, results will be saved in the specified output folder.

