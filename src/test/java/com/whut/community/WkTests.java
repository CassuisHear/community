package com.whut.community;

import java.io.IOException;

public class WkTests {
    public static void main(String[] args) {
        String cmd = "E:/wkhtmltox/wkhtmltopdf/bin/wkhtmltoimage --quality 75 https://www.nowcoder.com e:/work/data/wk-images/nowcoder.png";
        try {
            Runtime.getRuntime().exec(cmd);
            System.out.println("Ok.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
