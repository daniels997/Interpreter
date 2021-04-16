package plc.project;

import java.io.PrintWriter;

public final class  Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        writer.write("public class Main {");
        newline(0);
        newline(1);
        indent++;
        writer.write("public static void main(String[] args) {");
        newline(2);
        indent++;
        if(!ast.getFields().isEmpty()) {
            for (Ast.Field field  : ast.getFields()) {
                visit(field);
            }
        }
        writer.write("System.exit(new Main().main());");
        newline(1);
        indent--;
        writer.write("}");
        newline(0);
        indent = 0;
        newline(1);
        indent++;
        if(!ast.getMethods().isEmpty()) {
            for (Ast.Method method  : ast.getMethods()) {
                visit(method);
            }
        }
        newline(0);
        indent = 0;
        newline(0);
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        writer.write(ast.getTypeName() + " " + ast.getName());
        ast.getValue().ifPresent( value -> {
            writer.write(" = ");
            visit(value);
        });
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        writer.write(ast.getFunction().getReturnType().getJvmName() + " " + ast.getName() + "(");
        for (int i = 0; i < ast.getParameters().size(); i++) {
            writer.write(ast.getParameterTypeNames().get(i) + " " + ast.getParameters().get(i));
            if (i != ast.getParameters().size() - 1)
                writer.write(", ");
        }
        writer.write(") {");
        newline(indent + 1);
        indent++;
        for (Ast.Stmt stmt : ast.getStatements()) {
            visit(stmt);
            if (stmt != ast.getStatements().get(ast.getStatements().size() - 1))
                newline(indent);
        }
        newline(indent - 1);
        indent--;
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        writer.write(ast.getVariable().getType().getJvmName() + " " + ast.getName());
        ast.getValue().ifPresent( value -> {
            writer.write(" = ");
            visit(value);
        });
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        visit(ast.getReceiver());
        writer.write(" = ");
        visit(ast.getValue());
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        writer.write("if (");
        visit(ast.getCondition());
        writer.write(") {");
        newline(indent + 1);
        indent++;
        for(Ast.Stmt stmt : ast.getThenStatements()) {
            visit(stmt);
            if (stmt != ast.getThenStatements().get(ast.getThenStatements().size() - 1))
                newline(indent);
        }
        newline(indent - 1);
        indent--;
        writer.write("}");
        if (!ast.getElseStatements().isEmpty()) {
            writer.write(" else {");
            newline(indent + 1);
            indent++;
            for(Ast.Stmt stmt : ast.getElseStatements()) {
                visit(stmt);
                if (stmt != ast.getElseStatements().get(ast.getElseStatements().size() - 1))
                    newline(indent);
            }
            newline(indent - 1);
            indent--;
            writer.write("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        writer.write("for (int " + ast.getName() + " : ");
        visit(ast.getValue());
        writer.write(") {");
        newline(indent + 1);
        indent++;
        for (Ast.Stmt stmt : ast.getStatements()) {
            visit(stmt);
            if (stmt != ast.getStatements().get(ast.getStatements().size() - 1))
                newline(indent);
        }
        newline(indent - 1);
        indent--;
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        writer.write("while (");
        visit(ast.getCondition());
        writer.write(") {");
        newline(indent + 1);
        indent++;
        for(Ast.Stmt stmt : ast.getStatements()) {
            visit(stmt);
            if (stmt != ast.getStatements().get(ast.getStatements().size() - 1))
                newline(indent);
        }
        newline(indent - 1);
        indent--;
        writer.write("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        writer.write("return ");
        visit(ast.getValue());
        writer.write(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if (ast.getType().equals(Environment.Type.STRING)) {
            writer.write("\"" + ast.getLiteral() + "\"");
        }
        else if (ast.getType().equals(Environment.Type.CHARACTER)) {
            writer.write("'" + ast.getLiteral() + "'");
        }
        else {
            writer.write(ast.getLiteral().toString());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        writer.write("(");
        visit(ast.getExpression());
        writer.write(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        visit(ast.getLeft());
        if (ast.getOperator().equals("AND"))
            writer.write(" && ");
        else if (ast.getOperator().equals("OR")) {
            writer.write(" || ");
        }
        else {
            writer.write(" " + ast.getOperator() + " ");
        }
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        writer.write(ast.getVariable().getJvmName());
        ast.getReceiver().ifPresent( receiver -> {
            writer.write("." + receiver);
        });
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        ast.getReceiver().ifPresent( receiver -> {
            visit(receiver);
            writer.write(".");
        });
        writer.write(ast.getFunction().getJvmName() + "(");
        for (Ast.Expr expr : ast.getArguments()) {
            visit(expr);
            if (expr != ast.getArguments().get(ast.getArguments().size() - 1))
                writer.write(", ");
        }
        writer.write(")");
        return null;
    }

}
