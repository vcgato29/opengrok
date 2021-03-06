/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
 * Portions Copyright (c) 2017, Chris Fraire <cfraire@me.com>.
 */

package org.opensolaris.opengrok.analysis.sql;

import java.io.IOException;
import org.opensolaris.opengrok.analysis.JFlexSymbolMatcher;
import org.opensolaris.opengrok.util.StringUtils;
import org.opensolaris.opengrok.web.HtmlConsts;
%%
%public
%class SQLXref
%extends JFlexSymbolMatcher
%unicode
%ignorecase
%int
%char
%init{
    yyline = 1;
%init}
%include CommonLexer.lexh
%{
    private int commentLevel;

    @Override
    public void reset() {
        super.reset();
        commentLevel = 0;
    }

    @Override
    public void yypop() throws IOException {
        onDisjointSpanChanged(null, yychar);
        super.yypop();
    }
%}

Sign = "+" | "-"
SimpleNumber = [0-9]+ | [0-9]+ "." [0-9]* | [0-9]* "." [0-9]+
ScientificNumber = ({SimpleNumber} [eE] {Sign}? [0-9]+)
BinaryConstant = [0][xX] [0-9a-fA-F]*

Number = {Sign}? ({SimpleNumber} | {ScientificNumber} | {BinaryConstant})

Identifier = [a-zA-Z] [a-zA-Z0-9_]*

%state STRING QUOTED_IDENTIFIER SINGLE_LINE_COMMENT BRACKETED_COMMENT

%include Common.lexh
%include CommonURI.lexh
%%

<YYINITIAL> {
    {Identifier} {
        String id = yytext();
        onFilteredSymbolMatched(id, yychar, Consts.getReservedKeywords());
    }

    {Number} {
        onDisjointSpanChanged(HtmlConsts.NUMBER_CLASS, yychar);
        onNonSymbolMatched(yytext(), yychar);
        onDisjointSpanChanged(null, yychar);
    }

    [nN]? "'"    {
        String capture = yytext();
        String prefix = capture.substring(0, capture.length() - 1);
        String rest = capture.substring(prefix.length());
        onNonSymbolMatched(prefix, yychar);
        yypush(STRING);
        onDisjointSpanChanged(HtmlConsts.STRING_CLASS, yychar);
        onNonSymbolMatched(rest, yychar);
    }

    \"    {
        yypush(QUOTED_IDENTIFIER);
        onDisjointSpanChanged(HtmlConsts.STRING_CLASS, yychar);
        onNonSymbolMatched(yytext(), yychar);
    }

    "--"    {
        yypush(SINGLE_LINE_COMMENT);
        onDisjointSpanChanged(HtmlConsts.COMMENT_CLASS, yychar);
        onNonSymbolMatched(yytext(), yychar);
    }
}

<STRING> {
    "''"    { onNonSymbolMatched(yytext(), yychar); }
    "'"    {
        onNonSymbolMatched(yytext(), yychar);
        yypop();
    }
}

<QUOTED_IDENTIFIER> {
    \"\"    { onNonSymbolMatched(yytext(), yychar); }
    \"    {
        onNonSymbolMatched(yytext(), yychar);
        yypop();
    }
}

<SINGLE_LINE_COMMENT> {
    {WhspChar}*{EOL} {
        yypop();
        onEndOfLineMatched(yytext(), yychar);
    }
}

<YYINITIAL, BRACKETED_COMMENT> {
    "/*" {
        if (commentLevel++ == 0) {
            yypush(BRACKETED_COMMENT);
            onDisjointSpanChanged(HtmlConsts.COMMENT_CLASS, yychar);
        }
        onNonSymbolMatched(yytext(), yychar);
    }
}

<BRACKETED_COMMENT> {
    "*/" {
        onNonSymbolMatched(yytext(), yychar);
        if (--commentLevel == 0) {
            yypop();
        }
    }
}

<YYINITIAL, STRING, QUOTED_IDENTIFIER, SINGLE_LINE_COMMENT, BRACKETED_COMMENT> {
    {WhspChar}*{EOL}    { onEndOfLineMatched(yytext(), yychar); }
    [^\n]    { onNonSymbolMatched(yytext(), yychar); }
}

<STRING> {
    {BrowseableURI}    {
        onUriMatched(yytext(), yychar, SQLUtils.STRINGLITERAL_APOS_DELIMITER);
    }
}

<QUOTED_IDENTIFIER, SINGLE_LINE_COMMENT> {
    {BrowseableURI}    {
        onUriMatched(yytext(), yychar);
    }
}

<BRACKETED_COMMENT> {
    {BrowseableURI}    {
        onUriMatched(yytext(), yychar, StringUtils.END_C_COMMENT);
    }
}
