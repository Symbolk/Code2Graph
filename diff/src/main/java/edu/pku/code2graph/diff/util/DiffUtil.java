package edu.pku.code2graph.diff.util;

import edu.pku.code2graph.diff.model.ContentType;
import edu.pku.code2graph.diff.model.FileStatus;
import edu.pku.code2graph.diff.model.FileType;
import edu.pku.code2graph.util.SysUtil;
import org.mozilla.universalchardet.UniversalDetector;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DiffUtil {
  /**
   * Check the file type by file path. Two ways to determine whether a file is binary: by git diff
   * or by file -bI, the file must accessible on disk.
   *
   * @return
   */
  public static FileType checkFileType(String repoPath, String filePath) {
    //    String output = Utils.runSystemCommand(repoPath, StandardCharsets.UTF_8, "file", "-bI",
    // filePath);
    //    if(output.trim().endsWith("binary")){
    //      return FileType.BIN;
    //    }
    String output =
        SysUtil.runSystemCommand(
            repoPath,
            StandardCharsets.UTF_8,
            "git",
            "diff",
            "--no-index",
            "--numstat",
            "/dev/null",
            filePath);
    if (output.trim().replaceAll("\\s+", "").startsWith("--")) {
      return FileType.BIN;
    } else {
      // match type by extension
      return Arrays.stream(FileType.values())
          .filter(fileType -> filePath.endsWith(fileType.extension))
          .findFirst()
          .orElse(FileType.OTHER);
    }
  }

  /**
   * Convert the abbr symbol to status enum
   *
   * @param symbol
   * @return
   */
  public static FileStatus convertSymbolToStatus(String symbol) {
    for (FileStatus status : FileStatus.values()) {
      if (symbol.equals(status.symbol) || symbol.startsWith(status.symbol)) {
        return status;
      }
    }
    return FileStatus.UNMODIFIED;
  }

  public static Charset detectCharset(String filePath) {
    try {
      Path fileLocation = Paths.get(filePath);
      byte[] content = Files.readAllBytes(fileLocation);
      UniversalDetector detector = new UniversalDetector(null);
      detector.handleData(content, 0, content.length);
      detector.dataEnd();
      String detectorCode = detector.getDetectedCharset();
      if (detectorCode != null && detectorCode.startsWith("GB")) {
        return Charset.forName("GBK");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return StandardCharsets.UTF_8;
  }

  /**
   * Convert string to a list of lines
   *
   * @param s
   * @return
   */
  public static List<String> convertStringToList(String s) {
    return Arrays.asList(s.split("\\r?\\n"));
  }

  /**
   * Check the content type of hunk
   *
   * @param codeLines
   * @return
   */
  public static ContentType checkContentType(List<String> codeLines) {
    if (codeLines.isEmpty()) {
      return ContentType.EMPTY;
    }
    boolean isAllEmpty = true;
    Set<String> lineTypes = new HashSet<>();

    for (int i = 0; i < codeLines.size(); ++i) {
      String trimmedLine = codeLines.get(i).trim();
      if (trimmedLine.length() > 0) {
        isAllEmpty = false;
        if (trimmedLine.startsWith("//")
            || trimmedLine.startsWith("/*")
            || trimmedLine.startsWith("/**")
            || trimmedLine.startsWith("*")) {
          lineTypes.add("COMMENT");
        } else {
          lineTypes.add("CODE");
        }
      }
    }

    if (isAllEmpty) {
      return ContentType.BLANKLINE;
    } else if (lineTypes.contains("CODE")) {
      return ContentType.CODE;
    } else if (lineTypes.contains("COMMENT")) {
      // pure comment
      return ContentType.COMMENT;
    }
    return ContentType.CODE;
  }
}
