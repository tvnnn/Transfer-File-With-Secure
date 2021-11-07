package ftpS;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    public static String hashAccount(String sString)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(sString.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (int i = 0; i < encodedhash.length; i++)
            {
                String hex = Integer.toHexString(0xff & encodedhash[i]);
                if(hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch(Exception ex)
        {
            System.out.println("Error while hashing: " + ex.toString());
        }
        return null;
    }
    
    public static void main(String args[]) throws Exception
    {
        Scanner s = new Scanner(System.in);
        int PORT = 21113;
        String content = null;
        ServerSocket socfd = new ServerSocket(PORT);
        System.out.println("FTP Server Started on Port Number " + String.valueOf(PORT));
        while(true)
        {
            try
            {
                System.out.print("1. Create User\n2. Delete User\n3. Run FTPServer\n4. Exit\n => Your choose is: ");
                int choose = s.nextInt();
                switch(choose)
                {
                    case 1:
                        try
                        {
                            File fl = new File("user.txt");
                            FileInputStream fi = new FileInputStream(fl);
                            DataInputStream di = new DataInputStream(fi);
                            System.out.print("    -> Enter Username: ");
                            s.nextLine();
                            String usr = s.nextLine();
                            System.out.print("    -> Enter Password: ");
                            String pas = s.nextLine();

                            if (usr.compareTo("") == 0 || pas.compareTo("") == 0)
                            {
                                System.out.print("Can't not be empty.\n");
                                break;
                            }
                            String str = null;
                            boolean dec = false;
                            str = di.readLine();
                            while(true)
                            {
                                if(str == null)
                                    break;
                                String str2 = di.readLine();
                                if(str.equals(hashAccount(usr)))
                                {
                                    dec = true;
                                    break;
                                }
                                str = di.readLine();
                            }
                            if(dec == true)
                                System.out.println("User already exist!");
                            else
                            {
                                System.out.println("Add user successfully !!");
                                FileWriter fw = new FileWriter("user.txt", true);
                                fw.write(hashAccount(usr) + "\n" + hashAccount(pas) + "\n");
                                fw.close();
                            }
                            fi.close();
                            di.close();
                        }
                        catch (Exception e)
                        {
                            System.out.println(e);
                        }
                        break;
                    case 2:
                        try
                        {
                            boolean flag = false;
                            System.out.print("    -> Enter Username: ");
                            s.nextLine();
                            String usr = s.nextLine();

                            File inputFile = new File("user.txt");
                            File tempFile = new File("user.txt.tmp");
                            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
                            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

                            String lineToRemove = hashAccount(usr);
                            String currentLine;

                            while((currentLine = reader.readLine()) != null) {
                                String trimmedLine = currentLine.trim();
                                if(trimmedLine.equals(lineToRemove))
                                {
                                    flag = true;
                                    continue;
                                }
                                writer.write(currentLine + System.getProperty("line.separator"));
                                writer.write(reader.readLine().trim() + System.getProperty("line.separator"));
                            }
                            if (flag == true)
                                System.out.println("User Deleted!");
                            else
                                System.out.println("Can't find user!");
                            writer.close(); 
                            reader.close();

                            inputFile.delete();
                            File aRename = new File("user.txt");
                            tempFile.renameTo(aRename);
                        }
                        catch (Exception ex)
                        {
                            System.out.println(ex.toString());
                        }
                        break;
                    case 3:
                        while(true)
                        {
                            System.out.println("Waiting for Connection ...");
                            ftp t = new ftp(socfd.accept());
                            PrintWriter out = new PrintWriter(t.ClientSoc.getOutputStream(), true);
                            BufferedReader in = new BufferedReader(new InputStreamReader(t.ClientSoc.getInputStream()));
                        }
                    case 4:
                        System.exit(0);
                        break;
                    default:
                        System.out.println("Bruh choose :)");
                        break;
                }   
            }
            catch (Exception ex)
            {
                System.out.println(ex.toString());
                return;
            }
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
        String filename = decryptString(din.readUTF());

        File f = new File(filename);
        if (f.canRead() == false)
        {
            dout.writeUTF(encryptString("PM"));
            return;
        }

        if (f.isDirectory() == true)
        {
            dout.writeUTF(encryptString("CantDownDir"));
            return;
        }

        if(!f.exists())
        {
            dout.writeUTF(encryptString("File Not Found"));
            return;
        }
        else
        {
            dout.writeUTF(encryptString("READY"));
            FileInputStream fin = new FileInputStream(f);
            int ch;
            do
            {
                ch = fin.read();
                dout.writeUTF(String.valueOf(ch)); ///////////////////////////////////////////////// ADD Encryption
            }
            while(ch != -1);    
            fin.close();    
            dout.writeUTF(encryptString("File Receive Successfully"));
        }
    }
    
    void ReceiveFile() throws Exception
    {
        String filename = decryptString(din.readUTF());
        String[] part = filename.split(Matcher.quoteReplacement("\\"));
        File f = new File(path + "/" + part[part.length - 1]);
        String option;
        if(f.exists())
        {
            dout.writeUTF(encryptString("File Already Exists"));
            option = decryptString(din.readUTF());
        }
        else
        {
            dout.writeUTF(encryptString("SendFile"));
            option = "Y";
        }
            
        if(option.compareTo("Y") == 0)
        {
            FileOutputStream fout = new FileOutputStream(f);
            int ch;
            String temp;
            do
            {
                temp = din.readUTF(); ///////////////////////////////////////// ADD Decryption
                ch = Integer.parseInt(temp);
                if(ch != -1)
                    fout.write(ch);                    
            }
            while(ch != -1);
            fout.close();
            dout.writeUTF(encryptString("File Send Successfully"));
        }
        else
            return;
    }

    void DeleteFile() throws Exception
    {
        String filename = decryptString(din.readUTF());
        File f = new File(filename);
        if (f.canRead() == false)
        {
            dout.writeUTF(encryptString("PM"));
            return;
        }
        else
        {
            if (f.exists())
            {
                dout.writeUTF(encryptString("SURE"));
                if(decryptString(din.readUTF()).compareTo("Y") == 0)
                    f.delete();
                else
                    return;
            }
        }

    }

    void BrowseDir() throws Exception
    {
        path = decryptString(din.readUTF());
        File folder = new File(path);
        ArrayList<String> files = new ArrayList<String>();
        ArrayList<String> directories = new ArrayList<String>();
        File[] listOfFiles = folder.listFiles();
        String allmsg = "";

        if(!folder.exists())
        {
            allmsg = encryptString("Dinvalid");
            dout.writeUTF(allmsg);
            return;
        }
        if (folder.isFile())
        {
            allmsg = encryptString("IsFile");
            dout.writeUTF(allmsg);
            return;
        }
        if (folder.canRead() == false)
        {
            allmsg = encryptString("PM");
            dout.writeUTF(allmsg);
            return;
        }
        else
        {
            allmsg = encryptString("OK");
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
                dout.writeUTF(encryptString(directories.get(i)));
            
            dout.write(files.size());
            for (int i = 0; i < files.size(); i++)
                dout.writeUTF(encryptString(files.get(i)));
            return;
        }
        
    }

    void NewFile() throws Exception
    {
        try
        {
            String fileName = decryptString(din.readUTF());
            File f = new File(path + "/" + fileName);

            if (f.exists())
            {
                dout.writeUTF(encryptString("File Already Exists"));
                return;
            }
            f.createNewFile();
            dout.writeUTF(encryptString("OK"));
        }
        catch (IOException io)
        {
            dout.writeUTF(encryptString("PM"));
        }
    }


    void NewFolder() throws Exception
    {
        try
        {
            String folderName = decryptString(din.readUTF());
            File f = new File(path + "/" + folderName);

            if (f.exists())
            {
                dout.writeUTF(encryptString("Folder Already Exists"));
                return;
            }

            if (f.mkdir() == true)
                dout.writeUTF(encryptString("OK"));
            else
                dout.writeUTF(encryptString("PM"));
        }
        catch (IOException io)
        {
            
        }
    }

    void Rename() throws Exception
    {
        try
        {
            String oldName = decryptString(din.readUTF());
            File pre = new File(path + "/" + oldName);
            String newName = decryptString(din.readUTF());
            File des = new File(path + "/" + newName);

            if (pre.canRead() == false)
            {
                dout.writeUTF(encryptString("PM"));
                return;
            }
            if (des.exists())
            {
                dout.writeUTF(encryptString("File or Folder Already Exists"));
                return;
            }
            else
            {
                dout.writeUTF(encryptString("OK"));
                pre.renameTo(des);
            }
        }
        catch (Exception e)
        {

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
                    dout.writeUTF(encryptString("login successful"));
                    break;
                }
                else
                    dout.writeUTF(encryptString("login failed"));
                System.out.println("Login fail");
            }
            catch (IOException e)
            {
                //e.printStackTrace();
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
                String Command = decryptString(din.readUTF());
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
                else if(Command.compareTo("DELETE") == 0)
                {
                    DeleteFile();
                }
                else if(Command.compareTo("NewFile") == 0)
                {
                    NewFile();
                }
                else if(Command.compareTo("NewFolder") == 0)
                {
                    NewFolder();
                }
                else if(Command.compareTo("Rename") == 0)
                {
                    Rename();
                }
            }
            catch(Exception ex)
            {
                //System.out.println(ex.toString());
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
            //System.out.println("Error while encrypting: " + ex.toString());
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
          //System.out.println("Error while decrypting: " + e.toString());
        }
        return null;
    }
    
    public String encryptString(String strToEncrypt)
    {
        try
        {
            String myKey = "bisiceteasec";
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] key = myKey.getBytes("UTF-8");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
        }
        return null;
    }

    public String decryptString(String strToDecrypt)
    {
        try
        {
            String myKey = "bisiceteasec";
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] key = myKey.getBytes("UTF-8");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16);
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
        }
        return null;
    }
}

