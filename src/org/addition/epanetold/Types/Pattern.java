package org.addition.epanetold.Types;

import java.util.List;

/*
 Pattern > global pattern list
*/
/* TIME PATTERN OBJECT */
public class Pattern {
    private String id;
    private DblList factors = new DblList();

    public void setId(String text){
        id = text;
    }
    
    public String getId() {
        return id;
    }

    public void add(Double factor){
        factors.add(factor);    
    }

    public List<Double> getFactorsList(){
        return factors;
    }

    public Pattern() {
        this.id = "";
    }

}
