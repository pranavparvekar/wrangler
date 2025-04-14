/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.parser;

import io.cdap.wrangler.api.RecipeException;
import io.cdap.wrangler.api.RecipeSymbol;
import io.cdap.wrangler.api.TokenGroup;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.DirectiveName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.Token;
import io.cdap.wrangler.api.parser.TokenType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Unit tests for GrammarBasedParser.
 */
public class GrammarBasedParserTest {

  @Test
  public void testParseDirective() throws Exception {
    String recipe = "parse-as-csv :body , true";
    GrammarBasedParser parser = new GrammarBasedParser("test", Collections.singletonList(recipe), false);
    RecipeSymbol symbol = parser.parse();

    Iterator<TokenGroup> iterator = symbol.iterator();
    Assert.assertTrue(iterator.hasNext());
    TokenGroup group = iterator.next();
    List<Token> tokens = group.getTokens();
    Assert.assertEquals(4, tokens.size());
    Assert.assertEquals(TokenType.DIRECTIVE_NAME, tokens.get(0).type());
    Assert.assertEquals("parse-as-csv", ((DirectiveName) tokens.get(0)).value());
    Assert.assertEquals(TokenType.COLUMN_NAME, tokens.get(1).type());
    Assert.assertEquals("body", ((ColumnName) tokens.get(1)).value());
    Assert.assertEquals(TokenType.TEXT, tokens.get(2).type());
    Assert.assertEquals(",", ((Text) tokens.get(2)).value());
    Assert.assertEquals(TokenType.BOOLEAN, tokens.get(3).type());
    Assert.assertEquals("true", ((Text) tokens.get(3)).value());
  }

  @Test
  public void testSetDirective() throws Exception {
    String recipe = "set-column :new_col body + '_suffix'";
    GrammarBasedParser parser = new GrammarBasedParser("test", Collections.singletonList(recipe), false);
    RecipeSymbol symbol = parser.parse();

    Iterator<TokenGroup> iterator = symbol.iterator();
    Assert.assertTrue(iterator.hasNext());
    TokenGroup group = iterator.next();
    List<Token> tokens = group.getTokens();
    Assert.assertEquals(3, tokens.size());
    Assert.assertEquals(TokenType.DIRECTIVE_NAME, tokens.get(0).type());
    Assert.assertEquals("set-column", ((DirectiveName) tokens.get(0)).value());
    Assert.assertEquals(TokenType.COLUMN_NAME, tokens.get(1).type());
    Assert.assertEquals("new_col", ((ColumnName) tokens.get(1)).value());
    Assert.assertEquals(TokenType.EXPRESSION, tokens.get(2).type());
    Assert.assertEquals("body + '_suffix'", ((Text) tokens.get(2)).value());
  }

  @Test
  public void testDropDirective() throws Exception {
    String recipe = "drop :col1 :col2";
    GrammarBasedParser parser = new GrammarBasedParser("test", Collections.singletonList(recipe), false);
    RecipeSymbol symbol = parser.parse();

    Iterator<TokenGroup> iterator = symbol.iterator();
    Assert.assertTrue(iterator.hasNext());
    TokenGroup group = iterator.next();
    List<Token> tokens = group.getTokens();
    Assert.assertEquals(3, tokens.size());
    Assert.assertEquals(TokenType.DIRECTIVE_NAME, tokens.get(0).type());
    Assert.assertEquals("drop", ((DirectiveName) tokens.get(0)).value());
    Assert.assertEquals(TokenType.COLUMN_NAME, tokens.get(1).type());
    Assert.assertEquals("col1", ((ColumnName) tokens.get(1)).value());
    Assert.assertEquals(TokenType.COLUMN_NAME, tokens.get(2).type());
    Assert.assertEquals("col2", ((ColumnName) tokens.get(2)).value());
  }

  @Test
  public void testAggregateStatsParsing() throws Exception {
    String recipe = "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec";
    GrammarBasedParser parser = new GrammarBasedParser("test", Collections.singletonList(recipe), false);
    RecipeSymbol symbol = parser.parse();

    Iterator<TokenGroup> iterator = symbol.iterator();
    Assert.assertTrue(iterator.hasNext());
    TokenGroup group = iterator.next();
    List<Token> tokens = group.getTokens();
    Assert.assertEquals(5, tokens.size());
    Assert.assertEquals(TokenType.DIRECTIVE_NAME, tokens.get(0).type());
    Assert.assertEquals("aggregate-stats", ((DirectiveName) tokens.get(0)).value());
    Assert.assertEquals(TokenType.COLUMN_NAME, tokens.get(1).type());
    Assert.assertEquals("data_transfer_size", ((ColumnName) tokens.get(1)).value());
    Assert.assertEquals(TokenType.COLUMN_NAME, tokens.get(2).type());
    Assert.assertEquals("response_time", ((ColumnName) tokens.get(2)).value());
    Assert.assertEquals(TokenType.COLUMN_NAME, tokens.get(3).type());
    Assert.assertEquals("total_size_mb", ((ColumnName) tokens.get(3)).value());
    Assert.assertEquals(TokenType.COLUMN_NAME, tokens.get(4).type());
    Assert.assertEquals("total_time_sec", ((ColumnName) tokens.get(4)).value());
  }

  @Test(expected = RecipeException.class)
  public void testInvalidAggregateStatsSyntax() throws Exception {
    String recipe = "aggregate-stats :size"; // Missing arguments
    GrammarBasedParser parser = new GrammarBasedParser("test", Collections.singletonList(recipe), false);
    parser.parse();
  }

  @Test
  public void testMultipleDirectives() throws Exception {
    List<String> recipes = Arrays.asList(
      "parse-as-json :body",
      "drop :temp_col",
      "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
    );
    GrammarBasedParser parser = new GrammarBasedParser("test", recipes, false);
    RecipeSymbol symbol = parser.parse();

    Iterator<TokenGroup> iterator = symbol.iterator();

    // First directive: parse-as-json
    Assert.assertTrue(iterator.hasNext());
    TokenGroup group1 = iterator.next();
    List<Token> tokens1 = group1.getTokens();
    Assert.assertEquals(2, tokens1.size());
    Assert.assertEquals("parse-as-json", ((DirectiveName) tokens1.get(0)).value());
    Assert.assertEquals("body", ((ColumnName) tokens1.get(1)).value());

    // Second directive: drop
    Assert.assertTrue(iterator.hasNext());
    TokenGroup group2 = iterator.next();
    List<Token> tokens2 = group2.getTokens();
    Assert.assertEquals(2, tokens2.size());
    Assert.assertEquals("drop", ((DirectiveName) tokens2.get(0)).value());
    Assert.assertEquals("temp_col", ((ColumnName) tokens2.get(1)).value());

    // Third directive: aggregate-stats
    Assert.assertTrue(iterator.hasNext());
    TokenGroup group3 = iterator.next();
    List<Token> tokens3 = group3.getTokens();
    Assert.assertEquals(5, tokens3.size());
    Assert.assertEquals("aggregate-stats", ((DirectiveName) tokens3.get(0)).value());
    Assert.assertEquals("data_transfer_size", ((ColumnName) tokens3.get(1)).value());
    Assert.assertEquals("response_time", ((ColumnName) tokens3.get(2)).value());
    Assert.assertEquals("total_size_mb", ((ColumnName) tokens3.get(3)).value());
    Assert.assertEquals("total_time_sec", ((ColumnName) tokens3.get(4)).value());

    Assert.assertFalse(iterator.hasNext());
  }

  @Test(expected = RecipeException.class)
  public void testUnknownDirective() throws Exception {
    String recipe = "unknown-directive :col";
    GrammarBasedParser parser = new GrammarBasedParser("test", Collections.singletonList(recipe), false);
    parser.parse();
  }

  @Test(expected = RecipeException.class)
  public void testEmptyRecipe() throws Exception {
    String recipe = "";
    GrammarBasedParser parser = new GrammarBasedParser("test", Collections.singletonList(recipe), false);
    parser.parse();
  }
}
