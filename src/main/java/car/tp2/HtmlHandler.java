package car.tp2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HtmlHandler {
	private Path rootDir;
	
	public HtmlHandler() throws IOException{
		this("resources/html");
	}
	
	public HtmlHandler(String root) throws IOException{
		this.rootDir = Paths.get(root);
		Files.createDirectories(this.rootDir);
	}
	
	public Path getBasePath(){
		return this.rootDir;
	}
	public String render(String filename){
		try {
			String rendering = new String(Files.readAllBytes(Paths.get(rootDir.toFile().getAbsolutePath()+File.separator+filename)));
			System.out.println("Rendering html");
			return rendering;
		} catch (IOException e) {
			System.out.println("[HtmlHandler Render] ERROR");
			e.printStackTrace();
			return 
			"<!doctype html>"
			+ "<html>"
			+ "<head>"
			+ "<meta charset=\"UTF-8\">"
			+ "<title>Passerelle REST</title>"
			+ "</head>"
			+ "<body>"
			+ "<h1>Render Error !</h1>"
			+ "</body>"
			+ "</html>";
		}
	}
}
