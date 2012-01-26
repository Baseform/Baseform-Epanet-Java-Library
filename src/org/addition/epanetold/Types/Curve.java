package org.addition.epanetold.Types;

import java.util.List;

/* CURVE OBJECT */
public class Curve {

    String id;
    EnumVariables.CurveType type;
    DblList x = new DblList();
    DblList y = new DblList();

    public int getNpts() {
        if(x.size()!=y.size()){
            System.out.println("Different x,y vector sizes");
            return 0;
            //TODO:Tamanho das listas diferente.
        }
        return x.size();
    }
    public List<Double> getX(){
        return x;
    }

    public List<Double> getY(){
        return y;
    }

    public String getId() {
        return id;
    }

    public void setId(String Id) {
        this.id = Id;
    }
    
    public EnumVariables.CurveType getType() {
        return type;
    }


    public void setType(EnumVariables.CurveType Type) {
        this.type = Type;
    }
}
