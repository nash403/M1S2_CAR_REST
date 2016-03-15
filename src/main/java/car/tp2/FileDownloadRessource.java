package car.tp2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.text.SimpleDateFormat;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

public class FileDownloadRessource {

	protected FTPClient ftp;
	protected boolean initialized = false;
	private String basePath = "http://localhost:8080/rest/tp2/";
	private boolean isAuthenticated = false;
	private HtmlHandler html;

	public FileDownloadRessource() {
		try {
			this.initialized = init();
			this.html = new HtmlHandler();
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
		} catch (IOException e) {
			System.out.println("FTP Connect error: connection refused");
			error = true;
			e.printStackTrace();
		} finally {

		}
		return !error;
	}

	private boolean connectToServer(String id, String password) {
		try {
			return this.ftp.login(id, password);
		} catch (IOException e) {
			System.out.println("FTP Connect error: connection refused");
			return false;
		}
	}
	
	@GET
	@Produces("text/html")
	public String displayFileList() {
		return processListFiles(null);
	}

	@GET
	@Produces("text/html")
	@Path("list/{var: .*}")
	public String displayFileList(@PathParam("var") String pathname) {
		return processListFiles(pathname);
	}

	private String processListFiles(String path) {
		System.out.println("[DISPLAY DIR CONTENTS] "+path);
		if (!isAuthenticated) {
			System.out.println("[DISPLAY DIR CONTENTS] not authenticated");
			return generateConnectHTML();
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm");
		String rendered = this.html.render("list.html");
		String response = "";
		response += path == null ? "" : "<tr class='$class'>\n" + "<td>" + "<a href ='" + basePath + "list/" + path + ".."+ "'>..</a></td><td></td></tr>";
		for (FTPFile f : this.getFiles(path)) {

			response += "<tr class='$class'>\n" + "<td>";
			if (f.isFile()) {
				response += "<a href ='" + basePath + "get/" + (path == null ? "" : path) + f.getName() + "'>" + f.getName() + "</a>";
			}
			if (f.isDirectory()) {
					response += "  <a href ='" + basePath + "list/" + f.getName() + "'>" + f.getName() + "</a>";
			}
			response += "</td>";
			response += "<td>" + f.getSize() + "</td>\n" + "</tr>\n";
		}
		rendered = rendered.replace("<tbody></tbody>", "<tbody>" + response + "</tbody>");
		// Upload form.
		String formulaire = this.html.render("upload-form.html");
		formulaire = formulaire.replace("{{basePath}}", this.basePath);
		rendered = rendered.replace("</body>", formulaire + "</body>");

		return rendered;
	}

	private FTPFile[] getFiles(String path) {
		try {
			return path == null ? this.ftp.listFiles() : this.ftp.listFiles(path);
		} catch (IOException e) {
			e.printStackTrace();
			return new FTPFile[] {};
		}
	}

	@GET
	@Produces("application/octet-stream")
	@Path("get/{filename}")
	public Response getFile(@PathParam("filename") String filename) {
		System.out.println("[GET] " + filename);
		if (!isAuthenticated) {
			System.out.println("[GET file] not authenticated");
			return Response.notModified().build();
		}
		InputStream in;
		try {
			System.out.println("[GET file] Start retrieving file stream");
			in = this.ftp.retrieveFileStream(filename);
			Response response = Response.ok(in).build();
			ftp.completePendingCommand();
			System.out.println("[GET file] Sending file to client");
			return response;
		} catch (IOException e) {
			System.out.println("[GET file] Erreur lors du téléchargement du fichier :" + filename);
		}
		return null;
	}

	@GET
	@Produces("application/octet-stream")
	@Path("get/{var: .*}/{filename}")
	public Response getFile(@PathParam("var") String pathname, @PathParam("filename") String filename) {
		System.out.println("[GET] " + pathname + filename);
		if (!isAuthenticated) {
			System.out.println("[GET file] not authenticated");
			return Response.notModified().build();
		}
		InputStream in;
		try {
			System.out.println("[GET file] Start retrieving file stream");
			in = this.ftp.retrieveFileStream(pathname + "/" + filename);
			Response response = Response.ok(in).build();
			ftp.completePendingCommand();
			System.out.println("[GET file] Sending file to client");
			return response;
		} catch (IOException e) {
			System.out.println("[GET file] Erreur lors du téléchargement du fichier :" + filename);
		}
		return null;
	}

	@DELETE
	@Path("/delete/{var: .*}/{fileName}")
	@Produces("text/html")
	public String deleteFile(@PathParam("var") String pathname, @PathParam("fileName") String fileName) {

		try {
			ftp.deleteFile(pathname + "/" + fileName);
			String msg = "<h1>File deletion</h1>\n";
			msg += "<p>The file " + pathname + "/" + fileName + " has been deleted.</p>";
			return msg;
		} catch (IOException e) {
			System.out.println("Echec de la supression du fichier " + pathname + "/" + fileName);
		}
		return null;
	}

	@DELETE
	@Path("/delete/{fileName}")
	@Produces("text/html")
	public String deleteFile(@PathParam("fileName") String fileName) {
		try {
			ftp.deleteFile(fileName);
			String msg = "<h1>File deletion</h1>\n";
			msg += "<p>The file " + fileName + " has been deleted.</p>";
			return msg;
		} catch (IOException e) {
			System.out.println("Echec de la supression du fichier " + fileName);
		}
		return null;
	}

	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces("text/html")
	public String upload(@FormDataParam("file") InputStream file,
			@FormDataParam("file") FormDataContentDisposition fileDetail) {
		try {
			ftp.storeFile("test.txt", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "<h1>File correctly uploaded !</h1>\n";
	}

	@POST
	@Path("/upload/{var: .*}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces("text/html")
	public String upload(@FormDataParam("file") InputStream file,
			@FormDataParam("file") FormDataContentDisposition fileDetail, @PathParam("var") String pathname) {
		try {
			ftp.storeFile(pathname + "/test.txt", file);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "<h1>File correctly uploaded !</h1>\n";
	}

	@GET
	@Path("/connect")
	@Produces("text/html")
	public String connect() {
		System.out.println("[CONNECT] formulaire de connection");
		return generateConnectHTML();
	}

	@POST
	@Path("/connect")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces("text/html")
	public String connect(@FormParam("id") String id, @FormParam("password") String password) {
		System.out.print("[CONNECT] formulaire de connection");
		if (connectToServer(id, password)) {
			isAuthenticated = true;
			System.out.println("[CONNECT OK] Connected as login: " + id + ", pass: " + password);
			return "<h1>You are connected !</h1>\n";
		} else {
			System.out.println("[CONNECT KO]");
			return "<h1>Connection failed</h1>\n";
		}
	}

	public String generateConnectHTML() {
		System.out.println("[CONNECT HTML]");
		return this.html.render("connect.html");
	}
}
