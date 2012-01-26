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

package org.addition.epanet.msx;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Logger;

import org.addition.epanet.msx.EnumTypes.*;
import org.addition.epanet.msx.Structures.MathExpr;
import org.addition.epanet.msx.Structures.Source;

public class InpReader {

    Logger log;
    private static final int MAXERRS = 100; // Max. input errors reported

    private Network MSX;
    private ENToolkit2 epanet;
    private Project project;

    public void loadDependencies(EpanetMSX epa) {
        this.MSX = epa.getNetwork();
        this.epanet = epa.getENToolkit();
        this.project = epa.getProject();
    }

    // Error codes (401 - 409)
    private enum InpErrorCodes {
        INP_ERR_FIRST       (400),
        ERR_LINE_LENGTH     (401),
        ERR_ITEMS           (402),
        ERR_KEYWORD         (403),
        ERR_NUMBER          (404),
        ERR_NAME            (405),
        ERR_RESERVED_NAME   (406),
        ERR_DUP_NAME        (407),
        ERR_DUP_EXPR        (408),
        ERR_MATH_EXPR       (409),
        INP_ERR_LAST        (410);
        public final int id;
        InpErrorCodes(int val){this.id = val;}
    };

    // Respective error messages.
    private static String [] InpErrorTxt = {"",
            "Error 401 (too many characters)",
            "Error 402 (too few input items)",
            "Error 403 (invalid keyword)",
            "Error 404 (invalid numeric value)",
            "Error 405 (reference to undefined object)",
            "Error 406 (illegal use of a reserved name)",
            "Error 407 (name already used by another object)",
            "Error 408 (species already assigned an expression)",
            "Error 409 (illegal math expression)"};

    // Reads multi-species input file to determine number of system objects.
    int countMsxObjects(BufferedReader reader)
    {
        String      line;                   // line from input data file
        SectionType sect        = null;     // input data sections
        int         errcode     = 0;        // error code
        int         errsum      = 0;        // number of errors found
        long        lineCount   = 0;


        //MSX.Msg+=MSX.MsxFile.getFilename();
        //epanet.ENwriteline(MSX.Msg);
        //epanet.ENwriteline("");

        //BufferedReader reader = (BufferedReader)MSX.MsxFile.getFileIO();

        for(;;)
        {
            try{
                line = reader.readLine();
            }
            catch(IOException e){
                break;
            }

            if(line == null)
                break;

            errcode = 0;
            line = line.trim();
            lineCount++;

            int comentPosition = line.indexOf(';');
                 if(comentPosition!=-1)
                     line = line.substring(0,comentPosition);

            if (line.length() == 0)
                continue;

            String[] tok = line.split("[ \t]+");

            if ( tok.length  == 0 || tok[0].length() >0 && tok[0].charAt(0) == ';' ) continue;

            int [] sect_temp = new int[1];
            if ( getNewSection(tok[0], Constants.MsxSectWords, sect_temp) !=0 ){
                sect = SectionType.values()[sect_temp[0]];
                continue;
            }

            if ( sect == SectionType.s_SPECIES )
                errcode = addSpecies(tok);
            if ( sect == SectionType.s_COEFF )
                errcode = addCoeff(tok);
            if ( sect == SectionType.s_TERM )
                errcode = addTerm(tok);
            if ( sect == SectionType.s_PATTERN )
                errcode = addPattern(tok);


            if ( errcode!=0 )
            {
                writeInpErrMsg(errcode, Constants.MsxSectWords[sect.id], line,(int) lineCount);
                errsum++;
                if (errsum >= MAXERRS ) break;
            }
        }

        //return error code

        if ( errsum > 0 ) return ErrorCodeType.ERR_MSX_INPUT.id;
        return errcode;
    }

    // Queries EPANET database to determine number of network objects.
    int countNetObjects()
    {
        MSX.Nobjects[ObjectTypes.NODE.id] = epanet.ENgetcount(ENToolkit2.EN_NODECOUNT);
        MSX.Nobjects[ObjectTypes.TANK.id] =  epanet.ENgetcount(ENToolkit2.EN_TANKCOUNT);
        MSX.Nobjects[ObjectTypes.LINK.id] = epanet.ENgetcount(ENToolkit2.EN_LINKCOUNT);
        return 0;
    }

    // retrieves required input data from the EPANET project data.
    int readNetData()
    {
        int   i, k, n, t = 0;
        int   n1 = 0, n2 = 0;
        float diam = 0.0f, len = 0.0f, v0 = 0.0f, xmix = 0.0f, vmix = 0.0f;
        float roughness = 0.0f;

        // Get flow units & time parameters
        MSX.Flowflag = FlowUnitsType.values()[epanet.ENgetflowunits()];
        if ( MSX.Flowflag.ordinal() >= FlowUnitsType.LPS.ordinal())//EN_LPS )
            MSX.Unitsflag = UnitSystemType.SI;
        else
            MSX.Unitsflag = UnitSystemType.US;

        MSX.Dur = epanet.ENgettimeparam(ENToolkit2.EN_DURATION);
        MSX.Qstep = epanet.ENgettimeparam(ENToolkit2.EN_QUALSTEP);
        MSX.Rstep = epanet.ENgettimeparam(ENToolkit2.EN_REPORTSTEP);
        MSX.Rstart = epanet.ENgettimeparam(ENToolkit2.EN_REPORTSTART);
        MSX.Pstep = epanet.ENgettimeparam(ENToolkit2.EN_PATTERNSTEP);
        MSX.Pstart = epanet.ENgettimeparam(ENToolkit2.EN_PATTERNSTART);
        MSX.Statflag = TstatType.values()[(int)epanet.ENgettimeparam(ENToolkit2.EN_STATISTIC)];

        // Read tank/reservoir data
        n = MSX.Nobjects[ObjectTypes.NODE.id] - MSX.Nobjects[ObjectTypes.TANK.id];
        for (i=1; i<=MSX.Nobjects[ObjectTypes.NODE.id]; i++)
        {
            k = i - n;
            if ( k > 0 )
            {
                try{
                    t = epanet.ENgetnodetype(i);
                    v0 = epanet.ENgetnodevalue(i, ENToolkit2.EN_INITVOLUME);
                    xmix = epanet.ENgetnodevalue(i, ENToolkit2.EN_MIXMODEL);
                    vmix = epanet.ENgetnodevalue(i, ENToolkit2.EN_MIXZONEVOL);
                }
                catch(Exception e){return Integer.parseInt(e.getMessage());}

                MSX.Node[i].setTank(k);
                MSX.Tank[k].setNode(i);
                if ( t == ENToolkit2.EN_RESERVOIR )
                    MSX.Tank[k].setA( 0.0 );
                else
                    MSX.Tank[k].setA( 1.0 );
                MSX.Tank[k].setV0(v0);
                MSX.Tank[k].setMixModel((int)xmix);
                MSX.Tank[k].setvMix(vmix);
            }
        }

        // Read link data
        for (i=1; i<=MSX.Nobjects[ObjectTypes.LINK.id]; i++)
        {
            int [] n_temp;
            try {
                n_temp = epanet.ENgetlinknodes(i);
            } catch (Exception e) {return Integer.parseInt(e.getMessage());}
            n1 = n_temp[0];
            n2 = n_temp[1];
            try{
                diam = epanet.ENgetlinkvalue(i, ENToolkit2.EN_DIAMETER);
                len = epanet.ENgetlinkvalue(i, ENToolkit2.EN_LENGTH);
                roughness = epanet.ENgetlinkvalue(i, ENToolkit2.EN_ROUGHNESS);
            }
            catch(Exception e){return Integer.parseInt(e.getMessage());}

            MSX.Link[i].setN1(n1);
            MSX.Link[i].setN2(n2);
            MSX.Link[i].setDiam(diam);
            MSX.Link[i].setLen(len);
            MSX.Link[i].setRoughness(roughness);
        }
        return 0;
    }

    // Reads multi-species data from the EPANET-MSX input file.
    int readMsxData(BufferedReader rin)
    {
        String  line;             // line from input data file
        int     sect        = -1; // input data sections
        int     errsum      = 0;  // number of errors found
        int     inperr      = 0;  // input error code
        int     lineCount   = 0;  // line count

        // rewind
        //MSX.MsxFile.close();
        //MSX.MsxFile.openAsTextReader();

        //BufferedReader rin = (BufferedReader)MSX.MsxFile.getFileIO();

        for(;;){

            try {
                line = rin.readLine();
            } catch (IOException e) {
                break;
            }

            if(line==null)
                break;

            lineCount++;
            line = line.trim();

            int comentPosition = line.indexOf(';');
                 if(comentPosition!=-1)
                     line = line.substring(0,comentPosition);

            if (line.length() == 0)
                continue;

            String[] tok = line.split("[ \t]+");

            if ( tok.length  == 0) continue;

            if ( getLineLength(line) >= Constants.MAXLINE )
            {
                inperr = InpErrorCodes.ERR_LINE_LENGTH.id;
                writeInpErrMsg(inperr, Constants.MsxSectWords[sect], line, lineCount);
                errsum++;
            }

            int [] sect_tmp = new int[1];
            if ( getNewSection(tok[0], Constants.MsxSectWords,sect_tmp) !=0){
                sect = sect_tmp[0];
                continue;
            }

            inperr = parseLine(SectionType.values()[sect], line, tok);

            if ( inperr > 0 )
            {
                errsum++;
                writeInpErrMsg(inperr, Constants.MsxSectWords[sect], line, lineCount);
            }

            // Stop if reach end of file or max. error count
            if (errsum >= MAXERRS) break;
        }

        if (errsum > 0)
            return 200;

        return 0;
    }

    //  reads multi-species data from the EPANET-MSX input file.
    public String   MSXinp_getSpeciesUnits(int m)
    {
        String units = MSX.Species[m].getUnits();
        units+= "/";
        if ( MSX.Species[m].getType() == SpeciesType.BULK )
            units += "L";
        else
            units += Constants.AreaUnitsWords[MSX.AreaUnits.id];

        return units;
    }

    // determines number of characters of data in a line of input.
    int  getLineLength(String line)
    {
        int index = line.indexOf(';');

        if(index!=-1){
            return line.substring(0,index).length();
        }

        return line.length();
    }

    // checks if a line begins a new section in the input file.
    int  getNewSection(String tok, String [] sectWords, int [] sect)
    {
        int newsect;
        if(tok.length()==0)
            return 0;
        // --- check if line begins with a new section heading

        if ( tok.charAt(0) == '[' )
        {
            // --- look for section heading in list of section keywords

            newsect = Utilities.MSXutils_findmatch(tok, sectWords);
            if ( newsect >= 0 ) sect[0] = newsect;
            else
                sect[0] = -1;
            return 1;
        }
        return 0;
    }

    // adds a species ID name to the project.
    int addSpecies(String []Tok)
    {
        int errcode = 0;
        if (  Tok.length < 2 ) return InpErrorCodes.ERR_ITEMS.id;
        errcode = checkID(Tok[1]);
        if ( errcode!=0 ) return errcode;
        if ( project.MSXproj_addObject(ObjectTypes.SPECIES, Tok[1], MSX.Nobjects[ObjectTypes.SPECIES.id]+1) < 0 )
            errcode = 101;
        else MSX.Nobjects[ObjectTypes.SPECIES.id]++;
        return errcode;
    }

    // adds a coefficient ID name to the project.
    int addCoeff(String [] Tok)
    {
        ObjectTypes k;
        int errcode = 0;

        // determine the type of coeff.

        if ( Tok.length < 2 ) return InpErrorCodes.ERR_ITEMS.id;
        if      (Utilities.MSXutils_match(Tok[0], "PARAM")) k = ObjectTypes.PARAMETER;
        else if (Utilities.MSXutils_match(Tok[0], "CONST")) k = ObjectTypes.CONSTANT;
        else return InpErrorCodes.ERR_KEYWORD.id;

        // check for valid id name

        errcode = checkID(Tok[1]);
        if ( errcode !=0) return errcode;
        if ( project.MSXproj_addObject(k, Tok[1], MSX.Nobjects[k.id] + 1) < 0 )
            errcode = 101;
        else MSX.Nobjects[k.id]++;
        return errcode;
    }


    // adds an intermediate expression term ID name to the project.
    int addTerm(String [] id)
    {
        int errcode = checkID(id[0]);
        if ( errcode == 0 )
        {
            if ( project.MSXproj_addObject(ObjectTypes.TERM, id[0], MSX.Nobjects[ObjectTypes.TERM.id]+1) < 0 )
                errcode = 101;
            else MSX.Nobjects[ObjectTypes.TERM.id]++;
        }
        return errcode;
    }


    // adds a time pattern ID name to the project.
    int addPattern(String [] tok)
    {
        int errcode = 0;

        // A time pattern can span several lines

        if ( project.MSXproj_findObject( ObjectTypes.PATTERN, tok[0]) <= 0 )
        {
            if ( project.MSXproj_addObject(ObjectTypes.PATTERN, tok[0], MSX.Nobjects[ObjectTypes.PATTERN.id]+1) < 0 )
                errcode = 101;
            else MSX.Nobjects[ObjectTypes.PATTERN.id]++;
        }
        return errcode;
    }


    // checks that an object's name is unique
    int checkID(String id)
    {
        // Check that id name is not a reserved word
        int i = 1;
        //while (HydVarWords[i] != NULL)
        for(String word : Constants.HydVarWords)
        {
            if (Utilities.MSXutils_strcomp(id, word)) return InpErrorCodes.ERR_RESERVED_NAME.id;
            i++;
        }

        // Check that id name not used before

        if ( project.MSXproj_findObject(ObjectTypes.SPECIES, id) > 0 ||
                project.MSXproj_findObject(ObjectTypes.TERM, id)   > 0 ||
                project.MSXproj_findObject(ObjectTypes.PARAMETER, id)  > 0 ||
                project.MSXproj_findObject(ObjectTypes.CONSTANT, id)  > 0
                ) return InpErrorCodes.ERR_DUP_NAME.id;
        return 0;
    }


    // parses the contents of a line of input data.
    int parseLine(SectionType sect, String line, String [] Tok)
    {
        switch(sect)
        {
            case s_TITLE:
                MSX.Title =  line;
                break;

            case s_OPTION:
                return parseOption(Tok);

            case s_SPECIES:
                return parseSpecies(Tok);

            case s_COEFF:
                return parseCoeff(Tok);

            case s_TERM:
                return parseTerm(Tok);

            case s_PIPE:
                return parseExpression(ObjectTypes.LINK, Tok);

            case s_TANK:
                return parseExpression(ObjectTypes.TANK, Tok);

            case s_SOURCE:
                return parseSource(Tok);

            case s_QUALITY:
                return parseQuality(Tok);

            case s_PARAMETER:
                return parseParameter(Tok);

            case s_PATTERN:
                return parsePattern(Tok);

            case s_REPORT:
                return parseReport(Tok);
        }
        return 0;
    }

    // parses an input line containing a project option.
    int parseOption(String [] Tok)
    {
        int k;

        // Determine which option is being read

        if ( Tok.length < 2 ) return 0;
        k = Utilities.MSXutils_findmatch(Tok[0], Constants.OptionTypeWords);
        if ( k < 0 ) return InpErrorCodes.ERR_KEYWORD.id;

        // Parse the value for the given option
        switch ( OptionType.values()[k] )
        {
            case AREA_UNITS_OPTION:
                k = Utilities.MSXutils_findmatch(Tok[1], Constants.AreaUnitsWords);
                if ( k < 0 ) return InpErrorCodes.ERR_KEYWORD.id;
                MSX.AreaUnits = AreaUnitsType.values()[k];
                break;

            case RATE_UNITS_OPTION:
                k = Utilities.MSXutils_findmatch(Tok[1], Constants.TimeUnitsWords);
                if ( k < 0 ) return InpErrorCodes.ERR_KEYWORD.id;
                MSX.RateUnits = RateUnitsType.values()[k];
                break;

            case SOLVER_OPTION:
                k = Utilities.MSXutils_findmatch(Tok[1], Constants.SolverTypeWords);
                if ( k < 0 ) return InpErrorCodes.ERR_KEYWORD.id;
                MSX.Solver = SolverType.values()[k];
                break;

            case COUPLING_OPTION:
                k = Utilities.MSXutils_findmatch(Tok[1], Constants.CouplingWords);
                if ( k < 0 ) return InpErrorCodes.ERR_KEYWORD.id;
                MSX.Coupling = CouplingType.values()[k];
                break;

            case TIMESTEP_OPTION:
                k = Integer.parseInt(Tok[1]);
                if ( k <= 0 ) return InpErrorCodes.ERR_NUMBER.id;
                MSX.Qstep = k;
                break;

            case RTOL_OPTION:
            {
                double [] tmp = new double[1];
                if ( !Utilities.MSXutils_getDouble(Tok[1], tmp) ) return InpErrorCodes.ERR_NUMBER.id;
                MSX.DefRtol = tmp[0];
                break;
            }
            case ATOL_OPTION:
            {
                double [] tmp = new double[1];
                if ( !Utilities.MSXutils_getDouble(Tok[1], tmp) ) return InpErrorCodes.ERR_NUMBER.id;
                MSX.DefAtol = tmp[0];
            }
            break;
        }
        return 0;
    }

    // Parses an input line containing a species variable.
    int parseSpecies(String [] Tok)
    {
        int i;

        // Get secies index
        if ( Tok.length < 3 ) return InpErrorCodes.ERR_ITEMS.id;
        i = project.MSXproj_findObject(ObjectTypes.SPECIES, Tok[1]);
        if ( i <= 0 ) return InpErrorCodes.ERR_NAME.id;

        // Get pointer to Species name
        MSX.Species[i].setId(project.MSXproj_findID(ObjectTypes.SPECIES, Tok[1]));

        // Get species type
        if      ( Utilities.MSXutils_match(Tok[0], "BULK") ) MSX.Species[i].setType(SpeciesType.BULK);
        else if ( Utilities.MSXutils_match(Tok[0], "WALL") ) MSX.Species[i].setType(SpeciesType.WALL);
        else return InpErrorCodes.ERR_KEYWORD.id;

        // Get Species units
        MSX.Species[i].setUnits(Tok[2]);

        // Get Species error tolerance
        MSX.Species[i].setaTol(0.0);
        MSX.Species[i].setrTol(0.0);
        if ( Tok.length >= 4)
        {
            double [] tmp= new double[1];
            if ( !Utilities.MSXutils_getDouble(Tok[3], tmp))//&MSX.Species[i].aTol) )
                MSX.Species[i].setaTol (tmp[0]);
            return InpErrorCodes.ERR_NUMBER.id;
        }
        if ( Tok.length >= 5)
        {
            double [] tmp= new double[1];
            if ( !Utilities.MSXutils_getDouble(Tok[4], tmp))//&MSX.Species[i].rTol) )
                MSX.Species[i].setrTol(tmp[0]);
            return InpErrorCodes.ERR_NUMBER.id;
        }
        return 0;
    }

    // parses an input line containing a coefficient definition.
    int parseCoeff(String [] Tok)
    {
        int i, j;
        double [] x = new double[1];

        // Check if variable is a Parameter
        if ( Tok.length < 2 ) return 0;
        if ( Utilities.MSXutils_match(Tok[0], "PARAM") )
        {
            // Get Parameter's index
            i = project.MSXproj_findObject(ObjectTypes.PARAMETER, Tok[1]);
            if ( i <= 0 ) return InpErrorCodes.ERR_NAME.id;

            // Get Parameter's value
            MSX.Param[i].setId(project.MSXproj_findID(ObjectTypes.PARAMETER, Tok[1]));
            if ( Tok.length >= 3 )
            {
                if (Utilities.MSXutils_getDouble(Tok[2], x)) return InpErrorCodes.ERR_NUMBER.id;
                MSX.Param[i].setValue(x[0]);
                for (j=1; j<=MSX.Nobjects[ObjectTypes.LINK.id]; j++) MSX.Link[j].getParam()[i] = x[0];
                for (j=1; j<=MSX.Nobjects[ObjectTypes.TANK.id]; j++) MSX.Tank[j].getParam()[i] = x[0];
            }
            return 0;
        }

        // Check if variable is a Constant
        else if ( Utilities.MSXutils_match(Tok[0], "CONST") )
        {
            // Get Constant's index
            i = project.MSXproj_findObject(ObjectTypes.CONSTANT, Tok[1]);
            if ( i <= 0 ) return InpErrorCodes.ERR_NAME.id;

            // Get constant's value
            MSX.Const[i].setId(project.MSXproj_findID(ObjectTypes.CONSTANT, Tok[1]));
            MSX.Const[i].setValue(0.0);
            if ( Tok.length >= 3 )
            {
                double [] tmp = new double[1];
                if ( !Utilities.MSXutils_getDouble(Tok[2], tmp))//&MSX.Const[i].value) )
                    return InpErrorCodes.ERR_NUMBER.id;
                MSX.Const[i].setValue(tmp[0]);
            }
            return 0;
        }
        else
            return InpErrorCodes.ERR_KEYWORD.id;
    }

    //=============================================================================
    // parses an input line containing an intermediate expression term .
    int parseTerm(String [] Tok)
    {
        int i, j;
        String s = "";
        MathExpr expr;

        // --- get term's name

        if ( Tok.length < 2 ) return 0;
        i = project.MSXproj_findObject(ObjectTypes.TERM, Tok[0]);

        // --- reconstruct the expression string from its tokens

        for (j=1; j<Tok.length; j++) s+= Tok[j];

        // --- convert expression into a postfix stack of op codes

        //expr = mathexpr_create(s, getVariableCode);
        expr = MathExpr.create(s, new VariableInterface(){
            public double getValue(int id) {return 0;}
            public int getIndex(String id) {return getVariableCode(id);}
        });
        if ( expr == null ) return InpErrorCodes.ERR_MATH_EXPR.id;

        // --- assign the expression to a Term object

        MSX.Term[i].setExpr(expr);
        return 0;
    }

    //=============================================================================
    // parses an input line containing a math expression.
    int parseExpression(ObjectTypes classType,String []Tok)
    {
        int i, j, k;
        String s = "";
        MathExpr expr;

        // --- determine expression type

        if ( Tok.length < 3 ) return InpErrorCodes.ERR_ITEMS.id;
        k = Utilities.MSXutils_findmatch(Tok[0], Constants.ExprTypeWords);
        if ( k < 0 ) return InpErrorCodes.ERR_KEYWORD.id;

        // --- determine species associated with expression

        i = project.MSXproj_findObject(ObjectTypes.SPECIES, Tok[1]);
        if ( i < 1 ) return InpErrorCodes.ERR_NAME.id;

        // --- check that species does not already have an expression

        if ( classType == ObjectTypes.LINK )
        {
            if ( MSX.Species[i].getPipeExprType() != ExpressionType.NO_EXPR ) return InpErrorCodes.ERR_DUP_EXPR.id;
        }
        if ( classType == ObjectTypes.TANK )
        {
            if ( MSX.Species[i].getTankExprType() != ExpressionType.NO_EXPR ) return InpErrorCodes.ERR_DUP_EXPR.id;
        }

        // --- reconstruct the expression string from its tokens

        for (j=2; j<Tok.length; j++) s+= Tok[j];

        // --- convert expression into a postfix stack of op codes

        //expr = mathexpr_create(s, getVariableCode);
        expr = MathExpr.create(s, new VariableInterface(){
            public double getValue(int id) {return 0;}
            public int getIndex(String id) {return getVariableCode(id);}
        });//createMathExpr()

        if ( expr == null ) return InpErrorCodes.ERR_MATH_EXPR.id;

        // --- assign the expression to the species

        switch (classType)
        {
            case LINK:
                MSX.Species[i].setPipeExpr(expr);
                MSX.Species[i].setPipeExprType(ExpressionType.values()[k]);
                break;
            case TANK:
                MSX.Species[i].setTankExpr(expr);
                MSX.Species[i].setTankExprType(ExpressionType.values()[k]);
                break;
        }
        return 0;
    }

    //=============================================================================
    // parses an input line containing initial species concentrations.
    int parseQuality(String [] Tok)
    {
        int err, i, j, k, m;
        double [] x = new double[1];

        // --- determine if quality value is global or object-specific

        if ( Tok.length < 3 ) return InpErrorCodes.ERR_ITEMS.id;
        if      ( Utilities.MSXutils_match(Tok[0], "GLOBAL") ) i = 1;
        else if ( Utilities.MSXutils_match(Tok[0], "NODE") )   i = 2;
        else if ( Utilities.MSXutils_match(Tok[0], "LINK") )   i = 3;
        else return InpErrorCodes.ERR_KEYWORD.id;

        // --- find species index

        k = 1;
        if ( i >= 2 ) k = 2;
        m = project.MSXproj_findObject(ObjectTypes.SPECIES, Tok[k]);
        if ( m <= 0 ) return InpErrorCodes.ERR_NAME.id;

        // --- get quality value

        if ( i >= 2  && Tok.length < 4 ) return InpErrorCodes.ERR_ITEMS.id;
        k = 2;
        if ( i >= 2 ) k = 3;
        if ( !Utilities.MSXutils_getDouble(Tok[k], x) ) return InpErrorCodes.ERR_NUMBER.id;

        // --- for global specification, set initial quality either for
        //     all nodes or links depending on type of species

        if ( i == 1)
        {
            MSX.C0[m] = x[0];
            if ( MSX.Species[m].getType() == SpeciesType.BULK )
            {
                for (j=1; j<=MSX.Nobjects[ObjectTypes.NODE.id]; j++) MSX.Node[j].getC0()[m] = x[0];
            }
            for (j=1; j<=MSX.Nobjects[ObjectTypes.LINK.id]; j++) MSX.Link[j].getC0()[m] = x[0];
        }

        // --- for a specific node, get its index & set its initial quality

        else if ( i == 2 )
        {
            int [] tmp = new int[1];
            err = epanet.ENgetnodeindex(Tok[1], tmp);
            j = tmp[0];
            if ( err!=0 ) return InpErrorCodes.ERR_NAME.id;
            if ( MSX.Species[m].getType() == SpeciesType.BULK ) MSX.Node[j].getC0()[m] = x[0];
        }

        // --- for a specific link, get its index & set its initial quality

        else if ( i == 3 )
        {
            int [] tmp = new int[1];
            err = epanet.ENgetlinkindex(Tok[1], tmp);
            j = tmp[0];
            if ( err!=0 )
                return InpErrorCodes.ERR_NAME.id;

            MSX.Link[j].getC0()[m] = x[0];
        }
        return 0;
    }

    //=============================================================================
    // parses an input line containing a parameter data.
    int parseParameter(String [] Tok)
    {
        int err, i, j;
        double x;

        // --- get parameter name

        if ( Tok.length < 4 ) return 0;
        i = project.MSXproj_findObject(ObjectTypes.PARAMETER, Tok[2]);

        // --- get parameter value

        double [] x_tmp = new double[1];
        if ( !Utilities.MSXutils_getDouble(Tok[3], x_tmp) ) return InpErrorCodes.ERR_NUMBER.id;
        x = x_tmp[0];
        // --- for pipe parameter, get pipe index and update parameter's value

        if ( Utilities.MSXutils_match(Tok[0], "PIPE") )
        {
            int [] j_tmp = new int [1];
            err = epanet.ENgetlinkindex(Tok[1], j_tmp);
            j = j_tmp[0];
            if ( err != 0) return InpErrorCodes.ERR_NAME.id;
            MSX.Link[j].getParam()[i] = x;
        }

        // --- for tank parameter, get tank index and update parameter's value

        else if ( Utilities.MSXutils_match(Tok[0], "TANK") )
        {
            int [] j_temp = new int [1];
            err = epanet.ENgetnodeindex(Tok[1], j_temp);
            j = j_temp[0];
            if ( err!=0 ) return InpErrorCodes.ERR_NAME.id;
            j = MSX.Node[j].getTank();
            if ( j > 0 ) MSX.Tank[j].getParam()[i] = x;
        }
        else return InpErrorCodes.ERR_KEYWORD.id;
        return 0;
    }

    //=============================================================================
    // parses an input line containing a source input data.
    int parseSource(String [] Tok)
    {
        int err, i, j, k, m;
        double  x;
        Source source = null;

        // --- get source type

        if ( Tok.length < 4 ) return InpErrorCodes.ERR_ITEMS.id;
        k = Utilities.MSXutils_findmatch(Tok[0], Constants.SourceTypeWords);
        if ( k < 0 ) return InpErrorCodes.ERR_KEYWORD.id;

        // --- get node index

        int [] j_tmp = new int[1];
        err = epanet.ENgetnodeindex(Tok[1], j_tmp);
        j = j_tmp[0];
        if ( err != 0) return InpErrorCodes.ERR_NAME.id;

        //  --- get species index

        m = project.MSXproj_findObject(ObjectTypes.SPECIES, Tok[2]);
        if ( m <= 0 ) return InpErrorCodes.ERR_NAME.id;

        // --- check that species is a BULK species

        if ( MSX.Species[m].getType() != SpeciesType.BULK ) return 0;

        // --- get base strength

        double [] x_tmp = new double[1];
        if ( !Utilities.MSXutils_getDouble(Tok[3], x_tmp) ) return InpErrorCodes.ERR_NUMBER.id;
        x = x_tmp[0];
        // --- get time pattern if present

        i = 0;
        if ( Tok.length >= 5 )
        {
            i = project.MSXproj_findObject(ObjectTypes.PATTERN, Tok[4]);
            if ( i <= 0 ) return InpErrorCodes.ERR_NAME.id;
        }

        // --- check if a source for this species already exists

        /*source = MSX.Node[j].sources;
        while ( source )
        {
            if ( source->species == m ) break;
            source = source->next;
        }*/

        for(Source src : MSX.Node[j].getSources())
        {
            if ( src.getSpecies() == m ){
                source = src;
                break;
            }

        }
        // --- otherwise create a new source object

        if ( source == null )
        {
            source = new Source();//(struct Ssource *) malloc(sizeof(struct Ssource));
            //if ( source == NULL ) return 101;
            //source->next = MSX.Node[j].sources;
            //MSX.Node[j].sources = source;
            MSX.Node[j].getSources().add(0,source);
        }

        // --- save source's properties

        source.setType(SourceType.values()[k]);
        source.setSpecies(m);
        source.setC0(x);
        source.setPattern(i);
        return 0;
    }

    //=============================================================================
    // parses an input line containing a time pattern data.
    int parsePattern(String [] Tok)
    {
        int i;
        double [] x = new double[1];
        //List<Double> listItem = new ArrayList<Double>();
        //SnumList *listItem;

        // --- get time pattern index

        if ( Tok.length < 2 ) return InpErrorCodes.ERR_ITEMS.id;
        i = project.MSXproj_findObject(ObjectTypes.PATTERN, Tok[0]);
        if ( i <= 0 ) return InpErrorCodes.ERR_NAME.id;
        MSX.Pattern[i].setId(project.MSXproj_findID(ObjectTypes.PATTERN, Tok[0]));

        // --- begin reading pattern multipliers

        //k = 1;
        //while ( k < Tok.length )
        for(int k = 1;k< Tok.length;k++)//String token : Tok)
        {

            if ( !Utilities.MSXutils_getDouble(Tok[k], x) ) return InpErrorCodes.ERR_NUMBER.id;

            MSX.Pattern[i].getMultipliers().add(x[0]);
            /*listItem = (SnumList *) malloc(sizeof(SnumList));
            if ( listItem == NULL ) return 101;
            listItem->value = x;
            listItem->next = NULL;
            if ( MSX.Pattern[i].first == NULL )
            {
                MSX.Pattern[i].current = listItem;
                MSX.Pattern[i].first = listItem;
            }
            else
            {
                MSX.Pattern[i].current->next = listItem;
                MSX.Pattern[i].current = listItem;
            } */

            // k++;
        }
        return 0;
    }

    int parseReport(String [] Tok)
    {
        int  i, j, k, err;

        // Get keyword
        if ( Tok.length < 2 )
            return 0;

        k = Utilities.MSXutils_findmatch(Tok[0], Constants.ReportWords);

        if ( k < 0 )
            return InpErrorCodes.ERR_KEYWORD.id;

        switch(k)
        {
            // Keyword is NODE; parse ID names of reported nodes
            case 0:
                if ( Utilities.MSXutils_strcomp(Tok[1], Constants.ALL) )
                {
                    for (j=1; j<=MSX.Nobjects[ObjectTypes.NODE.id]; j++) MSX.Node[j].setRpt(true);
                }
                else if ( Utilities.MSXutils_strcomp(Tok[1], Constants.NONE) )
                {
                    for (j=1; j<=MSX.Nobjects[ObjectTypes.NODE.id]; j++) MSX.Node[j].setRpt(false);
                }
                else for (i=1; i<Tok.length; i++)
                    {
                        int [] j_tmp = new int [1];
                        err = epanet.ENgetnodeindex(Tok[i], j_tmp);
                        j = j_tmp[0];
                        if ( err != 0)
                            return InpErrorCodes.ERR_NAME.id;
                        MSX.Node[j].setRpt(true);
                    }
                break;

            // Keyword is LINK: parse ID names of reported links
            case 1:
                if ( Utilities.MSXutils_strcomp(Tok[1], Constants.ALL) )
                {
                    for (j=1; j<=MSX.Nobjects[ObjectTypes.LINK.id]; j++) MSX.Link[j].setRpt(true);
                }
                else if (  Utilities.MSXutils_strcomp(Tok[1], Constants.NONE) )
                {
                    for (j=1; j<=MSX.Nobjects[ObjectTypes.LINK.id]; j++) MSX.Link[j].setRpt(false);
                }
                else for (i=1; i<Tok.length; i++)
                    {
                        int j_temp [] = new int [1];
                        err = epanet.ENgetlinkindex(Tok[i], j_temp);
                        j = j_temp[0];
                        if ( err != 0) return InpErrorCodes.ERR_NAME.id;
                        MSX.Link[j].setRpt(true);
                    }
                break;

            // Keyword is SPECIES; get YES/NO & precision
            case 2:
                j = project.MSXproj_findObject(ObjectTypes.SPECIES, Tok[1]);
                if ( j <= 0 ) return InpErrorCodes.ERR_NAME.id;
                if ( Tok.length >= 3 )
                {
                    if ( Utilities.MSXutils_strcomp(Tok[2], Constants.YES) ) MSX.Species[j].setRpt((char)1);
                    else if ( Utilities.MSXutils_strcomp(Tok[2], Constants.NO)  ) MSX.Species[j].setRpt((char)0);
                    else return InpErrorCodes.ERR_KEYWORD.id;
                }
                if ( Tok.length >= 4 )
                {
                    int [] precision_tmp = new int[1];
                    if ( !Utilities.MSXutils_getInt(Tok[3], precision_tmp) );
                    MSX.Species[j].setPrecision(precision_tmp[0]);
                    return InpErrorCodes.ERR_NUMBER.id;
                }
                break;

            // Keyword is FILE: get name of report file
            case 3:
                MSX.rptFilename = Tok[1] ;
                break;

            // Keyword is PAGESIZE;
            case 4:
                int [] pagesize_tmp = new int[1];
                if ( !Utilities.MSXutils_getInt(Tok[1], pagesize_tmp) )
                    return InpErrorCodes.ERR_NUMBER.id;
                MSX.PageSize = pagesize_tmp[0];
                break;
        }
        return 0;
    }

    //=============================================================================
    // Finds the index assigned to a species, intermediate term, parameter, or constant that appears in a math expression.
    int getVariableCode(String id)
    {
        int j = project.MSXproj_findObject(ObjectTypes.SPECIES, id);
        if ( j >= 1 ) return j;
        j = project.MSXproj_findObject(ObjectTypes.TERM, id);
        if ( j >= 1 ) return MSX.Nobjects[ObjectTypes.SPECIES.id] + j;
        j = project.MSXproj_findObject(ObjectTypes.PARAMETER, id);
        if ( j >= 1 ) return MSX.Nobjects[ObjectTypes.SPECIES.id] + MSX.Nobjects[ObjectTypes.TERM.id] + j;
        j = project.MSXproj_findObject(ObjectTypes.CONSTANT, id);
        if ( j >= 1 ) return MSX.Nobjects[ObjectTypes.SPECIES.id] + MSX.Nobjects[ObjectTypes.TERM.id] +
                MSX.Nobjects[ObjectTypes.PARAMETER.id] + j;
        j = Utilities.MSXutils_findmatch(id, Constants.HydVarWords);
        if ( j >= 1 ) return MSX.Nobjects[ObjectTypes.SPECIES.id] + MSX.Nobjects[ObjectTypes.TERM.id] +
                MSX.Nobjects[ObjectTypes.PARAMETER.id] + MSX.Nobjects[ObjectTypes.CONSTANT.id] + j;
        return -1;
    }

    //=============================================================================
    // Scans a string for tokens, saving pointers to them
    //in shared variable Tok[].
    //int  getTokens(String s)
    //{
    //    int  len, m, n;
    //    String c;
    //
    //    // --- begin with no tokens
    //
    //    for (n = 0; n < MAXTOKS; n++) Tok[n] = NULL;
    //    n = 0;
    //
    //    // --- truncate s at start of comment
    //
    //    c = strchr(s,';');
    //    if (c) *c = '\0';
    //    len = strlen(s);
    //
    //    // --- scan s for tokens until nothing left
    //
    //    while (len > 0 && n < MAXTOKS)
    //    {
    //        m = strcspn(s,SEPSTR);              // find token length
    //        if (m == 0) s++;                    // no token found
    //        else
    //        {
    //            if (*s == '"')                  // token begins with quote
    //            {
    //                s++;                        // start token after quote
    //                len--;                      // reduce length of s
    //                m = strcspn(s,"\"\n");      // find end quote or new line
    //            }
    //            s[m] = '\0';                    // null-terminate the token
    //            Tok[n] = s;                     // save pointer to token
    //            n++;                            // update token count
    //            s += m+1;                       // begin next token
    //        }
    //        len -= m+1;                         // update length of s
    //    }
    //    return(n);
    //}

    //=============================================================================

    void writeInpErrMsg(int errcode, String sect, String line, int lineCount)
    {

        String msg;
        if ( errcode >= InpErrorCodes.INP_ERR_LAST.id  || errcode <= InpErrorCodes.INP_ERR_FIRST.id )
        {
            System.out.println(String.format("Error Code = %d", errcode));
        }
        else
        {
            System.out.println(String.format("%s at line %d of %s] section:",
                    InpErrorTxt[errcode-InpErrorCodes.INP_ERR_FIRST.id], lineCount, sect));
        }
        //epanet.ENwriteline("");
        //epanet.ENwriteline(msg);
        //epanet.ENwriteline(line);
    }

}
