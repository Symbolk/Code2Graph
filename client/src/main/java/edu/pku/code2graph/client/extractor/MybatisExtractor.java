package edu.pku.code2graph.client.extractor;

import edu.pku.code2graph.client.MybatisPreprocesser;
import edu.pku.code2graph.gen.jdt.AbstractJdtVisitor;
import edu.pku.code2graph.gen.xml.MybatisMapperHandler;
import edu.pku.code2graph.gen.xml.model.MybatisElement;
import edu.pku.code2graph.model.URI;
import edu.pku.code2graph.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.apache.commons.lang3.tuple.Pair;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class MybatisExtractor extends AbstractExtractor {
  private Map<String, Map<String, List<MybatisParam>>> paramMap = new HashMap<>();
  private Map<String, Map<String, URI>> fieldMap = new HashMap<>();
  Map<String, Map<String, MybatisElement>> queryMap;
  Map<String, String> xmlToJavaMapper;

  private static final String JRE_PATH =
      System.getProperty("java.home") + File.separator + "lib/rt.jar";

  public List<Pair<URI, URI>> generateInstances(String repoRoot, String repoPath)
      throws ParserConfigurationException, SAXException {
    FileUtil.setRootPath(repoRoot);
    extractFromJavaFile(repoPath);
    extractFromXmlFile(repoPath);
    findPairs();
    return uriPairs;
  }

  public void extractFromJavaFile(String repoPath) {
    List<String> filePaths = new ArrayList<>();
    List<String> exts = Arrays.asList(".java");
    findExtInRepo(repoPath, exts, filePaths);

    // the absolute file path of the compilation units to create ASTs for
    String[] srcPaths = new String[filePaths.size()];
    filePaths.toArray(srcPaths);

    // the absolute source path entries to be used to resolve bindings
    Set<String> srcFolderSet = new HashSet<>();
    for (String path : filePaths) {
      File file = new File(path);
      if (file.exists() && file.isFile()) {
        srcFolderSet.add(file.getParentFile().getAbsolutePath());
      }
    }
    String[] srcFolderPaths = new String[srcFolderSet.size()];
    srcFolderSet.toArray(srcFolderPaths);

    //
    String[] encodings = new String[srcFolderPaths.length];
    Arrays.fill(encodings, "UTF-8");

    ASTParser parser = ASTParser.newParser(AST.JLS14);
    Map<String, String> options = JavaCore.getOptions();
    options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
    options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
    options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
    options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_8, options);
    parser.setCompilerOptions(options);

    //        parser.setProject(WorkspaceUtilities.javaProject);
    parser.setKind(ASTParser.K_COMPILATION_UNIT);
    parser.setEnvironment(new String[] {JRE_PATH}, srcFolderPaths, encodings, true);
    parser.setResolveBindings(true);
    parser.setBindingsRecovery(true);

    AbstractJdtVisitor visitor = new MybatisExpressionVisitor(paramMap, fieldMap, uriPairs);
    // create nodes and nesting edges while visiting the ASTs
    encodings = new String[srcPaths.length];
    Arrays.fill(encodings, "UTF-8");

    parser.createASTs(
        srcPaths,
        encodings,
        new String[] {},
        new FileASTRequestor() {
          @Override
          public void acceptAST(String sourceFilePath, CompilationUnit cu) {
            // use relative path to project root
            visitor.setFilePath(sourceFilePath);
            visitor.setCu(cu);
            cu.accept(visitor);
          }
        },
        null);
  }

  public void extractFromXmlFile(String repoPath)
      throws ParserConfigurationException, SAXException {
    MybatisPreprocesser.preprocessMapperXmlFile(repoPath);

    queryMap = MybatisPreprocesser.getHandler().getQueryMap();
    xmlToJavaMapper = MybatisPreprocesser.getHandler().getXmlToJavaMapper();
  }

  private void findPairs() {
    for (String filepath : queryMap.keySet()) {
      Map<String, MybatisElement> queryInFile = queryMap.get(filepath);
      String javaMapperPackage = xmlToJavaMapper.get(filepath);
      Map<String, List<MybatisParam>> paramsInJava = null;
      if (javaMapperPackage != null) {
        paramsInJava = paramMap.get(javaMapperPackage);
      }
      for (String queryId : queryInFile.keySet()) {
        MybatisElement query = queryInFile.get(queryId);
        List<URI> identifiers = query.getIdentifierList();
        if (identifiers == null) continue;
        for (URI identifier : identifiers) {
          String name = identifier.getSymbol();
          if (name.contains(".")) {
            if (name.startsWith("#{") || name.startsWith("${"))
              name = name.substring(2, name.length() - 1);
            if (paramsInJava == null) continue;
            String[] tokens = name.split("\\.");
            if (tokens.length > 2) continue;
            List<MybatisParam> targetJavaParams = paramsInJava.get(queryId);
            for (MybatisParam param : targetJavaParams) {
              if (param.symbol.equals(tokens[0])) {
                String className = param.className;
                for (String classPackagePath : fieldMap.keySet()) {
                  if (classPackagePath.endsWith(className)) {
                    if (fieldMap.get(classPackagePath).get(tokens[0]) != null)
                      uriPairs.add(
                          new ImmutablePair<>(
                              identifier, fieldMap.get(classPackagePath).get(tokens[0])));
                  }
                }
              }
            }
          } else if (name.startsWith("#{") || name.startsWith("${")) {
            String parameterType = query.getParameterType();
            String symbol = name.substring(2, name.length() - 1);
            if (symbol.contains(",")) symbol = symbol.split(",")[0];
            if (parameterType != null) {
              for (String classPackagePath : fieldMap.keySet()) {
                if (classPackagePath.endsWith(parameterType)) {
                  if (fieldMap.get(classPackagePath).get(symbol) != null)
                    uriPairs.add(
                        new ImmutablePair<>(
                            identifier, fieldMap.get(classPackagePath).get(symbol)));
                }
              }
            }
          } else {
            String resultType = query.getResultType();
            if (resultType != null) {
              for (String classPackagePath : fieldMap.keySet()) {
                if (classPackagePath.endsWith(resultType)) {
                  if (fieldMap.get(classPackagePath).get(name) != null)
                    uriPairs.add(
                        new ImmutablePair<>(identifier, fieldMap.get(classPackagePath).get(name)));
                }
              }
            }
          }
        }
      }
    }
  }
}
