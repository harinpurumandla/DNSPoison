package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.util.Scanner;
import java.util.StringTokenizer;

/*
 * BSD Licensed:
 * Copyright (c) 2010, Trent Z.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the DenisDNS project nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


/*
 * DenisDNS - extended by Jacob Saunders
 * Added command line arguments.
 * -s Set the DNS server this queries as an IPv4 addres. 
 * If omitted, it checks /etc/resolv.conf on a *NIX machine, otherwise it resorts to a hardcoded address in DnsServer.java
 * -p Port to run the DNS server on. If this is omitted, it defaults to port 53. If
 * this is blocked (due to account privileges, most likely, 
 * it instead runs on port 23456, and increments this by one until it finds one 
 * it can open.
 */

public class Denis {
	
	public static byte[] dnsSource = {(byte)130, (byte)108, (byte)128, (byte)200};
	public static int port = 53, fallbackPort = 23456;

	/**
	 * @param args
	 */
	public static void server() {
		// TODO Auto-generated method stub
		try {
			DatagramSocket socket = new DatagramSocket(5454);
			byte buffer[] = new byte[1024];
			DatagramPacket p = new DatagramPacket(buffer, buffer.length);
			
			System.out.println("going to receive");
			socket.receive(p);
			DnsRequest request = new DnsRequest(buffer, p.getLength());
			System.out.println("got " + p.getLength() + " bytes from " + p.getAddress());
			System.out.println("Request: " + request);
			
			// reply
			HostRecord hostRecord = new HostRecord(request.getQuestions().get(0).getDomainName());
			hostRecord.addIpAddress(new IPAddress(new byte[]{1,2,3,4}));
			hostRecord.addIpAddress(new IPAddress(new byte[]{5,6,7,8}));
			byte reply[] = DnsResponse.constructPacket(hostRecord, request.getTxnId());
			DatagramPacket replyPkt = new DatagramPacket(reply, reply.length);
			replyPkt.setPort(p.getPort());
			replyPkt.setAddress(p.getAddress());
			socket.send(replyPkt);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void client() {
		try {
			DatagramSocket socket = new DatagramSocket();
			byte buffer[] = DnsRequest.constructPacket(1234, 0x100, "baidu.com");
			DatagramPacket p = new DatagramPacket(buffer, buffer.length);
			p.setAddress(Inet4Address.getByAddress(new byte[] {(byte) 192, (byte) 168, (byte) 200, 10}));
			p.setPort(53);
			socket.send(p);
			// receive
			byte recvBuffer[] = new byte[1024];
			p = new DatagramPacket(recvBuffer, recvBuffer.length);
			System.out.println("Going to receive");
			socket.receive(p);
			DnsResponse response = new DnsResponse(recvBuffer, p.getLength());
			System.out.println("Response " + response);
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void testHttp() throws Exception {
		HttpResolver r = new HttpResolver();
		HostRecord record = r.addressForHost("baidu.com");
		System.out.println(record);
	}
	
	public static void main(String args[]) {
		//Get the DNS server settings.
		System.out.println("DenisDNS");
		getSettings(args);
		
		System.out.println("Querying DNS Server: " + ip4ByteToString(dnsSource));
		
		try {
			DnsServer s = new DnsServer(port);
			s.start();
			s.runShell();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main_x(String args[]) {
		try {
			testHttp();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Reads in the arguments and sets the DNS server to recurse to
	 * and the port to run this server on.
	 * If this fails to get an IP address, it will check /etc/resolv.conf
	 */
	public static void getSettings(String[] args)
	{
		boolean hasDNSip = false;
		
		//Loop through the arguments.
		for (int i = 0; i < args.length; i += 2)
		{
			if ((i + 1) >= args.length)
			{
				System.out.println("Extraneous parameter: " + args[i]);
			}
			if (args[i].equals("-s"))
			{
				//Read in the DNS server IP address.
				dnsSource = ip4StringToByte(args[i+1]);
				hasDNSip = true;
			}
			if (args[i].equals("-p"))
			{
				//Read in the port number.
				try {
					port = Integer.parseInt(args[i+1]);
				} catch (NumberFormatException e) {
					System.out.println("Invalid port number: " + args[i+1]);
					System.exit(-1);
				}
				
				if (port < 0 || port > 65535)
				{
					System.out.println("Port must be 0<=p<=65535.");
					System.exit(-1);
				}
			}
		}//Finish for loop.
		
		//If we don't have an address, check /etc/resolv.conf
		if (hasDNSip)
		{
			return;
		}
		File resolvFile = new File ("/etc/resolv.conf");
		if (!resolvFile.canRead())
		{
			System.out.println("Cannot read /etc/resolv.conf.");
			return;
		}
		
		Scanner resolvConf;
		try {
			resolvConf = new Scanner(resolvFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			System.out.println("Cannot read /etc/resolv.conf.");
			return;
		}
		
		//Loop through until you find the first server and set this to that.
		while (resolvConf.hasNextLine())
		{
			String line = resolvConf.nextLine();
			StringTokenizer lineTokens = new StringTokenizer(line, " ");
			
			//See if this is a nameserver line.
			if (lineTokens.countTokens() != 2 || 
					!lineTokens.nextToken().equalsIgnoreCase("nameserver"))
			{
				continue;
			}
			
			//Convert to a byte array, store, and break.
			dnsSource = ip4StringToByte(lineTokens.nextToken());
			System.out.println("Using nameserver from /etc/resolv.conf");
			break;
		}
		
		resolvConf.close();

	}
	
	/*
	 * This method converts an IPv4 address from a string representation
	 * to a byte array.
	 * ipAddress: The string representation of an IPv4 address.
	 */
	public static byte[] ip4StringToByte(String ipAddress)
	{
		byte[] ipByte = new byte[4];
		
		//Tokenize by .'s.
		StringTokenizer tokenizer = new StringTokenizer(ipAddress, ".");
		
		if (tokenizer.countTokens() != 4)
		{
			System.out.println("Invalid IP address: " + ipAddress);
			System.exit(-1);
		}
		
		//Parse the bytes.
		for (int i = 0; i < 4; i++)
		{
			String token = tokenizer.nextToken();
			try {
				int ipOctet = Integer.parseInt(token);
				ipByte[i] = (byte)ipOctet;
			} catch (NumberFormatException e) {
				System.out.println("Invalid IP address: " + ipAddress);
				System.exit(-1);
			}
		}
		
		
		return ipByte;
	}
	
	/*
	 * This method takes an IPv4 passed to it in byte[] format and returns
	 * a string representation of it.
	 * ipAddress: ipAddress in octets.
	 */
	public static String ip4ByteToString(byte[] ipAddress)
	{
		String ipString = "";
		
		for (int i = 0; i < ipAddress.length; i++)
		{
			//Convert to an unsigned int.
			int octet = ipAddress[i] & 0xFF;

			ipString += (octet);
			//Add a period if there's another octet remaining.
			if (i + 1 < ipAddress.length)
			{
				ipString += ".";
			}
		}
		
		return ipString;
	}
	
}
