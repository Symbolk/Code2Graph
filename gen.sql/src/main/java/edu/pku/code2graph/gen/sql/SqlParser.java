package edu.pku.code2graph.gen.sql;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class SqlParser {
  protected Logger logger = LoggerFactory.getLogger(SqlParser.class);

  public Statement parseLine(String sql) {
    try {
      return CCJSqlParserUtil.parse(sql);
    } catch (JSQLParserException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Statements parseLines(String sqls) {
    try {
      return CCJSqlParserUtil.parseStatements(sqls);
    } catch (JSQLParserException e) {
      e.printStackTrace();
    }
    return null;
  }

  public Statements parseFile(String filename) throws IOException {
    String data = "";
    BufferedReader fr = new BufferedReader(new FileReader(filename));
    String buffer;
    while ((buffer = fr.readLine()) != null) {
      data = data + buffer;
    }
    logger.debug(data);
    return parseLines(data);
  }
}
