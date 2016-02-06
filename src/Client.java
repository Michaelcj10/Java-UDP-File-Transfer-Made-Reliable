import java.io.*;
import java.net.*;
import java.util.Random;

/*
 * java Client <send rate> <host> <port> <local file> <call on server>
 * eg Java Client 99 localhost 9999 my_file.png my.png
 */
class Client {

    private static int totalTransferred = 0;
    private static final double previousTimeElapsed = 0;
    private static final int previousSize = 0;
    private static int sendRate = 0;
    private static String hostName;
    private static int port;
    private static String fileName;
    private static String destFileName;
    private static StartTime timer = null;
    private static int retransmitted = 0;

    public static void main(String args[]) throws Exception {

        sendRate = Integer.parseInt(args[0]);
        setLossRate(sendRate);
        hostName = args[1];
        setHostname(hostName);
        port = Integer.parseInt(args[2]);
        setPort(port);
        fileName = args[3];
        setFileName(fileName);
        destFileName = args[4];
        setDestFile(destFileName);
        setUp();
    }

    public static void setUp() throws IOException {

        System.out.println("Sending the file");
        DatagramSocket socket = new DatagramSocket();
        InetAddress address = InetAddress.getByName(getHostname());

        String saveFileAs = getDestFileName();
        byte[] saveFileAsData = saveFileAs.getBytes();

        DatagramPacket fileStatPacket = new DatagramPacket(saveFileAsData, saveFileAsData.length, address, getPort());
        socket.send(fileStatPacket);

        File file = new File(getFileName());
        // Create a byte array to store file
        //InputStream inFromFile = new FileInputStream(file);
        byte[] fileByteArray = new byte[(int) file.length()];

        startTimer();
        beginTransfer(socket, fileByteArray, address);
        String finalStatString = getFinalStatistics(fileByteArray, retransmitted);
        sendServerFinalStatistics(socket, address, finalStatString);
        closeSocket(socket);
    }

    private static void closeSocket(DatagramSocket socket) {
        socket.close();
    }

    private static void sendServerFinalStatistics(DatagramSocket socket, InetAddress address, String finalStatString) {
        byte[] bytesData;
        // convert string to bytes so we can send
        bytesData = finalStatString.getBytes();
        DatagramPacket statPacket = new DatagramPacket(bytesData,
                bytesData.length, address, getPort());
        try {
            socket.send(statPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void beginTransfer(DatagramSocket socket, byte[] fileByteArray, InetAddress address) throws IOException {

        int sequenceNumber = 0;
        boolean flag;
        int ackSequence = 0;

        for (int i = 0; i < fileByteArray.length; i = i + 1021) {
            sequenceNumber += 1;
            // Create message
            byte[] message = new byte[1024];
            message[0] = (byte) (sequenceNumber >> 8);
            message[1] = (byte) (sequenceNumber);

            if ((i + 1021) >= fileByteArray.length) {
                flag = true;
                message[2] = (byte) (1);
            } else {
                flag = false;
                message[2] = (byte) (0);
            }

            if (!flag) {
                System.arraycopy(fileByteArray, i, message, 3, 1021);
            } else { // If it is the last message
                System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i);
            }

            int randomInt = shouldThisPacketBeSent();

            DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, getPort());

            if (randomInt <= getLossRate()) {
                socket.send(sendPacket);
            }

            totalTransferred = gatherTotalDataSentSoFarStatistic(sendPacket);

            if (Math.round(totalTransferred / 1000) % 50 == 0) {
                PrintFactory.printCurrentStatistics(totalTransferred, previousSize, timer, previousTimeElapsed);
            }

            System.out.println("Sent: Sequence number = " + sequenceNumber);

            // For verifying the the packet
            boolean ackRec;

            // The acknowledgment is not correct
            while (true) {
                // Create another packet by setting a byte array and creating
                // data gram packet
                byte[] ack = new byte[2];
                DatagramPacket ackpack = new DatagramPacket(ack, ack.length);

                try {
                    // set the socket timeout for the packet acknowledgment
                    socket.setSoTimeout(50);
                    socket.receive(ackpack);
                    ackSequence = ((ack[0] & 0xff) << 8)
                            + (ack[1] & 0xff);
                    ackRec = true;

                }
                // we did not receive an ack
                catch (SocketTimeoutException e) {
                    System.out.println("Socket timed out waiting for the ");
                    ackRec = false;
                }

                // everything is ok so we can move on to next packet
                // Break if there is an acknowledgment next packet can be sent
                if ((ackSequence == sequenceNumber)
                        && (ackRec)) {
                    System.out.println("Ack received: Sequence Number = "
                            + ackSequence);
                    break;
                }

                // Re send the packet
                else {
                    socket.send(sendPacket);
                    System.out.println("Resending: Sequence Number = "
                            + sequenceNumber);
                    // Increment retransmission counter
                    retransmitted += 1;
                }
            }
        }
    }

    private static int gatherTotalDataSentSoFarStatistic(DatagramPacket sendPacket) {
        totalTransferred = sendPacket.getLength() + totalTransferred;
        totalTransferred = Math.round(totalTransferred);

        return totalTransferred;
    }

    private static int shouldThisPacketBeSent() {
        Random randomGenerator = new Random();

        return randomGenerator.nextInt(100);
    }

    private static String getFinalStatistics(byte[] fileByteArray, int retransmitted) {

        double fileSizeKB = (fileByteArray.length) / 1024;
        double transferTime = timer.getTimeElapsed() / 1000;
        double fileSizeMB = fileSizeKB / 1000;
        double throughput = fileSizeMB / transferTime;

        System.out.println("File " + getFileName() + " has been sent");
        PrintFactory.printSpace();
        PrintFactory.printSpace();
        System.out.println("Statistics of transfer");
        PrintFactory.printSeperator();
        System.out.println("File " + getFileName() + " has been sent successfully.");
        System.out.println("The size of the File was " + totalTransferred / 1000 + " KB");
        System.out.println("This is approx: " + totalTransferred / 1000 / 1000 + " MB");
        System.out.println("Time for transfer was " + timer.getTimeElapsed() / 1000 + " Seconds");
        System.out.printf("Throughput was %.2f MB Per Second\n", +throughput);
        System.out.println("Number of retransmissions: " + retransmitted);
        PrintFactory.printSeperator();

        return "File Size: " + fileSizeMB + "mb\n"
                + "Throughput: " + throughput + " Mbps"
                + "\nTotal transfer time: " + transferTime + " Seconds";
    }

    private static void startTimer() {
        timer = new StartTime();
    }

    private static int getLossRate() {
        return sendRate;
    }

    private static void setLossRate(int passed_loss_rate) {
        sendRate = passed_loss_rate;
    }

    private static int getPort() {
        return port;
    }

    private static void setPort(int passed_port) {
        port = passed_port;
    }

    private static String getFileName() {
        return fileName;
    }

    private static void setFileName(String passed_file_name) {
        fileName = passed_file_name;
    }

    private static void setDestFile(String passed_dest_file) {
        destFileName = passed_dest_file;
    }

    private static String getDestFileName() {
        return destFileName;
    }

    private static String getHostname() {
        return hostName;
    }

    private static void setHostname(String passed_host_name) {
        hostName = passed_host_name;
    }
}