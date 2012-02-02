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

package org.addition.epanet.network;


import org.addition.epanet.Constants;
import org.addition.epanet.network.io.Keywords;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.Utilities;

import java.util.*;

/**
 * Simulation configuration properties map.
 */
public class PropertiesMap {

    /**
     * Flow units.
     */
    static public enum FlowUnitsType {
        AFD(Keywords.w_AFD),         //   acre-feet per day
        CFS(Keywords.w_CFS),         //   cubic feet per second
        CMD(Keywords.w_CMD),         //   cubic meters per day
        CMH(Keywords.w_CMH),         //   cubic meters per hour
        GPM(Keywords.w_GPM),         //   gallons per minute
        IMGD(Keywords.w_IMGD),       //   imperial million gal. per day
        LPM(Keywords.w_LPM),         //   liters per minute
        LPS(Keywords.w_LPS),         //   liters per second
        MGD(Keywords.w_MGD),         //   million gallons per day
        MLD(Keywords.w_MLD);         //   megaliters per day

        public static FlowUnitsType parse(String text) {
            for (FlowUnitsType type : FlowUnitsType.values())
                if (Utilities.match(text, type.parseStr)) return type;
            return null;
        }

        public final String parseStr;

        private FlowUnitsType(String parseStr) {
            this.parseStr = parseStr;
        }
    }

    /**
     * Head loss formula.
     */
    static public enum FormType {
        /**
         * Chezy-Manning
         */
        CM(Keywords.w_CM),
        /**
         * Darcy-Weisbach
         */
        DW(Keywords.w_DW),
        /**
         * Hazen-Williams
         */
        HW(Keywords.w_HW);
        /**
         * Parse string id.
         */
        public final String parseStr;

        FormType(String parseStr) {
            this.parseStr = parseStr;
        }
    }

    /**
     * Hydraulics solution option.
     */
    static public enum Hydtype {
        /**
         * Save after current run.
         */
        SAVE,
        /**
         * Use temporary file.
         */
        SCRATCH,
        /**
         * Use from previous run.
         */
        USE
    }

    /**
     * Pressure units.
     */
    static public enum PressUnitsType {
        KPA(Keywords.w_KPA),        // pounds per square inch
        METERS(Keywords.w_METERS),        // kiloPascals
        PSI(Keywords.w_PSI);  // meters

        public final String parseStr;

        PressUnitsType(String parseStr) {
            this.parseStr = parseStr;
        }
    }

    /**
     * Water quality analysis option.
     */
    static public enum QualType {
        /**
         * Analyze water age.
         */
        AGE(2, Keywords.w_AGE),
        /**
         * Analyze a chemical.
         */
        CHEM(1, Keywords.w_CHEM),
        /**
         * No quality analysis.
         */
        NONE(0, Keywords.w_NONE),
        /**
         * Trace % of flow from a source
         */
        TRACE(3, Keywords.w_TRACE);

        /**
         * Parse quality type from string.
         */
        public static QualType parse(String text) {
            for (QualType type : QualType.values())
                if (Utilities.match(text, type.parseStr)) return type;
            return null;
        }

        /**
         * Sequencial id.
         */
        public final int id;

        /**
         * Parse string id.
         */
        public final String parseStr;

        private QualType(int id, String pStr) {
            this.id = id;
            this.parseStr = pStr;
        }
    }

    /**
     * Reporting flag.
     */
    static public enum ReportFlag {
        FALSE, //0
        SOME, //2,
        // //1
        TRUE
    }

    /**
     * Status report options.
     */
    static public enum StatFlag {
        FALSE(Keywords.w_NO),
        FULL(Keywords.w_FULL),
        TRUE(Keywords.w_YES);

        public static StatFlag parse(String text) {
            for (StatFlag type : StatFlag.values())
                if (Utilities.match(text, type.parseStr)) return type;
            return null;
        }

        public final String parseStr;

        private StatFlag(String parseStr) {
            this.parseStr = parseStr;
        }

    }

    /**
     * Time series statistics.
     */
    static public enum TstatType {
        AVG(Keywords.w_AVG),      // none
        MAX(Keywords.w_MAX),       // time-averages
        MIN(Keywords.w_MIN),       // minimum values
        RANGE(Keywords.w_RANGE),       // maximum values
        SERIES(Keywords.w_NONE);     // max - min values

        public final String parseStr;

        private TstatType(String parseStr) {
            this.parseStr = parseStr;
        }
    }


    /**
     * Unit system.
     */
    static public enum UnitsType {
        /**
         * SI (metric)
         */
        SI,
        /**
         * US
         */
        US
    }

    public static final String ALTREPORT = "AltReport";
    public static final String BULKORDER = "BulkOrder";
    public static final String CHECK_FREQ = "CheckFreq";
    public static final String CHEM_NAME = "ChemName";
    public static final String CHEM_UNITS = "ChemUnits";
    public static final String CLIMIT = "Climit";
    public static final String CTOL = "Ctol";
    public static final String DAMP_LIMIT = "DampLimit";
    public static final String DCOST = "Dcost";
    public static final String DEF_PAT_ID = "DefPatID";
    public static final String DIFFUS = "Diffus";
    public static final String DMULT = "Dmult";
    public static final String DUR = "Dur";
    public static final String ECOST = "Ecost";
    public static final String EMAX = "Emax";
    public static final String ENERGYFLAG = "Energyflag";
    public static final String EPAT_ID = "EpatID";
    public static final String EPUMP = "Epump";
    public static final String EXTRA_ITER = "ExtraIter";
    public static final String FLOWFLAG = "Flowflag";
    public static final String FORMFLAG = "Formflag";
    public static final String HACC = "Hacc";
    public static final String HEXP = "Hexp";
    public static final String HSTEP = "Hstep";
    public static final String HTOL = "Htol";
    public static final String HYD_FNAME = "HydFname";
    public static final String HYDFLAG = "Hydflag";
    public static final String KBULK = "Kbulk";
    public static final String KWALL = "Kwall";
    public static final String LINKFLAG = "Linkflag";
    public static final String MAP_FNAME = "MapFname";
    public static final String MAXCHECK = "MaxCheck";
    public static final String MAXITER = "MaxIter";
    public static final String MESSAGEFLAG = "Messageflag";
    public static final String NODEFLAG = "Nodeflag";
    public static final String PAGE_SIZE = "PageSize";
    public static final String PRESSFLAG = "Pressflag";
    public static final String PSTART = "Pstart";
    public static final String PSTEP = "Pstep";
    public static final String QEXP = "Qexp";
    public static final String QSTEP = "Qstep";
    public static final String QTOL = "Qtol";
    public static final String QUALFLAG = "Qualflag";
    public static final String RFACTOR = "Rfactor";
    public static final String RQTOL = "RQtol";
    public static final String RSTART = "Rstart";
    public static final String RSTEP = "Rstep";
    public static final String RULESTEP = "Rulestep";
    public static final String SPGRAV = "SpGrav";
    public static final String STATFLAG = "Statflag";
    public static final String SUMMARYFLAG = "Summaryflag";
    public static final String TANKORDER = "TankOrder";
    public static final String TRACE_NODE = "TraceNode";
    public static final String TSTART = "Tstart";
    public static final String TSTATFLAG = "Tstatflag";
    public static final String UNITSFLAG = "Unitsflag";
    public static final String VISCOS = "Viscos";

    public static final String WALLORDER = "WallOrder";
    public static final String[] EpanetObjectsNames = {TSTATFLAG, HSTEP, DUR, QSTEP, CHECK_FREQ,
            MAXCHECK, DMULT, ALTREPORT, QEXP, HEXP, RQTOL, QTOL, BULKORDER, TANKORDER, WALLORDER,
            RFACTOR, CLIMIT, KBULK, KWALL, DCOST, ECOST, EPAT_ID, EPUMP, PAGE_SIZE, STATFLAG, SUMMARYFLAG,
            MESSAGEFLAG, ENERGYFLAG, NODEFLAG, LINKFLAG, RULESTEP, PSTEP, PSTART, RSTEP, RSTART, TSTART,
            FLOWFLAG, PRESSFLAG, FORMFLAG, HYDFLAG, QUALFLAG, UNITSFLAG, HYD_FNAME, CHEM_NAME, CHEM_UNITS,
            DEF_PAT_ID, MAP_FNAME, TRACE_NODE, EXTRA_ITER, CTOL, DIFFUS, DAMP_LIMIT, VISCOS, SPGRAV, MAXITER,
            HACC, HTOL, EMAX
    };

    private Map<String, Object> values;



    public PropertiesMap() {
        values = new HashMap<String, Object>();
        loadDefaults();
    }

    /**
     * Get an object from the map.
     *
     * @param name Object name.
     * @return Object refernce.
     * @throws ENException If object name not found.
     */
    public Object get(String name) throws ENException {
        return values.get(name);
    }

    public String getAltReport() throws ENException {
        return (String) get(ALTREPORT);
    }

    public Double getBulkOrder() throws ENException {
        return (Double) get(BULKORDER);
    }

    public Integer getCheckFreq() throws ENException {
        return (Integer) get(CHECK_FREQ);
    }

    public String getChemName() throws ENException {
        return (String) get(CHEM_NAME);
    }

    public String getChemUnits() throws ENException {
        return (String) get(CHEM_UNITS);
    }

    public Double getClimit() throws ENException {
        return (Double) get(CLIMIT);
    }

    public Double getCtol() throws ENException {
        return (Double) get(CTOL);
    }

    public Double getDampLimit() throws ENException {
        return (Double) get(DAMP_LIMIT);
    }

    public Double getDcost() throws ENException {
        return (Double) get(DCOST);
    }

    public String getDefPatId() throws ENException {
        return (String) get(DEF_PAT_ID);
    }

    public Double getDiffus() throws ENException {
        return (Double) get(DIFFUS);
    }

    public Double getDmult() throws ENException {
        return (Double) get(DMULT);
    }

    public Long getDuration() throws ENException {
        return (Long) get(DUR);
    }

    public Double getEcost() throws ENException {
        return (Double) get(ECOST);
    }

    public Double getEmax() throws ENException {
        return (Double) get(EMAX);
    }

    public Boolean getEnergyflag() throws ENException {
        return (Boolean) get(ENERGYFLAG);
    }

    public String getEpatId() throws ENException {
        return (String) get(EPAT_ID);
    }

    public Double getEpump() throws ENException {
        return (Double) get(EPUMP);
    }

    public Integer getExtraIter() throws ENException {
        return (Integer) get(EXTRA_ITER);
    }

    public FlowUnitsType getFlowflag() throws ENException {
        return (FlowUnitsType) get(FLOWFLAG);
    }

    public FormType getFormflag() throws ENException {
        return (FormType) get(FORMFLAG);
    }

    public Double getHacc() throws ENException {
        return (Double) get(HACC);
    }

    public Double getHexp() throws ENException {
        return (Double) get(HEXP);
    }

    public Long getHstep() throws ENException {
        return (Long) get(HSTEP);
    }

    public Double getHtol() throws ENException {
        return (Double) get(HTOL);
    }

    public Hydtype getHydflag() throws ENException {
        return (Hydtype) get(HYDFLAG);
    }

    public String getHydFname() throws ENException {
        return (String) get(HYD_FNAME);
    }

    public Double getKbulk() throws ENException {
        return (Double) get(KBULK);
    }

    public Double getKwall() throws ENException {
        return (Double) get(KWALL);
    }

    public ReportFlag getLinkflag() throws ENException {
        return (ReportFlag) get(LINKFLAG);
    }

    public String getMapFname() throws ENException {
        return (String) get(MAP_FNAME);
    }

    public Integer getMaxCheck() throws ENException {
        return (Integer) get(MAXCHECK);
    }

    public Integer getMaxIter() throws ENException {
        return (Integer) get(MAXITER);
    }

    public Boolean getMessageflag() throws ENException {
        return (Boolean) get(MESSAGEFLAG);
    }

    public ReportFlag getNodeflag() throws ENException {
        return (ReportFlag) get(NODEFLAG);
    }

    /**
     * Get objects names in this map.
     *
     * @param exclude_epanet exclude Epanet objects.
     * @return List of objects names.
     */
    public List<String> getObjectsNames(boolean exclude_epanet) {
        List<String> allObjs = new ArrayList<String>(values.keySet());
        if (exclude_epanet)
            allObjs.removeAll(Arrays.asList(EpanetObjectsNames));
        return allObjs;
    }

    public Integer getPageSize() throws ENException {
        return (Integer) get(PAGE_SIZE);
    }

    public PressUnitsType getPressflag() throws ENException {
        return (PressUnitsType) get(PRESSFLAG);
    }

    public Long getPstart() throws ENException {
        return (Long) get(PSTART);
    }

    public Long getPstep() throws ENException {
        return (Long) get(PSTEP);
    }

    public Double getQexp() throws ENException {
        return (Double) get(QEXP);
    }

    public Long getQstep() throws ENException {
        return (Long) get(QSTEP);
    }

    public Double getQtol() throws ENException {
        return (Double) get(QTOL);
    }

    public QualType getQualflag() throws ENException {
        return (QualType) get(QUALFLAG);
    }

    public Double getRfactor() throws ENException {
        return (Double) get(RFACTOR);
    }

    public Double getRQtol() throws ENException {
        return (Double) get(RQTOL);
    }

    public Long getRstart() throws ENException {
        return (Long) get(RSTART);
    }

    public Long getRstep() throws ENException {
        return (Long) get(RSTEP);
    }

    public Long getRulestep() throws ENException {
        return (Long) get(RULESTEP);
    }

    public Double getSpGrav() throws ENException {
        return (Double) get(SPGRAV);
    }

    public StatFlag getStatflag() throws ENException {
        return (StatFlag) get(STATFLAG);
    }

    public Boolean getSummaryflag() throws ENException {
        return (Boolean) get(SUMMARYFLAG);
    }

    public Double getTankOrder() throws ENException {
        return (Double) get(TANKORDER);
    }

    public String getTraceNode() throws ENException {
        return (String) get(TRACE_NODE);
    }

    public Long getTstart() throws ENException {
        return (Long) get(TSTART);
    }

    public TstatType getTstatflag() throws ENException {
        return (TstatType) get(TSTATFLAG);
    }

    public UnitsType getUnitsflag() throws ENException {
        return (UnitsType) get(UNITSFLAG);
    }

    public Double getViscos() throws ENException {
        return (Double) get(VISCOS);
    }

    public Double getWallOrder() throws ENException {
        return (Double) get(WALLORDER);
    }

    /**
     * Init properties with default value.
     */
    private void loadDefaults() {
        put(BULKORDER, new Double(1.0d));     // 1st-order bulk reaction rate
        put(TANKORDER, new Double(1.0d));     // 1st-order tank reaction rate
        put(WALLORDER, new Double(1.0d));     // 1st-order wall reaction rate
        put(RFACTOR, new Double(1.0d));     // No roughness-reaction factor
        put(CLIMIT, new Double(0.0d));     // No limiting potential quality
        put(KBULK, new Double(0.0d));     // No global bulk reaction
        put(KWALL, new Double(0.0d));     // No global wall reaction
        put(DCOST, new Double(0.0d));     // Zero energy demand charge
        put(ECOST, new Double(0.0d));     // Zero unit energy cost
        put(EPAT_ID, "");                   // No energy price pattern
        put(EPUMP, Constants.EPUMP);      // Default pump efficiency
        put(PAGE_SIZE, Constants.PAGESIZE);
        put(STATFLAG, StatFlag.FALSE);
        put(SUMMARYFLAG, true);
        put(MESSAGEFLAG, true);
        put(ENERGYFLAG, false);
        put(NODEFLAG, ReportFlag.FALSE);
        put(LINKFLAG, ReportFlag.FALSE);
        put(TSTATFLAG, TstatType.SERIES);     // Generate time series output
        put(HSTEP, new Long(3600));       // 1 hr hydraulic time step
        put(DUR, new Long(0));          // 0 sec duration (steady state)
        put(QSTEP, new Long(0));          // No pre-set quality time step
        put(RULESTEP, new Long(0));          // No pre-set rule time step
        put(PSTEP, new Long(3600));       // 1 hr time pattern period
        put(PSTART, new Long(0));          // Starting pattern period
        put(RSTEP, new Long(3600));       // 1 hr reporting period
        put(RSTART, new Long(0));          // Start reporting at time 0
        put(TSTART, new Long(0));          // Starting time of day
        put(FLOWFLAG, FlowUnitsType.GPM);    // Flow units are gpm
        put(PRESSFLAG, PressUnitsType.PSI);   // Pressure units are psi
        put(FORMFLAG, FormType.HW);          // Use Hazen-Williams formula
        put(HYDFLAG, Hydtype.SCRATCH);      // No external hydraulics file
        put(QUALFLAG, QualType.NONE);        // No quality simulation
        put(UNITSFLAG, UnitsType.US);         // US unit system
        put(HYD_FNAME, "");
        put(CHEM_NAME, Keywords.t_CHEMICAL);
        put(CHEM_UNITS, Keywords.u_MGperL);    // mg/L
        put(DEF_PAT_ID, Constants.DEFPATID);   // Default demand pattern index
        put(MAP_FNAME, "");
        put(ALTREPORT, "");
        put(TRACE_NODE, "");                   // No source tracing
        put(EXTRA_ITER, new Integer(-1));      // Stop if network unbalanced
        put(CTOL, Constants.MISSING);    // No pre-set quality tolerance
        put(DIFFUS, Constants.MISSING);    // Temporary diffusivity
        put(DAMP_LIMIT, Constants.DAMPLIMIT);
        put(VISCOS, Constants.MISSING);    // Temporary viscosity
        put(SPGRAV, Constants.SPGRAV);     // Default specific gravity
        put(MAXITER, Constants.MAXITER);    // Default max. hydraulic trials
        put(HACC, Constants.HACC);       // Default hydraulic accuracy
        put(HTOL, Constants.HTOL);       // Default head tolerance
        put(QTOL, Constants.QTOL);       // Default flow tolerance
        put(RQTOL, Constants.RQTOL);      // Default hydraulics parameters
        put(HEXP, new Double(0.0d));
        put(QEXP, new Double(2.0d));     // Flow exponent for emitters
        put(CHECK_FREQ, Constants.CHECKFREQ);
        put(MAXCHECK, Constants.MAXCHECK);
        put(DMULT, new Double(1.0d));     // Demand multiplier
        put(EMAX, new Double(0.0d));     // Zero peak energy usage


    }


    /**
     * Insert an object into the map.
     *
     * @param name Object name.
     * @param obj  Object reference.
     */
    public void put(String name, Object obj) {
        values.put(name, obj);
    }

    public void setAltReport(String str) throws ENException {
        put(ALTREPORT, str);
    }

    public void setBulkOrder(Double bulkOrder) throws ENException {
        put(BULKORDER, bulkOrder);
    }

    public void setCheckFreq(int checkFreq) throws ENException {
        put(CHECK_FREQ, checkFreq);
    }

    public void setChemName(String chemName) throws ENException {
        put(CHEM_NAME, chemName);
    }

    public void setChemUnits(String chemUnits) throws ENException {
        put(CHEM_UNITS, chemUnits);
    }

    public void setClimit(Double climit) throws ENException {
        put(CLIMIT, climit);
    }

    public void setCtol(Double ctol) throws ENException {
        put(CTOL, ctol);
    }

    public void setDampLimit(Double dampLimit) throws ENException {
        put(DAMP_LIMIT, dampLimit);
    }

    public void setDcost(Double dcost) throws ENException {
        put(DCOST, dcost);
    }

    public void setDefPatId(String defPatID) throws ENException {
        put(DEF_PAT_ID, defPatID);
    }

    public void setDiffus(Double diffus) throws ENException {
        put(DIFFUS, diffus);
    }

    public void setDmult(Double dmult) throws ENException {
        put(DMULT, dmult);
    }

    public void setDuration(long dur) throws ENException {
        put(DUR, dur);
    }

    public void setEcost(Double ecost) throws ENException {
        put(ECOST, ecost);
    }

    public void setEmax(Double emax) throws ENException {
        put(EMAX, emax);
    }

    public void setEnergyflag(boolean energyflag) throws ENException {
        put(ENERGYFLAG, energyflag);
    }

    public void setEpatId(String epat) throws ENException {
        put(EPAT_ID, epat);
    }

    public void setEpump(Double epump) throws ENException {
        put(EPUMP, epump);
    }

    public void setExtraIter(int extraIter) throws ENException {
        put(EXTRA_ITER, extraIter);
    }

    public void setFlowflag(FlowUnitsType flowflag) throws ENException {
        put(FLOWFLAG, flowflag);
    }

    public void setFormflag(FormType formflag) throws ENException {
        put(FORMFLAG, formflag);
    }

    public void setHacc(Double hacc) throws ENException {
        put(HACC, hacc);
    }

    public void setHexp(Double hexp) throws ENException {
        put(HEXP, hexp);
    }

    public void setHstep(long hstep) throws ENException {
        put(HSTEP, hstep);
    }

    public void setHtol(Double htol) throws ENException {
        put(HTOL, htol);
    }

    public void setHydflag(Hydtype hydflag) throws ENException {
        put(HYDFLAG, hydflag);
    }

    public void setHydFname(String hydFname) throws ENException {
        put(HYD_FNAME, hydFname);
    }

    public void setKbulk(Double kbulk) throws ENException {
        put(KBULK, kbulk);
    }

    public void setKwall(Double kwall) throws ENException {
        put(KWALL, kwall);
    }

    public void setLinkflag(ReportFlag linkflag) throws ENException {
        put(LINKFLAG, linkflag);
    }

    public void setMapFname(String mapFname) throws ENException {
        put(MAP_FNAME, mapFname);
    }

    public void setMaxCheck(int maxCheck) throws ENException {
        put(MAXCHECK, maxCheck);
    }

    public void setMaxIter(int maxIter) throws ENException {
        put(MAXITER, maxIter);
    }

    public void setMessageflag(boolean messageflag) throws ENException {
        put(MESSAGEFLAG, messageflag);
    }

    public void setNodeflag(ReportFlag nodeflag) throws ENException {
        put(NODEFLAG, nodeflag);
    }

    public void setPageSize(int pageSize) throws ENException {
        put(PAGE_SIZE, pageSize);
    }

    public void setPressflag(PressUnitsType pressflag) throws ENException {
        put(PRESSFLAG, pressflag);
    }

    public void setPstart(long pstart) throws ENException {
        put(PSTART, pstart);
    }

    public void setPstep(long pstep) throws ENException {
        put(PSTEP, pstep);
    }

    public void setQexp(Double qexp) throws ENException {
        put(QEXP, qexp);
    }

    public void setQstep(long qstep) throws ENException {
        put(QSTEP, qstep);
    }

    public void setQtol(Double qtol) throws ENException {
        put(QTOL, qtol);
    }

    public void setQualflag(QualType qualflag) throws ENException {
        put(QUALFLAG, qualflag);
    }

    public void setRfactor(Double rfactor) throws ENException {
        put(RFACTOR, rfactor);
    }

    public void setRQtol(Double RQtol) throws ENException {
        put(RQTOL, RQtol);
    }

    public void setRstart(long rstart) throws ENException {
        put(RSTART, rstart);
    }

    public void setRstep(long rstep) throws ENException {
        put(RSTEP, rstep);
    }

    public void setRulestep(long rulestep) throws ENException {
        put(RULESTEP, rulestep);
    }

    public void setSpGrav(Double spGrav) throws ENException {
        put(SPGRAV, spGrav);
    }

    public void setStatflag(StatFlag statflag) throws ENException {
        put(STATFLAG, statflag);
    }

    public void setSummaryflag(boolean summaryflag) throws ENException {
        put(SUMMARYFLAG, summaryflag);
    }

    public void setTankOrder(Double tankOrder) throws ENException {
        put(TANKORDER, tankOrder);
    }

    public void setTraceNode(String traceNode) throws ENException {
        put(TRACE_NODE, traceNode);
    }

    public void setTstart(long tstart) throws ENException {
        put(TSTART, tstart);
    }

    public void setTstatflag(TstatType tstatflag) throws ENException {
        put(TSTATFLAG, tstatflag);
    }

    public void setUnitsflag(UnitsType unitsflag) throws ENException {
        put(UNITSFLAG, unitsflag);
    }

    public void setViscos(Double viscos) throws ENException {
        put(VISCOS, viscos);
    }

    public void setWallOrder(Double wallOrder) throws ENException {
        put(WALLORDER, wallOrder);
    }


}
