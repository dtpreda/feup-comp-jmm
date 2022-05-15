package pt.up.fe.comp.backend;

import org.specs.comp.ollir.*;

public class InstructionTranslator {
    public String translateInstruction(Instruction instruction, Method ancestorMethod, int indentationLevel) {
        InstructionType instructionType = instruction.getInstType();

        switch (instructionType) {
            case CALL:
                return translateInstruction((CallInstruction) instruction, ancestorMethod, indentationLevel);
            case RETURN:
                return translateInstruction((ReturnInstruction) instruction, indentationLevel);
            case PUTFIELD:
                return translateInstruction((PutFieldInstruction) instruction, ancestorMethod, indentationLevel);
            default:
                return "";
        }
    }

    public String translateInstruction(PutFieldInstruction instruction, Method ancestorMethod, int indentationLevel) {
        Element destinationObject = instruction.getFirstOperand();
        Element destinationField = instruction.getSecondOperand();

        if (destinationObject.isLiteral() || destinationField.isLiteral()) {
            return "THERE ARE NO FIELD LITERALS";
        }

        StringBuilder jasminInstruction = new StringBuilder();
        Element newFieldValue = instruction.getThirdOperand();

        jasminInstruction.append(getIndentation(indentationLevel)).append(getCorrespondingLoad(newFieldValue, ancestorMethod)).append("\n");
        jasminInstruction.append(getIndentation(indentationLevel)).append("putfield ");

        jasminInstruction.append(((Operand) destinationObject).getName()).append("/").append(((Operand) destinationField).getName());
        jasminInstruction.append(" ").append(JasminUtils.translateType(ancestorMethod.getOllirClass(), destinationField.getType()));

        return jasminInstruction.toString();
    }

    public String translateInstruction(AssignInstruction instruction) {
        return "";
    }

    public String translateInstruction(CallInstruction instruction, Method ancestorMethod, int indentationLevel) {
        StringBuilder jasminInstruction = new StringBuilder();
        StringBuilder parametersDescriptor = new StringBuilder();
        Operand caller = (Operand) instruction.getFirstArg();
        LiteralElement methodName = (LiteralElement) instruction.getSecondArg();

        CallType callType = instruction.getInvocationType();

        switch (callType) {
            case invokestatic:
            case invokevirtual:
            case invokespecial:

                for (Element element: instruction.getListOfOperands()) {
                    jasminInstruction.append(getIndentation(indentationLevel)).append(getCorrespondingLoad(element, ancestorMethod)).append("\n");
                    parametersDescriptor.append(JasminUtils.translateType(ancestorMethod.getOllirClass(), element.getType()));
                }

                jasminInstruction.append(getIndentation(indentationLevel));

                if (callType == CallType.invokestatic) {
                    jasminInstruction.append("invokestatic ").append(caller.getName());
                } else {
                    if (callType == CallType.invokevirtual){
                        jasminInstruction.append("invokevirtual ");
                    } else {
                        jasminInstruction.append("invokespecial ");
                    }

                    ClassType classType = (ClassType) instruction.getFirstArg().getType();
                    jasminInstruction.append(JasminUtils.getFullClassName(ancestorMethod.getOllirClass(), classType.getName()));
                }


                jasminInstruction.append(".").append(JasminUtils.trimLiteral(methodName.getLiteral()));
                jasminInstruction.append("(").append(parametersDescriptor);


                jasminInstruction.append(")").append(JasminUtils.translateType(ancestorMethod.getOllirClass(), instruction.getReturnType()));
                break;
            case NEW:
                break;
            case arraylength:
                break;
        }
        return jasminInstruction.toString();
    }

    public String translateInstruction(BinaryOpInstruction instruction) {
        return "";
    }

    public String translateInstruction(ReturnInstruction instruction, int indentationLevel) {
        StringBuilder jasminInstruction = new StringBuilder(getIndentation(indentationLevel));
        switch (instruction.getOperand().getType().getTypeOfElement()) {
            case BOOLEAN:
            case INT32:
                jasminInstruction.append("ireturn");
                break;
            case OBJECTREF:
            case CLASS:
            case STRING:
                jasminInstruction.append("areturn");
                break;
            case VOID:
                jasminInstruction.append("return");
                break;
        }

        return jasminInstruction.toString();
    }

    private String getCorrespondingLoad(Element element, Method ancestorMethod) {
        if (element.isLiteral()) {
            LiteralElement literalElement = (LiteralElement) element;

            switch (literalElement.getType().getTypeOfElement()) {
                case INT32:
                case STRING:
                    return "ldc " + JasminUtils.trimLiteral(literalElement.getLiteral());
                case BOOLEAN:
                    String literal = JasminUtils.trimLiteral(literalElement.getLiteral());
                    return literal.equals("true") ? "ldc 1" : "ldc 0";
                default:
                    return "";
            }
        } else {
            Operand operand = (Operand) element;

            Descriptor operandDescriptor = ancestorMethod.getVarTable().get(operand.getName());

            switch (operand.getType().getTypeOfElement()) {
                case INT32:
                case BOOLEAN:
                    return "iload " + operandDescriptor.getVirtualReg();
                case CLASS:
                case OBJECTREF:
                case THIS:
                case STRING:
                    return "aload " + operandDescriptor.getVirtualReg();
                default:
                    return "";
            }
        }
    }

    private String getIndentation(int indentationLevel) {
        return "\t".repeat(indentationLevel);
    }
}
