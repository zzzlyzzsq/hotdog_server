import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.lang.Process;
import java.lang.Runtime;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.lang.ProcessBuilder.Redirect;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.*;
import java.util.*;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.InetAddress;

public class client {
    private Socket socket;
    private Socket new_socket;
    //private String ip = "10.186.62.55";
    private int port = 2222;
    //private int new_port = 2223;
    File file;
    String[] imgStrings;
    String pathString = "./local";
    public static void main(String[] args) {
        new client();
    }
    public client() {

        //String pathString = "./local";
        String ipString = "127.0.0.1";
        //String cloud_ipString = cloud_ip.getText().toString();
        imgStrings = new String[]{};
        if (pathString.trim().length() == 0)
            return;
        System.out.println("-----------------------------------------------");
        System.out.println(pathString);
        System.out.println("-----------------------------------------------");
        String[] paths = listFile(pathString);
        //long start_time1 = System.currentTimeMillis();
        //String[] paths= {"/storage/sdcard1/test/hotdog (125).jpg","/storage/sdcard1/test/hotdog (1).jpg","/storage/sdcard1/test/hotdog (7).jpg"};
        if (paths.length == 0) {

            return;
        } else {
            for (int j = 0; j < paths.length; j++) {
                long start_time1 = System.currentTimeMillis();
                seriesUpload(paths[j], ipString,paths);
                long end_time1 = System.currentTimeMillis();
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(end_time1 - start_time1);
                System.out.println("图片"+"耗时: " + c.get(Calendar.MINUTE) + "分 " + c.get(Calendar.SECOND) + "秒 " + c.get(Calendar.MILLISECOND) + " 微秒");
                //getResult(j);
            }


        }

        /*
        long end_time1 = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(end_time1 - start_time1);
        System.out.println("耗时: " + c.get(Calendar.MINUTE) + "分 " + c.get(Calendar.SECOND) + "秒 " + c.get(Calendar.MILLISECOND) + " 微秒");
        */
    }
    private void getResult (int i) {
        if(file.length() != 0)
            //Toast.makeText(MainActivity.this,"Upload Success!",Toast.LENGTH_SHORT).show();
            System.out.println("Nice");
        else
            System.out.println("Photo number "+ i + " Upload Failure!");
    }

    public String[] listFile(String derect){
        File file = new File(derect);
        File[] f = file.listFiles();
        if(f == null) return null;
        String Path[] = new String[f.length];
        for (int i = 0; i < f.length; i++) {
            Path[i] = f[i].getPath();
            //System.out.println(Path[i]);
        }
        return Path;
    }

    private void seriesUpload(String dirpath,String ip,String[] paths){
        DataOutputStream dos;
        FileInputStream fis;
        int isAlive = 0;
        //socket = new Socket(ip, port);
        try {
            file = new File(dirpath);
            if (file.length() == 0) {
                System.out.println("Am i here?");
                return;
            } else {
                //System.out.println("I am in the socket!!!");

                socket = new Socket(ip, port);
                dos = new DataOutputStream(socket.getOutputStream());
                fis = new FileInputStream(file);
                String trueName=dirpath.substring(dirpath.lastIndexOf("\\") + 1);
                //System.out.println("True Name="+trueName);
                dos.writeUTF(trueName);//截取图片名称
                dos.flush();
                byte[] sendBytes = new byte[1024 * 8];
                int length;


                while ((length = fis.read(sendBytes, 0, sendBytes.length)) > 0) {
                    dos.write(sendBytes, 0, length);
                    dos.flush();// 发送给服务器
                }
                socket.shutdownOutput();
                fis.close();


                try {

                    System.out.println("I am receiving");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(recv_socket.getOutputStream()));


                    String response = reader.readLine();


                    System.out.println("Client received response: " + response);

                    if(response.charAt(0)==trueName.charAt(0))
                    {
                        file = new File(pathString +trueName);
                        Boolean flag = file.delete();
                    }
                        /*
                        dos.close();//在发送消息完之后一定关闭，否则服务端无法继续接收信息后处理，手机卡机
                        fis.close();
                        */
                    //socket.close();

                } catch (IOException e) {
                    System.out.println("Caught Exception: " + e.toString());
                }
                //}




            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
