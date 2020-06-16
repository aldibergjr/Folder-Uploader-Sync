import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.client.http.FileContent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class FolderUploadSync {
    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = FolderUploadSync.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();

        sendFiles(service);

        
        // // Print the names and IDs for up to 10 files.
        // FileList result = service.files().list()
        //         .setPageSize(10)
        //         .setFields("nextPageToken, files(id, name)")
        //         .execute();
        // List<File> files = result.getFiles();
        // if (files == null || files.isEmpty()) {
        //     System.out.println("No files found.");
        // } else {
        //     System.out.println("Files:");
        //     for (File file : files) {
        //         System.out.printf("%s (%s)\n", file.getName(), file.getId());
        //     }
        // }
    }
    
    private static void sendFiles(Drive driveService) throws IOException, GeneralSecurityException{
        //File.listFiles() https://www.tutorialspoint.com/how-to-get-list-of-all-files-folders-from-a-folder-in-java#:~:text=The%20ListFiles()%20method,file%2Fdirectory%20in%20a%20folder.
        //Create folder and get id - > https://developers.google.com/drive/api/v3/folder
        //TODO -> CREATE FOLDER , INSERT INTO FOLDER , LIST FILES IN FOLDER -> INSERTION AND DELETION 
        //TODO-> REUPLOAD BY DATE MODIFIED    
        File fileMetadata = new File();
        fileMetadata.setName("photo.jpg");
        String i = "";
        java.io.File filePath = new java.io.File(FolderUploadSync.class.getResource("/zaragoza.jpg").getPath());
        try{
            FileContent mediaContent = new FileContent("image/jpeg", filePath);
            File file = driveService.files().create(fileMetadata, mediaContent)
            .setFields("id")
            .execute();
            System.out.println("File ID: " + file.getId());
            i = file.getId();
            
        }catch(Exception e){
            System.out.println(e.getMessage());
        }finally{
            // // Print the names and IDs for up to 10 files.
            FileList result = driveService.files().list()
                    .setQ(" name = 'photo.jpg'")
                    
                    .setFields("*")
                    .execute();
            List<File> files = result.getFiles();
            if (files == null || files.isEmpty()) {
                System.out.println("No files found.");
            } else {
                System.out.println("Files:");
                for (File file : files) {
                    //https://developers.google.com/drive/api/v2/reference/files/update
                    //Sync by modified time. deletion + insertion by simply checking listFiles
                    System.out.printf("%s (%s)\n", file.getModifiedTime(), file.getId());
                }
            }
        }

    }
}