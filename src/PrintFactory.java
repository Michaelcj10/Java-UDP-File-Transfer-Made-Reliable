
class PrintFactory {

    public PrintFactory() {
    }

    public static void printCurrentStatistics(int totalTransferred, int previousSize, StartTime timer, double previousTimeElapsed) {
        printSpace();
        printSpace();
        printSeperator();
        System.out.println("Milestone Statistics");

        int sizeDifference = totalTransferred / 1000 - previousSize;
        double difference = timer.getTimeElapsed() - previousTimeElapsed;
        double throughput = totalTransferred / 1000 / timer.getTimeElapsed();


        System.out.println("We just receieved another: " + sizeDifference + "Kb");
        System.out.println("You have now receieved " + totalTransferred / 1000 + "Kb");
        System.out.println("Time taken so far: " + timer.getTimeElapsed() / 1000 + " Seconds");
        System.out.println("Throughput average so far :" + throughput + "Mbps");
        System.out.println("Throughput for last 50: " + sizeDifference / difference + "Mbps");

        printSeperator();
        printSpace();
        printSpace();

    }

    public static void printSpace() {
        System.out.println();
    }

    public static void printSeperator() {
        System.out.println("------------------------------");
    }
}
