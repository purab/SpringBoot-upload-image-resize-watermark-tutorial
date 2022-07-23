package in.purabtech.imagewatermark;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileStorageService {

    @Value("${upload.path}")
    private String uploadPath;

    private String uploadOriginalPath = "uploads/users";

    private String uploadCroppedPath =  "uploads/users";

    // overlay settings
    private String text = "\u00a9 purabtech.in";

    @PostConstruct
    public void init() {
        try {
            System.out.println("################# "+uploadOriginalPath);
            System.out.println("################# "+uploadCroppedPath);
            Files.createDirectories(Paths.get(uploadPath));
            Files.createDirectories(Paths.get(uploadOriginalPath));
            Files.createDirectories(Paths.get(uploadCroppedPath));
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload folder!");
        }
    }

    private String getExtension(File file)
    {
        String fileName = file.getName();
        String[] ext = fileName.split("\\.");
        return ext[ext.length -1];
    }

    public ResponseEntity<ResponseMessage> store(MultipartFile file) throws IOException {
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            Path root = Paths.get(uploadOriginalPath);
            if (!Files.exists(root)) {
                init();
            }

            String fileExt = getExtension(new File(fileName));

            UUIDGenerator uuidGenerator = new UUIDGenerator();
            String newFileName = uuidGenerator.generateType5UUID(fileName) + "."+ fileExt;
            String newCroppedFileName = uuidGenerator.generateType5UUID(fileName) + "_cropped."+ fileExt;
            String newResizedFileName = uuidGenerator.generateType5UUID(fileName) + "_resized."+ fileExt;

            //check file exits then delete it
            File existingFile = new File(uploadOriginalPath + "/" + newFileName);

            if (existingFile.exists() && existingFile.isFile()) {
                existingFile.delete();
            }

            Files.copy(file.getInputStream(), root.resolve(newFileName));

            System.out.println("################# this is file ext:"+fileExt);

            //file.transferTo(Paths.get("/uploads/" + file.getOriginalFilename()));
            File input = new File(uploadOriginalPath+"/"+newFileName);
            File output = new File(uploadCroppedPath+"/"+newCroppedFileName);
            File outputCropped = new File(uploadCroppedPath+"/"+newResizedFileName);

            // adding text as overlay to an image
            addTextWatermark(text, fileExt, input, output);
            /////end watermark image

            //resize image and save to path --start resize
            BufferedImage img = null;
            try {
                img = ImageIO.read(input); // eventually C:\\ImageTest\\pic2.jpg
            } catch (IOException e) {
                e.printStackTrace();
            }
            BufferedImage resized_image = resizeImage(img,50,50);
            ImageIO.write(resized_image, "png", outputCropped);
            ////end resize image

            String message = "";
            message = "Uploaded the file successfully: " + file.getOriginalFilename();
            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
        } catch (Exception e) {
            throw new RuntimeException("Could not store the file. Error: " + e.getMessage());
        }
    }

    private static void addTextWatermark(String text, String type, File source, File destination) throws IOException {
        BufferedImage image = ImageIO.read(source);

        // determine image type and handle correct transparency
        int imageType = "png".equalsIgnoreCase(type) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage watermarked = new BufferedImage(image.getWidth(), image.getHeight(), imageType);

        // initializes necessary graphic properties
        Graphics2D w = (Graphics2D) watermarked.getGraphics();
        w.drawImage(image, 0, 0, null);
        AlphaComposite alphaChannel = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
        w.setComposite(alphaChannel);
        w.setColor(Color.GRAY);
        w.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        FontMetrics fontMetrics = w.getFontMetrics();
        Rectangle2D rect = fontMetrics.getStringBounds(text, w);

        // calculate center of the image
        int centerX = (image.getWidth() - (int) rect.getWidth()) / 2;
        int centerY = image.getHeight() / 2;

        // add text overlay to the image
        w.drawString(text, centerX, centerY);
        ImageIO.write(watermarked, type, destination);
        w.dispose();
    }

    BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();
        return resizedImage;
    }

}
