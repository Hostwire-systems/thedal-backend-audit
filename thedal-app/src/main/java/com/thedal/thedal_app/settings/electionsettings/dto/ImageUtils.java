//package com.thedal.thedal_app.settings.electionsettings.dto;
//
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.util.Base64;
//
//import javax.imageio.ImageIO;
//
//public class ImageUtils {
//	
//	public static boolean isValidBase64Image(String base64Image) {
//        try {
//            byte[] decodedBytes = Base64.getDecoder().decode(base64Image);
//            BufferedImage image = ImageIO.read(new ByteArrayInputStream(decodedBytes));
//            return image != null;
//        } catch (IllegalArgumentException | IOException e) {
//            return false;
//        }
//    }
//
//}
