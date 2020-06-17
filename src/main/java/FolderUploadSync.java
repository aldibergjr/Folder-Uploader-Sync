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
import java.nio.file.Files;
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
        
        getFolder(service);
    }
    private static File checkFolder(Drive driveService, String path) throws IOException, GeneralSecurityException{
        java.io.File auxFile = new java.io.File(path);
        FileList searchFolder = driveService.files().list()
                .setQ("name = '"+auxFile.getName()+ "'")
                .execute();
        List<File> folders = searchFolder.getFiles();
        if(folders.isEmpty())
            return null;
        else
            return folders.get(0);
    }
    private static void getFolder(Drive driveService) throws IOException, GeneralSecurityException{
        String path = "C:/Users/Berg/SyncFolder";
        java.io.File directoryPath = new java.io.File(path);
        File folder = checkFolder(driveService, path);
        if(directoryPath.exists())
        {
            String folderName = directoryPath.getName();
            if(folder == null)
            {
                File folderMetaData = new File();
                folderMetaData.setName(folderName);
                folderMetaData.setMimeType("application/vnd.google-apps.folder");
        
                File file = driveService.files().create(folderMetaData)
                    .setFields("id")
                    .execute();
                String folderId = file.getId();
                
                System.out.println("Pasta criada para ser acompanhada no drive");
                sendFiles(driveService, directoryPath.listFiles(), folderId);
            }else{
                //Auto sync ? 
                doSync(driveService, folder, path);
                System.out.println("A pasta já existe");
            }
            
        }else{
            System.out.println("Diretório inválido");
        }
    }
    private static void sendFiles(Drive driveService, java.io.File[] contents, String parentFolder) throws IOException, GeneralSecurityException{
        //Get all files in folder 
        for(java.io.File fileMetadata : contents){
           // FileContent mediaContent = new FileContent(fileMetadata., file)
           File fileMetaFile = new File();
           fileMetaFile.setName(fileMetadata.getName());
           //Get mime/type
           FileContent mediaContent = new FileContent(Files.probeContentType(fileMetadata.toPath()),  fileMetadata);
           fileMetaFile.setParents(Collections.singletonList(parentFolder));
           File file = driveService.files().create(fileMetaFile, mediaContent).setFields("id, parents").execute();
           System.out.println("Sent file : " + file.getName());
        }
    }
    public static void doSync(Drive driveService, File folderRef, String path) throws IOException, GeneralSecurityException{
        java.io.File localFolder = new java.io.File(path);
        String parentId = folderRef.getId();
        FileList result = driveService.files().list().setQ("parents='"+parentId+"'").execute();
        List<File> filesDrive = result.getFiles();
        java.io.File[] filesLocal = localFolder.listFiles();
        boolean[] filesSync = new boolean[filesDrive.size()]; 
        //Look for changes -> 
        
        for(java.io.File local : filesLocal){
            boolean found = false;
            int auxI = 0;
            for(File remote : filesDrive){
                if(remote.getName().equals(local.getName())){
                    //https://developers.google.com/drive/api/v2/reference/files/update
                    System.out.println("checking for file modification for file:  " + remote.getName());
                    found = true;
                    filesSync[auxI] = true;
                    break;
                }
                ++auxI;
            }
            if(!found){
                System.out.println("New file to be added: " + local.getName());
                //filesSync[auxI] = true;
            }
           
        }
        for (int i = 0; i < filesSync.length; i++) {
            if(!filesSync[i]){
                File removed = filesDrive.get(i);
                System.out.println("removed/renamed file : " + removed.getName());
            }
        }
    }
}