package pt.up.fe.comp.backend;

import org.specs.comp.ollir.*;

public class JasminUtils {
    public static String getMethodDescriptor(Method method) {
        StringBuilder descriptor = new StringBuilder("(");

        for (Element parameter: method.getParams()) {
            descriptor.append(JasminUtils.translateType(method.getOllirClass(), parameter.getType()));
        }

        descriptor.append(")").append(translateType(method.getOllirClass(), method.getReturnType()));

        return descriptor.toString();
    }

    public static String translateType(ClassUnit ollirClass, Type type) {
        ElementType elementType = type.getTypeOfElement();

        switch (elementType) {
            case ARRAYREF:
                return "[" + translateType(((ArrayType) type).getTypeOfElements());
            case OBJECTREF:
            case CLASS:
                return "L" + getFullClassName(ollirClass, ((ClassType) type).getName()) + ";";
            default:
                return translateType(elementType);
        }
    }

    private static String translateType(ElementType elementType) {
        switch (elementType) {
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case STRING:
                return "Ljava/lang/String;";
            case THIS:
                return "this";
            case VOID:
                return "V";
            default:
                return "";
        }
    }

    private static String getFullClassName(ClassUnit ollirClass, String className) {
        if (ollirClass.isImportedClass(className)) {
            for(String fullImport: ollirClass.getImports()) {
                int lastSeparatorIndex = className.lastIndexOf(".");

                if (lastSeparatorIndex < 0 && fullImport.equals(className)) {
                    return className;
                } else if (fullImport.substring(lastSeparatorIndex + 1).equals(className)) {
                    return fullImport;
                }
            }
        }

        return className;
    }
}
