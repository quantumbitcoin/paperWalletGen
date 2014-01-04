package no.werner.walletgen;

import at.archistar.crypto.SecretSharing;
import at.archistar.crypto.ShamirPSS;
import at.archistar.crypto.WeakSecurityException;
import at.archistar.crypto.data.Share;
import at.archistar.helper.ShareSerializer;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Base58;
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
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.List;


public class App {
    private static final int NUMBER_OF_SHARED_SECRETS = 4;
    private static final int MINIMUM_NUMBER_OF_SHARED_SECRETS_NEEDED = 3;
    private static final SecretSharing ALGORITHM = new ShamirPSS(NUMBER_OF_SHARED_SECRETS, MINIMUM_NUMBER_OF_SHARED_SECRETS_NEEDED, new SecureRandomSource());
    private static final Font titleFont = new Font(Font.FontFamily.TIMES_ROMAN, 36, Font.BOLD);
    private static final Font headerFont = new Font(Font.FontFamily.TIMES_ROMAN, 20, Font.BOLD);
    private static final Font hexFont = new Font(Font.FontFamily.COURIER, 16, Font.NORMAL, BaseColor.RED);
    private static final Font warningHeaderFont = new Font(Font.FontFamily.TIMES_ROMAN, 20, Font.BOLD | Font.UNDERLINE, BaseColor.RED);
    private static final Font warningTextFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL);

    public static void main(String[] args) throws Exception {
        Scanner keyboard = new Scanner(System.in);
        System.out.println("1. Create paper wallet");
        System.out.println("2. Restore wallet from shared secrets");

        int key = keyboard.nextInt();
        
        if (key == 1) {
            generateDocuments(generateAddressAndPrivateKey());
        } else {
            keyboard = new Scanner(System.in);
            System.out.println("Enter first secret");
            String secret1 = keyboard.next();
            System.out.println("Enter second secret");
            String secret2 = keyboard.next();
            System.out.println("Enter third secret");
            String secret3 = keyboard.next();

            generateDocuments(combineSharedSecrets(new String[] {secret1, secret2, secret3}));
        }
    }

    private static Map<String, String> combineSharedSecrets(String[] sharedSecrets) throws IllegalStateException {
        List<Share> sharedSecretList = new ArrayList<Share>();

        try {
            for (String sharedSecret : sharedSecrets) {
                sharedSecretList.add(ShareSerializer.deserializeShare(Base58.decode(sharedSecret)));
            }

            Share[] sharedSecretArray = sharedSecretList.toArray(new Share[sharedSecretList.size()]);

            if ((sharedSecretArray.length < MINIMUM_NUMBER_OF_SHARED_SECRETS_NEEDED)) {
                throw new IllegalStateException(String.format("Found only %d secrets, where a minimum number of %d was expected", sharedSecretArray.length, MINIMUM_NUMBER_OF_SHARED_SECRETS_NEEDED));
            }

            if ((sharedSecretArray.length > NUMBER_OF_SHARED_SECRETS)) {
                throw new IllegalStateException(String.format("Found %d secrets, where only %d was expected", sharedSecretArray.length, NUMBER_OF_SHARED_SECRETS));
            }

            String data = new String(ALGORITHM.reconstruct(sharedSecretArray));

            String address = data.split(";")[0];
            String privateKey = data.split(";")[1];

            new Address(MainNetParams.get(), address);

            Map<String, String> result = new HashMap<String, String>();
            result.put("address", address);
            result.put("privateKey", privateKey);

            return result;
        } catch (AddressFormatException ex) {
            throw new IllegalStateException("Data corruption");
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Data corruption");
        }
    }

    private static Map<String, String> generateAddressAndPrivateKey() {
        Map<String, String> result = new HashMap<String, String>();

        ECKey key = new ECKey();
        result.put("address", key.toAddress(MainNetParams.get()).toString());
        result.put("privateKey", key.getPrivateKeyEncoded(MainNetParams.get()).toString());

        return result;
    }

    private static void generateDocuments(Map<String, String> addressAndPrivateKey) {
        try {
            String address = addressAndPrivateKey.get("address");
            String privateKey = addressAndPrivateKey.get("privateKey");

            Document walletDocument = new Document();
            walletDocument.setPageSize(PageSize.A4);
            PdfWriter.getInstance(walletDocument, new FileOutputStream("./paperwallet.pdf"));
            walletDocument.open();
            addWalletQRCodes(walletDocument, address, privateKey);
            walletDocument.close();

            Document sharedSecretDocument = new Document();
            sharedSecretDocument.setPageSize(PageSize.A4);
            PdfWriter.getInstance(sharedSecretDocument, new FileOutputStream("./secrets.pdf"));
            sharedSecretDocument.open();
            addSharedSecretQRCodes(sharedSecretDocument, address, getSharedSecrets(address, privateKey));
            sharedSecretDocument.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Share[] getSharedSecrets(String address, String privateKey) throws GeneralSecurityException, WeakSecurityException {
        return ALGORITHM.share(String.format("%s;%s", address, privateKey).getBytes());
    }

    private static void addWalletQRCodes(Document document, String address, String privateKey)
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

    private static void addSharedSecretQRCodes(Document document, String address, Share shares[])
            throws DocumentException, IOException, WriterException {

        int n = 0;
        for (Share share : shares) {
            n++;
            addTextToDocumentWithFont(document, String.format("Shared secret %d of %d for Bitcoin address", n, shares.length), headerFont);
            addTextToDocumentWithFont(document, address, hexFont);
            String secret = getSharedSecretAsBase58(share);
            Image secretImage = PngImage.getImage(createQRImage(secret, 500));
            document.add(secretImage);
            document.newPage();
        }
    }

    private static String getSharedSecretAsBase58(Share share) {
        return Base58.encode(ShareSerializer.serializeShare(share));
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