import java.io.*;
import java.net.*;

class Server {

    private static int totalTransferred = 0;
    private static StartTime timer;
    private static String fileName = "";
    private static String decodedDataUsingUTF82 = null;

    public static void main(String args[]) throws Exception {

        System.out.println("Ready!");
        final int port = Integer.parseInt(args[0]);
        setUp(port);
    }

    public static void setUp(int port) throws IOException {

        DatagramSocket socket = new DatagramSocket(port);

        byte[] receiveFileNameChoice = new byte[1024];
        DatagramPacket receiveFileNameChoicePacket = new DatagramPacket(
                receiveFileNameChoice, receiveFileNameChoice.length);
        socket.receive(receiveFileNameChoicePacket);

        try {
            decodedDataUsingUTF82 = new String(receiveFileNameChoice, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String savedFileName = decodedDataUsingUTF82.trim();
        fileName = savedFileName;
        setFileName(savedFileName);
        File file = new File(fileName);
        FileOutputStream outToFile = new FileOutputStream(file);

        acceptTransferOfFile(outToFile, socket);

        byte[] finalStatData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(finalStatData,
                finalStatData.length);
        socket.receive(receivePacket);
        printFinalStatistics(finalStatData);
    }

    private static void printFinalStatistics(byte[] finalStatData) {
        try {
            String decodedDataUsingUTF8 = new String(finalStatData, "UTF-8");
            PrintFactory.printSpace();
            PrintFactory.printSpace();
            System.out.println("Statistics of transfer");
            PrintFactory.printSeperator();
            System.out.println("File has been saved as: " + getFileName());
            System.out.println("Statistics of transfer");
            System.out.println("" + decodedDataUsingUTF8.trim());
            PrintFactory.printSeperator();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static void acceptTransferOfFile(FileOutputStream outToFile, DatagramSocket socket) throws IOException {

        // last message flag
        boolean flag;
        int sequenceNumber = 0;
        int findLast = 0;

        while (true) {
            byte[] message = new byte[1024];
            byte[] fileByteArray = new byte[1021];

            // Receive packet and retrieve message
            DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
            socket.setSoTimeout(0);
            socket.receive(receivedPacket);

            message = receivedPacket.getData();
            totalTransferred = receivedPacket.getLength() + totalTransferred;
            totalTransferred = Math.round(totalTransferred);

            // start the timer at the point transfer begins
            if (sequenceNumber == 0) {
                timer = new StartTime();
            }

            if (Math.round(totalTransferred / 1000) % 50 == 0) {
                double previousTimeElapsed = 0;
                int previousSize = 0;
                PrintFactory.printCurrentStatistics(totalTransferred, previousSize,
                        timer, previousTimeElapsed);
            }
            // Get port and address for sending acknowledgment
            InetAddress address = receivedPacket.getAddress();
            int port = receivedPacket.getPort();

            // Retrieve sequence number
            sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);
            // Retrieve the last message flag
            // a returned value of true means we have a problem
            flag = (message[2] & 0xff) == 1;
            // if sequence number is the last one +1, then it is correct
            // we get the data from the message and write the message
            // that it has been received correctly
            if (sequenceNumber == (findLast + 1)) {

                // set the last sequence number to be the one we just received
                findLast = sequenceNumber;

                // Retrieve data from message
                System.arraycopy(message, 3, fileByteArray, 0, 1021);

                // Write the message to the file and print received message
                outToFile.write(fileByteArray);
                System.out.println("Received: Sequence number:"
                        + findLast);

                // Send acknowledgement
                sendAck(findLast, socket, address, port);
            } else {
                System.out.println("Expected sequence number: "
                        + (findLast + 1) + " but received "
                        + sequenceNumber + ". DISCARDING");
                // Re send the acknowledgement
                sendAck(findLast, socket, address, port);
            }

            // Check for last message
            if (flag) {
                outToFile.close();
                break;
            }
        }
    }

    private static String getFileName() {
        return fileName;
    }

    private static void setFileName(String passed_file_name) {
        fileName = passed_file_name;
    }

    private static void sendAck(int findLast, DatagramSocket socket, InetAddress address, int port) throws IOException {
        // send acknowledgement
        byte[] ackPacket = new byte[2];
        ackPacket[0] = (byte) (findLast >> 8);
        ackPacket[1] = (byte) (findLast);
        // the datagram packet to be sent
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket,
                ackPacket.length, address, port);
        socket.send(acknowledgement);
        System.out.println("Sent ack: Sequence Number = " + findLast);
    }
}