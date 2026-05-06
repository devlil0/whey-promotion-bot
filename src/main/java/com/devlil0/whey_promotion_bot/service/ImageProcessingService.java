package com.devlil0.whey_promotion_bot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;

@Service
public class ImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessingService.class);

    /**
     * Downloads the image at the given URL and re-encodes it as JPEG at 97% quality,
     * preserving the original dimensions. Transparency is flattened to white.
     *
     * @return processed bytes, or {@code null} if anything goes wrong (caller should fall back to URL)
     */
    public byte[] enhance(String imageUrl) {
        try {
            BufferedImage original = downloadImage(imageUrl);
            if (original == null) return null;
            return encodeToJpeg(flattenAlpha(original));
        } catch (Exception e) {
            log.warn("Falha ao processar imagem {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }

    private BufferedImage downloadImage(String imageUrl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
        conn.setConnectTimeout(5_000);
        conn.setReadTimeout(10_000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        try (InputStream in = conn.getInputStream()) {
            return ImageIO.read(in);
        }
    }

    private BufferedImage flattenAlpha(BufferedImage img) {
        if (img.getTransparency() == Transparency.OPAQUE) return img;
        BufferedImage flat = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = flat.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return flat;
    }

    private byte[] encodeToJpeg(BufferedImage img) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IOException("Nenhum ImageWriter JPEG disponível");
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.97f);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }
}
