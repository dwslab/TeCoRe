package models;

public enum Reasoner {

    MLN(controllers.routes.MlnController.result().url()),
    PSL(controllers.routes.PslController.result().url());

    private final String route;

    private Reasoner(String route) {
        this.route = route;
    }

    public String getRoute() {
        return route;
    }

}
