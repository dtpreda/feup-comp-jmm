package pt.up.fe.comp.semantic.visitors.symbolTableLookup;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.semantic.tables.JmmSymbolTable;
import pt.up.fe.comp.semantic.visitors.ReportCollectorJmmNodeVisitor;

import java.util.List;
import java.util.Optional;

public class TypeCheckVisitor extends ReportCollectorJmmNodeVisitor<Type,Type> {

    JmmSymbolTable symbolTable;

    public TypeCheckVisitor(JmmSymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        addVisit("IntLiteral", this::visitIntLiteral);
        addVisit("BooleanLiteral", this::visitBooleanLiteral);
        addVisit("ThisLiteral", this::visitThisLiteral);
        addVisit("Identifier", this::visitIdentifier);
        addVisit("UnaryOp", this::visitUnaryOp);
        addVisit("BinOp", this::visitBinOp);
        addVisit("ArrayElement", this::visitArrayElement);
        addVisit("CompoundExpression", this::visitCompoundExpression);
        addVisit("Indexation", this::visitIndexation);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("AttributeGet", this::visitAttributeGet);
        addVisit("NewArray", this::visitNewArray);
        addVisit("NewObject",this::visitNewObject);
        addVisit("If", this::visitIf);
        addVisit("While", this::visitWhile);
        addVisit("Return", this::visitReturn);
        addVisit("Arguments", this::visitArguments);
        addVisit("VarDeclaration", this::visitVarDeclaration);

        setDefaultVisit(this::visitDefault);
    }

    private Type visitDefault(JmmNode node, Type dummy) {
        for (var child : node.getChildren()) {
            visit(child, dummy);
        }
        return null;
    }

    public Type visitIntLiteral(JmmNode node, Type dummy) { return new Type("int",false); }

    public Type visitBooleanLiteral(JmmNode node, Type dummy) { return new Type("boolean",false); }

    public Type visitThisLiteral(JmmNode node, Type type) { return new Type(symbolTable.getClassName(), false); }

    public Type visitIdentifier(JmmNode node, Type dummy) {
        if (node.getAncestor("MethodBody").isEmpty() && node.getAncestor("Return").isEmpty()) { return null; }

        if (node.getJmmParent().getKind().equals("MethodCall")) {
            if (isThisMethodCall(node.getJmmParent()) && symbolTable.getMethods().contains(node.get("name"))) {
                List<String> methods = symbolTable.getMethods();
                for (String method : methods) {
                    if (node.get("name").equals(method)) {
                        return symbolTable.getReturnType(method);
                    }
                }
            } else if (isThisMethodCall(node.getJmmParent()) && symbolTable.getSuper() != null) {
                return new Type("extern",false);
            } else if (!isThisMethodCall(node.getJmmParent())) {
                return new Type("extern",false);
            }
        } else {
            Optional<Symbol> child = symbolTable.getClosestSymbol(node, node.get("name"));
            if (child.isPresent()) {
                return child.get().getType();
            }

            for (var imp: symbolTable.getImports()) {
                String classImported = imp.substring(imp.lastIndexOf('.') + 1);
                if (classImported.equals(node.get("name"))) {
                    return new Type(classImported, false);
                }
            }
        }

        addSemanticErrorReport(node, "Identifier " + node.get("name") + " does not exists");
        return new Type("", false);
    }

    private Boolean isThisMethodCall(JmmNode methodCall) {
        Optional<JmmNode> compoundExpression = methodCall.getAncestor("CompoundExpression");
        if (compoundExpression.isPresent()) {
            JmmNode child = compoundExpression.get().getChildren().get(0);

            if (child.getKind().equals("ThisLiteral")) { return true; }

            Optional<Symbol> closest = symbolTable.getClosestSymbol(child, child.get("name"));
            if (closest.isPresent()) {
                return closest.get().getType().getName().equals(symbolTable.getClassName());
            }
        }
        return false;
    }

    public Type visitUnaryOp(JmmNode node, Type dummy) {
        Type childType = visit(node.getChildren().get(0), dummy);

        if ((childType.getName().equals("boolean") && !childType.isArray()) || childType.getName().equals("extern")) {
            return childType;
        }

        addSemanticErrorReport(node, "Incompatible types. Not operation expects a boolean");
        return new Type("", false);
    }

    private boolean isExtending(Type assigned, Type assignee) {
        return assignee.getName().equals(symbolTable.getClassName()) && assigned.getName().equals(symbolTable.getSuper());
    }

    private boolean isImported(Type type) {
        for (var imp: symbolTable.getImports()) {
            String classImported = imp.substring(imp.lastIndexOf('.') + 1);
            if (classImported.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    public Type visitBinOp(JmmNode node, Type dummy) {
        System.out.println("entrei binop");
        Type firstChildType = visit(node.getChildren().get(0), dummy);
        Type secondChildType = visit(node.getChildren().get(1), dummy);

        switch (node.get("op")) {
            case "assign":
                if (((!firstChildType.getName().equals(secondChildType.getName()) && !isExtending(firstChildType,secondChildType)) ||
                        firstChildType.isArray() != secondChildType.isArray()) &&
                        !(firstChildType.getName().equals("extern") || secondChildType.getName().equals("extern")) &&
                        !(isImported(firstChildType) && isImported(secondChildType))) {
                    addSemanticErrorReport(node,"olaType of the assignee must be compatible with the assigned");
                }
                return new Type("", false);

            case "and": case "or":
                if ((!firstChildType.getName().equals("extern") &&
                        (!firstChildType.getName().equals("boolean") || firstChildType.isArray())) ||
                    (!secondChildType.getName().equals("extern") &&
                        (!secondChildType.getName().equals("boolean") || secondChildType.isArray()))) {
                    addSemanticErrorReport(node,"Types are not compatible with the operation");
                }
                return new Type("boolean", false);

            case "lt":
                if ((!firstChildType.getName().equals("extern") &&
                        (!firstChildType.getName().equals("int") || firstChildType.isArray())) ||
                    (!secondChildType.getName().equals("extern") &&
                        (!secondChildType.getName().equals("int") || secondChildType.isArray()))) {
                    addSemanticErrorReport(node,"Types are not compatible with the operation");
                }
                return new Type("boolean", false);

            default:
                if ((!firstChildType.getName().equals("extern") &&
                        (!firstChildType.getName().equals("int") || firstChildType.isArray())) ||
                    (!secondChildType.getName().equals("extern") &&
                        (!secondChildType.getName().equals("int") || secondChildType.isArray()))) {
                    addSemanticErrorReport(node,"Types are not compatible with the operation");
                }
                return new Type("int", false);
        }
    }

    public Type visitArrayElement(JmmNode node, Type dummy) {
        Type firstChildType = visit(node.getChildren().get(0), dummy);
        Type secondChildType = visit(node.getChildren().get(1), dummy);

        if (!secondChildType.getName().equals("int")) {
            addSemanticErrorReport(node, "Array access index must be an expression of type integer");
        }
        return new Type(firstChildType.getName(), false);
    }

    public Type visitCompoundExpression(JmmNode node, Type dummy) {
        List<JmmNode> children = node.getChildren();
        Type type = dummy;
        for (JmmNode child : children) {
            type = visit(child, type);
            if (type.getName().isEmpty()) { break; }
        }
        return type;
    }

    public Type visitIndexation(JmmNode node, Type type) {
        boolean err = false;
        Type childType = visit(node.getChildren().get(0), type);

        if (!type.getName().equals("extern")) {
            if (!type.isArray()) {
                addSemanticErrorReport(node, "Array access is not done over an array");
                err = true;
            }
            if (!childType.getName().equals("int")) {
                addSemanticErrorReport(node, "Array access index must be an expression of type integer");
                err = true;
            }
            if (err) {
                return new Type("", false);
            }
        }

        return new Type(type.getName(),false); //only work for 1d array
    }

    public Type visitMethodCall(JmmNode node, Type type) {
        Type childType = visit(node.getChildren().get(0), type);
        if (!childType.getName().equals("extern")) {
            List<JmmNode> children = node.getChildren();
            for (int i = 1 ; i < children.size(); i++) {
                visit(children.get(i),type);
            }
        }
        return visit(node.getChildren().get(0), type);
    }

    public Type visitAttributeGet(JmmNode node, Type type) {
        if (type.getName().equals(symbolTable.getClassName())) {
            for (var field : symbolTable.getFields()) {
                if (field.getName().equals(node.getChildren().get(0).get("name"))) {
                    return field.getType();
                }
            }
            if (symbolTable.getSuper() == null) {
                addSemanticErrorReport(node, "Attribute " + node.getChildren().get(0).get("name") +
                        " of class " + type.getName() + " does not exist");
            } else {
                return new Type("extern", false);
            }
        } else {
            return new Type("extern", false);
        }
        return new Type("", false);
    }

    public Type visitNewArray(JmmNode node, Type type) {
        Type childType = visit(node.getChildren().get(0), type);

        if (!childType.getName().equals("extern")) {
            if (!childType.getName().equals("int")) {
                addSemanticErrorReport(node, "Array access index must be an expression of type integer");
            }
        }

        return new Type("int", true);
    }

    public Type visitNewObject(JmmNode node, Type type) {
        return new Type(node.get("class"), false);
    }

    public Type visitIf(JmmNode node, Type type) {
        Type condition = visit(node.getChildren().get(0), type);

        if (!condition.getName().equals("extern")) {
            if (!condition.getName().equals("boolean")) {
                addSemanticErrorReport(node, "IF condition must be of type boolean");
            }
        }

        return new Type("", false);
    }

    public Type visitWhile(JmmNode node, Type type) {
        Type condition = visit(node.getChildren().get(0), type);
        if (!condition.getName().equals("extern")) {
            if (!condition.getName().equals("boolean")) {
                addSemanticErrorReport(node, "WHILE condition must be of type boolean");
            }
        }

        return new Type("", false);
    }

    public Type visitReturn(JmmNode node, Type dummy) {
        System.out.println("entrei return");
        Type retType = visit(node.getChildren().get(0),dummy);
        Optional<JmmNode> regularMethod = node.getAncestor("RegularMethod");
        Type ret = new Type("", false);
        if (regularMethod.isPresent()) {
            for (var method : symbolTable.getMethods()) {
                if (method.equals(regularMethod.get().getChildren().get(1).get("name"))) {
                    ret = symbolTable.getReturnType(method);
                }
            }
        } else {
            addSemanticErrorReport(node, "Invalid return");
        }
        if (!(retType.getName().equals(ret.getName()) && retType.isArray() == ret.isArray()) && !retType.getName().equals("extern")) {
            addSemanticErrorReport(node, "Invalid return type");
        }
        return ret;
    }

    private Type visitArguments(JmmNode node, Type dummy) {
        Optional<JmmNode> methodCall = node.getAncestor("MethodCall");
        List<JmmNode> args = node.getChildren();
        if (methodCall.isPresent()) {
            System.out.println("tou");
            List<Symbol> params = symbolTable.getParameters(methodCall.get().getChildren().get(0).get("name"));
            if (params.size() != args.size()) {
                addSemanticErrorReport(node, "Invalid number of arguments");
            } else {
                System.out.println("tou2");
                for (int i = 0; i < args.size(); i++) {
                    Type paramType = params.get(i).getType();
                    Type argType = visit(args.get(i), dummy);
                    if (!argType.getName().equals("extern") && !argType.getName().equals(paramType.getName())) {
                        addSemanticErrorReport(node, "Invalid Argument of type " + argType.getName() +
                                ". Expected argument of type " + paramType.getName());
                    }
                }
            }
        } else {
            addSemanticErrorReport(node, "Invalid arguments");
        }
        return new Type("", false);
    }

    private Type visitVarDeclaration(JmmNode node, Type dummy) {
        Type type = new Type(node.getChildren().get(0).get("name"),node.getChildren().get(0).get("isArray").equals("true"));
        if (!type.getName().equals("int") && !type.getName().equals("boolean") && !type.getName().equals("String") &&
                !type.getName().equals(symbolTable.getClassName()) &&
                !type.getName().equals(symbolTable.getSuper()) &&
                !isImported(type)) {
            addSemanticErrorReport(node, "Class " + type.getName() + " does not exists");
        }
        return new Type("", false);
    }
}
