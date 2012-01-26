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

package org.addition.epanet.util;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Epanet exception codes handler.
 */
public class ENException extends Exception{

    /**
     * Error text bundle.
     */
    private static final ResourceBundle errorBundle = PropertyResourceBundle.getBundle("Error");

    /**
     * Array of arguments to be used in the error string creation.
     */
    private Object [] arguments;

    /**
     * Epanet error code.
     */
    private int codeID;

    /**
     * Get error code.
     * @return Code id.
     */
    public int getCodeID(){
        return codeID;
    }

    /**
     * Contructor from error code id.
     * @param id Error code id.
     */
    public ENException(int id){
        arguments = null;
        codeID = id;
    }

    /**
     * Contructor from error code id and multiple arguments.
     * @param id Error code id.
     * @param arg Extra arguments.
     */
    public ENException(int id, Object ... arg){
        codeID = id;
        arguments = arg;
    }

    /**
     * Contructor from other exception and multiple arguments.
     * @param e
     * @param arg
     */
    public ENException(ENException e, Object ... arg){
        arguments = arg;
        codeID = e.getCodeID();
    }

    /**
     * Get arguments array.
     */
    public Object [] getArguments(){
        return arguments;
    }

    /**
     * Handles the exception string conversion.
     * @return Final error string.
     */
    public String toString(){
        String str = errorBundle.getString("ERR"+codeID);
        if(str==null)
            return String.format("Unknown error message (%d)",codeID);
        else if(arguments!=null)
            return String.format(str,arguments);
        return str;
    }

}
