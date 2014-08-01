package driverdownloader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import com.enterprisedt.net.ftp.FTPConnectMode;
import com.enterprisedt.net.ftp.FileTransferClient;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

/**
 * This is a class with functionality to download both HTML pages
 * and files. Files can be downloaded from both http and ftp links.
 * 
 * It could be made a pure static class, but it would probably be even
 * better to make it an base class, with an abstract method to get an
 * InputStream object from a link, that can be used in the method
 * downloadFileTry. Derived classes can then use different methods to
 * create the InputStream object, which is now done based on the domain
 * in the link.
 * Classes that derive from DriverDownloader could then decide which
 * implementation the use.
 * 
 * However, it is also possible that the edtFTP library can handle all ftp
 * links, I did not have time to test this. If so it is easier to make all
 * methods static.
 * 
 * @author Mark Van Peteghem
 */
public class Download {
    private static Download download = new Download();

    private Download() {
    }

    private DownloadObservable downloadObservable = DownloadObservable.get();

    static Download get() {
        return download;
    }

    public DownloadObservable getDownloadObservable() {
        return downloadObservable;
    }

    private String getIndent() {
        StringBuffer str = new StringBuffer();
        int level = Level.Get();
        for (int l=0; l<level; ++l)
            str.append(" ");
        return str.toString();
    }

    protected DownloadedPage getPage(PostableUrl url) throws Exception {
        String strUrl = url.getLink();
        System.out.println(getIndent()+"Loading page "+strUrl);

        // sometimes loading a page gives an error, that may work fine the next
        // time, so we first try it three times ignoring exceptions, then
        // once more without ignoring exceptions
        for (int i=0; i<3; ++i) {
            try {
                DownloadedPage page = getPageAndCheckRedirect(url);
                return page;
            } catch (Exception ex) {
                System.err.println(getIndent()+"Error loading page "+strUrl+"\nMessage: "+ex+"\nRetrying...");
            }
            Thread.sleep(1000);
        }
        return getPageAndCheckRedirect(url);
    }

    private StringBuffer buffer = new StringBuffer();

    protected DownloadedPage getPageAndCheckRedirect(PostableUrl postableUrl) throws Exception {
    	DownloadedPage page = getPageTry(postableUrl);
    	if (null==page)
    		return page; // it was probably interrupted
    	
    	String content = page.getContent();
    	
    	String refreshStart = "<meta http-equiv=\"refresh\"";
    	if (content.contains(refreshStart)) {
    		String refreshPart = ParseUtils.findSubstringByHeaderString(content, refreshStart, ">", false);
    		String newUrl;
    		if (refreshPart.contains("URL=\""))
    			newUrl = ParseUtils.findSubstringByHeaderString(refreshPart, "URL=\"", "\"", false);
    		else
    			newUrl = ParseUtils.findSubstringByHeaderString(refreshPart, "URL=", "\"", false);
    		int slashPos = postableUrl.getLink().indexOf('/', 8);
    		if (newUrl.startsWith("/")) {
    			newUrl = postableUrl.getLink().substring(0, slashPos)+newUrl;
    		} else if (!newUrl.contains("/")) {
    			newUrl = postableUrl.getLink().substring(0, slashPos+1)+newUrl;
    		}
    		PostableUrl newPostableUrl = new PostableUrl(newUrl, postableUrl.getPostData());
    		page = getPageTry(newPostableUrl);
    	}
    	return page;
    }
    
    protected DownloadedPage getPageTry(PostableUrl postableUrl) throws Exception {
        HttpURLConnection.setFollowRedirects(true);

        URLConnection connection = null;
        BufferedReader in = null;
        try {
            URL url = new URL(postableUrl.getLink());
            connection = url.openConnection();
            
            Map<String, String> post = postableUrl.getPostData();
            if (null!=post && !post.isEmpty()) {
            	((HttpURLConnection)connection).setRequestMethod("POST");
            	connection.setDoOutput(true);
            	connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	            String strPost = "";
	            for (Map.Entry<String, String> pair: post.entrySet()) {
	                strPost = strPost+URLEncoder.encode(pair.getKey(), "UTF-8")+"="+URLEncoder.encode(pair.getValue(), "UTF-8")+"&";
	            }
	            strPost = strPost.substring(0, strPost.length()-1);
	            OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
	            wr.write(strPost);
	            wr.flush();
            }

            connection.connect();
            // It is very important to get the header fields,
            // otherwise the redirected url is not in the connection object
            Map<String, List<String>> headers = connection.getHeaderFields();
            URL url2 = connection.getURL();
            if (!url2.toString().equals(postableUrl.getLink())) {
            	System.out.println(getIndent()+"  redirected to "+url2.toString());

            	String newLink = url2.toString();
                if (newLink.contains(" ")) {
                	// This is necessary for Dell:
                	newLink = newLink.replaceAll(" ", "%20");
                	System.out.println(getIndent()+"  corrected to  "+newLink);
                	postableUrl = new PostableUrl(newLink, postableUrl.getPostData());
                	return getPageTry(postableUrl);
                }

            }
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            buffer.setLength(0);
            char cbuf[] = new char[1000];
            while (true) {
                int nrBytes = in.read(cbuf);
                if (nrBytes<0)
                    break;
                if (Interrupt.get())
                    return null;
                buffer.append(cbuf, 0, nrBytes);
            }
            return new DownloadedPage(buffer.toString(), url2.toString());
        }
        finally {
            if (in!=null)
                try {
                    in.close();
                } catch (IOException ex) {
                }
        }
    }

    protected void downloadFile(String strUrl, String localPath) throws Exception {

        final int nrTries = 3;
        for (int i=1; i<=nrTries; ++i) {
            try {
                downloadFileTry(strUrl, localPath);
                return;
            } catch (Exception ex) {
                // Ignore exceptions on first tries
            	if (nrTries==i) {
            		MessageHandler.addError(ex);
            	}
            }
            downloadObservable.updateFilename("Retrying");
            Thread.sleep(1000);
        }
    }

    enum DownloadMethod { URLConnection, ApacheNet, edtFTP }

    private DownloadMethod getDownloadMethod(String url) {
        
    	url = url.replaceAll(" ", "%20");
    	
    	// URLConnection can't handle FTP very well with some servers,
        // so we use the Apache Commons or the edtFTP library for that.
    	// Which of those two depends on experience.

    	if (url.startsWith("ftp://")) {
	    	if (url.contains("sony.com"))
	    		return DownloadMethod.edtFTP;
	    	if (url.contains("gateway.com"))
	    		return DownloadMethod.ApacheNet;
	    	if (url.contains("hp.com"))
	    		return DownloadMethod.ApacheNet;
    	}
    	return DownloadMethod.URLConnection;
    }

    protected void downloadFileTry(String strUrl, String localPath) throws Exception {
        InputStream raw = null;
        InputStream in = null;
        FileOutputStream out = null;
        long totalBytesRead = 0;
        long fileLength = 0;
        FTPClient ftp = null;
        FileTransferClient ftp2 = null;
        try {
            String filename = strUrl.substring(strUrl.lastIndexOf('/') + 1);
            int questionMarkPos = filename.indexOf("?");
            if (questionMarkPos>=0)
                filename = filename.substring(0, questionMarkPos);
            filename = filename.replaceAll("%20", " ");
            
            ExceptionList.load(); // load every time
            if (ExceptionList.has(filename)) {
                System.out.println("Skipping file "+strUrl);
            	return;
            } else {
                System.out.println("Download file "+strUrl);            	
            }

            if (GlobalOptions.isDryRun())
                return;

            String fullFilename = localPath+File.separator+filename;
            File file = new File(fullFilename);
            if (file.exists())
                return;

            downloadObservable.updateFilename(filename);

            String userNameAndPassWord[] = getUserNameAndPassWord(strUrl);
            String userName = userNameAndPassWord[0];
            if (null==userName)
                    userName = "anonymous";
            String passWord = userNameAndPassWord[1];
            if (null==passWord)
                passWord = "";
            String server = userNameAndPassWord[2];
            String remoteFilename = userNameAndPassWord[3];

            DownloadMethod downloadMethod = getDownloadMethod(strUrl);

            if (DownloadMethod.edtFTP==downloadMethod) {
            	ftp2 = new FileTransferClient();

                // Use passive mode as default because most of us are
                // behind fire walls these days.
            	ftp2.getAdvancedFTPSettings().setConnectMode(FTPConnectMode.PASV);

            	ftp2.setRemoteHost(server);
            	
            	ftp2.setUserName(userName);
            	ftp2.setPassword(passWord);
            	
            	for (int i=0; i<2; ++i) {
            		try {
		            	ftp2.connect();
		            	raw = ftp2.downloadStream(remoteFilename);
		            	if (null!=raw)
		            		break;
            		} catch (Exception ex) {
            			raw = null;
            		} finally {
            			if (null==raw)
            				ftp2.disconnect();
            		}
            	}
            	
    			if (null==raw) {
	            	// try once more without catching exceptions:
	            	ftp2.connect();
	            	raw = ftp2.downloadStream(remoteFilename);
    			}
            	
            } else if (DownloadMethod.ApacheNet==downloadMethod) {

                int sepPos = remoteFilename.lastIndexOf("/");
                String remoteDirectory;
                if (sepPos<0)
                    remoteDirectory = "";
                else
                    remoteDirectory = remoteFilename.substring(0, sepPos);

                ftp = new FTPClient();
                // Try to connect 3 times ignoring exceptions
                for (int i=0; i<3; ++i) {
                    try {
                        ftp.connect(server);
                        break;
                    } catch (Exception ex) {
                    }
                }
                if (!ftp.isConnected())
                    // if still not connected, try once more without ignoring exceptions
                    ftp.connect(server);

                // After connection attempt, you should check the reply code to verify
                // success.
                int reply = ftp.getReplyCode();

                if (!FTPReply.isPositiveCompletion(reply))
                {
                    ftp.disconnect();
                    throw new Exception("ftp server at "+server+" did not reply positively");
                }
                if (null!=userName && !ftp.login(userName, passWord))
                {
                    ftp.logout();
                    String msg = "ftp server at "+server+" did not accept login "+userName;
                    if (null!=passWord) {
                        msg = msg+", password "+passWord;
                    }
                    throw new Exception(msg);
                }

                //System.out.println("Remote system is " + ftp.getSystemName());

                ftp.setFileType(FTP.BINARY_FILE_TYPE);

                // Use passive mode as default because most of us are
                // behind firewalls these days.
                ftp.enterLocalPassiveMode();

                FTPFile files[] = ftp.listFiles(remoteFilename);
                if (files.length==1)
                    fileLength = files[0].getSize();

                // Try 3 times to retrieve the input stream ignoring exceptions
                for (int i=0; i<3; ++i) {
                    try {
                        boolean b = ftp.changeWorkingDirectory(remoteDirectory);
                        raw = ftp.retrieveFileStream(filename);
                        if (null!=raw)
                            break;
                    } catch (Exception ex) {
                    }
                }
                if (null==raw)
                    // If still not successful, try once more without ignoring exceptions
                    raw = ftp.retrieveFileStream(remoteFilename);
            } else {
                URL url = new URL(strUrl);
                URLConnection urlConnection = url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.connect();
                fileLength = urlConnection.getContentLength();
                String contentType = urlConnection.getContentType();
                raw = urlConnection.getInputStream();
            }

            in = new BufferedInputStream(raw);

            String fullFilenameIncomplete = fullFilename+".incomplete";
            File incompleteFile = new File(fullFilenameIncomplete);
            if (incompleteFile.exists()) {
                // remove if an incomplete file still exists from previous failed download attempt
                incompleteFile.delete();
            }
            out = new FileOutputStream(fullFilenameIncomplete);

            byte[] data = new byte[1000];
            int bytesRead = 0;
            while (true) {
                bytesRead = in.read(data, 0, data.length);
                if (bytesRead == -1)
                    break;
                out.write(data, 0, bytesRead);
                totalBytesRead += bytesRead;
                downloadObservable.updateProgress(totalBytesRead);
                if (fileLength!=0 && totalBytesRead>=fileLength)
                    break;
                if (Interrupt.get())
                    break;
            }
            out.close();
            out = null;
            if (!Interrupt.get())
                incompleteFile.renameTo(file);

        } catch (Exception ex) {
            String msg = "There was an error while trying to load the file:\n"+ex;
            MessageHandler.addError(msg);
        } finally {
            if (in!=null)
                in.close();
            if (raw!=null)
                raw.close();
            if (out!=null) {
                out.flush();
                out.close();
            }
            if (null!=ftp) {
                ftp.logout();
                ftp.disconnect();
            }
            if (null!=ftp2) {
                ftp2.disconnect();
            }
        }
        downloadObservable.updateProgress(-1);
        downloadObservable.updateFilename("");
    }

    static public String[] getUserNameAndPassWord(String strUrl) {
        String userName = null;
        String passWord = null;
        String server, file;
        int doubleSlashPos = strUrl.indexOf("//"); // skip slashes of ftp:// or http://
        if (doubleSlashPos<0)
        	doubleSlashPos = 0;
        int slashPos = strUrl.indexOf("/", doubleSlashPos+2);
        int atPos = strUrl.indexOf("@");
        if (slashPos >= 0 && atPos >= 0 && atPos < slashPos) {
            // there is a @ in the first part, so a username and maybe password is given
            server = strUrl.substring(atPos+1, slashPos);
            int colonPos = strUrl.indexOf(":", 7);
            if (colonPos >= 0 && colonPos < atPos) {
                userName = strUrl.substring(6, colonPos);
                passWord = strUrl.substring(colonPos + 1, atPos);
            } else {
                userName = strUrl.substring(6, atPos);
            }
        } else {
            server = strUrl.substring(6, slashPos);
        }
        file = strUrl.substring(slashPos+1);
        return new String[] { userName, passWord, server, file };
    }
}
