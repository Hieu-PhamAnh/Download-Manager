package downloadmanager_main;

// download link https://download.oracle.com/java/19/latest/jdk-19_linux-x64_bin.tar.gz 181,1MB
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class Download extends Observable implements Runnable {
     
    private static final int MAX_BUFFER_SIZE = 1024;
     
    public static final String STATUSES[] = {"Downloading",
    "Paused", "Complete", "Cancelled", "Error"};
     
    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;
     
    private final URL url; 
    private long size; 
    private long downloaded; 
    private int status; 
    private long initTime; 
    private long startTime; 
    private long readSinceStart; 
    private long elapsedTime=0; 
    private long prevElapsedTime=0; 
    private long remainingTime=-1; 
    private float avgSpeed=0; 
    private float speed=0; 
    private final long startByte;       
    private final long endByte;
    private final int threadNum;    
    private InputStream stream;
    PrintWriter output;
    
    public Download(URL url,long startByte, long endByte, int threadNum) {
        this.url = url;
        size = -1;
        this.startByte = startByte;
        this.endByte = endByte;
        this.threadNum = threadNum;
        downloaded = 0;
        status = DOWNLOADING;
        download();     
    }
     
    public String getUrl() {
        return url.toString();
    }
     
    public long getSize() {
        return size;
    }
    
    public float getSpeed() {
        return speed;
    }

    public float getAvgSpeed() {
        return avgSpeed;
    }

    public String getElapsedTime() {
        return formatTime(elapsedTime/1000000000);
    }

    public String getRemainingTime() {
        if(remainingTime<0)   return "Unknown";
        else    return formatTime(remainingTime);
    }

    public String formatTime(long time) { 
        String s="";
        s+=(String.format("%02d", time/3600))+":";
        time%=3600;
        s+=(String.format("%02d", time/60))+":";
        time%=60;
        s+=String.format("%02d", time);
        return s;
    }

    public float getProgress() {
        return ((float) downloaded / size) * 100;
    }
     
    public int getStatus() {
        return status;
    }

    public void pause() {
        prevElapsedTime=elapsedTime;
        status = PAUSED;
        System.out.println(getFileName(url) +" "+ STATUSES[status]);
        stateChanged();
    }

    public void resume() {
        status = DOWNLOADING;
        System.out.println(getFileName(url) +" "+ STATUSES[status]);
        stateChanged();
        download();
    }

    public void cancel() {
        prevElapsedTime=elapsedTime;
        status = CANCELLED;
        System.out.println(getFileName(url) +" "+ STATUSES[status]);
        stateChanged();
    }

    private void error() {
        prevElapsedTime=elapsedTime;
        status = ERROR;
        System.out.println(getFileName(url) +" "+ STATUSES[status]);
        stateChanged();
    }

    private void download() {
        Thread thread = new Thread(this);
        thread.start();
        System.out.println(getFileName(url) +" "+ STATUSES[status]);
    }

    private String getFileName(URL url) {
        String fileName = url.getFile();
        return fileName.substring(fileName.lastIndexOf('/') + 1)+String.valueOf(threadNum);
    }

    public void run() {
        RandomAccessFile file = null;
        InputStream stream = null;
         
        try {
            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Range",
                    "bytes=" + (startByte + downloaded) + "-"+ endByte);
            connection.connect();

            if (connection.getResponseCode() / 100 != 2) {
                error();
            }
             
            int contentLength = connection.getContentLength();
            if (contentLength < 1) {
                error();
            }

            if (size == -1) {
                size = contentLength;
                stateChanged();
            }
            int i=0;
            file = new RandomAccessFile(getFileName(url), "rw");
            file.seek(downloaded);
             
            stream = connection.getInputStream();
            initTime = System.nanoTime();
            while (status == DOWNLOADING) {

                if(i==0)
                {   startTime = System.nanoTime();
                    readSinceStart = 0;
                }
                byte buffer[];
                if (size - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[(int)(size - downloaded)];
                }

                int read = stream.read(buffer);
                if (read == -1)
                    break;

                file.write(buffer, 0, read);
                downloaded += read;
                readSinceStart+=read;

                i++;
                if(i>=50)
                {   speed=(readSinceStart*976562.5f)/(System.nanoTime()-startTime);
                    if(speed>0) remainingTime=(long)((size-downloaded)/(speed*1024));
                    else remainingTime=-1;
                    elapsedTime=prevElapsedTime+(System.nanoTime()-initTime);
                    avgSpeed=(downloaded*976562.5f)/elapsedTime;
                    i=0;
                }
                stateChanged();
            }

            if (status == DOWNLOADING) {
                status = COMPLETE;
                System.out.println(getFileName(url) +" "+ STATUSES[status]);          
                if(threadNum==4){
                    mergeFile();
                }
                stateChanged();
            }
        } catch (Exception e) {
            System.out.println(e);
            error();
        } finally {
            if (file != null) {
                try {
                    file.close();
                } catch (Exception e) {}
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {}
            }
        }
    }
    private void mergeFile(){
        System.out.println("Gh??p file th??nh c??ng");
    }
    private void stateChanged() {
        setChanged();
        notifyObservers();
//        System.out.println(STATUSES[status]);
    }
    private void logFile(){
        output = createWriter("logFile.txt");
    }

    private PrintWriter createWriter(String logFiletxt) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}