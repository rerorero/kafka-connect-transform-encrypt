// Generated from JsonPath.g4 by ANTLR 4.9.2

package com.github.rerorero.kafka.jsonpath;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link JsonPathParser}.
 */
public interface JsonPathListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#jsonpath}.
	 * @param ctx the parse tree
	 */
	void enterJsonpath(JsonPathParser.JsonpathContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#jsonpath}.
	 * @param ctx the parse tree
	 */
	void exitJsonpath(JsonPathParser.JsonpathContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#subscript}.
	 * @param ctx the parse tree
	 */
	void enterSubscript(JsonPathParser.SubscriptContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#subscript}.
	 * @param ctx the parse tree
	 */
	void exitSubscript(JsonPathParser.SubscriptContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#subscriptDot}.
	 * @param ctx the parse tree
	 */
	void enterSubscriptDot(JsonPathParser.SubscriptDotContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#subscriptDot}.
	 * @param ctx the parse tree
	 */
	void exitSubscriptDot(JsonPathParser.SubscriptDotContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#subscriptBracket}.
	 * @param ctx the parse tree
	 */
	void enterSubscriptBracket(JsonPathParser.SubscriptBracketContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#subscriptBracket}.
	 * @param ctx the parse tree
	 */
	void exitSubscriptBracket(JsonPathParser.SubscriptBracketContext ctx);
	/**
	 * Enter a parse tree produced by {@link JsonPathParser#arraySub}.
	 * @param ctx the parse tree
	 */
	void enterArraySub(JsonPathParser.ArraySubContext ctx);
	/**
	 * Exit a parse tree produced by {@link JsonPathParser#arraySub}.
	 * @param ctx the parse tree
	 */
	void exitArraySub(JsonPathParser.ArraySubContext ctx);
}