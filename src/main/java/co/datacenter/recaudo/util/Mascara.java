package co.datacenter.recaudo.util;

public final class Mascara {

    private Mascara() {
    }

    public static String documento(String valor) {
        if (valor == null || valor.length() < 4) {
            return "***";
        }
        return "***" + valor.substring(valor.length() - 4);
    }
}
