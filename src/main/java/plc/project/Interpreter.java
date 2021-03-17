package plc.project;

import javax.management.relation.RelationNotFoundException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        List<Environment.PlcObject> sourceList = new ArrayList<>();
        for(Ast.Field fields : ast.getFields()) {
            sourceList.add(visit(fields));
        }
        for(Ast.Method methods : ast.getMethods()) {
            visit(methods);
        }
        return scope.lookupFunction("main", 0).invoke(sourceList);
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        if(ast.getReceiver() instanceof Ast.Expr.Access) {
            Environment.PlcObject result = visit(ast.getReceiver());
            try {
                scope = scope.getParent();
                result.setField(((Ast.Expr.Access) ast.getReceiver()).getName(), visit(ast.getValue()));
            }
            catch(RuntimeException E){
                scope.defineVariable(((Ast.Expr.Access) ast.getReceiver()).getName(), visit(ast.getValue()));
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        if(requireType(Boolean.class, visit(ast.getCondition()))) {
            scope = new Scope(scope);
            for(Ast.Stmt stmt : ast.getThenStatements()) {
                visit(stmt);
            }
        }
        else {
            scope = new Scope(scope);
            for(Ast.Stmt stmt : ast.getElseStatements()) {
                visit(stmt);
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        Iterable iter = requireType(Iterable.class, visit(ast.getValue()));
        for (Object obj : iter) {
            try {
                scope = new Scope(scope);
                scope.defineVariable(ast.getName(), (Environment.PlcObject)obj);
                for(Ast.Stmt stmt : ast.getStatements()){
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while(requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for(Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if(ast.getLiteral() != null) { //check this condition
            return Environment.create(ast.getLiteral());
        }
        else {
            return Environment.NIL;
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        if(ast.getOperator().equals("AND")) {
            if(requireType(Boolean.class, visit(ast.getLeft()))) {
                if(requireType(Boolean.class, visit(ast.getRight()))) {
                    return Environment.create(Boolean.TRUE);
                }
                else {
                    return Environment.create(Boolean.FALSE);
                }
            }
            else {
                return Environment.create(Boolean.FALSE);
            }
        }
        else if(ast.getOperator().equals("OR")) {
            if(!requireType(Boolean.class, visit(ast.getLeft()))) {
                if(!requireType(Boolean.class, visit(ast.getRight()))) {
                    return Environment.create(Boolean.FALSE);
                }
                else {
                    return Environment.create(Boolean.TRUE);
                }
            }
            else {
                return Environment.create(Boolean.TRUE);
            }
        }
        else if(ast.getOperator().equals("<") || ast.getOperator().equals("<=") || ast.getOperator().equals(">") || ast.getOperator().equals(">=")) {
            Object left = visit(ast.getLeft()).getValue();
            Object right = visit(ast.getRight()).getValue();
            if(left.getClass() == right.getClass()) {
                if(left instanceof BigInteger) {
                    BigInteger leftBI = requireType(BigInteger.class, visit(ast.getLeft()));
                    BigInteger rightBI = requireType(BigInteger.class, visit(ast.getRight()));
                    int result = leftBI.compareTo(rightBI);
                    if(ast.getOperator().equals("<")) {
                        if(result < 0) {
                            return Environment.create(Boolean.TRUE);
                        }
                        else {
                            return Environment.create(Boolean.FALSE);
                        }
                    }
                    else if(ast.getOperator().equals("<=")) {
                        if(result <= 0) {
                            return Environment.create(Boolean.TRUE);
                        }
                        else {
                            return Environment.create(Boolean.FALSE);
                        }
                    }
                    else if(ast.getOperator().equals(">")) {
                        if(result > 0) {
                            return Environment.create(Boolean.TRUE);
                        }
                        else {
                            return Environment.create(Boolean.FALSE);
                        }
                    }
                    else if(ast.getOperator().equals(">=")) {
                        if(result >= 0) {
                            return Environment.create(Boolean.TRUE);
                        }
                        else {
                            return Environment.create(Boolean.FALSE);
                        }
                    }
                }
                else if(left.getClass() == BigDecimal.class) {
                    BigDecimal leftBD = requireType(BigDecimal.class, visit(ast.getLeft()));
                    BigDecimal rightBD = requireType(BigDecimal.class, visit(ast.getRight()));
                    int result = leftBD.compareTo(rightBD);
                    if(ast.getOperator().equals("<")) {
                        if(result < 0) {
                            return Environment.create(Boolean.TRUE);
                        }
                        else {
                            return Environment.create(Boolean.FALSE);
                        }
                    }
                    else if(ast.getOperator().equals("<=")) {
                        if(result <= 0) {
                            return Environment.create(Boolean.TRUE);
                        }
                        else {
                            return Environment.create(Boolean.FALSE);
                        }
                    }
                    else if(ast.getOperator().equals(">")) {
                        if(result > 0) {
                            return Environment.create(Boolean.TRUE);
                        }
                        else {
                            return Environment.create(Boolean.FALSE);
                        }
                    }
                    else if(ast.getOperator().equals(">=")) {
                        if(result >= 0) {
                            return Environment.create(Boolean.TRUE);
                        }
                        else {
                            return Environment.create(Boolean.FALSE);
                        }
                    }
                }
                throw new RuntimeException("type not valid for comparable");
            }
            throw new RuntimeException("type mismatch for comparable");
        }
        else if(ast.getOperator().equals("-")) {
            Object left = visit(ast.getLeft()).getValue();
            Object right = visit(ast.getRight()).getValue();
            if(left.getClass() == right.getClass()) {
                if(left.getClass() == BigInteger.class) {
                    BigInteger leftBI = requireType(BigInteger.class, visit(ast.getLeft()));
                    BigInteger rightBI = requireType(BigInteger.class, visit(ast.getRight()));
                    return Environment.create(leftBI.subtract(rightBI));
                }
                else if(left.getClass() == BigDecimal.class) {
                    BigDecimal leftBD = requireType(BigDecimal.class, visit(ast.getLeft()));
                    BigDecimal rightBD = requireType(BigDecimal.class, visit(ast.getRight()));
                    return Environment.create(leftBD.subtract(rightBD));
                }
                else {
                    throw new RuntimeException("Type not valid for -");
                }
            }
            throw new RuntimeException("Left and Right are not same type for -");
        }
        else if(ast.getOperator().equals("*")) {
            Object left = visit(ast.getLeft()).getValue();
            Object right = visit(ast.getRight()).getValue();
            if(left.getClass() == right.getClass()) {
                if(left.getClass() == BigInteger.class) {
                    BigInteger leftBI = requireType(BigInteger.class, visit(ast.getLeft()));
                    BigInteger rightBI = requireType(BigInteger.class, visit(ast.getRight()));
                    return Environment.create(leftBI.multiply(rightBI));
                }
                else if(left.getClass() == BigDecimal.class) {
                    BigDecimal leftBD = requireType(BigDecimal.class, visit(ast.getLeft()));
                    BigDecimal rightBD = requireType(BigDecimal.class, visit(ast.getRight()));
                    return Environment.create(leftBD.multiply(rightBD));
                }
                else {
                    throw new RuntimeException("Type not valid for *");
                }
            }
            throw new RuntimeException("Left and Right are not same type for *");
        }
        else if(ast.getOperator().equals("/")) {
            Object left = visit(ast.getLeft()).getValue();
            Object right = visit(ast.getRight()).getValue();
            if(left.getClass() == right.getClass()) {
                if(left.getClass() == BigInteger.class) {
                    BigInteger leftBI = requireType(BigInteger.class, visit(ast.getLeft()));
                    BigInteger rightBI = requireType(BigInteger.class, visit(ast.getRight()));
                    if(rightBI.equals(BigInteger.ZERO)) {
                        throw new RuntimeException("Cannot divide by 0 for Integer");
                    }
                    return Environment.create(leftBI.divide(rightBI));
                }
                else if(left.getClass() == BigDecimal.class) {
                    BigDecimal leftBD = requireType(BigDecimal.class, visit(ast.getLeft()));
                    BigDecimal rightBD = requireType(BigDecimal.class, visit(ast.getRight()));
                    if(rightBD.equals(BigDecimal.ZERO)) {
                        throw new RuntimeException("Cannot divide by 0 for Decimal");
                    }
                    return Environment.create(leftBD.divide(rightBD, RoundingMode.HALF_EVEN));
                }
                else {
                    throw new RuntimeException("Type not valid for /");
                }
            }
            throw new RuntimeException("Left and Right are not same type for /");
        }
        else if(ast.getOperator().equals("==") || ast.getOperator().equals("!=")) {
            Boolean result = visit(ast.getLeft()).getValue().equals(visit(ast.getRight()).getValue());
            if(ast.getOperator().equals("!=")) {
                result = !result;
            }
            return Environment.create(result);
        }
        else if(ast.getOperator().equals("+")) {
            Object left = visit(ast.getLeft()).getValue();
            Object right = visit(ast.getRight()).getValue();
            if(left.getClass() == right.getClass()) {
                if(left.getClass() == String.class) {
                    String leftStr = requireType(String.class, visit(ast.getLeft()));
                    String rightStr = requireType(String.class, visit(ast.getRight()));
                    return Environment.create(leftStr+rightStr);
                }
                else if(left.getClass() == BigInteger.class) {
                    BigInteger leftBI = requireType(BigInteger.class, visit(ast.getLeft()));
                    BigInteger rightBI = requireType(BigInteger.class, visit(ast.getRight()));
                    return Environment.create(leftBI.add(rightBI));
                }
                else if(left.getClass() == BigDecimal.class) {
                    BigDecimal leftBD = requireType(BigDecimal.class, visit(ast.getLeft()));
                    BigDecimal rightBD = requireType(BigDecimal.class, visit(ast.getRight()));
                    return Environment.create(leftBD.add(rightBD));
                }
                else {
                    throw new RuntimeException("Type not valid for +");
                }
            }
            throw new RuntimeException("Left and Right are not same type for +");
        }
        throw new RuntimeException("Binary broken dawg");
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            return Environment.create(scope.lookupVariable((String)visit(ast.getReceiver().get()).getValue()).getValue().getField(ast.getName()).getValue().getValue());
        }
        else {
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        /*if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            return Environment.create(scope.lookupFunction());
        }
        else {
            return scope.lookupFunction(ast.getName(), );
        }*/
        return Environment.NIL;
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
