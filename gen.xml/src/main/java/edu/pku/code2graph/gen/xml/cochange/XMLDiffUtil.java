package edu.pku.code2graph.gen.xml.cochange;

import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.gen.TreeGenerator;
import com.github.gumtreediff.gen.antlr3.xml.XmlTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.TreeContext;
import edu.pku.code2graph.util.SysUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XMLDiffUtil {

  public static List<XMLDiff> computeXMLChanges(
      Pair<List<String>, List<String>> tempFilePaths, String aRelativePath, String bRelativePath) {
    // find the absolute file path with relative path
    Optional<String> aPath =
        tempFilePaths.getLeft().stream()
            .filter(path -> path.trim().endsWith(aRelativePath))
            .findFirst();
    Optional<String> bPath =
        tempFilePaths.getRight().stream()
            .filter(path -> path.trim().endsWith(bRelativePath))
            .findFirst();

    String output = "";
    //    System.out.println(
    //        SysUtil.runSystemCommand(tempDir, Charset.defaultCharset(), "python", "--version"));
    if (aPath.isPresent() && bPath.isPresent()) {
      // compare with system command
      output =
          SysUtil.runSystemCommand(
              System.getProperty("user.dir"),
              Charset.defaultCharset(),
              "xmldiff",
              "-p",
              aPath.get(),
              bPath.get());
    }
    // TODO process added and deleted files
    // parse the output and collected diff
    Stream<String> lines = output.lines();
    List<XMLDiff> xmlDiffs = new ArrayList<>();
    lines.forEach(line -> xmlDiffs.add(convertXMLDiff(line)));
    return xmlDiffs;
  }

  public static XMLDiff convertXMLDiff(String line) {
    List<String> parts =
        Arrays.stream(StringUtils.removeEnd(StringUtils.removeStart(line, "["), "]").split(","))
            .map(String::trim)
            .collect(Collectors.toList());
    // convert the diff to objects
    switch (parts.size()) {
      case 4:
        return new XMLDiff(parts.get(0), parts.get(1), parts.get(2), parts.get(3));
      case 3:
        return new XMLDiff(parts.get(0), parts.get(1), parts.get(2));
      case 2:
        return new XMLDiff(parts.get(0), parts.get(1));
    }
    return null;
  }

  /**
   * Compute diff and return edit script
   *
   * @param oldContent
   * @param newContent
   * @return
   */
  public static EditScript computeXMLChangesWithGumtree(String oldContent, String newContent) {
    //        Generators generator = Generators.getInstance();
    TreeGenerator generator = new XmlTreeGenerator();
    try {
      TreeContext oldContext = generator.generateFrom().string(oldContent);
      TreeContext newContext = generator.generateFrom().string(newContent);
      Matcher matcher = Matchers.getInstance().getMatcher();

      MappingStore mappings = matcher.match(oldContext.getRoot(), newContext.getRoot());
      EditScript editScript = new ChawatheScriptGenerator().computeActions(mappings);
      return editScript;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
