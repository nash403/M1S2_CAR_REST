package car.tp2;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

@Path("/download")
public class FileDownloadRessource {

  protected FTPClient ftp;
  protected boolean initialized = false;

  public FileDownloadRessource() {
    try {
      this.initialized = init();
    } catch (Exception e) {
      this.initialized = false;
      e.printStackTrace();
    }
  }

  public boolean init() throws SocketException, IOException {
    this.ftp = new FTPClient();
    boolean error = false;
    try {
      int reply;
      String server = "localhost";
      int serverPort = 1111;
      this.ftp.connect(server, serverPort);
      System.out.print(this.ftp.getReplyString());
      System.out.println("Connected to " + server + " on port "
      + serverPort + ".");

      // After connection attempt, you should check the reply code to
      // verify
      // success.
      reply = this.ftp.getReplyCode();

      if (!FTPReply.isPositiveCompletion(reply)) {
        this.ftp.disconnect();
        System.err.println("FTP server refused connection.");
        return false;
      }
      this.ftp.login("Bobby", "123");
      System.out.print(this.ftp.getReplyString());
      // transfer files
      // ftp.logout();
    } catch (IOException e) {
      System.out.println("FTP Connect error: connection refused");
      error = true;
      // e.printStackTrace();
    }
    // } finally {
    // if (ftp.isConnected()) {
    // try {
    // ftp.disconnect();
    // } catch (IOException ioe) {
    // // do nothing
    // }
    // }
    // }
    return !error;
  }

  @GET
  @Produces("text/html")
  public String sayHello() {
    return "<h1>File Download</h1>\n<p>If you want a file, ask for URL: <strong>/rest/tp2/download/{filename or path}></strong></p>";
  }

  @GET
  @Produces("application/octet-stream")
  @Path("/{filename}")
  public File getFile(@PathParam("filename") String filename) {
    File f = new File("src/main/java/car/tp2/Config.java");
    System.out.println("GET " + (f.exists() ? 200 : 404)
    + " /rest/tp2/download/" + filename + " -> "
    + f.getAbsolutePath());
    return f;
  }

}
