/*
 * Handy coordinate conversion utilities for working in dec degrees, DMS and DDM
 */
package uk.ac.antarctica.helpdesk.util;

public class CoordinateConversionUtils {
    
    public static final double RESOLUTION = 1e-05;
    
    public static Double toDecDegrees(String coord, boolean isLat) {        
        Double convertedCoord = null; 
        if (coord != null && !coord.isEmpty()) {
            try {
                /* Catch the case of decimal degrees already first */
                convertedCoord = Double.parseDouble(coord);
            } catch (NumberFormatException nfe) {
                try {         
                    char hh = 'X';
                    double dd = 0.0, mm = 0.0, ss = 0.0;
                    /* Find out where the hemisphere information is - assumed at the start or end */
                    coord = coord.trim().toUpperCase();
                    char c1 = coord.charAt(0), cn = coord.charAt(coord.length()-1);
                    if (c1 == 'N' || c1 == 'S' || c1 == 'E' || c1 == 'W') {
                        hh = c1;
                        coord = coord.substring(1);
                    } else if (cn == 'N' || cn == 'S' || cn == 'E' || cn == 'W') {
                        hh = cn;
                        coord = coord.substring(0, coord.length()-1);
                    }
                    if (hh != 'X') {
                        /* Replace all non-numerical stuff by spaces */
                        coord = coord.replaceAll("[^0-9.]{1,}", " ");
                        coord = coord.trim();                    
                        String[] parts = coord.split("[\\s]");
                        dd = Double.valueOf(parts[0]);
                        if (parts.length > 1) {
                            mm = Double.valueOf(parts[1]);
                        } 
                        if (parts.length > 2) {
                            ss = Double.valueOf(parts[2]);
                        }
                        if (validateCoordinate(dd, mm, ss, hh, isLat)) {
                            convertedCoord = (dd + mm / 60.0 + ss / 3600.0) * ((hh == 'S' || hh == 'W') ? -1.0 : 1.0);                       
                        }     
                    }
                } catch(NumberFormatException | NullPointerException nfe2) {            
                }
            }           
        }
        return(convertedCoord);
    }
    
    public static boolean validateCoordinate(double dd, double mm, double ss, char hh, boolean isLat) {
        double bdry = isLat ? 90.0 : 180.0; 
        boolean validh = isLat ? (hh == 'N' || hh == 'S') : (hh == 'E' || hh == 'W');
        return(
            ((dd > -bdry && dd < bdry) || withinResolution(dd, -bdry) || withinResolution(dd, bdry)) &&
            (mm >= 0.0 && mm <= 60.0) &&
            (ss >= 0.0 && ss <= 60.0) &&
            validh
        );
    }
       
    
    public static boolean withinResolution(double test, double value) {
        return(Math.abs(test - value) <= RESOLUTION);
    }
    
}
