package com.example.glowring;

public class Flags {

    // Each entry is "Name;colors" or "Name;colors;repeat".
    // colors = RRGGBB:width pairs in order around the ring (width defaults to 1).
    static final String[] DATA = {
            "Albania;E41E20:3,000000:1",
            "Andorra;0018A8,FEDF00,D50032",
            "Argentina;74ACDF,FFFFFF,74ACDF",
            "Armenia;D90012,0033A0,F2A800",
            "Australia;00247D:4,FFFFFF:1,E8202D:1,FFFFFF:1",
            "Austria;ED2939,FFFFFF,ED2939",
            "Azerbaijan;0092BC,ED2939,00AF66",
            "Bahamas;00778B,FFC72C,00778B",
            "Bangladesh;006A4E:6,F42A41:3",
            "Barbados;00267F,FFC726,00267F",
            "Belarus;C8313E:2,4AA657:1",
            "Belgium;000000,FDDA24,EF3340",
            "Bi;D60270:2,9B4F96:1,0038A8:2",
            "Bolivia;D52B1E,F9E300,007934",
            "Bosnia and Herzegovina;002395:3,FECB00:2,FFFFFF:1",
            "Brazil;00A859:4,FFDF00:3,002776:2,FFDF00:3",
            "Bulgaria;FFFFFF,00966E,D62612",
            "Cambodia;032EA1,E00025:2,032EA1",
            "Cameroon;007A5E,CE1126,FCD116",
            "Canada;E8202D,FFFFFF:2,E8202D",
            "Chad;002664,FECB00,C60C30",
            "Chile;0039A6,FFFFFF,D52B1E:2",
            "China;EE1C25:4,FFDE00:1",
            "Colombia;FCD116:2,003893,CE1126",
            "Costa Rica;002B7F,FFFFFF,CE1126:2,FFFFFF,002B7F",
            "Croatia;FF0000,FFFFFF,171796",
            "Cuba;CF142B:2,002A8F,FFFFFF,002A8F,FFFFFF,002A8F",
            "Czechia;FFFFFF,D7141A,11457E",
            "Denmark;C8102E:2,FFFFFF:1",
            "Ecuador;FFDD00:2,034EA2,ED1C24",
            "Egypt;CE1126,FFFFFF,000000",
            "El Salvador;0047AB,FFFFFF,0047AB",
            "Estonia;0072CE,000000,FFFFFF",
            "Ethiopia;078930,FCDD09,DA121A",
            "Finland;FFFFFF:2,003580:1",
            "Gabon;009E60,FCD116,3A75C4",
            "Georgia;FFFFFF:2,E8202D:1",
            "Germany;000000,DD0000,FFCE00",
            "Ghana;CE1126,FCD116,006B3F",
            "Greece;0D5EAF,FFFFFF;4",
            "Guatemala;4997D0,FFFFFF,4997D0",
            "Guinea;CE1126,FCD116,009460",
            "Honduras;0073CF,FFFFFF,0073CF",
            "Hungary;CE2939,FFFFFF,477050",
            "Iceland;02529C:2,FFFFFF:1,DC1E35:1,FFFFFF:1",
            "India;FF9933,FFFFFF,138808",
            "Indonesia;FF0000,FFFFFF",
            "Iran;239F40,FFFFFF,DA0000",
            "Iraq;CE1126,FFFFFF,000000",
            "Ireland;169B62,FFFFFF,FF883E",
            "Israel;0038B8,FFFFFF:3,0038B8",
            "Italy;009246,FFFFFF,CE2B37",
            "Ivory Coast;F77F00,FFFFFF,009E60",
            "Jamaica;009B3A:2,FED100,000000:2,FED100",
            "Japan;FFFFFF:7,E8202D:2",
            "Jordan;000000,FFFFFF,007A3D,CE1126:2",
            "Kazakhstan;00AFCA:5,FEC50C:1",
            "Kenya;000000,FFFFFF,BB0000,FFFFFF,006600",
            "Latvia;9E3039:2,FFFFFF:1,9E3039:2",
            "Lebanon;ED1C24:1,FFFFFF:2,00A651:1,FFFFFF:1,ED1C24:1",
            "Libya;E70013,000000:2,239E46",
            "Lithuania;FDB913,006A44,C1272D",
            "Luxembourg;EF3340,FFFFFF,00A3E0",
            "Malaysia;010066:2,CC0001,FFFFFF,CC0001,FFFFFF,CC0001,FFFFFF",
            "Mali;14B53A,FCD116,CE1126",
            "Mexico;006847,FFFFFF,CE1126",
            "Moldova;0046AE,FFD200,CC092F",
            "Mongolia;C4272F,015197,C4272F",
            "Morocco;C1272D:5,006233:1",
            "Myanmar;FECB00,34B233,EA2839",
            "Netherlands;AE1C28,FFFFFF,21468B",
            "New Zealand;00247D:4,CC142B:1,FFFFFF:1",
            "Nicaragua;0067C6,FFFFFF,0067C6",
            "Nigeria;008751,FFFFFF,008751",
            "North Macedonia;D20000:3,FFE600:1",
            "Norway;BA0C2F:2,FFFFFF:1,00205B:1,FFFFFF:1",
            "Pakistan;FFFFFF:1,01411C:4",
            "Palestine;000000,FFFFFF,009736,CE1126:2",
            "Pan;FF218C,FFD800,21B1FF",
            "Peru;D91023,FFFFFF,D91023",
            "Philippines;0038A8,CE1126,FCD116",
            "Poland;FFFFFF,DC143C",
            "Portugal;006600:2,FF0000:3,FFE000:1",
            "Rainbow;FF0000,FF8000,FFE000,00C000,0060FF,8000C0",
            "Romania;002B7F,FCD116,CE1126",
            "Saudi Arabia;006C35:6,FFFFFF:1",
            "Senegal;00853F,FDEF42,E31B23",
            "Serbia;E8202D,1E56D8,FFFFFF",
            "Sierra Leone;1EB53A,FFFFFF,0072C6",
            "Singapore;ED2939,FFFFFF",
            "Slovakia;FFFFFF,0B4EA2,EE1C25",
            "Slovenia;FFFFFF,005DA4,ED1C24",
            "Somalia;4189DD:6,FFFFFF:1",
            "South Africa;E03C31,FFFFFF,007749,FFB81C,000000,FFFFFF,002395",
            "South Korea;FFFFFF:5,CD2E3A:1,0047A0:1,000000:1",
            "Spain;AA151B,F1BF00:2,AA151B",
            "Sudan;D21034,FFFFFF,000000,007229:2",
            "Sweden;006AA7:2,FECC00:1,006AA7:1,FECC00:1",
            "Switzerland;DA291C:5,FFFFFF:1",
            "Syria;CE1126,FFFFFF,000000",
            "Thailand;A51931,F4F5F8,2D2A4A:2,F4F5F8,A51931",
            "Trans;5BCEFA,F5A9B8,FFFFFF,F5A9B8,5BCEFA",
            "Tunisia;E70013:5,FFFFFF:1",
            "Turkey;E30A17:5,FFFFFF:1",
            "Uganda;000000,FCDC04,DE3908,000000,FCDC04,DE3908",
            "Ukraine;0057B7,FFD700",
            "United Arab Emirates;00732F,FFFFFF,000000,FF0000:2",
            "United Kingdom;012169:3,FFFFFF:1,C8102E:1,FFFFFF:1",
            "United States;2B4BCC:3,FFFFFF:1,2B4BCC:1,FFFFFF:1,2B4BCC:1,FFFFFF:1,2B4BCC:3,"
                    + "E8202D:2,FFFFFF:2,E8202D:2,FFFFFF:2,E8202D:2,FFFFFF:2,E8202D:2,"
                    + "FFFFFF:2,E8202D:2,FFFFFF:2,E8202D:2,FFFFFF:2,E8202D:2",
            "Uruguay;FFFFFF,0038A8;4",
            "Venezuela;FFCC00,00247D,CF142B",
            "Vietnam;DA251D:5,FFFF00:1",
            "White;FFFFFF",
            "Yemen;CE1126,FFFFFF,000000",
            "Zambia;198A00:4,DE2010,000000,EF7D00",
            "Zimbabwe;006400,FFD200,D40000,000000,D40000,FFD200,006400"
    };

    static String name(String entry) {
        return entry.split(";")[0];
    }

    static String spec(String entry) {
        return entry.split(";")[1];
    }

    static int repeat(String entry) {
        String[] p = entry.split(";");
        if (p.length > 2) {
            try {
                return Math.max(1, Integer.parseInt(p[2].trim()));
            } catch (NumberFormatException e) {
                return 1;
            }
        }
        return 1;
    }

    static String byName(String name) {
        for (String e : DATA) {
            if (name(e).equals(name)) return e;
        }
        return null;
    }
}
