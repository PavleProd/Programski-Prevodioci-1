package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{
	// pravljenje tokena na osnovu tipa
	// takodje dodajemo informaciju o liniji i koloni
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn);
	}
	
	// pravljenje tokena na osnovu tipa i vrednosti
	// takodje dodajemo informaciju o liniji i koloni
	private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn, value);
	}

%}

%cup
%line
%column

%xstate COMMENT

%eofval{
	return new_symbol(sym.EOF);
%eofval}

%%

" " 	{ }
"\b" 	{ }
"\t" 	{ }
"\r\n" 	{ }
"\f" 	{ }

"program"   { return new_symbol(sym.PROGRAM, yytext()); }
"break"		{ return new_symbol(sym.BREAK, yytext()); }
"continue"	{ return new_symbol(sym.CONTINUE, yytext()); }
"if"		{ return new_symbol(sym.IF, yytext()); }
"else"		{ return new_symbol(sym.ELSE, yytext()); }
"const"		{ return new_symbol(sym.CONST, yytext()); }
"new"		{ return new_symbol(sym.NEW, yytext()); }
"print"		{ return new_symbol(sym.PRINT, yytext()); }
"read"		{ return new_symbol(sym.READ, yytext()); }
"return"	{ return new_symbol(sym.RETURN, yytext()); }
"void"		{ return new_symbol(sym.VOID, yytext()); }
"for"		{ return new_symbol(sym.FOR, yytext()); }
"static"		{ return new_symbol(sym.STATIC, yytext()); }
"namespace"		{ return new_symbol(sym.NAMESPACE, yytext()); }

"class"		{ return new_symbol(sym.CLASS, yytext()); }
"extends"	{ return new_symbol(sym.CONST, yytext()); }

"{"		{ return new_symbol(sym.LBRACE, yytext()); }
"}"		{ return new_symbol(sym.RBRACE, yytext()); }
"("		{ return new_symbol(sym.LPAREN, yytext()); }
")"		{ return new_symbol(sym.RPAREN, yytext()); }
"["		{ return new_symbol(sym.LBRACK, yytext()); }
"]"		{ return new_symbol(sym.RBRACK, yytext()); }

"+"		{ return new_symbol(sym.PLUS, yytext()); }
"-"		{ return new_symbol(sym.MINUS, yytext()); }
"*"		{ return new_symbol(sym.MUL, yytext()); }
"/"		{ return new_symbol(sym.DIV, yytext()); }
"%"		{ return new_symbol(sym.MOD, yytext()); }

"="		{ return new_symbol(sym.EQUAL, yytext()); }
"=="		{ return new_symbol(sym.DOUBLE_EQUAL, yytext()); }
"!="		{ return new_symbol(sym.NOT_EQUAL, yytext()); }

">"		{ return new_symbol(sym.GT, yytext()); }
">="		{ return new_symbol(sym.GTE, yytext()); }
"<"		{ return new_symbol(sym.LT, yytext()); }
"<="		{ return new_symbol(sym.LTE, yytext()); }
"&&"		{ return new_symbol(sym.AND, yytext()); }
"||"		{ return new_symbol(sym.OR, yytext()); }

"++"		{ return new_symbol(sym.INC, yytext()); }
"--"		{ return new_symbol(sym.DEC, yytext()); }

";"		{ return new_symbol(sym.SEMICOLON, yytext()); }
":"		{ return new_symbol(sym.COLON, yytext()); }
","		{ return new_symbol(sym.COMMA, yytext()); }
"."		{ return new_symbol(sym.DOT, yytext()); }


<YYINITIAL> "//" { yybegin(COMMENT); }
<COMMENT> .      { yybegin(COMMENT); }
<COMMENT> "\r\n" { yybegin(YYINITIAL); }

[0-9]+  { return new_symbol(sym.NUMBER, new Integer (yytext())); }
"true"|"false" { return new_symbol(sym.BOOL, new Boolean (yytext().equals("true") ? true : false)); }
"'"."'" {return new_symbol(sym.CHAR, new Character(yytext().charAt(1))); }

([a-z]|[A-Z])[a-z|A-Z|0-9|_]* 	{return new_symbol (sym.IDENT, yytext()); }

. { System.err.println("Leksicka greska ("+yytext()+") u liniji "+(yyline+1)); }






