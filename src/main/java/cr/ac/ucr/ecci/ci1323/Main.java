package cr.ac.ucr.ecci.ci1323;

import cr.ac.ucr.ecci.ci1323.controller.SimulationController;

public class Main {

    public static void main(String[] args) {

        try {

            for (int i = 0; i < 30; i++) {
                SimulationController simulationController = new SimulationController(2);
                simulationController.runSimulation();
                Thread.sleep(300);
            }

            for (int i = 0; i < 30; i++) {
                SimulationController simulationController = new SimulationController(6);
                simulationController.runSimulation();
                Thread.sleep(300);
            }

            for (int i = 0; i < 30; i++) {
                SimulationController simulationController = new SimulationController(600);
                simulationController.runSimulation();
                Thread.sleep(300);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}
