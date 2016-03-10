package car.tp2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.http.MediaType;

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
			System.out.println("Connected to " + server + " on port " + serverPort + ".");
			System.out.print(this.ftp.getReplyString());

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

		} catch (IOException e) {
			System.out.println("FTP Connect error: connection refused");
			error = true;
			e.printStackTrace();
		} finally {
			
		}
		return !error;
	}

	@GET
	@Produces("text/html")
	public String sayHello() {
		return "<h1>File Download</h1>\n<p>If you want a file, ask for URL: <strong>/rest/tp2/download/{filename or path}></strong></p>";
	}

	@GET
	@Produces("text/html")
	@Path("/toto")
	public String sayTOTO() {
		try {
		System.out.println("received toto");
		// if (ftp.doCommand("PASV", null)) {
		// System.out.println("pasv ok");
		// int code = this.ftp.getReplyCode();
		// String response = this.ftp.getReplyString();
		// String[] parsed = response.substring(response.indexOf('('),
		// response.indexOf(')')).split(",");
		// int port = Integer.parseInt(parsed[4]) * 256 +
		// Integer.parseInt(parsed[5]);
		// System.out.println("parsed code= " + port);
		// System.out.println("reponse FTP pasv: " + code + " \n\t" +
		// response);
		//
		// } else {
		// System.out.println("pasv raté");
		// }
		System.out.println("avant port " + this.ftp.ACTIVE_LOCAL_DATA_CONNECTION_MODE);
			this.ftp.enterLocalActiveMode();
			this.ftp.enterRemoteActiveMode(this.ftp.getLocalAddress(), this.ftp.getLocalPort());
		System.out.println("après prot " + this.ftp.ACTIVE_LOCAL_DATA_CONNECTION_MODE);
		String response = this.ftp.getReplyString();
		System.out.println("reponse FTP port: " + response);
		System.out.println("list :"+this.ftp.list());
		response = this.ftp.getReplyString();
		System.out.println("reponse FTP list: " + response);
		// if(ftp.doCommand("LIST",null)){
		// System.out.println("ask ok");
		// int code = this.ftp.getReplyCode();
		// String response = this.ftp.getReplyString();
		// System.out.println("reponse FTP: "+code+" \n\t"+response);
		// }
		// else {
		// System.out.println("raté");
		// }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "<h1>Ok</h1>";

	}

	@GET
	@Produces("application/octet-stream")
	@Path("/{filename}")
	public Response getFile(@PathParam("filename") String filename) {
		/*File f = new File("src/main/java/car/tp2/Config.java");
		System.out.println(
				"GET " + (f.exists() ? 200 : 404) + " /rest/tp2/download/" + filename + " -> " + f.getAbsolutePath());*/
		InputStream in;
		try {
			in = this.ftp.retrieveFileStream(filename);
			Response response = Response.ok(in).build();
			return response;
		} catch (IOException e) {
			System.out.print("Erreur lors du téléchargement du fichier " + filename);
		}
       return null;
	}
	
	
	

}
