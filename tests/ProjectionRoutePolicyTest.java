package com.hfpvoipfix;
public final class ProjectionRoutePolicyTest {
    private static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        check(ProjectionRoutePolicy.useCar(true,2));
        check(!ProjectionRoutePolicy.useCar(false,2));
        check(!ProjectionRoutePolicy.useCar(true,0));
        System.out.println("PASS projection route requires Android Auto and an active remote mix");
    }
}
