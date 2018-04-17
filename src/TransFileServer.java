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

//thread.currentthread().getid()


public class TransFileServer {
    private static final int PORT = 2222;
    //private List<Socket> mList = new ArrayList<Socket>();
    private ServerSocket server = null;
    private ExecutorService myExecutorService = null;
    int queue_size;
    int active_thread;
    int failure=0;

    public static void main(String[] args) {
        new TransFileServer();
    }

    public TransFileServer(){
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
            Socket client = null;
            while(true)
            {

                client = server.accept();
                myExecutorService.execute(new ImplementsRunnable(client));
                Thread.sleep(1);
                queue_size=myExecutorService.getQueue().size();
                System.out.println("接收到请求，queue size="+queue_size);

                active_thread=myExecutorService.getActiveCount();
                System.out.println("active thread size="+active_thread);
                //mList.add(client);

            }

        }catch(Exception e){e.printStackTrace();}

    }


class ImplementsRunnable implements Runnable {

    //private static final int NEW_HOST_PORT = 2223;
    private Socket skt;
    DataInputStream inputStream;
    FileOutputStream fos;
    String flag = "";
    int cnt = 0;


    public ImplementsRunnable(Socket socket) {
        this.skt = socket;
    }

    @Override
    public void run() {
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

            //开始识别
            //try {
                //System.out.println("Photo "+trueName+" recognition is ongoing");
                ProcessBuilder pb = new ProcessBuilder("python", "label_image_edge.py", trueName);
                pb.redirectError(Redirect.INHERIT);
                Process p = pb.start();
                pb.redirectError(Redirect.INHERIT.INHERIT);

                BufferedReader in=new BufferedReader(new InputStreamReader(skt.getInputStream()));
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
                                    System.out.println("Delete operation is failed."+trueName);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            failure += 1;
                            return;
                        }
                    }catch(IOException E){}
                }

                //p.waitFor();

                // 更改输入输出流
                BufferedReader bfr = new BufferedReader(new InputStreamReader(p.getInputStream()));

                String line = "";
                line = bfr.readLine();
                //System.out.println(line);
                // 服务器发送消息
                writer.println(line);
                //System.out.println("Photo "+trueName+" recognition is completed");
                long end1=System.currentTimeMillis();

                cc.setTimeInMillis( end1- start1);
                //System.out.println(trueName+"识别耗时: " + cc.get(Calendar.MINUTE) + "分 " + cc.get(Calendar.SECOND) + "秒 " + cc.get(Calendar.MILLISECOND) + " 微秒");

                writer.flush();
                writer.close();
                skt.close();
                inputStream.close();


           // } catch (IOException e) {
            //    e.printStackTrace();
             //   System.out.println("connection failed");
             //   return;
            //} //catch (InterruptedException e) {
              //  e.printStackTrace();
           // }

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
}