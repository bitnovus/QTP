import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;

public class ctpChunker {
    
    private static final int SAFETY_INDEX = 0;
    private static final int ORDER_INDEX = 0+2;
    private static final int COUNT_INDEX = 4+2;
    private static final int SIZE_INDEX = 8+2;
    private static final int DATA_INDEX = 12+2;
    private static final int CHUNK_SIZE = 780+4;
    private static final int DATA_SIZE = 768;
    private static final int SAFETY2_INDEX = 782;

    static BitMatrix qrize(byte[] input) {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = null;
        try {
            String inputTest = Base64.encodeBase64String(input);
            matrix = writer.encode(inputTest, BarcodeFormat.QR_CODE, 768, 768);
        } catch (WriterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }    
        return matrix;
    }
    
    static void draw(BufferedImage input) {
        StdDraw.setCanvasSize(768, 768);
        StdDraw.setScale(0, 1024);
        StdDraw.drawBufferedImage(512, 512, input);
    }
    
    static byte[] unqrize(BufferedImage inputImage) throws IOException {
        BufferedImage image = inputImage;
        BinaryBitmap binaryMap = new BinaryBitmap(new GlobalHistogramBinarizer(new BufferedImageLuminanceSource(image)));
        QRCodeReader reader = new QRCodeReader();
        //draw(inputImage);
        try {
            Hashtable<DecodeHintType, Object> hints = new Hashtable<DecodeHintType, Object>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
            
            Result res = reader.decode(binaryMap, hints);
            String output = res.getText();
            byte[] boutput = Base64.decodeBase64(output);

            return boutput;

        } catch (NotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ChecksumException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (FormatException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    static String test() throws IOException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = null;
        try {
            byte[] test = { 1, 5, 3, 2, 1 };
            String inputTest = Base64.encodeBase64String(test);
            matrix = writer.encode(inputTest, BarcodeFormat.QR_CODE, 256, 256);
        } catch (WriterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }    
        if (matrix != null) {            
            try {
                File file = new File("test.gif");
                MatrixToImageWriter.writeToFile(matrix, "GIF", file);
                System.out.println(file.getAbsolutePath());
                return file.getAbsolutePath().toString();
            }
            catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        return null;
    }

    static void verify(String path) throws IOException {
        BufferedImage image = ImageIO.read(new File(path));
        BinaryBitmap binaryMap = new BinaryBitmap(new GlobalHistogramBinarizer(new BufferedImageLuminanceSource(image)));
        QRCodeReader reader = new QRCodeReader();
        try {
            Result res = reader.decode(binaryMap);
            //byte[] output = Base64.decodeBase64(res.getRawBytes());
            String output = res.getText();
            byte[] boutput = Base64.decodeBase64(output);
            for (byte temp : boutput)
                System.out.print(temp);
            System.out.println();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (ChecksumException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
    }

    private static void prep(int order, int size, byte[] data) {
        //buffer[0] = (value >> 24) & 0xFF;
        //buffer[1] = (value >> 16) & 0xFF;
        //buffer[2] = (value >> 8) & 0xFF;
        //buffer[3] = value & 0xFF;
        
        data[SAFETY_INDEX] = (byte) 1;
        data[SAFETY_INDEX+1] = (byte) 1;

        //data[ORDER_INDEX] = (byte) order;
        data[ORDER_INDEX] = (byte) ((order >> 24) & 0xFF);
        data[ORDER_INDEX+1] = (byte) ((order >> 16) & 0xFF);
        data[ORDER_INDEX+2] = (byte) ((order >> 8) & 0xFF);
        data[ORDER_INDEX+3] = (byte) (order & 0xFF);
        data[SIZE_INDEX] = (byte) ((size >> 24) & 0xFF);
        data[SIZE_INDEX+1] = (byte) ((size >> 16) & 0xFF);
        data[SIZE_INDEX+2] = (byte) ((size >> 8) & 0xFF);
        data[SIZE_INDEX+3] = (byte) (size & 0xFF);
        //data[SIZE_INDEX] = (byte) size;
        data[SAFETY2_INDEX] = (byte) 1;
        data[SAFETY2_INDEX] = (byte) 1;

    }

    private static ArrayList<byte[]> chunkFile(String pathToFile) throws IOException {
        FileInputStream in = null;
        ArrayList<byte[]> alBit = new ArrayList<byte[]>();

        try {
            in = new FileInputStream(pathToFile);
            int temp = 0;
            int size = 1;
            int chunkNumber = 1;
            byte[] qrDump = new byte[CHUNK_SIZE];
            while (temp != -1) {
                if (size >= DATA_SIZE) {
                    prep(chunkNumber, size, qrDump);
                    alBit.add(qrDump);

                    qrDump = new byte[CHUNK_SIZE];
                    size = 1;
                    chunkNumber++;
                }

                temp = in.read();
                if (temp != -1) {
                    qrDump[size + DATA_INDEX - 1] = (byte) temp;
                    size++;
                }
                else { // for last one
                    prep(chunkNumber, size, qrDump);
                    alBit.add(qrDump);
                }
            }
            for (byte[] tempByteArr : alBit)
            {
                tempByteArr[COUNT_INDEX] = (byte) ((chunkNumber >> 24) & 0xFF);
                tempByteArr[COUNT_INDEX+1] = (byte) ((chunkNumber >> 16) & 0xFF);
                tempByteArr[COUNT_INDEX+2] = (byte) ((chunkNumber >> 8) & 0xFF);
                tempByteArr[COUNT_INDEX+3] = (byte) (chunkNumber & 0xFF);
                //tempByteArr[COUNT_INDEX] = (byte) j;
            }

        } finally {
            if (in != null) {
                in.close();
            }
        }
        return alBit;
    }

    public static int toInt(byte[] bytes, int offset) {
        int ret = ((bytes[0+offset] & 0xFF) << 24)
                | ((bytes[1+offset] & 0xFF) << 16)
                | ((bytes[2+offset] & 0xFF) << 8)
                | (bytes[3+offset] & 0xFF);
        return ret;
    }

    private static void unchunkFile(byte[][] input, String pathToFile) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(pathToFile);
            for (byte[] tempArr : input) {
                int size = toInt(tempArr, SIZE_INDEX);
                for (int i = 0; i < size - 1; i++) {
                    out.write(tempArr[DATA_INDEX+i]);
                }
            }

        } finally {
            if (out != null)
                out.close();
        }
    }

    private static byte[][] rebuildFile(ArrayList<byte[]> inputAL) {
        int count = toInt(inputAL.get(0), COUNT_INDEX);
        int size = CHUNK_SIZE;
        byte[][] aggregate = new byte[count][size];
        for (int j = 0; j < count; j++) {
            byte[] tempArr = inputAL.get(j);
            int order = toInt(tempArr, ORDER_INDEX);
            for (int i = 0; i < size; i++)
                aggregate[order-1][i] = tempArr[i];
        }
        return aggregate;
    }

    private static void displaySeries(ArrayList<BufferedImage> alB) {
        /*simplepaint paint = new simplepaint(alB);
        paint.setTitle("CPT");
        paint.pack();
        paint.requestFocusInWindow();
        paint.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);*/
        StdDraw.setCanvasSize(768, 768);
        StdDraw.setScale(0, 1024);
        int i = 0;
        int size = alB.size();
        
        StdDraw.setFont(new Font("SansSerif", Font.PLAIN, 12));

        for (BufferedImage tempImg : alB) {
            i++;
            StdDraw.drawBufferedImage(512, 512, tempImg);
            //StdDraw.text(0, 0, "frame: " + i + "/" + size);
            try {
                Thread.sleep(50);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        i = 0;
    }

    public static void main(String[] args) throws IOException {
        ArrayList<byte[]> tempAL = chunkFile("small.txt");
        
        // Encoding
        ArrayList<BufferedImage> QRSeries = new ArrayList<BufferedImage>();

        for (byte[] tempArr : tempAL) {
            BitMatrix tempM = qrize(tempArr);
            if (tempM != null)
                QRSeries.add(MatrixToImageWriter.toBufferedImage(tempM));
        }
        System.out.println(QRSeries.size());
        displaySeries(QRSeries);

        System.out.println("Done Processing");

        
        // Decoding
        // Rebuilding Here
        ArrayList<byte[]> decodeList = new ArrayList<byte[]>();
        for (BufferedImage tempImage : QRSeries) {
            byte[] tempByteArr = unqrize(tempImage);
            if (tempByteArr != null)
                decodeList.add(tempByteArr);
        }
        
        int expectedNumber = toInt(decodeList.get(0), COUNT_INDEX);
        HashMap<Integer, byte[]> hmChunks = new HashMap<Integer, byte[]>();
        while (hmChunks.size() < expectedNumber)
        {

            int randomIndex = (int) Math.floor((Math.random() * decodeList.size()));
            System.out.println(expectedNumber + " " + hmChunks.size() + " " + randomIndex);
            byte[] tempByteArr = decodeList.get(randomIndex);
            
            int arrID = toInt(tempByteArr, ORDER_INDEX);
            if (!hmChunks.containsKey(arrID))
                hmChunks.put(arrID, tempByteArr);
        }

        ArrayList<byte[]> outOfOrder = new ArrayList<byte[]>();
        for (byte[] temp : hmChunks.values())
            outOfOrder.add(temp);

        byte[][] aggregate = rebuildFile(outOfOrder);
        unchunkFile(aggregate, "verify.txt");
        System.out.println("Fully done.");
    }
}