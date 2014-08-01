jar -cmf MANIFEST.MF DriverDownloader.jar -C bin driverdownloader

mkdir dist
mkdir dist\lib
mv DriverDownloader.jar dist
cp "C:\Documents and Settings\Mark\My Documents\JavaLibs\edtftpj.jar" dist\lib
cp "C:\Documents and Settings\Mark\My Documents\JavaLibs\commons-net-2.0\commons-net-2.0.jar" dist\lib
cp "C:\Documents and Settings\Mark\My Documents\JavaLibs\json.jar" dist\lib
cp "C:\Documents and Settings\Mark\My Documents\JavaLibs\miglayout-3.7-swing.jar" dist\lib
