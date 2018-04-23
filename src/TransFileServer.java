import javax.xml.crypto.Data;
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
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.lang.ProcessBuilder.Redirect;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.*;
import java.util.*;


public class TransFileServer {
    private static final int PORT = 2222;
    //private List<Socket> mList = new ArrayList<Socket>();
    private ServerSocket server = null;
    private ServerSocket edge_server = null;
    private int queue_size;
    private int remote_queue_size;
    int active_thread;
    int failure=0;
    private String EdgeIP="";                             //--------------------------------------------------------------------------------------
    private static  final int EdgePORT=2223;
    private boolean flag; //indicate whether it is necessary to send the message to another edge;
    private int TimeLimit=18000;
    private Socket client;
    public static void main(String[] args) {
        new TransFileServer();
    }
    public TransFileServer(){
        //establish connection with another edge device
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    edge_server=new ServerSocket(EdgePORT);
                    server.setSoTimeout(4000);
                    Socket edge_client=edge_server.accept();
                    System.out.println("connection with anoterh edge deivce is establised successfully");
                    DataInputStream dis=new DataInputStream(edge_client.getInputStream());
                    DataOutputStream dos=new DataOutputStream(edge_client.getOutputStream());
                    //exchange information every 2000 millsecond
                    while(true)
                    {
                        Thread.sleep(2000);
                        remote_queue_size=dis.readInt();
                        dos.writeInt(queue_size);
                    }

                }catch(SocketTimeoutException e){
                    System.out.println("exception happens"+e);
                    return;
                }
                catch(Exception e){
                    System.out.println("exception happens"+e);
                    return;
                }
            }
        }).start();


        //accept new connections from the client
        try
        {
            server = new ServerSocket(PORT);
            //创建线程池
            server.setSoTimeout(300000);
            //ThreadPoolExecutor myExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(5);
            int N_CPUS = Runtime.getRuntime().availableProcessors();
            System.out.println(N_CPUS);
            ThreadPoolExecutor myExecutorService =new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            System.out.println("server is working...\n");
            //Socket client = null;
            while(true)
            {
                client = server.accept();
                UploadRunnable up=new UploadRunnable(client);
                Thread t=new Thread(up);
                t.start();
                t.join();
                if(up.getResult()==false) continue;                            //upload failure
                String trueName=up.getTrueName();

                if(remote_queue_size-queue_size>1)
                {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String response = TransFileEdge(trueName, EdgeIP, TimeLimit);
                                if (!response.equals("false")) {
                                    PrintWriter writer = new PrintWriter(client.getOutputStream());
                                    writer.println(response);
                                    writer.flush();
                                    writer.close();
                                    client.close();
                                }
                            }catch (Exception e){return;}

                        }
                    }).start();
                continue;
                }
                myExecutorService.execute(new ImplementsRunnable(client,trueName));
                Thread.sleep(1);
                queue_size=myExecutorService.getQueue().size();
                System.out.println("接收到请求，queue size="+queue_size);
                active_thread=myExecutorService.getActiveCount();
                System.out.println("active thread size="+active_thread);
            }
        }catch(Exception e){e.printStackTrace();}

    }



//revieve photo thread
public class UploadRunnable implements Runnable{
    //private static final int NEW_HOST_PORT = 2223;
    private Socket skt;
    DataInputStream inputStream;
    FileOutputStream fos;
    String trueName;
    boolean result=true;

    public UploadRunnable(Socket socket) {
        this.skt = socket;
    }

    public void run(){
        Calendar cc = Calendar.getInstance();
        //Socket new_skt = null;
        long start_time1 = System.currentTimeMillis();
        try {
            System.out.println("start transmitting file");
            long start1 = System.currentTimeMillis();
            //System.out.println("Host port is " + PORT);
            //接收客户端文件
            inputStream = new DataInputStream(skt.getInputStream());
            trueName = inputStream.readUTF();
            long fileSize = inputStream.readLong();
            if (trueName == null || fileSize == -1) {
                System.out.println("connection closed before it starts");
                result=false;
                return;
            }
            fos = new FileOutputStream("./edge/" + trueName);
            byte[] inputByte = new byte[1024 * 8];
            int length;
            while (fileSize > 0) {//&&(length = inputStream.read(inputByte,0,(int)Math.min(inputByte.length, fileSize))) != -1) {
                length = inputStream.read(inputByte, 0, (int) Math.min(inputByte.length, fileSize));
                if (length == -1 || length == 0) {
                    System.out.println("connection broken during transmission" + trueName);
                    result=false;
                    return;
                }
                //System.out.println("正在接收数据..." + length);
                fos.write(inputByte, 0, length);
                fileSize -= length;
                fos.flush();
            }
            fos.close();
            inputStream.close();
            System.out.println(trueName + "图片接收完成");
            long end0 = System.currentTimeMillis();
            cc.setTimeInMillis(end0 - start1);
            System.out.println(trueName + "传输耗时: " + cc.get(Calendar.MINUTE) + "分 " + cc.get(Calendar.SECOND) + "秒 " + cc.get(Calendar.MILLISECOND) + " 微秒");
        }catch (IOException e) {
            e.printStackTrace();
            System.out.println("fail outside");
            result=false;
            return;
        }

}
    public String getTrueName() { return trueName; }
    public Socket getSocket() { return skt; }
    public boolean getResult(){return result;}
    //public String getDirpath(){return }
}




//Computation Thread
class ImplementsRunnable implements Runnable {

    //private static final int NEW_HOST_PORT = 2223;
    private Socket skt;
    DataInputStream inputStream;
    FileOutputStream fos;
    String line;
    String trueName;


    public ImplementsRunnable(Socket socket,String trueName) {
        this.skt = socket;
        this.trueName=trueName;
    }

    @Override
    public void run() {
        /*
        String trueName="";
        Calendar cc = Calendar.getInstance();
        //Socket new_skt = null;
        long start_time1 = System.currentTimeMillis();
        try {
            System.out.println("start transmitting file");
            long start1=System.currentTimeMillis();
            //System.out.println("Host port is " + PORT);


            //接收客户端文件
            inputStream = new DataInputStream(skt.getInputStream());
            PrintWriter writer = new PrintWriter(skt.getOutputStream());

            trueName = inputStream.readUTF();
            long fileSize = inputStream.readLong();
            if(trueName==null||fileSize==-1)
            {
                System.out.println("connection closed before it starts");
                return;
            }
            fos = new FileOutputStream("./edge/" + trueName);
            byte[] inputByte = new byte[1024 * 8];
            int length;


            while (fileSize > 0 ){//&&(length = inputStream.read(inputByte,0,(int)Math.min(inputByte.length, fileSize))) != -1) {
                length = inputStream.read(inputByte,0,(int)Math.min(inputByte.length, fileSize));
                if (length == -1||length==0)
                {
                    System.out.println("connection broken during transmission"+trueName);
                    return;
                }
                //System.out.println("正在接收数据..." + length);

                fos.write(inputByte, 0, length);
                fileSize-=length;
                fos.flush();

            }
            fos.close();

            System.out.println(trueName+"图片接收完成");
            long end0=System.currentTimeMillis();
            cc.setTimeInMillis( end0- start1);
            System.out.println(trueName+"传输耗时: " + cc.get(Calendar.MINUTE) + "分 " + cc.get(Calendar.SECOND) + "秒 " + cc.get(Calendar.MILLISECOND) + " 微秒");

            */
            //if(flag==false) {
                //开始识别
                //try {
                //System.out.println("Photo "+trueName+" recognition is ongoing");
        Calendar cc = Calendar.getInstance();
        long start_time1 = System.currentTimeMillis();
        try{
                PrintWriter writer = new PrintWriter(skt.getOutputStream());
                ProcessBuilder pb = new ProcessBuilder("python", "label_image_edge.py", trueName);
                pb.redirectError(Redirect.INHERIT);
                Process p = pb.start();
                pb.redirectError(Redirect.INHERIT.INHERIT);

                BufferedReader in = new BufferedReader(new InputStreamReader(skt.getInputStream()));
                while (p.isAlive()) {
                    skt.setSoTimeout(250);
                    try {
                        String d = in.readLine();
                        if (d == null) {
                            System.out.println("Failure " + trueName);
                            //删除文件
                            try {
                                File file = new File("./edge/" + trueName);
                                if (file.delete()) {
                                    //System.out.println(file.getName() + " is deleted!");
                                } else {
                                    System.out.println("Delete operation is failed." + trueName);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            failure += 1;
                            return;
                        }
                    } catch (IOException E) {
                    }
                }

                // 更改输入输出流
                BufferedReader bfr = new BufferedReader(new InputStreamReader(p.getInputStream()));
                line = bfr.readLine();
                //System.out.println(line);
           // }
            //read result from cloud if fail return true.
            //else{
            //}
                // 服务器发送消息
                writer.println(line);
                //System.out.println("Photo "+trueName+" recognition is completed");
                long end1 = System.currentTimeMillis();
                cc.setTimeInMillis(end1 - start_time1);
                //System.out.println(trueName+"识别耗时: " + cc.get(Calendar.MINUTE) + "分 " + cc.get(Calendar.SECOND) + "秒 " + cc.get(Calendar.MILLISECOND) + " 微秒");
                writer.flush();
                writer.close();
                skt.close();
                //inputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("fail outside");
            return;
        }

        //删除文件
        try {
            File file = new File("./edge/" + trueName);

            if (file.delete()) {
                //System.out.println(file.getName() + " is deleted!");
            } else {
                System.out.println("Delete operation is failed."+trueName);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        long end_time1 = System.currentTimeMillis();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(end_time1 - start_time1);
        System.out.println("总共耗时: " + c.get(Calendar.MINUTE) + "分 " + c.get(Calendar.SECOND) + "秒 " + c.get(Calendar.MILLISECOND) + " 微秒");


    }
}

    public String TransFileEdge(String trueName,String ip,int Timelimit) {

        System.out.println(trueName + "sent to edge");
        int port = 2222;
        DataOutputStream dos;
        FileInputStream fis;
        try {
            File file = new File("./edge/" + trueName);
            long start1 = System.currentTimeMillis();
            Calendar cc = Calendar.getInstance();
            Socket socket = new Socket(ip, port);
            //System.out.println(ip);
            socket.setSoTimeout(100000);
            dos = new DataOutputStream(socket.getOutputStream());
            fis = new FileInputStream(file);
            System.out.println("start transmitting information to edge");
            dos.writeUTF(trueName);//截取图片名称
            dos.flush();
            byte[] sendBytes = new byte[1024 * 8];
            int length;
            long fileSize = file.length();
            dos.writeLong(fileSize);
            dos.flush();
            long start2 = System.currentTimeMillis();
            long end = System.currentTimeMillis();
            while ((length = fis.read(sendBytes, 0, sendBytes.length)) > 0) {
                if (end - start1 > Timelimit) {
                    System.out.println(trueName + "upload file exceeds time limit, try to connect to another edge device");
                    socket.close();
                    return "false";
                }
                dos.write(sendBytes, 0, length);
                dos.flush();
                end = System.currentTimeMillis();
            }
            System.out.println("sent Successfully");
            long end1 = System.currentTimeMillis();
            cc.setTimeInMillis(end1 - start2);
            System.out.println(trueName+"传输耗时: " + cc.get(Calendar.MINUTE) + "分 " + cc.get(Calendar.SECOND) + "秒 " + cc.get(Calendar.MILLISECOND) + " 微秒");
            fis.close();
            int a=(int)(end1-start1);
            if(Timelimit-a<0)
            {
                System.out.println("failure");
                //fail_count+=1;
                return "false";
            }
            else{
                socket.setSoTimeout(Timelimit-a);
                //System.out.println("I am receiving");
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String response;
                try{
                    response = reader.readLine();
                }catch(SocketTimeoutException E){
                    System.out.println("Caught timeout exception: ");
                    //fail_count=fail_count+1;
                    //System.out.println("count="+fail_count);
                    socket.close();
                    return "false";
                }


                socket.close();
                long end2=System.currentTimeMillis();
                cc.setTimeInMillis( end2- start1);
                System.out.println(trueName+"纯总共耗时: " + cc.get(Calendar.MINUTE) + "分 " + cc.get(Calendar.SECOND) + "秒 " + cc.get(Calendar.MILLISECOND) + " 微秒");
                return response;
            }

        } catch (Exception E) {
            System.out.println("error occurs when transmiting to edge");
            E.printStackTrace();
            return "false";
        }
    }



}