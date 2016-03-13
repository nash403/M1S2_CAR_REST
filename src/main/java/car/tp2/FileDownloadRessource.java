package car.tp2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileEntryParser;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.parser.FTPFileEntryParserFactory;
import org.apache.commons.net.ftp.parser.ParserInitializationException;
import org.apache.cxf.helpers.IOUtils;
import org.springframework.http.MediaType;

@Path("/download")
public class FileDownloadRessource {

	protected FTPClient ftp;
	protected boolean initialized = false;
	private String basePath = "http://localhost:8080/rest/tp2/download/";
	
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
		String welcomeMsg = "<h1>File Download</h1>\n" ;
		welcomeMsg += "<p>If you want to download a file, ask for URL: <strong>/rest/tp2/download/{filename or path}</strong></p>";
		welcomeMsg += "<p>If you want to display a file, ask for URL: <strong>/rest/tp2/download/html/{filename or path}</strong></p>";
		return welcomeMsg;
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
	@Path("{filename}")
	public Response getFile(@PathParam("filename") String filename) {
		InputStream in;
		try {
			in = this.ftp.retrieveFileStream(filename);
			Response response = Response.ok(in).build();
			ftp.completePendingCommand();
			return response;
		} catch (IOException e) {
			System.out.print("Erreur lors du téléchargement du fichier :" + filename);
		}
       return null;
	}

	@GET
	@Produces("application/octet-stream")
	@Path("{var: .*}/{filename}")
	public Response getFile(@PathParam("var") String pathname, @PathParam("filename") String filename) {
		InputStream in;
		try {
			in = this.ftp.retrieveFileStream(pathname + "/" + filename);
			Response response = Response.ok(in).build();
			ftp.completePendingCommand();
			return response;
		} catch (IOException e) {
			System.out.print("Erreur lors du téléchargement du fichier :" + filename);
		}
       return null;
	}

	@GET
	@Produces("text/html")
	@Path("/html/{filename}")
	public String updateFile(@PathParam("filename") String filename) {
		InputStream in;
		try {
			in = this.ftp.retrieveFileStream(filename);
			return IOUtils.toString(in, "UTF-8") ;
		} catch (IOException e) {
			System.out.print("Erreur lors du téléchargement et de l'affichage du fichier :" + filename);
		}
       return null;
	}
	
	@GET
    @Path("/delete/{var: .*}/{fileName}")
    @Produces("text/html")
    public String deleteFile(@PathParam("var") String pathname, @PathParam("fileName") String fileName) {
       
		try {
			ftp.deleteFile(pathname + "/" +fileName);
			String msg = "<h1>File deletion</h1>\n" ;
			msg += "<p>The file "+ pathname + "/" + fileName +" has been deleted.</p>";
			return msg;
		} catch (IOException e) {
			System.out.println("Echec de la supression du fichier " + pathname + "/" + fileName);
		}
		return null;
    }
	
	@GET
    @Path("/delete/{fileName}")
    @Produces("text/html")
    public String deleteFile(@PathParam("fileName") String fileName) { 
		try {
			ftp.deleteFile(fileName);
			String msg = "<h1>File deletion</h1>\n" ;
			msg += "<p>The file "+ fileName +" has been deleted.</p>";
			return msg;
		} catch (IOException e) {
			System.out.println("Echec de la supression du fichier " + fileName);
		}
		return null;
    }
	
	@GET
	@Produces("text/html")
	@Path("/application/html")
	public String displayFileList() {
		try {
			String html = "<h1>Server files</h1>\n" ;

			for(FTPFile f : ftp.listFiles()){
				if(!f.getName().equals(".")){
					html += "<p>" ;
					html += "Name : " + f.getName() + " / ";
					html += "Size in bytes : " + f.getSize() + " / ";
					if(f.isFile()){
						html += "  <a href =" + basePath + f.getName()+ ">Download</a>" ;
					}
					if(f.isDirectory()){
						if(f.getName().equals("..")){
							html += "  <a href =" + basePath + "application/html>Open</a>" ;
						}else{
							html += "  <a href =" + basePath + "application/html/" + f.getName()+ ">Open</a>" ;
						}
					}
				}
			}
			
			
			return html;
		} catch (IOException e) {
			System.out.print("Erreur lors de l'affichage de la liste des fichiers du serveur ");
		}
       return null;
	}
	
	@GET
	@Produces("text/html")
	@Path("/application/html/{var: .*}")
	public String displayFileList(@PathParam("var") String pathname) {
		try {
			String html = "<h1>Server files</h1>\n" ;
			if(pathname.length() > 0 && pathname.lastIndexOf("/") == pathname.length()-1){
				pathname = pathname.substring(0, pathname.length()-1);
			}

			for(FTPFile f : ftp.listFiles(pathname)){
				if(!f.getName().equals(".")){
					html += "<p>" ;
					html += "Name : " + f.getName() + " / ";
					html += "Size in bytes : " + f.getSize() + " / ";
					if(f.isFile()){
						html += "  <a href =" + basePath + pathname + "/" + f.getName()+ ">Download</a>" ;
					}
					if(f.isDirectory()){
						if(f.getName().equals("..")){
							html += "  <a href =" + basePath + "application/html/" + pathname + "/" + f.getName()+ ">Open</a>" ;
						}else{
							html += "  <a href =" + basePath + "application/html/" + pathname + "/" + f.getName()+ ">Open</a>" ;
						}
					}
					html += "</p>";
				}
			}
			return html;
		} catch (IOException e) {
			System.out.print("Erreur lors de l'affichage de la liste des fichiers du serveur ");
		}
       return null;
	}
	
	
	

}
