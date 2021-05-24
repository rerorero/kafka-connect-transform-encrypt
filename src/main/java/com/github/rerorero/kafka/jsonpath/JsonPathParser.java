// Generated from JsonPath.g4 by ANTLR 4.9.2

package com.github.rerorero.kafka.jsonpath;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class JsonPathParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		ROOT=1, WILDCARD=2, BRACKET_LEFT=3, BRACKET_RIGHT=4, SUBSCRIPT_DOT=5, 
		ID=6, STRING=7, NUMBER=8, WS=9;
	public static final int
		RULE_jsonpath = 0, RULE_subscript = 1, RULE_subscriptDot = 2, RULE_subscriptBracket = 3, 
		RULE_arraySub = 4;
	private static String[] makeRuleNames() {
		return new String[] {
			"jsonpath", "subscript", "subscriptDot", "subscriptBracket", "arraySub"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'$'", "'*'", "'['", "']'", "'.'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "ROOT", "WILDCARD", "BRACKET_LEFT", "BRACKET_RIGHT", "SUBSCRIPT_DOT", 
			"ID", "STRING", "NUMBER", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "JsonPath.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public JsonPathParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class JsonpathContext extends ParserRuleContext {
		public TerminalNode ROOT() { return getToken(JsonPathParser.ROOT, 0); }
		public TerminalNode EOF() { return getToken(JsonPathParser.EOF, 0); }
		public List<SubscriptContext> subscript() {
			return getRuleContexts(SubscriptContext.class);
		}
		public SubscriptContext subscript(int i) {
			return getRuleContext(SubscriptContext.class,i);
		}
		public JsonpathContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_jsonpath; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathListener ) ((JsonPathListener)listener).enterJsonpath(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathListener ) ((JsonPathListener)listener).exitJsonpath(this);
		}
	}

	public final JsonpathContext jsonpath() throws RecognitionException {
		JsonpathContext _localctx = new JsonpathContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_jsonpath);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(10);
			match(ROOT);
			setState(14);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==BRACKET_LEFT || _la==SUBSCRIPT_DOT) {
				{
				{
				setState(11);
				subscript();
				}
				}
				setState(16);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(17);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SubscriptContext extends ParserRuleContext {
		public SubscriptDotContext subscriptDot() {
			return getRuleContext(SubscriptDotContext.class,0);
		}
		public SubscriptBracketContext subscriptBracket() {
			return getRuleContext(SubscriptBracketContext.class,0);
		}
		public SubscriptContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subscript; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathListener ) ((JsonPathListener)listener).enterSubscript(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathListener ) ((JsonPathListener)listener).exitSubscript(this);
		}
	}

	public final SubscriptContext subscript() throws RecognitionException {
		SubscriptContext _localctx = new SubscriptContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_subscript);
		try {
			setState(21);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case SUBSCRIPT_DOT:
				enterOuterAlt(_localctx, 1);
				{
				setState(19);
				subscriptDot();
				}
				break;
			case BRACKET_LEFT:
				enterOuterAlt(_localctx, 2);
				{
				setState(20);
				subscriptBracket();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SubscriptDotContext extends ParserRuleContext {
		public TerminalNode SUBSCRIPT_DOT() { return getToken(JsonPathParser.SUBSCRIPT_DOT, 0); }
		public TerminalNode ID() { return getToken(JsonPathParser.ID, 0); }
		public ArraySubContext arraySub() {
			return getRuleContext(ArraySubContext.class,0);
		}
		public SubscriptDotContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subscriptDot; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathListener ) ((JsonPathListener)listener).enterSubscriptDot(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathListener ) ((JsonPathListener)listener).exitSubscriptDot(this);
		}
	}

	public final SubscriptDotContext subscriptDot() throws RecognitionException {
		SubscriptDotContext _localctx = new SubscriptDotContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_subscriptDot);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(23);
			match(SUBSCRIPT_DOT);
			setState(24);
			match(ID);
			setState(26);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				{
				setState(25);
				arraySub();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SubscriptBracketContext extends ParserRuleContext {
		public TerminalNode BRACKET_LEFT() { return getToken(JsonPathParser.BRACKET_LEFT, 0); }
		public TerminalNode STRING() { return getToken(JsonPathParser.STRING, 0); }
		public TerminalNode BRACKET_RIGHT() { return getToken(JsonPathParser.BRACKET_RIGHT, 0); }
		public ArraySubContext arraySub() {
			return getRuleContext(ArraySubContext.class,0);
		}
		public SubscriptBracketContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_subscriptBracket; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathListener ) ((JsonPathListener)listener).enterSubscriptBracket(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathListener ) ((JsonPathListener)listener).exitSubscriptBracket(this);
		}
	}

	public final SubscriptBracketContext subscriptBracket() throws RecognitionException {
		SubscriptBracketContext _localctx = new SubscriptBracketContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_subscriptBracket);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(28);
			match(BRACKET_LEFT);
			setState(29);
			match(STRING);
			setState(30);
			match(BRACKET_RIGHT);
			setState(32);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				{
				setState(31);
				arraySub();
				}
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArraySubContext extends ParserRuleContext {
		public TerminalNode BRACKET_LEFT() { return getToken(JsonPathParser.BRACKET_LEFT, 0); }
		public TerminalNode BRACKET_RIGHT() { return getToken(JsonPathParser.BRACKET_RIGHT, 0); }
		public TerminalNode NUMBER() { return getToken(JsonPathParser.NUMBER, 0); }
		public TerminalNode WILDCARD() { return getToken(JsonPathParser.WILDCARD, 0); }
		public ArraySubContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arraySub; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathListener ) ((JsonPathListener)listener).enterArraySub(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof JsonPathListener ) ((JsonPathListener)listener).exitArraySub(this);
		}
	}

	public final ArraySubContext arraySub() throws RecognitionException {
		ArraySubContext _localctx = new ArraySubContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_arraySub);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(34);
			match(BRACKET_LEFT);
			setState(35);
			_la = _input.LA(1);
			if ( !(_la==WILDCARD || _la==NUMBER) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(36);
			match(BRACKET_RIGHT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\13)\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\3\2\3\2\7\2\17\n\2\f\2\16\2\22\13\2\3\2\3\2"+
		"\3\3\3\3\5\3\30\n\3\3\4\3\4\3\4\5\4\35\n\4\3\5\3\5\3\5\3\5\5\5#\n\5\3"+
		"\6\3\6\3\6\3\6\3\6\2\2\7\2\4\6\b\n\2\3\4\2\4\4\n\n\2\'\2\f\3\2\2\2\4\27"+
		"\3\2\2\2\6\31\3\2\2\2\b\36\3\2\2\2\n$\3\2\2\2\f\20\7\3\2\2\r\17\5\4\3"+
		"\2\16\r\3\2\2\2\17\22\3\2\2\2\20\16\3\2\2\2\20\21\3\2\2\2\21\23\3\2\2"+
		"\2\22\20\3\2\2\2\23\24\7\2\2\3\24\3\3\2\2\2\25\30\5\6\4\2\26\30\5\b\5"+
		"\2\27\25\3\2\2\2\27\26\3\2\2\2\30\5\3\2\2\2\31\32\7\7\2\2\32\34\7\b\2"+
		"\2\33\35\5\n\6\2\34\33\3\2\2\2\34\35\3\2\2\2\35\7\3\2\2\2\36\37\7\5\2"+
		"\2\37 \7\t\2\2 \"\7\6\2\2!#\5\n\6\2\"!\3\2\2\2\"#\3\2\2\2#\t\3\2\2\2$"+
		"%\7\5\2\2%&\t\2\2\2&\'\7\6\2\2\'\13\3\2\2\2\6\20\27\34\"";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}