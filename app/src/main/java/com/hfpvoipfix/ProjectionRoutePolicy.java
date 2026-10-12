package com.hfpvoipfix;

/** Selects Android Auto's active projection mix without depending on USB or Wi-Fi. */
final class ProjectionRoutePolicy {
    static boolean useCar(boolean gearheadActive,int remoteSubmixOutputs){return gearheadActive&&remoteSubmixOutputs>0;}
}
