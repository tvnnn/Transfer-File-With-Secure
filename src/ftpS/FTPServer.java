package ftpS;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.DefaultListModel;
import java.util.regex.Matcher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;

public class FTPServer
{
    public static void main(String args[]) throws Exception
    {
        int PORT = 21113;
        ServerSocket socfd = new ServerSocket(PORT);
        System.out.println("FTP Server Started on Port Number " + String.valueOf(PORT));
        while(true)
        {
            System.out.println("Waiting for Connection ...");
            ftp t = new ftp(socfd.accept());
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
    String path = "";
    private static final String SECRET_KEY = "sup3r_s3cr3t_k3y_by_b1s1c3t3aS3c";
    private static final String SALT = "sup3r_s4lt_h1h1_b1s1c3t3a";
    ftp(Socket soc)
    {
        try
        {
            ClientSoc = soc;
            din = new DataInputStream(ClientSoc.getInputStream());
            dout = new DataOutputStream(ClientSoc.getOutputStream());
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
        String filename = din.readUTF();

        File f = new File(filename);
        if (f.canRead() == false)
        {
            dout.writeUTF("PM");
            return;
        }

        if (f.isDirectory() == true)
        {
            dout.writeUTF("CantDownDir");
            return;
        }

        if(!f.exists())
        {
            dout.writeUTF("File Not Found");
            return;
        }
        else
        {
            dout.writeUTF("READY");
            FileInputStream fin = new FileInputStream(f);
            int ch;
            do
            {
                ch = fin.read();
                dout.writeUTF(encrypt(String.valueOf(ch)));
            }
            while(ch != -1);    
            fin.close();    
            dout.writeUTF("File Receive Successfully");                            
        }
    }
    
    void ReceiveFile() throws Exception
    {
        String filename = din.readUTF();
        String[] part = filename.split(Matcher.quoteReplacement("\\"));

        File f = new File(path + "/" + part[part.length -1]);
        String option;
        if(f.exists())
        {
            dout.writeUTF("File Already Exists");
            option = din.readUTF();
        }
        else
        {
            dout.writeUTF("SendFile");
            option = "Y";
        }
            
        if(option.compareTo("Y") == 0)
        {
            FileOutputStream fout = new FileOutputStream(f);
            int ch;
            String temp;
            do
            {
                temp = decrypt(din.readUTF());
                ch = Integer.parseInt(temp);
                if(ch != -1)
                    fout.write(ch);                    
            }
            while(ch != -1);
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
        path = din.readUTF();
        File folder = new File(path);
        ArrayList<String> files = new ArrayList<String>();
        ArrayList<String> directories = new ArrayList<String>();
        File[] listOfFiles = folder.listFiles();
        String allmsg = "";

        if(!folder.exists())
        {
            allmsg = "Dinvalid";
            dout.writeUTF(allmsg);
            return;
        }
        if (folder.isFile())
        {
            allmsg = "IsFile";
            dout.writeUTF(allmsg);
            return;
        }
        if (folder.canRead() == false)
        {
            allmsg = "PM";
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
        while(true)
        {
            try
            {
                String usr = din.readUTF();
                String pass = din.readUTF();
                File fl = new File("users.txt");
                FileInputStream fi = new FileInputStream(fl);
                DataInputStream di = new DataInputStream(fi);
                String str = null;
                boolean dec = false;
                str = di.readLine();
                while(true)
                {
                    if(str == null)
                        break;
                    String pas = di.readLine();
                    if(str.equals(usr) && pas.equals(pass))
                    {
                        dec = true;
                        break;
                    }
                    str = di.readLine();
                }
                if(dec)
                {
                    dout.writeUTF("login successful");
                    break;
                }
                else
                    dout.writeUTF("login failed");
                System.out.println("reached");
            }
            catch (IOException e)
            {
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
                String Command = din.readUTF();
                if(Command.compareTo("RECEIVE") == 0)
                {
                    SendFile();
                    continue;
                }
                else if(Command.compareTo("SEND") == 0)
                {             
                    ReceiveFile();
                    continue;
                }
                else if(Command.compareTo("LIST") == 0)
                {           
                    BrowseDir();
                    continue;
                }
                else if(Command.compareTo("DISCONNECT") == 0)
                {
                    System.exit(1);
                }
                else if(Command.compareTo("LOGOUT") == 0)
                {
                    verify();
                }
            }
            catch(Exception ex)
            {
                System.out.println(ex.toString());
            }
        }
    }
    public static String encrypt(String strToEncrypt)
    {
        try
        {
            byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        }
        catch(Exception ex)
        {
            System.out.println("Error while encrypting: " + ex.toString());
        }
        return null;
    }
    
    public static String decrypt(String strToDecrypt)
    {
        try
        {
            byte[] iv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), SALT.getBytes(), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        }
        catch (Exception e)
        {
          System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }
}