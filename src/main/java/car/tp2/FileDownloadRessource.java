package car.tp2;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.glassfish.jersey.media.multipart.FormDataParam;

import sun.misc.BASE64Decoder;

// base path is "http://localhost:8080/rest/tp2/"
public class FileDownloadRessource {

	protected FTPClient ftp;
	protected boolean initialized = false;
	private String basePath = "http://localhost:8080/rest/tp2/";
	private boolean isAuthenticated = false;
	private String user = "";
	private HtmlHandler html;

	@Context
	private HttpServletRequest request;

	@Context
	private HttpServletResponse response;

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

	private boolean isConnected() {
		String decoded;
		try {
			// Get the Authorisation Header from Request
			String header = request.getHeader("authorization");

			if (header == null) {
				System.out.println("[CONNECT KO] no auth headers");
				isAuthenticated = false;
				response.setHeader("WWW-Authenticate", "Basic realm=\"HAHA\"");
				return false;
			}
			// Header is in the format "Basic 3nc0dedDat4"
			// We need to extract data before decoding it back to original
			// string
			String data = header.substring(header.indexOf(" ") + 1);

			// Decode the data back to original string
			byte[] bytes = new BASE64Decoder().decodeBuffer(data);
			decoded = new String(bytes);

			String[] id_pass = decoded.split(":");
			String id = id_pass[0];
			String pass = id_pass[1];
			if (!isAuthenticated && connectToServer(id, pass)) {
				isAuthenticated = true;
				user = id;
				System.out.println("[CONNECT OK] Connected as login: " + id + ", pass: " + pass);
				return true;
			} else {
				if (!isAuthenticated)
					System.out.println("[CONNECT KO] login/pass error");
					response.setHeader("WWW-Authenticate", "Basic realm=\"HAHA\"");
				return isAuthenticated;
			}

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("[CONNECT KO] ERROR");
			isAuthenticated = false;
			response.setHeader("WWW-Authenticate", "Basic realm=\"HAHA\"");
			return false;
		}
	}

	@GET
	@Produces("text/html")
	public String displayFileList() {
		if (!isConnected()) {
			System.out.println("[HOME] not authenticated");
			response.setStatus(401);
			return generateErrorConnectHTML();
		}
		return processListFiles(null);
	}

	@GET
	@Produces("text/html")
	@Path("list/{var: .*}")
	public String displayFileList(@PathParam("var") String pathname) {
		if (!isConnected()) {
			System.out.println("[LIST] not authenticated");
			response.setStatus(401);
			return generateErrorConnectHTML();
		}
		return processListFiles(pathname);
	}

	private String processListFiles(String path) {
		System.out.println("[LIST] " + path);
		String rendered = this.html.render("list.html");
		String response = "";
		
		if(path.length() > 0 && path.lastIndexOf("/") == path.length()-1){
			path = path.substring(0, path.length()-1);
		}
		 
		for (FTPFile f : this.getFiles(path)) {

			if(f.getName().equals("..")){
				if(!path.equals("")){
					response += "<tr class='$class'>\n" + "<td>";
					response += "  <a href ='" + basePath + "list" + (path.equals("") ? "/" : "/" + path + "/") + f.getName() + "'>" + f.getName() + "</a>";
					response += "</td>";
					response += "<td> - </td>";
				}
			}else if(!f.getName().equals(".")){
				response += "<tr class='$class'>\n" + "<td>";
				if (f.isFile()) {
					response += "<a href ='" + basePath + "get" + (path == null ? "" : (path.equals("") ? "/" : "/" + path + "/")) + f.getName() + "'>"
							+ f.getName() + "</a>";
					response += "</td>";
					response += "<td>" + f.getSize() + "</td>";
				}
				if (f.isDirectory()) {
					response += "  <a href ='" + basePath + "list" + (path.equals("") ? "/" : "/" + path + "/") + f.getName() + "'>" + f.getName() + "</a>";
					response += "</td>";
					response += "<td> - </td>";
				}
				
				response += "<td><a id='"+f.getName()+"' class='line' href='javascript:void(0)' data-chemin='" + basePath + "del/"
						+ (path == null ? "" : (path.equals("") ? "" : path + "/")) + f.getName() + "' onclick=\"del('"+f.getName()+"')\">Delete</a></td></tr>\n";
			}
		}
		rendered = rendered.replace("{{user}}", this.user).replace("<tbody></tbody>",
				"<tbody>" + response + "</tbody>");
		// Upload form.
		String formulaire = this.html.render("upload-form.html");
		formulaire = formulaire.replaceAll("basePath", this.basePath).replaceAll("filepath", path == null ? "" : path + "/");
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
		if (!isConnected()) {
			System.out.println("[GET] not authenticated");
			response.setStatus(401);
			return Response.notModified().build();
		}
		InputStream in;
		try {
			System.out.println("[GET] Start retrieving file stream");
			in = this.ftp.retrieveFileStream(filename);
			Response response = Response.ok(in).build();
			ftp.completePendingCommand();
			System.out.println("[GET] Sending file to client");
			return response;
		} catch (IOException e) {
			System.out.println("[GET] Erreur lors du téléchargement du fichier :" + filename);
		}
		return null;
	}

	@GET
	@Produces("application/octet-stream")
	@Path("get/{var: .*}/{filename}")
	public Response getFile(@PathParam("var") String pathname, @PathParam("filename") String filename) {
		System.out.println("[GET] " + pathname + filename);
		if (!isConnected()) {
			System.out.println("[GET] not authenticated");
			response.setStatus(401);
			return Response.notModified().build();
		}
		InputStream in;
		try {
			System.out.println("[GET] Start retrieving file stream");
			in = this.ftp.retrieveFileStream(pathname + "/" + filename);
			Response response = Response.ok(in).build();
			ftp.completePendingCommand();
			System.out.println("[GET] Sending file to client");
			return response;
		} catch (IOException e) {
			System.out.println("[GET] Erreur lors du téléchargement du fichier :" + filename);
		}
		return null;
	}

	@DELETE
	@Path("/del/{var: .*}/{fileName}")
	@Produces("text/html")
	public String deleteFile(@PathParam("var") String pathname, @PathParam("fileName") String fileName) {
		System.out.println("[DELETE] " + fileName);
		if (!isConnected()) {
			System.out.println("[DELETE] not authenticated");
			response.setStatus(401);
			return generateErrorConnectHTML();
		}
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
	@Path("/del/{fileName}")
	@Produces("text/html")
	public String deleteFile(@PathParam("fileName") String fileName) {
		System.out.println("[DELETE] " + fileName);
		if (!isConnected()) {
			System.out.println("[DELETE] not authenticated");
			response.setStatus(401);
			return generateErrorConnectHTML();
		}
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
	@Path("/upload/{var: .*}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces("text/html")
	public String upload(@FormDataParam("file") InputStream file, @PathParam("var") String pathname) {
		System.out.println("[UPLOAD] " + pathname);
		if (!isConnected()) {
			System.out.println("[UPLOAD] not authenticated");
			response.setStatus(401);
			return generateErrorConnectHTML();
		}
		try {
			ftp.storeFile(pathname, file);
		} catch (IOException e) {
			System.out.println("[UPLOAD ERROR] " + pathname);
			e.printStackTrace();
			return "<h1>Error while storing file !</h1>\n";
		}
		return "<h1>File correctly uploaded !</h1>\n";
	}

	public String generateErrorConnectHTML() {
		return "<h1>401 UnAuthorized !</h1>";
	}
}
