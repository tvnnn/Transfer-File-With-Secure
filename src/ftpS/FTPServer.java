package ftpS;

import java.net.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.DefaultListModel;

public class FTPServer
{
    public static void main(String args[]) throws Exception
    {
        int PORT = 21112;
        ServerSocket socfd = new ServerSocket(PORT);
        System.out.println("FTP Server Started on Port Number " + String.valueOf(PORT));
        while(true)
        {
            System.out.println("Waiting for Connection ...");
            ftp t=new ftp(socfd.accept());
            PrintWriter out = new PrintWriter(t.ClientSoc.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(t.ClientSoc.getInputStream()));
        }
    }
}

class ftp extends Thread
{

    Socket ClientSoc;
    DataInputStream din;
    DataOutputStream dout;
    Scanner sc;
    PrintWriter pw;
    ftp(Socket soc)
    {
        try
        {
            ClientSoc=soc;                        
            din=new DataInputStream(ClientSoc.getInputStream());
            dout=new DataOutputStream(ClientSoc.getOutputStream());
            sc = new Scanner(ClientSoc.getInputStream());
            pw = new PrintWriter(ClientSoc.getOutputStream());
            System.out.println("FTP Client Connected ...");
            start();
            
        }
        catch(Exception ex)
        {
        }        
    }
    void SendFile() throws Exception
    {        
        String filename=din.readUTF();
        File f=new File(filename);
        if(!f.exists())
        {
            dout.writeUTF("File Not Found");
            return;
        }
        else
        {
            dout.writeUTF("READY");
            FileInputStream fin=new FileInputStream(f);
            int ch;
            do
            {
                ch=fin.read();
                dout.writeUTF(String.valueOf(ch));
            }
            while(ch!=-1);    
            fin.close();    
            dout.writeUTF("File Receive Successfully");                            
        }
    }
    
    void ReceiveFile() throws Exception
    {
        String filename=din.readUTF();
        if(filename.compareTo("File not found")==0)
        {
            return;
        }
        File f=new File(filename);
        String option;
        
        if(f.exists())
        {
            dout.writeUTF("File Already Exists");
            option=din.readUTF();
        }
        else
        {
            dout.writeUTF("SendFile");
            option="Y";
        }
            
            if(option.compareTo("Y")==0)
            {
                FileOutputStream fout=new FileOutputStream(f);
                int ch;
                String temp;
                do
                {
                    temp=din.readUTF();
                    ch=Integer.parseInt(temp);
                    if(ch!=-1)
                    {
                        fout.write(ch);                    
                    }
                }while(ch!=-1);
                fout.close();
                dout.writeUTF("File Send Successfully");
            }
            else
            {
                return;
            }
            
    }

    void BrowseDir() throws Exception
    {
        String filename = din.readUTF();
        File folder = new File(filename);
        ArrayList<String> files = new ArrayList<String>();
        ArrayList<String> directories = new ArrayList<String>();
        File[] listOfFiles = folder.listFiles();
        String allmsg = "";
        if (filename.equalsIgnoreCase("back"))
        {
            dout.writeUTF("Initial Point");
            return;
        }

        if(!folder.exists())
        {
            allmsg = "Dinvalid";
            dout.writeUTF(allmsg);
            return;
        }
        else
        {
            allmsg = "OK";
            dout.writeUTF(allmsg);
            
            for (int i = 0; i < listOfFiles.length; i++)
            {
                DefaultListModel dlm = new DefaultListModel();
                if (listOfFiles[i].isFile())
                    files.add(listOfFiles[i].getName());
                else
                    if (listOfFiles[i].isDirectory())
                        directories.add(listOfFiles[i].getName());
            }
            
            dout.write(directories.size());
            for (int i = 0; i < directories.size(); i++)
                dout.writeUTF(directories.get(i));
            
            dout.write(files.size());
            for (int i = 0; i < files.size(); i++)
                dout.writeUTF(files.get(i));
            return;
        }
        
    }

    void verify(){
    	while(true){
    		try {
				String usr = decrypt(din.readUTF(), "akuma");
				String pass = decrypt(din.readUTF(), "bisicetea");
				File fl = new File("D:\\users.txt");
				FileInputStream fi= new FileInputStream(fl);
				DataInputStream di=new DataInputStream(fi);
				String str=null;
				boolean dec =false;
				str=di.readLine();
				while(true){
					if(str==null) break;
					String pas = di.readLine();
					if(str.equals(usr) && pas.equals(pass)){
						dec=true;
						break;
					}
					str=di.readLine();
				}
				if(dec){
					dout.writeUTF("login successful");
					break;
				}
				else{
					dout.writeUTF("login failed");
				}
				System.out.println("reached");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
    	}
    }
    public void run()
    {
        
    	verify();
    	
    	while(true)
        {
            try
            {
            String Command=din.readUTF();
            if(Command.compareTo("RECEIVE")==0)
            {
                SendFile();
                continue;
            }
            else if(Command.compareTo("SEND")==0)
            {             
                ReceiveFile();
                continue;
            }
            else if(Command.compareTo("LIST")==0)
            {           
                BrowseDir();
                continue;
            }
            else if(Command.compareTo("DISCONNECT")==0)
            {
                System.exit(1);
            }
            }
            catch(Exception ex)
            {
            }
        }
    }
    public String decrypt(String strToDecrypt, String myKey) {
      try
      {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] key = myKey.getBytes("UTF-8");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
      }
      catch (Exception e) {
            System.out.println(e.toString());
      }
      return null;
}
}