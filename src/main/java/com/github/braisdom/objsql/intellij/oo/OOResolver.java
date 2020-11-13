/* Copyright 2013 Artem Melentyev <amelentev@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.braisdom.objsql.intellij.oo;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.TypeConversionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OOResolver {
    public static final PsiType NoType = TypeConversionUtil.NULL_TYPE;

    private OOResolver() {
    }

    public static @NotNull PsiType getOOType(PsiBinaryExpression e) {
        if (e == null || e.getROperand() == null)
            return NoType;
        return getOOType(e.getLOperand().getType(), e.getROperand().getType(), e.getOperationSign());
    }

    public static @NotNull PsiType getOOType(PsiType left, PsiType right, PsiJavaToken op) {
        if (op == null) return NoType;
        String methodname = OOMethods.binary.get(op.getText());
        if (methodname != null && right != null) {
            PsiType res = resolveMethod(left, methodname, right);
            if (res == null)
                res = resolveMethod(right, methodname + OOMethods.revSuffix, left);
            if (res != null)
                return res;
        }
        return NoType;
    }

    public static @NotNull PsiType getOOType(PsiPrefixExpression e) {
        if (e == null || e.getOperand() == null)
            return NoType;
        String methodName = OOMethods.unary.get(e.getOperationSign().getText());
        if (methodName != null) {
            PsiType res = resolveMethod(e.getOperand(), methodName);
            if (res != null)
                return res;
        }
        return NoType;
    }

    public static @NotNull PsiType indexGet(PsiArrayAccessExpression e) {
        if (e == null || e.getIndexExpression() == null)
            return NoType;
        PsiType res = resolveMethod(e.getArrayExpression(), OOMethods.indexGet, e.getIndexExpression());
        return res != null ? res : NoType;
    }

    public static @NotNull PsiType indexSet(PsiArrayAccessExpression paa, PsiExpression value) {
        if (paa == null) return NoType;
        for (String method : OOMethods.indexSet) {
            PsiType res = resolveMethod(paa.getArrayExpression(), method, paa.getIndexExpression(), value);
            if (res != null) return res;
        }
        return NoType;
    }

    public static boolean isTypeConvertible(PsiType to, PsiExpression from) {
        if (from != null)
            for (String methodName : OOMethods.valueOf)
                if (resolveMethod(from.getProject(), from.getContext(), to.getCanonicalText(), methodName, from) != null)
                    return true;
        return false;
    }

    public static @Nullable PsiType resolveMethod(PsiExpression receiver, String methodName, @NotNull PsiExpression... args) {
        return resolveMethod(receiver.getProject(), receiver.getContext(), receiver.getText(), methodName, args);
    }

    public static @Nullable PsiType resolveMethod(Project proj, PsiElement context, String clas, String methodName, @NotNull PsiExpression... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(clas).append(".").append(methodName).append("(");
        boolean comma = false;
        for (PsiExpression arg : args) {
            if (comma)
                sb.append(",");
            sb.append(arg.getText());
            comma = true;
        }
        sb.append(")");
        PsiExpression exp = JavaPsiFacade.getElementFactory(proj).createExpressionFromText(sb.toString(), context);
        return exp.getType();
        /*if (clas == null || methodName == null) return null;
        PsiType[] argTypes = new PsiType[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null) return null;
            argTypes[i] = args[i].getType();
        }
        return resolveMethod(clas.getType(), methodName, argTypes);*/
    }

    public static @Nullable PsiType resolveMethod(@Nullable PsiType type, String methodName, @NotNull PsiType... argTypes) {
        if (!(type instanceof PsiClassType) || methodName == null) return null;
        for (PsiType a : argTypes)
            if (a == null)
                return null;
        PsiClassType clas = (PsiClassType) type;
        PsiSubstitutor subst = clas.resolveGenerics().getSubstitutor();
        PsiClass psiClass = clas.resolve();
        if (psiClass == null)
            return null;
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, true);
        for (PsiMethod method : methods) {
            PsiParameter[] parameters = method.getParameterList().getParameters();
            for (int i = 0; i < argTypes.length; i++) {
                PsiType argType = argTypes[i];
                if(i < parameters.length) {
                    PsiType methodArgType = parameters[i].getType();
                    if(argType.isConvertibleFrom(methodArgType))
                        return subst.substitute(method.getReturnType());
                } else return null;
            }
        }
        return null;
    }
}
