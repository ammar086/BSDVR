package app;

public class Vector{
    private Integer cost;
    private Integer state; // {0: inactive, 1: active}
    public Vector(Integer c, Integer s){
        cost = c;
        state = s;
    }
    Vector(Vector v){
        cost = v.cost;
        state = v.state;
    }
    //getters
    public Integer getCost(){return cost;}
    public Integer getState(){return state;}
    //setters
    public void setCost(Integer c){cost = c;}
    public void setState(Integer s){state = s;} 
    //methods
    @Override 
    public String toString(){
        String co = "";
        if(cost == Integer.MAX_VALUE){co = "Inf";}else{co = Integer.toString(cost);}
        return "{cost: "+co+", state: "+Integer.toString(state)+"}";}
}