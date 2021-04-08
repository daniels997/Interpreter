package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for(Ast.Field fields : ast.getFields()) {
            visit(fields);
            scope.lookupFunction("main", 0);
            if(scope.lookupFunction("main", 0).getReturnType() != Environment.Type.INTEGER) {
                throw new RuntimeException("Does not have an Integer Return type");
            }
        }
        for(Ast.Method methods : ast.getMethods()) {
            visit(methods);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        visit(ast.getValue());
        requireAssignable(Environment.Type.INTEGER_ITERABLE, ast.getValue().getType());
        if(ast.getStatements().size() == 0) {
            throw new RuntimeException("The list of statements in the for loop is empty");
        }
        try {
            scope = new Scope(scope);
            scope.defineVariable(Environment.Type.INTEGER.getName(), Environment.Type.INTEGER.getJvmName(), Environment.Type.INTEGER, Environment.NIL);
            for(Ast.Stmt stmt : ast.getStatements()){
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        visit(ast.getReceiver().get());

        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());

        }
        else {
            scope.lookupVariable(ast.getName()).getValue();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if(target.getJvmName() != type.getJvmName()) {
            if (target.getName() == "Any") {
                return;
            }
            else if (target.getName().equals("Comparable")) {
                if (type.getName() == "Integer" || type.getName() == "Decimal" || type.getName() == "Character" || type.getName() == "String") {
                    return;
                }
            }
            else {
                throw new RuntimeException("Expected target " + target.getName() + ", received " + type.getName() + ".");
            }
        }
    }

}