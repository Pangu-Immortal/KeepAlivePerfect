package com.daemon.utils;


import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileStorage{
    private static String TAG = "FileStorage";
    private static String FILE_DIRECTORY = "/mnt/sdcard/4C89F979-C13C-4DDB-B2F1-0A138C613DD1/";

    public static String readFile(String name){
        File file = new File(FILE_DIRECTORY+name);
        if(!file.exists() || !file.isFile()){
            return "";
        }
        try{
            RandomAccessFile reader = new RandomAccessFile(file,"r");
            FileChannel fileChannel = reader.getChannel();
            fileChannel.lock();
            int size = (int)fileChannel.size();
            byte[] buffer = new byte[size];
            reader.read(buffer);
            reader.close();
            return new String(buffer);
        }catch (Exception exp) {
            exp.printStackTrace();
        }
        return "";
    }

    public static Boolean writeFile(String name,String data){
        File file = new File(FILE_DIRECTORY+name);
        if(file.exists()){
            if (!file.isFile()){
                return false;
            }
            file.delete();
        }
        file.getParentFile().mkdirs();
        try{
            file.createNewFile();
            RandomAccessFile reader = new RandomAccessFile(file,"w");
            FileChannel fileChannel = reader.getChannel();
            fileChannel.lock();
            fileChannel.write(ByteBuffer.wrap(data.getBytes()));
            reader.close();
            return true;
        }catch (Exception exp) {
            exp.printStackTrace();
        }
        return false;
    }
}
