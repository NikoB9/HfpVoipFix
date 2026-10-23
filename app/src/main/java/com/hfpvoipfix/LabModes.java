package com.hfpvoipfix;

/** One mode catalogue shared by UI and hooks. Never infer a codec from a HAL write. */
public final class LabModes {
    public static final String PROP="debug.hfpvoipfix.mode";
    public static final String[] IDS={"observe","voip","wbs","endpoints","bypass","bypass_comm","soft_rx","soft_tx","soft_duplex","soft_duplex_test","telecom_duplex","telecom_aa","projection_only","codec","rx","tx","bridge"};
    public static final String[] NAMES={"0 · Appels natifs","A · VoIP uniquement","B · VoIP + WBS confirmé","C · Ports SCO","D · Contournement Telecom","D1 · Contournement + communication","D2 · Réception logicielle validée","D3 · Essai micro logiciel","D4 · Duplex continu","D4 · Diagnostic en 4 phases","D5 · Relais local (base 1.9.0)","D6 · AA avec Telecom (ancien parcours)","D6 · AA isolé · appels depuis Samsung","E · Alignement codec HAL","G · AudioPatch réception","H · AudioPatch microphone","I · AudioPatch bidirectionnel"};
    public static final String[] DETAILS={
        "Aucune intervention audio. Appels gérés par Android. Journaux uniquement.",
        "Connexion HFP déclarée VoIP avant retour à Telecom ; aucun paramètre ni port ajouté. Risque modéré.",
        "A + bt_wbs=on, seulement si mAudioWbs=true et valeur HAL précédente lisible. Ne négocie pas le codec. Risque modéré.",
        "A + déclaration des ports SCO absents ; sans AudioPatch ni forçage WBS. Risque expérimental.",
        "Bloque handleCall du pont HFP vers Telecom si aucune connexion Telecom HFP préexistante. Appeler, répondre et raccrocher sur le Samsung. Audio natif conservé. Risque expérimental.",
        "D + MODE_IN_COMMUNICATION demandé dans Bluetooth après confirmation du contournement. Seulement depuis MODE_NORMAL ; demande libérée après appel et SCO, ou déconnexion. Sans patch, port ajouté ni écriture codec. Risque expérimental.",
        "Réception seule validée : capture SCO vers haut-parleur. Active temporairement BT_SCO et WBS négocié, puis restaure les deux paramètres. Le micro n’est pas transmis.",
        "Essai expérimental émission seule : microphone du Redmi vers sortie SCO du Samsung. Démarrage manuel, notification persistante, aucun enregistrement. Le correspondant doit confirmer qu’il entend la voix.",
        "Duplex continu jusqu’au raccrochage : son reçu et micro transmis. Préparation automatique, notification d’arrêt. Régler le volume Média du Redmi. Patch natif requis ; aucun enregistrement.",
        "Test progressif : quatre phases de 15 s, RX seul, ajout capture micro, ajout TX SCO, arrêt TX avec capture micro maintenue. Noter le son à chaque phase. Préparation automatique pendant la capture, notification persistante, arrêt à la fin de l’appel. Aucun enregistrement. Confirmer les deux sens audio.",
        "D5 expérimental : l’application Téléphone gère les appels du Samsung via le compte HFP Android ; le pont ajoute le duplex. Activer le compte Samsung dans les comptes d’appel et le sélectionner pour appeler. Patch natif requis. Audio local au Redmi, Android Auto non validé.",
        "D6 historique : appel publié dans Telecom et commande depuis Android Auto. Audio habitacle non validé.",
        "D6 isolé : audio vers Android Auto sans connexion Telecom locale. Appeler, répondre et raccrocher sur le Samsung. Ne pas lancer l’appel depuis l’autoradio. Micro Redmi ; aucun second SCO. Fonctionnement matériel à vérifier.",
        "A + bt_wbs aligné sur mAudioWbs (off si bande étroite, on si large bande). Ancienne valeur restaurée. Ne force pas CVSD ; peut être identique à B en WBS. Risque modéré.",
        "C + SCO_IN → haut-parleur uniquement. Aucun pont microphone. Risque élevé.",
        "C + microphone → SCO_OUT uniquement. Aucun pont réception. Risque élevé.",
        "C + les deux AudioPatch. Référence de pont ; ne force pas WBS. Retour micro possible. Risque élevé."
    };
    public static int index(String s){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(s))return i;return 0;}
    public static String valid(String s){return IDS[index(s)];}
    public static boolean bypass(String s){return s.equals("bypass")||s.equals("bypass_comm")||(softwareBridge(s)&&!telecom(s));}
    public static boolean voip(String s){return !s.equals("observe")&&!bypass(s);}
    public static boolean ports(String s){return softwareBridge(s)||s.equals("endpoints")||s.equals("rx")||s.equals("tx")||s.equals("bridge");}
    public static boolean softwareBridge(String s){return "soft_rx".equals(s)||"soft_tx".equals(s)||duplex(s);}
    public static boolean telecom(String s){return "telecom_duplex".equals(s)||"telecom_aa".equals(s);}
    public static boolean car(String s){return "telecom_aa".equals(s)||isolated(s);}
    public static boolean isolated(String s){return "projection_only".equals(s);}
    public static boolean duplex(String s){return "soft_duplex".equals(s)||"soft_duplex_test".equals(s)||telecom(s)||isolated(s);}
    public static boolean risky(String s){return ports(s)||bypass(s);}
}
