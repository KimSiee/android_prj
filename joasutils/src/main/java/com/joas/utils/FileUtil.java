/*
 * Copyright (C) 2017 JoongAng Control, Inc - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Sungchul Choi <scchoi@joas.co.kr>, 18. 1. 18 오전 9:28
 *
 */

package com.joas.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Logger;

public class FileUtil {
    public static final String TAG = "FileUtil";

    public static boolean bufferToFile(String filePath, String fileName, byte[] data, boolean append) {
        boolean ret = true;
        try {
            File parent = new File(filePath);
            if (!parent.exists()) {
                parent.mkdirs();
            }

            File file = new File(filePath+"/"+fileName);
            if ( !file.exists() ) file.createNewFile();

            //Create a stream to file path
            FileOutputStream outPutStream = new FileOutputStream(file, append);
            outPutStream.write(data);
            //Clear Stream
            outPutStream.flush();
            //Terminate STream
            outPutStream.close();
        }
        catch (Exception e) {
            LogWrapper.e("FileUtil", "bufferToFile error WriteFile:"+e.toString());
            ret = false;
        }

        return ret;

    }

    public static boolean stringToFile(String filePath, String fileName, String data, boolean append) {
        boolean ret = true;
        try {
            File parent = new File(filePath);
            if (!parent.exists()) {
                parent.mkdirs();
            }

            File file = new File(filePath+"/"+fileName);
            if ( !file.exists() ) file.createNewFile();

            //Create a stream to file path
            FileOutputStream outPutStream = new FileOutputStream(file, append);
            //Create Writer to write STream to file Path
            OutputStreamWriter outPutStreamWriter = new OutputStreamWriter(outPutStream);
            // Stream Byte Data to the file
            outPutStreamWriter.append(data);
            //Close Writer
            outPutStreamWriter.close();
            //Clear Stream
            outPutStream.flush();
            //Terminate STream
            outPutStream.close();
        }
        catch (Exception e) {
            LogWrapper.e("FileUtil", "stringToFile error WriteFile:"+e.toString());
            ret = false;
        }

        return ret;
    }


    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    public static String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }

    public static void pastDateLogRemove(String filePath, String fileExt, int days) {
        Calendar cal  = Calendar.getInstance();
        try {
            cal.add(Calendar.DATE, -1*days);

            String timeStamp = new SimpleDateFormat("yyyyMMdd").format(cal.getTime());

            File logFile = new File(filePath + "/" + timeStamp + "." + fileExt);

            if (logFile.exists()) {
                logFile.delete();
            }
        }
        catch(Exception e) {
            LogWrapper.e(TAG, "pastDateLogRemove e:"+e.toString());
        }
    }

    public static boolean isDateLogFileExist(String filePath, String fileExt) {
        Date curTime = Calendar.getInstance().getTime();
        String timeStamp = new SimpleDateFormat("yyyyMMdd").format(curTime);

        File logFile = new File(filePath+"/"+timeStamp+"."+fileExt);

        if (logFile.exists()) return true;
        return false;
    }

    public static void appendDateLog(String filePath, String text) {
        appendDateLog(filePath, text, "txt", true);
    }

    public static void appendDateLog(String filePath, String text, String fileExt, boolean isTimeHeader) {
        Date curTime = Calendar.getInstance().getTime();
        String timeStamp = new SimpleDateFormat("yyyyMMdd").format(curTime);

        File parent = new File(filePath);
        if (!parent.exists()) {
            parent.mkdirs();
        }

        File logFile = new File(filePath+"/"+timeStamp+"."+fileExt);

        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                //e.printStackTrace();
                return;
            }
        }

        try
        {
            timeStamp = new SimpleDateFormat("MM/dd HH:mm:ss").format(curTime);

            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            if ( isTimeHeader ) buf.append(timeStamp + " " + text);
            else buf.append(text);

            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {
            //e.printStackTrace();
            return;
        }
    }

    public static boolean serializeToFile(String filePath, String fileName, Object object) {
        boolean ret = true;
        try {
            File parent = new File(filePath);
            if (!parent.exists()) {
                parent.mkdirs();
            }

            File file = new File(filePath+"/"+fileName);
            if ( !file.exists() ) file.createNewFile();

            //Create a stream to file path
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(object);
            oos.close();

            fos.flush();
            fos.close();
        }
        catch (Exception e) {
            LogWrapper.e("FileUtil", "serializeToFile error WriteFile:"+e.toString());
            ret = false;
        }
        return ret;
    }

    public static Object deserializeFromFile(String filePath) {
        Object ret = null;

        try {
            File fl = new File(filePath);
            FileInputStream fin = new FileInputStream(fl);

            ObjectInputStream ois = new ObjectInputStream(fin);
            ret = ois.readObject();

            ois.close();
            //Make sure you close all streams.
            fin.close();
        }
        catch (Exception e) {
            LogWrapper.e("FileUtil", "serializeToFile error WriteFile:"+e.toString());
        }
        return ret;
    }
}
