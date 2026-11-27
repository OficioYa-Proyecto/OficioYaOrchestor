package eci.edu.dosw.security;

/** Oculta datos personales antes de escribirlos en los logs (Ley 1581 de 2012). */
public final class LogMask {

    private LogMask() {
    }

    /** Ejemplo: ana@x.com -> a***@x.com */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return "(sin correo)";
        }
        int arroba = email.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(arroba);
    }
}