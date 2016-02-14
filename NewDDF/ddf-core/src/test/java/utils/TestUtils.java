package utils;

import org.apache.commons.io.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by sangdn on 1/5/16.
 */
public class TestUtils {
    public static final String TAB_SEPARATOR = String.valueOf('\t');
    public static final String COMMA_SEPARATOR = ",";



    public static boolean makeCSVFileUserInfo(String fileName, int numOfLine, String delimiter) {
        File fileUser = new File(fileName);
        if (fileUser.exists()) fileUser.delete();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileUser))) {

            for (int i = 0; i < numOfLine; ++i) {
                //user info schema
                //username age isMarried birthday
                bw.write(TestUtils.generateStringUserInfo(delimiter));
                bw.newLine();
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return false;
        }
        return true;
    }

    public static boolean makeJSONFileUserInfo(String fileName, int numOfLine) {
        File fileUser = new File(fileName);
        if (fileUser.exists()) fileUser.delete();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileUser))) {

            for (int i = 0; i < numOfLine; ++i) {
                //user info schema
                //username age isMarried birthday
                bw.write(TestUtils.generateJsonUserInfo());
                bw.newLine();
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            return false;
        }
        return true;
    }



    /**
     * random generate user info
     * username age isMarried birthday
     *
     * @return
     */
    public static String generateStringUserInfo(String delimiter) {
        //birthday: dd-MM-yyyy
        String birthday = "%d-%d-%d";
        String date = String.format(birthday, 1960 + genInt() % 30, genInt() % 12 + 1, genInt() % 30 + 1);
        StringBuilder sb = new StringBuilder();
        sb.append(genString()).append(delimiter)
                .append(genInt()).append(delimiter)
                .append(genBool()).append(delimiter)
                .append(date);
        return sb.toString();

    }

    public static String generateJsonUserInfo() {
        //birthday: dd-MM-yyyy
        String birthday = "%d-%d-%d";
        String date = String.format(birthday, 1960 + genInt() % 30, genInt() % 12 + 1, genInt() % 30 + 1);
        String jsonString = "{\"username\":\"%s\",\"age\":%d,\"isMarried\":\"%s\",\"birthday\":\"%s\"}";

        return String.format(jsonString, genString(), genInt(), String.valueOf(genBool()), date);

    }

    public static String generateParquetUserInfo(String delimiter) {
        //birthday: dd-MM-yyyy
        String birthday = "%d-%d-%d";
        String date = String.format(birthday, 1960 + genInt() % 30, genInt() % 12 + 1, genInt() % 30 + 1);
        StringBuilder sb = new StringBuilder();
        sb.append(genString()).append(delimiter)
                .append(genInt()).append(delimiter)
                .append(genBool()).append(delimiter)
                .append(date);
        return sb.toString();

    }

    public static void deleteFile(String path) {
        try {
            File tmp = new File(path);
            if (tmp.isDirectory()) {
                FileUtils.deleteDirectory(tmp);
            } else {
                if (tmp.exists()) tmp.delete();
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }


    public static int genInt() {
        return Math.abs(ThreadLocalRandom.current().nextInt() % 1000);
    }

    public static long genLong() {
        return Math.abs(ThreadLocalRandom.current().nextLong() % 1000);
    }

    public static List<Long> genListLong() {
        List<Long> list = new ArrayList<Long>();
        int n = genInt() % 10 + 1;
        for (int i = 0; i < n; ++i) {
            list.add((long) genInt());
        }
        return list;
    }

    public static List<String> genListString() {
        List<String> list = new ArrayList<String>();
        int n = genInt() % 10 + 1;
        for (int i = 0; i < n; ++i) {
            list.add(genString());
        }
        return list;
    }

    public static String genString() {
        int n = genInt() % 10 + 1;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < n; ++i) {
            sb.append((char) (65 + genInt() % 25));
        }
        return sb.toString();
    }

    public static boolean genBool() {
        return genInt() % 2 == 0;
    }
}