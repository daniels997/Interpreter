package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fieldList = new ArrayList<>();
        List<Ast.Method> methodList = new ArrayList<>();
        while(match("LET")) {
            fieldList.add(parseField());
        }
        while(match("DEF")) {
            methodList.add(parseMethod());
        }
        Ast.Source source = new Ast.Source(fieldList, methodList);
        return source;
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            Optional<Ast.Expr> expression = Optional.empty();
            if(match("=")) {
                expression = Optional.of(parseExpression());
            }
            if (match(";")) {
                Ast.Field field = new Ast.Field(name, expression);
                return field;
            }
            else {
                throw new ParseException("Error with semicolon in parseField", tokens.index);
            }
        }
        else {
            throw new ParseException("Error with identifier in parseField", tokens.index);
        }
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        if (match(Token.Type.IDENTIFIER,"(")) {
            String name = tokens.get(-2).getLiteral();
            List<String> parameters = new ArrayList<>();
            List<Ast.Stmt> statements = new ArrayList<>();
            if (match(Token.Type.IDENTIFIER)) {
                parameters.add(tokens.get(-1).getLiteral());
                while(match(",")) {
                    if(match(Token.Type.IDENTIFIER)) {
                        parameters.add(tokens.get(-1).getLiteral());
                    }
                    else {
                        throw new ParseException("No identifier in parseMethod", tokens.index);
                    }
                }
            }
            if (match(")","DO")) {
                while(!match("END")) {
                    if (tokens.tokens.size() == tokens.index) {
                        throw new ParseException("No 'END' found", tokens.index);
                    }
                    statements.add(parseStatement());
                }
            }
            Ast.Method method = new Ast.Method(name, parameters, statements);
            return method;
        }
        else {
            throw new ParseException("No name in parseMethod", tokens.index);
        }
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        if(match("LET")) {
            return parseDeclarationStatement();
        }
        else if (match("IF")) {
            return parseIfStatement();
        }
        else if (match("FOR")) {
            return parseForStatement();
        }
        else if (match("WHILE")) {
            return parseWhileStatement();
        }
        else if (match("RETURN")) {
            return parseReturnStatement();
        }
        Ast.Expr expression = parseExpression();
        Ast.Expr value = null;
        Ast.Stmt statement = null;
        if(match("=")) {
            value = parseExpression();
        }
        if(value != null) {
            statement = new Ast.Stmt.Assignment(expression, value);
        }
        else {
            statement = new Ast.Stmt.Expression(expression);
        }
        if (match(";")) {
            return statement;
        }
        else {
            throw new ParseException("No semicolon for Assignment", tokens.index);
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            Optional<Ast.Expr> expression = Optional.empty();
            if (match("=")) {
                expression = Optional.of(parseExpression());
            }
            if (match(";")) {
                Ast.Stmt.Declaration statement = new Ast.Stmt.Declaration(name, expression);
                return statement;
            }
            else {
                throw new ParseException("Missing Semicolon for declaration", tokens.index);
            }
        }
        else {
            throw new ParseException("Missing identifier in declaration", tokens.index);
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        Ast.Expr expression = parseExpression();
        if (match("DO")) {
            List<Ast.Stmt> thenStatements = new ArrayList<>();
            List<Ast.Stmt> elseStatements = new ArrayList<>();
            while(!peek("ELSE") || !peek("END")) {
                if (tokens.tokens.size() == tokens.index) {
                    throw new ParseException("No 'ELSE or END' found", tokens.index);
                }
                thenStatements.add(parseStatement());
                if(!peek("ELSE") || !peek("END")) {
                    break;
                }
            }
            if (match("ELSE")) {
                while(!peek("END")) {
                    if (tokens.tokens.size() == tokens.index) {
                        throw new ParseException("No 'END' found", tokens.index);
                    }
                    elseStatements.add(parseStatement());
                }
            }
            if (match("END")) {
                Ast.Stmt.If ifStatement = new Ast.Stmt.If(expression, thenStatements, elseStatements);
                return ifStatement;
            }
            else {
                throw new ParseException("END not found for IF statement", tokens.index);
            }
        }
        else {
            throw new ParseException("Do not found for IF statement", tokens.index);
        }
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        if (match(Token.Type.IDENTIFIER, "IN")) {
            String name = tokens.get(-2).getLiteral();
            Ast.Expr expression = parseExpression();
            List<Ast.Stmt> statements = new ArrayList<>();
            if (match("DO")) {
                while(!match("END")) {
                    if (tokens.tokens.size() == tokens.index) {
                        throw new ParseException("No 'END' found in FOR", tokens.index);
                    }
                    statements.add(parseStatement());
                }
                Ast.Stmt.For forStatement = new Ast.Stmt.For(name, expression, statements);
                return forStatement;
            }
            else {
                throw new ParseException("No 'DO' in FOR", tokens.index);
            }
        }
        else {
            throw new ParseException("No 'IN' in FOR", tokens.index);
        }
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        Ast.Expr expression = parseExpression();
        if (match("DO")) {
            List<Ast.Stmt> statements = new ArrayList<>();
            while(!peek("END")) {
                if (tokens.tokens.size() == tokens.index) {
                    throw new ParseException("No 'END' found", tokens.index);
                }
                statements.add(parseStatement());
            }
            if (match("END")) {
                Ast.Stmt.While whileStatement = new Ast.Stmt.While(expression, statements);
                return whileStatement;
            }
            else {
                throw new ParseException("END not found for While statement", tokens.index);
            }
        }
        else {
            throw new ParseException("Do not found for While statement", tokens.index);
        }
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        Ast.Expr expression = parseExpression();
        if(match(";")) {
            Ast.Stmt.Return returnStatement = new Ast.Stmt.Return(expression);
            return returnStatement;
        }
        else {
            throw new ParseException("Bruh you need a semicolon dawhg", tokens.index);
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        Ast.Expr expression1 = parseEqualityExpression();
        if(match("AND") || match("OR")) {
            String name = tokens.get(-1).getLiteral();
            Ast.Expr expression2 = parseLogicalExpression();
            Ast.Expr.Binary binary = new Ast.Expr.Binary(name, expression1, expression2);
            return binary;
        }
        return expression1;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        Ast.Expr expression1 = parseAdditiveExpression();
        if(match("<") || match("<=")|| match(">")|| match(">=")|| match("==")|| match("!=")) {
            String name = tokens.get(-1).getLiteral();
            Ast.Expr expression2 = parseEqualityExpression();
            Ast.Expr.Binary binary = new Ast.Expr.Binary(name, expression1, expression2);
            return binary;
        }
        return expression1;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr expression1 = parseMultiplicativeExpression();
        if(match("+") || match("-")) {
            String name = tokens.get(-1).getLiteral();
            Ast.Expr expression2 = parseAdditiveExpression();
            Ast.Expr.Binary binary = new Ast.Expr.Binary(name, expression1, expression2);
            return binary;
        }
        return expression1;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr expression1 = parseSecondaryExpression();
        if(match("*") || match("/")) {
            String name = tokens.get(-1).getLiteral();
            Ast.Expr expression2 = parseMultiplicativeExpression();
            Ast.Expr.Binary binary = new Ast.Expr.Binary(name, expression1, expression2);
            return binary;
        }
        return expression1;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    /*Optional<Ast.Expr> receiver;
    private final String name;
    private final List<Ast.Expr> arguments;*/
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        if(!peek(Token.Type.IDENTIFIER,".")) {
            return parsePrimaryExpression();
        }
        tokens.advance();
        Ast.Expr expression2 = new Ast.Expr.Access(Optional.empty(), tokens.get(-1).getLiteral());
        while (match(".")) {
            if (match(Token.Type.IDENTIFIER)) {
                String name = tokens.get(-1).getLiteral();
                if(match("(")) {
                    List<Ast.Expr> arguments = new ArrayList<>();
                    if(!peek(")")) {
                        arguments.add(parseExpression());
                        while (match(",")) {
                            arguments.add(parseExpression());
                        }
                    }
                    if (match(")")) {
                        Ast.Expr.Function functionExpression = new Ast.Expr.Function(Optional.of(expression2), name, arguments);
                        expression2 = functionExpression;
                    }
                    else {
                        throw new ParseException("Missing Closing Parenthesis in Secondary Expression", tokens.index);
                    }
                }
                else {
                    Ast.Expr.Access accessExpression = new Ast.Expr.Access(Optional.of(expression2), name);
                    expression2 = accessExpression;
                }
            }
            else {
                throw new ParseException("Missing Expression in secondary expression", tokens.index);
            }
        }
        return expression2;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        if(match("NIL")) {
            return new Ast.Expr.Literal(null);
        }
        else if (match("TRUE")) {
            return new Ast.Expr.Literal(Boolean.TRUE);
        }
        else if (match("FALSE")) {
            return new Ast.Expr.Literal(Boolean.FALSE);
        }
        else if (match(Token.Type.INTEGER)) {
            return new Ast.Expr.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expr.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }
        else if (match(Token.Type.CHARACTER)) {
            if(tokens.get(-1).getLiteral().length() == 3) {
                return new Ast.Expr.Literal(new Character(tokens.get(-1).getLiteral().charAt(1)));
            }
            else if(tokens.get(-1).getLiteral().length() == 4) {
                String stringName = tokens.get(-1).getLiteral().replace("\\\\", "\\");
                stringName = stringName.replace("\\b","\b");
                stringName = stringName.replace("\\n","\n");
                stringName = stringName.replace("\\r","\r");
                stringName = stringName.replace("\\t","\t");
                stringName = stringName.replace("\\'","\'");
                stringName = stringName.replace("\\\"","\"");
                return new Ast.Expr.Literal(new Character(stringName.charAt(1)));
            }
        }
        else if(match(Token.Type.STRING)) {
            String stringName = tokens.get(-1).getLiteral().replace("\\\\", "\\");
            stringName = stringName.replace("\\b","\b");
            stringName = stringName.replace("\\n","\n");
            stringName = stringName.replace("\\r","\r");
            stringName = stringName.replace("\\t","\t");
            stringName = stringName.replace("\\'","\'");
            stringName = stringName.replace("\\\"","\"");
            return new Ast.Expr.Literal(stringName.substring(1,stringName.length()-1));
        }
        else if (match("(")) {
            Ast.Expr expression = parseExpression();
            if(match(")")) {
                return new Ast.Expr.Group(expression);
            }
            else {
                throw new ParseException("Need closing parenthesis", tokens.index);
            }
        }
        else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            if(match("(")) {
                List<Ast.Expr> expressionList = new ArrayList<>();
                if(!peek(")")) {
                    expressionList.add(parseExpression());
                    while(match(",")) {
                        expressionList.add(parseExpression());
                    }
                }
                if(match(")")) {
                    return new Ast.Expr.Function(Optional.empty(), name, expressionList);
                }
                else {
                    throw new ParseException("Need closing parenthesis", tokens.index);
                }
            }
            else {
                return new Ast.Expr.Access(Optional.empty(), name);
            }
        }
        else {
            throw new ParseException("No expression was matched", tokens.index);
        }
        return null;
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    public boolean peek(Object... patterns) {
        for(int i = 0; i < patterns.length; i++) {
            if(!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    public boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if(peek) {
            for(int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
