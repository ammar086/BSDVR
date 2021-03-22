package app;

public class LinkFailureHandler implements Runnable{

    private Router router;
    private Integer neighbor;
    public LinkFailureHandler(Router r, Integer n){
        router = r;
        neighbor = n;
    }
    @Override
    public void run() {
        
    }
}
