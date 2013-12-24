package no.werner.walletgen;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.params.MainNetParams;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.codec.PngImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;


public class App {
    private static Font titleFont = new Font(Font.FontFamily.TIMES_ROMAN, 36, Font.BOLD);
    private static Font headerFont = new Font(Font.FontFamily.TIMES_ROMAN, 20, Font.BOLD);
    private static Font hexFont = new Font(Font.FontFamily.COURIER, 16, Font.NORMAL, BaseColor.RED);
    private static Font warningHeaderFont = new Font(Font.FontFamily.TIMES_ROMAN, 20, Font.BOLD | Font.UNDERLINE, BaseColor.RED);
    private static Font warningTextFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL);

    public static void main(String[] args) {
        try {
            ECKey key = new ECKey();

            String address = key.toAddress(MainNetParams.get()).toString();
            String privateKey = key.getPrivateKeyEncoded(MainNetParams.get()).toString();

            Document document = new Document();
            document.setPageSize(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream("./paperwallet.pdf"));
            document.open();
            addQRCodes(document, address, privateKey);
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addQRCodes(Document document, String address, String privateKey)
            throws DocumentException, IOException, WriterException {

        addTextToDocumentWithFont(document, "Bitcoin Paper wallet", titleFont);
        addTextToDocumentWithFont(document, " ", headerFont);
        addTextToDocumentWithFont(document, "Bitcoin address", headerFont);
        addTextToDocumentWithFont(document, address, hexFont);

        Image addressImage = PngImage.getImage(createQRImage(address, 150));
        document.add(addressImage);

        addTextToDocumentWithFont(document, "Private key", headerFont);
        addTextToDocumentWithFont(document, privateKey, hexFont);

        Image privateKeyImage = PngImage.getImage(createQRImage(privateKey, 150));
        document.add(privateKeyImage);

        addTextToDocumentWithFont(document, " ", headerFont);
        addTextToDocumentWithFont(document, "Warning!", warningHeaderFont);
        addTextToDocumentWithFont(document, "Please fold this paper wallet after printing it, and put it in an envelope, for added security.  If you printed this wallet on a public printer in an internet cafe or similar, it may be compromised!  You should never use a paper wallet that you did not generate yourself.  You should never transfer big amounts to an address on a paper wallet generated with software you do not trust.", warningTextFont);

        document.newPage();
    }

    private static void addTextToDocumentWithFont(Document document, String text, Font font) throws DocumentException {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new Paragraph(text, font));
        document.add(paragraph);
    }

    public static byte[] createQRImage(String text, int size) throws WriterException, IOException {
        Hashtable hintMap = new Hashtable();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix byteMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hintMap);
        BufferedImage qrImage = new BufferedImage(byteMatrix.getWidth(), byteMatrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        qrImage.createGraphics();

        Graphics2D g2d = (Graphics2D) qrImage.getGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, byteMatrix.getWidth(), byteMatrix.getHeight());
        g2d.setColor(Color.BLACK);

        for (int x = 0; x < byteMatrix.getWidth(); x++) {
            for (int y = 0; y < byteMatrix.getHeight(); y++) {
                if (byteMatrix.get(x, y)) {
                    g2d.fillRect(x, y, 1, 1);
                }
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "png", outputStream);
        outputStream.flush();
        byte[] result = outputStream.toByteArray();
        outputStream.close();

        return result;
    }
} 