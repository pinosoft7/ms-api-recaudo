package co.datacenter.recaudo.persistence;

public enum EstadoPago {
    APROBADO(1),
    RECHAZADO(2),
    PENDIENTE(3),
    POR_CONFIRMAR(4);

    private final int codigo;

    EstadoPago(int codigo) {
        this.codigo = codigo;
    }

    public int codigo() {
        return codigo;
    }

    public boolean abierto() {
        return this == PENDIENTE || this == POR_CONFIRMAR;
    }

    public static EstadoPago de(int codigo) {
        for (EstadoPago estado : values()) {
            if (estado.codigo == codigo) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de pago desconocido: " + codigo);
    }
}
