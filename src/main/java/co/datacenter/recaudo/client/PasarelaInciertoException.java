package co.datacenter.recaudo.client;

public class PasarelaInciertoException extends RuntimeException {

    public PasarelaInciertoException(String mensaje) {
        super(mensaje);
    }

    public PasarelaInciertoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
