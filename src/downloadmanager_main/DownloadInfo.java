/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package downloadmanager_main;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DownloadInfo {         

    private final String url;

    private String fileName;       

    private String fileExtension;

    private URL nonStringUrl;

    private HttpURLConnection connection;

    private int fileSize;

    private int remainingByte;

    private RandomAccessFile outputFile;

    public DownloadInfo(String url) {
        this.url = url;

        initiateInformation();
    }

    private void initiateInformation(){        
        fileName = url.substring(url.lastIndexOf('/') + 1, url.length());

        fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length());

        try {

            nonStringUrl = new URL(url);

            connection = (HttpURLConnection) nonStringUrl.openConnection();

            fileSize = ((connection.getContentLength()));

            System.out.printf("File Size is : %d \n", fileSize);

            System.out.printf("Remain File Size is : %d \n", fileSize % 8);

            remainingByte = fileSize % 8;

            fileSize /= 8;

            outputFile = new RandomAccessFile(fileName, "rw");

        } catch (MalformedURLException ex) {
            Logger.getLogger(DownloadInfo.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DownloadInfo.class.getName()).log(Level.SEVERE, null, ex);
        }



        System.out.printf("File Name is : %s\n", fileName);
        System.out.printf("File Extension is : %s\n", fileExtension);        
        System.out.printf("Partition Size is : %d MB\n", fileSize);

        int first = 0 , last = fileSize - 1;

        ExecutorService thread_pool = Executors.newFixedThreadPool(8);        

        for(int i=0;i<8;i++){            
            if(i != 7){
                thread_pool.submit(new Downloader(nonStringUrl, first, last, (i+1), outputFile));
            }
            else{
                thread_pool.submit(new Downloader(nonStringUrl, first, last + remainingByte, (i+1), outputFile));
            }
            first = last + 1;
            last += fileSize;
        }
        thread_pool.shutdown();

        try {
            thread_pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Logger.getLogger(DownloadInfo.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}