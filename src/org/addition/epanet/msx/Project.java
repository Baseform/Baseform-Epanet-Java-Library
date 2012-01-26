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
import org.addition.epanet.msx.EnumTypes.*;
import org.addition.epanet.msx.Structures.*;

import java.io.*;
import java.util.Hashtable;
import java.util.Map;

public class Project {


    public void loadDependencies(EpanetMSX epa)
    {
        MSX = epa.getNetwork();
        reader = epa.getReader();
    }

    Network MSX;

    Map<String, Integer> [] Htable;

    InpReader reader;

    // Opens an EPANET-MSX project.
    int  MSXproj_open(File msxFile) throws IOException {
        int errcode = 0;

        //MSX.QualityOpened = false;

        BufferedReader buffReader = new BufferedReader(new FileReader(msxFile));
        // initialize data to default values
        setDefaults();

        // Open the MSX input file
        //MSX.MsxFile.setFilename(fname);
        //if(!MSX.MsxFile.openAsTextReader())
        //    return ErrorCodeType.ERR_OPEN_MSX_FILE.id;

        // create hash tables to look up object ID names
        errcode = Utilities.CALL(errcode, createHashTables());

        // allocate memory for the required number of objects
        errcode = Utilities.CALL(errcode, reader.countMsxObjects(buffReader));
        errcode = Utilities.CALL(errcode, reader.countNetObjects());
        errcode = Utilities.CALL(errcode, createObjects());

        buffReader.close();
        buffReader = new BufferedReader(new FileReader(msxFile));

        // Read in the EPANET and MSX object data

        errcode = Utilities.CALL(errcode, reader.readNetData());
        errcode = Utilities.CALL(errcode, reader.readMsxData(buffReader));

        //if (MSX.RptFile.getFilename().equals(""))
        //    errcode = Utilities.CALL(errcode, openRptFile());

        // Convert user's units to internal units
        errcode = Utilities.CALL(errcode, convertUnits());

        // Close input file
        //MSX.MsxFile.close();

        return errcode;
    }

    //=============================================================================
    // closes the current EPANET-MSX project.
    void MSXproj_close()
    {
        //if ( MSX.RptFile.file ) fclose(MSX.RptFile.file);                          //(LR-11/20/07, to fix bug 08)
        //MSX.RptFile.close();
        //if ( MSX.HydFile.file ) fclose(MSX.HydFile.file);
        //if ( MSX.HydFile.getMode() == FileModeType.SCRATCH_FILE ) //remove(MSX.HydFile.name);
        //    MSX.HydFile.remove();
        //if ( MSX.TmpOutFile.getFileIO() != null && MSX.TmpOutFile.getFileIO() != MSX.OutFile.getFileIO() )
        //{
        //    //fclose(MSX.TmpOutFile.file);
        //    MSX.TmpOutFile.close();
        //    MSX.TmpOutFile.remove();
        //    //remove(MSX.TmpOutFile.name);
        //}
        //if ( MSX.OutFile.file ) fclose(MSX.OutFile.file);
        //MSX.OutFile.close();
        //
        //if ( MSX.OutFile.getMode() == FileModeType.SCRATCH_FILE )// remove(MSX.OutFile.name);
        //    MSX.OutFile.close();
        //MSX.RptFile.file = null;                                                   //(LR-11/20/07, to fix bug 08)
        //MSX.HydFile.file = null;
        //MSX.OutFile.file = null;
        //MSX.TmpOutFile.file = null;
        deleteObjects();
        deleteHashTables();
        //MSX.ProjectOpened = false;
    }

    //=============================================================================
    // adds an object ID to the project's hash tables.
    int   MSXproj_addObject(ObjectTypes type, String id, int n)
    {
        int  result = 0;
        int  len;
        String newID = id;

// --- do nothing if object already exists in a hash table

        if ( MSXproj_findObject(type, id) > 0 ) return 0;



// --- insert object's ID into the hash table for that type of object

        //result = HTinsert(Htable[type], newID, n);
        if(Htable[type.id].put(newID,n)==null)
            result = 1;

        if ( result == 0 )
            result = -1;
        return result;
    }

    //=============================================================================
    // uses hash table to find index of an object with a given ID.
    int   MSXproj_findObject(ObjectTypes type, String id)
    {
        Integer val = Htable[type.id].get(id);
        return val==null?-1:val;
    }

    //=============================================================================
    // uses hash table to find address of given string entry.
    String MSXproj_findID(ObjectTypes type, String id)
    {
        Integer val = Htable[type.id].get(id);
        return val==null?"":id;
    }

    //=============================================================================
    // gets the text of an error message.
    String MSXproj_getErrmsg(int errcode)
    {
        if ( errcode <= ErrorCodeType.ERR_FIRST.id || errcode >= ErrorCodeType.ERR_MAX.id ) return Constants.Errmsg[0];
        else return Constants.Errmsg[errcode - ErrorCodeType.ERR_FIRST.id];
    }

    // assigns default values to project variables.
    void setDefaults()
    {
        MSX.Title = "";
        MSX.Rptflag = false;
        for (int i=0; i<ObjectTypes.MAX_OBJECTS.id; i++)
            MSX.Nobjects[i] = 0;
        MSX.Unitsflag = UnitSystemType.US;
        MSX.Flowflag = FlowUnitsType.GPM;
        MSX.Statflag = TstatType.SERIES;
        MSX.DefRtol = 0.001;
        MSX.DefAtol = 0.01;
        MSX.Solver = SolverType.EUL;
        MSX.Coupling = CouplingType.NO_COUPLING;
        MSX.AreaUnits = AreaUnitsType.FT2;
        MSX.RateUnits = RateUnitsType.DAYS;
        MSX.Qstep = 300;
        MSX.Rstep = 3600;
        MSX.Rstart = 0;
        MSX.Dur = 0;
        MSX.Node = null;
        MSX.Link = null;
        MSX.Tank = null;
        MSX.D = null;
        MSX.Q = null;
        MSX.H = null;
        MSX.Species = null;
        MSX.Term = null;
        MSX.Const = null;
        MSX.Pattern = null;
    }

    // Converts user's units to internal EPANET units.
    int convertUnits()
    {
        // Flow conversion factors (to cfs)
        double fcf[] = {1.0, Constants.GPMperCFS, Constants.MGDperCFS, Constants.IMGDperCFS, Constants.AFDperCFS,
                Constants.LPSperCFS, Constants.LPMperCFS, Constants.MLDperCFS, Constants.CMHperCFS, Constants.CMDperCFS};

        // Rate time units conversion factors (to sec)
        double rcf[] = {1.0, 60.0, 3600.0, 86400.0};

        int i, m, errcode = 0;

        // Conversions for length & tank volume
        if ( MSX.Unitsflag == UnitSystemType.US )
        {
            MSX.Ucf[UnitsType.LENGTH_UNITS.id] = 1.0;
            MSX.Ucf[UnitsType.DIAM_UNITS.id]   = 12.0;
            MSX.Ucf[UnitsType.VOL_UNITS.id]    = 1.0;
        }
        else
        {
            MSX.Ucf[UnitsType.LENGTH_UNITS.id] = Constants.MperFT;
            MSX.Ucf[UnitsType.DIAM_UNITS.id]   = 1000.0*Constants.MperFT;
            MSX.Ucf[UnitsType.VOL_UNITS.id]    = Constants.M3perFT3;
        }

        // Conversion for surface area
        MSX.Ucf[UnitsType.AREA_UNITS.id] = 1.0;
        switch (MSX.AreaUnits)
        {
            case M2:  MSX.Ucf[UnitsType.AREA_UNITS.id] = Constants.M2perFT2;  break;
            case CM2: MSX.Ucf[UnitsType.AREA_UNITS.id] = Constants.CM2perFT2; break;
        }

        // Conversion for flow rate
        MSX.Ucf[UnitsType.FLOW_UNITS.id] = fcf[MSX.Flowflag.id];
        MSX.Ucf[UnitsType.CONC_UNITS.id] = Constants.LperFT3;

        // Conversion for reaction rate time
        MSX.Ucf[UnitsType.RATE_UNITS.id] = rcf[MSX.RateUnits.id];

        // Convert pipe diameter & length
        for (i=1; i<=MSX.Nobjects[ObjectTypes.LINK.id]; i++){
            MSX.Link[i].setDiam( MSX.Link[i].getDiam() / MSX.Ucf[UnitsType.DIAM_UNITS.id]);
            MSX.Link[i].setLen(MSX.Link[i].getLen() /  MSX.Ucf[UnitsType.LENGTH_UNITS.id]);
        }

        // Convert initial tank volumes
        for (i=1; i<=MSX.Nobjects[ObjectTypes.TANK.id]; i++){
            MSX.Tank[i].setV0(MSX.Tank[i].getV0() / MSX.Ucf[UnitsType.VOL_UNITS.id]);
            MSX.Tank[i].setvMix(MSX.Tank[i].getvMix() / MSX.Ucf[UnitsType.VOL_UNITS.id]);
        }

        // Assign default tolerances to species
        for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++){
            if ( MSX.Species[m].getrTol() == 0.0 ) MSX.Species[m].setrTol(MSX.DefRtol);
            if ( MSX.Species[m].getaTol()  == 0.0 ) MSX.Species[m].setaTol(MSX.DefAtol);
        }

        return errcode;
    }

    // creates multi-species data objects.
    int createObjects()
    {
        // Create nodes, links, & tanks
        MSX.Node = new Node[MSX.Nobjects[ObjectTypes.NODE.id]+1];
        MSX.Link = new Link[MSX.Nobjects[ObjectTypes.LINK.id]+1];
        MSX.Tank = new Tank[MSX.Nobjects[ObjectTypes.TANK.id]+1];

        // Create species, terms, parameters, constants & time patterns
        MSX.Species = new Species[MSX.Nobjects[ObjectTypes.SPECIES.id]+1];
        MSX.Term    = new Term[MSX.Nobjects[ObjectTypes.TERM.id]+1];
        MSX.Param   = new Param[MSX.Nobjects[ObjectTypes.PARAMETER.id]+1];
        MSX.Const   = new Const[MSX.Nobjects[ObjectTypes.CONSTANT.id]+1];
        MSX.Pattern = new Pattern[MSX.Nobjects[ObjectTypes.PATTERN.id]+1];

        for(int i = 0;i<=MSX.Nobjects[ObjectTypes.CONSTANT.id];i++)
            MSX.Const[i] = new Const();

        // Create arrays for demands, heads, & flows
        MSX.D = new float[MSX.Nobjects[ObjectTypes.NODE.id]+1];
        MSX.H = new float[MSX.Nobjects[ObjectTypes.NODE.id]+1];
        MSX.Q = new float[MSX.Nobjects[ObjectTypes.LINK.id]+1];

        // create arrays for current & initial concen. of each species for each node
        MSX.C0 = new double[MSX.Nobjects[ObjectTypes.SPECIES.id]+1];
        for (int i=1; i<=MSX.Nobjects[ObjectTypes.NODE.id]; i++)
        {
            MSX.Node[i] = new Node(MSX.Nobjects[ObjectTypes.SPECIES.id]+1);

            //MSX.Node[i].c =  new double[MSX.Nobjects[SPECIES]+1, sizeof(double));
            //MSX.Node[i].c0 = new double[MSX.Nobjects[SPECIES]+1, sizeof(double));
            //MSX.Node[i].rpt = 0;
        }

        // Create arrays for init. concen. & kinetic parameter values for each link

        for (int i=1; i<=MSX.Nobjects[ObjectTypes.LINK.id]; i++)
        {
            MSX.Link[i] = new Link(MSX.Nobjects[ObjectTypes.SPECIES.id]+1,MSX.Nobjects[ObjectTypes.PARAMETER.id]+1);
            //MSX.Link[i].c0 = (double *)
            //calloc(MSX.Nobjects[SPECIES]+1, sizeof(double));
            //MSX.Link[i].param = (double *)
            //calloc(MSX.Nobjects[PARAMETER]+1, sizeof(double));
            //MSX.Link[i].rpt = 0;
        }

        // Create arrays for kinetic parameter values & current concen. for each tank

        for (int i=1; i<=MSX.Nobjects[ObjectTypes.TANK.id]; i++)
        {
            MSX.Tank[i] = new Tank(MSX.Nobjects[ObjectTypes.PARAMETER.id]+1,MSX.Nobjects[ObjectTypes.SPECIES.id]+1);

            //MSX.Tank[i].param = (double *)
            //calloc(MSX.Nobjects[PARAMETER]+1, sizeof(double));
            //MSX.Tank[i].c = (double *)
            //calloc(MSX.Nobjects[SPECIES]+1, sizeof(double));
        }

        // Initialize contents of each time pattern object

        for (int i=1; i<=MSX.Nobjects[ObjectTypes.PATTERN.id]; i++)
        {
            MSX.Pattern[i] = new Pattern();
            //MSX.Pattern[i].length = 0;
            //MSX.Pattern[i].first = null;
            //MSX.Pattern[i].current = null;
        }

        // Initialize reaction rate & equil. formulas for each species

        for (int i=1; i<=MSX.Nobjects[ObjectTypes.SPECIES.id]; i++)
        {
            MSX.Species[i] = new Species();
            //MSX.Species[i].pipeExpr     = null;
            //MSX.Species[i].tankExpr     = null;
            //MSX.Species[i].pipeExprType = NO_EXPR;
            //MSX.Species[i].tankExprType = NO_EXPR;
            //MSX.Species[i].precision    = 2;
            //MSX.Species[i].rpt = 0;
        }

        // Initialize math expressions for each intermediate term

        for (int i=1; i<=MSX.Nobjects[ObjectTypes.TERM.id]; i++){
            MSX.Term[i] = new Term();
            //MSX.Term[i].expr = null;
        }
        return 0;
    }

    //=============================================================================
    // Deletes multi-species data objects.
    void deleteObjects()
    {
        //int i;
        ////SnumList *listItem;
        //
// --- f//ree memory used by nodes, links, and tanks
        //
        //if (MSX.Node) for (i=1; i<=MSX.Nobjects[NODE]; i++)
        //{
        //    FREE(MSX.Node[i].c);
        //    FREE(MSX.Node[i].c0);
        //}
        //if (MSX.Link) for (i=1; i<=MSX.Nobjects[LINK]; i++)
        //{
        //    FREE(MSX.Link[i].c0);
        //    FREE(MSX.Link[i].param);
        //}
        //if (MSX.Tank) for (i=1; i<=MSX.Nobjects[TANK]; i++)
        //{
        //    FREE(MSX.Tank[i].param);
        //    FREE(MSX.Tank[i].c);
        //}
        //
// --- f//ree memory used by time patterns
        //
        //if (MSX.Pattern) for (i=1; i<=MSX.Nobjects[PATTERN]; i++)
        //{
        //    listItem = MSX.Pattern[i].first;
        //    while (listItem)
        //    {
        //        MSX.Pattern[i].first = listItem->next;
        //        free(listItem);
        //        listItem = MSX.Pattern[i].first;
        //    }
        //}
        //FREE(MSX.Pattern);
        //
// --- f//ree memory used for hydraulics results
        //
        //FREE(MSX.D);
        //FREE(MSX.H);
        //FREE(MSX.Q);
        //FREE(MSX.C0);
        //
// --- d//elete all nodes, links, and tanks
        //
        //FREE(MSX.Node);
        //FREE(MSX.Link);
        //FREE(MSX.Tank);
        //
// --- f//ree memory used by reaction rate & equilibrium expressions
        //
        //if (MSX.Species) for (i=1; i<=MSX.Nobjects[SPECIES]; i++)
        //{
        //    // --- free the species tank expression only if it doesn't
        //    //     already point to the species pipe expression
        //    if ( MSX.Species[i].tankExpr != MSX.Species[i].pipeExpr )
        //    {
        //        mathexpr_delete(MSX.Species[i].tankExpr);
        //    }
        //    mathexpr_delete(MSX.Species[i].pipeExpr);
        //}
        //
// --- d//elete all species, parameters, and constants
        //
        //FREE(MSX.Species);
        //FREE(MSX.Param);
        //FREE(MSX.Const);
        //
// --- f//ree memory used by intermediate terms
        //
        //if (MSX.Term) for (i=1; i<=MSX.Nobjects[TERM]; i++)
        //    mathexpr_delete(MSX.Term[i].expr);
        //FREE(MSX.Term);
    }

    // allocates memory for object ID hash tables.
    int createHashTables()
    {
        int j;

        // create a hash table for each type of object
        Htable = new Map[ObjectTypes.MAX_OBJECTS.id];

        for (j = 0; j < ObjectTypes.MAX_OBJECTS.id ; j++){
            Htable[j] = new Hashtable<String, Integer>();
        }

        return 0;
    }

    //=============================================================================
    // frees memory allocated for object ID hash tables.
    void deleteHashTables()
    {
        //int j;
        //
// --- f//ree the hash tables
        //
        //for (j = 0; j < MAX_OBJECTS; j++)
        //{
        //    if ( Htable[j] != null ) HTfree(Htable[j]);
        //}
        //
// --- f//ree the object ID memory pool
        //
        //if ( HashPool )
        //{
        //    AllocSetPool(HashPool);
        //    AllocFreePool();
        //}
    }

    // New function added (LR-11/20/07, to fix bug 08)
   //int openRptFile()
   //{
   //    if( MSX.RptFile.getFilename().equals(""))
   //        return 0;
   //
   //    //if ( MSX.RptFile.file ) fclose(MSX.RptFile.file);
   //    MSX.RptFile.close();
   //    //MSX.RptFile.file = fopen(MSX.RptFile.name, "wt");
   //    if(!MSX.RptFile.openAsTextWriter())
   //        return ErrorCodeType.ERR_OPEN_RPT_FILE.id;
   //    //if ( MSX.RptFile.file == null ) return ErrorCodeType.ERR_OPEN_RPT_FILE.id;
   //    return 0;
   //}


}
