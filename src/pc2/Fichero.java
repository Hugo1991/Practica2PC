package pc2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

public class Fichero{
	public static void descargaFichero(String url,String path){
		URL website;
		try{
			website = new URL(url);
			InputStream in = website.openStream();
			Path pathOut = Paths.get(path);
			Files.copy(in,pathOut,StandardCopyOption.REPLACE_EXISTING);
			in.close();
		}catch(MalformedURLException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void leerFichero(String url){
	}

	public static void mergeFile(String dir,String fileStart){
		File ofile = new File(dir + "/" + fileStart);
		FileOutputStream fos;
		FileInputStream fis;
		byte[] fileBytes;
		int bytesRead = 0;
		String[] files = new File(dir)
					.list((path,name) -> Pattern.matches(fileStart + Pattern.quote(".") + "part.*",name));
		try{
			fos = new FileOutputStream(ofile,true);
			for(String fileName:files){
				File f = new File(dir + "/" + fileName);
				System.out.println(f.getAbsolutePath());
				fis = new FileInputStream(f);
				fileBytes = new byte[(int)f.length()];
				bytesRead = fis.read(fileBytes,0,(int)f.length());
				assert (bytesRead == fileBytes.length);
				assert (bytesRead == (int)f.length());
				fos.write(fileBytes);
				fos.flush();
				fileBytes = null;
				fis.close();
				fis = null;
			}
			fos.close();
			fos = null;
		}catch(Exception exception){
			exception.printStackTrace();
		}
	}

	public void eliminarFicheros(){
	}
}
