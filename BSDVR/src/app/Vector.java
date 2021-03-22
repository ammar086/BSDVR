package app;

public class Vector{
    private Integer cost;
    private Integer state; // {0: inactive, 1: active}
    public Vector(Integer c, Integer s){
        this.cost = c;
        this.state = s;
    }
    Vector(Vector v){
        cost = v.cost;
        state = v.state;
    }
    //getters
    public Integer getCost(){return this.cost;}
    public Integer getState(){return this.state;}
    //setters
    public void setCost(Integer c){this.cost = c;}
    public void setState(Integer s){this.state = s;} 
    //methods
    @Override 
    public String toString(){
        String co = "";
        if(this.cost == Integer.MAX_VALUE){co = "Inf";}else{co = Integer.toString(this.cost);}
        return "{cost: "+co+", state: "+Integer.toString(this.state)+"}";}
}