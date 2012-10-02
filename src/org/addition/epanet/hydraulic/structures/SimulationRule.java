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

package org.addition.epanet.hydraulic.structures;


import org.addition.epanet.Constants;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.Utilities;
import java.util.logging.Logger;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.Rule;
import org.addition.epanet.network.structures.Rule.*;
import org.addition.epanet.network.FieldsMap.*;
import org.addition.epanet.network.structures.Link.*;

import java.util.ArrayList;
import java.util.List;

public class SimulationRule {

    // Temporary action item
    public static class ActItem{
        public ActItem(SimulationRule rule, Action action) {
            this.rule = rule;
            this.action = action;
        }

        SimulationRule rule;
        SimulationRule.Action action;
    }

    // Rules execution result
    public static class Result{
        public Result(long step, long htime) {
            this.step = step;
            this.htime = htime;
        }

        public long step;
        public long htime;

    }

    // Rule premise
    public class Premise{

        public Premise(String []Tok,Rule.Rulewords lOp,List<SimulationNode> nodes, List<SimulationLink> links) throws ENException {
            Rule.Objects    loType;
            Rule.Varwords   lVar;
            Object          lObj;
            Operators       lROp;

            if (Tok.length != 5 && Tok.length != 6)
                throw new ENException(201);

            loType = Rule.Objects.parse(Tok[1]);

            if (loType == Rule.Objects.r_SYSTEM){
                lVar = Rule.Varwords.parse(Tok[2]);

                if (lVar != Rule.Varwords.r_DEMAND && lVar != Rule.Varwords.r_TIME && lVar != Rule.Varwords.r_CLOCKTIME)
                    throw new ENException(201);

                lObj = Rule.Objects.r_SYSTEM;
            }
            else
            {
                lVar = Rule.Varwords.parse(Tok[3]);
                if (lVar == null)
                    throw new ENException(201);

                switch (loType)
                {
                    case r_NODE:
                    case r_JUNC:
                    case r_RESERV:
                    case r_TANK:
                        loType = Rule.Objects.r_NODE;
                        break;
                    case r_LINK:
                    case r_PIPE:
                    case r_PUMP:
                    case r_VALVE:
                        loType = Rule.Objects.r_LINK;
                        break;
                    default:
                        throw new ENException(201);
                }

                if (loType == Rule.Objects.r_NODE){
                    //Node nodeRef = net.getNode(Tok[2]);
                    SimulationNode nodeRef = null;
                    for(SimulationNode simNode : nodes)
                        if(simNode.getNode().getId().equals(Tok[2]))
                            nodeRef = simNode;

                    if (nodeRef == null)
                        throw new ENException(203);
                    switch (lVar){
                        case r_DEMAND:
                        case r_HEAD:
                        case r_GRADE:
                        case r_LEVEL:
                        case r_PRESSURE:
                            break;
                        case r_FILLTIME:
                        case r_DRAINTIME: if (nodeRef instanceof SimulationTank) throw new ENException(201); break;

                        default:
                            throw new ENException(201);
                    }
                    lObj = nodeRef;
                }
                else{
                    //Link linkRef = net.getLink(Tok[2]);
                    SimulationLink linkRef = null;
                    for(SimulationLink simLink : links)
                        if(simLink.getLink().getId().equals(Tok[2]))
                            linkRef = simLink;

                    if (linkRef == null)
                        throw new ENException(204);
                    switch (lVar){
                        case r_FLOW:
                        case r_STATUS:
                        case r_SETTING:
                            break;
                        default:
                            throw new ENException(201);
                    }
                    lObj = linkRef;
                }
            }

            Rule.Operators op;

            if (loType == Rule.Objects.r_SYSTEM)
                op = Rule.Operators.parse(Tok[3]);
            else
                op = Rule.Operators.parse(Tok[4]);

            if (op == null)
                throw new ENException(201);

            switch(op)
            {
                case IS:
                    lROp = Operators.EQ;
                    break;
                case NOT:
                    lROp = Operators.NE;
                    break;
                case BELOW:
                    lROp = Operators.LT;
                    break;
                case ABOVE:
                    lROp = Operators.GT;
                    break;
                default:
                    lROp = op;
            }

            Values lStat = Values.IS_NUMBER;
            Double lVal = Constants.MISSING;

            if (lVar == Varwords.r_TIME || lVar == Varwords.r_CLOCKTIME)
            {
                if (Tok.length == 6)
                    lVal = Utilities.getHour(Tok[4],Tok[5])*3600.;
                else
                    lVal = Utilities.getHour(Tok[4],"")*3600.;

                if (lVal < 0.0)
                    throw new ENException(202);
            }
            else{
                Values k = Values.parse(Tok[Tok.length-1]);

                if(k == null || lStat.id <= Values.IS_NUMBER.id){
                    if (lStat==null || lStat.id <= Values.IS_NUMBER.id){
                        if ((lVal = Utilities.getDouble(Tok[Tok.length - 1]))==null)
                            throw new ENException(202);
                        if (lVar == Varwords.r_FILLTIME || lVar == Varwords.r_DRAINTIME)
                            lVal = lVal*3600.0;
                    }
                }
                else{
                    lStat = k;
                }

            }

            status = lStat;
            value = lVal;
            logop = lOp;
            relop = lROp;
            variable = lVar;
            object = lObj;
        }

        private final Object      object;
        private final Rulewords   logop;      // Logical operator
        private final Varwords    variable;   // Pressure, flow, etc
        private final Operators   relop;      // Relational operator
        private final Values      status;     // Variable's status
        private final double      value;      // Variable's value

        public Rulewords getLogop(){
            return logop;
        }

        public Object getObject(){
            return object;
        }

        public Varwords getVariable(){
            return variable;
        }

        public Operators getRelop(){
            return relop;
        }

        public Values getStatus(){
            return status;
        }

        public double getValue(){
            return value;
        }


        // Checks if a particular premise is true
        private boolean checkPremise(FieldsMap fMap,PropertiesMap pMap,
                                     long Time1, long Htime, double dsystem) throws ENException
        {
            if (variable == Varwords.r_TIME || variable ==  Varwords.r_CLOCKTIME)
                return(checkTime(pMap,Time1,Htime));
            else if (status.id > Values.IS_NUMBER.id)
                return(checkStatus());
            else
                return(checkValue(fMap,dsystem));
        }

        // Checks if condition on system time holds
        private boolean checkTime(PropertiesMap pMap, long Time1, long Htime) throws ENException
        {
            boolean  flag;
            long  t1,t2,x;

            if (variable == Varwords.r_TIME){
                t1 = Time1;
                t2 = Htime;
            }
            else if (variable == Varwords.r_CLOCKTIME)
            {
                t1 = (Time1 + pMap.getTstart()) % Constants.SECperDAY;
                t2 = (Htime + pMap.getTstart()) % Constants.SECperDAY;
            }
            else
                return false;

            x = (long)(value);
            switch (relop)
            {
                case LT: if (t2 >= x) return(false); break;
                case LE: if (t2 >  x) return(false); break;
                case GT: if (t2 <= x) return(false); break;
                case GE: if (t2 <  x) return(false); break;

                case EQ:
                case NE:
                    flag = false;
                    if (t2 < t1)
                    {
                        if (x >= t1 || x <= t2) flag = true;
                    }
                    else
                    {
                        if (x >= t1 && x <= t2) flag = true;
                    }
                    if (relop == Operators.EQ && !flag) return true;
                    if (relop == Operators.NE && flag)  return true;
                    break;
            }

            return true;
        }

        // Checks if condition on link status holds
        private boolean checkStatus()
        {
            switch (status)
            {
                case IS_OPEN:
                case IS_CLOSED:
                case IS_ACTIVE:
                    Values  j;
                    StatType i=null;
                    if(object instanceof SimulationLink)
                        i = ((SimulationLink) object).getSimStatus();

                    if (i!=null && i.id <= StatType.CLOSED.id)
                        j = Values.IS_CLOSED;
                    else if (i == StatType.ACTIVE)
                        j = Values.IS_ACTIVE;
                    else
                        j = Values.IS_OPEN;

                    if (j == status && relop == Operators.EQ)
                        return true;
                    if (j != status && relop == Operators.NE)
                        return true;
            }
            return false;
        }

        // Checks if numerical condition on a variable is true.
        private boolean checkValue(FieldsMap fMap,double dsystem) throws ENException
        {
            double tol = 1.e-3;
            double x;

            SimulationLink link = null;
            SimulationNode node = null;

            if(object instanceof SimulationLink)
                link = ((SimulationLink)object);
            else if(object instanceof SimulationNode)
                node = ((SimulationNode)object);

            switch (variable)
            {
                case r_DEMAND:
                    if (object == Objects.r_SYSTEM)
                        x = dsystem*fMap.getUnits(Type.DEMAND);
                    else
                        x = node.getSimDemand()*fMap.getUnits(Type.DEMAND);
                    break;

                case r_HEAD:
                case r_GRADE:
                    x = node.getSimHead() * fMap.getUnits(Type.HEAD);
                    break;
                case r_PRESSURE:
                    x = (node.getSimHead() - node.getElevation())*fMap.getUnits(Type.PRESSURE);
                    break;
                case r_LEVEL:
                    x = (node.getSimHead() - node.getElevation())*fMap.getUnits(Type.HEAD);
                    break;
                case r_FLOW:
                    x = Math.abs(link.getSimFlow())*fMap.getUnits(Type.FLOW);
                    break;
                case r_SETTING:

                    if (link.getSimSetting() == Constants.MISSING)
                        return false;
                    x = link.getSimSetting();
                    switch (link.getType())
                    {
                        case PRV:
                        case PSV:
                        case PBV:
                            x = x*fMap.getUnits(Type.PRESSURE);
                            break;
                        case FCV:
                            x = x*fMap.getUnits(Type.FLOW);
                            break;
                    }
                    break;
                case r_FILLTIME:
                {
                    if(!(object instanceof SimulationTank))
                        return false;

                    SimulationTank tank = (SimulationTank)object;

                    if (tank.isReservoir())
                        return false;

                    if (tank.getSimDemand() <= Constants.TINY)
                        return false;

                    x = (tank.getVmax() - tank.getSimVolume())/tank.getSimDemand();

                    break;
                }
                case r_DRAINTIME:
                {
                    if(!(object instanceof SimulationTank))
                        return false;

                    SimulationTank tank = (SimulationTank)object;

                    if (tank.isReservoir())
                        return false;

                    if (tank.getSimDemand() >= -Constants.TINY)
                        return false;

                    x = (tank.getVmin() - tank.getSimVolume())/tank.getSimDemand();
                    break;
                }
                default:
                    return false;
            }
            switch (relop)
            {
                case EQ:
                    if (Math.abs(x - value) > tol)
                        return false;
                    break;
                case NE:
                    if (Math.abs(x - value) < tol)
                        return false;
                    break;
                case LT:
                    if (x > value + tol)
                        return false;
                    break;
                case LE:
                    if (x > value - tol)
                        return false;
                    break;
                case GT:
                    if (x < value - tol)
                        return false;
                    break;
                case GE:
                    if (x < value + tol)
                        return false;
                    break;
            }
            return true;
        }

    }

    public class Action{
        public Action(String[] tok, List<SimulationLink> links) throws ENException {
            int Ntokens = tok.length;

            Values s,k;
            Double x;

            if (Ntokens != 6)
                throw new ENException(201);

            //Link linkRef = net.getLink(tok[2]);
            SimulationLink linkRef = null;
            for(SimulationLink simLink : links)
                if(simLink.getLink().getId().equals(tok[2]))
                    linkRef = simLink;

            if (linkRef == null)
                throw new ENException(204);

            if (linkRef.getType() == LinkType.CV)
                throw new ENException(207);

            s = null;
            x = Constants.MISSING;
            k = Values.parse(tok[5]);

            if (k!=null && k.id > Values.IS_NUMBER.id)
                s = k;
            else
            {
                if ( (x = Utilities.getDouble(tok[5]))==null )
                    throw new ENException(202);
                if (x < 0.0)
                    throw new ENException(202);
            }

            if (x != Constants.MISSING && linkRef.getType() == LinkType.GPV)
                throw new ENException(202);

            if (x != Constants.MISSING && linkRef.getType() == LinkType.PIPE){
                if (x == 0.0)
                    s = Values.IS_CLOSED;
                else
                    s = Values.IS_OPEN;
                x = Constants.MISSING;
            }


            link = linkRef;
            status = s;
            setting  = x;
        }
        private final SimulationLink link;
        private final Values status;
        private final double setting;

        public SimulationLink getLink() {
            return link;
        }

        public Values getStatus() {
            return status;
        }

        public double getSetting() {
            return setting;
        }

        // Execute action, returns true if the link was alterated.
        private boolean execute(FieldsMap fMap,PropertiesMap pMap,Logger log, double tol,long Htime) throws ENException {
            boolean flag = false;

            StatType s = link.getSimStatus();
            double v = link.getSimSetting();
            double x = setting;

            if (status == Values.IS_OPEN && s.id <= StatType.CLOSED.id){  // Switch link from closed to open
                link.setLinkStatus(true);
                flag = true;
            }
            else if (status == Values.IS_CLOSED && s.id > StatType.CLOSED.id){  // Switch link from not closed to closed
                link.setLinkStatus(false);
                flag = true;
            }
            else if(x != Constants.MISSING){  // Change link's setting
                switch(link.getType())
                {
                    case PRV:
                    case PSV:
                    case PBV:    x = x/fMap.getUnits(Type.PRESSURE);  break;
                    case FCV:    x = x/fMap.getUnits(Type.FLOW);      break;
                }
                if (Math.abs(x-v) > tol)
                {
                    link.setLinkSetting(x);
                    flag = true;
                }
            }

            if (flag){
                if (pMap.getStatflag()!=null) // Report rule action
                    logRuleExecution(log,Htime);
                return true;
            }

            return false;
        }

        public void logRuleExecution(Logger log, long Htime){
            log.warning(String.format(Utilities.getText("FMT63"),Utilities.getClockTime(Htime), link.getType().parseStr, link.getLink().getId(), label));
        }
    }


    private final String           label;
    private final double           priority;
    private final List<Premise>    Pchain;
    private final List<Action>     Tchain;
    private final List<Action>     Fchain;

    public String getLabel() {
        return label;
    }

    public double getPriority() {
        return priority;
    }

    public Premise[] getPchain() {
        return Pchain.toArray(new Premise[]{});
    }

    public List<Action> getTchain() {
        return Tchain;
    }

    public List<Action> getFchain() {
        return Fchain;
    }


    // Simulation Methods


    // Evaluate rule premises.
    private boolean evalPremises(FieldsMap fMap,PropertiesMap pMap,
                                 long Time1, long Htime, double dsystem) throws ENException
    {
        boolean result=true;

        for(SimulationRule.Premise p : getPchain())
        {
            if (p.getLogop() == Rulewords.r_OR){
                if (!result)
                    result = p.checkPremise(fMap,pMap,Time1,Htime,dsystem);
            }
            else{
                if (!result)
                    return false;
                result = p.checkPremise(fMap,pMap,Time1,Htime,dsystem);
            }

        }
        return result;
    }

    // Adds rule's actions to action list
    private static void updateActionList(SimulationRule rule, List<ActItem> actionList, boolean branch){
        if(branch){ // go through the true action branch
            for(Action a : rule.getTchain()){
                if(!checkAction(rule,a,actionList)) // add a new action from the "true" chain
                    actionList.add(new ActItem(rule,a));
            }
        }
        else{
            for(Action a : rule.getFchain()){
                if(!checkAction(rule,a,actionList)) // add a new action from the "false" chain
                    actionList.add(new ActItem(rule,a));
            }
        }
    }

    // Checks if an action with the same link is already on the Action List
    private static boolean checkAction(SimulationRule rule,Action action, List<ActItem> actionList){

        for(ActItem item : actionList)
        {
            if(item.action.link == action.link){ // Action with same link
                if(rule.priority > item.rule.priority){ // Replace Actitem action with higher priority rule
                    item.rule = rule;
                    item.action = action;
                }

                return true;
            }
        }

        return false;
    }

    // Implements actions on action list, returns the number of actions executed.
    private static int takeActions(FieldsMap fMap, PropertiesMap pMap, Logger log,List<ActItem> actionList,
                                   long htime) throws ENException
    {
        double  tol = 1.e-3;
        int n = 0;

        for(ActItem item : actionList){
            if(item.action.execute(fMap,pMap,log,tol,htime))
                n++;
        }

        return n;
    }


    // Checks which rules should fire at current time.
    private static int check(FieldsMap fMap,PropertiesMap pMap, List<SimulationRule> rules,Logger log,
                             long Htime,long dt,double dsystem) throws ENException {
        // Start of rule evaluation time interval
        long Time1 = Htime - dt + 1;

        List<ActItem> actionList = new ArrayList<ActItem>();

        for(SimulationRule rule : rules)
            updateActionList(rule,actionList,rule.evalPremises(fMap,pMap,Time1,Htime,dsystem));

        return takeActions(fMap,pMap,log,actionList,Htime);
    }

    // updates next time step by checking if any rules will fire before then; also updates tank levels.
    public static Result minimumTimeStep(FieldsMap fMap,PropertiesMap pMap,Logger log,
                                         List<SimulationRule> rules,List<SimulationTank> tanks,
                                         long Htime,long tstep,double dsystem) throws ENException
    {
        long    tnow,   // Start of time interval for rule evaluation
                tmax,   // End of time interval for rule evaluation
                dt,     // Normal time increment for rule evaluation
                dt1;    // Actual time increment for rule evaluation

        // Find interval of time for rule evaluation
        tnow = Htime;
        tmax = tnow + tstep;

        //If no rules, then time increment equals current time step
        if (rules.size() == 0) {
            dt = tstep;
            dt1 = dt;
        }
        else{
            // Otherwise, time increment equals rule evaluation time step and
            // first actual increment equals time until next even multiple of
            // Rulestep occurs.
            dt = pMap.getRulestep();
            dt1 = pMap.getRulestep()  - (tnow % pMap.getRulestep());
        }

        // Make sure time increment is no larger than current time step
        dt = Math.min(dt, tstep);
        dt1 = Math.min(dt1, tstep);

        if (dt1 == 0)
            dt1 = dt;

        // Step through time, updating tank levels, until either
        // a rule fires or we reach the end of evaluation period.
        //
        // Note: we are updating the global simulation time (Htime)
        //       here because it is used by functions in RULES.C(this class)
        //       to evaluate rules when checkrules() is called.
        //       It is restored to its original value after the
        //       rule evaluation process is completed (see below).
        //       Also note that dt1 will equal dt after the first
        //       time increment is taken.

        do {
            Htime += dt1;                                       // Update simulation clock
            SimulationTank.stepWaterLevels(tanks, fMap, dt1);        // Find new tank levels
            if (check(fMap,pMap,rules,log,Htime,dt1,dsystem) != 0) break; // Stop if rules fire
            dt = Math.min(dt, tmax - Htime);                    // Update time increment
            dt1 = dt;                                           // Update actual increment
        }
        while (dt > 0);

        //Compute an updated simulation time step (*tstep)
        // and return simulation time to its original value
        tstep = Htime - tnow;
        Htime = tnow;

        return new Result(tstep,Htime);
    }

    public SimulationRule(Rule _rule, List<SimulationLink> links,List<SimulationNode> nodes)  throws ENException{
        label = _rule.getLabel();
        Pchain = new ArrayList<Premise>();
        Tchain = new ArrayList<Action>();
        Fchain = new ArrayList<Action>();

        Double tempPriority = 0.0;

        Rule.Rulewords ruleState = Rule.Rulewords.r_RULE;
        for(String _line : _rule.getCode().split("\n")){
            String [] tok = _line.split("[ \t]+");
            Rule.Rulewords key = Rule.Rulewords.parse(tok[0]);

            if(key == null)  throw new ENException(201);

            switch (key)
            {
                case r_IF:
                    if (!ruleState.equals(Rule.Rulewords.r_RULE))
                        throw new ENException(221);
                    ruleState = Rule.Rulewords.r_IF;
                    parsePremise(tok, Rule.Rulewords.r_AND,nodes,links);
                    break;

                case r_AND:
                    if (ruleState == Rule.Rulewords.r_IF)
                        parsePremise(tok, Rule.Rulewords.r_AND,nodes,links);
                    else if (ruleState == Rule.Rulewords.r_THEN || ruleState == Rule.Rulewords.r_ELSE)
                        parseAction(ruleState,tok,links);
                    else
                        throw new ENException(221);
                    break;

                case r_OR:
                    if (ruleState == Rule.Rulewords.r_IF)
                        parsePremise(tok, Rule.Rulewords.r_OR,nodes,links);
                    else
                        throw new ENException(221);
                    break;

                case r_THEN:
                    if (ruleState != Rule.Rulewords.r_IF)
                        throw new ENException (221);
                    ruleState = Rule.Rulewords.r_THEN;
                    parseAction(ruleState,tok,links);
                    break;

                case r_ELSE:
                    if (ruleState != Rule.Rulewords.r_THEN)
                        throw new ENException(221);
                    ruleState = Rule.Rulewords.r_ELSE;
                    parseAction(ruleState,tok,links);
                    break;

                case r_PRIORITY:
                {
                    if (!ruleState.equals(Rule.Rulewords.r_THEN) && !ruleState.equals(Rule.Rulewords.r_ELSE))
                        throw new ENException(221);

                    ruleState = Rule.Rulewords.r_PRIORITY;

                    if ( (tempPriority = Utilities.getDouble(tok[1]))==null)
                        throw new ENException(202);

                    break;
                }

                default:
                    throw new ENException(201);
            }
        }

        priority = tempPriority;
    }


    protected void parsePremise(String []Tok,Rule.Rulewords logop,List<SimulationNode> nodes, List<SimulationLink> links) throws ENException {
        Premise p = new Premise(Tok,logop,nodes, links);
        Pchain.add(p);

    }

    protected void parseAction(Rulewords state, String[] tok, List<SimulationLink> links) throws ENException {
        Action a = new Action(tok,links);

        if (state == Rulewords.r_THEN)
            Tchain.add(0,a);
        else
            Fchain.add(0, a);
    }

}
