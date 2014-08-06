package com.aboo.createlogoqrcode.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.*;
import java.util.Calendar;


public class MainActivity extends Activity {

    private Context mContext;
    // logo图片的一半宽度
    private static final int IMAGE_HALF_WIDTH = 25;
    // 二维码图片大小
    private static final int QRCODE_WIDTH = 300;
    private static final int QRCODE_HEIGHT = 300;
    // 显示二维码图片
    private ImageView mQrcodeImg;
    // 插入到二维码里面的图片对象
    private Bitmap mLogoBitmap;
    // 需要插图图片的大小 这里设定为40*40
    private int[] pixels = new int[2 * IMAGE_HALF_WIDTH * 2 * IMAGE_HALF_WIDTH];
    // 二维码图片保存路径
    private String mAppDir = Environment.getExternalStorageDirectory().getPath() + "/QRCode/";
    private String mSavePath = mAppDir + "/temp_qrcode.jpg";
    private String mAppImgPath = mAppDir + "/Image/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        final EditText content = (EditText) findViewById(R.id.main_content);
        mQrcodeImg = (ImageView) findViewById(R.id.main_qrcode_img);

        createAppDirIfNotExists();

        Button createBtn = (Button) findViewById(R.id.main_create_btn);
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 生成二维码
                try {

                    String qrcodeContentStr = content.getText().toString().trim();
                    if (qrcodeContentStr.length() != 0) {
                        // 二维码内容转码,不然扫描出来的结果是乱码
                        qrcodeContentStr = new String(qrcodeContentStr.getBytes(), "ISO-8859-1");
                        mLogoBitmap = ((BitmapDrawable) getResources().getDrawable(
                                R.drawable.logo)).getBitmap();
                        // 缩放Logo图片
                        Matrix m = new Matrix();
                        float sx = (float) 2 * IMAGE_HALF_WIDTH / mLogoBitmap.getWidth();
                        float sy = (float) 2 * IMAGE_HALF_WIDTH / mLogoBitmap.getHeight();
                        m.setScale(sx, sy);
                        mLogoBitmap = Bitmap.createBitmap(mLogoBitmap, 0, 0, mLogoBitmap.getWidth(),
                                mLogoBitmap.getHeight(), m, false);

                        //显示生成的二维码
                        Bitmap qrcodeBitmap = createQRCode(qrcodeContentStr);

                        saveQRCodeImg(qrcodeBitmap);

                        mQrcodeImg.setImageBitmap(qrcodeBitmap);
                    }
                } catch (Exception e) {
                    Toast.makeText(mContext, "生成0二维码失败", Toast.LENGTH_SHORT).show();
                }
            }
        });

        final Button saveBtn = (Button) findViewById(R.id.main_save_btn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //保存图片到指定路径
                File tempFile = new File(mSavePath);

                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);

                // 将暂存的图片复制到应用文件夹
                try {
                    // 二维码图片保存文件名
                    String fileName = year + "-" + month + "-" + day + "-" + hour + "-" + minute + "-" + second + ".jpg";
                    File saveFile = new File(mAppImgPath + fileName);
                    if (!saveFile.exists()) {
                        saveFile.createNewFile();
                    }

                    if (copyFile(tempFile, saveFile)) {
                        Toast.makeText(mContext, "保存二维码图片成功", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(mContext, "保存二维码图片失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createAppDirIfNotExists() {
        File dir = new File(mAppImgPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * 将二维码图片保存到指定文件夹
     *
     * @param sourceFile
     * @param targetFile
     * @return
     * @throws IOException
     */
    private boolean copyFile(File sourceFile, File targetFile) throws IOException {
        BufferedInputStream inBuff = null;
        BufferedOutputStream outBuff = null;
        try {
            // 新建文件输入流并对它进行缓冲
            inBuff = new BufferedInputStream(new FileInputStream(sourceFile));

            // 新建文件输出流并对它进行缓冲
            outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));

            // 缓冲数组
            byte[] b = new byte[1024 * 5];
            int len;
            while ((len = inBuff.read(b)) != -1) {
                outBuff.write(b, 0, len);
            }
            // 刷新此缓冲的输出流
            outBuff.flush();

            return true;
        } finally {
            // 关闭流
            if (inBuff != null)
                inBuff.close();
            if (outBuff != null)
                outBuff.close();
        }
    }


    /**
     * 将二维码图片暂存起来
     *
     * @param qrcodeBitmap 二维码图片
     * @throws Exception
     */
    private void saveQRCodeImg(Bitmap qrcodeBitmap) throws Exception {
        File f = new File(mSavePath);
        if (!f.exists()) {
            f.createNewFile();
        }

        FileOutputStream fileOut = new FileOutputStream(f);
        qrcodeBitmap.compress(Bitmap.CompressFormat.PNG, 50, fileOut);
        fileOut.flush();
    }

    /**
     * 生成二维码
     *
     * @param content 二维码内容
     * @return Bitmap
     * @throws WriterException
     */
    public Bitmap createQRCode(String content) throws WriterException {

        // 生成二维矩阵,编码时指定大小,不要生成了图片以后再进行缩放,这样会模糊导致识别失败
        BitMatrix matrix = new MultiFormatWriter().encode(content,
                BarcodeFormat.QR_CODE, QRCODE_WIDTH, QRCODE_HEIGHT);
        int width = matrix.getWidth();
        int height = matrix.getHeight();

        // 二维矩阵转为一维像素数组
        int halfW = width / 2;
        int halfH = height / 2;
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x > halfW - IMAGE_HALF_WIDTH && x < halfW + IMAGE_HALF_WIDTH && y > halfH - IMAGE_HALF_WIDTH
                        && y < halfH + IMAGE_HALF_WIDTH) {
                    pixels[y * width + x] = mLogoBitmap.getPixel(x - halfW + IMAGE_HALF_WIDTH, y
                            - halfH + IMAGE_HALF_WIDTH);
                } else {
                    //此处可以修改二维码的颜色，可以分别制定二维码和背景的颜色；
                    int qrcodeColor = 0xff262f22;
                    int qrcodeBgColor = 0xfff1f1f1;
                    pixels[y * width + x] = matrix.get(x, y) ? qrcodeColor : qrcodeBgColor;
                }

            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);

        // 通过像素数组生成bitmap
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        return bitmap;
    }

}
