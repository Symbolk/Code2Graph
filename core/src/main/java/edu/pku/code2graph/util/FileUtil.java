package edu.pku.code2graph.util;

import edu.pku.code2graph.model.Language;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {
  /**
   * Create a folder if not exists
   *
   * @param dir abs path
   * @return
   */
  public static String createDir(String dir) {
    File directory = new File(dir);
    if (!directory.exists()) {
      // create the entire directory path including parents
      directory.mkdirs();
    }
    return directory.getAbsolutePath();
  }

  public static String prepareDir(String dir) {
    File file = new File(dir);
    if (file.exists()) {
      file.delete();
    }
    if (!file.exists()) {
      file.mkdirs();
    }
    return file.getAbsolutePath();
  }

  /**
   * Delete all files and subfolders to clear the directory
   *
   * @param dir absolute path
   * @return
   */
  public static boolean clearDir(String dir) {
    File file = new File(dir);
    if (!file.exists()) {
      file.mkdirs();
      return false;
    }

    String[] content = file.list();
    if (content != null) {
      for (String name : content) {
        File temp = new File(dir, name);
        if (temp.isDirectory()) {
          clearDir(temp.getAbsolutePath());
          temp.delete();
        } else {
          if (!temp.delete()) {
            System.err.println("Failed to delete the directory: " + name);
          }
        }
      }
    }
    return true;
  }

  /**
   * Get file name from path
   *
   * @return
   */
  public static String getFileNameFromPath(String filePath) {
    return Paths.get(filePath).getFileName().toString().trim();
  }

  /**
   * Get the name of the direct parent folder
   *
   * @return
   */
  public static String getParentFolderName(String filePath) {
    return Paths.get(filePath).getParent().getFileName().toString();
  }

  private static String rootPath;

  public static void setRootPath(String rootPath) {
    FileUtil.rootPath = rootPath;
  }

  /**
   * Get path relative to the root path
   *
   * @param absolutePath
   * @param rootPath
   * @return
   */
  public static String getRelativePath(String absolutePath) {
    return FilenameUtils.separatorsToUnix(FileUtil.getRelativePath(rootPath, absolutePath));
  }

  /**
   * Get path relative to the root path
   *
   * @param absolutePath
   * @param rootPath
   * @return
   */
  public static String getRelativePath(String rootPath, String absolutePath) {
    return Paths.get(rootPath).relativize(Paths.get(absolutePath)).toString();
  }

  /**
   * Write the given content in the file of the given file path.
   *
   * @param content
   * @param filePath
   * @return boolean indicating the success of the write operation.
   */
  public static boolean writeStringToFile(String content, String filePath) {
    try {
      FileUtils.writeStringToFile(new File(filePath), content, "UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  /**
   * Read the content of a file into string Actually wraps commons io API
   *
   * @return
   */
  public static String readFileToString(String filePath) {
    String content = "";
    try {
      content = FileUtils.readFileToString(new File(filePath), "UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return content;
  }

  /**
   * Read the content of a given file.
   *
   * @param path to be read
   * @return string content of the file, or null in case of errors.
   */
  public static List<String> readFileToLines(String path) {
    List<String> lines = new ArrayList<>();
    File file = new File(path);
    if (file.exists()) {
      try (BufferedReader reader =
          Files.newBufferedReader(Paths.get(path), StandardCharsets.UTF_8)) {
        lines = reader.lines().collect(Collectors.toList());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      return lines;
    }
    return lines;
  }

  public static void writeObjectToFile(Object object, String filePath, boolean append) {
    try {
      ObjectOutputStream out =
          new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(filePath, append)));
      out.writeObject(object);
      out.flush();
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }

  public static Object readObjectFromFile(String objectFile) {
    try {
      ObjectInputStream in =
          new ObjectInputStream(new BufferedInputStream(new FileInputStream(objectFile)));
      Object object = in.readObject();
      in.close();
      return object;
    } catch (Exception e) {
      // e.printStackTrace();
      return null;
    }
  }

  public static Map<String, List<String>> categorizeFilesByExtension(List<String> filePaths) {
    Map<String, List<String>> result = new LinkedHashMap<>();
    for (String path : filePaths) {
      String extension = FilenameUtils.getExtension(path);
      if (!result.containsKey(extension)) {
        result.put(extension, new ArrayList<>());
      }
      result.get(extension).add(path);
    }
    return result;
  }

  /**
   * List all files with specific extension under a folder/directory
   *
   * @param dir
   * @return absolute paths
   */
  public static List<String> listFilePaths(String dir, String extension) {
    List<String> result = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(Paths.get(dir))) {
      if (extension.isEmpty() || extension.isBlank()) {
        result = walk.filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toList());
      } else {
        result =
            walk.filter(Files::isRegularFile)
                .map(Path::toString)
                .filter(path -> path.endsWith(extension))
                //              .map(s -> s.substring(dir.length()))
                .collect(Collectors.toList());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * List { extension : [filePath] } under a folder/directory
   *
   * @param dir
   * @param languages
   * @return
   */
  public static Map<String, List<String>> listFilePathsInLanguages(
      String dir, Set<Language> languages) {
    if (languages.isEmpty()) {
      return new HashMap<>();
    }
    Map<String, List<String>> result = new LinkedHashMap<>();

    Set<String> extensions =
        languages.stream()
            .map(language -> language.extension.replace(".", ""))
            .collect(Collectors.toSet());
    try (Stream<Path> walk = Files.walk(Paths.get(dir))) {
      walk.filter(Files::isRegularFile)
          .map(Path::toString)
          .forEach(
              path -> {
                String ext = FilenameUtils.getExtension(path);
                if (extensions.contains(ext)) {
                  if (!result.containsKey(ext)) {
                    result.put(ext, new ArrayList<>());
                  }
                  result.get(ext).add(path);
                }
              });
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  public static String getPathFromURL(URL url) {
    if (url == null) {
      return "";
    }
    try {
      return new File(url.toURI()).getAbsolutePath();
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return "";
    }
  }
}
