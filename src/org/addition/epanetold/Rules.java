package org.addition.epanetold;

import java.util.ArrayList;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.addition.epanetold.Types.EnumVariables.*;

public strictfp class Rules {
    
    static final ResourceBundle errorBundle = PropertyResourceBundle.getBundle("Error");

    // Rules tokens
    private static String w_RULE     ="RULE";
    private static String w_IF       ="IF";
    private static String w_AND      ="AND";
    private static String w_OR       ="OR";
    private static String w_THEN     ="THEN";
    private static String w_ELSE     ="ELSE";
    private static String w_PRIORITY ="PRIO";
    private static String w_DEMAND   ="DEMA";
    private static String w_PRESSURE ="PRES";
    private static String w_FLOW     ="FLOW";
    private static String w_SETTING  ="SETT";
    private static String w_CLOCKTIME="CLOCKTIME";
    private static String w_FILLTIME ="FILL";
    private static String w_DRAINTIME="DRAI";
    private static String w_GRADE    ="GRADE";
    private static String w_LEVEL    ="LEVEL";
    private static String w_HEAD     ="HEAD";
    private static String w_POWER    ="POWE";
    private static String w_STATUS   ="STATUS";
    private static String w_TIME     ="TIME";
    private static String w_SYSTEM   ="SYST";
    private static String w_JUNC     ="Junc";
    private static String w_RESERV   ="Reser";
    private static String w_TANK     ="Tank";
    private static String w_PIPE     ="Pipe";
    private static String w_PUMP     ="Pump";
    private static String w_VALVE    ="Valve";
    private static String w_NODE     ="NODE";
    private static String w_LINK     ="LINK";
    private static String w_IS       ="IS";
    private static String w_NOT      ="NOT";
    private static String w_OPEN     ="OPEN";
    private static String w_CLOSED   ="CLOSED";
    private static String w_ACTIVE   ="ACTIVE";
    private static String w_ABOVE    ="ABOVE";
    private static String w_BELOW    ="BELOW";

    private static String t_RULE     ="Rule";
    private static String t_RULES_SECT ="[RULES] section";

    private enum        Rulewords {r_RULE,r_IF,r_AND,r_OR,r_THEN,r_ELSE,r_PRIORITY,r_ERROR};
    private String[]    Ruleword  ={w_RULE,w_IF,w_AND,w_OR,w_THEN,w_ELSE,w_PRIORITY};

    private enum        Varwords  {r_DEMAND, r_HEAD, r_GRADE, r_LEVEL, r_PRESSURE,r_FLOW, r_STATUS, r_SETTING,
        r_POWER, r_TIME,r_CLOCKTIME, r_FILLTIME, r_DRAINTIME};
    private String[]    Varword   ={w_DEMAND, w_HEAD, w_GRADE, w_LEVEL, w_PRESSURE,w_FLOW, w_STATUS, w_SETTING,
            w_POWER,w_TIME,w_CLOCKTIME,w_FILLTIME,w_DRAINTIME};

    private enum        Objects   {r_JUNC,r_RESERV,r_TANK,r_PIPE,r_PUMP,r_VALVE,r_NODE,r_LINK,r_SYSTEM};
    private String[]    Object    ={w_JUNC,w_RESERV,w_TANK,w_PIPE,w_PUMP,w_VALVE,w_NODE,w_LINK,w_SYSTEM};

    private enum        Operators { EQ, NE,  LE,  GE,  LT, GT, IS,  NOT,  BELOW,  ABOVE};
    private String[]    Operator  ={"=","<>","<=",">=","<",">",w_IS,w_NOT,w_BELOW,w_ABOVE};

    private enum        Values    {IS_NUMBER,IS_OPEN,IS_CLOSED,IS_ACTIVE};
    private String[]    Value     = {"XXXX",   w_OPEN, w_CLOSED, w_ACTIVE};

    Epanet epanet;
    Network net; // Reference to network structure.
    Hydraulic hyd; // Hydraulic simulation support.
    InputReader reader; // Input reader.
    Report report;

    // Rule Premise Clause
    private class Premise
    {
        Rulewords   logop;
        Objects     object;
        int         index;
        Varwords    variable;
        Operators   relop;
        Values      status;
        double      value;
    }

    // Rule Action Clause 
    private class Action{
        int    link;
        Values status;
        double setting;
    }

    // Control Rule Structure
    private class aRule{
        String          label;
        double          priority;
        List<Premise>   Pchain;
        List<Action>    Tchain;
        List<Action>    Fchain;
    }

    // Action list item
    private class ActItem{
        int             ruleindex;
        Action          action;
    }

    private aRule[]         Rule;       // Array of rules
    private List<ActItem>   ActList;    // Linked list of action items
    private Rulewords       RuleState;  // State of rule interpreter
    private long            Time1;      // Start of rule evaluation time interval (sec)
    //private Premise         Plast;      // Previous premise clause
    private int             MaxRules;
    private int             Nrules;     // Rules counter.

    int getNRules(){
        return Nrules;
    }
    Rules(Epanet epanet){
        ActList = new ArrayList<ActItem>();
        this.epanet = epanet;
    }

    void loadDependencies()
    {
        net = epanet.getNetwork();
        hyd = epanet.getHydraulicsSolver();
        reader = epanet.getInputReader();
        report = epanet.getReport();
    }

    //Initializes rule base.
    void initrules(){
        Nrules = 0;
        RuleState = Rulewords.r_PRIORITY;
        Rule = null;
    }

    // Updates rule count if RULE keyword found in line of input.
    void addrule(String tok){
        if (Utilities.match(tok,w_RULE)) MaxRules++;
    }

    // Allocates memory for rule-based controls.
    void  allocrules(){
        Rule = new aRule[MaxRules+1];
    }

    // Checks which rules should fire at current time.
    int checkrules(long dt)
    {
        int i,r;

        Time1 = hyd.Htime - dt + 1;

        ActList.clear();
        r = 0;
        for (i=1; i<=Nrules; i++)
        {

            if (evalpremises(i) == true)
                updateactlist(i,Rule[i].Tchain);
            else
            {
                if (Rule[i].Fchain.size()>0)
                    updateactlist(i,Rule[i].Fchain);
            }
        }

        if (ActList.size()>0) r = takeactions();
        clearactlist();
        return(r);
    }

    // Parses a line from [RULES] section of input.
    int  parseRule(String [] Tok)
    {
        int key;
        int err = 0;

        if (RuleState == Rulewords.r_ERROR) return(0);

        key = Utilities.findMatch(Tok[0],Ruleword);

        if(key != -1)
            switch (Rulewords.values()[key])
            {
                case r_RULE:
                    Nrules++;
                    newrule(Tok);
                    RuleState = Rulewords.r_RULE;
                    break;
                case r_IF:   if (!RuleState.equals(Rulewords.r_RULE))
                {
                    err = 221;
                    break;
                }
                    RuleState = Rulewords.r_IF;
                    err = newpremise(Tok,Rulewords.r_AND);
                    break;
                case r_AND:  if (RuleState == Rulewords.r_IF) err = newpremise(Tok,Rulewords.r_AND);
                else if (RuleState == Rulewords.r_THEN || RuleState == Rulewords.r_ELSE)
                    err = newaction(Tok);
                else err = 221;
                    break;
                case r_OR:   if (RuleState == Rulewords.r_IF) err = newpremise(Tok,Rulewords.r_OR);
                else err = 221;
                    break;
                case r_THEN: if (RuleState != Rulewords.r_IF)
                {
                    err = 221;
                    break;
                }
                    RuleState = Rulewords.r_THEN;
                    err = newaction(Tok);
                    break;
                case r_ELSE: if (RuleState != Rulewords.r_THEN)
                {
                    err = 221;
                    break;
                }
                    RuleState = Rulewords.r_ELSE;
                    err = newaction(Tok);
                    break;
                case r_PRIORITY: if (!RuleState.equals(Rulewords.r_THEN) && !RuleState.equals(Rulewords.r_ELSE))
                {
                    err = 221;
                    break;
                }
                    RuleState = Rulewords.r_PRIORITY;
                    err = newpriority(Tok);
                    break;
                default:
                    err = 201;
            }
        else
            err = 201;

        if (err>0)
        {
            RuleState = Rulewords.r_ERROR;
            ruleerrmsg(Tok,err);
            err = 200;
        }

        return(err);
    }

    // Clears memory used for action list
    void  clearactlist()
    {
        /*struct ActItem *a;
        struct ActItem *anext;
        a = ActList;
        while (a != NULL)
        {
            anext = a->next;
            free(a);
            a = anext;
        }*/
    }

    // Clears memory used for premises & actions for all rules
    void  clearrules()
    {
        /*struct Premise *p;
        struct Premise *pnext;
        struct Action  *a;
        struct Action  *anext;
        int i;
        for (i=1; i<=Nrules; i++)
        {
            p = Rule[i].Pchain;
            while (p != NULL)
            {
                pnext = p->next;
                free(p);
                p = pnext;
            }
            a = Rule[i].Tchain;
            while (a != NULL)
            {
                anext = a->next;
                free(a);
                a = anext;
            }
            a = Rule[i].Fchain;
            while (a != NULL)
            {
                anext = a->next;
                free(a);
                a = anext;
            }
        }*/
    }

    // Adds new rule to rule base
    void  newrule(String [] Tok)
    {
        Rule[Nrules] = new aRule();
        Rule[Nrules].label = Tok[1];
        Rule[Nrules].priority = 0.0;
        Rule[Nrules].Pchain = new ArrayList<Premise>();
        Rule[Nrules].Fchain = new ArrayList<Action>();
        Rule[Nrules].Tchain = new ArrayList<Action>();   
        //Plast = null;
    }

    // Adds new premise to current rule.
    int  newpremise(String []Tok,Rulewords logop)
    {
        int Ntokens = Tok.length;
        Objects i,k;
        Varwords v;
        Operators r;
        int j,m;
        Values s;
        Double x;
        Premise p;

        if (Ntokens != 5 && Ntokens != 6) return(201);

        int i_id = Utilities.findMatch(Tok[1],Object);
        i = i_id == -1 ? null : Objects.values()[i_id];

        if (i == Objects.r_SYSTEM)
        {
            j = 0;
            int v_id = Utilities.findMatch(Tok[2],Varword);
            v = v_id == -1 ? null : Varwords.values()[v_id];
            if (v != Varwords.r_DEMAND && v != Varwords.r_TIME && v != Varwords.r_CLOCKTIME) return(201);
        }
        else
        {
            int v_id = Utilities.findMatch(Tok[3],Varword);
            v = v_id == -1 ? null : Varwords.values()[v_id];
            if (v_id < 0) return(201);
            switch (i)
            {
                case r_NODE:
                case r_JUNC:
                case r_RESERV:
                case r_TANK:   k = Objects.r_NODE; break;
                case r_LINK:
                case r_PIPE:
                case r_PUMP:
                case r_VALVE:  k = Objects.r_LINK; break;
                default: return(201);
            }
            i = k;
            if (i == Objects.r_NODE)
            {
                j = reader.findNode(Tok[2]);
                if (j == 0) return(203);
                switch (v)
                {
                    case r_DEMAND:
                    case r_HEAD:
                    case r_GRADE:
                    case r_LEVEL:
                    case r_PRESSURE: break;

                    case r_FILLTIME:
                    case r_DRAINTIME: if (j <= net.getSections(SectType._JUNCTIONS)) return(201); break;

                    default: return(201);
                }
            }
            else
            {
                j = reader.findLink(Tok[2]);
                if (j == 0) return(204);
                switch (v)
                {
                    case r_FLOW:
                    case r_STATUS:
                    case r_SETTING: break;
                    default: return(201);
                }
            }
        }

        if (i == Objects.r_SYSTEM)
            m = 3;
        else
            m = 4;

        int op_id=Utilities.findMatch(Tok[m],Operator);
        Operators o = op_id==-1?null:Operators.values()[op_id];
        if (op_id < 0) return(201);
        switch(o)
        {
            case IS:    r = Operators.EQ; break;
            case NOT:   r = Operators.NE; break;
            case BELOW: r = Operators.LT; break;
            case ABOVE: r = Operators.GT; break;
            default:    r = o;
        }

        s = Values.IS_NUMBER;
        x = Constants.MISSING;
        if (v == Varwords.r_TIME || v == Varwords.r_CLOCKTIME)
        {
            if (Ntokens == 6)
                x = Utilities.hour(Tok[4],Tok[5])*3600.;
            else
                x = Utilities.hour(Tok[4],"")*3600.;
            if (x < 0.0) return(202);
        }
        else{
            int s_sid=-1;
            if ((s_sid = Utilities.findMatch(Tok[Ntokens-1],Value)) > Values.IS_NUMBER.ordinal())
                s = Values.values()[ s_sid ];
            else{
                if ((x = Utilities.getFloat(Tok[Ntokens-1]))==null)
                    return(202);
                if (v == Varwords.r_FILLTIME || v == Varwords.r_DRAINTIME)
                    x = x*3600.0;
            }
        }


        p = new Premise();

        p.object = i;
        p.index =  j;
        p.variable = v;
        p.relop = r;
        p.logop = logop;
        p.status   = s;
        p.value    = x;

        Rule[Nrules].Pchain.add(0,p);

        return(0);
    }

    // Adds new action to current rule.
    int  newaction(String [] Tok)
    {
        int Ntokens = Tok.length;
        int   j;
        Values s,k;
        Double x;


        if (Ntokens != 6) return(201);

        j = reader.findLink(Tok[2]);
        if (j == 0) return(204);

        if (net.getLink(j).getType() == LinkType.CV) return(207);

        s = null;
        x = Constants.MISSING;
        int k_id = Utilities.findMatch(Tok[5],Value);
        k = (k_id==-1)?null:Values.values()[k_id];
        
        if (k!=null && k.ordinal() > Values.IS_NUMBER.ordinal())
            s = k;
        else
        {
            if ( (x = Utilities.getFloat(Tok[5]))==null ) return(202);
            if (x < 0.0) return(202);
        }

        if (x != Constants.MISSING && net.getLink(j).getType() == LinkType.GPV) return(202);


        if (x != Constants.MISSING && net.getLink(j).getType() == LinkType.PIPE){
            if (x == 0.0)
                s = Values.IS_CLOSED;
            else
                s = Values.IS_OPEN;
            x = Constants.MISSING;
        }

        Action a = new Action();
        a.link = j;
        a.status = s;
        a.setting = x;

        if (RuleState == Rulewords.r_THEN)
            Rule[Nrules].Tchain.add(0,a);
        else
            Rule[Nrules].Fchain.add(0,a);

        return(0);
    }

    // Adds priority rating to current rule
    int  newpriority(String [] Tok)
    {
        Double x;
        if ( (x = Utilities.getFloat(Tok[1]))==null) return(202);
        Rule[Nrules].priority = x;
        return(0);
    }

    // Checks if premises to rule i are true
    boolean  evalpremises(int i)
    {
        boolean result=true;


        for(Premise p : Rule[i].Pchain)
        {
            if (p.logop == Rulewords.r_OR)
            {
                if (result == false)
                {
                    result = checkpremise(p);
                }
            }
            else
            {
                if (result == false) return false;
                result = checkpremise(p);
            }

        }
        return result;
    }

    // Checks if a particular premise is true
    boolean  checkpremise(Premise p)
    {
        if (p.variable == Varwords.r_TIME || p.variable ==  Varwords.r_CLOCKTIME)
            return(checktime(p));
        else if (p.status.ordinal() > Values.IS_NUMBER.ordinal())
            return(checkstatus(p));
        else
            return(checkvalue(p));
    }

    // Checks if condition on system time holds
    boolean  checktime(Premise p)
    {
        boolean  flag;
        long  t1,t2,x;

        if (p.variable == Varwords.r_TIME)
        {
            t1 = Time1;
            t2 = hyd.Htime;
        }
        else if (p.variable == Varwords.r_CLOCKTIME)
        {
            t1 = (Time1 + net.Tstart) % Constants.SECperDAY;
            t2 = (hyd.Htime + net.Tstart) % Constants.SECperDAY;
        }
        else
            return false;

        x = (long)(p.value);
        switch (p.relop)
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
                if (p.relop == Operators.EQ && flag == false) return true;
                if (p.relop == Operators.NE && flag == true)  return true;
                break;
        }

        return true;
    }

    // Checks if condition on link status holds
    boolean  checkstatus(Premise p)
    {
        StatType i;
        Values  j;
        switch (p.status)
        {
            case IS_OPEN:
            case IS_CLOSED:
            case IS_ACTIVE:
                i = hyd.S[p.index];
                if      (i.ordinal() <= StatType.CLOSED.ordinal()) j = Values.IS_CLOSED;
                else if (i == StatType.ACTIVE) j = Values.IS_ACTIVE;
                else                  j = Values.IS_OPEN;
                if (j == p.status &&
                        p.relop == Operators.EQ)
                    return true;
                if (j != p.status &&
                        p.relop == Operators.NE)
                    return true;
        }
        return false;
    }

    // Checks if numerical condition on a variable is true.
    boolean  checkvalue(Premise p)
    {
        int   i,j;
        Varwords v;
        double x,
                tol = 1.e-3;

        i = p.index;
        v = p.variable;
        switch (v)
        {
            case r_DEMAND: if (p.object == Objects.r_SYSTEM) x = hyd.Dsystem*net.getUcf(FieldType.DEMAND);
            else x = hyd.D[i]*net.getUcf(FieldType.DEMAND);
                break;

            case r_HEAD:
            case r_GRADE:     x = hyd.H[i]*net.getUcf(FieldType.HEAD);
                break;
            case r_PRESSURE:  x = (hyd.H[i]-net.getNode(i).getElevation())*net.getUcf(FieldType.PRESSURE);
                break;
            case r_LEVEL:     x = (hyd.H[i]-net.getNode(i).getElevation())*net.getUcf(FieldType.HEAD);
                break;
            case r_FLOW:      x = Math.abs(hyd.Q[i])*net.getUcf(FieldType.FLOW);
                break;
            case r_SETTING:   if (hyd.K[i] == Constants.MISSING) return false;
                x = hyd.K[i];
                switch (net.getLink(i).getType())
                {
                    case PRV:
                    case PSV:
                    case PBV:  x = x*net.getUcf(FieldType.PRESSURE); break;
                    case FCV:  x = x*net.getUcf(FieldType.FLOW);     break;
                }
                break;
            case r_FILLTIME:  if (i <= net.getSections(SectType._JUNCTIONS)) return false;
                j = i-net.getSections(SectType._JUNCTIONS);
                if (net.getTank(j).getArea() == 0.0)return false;
                if (hyd.D[i] <= Constants.TINY) return false;
                x = (net.getTank(j).getVmax() - net.getTank(j).getVolume())/hyd.D[i];
                break;
            case r_DRAINTIME: if (i <= net.getSections(SectType._JUNCTIONS)) return false;
                j = i-net.getSections(SectType._JUNCTIONS);
                if (net.getTank(j).getArea() == 0.0) return false;
                if (hyd.D[i] >= -Constants.TINY) return false;
                x = (net.getTank(j).getVmin() - net.getTank(j).getVolume())/hyd.D[i];
                break;
            default:
                return false;
        }
        switch (p.relop)
        {
            case EQ:        if (Math.abs(x - p.value) > tol) return false;
                break;
            case NE:        if (Math.abs(x - p.value) < tol) return false;
                break;
            case LT:        if (x > p.value + tol) return false;
                break;
            case LE:        if (x > p.value - tol) return false;
                break;
            case GT:        if (x < p.value - tol) return false;
                break;
            case GE:        if (x < p.value + tol) return false;
                break;
        }
        return true;
    }

    // Adds rule's actions to action list
    void  updateactlist(int i, List<Action> actions)
    {
        for(Action a : actions)
        {
            if (!checkaction(i,a))
            {
                ActItem item = new ActItem();
                item.action=a;
                item.ruleindex = i;
                ActList.add(0,item);
            }

        }


    }

    //Checks if an action is already on the Action List
    boolean  checkaction(int i, Action a)
    {
        int i1,k,k1;

        k = a.link;

        for(ActItem item : ActList)
        {
            Action a1 = item.action;
            i1 = item.ruleindex;
            k1 = a1.link;

            if (k1 == k)
            {
                if (Rule[i].priority > Rule[i1].priority)
                {
                    item.action = a;
                    item.ruleindex = i;
                }
                return true;
            }
        }
        return false;
    }

    // Implements actions on action list
    int  takeactions()
    {
        //Action a;
        //ActItem item;
        boolean   flag;
        int    k, n;
        StatType s;
        double  tol = 1.e-3,
                v, x;

        n = 0;
        //item = ActList;
        //while (item != null)
        for(ActItem item : ActList)
        {
            flag = false;
            Action a = item.action;
            k = a.link;
            s = hyd.S[k];
            v = hyd.K[k];
            x = a.setting;

            if (a.status == Values.IS_OPEN && s.ordinal() <= StatType.CLOSED.ordinal())
            {
                hyd.setlinkstatus(k, 1);
                flag = true;
            }

            else if (a.status == Values.IS_CLOSED && s.ordinal() > StatType.CLOSED.ordinal())
            {
                hyd.setlinkstatus(k, 0);
                flag = true;
            }

            else if (x != Constants.MISSING)
            {
                switch(net.getLink(k).getType())
                {
                    case PRV:
                    case PSV:
                    case PBV:    x = x/net.getUcf(FieldType.PRESSURE);  break;
                    case FCV:    x = x/net.getUcf(FieldType.FLOW);      break;
                }
                if (Math.abs(x-v) > tol)
                {
                    hyd.setlinksetting(k, x);
                    //TODO ver isto!
                    flag = true;
                }
            }

            if (flag == true)
            {
                n++;
                if (net.Statflag!=null)
                    report.writeruleaction(k,Rule[item.ruleindex].label);
                    // TODO REPORT
            }
        }
        
        return(n);
    }

    // Reports error message
    void  ruleerrmsg(String []Tok,int err)
    {
        int    i;
        String label;
        String fmt = errorBundle.getString("R_ERR"+err);
        String Msg;

        if (Nrules > 0)
            label = t_RULE +" "+Rule[Nrules].label;
        else
            label = t_RULES_SECT;

        Msg = fmt + label + ":";

        report.writeline(Msg);

        fmt = Tok[0];
        for (i=1; i<Tok.length; i++){
            fmt+=" " + Tok[i];
        }

        report.writeline(fmt);

    }



}
