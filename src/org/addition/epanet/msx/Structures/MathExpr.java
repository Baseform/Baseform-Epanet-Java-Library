/*
 * Copyright (C) 2012  Addition, Lda. (addition at addition dot pt)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package org.addition.epanet.msx.Structures;



import org.addition.epanet.msx.VariableInterface;
import org.addition.epanet.util.Utilities;
import org.cheffo.jeplite.ASTVarNode;
import org.cheffo.jeplite.JEP;
import org.cheffo.jeplite.ParseException;
import org.cheffo.jeplite.SimpleNode;
import org.cheffo.jeplite.function.PostfixMathCommand;


import java.util.*;
import java.util.regex.*;
import java.util.regex.Pattern;

public class MathExpr {

    private JEP jeb;
    private Map<ASTVarNode,Integer> variables;
    private SimpleNode topNode;
    private static final Pattern PAT_NUMBER = Pattern.compile("^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$");

    static class MathExp_exp extends PostfixMathCommand{
        public MathExp_exp(){
            numberOfParameters = 1;
        }
        public final double operation(double[] params) throws ParseException{
            return Math.exp(params[0]);
        }
    }

    static class MathExp_sgn extends PostfixMathCommand{
        public MathExp_sgn(){
            numberOfParameters = 1;
        }
        public final double operation(double[] params) throws ParseException{
            return Utilities.getSignal(params[0]);
        }
    }

    static class MathExp_acot extends PostfixMathCommand{
        public MathExp_acot(){
            numberOfParameters = 1;
        }
        public final double operation(double[] params) throws ParseException{
            return  1.57079632679489661923 - Math.atan(params[0]);
        }
    }

    static class MathExp_sinh extends PostfixMathCommand{
        public MathExp_sinh(){
            numberOfParameters = 1;
        }
        public final double operation(double[] params) throws ParseException{
            return Math.sinh(params[0]);
        }
    }

    static class MathExp_cosh extends PostfixMathCommand{
        public MathExp_cosh(){
            numberOfParameters = 1;
        }
        public final double operation(double[] params) throws ParseException{
           return Math.cosh(params[0]);
        }
    }

    static class MathExp_tanh extends PostfixMathCommand{
        public MathExp_tanh(){
            numberOfParameters = 1;
        }
        public final double operation(double[] params) throws ParseException{
            return Math.tanh(params[0]);
        }
    }

    static class MathExp_coth extends PostfixMathCommand{
        public MathExp_coth(){
            numberOfParameters = 1;
        }
        public final double operation(double[] params) throws ParseException{
            return  (Math.exp(params[0])+Math.exp(-params[0]))/(Math.exp(params[0])-Math.exp(-params[0]));
        }
    }

    static class MathExp_log10 extends PostfixMathCommand{
        public MathExp_log10(){
            numberOfParameters = 1;
        }
        public final double operation(double[] params) throws ParseException{
            return  Math.log10(params[0]);
        }
    }

    static class MathExp_step extends PostfixMathCommand{
        public MathExp_step(){
            numberOfParameters = 1;
        }
        public final double operation(double[] params) throws ParseException{
            if (params[0] <= 0.0)
                return 0.0;
            else
                return 1.0;
        }
    }

    public MathExpr(){
        jeb = new JEP();
        jeb.addFunction("exp",new MathExp_exp());
        jeb.addFunction("sgn",new MathExp_sgn());
        jeb.addFunction("acot",new  MathExp_acot());
        jeb.addFunction("sinh",new  MathExp_sinh());
        jeb.addFunction("cosh",new  MathExp_cosh());
        jeb.addFunction("tanh",new  MathExp_tanh());
        jeb.addFunction("coth",new  MathExp_coth());
        jeb.addFunction("log10",new  MathExp_log10());
        jeb.addFunction("step",new  MathExp_step());
        jeb.addStandardConstants();
        jeb.addStandardFunctions();
        variables = new Hashtable<ASTVarNode,Integer>();
    }

    //public double evaluate(Chemical chem, boolean pipe){
    //    double res = 0;
    //
    //    for( Map.Entry<ASTVarNode,Integer> entry :  variables.entrySet()){
    //        if(pipe)
    //            entry.getKey().setValue(chem.getPipeVariableValue(entry.getValue()) );
    //        else
    //            entry.getKey().setValue(chem.getTankVariableValue(entry.getValue()) );
    //    }
    //
    //    try {
    //        return topNode.getValue();
    //    } catch (org.cheffo.jeplite.ParseException e) {
    //        return 0;
    //    }
    //}

    public double evaluatePipeExp(ExprVariable var){
        for( Map.Entry<ASTVarNode,Integer> entry :  variables.entrySet()){
            entry.getKey().setValue(var.getPipeVariableValue(entry.getValue()) );
        }

        try {
            return topNode.getValue();
        } catch (org.cheffo.jeplite.ParseException e) {
            return 0;
        }
    }

    public double evaluateTankExp(ExprVariable var){
        for( Map.Entry<ASTVarNode,Integer> entry :  variables.entrySet()){
            entry.getKey().setValue(var.getTankVariableValue(entry.getValue()) );
        }

        try {
            return topNode.getValue();
        } catch (org.cheffo.jeplite.ParseException e) {
            return 0;
        }
    }



    public static MathExpr create(String formula, VariableInterface var){
        MathExpr expr = new MathExpr();
        String [] colWords = formula.split("[\\W]");

        final List<String> mathFuncs = Arrays.asList(new String[]{"cos", "sin", "tan", "cot", "abs", "sgn",
                "sqrt", "log", "exp", "asin", "acos", "atan",
                "acot", "sinh", "cosh", "tanh", "coth", "log10",
                "step"});


        for(String word : colWords)
        {
            if(word.trim().length()!=0)  {

                if(!PAT_NUMBER.matcher(word).matches()){ // if it isn't a number
                    // its a word
                    if(!mathFuncs.contains(word.toLowerCase()))
                    {
                        expr.jeb.addVariable(word,0.0d);
                        ASTVarNode node = expr.jeb.getVarNode(word);

                        expr.variables.put(node,var.getIndex(word));
                    }
                    else  // it's a function
                    {
                        // it's an upper case function, convert to lower case
                        if(!word.equals(word.toLowerCase()))
                            formula = formula.replaceAll(word,word.toLowerCase());
                    }
                }

            }
        }

        expr.jeb.parseExpression(formula);
        expr.topNode = expr.jeb.getTopNode();

        return expr;
    }



}
